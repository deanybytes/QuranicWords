"""One-off migration: converts the bundled content JSON from flat *En/*Bn field pairs to the
new map-based LocalizedText schema ({"en": "...", "bn": "..."}), matching the Kotlin model
refactor in ExerciseContent.kt / ChapterEntity.kt / SectionEntity.kt / LessonEntity.kt /
WordFrequencyEntity.kt. Idempotent - if a file's already migrated (no *En/*Bn keys left), it's
left untouched. Run once; not part of any regular pipeline stage.
"""
import json
import os

CONTENT_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "content")


def merge_localized(obj, base_key, langs=("en", "bn")):
    """Pops `{base_key}En`/`{base_key}Bn` etc. into a `{base_key}: {lang: value}` map."""
    result = {}
    for lang in langs:
        key = f"{base_key}{lang.capitalize()}"
        if key in obj:
            value = obj.pop(key)
            if value is not None:
                result[lang] = value
    obj[base_key] = result


def migrate_chapters():
    path = os.path.join(CONTENT_DIR, "chapters.json")
    data = json.load(open(path, encoding="utf-8"))
    changed = 0
    for c in data["chapters"]:
        if "titleEn" in c:
            merge_localized(c, "title")
            merge_localized(c, "description")
            changed += 1
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    return changed, len(data["chapters"])


def migrate_sections():
    path = os.path.join(CONTENT_DIR, "sections.json")
    data = json.load(open(path, encoding="utf-8"))
    changed = 0
    for s in data["sections"]:
        if "titleEn" in s:
            merge_localized(s, "title")
            changed += 1
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    return changed, len(data["sections"])


def migrate_lessons():
    path = os.path.join(CONTENT_DIR, "lessons_vocabulary.json")
    data = json.load(open(path, encoding="utf-8"))
    changed = 0
    for l in data["lessons"]:
        if "titleEn" in l:
            merge_localized(l, "title")
            changed += 1
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    return changed, len(data["lessons"])


def migrate_word_frequency():
    path = os.path.join(CONTENT_DIR, "word_frequency.json")
    data = json.load(open(path, encoding="utf-8"))
    changed = 0
    for w in data["words"]:
        if "meaningEn" in w:
            merge_localized(w, "meaning")
            changed += 1
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    return changed, len(data["words"])


def migrate_choice_option(opt):
    if "labelEn" in opt or "labelBn" in opt:
        merge_localized(opt, "label")


def migrate_match_pair(pair):
    if "rightEn" in pair or "rightBn" in pair:
        merge_localized(pair, "right")


def migrate_content(content):
    ctype = content.get("type")
    if "promptEn" in content:
        merge_localized(content, "prompt")

    if ctype == "word_intro":
        if "meaningEn" in content:
            merge_localized(content, "meaning")
        if "meaningBnReviewed" in content:
            reviewed = content.pop("meaningBnReviewed")
            content["meaningReviewed"] = {"bn": reviewed} if reviewed else {}
        if "exampleVerseTranslationEn" in content:
            merge_localized(content, "exampleVerseTranslation")
        if "meaningHighlightEn" in content or "meaningHighlightBn" in content:
            merge_localized(content, "meaningHighlight")
        elif "meaningHighlight" not in content:
            content["meaningHighlight"] = {}
    elif ctype == "fill_in_the_blank":
        if "sentenceTranslationEn" in content:
            merge_localized(content, "sentenceTranslation")
    elif ctype == "word_order":
        if "translationEn" in content:
            merge_localized(content, "translation")
    elif ctype == "word_in_verse_tap":
        if "meaningEn" in content:
            merge_localized(content, "meaning")

    for opt in content.get("options", []):
        migrate_choice_option(opt)
    for pair in content.get("pairs", []):
        migrate_match_pair(pair)


def migrate_exercises():
    path = os.path.join(CONTENT_DIR, "exercises_vocabulary.json")
    data = json.load(open(path, encoding="utf-8"))
    changed = 0
    for ex in data["exercises"]:
        content = ex.get("content")
        if content and ("promptEn" in content or "meaningEn" in content):
            migrate_content(content)
            changed += 1
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    return changed, len(data["exercises"])


def main():
    results = {
        "chapters.json": migrate_chapters(),
        "sections.json": migrate_sections(),
        "lessons_vocabulary.json": migrate_lessons(),
        "word_frequency.json": migrate_word_frequency(),
        "exercises_vocabulary.json": migrate_exercises(),
    }
    for name, (changed, total) in results.items():
        print(f"{name}: {changed}/{total} entries migrated")


if __name__ == "__main__":
    main()
