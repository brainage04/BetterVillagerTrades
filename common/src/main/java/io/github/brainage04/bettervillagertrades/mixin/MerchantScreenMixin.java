package io.github.brainage04.bettervillagertrades.mixin;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.bettervillagertrades.BetterVillagerTradesCommands;
import io.github.brainage04.bettervillagertrades.TradeFilter;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Shadow private int shopItem;
    @Unique private CommandDispatcher<?> betterVillagerTrades$commands;
    @Unique private Button betterVillagerTrades$reroll;
    @Unique private Button betterVillagerTrades$filter;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void betterVillagerTrades$addControls(CallbackInfo ci) {
        int controlsY = topPos + imageHeight + 4;
        betterVillagerTrades$reroll =
                Button.builder(
                                Component.translatable("bettervillagertrades.button.reroll"),
                                button ->
                                        betterVillagerTrades$sendCommand(
                                                String.join(
                                                        " ",
                                                        BetterVillagerTradesCommands.REROLL_PATH)))
                        .bounds(leftPos + 5, controlsY, 96, 20)
                        .build();
        betterVillagerTrades$filter =
                Button.builder(
                                Component.translatable("bettervillagertrades.button.filter"),
                                button -> betterVillagerTrades$toggleSelectedFilter())
                        .bounds(leftPos + 105, controlsY, 96, 20)
                        .build();
        betterVillagerTrades$commands = null;
        betterVillagerTrades$showControls();
    }

    // MerchantScreen inherits containerTick (AbstractContainerScreen.java:563 in 26.3; neither
    // MerchantScreen.java nor NeoForge's patch of it declares one), so there is nothing to inject
    // into and this adds the override.
    @Override
    protected void containerTick() {
        super.containerTick();
        betterVillagerTrades$showControls();
    }

    // Every command tree the server sends replaces the dispatcher (ClientPacketListener.java:1695).
    // Only these two buttons are swapped: rebuildWidgets would also drop the buttons other mods
    // added, since Fabric runs ScreenEvents.AFTER_INIT again only from init(II)V and resize
    // (fabric-screen-api-v1 5.2.4, ScreenMixin).
    @Unique
    private void betterVillagerTrades$showControls() {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || connection.getCommands() == betterVillagerTrades$commands) {
            return;
        }
        betterVillagerTrades$commands = connection.getCommands();
        removeWidget(betterVillagerTrades$reroll);
        removeWidget(betterVillagerTrades$filter);
        if (BetterVillagerTradesCommands.isAvailable(
                betterVillagerTrades$commands, BetterVillagerTradesCommands.REROLL_PATH)) {
            addRenderableWidget(betterVillagerTrades$reroll);
        }
        if (BetterVillagerTradesCommands.isAvailable(
                betterVillagerTrades$commands, BetterVillagerTradesCommands.FILTER_TOGGLE_PATH)) {
            addRenderableWidget(betterVillagerTrades$filter);
        }
    }

    @Unique
    private void betterVillagerTrades$toggleSelectedFilter() {
        MerchantOffers offers = menu.getOffers();
        if (shopItem < 0 || shopItem >= offers.size()) {
            return;
        }
        TradeFilter filter = TradeFilter.fromStack(offers.get(shopItem).getResult());
        betterVillagerTrades$sendCommand(
                String.join(" ", BetterVillagerTradesCommands.FILTER_TOGGLE_PATH)
                        + " "
                        + filter.commandArguments());
    }

    @Unique
    private void betterVillagerTrades$sendCommand(String command) {
        if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand(command);
        }
    }
}
