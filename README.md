# BetterVillagerTrades

BetterVillagerTrades is a server-side Fabric and NeoForge mod for Minecraft 26.3 that improves villager trading without requiring modded clients.

Any player outside spectator mode, also on a vanilla client, can reroll a villager by sneak-right-clicking it with an emerald in their main hand. The emerald is not consumed. When the villager cannot be rerolled, as listed under [Rerolling trades](#rerolling-trades), the same gesture opens the villager's normal trading screen instead of doing nothing. Players with BetterVillagerTrades installed on the client also receive **Reroll trades** and **Toggle filter** buttons beneath the standard trading screen on servers that run BetterVillagerTrades. Each button appears only when the server lets the player use its command (`reroll` or `filter toggle`).

## Features

- Newly generated enchanted-book and enchanted-item outputs are raised to each enchantment's maximum level. Levels above the maximum, which a datapack can give, stay as they are.
- Players can reroll an eligible villager's offers.
- Persistent, per-player filters prevent rerolls when the current offers contain a desired item or enchantment.

### Rerolling trades

A villager can be rerolled only when all of these conditions are true:

- It is an adult whose profession has novice trades. Nitwits and unemployed villagers have none.
- It is still a novice with zero trade experience.
- None of its offers have been used.
- None of its current offers match one of the requesting player's filters.
- It is not trading with another player.

A player can reroll at most once every 10 ticks (0.5 seconds). A gesture in between does not reroll and opens normal trading, like every gesture on a villager that cannot be rerolled.

### Per-player filters

The trading-screen filter button toggles protection for the selected offer. Enchanted outputs are protected by item and enchantment; other outputs are protected by item.

For example, `/bettervillagertrades filter add minecraft:enchanted_book minecraft:mending` protects any Mending book offered by the villager. Filters are stored per player in the world save; one player's filters never become global rules.

## Getting started

- Fabric Loader >=0.19.5 with Fabric API, or NeoForge >=26.3.0.99
- Java 25 or newer

Install exactly one matching server JAR: the Fabric JAR with Fabric API, or the NeoForge JAR with NeoForge. Vanilla clients can join. Installing the matching mod on a client is optional and only adds the trading-screen controls.

## Commands

- `/bettervillagertrades reroll` - reroll the villager in your open trading session.
- `/bettervillagertrades filter add <item> [enchantment]` - protect matching trade outputs.
- `/bettervillagertrades filter remove <item> [enchantment]` - remove that filter.
- `/bettervillagertrades filter toggle <item> [enchantment]` - add or remove that filter.
- `/bettervillagertrades filter list` - list your protected trades.
- `/bettervillagertrades filter clear` - clear your filters.

## Configuration

Two boolean gamerules retain independent control of maximum-level outputs:

- `/gamerule bettervillagertrades:max_enchanted_book_trades <true|false>` - controls stored enchantments on enchanted books.
- `/gamerule bettervillagertrades:max_enchanted_item_trades <true|false>` - controls regular enchantments on enchanted items.

Both default to `true`.

## Compatibility

Vanilla and modded trade factories are supported as long as their outputs use standard item enchantment components.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - builds and GameTests
- [docs/RELEASE.md](docs/RELEASE.md) - release automation
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing
