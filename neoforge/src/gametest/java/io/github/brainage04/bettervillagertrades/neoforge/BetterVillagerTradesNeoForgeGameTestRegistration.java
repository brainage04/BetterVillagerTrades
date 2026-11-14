package io.github.brainage04.bettervillagertrades.neoforge;

import io.github.brainage04.bettervillagertrades.BetterVillagerTrades;
import io.github.brainage04.bettervillagertrades.BetterVillagerTradesGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Each function needs a matching {@code data/<mod_id>/test_instance/<name>.json} in this source
 * set's resources.
 */
@EventBusSubscriber(modid = BetterVillagerTrades.MOD_ID)
public final class BetterVillagerTradesNeoForgeGameTestRegistration {
    private BetterVillagerTradesNeoForgeGameTestRegistration() {}

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BetterVillagerTrades.MOD_ID, "generated_villager_trades_are_maxed"),
                () -> BetterVillagerTradesGameTests::generatedVillagerTradesAreMaxed);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BetterVillagerTrades.MOD_ID,
                        "gamerules_independently_control_trade_outputs"),
                () ->
                        BetterVillagerTradesGameTests
                                ::gamerulesIndependentlyControlBookAndItemTradeOutputs);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        BetterVillagerTrades.MOD_ID,
                        "rerolls_untraded_villagers_without_protected_offers"),
                () ->
                        BetterVillagerTradesGameTests
                                ::rerollsUntradedVillagersWithoutProtectedOffers);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("filter_commands_validate_and_isolate_players"),
                () -> BetterVillagerTradesGameTests::filterCommandsValidateAndIsolatePlayers);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("rejects_ineligible_villagers_and_preserves_emeralds"),
                () ->
                        BetterVillagerTradesGameTests
                                ::rejectsIneligibleVillagersAndPreservesEmeralds);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("filters_match_both_enchantment_components"),
                () -> BetterVillagerTradesGameTests::filtersMatchBothEnchantmentComponents);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("registered_gamerules_control_only_new_offers"),
                () -> BetterVillagerTradesGameTests::registeredGamerulesControlOnlyNewOffers);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("reroll_recalculates_open_merchant_result"),
                () -> BetterVillagerTradesGameTests::rerollRecalculatesOpenMerchantResult);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("spectators_cannot_reroll"),
                () -> BetterVillagerTradesGameTests::spectatorsCannotReroll);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("off_hand_emeralds_pass_through"),
                () -> BetterVillagerTradesGameTests::offHandEmeraldsPassThrough);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("professions_without_novice_trades_are_rejected"),
                () -> BetterVillagerTradesGameTests::professionsWithoutNoviceTradesAreRejected);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("level_up_maximizes_only_the_new_offers"),
                () -> BetterVillagerTradesGameTests::levelUpMaximizesOnlyTheNewOffers);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("reroll_command_reaches_the_whole_trading_range"),
                () -> BetterVillagerTradesGameTests::rerollCommandReachesTheWholeTradingRange);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("reroll_cooldown_spaces_repeated_rerolls"),
                () -> BetterVillagerTradesGameTests::rerollCooldownSpacesRepeatedRerolls);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                BetterVillagerTrades.of("saved_filters_skip_vanilla_data_fixers"),
                () -> BetterVillagerTradesGameTests::savedFiltersSkipVanillaDataFixers);
    }
}
