package io.github.brainage04.bettervillagertrades;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
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

/**
 * Loader-neutral server GameTest bodies. Both loaders compile this source set into their GameTest
 * mods: Fabric runs them through {@code @GameTest} methods, NeoForge through registered test
 * functions and {@code test_instance} data.
 */
public final class BetterVillagerTradesGameTests {
	private BetterVillagerTradesGameTests() {
	}

	public static void generatedVillagerTradesAreMaxed(GameTestHelper helper) {
		helper.runAtTickTime(1, () -> {
			ServerLevel level = helper.getLevel();

			boolean foundBookTrade = generatedTradesHaveOnlyMaxedEnchantments(
					helper, level, VillagerProfession.LIBRARIAN, 1, DataComponents.STORED_ENCHANTMENTS
			);
			boolean foundItemTrade = generatedTradesHaveOnlyMaxedEnchantments(
					helper, level, VillagerProfession.WEAPONSMITH, 5, DataComponents.ENCHANTMENTS
			);

			helper.assertTrue(foundBookTrade, "Expected at least one generated enchanted book trade");
			helper.assertTrue(foundItemTrade, "Expected at least one generated enchanted item trade");
			helper.succeed();
		});
	}

	public static void gamerulesIndependentlyControlBookAndItemTradeOutputs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> sharpness = enchantmentRegistry.wrapAsHolder(
				enchantmentRegistry.getValueOrThrow(Enchantments.SHARPNESS)
		);

		ItemStack book = enchantedStack(new ItemStack(Items.ENCHANTED_BOOK), DataComponents.STORED_ENCHANTMENTS, sharpness);
		ItemStack sword = enchantedStack(new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, sharpness);
		MerchantOffer bookOffer = new MerchantOffer(new ItemCost(Items.EMERALD), book, 1, 1, 0.0F);
		MerchantOffer swordOffer = new MerchantOffer(new ItemCost(Items.EMERALD), sword, 1, 1, 0.0F);

		BetterVillagerTrades.maximizeTradeOffer(bookOffer, false, true);
		BetterVillagerTrades.maximizeTradeOffer(swordOffer, true, false);
		helper.assertTrue(enchantmentLevel(bookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness) == 1,
				"The book gamerule must leave enchanted books unchanged when disabled");
		helper.assertTrue(enchantmentLevel(swordOffer, DataComponents.ENCHANTMENTS, sharpness) == 1,
				"The item gamerule must leave enchanted items unchanged when disabled");

		BetterVillagerTrades.maximizeTradeOffer(bookOffer, true, false);
		BetterVillagerTrades.maximizeTradeOffer(swordOffer, false, true);
		helper.assertTrue(enchantmentLevel(bookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness) == sharpness.value().getMaxLevel(),
				"The book gamerule must maximize enchanted books when enabled");
		helper.assertTrue(enchantmentLevel(swordOffer, DataComponents.ENCHANTMENTS, sharpness) == sharpness.value().getMaxLevel(),
				"The item gamerule must maximize enchanted items when enabled");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	public static void rerollsUntradedVillagersWithoutProtectedOffers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> mending = enchantmentRegistry.wrapAsHolder(
				enchantmentRegistry.getValueOrThrow(Enchantments.MENDING)
		);
		ItemStack mendingBook = enchantedStack(
				new ItemStack(Items.ENCHANTED_BOOK),
				DataComponents.STORED_ENCHANTMENTS,
				mending
		);
		TradeFilter filter = TradeFilter.fromStack(mendingBook);
		MerchantOffer protectedOffer = new MerchantOffer(new ItemCost(Items.EMERALD), mendingBook, 1, 1, 0.0F);
		MerchantOffers originalOffers = new MerchantOffers();
		originalOffers.add(protectedOffer);
		Villager villager = createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
		villager.setOffers(originalOffers);

		PlayerTradeFilters filters = PlayerTradeFilters.get(level.getServer());
		filters.clear(player);
		filters.add(player, filter);
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false),
				"A matching per-player filter must block the reroll");
		helper.assertTrue(villager.getOffers().getFirst() == protectedOffer,
				"A blocked reroll must preserve the existing offers");

		filters.clear(player);
		helper.assertTrue(TradeRerollService.reroll(player, villager, false),
				"An untraded novice villager without protected offers must reroll");
		helper.assertTrue(!villager.getOffers().contains(protectedOffer),
				"A successful reroll must replace the previous offers");

		originalOffers.clear();
		originalOffers.add(protectedOffer);
		villager.setOffers(originalOffers);
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));
		helper.assertTrue(TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND).consumesAction(),
				"Sneak-right-clicking with an emerald must consume the interaction");
		helper.assertTrue(!villager.getOffers().contains(protectedOffer),
				"The emerald interaction must reroll eligible villager trades");
		player.setShiftKeyDown(false);

		protectedOffer.increaseUses();
		originalOffers.clear();
		originalOffers.add(protectedOffer);
		villager.setOffers(originalOffers);
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false),
				"A villager with a used trade must be locked against rerolls");
		Villager nitwit = createVillager(helper, level, VillagerProfession.NITWIT, 1, 3, 2, 2);
		helper.assertTrue(!TradeRerollService.reroll(player, nitwit, false),
				"Nitwits cannot acquire a profession and must not report a successful reroll");
		helper.assertTrue(nitwit.getOffers().isEmpty(), "Rejected nitwits must keep their empty offers");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	public static void rerollRecalculatesOpenMerchantResult(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Villager villager = createVillager(helper, helper.getLevel(), VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
		MerchantOffers offers = new MerchantOffers();
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.NETHER_STAR), 1, 1, 0));
		villager.setOffers(offers);
		villager.setTradingPlayer(player);
		var previousMenu = player.containerMenu;
		var menu = new net.minecraft.world.inventory.MerchantMenu(1, player.getInventory(), villager);
		player.containerMenu = menu;
		try {
			menu.getSlot(0).set(new ItemStack(Items.EMERALD, 64));
			menu.setSelectionHint(0);
			helper.assertTrue(menu.getSlot(2).getItem().is(Items.NETHER_STAR), "Old offer must initially have a result");
			helper.assertTrue(TradeRerollService.reroll(player, villager, false), "An open eligible merchant must reroll");
			helper.assertTrue(!menu.getSlot(2).getItem().is(Items.NETHER_STAR), "Old output must not remain collectible after reroll");
			helper.assertTrue(menu.getSlot(0).getItem().getCount() == 64, "Recalculation must preserve payment items");
			helper.assertTrue(menu.getOffers() == villager.getOffers(), "Menu must expose the new merchant offers");
		} finally {
			player.containerMenu = previousMenu;
			villager.setTradingPlayer(null);
		}
		helper.succeed();
	}

	@SuppressWarnings("removal")
	public static void filterCommandsValidateAndIsolatePlayers(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		PlayerTradeFilters filters = PlayerTradeFilters.get(helper.getLevel().getServer());
		try {
			helper.assertTrue(command(player, "filter list") == 0, "New players have no filters");
			helper.assertTrue(command(player, "filter add minecraft:enchanted_book minecraft:mending") == 1, "Add must succeed");
			helper.assertTrue(command(player, "filter add minecraft:enchanted_book minecraft:mending") == 0, "Duplicates must not be added");
			helper.assertTrue(command(player, "filter list") == 1, "List must report the stored filter count");
			helper.assertTrue(filters.get(other).isEmpty(), "Commands must not change another player's filters");
			helper.assertTrue(command(player, "filter add bettervillagertrades:missing") == 0, "Unknown items must be rejected");
			helper.assertTrue(command(player, "filter add minecraft:book bettervillagertrades:missing") == 0, "Unknown enchantments must be rejected");
			helper.assertTrue(filters.get(player).size() == 1, "Invalid arguments must not add filters");
			helper.assertTrue(command(player, "filter remove minecraft:book") == 0, "Removing a missing filter must report no change");
			helper.assertTrue(command(player, "filter toggle minecraft:book") == 1, "Toggle must add");
			helper.assertTrue(filters.get(player).size() == 2, "Toggled filter must be stored");
			helper.assertTrue(command(player, "filter toggle minecraft:book") == 1, "Toggle must remove");
			helper.assertTrue(command(player, "filter remove minecraft:enchanted_book minecraft:mending") == 1, "Remove must succeed");
			helper.assertTrue(command(player, "filter clear") == 0, "Clearing empty filters must report no change");
			command(player, "filter add minecraft:book");
			helper.assertTrue(command(player, "filter clear") == 1, "Clear must remove stored filters");
			helper.assertTrue(command(player, "reroll") == 0, "Reroll requires an open trading session");
		} finally {
			filters.clear(player);
			filters.clear(other);
		}
		helper.succeed();
	}

	@SuppressWarnings("removal")
	public static void rejectsIneligibleVillagersAndPreservesEmeralds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		Villager villager = createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
		MerchantOffers offers = new MerchantOffers();
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.PAPER), 1, 1, 0));
		villager.setOffers(offers);
		villager.setBaby(true);
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false), "Babies must be rejected");
		villager.setBaby(false);
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NONE));
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false), "Unemployed villagers must be rejected");
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN).withLevel(2));
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false), "Non-novices must be rejected");
		villager.setVillagerData(villager.getVillagerData().withLevel(1));
		villager.setOffers(offers);
		villager.setVillagerXp(1);
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false), "Experienced novices must be rejected");
		villager.setVillagerXp(0);
		villager.setTradingPlayer(other);
		helper.assertTrue(!TradeRerollService.reroll(player, villager, false), "Another player's merchant must be protected");
		villager.setTradingPlayer(null);
		helper.assertTrue(villager.getOffers() == offers, "Rejected rerolls must preserve offers");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
		helper.assertTrue(!TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND).consumesAction(), "Standing interactions must pass through");
		player.setShiftKeyDown(true);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BOOK));
		helper.assertTrue(!TradeRerollService.interact(player, villager, InteractionHand.OFF_HAND).consumesAction(), "Non-emerald interactions must pass through");
		helper.assertTrue(TradeRerollService.interact(player, villager, InteractionHand.MAIN_HAND).consumesAction(), "Emerald reroll must be handled");
		helper.assertTrue(player.getMainHandItem().getCount() == 3, "Reroll must not consume emeralds");
		helper.assertTrue(villager.getOffers() != offers || !offers.getFirst().getResult().is(Items.PAPER), "Eligible interaction must replace offers");
		player.setShiftKeyDown(false);
		helper.succeed();
	}

	public static void filtersMatchBothEnchantmentComponents(GameTestHelper helper) {
		var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> mending = registry.getOrThrow(Enchantments.MENDING);
		Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
		ItemStack book = enchantedStack(new ItemStack(Items.ENCHANTED_BOOK), DataComponents.STORED_ENCHANTMENTS, mending);
		ItemStack sword = enchantedStack(new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, mending);
		TradeFilter bookFilter = TradeFilter.fromStack(book);
		TradeFilter swordFilter = TradeFilter.fromStack(sword);
		helper.assertTrue(bookFilter.matches(book), "Stored enchantments must match");
		helper.assertTrue(swordFilter.matches(sword), "Regular enchantments must match");
		helper.assertTrue(!bookFilter.matches(sword), "An enchantment filter must also match the item");
		helper.assertTrue(!swordFilter.matches(enchantedStack(new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, sharpness)), "Different enchantments must not match");
		helper.assertTrue(!swordFilter.matches(new ItemStack(Items.DIAMOND_SWORD)), "Missing enchantments must not match");
		TradeFilter allSwords = new TradeFilter(swordFilter.item(), java.util.Optional.empty());
		helper.assertTrue(allSwords.matches(new ItemStack(Items.DIAMOND_SWORD)), "Item-only filters must match unenchanted outputs");
		helper.assertTrue(allSwords.matches(sword), "Item-only filters must also match enchanted outputs");
		helper.assertTrue(swordFilter.description().getString().equals("minecraft:diamond_sword with minecraft:mending"), "Vanilla clients must receive readable fallback text");
		helper.succeed();
	}

	public static void registeredGamerulesControlOnlyNewOffers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var rules = level.getGameRules();
		boolean oldBooks = rules.get(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES);
		boolean oldItems = rules.get(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES);
		Holder<Enchantment> sharpness = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
		Villager villager = createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 2, 2, 2);
		try {
			for (boolean books : new boolean[]{false, true}) {
				for (boolean items : new boolean[]{false, true}) {
					rules.set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, books, level.getServer());
					rules.set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, items, level.getServer());
					MerchantOffers offers = new MerchantOffers();
					offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), enchantedStack(new ItemStack(Items.ENCHANTED_BOOK), DataComponents.STORED_ENCHANTMENTS, sharpness), 1, 1, 0));
					offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), enchantedStack(new ItemStack(Items.ENCHANTED_BOOK), DataComponents.STORED_ENCHANTMENTS, sharpness), 1, 1, 0));
					offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), enchantedStack(new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, sharpness), 1, 1, 0));
					villager.setOffers(offers);
					BetterVillagerTrades.maximizeNewVillagerOffers(villager, level, 1);
					helper.assertTrue(enchantmentLevel(offers.get(0), DataComponents.STORED_ENCHANTMENTS, sharpness) == 1, "Previously generated offers must be untouched");
					helper.assertTrue(enchantmentLevel(offers.get(1), DataComponents.STORED_ENCHANTMENTS, sharpness) == (books ? 5 : 1), "Registered book rule must control new outputs");
					helper.assertTrue(enchantmentLevel(offers.get(2), DataComponents.ENCHANTMENTS, sharpness) == (items ? 5 : 1), "Registered item rule must control new outputs");
				}
			}
		} finally {
			rules.set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, oldBooks, level.getServer());
			rules.set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, oldItems, level.getServer());
		}
		helper.succeed();
	}

	private static int command(ServerPlayer player, String command) {
		try {
			return player.level().getServer().getCommands().getDispatcher().execute(
					"bettervillagertrades " + command, player.createCommandSourceStack().withSuppressedOutput()
			);
		} catch (CommandSyntaxException exception) {
			throw new AssertionError("Player command must be registered and executable: " + command, exception);
		}
	}

	private static ItemStack enchantedStack(
			ItemStack stack, DataComponentType<ItemEnchantments> componentType, Holder<Enchantment> enchantment
	) {
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(enchantment, 1);
		stack.set(componentType, enchantments.toImmutable());
		return stack;
	}

	private static int enchantmentLevel(
			MerchantOffer offer, DataComponentType<ItemEnchantments> componentType, Holder<Enchantment> enchantment
	) {
		return offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY).getLevel(enchantment);
	}

	private static boolean generatedTradesHaveOnlyMaxedEnchantments(
			GameTestHelper helper, ServerLevel level, net.minecraft.resources.ResourceKey<VillagerProfession> profession,
			int professionLevel, DataComponentType<ItemEnchantments> componentType
	) {
		boolean foundEnchantments = false;
		for (int i = 0; i < 20; i++) {
			Villager villager = createVillager(helper, level, profession, professionLevel, 1 + (i % 5), 2, 1 + (i / 5));
			for (MerchantOffer offer : villager.getOffers()) {
				ItemEnchantments enchantments = offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY);
				if (enchantments.isEmpty()) continue;
				foundEnchantments = true;
				for (Holder<Enchantment> enchantment : enchantments.keySet()) {
					helper.assertTrue(enchantments.getLevel(enchantment) == enchantment.value().getMaxLevel(),
							"Generated villager trade enchantment should be max level");
				}
			}
		}
		return foundEnchantments;
	}

	private static Villager createVillager(
			GameTestHelper helper, ServerLevel level, net.minecraft.resources.ResourceKey<VillagerProfession> profession,
			int levelNumber, int x, int y, int z
	) {
		Villager villager = helper.spawn(EntityTypes.VILLAGER, x, y, z);
		villager.setNoAi(true);
		villager.getRandom().setSeed(31L * x + 17L * y + z);
		VillagerData villagerData = villager.getVillagerData().withProfession(level.registryAccess(), profession).withLevel(levelNumber);
		villager.setVillagerData(villagerData);
		return villager;
	}
}
