# Building the modern versions (1.20.1+)

One Gradle build produces a jar for every Minecraft version / loader in `targets/`.

## Requirements

- **JDK 25 or newer to run Gradle** (Fabric Loom 1.19 needs it). Locally JDK 27 works too.
- The targets compile with Java toolchains 17 / 21 / 25. Missing JDKs are downloaded automatically
  (foojay resolver), so CI only needs one JDK 25 installed.

## Commands

```bash
cd modern
./gradlew build                              # all targets
./gradlew build -Pmod_version=1.2.3          # release version (or set MOD_VERSION)
./gradlew collectJars -Pmod_version=1.2.3    # also copies every jar to modern/build/dist/
./gradlew build -Ptargets=1.21.1-neoforge    # only some targets (comma separated)
```

Jars: `modern/targets/<mc>-<loader>/build/libs/inventorytodo-<loader>-<mc>-<version>.jar`
(exactly one jar per target; `publish.json` at the repository root lists them).

GitHub Actions example:

```yaml
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: 25 }
- run: ./gradlew collectJars -Pmod_version=${VERSION} --no-daemon
  working-directory: modern
```

## Layout

| Path | Contents |
| --- | --- |
| `common/` | Version-independent logic (panel, HUD, settings, storage, text field). No Minecraft imports. |
| `common/src/main/resources/assets/inventorytodo/lang/` | Generated: `python tools/translations.py <dir> --json` |
| `versions/<code_version>/` | Adapter to one Minecraft API generation (`McImpl`, `GfxImpl`). |
| `loaders/<loader>/<code_version>/` | Loader entry point, events, metadata (fabric.mod.json / mods.toml). |
| `targets/<mc>-<loader>/gradle.properties` | One build target: versions of Minecraft, loader, Java. |
| `gradle/*.gradle` | Shared build logic per loader. |
| `devtest/` | Automated in-game test, compiled **only** with `-Pselftest` (never in release jars). |

Adding a target: create `targets/<mc>-<loader>/gradle.properties` (copy a neighbour). If the Minecraft API
differs, add `versions/<code_version>` and `loaders/<loader>/<code_version>` and point `code_version` at them.

## In-game self-test (development)

```bash
./gradlew build -Ptargets=26.3-fabric -Pselftest
```

Put the jar into an instance, create `config/inventorytodo/selftest.flag`, launch straight into a world
(`prismlauncher --launch <instance> --world <world>`). The test drives the panel (also with real mouse/keyboard
input through `java.awt.Robot`), saves screenshots and `selftest_result.txt` in the game directory, then quits.
