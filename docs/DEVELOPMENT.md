# Development

Run `./gradlew --no-daemon build` to emit both loader artifacts under `build/libs`.

The Fabric unit tests live in `fabric/src/test`. Shared server GameTests live in `common/src/gametest` and are registered by both loaders. The Fabric client scenario lives in `fabric/src/gametest`.

Run `./gradlew --no-daemon build runAllGameTests -Pfabricmoddingconventions.devClientOptions=/nonexistent` for the build, unit tests and both server GameTest suites. Run `./gradlew --no-daemon :fabric:runClientGameTest -Pfabricmoddingconventions.devClientOptions=/nonexistent` for the headless client scenario. On Thomas's hosts, put these commands through `heavy`, as described in the workspace `AGENTS.md`.

