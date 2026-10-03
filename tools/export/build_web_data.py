#!/usr/bin/env python3
"""Builds the web app's data from the app's bundled curriculum (tools/pipeline output), so the
web dictionary and the Android app always teach exactly the same words and meanings.

    data/index.json          every word, light fields only (loaded at start, ~1 MB)
    data/verses/LANG/ch_NN.json   per language and chapter: each word's proven senses (exact
                                  spans) and the verses they use, stored once (lazy-loaded)
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
    verses = json.loads((CONTENT / "verses.json").read_text(encoding="utf-8"))
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

    words, roots = [], defaultdict(list)
    # per language, per chapter: senses of each word + the verses they use (stored once)
    per = defaultdict(lambda: {"words": {}, "verses": {}})
    for e in exercises:
        if e["exerciseType"] != "WORD_INTRO":
            continue
        c = e["content"]
        lesson = lesson_by_id[e["lessonId"]]
        f = freq[c["wordId"]]
        ch = ch_num[lesson["chapterId"]]
        first = c["senses"]["en"][0]
        words.append({
            "id": c["wordId"], "ar": c["arabicWord"], "tl": c.get("transliteration", ""),
            "rt": c.get("root"), "cat": c["lemmaCategory"], "pos": c.get("partOfSpeechDetail") or c.get("verbForm"),
            "ch": ch, "sec": sec_num[lesson["sectionId"]], "les": regular_index[e["lessonId"]],
            "occ": c["quranOccurrenceCount"], "rank": f["frequencyRank"], "m": c["meaning"],
            "ref": verses[first["verse"]]["ref"],
            "sk": skeleton(c["arabicWord"]),
            "poly": {lang: len(items) for lang, items in c["senses"].items() if len(items) > 1},
            "vf": {k: c[k] for k in ("verbForm", "pastArabic", "presentArabic", "masdarArabic") if c.get(k)} or None,
        })
        for lang in LANGS:
            bucket = per[(lang, ch)]
            items = []
            for sn in c["senses"][lang]:
                v = verses[sn["verse"]]
                bucket["verses"][sn["verse"]] = {"ref": v["ref"], "ar": v["ar"], "wbw": v["wbw"][lang], "tr": v["tr"][lang]}
                items.append({"m": sn["meaning"], "v": sn["verse"], "s": sn["wordStart"], "e": sn["wordEnd"],
                              "ws": sn["wbwStart"], "we": sn["wbwEnd"],
                              "ts": sn["translationStart"], "te": sn["translationEnd"]})
            bucket["words"][c["wordId"]] = items
        if c.get("root"):
            roots[c["root"]].append(c["wordId"])
    words.sort(key=lambda w: w["rank"])

    total = sum(c["quranOccurrenceCount"] for c in chapters)
    meta = {
        "version": "3", "languages": LANGS, "wordCount": len(words), "rootCount": len(roots),
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
    for (lang, ch), bucket in per.items():
        dump(DATA / "verses" / lang / f"ch_{ch:02d}.json", bucket)
    dump(DATA / "roots.json", {r: ids for r, ids in sorted(roots.items(), key=lambda kv: -len(kv[1]))})
    print(f"{len(words)} words, {len(roots)} roots, {len(per)} verse files -> {DATA}")


if __name__ == "__main__":
    main()
