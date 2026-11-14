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

## Nested `legacy/` build

Targets that need a different Gradle plugin live in `modern/legacy/` (a separate build with its own plugin
classpath, so it cannot clash with Fabric Loom / ModDevGradle). `./gradlew build` and `./gradlew collectJars`
in `modern/` build it automatically; `-Ptargets=` and `-Pmod_version=` apply to it as well.

| Target | Plugin |
| --- | --- |
| `1.16.5-forge` | Architectury Loom (official mappings, remapped to SRG) |
| `1.12.2-forge` | RetroFuturaGradle (MCP names, reobfuscated) |
| `26.3-forge` | ForgeGradle 7 |
| `1.15.2-forge`, `1.14.4-forge` | Architectury Loom (official mappings, remapped to SRG) |
| `1.20.6-forge` | ForgeGradle 7 (Forge 1.20.6 runs on Mojang names: no reobfuscation) |
| `1.20.2-forge`, `1.20.4-forge` | ForgeGradle 7, then ForgeAutoRenamingTool renames the jar to SRG names (`gradle/forge7_srg.gradle`) |
| `1.21.1-forge` … `1.21.11-forge`, `26.1.2-forge`, `26.2-forge` | ForgeGradle 7 (Forge 1.20.6+ runs on Mojang names: no reobfuscation) |

Its jars: `modern/legacy/targets/<mc>-forge/build/libs/inventorytodo-forge-<mc>-<version>.jar`.
Java 8 toolchains (1.12.2 / 1.16.5) are downloaded automatically like the others.

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
With real input it also checks that the panel's tooltips are really drawn (pixels in the screenshots) and, if JEI,
EMI or REI is installed, drags an item from the viewer's list onto the panel.

## Nested `oldforge/` build (MinecraftForge 1.8.9 - 1.11.2)

A third nested build, `modern/oldforge/`, builds the old Forge targets with **Unimined** (MCP names in development,
reobfuscated to SRG names in the jar). It has its own plugin classpath like `legacy/`, and `./gradlew build`,
`collectJars`, `clean`, `-Ptargets=` and `-Pmod_version=` in `modern/` include it automatically.

| Target | Jar works on | Code |
| --- | --- | --- |
| `1.8.9-forge` | 1.8.9 | `versions/1.8.9`, `loaders/forge_uni/1.8.9` |
| `1.9.4-forge` | 1.9, 1.9.4 | `versions/1.9.4`, `loaders/forge_uni/1.9.4` |
| `1.10.2-forge` | 1.10, 1.10.2 | `versions/1.10.2`, `loaders/forge_uni/1.10.2` |
| `1.11.2-forge` | 1.11, 1.11.2 | `versions/1.11.2`, `loaders/forge_uni/1.11.2` |

Its jars: `modern/oldforge/targets/<mc>-forge/build/libs/inventorytodo-forge-<mc>-<version>.jar`.
Each target sets `forge_version` and `mcp_mappings` in its `gradle.properties`; `.lang` files are generated from the
shared JSON files (`lang_upper=true` gives the `en_US.lang` names used before 1.11).
