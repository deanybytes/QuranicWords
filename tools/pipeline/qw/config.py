"""Pinned inputs and build constants. Every external download is verified by sha256."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]          # repo root
PIPELINE = ROOT / "tools" / "pipeline"
CACHE = PIPELINE / ".cache"
OUT = PIPELINE / "out"
REPORTS = PIPELINE / "reports"
OVERRIDES = PIPELINE / "overrides"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "content"
GTAF_DIR = ROOT / "reference" / "word-by-word"

SEED = 20261003

# App content-language keys (Indonesian is "in", matching Language.INDONESIAN.tag).
LANGS = ["en", "bn", "ur", "hi", "in", "tr", "fa", "fr"]
GTAF_FILES = {
    "en": "QuranicWords_English.json", "bn": "QuranicWords_Bangla.json", "ur": "QuranicWords_Urdu.json",
    "hi": "QuranicWords_Hindi.json", "in": "QuranicWords_Indonesian.json", "tr": "QuranicWords_Turkish.json",
    "fa": "QuranicWords_Farsi.json", "fr": "QuranicWords_French.json",
}

QAC_URL = "https://raw.githubusercontent.com/mustafa0x/quran-morphology/master/quran-morphology.txt"
QAC_SHA256 = None  # filled by `run.py --pin` on first fetch; see sources.PINS_FILE

# Full-verse translations. For each language, the edition whose wording agrees most often with the
# GTAF word-by-word glosses (measured over every clean occurrence), so the card meaning can be
# found verbatim in the translation as often as possible. ("ac:" = alquran.cloud, "qc:" = quran.com)
VERSE_EDITIONS = {
    "en": ("ac:en.sahih", "Saheeh International"),
    "bn": ("qc:213", "Dr. Abu Bakr Muhammad Zakaria"),
    "ur": ("qc:158", "Dr. Israr Ahmad (Bayan-ul-Quran)"),
    "hi": ("qc:122", "Maulana Azizul Haque al-Umari"),
    "in": ("ac:id.indonesian", "Indonesian Ministry of Religious Affairs"),
    "tr": ("qc:124", "Muslim Shahin"),
    "fa": ("qc:29", "Hussein Taji Kal Dari"),
    "fr": ("ac:fr.hamidullah", "Muhammad Hamidullah"),
}
VERSE_URL = "https://api.alquran.cloud/v1/quran/{edition}"
QURAN_COM_URL = "https://api.quran.com/api/v4/quran/translations/{id}"

WORDS_PER_LESSON = 5
LESSONS_PER_SECTION = 10
