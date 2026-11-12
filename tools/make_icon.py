"""Draws the Inventory ToDo icon as 32x32 pixel art and scales it up (nearest neighbour).

python tools/make_icon.py  ->  docs/icon.png (512), docs/icon_128.png, docs/icon_64.png
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 32x32 pixel art, assembled row by row. Each character is one pixel.
PAPER = [  # 15 px wide, rows 6..25
    "ppppppppppppppp",
    "pkkkkpppppppppp",
    "pkggkpLLLLLLLLp",
    "pkggkpppppppppp",
    "pkkkkpLLLLLpppp",
    "ppppppppppppppp",
    "pkkkkpppppppppp",
    "pkggkpLLLLLLLLp",
    "pkggkpppppppppp",
    "pkkkkpLLLLLLppp",
    "ppppppppppppppp",
    "pkkkkpppppppppp",
    "pkeekpLLLLLLLpp",
    "pkeekpppppppppp",
    "pkkkkpLLLpppppp",
    "ppppppppppppppp",
    "ppppppppppppppp",
    "ppppppppppppppp",
]
INGOT = [  # 11 px wide, drawn over the bottom right (rows 20..27, from x 17)
    "....YYYYYY.",
    "...YyyyyyOY",
    "..YyyyyyOOY",
    ".YyyyyyyyyY",
    "YyyyyyyyyyY",
    "YyyyyyyyyY.",
    "YYYYYYYYYY.",
]


def build_art():
    rows = []
    for y in range(32):
        if y in (0, 31):
            rows.append("." * 32)
            continue
        if y in (1, 30):
            rows.append(".." + "B" * 28 + "..")
            continue
        r = ["D"] * 32
        r[0] = r[31] = "."
        r[1] = r[30] = "B"
        if 4 <= y <= 26:
            # clipboard board: x 6..24
            for x in range(6, 25):
                r[x] = "W" if (x in (6, 24) or y in (4, 26)) else "w"
            if 6 <= y <= 23:
                for i, ch in enumerate(PAPER[y - 6]):
                    r[8 + i] = ch
        # clip on top
        if y == 3:
            for x in range(11, 20):
                r[x] = "M"
        if y == 4:
            r[11] = r[19] = "M"
            for x in range(12, 19):
                r[x] = "m"
        if y == 5:
            for x in range(11, 20):
                r[x] = "M"
        if 20 <= y <= 26:
            for i, ch in enumerate(INGOT[y - 20]):
                if ch != ".":
                    r[17 + i] = ch
        rows.append("".join(r))
    return rows


ART = build_art()

COLORS = {
    ".": (0, 0, 0, 0),
    "B": (90, 90, 130, 255),      # outer border (panel border colour)
    "D": (32, 32, 52, 255),       # background (panel colour)
    "W": (92, 58, 30, 255),       # clipboard board dark
    "w": (140, 92, 48, 255),      # clipboard board light
    "M": (120, 120, 132, 255),    # clip dark
    "m": (190, 190, 200, 255),    # clip light
    "p": (240, 232, 206, 255),    # paper
    "k": (70, 70, 80, 255),       # checkbox frame
    "g": (60, 176, 67, 255),      # checked
    "e": (230, 230, 220, 255),    # unchecked (empty)
    "L": (130, 120, 105, 255),    # text line
    "Y": (150, 100, 10, 255),     # ingot outline
    "y": (250, 200, 40, 255),     # ingot (gold)
    "O": (255, 240, 150, 255),    # ingot highlight
}


def main():
    assert len(ART) == 32 and all(len(r) == 32 for r in ART), [len(r) for r in ART]
    img = Image.new("RGBA", (32, 32))
    for y, row in enumerate(ART):
        for x, ch in enumerate(row):
            img.putpixel((x, y), COLORS[ch])
    out = os.path.join(ROOT, "docs")
    os.makedirs(out, exist_ok=True)
    img.resize((512, 512), Image.NEAREST).save(os.path.join(out, "icon.png"))
    img.resize((128, 128), Image.NEAREST).save(os.path.join(out, "icon_128.png"))
    img.resize((64, 64), Image.NEAREST).save(os.path.join(out, "icon_64.png"))
    print("written to", out)


if __name__ == "__main__":
    main()
