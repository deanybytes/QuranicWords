"""Audit: for every word, do all its scattered meaning copies (word_frequency.json,
word_intro, multiple_choice's correct option, matching pairs, word_in_verse_tap) actually agree?

Why this runs BEFORE trusting word_frequency.json as the single canonical source (see
LessonViewModel.resolveCanonicalMeaning / rebuildOptions): word_frequency.json is not guaranteed
to hold the *correct* value just because it's the row we're about to treat as canonical. The
wf_10 (إِلَىٰ, "to/towards") case proved this: word_frequency/word_intro/multiple_choice/matching
all agreed on the Bengali gloss "সাথে" ("with") - wrong - while word_in_verse_tap alone, never
touched by the review pipeline, carried the correct "দিকে, প্রতি" ("towards"). A blind
sync-from-word_frequency would have frozen in the wrong value and *discarded* the one correct
copy. This script finds every such disagreement so they can be reviewed/fixed before that sync
runs (see 18_sync_meaning_from_canonical.py).

Compares en/bn only (the two languages with any human/gtaf review per CLAUDE.md; the other 10
are machine translations of these two, so an en/bn disagreement is the actual signal - a
same-en/bn-but-different-other-language row is not a meaningful conflict for this audit).

Output: a CSV-ish report to stdout, one row per (wordId, copySite) whose en or bn text differs
from word_frequency.json's value for that word, plus a summary count. Read-only - never writes.
"""
import json
from pathlib import Path

CONTENT_DIR = Path(__file__).resolve().parent.parent.parent / "app/src/main/assets/content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"
WORD_FREQ_PATH = CONTENT_DIR / "word_frequency.json"


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def en_bn(meaning: dict) -> tuple[str, str]:
    return (meaning.get("en", "").strip(), meaning.get("bn", "").strip())


def main():
    wf_data = load(WORD_FREQ_PATH)
    words = {w["id"]: w for w in wf_data["words"]}

    ex_data = load(EXERCISES_PATH)
    exercises = ex_data["exercises"]

    conflicts = []  # (wordId, copySite, exerciseId, canonicalEnBn, copyEnBn)

    for e in exercises:
        c = e.get("content", {})
        ctype = c.get("type")

        if ctype == "word_intro":
            wid = c["wordId"]
            canon = words.get(wid)
            if canon and en_bn(c["meaning"]) != en_bn(canon["meaning"]):
                conflicts.append((wid, "word_intro", e["id"], en_bn(canon["meaning"]), en_bn(c["meaning"])))

        elif ctype == "multiple_choice":
            wid = c.get("wordId")
            canon = words.get(wid)
            if not canon:
                continue
            correct = next((o for o in c["options"] if o["id"] == c["correctOptionId"]), None)
            if correct and en_bn(correct.get("label", {})) != en_bn(canon["meaning"]):
                conflicts.append((wid, "multiple_choice.correctOption", e["id"], en_bn(canon["meaning"]), en_bn(correct.get("label", {}))))

        elif ctype == "matching":
            for pair in c["pairs"]:
                wid = pair.get("wordId")
                canon = words.get(wid) if wid else None
                if canon and en_bn(pair["right"]) != en_bn(canon["meaning"]):
                    conflicts.append((wid, "matching.pair", e["id"], en_bn(canon["meaning"]), en_bn(pair["right"])))

        elif ctype == "word_in_verse_tap":
            wid = c["wordId"]
            canon = words.get(wid)
            if canon and en_bn(c["meaning"]) != en_bn(canon["meaning"]):
                conflicts.append((wid, "word_in_verse_tap", e["id"], en_bn(canon["meaning"]), en_bn(c["meaning"])))

    # Group by wordId so each word's full disagreement picture is visible together.
    by_word = {}
    for wid, site, ex_id, canon_val, copy_val in conflicts:
        by_word.setdefault(wid, []).append((site, ex_id, canon_val, copy_val))

    print(f"Total conflicting copies: {len(conflicts)}")
    print(f"Distinct words with at least one conflict: {len(by_word)}")
    print()

    out_path = Path(__file__).parent / "output" / "meaning_conflicts.tsv"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("wordId\tarabicWord\tsite\texerciseId\tcanonical_en\tcanonical_bn\tcopy_en\tcopy_bn\n")
        for wid, rows in sorted(by_word.items(), key=lambda kv: -len(kv[1])):
            arabic = words.get(wid, {}).get("arabicWord", "")
            for site, ex_id, canon_val, copy_val in rows:
                f.write(f"{wid}\t{arabic}\t{site}\t{ex_id}\t{canon_val[0]}\t{canon_val[1]}\t{copy_val[0]}\t{copy_val[1]}\n")

    print(f"Full report written to {out_path}")
    print()
    print("Top 20 words by conflict count:")
    for wid, rows in sorted(by_word.items(), key=lambda kv: -len(kv[1]))[:20]:
        print(f"  {wid}: {len(rows)} conflicting copies")


if __name__ == "__main__":
    main()
