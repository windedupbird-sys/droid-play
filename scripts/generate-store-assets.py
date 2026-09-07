#!/usr/bin/env python3
"""Generate Play Store marketing assets for Color Tap."""

from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "store-assets"
OUT.mkdir(parents=True, exist_ok=True)

BG = "#1A1A2E"
BG2 = "#16213E"
ACCENT = "#E94560"
GOLD = "#F9A826"
WHITE = "#FFFFFF"
MUTED = "#B8B8D1"
COLORS = ["#E94560", "#16C79A", "#F9A826", "#7B2CBF", "#00B4D8"]


def gradient_bg(img: Image.Image, top: str, bottom: str) -> None:
    draw = ImageDraw.Draw(img)
    w, h = img.size
    tr, tg, tb = int(top[1:3], 16), int(top[3:5], 16), int(top[5:7], 16)
    br, bg, bb = int(bottom[1:3], 16), int(bottom[3:5], 16), int(bottom[5:7], 16)
    for y in range(h):
        ratio = y / max(h - 1, 1)
        r = int(tr + (br - tr) * ratio)
        g = int(tg + (bg - tg) * ratio)
        b = int(tb + (bb - tb) * ratio)
        draw.line([(0, y), (w, y)], fill=(r, g, b))


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_circle(draw: ImageDraw.ImageDraw, x: int, y: int, r: int, color: str) -> None:
    draw.ellipse((x - r, y - r, x + r, y + r), fill=color)


def screenshot_menu() -> Image.Image:
    img = Image.new("RGB", (1080, 1920))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)
    title = load_font(96, True)
    body = load_font(40)
    small = load_font(36, True)

    draw.text((540, 760), "Color Tap", fill=WHITE, font=title, anchor="mm")
    draw.multiline_text(
        (540, 920),
        "Tap the circles before they fade away.\nFast taps score more points!",
        fill=MUTED,
        font=body,
        anchor="mm",
        align="center",
        spacing=12,
    )
    draw.text((540, 1120), "Best: 42", fill=GOLD, font=small, anchor="mm")
    draw.rounded_rectangle((390, 1240, 690, 1360), radius=26, fill=ACCENT)
    draw.text((540, 1300), "Play", fill=WHITE, font=small, anchor="mm")
    return img


def screenshot_gameplay() -> Image.Image:
    img = Image.new("RGB", (1080, 1920))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)
    hud = load_font(42, True)
    draw.text((80, 120), "Score: 18", fill=WHITE, font=hud)
    draw.text((920, 120), "♥♥♥", fill=ACCENT, font=hud)

    positions = [(280, 620, 90), (760, 780, 75), (520, 1050, 85), (180, 1180, 70)]
    for i, (x, y, r) in enumerate(positions):
        draw_circle(draw, x, y, r, COLORS[i % len(COLORS)])
    return img


def screenshot_game_over() -> Image.Image:
    img = Image.new("RGB", (1080, 1920))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)
    title = load_font(88, True)
    score = load_font(56, True)
    small = load_font(40, True)
    btn = load_font(36, True)

    draw.text((540, 760), "Game Over", fill=ACCENT, font=title, anchor="mm")
    draw.text((540, 900), "Score: 31", fill=WHITE, font=score, anchor="mm")
    draw.text((540, 990), "Best: 42", fill=GOLD, font=small, anchor="mm")
    draw.rounded_rectangle((360, 1140, 720, 1260), radius=26, fill=ACCENT)
    draw.text((540, 1200), "Play Again", fill=WHITE, font=btn, anchor="mm")
    return img


def feature_graphic() -> Image.Image:
    img = Image.new("RGB", (1024, 500))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)
    title = load_font(72, True)
    subtitle = load_font(30)

    draw.text((60, 170), "Color Tap", fill=WHITE, font=title)
    draw.text((60, 270), "Fast reflex arcade fun", fill=MUTED, font=subtitle)
    for i, color in enumerate(COLORS[:4]):
        draw_circle(draw, 760 + i * 55, 250, 42, color)
    return img


def main() -> None:
    screenshot_menu().save(OUT / "screenshot-01-menu.png")
    screenshot_gameplay().save(OUT / "screenshot-02-gameplay.png")
    screenshot_game_over().save(OUT / "screenshot-03-game-over.png")
    feature_graphic().save(OUT / "feature-graphic-1024x500.png")
    print(f"Generated assets in {OUT}")


if __name__ == "__main__":
    main()
