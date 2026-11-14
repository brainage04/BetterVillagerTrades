# Development

Run `./gradlew --no-daemon build` to emit both loader artifacts under `build/libs`.

The Fabric unit tests live in `fabric/src/test`. Shared server GameTests live in `common/src/gametest` and are registered by both loaders. The Fabric client scenario lives in `fabric/src/gametest`; the film it records (staging, captions and shots) lives in `fabric/src/recording`.

`TradePriceTest` checks a trade's enchantment level and price against vanilla's EnchantRandomlyFunction with jqwik properties: a maximized trade costs what vanilla charges for the maximum level, drawn from the same random sequence, a trade with the game rule off is rolled and priced exactly as vanilla does it, and maximizing raises every level below its maximum and lowers none. jqwik finds no properties inside the Fabric loader, so `:fabric:propertyTest` runs `TradePriceTest` without it, and `:fabric:test` runs the other unit tests in the loader; `check` runs both. `:fabric:pitest` runs mutation tests on `BetterVillagerTrades` against `TradePriceTest`; it is not part of `check`, and the report is written to `fabric/build/reports/pitest`.

The server GameTests join players through `GameTestPlayer` from BrainageLib's test-only `brainagelib-gametest` artifact, which never ships in the mod. It is not on Maven Central: build it from a sibling checkout by running `./gradlew --no-daemon publishAllPublicationsToLocalRepository` in `../BrainageLib`. CI does the same through `prepare_siblings`.

Run `./gradlew --no-daemon build runAllGameTests` for the build, unit tests and both server GameTest suites. Run `./gradlew --no-daemon :fabric:runClientGameTest` for the headless client scenario. On Thomas's hosts, put these commands through `heavy`, as described in the workspace `AGENTS.md`.

