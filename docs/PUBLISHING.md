# Publishing

| Where | ID / URL | Status |
| --- | --- | --- |
| GitHub | https://github.com/junjun1z/inventory-todo | public |
| Modrinth | project `WtZSRYTy`, slug `inventory-todo` (https://modrinth.com/mod/inventory-todo) | draft until submitted |
| CurseForge | project `1801027`, slug `inventory-todo` (https://www.curseforge.com/minecraft/mc-mods/inventory-todo) | visible after moderation |

GitHub secrets: `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`. Variables: `MODRINTH_PROJECT_ID`, `CURSEFORGE_PROJECT_ID`.

## Release

1. Add a `## [X.Y.Z]` section to `CHANGELOG.md` and make sure `publish.json` lists every jar.
2. `git tag X.Y.Z && git push origin X.Y.Z`
3. The **Release** workflow builds all targets, creates the GitHub release and uploads to Modrinth and CurseForge.
   Re-running it is safe for Modrinth (existing versions are skipped); CurseForge would get duplicate files.

Dry run without a tag: Actions → Release → Run workflow (version, dry run checked), or locally
`python tools/publish.py --version X.Y.Z --dry-run --allow-missing`.

## Modrinth page

`python tools/modrinth_setup.py --status | --update | --icon | --gallery | --submit`
(`--gallery` uploads `docs/screenshots/*.png`; `--submit` sends the draft to review and needs at least one version.)

After changing `publish.json`, run `python tools/gen_support_table.py` to refresh the version tables in the docs.
