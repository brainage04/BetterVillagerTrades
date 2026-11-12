# Development

Run `./gradlew --no-daemon build` to emit both loader artifacts under `build/libs`.

The Fabric unit tests live in `fabric/src/test`. Shared server GameTests live in `common/src/gametest` and are registered by both loaders. The Fabric client scenario lives in `fabric/src/gametest`; the film it records (staging, captions and shots) lives in `fabric/src/recording`.

The server GameTests join players through `GameTestPlayer` from BrainageLib's test-only `brainagelib-gametest` artifact, which never ships in the mod. Until BrainageLib 2.0.0 is on Maven Central, build it from a sibling checkout: run `./gradlew --no-daemon publishAllPublicationsToLocalRepository` in `../BrainageLib`.

Run `./gradlew --no-daemon build runAllGameTests` for the build, unit tests and both server GameTest suites. Run `./gradlew --no-daemon :fabric:runClientGameTest` for the headless client scenario. On Thomas's hosts, put these commands through `heavy`, as described in the workspace `AGENTS.md`.

