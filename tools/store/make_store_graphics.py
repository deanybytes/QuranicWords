#!/usr/bin/env python3
"""Builds the Google Play store graphics from real app screenshots.

    python3 tools/store/make_store_graphics.py [--lang en|bn]

Inputs : store/raw-screens/<lang>/*.png   (1080x2424 captures from the emulator, demo status bar)
Outputs: store/graphics/icon-512.png              512x512 32-bit PNG (Play app icon)
         store/graphics/feature-graphic.png       1024x500 (Play feature graphic)
         store/graphics/phone/<lang>/NN-*.png     1080x1920 (9:16) annotated phone screenshots

Arabic is shaped with Pillow's raqm layout; captions use Noto Sans (+ Noto Sans Bengali)."""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "store" / "raw-screens"
OUT = ROOT / "store" / "graphics"
NOTO = Path("/usr/share/fonts/truetype/noto")

GREEN_DARK = (3, 40, 28)
GREEN = (5, 56, 39)
GREEN_MID = (10, 84, 58)
GOLD = (235, 201, 113)
IVORY = (255, 248, 231)

W, H = 1080, 1920

# (file name, headline, subline, raw screens) - 8 slides covering every tab and feature.
SLIDES = {
    "en": [
        ("01-learn-the-quran", "Understand the Qur'an,\nword by word",
         "3,833 words · 97% of the Qur'an · 8 languages", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "Every word in its ayah",
         "Only the taught word is highlighted – with its exact meaning", ["teach_senses"]),
        ("03-learn-by-doing", "Learn by doing",
         "Quizzes, matching, fill-in & tap-the-word", ["quiz", "matching"]),
        ("04-home-screen-widgets", "Home-screen widgets",
         "Word of the moment, streak, goal & quests at a glance", ["widgets"]),
        ("05-stay-motivated", "Stay motivated",
         "XP, levels, streaks, daily quests, combos & hearts", ["summary"]),
        ("06-track-progress", "Track your progress",
         "Daily Review, word strength & 27 achievements", ["progress", "achievements"]),
        ("07-quran-fonts", "7 Qur'an fonts",
         "Madinah Mushaf, Noorani, Hafezi, Amiri & more", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "Beautiful in dark mode",
         "100% offline · ad-free · free forever", ["dark_teach", "about"]),
    ],
    "bn": [
        ("01-learn-the-quran", "শব্দে শব্দে\nকুরআন বুঝুন",
         "৩,৮৩৩টি শব্দ · কুরআনের ৯৭% · ৮টি ভাষা", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "প্রতিটি শব্দ তার আয়াতে",
         "শুধু শেখানো শব্দটি হাইলাইট – হুবহু অর্থসহ", ["teach_senses"]),
        ("03-learn-by-doing", "অনুশীলনে শিখুন",
         "কুইজ, মিলকরণ, শূন্যস্থান ও শব্দ চিহ্নিতকরণ", ["quiz", "matching"]),
        ("04-home-screen-widgets", "হোম স্ক্রিন উইজেট",
         "আজকের শব্দ, ধারাবাহিকতা, লক্ষ্য ও কোয়েস্ট এক নজরে", ["widgets"]),
        ("05-stay-motivated", "উৎসাহ ধরে রাখুন",
         "XP, লেভেল, ধারাবাহিকতা, দৈনিক কোয়েস্ট ও কম্বো", ["summary"]),
        ("06-track-progress", "অগ্রগতি দেখুন",
         "দৈনিক রিভিশন, স্মৃতির শক্তি ও ২৭টি অর্জন", ["progress", "achievements"]),
        ("07-quran-fonts", "৭টি কুরআনি ফন্ট",
         "মদিনা মুসহাফ, নূরানী, হাফেজী, আমিরী ও আরও", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "ডার্ক মোডেও সুন্দর",
         "১০০% অফলাইন · বিজ্ঞাপনমুক্ত · সম্পূর্ণ ফ্রি", ["dark_teach", "about"]),
    ],
}


def font(size, bold=False, lang="en"):
    if lang == "bn":
        return ImageFont.truetype(str(NOTO / ("NotoSansBengali-Bold.ttf" if bold else "NotoSansBengali-Regular.ttf")), size)
    return ImageFont.truetype(str(NOTO / ("NotoSans-Bold.ttf" if bold else "NotoSans-Regular.ttf")), size)


def background(w, h):
    """Deep-green vertical gradient with a faint gold eight-point-star lattice."""
    img = Image.new("RGB", (w, h), GREEN)
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        c = tuple(int(GREEN_DARK[i] * (1 - t) + GREEN_MID[i] * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    step = 120
    for y in range(-step, h + step, step):
        for x in range(-step, w + step, step):
            ox = x + (step // 2 if (y // step) % 2 else 0)
            r = 26
            d.regular_polygon((ox, y, r), 4, rotation=0, outline=GOLD + (34,), width=2)
            d.regular_polygon((ox, y, r), 4, rotation=45, outline=GOLD + (34,), width=2)
    img.paste(layer, (0, 0), layer)
    return img


def phone(raw: Image.Image, width: int) -> Image.Image:
    """A screenshot as a rounded 'device' with a thin gold rim and a soft shadow."""
    h = int(raw.height * width / raw.width)
    shot = raw.convert("RGB").resize((width, h), Image.LANCZOS)
    radius = int(width * 0.07)
    rim = 6
    frame = Image.new("RGBA", (width + 2 * rim, h + 2 * rim), (0, 0, 0, 0))
    mask = Image.new("L", (width, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, h - 1), radius, fill=255)
    fd = ImageDraw.Draw(frame)
    fd.rounded_rectangle((0, 0, width + 2 * rim - 1, h + 2 * rim - 1), radius + rim, fill=(20, 20, 20, 255),
                         outline=GOLD + (255,), width=3)
    frame.paste(shot, (rim, rim), mask)
    shadow = Image.new("RGBA", (frame.width + 80, frame.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((40, 50, frame.width + 40, frame.height + 50), radius + rim,
                                             fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    shadow.paste(frame, (40, 40), frame)
    return shadow


def _runs(text, fnt, lang):
    """Split text into (substring, font) runs: Bengali letters in the Bengali font, anything
    else (Latin, "·", "%") in Noto Sans, which the Bengali font lacks."""
    if lang != "bn":
        return [(text, fnt)]
    latin = ImageFont.truetype(str(NOTO / ("NotoSans-Bold.ttf" if "Bold" in fnt.path else "NotoSans-Regular.ttf")), fnt.size)
    runs, cur, cur_bn = [], "", None
    for ch in text:
        is_bn = "\u0980" <= ch <= "\u09ff" or (ch == " " and cur_bn)
        if cur_bn is None or is_bn == cur_bn:
            cur += ch
        else:
            runs.append((cur, fnt if cur_bn else latin))
            cur = ch
        cur_bn = is_bn
    if cur:
        runs.append((cur, fnt if cur_bn else latin))
    return runs


def draw_centered(d, text, y, fnt, fill, spacing=10, lang="en"):
    for line in text.split("\n"):
        runs = _runs(line, fnt, lang)
        widths = [d.textlength(t, font=f) for t, f in runs]
        x = (W - sum(widths)) / 2
        for (t, f), w in zip(runs, widths):
            d.text((x, y), t, font=f, fill=fill)
            x += w
        bbox = d.textbbox((0, 0), line, font=fnt)
        y += (bbox[3] - bbox[1]) + spacing + int(fnt.size * 0.18)
    return y


def slide(lang, name, headline, subline, screens):
    img = background(W, H).convert("RGBA")
    d = ImageDraw.Draw(img)
    y = draw_centered(d, headline, 92, font(70 if "\n" in headline else 76, True, lang), IVORY, lang=lang)
    y = draw_centered(d, subline, y + 14, font(36, False, lang), GOLD, lang=lang)
    top = max(y + 40, 380)
    raws = [Image.open(RAW / lang / f"{s}.png") for s in screens]
    if len(raws) == 1:
        width = min(700, int((H - top - 30) * 1080 / 2424))
        p = phone(raws[0], width)
        img.alpha_composite(p, ((W - p.width) // 2, top - 40))
    else:
        # Two phones, slightly overlapping and staggered, as large as the canvas allows.
        width = min(560, int((H - top - 150) * 1080 / 2424))
        ps = [phone(r, width) for r in raws]
        img.alpha_composite(ps[0], (40 - 40, top - 40))
        img.alpha_composite(ps[1], (W - 40 - width - 40 - 12, top - 40 + 130))
    out = OUT / "phone" / lang / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(out, optimize=True)
    return out


def icon():
    """Play icon: the launcher's own layers (emblem foreground on #053827), full-bleed square -
    Google Play applies the rounded mask itself."""
    fg = Image.open(ROOT / "app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png").convert("RGBA")
    img = Image.new("RGBA", fg.size, (5, 56, 39, 255))
    img.alpha_composite(fg)
    img = img.resize((512, 512), Image.LANCZOS)
    out = OUT / "icon-512.png"
    img.save(out, optimize=True)
    return out


def feature_graphic():
    w, h = 1024, 500
    img = background(w, h).convert("RGBA")
    d = ImageDraw.Draw(img)
    logo = Image.open(ROOT / "icons/icon-512.png").convert("RGBA").resize((300, 300), Image.LANCZOS)
    img.alpha_composite(logo, (60, 100))
    d.text((400, 112), "QuranicWords", font=font(76, True), fill=IVORY)
    d.text((402, 214), "Learn the words of the Qur'an,", font=font(34), fill=GOLD)
    d.text((402, 258), "word by word \u2013 with every ayah.", font=font(34), fill=GOLD)
    # Feature line (no scripture is used as decoration; verses appear only inside real screens).
    chips = ["3,833 words", "8 languages", "100% offline"]
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    x = 402
    f = font(24, True)
    for c in chips:
        tw = od.textbbox((0, 0), c, font=f)[2]
        od.rounded_rectangle((x, 336, x + tw + 32, 380), 22, fill=(255, 248, 231, 28), outline=GOLD + (255,), width=2)
        od.text((x + 16, 341), c, font=f, fill=IVORY + (255,))
        x += tw + 32 + 14
    img.alpha_composite(overlay)
    out = OUT / "feature-graphic.png"
    img.convert("RGB").save(out, optimize=True)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", default="all")
    args = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    print(icon())
    print(feature_graphic())
    for lang in (SLIDES if args.lang == "all" else [args.lang]):
        if not (RAW / lang).exists():
            print(f"skip {lang}: no raw screens")
            continue
        for spec in SLIDES[lang]:
            print(slide(lang, *spec))


if __name__ == "__main__":
    main()
