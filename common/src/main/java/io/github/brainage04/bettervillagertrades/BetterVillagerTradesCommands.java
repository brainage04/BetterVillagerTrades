package io.github.brainage04.bettervillagertrades;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/** Player commands used directly and by the optional merchant-screen buttons. */
public final class BetterVillagerTradesCommands {
	private static final String ITEM_ARGUMENT = "item";
	private static final String ENCHANTMENT_ARGUMENT = "enchantment";

	private BetterVillagerTradesCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bettervillagertrades")
				.then(Commands.literal("reroll").executes(context ->
						TradeRerollService.rerollTradingVillager(context.getSource().getPlayerOrException())))
				.then(Commands.literal("filter")
						.then(filterMutation("add", Mutation.ADD))
						.then(filterMutation("remove", Mutation.REMOVE))
						.then(filterMutation("toggle", Mutation.TOGGLE))
						.then(Commands.literal("list").executes(context -> list(context.getSource())))
						.then(Commands.literal("clear").executes(context -> clear(context.getSource())))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> filterMutation(String name, Mutation mutation) {
		return Commands.literal(name).then(itemArgument()
				.executes(context -> mutate(
						context.getSource(),
						IdentifierArgument.getId(context, ITEM_ARGUMENT),
						Optional.empty(),
						mutation
				))
				.then(enchantmentArgument().executes(context -> mutate(
						context.getSource(),
						IdentifierArgument.getId(context, ITEM_ARGUMENT),
						Optional.of(IdentifierArgument.getId(context, ENCHANTMENT_ARGUMENT)),
						mutation
				))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Identifier> itemArgument() {
		return Commands.argument(ITEM_ARGUMENT, IdentifierArgument.id())
				.suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Identifier> enchantmentArgument() {
		return Commands.argument(ENCHANTMENT_ARGUMENT, IdentifierArgument.id())
				.suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
						context.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElementIds().map(ResourceKey::identifier),
						builder
				));
	}

	private static int mutate(
			CommandSourceStack source,
			Identifier item,
			Optional<Identifier> enchantment,
			Mutation mutation
	) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!BuiltInRegistries.ITEM.containsKey(item)) {
			source.sendFailure(Component.translatableWithFallback("bettervillagertrades.filter.unknown_item", "Unknown item: %s", item.toString()));
			return 0;
		}
		if (enchantment.isPresent()) {
			ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantment.get());
			if (source.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).isEmpty()) {
				source.sendFailure(Component.translatableWithFallback("bettervillagertrades.filter.unknown_enchantment", "Unknown enchantment: %s", enchantment.get().toString()));
				return 0;
			}
		}

		TradeFilter filter = new TradeFilter(item, enchantment);
		PlayerTradeFilters filters = PlayerTradeFilters.get(source.getServer());
		return switch (mutation) {
			case ADD -> changed(source, filters.add(player, filter), "added", "already_added", "Protected %s.", "Already protected %s.", filter);
			case REMOVE -> changed(source, filters.remove(player, filter), "removed", "not_found", "Removed protection for %s.", "No protection existed for %s.", filter);
			case TOGGLE -> {
				boolean added = filters.toggle(player, filter);
				source.sendSuccess(() -> Component.translatableWithFallback(
						added ? "bettervillagertrades.filter.added" : "bettervillagertrades.filter.removed",
						added ? "Protected %s." : "Removed protection for %s.", filter.description()), false);
				yield 1;
			}
		};
	}

	private static int changed(
			CommandSourceStack source,
			boolean changed,
			String changedKey,
			String unchangedKey,
			String changedFallback,
			String unchangedFallback,
			TradeFilter filter
	) {
		source.sendSuccess(() -> Component.translatableWithFallback("bettervillagertrades.filter." + (changed ? changedKey : unchangedKey),
				changed ? changedFallback : unchangedFallback, filter.description()), false);
		return changed ? 1 : 0;
	}

	private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		List<TradeFilter> filters = PlayerTradeFilters.get(source.getServer()).get(source.getPlayerOrException());
		if (filters.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("bettervillagertrades.filter.empty", "You have no protected villager trades."), false);
			return 0;
		}
		source.sendSuccess(() -> Component.translatableWithFallback("bettervillagertrades.filter.list", "Protected villager trades (%s):", filters.size()), false);
		filters.forEach(filter -> source.sendSuccess(() -> Component.translatableWithFallback(
				"bettervillagertrades.filter.entry", "- %s", filter.description()), false));
		return filters.size();
	}

	private static int clear(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		boolean changed = PlayerTradeFilters.get(source.getServer()).clear(player);
		source.sendSuccess(() -> Component.translatableWithFallback(
				changed ? "bettervillagertrades.filter.cleared" : "bettervillagertrades.filter.empty",
				changed ? "Cleared all protected villager trades." : "You have no protected villager trades."), false);
		return changed ? 1 : 0;
	}

	private enum Mutation {
		ADD,
		REMOVE,
		TOGGLE
	}
}
