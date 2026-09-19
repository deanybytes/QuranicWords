# 🕌 QuranicWords

<p align="center">
  <img src="assets/image/LOGO.png" width="140" alt="QuranicWords logo" />
</p>

<p align="center">
  <strong>A gamified, game-like Android app for learning Quranic vocabulary — Arabic words taught in the order they actually appear in the Qur'an, most frequent first.</strong>
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="UI" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" />
  <img alt="Target SDK" src="https://img.shields.io/badge/targetSdk-36-blue" />
  <img alt="Min SDK" src="https://img.shields.io/badge/minSdk-24-success" />
  <img alt="License" src="https://img.shields.io/badge/license-GPL--3.0-blue" />
  <img alt="Release" src="https://img.shields.io/badge/release-v1.0.0-brightgreen" />
</p>

---

## ✨ What is this?

**QuranicWords** teaches the vocabulary of the Qur'an through short, game-like lessons — points, streaks, and immediate feedback — built specifically around Qur'anic Arabic and Islamic values, for a learner who can already read Arabic script and wants to understand the meaning of what they recite.

> 🕋 **No human faces, anywhere.** Icons, illustrations, and avatars use geometric, calligraphic, and nature motifs only.
> 📖 **No scripture as decoration.** Ayat/Mushaf text is never used as a loading-screen skin or gamification flourish — it only ever appears as real lesson content.
> 🌙 **11-language architecture** (English, বাংলা, اردو, हिन्दी, Bahasa Indonesia, Bahasa Melayu, Türkçe, فارسی, Hausa, Kiswahili, Français), chosen by the learner at setup — not inferred from device locale. Features 100% verified word meanings, unified contextual polysemic senses (*Wujūh al-Qur'an*), collision-free quiz distractors, and exact in-verse span highlights with complete Tashkīl and zero mismatches.
> 🟢 **One green identity**, day and night — Material You dynamic color is deliberately disabled.

---

## 📚 Table of contents

| Doc | What's in it |
|---|---|
| 📄 *(this file)* | Overview, features, tech stack, quick start |
| [🏗️ Architecture](docs/ARCHITECTURE.md) | Layered architecture, package map, DI graph |
| [🧭 User flows](docs/USER_FLOWS.md) | Onboarding and lesson gameplay flows as diagrams |
| [🗄️ Data model](docs/DATA_MODEL.md) | Room schema (ER diagram), bundled JSON content shape |
| [🧮 Algorithms](docs/ALGORITHMS.md) | Streak calculation, gamification scoring, curriculum ordering |
| [🎓 Curriculum design](docs/CURRICULUM_DESIGN.md) | The pedagogical shape of the vocabulary curriculum, and why it's teach-then-quiz |
| [📚 Content sources](docs/CONTENT_SOURCES.md) | Sourced open-licensed Qur'an corpora, with licenses |
| [🔐 Security](SECURITY.md) | Secrets handling and what's not yet hardened |
| [🗄️ Local-only design](docs/BACKUP_AND_SYNC.md) | Why there's no cloud sync, and how backup/restore works instead |
| [🗺️ Roadmap](docs/ROADMAP.md) | Built vs. deferred |
| [🤝 Contributing](CONTRIBUTING.md) | Build/test commands, ingestion pipeline, PR expectations |
| [📜 Code of Conduct](CODE_OF_CONDUCT.md) | Community standards |
| [📱 Play Store Listing](PLAY_STORE_DESCRIPTION.md) | Complete store metadata, character limits, keywords, and localizations |

---

## 🎯 The curriculum

QuranicWords is a structured Quranic vocabulary learning system divided into the three primary Arabic parts of speech (**Aqsam al-Kalimah**) across **10 Chapters**, **100 Sections**, **1,217 Lessons**, and **14,358 Exercises**:

1. **Ḥarf (الحرف — Particles)**: 173 Quranic particles (24,651 occurrences, 41.16% of Quranic text) structured across 10 sections in Chapter 1.
2. **Fi'l (الفعل — Verbs)**: 1,479 Quranic verbs (13,491 occurrences) structured across 30 sections in Chapters 2 to 4.
3. **Ism (الاسم — Nouns)**: 3,057 Quranic nouns (21,746 occurrences) structured across 60 sections in Chapters 5 to 10.

In total, **4,709 vocabulary lemmas** covering **~80%+ of total Quranic word occurrences (59,888 occurrences)** are taught **ordered by their real occurrence frequency in the Qur'an**. Every word features full grammatical categorization, root details, contextual polysemy (**Wujūh al-Qur'an**), and interactive verse examples with complete Tashkīl/Ḥarakāt (sukūn, fatḥah, kasrah, ḍammah, shaddah, tanwīn).

Content is organized as **chapters → sections → lessons**, with section and chapter exams gating progress into subsequent units.

## 🧪 Test-Only & Practice Modes

For learners who want focused recall testing without linear lesson progression, the dedicated **Test-Only Mode** offers 6 specialized practice modes:

1. 📖 **Ism Mode (الاسم — Nouns)**: 3,057 nouns.
2. ⚡ **Fi'l Mode (الفعل — Verbs)**: 1,479 verbs.
3. ✨ **Ḥarf Mode (الحرف — Particles)**: 173 particles.
4. 🔀 **Mix / Random Mode**: Dynamic shuffle across all 4,709 words with live grammar category tags.
5. 🔄 **Mistaken Words Review**: Adaptive spaced review of previously missed words with grammar category tags.
6. 📚 **Chapterwise Practice & Test Mode**: Targeted unit testing across all 10 Quranic chapters.

## 🎮 Gamification

```
✅ Correct answer         → +10 points
🏆 100% lesson accuracy   → +20 bonus points
🔥 Daily streak           → local-calendar-date based, timezone-safe
🔓 Progression unlocking  → each lesson and section unlocks the next on completion
📊 Lesson End Summary     → words covered (Alhamdulillah), mistakes, accuracy %, and next lesson preview
```

## 🧩 How a lesson teaches (not just tests)

Every word is **taught before it's quizzed** — a non-scored intro (the word, its meaning tabs for multiple contextual senses, and real example verses with full Tashkīl) immediately followed by that word's quiz, repeated per word, closed by an interactive lesson summary.

| Type | Interaction | Scored? |
|---|---|---|
| 📖 Word intro | Word + meaning tabs (Wujūh al-Qur'an) + real verse examples with Tashkīl, tap "Continue" | No — teaching only |
| 🔤 Multiple choice | Select the correct translation for the highlighted Arabic word | Yes |
| 🔗 Matching | Pair Arabic words with their corresponding meanings | Yes |
| ✏️ Fill in the blank | Complete a Quranic verse by choosing the missing word | Yes |
| 🧱 Word order builder | Assemble a verse from individual word chips in correct order | Yes |
| 👁️ Word in verse tap | Identify and tap the target word directly inside a Quranic verse | Yes |

All word meanings and polysemic senses are verified against word-by-word reference corpora. See [`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md) for data sourcing details.

## 🖋️ Qur'an script styles

At setup or via Settings, learners preview **Surah Al-Kawthar** in clean, streamlined font cards showing the typeface name and live Arabic sample:

| Style | Status |
|---|---|
| Uthmani (Amiri) | ✅ Bundled (SIL OFL) |
| Scheherazade Naskh | ✅ Bundled (SIL OFL) |
| Simple Naskh (Noto) | ✅ Bundled (SIL OFL) |
| IndoPak Naskh (Lateef) | ✅ Bundled (SIL OFL) |
| Nastaliq (Noto Urdu) | ✅ Bundled (SIL OFL) |

---

## 🛠️ Tech stack

```mermaid
mindmap
  root((QuranicWords))
    UI
      Jetpack Compose
      Material 3
      Navigation-Compose (type-safe)
    Architecture
      MVVM
      Hilt DI
      Feature-based packages
    Local data
      Room
      DataStore Preferences
      kotlinx.serialization
    Local backup
      JSON export/import
      Storage Access Framework
    Language
      Kotlin 2.3
      Coroutines + Flow
```

| Layer | Choice | Why |
|---|---|---|
| UI toolkit | **Jetpack Compose + Material 3** | Animated, game-like lesson screens without XML boilerplate |
| Architecture | **MVVM + Hilt** | Testable, standard Android architecture |
| Local storage | **Room + DataStore** | Offline-first: lessons, progress, and settings all work with zero network |
| Backup | **JSON export/import (SAF)** | Local-device-only: no accounts, no cloud sync — a Settings-screen export/import file carries progress across reinstalls/devices |
| Serialization | **kotlinx.serialization** | Bundled JSON content + type-safe navigation args |
| Build | **AGP 9.3 built-in Kotlin, KSP** | No `kotlin-android` plugin needed; KSP (not kapt) for Room/Hilt codegen |

---

## 🚀 Quick start

```bash
git clone git@github.com:rmrashahriar/QuranicWords.git
cd QuranicWords
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The app **builds and runs fully offline** with zero configuration — language selection, the font picker, and the full vocabulary lesson loop (scoring, streaks, points) all work with no account needed. There is no sign-in, no cloud sync, and no leaderboard; progress lives entirely on-device and travels between devices only via the local backup export/import feature described in [`docs/BACKUP_AND_SYNC.md`](docs/BACKUP_AND_SYNC.md).

## 📁 Project layout

```
app/src/main/java/com/quranicwords/app/
├── core/           # data (Room, DataStore, local backup), domain, DI, navigation, theme, shared UI
└── feature/        # one package per screen: splash, onboarding/*, home, lesson, settings...
app/src/main/assets/content/   # bundled vocabulary lesson JSON (seeds Room on first run)
docs/                          # the documentation set linked above
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full package map and layer diagram.

---

## 🤝 Open Source & Community

**QuranicWords** is an open-source project licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE). Contributions, bug reports, and pull requests are warmly welcomed to help make Quranic Arabic learning accessible to everyone worldwide.

- 🌐 **GitHub Repository**: [github.com/rmrashahriar/QuranicWords](https://github.com/rmrashahriar/QuranicWords)
- 📢 **DEANY TALKS Ecosystem**: Part of the **DEANY TALKS** digital Dawah platforms, creating modern, open Islamic educational tools.
- ✉️ **Contact & Feedback**: Reach out via email at `contact.deanstalks@gmail.com` or open an issue on GitHub.

