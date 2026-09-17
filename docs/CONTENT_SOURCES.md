# 📚 Content Sources

This repo's discipline is to **flag unsourced content rather than fabricate it**. This doc catalogs the real, open-licensed sources used for the 4,709-word vocabulary curriculum across the three primary parts of speech (**Ḥarf**, **Fi'l**, **Ism**), along with contextual polysemy (**Wujūh al-Qur'an**) and verse alignment data across 11 languages.

## Sourced Corpora

| Source | License | Provides | Used for |
|---|---|---|---|
| [Quranic Arabic Corpus](https://corpus.quran.com) (corpus.quran.com) | GPL, with explicit attribution/link-back requirement | Lemmatized Quranic vocabulary frequency & morphological part-of-speech categorization (verbs, particles, nouns) | Complete frequency rankings and POS classification |
| [Quran-bil-Quran](https://github.com/R3GENESI5/quran-bil-quran) | MIT | `roots_index.json` (triliteral root index, meanings, occurrences) and `verses_text.json` (6,236 Uthmani Arabic verses) | Root classification, verse citations, and Arabic text |
| [risan/quran-json](https://github.com/risan/quran-json) | CC BY-SA 4.0 | Full Bangla verse translations for all 114 surahs | Bengali contextual verse translations |
| [quran.gtaf.org](https://quran.gtaf.org) (Greentech Apps Foundation) | Public Dawah API | Comprehensive word-by-word Arabic glosses across multiple global languages | Multi-language vocabulary meanings and polysemic verification |
| [Amiri](https://github.com/aliftype/amiri), [Scheherazade New](https://github.com/silnrsi/font-scheherazade), [Noto Naskh Arabic](https://github.com/notofonts/arabic), [Lateef](https://github.com/silnrsi/font-lateef), [Noto Nastaliq Urdu](https://github.com/notofonts/nastaliq) | SIL OFL 1.1 | 5 bundled open-license Quranic script typefaces | In-app Quran typography and font selection |

## Curriculum Ingestion & Asset Compilation

`tools/ingestion/compile_10lang_curriculum_assets.py` and `tools/ingestion/audit_and_align_10lang_corpus.py` compile the production curriculum:
1. **Ḥarf (Particles)**: 173 particles partitioned into 10 sections in Chapter 1.
2. **Fi'l (Verbs)**: 1,479 verbs partitioned into 30 sections across Chapters 2 to 4.
3. **Ism (Nouns)**: 3,057 nouns partitioned into 60 sections across Chapters 5 to 10.
4. **Compiled JSON Assets**: Emits `chapters.json`, `sections.json`, `lessons_vocabulary.json`, `exercises_vocabulary.json`, and `word_frequency.json` containing all 4,709 lemmas across 11 languages.

## Contextual Polysemy (Wujūh al-Qur'an) & Full Tashkīl

- **Multi-sense tabs & Unified Meanings**: Words with distinct Quranic connotations carry structured `polysemyEntries` with dedicated contextual verse occurrences and translations. All 73 polysemous lemmas across Harf, Fil, and Ism combine senses via `' / '` separators across 10 languages (curated via `tools/fixes/unify_multisense_and_clean_quizzes.py`).
- **Tashkīl & Ḥarakāt Preservation**: Character spans (`arabicWordStart`/`arabicWordEnd`) preserve diacritical markings across all verses. Complete vocalization curated via `tools/fixes/curate_vocalization_and_diacritics.py` ensuring 100% presence of sukūn, fatḥah, kasrah, ḍammah, shaddah, and tanwīn.

## Audio Architecture

- **System Sound Effects**: Interactive feedback sounds (`CORRECT`, `WRONG`, `LESSON_COMPLETE`, `EXAM_PASS`, etc.) are synthesized and played via low-latency Android `SoundPool` (`SfxPlayer.kt`).
- **Word Pronunciation**: Word pronunciation playback buttons and listening quizzes have been removed to focus entirely on visual reading, contextual comprehension, and retention.

## Attribution Obligations

The bundled `NOTICE.txt` file (available in-app under Settings → About → Licenses & Sources) details full licensing and credit:
- **Quranic Arabic Corpus (GPL-3.0)**: Morphology and lemma data.
- **Quran-bil-Quran (MIT)**: Root index and English translations.
- **risan/quran-json (CC BY-SA 4.0)**: Bengali Quran translations.
- **SIL OFL 1.1 Fonts**: Amiri, Scheherazade New, Noto Naskh, Lateef, Noto Nastaliq Urdu.
