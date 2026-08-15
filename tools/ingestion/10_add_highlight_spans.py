# -*- coding: utf-8 -*-
"""
Post-processing pass adding word-highlight data to exercises_vocabulary.json's word_intro teach
steps, so the app can visually highlight the taught word within its example verse (Arabic side)
and, best-effort, the corresponding gloss within the translation (English/Bangla side).

Arabic side: reuses the same diacritic-normalized, token-boundary-aware matching approach
06_pick_verses.py already used to pick verses (exact skeleton match first across every token in
the verse, substring fallback for bound clitics only if no exact match exists anywhere - the same
two-pass structure that avoids short skeletons false-matching inside unrelated longer words, e.g.
"min" inside "ar-Rahman"), extended to keep a position map so a skeleton match can be reported
back as a char-offset range in the ORIGINAL (diacriticized) exampleVerseArabic string. Whole-token
granularity (not sub-token), matching this app's existing word-level highlighting granularity
elsewhere. No confident match -> both offsets left null, never guessed.

Translation side: no word-alignment data exists anywhere in the sourced data (translations are
full idiomatic sentences from Sahih International / risan's Bangla set). This is a best-effort
heuristic only: the longest content word from meaningEn/meaningBn that also appears as a whole
word in the corresponding translation. No match -> left null. Real, honestly-partial coverage,
printed at the end rather than assumed.
"""
import json
import re
from pathlib import Path

from arabic_utils import strip_diacritics, strip_diacritics_with_map, strip_definite_article

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
EXERCISES_PATH = CONTENT_DIR / "exercises_vocabulary.json"

EN_STOPWORDS = {
    "a", "an", "the", "of", "to", "in", "on", "at", "by", "for", "with", "and", "or", "is", "are",
    "was", "were", "be", "as", "that", "this", "it", "its", "see", "example", "context", "verse",
    "word", "meaning", "sense", "particle", "noun", "verb", "who", "which", "one", "also", "not",
}


def content_words_en(meaning: str):
    words = re.findall(r"[A-Za-z']+", meaning.lower())
    return sorted({w for w in words if w not in EN_STOPWORDS and len(w) > 2}, key=len, reverse=True)


def content_words_bn(meaning: str):
    # Bangla has no diacritic-stripping concern like Arabic; whitespace tokenization is enough.
    words = re.split(r"[\s,;।()]+", meaning.strip())
    return sorted({w for w in words if len(w) > 1}, key=len, reverse=True)


def find_en_highlight(meaning_en: str, translation_en: str):
    for word in content_words_en(meaning_en):
        m = re.search(r"\b" + re.escape(word) + r"\b", translation_en, re.IGNORECASE)
        if m:
            return translation_en[m.start():m.end()]
    return None


def find_bn_highlight(meaning_bn: str, translation_bn: str):
    for word in content_words_bn(meaning_bn):
        idx = translation_bn.find(word)
        if idx != -1:
            return translation_bn[idx:idx + len(word)]
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
    # anywhere in this verse, mirroring 06_pick_verses.py's own two-pass approach.
    for start, token, skeleton in token_skeletons:
        if skeleton and any(t in skeleton for t in targets):
            return start, start + len(token)

    return None, None


def main():
    with open(EXERCISES_PATH, encoding="utf-8") as f:
        data = json.load(f)

    total = 0
    arabic_found = 0
    en_found = 0
    bn_found = 0

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

        en_highlight = find_en_highlight(content["meaningEn"], content["exampleVerseTranslationEn"])
        if en_highlight:
            content["meaningHighlightEn"] = en_highlight
            en_found += 1

        bn_highlight = find_bn_highlight(content["meaningBn"], content["exampleVerseTranslationBn"])
        if bn_highlight:
            content["meaningHighlightBn"] = bn_highlight
            bn_found += 1

    with open(EXERCISES_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    print(f"word_intro entries processed: {total}")
    print(f"Arabic verse-word span found: {arabic_found} ({arabic_found * 100 // total}%)")
    print(f"English translation highlight found: {en_found} ({en_found * 100 // total}%)")
    print(f"Bangla translation highlight found: {bn_found} ({bn_found * 100 // total}%)")


if __name__ == "__main__":
    main()
