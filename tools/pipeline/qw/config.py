"""Pinned inputs and build constants. Every external download is verified by sha256."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]          # repo root
PIPELINE = ROOT / "tools" / "pipeline"
CACHE = PIPELINE / ".cache"
OUT = PIPELINE / "out"
REPORTS = PIPELINE / "reports"
OVERRIDES = PIPELINE / "overrides"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "content"
AUDIO_DIR = ROOT / "app" / "src" / "main" / "assets" / "audio" / "words"
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

# Full-verse translations (alquran.cloud editions).
VERSE_EDITIONS = {
    "en": "en.sahih", "bn": "bn.bengali", "ur": "ur.jalandhry", "hi": "hi.hindi",
    "in": "id.indonesian", "tr": "tr.diyanet", "fa": "fa.fooladvand", "fr": "fr.hamidullah",
}
VERSE_URL = "https://api.alquran.cloud/v1/quran/{edition}"

WORDS_PER_LESSON = 5
LESSONS_PER_SECTION = 10
