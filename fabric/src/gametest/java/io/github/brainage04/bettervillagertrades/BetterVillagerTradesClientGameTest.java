package io.github.brainage04.bettervillagertrades;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@SuppressWarnings("UnstableApiUsage")
public final class BetterVillagerTradesClientGameTest implements FabricClientGameTest {
    private static final ClientboundCommandsPacket.NodeInspector<Object> NO_COMMAND_DETAILS =
            new ClientboundCommandsPacket.NodeInspector<>() {
                @Override
                public Identifier suggestionId(ArgumentCommandNode<Object, ?> node) {
                    return null;
                }

                @Override
                public boolean isExecutable(CommandNode<Object> node) {
                    return false;
                }

                @Override
                public boolean isRestricted(CommandNode<Object> node) {
                    return false;
                }
            };
    private static final List<String> CONTROL_LABELS = List.of("Reroll trades", "Toggle filter");
    private static final String OTHER_MOD_LABEL = "Other mod";

    @Override
    public void runTest(ClientGameTestContext context) {
        ClientGameTestServers.withDedicatedServer(
                context,
                ClientGameTestServers.flatServerProperties(),
                "Better Villager Trades visual GameTest",
                server -> {
                    try {
                        server.runOnServer(BetterVillagerTradesShowcase::stage);
                        ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                        context.waitTicks(20);
                        BetterVillagerTradesShowcase.showMaxBook(context);

                        server.runOnServer(BetterVillagerTradesClientGameTest::openMerchantScreen);
                        context.waitForScreen(MerchantScreen.class);
                        context.waitFor(
                                client ->
                                        client.player.containerMenu instanceof MerchantMenu menu
                                                && !menu.getOffers().isEmpty());
                        BetterVillagerTradesShowcase.captionRerollControls(context);
                        assertMerchantControls(
                                context,
                                CONTROL_LABELS,
                                "Expected both BetterVillagerTrades controls in the trading screen.");
                        AtomicBoolean serverState = new AtomicBoolean();
                        context.clickScreenButton("Toggle filter");
                        waitForServerCondition(
                                context,
                                "Filter button must add protection",
                                () -> {
                                    server.runOnServer(
                                            s -> {
                                                ServerPlayer player =
                                                        s.getPlayerList().getPlayers().getFirst();
                                                serverState.set(
                                                        !PlayerTradeFilters.get(s)
                                                                .get(player)
                                                                .isEmpty());
                                            });
                                    return serverState.get();
                                });
                        context.clickScreenButton("Reroll trades");
                        context.clickScreenButton("Toggle filter");
                        waitForServerCondition(
                                context,
                                "Filter button must remove protection",
                                () -> {
                                    server.runOnServer(
                                            s ->
                                                    serverState.set(
                                                            PlayerTradeFilters.get(s)
                                                                    .get(
                                                                            s.getPlayerList()
                                                                                    .getPlayers()
                                                                                    .getFirst())
                                                                    .isEmpty()));
                                    return serverState.get();
                                });
                        server.runOnServer(
                                s -> {
                                    if (librarian(s).getOffers().size() != 1)
                                        throw new AssertionError(
                                                "Protected button reroll must preserve offers.");
                                });
                        context.clickScreenButton("Reroll trades");
                        context.waitFor(
                                client ->
                                        client.player.containerMenu instanceof MerchantMenu menu
                                                && menu.getOffers().size() > 1);
                        server.runOnServer(
                                s -> {
                                    if (librarian(s).getOffers().size() <= 1)
                                        throw new AssertionError(
                                                "Reroll button must replace server offers.");
                                });

                        BetterVillagerTradesShowcase.showMaxEquipment(context);

                        Runnable openOnServer =
                                () ->
                                        server.runOnServer(
                                                BetterVillagerTradesClientGameTest
                                                        ::openMerchantScreen);
                        sendCommandTree(context, commandTree("reroll"));
                        reopenMerchantScreen(context, openOnServer);
                        assertMerchantControls(
                                context,
                                List.of("Reroll trades"),
                                "Without filter toggle in the command tree, only Reroll trades"
                                        + " must be shown.");

                        sendCommandTree(context, commandTree("filter", "toggle"));
                        reopenMerchantScreen(context, openOnServer);
                        assertMerchantControls(
                                context,
                                List.of("Toggle filter"),
                                "Without reroll in the command tree, only Toggle filter must be"
                                        + " shown.");

                        sendCommandTree(context, new RootCommandNode<>());
                        reopenMerchantScreen(context, openOnServer);
                        assertMerchantControls(
                                context,
                                List.of(),
                                "A server without the bettervillagertrades command must not get"
                                        + " the trading-screen controls.");

                        // A permission change resends the command tree while the screen stays
                        // open (PlayerList.java:580), so the controls follow it without a reopen.
                        // Fabric mods add their own buttons in ScreenEvents.AFTER_INIT through
                        // Screens.getWidgets, which Fabric fires again only on init and resize,
                        // so following the tree must leave those buttons in place.
                        addOtherModButton(context);
                        sendCommandTree(context, commandTree("reroll"));
                        context.waitTick();
                        assertMerchantControls(
                                context,
                                List.of("Reroll trades"),
                                "Gaining reroll while trading must show Reroll trades without"
                                        + " reopening the screen.");
                        assertOtherModButtonKept(context, "gaining reroll");
                        sendCommandTree(context, new RootCommandNode<>());
                        context.waitTick();
                        assertMerchantControls(
                                context,
                                List.of(),
                                "Losing reroll while trading must remove Reroll trades without"
                                        + " reopening the screen.");
                        assertOtherModButtonKept(context, "losing reroll");
                    } finally {
                        server.runOnServer(BetterVillagerTradesShowcase::cleanup);
                    }
                });
    }

    private static Villager librarian(MinecraftServer server) {
        return (Villager) server.overworld().getEntity(BetterVillagerTradesShowcase.librarianId());
    }

    private static void waitForServerCondition(
            ClientGameTestContext context, String message, BooleanSupplier condition) {
        for (int tick = 0; tick < 100; tick++) {
            if (condition.getAsBoolean()) return;
            context.waitTick();
        }
        throw new AssertionError(message);
    }

    private static void openMerchantScreen(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        player.closeContainer();
        Villager librarian = librarian(server);
        MerchantOffers offers = new MerchantOffers();
        offers.add(
                new MerchantOffer(
                        new ItemCost(Items.EMERALD),
                        player.getInventory().getItem(0).copy(),
                        1,
                        1,
                        0.0F));
        librarian.setOffers(offers);
        librarian.setTradingPlayer(player);
        librarian.openTradingScreen(player, librarian.getDisplayName(), 1);
    }

    private static RootCommandNode<Object> commandTree(String... path) {
        LiteralArgumentBuilder<Object> node = LiteralArgumentBuilder.literal(path[path.length - 1]);
        for (int i = path.length - 2; i >= 0; i--) {
            node = LiteralArgumentBuilder.literal(path[i]).then(node);
        }
        RootCommandNode<Object> root = new RootCommandNode<>();
        root.addChild(LiteralArgumentBuilder.literal("bettervillagertrades").then(node).build());
        return root;
    }

    private static void sendCommandTree(
            ClientGameTestContext context, RootCommandNode<Object> root) {
        context.runOnClient(
                client ->
                        client.getConnection()
                                .handleCommands(
                                        new ClientboundCommandsPacket(root, NO_COMMAND_DETAILS)));
    }

    // The previous trading screen is still open, so waiting for any MerchantScreen could match it
    // and see buttons built from the previous command tree.
    private static void reopenMerchantScreen(ClientGameTestContext context, Runnable openOnServer) {
        Screen previous = context.computeOnClient(client -> client.gui.screen());
        openOnServer.run();
        context.waitFor(
                client ->
                        client.gui.screen() instanceof MerchantScreen
                                && client.gui.screen() != previous);
    }

    private static void assertMerchantControls(
            ClientGameTestContext context, List<String> expected, String message) {
        context.runOnClient(
                client -> {
                    List<String> controls =
                            merchantButtonLabels(client).stream()
                                    .filter(CONTROL_LABELS::contains)
                                    .toList();
                    if (!controls.equals(expected)) {
                        throw new AssertionError(message + " Shown: " + controls);
                    }
                });
    }

    private static void addOtherModButton(ClientGameTestContext context) {
        context.runOnClient(
                client -> {
                    if (!(client.gui.screen() instanceof MerchantScreen screen)) {
                        throw new AssertionError(
                                "Expected the villager trading screen to be open.");
                    }
                    Screens.getWidgets(screen)
                            .add(
                                    Button.builder(Component.literal(OTHER_MOD_LABEL), button -> {})
                                            .bounds(0, 0, 60, 20)
                                            .build());
                });
    }

    private static void assertOtherModButtonKept(ClientGameTestContext context, String change) {
        context.runOnClient(
                client -> {
                    if (!merchantButtonLabels(client).contains(OTHER_MOD_LABEL)) {
                        throw new AssertionError(
                                "Another mod's trading-screen button must survive "
                                        + change
                                        + " while trading.");
                    }
                });
    }

    private static List<String> merchantButtonLabels(Minecraft client) {
        if (!(client.gui.screen() instanceof MerchantScreen screen)) {
            throw new AssertionError("Expected the villager trading screen to be open.");
        }
        return screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .map(button -> button.getMessage().getString())
                .toList();
    }
}
