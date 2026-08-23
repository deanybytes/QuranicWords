"""Sync every embedded meaning copy in exercises_vocabulary.json to match word_frequency.json.

Why: word_frequency.json is the single canonical per-word meaning (see
LessonViewModel.resolveCanonicalMeaning, which the app now uses to override baked exercise text
at read time regardless of what's shipped in the JSON). This script makes the *shipped JSON*
internally consistent too, belt-and-suspenders - so inspecting the asset files directly, or a
future code path that doesn't go through the runtime resolver, doesn't show contradictions
either. Run this as the last step of any content-regeneration pass, and always AFTER
19_audit_meaning_consistency.py has been reviewed and any wrong canonical values in
word_frequency.json have been corrected - this script blindly propagates whatever
word_frequency.json currently says, including if it's wrong (see that script's docstring for why
"most recently reviewed" isn't the same as "correct").

Rewrites, for every language key present in word_frequency.json's `meaning` (all 12 supported
tags for 97%+ of words):
  - word_intro.meaning
  - multiple_choice's correct option's label (matched via wordId, not the fragile EN-text join
    14_verify_bn_meaning_via_gtaf.py used)
  - matching.pairs[].right (matched via wordId)
  - word_in_verse_tap.meaning (matched via wordId - the copy site every prior correction script
    missed entirely, and the one that only ever had en/bn populated)

Distractor options in multiple_choice are left untouched - they're regenerated at runtime from
WordFrequencyEntity anyway (LessonViewModel.rebuildOptions), so whatever's baked there is
already never shown as-is.
"""
import json
from pathlib import Path

CONTENT_DIR = Path(__file__).resolve().parent.parent.parent / "app/src/main/assets/content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"
WORD_FREQ_PATH = CONTENT_DIR / "word_frequency.json"


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main():
    wf_data = load(WORD_FREQ_PATH)
    words = {w["id"]: w for w in wf_data["words"]}

    ex_data = load(EXERCISES_PATH)
    exercises = ex_data["exercises"]

    changed = {"word_intro": 0, "multiple_choice": 0, "matching": 0, "word_in_verse_tap": 0}
    missing_word = 0

    for e in exercises:
        c = e.get("content", {})
        ctype = c.get("type")

        if ctype == "word_intro":
            canon = words.get(c["wordId"])
            if canon is None:
                missing_word += 1
                continue
            if c["meaning"] != canon["meaning"]:
                c["meaning"] = dict(canon["meaning"])
                changed["word_intro"] += 1

        elif ctype == "multiple_choice":
            canon = words.get(c.get("wordId"))
            if canon is None:
                missing_word += 1
                continue
            correct = next((o for o in c["options"] if o["id"] == c["correctOptionId"]), None)
            if correct is not None and correct.get("label") != canon["meaning"]:
                correct["label"] = dict(canon["meaning"])
                changed["multiple_choice"] += 1

        elif ctype == "matching":
            for pair in c["pairs"]:
                wid = pair.get("wordId")
                canon = words.get(wid) if wid else None
                if canon is None:
                    if wid:
                        missing_word += 1
                    continue
                if pair["right"] != canon["meaning"]:
                    pair["right"] = dict(canon["meaning"])
                    changed["matching"] += 1

        elif ctype == "word_in_verse_tap":
            canon = words.get(c["wordId"])
            if canon is None:
                missing_word += 1
                continue
            if c["meaning"] != canon["meaning"]:
                c["meaning"] = dict(canon["meaning"])
                changed["word_in_verse_tap"] += 1

    with open(EXERCISES_PATH, "w", encoding="utf-8") as f:
        json.dump(ex_data, f, ensure_ascii=False, separators=(",", ":"))

    print("Synced copies to canonical word_frequency.json values:")
    for site, count in changed.items():
        print(f"  {site}: {count} rewritten")
    print(f"missing word_frequency row (skipped): {missing_word}")


if __name__ == "__main__":
    main()
