# -*- coding: utf-8 -*-
"""
Fix a real content bug: quiz distractor options were originally drawn from the WHOLE lesson
group (07_emit_content.py) or the whole lesson's letter list (the alphabet generators), which
meant a multiple-choice quiz could show words/letters as wrong-answer options that haven't been
taught yet within that lesson - a "not taught yet" appearing in a quiz, reported by the user.

This operates directly on the final content JSON (not by re-running the original generators),
walking every lesson IN CURRICULUM ORDER and rebuilding each multiple-choice quiz's distractor
pool from only what's actually been taught so far (by teach-step adjacency, which both generators
guarantee: every quiz immediately follows its own teach step). Matching exercises are untouched -
they already only ever include items taught earlier in the same lesson, by construction.
"""
import json
from collections import defaultdict
from pathlib import Path

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"

# The non-connecting-letter conceptual quiz ("Does this letter connect...") is a fixed Yes/No
# question, not an identification quiz drawing from a taught-items pool - leave it untouched.
YES_NO_LABELS = {"Yes, it connects forward", "No, the word breaks after it"}


def fix_file(exercises_path: Path, lessons_path: Path, indent: int | None):
    with open(exercises_path, encoding="utf-8") as f:
        data = json.load(f)
    exercises = data["exercises"]

    with open(lessons_path, encoding="utf-8") as f:
        lessons = json.load(f)["lessons"]
    lesson_sort_order = {lesson["id"]: lesson["sortOrder"] for lesson in lessons}

    by_lesson = defaultdict(list)
    for ex in exercises:
        by_lesson[ex["lessonId"]].append(ex)
    for lesson_id in by_lesson:
        by_lesson[lesson_id].sort(key=lambda e: e["orderIndex"])

    ordered_lesson_ids = sorted(by_lesson.keys(), key=lambda lid: lesson_sort_order.get(lid, 0))

    taught_by_id: dict[str, dict] = {}
    taught_order: list[str] = []
    last_taught_id: str | None = None
    fixed_count = 0
    skipped_yes_no = 0

    for lesson_id in ordered_lesson_ids:
        for ex in by_lesson[lesson_id]:
            content = ex["content"]
            ctype = content.get("type")

            if ctype in ("letter_intro", "word_intro"):
                if ctype == "letter_intro":
                    item_id = content["letterId"]
                    info = {"en": content["transliterationEn"], "bn": content["transliterationBn"]}
                else:
                    item_id = content["wordId"]
                    info = {"en": content["meaningEn"][:60], "bn": content["meaningBn"]}
                if item_id not in taught_by_id:
                    taught_order.append(item_id)
                taught_by_id[item_id] = info
                last_taught_id = item_id

            elif ctype == "multiple_choice":
                option_labels = {o.get("labelEn") for o in content["options"]}
                if option_labels and option_labels <= YES_NO_LABELS:
                    skipped_yes_no += 1
                    continue
                if last_taught_id is None:
                    continue  # shouldn't happen given the generators' own teach-then-quiz shape

                current_id = last_taught_id
                current_label = taught_by_id[current_id]["en"]
                distractor_ids = []
                seen_labels = {current_label}
                for t in reversed(taught_order):
                    if t == current_id:
                        continue
                    label = taught_by_id[t]["en"]
                    if label in seen_labels:
                        continue  # e.g. duplicate generic "a noun (see the example verse...)" fallbacks
                    seen_labels.add(label)
                    distractor_ids.append(t)
                    if len(distractor_ids) == 3:
                        break
                chosen_ids = [current_id] + distractor_ids

                # Deterministic-but-varied correct-answer slot instead of always position 0.
                pos = len(taught_order) % len(chosen_ids)
                chosen_ids[0], chosen_ids[pos] = chosen_ids[pos], chosen_ids[0]

                options = []
                correct_option_id = None
                for idx, item_id in enumerate(chosen_ids):
                    oid = f"o{idx + 1}"
                    info = taught_by_id[item_id]
                    options.append({"id": oid, "labelEn": info["en"], "labelBn": info["bn"]})
                    if item_id == current_id:
                        correct_option_id = oid

                content["options"] = options
                content["correctOptionId"] = correct_option_id
                fixed_count += 1

    with open(exercises_path, "w", encoding="utf-8") as f:
        if indent is not None:
            json.dump(data, f, ensure_ascii=False, indent=indent)
        else:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    print(f"{exercises_path.name}: fixed {fixed_count} multiple_choice exercises, skipped {skipped_yes_no} yes/no quizzes")


if __name__ == "__main__":
    # Preserve each file's original formatting so the diff reflects only the actual content
    # change (options/correctOptionId), not a reformat of the whole file.
    fix_file(CONTENT_DIR / "exercises_alphabet.json", CONTENT_DIR / "lessons.json", indent=2)
    fix_file(CONTENT_DIR / "exercises_vocabulary.json", CONTENT_DIR / "lessons_vocabulary.json", indent=None)
