package io.github.brainage04.bettervillagertrades;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class BetterVillagerTradesGameTest {
    @GameTest(maxTicks = 40)
    public void generatedVillagerTradesAreMaxed(GameTestHelper helper) {
        BetterVillagerTradesGameTests.generatedVillagerTradesAreMaxed(helper);
    }

    @GameTest
    public void gamerulesIndependentlyControlBookAndItemTradeOutputs(GameTestHelper helper) {
        BetterVillagerTradesGameTests.gamerulesIndependentlyControlBookAndItemTradeOutputs(helper);
    }

    @GameTest(maxTicks = 40)
    public void rerollsUntradedVillagersWithoutProtectedOffers(GameTestHelper helper) {
        BetterVillagerTradesGameTests.rerollsUntradedVillagersWithoutProtectedOffers(helper);
    }

    @GameTest
    public void filterCommandsValidateAndIsolatePlayers(GameTestHelper helper) {
        BetterVillagerTradesGameTests.filterCommandsValidateAndIsolatePlayers(helper);
    }

    @GameTest
    public void rejectsIneligibleVillagersAndPreservesEmeralds(GameTestHelper helper) {
        BetterVillagerTradesGameTests.rejectsIneligibleVillagersAndPreservesEmeralds(helper);
    }

    @GameTest
    public void filtersMatchBothEnchantmentComponents(GameTestHelper helper) {
        BetterVillagerTradesGameTests.filtersMatchBothEnchantmentComponents(helper);
    }

    @GameTest
    public void registeredGamerulesControlOnlyNewOffers(GameTestHelper helper) {
        BetterVillagerTradesGameTests.registeredGamerulesControlOnlyNewOffers(helper);
    }

    @GameTest
    public void rerollRecalculatesOpenMerchantResult(GameTestHelper helper) {
        BetterVillagerTradesGameTests.rerollRecalculatesOpenMerchantResult(helper);
    }

    @GameTest
    public void spectatorsCannotReroll(GameTestHelper helper) {
        BetterVillagerTradesGameTests.spectatorsCannotReroll(helper);
    }

    @GameTest
    public void offHandEmeraldsPassThrough(GameTestHelper helper) {
        BetterVillagerTradesGameTests.offHandEmeraldsPassThrough(helper);
    }

    @GameTest
    public void professionsWithoutNoviceTradesAreRejected(GameTestHelper helper) {
        BetterVillagerTradesGameTests.professionsWithoutNoviceTradesAreRejected(helper);
    }

    @GameTest
    public void levelUpMaximizesOnlyTheNewOffers(GameTestHelper helper) {
        BetterVillagerTradesGameTests.levelUpMaximizesOnlyTheNewOffers(helper);
    }

    @GameTest
    public void rerollCommandReachesTheWholeTradingRange(GameTestHelper helper) {
        BetterVillagerTradesGameTests.rerollCommandReachesTheWholeTradingRange(helper);
    }

    @GameTest(maxTicks = 40)
    public void rerollCooldownSpacesRepeatedRerolls(GameTestHelper helper) {
        BetterVillagerTradesGameTests.rerollCooldownSpacesRepeatedRerolls(helper);
    }

    @GameTest
    public void savedFiltersSkipVanillaDataFixers(GameTestHelper helper) {
        BetterVillagerTradesGameTests.savedFiltersSkipVanillaDataFixers(helper);
    }
}
