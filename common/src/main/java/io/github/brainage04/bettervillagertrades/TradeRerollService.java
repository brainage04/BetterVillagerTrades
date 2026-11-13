package io.github.brainage04.bettervillagertrades;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;

public final class TradeRerollService {
    public static final int REROLL_COOLDOWN_TICKS = 10;

    private static final Map<ServerPlayer, Integer> COOLDOWN_END_TICKS = new WeakHashMap<>();

    private TradeRerollService() {}

    public static InteractionResult interact(Player player, Entity target, InteractionHand hand) {
        if (!(target instanceof Villager villager)
                || player.isSpectator()
                || hand != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || !player.getMainHandItem().is(Items.EMERALD)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // Only the server holds the offers and the stored filters, so the client cannot decide
            // and must leave the interaction to the attempt packet it sends.
            return InteractionResult.PASS;
        }
        if (coolingDown(serverPlayer)) {
            // A held use key sends one interaction every 4 ticks (Minecraft.java:1693,1965), so
            // without this the sneak-click would reroll once and then open the screen 4 ticks
            // later. The cooldown consumes the gesture, as it did before trading opened here.
            return InteractionResult.SUCCESS;
        }
        // A refusal still sends its reason, as it did before the refusal earned a trading screen.
        if (reroll(serverPlayer, villager, true)) {
            startCooldown(serverPlayer);
            announceReroll(serverPlayer);
        } else {
            openTrading(serverPlayer, villager, hand);
        }
        return InteractionResult.SUCCESS;
    }

    private static void openTrading(ServerPlayer player, Villager villager, InteractionHand hand) {
        // NeoForge will not trade while the player is sneaking: its Villager.mobInteract returns
        // the Mob result, which opens nothing, when Player.isSecondaryUseActive is true (the
        // isSecondaryUseActive branch its patches add to Villager.mobInteract). Clearing the flag
        // for this call opens trading through vanilla's own guards and screen on both loaders. The
        // villager's interact never posts NeoForge's PlayerInteractEvent, so this cannot re-enter
        // the mod.
        boolean sneaking = player.isShiftKeyDown();
        player.setShiftKeyDown(false);
        try {
            villager.interact(player, hand, villager.position());
        } finally {
            player.setShiftKeyDown(sneaking);
        }
    }

    public static int rerollTradingVillager(ServerPlayer player) {
        // AbstractVillager.stillValid (AbstractVillager.java:305) keeps the trading screen open
        // out to the entity interaction range plus 4 blocks, Container.DEFAULT_DISTANCE_BUFFER.
        double tradingRange = player.entityInteractionRange() + Container.DEFAULT_DISTANCE_BUFFER;
        Optional<Villager> villager =
                player
                        .level()
                        .getEntitiesOfClass(
                                Villager.class,
                                player.getBoundingBox().inflate(tradingRange),
                                candidate -> candidate.stillValid(player))
                        .stream()
                        .findFirst();
        if (villager.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatableWithFallback(
                            "bettervillagertrades.reroll.open_screen",
                            "Open a villager trading screen before using this reroll command."));
            return 0;
        }
        if (!rerollUnlessCoolingDown(player, villager.get(), true)) {
            return 0;
        }
        announceReroll(player);
        return 1;
    }

    private static boolean rerollUnlessCoolingDown(
            ServerPlayer player, Villager villager, boolean announceDenial) {
        if (coolingDown(player)) {
            return false;
        }
        boolean rerolled = reroll(player, villager, announceDenial);
        if (rerolled) {
            startCooldown(player);
        }
        return rerolled;
    }

    private static boolean coolingDown(ServerPlayer player) {
        Integer cooldownEnd = COOLDOWN_END_TICKS.get(player);
        return cooldownEnd != null && player.level().getServer().getTickCount() < cooldownEnd;
    }

    private static void startCooldown(ServerPlayer player) {
        COOLDOWN_END_TICKS.put(
                player, player.level().getServer().getTickCount() + REROLL_COOLDOWN_TICKS);
    }

    private static void announceReroll(ServerPlayer player) {
        player.sendSystemMessage(
                Component.translatableWithFallback(
                        "bettervillagertrades.reroll.success", "Villager trades rerolled."));
    }

    public static boolean reroll(ServerPlayer player, Villager villager, boolean announceDenial) {
        Component denial = eligibilityFailure(player, villager);
        if (denial != null) {
            if (announceDenial) player.sendSystemMessage(denial);
            return false;
        }

        MerchantOffers offers = villager.getOffers();
        Optional<TradeFilter> protectedBy =
                PlayerTradeFilters.get(player.level().getServer()).protectedBy(player, offers);
        if (protectedBy.isPresent()) {
            if (announceDenial) {
                player.sendSystemMessage(
                        Component.translatableWithFallback(
                                "bettervillagertrades.reroll.protected",
                                "Reroll blocked: this villager offers your protected trade %s.",
                                protectedBy.get().description()));
            }
            return false;
        }

        if (!(villager instanceof VillagerTradeReroller reroller)) {
            BetterVillagerTrades.LOGGER.error("Villager reroll mixin was not applied.");
            if (announceDenial)
                player.sendSystemMessage(
                        Component.translatableWithFallback(
                                "bettervillagertrades.reroll.unavailable",
                                "Trade rerolling is unavailable because the server mixin did not load."));
            return false;
        }

        reroller.betterVillagerTrades$rerollTrades(player.level());
        if (player.containerMenu instanceof MerchantMenu menu
                && villager.getTradingPlayer() == player) {
            menu.getSlot(0).setChanged();
            player.sendMerchantOffers(
                    menu.containerId,
                    villager.getOffers(),
                    villager.getVillagerData().level(),
                    villager.getVillagerXp(),
                    villager.showProgressBar(),
                    villager.canRestock());
            menu.broadcastChanges();
        }
        return true;
    }

    private static Component eligibilityFailure(ServerPlayer player, Villager villager) {
        if (villager.getTradingPlayer() != null && villager.getTradingPlayer() != player) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.busy",
                    "This villager is currently trading with another player.");
        }
        if (villager.isBaby()) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.baby",
                    "Baby villagers do not have trades to reroll.");
        }
        VillagerData data = villager.getVillagerData();
        if (data.level() != VillagerData.MIN_VILLAGER_LEVEL || villager.getVillagerXp() != 0) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.experienced",
                    "Only novice villagers with no trade experience can be rerolled.");
        }
        if (data.profession().value().getTrades(data.level()) == null) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.no_profession",
                    "This villager needs a trading profession before its trades can be rerolled.");
        }
        if (villager.getOffers().stream().anyMatch(offer -> offer.getUses() > 0)) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.used",
                    "This villager is locked because one of its trades has already been used.");
        }
        return null;
    }
}
