#!/usr/bin/env python3
"""Regenerates website raster assets.

Renderer: cairosvg (correct feGaussianBlur / gradients). The previous
ImageMagick MSVG path rendered blurred glow shapes as harsh solid blobs —
that was the "weird images" bug.

Inputs:
- docs/screenshots/*.svg (the real ClickGUI / HUD editor / module views)
- website/assets/img/logo.svg (the Gugugaga Client brand logo)

Run from the repo root:  python3 website/tools/generate_assets.py
(requires: pip install cairosvg pillow)
"""
import os
import sys

import cairosvg
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "website", "assets", "img")
SHOTS = os.path.join(ROOT, "docs", "screenshots")
LOGO = os.path.join(ASSETS, "logo.svg")

ACCENT = (59, 130, 246)
BG_DARK = (16, 16, 20)
TEXT = (242, 242, 242)
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def rasterize_svg(svg_path: str, width: int, bg: str = "#101014") -> Image.Image:
    out = f"/tmp/_gen_{os.path.basename(svg_path)}_{width}.png"
    cairosvg.svg2png(url=svg_path, write_to=out, output_width=width, background_color=bg)
    return Image.open(out).convert("RGB")


def rounded(img: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius=radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def save_shot(img: Image.Image, name: str):
    rounded(img, 16).save(os.path.join(ASSETS, f"{name}.webp"), quality=82, method=6)
    rounded(img, 16).save(os.path.join(ASSETS, f"{name}.png"), optimize=True)
    w = os.path.getsize(os.path.join(ASSETS, name + ".webp")) // 1024
    print(f"  {name}: webp={w} KB @ {img.size[0]}x{img.size[1]} (cairosvg)")


def shoot(name: str, svg: str, width: int = 1600):
    save_shot(rasterize_svg(os.path.join(SHOTS, svg), width), name)


def favicons():
    for size, name in [(16, "favicon-16x16.png"), (32, "favicon-32x32.png"),
                       (180, "apple-touch-icon.png"), (192, "icon-192.png"), (512, "icon-512.png")]:
        cairosvg.svg2png(url=LOGO, write_to=os.path.join(ASSETS, name), output_width=size, output_height=size)
    print("  favicons regenerated from logo.svg")


def social_preview():
    """1200x630 OG card with the brand logo, name and real slogan."""
    w, h = 1200, 630
    img = Image.new("RGB", (w, h), BG_DARK)
    d = ImageDraw.Draw(img)
    for x in range(60, w, 84):
        for y in range(60, h, 84):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(33, 34, 46))
    d.rounded_rectangle([24, 24, w - 24, h - 24], radius=28, outline=(*ACCENT, 255), width=3)

    logo = Image.open("/tmp/_gen_logo.svg_256.png").convert("RGBA")
    img.paste(logo, (80, 122), logo)
    d.text((330, 150), "Gugugaga Client", font=ImageFont.truetype(FONT_BOLD, 84), fill=TEXT)
    d.text((336, 254), "Minecraft Client", font=ImageFont.truetype(FONT_REG, 40), fill=(154, 154, 165))
    d.text((330, 366), "Aerial mace sequence, ClickGUI, HUD editor and", font=ImageFont.truetype(FONT_REG, 34), fill=(200, 202, 214))
    d.text((330, 414), "cloud config sharing for Minecraft 1.21.11.", font=ImageFont.truetype(FONT_REG, 34), fill=(200, 202, 214))
    chip_font = ImageFont.truetype(FONT_BOLD, 26)
    for i, label in enumerate(["v1.2.0", "Fabric", "MC 1.21.11"]):
        tw = d.textlength(label, font=chip_font)
        x0 = 330 + i * 200
        d.rounded_rectangle([x0, 508, x0 + tw + 36, 554], radius=16, fill=(30, 31, 42), outline=(70, 71, 88), width=1)
        d.text((x0 + 18, 518), label, font=chip_font, fill=(*ACCENT, 255))
    img.save(os.path.join(ASSETS, "social-preview.png"), optimize=True)
    img.save(os.path.join(ASSETS, "social-preview.webp"), quality=85, method=6)
    print(f"  social-preview regenerated ({os.path.getsize(os.path.join(ASSETS, 'social-preview.webp')) // 1024} KB webp)")


def main():
    os.makedirs(ASSETS, exist_ok=True)
    print("Rasterizing real client screenshots via cairosvg (correct blur/gradients)...")
    shoot("client-clickgui", "clickgui.svg")
    shoot("client-hud-editor", "hud-editor.svg")
    shoot("client-module-settings", "module.svg")
    print("Brand logo assets...")
    cairosvg.svg2png(url=LOGO, write_to="/tmp/_gen_logo.svg_256.png", output_width=256)
    favicons()
    social_preview()
    print("Done.")


if __name__ == "__main__":
    sys.exit(main())
