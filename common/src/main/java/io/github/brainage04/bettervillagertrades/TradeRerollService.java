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
        if (player instanceof ServerPlayer serverPlayer) {
            rerollUnlessCoolingDown(serverPlayer, villager);
        }
        return InteractionResult.SUCCESS;
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
        return rerollUnlessCoolingDown(player, villager.get()) ? 1 : 0;
    }

    private static boolean rerollUnlessCoolingDown(ServerPlayer player, Villager villager) {
        int tick = player.level().getServer().getTickCount();
        Integer cooldownEnd = COOLDOWN_END_TICKS.get(player);
        if (cooldownEnd != null && tick < cooldownEnd) {
            return false;
        }
        boolean rerolled = reroll(player, villager, true);
        if (rerolled) {
            COOLDOWN_END_TICKS.put(player, tick + REROLL_COOLDOWN_TICKS);
        }
        return rerolled;
    }

    public static boolean reroll(ServerPlayer player, Villager villager, boolean sendFeedback) {
        Component denial = eligibilityFailure(player, villager);
        if (denial != null) {
            if (sendFeedback) player.sendSystemMessage(denial);
            return false;
        }

        MerchantOffers offers = villager.getOffers();
        Optional<TradeFilter> protectedBy =
                PlayerTradeFilters.get(player.level().getServer()).protectedBy(player, offers);
        if (protectedBy.isPresent()) {
            if (sendFeedback) {
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
            if (sendFeedback)
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
        if (sendFeedback)
            player.sendSystemMessage(
                    Component.translatableWithFallback(
                            "bettervillagertrades.reroll.success", "Villager trades rerolled."));
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
        if (data.profession().value().getTrades(data.level()) == null) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.no_profession",
                    "This villager needs a trading profession before its trades can be rerolled.");
        }
        if (data.level() != 1 || villager.getVillagerXp() != 0) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.experienced",
                    "Only novice villagers with no trade experience can be rerolled.");
        }
        if (villager.getOffers().stream().anyMatch(offer -> offer.getUses() > 0)) {
            return Component.translatableWithFallback(
                    "bettervillagertrades.reroll.used",
                    "This villager is locked because one of its trades has already been used.");
        }
        return null;
    }
}
