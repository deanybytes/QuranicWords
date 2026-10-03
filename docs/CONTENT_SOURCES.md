# 📚 Content Sources

The repository's rule is to **source content, never invent it**. All vocabulary content is
produced by the reproducible pipeline in [`tools/pipeline`](../tools/pipeline/README.md) from the
pinned sources below. Every download is checksum-verified against `tools/pipeline/pins.json`.

| Source | License | Provides |
|---|---|---|
| [Quranic Arabic Corpus](https://corpus.quran.com) morphology v0.4, via the [mustafa0x/quran-morphology](https://github.com/mustafa0x/quran-morphology) fork (spelling and root fixes) | GPL (attribution and link-back to corpus.quran.com) | Lemma, root, part of speech, verb form and clitic segmentation for all 77,429 words |
| [quran.gtaf.org](https://quran.gtaf.org) word-by-word (Greentech Apps Foundation), bundled in `reference/word-by-word/` | Public Dawah data | Word-aligned glosses in English, Bangla, Urdu, Hindi, Indonesian, Turkish, Persian and French; the Uthmani word forms used for verses |
| [alquran.cloud](https://alquran.cloud) editions | Per edition | Full-verse translations: Saheeh International (en), Muhiuddin Khan (bn), Fateh Muhammad Jalandhry (ur), Suhel Farooq Khan & Saifur Rahman Nadwi (hi), Indonesian Ministry of Religious Affairs (id), Diyanet İşleri (tr), Mohammad Mahdi Fooladvand (fa), Muhammad Hamidullah (fr) |
| [Amiri](https://github.com/aliftype/amiri), [Scheherazade New](https://github.com/silnrsi/font-scheherazade), [Noto Naskh Arabic](https://github.com/notofonts/arabic), [Lateef](https://github.com/silnrsi/font-lateef), [Noto Nastaliq Urdu](https://github.com/notofonts/nastaliq) | SIL OFL 1.1 | Qur'anic script typefaces |

## How a meaning is verified

1. **Word-by-word alignment.** QAC and GTAF agree on the number of words in every one of the
   6,236 ayahs, so each QAC word is paired with its GTAF gloss by position.
2. **Clean occurrences only.** For each lemma the pipeline uses only occurrences where the Qur'anic
   word *is* the lemma. It allows the article, a leading وَ/فَ, or a verb's own subject ending, and
   nothing else attached. This is what prevents the previous failure, where a word's "meaning"
   was really a neighbouring word or a whole phrase.
3. **Cleaning and voting.** Implied words in parentheses, articles, and the conjunction's
   translation are removed. Inflected variants pool their votes. A second sense is kept when it is
   clearly attested (at least 25% of occurrences).
4. **Last resort.** For words that only occur with an attached pronoun, the pronoun's
   translation is stripped. These meanings are flagged `meaningReviewed: false` and listed in
   `reports/review_queue.tsv` for native-speaker review.
5. **Ship rule.** A lemma ships only if all 8 languages have a meaning that passes the shared
   fragment checks.
6. **Function words.** The 82 function-word entries are reviewed by hand in
   `tools/pipeline/overrides/function_words.tsv`.

## Languages

The app supports **8 languages** for both interface and content: en, bn, ur, hi, id, tr, fa and
fr. Malay, Hausa and Swahili were removed, because no verified word-by-word source exists for them.
Learners who had chosen one of them are switched to English automatically.

## Attribution obligations

- **Quranic Arabic Corpus.** The GPL requires attribution and a link back to
  https://corpus.quran.com. This is given in-app under Settings → About and in `NOTICE`.
- **Fonts.** The fonts are covered by SIL OFL 1.1 (see `NOTICE`).
