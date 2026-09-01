# 📚 Content Sources

This repo's discipline is to **flag unsourced content rather than fabricate it**. This doc catalogs the real, open-licensed sources used for the 4,616-word vocabulary curriculum across the three primary parts of speech (**Fi'l**, **Ḥarf**, **Ism**), along with contextual polysemy (**Wujūh al-Qur'an**) and verse alignment data.

## Sourced Corpora

| Source | License | Provides | Used for |
|---|---|---|---|
| [Quranic Arabic Corpus](https://corpus.quran.com) (corpus.quran.com) | GPL, with explicit attribution/link-back requirement | Lemmatized Quranic vocabulary frequency & morphological part-of-speech categorization (verbs, particles, nouns) | Complete frequency rankings and POS classification |
| [Quran-bil-Quran](https://github.com/R3GENESI5/quran-bil-quran) | MIT | `roots_index.json` (triliteral root index, meanings, occurrences) and `verses_text.json` (6,236 Uthmani Arabic verses) | Root classification, verse citations, and Arabic text |
| [risan/quran-json](https://github.com/risan/quran-json) | CC BY-SA 4.0 | Full Bangla verse translations for all 114 surahs | Bengali contextual verse translations |
| [quran.gtaf.org](https://quran.gtaf.org) (Greentech Apps Foundation) | Public Dawah API | Comprehensive word-by-word Arabic glosses across 12 languages | Multi-language vocabulary meanings and polysemic verification |
| [Amiri](https://github.com/aliftype/amiri), [Scheherazade New](https://github.com/silnrsi/font-scheherazade), [Noto Naskh Arabic](https://github.com/notofonts/arabic), [Lateef](https://github.com/silnrsi/font-lateef), [Noto Nastaliq Urdu](https://github.com/notofonts/nastaliq) | SIL OFL 1.1 | 5 bundled open-license Quranic script typefaces | In-app Quran typography and font selection |

## Curriculum Ingestion & POS Builder

`tools/ingestion/01_build_pos_curriculum.py` builds the modularized v2.2.0 curriculum:
1. **Fi'l (Verbs)**: 1,450 verbs partitioned into 15 sections (`section_fil_01.json` to `section_fil_15.json`), 145 lessons.
2. **Ḥarf (Particles)**: 109 particles partitioned into 2 sections (`section_harf_01.json` to `section_harf_02.json`), 11 lessons.
3. **Ism (Nouns)**: 3,057 nouns partitioned into 31 sections (`section_ism_01.json` to `section_ism_31.json`), 306 lessons.
4. **Manifest & Frequency Table**: Emits `curriculum_manifest.json` and `word_frequency.json` containing all 4,616 words.

## Contextual Polysemy (Wujūh al-Qur'an) & Full Tashkīl

- **Multi-sense tabs**: Words with distinct Quranic connotations carry structured `polysemyEntries` with dedicated contextual verse occurrences and translations.
- **Tashkīl & Ḥarakāt Preservation**: Character spans (`arabicWordStart`/`arabicWordEnd`) preserve diacritical markings across all verses.

## Audio Architecture

- **System Sound Effects**: Interactive feedback sounds (`CORRECT`, `WRONG`, `LESSON_COMPLETE`, `EXAM_PASS`, etc.) are synthesized and played via low-latency Android `SoundPool` (`SfxPlayer.kt`).
- **Word Pronunciation**: Word pronunciation playback buttons and listening quizzes have been removed to focus entirely on visual reading, contextual comprehension, and retention.

## Attribution Obligations

The bundled `NOTICE.txt` file (available in-app under Settings → About → Licenses & Sources) details full licensing and credit:
- **Quranic Arabic Corpus (GPL-3.0)**: Morphology and lemma data.
- **Quran-bil-Quran (MIT)**: Root index and English translations.
- **risan/quran-json (CC BY-SA 4.0)**: Bengali Quran translations.
- **SIL OFL 1.1 Fonts**: Amiri, Scheherazade New, Noto Naskh, Lateef, Noto Nastaliq Urdu.
