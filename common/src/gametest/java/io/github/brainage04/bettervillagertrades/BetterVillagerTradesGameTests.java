package io.github.brainage04.bettervillagertrades;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class BetterVillagerTradesGameTests {
    private BetterVillagerTradesGameTests() {}

    public static void generatedVillagerTradesAreMaxed(GameTestHelper helper) {
        helper.runAtTickTime(
                1,
                () -> {
                    ServerLevel level = helper.getLevel();

                    boolean foundBookTrade =
                            generatedTradesHaveOnlyMaxedEnchantments(
                                    helper,
                                    level,
                                    VillagerProfession.LIBRARIAN,
                                    1,
                                    DataComponents.STORED_ENCHANTMENTS);
                    boolean foundItemTrade =
                            generatedTradesHaveOnlyMaxedEnchantments(
                                    helper,
                                    level,
                                    VillagerProfession.WEAPONSMITH,
                                    5,
                                    DataComponents.ENCHANTMENTS);

                    helper.assertTrue(
                            foundBookTrade, "Expected at least one generated enchanted book trade");
                    helper.assertTrue(
                            foundItemTrade, "Expected at least one generated enchanted item trade");
                    helper.succeed();
                });
    }

    public static void gamerulesIndependentlyControlBookAndItemTradeOutputs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness =
                enchantmentRegistry.wrapAsHolder(
                        enchantmentRegistry.getValueOrThrow(Enchantments.SHARPNESS));

        ItemStack book =
                enchantedStack(
                        new ItemStack(Items.ENCHANTED_BOOK),
                        DataComponents.STORED_ENCHANTMENTS,
                        sharpness);
        ItemStack sword =
                enchantedStack(
                        new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, sharpness);
        MerchantOffer bookOffer = new MerchantOffer(new ItemCost(Items.EMERALD), book, 1, 1, 0.0F);
        MerchantOffer swordOffer =
                new MerchantOffer(new ItemCost(Items.EMERALD), sword, 1, 1, 0.0F);

        BetterVillagerTrades.maximizeTradeOffer(bookOffer, false, true);
        BetterVillagerTrades.maximizeTradeOffer(swordOffer, true, false);
        helper.assertTrue(
                enchantmentLevel(bookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness) == 1,
                "The book gamerule must leave enchanted books unchanged when disabled");
        helper.assertTrue(
                enchantmentLevel(swordOffer, DataComponents.ENCHANTMENTS, sharpness) == 1,
                "The item gamerule must leave enchanted items unchanged when disabled");

        BetterVillagerTrades.maximizeTradeOffer(bookOffer, true, false);
        BetterVillagerTrades.maximizeTradeOffer(swordOffer, false, true);
        helper.assertTrue(
                enchantmentLevel(bookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness)
                        == sharpness.value().getMaxLevel(),
                "The book gamerule must maximize enchanted books when enabled");
        helper.assertTrue(
                enchantmentLevel(swordOffer, DataComponents.ENCHANTMENTS, sharpness)
                        == sharpness.value().getMaxLevel(),
                "The item gamerule must maximize enchanted items when enabled");

        int aboveMaximum = sharpness.value().getMaxLevel() + 5;
        MerchantOffer overMaxBookOffer =
                new MerchantOffer(
                        new ItemCost(Items.EMERALD),
                        enchantedStack(
                                new ItemStack(Items.ENCHANTED_BOOK),
                                DataComponents.STORED_ENCHANTMENTS,
                                sharpness,
                                aboveMaximum),
                        1,
                        1,
                        0.0F);
        MerchantOffer overMaxSwordOffer =
                new MerchantOffer(
                        new ItemCost(Items.EMERALD),
                        enchantedStack(
                                new ItemStack(Items.DIAMOND_SWORD),
                                DataComponents.ENCHANTMENTS,
                                sharpness,
                                aboveMaximum),
                        1,
                        1,
                        0.0F);
        helper.assertFalse(
                BetterVillagerTrades.maximizeTradeOffer(overMaxBookOffer, true, true).modified(),
                "A book above the maximum level must not be reported as modified");
        helper.assertFalse(
                BetterVillagerTrades.maximizeTradeOffer(overMaxSwordOffer, true, true).modified(),
                "An item above the maximum level must not be reported as modified");
        helper.assertTrue(
                enchantmentLevel(overMaxBookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness)
                        == aboveMaximum,
                "Book levels above the maximum must not be lowered");
        helper.assertTrue(
                enchantmentLevel(overMaxSwordOffer, DataComponents.ENCHANTMENTS, sharpness)
                        == aboveMaximum,
                "Item levels above the maximum must not be lowered");
        helper.succeed();
    }

    public static void bookTradePriceFollowsTheMaxLevelFormula(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> sharpness =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS);
        int maxLevel = sharpness.value().getMaxLevel();
        Villager villager = createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
        TradePriceRandom random =
                new TradePriceRandom(
                        sharpness.value().getMaxLevel() - sharpness.value().getMinLevel() + 1);
        LootContext context =
                new LootContext.Builder(
                                new LootParams.Builder(level)
                                        .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                                        .withParameter(LootContextParams.THIS_ENTITY, villager)
                                        .withParameter(
                                                LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,
                                                Unit.INSTANCE)
                                        .create(LootContextParamSets.VILLAGER_TRADE))
                        .withOptionalRandomSource(random)
                        .create(java.util.Optional.empty());
        LootItemFunction function =
                EnchantRandomlyFunction.randomEnchantment()
                        .withEnchantment(sharpness)
                        .includeAdditionalCostComponent()
                        .build();

        ItemStack book = function.apply(new ItemStack(Items.BOOK), context);

        helper.assertTrue(
                book.is(Items.ENCHANTED_BOOK),
                "The trade's enchantment function must produce an enchanted book");
        helper.assertTrue(
                enchantmentLevel(book, DataComponents.STORED_ENCHANTMENTS, sharpness) == maxLevel,
                "A trade's enchanted book must be rolled at the maximum level");
        helper.assertTrue(
                random.lastBound == 5 + maxLevel * 10,
                "The emerald price must be drawn for the maximum level, so its bound must be "
                        + (5 + maxLevel * 10)
                        + " but vanilla drew "
                        + random.lastBound);
        helper.assertTrue(
                book.getOrDefault(DataComponents.ADDITIONAL_TRADE_COST, -1)
                        == 2 + random.lastDraw + 3 * maxLevel,
                "The emerald price must follow vanilla's formula 2 + draw + 3 * level at the"
                        + " maximum level");
        helper.succeed();
    }

    public static void generatedBookTradePricesUseTheMaxLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HolderSet<Enchantment> doubleTradePrice =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(EnchantmentTags.DOUBLE_TRADE_PRICE);
        boolean foundBook = false;
        for (int i = 0; i < 20; i++) {
            Villager villager =
                    createVillager(
                            helper,
                            level,
                            VillagerProfession.LIBRARIAN,
                            1,
                            1 + (i % 5),
                            2,
                            1 + (i / 5));
            for (MerchantOffer offer : villager.getOffers()) {
                ItemEnchantments stored =
                        offer.getResult()
                                .getOrDefault(
                                        DataComponents.STORED_ENCHANTMENTS,
                                        ItemEnchantments.EMPTY);
                if (stored.isEmpty()) continue;
                foundBook = true;
                for (Holder<Enchantment> enchantment : stored.keySet()) {
                    // EnchantRandomlyFunction.java:98 draws 2 + [0, 5 + level * 10) + 3 * level per
                    // enchantment, VillagerTrade.java:114-119 doubles it for the trade's
                    // double_trade_price enchantments, and TradeCost.toItemCost clamps the count to
                    // the item's stack size (TradeCost.java:34-37).
                    int multiplier = doubleTradePrice.contains(enchantment) ? 2 : 1;
                    int minimum =
                            Math.min(64, multiplier * (3 * enchantment.value().getMaxLevel() + 2));
                    helper.assertTrue(
                            offer.getBaseCostA().count() >= minimum,
                            "A generated enchanted book trade must cost at least "
                                    + minimum
                                    + " emeralds for "
                                    + enchantment.getRegisteredName()
                                    + " at its maximum level, but it costs "
                                    + offer.getBaseCostA().count());
                }
            }
        }
        helper.assertTrue(foundBook, "Expected at least one generated enchanted book trade");
        helper.succeed();
    }

    public static void rerollsUntradedVillagersWithoutProtectedOffers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        try (GameTestPlayer fixture = GameTestPlayer.mock(helper)) {
            ServerPlayer player = fixture.player();
            var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> mending =
                    enchantmentRegistry.wrapAsHolder(
                            enchantmentRegistry.getValueOrThrow(Enchantments.MENDING));
            ItemStack mendingBook =
                    enchantedStack(
                            new ItemStack(Items.ENCHANTED_BOOK),
                            DataComponents.STORED_ENCHANTMENTS,
                            mending);
            TradeFilter filter = TradeFilter.fromStack(mendingBook);
            MerchantOffer protectedOffer =
                    new MerchantOffer(new ItemCost(Items.EMERALD), mendingBook, 1, 1, 0.0F);
            MerchantOffers originalOffers = new MerchantOffers();
            originalOffers.add(protectedOffer);
            Villager villager =
                    createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            villager.setOffers(originalOffers);

            PlayerTradeFilters filters = PlayerTradeFilters.get(level.getServer());
            filters.clear(player);
            filters.add(player, filter);
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false),
                    "A matching per-player filter must block the reroll");
            helper.assertTrue(
                    villager.getOffers().getFirst() == protectedOffer,
                    "A blocked reroll must preserve the existing offers");

            filters.clear(player);
            helper.assertTrue(
                    TradeRerollService.reroll(player, villager, false),
                    "An untraded novice villager without protected offers must reroll");
            helper.assertTrue(
                    !villager.getOffers().contains(protectedOffer),
                    "A successful reroll must replace the previous offers");

            originalOffers.clear();
            originalOffers.add(protectedOffer);
            villager.setOffers(originalOffers);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
            helper.assertTrue(
                    TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "Sneak-right-clicking with an emerald must consume the interaction");
            helper.assertTrue(
                    !villager.getOffers().contains(protectedOffer),
                    "The emerald interaction must reroll eligible villager trades");
            player.setShiftKeyDown(false);

            protectedOffer.increaseUses();
            originalOffers.clear();
            originalOffers.add(protectedOffer);
            villager.setOffers(originalOffers);
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false),
                    "A villager with a used trade must be locked against rerolls");
            Villager nitwit = createVillager(helper, level, VillagerProfession.NITWIT, 1, 3, 2, 2);
            helper.assertTrue(
                    !TradeRerollService.reroll(player, nitwit, false),
                    "Nitwits cannot acquire a profession and must not report a successful reroll");
            helper.assertTrue(
                    nitwit.getOffers().isEmpty(), "Rejected nitwits must keep their empty offers");
        }
        helper.succeed();
    }

    public static void rerollRecalculatesOpenMerchantResult(GameTestHelper helper) {
        try (GameTestPlayer fixture = GameTestPlayer.mock(helper)) {
            ServerPlayer player = fixture.player();
            Villager villager =
                    createVillager(
                            helper, helper.getLevel(), VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            MerchantOffers offers = new MerchantOffers();
            offers.add(
                    new MerchantOffer(
                            new ItemCost(Items.EMERALD),
                            java.util.Optional.of(new ItemCost(Items.BOOK)),
                            new ItemStack(Items.DIAMOND),
                            1,
                            1,
                            0));
            offers.add(
                    new MerchantOffer(
                            new ItemCost(Items.EMERALD),
                            java.util.Optional.of(new ItemCost(Items.BOOK)),
                            new ItemStack(Items.NETHER_STAR),
                            1,
                            1,
                            0));
            villager.setOffers(offers);
            villager.setTradingPlayer(player);
            var previousMenu = player.containerMenu;
            var menu =
                    new net.minecraft.world.inventory.MerchantMenu(
                            1, player.getInventory(), villager);
            player.containerMenu = menu;
            try {
                menu.getSlot(0).set(new ItemStack(Items.EMERALD, 64));
                menu.getSlot(1).set(new ItemStack(Items.BOOK, 3));
                menu.setSelectionHint(1);
                helper.assertTrue(
                        menu.getSlot(2).getItem().is(Items.NETHER_STAR),
                        "Old offer must initially have a result");
                helper.assertTrue(
                        TradeRerollService.reroll(player, villager, false),
                        "An open eligible merchant must reroll");
                helper.assertTrue(
                        !menu.getSlot(2).getItem().is(Items.NETHER_STAR),
                        "Old output must not remain collectible after reroll");
                helper.assertTrue(
                        menu.getSlot(0).getItem().getCount() == 64,
                        "Recalculation must preserve the first payment");
                helper.assertTrue(
                        menu.getSlot(1).getItem().is(Items.BOOK)
                                && menu.getSlot(1).getItem().getCount() == 3,
                        "Recalculation must preserve the second payment");
                // Equal-cost offers expose the retained selection without depending on randomized
                // reroll outputs.
                MerchantOffers selectionProbe = new MerchantOffers();
                selectionProbe.add(
                        new MerchantOffer(
                                new ItemCost(Items.EMERALD),
                                java.util.Optional.of(new ItemCost(Items.BOOK)),
                                new ItemStack(Items.DIAMOND),
                                1,
                                1,
                                0));
                selectionProbe.add(
                        new MerchantOffer(
                                new ItemCost(Items.EMERALD),
                                java.util.Optional.of(new ItemCost(Items.BOOK)),
                                new ItemStack(Items.GOLD_INGOT),
                                1,
                                1,
                                0));
                villager.setOffers(selectionProbe);
                menu.getSlot(0).setChanged();
                helper.assertTrue(
                        menu.getSlot(2).getItem().is(Items.GOLD_INGOT),
                        "Reroll must retain offer 1 so the server stays aligned with the displayed client selection");
            } finally {
                player.containerMenu = previousMenu;
                villager.setTradingPlayer(null);
            }
        }
        helper.succeed();
    }

    public static void filterCommandsValidateAndIsolatePlayers(GameTestHelper helper) {
        try (GameTestPlayer playerFixture = GameTestPlayer.mock(helper);
                GameTestPlayer otherFixture = GameTestPlayer.mock(helper)) {
            ServerPlayer player = playerFixture.player();
            ServerPlayer other = otherFixture.player();
            PlayerTradeFilters filters = PlayerTradeFilters.get(helper.getLevel().getServer());
            try {
                helper.assertTrue(
                        command(player, "filter list") == 0, "New players have no filters");
                helper.assertTrue(
                        command(player, "filter add minecraft:enchanted_book minecraft:mending")
                                == 1,
                        "Add must succeed");
                helper.assertTrue(
                        command(player, "filter add minecraft:enchanted_book minecraft:mending")
                                == 0,
                        "Duplicates must not be added");
                helper.assertTrue(
                        command(player, "filter list") == 1,
                        "List must report the stored filter count");
                helper.assertTrue(
                        filters.get(other).isEmpty(),
                        "Commands must not change another player's filters");
                helper.assertTrue(
                        command(player, "filter add bettervillagertrades:missing") == 0,
                        "Unknown items must be rejected");
                helper.assertTrue(
                        command(player, "filter add minecraft:book bettervillagertrades:missing")
                                == 0,
                        "Unknown enchantments must be rejected");
                helper.assertTrue(
                        filters.get(player).size() == 1, "Invalid arguments must not add filters");
                helper.assertTrue(
                        command(player, "filter remove minecraft:book") == 0,
                        "Removing a missing filter must report no change");
                helper.assertTrue(
                        command(player, "filter toggle minecraft:book") == 1, "Toggle must add");
                helper.assertTrue(filters.get(player).size() == 2, "Toggled filter must be stored");
                helper.assertTrue(
                        command(player, "filter toggle minecraft:book") == 1, "Toggle must remove");
                helper.assertTrue(
                        command(player, "filter remove minecraft:enchanted_book minecraft:mending")
                                == 1,
                        "Remove must succeed");
                helper.assertTrue(
                        command(player, "filter clear") == 0,
                        "Clearing empty filters must report no change");
                command(player, "filter add minecraft:book");
                helper.assertTrue(
                        command(player, "filter clear") == 1, "Clear must remove stored filters");
                helper.assertTrue(
                        command(player, "reroll") == 0, "Reroll requires an open trading session");
            } finally {
                filters.clear(player);
                filters.clear(other);
            }
        }
        helper.succeed();
    }

    public static void rejectsIneligibleVillagersAndPreservesEmeralds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        try (GameTestPlayer playerFixture = GameTestPlayer.mock(helper);
                GameTestPlayer otherFixture = GameTestPlayer.mock(helper)) {
            ServerPlayer player = playerFixture.player();
            ServerPlayer other = otherFixture.player();
            Villager villager =
                    createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            MerchantOffers offers = new MerchantOffers();
            offers.add(
                    new MerchantOffer(
                            new ItemCost(Items.EMERALD), new ItemStack(Items.PAPER), 1, 1, 0));
            villager.setOffers(offers);
            villager.setBaby(true);
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false), "Babies must be rejected");
            villager.setBaby(false);
            villager.setVillagerData(
                    villager.getVillagerData()
                            .withProfession(level.registryAccess(), VillagerProfession.NONE));
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false),
                    "Unemployed villagers must be rejected");
            villager.setVillagerData(
                    villager.getVillagerData()
                            .withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN)
                            .withLevel(2));
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false),
                    "Non-novices must be rejected");
            villager.setVillagerData(villager.getVillagerData().withLevel(1));
            villager.setOffers(offers);
            villager.setVillagerXp(1);
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false),
                    "Experienced novices must be rejected");
            villager.setVillagerXp(0);
            villager.setTradingPlayer(other);
            helper.assertTrue(
                    !TradeRerollService.reroll(player, villager, false),
                    "Another player's merchant must be protected");
            villager.setTradingPlayer(null);
            helper.assertTrue(
                    villager.getOffers() == offers, "Rejected rerolls must preserve offers");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
            helper.assertTrue(
                    !TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "Standing interactions must pass through");
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BOOK));
            helper.assertTrue(
                    !TradeRerollService.interact(player, villager, InteractionHand.OFF_HAND)
                            .consumesAction(),
                    "Non-emerald interactions must pass through");
            helper.assertTrue(
                    TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "Emerald reroll must be handled");
            helper.assertTrue(
                    player.getMainHandItem().getCount() == 3, "Reroll must not consume emeralds");
            helper.assertTrue(
                    villager.getOffers() != offers
                            || !offers.getFirst().getResult().is(Items.PAPER),
                    "Eligible interaction must replace offers");
            player.setShiftKeyDown(false);
        }
        helper.succeed();
    }

    public static void spectatorsCannotReroll(GameTestHelper helper) {
        try (GameTestPlayer fixture = GameTestPlayer.mock(helper, GameType.SPECTATOR)) {
            ServerPlayer player = fixture.player();
            Villager villager =
                    createVillager(
                            helper, helper.getLevel(), VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            villager.setOffers(paperOffer());
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
            helper.assertFalse(
                    TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "Spectator interactions must pass through, as vanilla's Player.interactOn does");
            helper.assertTrue(offersPaper(villager), "Spectators must not reroll");
        }
        helper.succeed();
    }

    public static void offHandEmeraldsPassThrough(GameTestHelper helper) {
        try (GameTestPlayer fixture = GameTestPlayer.mock(helper)) {
            ServerPlayer player = fixture.player();
            Villager villager =
                    createVillager(
                            helper, helper.getLevel(), VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            villager.setOffers(paperOffer());
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.EMERALD));
            helper.assertFalse(
                    TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "An empty main hand must pass through even with an off-hand emerald");
            helper.assertFalse(
                    TradeRerollService.interact(player, villager, InteractionHand.OFF_HAND)
                            .consumesAction(),
                    "Off-hand emerald interactions must pass through to vanilla");
            helper.assertTrue(offersPaper(villager), "Off-hand emeralds must not reroll");
        }
        helper.succeed();
    }

    public static void professionsWithoutNoviceTradesAreRejected(GameTestHelper helper) {
        try (GameTestPlayer fixture = GameTestPlayer.mock(helper)) {
            Villager villager =
                    createVillager(
                            helper, helper.getLevel(), VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            VillagerData librarian = villager.getVillagerData();
            // Restored before the tick ends: VillagerData.STREAM_CODEC sends the profession as a
            // registry id (VillagerData.java:39), which these unregistered holders do not have.
            try {
                villager.setVillagerData(
                        librarian.withProfession(
                                unregisteredProfession(
                                        helper, "tradeless", Int2ObjectMaps.emptyMap())));
                helper.assertTrue(
                        deniedWith(fixture, villager, "bettervillagertrades.reroll.no_profession"),
                        "Professions without novice trades must be rejected");

                Int2ObjectMap<ResourceKey<TradeSet>> noviceAndMaster =
                        Int2ObjectMap.ofEntries(
                                Int2ObjectMap.entry(
                                        VillagerData.MIN_VILLAGER_LEVEL,
                                        TradeSets.WEAPONSMITH_LEVEL_1),
                                Int2ObjectMap.entry(
                                        VillagerData.MAX_VILLAGER_LEVEL,
                                        TradeSets.WEAPONSMITH_LEVEL_5));
                villager.setVillagerData(
                        librarian
                                .withProfession(
                                        unregisteredProfession(
                                                helper, "novice_and_master", noviceAndMaster))
                                .withLevel(3));
                helper.assertTrue(
                        deniedWith(fixture, villager, "bettervillagertrades.reroll.experienced"),
                        "A villager above novice must be told it is not a novice, also at a"
                                + " level its profession has no trades for");
            } finally {
                villager.setVillagerData(librarian);
            }
        }
        helper.succeed();
    }

    public static void filtersMatchBothEnchantmentComponents(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> mending = registry.getOrThrow(Enchantments.MENDING);
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        ItemStack book =
                enchantedStack(
                        new ItemStack(Items.ENCHANTED_BOOK),
                        DataComponents.STORED_ENCHANTMENTS,
                        mending);
        ItemStack sword =
                enchantedStack(
                        new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, mending);
        TradeFilter bookFilter = TradeFilter.fromStack(book);
        TradeFilter swordFilter = TradeFilter.fromStack(sword);
        helper.assertTrue(bookFilter.matches(book), "Stored enchantments must match");
        helper.assertTrue(swordFilter.matches(sword), "Regular enchantments must match");
        helper.assertTrue(
                !bookFilter.matches(sword), "An enchantment filter must also match the item");
        helper.assertTrue(
                !swordFilter.matches(
                        enchantedStack(
                                new ItemStack(Items.DIAMOND_SWORD),
                                DataComponents.ENCHANTMENTS,
                                sharpness)),
                "Different enchantments must not match");
        helper.assertTrue(
                !swordFilter.matches(new ItemStack(Items.DIAMOND_SWORD)),
                "Missing enchantments must not match");
        TradeFilter allSwords = new TradeFilter(swordFilter.item(), java.util.Optional.empty());
        helper.assertTrue(
                allSwords.matches(new ItemStack(Items.DIAMOND_SWORD)),
                "Item-only filters must match unenchanted outputs");
        helper.assertTrue(
                allSwords.matches(sword), "Item-only filters must also match enchanted outputs");
        helper.assertTrue(
                swordFilter
                        .description()
                        .getString()
                        .equals("minecraft:diamond_sword with minecraft:mending"),
                "Vanilla clients must receive readable fallback text");
        helper.succeed();
    }

    public static void registeredGamerulesControlOnlyNewOffers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var rules = level.getGameRules();
        boolean oldBooks = rules.get(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES);
        boolean oldItems = rules.get(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES);
        Holder<Enchantment> sharpness =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS);
        helper.assertTrue(
                BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES.defaultValue()
                        == BetterVillagerTrades.BOOK_TRADES_RULE.defaultValue(),
                "The registered book rule must default to the shared default");
        helper.assertTrue(
                BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES.defaultValue()
                        == BetterVillagerTrades.ITEM_TRADES_RULE.defaultValue(),
                "The registered item rule must default to the shared default");
        Villager villager = createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
        try {
            for (boolean books : new boolean[] {false, true}) {
                for (boolean items : new boolean[] {false, true}) {
                    rules.set(
                            BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES,
                            books,
                            level.getServer());
                    rules.set(
                            BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES,
                            items,
                            level.getServer());
                    MerchantOffers offers = new MerchantOffers();
                    offers.add(
                            new MerchantOffer(
                                    new ItemCost(Items.EMERALD),
                                    enchantedStack(
                                            new ItemStack(Items.ENCHANTED_BOOK),
                                            DataComponents.STORED_ENCHANTMENTS,
                                            sharpness),
                                    1,
                                    1,
                                    0));
                    offers.add(
                            new MerchantOffer(
                                    new ItemCost(Items.EMERALD),
                                    enchantedStack(
                                            new ItemStack(Items.ENCHANTED_BOOK),
                                            DataComponents.STORED_ENCHANTMENTS,
                                            sharpness),
                                    1,
                                    1,
                                    0));
                    offers.add(
                            new MerchantOffer(
                                    new ItemCost(Items.EMERALD),
                                    enchantedStack(
                                            new ItemStack(Items.DIAMOND_SWORD),
                                            DataComponents.ENCHANTMENTS,
                                            sharpness),
                                    1,
                                    1,
                                    0));
                    villager.setOffers(offers);
                    BetterVillagerTrades.maximizeNewVillagerOffers(villager, level, 1);
                    helper.assertTrue(
                            enchantmentLevel(
                                            offers.get(0),
                                            DataComponents.STORED_ENCHANTMENTS,
                                            sharpness)
                                    == 1,
                            "Previously generated offers must be untouched");
                    helper.assertTrue(
                            enchantmentLevel(
                                            offers.get(1),
                                            DataComponents.STORED_ENCHANTMENTS,
                                            sharpness)
                                    == (books ? 5 : 1),
                            "Registered book rule must control new outputs");
                    helper.assertTrue(
                            enchantmentLevel(offers.get(2), DataComponents.ENCHANTMENTS, sharpness)
                                    == (items ? 5 : 1),
                            "Registered item rule must control new outputs");
                }
            }
        } finally {
            rules.set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, oldBooks, level.getServer());
            rules.set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, oldItems, level.getServer());
        }
        helper.succeed();
    }

    public static void levelUpMaximizesOnlyTheNewOffers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var rules = level.getGameRules();
        boolean oldBooks = rules.get(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES);
        boolean oldItems = rules.get(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES);
        Holder<Enchantment> sharpness =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS);
        try {
            rules.set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, true, level.getServer());
            rules.set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, true, level.getServer());
            Villager villager =
                    createVillager(helper, level, VillagerProfession.WEAPONSMITH, 4, 2, 2, 2);
            MerchantOffer oldOffer =
                    new MerchantOffer(
                            new ItemCost(Items.EMERALD),
                            enchantedStack(
                                    new ItemStack(Items.DIAMOND_SWORD),
                                    DataComponents.ENCHANTMENTS,
                                    sharpness),
                            1,
                            VillagerData.getMaxXpPerLevel(4),
                            0.0F);
            MerchantOffers offers = new MerchantOffers();
            offers.add(oldOffer);
            villager.setOffers(offers);

            villager.notifyTrade(oldOffer);

            helper.assertTrue(
                    villager.getVillagerData().level() == 5,
                    "Trading the offer must level the weaponsmith up to master");
            helper.assertTrue(
                    enchantmentLevel(oldOffer, DataComponents.ENCHANTMENTS, sharpness) == 1,
                    "Offers from before the level-up must keep their enchantment levels");
            boolean foundNewEnchantedOffer = false;
            for (int i = 1; i < offers.size(); i++) {
                ItemEnchantments enchantments =
                        offers.get(i)
                                .getResult()
                                .getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                    foundNewEnchantedOffer = true;
                    helper.assertTrue(
                            enchantments.getLevel(enchantment) == enchantment.value().getMaxLevel(),
                            "Offers added by the level-up must be at maximum level");
                }
            }
            helper.assertTrue(
                    foundNewEnchantedOffer,
                    "The master weaponsmith level-up must add an enchanted offer");
        } finally {
            rules.set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, oldBooks, level.getServer());
            rules.set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, oldItems, level.getServer());
        }
        helper.succeed();
    }

    public static void rerollCommandReachesTheWholeTradingRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        try (GameTestPlayer fixture = GameTestPlayer.mock(helper, GameType.SURVIVAL)) {
            ServerPlayer player = fixture.moveTo(helper, new BlockPos(1, 1, 1)).player();
            player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(6.0);
            double tradingRange =
                    player.entityInteractionRange() + Container.DEFAULT_DISTANCE_BUFFER;

            Villager beyondRange =
                    createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            try {
                beyondRange.snapTo(
                        player.getX(), player.getEyeY() + tradingRange + 0.1, player.getZ());
                beyondRange.setTradingPlayer(player);
                helper.assertFalse(
                        beyondRange.stillValid(player),
                        "The far villager must be outside vanilla's trading range");
                helper.assertTrue(
                        TradeRerollService.rerollTradingVillager(player) == 0,
                        "The command must not reach a villager whose trading screen vanilla closes");
            } finally {
                beyondRange.discard();
            }

            Villager withinRange =
                    createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            try {
                withinRange.snapTo(player.getX(), player.getY() + 10.0, player.getZ());
                withinRange.setTradingPlayer(player);
                helper.assertTrue(
                        withinRange.stillValid(player),
                        "The near villager must be inside vanilla's trading range");
                helper.assertFalse(
                        player.getBoundingBox()
                                .inflate(8.0)
                                .intersects(withinRange.getBoundingBox()),
                        "The near villager must be more than 8 blocks away");
                helper.assertTrue(
                        TradeRerollService.rerollTradingVillager(player) == 1,
                        "The command must reach a trading villager anywhere in the trading range");
            } finally {
                withinRange.discard();
            }
        }
        helper.succeed();
    }

    public static void rerollCooldownSpacesRepeatedRerolls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GameTestPlayer fixture = GameTestPlayer.mock(helper);
        boolean closedLater = false;
        try {
            ServerPlayer player = fixture.moveTo(helper, new BlockPos(1, 1, 2)).player();
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
            Villager villager =
                    createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
            villager.setOffers(paperOffer());
            helper.assertTrue(
                    TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "The first reroll gesture must be handled");
            helper.assertFalse(
                    offersPaper(villager), "The first reroll gesture must reroll the villager");

            villager.setOffers(paperOffer());
            helper.assertTrue(
                    TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "A gesture during the cooldown must still be consumed");
            helper.assertTrue(
                    offersPaper(villager), "A gesture during the cooldown must not reroll");
            villager.setTradingPlayer(player);
            helper.assertTrue(
                    villager.stillValid(player), "The command must be able to see the villager");
            helper.assertTrue(
                    TradeRerollService.rerollTradingVillager(player) == 0,
                    "The command during the cooldown must not reroll");
            villager.setTradingPlayer(null);
            helper.assertTrue(
                    offersPaper(villager), "The command during the cooldown must keep the offers");

            helper.runAfterDelay(
                    TradeRerollService.REROLL_COOLDOWN_TICKS,
                    () -> {
                        try {
                            helper.assertTrue(
                                    TradeRerollService.interact(
                                                    player, villager, InteractionHand.MAIN_HAND)
                                            .consumesAction(),
                                    "The gesture after the cooldown must be handled");
                            helper.assertFalse(
                                    offersPaper(villager),
                                    "The gesture after the cooldown must reroll again");
                        } finally {
                            fixture.close();
                        }
                        helper.succeed();
                    });
            closedLater = true;
        } finally {
            if (!closedLater) fixture.close();
        }
    }

    public static void savedFiltersSkipVanillaDataFixers(GameTestHelper helper) {
        CompoundTag filters = new CompoundTag();
        filters.put(UUID.randomUUID().toString(), new ListTag());
        CompoundTag data = new CompoundTag();
        data.put("filters", filters);
        CompoundTag saved = new CompoundTag();
        saved.put("data", data);
        Path file = null;
        try {
            file = Files.createTempFile("bettervillagertrades-filters", ".dat");
            NbtIo.writeCompressed(saved, file);
            CompoundTag read =
                    helper.getLevel()
                            .getDataStorage()
                            .readTagFromDisk(
                                    file,
                                    PlayerTradeFilters.TYPE.dataFixType(),
                                    SharedConstants.getCurrentVersion().dataVersion().version());
            helper.assertTrue(
                    read.equals(saved), "Saved filters must load without a vanilla datafixer");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            deleteIfPresent(file);
        }
        helper.succeed();
    }

    private static int command(ServerPlayer player, String command) {
        try {
            return player.level()
                    .getServer()
                    .getCommands()
                    .getDispatcher()
                    .execute(
                            "bettervillagertrades " + command,
                            player.createCommandSourceStack().withSuppressedOutput());
        } catch (CommandSyntaxException exception) {
            throw new AssertionError(
                    "Player command must be registered and executable: " + command, exception);
        }
    }

    private static ItemStack enchantedStack(
            ItemStack stack,
            DataComponentType<ItemEnchantments> componentType,
            Holder<Enchantment> enchantment) {
        return enchantedStack(stack, componentType, enchantment, 1);
    }

    private static ItemStack enchantedStack(
            ItemStack stack,
            DataComponentType<ItemEnchantments> componentType,
            Holder<Enchantment> enchantment,
            int enchantmentLevel) {
        ItemEnchantments.Mutable enchantments =
                new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(enchantment, enchantmentLevel);
        stack.set(componentType, enchantments.toImmutable());
        return stack;
    }

    private static MerchantOffers paperOffer() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(
                new MerchantOffer(
                        new ItemCost(Items.EMERALD), new ItemStack(Items.PAPER), 1, 1, 0));
        return offers;
    }

    private static boolean offersPaper(Villager villager) {
        return villager.getOffers().stream().anyMatch(offer -> offer.getResult().is(Items.PAPER));
    }

    private static Holder<VillagerProfession> unregisteredProfession(
            GameTestHelper helper, String name, Int2ObjectMap<ResourceKey<TradeSet>> trades) {
        return new UnregisteredProfession(
                helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION),
                ResourceKey.create(Registries.VILLAGER_PROFESSION, BetterVillagerTrades.of(name)),
                new VillagerProfession(
                        Component.literal(name),
                        poi -> false,
                        poi -> false,
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        null,
                        trades));
    }

    private static boolean deniedWith(GameTestPlayer fixture, Villager villager, String key) {
        fixture.clearMessages();
        return !TradeRerollService.reroll(fixture.player(), villager, true)
                && fixture.messages().stream()
                        .anyMatch(
                                message ->
                                        message.getContents()
                                                        instanceof TranslatableContents contents
                                                && contents.getKey().equals(key));
    }

    private static void deleteIfPresent(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static int enchantmentLevel(
            ItemStack stack,
            DataComponentType<ItemEnchantments> componentType,
            Holder<Enchantment> enchantment) {
        return stack.getOrDefault(componentType, ItemEnchantments.EMPTY).getLevel(enchantment);
    }

    /**
     * Draws from a fixed-seed source but rolls the minimum enchantment level, so a price drawn for
     * the maximum level is distinguishable, and remembers the last draw for a price check.
     */
    private static final class TradePriceRandom implements RandomSource {
        private final RandomSource delegate = RandomSource.create(48271L);
        private final int levelSpan;
        int lastBound = -1;
        int lastDraw = -1;

        private TradePriceRandom(int levelSpan) {
            this.levelSpan = levelSpan;
        }

        @Override
        public int nextInt(int bound) {
            lastBound = bound;
            lastDraw = bound == levelSpan ? 0 : delegate.nextInt(bound);
            return lastDraw;
        }

        @Override
        public RandomSource fork() {
            return delegate.fork();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            return delegate.forkPositional();
        }

        @Override
        public void setSeed(long seed) {
            delegate.setSeed(seed);
        }

        @Override
        public int nextInt() {
            return delegate.nextInt();
        }

        @Override
        public long nextLong() {
            return delegate.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            return delegate.nextBoolean();
        }

        @Override
        public float nextFloat() {
            return delegate.nextFloat();
        }

        @Override
        public double nextDouble() {
            return delegate.nextDouble();
        }

        @Override
        public double nextGaussian() {
            return delegate.nextGaussian();
        }
    }

    private static int enchantmentLevel(
            MerchantOffer offer,
            DataComponentType<ItemEnchantments> componentType,
            Holder<Enchantment> enchantment) {
        return offer.getResult()
                .getOrDefault(componentType, ItemEnchantments.EMPTY)
                .getLevel(enchantment);
    }

    private static boolean generatedTradesHaveOnlyMaxedEnchantments(
            GameTestHelper helper,
            ServerLevel level,
            net.minecraft.resources.ResourceKey<VillagerProfession> profession,
            int professionLevel,
            DataComponentType<ItemEnchantments> componentType) {
        boolean foundEnchantments = false;
        for (int i = 0; i < 20; i++) {
            Villager villager =
                    createVillager(
                            helper,
                            level,
                            profession,
                            professionLevel,
                            1 + (i % 5),
                            2,
                            1 + (i / 5));
            for (MerchantOffer offer : villager.getOffers()) {
                ItemEnchantments enchantments =
                        offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY);
                if (enchantments.isEmpty()) continue;
                foundEnchantments = true;
                for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                    helper.assertTrue(
                            enchantments.getLevel(enchantment) == enchantment.value().getMaxLevel(),
                            "Generated villager trade enchantment should be max level");
                }
            }
        }
        return foundEnchantments;
    }

    private static Villager createVillager(
            GameTestHelper helper,
            ServerLevel level,
            net.minecraft.resources.ResourceKey<VillagerProfession> profession,
            int levelNumber,
            int x,
            int y,
            int z) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, x, y, z);
        villager.setNoAi(true);
        villager.getRandom().setSeed(31L * x + 17L * y + z);
        VillagerData villagerData =
                villager.getVillagerData()
                        .withProfession(level.registryAccess(), profession)
                        .withLevel(levelNumber);
        villager.setVillagerData(villagerData);
        return villager;
    }

    // Registries are frozen while GameTests run, so a modded profession is simulated with a keyed
    // holder outside the registry.
    private static final class UnregisteredProfession extends Holder.Reference<VillagerProfession> {
        private UnregisteredProfession(
                HolderOwner<VillagerProfession> owner,
                ResourceKey<VillagerProfession> key,
                VillagerProfession profession) {
            super(Type.STAND_ALONE, owner, key, profession);
        }
    }
}
