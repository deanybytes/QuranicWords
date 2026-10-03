"""Per-language senses with proven examples.

For every word and every language, each sense shown to the learner is a meaning the GTAF
word-by-word translation gives *for this very word in this very verse*. Each sense carries:

- the complete verse, with only the taught word highlighted (the QAC segment inside the written
  token - e.g. just مَا in وَمَا);
- the verse's word-by-word line in that language, with exactly the sense text highlighted inside
  this word's own gloss;
- the full translation, highlighted only when the sense text occurs in it exactly once and no
  other word of the verse is glossed with it - otherwise it is shown unhighlighted rather than
  wrongly highlighted.

Nothing here is inferred: a sense exists only if such an example exists."""
import re
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass

from . import config, glosses, spans

MAX_SENSES = 3
# A second/third sense must be well attested, not a one-off contextual rendering.
MIN_SENSE_SHARE = 0.15
MIN_SENSE_COUNT = 2

# Latin-script languages separate words with spaces and inflect less: match whole words only.
WHOLE_WORD = {"en", "fr", "in", "tr"}


@dataclass
class Example:
    surah: int
    ayah: int
    word: int            # 1-based token index
    word_start: int      # span of the taught word inside the verse's Arabic text
    word_end: int
    wbw: str             # word-by-word line for the language
    wbw_start: int
    wbw_end: int
    translation: str
    tr_start: int = None
    tr_end: int = None


@dataclass
class Sense:
    meaning: str
    example: Example
    support: int


def fold(text, lang):
    """Case-insensitive comparison key; Turkish pairs İ/i and I/ı."""
    if lang == "tr":
        text = text.replace("İ", "i").replace("I", "ı")
    return text.casefold()


def norm(text, lang):
    return re.sub(r"\s+", " ", glosses.normalize_lang(unicodedata.normalize("NFC", text or ""), lang)).strip()


def _is_word_char(ch):
    """Letters, digits and combining marks (Bangla/Hindi vowel signs, Arabic harakat) continue a
    word; punctuation such as the Urdu full stop ۔ does not."""
    return ch.isalnum() or unicodedata.category(ch).startswith("M") or ch in "_\u200c\u200d"


def find_all(needle, text, lang):
    """Non-overlapping case-insensitive whole-word occurrences of needle in text."""
    if not needle:
        return []
    key = (lambda t: t.replace("İ", "i").replace("I", "ı").lower()) if lang == "tr" else (lambda t: t.lower())
    hay, pin = key(text), key(needle)
    if len(hay) != len(text) or len(pin) != len(needle):
        hay, pin = text, needle
    out, i = [], 0
    while True:
        j = hay.find(pin, i)
        if j < 0:
            return out
        end = j + len(pin)
        ok_left = j == 0 or not _is_word_char(text[j - 1])
        # Whole words only, in every script: a highlight never cuts a word (প্রতি is not
        # প্রতিশ্রুত, জন্য is not জন্যে).
        ok_right = end == len(text) or not _is_word_char(text[end])
        if ok_left and ok_right:
            out.append((j, end))
            i = end
        else:
            i = j + 1


class VerseIndex:
    """Joined Arabic verse text and per-language word-by-word lines, with token offsets."""

    def __init__(self, gtaf):
        self.gtaf = gtaf
        self._ar = {}
        self._wbw = {}

    def arabic(self, s, a):
        if (s, a) not in self._ar:
            parts = [t["arabic"] for t in self.gtaf["en"][(s, a)]]
            starts, pos = [], 0
            for p in parts:
                starts.append(pos)
                pos += len(p) + 1
            self._ar[(s, a)] = (" ".join(parts), starts)
        return self._ar[(s, a)]

    def wbw(self, lang, s, a):
        key = (lang, s, a)
        if key not in self._wbw:
            parts = [norm(t["translation"], lang).strip() for t in self.gtaf[lang][(s, a)]]
            starts, pos = [], 0
            for p in parts:
                starts.append(pos)
                pos += len(p) + 1
            self._wbw[key] = (" ".join(parts), parts, starts)
        return self._wbw[key]


def _content_label(lem, o, raw, lang):
    """The sense text for a word: its cleaned gloss, which must be a contiguous piece of the raw
    gloss so it can be highlighted exactly."""
    cleaned = glosses.clean(raw, lang, lem.coarse, conj=o.conj_only and lem.track != "FUNCTION")
    category = "PARTICLE" if lem.track == "FUNCTION" else lem.category
    if not cleaned or glosses.meaning_problem(cleaned, lang, category):
        return None
    hits = find_all(cleaned, raw, lang)
    if not hits:
        return None
    start, end = hits[-1] if o.conj_only else hits[0]
    return raw[start:end], start


def _function_label(curated, raw, lang):
    """The curated sense of a function word that this occurrence's own gloss contains."""
    for sense in curated:
        hits = find_all(sense, raw, lang)
        if len(hits) == 1:
            start, end = hits[0]
            return raw[start:end], start
    return None


def _as_bare(o):
    from dataclasses import replace
    return replace(o, bare=True, det_only=False, conj_only=False, prefixed=False, suffixed=False)


def build(lem, qac, verses, translations, curated=None, display=None):
    """{lang: [Sense, ...]} for one lemma; a language maps to [] when no sense can be proven."""
    usable = []
    display_key = spans.letter_key(display) if display else None
    main_fine = lem.fines.most_common(1)[0][0] if lem.fines else None
    for o in lem.occurrences:
        segs = qac[(o.surah, o.ayah)][o.word]
        ar, starts = verses.arabic(o.surah, o.ayah)
        token = verses.gtaf["en"][(o.surah, o.ayah)][o.word - 1]["arabic"]
        base = starts[o.word - 1]
        if curated is not None and display_key and spans.letter_key(token) in (display_key, spans.letter_key(token, True)) \
                and spans.letter_key(token, True) == spans.letter_key(display, True):
            # The written word is exactly the taught form (ذَٰلِكَ is split into parts by QAC but
            # is one word to a learner): treat it as a bare occurrence of the whole token.
            usable.append((_as_bare(o), base, base + len(token)))
            continue
        span = spans.segment_span(token, segs, o.seg, o.seg_last)
        if span is None:
            continue
        clean_occurrence = o.bare or o.det_only or o.conj_only
        if curated is None and not clean_occurrence:
            continue
        if curated is not None and segs[o.seg].prefix and o.fine != main_fine:
            # An attached particle with several functions (لِ "for" vs emphatic لَ "surely"):
            # examples come only from its main function, which the reviewed senses describe.
            continue
        usable.append((o, base + span[0], base + span[1]))

    # Attached particles (وَ, بِ, لِ ...) never stand alone, so their gloss is always part of a longer
    # phrase: only a hand-reviewed sense found inside it can label them. Every other word is
    # labelled by its own gloss, exactly like content words.
    prefix_particle = curated is not None and not any(o.bare for o, _, _ in usable)
    if lem.coarse == "V":
        # Tenses are not senses: within a language every sense of a verb comes from one aspect
        # (past preferred), so "said / say" can't appear as two "meanings".
        aspects = ("PERF", "IMPF", "IMPV", None)
    else:
        aspects = (None,)

    out = {}
    for lang in config.LANGS:
        senses_curated = [s.strip() for s in curated[lang].split(" / ")] if curated else None
        by_label = defaultdict(list)
        for aspect in aspects:
            for o, ws, we in usable:
                if aspect is not None and o.aspect != aspect:
                    continue
                if curated is not None and not prefix_particle and not o.bare:
                    continue
                line, parts, pstarts = verses.wbw(lang, o.surah, o.ayah)
                raw = parts[o.word - 1]
                picked = (_function_label(senses_curated, raw, lang) if prefix_particle
                          else _content_label(lem, o, raw, lang))
                if not picked:
                    continue
                label, offset = picked
                key = fold(label, lang)
                by_label[key].append((o, ws, we, label, pstarts[o.word - 1] + offset))
            if by_label:
                break
        if not by_label:
            out[lang] = []
            continue
        total = sum(len(v) for v in by_label.values())
        if prefix_particle:
            order = [fold(s_, lang) for s_ in senses_curated if fold(s_, lang) in by_label]
        else:
            ranked = sorted(by_label, key=lambda k: (-len(by_label[k]), k))
            top = ranked[0]
            # Prefer the bare dictionary form when an inflected one tops the vote (দিনে -> দিন,
            # günü -> gün), provided the base form is itself well attested.
            bases = [k for k in by_label if k != top and len(k) >= 2 and top.startswith(k)
                     and len(by_label[k]) >= max(MIN_SENSE_COUNT, 0.3 * len(by_label[top]))]
            if bases:
                top = max(bases, key=lambda k: len(by_label[k]))
                ranked.remove(top)
                ranked.insert(0, top)
            order = [ranked[0]]
            for k in ranked[1:]:
                if len(order) == MAX_SENSES:
                    break
                n = len(by_label[k])
                if n < MIN_SENSE_COUNT or n / total < MIN_SENSE_SHARE:
                    continue
                if any(k in o_ or o_ in k or k.startswith(o_) or o_.startswith(k) for o_ in order):
                    continue
                order.append(k)
        senses = []
        for k in order[:MAX_SENSES]:
            ex = _best_example(by_label[k], lang, verses, translations)
            if ex:
                label = _display_label(by_label[k])
                senses.append(Sense(meaning=label, example=ex, support=len(by_label[k])))
        out[lang] = senses
    return out


def _display_label(cands):
    """Most common casing of the label, preferring forms seen mid-verse (GTAF capitalizes the
    first word of an ayah)."""
    mid = Counter(c[3] for c in cands if c[0].word > 1)
    allc = Counter(c[3] for c in cands)
    return (mid or allc).most_common(1)[0][0]


def _best_example(cands, lang, verses, translations):
    label = _display_label(cands)
    best, best_key = None, None
    for o, ws, we, raw_label, wbw_off in cands:
        if raw_label != label:
            continue  # the card shows `label`; the highlighted text must be exactly that
        line, parts, pstarts = verses.wbw(lang, o.surah, o.ayah)
        tr = norm(translations[lang][(o.surah, o.ayah)], lang)
        hits = find_all(label, tr, lang)
        others_use_it = any(find_all(label, p, lang) for i, p in enumerate(parts) if i != o.word - 1)
        tr_span = hits[0] if len(hits) == 1 and not others_use_it else None
        n_words = len(parts)
        key = (
            tr_span is None,                       # verified highlight in the full translation first
            not o.verb_3ms_perf if o.aspect else False,   # verbs: the dictionary form (قَالَ)
            not o.bare,                            # then the word on its own
            n_words > 25,                          # then a short, readable verse
            n_words,
            o.surah, o.ayah, o.word,
        )
        if best_key is None or key < best_key:
            best_key = key
            best = Example(
                surah=o.surah, ayah=o.ayah, word=o.word, word_start=ws, word_end=we,
                wbw=line, wbw_start=wbw_off, wbw_end=wbw_off + len(label),
                translation=tr,
                tr_start=tr_span[0] if tr_span else None, tr_end=tr_span[1] if tr_span else None,
            )
    return best
