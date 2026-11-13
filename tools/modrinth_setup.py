#!/usr/bin/env python3
"""Creates and maintains the Modrinth project page.

  python tools/modrinth_setup.py --create      create the project as a draft (once), set the GitHub variable
  python tools/modrinth_setup.py --update      update summary, body (docs/description.md), links, categories
  python tools/modrinth_setup.py --icon        upload docs/icon.png
  python tools/modrinth_setup.py --gallery     upload docs/screenshots/*.png that are not in the gallery yet
  python tools/modrinth_setup.py --submit      submit the project for review (needs a version + gallery)
  python tools/modrinth_setup.py --status      show the current state
  add --dry-run to print what would be sent

The token is read from MODRINTH_TOKEN or .secrets/modrinth_token.
"""
import argparse
import glob
import json
import os
import subprocess
import sys

import requests

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API = "https://api.modrinth.com/v2"
USER_AGENT = "junjun1z/inventory-todo-setup (https://github.com/junjun1z/inventory-todo)"
REPO = "junjun1z/inventory-todo"

SLUG = "inventory-todo"
TITLE = "Inventory ToDo"
SUMMARY = ("A to-do list next to your inventory: notes and item goals that count up as you collect. "
           "Works in any modpack, with any mods - no required dependencies.")
CATEGORIES = ["utility", "management"]
LINKS = {
    "source_url": "https://github.com/%s" % REPO,
    "issues_url": "https://github.com/%s/issues" % REPO,
    "wiki_url": None,
    "discord_url": None,
}

# Nice titles for known screenshot file names (others use the file name)
GALLERY_TITLES = {
    "inventory-panel": "The ToDo panel next to the inventory: notes and item goals that count up",
    "chest-counting": "Open containers count too: the chest's glass and iron are included",
    "hud": "HUD of unfinished tasks while exploring",
    "item-search": "Search items by name or ID",
    "row-menu": "Right-click a row for more options",
    "recipe-ingredients": "Ctrl+click an item to add its recipe ingredients",
    "jei": "Add items straight from JEI's list",
    "minimized": "Minimized to a small icon with the number of open tasks",
    "settings": "In-game settings",
    "nei-gtnh": "Minecraft 1.7.10 in GT New Horizons, with NEI",
}


def token():
    v = os.environ.get("MODRINTH_TOKEN", "").strip()
    if not v:
        p = os.path.join(ROOT, ".secrets", "modrinth_token")
        if os.path.exists(p):
            v = open(p, encoding="utf-8-sig").read().strip()
    if not v:
        sys.exit("Modrinth token not found (MODRINTH_TOKEN or .secrets/modrinth_token)")
    return v


def session():
    s = requests.Session()
    s.headers.update({"Authorization": token(), "User-Agent": USER_AGENT})
    return s


def body():
    return open(os.path.join(ROOT, "docs", "description.md"), encoding="utf-8").read()


def get_project(s):
    r = s.get("%s/project/%s" % (API, SLUG), timeout=30)
    return r.json() if r.status_code == 200 else None


def check(r, what):
    if r.status_code >= 300:
        sys.exit("%s failed (%d): %s" % (what, r.status_code, r.text[:800]))


def set_github_variable(project_id, dry):
    if dry:
        print("(dry-run) gh variable set MODRINTH_PROJECT_ID=%s" % project_id)
        return
    subprocess.run(["gh", "variable", "set", "MODRINTH_PROJECT_ID", "--body", project_id, "-R", REPO], check=True)
    print("GitHub variable MODRINTH_PROJECT_ID set")


def create(s, dry):
    existing = get_project(s)
    if existing:
        print("Project already exists: id=%s status=%s" % (existing["id"], existing["status"]))
        set_github_variable(existing["id"], dry)
        return
    data = {
        "slug": SLUG,
        "title": TITLE,
        "description": SUMMARY,
        "categories": CATEGORIES,
        "additional_categories": [],
        "client_side": "required",
        "server_side": "unsupported",
        "body": body(),
        "project_type": "mod",
        "initial_versions": [],
        "is_draft": True,
        "license_id": "MIT",
        "donation_urls": [],
    }
    data.update({k: v for k, v in LINKS.items() if v})
    if dry:
        print("(dry-run) POST /project", json.dumps({k: v for k, v in data.items() if k != "body"}, indent=2))
        return
    files = {"data": (None, json.dumps(data), "application/json")}
    icon = os.path.join(ROOT, "docs", "icon.png")
    fh = open(icon, "rb") if os.path.exists(icon) else None
    if fh:
        files["icon"] = ("icon.png", fh, "image/png")
    try:
        r = s.post("%s/project" % API, files=files, timeout=60)
    finally:
        if fh:
            fh.close()
    check(r, "Create project")
    p = r.json()
    print("Created project id=%s slug=%s status=%s" % (p["id"], p["slug"], p["status"]))
    set_github_variable(p["id"], dry)


def update(s, dry):
    p = get_project(s) or sys.exit("Project not found; run --create first")
    data = {"title": TITLE, "description": SUMMARY, "body": body(), "categories": CATEGORIES,
            "client_side": "required", "server_side": "unsupported", "license_id": "MIT"}
    data.update(LINKS)
    if dry:
        print("(dry-run) PATCH /project/%s" % p["id"], sorted(data))
        return
    check(s.patch("%s/project/%s" % (API, p["id"]), json=data, timeout=60), "Update project")
    print("Project page updated")


def icon(s, dry):
    p = get_project(s) or sys.exit("Project not found; run --create first")
    path = os.path.join(ROOT, "docs", "icon.png")
    if dry:
        print("(dry-run) PATCH icon from", path)
        return
    with open(path, "rb") as f:
        r = s.patch("%s/project/%s/icon" % (API, p["id"]), params={"ext": "png"}, data=f.read(),
                    headers={"Content-Type": "image/png"}, timeout=60)
    check(r, "Upload icon")
    print("Icon uploaded")


FEATURED = "01_inventory-panel"


def gallery(s, dry, replace=False):
    p = get_project(s) or sys.exit("Project not found; run --create first")
    if replace:
        for g in p.get("gallery", []):
            if dry:
                print("(dry-run) gallery -", g.get("title"))
                continue
            r = s.delete("%s/project/%s/gallery" % (API, p["id"]), params={"url": g["url"]}, timeout=60)
            check(r, "Gallery delete " + str(g.get("title")))
            print("gallery -", g.get("title"))
        p["gallery"] = []
    have = {g.get("title") for g in p.get("gallery", [])}
    images = sorted(glob.glob(os.path.join(ROOT, "docs", "screenshots", "*.png")),
                    key=lambda x: (FEATURED not in x, x))
    if not images:
        print("No images in docs/screenshots/")
        return
    for i, path in enumerate(images):
        name = os.path.splitext(os.path.basename(path))[0]
        key = name.split("_", 1)[-1] if name[:2].isdigit() else name
        title = GALLERY_TITLES.get(key, key.replace("_", " ").replace("-", " ").capitalize())
        if title in have:
            print("skip (already in gallery):", title)
            continue
        params = {"ext": "png", "featured": "true" if i == 0 and not have and FEATURED in path else "false",
                  "title": title, "ordering": str(i)}
        if dry:
            print("(dry-run) gallery +", os.path.basename(path), params)
            continue
        with open(path, "rb") as f:
            r = s.post("%s/project/%s/gallery" % (API, p["id"]), params=params, data=f.read(),
                       headers={"Content-Type": "image/png"}, timeout=120)
        check(r, "Gallery upload " + path)
        print("gallery +", title)


def submit(s, dry):
    p = get_project(s) or sys.exit("Project not found; run --create first")
    versions = s.get("%s/project/%s/version" % (API, p["id"]), timeout=30).json()
    if not versions:
        sys.exit("No versions uploaded yet - release first, then submit")
    if not p.get("gallery"):
        print("WARNING: the gallery is empty")
    if p["status"] not in ("draft", "rejected"):
        print("Status is already '%s', nothing to submit" % p["status"])
        return
    if dry:
        print("(dry-run) PATCH status=processing (versions: %d)" % len(versions))
        return
    check(s.patch("%s/project/%s" % (API, p["id"]), json={"status": "processing"}, timeout=60), "Submit")
    print("Submitted for review")


def status(s):
    p = get_project(s)
    if not p:
        print("Project '%s' does not exist yet" % SLUG)
        return
    versions = s.get("%s/project/%s/version" % (API, p["id"]), timeout=30).json()
    print("id=%s status=%s versions=%d gallery=%d icon=%s"
          % (p["id"], p["status"], len(versions), len(p.get("gallery", [])), bool(p.get("icon_url"))))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--replace", action="store_true", help="with --gallery: delete the current gallery first")
    for a in ("create", "update", "icon", "gallery", "submit", "status"):
        ap.add_argument("--" + a, action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    s = session()
    if args.create:
        create(s, args.dry_run)
    if args.update:
        update(s, args.dry_run)
    if args.icon:
        icon(s, args.dry_run)
    if args.gallery:
        gallery(s, args.dry_run, args.replace)
    if args.submit:
        submit(s, args.dry_run)
    if args.status or not any(vars(args)[k] for k in ("create", "update", "icon", "gallery", "submit")):
        status(s)


if __name__ == "__main__":
    main()
