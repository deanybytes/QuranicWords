"""Lemma inventory from the Quranic Arabic Corpus: one entry per (coarse POS, lemma), with every
occurrence located by (surah, ayah, word) so glosses/verses can be drawn from the same tokens."""
from collections import Counter, defaultdict
from dataclasses import dataclass, field

from . import arabic

# Closed-class nominals taught alongside particles as "function words". Their grammatical
# category stays Ism (NOUN) - only their position in the curriculum changes.
FUNCTION_NOMINAL_TAGS = {"PRON", "DEM", "REL", "INTG", "COND"}
# Time/place adverbs count as function words only when rootless (إِذْ, حَيْثُ); rooted ones
# (بُكْرَة "morning", أَصِيل "evening") are ordinary vocabulary.
ADVERB_TAGS = {"T", "LOC"}
# Fine tags that are not part-of-speech (QAC puts ROOT/LEM first for plain nouns).
NON_POS_PREFIXES = ("ROOT:", "LEM:", "VF:", "MOOD:", "SP:")


@dataclass
class Occurrence:
    surah: int
    ayah: int
    word: int            # 1-based word index within the ayah (aligned with GTAF tokens)
    fine: str            # fine POS tag of the stem segment
    bare: bool           # word is just this segment (plus, for a verb, its own subject ending)
    det_only: bool       # word is the article + this segment
    prefixed: bool       # has a (non-article) prefix clitic
    suffixed: bool       # has a suffix pronoun
    form: str            # vocalized form of this segment
    conj_only: bool = False  # only attachment is a leading وَ / فَ conjunction
    verb_3ms_perf: bool = False


@dataclass
class Lemma:
    key: str             # stable identity, e.g. "V|قالَ"
    coarse: str          # N / V / P
    lemma: str           # display form (vocalized)
    root: str = None     # "ك-ت-ب" or None
    occurrences: list = field(default_factory=list)
    fines: Counter = field(default_factory=Counter)
    verb_form: str = None
    forms: Counter = field(default_factory=Counter)

    @property
    def frequency(self):
        return len(self.occurrences)

    @property
    def category(self):
        return {"V": "VERB", "P": "PARTICLE"}.get(self.coarse, "NOUN")

    @property
    def track(self):
        if self.coarse == "P":
            return "FUNCTION"
        if self.coarse == "N" and self.fines:
            top = self.fines.most_common(1)[0][0]
            if top in FUNCTION_NOMINAL_TAGS or (top in ADVERB_TAGS and not self.root):
                return "FUNCTION"
        return "VERB" if self.coarse == "V" else "NOUN"

    @property
    def is_proper_noun(self):
        return self.fines.get("PN", 0) > len(self.occurrences) / 2


def _fine_tag(seg):
    for t in seg.tags:
        if not t.startswith(NON_POS_PREFIXES) and t not in ("PREF", "SUFF"):
            return t
    return seg.coarse


PERSONS = {"1S", "1P", "2MS", "2FS", "2D", "2MD", "2FD", "2MP", "2FP", "3MS", "3FS", "3D", "3MD", "3FD", "3MP", "3FP"}


def _person(seg):
    return next((t for t in seg.tags if t in PERSONS), None)


def _is_subject_suffix(segs, suffix):
    """The first suffix after a verb stem carrying the verb's own person (يُؤْمِنُ 3MP + ونَ 3MP)
    is its subject ending - part of the verb, unlike an attached object pronoun."""
    for i, g in enumerate(segs):
        if g is suffix:
            prev = segs[i - 1] if i > 0 else None
            return prev is not None and prev.coarse == "V" and not prev.prefix and _person(prev) == _person(g)
    return False


def format_root(root):
    if not root:
        return None
    letters = [c for c in arabic.nfc(root) if "ء" <= c <= "ي"]
    return "-".join(letters) if letters else None


def build(qac):
    lemmas = {}
    pronoun_forms = defaultdict(Counter)
    for (s, a), words in sorted(qac.items()):
        for w, segs in sorted(words.items()):
            stems = [g for g in segs if not g.prefix and not g.suffix]
            prefixes = [g for g in segs if g.prefix]
            has_det = any(g.has("DET") for g in prefixes)
            other_prefix = any(not g.has("DET") for g in prefixes)
            conj_prefix = bool(prefixes) and all(
                g.has("DET") or (g.lemma in ("و", "ف") and not g.has("P")) for g in prefixes
            ) and any(not g.has("DET") for g in prefixes)
            suffixed = any(g.suffix for g in segs if not _is_subject_suffix(segs, g))
            for g in segs:
                if g.suffix or g.has("DET"):
                    continue
                is_stem = not g.prefix
                lemma_text = g.lemma
                if not lemma_text:
                    if g.coarse == "N" and _fine_tag(g) == "PRON" and is_stem:
                        lemma_text = arabic.clean_display(g.form.replace("ٱ", "ا"))
                        if len(arabic.skeleton(lemma_text)) < 2:
                            continue  # attached pronoun letter (كَ, ى), not a standalone word
                        pronoun_forms[arabic.skeleton(lemma_text)][lemma_text] += 1
                        lemma_text = "PRON:" + arabic.skeleton(lemma_text)
                    else:
                        continue
                key = arabic.nfc(f"{g.coarse}|{lemma_text}")
                lem = lemmas.get(key)
                if lem is None:
                    display = arabic.clean_display(lemma_text) if not lemma_text.startswith("PRON:") else None
                    lem = lemmas[key] = Lemma(key=key, coarse=g.coarse, lemma=display, root=format_root(g.root))
                if lem.root is None and g.root:
                    lem.root = format_root(g.root)
                fine = _fine_tag(g)
                lem.fines[fine] += 1
                if lem.verb_form is None and (g.coarse == "V" or g.has("VN")):
                    lem.verb_form = g.feature("VF")
                lem.occurrences.append(Occurrence(
                    surah=s, ayah=a, word=w, fine=fine,
                    bare=is_stem and not prefixes and not suffixed and len(stems) == 1,
                    det_only=is_stem and has_det and not other_prefix and not suffixed and len(stems) == 1,
                    prefixed=other_prefix if is_stem else True,
                    suffixed=suffixed,
                    form=g.form,
                    verb_3ms_perf=g.coarse == "V" and g.has("PERF") and "3MS" in g.tags,
                    conj_only=is_stem and conj_prefix and not suffixed and len(stems) == 1,
                ))
                lem.forms[arabic.clean_display(g.form)] += 1
    for key, lem in lemmas.items():
        if key.startswith("N|PRON:"):
            lem.lemma = pronoun_forms[key.split(":", 1)[1]].most_common(1)[0][0]
    # Pronoun pieces QAC splits out of a larger word (the كُمْ of ذَٰلِكُمْ) are not words a
    # learner meets on their own.
    for key in [k for k, l in lemmas.items() if k.startswith("N|PRON:") and not any(o.bare for o in l.occurrences)]:
        del lemmas[key]
    return lemmas


PREFIX_PARTICLE_FORMS = {"و": "وَ", "ف": "فَ", "ب": "بِ", "ل": "لِ", "ك": "كَ", "س": "سَ", "ت": "تَ", "أ": "أَ"}


def assign_function_display(lemmas, gtaf_tokens):
    """Function words are recognized by their written form (ذَٰلِكَ, عَلَىٰ), not an abstract
    dictionary lemma (ذا, على) - teach the whole word learners actually meet in the mushaf.
    Prefix particles (وَ, بِ, لِ...) never stand alone, so they keep their vocalized letter."""
    for key, lem in lemmas.items():
        if lem.track != "FUNCTION":
            continue
        bare_key = key.split("|", 1)[1]
        if lem.coarse == "P" and bare_key in PREFIX_PARTICLE_FORMS and not any(o.bare for o in lem.occurrences):
            lem.lemma = PREFIX_PARTICLE_FORMS[bare_key]
            continue
        words = Counter()
        for o in lem.occurrences:
            if o.prefixed or o.suffixed:
                continue
            tok = gtaf_tokens.get((o.surah, o.ayah))
            if tok and o.word <= len(tok):
                words[arabic.clean_display(tok[o.word - 1]["arabic"])] += 1
        if words:
            lem.lemma = words.most_common(1)[0][0]
