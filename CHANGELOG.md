# Changelog

All notable changes to Inventory ToDo. The newest version is at the top.
The release workflow uses the section that matches the tag as the release notes.

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
