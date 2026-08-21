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

Translation side: no open-licensed word-alignment dataset was found for Quranic Arabic <-> English/
Bangla (checked corpus.quran.com's own word-by-word view and quranwbw.com - both plausible
candidates, neither had a license confirmed for this use; see docs/CONTENT_SOURCES.md). This
stays a best-effort heuristic: for English, the full gloss (stopwords stripped) as a contiguous
phrase first, falling back to the longest single content word (tried exact, then with light
suffix-tolerance - "believe" in the gloss can match "believing" in the translation); for Bangla,
the longest single content word only (no stemmer available). No match -> left null. Real,
honestly-partial coverage, printed at the end rather than assumed.

Operates on the current LocalizedText map-based schema (content["meaning"]["en"]/["bn"],
content["exampleVerseTranslation"]["en"]/["bn"], content["meaningHighlight"]["en"]/["bn"]) - a
prior version of this script used the pre-refactor flat field names (meaningEn/meaningBn/etc.)
and silently went stale when Phase 3's map-based-localization refactor landed; fixed alongside the
QW-18 re-verification pass that first surfaced it (running this script is the only way to notice,
since it would otherwise KeyError instead of quietly corrupting data - still worth flagging so a
future schema change doesn't repeat it unnoticed for as long).
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


# Light stemming (en-only; no Bangla stemmer available in-repo) so a gloss word like "believe"
# still cross-matches a differently-inflected translation word like "believing"/"believed" -
# rather than trying to enumerate every English suffix (irregular spelling changes like the
# silent-e drop before "-ing" make that fragile), take a shortening set of prefixes of the word
# itself (down to a 4-character floor, short enough to catch real inflection, long enough to stay
# a meaningfully specific match) and search each as a whole-word-start anchor. Still a single
# content word, still whole-word-bounded - just inflection-tolerant, not a fuzzy/approximate match.
def _en_word_stems(word: str):
    stems = []
    for cut in range(1, len(word) - 3):
        stem = word[:-cut]
        if len(stem) >= 4:
            stems.append(stem)
    return stems


def find_en_highlight(meaning_en: str, translation_en: str):
    if not meaning_en or not translation_en:
        return None
    # Pass 1: the full gloss (stopwords stripped) as a contiguous phrase - catches multi-word
    # idioms like "the straight path" that pass 2's single-longest-word approach can only
    # partially match.
    phrase_words = [w for w in re.findall(r"[A-Za-z']+", meaning_en.lower()) if w not in EN_STOPWORDS]
    if len(phrase_words) > 1:
        phrase_pattern = r"\b" + r"\s+".join(re.escape(w) for w in phrase_words) + r"\b"
        m = re.search(phrase_pattern, translation_en, re.IGNORECASE)
        if m:
            return translation_en[m.start():m.end()]
    # Pass 2: longest single content word, tried as an exact whole word first, then - only if
    # that fails - as a stemmed prefix (so "believe" in the gloss can match "believing" in the
    # translation, still whole-word-bounded, just inflection-tolerant).
    for word in content_words_en(meaning_en):
        m = re.search(r"\b" + re.escape(word) + r"\b", translation_en, re.IGNORECASE)
        if m:
            return translation_en[m.start():m.end()]
        for stem in _en_word_stems(word):
            m = re.search(r"\b" + re.escape(stem) + r"[a-z']*\b", translation_en, re.IGNORECASE)
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

        meaning = content.get("meaning", {})
        translation = content.get("exampleVerseTranslation", {})
        highlight = content.get("meaningHighlight", {})

        en_highlight = find_en_highlight(meaning.get("en", ""), translation.get("en", ""))
        if en_highlight:
            highlight["en"] = en_highlight
            en_found += 1

        bn_highlight = find_bn_highlight(meaning.get("bn", ""), translation.get("bn", ""))
        if bn_highlight:
            highlight["bn"] = bn_highlight
            bn_found += 1

        content["meaningHighlight"] = highlight

    with open(EXERCISES_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    print(f"word_intro entries processed: {total}")
    print(f"Arabic verse-word span found: {arabic_found} ({arabic_found * 100 // total}%)")
    print(f"English translation highlight found: {en_found} ({en_found * 100 // total}%)")
    print(f"Bangla translation highlight found: {bn_found} ({bn_found * 100 // total}%)")


if __name__ == "__main__":
    main()
