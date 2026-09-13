#!/usr/bin/env python3
"""Generate Play Store marketing assets for Color Tap."""

from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "store-assets"
OUT.mkdir(parents=True, exist_ok=True)

BG = "#14142B"
BG2 = "#1F1D36"
ACCENT = "#E94560"
GOLD = "#FFD700"
WHITE = "#FFFFFF"
MUTED = "#CBD5E1"
CYAN = "#00ADB5"
EMERALD = "#38EF7D"
COLORS = ["#E94560", "#00ADB5", "#38EF7D", "#FFD700", "#9D4EDD", "#FF007F"]


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


def draw_target(draw: ImageDraw.ImageDraw, x: int, y: int, r: int, fill: str, border: str, symbol: str, font: ImageFont.ImageFont) -> None:
    draw.ellipse((x - r, y - r, x + r, y + r), fill=fill, outline=border, width=5)
    draw.text((x, y), symbol, fill=WHITE, font=font, anchor="mm")


def play_store_icon() -> Image.Image:
    img = Image.new("RGB", (512, 512))
    gradient_bg(img, "#14142B", "#1F1D36")
    draw = ImageDraw.Draw(img)
    f_sym = load_font(130, True)

    # Concentric outer rings
    draw.ellipse((56, 56, 456, 456), outline="#33FFFFFF", width=4)
    draw.ellipse((96, 96, 416, 416), fill="#E94560", outline="#FFD700", width=8)
    draw.ellipse((166, 166, 346, 346), fill="#14142B")
    draw.text((256, 256), "★", fill="#FFD700", font=f_sym, anchor="mm")
    return img


def screenshot_menu() -> Image.Image:
    img = Image.new("RGB", (1080, 1920))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)
    title = load_font(96, True)
    body = load_font(38)
    small = load_font(34, True)
    sub = load_font(28)

    draw.text((540, 520), "COLOR TAP", fill=WHITE, font=title, anchor="mm")
    draw.multiline_text(
        (540, 640),
        "Tap targets before they fade!\nBuild combos for massive score multipliers.",
        fill=MUTED,
        font=body,
        anchor="mm",
        align="center",
        spacing=12,
    )

    # Mode selector pill
    draw.rounded_rectangle((220, 750, 860, 840), radius=22, fill="#252440")
    draw.rounded_rectangle((225, 755, 435, 835), radius=18, fill=ACCENT)
    draw.text((330, 795), "Classic", fill=WHITE, font=small, anchor="mm")
    draw.text((540, 795), "Time Attack", fill=MUTED, font=small, anchor="mm")
    draw.text((750, 795), "Zen", fill=MUTED, font=small, anchor="mm")

    # Legend Card
    draw.rounded_rectangle((140, 890, 940, 1260), radius=26, fill="#1D1C34", outline="#302F52", width=3)
    f_item = load_font(32)
    f_icon = load_font(36, True)

    legend_items = [
        ("★", CYAN, "Standard: quick tap gives +3 pts"),
        ("✦", GOLD, "Golden: triple base bonus points"),
        ("♥", "#FF4D6D", "Heart: restores 1 life"),
        ("❄", "#64DFDF", "Freeze: slows down time & targets"),
        ("✖", "#FF3366", "Bomb: DO NOT TAP! (-1 life)"),
    ]
    y_start = 930
    for sym, col, text in legend_items:
        draw.ellipse((180, y_start, 230, y_start + 50), fill=col)
        draw.text((205, y_start + 25), sym, fill=WHITE, font=f_icon, anchor="mm")
        draw.text((260, y_start + 25), text, fill=WHITE, font=f_item, anchor="lm")
        y_start += 62

    draw.text((540, 1340), "CLASSIC BEST: 128", fill=GOLD, font=small, anchor="mm")
    draw.rounded_rectangle((320, 1420, 760, 1530), radius=28, fill=ACCENT)
    draw.text((540, 1475), "Play Classic", fill=WHITE, font=load_font(42, True), anchor="mm")

    # Navigation cards
    draw.rounded_rectangle((140, 1600, 380, 1720), radius=20, fill="#252440")
    draw.text((260, 1640), "🏆", font=f_icon, anchor="mm")
    draw.text((260, 1685), "Ranks", fill=WHITE, font=sub, anchor="mm")

    draw.rounded_rectangle((420, 1600, 660, 1720), radius=20, fill="#252440")
    draw.text((540, 1640), "🏅", font=f_icon, anchor="mm")
    draw.text((540, 1685), "Badges", fill=WHITE, font=sub, anchor="mm")

    draw.rounded_rectangle((700, 1600, 940, 1720), radius=20, fill="#252440")
    draw.text((820, 1640), "🎨", font=f_icon, anchor="mm")
    draw.text((820, 1685), "Themes", fill=WHITE, font=sub, anchor="mm")

    return img


def screenshot_gameplay() -> Image.Image:
    img = Image.new("RGB", (1080, 1920))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)

    hud_score = load_font(58, True)
    hud_multi = load_font(40, True)
    hud_combo = load_font(30, True)
    hud_hearts = load_font(46, True)
    f_sym = load_font(44, True)

    # HUD
    draw.text((80, 100), "84", fill=WHITE, font=hud_score)
    draw.text((165, 110), "3x", fill=GOLD, font=hud_multi)
    draw.text((80, 165), "COMBO 14", fill=CYAN, font=hud_combo)
    draw.text((820, 110), "♥♥♥♡♡", fill="#FF4D6D", font=hud_hearts)

    # Freeze & Fever badges
    draw.rounded_rectangle((80, 215, 340, 265), radius=10, fill="#20404C")
    draw.text((210, 240), "❄ FROZEN 3s", fill="#64DFDF", font=load_font(26, True), anchor="mm")

    # Target circles
    targets = [
        (320, 680, 85, "#E94560", WHITE, "★"),
        (760, 790, 85, GOLD, "#FFFF00", "✦"),
        (480, 1100, 90, "#64DFDF", "#E0F7FA", "❄"),
        (220, 1340, 85, "#222222", "#FF1744", "✖"),
        (820, 1280, 85, "#38EF7D", WHITE, "★"),
    ]
    for x, y, r, fill, border, sym in targets:
        draw_target(draw, x, y, r, fill, border, sym, f_sym)

    # Popups
    draw.text((760, 710), "+9 (3x)", fill=GOLD, font=load_font(34, True), anchor="mm")
    draw.text((480, 1010), "+6 ❄ FREEZE", fill="#64DFDF", font=load_font(32, True), anchor="mm")

    return img


def screenshot_game_over() -> Image.Image:
    img = Image.new("RGB", (1080, 1920))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)

    title = load_font(84, True)
    score_f = load_font(90, True)
    sub = load_font(34, True)
    row_f = load_font(32)

    draw.text((540, 480), "GAME OVER", fill="#FF4D6D", font=title, anchor="mm")
    draw.text((540, 560), "CLASSIC MODE", fill="#90A4AE", font=load_font(28, True), anchor="mm")
    draw.text((540, 680), "128", fill=WHITE, font=score_f, anchor="mm")
    draw.text((540, 770), "NEW HIGH SCORE!", fill=GOLD, font=sub, anchor="mm")

    # Achievement unlocked card
    draw.rounded_rectangle((160, 840, 920, 950), radius=20, fill="#3D3200", outline=GOLD, width=3)
    draw.text((540, 875), "🎖 ACHIEVEMENT UNLOCKED!", fill=GOLD, font=load_font(28, True), anchor="mm")
    draw.text((540, 915), "⚡ Combo Master: Reach a 20x combo streak", fill=WHITE, font=load_font(24), anchor="mm")

    # Stats card
    draw.rounded_rectangle((160, 990, 920, 1420), radius=22, fill="#1D1C34", outline="#302F52", width=3)
    stats = [
        ("Total Taps", "68"),
        ("Perfect Taps (3 pts)", "34"),
        ("Highest Combo", "22x"),
        ("Freeze Hits", "5"),
        ("Armor Broken", "3"),
        ("Missed Targets", "3"),
    ]
    y_pos = 1040
    for label, val in stats:
        draw.text((210, y_pos), label, fill=MUTED, font=row_f, anchor="lm")
        draw.text((870, y_pos), val, fill=WHITE, font=load_font(32, True), anchor="rm")
        y_pos += 60

    draw.rounded_rectangle((280, 1480, 800, 1590), radius=28, fill=ACCENT)
    draw.text((540, 1535), "Play Again", fill=WHITE, font=load_font(38, True), anchor="mm")

    draw.rounded_rectangle((280, 1620, 800, 1720), radius=28, outline=WHITE, width=2)
    draw.text((540, 1670), "Main Menu", fill=WHITE, font=load_font(34, True), anchor="mm")

    return img


def feature_graphic() -> Image.Image:
    img = Image.new("RGB", (1024, 500))
    gradient_bg(img, BG, BG2)
    draw = ImageDraw.Draw(img)

    title = load_font(84, True)
    subtitle = load_font(34)
    tagline = load_font(26)
    f_sym = load_font(32, True)

    draw.text((72, 130), "Color Tap", fill=WHITE, font=title)
    draw.text((72, 230), "Fast-reflex arcade tap action", fill=MUTED, font=subtitle)
    draw.text((72, 290), "Combos • 3 Modes • Power-ups • Achievements", fill=GOLD, font=tagline)

    draw.rounded_rectangle((72, 350, 310, 415), radius=22, fill=ACCENT)
    draw.text((191, 382), "Play Free", fill=WHITE, font=load_font(30, True), anchor="mm")

    # Illustrated circles on right
    circle_specs = [
        (710, 150, 58, "#E94560", WHITE, "★"),
        (860, 120, 48, GOLD, "#FFFF00", "✦"),
        (930, 240, 54, "#64DFDF", "#E0F7FA", "❄"),
        (780, 290, 44, "#FF4D6D", WHITE, "♥"),
        (880, 370, 52, "#38EF7D", WHITE, "★"),
        (650, 310, 40, CYAN, WHITE, "★"),
    ]
    for x, y, r, fill, border, sym in circle_specs:
        draw_target(draw, x, y, r, fill, border, sym, f_sym)

    return img


def main() -> None:
    play_store_icon().save(OUT / "play-store-icon-512.png")
    screenshot_menu().save(OUT / "screenshot-01-menu.png")
    screenshot_gameplay().save(OUT / "screenshot-02-gameplay.png")
    screenshot_game_over().save(OUT / "screenshot-03-game-over.png")
    feature_graphic().save(OUT / "feature-graphic-1024x500.png")
    print(f"Generated assets in {OUT}")


if __name__ == "__main__":
    main()
