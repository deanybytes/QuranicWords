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
    "fr": [
        ("01-learn-the-quran", "Comprendre le Coran,\nmot à mot",
         "3 833 mots · 97 % du Coran · 8 langues", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "Chaque mot dans son verset",
         "Seul le mot étudié est surligné – avec son sens exact", ["teach_senses"]),
        ("03-learn-by-doing", "Apprendre en pratiquant",
         "QCM, associations, textes à trous, mot à toucher", ["quiz", "matching"]),
        ("04-home-screen-widgets", "Widgets d'écran d'accueil",
         "Mot du moment, série, objectif et quêtes d'un coup d'œil", ["widgets"]),
        ("05-stay-motivated", "Restez motivé",
         "XP, niveaux, séries, quêtes du jour, combos et cœurs", ["summary"]),
        ("06-track-progress", "Suivez vos progrès",
         "Révision du jour, jauge de mémoire et 27 succès", ["progress", "achievements"]),
        ("07-quran-fonts", "7 polices coraniques",
         "Moushaf de Médine, Nourani, Hafizi, Amiri et plus", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "Superbe en mode sombre",
         "100 % hors ligne · sans pub · gratuit pour toujours", ["dark_teach", "about"]),
    ],
    "tr": [
        ("01-learn-the-quran", "Kur'an'ı\nkelime kelime anlayın",
         "3.833 kelime · Kur'an'ın %97'si · 8 dil", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "Her kelime kendi ayetinde",
         "Yalnızca öğrenilen kelime vurgulanır – birebir anlamıyla", ["teach_senses"]),
        ("03-learn-by-doing", "Yaparak öğrenin",
         "Test, eşleştirme, boşluk doldurma ve kelimeye dokunma", ["quiz", "matching"]),
        ("04-home-screen-widgets", "Ana ekran widget'ları",
         "Anlık kelime, seri, hedef ve görevler tek bakışta", ["widgets"]),
        ("05-stay-motivated", "Motivasyonunuzu koruyun",
         "XP, seviyeler, seriler, günlük görevler, kombolar ve kalpler", ["summary"]),
        ("06-track-progress", "İlerlemenizi izleyin",
         "Günlük Tekrar, hafıza gücü ve 27 başarım", ["progress", "achievements"]),
        ("07-quran-fonts", "7 Kur'an yazı tipi",
         "Medine Mushafı, Nurani, Hafızlık, Amiri ve dahası", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "Koyu temada da şık",
         "%100 çevrimdışı · reklamsız · tamamen ücretsiz", ["dark_teach", "about"]),
    ],
    "id": [
        ("01-learn-the-quran", "Pahami Al-Qur'an\nkata demi kata",
         "3.833 kata · 97% Al-Qur'an · 8 bahasa", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "Setiap kata dalam ayatnya",
         "Hanya kata yang dipelajari yang disorot – dengan makna persisnya", ["teach_senses"]),
        ("03-learn-by-doing", "Belajar sambil berlatih",
         "Kuis, mencocokkan, isian, dan ketuk kata", ["quiz", "matching"]),
        ("04-home-screen-widgets", "Widget layar utama",
         "Kata saat ini, runtunan, target, dan misi sekilas", ["widgets"]),
        ("05-stay-motivated", "Tetap termotivasi",
         "XP, level, runtunan, misi harian, kombo, dan hati", ["summary"]),
        ("06-track-progress", "Pantau kemajuan Anda",
         "Ulasan Harian, kekuatan ingatan, dan 27 pencapaian", ["progress", "achievements"]),
        ("07-quran-fonts", "7 font Al-Qur'an",
         "Mushaf Madinah, Nurani, Hafizi, Amiri, dan lainnya", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "Indah dalam mode gelap",
         "100% offline · tanpa iklan · gratis selamanya", ["dark_teach", "about"]),
    ],
    "hi": [
        ("01-learn-the-quran", "क़ुरआन को\nशब्द-दर-शब्द समझें",
         "3,833 शब्द · क़ुरआन का 97% · 8 भाषाएँ", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "हर शब्द अपनी आयत में",
         "सिर्फ़ सिखाया गया शब्द हाइलाइट – उसके सटीक अर्थ के साथ", ["teach_senses"]),
        ("03-learn-by-doing", "अभ्यास से सीखें",
         "क्विज़, मिलान, रिक्त स्थान और शब्द पर टैप", ["quiz", "matching"]),
        ("04-home-screen-widgets", "होम स्क्रीन विजेट",
         "आज का शब्द, सिलसिला, लक्ष्य और क्वेस्ट एक नज़र में", ["widgets"]),
        ("05-stay-motivated", "प्रेरित रहें",
         "XP, लेवल, सिलसिला, रोज़ के क्वेस्ट, कॉम्बो और दिल", ["summary"]),
        ("06-track-progress", "अपनी प्रगति देखें",
         "रोज़ाना दोहराई, याददाश्त की मज़बूती और 27 उपलब्धियाँ", ["progress", "achievements"]),
        ("07-quran-fonts", "7 क़ुरआनी फ़ॉन्ट",
         "मदीना मुसहफ़, नूरानी, हाफ़िज़ी, अमीरी और भी", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "डार्क मोड में भी सुंदर",
         "100% ऑफ़लाइन · विज्ञापन-मुक्त · हमेशा मुफ़्त", ["dark_teach", "about"]),
    ],
    "ur": [
        ("01-learn-the-quran", "قرآن کو\nلفظ بہ لفظ سمجھیں",
         "۳٬۸۳۳ الفاظ، قرآن کا ۹۷٪، ۸ زبانیں", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "ہر لفظ اپنی آیت میں",
         "صرف سکھایا جانے والا لفظ نمایاں، اپنے درست معنی کے ساتھ", ["teach_senses"]),
        ("03-learn-by-doing", "مشق سے سیکھیں",
         "کوئز، جوڑ ملانا، خالی جگہ اور لفظ پر ٹیپ", ["quiz", "matching"]),
        ("04-home-screen-widgets", "ہوم اسکرین ویجیٹس",
         "لمحے کا لفظ، تسلسل، ہدف اور کام ایک نظر میں", ["widgets"]),
        ("05-stay-motivated", "حوصلہ برقرار رکھیں",
         "XP، لیول، تسلسل، روزانہ کے کام، کومبو اور دل", ["summary"]),
        ("06-track-progress", "اپنی پیش رفت دیکھیں",
         "روزانہ دہرائی، یادداشت کی مضبوطی اور ۲۷ کامیابیاں", ["progress", "achievements"]),
        ("07-quran-fonts", "۷ قرآنی فونٹس",
         "مدینہ مصحف، نورانی، حافظی، امیری اور مزید", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "ڈارک موڈ میں بھی خوبصورت",
         "۱۰۰٪ آف لائن، اشتہارات سے پاک، ہمیشہ مفت", ["dark_teach", "about"]),
    ],
    "fa": [
        ("01-learn-the-quran", "قرآن را\nواژه به واژه بفهمید",
         "۳٬۸۳۳ واژه، ۹۷٪ قرآن، ۸ زبان", ["tour_coverage", "home"]),
        ("02-word-in-its-ayah", "هر واژه در آیهٔ خودش",
         "فقط واژهٔ آموزشی برجسته است، با معنای دقیقش", ["teach_senses"]),
        ("03-learn-by-doing", "با تمرین یاد بگیرید",
         "آزمون، جورکردن، جای خالی و لمس واژه", ["quiz", "matching"]),
        ("04-home-screen-widgets", "ابزارک‌های صفحهٔ اصلی",
         "واژهٔ لحظه، پیوستگی، هدف و مأموریت‌ها در یک نگاه", ["widgets"]),
        ("05-stay-motivated", "انگیزه‌تان را حفظ کنید",
         "XP، سطح‌ها، پیوستگی، مأموریت‌های روزانه، کامبو و قلب‌ها", ["summary"]),
        ("06-track-progress", "پیشرفتتان را ببینید",
         "مرور روزانه، قدرت حافظه و ۲۷ دستاورد", ["progress", "achievements"]),
        ("07-quran-fonts", "۷ قلم قرآنی",
         "مصحف مدینه، نورانی، حافظی، امیری و بیشتر", ["settings_fonts", "tour_fonts"]),
        ("08-dark-mode-offline", "زیبا در حالت تیره",
         "۱۰۰٪ آفلاین، بدون تبلیغ، همیشه رایگان", ["dark_teach", "about"]),
    ],
}


RTL = {"ur", "fa"}


def _font_file(script, bold, lang):
    if script == "bn":
        return "NotoSansBengali-Bold.ttf" if bold else "NotoSansBengali-Regular.ttf"
    if script == "deva":
        return "NotoSansDevanagari-Bold.ttf" if bold else "NotoSansDevanagari-Regular.ttf"
    if script == "arab":
        if lang == "ur":   # Urdu is read in Nastaliq
            return "NotoNastaliqUrdu-Bold.ttf" if bold else "NotoNastaliqUrdu-Regular.ttf"
        return "NotoNaskhArabic-Bold.ttf" if bold else "NotoNaskhArabic-Regular.ttf"
    return "NotoSans-Bold.ttf" if bold else "NotoSans-Regular.ttf"


def font(size, bold=False, lang="en", script=None):
    """The caption font for a language (or for one script run inside it)."""
    script = script or {"bn": "bn", "hi": "deva", "ur": "arab", "fa": "arab"}.get(lang, "latn")
    if script == "arab" and lang == "ur":
        size = int(size * 0.82)   # Nastaliq draws much taller than Naskh at the same size
    return ImageFont.truetype(str(NOTO / _font_file(script, bold, lang)), size)


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


def _script(ch):
    o = ord(ch)
    if 0x0980 <= o <= 0x09FF:
        return "bn"
    if 0x0900 <= o <= 0x097F:
        return "deva"
    if 0x0600 <= o <= 0x06FF or 0x0750 <= o <= 0x077F or 0xFB50 <= o <= 0xFDFF or 0xFE70 <= o <= 0xFEFF or o == 0x200C:
        return "arab"
    return "latn"


def _runs(text, size, bold, lang):
    """(substring, font, script) runs: each script in its own font (the Bengali, Devanagari and
    Arabic fonts lack Latin letters and "·"); spaces stay with the run they are in."""
    runs, cur, cur_s = [], "", None
    for ch in text:
        sc = cur_s if ch == " " and cur_s else _script(ch)
        if cur_s is None or sc == cur_s:
            cur += ch
        else:
            runs.append((cur, cur_s))
            cur = ch
        cur_s = sc
    if cur:
        runs.append((cur, cur_s))
    return [(t, font(size, bold, lang, sc), sc) for t, sc in runs]


def draw_centered(d, text, y, size, bold, fill, lang="en", spacing=10, max_width=W - 80):
    """Centered multi-line text; right-to-left languages lay their runs out from the right.
    The size shrinks until the widest line fits within max_width."""
    while size > 22 and max(_width(d, ln, size, bold, lang) for ln in text.split("\n")) > max_width:
        size -= 1
    for line in text.split("\n"):
        runs = _runs(line, size, bold, lang)
        widths = [d.textlength(t, font=f, direction="rtl" if sc == "arab" else None) for t, f, sc in runs]
        total = sum(widths)
        x = (W + total) / 2 if lang in RTL else (W - total) / 2
        # All runs share the main font's baseline (anchor "ls"), so a Latin "–", "·" or "XP" sits
        # on the same line as the Bengali, Devanagari or Arabic text around it.
        base = y + font(size, bold, lang).getmetrics()[0]
        for (t, f, sc), w in zip(runs, widths):
            if lang in RTL:
                x -= w
                d.text((x, base), t, font=f, fill=fill, anchor="ls", direction="rtl" if sc == "arab" else None)
            else:
                d.text((x, base), t, font=f, fill=fill, anchor="ls", direction="rtl" if sc == "arab" else None)
                x += w
        line_h = size * (1.95 if lang == "ur" else 1.45 if lang in ("bn", "hi", "fa") else 1.25)
        y += int(line_h) + spacing
    return y


def slide(lang, name, headline, subline, screens):
    img = background(W, H).convert("RGBA")
    d = ImageDraw.Draw(img)
    y = draw_centered(d, headline, 80, 70 if "\n" in headline else 76, True, IVORY, lang=lang)
    y = draw_centered(d, subline, y + 6, 36, False, GOLD, lang=lang)
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


FEATURE = {
    "en": ("Learn the words of the Qur'an,", "word by word \u2013 with every ayah.", ["3,833 words", "8 languages", "100% offline"]),
    "bn": ("কুরআনের শব্দ শিখুন,", "শব্দে শব্দে \u2013 প্রতিটি আয়াতসহ।", ["৩,৮৩৩টি শব্দ", "৮টি ভাষা", "১০০% অফলাইন"]),
    "ur": ("قرآن کے الفاظ سیکھیں،", "لفظ بہ لفظ، ہر آیت کے ساتھ", ["۳٬۸۳۳ الفاظ", "۸ زبانیں", "۱۰۰٪ آف لائن"]),
    "hi": ("क़ुरआन के शब्द सीखें,", "शब्द-दर-शब्द \u2013 हर आयत के साथ", ["3,833 शब्द", "8 भाषाएँ", "100% ऑफ़लाइन"]),
    "id": ("Pelajari kata-kata Al-Qur'an,", "kata demi kata \u2013 dengan setiap ayat.", ["3.833 kata", "8 bahasa", "100% offline"]),
    "tr": ("Kur'an'ın kelimelerini öğrenin,", "kelime kelime \u2013 her ayetiyle.", ["3.833 kelime", "8 dil", "%100 çevrimdışı"]),
    "fa": ("واژه‌های قرآن را بیاموزید،", "واژه به واژه، همراه هر آیه", ["۳٬۸۳۳ واژه", "۸ زبان", "۱۰۰٪ آفلاین"]),
    "fr": ("Apprenez les mots du Coran,", "mot à mot \u2013 avec chaque verset.", ["3 833 mots", "8 langues", "100 % hors ligne"]),
}


def _width(d, text, size, bold, lang):
    return sum(d.textlength(t, font=f, direction="rtl" if sc == "arab" else None) for t, f, sc in _runs(text, size, bold, lang))


def draw_at(d, text, x, y, size, bold, fill, lang, align="left"):
    """One line of mixed-script text starting at x (left) or ending at x (right)."""
    runs = _runs(text, size, bold, lang)
    widths = [d.textlength(t, font=f, direction="rtl" if sc == "arab" else None) for t, f, sc in runs]
    base = y + font(size, bold, lang).getmetrics()[0]   # shared baseline for every run
    if lang in RTL:
        cx = x if align == "right" else x + sum(widths)
        for (t, f, sc), w in zip(runs, widths):
            cx -= w
            d.text((cx, base), t, font=f, fill=fill, anchor="ls", direction="rtl" if sc == "arab" else None)
    else:
        cx = x if align == "left" else x - sum(widths)
        for (t, f, sc), w in zip(runs, widths):
            d.text((cx, base), t, font=f, fill=fill, anchor="ls")
            cx += w
    return sum(widths)


def feature_graphic(lang="en"):
    """1024x500 feature graphic: logo, brand name, translated tagline and chips. Urdu and Persian
    are mirrored (logo on the right, text right-aligned). No scripture is used as decoration."""
    w, h = 1024, 500
    rtl = lang in RTL
    img = background(w, h).convert("RGBA")
    d = ImageDraw.Draw(img)
    logo = Image.open(ROOT / "icons/icon-512.png").convert("RGBA").resize((300, 300), Image.LANCZOS)
    img.alpha_composite(logo, (w - 60 - 300 if rtl else 60, 100))
    edge = w - 400 if rtl else 400          # text block edge next to the logo
    far = 40 if rtl else w - 40             # outer edge the text must not cross
    room = abs(far - edge)
    brand = font(76, True)
    bw = d.textlength("QuranicWords", font=brand)
    d.text(((edge - bw) if rtl else edge, 112), "QuranicWords", font=brand, fill=IVORY)
    line1, line2, chips = FEATURE[lang]
    size = 34
    while size > 22 and max(_width(d, line1, size, False, lang), _width(d, line2, size, False, lang)) > room:
        size -= 1
    gap = int(size * (1.75 if lang == "ur" else 1.3))
    draw_at(d, line1, edge, 214, size, False, GOLD, lang, "right" if rtl else "left")
    draw_at(d, line2, edge, 214 + gap, size, False, GOLD, lang, "right" if rtl else "left")
    # Chips
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    csize = 24
    def chips_width(cs):
        return sum(_width(od, c, cs, True, lang) + 32 for c in chips) + 14 * (len(chips) - 1)
    while csize > 16 and chips_width(csize) > room:
        csize -= 1
    cy = 214 + 2 * gap + 22
    ch = int(csize * (2.4 if lang == "ur" else 1.85))
    x = edge
    for c in chips:
        tw = _width(od, c, csize, True, lang)
        x0, x1 = (x - tw - 32, x) if rtl else (x, x + tw + 32)
        od.rounded_rectangle((x0, cy, x1, cy + ch), ch // 2, fill=(255, 248, 231, 28), outline=GOLD + (255,), width=2)
        ty = cy + (ch - csize * (1.9 if lang == "ur" else 1.35)) / 2
        draw_at(od, c, x0 + 16, ty, csize, True, IVORY + (255,), lang, "left")
        x = (x0 - 14) if rtl else (x1 + 14)
    img.alpha_composite(overlay)
    out = OUT / "feature-graphic" / f"{lang}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(out, optimize=True)
    if lang == "en":
        img.convert("RGB").save(OUT / "feature-graphic.png", optimize=True)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", default="all")
    args = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    print(icon())
    for lang in FEATURE:
        print(feature_graphic(lang))
    for lang in (SLIDES if args.lang == "all" else [args.lang]):
        if not (RAW / lang).exists():
            print(f"skip {lang}: no raw screens")
            continue
        for spec in SLIDES[lang]:
            print(slide(lang, *spec))


if __name__ == "__main__":
    main()
