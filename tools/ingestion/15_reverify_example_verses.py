# -*- coding: utf-8 -*-
"""
Re-verifies exampleVerseArabic/exampleVerseReference for every word_intro entry (QW-18). For the
86.7% of lemmas matched via root (06_pick_verses.py's found_via_root path), the example verse
came from the matched root's own curated Quran-bil-Quran occurrence list - illustrating the
root's concept, never actually checked to contain the exact lemma's own surface form. Real,
measured gap (e.g. "قَوْم" paired with a verse that doesn't mention it at all).

Runs the exact same word-boundary-aware, diacritic-normalized token search 06_pick_verses.py
already uses (reusing arabic_utils.strip_diacritics directly - not reimplemented), against every
word_intro entry unconditionally rather than trying to reconstruct which of the original three
paths (root / exact-word / substring-fallback) each entry took - cheap (a few seconds), and
self-correcting for any future hand-edited entries too.

On a genuine literal hit: overwrites exampleVerseReference/exampleVerseArabic/
exampleVerseTranslation with the new occurrence (lowest surah:ayah on ties, same tie-break as
06_pick_verses.py), clears the now-stale arabicWordStart/End + meaningHighlight (computed against
the OLD verse text - 10_add_highlight_spans.py must recompute them fresh), and sets the new
exampleVerseVerified flag true. On a miss: the existing root-derived verse is left untouched
(there's no "no example verse" UI state to fall back to) and exampleVerseVerified is set false -
flagged honestly, never guessed.

Requires the raw Quran-bil-Quran (MIT) verses_text.json/en.sahih.json and risan/quran-json
(CC BY-SA 4.0) quran_bn.json under raw/ - same sources 06_pick_verses.py uses, already credited in
NOTICE. Not committed (tools/ingestion/raw/ is gitignored, reproducible by re-downloading from
those repos).

In place, minified JSON output - same convention as the rest of this pipeline's post-processing
passes.
"""
import json
from pathlib import Path

from arabic_utils import strip_diacritics

RAW = Path(__file__).parent / "raw"
CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"


def surah_ayah_key(ref: str):
    s, a = ref.split(":")
    return (int(s), int(a))


def load_verses_bn(chapters_bn):
    verses_bn = {}
    for chapter in chapters_bn:
        surah_id = chapter["id"]
        for verse in chapter["verses"]:
            verses_bn[f"{surah_id}:{verse['id']}"] = verse["translation"]
    return verses_bn


def find_best_ref(skeleton: str, verses_tokens: dict):
    """Same two-pass method as 06_pick_verses.py: exact token match first, substring fallback
    (bound clitics) only if no exact match exists anywhere. Lowest surah:ayah wins ties, matching
    06_pick_verses.py's own tie-break for root-sourced verses."""
    exact = [vref for vref, tokens in verses_tokens.items() if skeleton in tokens]
    if exact:
        return min(exact, key=surah_ayah_key)
    substring = [vref for vref, tokens in verses_tokens.items() if any(skeleton in tok for tok in tokens)]
    if substring:
        return min(substring, key=surah_ayah_key)
    return None


def main():
    with open(RAW / "verses_text.json", encoding="utf-8") as f:
        verses_ar = json.load(f)
    with open(RAW / "en_sahih.json", encoding="utf-8") as f:
        verses_en = json.load(f)
    with open(RAW / "quran_bn.json", encoding="utf-8") as f:
        chapters_bn = json.load(f)
    verses_bn = load_verses_bn(chapters_bn)

    verses_tokens = {
        ref: [strip_diacritics(tok) for tok in text.split(" ")]
        for ref, text in verses_ar.items()
    }

    with open(EXERCISES_PATH, encoding="utf-8") as f:
        doc = json.load(f)

    total = 0
    already_literal = 0
    replaced = 0
    kept_unverified = 0

    for ex in doc["exercises"]:
        content = ex["content"]
        if content.get("type") != "word_intro":
            continue
        total += 1

        skeleton = strip_diacritics(content["arabicWord"])
        current_ref = content.get("exampleVerseReference")
        found_ref = find_best_ref(skeleton, verses_tokens) if skeleton else None

        if found_ref is None:
            content["exampleVerseVerified"] = False
            kept_unverified += 1
            continue

        content["exampleVerseVerified"] = True
        if found_ref == current_ref:
            already_literal += 1
            continue

        content["exampleVerseReference"] = found_ref
        content["exampleVerseArabic"] = verses_ar[found_ref]
        translation = content.get("exampleVerseTranslation", {})
        translation["en"] = verses_en.get(found_ref, translation.get("en", ""))
        if found_ref in verses_bn:
            translation["bn"] = verses_bn[found_ref]
        content["exampleVerseTranslation"] = translation
        # Stale relative to the new verse text - 10_add_highlight_spans.py recomputes fresh.
        content["arabicWordStart"] = None
        content["arabicWordEnd"] = None
        content["meaningHighlight"] = {}
        replaced += 1

    with open(EXERCISES_PATH, "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))

    print(f"Total word_intro entries: {total}")
    print(f"Already literal (verse unchanged, now flagged verified): {already_literal}")
    print(f"Replaced with a genuine literal-occurrence verse: {replaced}")
    print(f"No literal occurrence found anywhere (kept prior root-derived verse, flagged unverified): {kept_unverified}")
    pct = round(100 * (already_literal + replaced) / total, 1) if total else 0
    print(f"Total now exampleVerseVerified=true: {already_literal + replaced}/{total} ({pct}%)")


if __name__ == "__main__":
    main()
