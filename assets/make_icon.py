#!/usr/bin/env python3
"""Ve lai icon Benchmark Performance tu dau, khong dung anh AI.

Nguon that duy nhat: diem pulse trong POINTS ben duoi + 3 mau phang.
Chay: python3 assets/make_icon.py
Ra: mipmap mdpi..xxxhdpi (ic_launcher + ic_launcher_round),
mipmap-xxxhdpi/ic_launcher_foreground.png (adaptive, 432x432),
va icon-512.png o assets/ de xem truoc.
"""
from PIL import Image, ImageDraw

BG = (11, 11, 12)        # #0B0B0C
GRID = (26, 26, 28)      # #1A1A1C
ACCENT = (255, 184, 0)   # #FFB800
# Diem pulse tren canvas 512, ve tay — giong het assets/icon.svg.
POINTS = [(72, 300), (170, 300), (210, 300), (250, 180),
          (285, 380), (320, 240), (345, 300), (440, 300)]

def draw(size: int) -> Image.Image:
    img = Image.new("RGB", (size, size), BG)
    d = ImageDraw.Draw(img)
    s = size / 512.0
    for pos in (128, 256, 384):
        p = int(pos * s)
        d.line([(p, 0), (p, size)], fill=GRID, width=max(1, int(2 * s)))
        d.line([(0, p), (size, p)], fill=GRID, width=max(1, int(2 * s)))
    pts = [(int(x * s), int(y * s)) for x, y in POINTS]
    d.line(pts, fill=ACCENT, width=max(2, int(26 * s)), joint="curve")
    cx, cy = pts[-1]
    r = max(2, int(14 * s))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ACCENT)
    return img

def round_icon(size: int) -> Image.Image:
    img = draw(size).convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size, size], fill=255)
    img.putalpha(mask)
    return img

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
base = __import__("pathlib").Path(__file__).resolve().parent.parent
for name, size in DENSITIES.items():
    out = base / f"android/app/src/main/res/mipmap-{name}"
    out.mkdir(parents=True, exist_ok=True)
    draw(size).save(out / "ic_launcher.png")
    round_icon(size).save(out / "ic_launcher_round.png")
# Adaptive foreground: 432x432, pulse nam trong vung an toan 66dp giua.
fg = draw(432)
fg.save(base / "android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png")
draw(512).save(base / "assets/icon-512.png")
print("icon xong: 5 densities + foreground 432 + assets/icon-512.png")
