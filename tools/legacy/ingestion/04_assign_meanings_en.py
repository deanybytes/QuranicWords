"""
Assign meaningEn to every lemma:
  1. Curated top-37 overrides (already hand-verified) - use as-is.
  2. Root-matched entries - derive a short gloss from the root's descriptive text (first clause,
     truncated at a sentence boundary) plus the POS label. This is a mechanical extraction from
     Quran-bil-Quran's own sourced text, not an invented definition - but it describes the
     shared ROOT's core concept, not necessarily this exact lemma's precise sense (documented in
     docs/CONTENT_SOURCES.md).
  3. Unmatched, non-curated entries - POS-grounded fallback naming what's known (the part of
     speech) without asserting a specific sense we don't have a source for.
"""
import json
import re
from pathlib import Path

IN_PATH = Path(__file__).parent / "output" / "lemmas_matched.json"
OUT_PATH = Path(__file__).parent / "output" / "lemmas_with_meanings.json"

POS_FALLBACK_EN = {
    "Preposition": "a preposition (see the example verse for context)",
    "Proper noun": "a proper name",
    "Relative pronoun": "a relative pronoun (\"who\"/\"which\"/\"that\")",
    "Negative particle": "a negation particle (\"not\")",
    "Conditional particle": "a conditional particle (\"if\")",
    "Subordinating conjunction": "a subordinating conjunction (\"that\")",
    "Restriction particle": "a restriction particle (\"except\"/\"only\")",
    "Demonstrative pronoun": "a demonstrative pronoun (\"this\"/\"that\")",
    "Particle of certainty": "a particle of certainty (\"indeed\")",
    "Time adverb": "a time adverb (\"when\")",
    "Accusative particle": "an emphasis particle (\"indeed\"/\"that\")",
    "Coordinating conjunction": "a coordinating conjunction",
    "Prohibition particle": "a prohibition particle (\"do not\")",
    "Location adverb": "a location adverb",
    "Interogative particle": "a question particle",
    "Subordinating conjunction": "a subordinating conjunction",
    "Vocative particle": "a vocative particle (\"O...\")",
    "Exceptive particle": "an exceptive particle (\"except\")",
    "Imperative verbal noun": "an imperative verbal noun",
    "Verb": "a verb (see the example verse for context)",
    "Noun": "a noun (see the example verse for context)",
    "Adjective": "a descriptive adjective (see the example verse for context)",
    "Personal pronoun": "a personal pronoun",
}

SENTENCE_SPLIT = re.compile(r"(?<=[.!?])\s+")

def short_gloss_from_root(root_meaning: str) -> str:
    first = SENTENCE_SPLIT.split(root_meaning.strip())[0]
    if len(first) > 140:
        first = first[:140].rsplit(" ", 1)[0] + "..."
    return first

def main():
    with open(IN_PATH, encoding="utf-8") as f:
        lemmas = json.load(f)

    counts = {"curated": 0, "root": 0, "fallback": 0}
    for l in lemmas:
        if l["curated"]:
            l["meaningEn"] = l["meaningEnCurated"]
            l["meaningSource"] = "curated"
            counts["curated"] += 1
        elif l["root"]:
            gloss = short_gloss_from_root(l["rootMeaning"])
            l["meaningEn"] = gloss
            l["meaningSource"] = "root-derived"
            counts["root"] += 1
        else:
            fallback = POS_FALLBACK_EN.get(l["pos"], f"a {l['pos'].lower()} (see the example verse for context)")
            l["meaningEn"] = fallback
            l["meaningSource"] = "pos-fallback"
            counts["fallback"] += 1

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(lemmas, f, ensure_ascii=False, indent=2)

    print("meaningEn source breakdown:", counts)
    print(f"Wrote {OUT_PATH}")
    # spot check across bands
    for band in ["0-25", "25-50", "50-75", "75-100"]:
        sample = next(l for l in lemmas if l["coverageBand"] == band)
        print(f"[{band}] {sample['arabic']} ({sample['pos']}) -> {sample['meaningEn']!r} [{sample['meaningSource']}]")

if __name__ == "__main__":
    main()
