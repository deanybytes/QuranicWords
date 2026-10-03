#!/usr/bin/env python3
"""QuranicWords content build - the single, reproducible way to produce the app's content.

    python tools/pipeline/run.py              # build + validate, write assets and report
    python tools/pipeline/run.py --check      # build + validate only, write nothing
    python tools/pipeline/run.py --pin        # first run: record sha256 of downloaded sources
"""
import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from qw import config, curriculum, legacy, sources, validate, words  # noqa: E402


def surah_names():
    src = (config.ROOT / "app/src/main/java/com/quranicwords/app/core/util/SurahNames.kt").read_text(encoding="utf-8")
    names = re.findall(r'Entry\("([^"]+)"', src)
    assert len(names) == 114, len(names)
    return {i: n for i, n in enumerate(names, 1)}


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"), sort_keys=False)


def build(pin=False):
    qac = sources.load_qac(pin)
    gtaf = sources.load_gtaf()
    verse_tr = sources.load_verse_translations(pin)
    ws, excluded, total = words.build_words(qac, gtaf, verse_tr)
    b = curriculum.Builder(ws, gtaf, verse_tr, surah_names(), total)
    b.build()
    assets = {
        "chapters": {"chapters": b.chapters},
        "sections": {"sections": b.sections},
        "lessons": {"lessons": b.lessons},
        "exercises": {"exercises": b.exercises},
        "word_frequency": {"words": b.word_frequency()},
        "verses": b.verse_store,
    }
    legacy_map, legacy_stats = legacy.build(ws, qac, gtaf["en"])
    low_conf = sum(1 for w in ws for lang in config.LANGS if not w.reviewed[lang])
    report = {
        "words": len(ws), "excluded": len(excluded),
        "by_track": {t: sum(1 for w in ws if w.track == t) for t in ("FUNCTION", "VERB", "NOUN")},
        "chapters": len(b.chapters), "sections": len(b.sections), "lessons": len(b.lessons),
        "exercises": len(b.exercises), "total_quran_segments": total,
        "taught_occurrences": sum(w.frequency for w in ws),
        "coverage_percent": round(100 * sum(w.frequency for w in ws) / total, 2),
        "low_confidence_meanings": low_conf, "legacy_mapping": legacy_stats,
    }
    report["verses"] = len(b.verse_store)
    report["senses"] = sum(len(w.senses[l]) for w in ws for l in config.LANGS)
    report["translation_highlights"] = sum(1 for w in ws for l in config.LANGS for sn in w.senses[l] if sn.example.tr_start is not None)
    return assets, legacy_map, report, excluded, ws, (qac, gtaf, verse_tr)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--pin", action="store_true")
    args = ap.parse_args()
    assets, legacy_map, report, excluded, ws, raw = build(args.pin)
    errors = validate.run(assets, raw)
    ayah_errors = validate.check_complete_ayahs(assets["verses"], sources.load_reference_arabic(args.pin))
    report["verses_checked_against_reference_text"] = len(assets["verses"]) if not ayah_errors else 0
    errors += ayah_errors
    report["validation_errors"] = len(errors)
    for e in errors[:50]:
        print("  ✗", e)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if errors:
        print(f"{len(errors)} validation errors")
        return 1
    if args.check:
        return 0
    files = {"chapters.json": assets["chapters"], "sections.json": assets["sections"],
             # A top-level array so the app can stream-decode it (Json.decodeToSequence) instead
             # of materializing ~70 MB of objects at once.
             "lessons_vocabulary.json": assets["lessons"], "exercises_vocabulary.json": assets["exercises"]["exercises"],
             "word_frequency.json": assets["word_frequency"], "legacy_progress_map.json": legacy_map,
             "verses.json": assets["verses"]}
    manifest = {}
    for name, obj in files.items():
        data = dump(obj).encode("utf-8")
        (config.ASSETS / name).write_bytes(data)
        manifest[name] = hashlib.sha256(data).hexdigest()
    config.REPORTS.mkdir(parents=True, exist_ok=True)
    (config.REPORTS / "build_report.json").write_text(json.dumps({**report, "asset_sha256": manifest}, indent=2, ensure_ascii=False) + "\n")
    with open(config.REPORTS / "excluded_lemmas.tsv", "w", encoding="utf-8") as f:
        f.write("key\tlemma\tfrequency\treason\n")
        for x in sorted(excluded, key=lambda x: -x["frequency"]):
            f.write(f"{x['key']}\t{x['lemma']}\t{x['frequency']}\t{x['reason']}\n")
    with open(config.REPORTS / "review_queue.tsv", "w", encoding="utf-8") as f:
        f.write("word_id\tarabic\tlang\tmeaning\tsupport\ttotal\tmethod\n")
        for w in sorted(ws, key=lambda w: -w.frequency):
            for lang in config.LANGS:
                if not w.reviewed[lang]:
                    c, t, m = w.confidence[lang]
                    f.write(f"{w.id}\t{w.arabic}\t{lang}\t{w.meaning[lang]}\t{c}\t{t}\t{m}\n")
    print("assets written to", config.ASSETS)
    return 0


if __name__ == "__main__":
    sys.exit(main())
