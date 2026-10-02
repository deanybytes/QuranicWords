"""Turns the raw lexicon into shippable word records: verified meanings in every content
language, an example verse with an exact highlight, transliteration, root, verb forms and
contextual senses. A lemma ships only if every language has a source-derived meaning."""
import csv
import hashlib
import re
from collections import Counter
from dataclasses import dataclass, field

from . import arabic, config, glosses, lexicon

PARTICLE_TYPES = {
    "P": "Preposition (Ḥarf Jarr)", "CONJ": "Conjunction (Ḥarf ʿAṭf)", "REM": "Resumption particle",
    "NEG": "Negative particle (Ḥarf Nafy)", "ACC": "Accusative particle (Ḥarf Naṣb)",
    "SUB": "Subordinating particle", "COND": "Conditional particle (Ḥarf Sharṭ)",
    "INTG": "Interrogative (Istifhām)", "EMPH": "Emphatic particle (Tawkīd)", "VOC": "Vocative (Nidāʾ)",
    "CERT": "Particle of certainty", "RES": "Restriction particle (Ḥaṣr)", "FUT": "Future particle",
    "PRO": "Prohibition particle (Nahy)", "RET": "Retraction particle (Iḍrāb)", "ANS": "Answer particle",
    "AVR": "Aversion particle", "EXH": "Exhortation particle", "EXL": "Explanation particle",
    "ATT": "Attention particle (Tanbīh)", "AMD": "Amendment particle (Istidrāk)",
    "PRON": "Pronoun (Ḍamīr)", "DEM": "Demonstrative (Ism Ishārah)", "REL": "Relative pronoun (Ism Mawṣūl)",
    "T": "Time adverb (Ẓarf Zamān)", "LOC": "Place adverb (Ẓarf Makān)", "SUR": "Surprise particle",
    "EQ": "Equalization particle", "INC": "Inceptive particle", "PRP": "Purpose particle",
    "RSLT": "Result particle", "CIRC": "Circumstantial particle", "SUP": "Supplemental particle",
    "CAUS": "Causative particle", "PREV": "Preventive particle", "INT": "Interpretation particle",
    "EXP": "Exceptive particle (Istithnāʾ)", "COM": "Comitative particle",
}
NOUN_DETAIL = {"PN": "Proper noun", "ADJ": "Adjective (Ṣifah)", "ACT_PCPL": "Active participle (Ism Fāʿil)",
               "PASS_PCPL": "Passive participle (Ism Mafʿūl)", "VN": "Verbal noun (Maṣdar)"}
ROMAN = {"1": "I", "2": "II", "3": "III", "4": "IV", "5": "V", "6": "VI", "7": "VII", "8": "VIII",
         "9": "IX", "10": "X", "11": "XI", "12": "XII"}


@dataclass
class Word:
    id: str
    lemma: lexicon.Lemma
    arabic: str
    category: str                 # NOUN / VERB / PARTICLE
    track: str                    # FUNCTION / VERB / NOUN
    meaning: dict                 # lang -> gloss
    confidence: dict              # lang -> (support, total, method)
    reviewed: dict                # lang -> bool (high-confidence source gloss)
    translit: str
    root: str = None
    verse: tuple = None           # (surah, ayah, word index)
    senses: list = field(default_factory=list)
    past: str = None
    present: str = None
    masdar: str = None
    verb_form: str = None
    particle_type: str = None
    pos_detail: str = None
    audio: str = None

    @property
    def frequency(self):
        return self.lemma.frequency


def word_id(lem, category):
    prefix = {"PARTICLE": "wp_", "VERB": "wv_"}.get(category, "wn_")
    return prefix + hashlib.sha1(lem.key.encode("utf-8")).hexdigest()[:8]


def load_function_overrides():
    rows = {}
    with open(config.OVERRIDES / "function_words.tsv", encoding="utf-8") as f:
        for row in csv.DictReader(f, delimiter="\t"):
            row["key"] = arabic.nfc(row["key"])
            row["action"] = arabic.nfc(row["action"])
            rows[row["key"]] = row
    return rows


def apply_function_overrides(lemmas):
    """Merges split duplicates and drops non-words per the reviewed function-word table; returns
    {key: row} for the kept entries. Every function lemma must be listed, so nothing slips in
    unreviewed."""
    rows = load_function_overrides()
    function_keys = [k for k, l in lemmas.items() if l.track == "FUNCTION"]
    missing = [k for k in function_keys if k not in rows]
    if missing:
        raise SystemExit(f"function words missing from overrides/function_words.tsv: {missing}")
    for key in function_keys:
        action = rows[key]["action"]
        if action == "exclude":
            del lemmas[key]
        elif action.startswith("merge:"):
            target = action.split(":", 1)[1]
            lemmas[target].occurrences.extend(lemmas[key].occurrences)
            lemmas[target].fines.update(lemmas[key].fines)
            del lemmas[key]
    return {k: rows[k] for k in lemmas if k in rows}


def _verb_forms(lem, qac_forms):
    """3MS perfect and imperfect as they actually occur, for the verb-forms panel."""
    perf, impf = Counter(), Counter()
    for o in lem.occurrences:
        f = qac_forms.get((o.surah, o.ayah, o.word))
        if not f:
            continue
        seg, tags = f
        if "3MS" not in tags:
            continue
        if "PERF" in tags and "PASS" not in tags:
            perf[arabic.clean_display(seg)] += 1
        elif "IMPF" in tags and "PASS" not in tags:
            impf[arabic.clean_display(seg)] += 1
    p = perf.most_common(1)[0][0] if perf else None
    i = impf.most_common(1)[0][0] if impf else None
    return p, i


def _example_occurrence(lem, gtaf_en):
    """Shortest ayah containing a clean occurrence (so the highlighted token *is* the word),
    earliest in the mushaf on ties."""
    def ayah_len(o):
        return len(gtaf_en.get((o.surah, o.ayah), []))
    clean = [o for o in lem.occurrences if o.bare or o.det_only or o.conj_only]
    pool = clean or lem.occurrences
    short = [o for o in pool if 3 <= ayah_len(o) <= 30] or pool
    return min(short, key=lambda o: (ayah_len(o) > 18, ayah_len(o), o.surah, o.ayah, o.word))


def _senses(lem, gtaf, meaning):
    """One contextual sense per " / "-separated English meaning, each with its own verse whose
    English gloss for this word is exactly that sense."""
    en_senses = [s.strip() for s in meaning["en"].split(" / ")]
    if len(en_senses) < 2:
        return []
    per_lang_senses = {lang: [s.strip() for s in meaning[lang].split(" / ")] for lang in config.LANGS}
    out = []
    for idx, sense in enumerate(en_senses):
        for o in lem.occurrences:
            if not (o.bare or o.det_only or o.conj_only):
                continue
            toks = gtaf["en"].get((o.surah, o.ayah))
            g = glosses.clean(toks[o.word - 1]["translation"], "en", lem.coarse, conj=o.conj_only)
            if glosses.norm(g) == glosses.norm(sense):
                ctx = {}
                for lang in config.LANGS:
                    ls = per_lang_senses[lang]
                    ctx[lang] = ls[idx] if idx < len(ls) else ls[0]
                out.append((idx + 1, ctx, (o.surah, o.ayah, o.word)))
                break
    return out if len(out) >= 2 else []


def _merge_spelling_variants(lemmas):
    """QAC occasionally lists one word under two lemma spellings (مُهْتَدِي / مُهتَدي); a learner
    would see the same word twice. Fold each variant into its most frequent spelling."""
    groups = {}
    for key, lem in list(lemmas.items()):
        if lem.track == "FUNCTION":
            continue
        gk = (arabic.pronunciation_key(lem.lemma), lem.coarse)
        keep = groups.get(gk)
        if keep is None:
            groups[gk] = key
            continue
        a, b = lemmas[keep], lem
        winner, loser = (a, b) if a.frequency >= b.frequency else (b, a)
        winner.occurrences.extend(loser.occurrences)
        winner.fines.update(loser.fines)
        winner.root = winner.root or loser.root
        groups[gk] = winner.key
        del lemmas[loser.key]


def build_words(qac, gtaf):
    lemmas = lexicon.build(qac)
    lexicon.assign_function_display(lemmas, gtaf["en"])
    total_occurrences = sum(l.frequency for l in lemmas.values())
    fn_rows = apply_function_overrides(lemmas)
    _merge_spelling_variants(lemmas)

    qac_forms = {}
    for (s, a), ws in qac.items():
        for w, segs in ws.items():
            for g in segs:
                if g.coarse == "V" and not g.prefix and not g.suffix:
                    qac_forms[(s, a, w)] = (g.form, g.tags)
    masdars = {}
    for l in lemmas.values():
        if l.coarse == "N" and l.fines.get("VN") and l.root and l.verb_form:
            masdars.setdefault((l.root, l.verb_form), []).append(l)

    words, excluded = [], []
    for key, lem in lemmas.items():
        category = lem.category
        if lem.track == "FUNCTION":
            row = fn_rows[key]
            meaning = {lang: row[lang] for lang in config.LANGS}
            display = arabic.clean_display(row["display"])
            confidence = {lang: (0, 0, "curated") for lang in config.LANGS}
            reviewed = {lang: True for lang in config.LANGS}
        else:
            meaning, confidence, reviewed = {}, {}, {}
            for lang in config.LANGS:
                g, c, t, m = glosses.gloss_lemma(lem, gtaf, lang)
                if g is None:
                    break
                meaning[lang] = g
                confidence[lang] = (c, t, m)
                reviewed[lang] = m != "pronoun" and glosses.accepted(c, t)
            for lang in list(meaning):
                problem = glosses.meaning_problem(meaning[lang], lang, category)
                if problem:
                    del meaning[lang]
            if len(meaning) < len(config.LANGS):
                missing = [l for l in config.LANGS if l not in meaning]
                excluded.append({"key": key, "lemma": lem.lemma, "frequency": lem.frequency,
                                 "reason": "no source-verified meaning in: " + ",".join(missing)})
                continue
            display = arabic.clean_display(lem.lemma)
        w = Word(
            id=word_id(lem, category), lemma=lem, arabic=display, category=category, track=lem.track,
            meaning=meaning, confidence=confidence, reviewed=reviewed,
            translit=arabic.transliterate(display), root=lem.root,
        )
        top_fine = lem.fines.most_common(1)[0][0] if lem.fines else None
        if category == "VERB":
            w.verb_form = f"Form {ROMAN.get(lem.verb_form, lem.verb_form)}" if lem.verb_form else None
            w.past, w.present = _verb_forms(lem, qac_forms)
            if w.past:
                # The 3rd-person perfect (قَالَ, أَغْرَقَ) is the conventional dictionary form;
                # QAC's citation lemma is sometimes an imperfect/1st-person form.
                w.arabic = w.past
                w.translit = arabic.transliterate(w.arabic)
            cands = masdars.get((lem.root, lem.verb_form), [])
            if cands:
                w.masdar = arabic.clean_display(max(cands, key=lambda m: m.frequency).lemma)
        if lem.track == "FUNCTION":
            w.particle_type = PARTICLE_TYPES.get(top_fine)
        elif category == "NOUN":
            w.pos_detail = NOUN_DETAIL.get(top_fine) or ("Proper noun" if lem.is_proper_noun else "Noun (Ism)")
        o = _example_occurrence(lem, gtaf["en"])
        w.verse = (o.surah, o.ayah, o.word)
        w.senses = _senses(lem, gtaf, meaning)
        words.append(w)

    # Same id twice would mean two lemmas hashed together - impossible in practice, but fatal.
    assert len({w.id for w in words}) == len(words), "word id collision"
    return words, excluded, total_occurrences


def meaning_tokens(gloss):
    return {t for t in re.split(r"[\s/(),;]+", gloss.lower()) if len(t) > 2}
