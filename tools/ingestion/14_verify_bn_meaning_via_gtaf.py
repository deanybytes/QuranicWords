# -*- coding: utf-8 -*-
"""
Independently verifies (or replaces, where they disagree) the pre-existing AI-drafted
`meaning["bn"]` values using quran.gtaf.org's own Bangla word-by-word gloss data
(reference/word-by-word/QuranicWords_Bangla.json), the same reference corpus already used for
the other 10 languages in 13_translate_content_12lang.py. Bangla was deliberately excluded from
that script's NEW_LANGUAGES list because it already had *some* value (AI-drafted, unreviewed);
this script is the follow-up that actually cross-checks it against gtaf.org, closing QW-17/35-38.

Policy (matches how the other 10 languages' gtaf.org-sourced values are treated): where gtaf.org
has a confident, non-placeholder gloss for a word's exact verse position, that value is treated as
independently-sourced and REPLACES the AI draft; `meaningReviewed["bn"]` flips to true for that
word. Where gtaf.org has no data for a word (no confirmed arabicWordStart/End span, or no matching
entry), the existing AI-drafted value and its `false` reviewed flag are left untouched - this
script only raises confidence where it has independent evidence to do so, never fabricates
coverage it doesn't have.

Same in-place, minified JSON output convention as the rest of this pipeline.
"""
import json
from pathlib import Path

from arabic_utils import strip_diacritics

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
REFERENCE_DIR = Path(__file__).parent.parent.parent / "reference" / "word-by-word"

PLACEHOLDER_VALUES = {"*", "-", "", "—", "–"}


def load_bangla_reference():
    with open(REFERENCE_DIR / "QuranicWords_Bangla.json", encoding="utf-8") as f:
        return json.load(f)["data"]


def find_translation(ref_data, sura, ayah, target_arabic):
    ayat = ref_data.get(str(sura))
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


def patch_options_and_pairs_bn(exercises_doc, word_freq_by_id):
    """Same propagation gap fixed for the 10 new languages in 13_translate_content_12lang.py's
    patch_options_and_pairs(): ChoiceOption.label / MatchPair.right are separate embedded copies
    of a word's meaning baked in at emission time, not references - so updating
    word_freq_by_id[...]['meaning']['bn'] alone doesn't reach these. Overwrites (not just fills)
    since the underlying source value may have changed."""
    label_en_to_meaning = {w["meaning"]["en"]: w["meaning"] for w in word_freq_by_id.values() if "en" in w["meaning"]}
    option_updated = 0
    pair_updated = 0

    for ex in exercises_doc["exercises"]:
        content = ex["content"]
        if content.get("type") == "multiple_choice":
            for option in content.get("options", []):
                label = option.get("label", {})
                source_meaning = label_en_to_meaning.get(label.get("en"))
                if source_meaning is None or "bn" not in source_meaning:
                    continue
                if label.get("bn") != source_meaning["bn"]:
                    label["bn"] = source_meaning["bn"]
                    option["label"] = label
                    option_updated += 1
        elif content.get("type") == "matching":
            for pair in content.get("pairs", []):
                wf_row = word_freq_by_id.get(pair.get("wordId"))
                if wf_row is None or "bn" not in wf_row["meaning"]:
                    continue
                right = pair.get("right", {})
                if right.get("bn") != wf_row["meaning"]["bn"]:
                    right["bn"] = wf_row["meaning"]["bn"]
                    pair["right"] = right
                    pair_updated += 1

    return option_updated, pair_updated


def main():
    bangla_ref = load_bangla_reference()

    with open(CONTENT_DIR / "exercises_vocabulary.json", encoding="utf-8") as f:
        exercises_doc = json.load(f)
    with open(CONTENT_DIR / "word_frequency.json", encoding="utf-8") as f:
        word_freq_doc = json.load(f)

    word_freq_by_id = {w["id"]: w for w in word_freq_doc["words"]}

    attempted = 0
    no_span = 0
    verified = 0
    unchanged_no_gtaf_data = 0
    replaced_disagreement = 0

    for ex in exercises_doc["exercises"]:
        content = ex["content"]
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

        translation = find_translation(bangla_ref, sura, ayah, target_arabic)
        if translation is None:
            unchanged_no_gtaf_data += 1
            continue

        meaning = content.get("meaning", {})
        meaning_reviewed = content.get("meaningReviewed", {})
        word_id = content["wordId"]

        if meaning.get("bn") != translation:
            replaced_disagreement += 1
        meaning["bn"] = translation
        meaning_reviewed["bn"] = True
        content["meaning"] = meaning
        content["meaningReviewed"] = meaning_reviewed
        verified += 1

        wf_row = word_freq_by_id.get(word_id)
        if wf_row is not None:
            wf_row["meaning"]["bn"] = translation

    option_updated, pair_updated = patch_options_and_pairs_bn(exercises_doc, word_freq_by_id)

    with open(CONTENT_DIR / "exercises_vocabulary.json", "w", encoding="utf-8") as f:
        json.dump(exercises_doc, f, ensure_ascii=False, separators=(",", ":"))
    with open(CONTENT_DIR / "word_frequency.json", "w", encoding="utf-8") as f:
        json.dump(word_freq_doc, f, ensure_ascii=False, separators=(",", ":"))

    total = attempted + no_span
    print(f"WordIntro exercises with a matched verse span: {attempted}/{total}")
    print(f"Verified against gtaf.org (meaningReviewed[bn] -> true): {verified}/{total} ({round(100 * verified / total, 1)}%)")
    print(f"  of which gtaf.org's value disagreed with the AI draft and replaced it: {replaced_disagreement}")
    print(f"No gtaf.org Bangla data available (span matched, but no gloss found): {unchanged_no_gtaf_data}")
    print(f"No verse span at all (unchanged, still AI-drafted/unreviewed): {no_span}")
    print(f"ChoiceOption.label[bn] updated: {option_updated}, MatchPair.right[bn] updated: {pair_updated}")


if __name__ == "__main__":
    main()
