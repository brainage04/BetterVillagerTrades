# BetterVillagerTrades

BetterVillagerTrades is a server-side Fabric and NeoForge mod for Minecraft 26.3 that improves villager trading without requiring modded clients.

Any player outside spectator mode, also on a vanilla client, can reroll a villager by sneak-right-clicking it with an emerald in their main hand. The emerald is not consumed. When the villager cannot be rerolled, as listed under [Rerolling trades](#rerolling-trades), the same gesture opens the villager's normal trading screen and still says why it could not reroll. Players with BetterVillagerTrades installed on the client also receive **Reroll trades** and **Toggle filter** buttons beneath the standard trading screen on servers that run BetterVillagerTrades. Each button appears only when the server lets the player use its command (`reroll` or `filter toggle`).

## Features

- Newly generated enchanted-book and enchanted-item outputs are raised to each enchantment's maximum level. Levels above the maximum, which a datapack can give, stay as they are.
- An enchanted book is priced for that maximum level: the emerald count is worked out again with vanilla's own enchantment price, so a Sharpness V book costs what vanilla would charge for Sharpness V.
- Players can reroll an eligible villager's offers.
- Persistent, per-player filters prevent rerolls when the current offers contain a desired item or enchantment.

### Rerolling trades

A villager can be rerolled only when all of these conditions are true:

- It is an adult whose profession has novice trades. Nitwits and unemployed villagers have none.
- It is still a novice with zero trade experience.
- None of its offers have been used.
- None of its current offers match one of the requesting player's filters.
- It is not trading with another player.

A player can reroll at most once every 10 ticks (0.5 seconds). A gesture inside that cooldown does nothing: it neither rerolls nor opens trading, so a held sneak-use keeps rerolling at that rate.

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

### Book prices

Vanilla prices an enchanted book trade at whatever level it rolled: `2 + random(5 + level * 10) + 3 * level` emeralds, doubled for enchantments in `#minecraft:double_trade_price`, added to the trade's own emerald count (`EnchantRandomlyFunction.java:98` and `VillagerTrade.java:109-122` in the 26.3 sources). Because the level is raised before that line runs, the price follows the maximum level: a book that vanilla rolled at Sharpness I and the mod raises to Sharpness V costs 17 to 64 emeralds, the vanilla range for a Sharpness V roll clamped at the emerald stack size, instead of the 5 to 19 it cost at Sharpness I.

Enchanted items keep vanilla's price. Vanilla prices an item trade by the enchantment power it rolled, not by the levels that power produces (`EnchantWithLevelsFunction.java:77-83`), so raising the levels leaves that price as vanilla charged it.

Trades that set their own price and only happen to carry enchantments are not repriced either, and the `trade_rebalance` experiment is one of them. Its master librarian sells one fixed-level book per villager type and vanilla charges `3 * level + 2` plus up to `5 + 10 * level` more for the level it lists (`TradeRebalanceVillagerTrades.java:345-378`, priced in that trade's own emerald count at `TradeRebalanceVillagerTrades.java:865-875`). Those books are not rolled, so vanilla never prices them from an enchantment level; the mod raises them to the maximum in its pass over the finished offers, and they keep the price of the level vanilla listed: a Sharpness III book raised to Sharpness V still costs what Sharpness III cost.

## Compatibility

Vanilla and modded trade factories are supported as long as their outputs use standard item enchantment components.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - builds and GameTests
- [docs/RELEASE.md](docs/RELEASE.md) - release automation
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing
