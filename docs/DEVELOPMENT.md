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
