"""Arabic text helpers: comparison skeletons, light normalization and transliteration."""
import re
import unicodedata

FATHA, DAMMA, KASRA = "َ", "ُ", "ِ"
FATHATAN, DAMMATAN, KASRATAN = "ً", "ٌ", "ٍ"
SUKUN, SHADDA, DAGGER_ALIF = "ْ", "ّ", "ٰ"
QURANIC_SUKUN = "ۡ"
VOWELS = {FATHA: "a", DAMMA: "u", KASRA: "i"}
TANWEEN = {FATHATAN: "an", DAMMATAN: "un", KASRATAN: "in"}

_LETTER_FOLD = str.maketrans({
    "أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ئ": "ي", "ؤ": "و", "ء": "", "ة": "ه",
    "ی": "ي", "ک": "ك", "ۀ": "ه",
})


def nfc(s):
    return unicodedata.normalize("NFC", s or "")


def skeleton(s):
    """Base letters only, with alif/ya/hamza/ta-marbuta variants folded - for comparing the same
    word written in different Qur'anic orthographies (QAC vs GTAF vs Tanzil)."""
    s = nfc(s).translate(_LETTER_FOLD)
    return "".join(ch for ch in s if unicodedata.category(ch) == "Lo" and "ء" <= ch <= "ي")


def strip_marks(s):
    return "".join(ch for ch in nfc(s) if not unicodedata.category(ch).startswith("M"))


def light_normalize(s):
    """Keep letters and harakat (what a reciter/TTS actually voices) but drop recitation-only
    marks: Qur'anic small signs, tatweel, sukun spelling variants, waqf marks."""
    s = nfc(s).replace("ٱ", "ا").replace("ـ", "").replace(QURANIC_SUKUN, SUKUN)
    s = "".join(ch for ch in s if not ("ۖ" <= ch <= "ۭ") and ch not in "۝۞۩")
    s = s.replace(SUKUN, "")
    return re.sub(r"\s+", " ", s).strip()


def clean_display(s):
    """Lemma display form: NFC, no leading combining mark, no tatweel/whitespace noise."""
    s = nfc(s).replace("ـ", "").strip()
    while s and unicodedata.category(s[0]).startswith("M"):
        s = s[1:]
    return s


_CONS = {
    "ء": "ʾ", "أ": "ʾ", "إ": "ʾ", "ؤ": "ʾ", "ئ": "ʾ", "ب": "b", "ت": "t", "ث": "th", "ج": "j",
    "ح": "ḥ", "خ": "kh", "د": "d", "ذ": "dh", "ر": "r", "ز": "z", "س": "s", "ش": "sh", "ص": "ṣ",
    "ض": "ḍ", "ط": "ṭ", "ظ": "ẓ", "ع": "ʿ", "غ": "gh", "ف": "f", "ق": "q", "ك": "k", "ل": "l",
    "م": "m", "ن": "n", "ه": "h", "و": "w", "ي": "y",
}
_MARKS = set(VOWELS) | set(TANWEEN) | {SUKUN, SHADDA, DAGGER_ALIF, QURANIC_SUKUN}


_SPECIAL = {nfc(k): v for k, v in {
    "اللَّه": "Allāh", "اللّٰه": "Allāh", "اللَّهُمَّ": "Allāhumma",
    "أُولٰئِك": "ulāʾika", "أُولٰٓئِكَ": "ulāʾika", "أُولُوا": "ulū", "أُولِي": "ulī",
}.items()}


def transliterate(word):
    """Simplified academic transliteration (ā ī ū ʿ ʾ ḥ ṣ ḍ ṭ ẓ th kh dh sh gh) of a vocalized
    lemma. Deterministic and Latin-only by construction."""
    if nfc(word) in _SPECIAL:
        return _SPECIAL[nfc(word)]
    w = nfc(word).replace("\u0640", "")
    w = "".join(ch for ch in w if not ("ۖ" <= ch <= "ۭ") or ch == QURANIC_SUKUN)
    out = []
    chars = list(w)
    i = 0
    n = len(chars)

    def marks_after(j):
        m = []
        k = j + 1
        while k < n and chars[k] in _MARKS:
            m.append(chars[k])
            k += 1
        return m, k

    prev_vowel = None
    while i < n:
        ch = chars[i]
        if ch == " ":
            out.append(" ")
            prev_vowel = None
            i += 1
            continue
        if ch in _MARKS:
            i += 1
            continue
        marks, nxt = marks_after(i)
        vowel = next((VOWELS[m] for m in marks if m in VOWELS), None)
        tanween = next((TANWEEN[m] for m in marks if m in TANWEEN), None)
        doubled = SHADDA in marks
        dagger = DAGGER_ALIF in marks
        at_start = not out or out[-1] == " "

        if ch in "اٱ":
            if at_start:
                # Article "al-" or a bare initial vowel carrier.
                if nxt < n and chars[nxt] == "ل" and (nxt + 1 < n) and SHADDA not in marks_after(nxt)[0] \
                        and not any(m in VOWELS for m in marks_after(nxt)[0]):
                    out.append("al-")
                    i = nxt + 1
                    prev_vowel = None
                    continue
                # Hamzat al-wasl is voiced "a" at the start of a word (e.g. alladhī).
                v = vowel or "a"
                out.append(v)
                prev_vowel = v
            elif prev_vowel == "a" or prev_vowel is None:
                if out and out[-1] == "a":
                    out[-1] = "ā"
                elif not (out and out[-1] == "ā"):
                    out.append("ā")
                prev_vowel = "ā"
            if tanween:
                out.append("an")
            i = nxt
            continue
        if ch == "آ":
            out.append("ā" if at_start else "ʾā")
            prev_vowel = "ā"
            i = nxt
            continue
        if ch == "ى":
            if out and out[-1] == "a":
                out[-1] = "ā"
            elif not (out and out[-1] in "āī"):
                out.append("ā")
            prev_vowel = "ā"
            i = nxt
            continue
        if ch == "ة":
            final = nxt >= n or chars[nxt] == " "
            if final:
                out.append("h" if out and out[-1] in ("a", "ā") else "ah")
            else:
                out.append("t" + vowel if vowel else "t")
            prev_vowel = None
            i = nxt
            continue
        if ch in "وي" and dagger and not vowel:
            # Qur'anic spelling of a long ā on a wāw/yā' seat (e.g. ṣalāh, zakāh).
            if out and out[-1] == "a":
                out[-1] = "ā"
            else:
                out.append("ā")
            prev_vowel = "ā"
            i = nxt
            continue
        if ch in "وي" and not vowel and not doubled and not tanween and SUKUN not in marks \
                and QURANIC_SUKUN not in marks and prev_vowel in ("u", "i"):
            long_ = "ū" if ch == "و" else "ī"
            if (ch == "و" and prev_vowel == "u") or (ch == "ي" and prev_vowel == "i"):
                out[-1] = long_
                prev_vowel = long_
                i = nxt
                continue
        if ch in ("أ", "إ") and at_start:
            v = vowel or ("i" if ch == "إ" else "a")
            out.append(v)
            prev_vowel = v
            i = nxt
            continue
        cons = _CONS.get(ch)
        if cons is None:
            i = nxt
            continue
        out.append(cons + cons if doubled and cons != "ʾ" else cons)
        if vowel:
            out.append(vowel)
            prev_vowel = vowel
        elif tanween:
            out.append(tanween)
            prev_vowel = None
        else:
            prev_vowel = None
        if dagger:
            if out and out[-1] == "a":
                out[-1] = "ā"
            else:
                out.append("ā")
            prev_vowel = "ā"
        i = nxt
    text = "".join(out).replace("--", "-").strip()
    # "allāh" / "al-lāh" readability: collapse the article before a sun letter only stays literal.
    return text


def pronunciation_key(s):
    """Two spellings get the same key only if they are read identically: Uthmani vs standard
    orthography (dagger alif vs alif, alif wasla, final ى/ي after kasra, redundant fatha before
    alif / kasra before yā' / ḍamma before wāw) - never a difference a reciter would voice."""
    s = nfc(s).replace("ٱ", "ا").replace("ـ", "").replace("ٓ", "").replace(QURANIC_SUKUN, SUKUN)
    s = "".join(ch for ch in s if not ("ۖ" <= ch <= "ۭ"))
    s = s.replace(FATHA + DAGGER_ALIF, "ا").replace(DAGGER_ALIF, "ا")
    s = s.replace(KASRA + "ى", KASRA + "ي")
    s = s.replace(SUKUN, "")
    s = re.sub(FATHA + "(?=ا)", "", s)
    s = re.sub(KASRA + "(?=ي(?![" + FATHA + DAMMA + KASRA + SHADDA + "]))", "", s)
    s = re.sub(DAMMA + "(?=و(?![" + FATHA + DAMMA + KASRA + SHADDA + "]))", "", s)
    # A shadda on the very first letter (left over from a stripped article, ٱلرَّحْمَٰن) is not voiced.
    s = re.sub("^(.)" + SHADDA, r"\1", s.strip())
    return s
