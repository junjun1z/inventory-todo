<p align="center"><img src="docs/icon_128.png" alt="Inventory ToDo icon" width="128"></p>

<h1 align="center">Inventory ToDo</h1>

<p align="center">
A to-do list right next to your inventory — notes and item goals that count up as you collect.<br>
<a href="https://modrinth.com/mod/inventory-todo">Modrinth</a> ·
<a href="https://www.curseforge.com/minecraft/mc-mods/inventory-todo">CurseForge</a> ·
<a href="README.ja.md">日本語</a>
</p>

> **Works in any modpack, with any combination of mods — no required dependencies.**
> Forge, Fabric and NeoForge, from 1.7.10 to the latest versions. Recipe viewers (NEI / JEI / EMI / REI) are optional extras.

## Features

- **Notes and item goals in one list** — write anything, or add an item and set how many you need.
- **Live counting** — shows `[32/64]` from your inventory and ticks rows off automatically when the goal is reached.
- **Add items in seconds** — Alt+click in your inventory, click the panel while holding an item, search by name or ID (English names work in every language), or drag & drop from your recipe viewer.
- **Quick goal editing** — mouse wheel (Shift ×16, Ctrl ×64), click to type, right-click to use your current amount.
- **Out of your way** — follow the inventory, pin to the left edge, or drag anywhere; minimize to an icon with the number of open tasks.
- **HUD** — unfinished tasks on screen while you play (toggle key).
- **Recipe ingredients** — Ctrl+click an item's icon to add its crafting ingredients, multiplied by what you still need.
- **Counts open containers** — with a chest open, its contents count too (optional).
- **Per-world or shared lists**, **key bindings**, exact item matching (metadata/NBT), **10 languages**.

Client-side only: not needed on servers, and you can join servers without it.

## Supported versions

<!-- support-table:start -->
| Minecraft | Loaders |
| --- | --- |
| 26.3 | Forge, NeoForge, Fabric |
| 26.2 | Forge, NeoForge, Fabric |
| 26.1.2 | Forge, NeoForge, Fabric |
| 26.1.1 | NeoForge, Fabric |
| 26.1 | NeoForge, Fabric |
| 1.21.11 | Forge, NeoForge, Fabric |
| 1.21.10 | Forge, NeoForge, Fabric |
| 1.21.9 | Forge, Fabric |
| 1.21.8 | Forge, NeoForge, Fabric |
| 1.21.7 | Fabric |
| 1.21.6 | Fabric |
| 1.21.5 | Forge, NeoForge, Fabric |
| 1.21.4 | Forge, NeoForge, Fabric |
| 1.21.3 | Forge, NeoForge, Fabric |
| 1.21.2 | Fabric |
| 1.21.1 | Forge, NeoForge, Fabric |
| 1.21 | NeoForge, Fabric |
| 1.20.6 | Forge, NeoForge, Fabric |
| 1.20.5 | Fabric |
| 1.20.4 | Forge, NeoForge, Fabric |
| 1.20.3 | Forge, Fabric |
| 1.20.2 | Forge, Fabric |
| 1.20.1 | Forge, NeoForge, Fabric |
| 1.20 | Forge, NeoForge, Fabric |
| 1.19.4 | Forge, Fabric |
| 1.19.3 | Forge, Fabric |
| 1.19.2 | Forge, Fabric |
| 1.19.1 | Forge, Fabric |
| 1.19 | Forge, Fabric |
| 1.18.2 | Forge, Fabric |
| 1.18.1 | Forge, Fabric |
| 1.18 | Forge, Fabric |
| 1.17.1 | Forge, Fabric |
| 1.17 | Fabric |
| 1.16.5 | Forge, Fabric |
| 1.16.4 | Forge, Fabric |
| 1.16.3 | Forge, Fabric |
| 1.16.2 | Fabric |
| 1.16.1 | Fabric |
| 1.16 | Fabric |
| 1.15.2 | Forge, Fabric |
| 1.15.1 | Forge, Fabric |
| 1.15 | Fabric |
| 1.14.4 | Forge, Fabric |
| 1.14.3 | Fabric |
| 1.14.2 | Fabric |
| 1.14.1 | Fabric |
| 1.14 | Fabric |
| 1.12.2 | Forge |
| 1.12.1 | Forge |
| 1.12 | Forge |
| 1.11.2 | Forge |
| 1.11 | Forge |
| 1.10.2 | Forge |
| 1.10 | Forge |
| 1.9.4 | Forge |
| 1.9 | Forge |
| 1.8.9 | Forge |
| 1.7.10 | Forge |
<!-- support-table:end -->

## Optional integrations

| Mod | What you get |
| --- | --- |
| NEI (original or GTNH fork) | Modifier+click / drag & drop from the item list, recipes/usages on icon click, NEI keys over list items |
| JEI (1.18+) / EMI / REI (1.17+) | Hovered-item adding, drag & drop, recipes/usages on icon click |

## FAQ

- **Server?** Not needed — client-side only.
- **Modpacks?** MIT licensed: include it in any modpack, no permission needed.
- **Where is my list?** `config/inventorytodo/` (one file per world/server, plus the shared list).
- **Coming from "GTNH ToDo"?** Lists and settings are imported automatically.

## For developers

| Folder | Contents |
| --- | --- |
| `mc1.7.10/` | Forge 1.7.10 (GTNH buildscript). `./gradlew build` with JDK 17+ |
| `modern/` | Forge / Fabric / NeoForge for modern versions. See `modern/BUILD.md` |
| `tools/` | Translations, icon, publishing scripts |
| `publish.json` | Which jar goes to which loader / Minecraft version |

**Releasing:** update `CHANGELOG.md`, then push a tag like `1.2.3`. GitHub Actions builds every target and publishes to GitHub Releases, Modrinth and CurseForge (`.github/workflows/release.yml`). You can also run the Release workflow by hand as a dry run.

Translations live in `tools/translations.py` — pull requests for new languages or fixes are welcome.

## License

[MIT](LICENSE)
