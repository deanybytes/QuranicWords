"""
Best-effort match each lemma to a Quran-bil-Quran root entry, using the lemma's own Arabic
script (diacritics stripped) as a consonant-skeleton subsequence check against each root's
Arabic key. This is explicitly best-effort, not authoritative - see docs/CONTENT_SOURCES.md.
"""
import json
from pathlib import Path

from arabic_utils import strip_diacritics, strip_definite_article

LEMMAS_PATH = Path(__file__).parent / "output" / "lemmas.json"
ROOTS_PATH = Path(__file__).parent / "raw" / "roots_index.json"
OUT_PATH = Path(__file__).parent / "output" / "lemmas_matched.json"

def is_subsequence(needle: str, haystack: str) -> bool:
    if not needle:
        return False
    it = iter(haystack)
    return all(ch in it for ch in needle)

def best_root_match(lemma_consonants: str, roots: dict):
    candidates = []
    search_forms = {lemma_consonants, strip_definite_article(lemma_consonants)}
    for root_key, root_data in roots.items():
        root_consonants = strip_diacritics(root_key)
        if len(root_consonants) < 2:
            continue
        for form in search_forms:
            if is_subsequence(root_consonants, form):
                extra = len(form) - len(root_consonants)
                candidates.append((extra, root_key, root_data))
                break
    if not candidates:
        return None
    candidates.sort(key=lambda c: c[0])
    return candidates[0]

def main():
    with open(LEMMAS_PATH, encoding="utf-8") as f:
        lemmas = json.load(f)
    with open(ROOTS_PATH, encoding="utf-8") as f:
        roots = json.load(f)

    matched_count = 0
    for lemma in lemmas:
        consonants = strip_diacritics(lemma["arabic"])
        match = best_root_match(consonants, roots)
        if match:
            extra, root_key, root_data = match
            lemma["root"] = root_key
            lemma["rootMeaning"] = root_data["m"]
            lemma["rootExampleVerses"] = root_data["v"]
            matched_count += 1
        else:
            lemma["root"] = None
            lemma["rootMeaning"] = None
            lemma["rootExampleVerses"] = []

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(lemmas, f, ensure_ascii=False, indent=2)

    print(f"Matched {matched_count} / {len(lemmas)} lemmas to a root ({matched_count/len(lemmas)*100:.1f}%)")
    print(f"Wrote {OUT_PATH}")

    for l in lemmas[:15]:
        print(l["rank"], l["arabic"], "->", l["root"], f"(pos={l['pos']})")

if __name__ == "__main__":
    main()
