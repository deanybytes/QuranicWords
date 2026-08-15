"""
Shared Arabic text normalization for the ingestion pipeline.

Correctness note: an earlier version of this logic used a hand-rolled Unicode range regex
("[ؐ-ًؚ-ِْ-ٰٟۖ-ۭ]") to strip diacritics, which was wrong - the range boundary ؐ-ً (U+0610-U+064B)
overlaps almost the entire Arabic base-letter block (U+0621-U+064A), so it silently stripped real
consonants, not just diacritics (e.g. "فِى" skeletonized to "" instead of "في"). That produced
correct-looking results for some words by chance and silently wrong ones for others (an empty
skeleton is a trivial substring/subsequence match for anything). Fixed by classifying via
unicodedata.category() instead of guessing codepoint ranges by eye - category "Lo" (Letter,
other) is exactly the real Arabic base letters; everything else (Mn combining marks, Lm tatweel
and small recitation letters, So page/sajdah symbols) is discarded.
"""
import unicodedata

SHADDA = "ّ"

ALIF_VARIANTS = str.maketrans({
    "أ": "ا",  # hamza on alif -> plain alif
    "إ": "ا",  # hamza under alif -> plain alif
    "آ": "ا",  # alif madda -> plain alif
    "ٱ": "ا",  # alif wasla -> plain alif
})

def strip_diacritics(s: str) -> str:
    """Reduce Arabic text to its bare consonant/long-vowel-letter skeleton. Shadda doubles the
    preceding consonant (gemination) rather than being discarded, since that's a root-bearing
    distinction (e.g. "Rabb" -> "ربب", matching the geminated root ربب, not "رب")."""
    s = s.translate(ALIF_VARIANTS)
    out = []
    for ch in s:
        if ch == SHADDA:
            if out:
                out.append(out[-1])
            continue
        if unicodedata.category(ch) == "Lo":
            out.append(ch)
    return "".join(out)

def strip_definite_article(consonants: str) -> str:
    if consonants.startswith("ال") and len(consonants) > 3:
        return consonants[2:]
    return consonants

def strip_diacritics_with_map(s: str):
    """Same skeleton reduction as [strip_diacritics], but also returns a parallel list mapping
    each output character back to its index in the original string [s] - so a match found in the
    skeleton can be mapped back to a char offset range in the original (diacriticized) text.
    `str.translate` with a single-char-to-single-char table is length/position preserving, so only
    the "drop non-letter chars" step needs explicit index tracking."""
    s2 = s.translate(ALIF_VARIANTS)
    out_chars = []
    out_indices = []
    for i, ch in enumerate(s2):
        if ch == SHADDA:
            if out_chars:
                out_chars.append(out_chars[-1])
                out_indices.append(i)
            continue
        if unicodedata.category(ch) == "Lo":
            out_chars.append(ch)
            out_indices.append(i)
    return "".join(out_chars), out_indices
