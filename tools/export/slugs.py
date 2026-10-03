"""Stable, readable URL slugs for the word and root pages (shared by the dataset, the static SEO
pages and the web app's data, so every link points at the same page)."""
import re
import unicodedata

# Academic transliteration -> plain ASCII (ʿ ʾ dropped, dotted letters folded).
_LATIN = str.maketrans({"ʿ": "", "ʾ": "", "'": "", "’": "", "‘": "", "`": ""})

# Root letters -> ASCII. Hamza and ʿayn have no ASCII sound of their own: they read as a / e.
_ROOT_SLUG = {
    "ء": "a", "أ": "a", "إ": "a", "آ": "a", "ؤ": "a", "ئ": "a", "ا": "a", "ب": "b", "ت": "t", "ث": "th",
    "ج": "j", "ح": "h", "خ": "kh", "د": "d", "ذ": "dh", "ر": "r", "ز": "z", "س": "s", "ش": "sh",
    "ص": "s", "ض": "d", "ط": "t", "ظ": "z", "ع": "e", "غ": "gh", "ف": "f", "ق": "q", "ك": "k",
    "ل": "l", "م": "m", "ن": "n", "ه": "h", "و": "w", "ي": "y", "ى": "y", "ة": "h",
}


def _ascii(text):
    t = unicodedata.normalize("NFKD", (text or "").translate(_LATIN))
    t = "".join(c for c in t if not unicodedata.combining(c)).lower()
    return re.sub(r"[^a-z0-9]+", "-", t).strip("-")


def word_slug(word_id, transliteration):
    """'khāfa', 'wv_5536bfac' -> 'khafa-5536bfac' (the id suffix keeps it unique and stable)."""
    base = _ascii(transliteration)[:40].strip("-") or "word"
    return f"{base}-{word_id.split('_', 1)[-1]}"


def root_slugs(roots):
    """Arabic roots ('ر-ح-م') -> unique ASCII slugs ('r-h-m'); clashes such as ح/ه get -2, -3."""
    out, used = {}, {}
    for root in sorted(roots):
        base = "-".join(filter(None, (_ROOT_SLUG.get(ch, "") for ch in root.split("-")))) or "root"
        n = used.get(base, 0) + 1
        used[base] = n
        out[root] = base if n == 1 else f"{base}-{n}"
    return out
