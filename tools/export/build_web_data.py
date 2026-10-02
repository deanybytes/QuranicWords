#!/usr/bin/env python3
"""Builds the web app's data from the app's bundled curriculum (tools/pipeline output), so the
web dictionary and the Android app always teach exactly the same words and meanings.

    data/index.json          every word, light fields only (loaded at start, ~1 MB)
    data/verses/ch_NN.json   example verses + contextual senses per chapter (lazy-loaded)
    data/roots.json          root -> word ids
"""
import json
import re
import shutil
import unicodedata
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"
DATA = ROOT / "data"
LANGS = ["en", "bn", "ur", "hi", "in", "tr", "fa", "fr"]
AUDIO_PREFIX = "/app/src/main/assets/"


def skeleton(text):
    t = unicodedata.normalize("NFC", text or "")
    t = t.translate(str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ة": "ه", "ؤ": "و", "ئ": "ي"}))
    return "".join(c for c in t if "ء" <= c <= "ي")


def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def main():
    chapters = json.loads((CONTENT / "chapters.json").read_text(encoding="utf-8"))["chapters"]
    sections = json.loads((CONTENT / "sections.json").read_text(encoding="utf-8"))["sections"]
    lessons = json.loads((CONTENT / "lessons_vocabulary.json").read_text(encoding="utf-8"))["lessons"]
    exercises = json.loads((CONTENT / "exercises_vocabulary.json").read_text(encoding="utf-8"))
    freq = {w["id"]: w for w in json.loads((CONTENT / "word_frequency.json").read_text(encoding="utf-8"))["words"]}

    ch_num = {c["id"]: c["sortOrder"] for c in chapters}
    sec_num = {s["id"]: s["sortOrder"] for s in sections}
    lesson_by_id = {l["id"]: l for l in lessons}
    regular_index = {}
    counters = defaultdict(int)
    for l in lessons:
        if l["kind"] == "REGULAR":
            counters[l["sectionId"]] += 1
            regular_index[l["id"]] = counters[l["sectionId"]]

    words, verses, roots = [], defaultdict(dict), defaultdict(list)
    for e in exercises:
        if e["exerciseType"] != "WORD_INTRO":
            continue
        c = e["content"]
        lesson = lesson_by_id[e["lessonId"]]
        f = freq[c["wordId"]]
        ch = ch_num[lesson["chapterId"]]
        words.append({
            "id": c["wordId"], "ar": c["arabicWord"], "tl": c.get("transliteration", ""),
            "rt": c.get("root"), "cat": c["lemmaCategory"], "pos": c.get("partOfSpeechDetail") or c.get("verbForm"),
            "ch": ch, "sec": sec_num[lesson["sectionId"]], "les": regular_index[e["lessonId"]],
            "occ": c["quranOccurrenceCount"], "rank": f["frequencyRank"], "m": c["meaning"],
            "ref": c["exampleVerseReference"], "au": AUDIO_PREFIX + c["audioAssetPath"] if c.get("audioAssetPath") else None,
            "sk": skeleton(c["arabicWord"]), "poly": len(c.get("polysemyEntries", [])) > 1,
            "vf": {k: c[k] for k in ("verbForm", "pastArabic", "presentArabic", "masdarArabic") if c.get(k)} or None,
        })
        verses[ch][c["wordId"]] = {
            "v_ar": c["exampleVerseArabic"], "s": c["arabicWordStart"], "e": c["arabicWordEnd"],
            "v_tr": c["exampleVerseTranslation"], "hl": c.get("meaningHighlight", {}),
            "senses": [{"i": p["meaningIndex"], "m": p["contextualMeaning"], "ref": p["verseReference"],
                        "v_ar": p["verseArabic"], "s": p["arabicWordStart"], "e": p["arabicWordEnd"],
                        "v_tr": p["verseTranslation"]} for p in c.get("polysemyEntries", [])],
        }
        if c.get("root"):
            roots[c["root"]].append(c["wordId"])
    words.sort(key=lambda w: w["rank"])

    total = sum(c["quranOccurrenceCount"] for c in chapters)
    meta = {
        "version": "2", "languages": LANGS, "wordCount": len(words), "rootCount": len(roots),
        "occurrences": total,
        "coveragePercent": round(sum(c["quranOccurrencePercent"] for c in chapters), 1),
        "chapters": [{"n": c["sortOrder"], "id": c["id"], "title": c["title"], "words": c["wordCount"],
                      "pct": c["quranOccurrencePercent"]} for c in chapters],
    }
    if (DATA / "verses").exists():
        shutil.rmtree(DATA / "verses")
    for stale in ("words.json", "words_summary.json", "metadata.json"):
        (DATA / stale).unlink(missing_ok=True)
    dump(DATA / "index.json", {"meta": meta, "words": words})
    for ch, entries in verses.items():
        dump(DATA / "verses" / f"ch_{ch:02d}.json", entries)
    dump(DATA / "roots.json", {r: ids for r, ids in sorted(roots.items(), key=lambda kv: -len(kv[1]))})
    print(f"{len(words)} words, {len(roots)} roots, {len(verses)} verse files -> {DATA}")


if __name__ == "__main__":
    main()
