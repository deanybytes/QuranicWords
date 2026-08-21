# -*- coding: utf-8 -*-
"""
Internal content-authoring CLI (QW-27/8) - lets a maintainer look up and hand-edit a single
word's content without touching raw JSON or writing a one-off script for a small fix. Deliberately
stays a local, single-maintainer CLI rather than a hosted web tool: this app makes zero network
requests by design (see CLAUDE.md/SECURITY.md), and a web CMS would be the first server/backend
this project has ever had - out of scope here, same as multi-user auth (explicitly out of scope
per the ticket).

Edits word_frequency.json and exercises_vocabulary.json's word_intro entries directly, in the same
minified-JSON format the rest of tools/ingestion/ writes. Preserves the "flag unsourced content,
never fabricate" discipline: any field edited here is marked meaningReviewed[lang] = False for the
edited language (a human typed it just now, but it hasn't been through the same independent-source
cross-check the ingestion pipeline's automated passes use - same honesty standard, not a lower one).

Usage: python 16_cms.py
"""
import json
import sys
from pathlib import Path

# Windows' default console codepage (cp1252) can't print Arabic text at all - this CLI shows
# Arabic constantly, so force UTF-8 stdout/stdin rather than crashing on the first word looked up.
# No-op on platforms where stdout is already UTF-8.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"
WORD_FREQ_PATH = CONTENT_DIR / "word_frequency.json"
CONTENT_SEEDER_PATH = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "java" / \
    "com" / "quranicwords" / "app" / "core" / "data" / "assets" / "ContentSeeder.kt"

REQUIRED_WORD_INTRO_FIELDS = [
    "wordId", "arabicWord", "meaning", "exampleVerseArabic", "exampleVerseTranslation", "exampleVerseReference"
]


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))


def find_word_intro(exercises_doc, query):
    """Look up by exact wordId, or by exact/substring match on arabicWord."""
    matches = []
    for ex in exercises_doc["exercises"]:
        content = ex["content"]
        if content.get("type") != "word_intro":
            continue
        if content.get("wordId") == query or query in content.get("arabicWord", ""):
            matches.append(content)
    return matches


def print_word(content):
    print(f"wordId: {content['wordId']}")
    print(f"arabicWord: {content['arabicWord']}")
    print(f"meaning: {json.dumps(content.get('meaning', {}), ensure_ascii=False)}")
    print(f"meaningReviewed: {json.dumps(content.get('meaningReviewed', {}), ensure_ascii=False)}")
    print(f"exampleVerseReference: {content.get('exampleVerseReference')}")
    print(f"exampleVerseArabic: {content.get('exampleVerseArabic')}")
    print(f"exampleVerseVerified: {content.get('exampleVerseVerified')}")
    print(f"exampleVerseTranslation: {json.dumps(content.get('exampleVerseTranslation', {}), ensure_ascii=False)}")


def edit_meaning(content, word_freq_by_id):
    lang = input("Language tag to edit (e.g. en, bn, fr): ").strip()
    if not lang:
        print("No language given, cancelled.")
        return False
    current = content.get("meaning", {}).get(lang, "(none)")
    print(f"Current meaning[{lang}]: {current}")
    new_value = input(f"New meaning[{lang}] (blank to cancel): ").strip()
    if not new_value:
        print("Cancelled.")
        return False

    meaning = content.get("meaning", {})
    meaning[lang] = new_value
    content["meaning"] = meaning

    reviewed = content.get("meaningReviewed", {})
    # A human just typed this, but it hasn't been through the same independent-source cross-check
    # the automated passes use (gtaf.org, quranwbw.com, etc.) - flagged honestly, not assumed true.
    reviewed[lang] = False
    content["meaningReviewed"] = reviewed

    word_id = content["wordId"]
    wf_row = word_freq_by_id.get(word_id)
    if wf_row is not None:
        wf_meaning = wf_row.get("meaning", {})
        wf_meaning[lang] = new_value
        wf_row["meaning"] = wf_meaning
        print(f"Also updated word_frequency.json's meaning[{lang}] for {word_id} (kept in sync).")

    print("Updated. Remember to bump CONTENT_VERSION before this ships (see the 'finalize' menu option).")
    return True


def edit_example_verse(content):
    print("Editing the example verse. Leave a field blank to keep its current value.")
    new_ref = input(f"exampleVerseReference [{content.get('exampleVerseReference')}]: ").strip()
    new_arabic = input(f"exampleVerseArabic [{content.get('exampleVerseArabic')}]: ").strip()
    if new_ref:
        content["exampleVerseReference"] = new_ref
    if new_arabic:
        content["exampleVerseArabic"] = new_arabic
    if new_ref or new_arabic:
        # A hand-edited verse hasn't been through 15_reverify_example_verses.py's automated
        # word-boundary check - flagged honestly rather than left claiming a verification that
        # didn't happen. Also clears the now-stale highlight span, same as that script does.
        content["exampleVerseVerified"] = False
        content["arabicWordStart"] = None
        content["arabicWordEnd"] = None
        content["meaningHighlight"] = {}
        print("Updated. exampleVerseVerified reset to false (re-run 10_add_highlight_spans.py to recompute the highlight span).")
        return True
    print("No changes.")
    return False


def validate(exercises_doc):
    """Basic shape/required-field checks - not a replacement for the app's own
    GeneratedContentParsesTest (which decodes through the real Kotlin serializers), but a fast
    local sanity check before that runs."""
    errors = []
    seen_word_ids = set()
    for ex in exercises_doc["exercises"]:
        content = ex["content"]
        if content.get("type") != "word_intro":
            continue
        word_id = content.get("wordId", "<missing>")
        for field in REQUIRED_WORD_INTRO_FIELDS:
            if field not in content or content[field] in (None, ""):
                errors.append(f"{word_id}: missing or empty required field '{field}'")
        meaning = content.get("meaning", {})
        if "en" not in meaning or "bn" not in meaning:
            errors.append(f"{word_id}: meaning missing 'en' or 'bn' (required for every word)")
        seen_word_ids.add(word_id)

    if errors:
        print(f"\n{len(errors)} validation issue(s) found:")
        for e in errors[:50]:
            print(f"  - {e}")
        if len(errors) > 50:
            print(f"  ... and {len(errors) - 50} more")
    else:
        print(f"\nNo validation issues found across {len(seen_word_ids)} word_intro entries.")
    return len(errors) == 0


def print_finalize_reminder():
    content_version = None
    if CONTENT_SEEDER_PATH.exists():
        text = CONTENT_SEEDER_PATH.read_text(encoding="utf-8")
        for line in text.splitlines():
            if "CONTENT_VERSION" in line and "=" in line:
                content_version = line.strip()
                break
    print("\nBefore this ships:")
    print(f"  1. Bump CONTENT_VERSION in {CONTENT_SEEDER_PATH}")
    if content_version:
        print(f"     Current line: {content_version}")
    print("     (a value-only change still needs a bump - ContentSeeder gates the reseed on the")
    print("      version flag alone, not a per-row diff, so a forgotten bump means already-seeded")
    print("      devices never see the correction.)")
    print("  2. Run ./gradlew :app:testDebugUnitTest to confirm GeneratedContentParsesTest still passes.")
    print("  3. If you edited an example verse, consider re-running 10_add_highlight_spans.py.")


def main():
    exercises_doc = load_json(EXERCISES_PATH)
    word_freq_doc = load_json(WORD_FREQ_PATH)
    word_freq_by_id = {w["id"]: w for w in word_freq_doc["words"]}
    dirty = False

    print("QuranicWords content CMS - local, single-maintainer CLI (see docs/CONTENT_SOURCES.md)")
    while True:
        print("\n1) Look up a word\n2) Validate content\n3) Finalize / release checklist\n4) Save and quit\n5) Quit without saving")
        choice = input("> ").strip()

        if choice == "1":
            query = input("wordId or Arabic text to search for: ").strip()
            matches = find_word_intro(exercises_doc, query)
            if not matches:
                print("No matches.")
                continue
            if len(matches) > 1:
                print(f"{len(matches)} matches, showing the first 10:")
                for m in matches[:10]:
                    print(f"  {m['wordId']}: {m['arabicWord']}")
                continue
            content = matches[0]
            print_word(content)
            sub = input("Edit (m)eaning, edit (v)erse, or (b)ack? ").strip().lower()
            if sub == "m":
                dirty = edit_meaning(content, word_freq_by_id) or dirty
            elif sub == "v":
                dirty = edit_example_verse(content) or dirty

        elif choice == "2":
            validate(exercises_doc)

        elif choice == "3":
            print_finalize_reminder()

        elif choice == "4":
            if dirty:
                save_json(EXERCISES_PATH, exercises_doc)
                save_json(WORD_FREQ_PATH, word_freq_doc)
                print(f"Saved {EXERCISES_PATH} and {WORD_FREQ_PATH}.")
                print_finalize_reminder()
            else:
                print("No changes to save.")
            break

        elif choice == "5":
            if dirty:
                confirm = input("You have unsaved changes - really quit without saving? (y/N): ").strip().lower()
                if confirm != "y":
                    continue
            break

        else:
            print("Unrecognized choice.")


if __name__ == "__main__":
    main()
