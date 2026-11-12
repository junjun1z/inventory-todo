# Inventory ToDo — Minecraft 1.7.10 (Forge)

Source of the 1.7.10 version. See the [main README](../README.md) for features and downloads.

- Works on its own (Forge only). When NotEnoughItems is installed (ChickenBones' original 1.0.5.x or the GTNH fork), it integrates with it automatically.
- Client-side only.

## Build

Requires JDK 17 or newer (the GTNH build script compiles Java 8 bytecode with Jabel).

```
./gradlew build
```

The mod jar is `build/libs/inventorytodo-<version>.jar` (ignore `-dev` and `-sources`).

## Layout

| Path | Contents |
| --- | --- |
| `TodoPanel` | The panel: input, drawing, data handling. No dependency on other mods. |
| `TodoHud` | List shown on the HUD while playing |
| `compat/VanillaFrontend` | Standalone mode: reads GUI input right before the screen does, draws after the screen |
| `compat/nei/*` | NEI mode: NEI GUI hooks. Loaded only when NEI is present; implements every interface method so it works with the original NEI too |
| `devtools/SelfTestMod.java` | Development-only automated in-game test (never part of the release jar) |

Language files are generated from `tools/translations.py` and `tools/translations_features.py`:

```
python ../tools/translations.py src/main/resources/assets/inventorytodo/lang
```
