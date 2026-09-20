#!/usr/bin/env python3
"""Regenerates website raster assets from the client's real SVG screenshots.

Derives everything from the actual client branding:
- Accent color: 0xFF3B82F6 (ThemeManager accent)
- Background: #101014 (ThemeManager background)
- Screenshots: docs/screenshots/*.svg (the real ClickGUI / HUD editor / module views)

Run from the repo root:  python3 website/tools/generate_assets.py
"""
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "website", "assets", "img")
SHOTS = os.path.join(ROOT, "docs", "screenshots")

ACCENT = (59, 130, 246)          # ThemeManager: 0xFF3B82F6
BG_DARK = (16, 16, 20)           # ThemeManager: 0xC0101014
PANEL = (22, 22, 28)             # ThemeManager: 0xE016161C
TEXT = (242, 242, 242)           # ThemeManager: 0xFFF2F2F2

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def rasterize_svg(svg_name: str, width: int) -> Image.Image:
    """Convert one of the real client screenshots to PNG at the given width."""
    out = f"/tmp/_site_{svg_name}.png"
    subprocess.run(
        ["convert", "-background", "#101014", os.path.join(SHOTS, svg_name),
         "-resize", f"{width}x", out],
        check=True, cwd=ROOT,
    )
    return Image.open(out).convert("RGB")


def rounded(img: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius=radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def shoot(name: str, svg: str, width: int = 1280):
    """Real screenshot as a web-ready rounded PNG."""
    img = rasterize_svg(svg, width)
    rounded(img, 14).save(os.path.join(ASSETS, f"{name}.png"), optimize=True)
    print(f"  {name}.png  {img.size[0]}x{img.size[1]}")


def logo_mark(size: int) -> Image.Image:
    """The client logo mark: rounded tile + stylized mace head."""
    s = size
    scale = s / 128.0
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def r(v):
        return int(round(v * scale))

    d.rounded_rectangle([r(6), r(6), r(122), r(122)], radius=r(24), fill=(*ACCENT, 255))
    d.rounded_rectangle([r(6), r(6), r(122), r(122)], radius=r(24), outline=(*TEXT, 90), width=max(1, r(2)))
    # Mace head: diamond of studs
    cx, cy = r(64), r(58)
    d.polygon([(cx, cy - r(30)), (cx + r(30), cy), (cx, cy + r(30)), (cx - r(30), cy)], fill=(*TEXT, 255))
    for ox, oy in [(0, -18), (18, 0), (0, 18), (-18, 0)]:
        d.ellipse([cx + r(ox) - r(7), cy + r(oy) - r(7), cx + r(ox) + r(7), cy + r(oy) + r(7)], fill=(*ACCENT, 255))
    # Handle
    d.rounded_rectangle([cx - r(6), cy + r(18), cx + r(6), cy + r(44)], radius=r(4), fill=(*TEXT, 235))
    return img


def favicons():
    mark = logo_mark(512)
    for size, name in [(16, "favicon-16x16.png"), (32, "favicon-32x32.png"), (180, "apple-touch-icon.png"), (192, "icon-192.png"), (512, "icon-512.png")]:
        mark.resize((size, size), Image.LANCZOS).save(os.path.join(ASSETS, name), optimize=True)
    print("  favicons done")


def social_preview():
    """1200x630 OG card: dark panel, logo, name, real slogan."""
    w, h = 1200, 630
    img = Image.new("RGB", (w, h), BG_DARK)
    d = ImageDraw.Draw(img)
    # Subtle grid of accent dots (matches the client's bordered accent style)
    for x in range(60, w, 84):
        for y in range(60, h, 84):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(35, 36, 48))
    # Accent frame
    d.rounded_rectangle([24, 24, w - 24, h - 24], radius=28, outline=(*ACCENT, 255), width=3)
    # Logo + name
    logo = logo_mark(168)
    img.paste(logo, (96, 150), logo)
    name_font = ImageFont.truetype(FONT_BOLD, 84)
    d.text((300, 158), "Gugugaga Client", font=name_font, fill=TEXT)
    d.text((306, 262), "Minecraft Client", font=ImageFont.truetype(FONT_REG, 40), fill=(154, 154, 165))
    # Slogan (the client's real description, shortened)
    slogan_font = ImageFont.truetype(FONT_REG, 34)
    d.text((300, 372), "Aerial mace sequence, ClickGUI, HUD editor and", font=slogan_font, fill=(200, 202, 214))
    d.text((300, 420), "cloud config sharing for Minecraft 1.21.11.", font=slogan_font, fill=(200, 202, 214))
    # Footer chips
    chip_font = ImageFont.truetype(FONT_BOLD, 26)
    for i, label in enumerate(["v1.2.0", "Fabric", "MC 1.21.11"]):
        tw = d.textlength(label, font=chip_font)
        x0 = 300 + i * 200
        d.rounded_rectangle([x0, 510, x0 + tw + 36, 556], radius=16, fill=(30, 31, 42), outline=(70, 71, 88), width=1)
        d.text((x0 + 18, 520), label, font=chip_font, fill=(*ACCENT, 255))
    img.save(os.path.join(ASSETS, "social-preview.png"), optimize=True)
    print("  social-preview.png 1200x630")


def main():
    os.makedirs(ASSETS, exist_ok=True)
    print("Rasterizing real client screenshots...")
    shoot("client-clickgui", "clickgui.svg")
    shoot("client-hud-editor", "hud-editor.svg")
    shoot("client-module-settings", "module.svg")
    print("Branding assets...")
    favicons()
    social_preview()
    print("Done.")


if __name__ == "__main__":
    sys.exit(main())
