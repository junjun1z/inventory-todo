# Changelog

All notable changes to Inventory ToDo. The newest version is at the top.
The release workflow uses the section that matches the tag as the release notes.

## [1.2.0]

### Added
- Every Minecraft release from 1.8.9 to 26.3 is now supported (60+ game versions):
  - Forge 1.8.9, 1.9.x, 1.10.x, 1.11.x, 1.12–1.12.1, 1.14.4, 1.15.x, 1.16.3–1.16.4, 1.17.1, 1.18–1.18.1, 1.19–1.19.4, 1.20–1.20.6, 1.21.1–1.21.5, 1.21.8–1.21.11, 26.1.2, 26.2
  - Fabric 1.14–1.14.4, 1.15–1.15.2, 1.16–1.16.4, 1.17–1.17.1, 1.18–1.18.1, 1.19–1.19.4, 1.20–1.20.6, 1.21–1.21.10
  - NeoForge 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.3–1.21.5, 1.21.8, 1.21.10
- See the "Supported versions" table for the full list

## [1.1.0]

### Added
- Minecraft 1.12.2 (Forge), 1.16.5 (Forge, Fabric), 1.21.11 (Fabric, NeoForge) and 26.3 (Forge)

### Fixed
- 26.x: left click opened the row menu and right click / header dragging did nothing (mouse button numbering)
- 26.x: typing into notes and goal amounts did not work
- All modern versions: releasing a click on the panel could reach the inventory screen and drop the held item

## [1.0.0]

First release.

- To-do panel next to every inventory screen: notes and item goals in one list
- Live counting of goal items with automatic check-off when the goal is reached
- Add items with modifier+click, by clicking the panel with a held item, by name/ID search (English names work in every language) or by drag & drop from a recipe viewer
- Quick goal editing: mouse wheel (Shift ×16, Ctrl ×64), typing, right-click to use the current amount
- Three positions (follow inventory, left edge, free) and minimize to an icon
- HUD showing unfinished tasks, Ctrl+click to add recipe ingredients, counting open containers, per-world or shared lists, key bindings
- Optional integration with NEI (original and GTNH fork), JEI, EMI and REI — none of them are required
- Works in any modpack with any combination of mods; client-side only
- 10 languages: English, Japanese, Simplified Chinese, Traditional Chinese, Korean, Russian, German, French, Spanish, Brazilian Portuguese
- Imports lists and settings from "GTNH ToDo" automatically
