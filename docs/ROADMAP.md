# 🗺️ Roadmap

QuranicWords is a focused Quranic vocabulary app structured around the three traditional parts of speech: **Fi'l (Verbs)**, **Ḥarf (Particles)**, and **Ism (Nouns)**, ordered strictly by frequency of occurrence in the Qur'an.

## ✅ Built so far

- [x] **Comprehensive Master Curriculum (4,709 Quranic Lemmas)**:
  - ✨ **Ḥarf (Particles)**: 173 words across 10 sections in Chapter 1 (24,651 occurrences, 41.16% of Qur'an).
  - ⚡ **Fi'l (Verbs)**: 1,479 verbs across 30 sections in Chapters 2 to 4 (13,491 occurrences).
  - 📖 **Ism (Nouns)**: 3,057 nouns across 60 sections in Chapters 5 to 10 (21,746 occurrences).
  - Total: 10 Chapters, 100 Sections, 1,217 Lessons, 14,358 Exercises (~80%+ Quranic coverage).
- [x] **Complete Arabic Vocalization (Tashkīl Overhaul)**: 100% diacritical preservation (sukūn, fatḥah, kasrah, ḍammah, shaddah, tanwīn) on target words and in-verse highlights.
- [x] **Contextual Polysemy (Wujūh al-Qur'an)**: Multi-sense contextual meanings and authentic verse examples per word.
- [x] **6-Mode Test Hub**: Dedicated testing for Ism, Fi'l, Ḥarf, Mix/Random (with live grammar tags), adaptive Mistaken Words Review, and Chapterwise Practice.
- [x] **End-of-Lesson Performance Summary**: Words covered (*Alhamdulillah*), mistake count, accuracy percentage, time spent, and next lesson preview.
- [x] **Streamlined Font Picker**: Clean typeface selection showing font name and live Surah Al-Kawthar preview (5 bundled OFL fonts).
- [x] **Low-Latency System Audio**: Interactive sound effects (`SfxPlayer.kt`) via Android `SoundPool`.
- [x] **Achievements System**: 17 unlockable milestone badges with custom-drawn medallions.
- [x] **11-Language Architecture**: English, বাংলা, اردو, हिन्दी, Bahasa Indonesia, Bahasa Melayu, Türkçe, فارسی, Hausa, Kiswahili, Français. 100% verified glosses and in-verse highlights.
- [x] **100% Offline & Private**: Zero network dependencies, zero telemetry, local backup export/import via SAF.
- [x] **Play Store v1.0.0 & v1.0.1 Production Releases**: targetSdk 36 (Android 16), unstripped native debug symbols, language split disabled for offline multi-language use.
- [x] **Unified Multi-Sense & Zero-Collision Distractors (v1.0.1)**: Harmonized all 73 polysemous words with slash-separated senses across 10 languages and eliminated all distractor collisions across 4,709 exercises.
- [x] **Complete Curriculum Synthesis & Auto-Healing Progression (v1.0.1, versionCode 20 & 21)**: Synthesized closing matching quizzes for all 997 regular lessons (1,207 matching exercises total), populated all 210 exam and flashback lessons (14,358 exercises total across 1,217 lessons; 0 empty lessons), implemented auto-healing curriculum unlock progression and auto-expand active units, deranged matching exercise randomization, aligned completed/active lesson node icons, resolved consecutive-lesson stability, and reseeded database with `ContentSeeder.CONTENT_VERSION = 34`.
- [x] **Surah Names, Auto-Navigation & Quality Hardening (v1.0.0, versionCode 25)**: Added canonical Surah names to verse references, auto-scroll and auto-collapse curriculum navigation on Home screen, direct roadmap navigation for unlocked & completed units, widget locale formatting robustness, consolidated Indonesian string resources, and enforced zero-warning compiler and lint quality gates.
- [x] **Open Source & Ecosystem**: GPL-3.0 licensed on GitHub, part of the DEANY TALKS Dawah platform ecosystem.

## 🔜 Future Enhancements

| Item | Description |
|---|---|
| **Curriculum Search** | Fast fuzzy search across all 4,709 words with root and meaning filtering |
| **Spaced Repetition Flashcards** | Advanced SRS algorithm for customized daily word reviews |
| **Tajweed Rules Visualizer** | Interactive color-coded Tajweed indicators for Quranic verse examples |

