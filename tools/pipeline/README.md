# QuranicWords content pipeline

This is the single, reproducible way to build the app's vocabulary content:
- `app/src/main/assets/content/*.json`
- the progress-migration map for older installs

```bash
python3 tools/pipeline/run.py          # build + validate + write assets and reports
python3 tools/pipeline/run.py --check  # build + validate only
python3 -m pytest tools/pipeline/tests # golden words, invariants, determinism
```

The only runtime dependency is the Python standard library (`pytest` is used for the tests).
External downloads are cached in `.cache/` (gitignored) and verified against the sha256 values
in `pins.json`. A changed upstream file stops the build.

## Sources

| Source | Provides | License |
|---|---|---|
| Quranic Arabic Corpus morphology v0.4, via the [mustafa0x/quran-morphology](https://github.com/mustafa0x/quran-morphology) fork | Lemma, root, part of speech, verb form and clitic segmentation for every word | GPL |
| GTAF word-by-word (`reference/word-by-word/`) | Word-aligned glosses in en, bn, ur, hi, id, tr, fa and fr | Greentech Apps Foundation public Dawah data |
| alquran.cloud editions | Full-verse translations: Saheeh International (en), Muhiuddin Khan (bn), Jalandhry (ur), Suhel Farooq Khan (hi), Indonesian Ministry (id), Diyanet (tr), Fooladvand (fa), Hamidullah (fr) | Per edition |

## How meanings are made

The goal is that a meaning is never a neighbouring word or a phrase fragment. For every lemma
and language, the pipeline:

1. **Selects clean occurrences.** QAC and GTAF are aligned word for word in all 6,236 ayahs.
   Only occurrences where the Qur'anic word is the lemma itself are used: no attached
   preposition or pronoun, apart from the article, a leading وَ/فَ, or a verb's own subject
   ending.
2. **Cleans each gloss.** GTAF's parenthesised implied words are removed, along with articles,
   the conjunction's translation and leading subject pronouns (fixed per-language lists).
3. **Votes.** Inflected variants (দিন/দিনে, gün/günü) pool their votes, and the plurality
   wins. A second sense is added when it has at least 25% of the votes.
4. **Falls back, as a last resort**, to occurrences carrying an attached pronoun, with that
   pronoun's translation stripped. These meanings are marked `meaningReviewed: false` and
   listed in `reports/review_queue.tsv`.
5. **Ships only complete words.** A lemma ships only if every language has a meaning that
   passes the shared fragment checks. The rest go to `reports/excluded_lemmas.tsv`.

Function words are a closed set (82 entries). Their display forms and meanings are reviewed by
hand in `overrides/function_words.tsv`, which also merges QAC's split duplicates (مَا, إِذَا…).

## Curriculum

- Chapter 1 teaches the function words.
- Chapters 2–10 alternate verbs and nouns by frequency band, so اللَّه, رَبّ and يَوْم come
  early. Each chapter is still a single part of speech.
- Sections hold 10 lessons of 5 words each, followed by a review and an exam. Each chapter
  ends with a chapter exam.
- Each word gets a teach step, a meaning quiz, a verse exercise (fill-in-the-blank or
  tap-the-word), a listening quiz when verified audio exists, and a closing matching round.
- Distractors come from the same chapter. They never share the answer's written form, root,
  or any meaning in any language.

## Audio

Clips from commit `c21e5cdd` (Google TTS) are reused only where their source text is read
identically to the new word (`arabic.pronunciation_key`). Clips that match nothing are listed
in `reports/unused_audio_clips.txt` and removed from the assets.

## Validation (`qw/validate.py`)

The build fails on any of these:
- duplicate ids or duplicate words
- a missing language
- a fragment or stopword meaning
- an out-of-range verse span
- broken or ambiguous options
- a word taught in anything other than exactly one regular lesson
- a lesson with nothing scored
- a stored count that disagrees with the content
