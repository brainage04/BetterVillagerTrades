package io.github.brainage04.bettervillagertrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.junit.jupiter.api.Test;

/**
 * Checks the level and price of an enchanted trade against vanilla's own formula in 26.3, where
 * EnchantRandomlyFunction.enchantItem rolls the level with {@code Mth.nextInt(random, minLevel,
 * maxLevel)} (EnchantRandomlyFunction.java:91) and then prices it with {@code 2 + random.nextInt(5
 * + level * 10) + 3 * level} (EnchantRandomlyFunction.java:97).
 */
class TradePriceTest {
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Item default components are bound only when a server loads its resources
        // (ReloadableServerResources.java:86-111), and an ItemStack of an unbound item throws
        // (Holder.java:268-270). These tests set every component they read, so empty defaults do.
        for (Item item : List.of(Items.ENCHANTED_BOOK, Items.EMERALD)) {
            ResourceKey<Item> key = BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
            Holder.Reference<Item> holder = BuiltInRegistries.ITEM.getOrThrow(key);
            if (!holder.areComponentsBound()) {
                holder.bindComponents(DataComponentMap.EMPTY);
            }
        }
    }

    record Roll(long seed, boolean xoroshiro, int minLevel, int maxLevel) {
        RandomSource random() {
            return xoroshiro ? new XoroshiroRandomSource(seed) : RandomSource.create(seed);
        }
    }

    record Level(int maxLevel, int level) {}

    /** The additional cost EnchantRandomlyFunction.java:97 sets for {@code level}. */
    private static int vanillaAdditionalCost(RandomSource random, int level) {
        return 2 + random.nextInt(5 + level * 10) + 3 * level;
    }

    @Provide
    Arbitrary<Roll> rolls() {
        Arbitrary<Integer> minLevel = Arbitraries.integers().between(1, 5);
        Arbitrary<Integer> span = Arbitraries.integers().between(0, 254);
        return Combinators.combine(Arbitraries.longs(), Arbitraries.of(true, false), minLevel, span)
                .as(
                        (seed, xoroshiro, min, extra) ->
                                new Roll(seed, xoroshiro, min, Math.min(255, min + extra)));
    }

    @Property
    void aMaximizedTradeCostsWhatVanillaChargesForTheMaximumLevel(@ForAll("rolls") Roll roll) {
        RandomSource random = roll.random();
        int level = BetterVillagerTrades.tradeLevel(random, roll.minLevel(), roll.maxLevel(), true);
        int cost = vanillaAdditionalCost(random, level);

        RandomSource vanilla = roll.random();
        Mth.nextInt(vanilla, roll.minLevel(), roll.maxLevel());
        int max = roll.maxLevel();
        assertEquals(max, level);
        assertEquals(vanillaAdditionalCost(vanilla, max), cost);
        assertTrue(cost >= 2 + 3 * max && cost <= 6 + 13 * max, "cost " + cost);
    }

    @Property
    void withoutTheRuleATradeIsRolledAndPricedAsVanillaRollsAndPricesIt(
            @ForAll("rolls") Roll roll) {
        RandomSource random = roll.random();
        int level =
                BetterVillagerTrades.tradeLevel(random, roll.minLevel(), roll.maxLevel(), false);
        int cost = vanillaAdditionalCost(random, level);

        RandomSource vanilla = roll.random();
        int vanillaLevel = Mth.nextInt(vanilla, roll.minLevel(), roll.maxLevel());
        assertEquals(vanillaLevel, level);
        assertEquals(vanillaAdditionalCost(vanilla, vanillaLevel), cost);
    }

    @Test
    void aSingleLevelEnchantmentDrawsNothingForItsLevel() {
        // Mth.nextInt draws only when the range holds more than one level (Mth.java:146-148).
        RandomSource random = RandomSource.create(42L);
        assertEquals(1, BetterVillagerTrades.tradeLevel(random, 1, 1, true));
        assertEquals(RandomSource.create(42L).nextInt(15), random.nextInt(15));
    }

    @Test
    void aMissingOfferIsLeftAlone() {
        BetterVillagerTrades.TradeModification modification =
                BetterVillagerTrades.maximizeTradeOffer(null, true, true);
        assertNull(modification.offer());
        assertEquals(List.of(), modification.changes());
    }

    @Provide
    Arbitrary<List<Level>> levels() {
        return Combinators.combine(
                        Arbitraries.integers().between(1, 10),
                        Arbitraries.integers().between(1, 255))
                .as(Level::new)
                .list()
                .ofMaxSize(4);
    }

    @Property
    void maximizingRaisesEveryLevelBelowItsMaximumAndLowersNone(
            @ForAll("levels") List<Level> stored,
            @ForAll("levels") List<Level> applied,
            @ForAll boolean maxBookTrades,
            @ForAll boolean maxItemTrades,
            @ForAll @IntRange(min = 1, max = 64) int price) {
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        List<Holder<Enchantment>> storedHolders =
                enchant(result, DataComponents.STORED_ENCHANTMENTS, stored, "stored");
        List<Holder<Enchantment>> appliedHolders =
                enchant(result, DataComponents.ENCHANTMENTS, applied, "applied");
        MerchantOffer offer =
                new MerchantOffer(new ItemCost(Items.EMERALD, price), result, 12, 1, 0.05F);

        BetterVillagerTrades.TradeModification modification =
                BetterVillagerTrades.maximizeTradeOffer(offer, maxBookTrades, maxItemTrades);

        List<String> expectedChanges = new ArrayList<>();
        expectLevels(
                offer.getResult(),
                DataComponents.STORED_ENCHANTMENTS,
                stored,
                storedHolders,
                maxBookTrades,
                expectedChanges);
        expectLevels(
                offer.getResult(),
                DataComponents.ENCHANTMENTS,
                applied,
                appliedHolders,
                maxItemTrades,
                expectedChanges);
        List<String> changes =
                new ArrayList<>(
                        modification.changes().stream()
                                .map(change -> change.oldLevel() + "->" + change.newLevel())
                                .toList());
        expectedChanges.sort(null);
        changes.sort(null);
        assertEquals(expectedChanges, changes);
        assertEquals(!expectedChanges.isEmpty(), modification.modified());
        assertEquals(price, offer.getItemCostA().count());
    }

    private static List<Holder<Enchantment>> enchant(
            ItemStack stack,
            DataComponentType<ItemEnchantments> component,
            List<Level> levels,
            String prefix) {
        ItemEnchantments.Mutable enchantments =
                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        List<Holder<Enchantment>> holders = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            Holder<Enchantment> holder =
                    Holder.direct(
                            Enchantment.enchantment(
                                            Enchantment.definition(
                                                    HolderSet.direct(List.of()),
                                                    1,
                                                    levels.get(i).maxLevel(),
                                                    Enchantment.constantCost(1),
                                                    Enchantment.constantCost(1),
                                                    1,
                                                    EquipmentSlotGroup.ANY))
                                    .build(Identifier.withDefaultNamespace(prefix + "_" + i)));
            enchantments.set(holder, levels.get(i).level());
            holders.add(holder);
        }
        stack.set(component, enchantments.toImmutable());
        return holders;
    }

    private static void expectLevels(
            ItemStack stack,
            DataComponentType<ItemEnchantments> component,
            List<Level> levels,
            List<Holder<Enchantment>> holders,
            boolean maximized,
            List<String> expectedChanges) {
        ItemEnchantments enchantments = stack.getOrDefault(component, ItemEnchantments.EMPTY);
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            boolean raised = maximized && level.level() < level.maxLevel();
            assertEquals(
                    raised ? level.maxLevel() : level.level(),
                    enchantments.getLevel(holders.get(i)),
                    component + " " + level);
            if (raised) {
                expectedChanges.add(level.level() + "->" + level.maxLevel());
            }
        }
    }
}
