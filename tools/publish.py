#!/usr/bin/env python3
"""Upload release jars listed in publish.json to Modrinth and CurseForge.

Usage:
  python tools/publish.py --version 1.2.3 [--dry-run] [--only modrinth|curseforge]

Environment:
  MODRINTH_TOKEN, MODRINTH_PROJECT_ID        (Modrinth is skipped if either is missing)
  CURSEFORGE_TOKEN, CURSEFORGE_PROJECT_ID    (CurseForge is skipped if either is missing)
  Locally, tokens are also read from .secrets/modrinth_token and .secrets/curseforge_token.

publish.json format:
  {"artifacts": [{"path": "<glob>", "exclude": ["<glob>"], "loaders": ["fabric"],
                  "game_versions": ["1.21.1"], "java": 21,
                  "dependencies": {"required": ["fabric-api"], "optional": ["jei"]}}]}
"""

import argparse
import fnmatch
import glob
import json
import os
import re
import subprocess
import sys
import time

import requests

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
USER_AGENT = "junjun1z/inventory-todo-publisher (https://github.com/junjun1z/inventory-todo)"
MODRINTH_API = "https://api.modrinth.com/v2"
CURSEFORGE_API = "https://minecraft.curseforge.com/api"

LOADER_NAMES = {"forge": "Forge", "fabric": "Fabric", "neoforge": "NeoForge", "quilt": "Quilt"}
# publish.json uses Modrinth-style slugs; CurseForge slugs differ for some projects
CURSEFORGE_SLUGS = {"rei": "roughly-enough-items", "notenoughitems": "notenoughitems"}
MODRINTH_SLUGS = {"notenoughitems": None}  # not on Modrinth


def log(msg):
    print(msg, flush=True)


def read_secret(env, filename):
    v = os.environ.get(env, "").strip()
    if v:
        return v
    p = os.path.join(ROOT, ".secrets", filename)
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8-sig") as f:
            return f.read().strip()
    return ""


# --------------------------------------------------------------------------- artifacts

def load_artifacts(allow_missing):
    with open(os.path.join(ROOT, "publish.json"), encoding="utf-8") as f:
        data = json.load(f)
    result = []
    for a in data.get("artifacts", []):
        files = sorted(glob.glob(os.path.join(ROOT, a["path"])))
        excludes = a.get("exclude", [])
        files = [p for p in files if not any(fnmatch.fnmatch(os.path.basename(p), e) for e in excludes)]
        if not files:
            msg = "No file matches %s" % a["path"]
            if allow_missing:
                log("WARN: " + msg + " (skipped)")
                continue
            raise SystemExit("ERROR: " + msg)
        if len(files) > 1:
            # keep the newest one (old builds may still be lying around locally)
            files.sort(key=os.path.getmtime)
            log("WARN: %d files match %s, using %s" % (len(files), a["path"], os.path.basename(files[-1])))
        result.append(dict(a, file=files[-1]))
    return result


def label(a):
    loaders = "/".join(LOADER_NAMES.get(l, l) for l in a["loaders"])
    gv = a["game_versions"]
    mc = gv[0] if len(gv) == 1 else "%s-%s" % (gv[0], gv[-1])
    return loaders, mc


# --------------------------------------------------------------------------- changelog

def changelog_for(version):
    p = os.path.join(ROOT, "CHANGELOG.md")
    if os.path.exists(p):
        text = open(p, encoding="utf-8").read()
        m = re.search(r"^## \[?%s\]?[^\n]*\n(.*?)(?=^## |\Z)" % re.escape(version), text, re.M | re.S)
        if m and m.group(1).strip():
            return m.group(1).strip()
    try:
        tags = subprocess.check_output(["git", "tag", "--sort=-creatordate"], cwd=ROOT, text=True, stderr=subprocess.DEVNULL).split()
        prev = next((t for t in tags if t != version), None)
        rng = "%s..%s" % (prev, version) if prev else version
        lines = subprocess.check_output(["git", "log", "--pretty=- %s", rng], cwd=ROOT, text=True, stderr=subprocess.DEVNULL).strip()
        if lines:
            return lines
    except Exception:
        pass
    return "Release %s" % version


# --------------------------------------------------------------------------- Modrinth

class Modrinth:
    def __init__(self, token, project, dry_run):
        self.s = requests.Session()
        self.s.headers.update({"Authorization": token, "User-Agent": USER_AGENT})
        self.project = project
        self.dry = dry_run
        self._ids = {}

    def project_id(self, slug):
        slug = MODRINTH_SLUGS.get(slug, slug)
        if slug is None:
            return None
        if slug not in self._ids:
            r = self.s.get("%s/project/%s" % (MODRINTH_API, slug), timeout=30)
            self._ids[slug] = r.json()["id"] if r.status_code == 200 else None
            if r.status_code != 200:
                log("  WARN: Modrinth project '%s' not found, dependency skipped" % slug)
        return self._ids[slug]

    def existing_versions(self):
        r = self.s.get("%s/project/%s/version" % (MODRINTH_API, self.project), timeout=30)
        r.raise_for_status()
        return {v["version_number"] for v in r.json()}

    def upload(self, a, version, changelog, existing):
        loaders, mc = label(a)
        number = "%s+%s-%s" % (version, "-".join(a["loaders"]), mc)
        if number in existing:
            log("  Modrinth: %s already exists, skipped" % number)
            return
        deps = []
        for kind, dtype in (("required", "required"), ("optional", "optional")):
            for slug in a.get("dependencies", {}).get(kind, []):
                pid = self.project_id(slug)
                if pid:
                    deps.append({"project_id": pid, "dependency_type": dtype})
        data = {
            "project_id": self.project,
            "name": "Inventory ToDo %s (%s %s)" % (version, loaders, mc),
            "version_number": number,
            "changelog": changelog,
            "dependencies": deps,
            "game_versions": a["game_versions"],
            "version_type": "beta" if re.search(r"(alpha|beta|rc|pre)", version) else "release",
            "loaders": a["loaders"],
            "featured": True,
            "file_parts": ["file"],
            "primary_file": "file",
        }
        if self.dry:
            log("  Modrinth (dry-run): %s <- %s deps=%d" % (number, os.path.basename(a["file"]), len(deps)))
            return
        with open(a["file"], "rb") as f:
            files = {"data": (None, json.dumps(data), "application/json"),
                     "file": (os.path.basename(a["file"]), f, "application/java-archive")}
            r = self.s.post("%s/version" % MODRINTH_API, files=files, timeout=300)
        if r.status_code >= 300:
            raise RuntimeError("Modrinth upload failed (%d): %s" % (r.status_code, r.text[:500]))
        log("  Modrinth: uploaded %s" % number)
        # Modrinth requires an environment per version (v3 field; v2 uploads leave it "unknown").
        # The mod is client-side only.
        vid = r.json().get("id")
        if vid:
            e = self.s.patch("https://api.modrinth.com/v3/version/%s" % vid,
                             json={"environment": "client_only"}, timeout=30)
            if e.status_code >= 300:
                log("  WARN: could not set environment on %s (%d)" % (number, e.status_code))


# --------------------------------------------------------------------------- CurseForge

class CurseForge:
    def __init__(self, token, project, dry_run):
        self.s = requests.Session()
        self.s.headers.update({"X-Api-Token": token, "User-Agent": USER_AGENT})
        self.project = project
        self.dry = dry_run
        types = self.s.get(CURSEFORGE_API + "/game/version-types", timeout=60)
        types.raise_for_status()
        versions = self.s.get(CURSEFORGE_API + "/game/versions", timeout=60)
        versions.raise_for_status()
        self.types = {t["id"]: t["slug"] for t in types.json()}
        self.versions = versions.json()

    def _find(self, name, type_pred):
        for v in self.versions:
            if v["name"] == name and type_pred(self.types.get(v["gameVersionTypeID"], "")):
                return v["id"]
        return None

    def version_ids(self, a):
        ids, missing = [], []
        for gv in a["game_versions"]:
            vid = self._find(gv, lambda t: t.startswith("minecraft-"))
            (ids.append(vid) if vid else missing.append("Minecraft " + gv))
        for l in a["loaders"]:
            vid = self._find(LOADER_NAMES.get(l, l), lambda t: t == "modloader")
            (ids.append(vid) if vid else missing.append("loader " + l))
        if a.get("java"):
            vid = self._find("Java %d" % a["java"], lambda t: t == "java")
            if vid:
                ids.append(vid)
        client = self._find("Client", lambda t: t == "environment")
        if client:
            ids.append(client)
        return ids, missing

    def upload(self, a, version, changelog):
        loaders, mc = label(a)
        ids, missing = self.version_ids(a)
        if missing:
            raise RuntimeError("CurseForge does not know: %s" % ", ".join(missing))
        relations = []
        for kind, rtype in (("required", "requiredDependency"), ("optional", "optionalDependency")):
            for slug in a.get("dependencies", {}).get(kind, []):
                relations.append({"slug": CURSEFORGE_SLUGS.get(slug, slug), "type": rtype})
        meta = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": "Inventory ToDo %s (%s %s)" % (version, loaders, mc),
            "gameVersions": ids,
            "releaseType": "beta" if re.search(r"(alpha|beta|rc|pre)", version) else "release",
        }
        if relations:
            meta["relations"] = {"projects": relations}
        if self.dry:
            log("  CurseForge (dry-run): %s <- %s versionIds=%s relations=%d"
                % (meta["displayName"], os.path.basename(a["file"]), ids, len(relations)))
            return
        url = "%s/projects/%s/upload-file" % (CURSEFORGE_API, self.project)
        for attempt in range(2):
            with open(a["file"], "rb") as f:
                files = {"metadata": (None, json.dumps(meta), "application/json"),
                         "file": (os.path.basename(a["file"]), f, "application/java-archive")}
                r = self.s.post(url, files=files, timeout=300)
            if r.status_code < 300:
                log("  CurseForge: uploaded %s (file id %s)" % (meta["displayName"], r.json().get("id")))
                return
            if attempt == 0 and "relations" in meta and "relation" in r.text.lower():
                log("  WARN: CurseForge rejected a relation, retrying without relations: %s" % r.text[:200])
                meta.pop("relations")
                continue
            raise RuntimeError("CurseForge upload failed (%d): %s" % (r.status_code, r.text[:500]))


# --------------------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--only", choices=["modrinth", "curseforge"])
    ap.add_argument("--allow-missing", action="store_true", help="skip publish.json entries without files")
    ap.add_argument("--list-files", action="store_true", help="print the artifact paths and exit")
    ap.add_argument("--print-changelog", action="store_true", help="print the changelog and exit")
    args = ap.parse_args()

    if args.print_changelog:
        print(changelog_for(args.version))
        return
    artifacts = load_artifacts(args.allow_missing)
    if args.list_files:
        for a in artifacts:
            print(os.path.relpath(a["file"], ROOT).replace(os.sep, "/"))
        return
    changelog = changelog_for(args.version)
    log("Version %s, %d artifact(s)" % (args.version, len(artifacts)))
    for a in artifacts:
        loaders, mc = label(a)
        log("  - %s  [%s %s]" % (os.path.relpath(a["file"], ROOT), loaders, mc))

    failures = 0
    if args.only in (None, "modrinth"):
        token, project = read_secret("MODRINTH_TOKEN", "modrinth_token"), os.environ.get("MODRINTH_PROJECT_ID", "").strip()
        if not token or not project:
            log("Modrinth: token or MODRINTH_PROJECT_ID missing, skipped")
        else:
            log("Modrinth:")
            m = Modrinth(token, project, args.dry_run)
            existing = m.existing_versions()
            for a in artifacts:
                try:
                    m.upload(a, args.version, changelog, existing)
                    time.sleep(1)
                except Exception as e:
                    failures += 1
                    log("  ERROR: %s" % e)

    if args.only in (None, "curseforge"):
        token, project = read_secret("CURSEFORGE_TOKEN", "curseforge_token"), os.environ.get("CURSEFORGE_PROJECT_ID", "").strip()
        if not token or not project:
            log("CurseForge: token or CURSEFORGE_PROJECT_ID missing, skipped")
        else:
            log("CurseForge:")
            c = CurseForge(token, project, args.dry_run)
            for a in artifacts:
                try:
                    c.upload(a, args.version, changelog)
                    time.sleep(1)
                except Exception as e:
                    failures += 1
                    log("  ERROR: %s" % e)

    if failures:
        raise SystemExit("%d upload(s) failed" % failures)


if __name__ == "__main__":
    main()
