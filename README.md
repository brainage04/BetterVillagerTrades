# BetterVillagerTrades

BetterVillagerTrades is a server-side Fabric and NeoForge mod for Minecraft 26.2 that improves villager trading without requiring modded clients.

Vanilla clients can sneak-right-click an eligible villager while holding an emerald. The emerald is not consumed. Players with BetterVillagerTrades installed on the client also receive **Reroll trades** and **Toggle filter** buttons beneath the standard trading screen.

## Features

- Newly generated enchanted-book and enchanted-item outputs use each enchantment's maximum level.
- Players can reroll an eligible villager's offers.
- Persistent, per-player filters prevent rerolls when the current offers contain a desired item or enchantment.

### Rerolling trades

A villager can be rerolled only when all of these conditions are true:

- It is an adult with a trading profession, not a nitwit.
- It is still a novice with zero trade experience.
- None of its offers have been used.
- None of its current offers match one of the requesting player's filters.
- It is not trading with another player.

### Per-player filters

The trading-screen filter button toggles protection for the selected offer. Enchanted outputs are protected by item and enchantment; other outputs are protected by item.

For example, `/bettervillagertrades filter add minecraft:enchanted_book minecraft:mending` protects any Mending book offered by the villager. Filters are stored per player in the world save; one player's filters never become global rules.

## Getting started

- Fabric Loader >=0.19.3 with Fabric API, or NeoForge >=26.2.0.88
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
