# Development

Run `./gradlew --no-daemon build` to emit both loader artifacts under `build/libs`.

The Fabric unit tests live in `fabric/src/test`. Shared server GameTests live in `common/src/gametest` and are registered by both loaders. The Fabric client scenario lives in `fabric/src/gametest`.

Run `./gradlew --no-daemon build runAllGameTests -Pfabricmoddingconventions.devClientOptions=/nonexistent` for the build, unit tests and both server GameTest suites. Run `./gradlew --no-daemon :fabric:runClientGameTest -Pfabricmoddingconventions.devClientOptions=/nonexistent` for the headless client scenario. On Thomas's hosts, put these commands through `heavy`, as described in the workspace `AGENTS.md`.

## Test pruning review (2026-10-19)

Frozen rubric: keep a test that exercises production behaviour with an observable result; mark it redundant only when another named test covers the same path and assertions; mark it tautological only when it checks a copied implementation, mock echo, wiring without behaviour, or a constant without behaviour.

All existing tests are kept:

- `registersCustomGameRulesWithEnabledDefaults`: constructs real game rules after loader registration and checks the defaults players receive.
- `formatsTradeModificationLog`: checks the production formatter's context and enchantment change output, not a copied formatter.
- `keepsTradeFiltersIsolatedPerPlayer`: checks mutation, duplicate prevention and player isolation.
- `generatedVillagerTradesAreMaxed`: checks real generated villager offers.
- `gamerulesIndependentlyControlBookAndItemTradeOutputs`: checks independent component changes under each rule combination.
- `rerollsUntradedVillagersWithoutProtectedOffers`: checks protection, replacement, emerald interaction and used-trade/nitwit rejection.
- `BetterVillagerTradesClientGameTest.runTest`: checks client-visible book and equipment enchantments and merchant controls.

The Fabric methods and NeoForge registrations delegate to the shared server bodies so each loader exercises its own integration. They are not redundant test implementations. No tests were flagged or removed.

## Coverage additions (2026-10-19)

The review found no command-dispatch tests, no saved-filter round trip, no negative item/enchantment matching cases, no tests for most eligibility failures or emerald preservation, and no registered-gamerule test that distinguishes existing offers from new offers. The client scenario only checked button labels, not their effect.

Added a unit test for saved filters and four shared GameTests for commands, eligibility/interaction, component matching and the registered gamerules. Both loaders register all four. Tests restore changed gamerules and clear their player filters; generated villagers have fixed random seeds and no AI.

The Fabric client scenario now opens a server-backed merchant menu and clicks both controls. It waits for server filter changes, checks that protection blocks replacement, then checks the rerolled offers arrive on the client.

Remaining unautomated player-visible areas: NeoForge client controls, real server restart/disk persistence (the save codec is covered), translated non-English resource packs and physical mouse layout at unusual GUI scales. This mod has no custom rendering, config file, background tasks or shutdown resources.

## Code review (2026-10-19)

Fixed an open-menu bug: replacing merchant offers did not recalculate the merchant container's active offer or result slot. The old result could remain available until a payment or selection changed. Rerolls now select the first new offer, recalculate the result and broadcast the updated slots. `rerollRecalculatesOpenMerchantResult` covers the stale-output case on both loaders and checks that payment items are retained.

The loader callbacks use shared server-thread logic; saved filters are owned by the server's overworld data storage. No worker threads, listeners requiring removal, file handles or shutdown hooks were found.

Judgement calls left unchanged: the filter button protects only the first enchantment sorted by identifier when an output has several, and client controls remain visible on servers without the mod. The former is the existing deterministic single-enchantment filter design; the latter needs a server-capability protocol rather than guessing from command availability.
