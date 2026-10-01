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
}
