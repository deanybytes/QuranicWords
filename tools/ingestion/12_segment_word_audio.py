# -*- coding: utf-8 -*-
"""
Segments single-word pronunciation clips from EveryAyah.com's Alafasy recitation, using
cpfair/quran-align's word-level timing data (both CC BY 4.0 - see NOTICE) to find each word's
millisecond range within its verse's audio, then ffmpeg to cut it out.

Only words with a confirmed `arabicWordStart`/`arabicWordEnd` character span (computed by the
earlier `10_add_highlight_spans.py` pass, ~1,582 of 3,680 words - see docs/CONTENT_SOURCES.md)
can be segmented at all, since that's what locates the word within its verse's word-index
sequence in the first place.

This script processes a bounded SAMPLE (see MAX_WORDS below), not the full word list - fetching,
aligning, and segmenting all ~1,582 eligible words means downloading well over a thousand verse
recordings, which is real network/processing work better run as its own batch job than inline
here. Treat this as a verified, working proof of the pipeline, not a claim that every word's
audio exists yet - see docs/CONTENT_SOURCES.md's "explicitly not sourced" table and MEMORY.md for
the honest current state. Re-run with a higher MAX_WORDS (or None for all) to extend coverage.

Usage: python3 tools/ingestion/12_segment_word_audio.py [max_words]
"""
import json
import subprocess
import sys
import urllib.request
from pathlib import Path

CONTENT_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets" / "content"
CACHE_DIR = Path(__file__).parent / "output" / "audio_cache"
OUT_DIR = Path(__file__).parent / "output" / "word_audio"

RECITER = "Alafasy_128kbps"
EVERYAYAH_BASE = f"https://everyayah.com/data/{RECITER}"
QURAN_ALIGN_RELEASE = (
    "https://github.com/cpfair/quran-align/releases/download/"
    "release-2016-11-24/quran-align-data-2016-11-24.zip"
)
ALIGN_JSON_NAME = f"{RECITER}.json"

PAD_MSEC = 40  # small symmetric padding so the cut doesn't clip the word's attack/release
MAX_WORDS = int(sys.argv[1]) if len(sys.argv) > 1 else 20


def ensure_align_data() -> list:
    align_path = CACHE_DIR / ALIGN_JSON_NAME
    if not align_path.exists():
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        zip_path = CACHE_DIR / "quran-align-data.zip"
        print(f"Fetching quran-align data ({QURAN_ALIGN_RELEASE})...")
        urllib.request.urlretrieve(QURAN_ALIGN_RELEASE, zip_path)
        import zipfile
        with zipfile.ZipFile(zip_path) as zf:
            zf.extract(ALIGN_JSON_NAME, CACHE_DIR)
        zip_path.unlink()
    with open(align_path, encoding="utf-8") as f:
        return json.load(f)


def ensure_verse_audio(surah: int, ayah: int) -> Path:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    dest = CACHE_DIR / f"{surah:03d}{ayah:03d}.mp3"
    if not dest.exists():
        url = f"{EVERYAYAH_BASE}/{surah:03d}{ayah:03d}.mp3"
        urllib.request.urlretrieve(url, dest)
    return dest


def word_index_for_span(verse_text: str, start: int) -> int:
    """quran-align's segment indices split the verse by plain whitespace - count complete
    space-separated tokens before `start` to find which 0-based word index it falls in."""
    return len(verse_text[:start].split())


def find_segment(align_entry: dict, word_index: int):
    for seg in align_entry["segments"]:
        seg_start, seg_end, start_msec, end_msec = seg
        if seg_start <= word_index < seg_end:
            return start_msec, end_msec
    return None


def main():
    words = json.load(open(CONTENT_DIR / "word_frequency.json", encoding="utf-8"))["words"]
    words.sort(key=lambda w: w["frequencyRank"])
    exercises = json.load(open(CONTENT_DIR / "exercises_vocabulary.json", encoding="utf-8"))["exercises"]
    word_intro_by_id = {
        e["content"]["wordId"]: e["content"]
        for e in exercises
        if e["content"]["type"] == "word_intro" and e["content"].get("arabicWordStart") is not None
    }

    eligible = [w for w in words if w["id"] in word_intro_by_id]
    sample = eligible if MAX_WORDS is None else eligible[:MAX_WORDS]
    print(f"{len(eligible)} words have a confirmed span; processing {len(sample)}")

    align_by_key = {}
    for entry in ensure_align_data():
        align_by_key[(entry["surah"], entry["ayah"])] = entry

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    produced, skipped = [], []

    for word in sample:
        wi = word_intro_by_id[word["id"]]
        surah_str, ayah_str = wi["exampleVerseReference"].split(":")
        surah, ayah = int(surah_str), int(ayah_str)
        align_entry = align_by_key.get((surah, ayah))
        if align_entry is None:
            skipped.append((word["id"], "no alignment data for this verse"))
            continue

        word_index = word_index_for_span(wi["exampleVerseArabic"], wi["arabicWordStart"])
        segment = find_segment(align_entry, word_index)
        if segment is None:
            skipped.append((word["id"], f"no segment for word index {word_index}"))
            continue
        start_msec, end_msec = segment

        try:
            verse_audio = ensure_verse_audio(surah, ayah)
        except Exception as exc:
            skipped.append((word["id"], f"verse audio fetch failed: {exc}"))
            continue

        out_path = OUT_DIR / f"{word['id']}.mp3"
        start_sec = max(0, start_msec - PAD_MSEC) / 1000.0
        duration_sec = (end_msec - start_msec + 2 * PAD_MSEC) / 1000.0
        result = subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error",
                "-ss", f"{start_sec:.3f}", "-i", str(verse_audio), "-t", f"{duration_sec:.3f}",
                "-ar", "44100", "-ac", "1", "-c:a", "libmp3lame", "-q:a", "4",
                str(out_path),
            ],
            capture_output=True,
        )
        if result.returncode != 0 or not out_path.exists():
            skipped.append((word["id"], f"ffmpeg failed: {result.stderr.decode(errors='replace')[:200]}"))
            continue
        produced.append(word["id"])

    print(f"produced {len(produced)} clips in {OUT_DIR}")
    if skipped:
        print(f"skipped {len(skipped)}:")
        for word_id, reason in skipped[:20]:
            print(f"  {word_id}: {reason}")


if __name__ == "__main__":
    main()
