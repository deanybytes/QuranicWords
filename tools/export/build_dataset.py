#!/usr/bin/env python3
"""Builds the open dataset in dataset/ from the app's verified curriculum (tools/pipeline
output), so researchers and developers can reuse the vocabulary without rebuilding it.

    python3 tools/export/build_dataset.py            # writes dataset/
    python3 tools/export/build_dataset.py --zip OUT  # also writes a release zip

    dataset/words.json      3,833 lemmas: Arabic, root, grammar, frequency, curriculum position,
                            meanings and senses in 8 languages, each sense tied to an ayah
    dataset/words.csv       the same words, one row each (meanings as columns)
    dataset/roots.json/.csv every root with its words and total occurrences
    dataset/verses.json     every ayah the senses cite: Uthmani text, word-by-word gloss and full
                            translation in 8 languages, with the exact character spans
    dataset/metadata.json   counts, languages, sources (with pinned sha256) and licences
"""
import argparse
import csv
import io
import json
import zipfile
from collections import defaultdict
from pathlib import Path

from slugs import root_slugs, word_slug

ROOT = Path(__file__).resolve().parents[2]
CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"
OUT = ROOT / "dataset"
PINS = ROOT / "tools" / "pipeline" / "pins.json"
LANGS = ["en", "bn", "ur", "hi", "in", "tr", "fa", "fr"]
LANG_NAMES = {"en": "English", "bn": "Bangla", "ur": "Urdu", "hi": "Hindi", "in": "Indonesian",
              "tr": "Turkish", "fa": "Persian", "fr": "French"}
CATEGORY = {"PARTICLE": "particle", "VERB": "verb", "NOUN": "noun"}
SITE = "https://quranicwords.vercel.app"
VERSION = "1.0.0"
# Same editions as tools/pipeline/qw/config.py VERSE_EDITIONS (shown in the dataset's metadata).
TRANSLATIONS = {
    "en": "Saheeh International", "bn": "Dr. Abu Bakr Muhammad Zakaria",
    "ur": "Dr. Israr Ahmad (Bayan-ul-Quran)", "hi": "Maulana Azizul Haque al-Umari",
    "in": "Indonesian Ministry of Religious Affairs", "tr": "Muslim Shahin",
    "fa": "Hussein Taji Kal Dari", "fr": "Muhammad Hamidullah",
}


def load(name):
    return json.loads((CONTENT / name).read_text(encoding="utf-8"))


def lines_json(items):
    """A JSON array with one record per line: valid JSON that still diffs line by line."""
    return "[\n" + ",\n".join(json.dumps(i, ensure_ascii=False, separators=(",", ":")) for i in items) + "\n]\n"


def build():
    chapters = load("chapters.json")["chapters"]
    sections = load("sections.json")["sections"]
    lessons = {l["id"]: l for l in load("lessons_vocabulary.json")["lessons"]}
    verses = load("verses.json")
    freq = {w["id"]: w for w in load("word_frequency.json")["words"]}
    ch_num = {c["id"]: c["sortOrder"] for c in chapters}
    ch_title = {c["sortOrder"]: c["title"] for c in chapters}
    sec_num = {s["id"]: s["sortOrder"] for s in sections}
    lesson_no, counters = {}, defaultdict(int)
    for l in load("lessons_vocabulary.json")["lessons"]:
        if l["kind"] == "REGULAR":
            counters[l["sectionId"]] += 1
            lesson_no[l["id"]] = counters[l["sectionId"]]

    words, cited = [], set()
    for e in load("exercises_vocabulary.json"):
        if e["exerciseType"] != "WORD_INTRO":
            continue
        c = e["content"]
        lesson = lessons[e["lessonId"]]
        senses = {}
        for lang in LANGS:
            senses[lang] = []
            for s in c["senses"][lang]:
                cited.add(s["verse"])
                senses[lang].append({
                    "meaning": s["meaning"], "verse": s["verse"], "word_index": s["word"],
                    "arabic_span": [s["wordStart"], s["wordEnd"]],
                    "translation_span": [s["translationStart"], s["translationEnd"]]
                    if s["translationStart"] is not None else None,
                    "word_by_word_span": [s["wbwStart"], s["wbwEnd"]],
                })
        vf = {k: c[k] for k in ("verbForm", "pastArabic", "presentArabic", "masdarArabic") if c.get(k)}
        words.append({
            "id": c["wordId"],
            "rank": freq[c["wordId"]]["frequencyRank"],
            "arabic": c["arabicWord"],
            "transliteration": c.get("transliteration") or None,
            "root": c.get("root"),
            "category": CATEGORY[c["lemmaCategory"]],
            "part_of_speech": c.get("partOfSpeechLabel") or None,
            "verb_form": {"form": vf.get("verbForm"), "label": c.get("verbFormLabel"),
                          "past": vf.get("pastArabic"), "present": vf.get("presentArabic"),
                          "masdar": vf.get("masdarArabic")} if vf else None,
            "occurrences": c["quranOccurrenceCount"],
            "curriculum": {"chapter": ch_num[lesson["chapterId"]], "section": sec_num[lesson["sectionId"]],
                           "lesson": lesson_no[e["lessonId"]]},
            "meaning": {lang: c["meaning"][lang] for lang in LANGS},
            "meaning_reviewed": {lang: bool(c["meaningReviewed"][lang]) for lang in LANGS},
            "senses": senses,
            "url": f"{SITE}/quran-words/{word_slug(c['wordId'], c.get('transliteration'))}.html",
        })
    words.sort(key=lambda w: w["rank"])

    by_root = defaultdict(list)
    for w in words:
        if w["root"]:
            by_root[w["root"]].append(w)
    slugs = root_slugs(by_root)
    roots = [{
        "root": r, "slug": slugs[r], "word_count": len(ws),
        "occurrences": sum(w["occurrences"] for w in ws), "words": [w["id"] for w in ws],
        "url": f"{SITE}/quran-roots/{slugs[r]}.html",
    } for r, ws in sorted(by_root.items(), key=lambda kv: (-sum(w["occurrences"] for w in kv[1]), kv[0]))]

    def verse_key(k):
        s, a = k.split(":")
        return int(s), int(a)

    verse_rows = [{
        "verse": k, "reference": verses[k]["ref"], "arabic": verses[k]["ar"],
        "word_by_word": {lang: verses[k]["wbw"][lang] for lang in LANGS},
        "translation": {lang: verses[k]["tr"][lang] for lang in LANGS},
    } for k in sorted(cited, key=verse_key)]

    total = sum(c["quranOccurrenceCount"] for c in chapters)
    pins = json.loads(PINS.read_text(encoding="utf-8"))
    meta = {
        "name": "QuranicWords open vocabulary dataset",
        "version": VERSION,
        "description": "The 3,833 most important lemmas of the Qur'an in curriculum order, with "
                       "roots, grammar, frequencies and meanings in 8 languages; every meaning is "
                       "tied to a complete ayah with exact character spans.",
        "homepage": SITE, "repository": "https://github.com/deanybytes/QuranicWords",
        "license": "GPL-3.0-or-later (derived from the GPL-licensed Quranic Arabic Corpus); "
                   "third-party texts keep their own terms, see dataset/README.md",
        "counts": {"words": len(words), "roots": len(roots), "verses": len(verse_rows),
                   "occurrences_covered": total,
                   "coverage_percent": round(sum(c["quranOccurrencePercent"] for c in chapters), 1)},
        "languages": {lang: LANG_NAMES[lang] for lang in LANGS},
        "language_codes_note": "'in' is Indonesian (ISO 639-1 'id'); it matches Android's legacy locale code.",
        "chapters": [{"chapter": c["sortOrder"], "title": c["title"], "words": c["wordCount"],
                      "coverage_percent": c["quranOccurrencePercent"]} for c in chapters],
        "translations": TRANSLATIONS,
        "sources": {
            "morphology": "Quranic Arabic Corpus v0.4 (corpus.quran.com), via github.com/mustafa0x/quran-morphology",
            "word_by_word": "Greentech Apps Foundation word-by-word translations (quran.gtaf.org)",
            "verse_translations": "alquran.cloud and quran.com API v4 editions (see translations)",
            "quran_text": "Tanzil Quran Text, Uthmani (tanzil.net), used to verify every cited ayah",
            "pinned_sha256": pins,
        },
    }
    return words, roots, verse_rows, meta, ch_title


def words_csv(words):
    buf = io.StringIO()
    cols = ["id", "rank", "arabic", "transliteration", "root", "category", "part_of_speech_en",
            "verb_form", "occurrences", "chapter", "section", "lesson",
            *[f"meaning_{lang}" for lang in LANGS], "example_verse", "url"]
    wr = csv.writer(buf, lineterminator="\n")
    wr.writerow(cols)
    for w in words:
        wr.writerow([w["id"], w["rank"], w["arabic"], w["transliteration"] or "", w["root"] or "",
                     w["category"], (w["part_of_speech"] or {}).get("en", ""),
                     (w["verb_form"] or {}).get("form") or "", w["occurrences"],
                     w["curriculum"]["chapter"], w["curriculum"]["section"], w["curriculum"]["lesson"],
                     *[w["meaning"][lang] for lang in LANGS], w["senses"]["en"][0]["verse"], w["url"]])
    return buf.getvalue()


def roots_csv(roots, words):
    ar = {w["id"]: w["arabic"] for w in words}
    buf = io.StringIO()
    wr = csv.writer(buf, lineterminator="\n")
    wr.writerow(["root", "slug", "word_count", "occurrences", "words_arabic", "word_ids"])
    for r in roots:
        wr.writerow([r["root"], r["slug"], r["word_count"], r["occurrences"],
                     " ".join(ar[i] for i in r["words"]), " ".join(r["words"])])
    return buf.getvalue()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--zip", type=Path, help="also write a zip of dataset/ to this path")
    args = ap.parse_args()
    words, roots, verse_rows, meta, _ = build()
    files = {
        "words.json": lines_json(words),
        "words.csv": words_csv(words),
        "roots.json": lines_json(roots),
        "roots.csv": roots_csv(roots, words),
        "verses.json": lines_json(verse_rows),
        "metadata.json": json.dumps(meta, ensure_ascii=False, indent=2) + "\n",
    }
    OUT.mkdir(exist_ok=True)
    for name, text in files.items():
        # CSV gets a BOM so spreadsheet apps open the Arabic and other scripts as UTF-8.
        (OUT / name).write_text(("﻿" if name.endswith(".csv") else "") + text, encoding="utf-8")
    print(f"dataset: {len(words)} words, {len(roots)} roots, {len(verse_rows)} verses -> {OUT}")
    if args.zip:
        args.zip.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(args.zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
            for p in sorted(OUT.iterdir()):
                if p.is_file():
                    z.write(p, f"QuranicWords-dataset/{p.name}")
        print(f"zip: {args.zip} ({args.zip.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
