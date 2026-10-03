"""
Pick one example verse per lemma:
  - If the lemma has a matched root, use one of the root's own example verses (Quran-bil-Quran's
    own curated occurrence list) - prefer an early, well-known verse (lowest surah:ayah) for
    learnability.
  - If not (particles/pronouns with no root - the majority of the highest-frequency band), search
    verses_text.json directly for a verse whose diacritic-stripped text actually contains the
    lemma's consonant skeleton as a substring, so the example verse genuinely contains the word,
    not a guess.
Then attach the verse's Arabic text, English translation (en_sahih.json), and Bangla translation
(risan/quran-json's quran_bn.json, restructured into a surah:ayah lookup).
"""
import json
from pathlib import Path

from arabic_utils import strip_diacritics

RAW = Path(__file__).parent / "raw"
OUT_DIR = Path(__file__).parent / "output"
IN_PATH = OUT_DIR / "lemmas_final.json"
OUT_PATH = OUT_DIR / "lemmas_with_verses.json"

def surah_ayah_key(ref: str):
    s, a = ref.split(":")
    return (int(s), int(a))

def main():
    with open(IN_PATH, encoding="utf-8") as f:
        lemmas = json.load(f)
    with open(RAW / "verses_text.json", encoding="utf-8") as f:
        verses_ar = json.load(f)
    with open(RAW / "en_sahih.json", encoding="utf-8") as f:
        verses_en = json.load(f)
    with open(RAW / "quran_bn.json", encoding="utf-8") as f:
        chapters_bn = json.load(f)

    verses_bn = {}
    for chapter in chapters_bn:
        surah_id = chapter["id"]
        for verse in chapter["verses"]:
            verses_bn[f"{surah_id}:{verse['id']}"] = verse["translation"]

    # Tokenize per verse (word-boundary aware) rather than treating each verse as one giant
    # concatenated string - a raw substring search across the whole verse text produces false
    # positives for short lemmas (e.g. "مِن" ["min"] is a coincidental substring of "ٱلرَّحْمَـٰنِ"
    # ["ar-Rahman"], since that word's skeleton happens to end in the same two letters - not a
    # real occurrence of the standalone word "min" at all).
    verses_tokens = {
        ref: [strip_diacritics(tok) for tok in text.split(" ")]
        for ref, text in verses_ar.items()
    }

    found_via_root = 0
    found_via_exact_word = 0
    found_via_substring_fallback = 0
    not_found = 0

    for l in lemmas:
        ref = None
        if l["rootExampleVerses"]:
            ref = min(l["rootExampleVerses"], key=surah_ayah_key)
            found_via_root += 1
        else:
            skeleton = strip_diacritics(l["arabic"])
            for vref, tokens in verses_tokens.items():
                if skeleton in tokens:
                    ref = vref
                    break
            if ref:
                found_via_exact_word += 1
            else:
                # Fallback for bound clitics that never appear as a standalone token (e.g. a
                # proclitic conjunction/preposition prefixed directly onto its host word with no
                # space) - a same-verse-text substring is the best available evidence, less
                # precise than an exact token match but still a real occurrence of those letters.
                for vref, tokens in verses_tokens.items():
                    if any(skeleton in tok for tok in tokens):
                        ref = vref
                        break
                if ref:
                    found_via_substring_fallback += 1
                else:
                    not_found += 1

        if ref and ref in verses_ar and ref in verses_en:
            l["exampleVerseReference"] = ref
            l["exampleVerseArabic"] = verses_ar[ref]
            l["exampleVerseTranslationEn"] = verses_en[ref]
            l["exampleVerseTranslationBn"] = verses_bn.get(ref, "")
        else:
            l["exampleVerseReference"] = None
            l["exampleVerseArabic"] = None
            l["exampleVerseTranslationEn"] = None
            l["exampleVerseTranslationBn"] = None

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(lemmas, f, ensure_ascii=False, indent=2)

    print(f"Found via root's own verse list: {found_via_root}")
    print(f"Found via exact word-token match: {found_via_exact_word}")
    print(f"Found via substring fallback (bound clitic): {found_via_substring_fallback}")
    print(f"Not found at all: {not_found}")
    print(f"Wrote {OUT_PATH}")

    missing = [l for l in lemmas if l["exampleVerseReference"] is None]
    for l in missing[:20]:
        print("MISSING:", l["rank"], l["arabic"], l["pos"])

if __name__ == "__main__":
    main()
