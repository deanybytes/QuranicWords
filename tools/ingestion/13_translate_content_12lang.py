# -*- coding: utf-8 -*-
"""
Phase 4 (QW-48): populate per-word `meaning` translations for the 10 languages added beyond
en/bn, using the word-by-word gloss data collected from quran.gtaf.org's public API in
reference/word-by-word/QuranicWords_<Language>.json (see docs/CONTENT_SOURCES.md).

Matching strategy: for each WordIntro exercise with a confirmed arabicWordStart/End span (see
10_add_highlight_spans.py - only ~1,582/3,680 words have one), extract the exact Arabic word
substring, then find the matching word inside that verse's word-by-word entry in each language's
reference data via diacritic-normalized skeleton comparison (arabic_utils.strip_diacritics) -
exact-then-first-match, same tolerance level as the rest of this pipeline's word matching.

Deliberately NOT attempted: synthesizing an `exampleVerseTranslation` for the 10 new languages by
concatenating word-by-word glosses into a fake sentence - that would produce broken, ungrammatical
pseudo-translations, a form of fabrication this pipeline avoids elsewhere. Full-verse translation
stays English-fallback (via LocalizedText.get) for these 10 languages; only the word-level
`meaning` field is populated, since that's what's actually sourced.

In place, minified JSON output - same convention as 10_add_highlight_spans.py.
"""
import json
from pathlib import Path

from arabic_utils import strip_diacritics
from prompts_12lang import TEACH_PROMPT, QUIZ_PROMPT, MATCH_PROMPT, NEW_LANGUAGES

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
REFERENCE_DIR = Path(__file__).parent.parent.parent / "reference" / "word-by-word"

LANG_FILE_NAMES = {
    "sq": "Albanian", "zh": "Chinese", "fa": "Farsi", "fr": "French", "de": "German",
    "hi": "Hindi", "in": "Indonesian", "ru": "Russian", "tr": "Turkish", "ur": "Urdu",
}


def load_reference_data():
    data = {}
    for lang, name in LANG_FILE_NAMES.items():
        path = REFERENCE_DIR / f"QuranicWords_{name}.json"
        with open(path, encoding="utf-8") as f:
            data[lang] = json.load(f)["data"]
    return data


# Some languages' gtaf.org data leaves a literal placeholder on one half of a two-word idiom
# whose gloss was attached entirely to the other half (e.g. Farsi/Turkish splitting "مِن قَبْلِكَ" -
# "before you" - unevenly). Measured: Farsi 5.5%, Turkish 8.9% of all word entries, smaller for
# others. A placeholder is never a real translation - treated as no-match rather than stored.
PLACEHOLDER_VALUES = {"*", "-", "", "—", "–"}


def find_translation(ref_data_for_lang, sura, ayah, target_arabic):
    ayat = ref_data_for_lang.get(str(sura))
    if not ayat:
        return None
    words = ayat.get(str(ayah))
    if not words:
        return None
    target_skeleton = strip_diacritics(target_arabic)
    if not target_skeleton:
        return None
    for w in words:
        if strip_diacritics(w["arabic"]) == target_skeleton:
            translation = w["translation"].strip()
            if translation in PLACEHOLDER_VALUES:
                return None
            return translation
    return None


def patch_prompt(content_obj):
    """Extends an already-emitted exercise's prompt map with the 10 new languages' AI-drafted
    translations, keyed by content type (WordIntro/MultipleChoice/Matching each use a fixed,
    known phrase - see prompts_12lang.py)."""
    prompt = content_obj.get("prompt", {})
    content_type = content_obj.get("type")
    source = {
        "word_intro": TEACH_PROMPT,
        "multiple_choice": QUIZ_PROMPT,
        "matching": MATCH_PROMPT,
    }.get(content_type)
    if source is None:
        return
    for lang in NEW_LANGUAGES:
        prompt.setdefault(lang, source[lang])
    content_obj["prompt"] = prompt


def main():
    reference_data = load_reference_data()

    with open(CONTENT_DIR / "exercises_vocabulary.json", encoding="utf-8") as f:
        exercises_doc = json.load(f)
    with open(CONTENT_DIR / "word_frequency.json", encoding="utf-8") as f:
        word_freq_doc = json.load(f)

    word_freq_by_id = {w["id"]: w for w in word_freq_doc["words"]}

    matched_counts = {lang: 0 for lang in NEW_LANGUAGES}
    attempted = 0
    no_span = 0

    for ex in exercises_doc["exercises"]:
        content = ex["content"]
        patch_prompt(content)

        if content.get("type") != "word_intro":
            continue

        start = content.get("arabicWordStart")
        end = content.get("arabicWordEnd")
        if start is None or end is None:
            no_span += 1
            continue

        attempted += 1
        target_arabic = content["exampleVerseArabic"][start:end]
        sura, ayah = content["exampleVerseReference"].split(":")

        meaning = content.get("meaning", {})
        meaning_reviewed = content.get("meaningReviewed", {})
        word_id = content["wordId"]
        wf_row = word_freq_by_id.get(word_id)

        for lang in NEW_LANGUAGES:
            translation = find_translation(reference_data[lang], sura, ayah, target_arabic)
            if translation is None:
                continue
            meaning[lang] = translation
            meaning_reviewed[lang] = False
            matched_counts[lang] += 1
            if wf_row is not None:
                wf_row["meaning"][lang] = translation

        content["meaning"] = meaning
        content["meaningReviewed"] = meaning_reviewed

    with open(CONTENT_DIR / "exercises_vocabulary.json", "w", encoding="utf-8") as f:
        json.dump(exercises_doc, f, ensure_ascii=False, separators=(",", ":"))
    with open(CONTENT_DIR / "word_frequency.json", "w", encoding="utf-8") as f:
        json.dump(word_freq_doc, f, ensure_ascii=False, separators=(",", ":"))

    print(f"WordIntro exercises with a matched verse span: {attempted}/{attempted + no_span}")
    print("Per-language word-meaning matches:")
    for lang in NEW_LANGUAGES:
        pct = round(100 * matched_counts[lang] / attempted, 1) if attempted else 0
        print(f"  {lang}: {matched_counts[lang]}/{attempted} ({pct}%)")
    print("Prompt translations (TEACH_WORD/MULTIPLE_CHOICE/MATCHING) patched for all exercises.")


if __name__ == "__main__":
    main()
