"""Exact character span of one QAC segment (the taught word) inside the GTAF Uthmani token it is
part of, e.g. only مَا inside وَمَا, only وَ inside وَمِنَ. Segments are laid onto the token's base
letters in order (prefixes from the start, suffixes from the end); a span is accepted only if
its letters equal the segment's letters - otherwise the occurrence is not used as an example."""
import unicodedata



_FOLD = str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ئ": "ي", "ؤ": "و", "ة": "ه", "ی": "ي", "ک": "ك"})


def letter_key(s, fold_madda=False):
    """Base letters only (hamza kept as a letter), orthographic variants folded. With
    [fold_madda], QAC's hamza + alif spelling of a madda (ءَا) counts as the single alif-with-madda
    letter the mushaf writes (أٓ); tried only when the plain count doesn't line up."""
    s = unicodedata.normalize("NFC", s or "").translate(_FOLD)
    s = "".join(ch for ch in s if unicodedata.category(ch) == "Lo" and ch != "\u0640")
    return s.replace("ءا", "ا") if fold_madda else s


def _letters(token):
    """Indices of base letters in the token, each extended over its following combining marks."""
    out = []
    for i, ch in enumerate(token):
        cat = unicodedata.category(ch)
        if cat == "Lo" and not ("ۥ" <= ch <= "ۦ") and ch != "ـ":
            out.append([i, i + 1])
        elif out:
            out[-1][1] = i + 1
    return out


def segment_span(token, segments, target_index, last_index=None):
    """(start, end) of segments[target_index..last_index] within token, or None when it can't be
    proven letter by letter."""
    last_index = target_index if last_index is None else last_index
    for fold in (False, True):
        span = _span(token, segments, target_index, last_index, fold)
        if span:
            return span
    return None


def _span(token, segments, first_seg, last_seg, fold):
    letters = _letters(token)
    sk = [letter_key(s.form, fold) for s in segments]
    counts = [len(k) for k in sk]
    wanted = "".join(sk[first_seg:last_seg + 1])
    if sum(counts) != len(letters) or not wanted:
        return None
    first = sum(counts[:first_seg])
    last = first + len(wanted) - 1
    start, end = letters[first][0], letters[last][1]
    piece = token[start:end]
    if letter_key(piece, fold) != wanted and letter_key(piece, True) != letter_key("".join(s.form for s in segments[first_seg:last_seg + 1]), True):
        return None
    return start, end
