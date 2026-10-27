# Building Nimbus (Phase 1)

Requirements: JDK 21, Gradle 9.x (only needed once, to generate the wrapper), internet access for the first build.

1. Confirm versions in `gradle.properties` against https://fabricmc.net/develop (Minecraft 1.21.11).
2. `gradle wrapper --gradle-version 9.2.1`   (or the version the Fabric example mod currently uses)
3. `./gradlew build`  -> jar in `build/libs/` (use the one without `-sources`)
4. `./gradlew runClient` to launch a dev client.

Phase 1 has no modules yet. Success looks like this line in the log:
`Nimbus started: 0 modules, profile 'default', policy mode SINGLEPLAYER|RESTRICTED`
and a `config/nimbus/config.json` file appearing on first exit.
