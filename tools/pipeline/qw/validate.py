"""Content invariants. Any violation fails the build - this is what would have caught the
repeated-lemma and misaligned-gloss corruption in the previous content."""
import re
from collections import Counter, defaultdict

from . import arabic, config, glosses

LATIN = re.compile(r"^[A-Za-zĀāĪīŪūḤḥṢṣḌḍṬṭẒẓʿʾ' -]+$")


def run(assets):
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
            if set(c.get("exampleVerseTranslation", {})) != set(config.LANGS):
                errors.append(f"{c['wordId']} verse translation languages")
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
