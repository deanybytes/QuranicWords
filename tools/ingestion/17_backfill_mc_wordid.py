"""Backfill a `wordId` field onto every `multiple_choice` exercise in exercises_vocabulary.json.

Why: ExerciseContent.MultipleChoice previously carried no reference to the word it quizzes -
only `promptArabic` (the literal Arabic surface form) and a per-exercise-local `correctOptionId`
(e.g. "o3", never a real word id). This made it impossible for the app to resolve the word's
canonical meaning at read time (see LessonViewModel.resolveCanonicalMeaning /
LessonViewModel.rebuildOptions), which is the root fix for the meaning-inconsistency bug where
the same word showed different translations on different screens.

`tap_what_you_hear` is not handled here: zero such exercises exist in the current content (no
audio-based MCQ content has been generated yet), confirmed by a direct count over the shipped
JSON before writing this script.

Resolution strategy per multiple_choice exercise, in order:
1. Look up all word_frequency.json rows whose `arabicWord` exactly matches the exercise's
   `promptArabic`. ~91% of words have a unique arabicWord, so most exercises resolve immediately.
2. Ambiguous (multiple rows share that surface form): prefer whichever candidate wordId has its
   own `word_intro` exercise in the *same lessonId* as this multiple_choice exercise - the
   curriculum builder places a word's teach step and its quiz in the same lesson, so this is a
   strong, independent (non-text-based) signal.
3. Still ambiguous, and all remaining candidates share byte-identical `meaning` across every
   language present (confirmed to be actual duplicate word_frequency rows for the same lemma, not
   distinct words that merely look alike): any one of them resolves to the same displayed text, so
   pick the lowest `frequencyRank` deterministically.
4. Still ambiguous: disambiguate using the correct option's English label - word_frequency's
   `meaning.en` for the right candidate must start with (or be started by - options are truncated
   to 60 chars at generation time) the correct option's `label.en`, since both were originally
   copied from the same source lemma. Only used when it narrows to exactly one candidate.
5. Anything still unresolved is reported, not guessed - the JSON is not rewritten if any
   exercise cannot be confidently resolved, forcing a manual look rather than silently mis-tagging
   a word.

Usage: py tools/ingestion/17_backfill_mc_wordid.py [--dry-run]
"""
import json
import sys
from pathlib import Path

CONTENT_DIR = Path(__file__).resolve().parent.parent.parent / "app/src/main/assets/content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"
WORD_FREQ_PATH = CONTENT_DIR / "word_frequency.json"


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main():
    dry_run = "--dry-run" in sys.argv

    wf_data = load(WORD_FREQ_PATH)
    words = wf_data["words"] if isinstance(wf_data, dict) else wf_data

    by_arabic = {}
    for w in words:
        by_arabic.setdefault(w["arabicWord"], []).append(w)

    ex_data = load(EXERCISES_PATH)
    exercises = ex_data["exercises"]

    # lessonId -> set of wordIds that have a word_intro exercise in that lesson.
    lesson_word_intros = {}
    for e in exercises:
        c = e.get("content", {})
        if c.get("type") == "word_intro":
            lesson_word_intros.setdefault(e["lessonId"], set()).add(c["wordId"])

    already_had = 0
    stats = {"unique": 0, "lesson_match": 0, "identical_meaning": 0, "label_match": 0}
    unresolved = []

    for e in exercises:
        content = e.get("content", {})
        if content.get("type") != "multiple_choice":
            continue
        if "wordId" in content:
            already_had += 1
            continue

        arabic = content.get("promptArabic")
        candidates = by_arabic.get(arabic, [])

        if len(candidates) == 1:
            content["wordId"] = candidates[0]["id"]
            stats["unique"] += 1
            continue

        if len(candidates) == 0:
            unresolved.append((e["id"], arabic, "no word_frequency match"))
            continue

        # Tier 2: same-lesson word_intro co-occurrence.
        same_lesson = [c for c in candidates if c["id"] in lesson_word_intros.get(e["lessonId"], set())]
        if len(same_lesson) == 1:
            content["wordId"] = same_lesson[0]["id"]
            stats["lesson_match"] += 1
            continue

        # Tier 3: all candidates are genuine duplicate rows (identical meaning in every language
        # present) - any one displays the same text, so pick deterministically.
        first_meaning = candidates[0]["meaning"]
        if all(c["meaning"] == first_meaning for c in candidates):
            chosen = min(candidates, key=lambda c: c["frequencyRank"])
            content["wordId"] = chosen["id"]
            stats["identical_meaning"] += 1
            continue

        # Tier 4: disambiguate via the correct option's English label against each candidate's
        # canonical English meaning.
        correct_id = content.get("correctOptionId")
        correct_option = next((o for o in content.get("options", []) if o["id"] == correct_id), None)
        correct_label_en = (correct_option or {}).get("label", {}).get("en", "").strip()

        matches = [
            c for c in candidates
            if correct_label_en and (
                c["meaning"].get("en", "").startswith(correct_label_en)
                or correct_label_en.startswith(c["meaning"].get("en", ""))
            )
        ]

        if len(matches) == 1:
            content["wordId"] = matches[0]["id"]
            stats["label_match"] += 1
            continue

        # Tier 5: the label narrowed it to >1 candidates that are themselves duplicate rows for
        # the two languages that actually matter for display consistency (en/bn - the only ones
        # with human/gtaf-reviewed translations per CLAUDE.md; other languages are machine
        # translations of these two and may vary row-to-row without affecting what's shown) - same
        # "doesn't matter which, they display the same text" logic as tier 3, applied to the
        # narrowed-down subset.
        def _display_meaning(c):
            return (c["meaning"].get("en"), c["meaning"].get("bn"))

        if len(matches) > 1 and all(_display_meaning(c) == _display_meaning(matches[0]) for c in matches):
            chosen = min(matches, key=lambda c: c["frequencyRank"])
            content["wordId"] = chosen["id"]
            stats["label_match"] += 1
        else:
            unresolved.append(
                (e["id"], arabic, f"{len(candidates)} candidates, {len(same_lesson)} lesson matches, {len(matches)} label matches")
            )

    print(f"already had wordId: {already_had}")
    print(f"resolved (unique arabicWord): {stats['unique']}")
    print(f"resolved (same-lesson word_intro): {stats['lesson_match']}")
    print(f"resolved (identical-meaning duplicate rows): {stats['identical_meaning']}")
    print(f"resolved (disambiguated via label): {stats['label_match']}")
    print(f"unresolved: {len(unresolved)}")
    for ex_id, arabic, reason in unresolved[:30]:
        print(f"  {ex_id}: {reason!r}")

    if unresolved:
        print("\nRefusing to write output while unresolved exercises remain. Fix manually or extend the script.")
        sys.exit(1)

    if dry_run:
        print("\n--dry-run: not writing file.")
        return

    with open(EXERCISES_PATH, "w", encoding="utf-8") as f:
        json.dump(ex_data, f, ensure_ascii=False, separators=(",", ":"))
    print(f"\nWrote {EXERCISES_PATH}")


if __name__ == "__main__":
    main()
