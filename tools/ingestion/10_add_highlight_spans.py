# -*- coding: utf-8 -*-
"""
Post-processing pass adding word-highlight data to exercises_vocabulary.json's word_intro teach
steps, so the app can visually highlight the taught word within its example verse (Arabic side)
and the corresponding gloss within the translation across all supported languages.
"""
import json
import re
import unicodedata
from pathlib import Path

from arabic_utils import strip_diacritics, strip_diacritics_with_map, strip_definite_article

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"


def normalize_text(text: str) -> str:
    """Strip combining diacritics / accents (e.g. macron vowels) for robust matching."""
    return "".join(
        c for c in unicodedata.normalize("NFKD", text)
        if not unicodedata.combining(c)
    )


def find_meaning_highlight(meaning: str, translation: str) -> str | None:
    if not meaning or not translation:
        return None

    # 1. Clean parentheticals from meaning (e.g., "(relative pronoun)", "[i.e. Quran]")
    cleaned = re.sub(r"\(.*?\)|\[.*?\]", "", meaning).strip()
    if not cleaned:
        return None

    norm_trans = normalize_text(translation)

    # 2. Split alternatives on commas, slashes, semicolons, and " or "
    candidates = []
    candidates.append(cleaned)
    for part in re.split(r"[,;/|]|\bor\b", cleaned, flags=re.IGNORECASE):
        p = part.strip()
        if len(p) >= 2:
            candidates.append(p)

    candidates = sorted(set(candidates), key=len, reverse=True)

    # 3. Pass 1: Try full candidate phrase matches (word-boundary bounded)
    for cand in candidates:
        norm_cand = normalize_text(cand)
        if not norm_cand.strip():
            continue
        pattern = r"\b" + re.escape(norm_cand) + r"\b"
        m = re.search(pattern, norm_trans, re.IGNORECASE)
        if m:
            return translation[m.start():m.end()]

    # 4. Pass 2: Try individual content words from candidates (longest first)
    for cand in candidates:
        words = [w for w in re.findall(r"[\w']+", cand, flags=re.UNICODE) if len(w) >= 2]
        for w in sorted(words, key=len, reverse=True):
            norm_w = normalize_text(w)
            if not norm_w.strip():
                continue
            pattern = r"\b" + re.escape(norm_w) + r"\b"
            m = re.search(pattern, norm_trans, re.IGNORECASE)
            if m:
                return translation[m.start():m.end()]

    # 5. Pass 3: Non-word-boundary substring fallback for agglutinative/complex scripts (Bangla, etc.)
    for cand in candidates:
        norm_cand = normalize_text(cand)
        if len(norm_cand) >= 2:
            idx = norm_trans.lower().find(norm_cand.lower())
            if idx != -1:
                return translation[idx:idx + len(norm_cand)]

    return None


def find_arabic_span(arabic_word: str, verse_arabic: str):
    word_skeleton = strip_diacritics(arabic_word)
    word_skeleton_noarticle = strip_definite_article(word_skeleton)
    targets = {t for t in (word_skeleton, word_skeleton_noarticle) if t}
    if not targets:
        return None, None

    tokens = [(m.start(), m.group()) for m in re.finditer(r"\S+", verse_arabic)]
    token_skeletons = []
    for start, token in tokens:
        skeleton, _ = strip_diacritics_with_map(token)
        token_skeletons.append((start, token, skeleton))

    # Pass 1: exact skeleton match anywhere in the verse (most reliable).
    for start, token, skeleton in token_skeletons:
        if skeleton in targets:
            return start, start + len(token)

    # Pass 2: substring fallback for bound clitics - only tried once no exact match exists
    for start, token, skeleton in token_skeletons:
        if skeleton and any(t in skeleton for t in targets):
            return start, start + len(token)

    return None, None


def main():
    with open(EXERCISES_PATH, encoding="utf-8") as f:
        data = json.load(f)

    total = 0
    arabic_found = 0
    lang_found = {}

    for ex in data["exercises"]:
        content = ex["content"]
        if content.get("type") != "word_intro":
            continue
        total += 1

        start, end = find_arabic_span(content["arabicWord"], content["exampleVerseArabic"])
        if start is not None:
            content["arabicWordStart"] = start
            content["arabicWordEnd"] = end
            arabic_found += 1

        meaning_map = content.get("meaning", {})
        trans_map = content.get("exampleVerseTranslation", {})
        highlight = content.get("meaningHighlight", {})

        for lang, meaning_str in meaning_map.items():
            trans_str = trans_map.get(lang, "")
            hl = find_meaning_highlight(meaning_str, trans_str)
            if hl:
                highlight[lang] = hl
                lang_found[lang] = lang_found.get(lang, 0) + 1

        content["meaningHighlight"] = highlight

    with open(EXERCISES_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    print(f"word_intro entries processed: {total}")
    print(f"Arabic verse-word span found: {arabic_found} ({arabic_found * 100 // total}%)")
    for lang, count in sorted(lang_found.items()):
        print(f"Language [{lang}] highlight found: {count} ({count * 100 // total}%)")


if __name__ == "__main__":
    main()
