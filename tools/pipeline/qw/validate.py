"""Content invariants. Any violation fails the build - this is what would have caught the
repeated-lemma and misaligned-gloss corruption in the previous content."""
import re
from collections import Counter, defaultdict

from . import arabic, config, glosses, senses as senses_mod, spans as spans_mod

LATIN = re.compile(r"^[A-Za-zĀāĪīŪūḤḥṢṣḌḍṬṭẒẓʿʾ' -]+$")


def run(assets, raw=None):
    errors = []
    words = assets["word_frequency"]["words"]
    lessons = assets["lessons"]["lessons"]
    exercises = assets["exercises"]["exercises"]
    word_ids = Counter(w["id"] for w in words)
    errors += [f"duplicate word id {k}" for k, v in word_ids.items() if v > 1]
    by_id = {w["id"]: w for w in words}

    # One lemma per written form and category.
    seen = defaultdict(list)
    intros = {e["content"]["wordId"]: e["content"] for e in exercises if e["exerciseType"] == "WORD_INTRO"}
    for w in words:
        cat = intros.get(w["id"], {}).get("lemmaCategory")
        seen[(arabic.pronunciation_key(w["arabicWord"]), cat)].append(w["id"])
    errors += [f"same word shipped twice: {k[0]} {v}" for k, v in seen.items() if len(v) > 1]

    # Meanings: every language, no fragments, no misalignment signature.
    gloss_owners = defaultdict(set)
    lesson_cat = {l["id"]: l["category"] for l in lessons}
    curated = {e["content"]["wordId"] for e in exercises
               if e["exerciseType"] == "WORD_INTRO" and lesson_cat.get(e["lessonId"]) == "MIXED"}
    for w in words:
        m = w["meaning"]
        if set(m) != set(config.LANGS):
            errors.append(f"{w['id']} meaning languages {sorted(m)}")
            continue
        for lang, g in m.items():
            if w["id"] in curated:
                continue  # function words: reviewed by hand in overrides/function_words.tsv
            problem = glosses.meaning_problem(g, lang, intros.get(w["id"], {}).get("lemmaCategory"))
            if problem:
                errors.append(f"{w['id']} {lang} meaning {problem}: {g}")
        gloss_owners[m["en"].lower()].add(w["id"])

    # Every word appears in exactly one regular lesson, with a verified verse span.
    lesson_kind = {l["id"]: l["kind"] for l in lessons}
    regular_count = Counter()
    for e in exercises:
        c = e["content"]
        if e["lessonId"] not in lesson_kind:
            errors.append(f"{e['id']} points at missing lesson {e['lessonId']}")
        if e["exerciseType"] == "WORD_INTRO":
            if lesson_kind.get(e["lessonId"]) == "REGULAR":
                regular_count[c["wordId"]] += 1
            v, s, t = c.get("exampleVerseArabic"), c.get("arabicWordStart"), c.get("arabicWordEnd")
            if not v or s is None or not (0 <= s < t <= len(v)):
                errors.append(f"{c['wordId']} bad verse span")
            errors += _check_senses(c, assets["verses"], by_id.get(c["wordId"]), raw)
            tr = c.get("transliteration", "")
            if not LATIN.match(tr):
                errors.append(f"{c['wordId']} transliteration not Latin: {tr}")
        if e["exerciseType"] in ("MULTIPLE_CHOICE", "FILL_IN_THE_BLANK"):
            opts = c["options"]
            ids = [o["id"] for o in opts]
            if c["correctOptionId"] not in ids or len(ids) != len(set(ids)) or len(ids) < 3:
                errors.append(f"{e['id']} broken options")
            for lang in config.LANGS:
                labels = [o["label"].get(lang, "").lower() for o in opts]
                if len(labels) != len(set(labels)):
                    errors.append(f"{e['id']} two options share a {lang} meaning")
                    break
            if c["wordId"] not in by_id:
                errors.append(f"{e['id']} unknown word {c['wordId']}")
        if e["exerciseType"] == "WORD_IN_VERSE_TAP":
            v = c["verseArabic"]
            if not (0 <= c["correctWordStart"] < c["correctWordEnd"] <= len(v)):
                errors.append(f"{e['id']} bad tap span")
        if e["exerciseType"] == "FILL_IN_THE_BLANK":
            v = c["sentenceArabic"]
            if not (0 <= c["blankStart"] < c["blankEnd"] <= len(v)):
                errors.append(f"{e['id']} bad blank span")
        if c.get("audioAssetPath") or e["exerciseType"] in ("TAP_WHAT_YOU_HEAR", "LISTEN_AND_TYPE"):
            errors.append(f"{e['id']} references audio, which the app no longer ships")
    for w in words:
        if regular_count[w["id"]] != 1:
            errors.append(f"{w['id']} taught in {regular_count[w['id']]} regular lessons")

    scored = Counter(e["lessonId"] for e in exercises if e["exerciseType"] not in ("WORD_INTRO", "CHAPTER_INTRO"))
    for l in lessons:
        if l["kind"] != "CHAPTER_INTRO" and scored[l["id"]] == 0:
            errors.append(f"lesson {l['id']} has no scored exercise")

    # Stored counts must equal what the curriculum actually contains.
    ch_words = Counter()
    lesson_ch = {l["id"]: l["chapterId"] for l in lessons}
    for e in exercises:
        if e["exerciseType"] == "WORD_INTRO" and lesson_kind.get(e["lessonId"]) == "REGULAR":
            ch_words[lesson_ch[e["lessonId"]]] += 1
    for ch in assets["chapters"]["chapters"]:
        if ch_words[ch["id"]] != ch["wordCount"]:
            errors.append(f"{ch['id']} wordCount {ch['wordCount']} != {ch_words[ch['id']]}")
    return errors


def _ayah_letters(word):
    """Spelling-insensitive letters of one written word: the two Uthmani encodings differ only
    in tatweel, small yā/wāw, hamza seats and yā before a dagger alif."""
    import unicodedata
    w = unicodedata.normalize("NFC", word).replace("\u0640", "")
    w = re.sub(r"[\u064B-\u065F\u06D6-\u06ED]", "", w)          # harakat, small letters, pause marks
    w = w.replace("\u0649\u0670", "\u0670")                      # صَىٰحِبَىِ = صَٰحِبَيِ
    w = re.sub(r"[^\u0621-\u064A\u0671]", "", w)
    for a, b in (("ى", "ي"), ("ٱ", "ا"), ("أ", "ا"), ("إ", "ا"), ("ؤ", "و"), ("ئ", "ي"), ("ة", "ه")):
        w = w.replace(a, b)
    return w.replace("ء", "").replace("ا", "")


BASMALA = ["بسم", "لله", "لرحمن", "لرحيم"]


def check_complete_ayahs(verses, reference):
    """Every cited verse must be the complete ayah: the same words, in order, as an independent
    full Qur'an text (the Basmala Tanzil prefixes to verse 1 is not part of the ayah)."""
    errs = []
    for key, v in verses.items():
        s, a = map(int, key.split(":"))
        theirs = [w for w in reference[(s, a)].split() if _ayah_letters(w)]
        if a == 1 and s not in (1, 9) and [_ayah_letters(w) for w in theirs[:4]] == BASMALA:
            theirs = theirs[4:]
        ours = [w for w in v["ar"].split(" ") if _ayah_letters(w)]
        if len(ours) != len(theirs):
            errs.append(f"verse {key}: {len(ours)} words, the complete ayah has {len(theirs)}")
        elif any(_ayah_letters(x) != _ayah_letters(y) for x, y in zip(ours, theirs)):
            errs.append(f"verse {key}: text differs from the reference ayah")
    return errs


def _check_senses(intro, verses, word_row, raw):
    """Second, independent proof of every sense: re-derived from the raw sources rather than
    trusting the builder. Each check is one of the user-visible guarantees."""
    errs = []
    wid = intro["wordId"]
    senses = intro.get("senses") or {}
    if set(senses) != set(config.LANGS):
        return [f"{wid} senses languages {sorted(senses)}"]
    qac, gtaf, translations = raw if raw else (None, None, None)
    for lang, items in senses.items():
        if not items:
            errs.append(f"{wid} {lang} has no sense")
            continue
        joined = " / ".join(it["meaning"] for it in items)
        if joined != intro["meaning"].get(lang) or (word_row and word_row["meaning"].get(lang) != joined):
            errs.append(f"{wid} {lang} card meaning differs from its senses")
        if len({senses_mod.fold(it["meaning"], lang) for it in items}) != len(items):
            errs.append(f"{wid} {lang} duplicate sense")
        for it in items:
            v = verses.get(it["verse"])
            if v is None:
                errs.append(f"{wid} {lang} missing verse {it['verse']}")
                continue
            s, a = map(int, it["verse"].split(":"))
            ar, wbw, tr = v["ar"], v["wbw"][lang], v["tr"][lang]
            # 1. Arabic: the highlighted text is exactly the taught word inside its own token.
            ws, we = it["wordStart"], it["wordEnd"]
            if not (0 <= ws < we <= len(ar)):
                errs.append(f"{wid} {lang} {it['verse']} Arabic span outside one word")
            # 2. Word-by-word line: highlight is exactly the card's sense text...
            if wbw[it["wbwStart"]:it["wbwEnd"]] != it["meaning"]:
                errs.append(f"{wid} {lang} {it['verse']} WBW highlight '{wbw[it['wbwStart']:it['wbwEnd']]}' != '{it['meaning']}'")
            # 3. Full translation: highlight only if exact and unique.
            if it["translationStart"] is not None:
                piece = tr[it["translationStart"]:it["translationEnd"]]
                if senses_mod.fold(piece, lang) != senses_mod.fold(it["meaning"], lang):
                    errs.append(f"{wid} {lang} {it['verse']} translation highlight '{piece}' != '{it['meaning']}'")
                elif len(senses_mod.find_all(it["meaning"], tr, lang)) != 1:
                    errs.append(f"{wid} {lang} {it['verse']} translation highlight not unique")
            if raw:
                toks = gtaf["en"][(s, a)]
                if ar != " ".join(t["arabic"] for t in toks):
                    errs.append(f"{wid} {it['verse']} verse text is not the complete ayah")
                if tr != senses_mod.norm(translations[lang][(s, a)], lang):
                    errs.append(f"{wid} {lang} {it['verse']} translation text altered")
                parts = [senses_mod.norm(t["translation"], lang).strip() for t in gtaf[lang][(s, a)]]
                if wbw != " ".join(parts):
                    errs.append(f"{wid} {lang} {it['verse']} WBW line altered")
                # ...and lies inside the gloss of the very word that is highlighted in Arabic.
                w = it["word"]
                start_tok = sum(len(p) + 1 for p in parts[: w - 1])
                if not (start_tok <= it["wbwStart"] and it["wbwEnd"] <= start_tok + len(parts[w - 1])):
                    errs.append(f"{wid} {lang} {it['verse']} WBW highlight is not inside word {w}'s gloss")
                a_start = sum(len(t["arabic"]) + 1 for t in toks[: w - 1])
                if not (a_start <= ws and we <= a_start + len(toks[w - 1]["arabic"])):
                    errs.append(f"{wid} {lang} {it['verse']} Arabic highlight is not inside word {w}")
                # The highlighted Arabic letters are one of the word's own QAC segments.
                segs = qac[(s, a)][w]
                piece = spans_mod.letter_key(ar[ws:we])
                # Allowed: one attached particle on its own (وَ, بِ, لِ), or a stem plus the
                # verb's own subject ending - never a stem together with an attached prefix.
                options = set()
                for i, g in enumerate(segs):
                    if g.prefix:
                        if not g.has("DET"):
                            for fold in (False, True):
                                options.add(spans_mod.letter_key(g.form, fold))
                        continue
                    if g.suffix:
                        continue
                    j = i
                    while True:
                        for fold in (False, True):
                            options.add("".join(spans_mod.letter_key(x.form, fold) for x in segs[i:j + 1]))
                        if j + 1 < len(segs) and segs[j + 1].suffix and segs[i].coarse == "V":
                            j += 1
                        else:
                            break
                # Also allowed: the whole written word when it is exactly the taught form
                # (ذَٰلِكَ is one word to a learner even though QAC splits it).
                tok_ar = toks[w - 1]["arabic"]
                if (ws, we) == (a_start, a_start + len(tok_ar)) and \
                        spans_mod.letter_key(tok_ar, True) == spans_mod.letter_key(intro["arabicWord"], True):
                    options.add(piece)
                if piece not in options and spans_mod.letter_key(ar[ws:we], True) not in options:
                    errs.append(f"{wid} {lang} {it['verse']} Arabic highlight is not a QAC segment")
    return errs
