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

from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "website", "assets", "img")
SHOTS = os.path.join(ROOT, "docs", "screenshots")

ACCENT = (59, 130, 246)          # ThemeManager: 0xFF3B82F6
ACCENT_LIGHT = (96, 165, 250)
BG_DARK = (16, 16, 20)           # ThemeManager: 0xC0101014
PANEL = (22, 22, 28)             # ThemeManager: 0xE016161C
TEXT = (242, 242, 242)           # ThemeManager: 0xFFF2F2F2

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def rasterize_svg(svg_name: str, width: int) -> Image.Image:
    """Convert one of the real client screenshots to PNG at the given width."""
    out = f"/tmp/_site_{svg_name}_{width}.png"
    subprocess.run(
        ["convert", "-background", "#101014", "-density", "192",
         os.path.join(SHOTS, svg_name), "-resize", f"{width}x", out],
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


def save_shot(img: Image.Image, name: str):
    """Save a screenshot as WebP (primary) and optimized PNG (fallback)."""
    rounded(img, 16).save(os.path.join(ASSETS, f"{name}.webp"), quality=82, method=6)
    rounded(img, 16).save(os.path.join(ASSETS, f"{name}.png"), optimize=True)
    print(f"  {name}: webp={os.path.getsize(os.path.join(ASSETS, name + '.webp')) // 1024} KB "
          f"png={os.path.getsize(os.path.join(ASSETS, name + '.png')) // 1024} KB @ {img.size[0]}x{img.size[1]}")


def shoot(name: str, svg: str, width: int = 1600):
    """Real screenshot as sharp, compressed web assets."""
    save_shot(rasterize_svg(svg, width), name)


def logo_mark(size: int) -> Image.Image:
    """The client logo mark: rounded tile with glow, stylized mace + sparkle."""
    s = size
    scale = s / 512.0

    def r(v):
        return int(round(v * scale))

    hi = Image.new("RGBA", (s, s), (0, 0, 0, 0))

    # Soft outer glow behind the tile
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.rounded_rectangle([r(46), r(46), r(466), r(466)], radius=r(96), fill=(*ACCENT, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(r(36)))
    hi.alpha_composite(glow)

    d = ImageDraw.Draw(hi)
    # Tile with vertical gradient (accent -> deeper blue)
    grad = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gdr = ImageDraw.Draw(grad)
    top, bottom = (74, 144, 250), (37, 99, 235)
    for y in range(r(64), r(448)):
        t = (y - r(64)) / max(1, r(384))
        col = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        gdr.line([(r(64), y), (r(448), y)], fill=(*col, 255))
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle([r(64), r(64), r(448), r(448)], radius=r(88), fill=255)
    hi.paste(grad, (0, 0), mask)

    # Inner highlight edge
    d.rounded_rectangle([r(64), r(64), r(448), r(448)], radius=r(88),
                        outline=(*TEXT, 120), width=max(1, r(6)))
    d.line([(r(110), r(96)), (r(400), r(96))], fill=(*TEXT, 70), width=max(1, r(6)))

    # Stylized mace: shaft + spiked head
    shaft_top = (r(256), r(120))
    shaft_bot = (r(256), r(372))
    d.line([shaft_top, shaft_bot], fill=(*TEXT, 235), width=r(26))
    # Head: rounded diamond
    head = [(r(256), r(96)), (r(352), r(192)), (r(256), r(288)), (r(160), r(192))]
    d.polygon(head, fill=(*TEXT, 250))
    # Studs
    for ox, oy in [(0, -34), (34, 0), (0, 34), (-34, 0)]:
        cx, cy = r(256) + r(ox), r(192) + r(oy)
        d.ellipse([cx - r(15), cy - r(15), cx + r(15), cy + r(15)], fill=(*ACCENT, 255))
    # Center gem
    d.ellipse([r(256) - r(26), r(192) - r(26), r(256) + r(26), r(192) + r(26)], fill=(29, 78, 216, 255))
    d.ellipse([r(256) - r(14), r(192) - r(20), r(256) + r(2), r(192) + r(2)], fill=(147, 197, 253, 255))
    # Pommel
    d.ellipse([r(256) - r(20), r(364), r(256) + r(20), r(404)], fill=(*ACCENT, 255))
    # Sparkle top-right
    d.line([(r(392), r(110)), (r(392), r(170))], fill=(*TEXT, 220), width=r(10))
    d.line([(r(362), r(140)), (r(422), r(140))], fill=(*TEXT, 220), width=r(10))
    return hi


def favicons():
    for size, name in [(16, "favicon-16x16.png"), (32, "favicon-32x32.png"),
                       (180, "apple-touch-icon.png"), (192, "icon-192.png"), (512, "icon-512.png")]:
        logo_mark(size).save(os.path.join(ASSETS, name), optimize=True)
    print("  favicons regenerated")


def social_preview():
    """1200x630 OG card: dark panel, new logo, name, real slogan."""
    w, h = 1200, 630
    img = Image.new("RGB", (w, h), BG_DARK)
    d = ImageDraw.Draw(img)
    for x in range(60, w, 84):
        for y in range(60, h, 84):
            d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(35, 36, 48))
    d.rounded_rectangle([24, 24, w - 24, h - 24], radius=28, outline=(*ACCENT, 255), width=3)

    logo = logo_mark(192)
    img.paste(logo, (86, 130), logo)
    d.text((320, 150), "Gugugaga Client", font=ImageFont.truetype(FONT_BOLD, 84), fill=TEXT)
    d.text((326, 254), "Minecraft Client", font=ImageFont.truetype(FONT_REG, 40), fill=(154, 154, 165))
    d.text((320, 366), "Aerial mace sequence, ClickGUI, HUD editor and", font=ImageFont.truetype(FONT_REG, 34), fill=(200, 202, 214))
    d.text((320, 414), "cloud config sharing for Minecraft 1.21.11.", font=ImageFont.truetype(FONT_REG, 34), fill=(200, 202, 214))
    chip_font = ImageFont.truetype(FONT_BOLD, 26)
    for i, label in enumerate(["v1.2.0", "Fabric", "MC 1.21.11"]):
        tw = d.textlength(label, font=chip_font)
        x0 = 320 + i * 200
        d.rounded_rectangle([x0, 508, x0 + tw + 36, 554], radius=16, fill=(30, 31, 42), outline=(70, 71, 88), width=1)
        d.text((x0 + 18, 518), label, font=chip_font, fill=(*ACCENT, 255))
    img.save(os.path.join(ASSETS, "social-preview.png"), optimize=True)
    img.save(os.path.join(ASSETS, "social-preview.webp"), quality=85, method=6)
    print(f"  social-preview regenerated ({os.path.getsize(os.path.join(ASSETS, 'social-preview.webp')) // 1024} KB webp)")


def main():
    os.makedirs(ASSETS, exist_ok=True)
    print("Rasterizing real client screenshots (2x sharp)...")
    shoot("client-clickgui", "clickgui.svg")
    shoot("client-hud-editor", "hud-editor.svg")
    shoot("client-module-settings", "module.svg")
    print("Branding assets...")
    favicons()
    social_preview()
    print("Done.")


if __name__ == "__main__":
    sys.exit(main())
