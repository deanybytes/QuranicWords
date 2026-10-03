#!/usr/bin/env python3
"""Writes the Google Play store listing for every supported language and checks Play's limits.

    python3 tools/store/build_listing.py

Sources: tools/store/listing_<lang>.py (TITLE, SHORT, FULL, NOTES)
Output : store/listing/<play-locale>/{title,short_description,full_description,release_notes}.txt
         store/release-notes.txt   - all languages in Play Console's <locale>...</locale> format"""
import importlib.util
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OUT = ROOT / "store" / "listing"
# app language -> Google Play Console locale code
LOCALES = {"en": "en-US", "bn": "bn-BD", "ur": "ur", "hi": "hi-IN", "id": "id", "tr": "tr-TR", "fa": "fa", "fr": "fr-FR"}
LIMITS = {"TITLE": 30, "SHORT": 80, "FULL": 4000, "NOTES": 500}
FILES = {"TITLE": "title.txt", "SHORT": "short_description.txt", "FULL": "full_description.txt", "NOTES": "release_notes.txt"}


def load(lang):
    spec = importlib.util.spec_from_file_location(f"listing_{lang}", HERE / f"listing_{lang}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def main():
    problems, notes = [], []
    print(f"{'locale':8} {'title':>6} {'short':>6} {'full':>6} {'notes':>6}")
    for lang, locale in LOCALES.items():
        mod = load(lang)
        d = OUT / locale
        d.mkdir(parents=True, exist_ok=True)
        lengths = []
        for key, fname in FILES.items():
            text = getattr(mod, key).strip()
            n = len(text)
            lengths.append(n)
            if n > LIMITS[key]:
                problems.append(f"{locale} {key}: {n} > {LIMITS[key]}")
            (d / fname).write_text(text + "\n", encoding="utf-8")
        print(f"{locale:8} " + " ".join(f"{n:>6}" for n in lengths))
        notes.append(f"<{locale}>\n{mod.NOTES.strip()}\n</{locale}>")
    (ROOT / "store" / "release-notes.txt").write_text("\n".join(notes) + "\n", encoding="utf-8")
    if problems:
        raise SystemExit("Over Play limits:\n  " + "\n  ".join(problems))
    print("all within Google Play limits")


if __name__ == "__main__":
    main()
