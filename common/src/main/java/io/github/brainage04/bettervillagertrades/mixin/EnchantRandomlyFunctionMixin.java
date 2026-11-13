package io.github.brainage04.bettervillagertrades.mixin;

import io.github.brainage04.bettervillagertrades.BetterVillagerTrades;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Raises a trade's enchantment to the maximum level while vanilla is pricing it, so that vanilla's
 * own formula charges for the level the offer ends up with.
 *
 * <p>Vanilla rolls, applies and prices the level in one place: EnchantRandomlyFunction.java:91-98
 * sets {@code 2 + nextInt(5 + level * 10) + 3 * level} as the item's ADDITIONAL_TRADE_COST, and
 * VillagerTrade.java:109-122 adds that to the wanted count and doubles it for enchantments in
 * #minecraft:double_trade_price. VillagerTrade.getOffer removes the component before the offer
 * exists, so a later pass over the offers cannot see what the enchantment added and cannot reprice
 * the maximum level; raising the level here lets vanilla compute that price itself.
 *
 * <p>ADDITIONAL_COST_COMPONENT_ALLOWED is set only for villager trades (AbstractVillager.java:242),
 * so chest and other loot keep their own rolls.
 */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {
    @Redirect(
            method = "enchantItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"))
    private static int betterVillagerTrades$maxTradeLevel(
            RandomSource random,
            int minInclusive,
            int maxInclusive,
            ItemStack stack,
            Holder<Enchantment> enchantment,
            LootContext context) {
        // Drawn even when it is replaced, so the draws after it stay those vanilla would make.
        int level = Mth.nextInt(random, minInclusive, maxInclusive);
        if (!context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)) {
            return level;
        }
        return BetterVillagerTrades.maxTradeLevel(stack, context.getLevel(), level, maxInclusive);
    }
}
