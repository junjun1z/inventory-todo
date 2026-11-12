"""Rebuilds the "Supported versions" table from publish.json.

Replaces the text between <!-- support-table:start --> and <!-- support-table:end -->
in docs/description.md, README.md and README.ja.md.

python tools/gen_support_table.py
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILES = ["docs/description.md", "README.md", "README.ja.md"]
LOADER_NAMES = {"forge": "Forge", "fabric": "Fabric", "neoforge": "NeoForge", "quilt": "Quilt"}
ORDER = ["forge", "neoforge", "fabric", "quilt"]


def version_key(v):
    return [int(p) if p.isdigit() else 0 for p in re.split(r"[.\-]", v)]


def table(header_mc, header_loaders):
    with open(os.path.join(ROOT, "publish.json"), encoding="utf-8") as f:
        data = json.load(f)
    by_mc = {}
    for a in data.get("artifacts", []):
        for gv in a["game_versions"]:
            by_mc.setdefault(gv, set()).update(a["loaders"])
    lines = ["| %s | %s |" % (header_mc, header_loaders), "| --- | --- |"]
    for mc in sorted(by_mc, key=version_key, reverse=True):
        loaders = sorted(by_mc[mc], key=lambda l: ORDER.index(l) if l in ORDER else 99)
        lines.append("| %s | %s |" % (mc, ", ".join(LOADER_NAMES.get(l, l) for l in loaders)))
    return "\n".join(lines)


def main():
    for rel in FILES:
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            continue
        text = open(p, encoding="utf-8").read()
        ja = rel.endswith(".ja.md")
        t = table("Minecraft", "ローダー" if ja else "Loaders")
        new = re.sub(r"(<!-- support-table:start -->\n).*?(\n<!-- support-table:end -->)",
                     lambda m: m.group(1) + t + m.group(2), text, flags=re.S)
        if new != text:
            open(p, "w", encoding="utf-8", newline="\n").write(new)
            print("updated", rel)


if __name__ == "__main__":
    main()
