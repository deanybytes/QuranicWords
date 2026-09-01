# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

QuranicWords — a gamified Android app for learning Quranic vocabulary: Arabic words structured into the 3 primary Arabic parts of speech (**Fi'l / Verbs**: 1,450 words; **Ḥarf / Particles**: 109 words; **Ism / Nouns**: 3,057 words = 4,616 total words), taught in descending occurrence frequency. Single `:app` module, Kotlin + Jetpack Compose + Material 3, MVVM + Hilt, Room (offline-first local data). Content is organized as **parts of speech → sections → lessons**, with section exams gating progression. **Local-device-only**: no sign-in, no cloud backend, and zero network requests. Progress carries across devices/reinstalls via `BackupRepository`'s JSON export/import (Settings screen).

Two hard product constraints baked into the codebase: **no human faces anywhere** in icons/illustrations (geometric/calligraphic/nature motifs only), and **no rendering of actual Ayat/Mushaf text as decoration** — scripture is never used as a loading-screen or gamification skin.

## Commands

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # run all unit tests (JVM, app/src/test)
./gradlew test --tests "com.quranicwords.app.core.domain.DistractorGeneratorTest"                     # single test class
./gradlew lint
./gradlew clean
```

The app builds and runs fully offline with zero configuration.

Content-authoring pipeline (`tools/ingestion/*.py`) is offline Python, not part of the Android build — it regenerates the JSON under `app/src/main/assets/content/` (`curriculum_manifest.json`, `section_*.json`, `word_frequency.json`).

## Architecture

### Layers

Feature packages (`feature/<screen>/`) on top of a shared `core/` layer. ViewModels call repository interfaces directly. `core/data/repository/*Impl` are the only classes that touch `QwDatabase`/DAOs directly.

```
core/
├── di/            Hilt modules (Database, Dispatcher, Clock, Repository) → SingletonComponent
├── data/
│   ├── local/     Room: QwDatabase, entity/, dao/, Converters
│   ├── datastore/ UserPreferencesDataStore — single source for onboarding + settings + reduce-motion + local user id
│   ├── assets/    ContentSeeder + seed DTOs (ContentSeedDtos.kt)
│   ├── repository/       *RepositoryImpl — the only classes touching QwDatabase directly (Content/Progress/BackupRepositoryImpl)
│   └── CurrentUserIdProvider.kt   resolves the effective progress-tracking user id
├── domain/
│   ├── model/           Language, ItemKind, ExerciseType, ExerciseContent (+ OptionsBearing), BackupPayload, QuranFontStyle, ThemeMode, LessonResult, ReviewSession
│   ├── repository/       Content/Progress/BackupRepository interfaces
│   ├── AdaptiveSequencer.kt   pure reorder function, no DI
│   └── DistractorGenerator.kt + WordCandidatePool   pure selection logic, no DI
├── navigation/    Routes.kt (type-safe, kotlinx.serialization), QwNavHost.kt (per-route-category transitions)
├── ui/
│   ├── theme/     Color, Theme, Type, Shape, QuranFont
│   ├── motion/    Motion, ReducedMotion, Haptics — shared animation vocabulary
│   └── components/  reusable Composables (GrammarCategoryBadge, QwButton, StatBadges, FeedbackBanner, StreakFlame, ...)
└── util/          StreakCalculator, GamificationConfig, SfxPlayer, QuranPreviewText, AppJson

feature/
├── splash/, onboarding/{language,path,font,style,goal}/
├── home/ (Curriculum map with Ism, Fi'l, Ḥarf chapters)
├── testonlyhome/ (5-mode test hub: Ism, Fi'l, Ḥarf, Mix, Mistaken)
├── lesson/ (+ exercise/: WordIntro, MultipleChoice, Matching, FillInTheBlank, WordOrderBuilder, TapWordInVerse)
├── lessonsummary/ (End-of-lesson performance stats & next lesson preview)
├── learnedwords/ (Dictionary with Wujūh al-Qur'an polysemy modals)
├── wordbrowse/ (3D flip-card story-fold browser)
├── intro/ (Statistical chapter/section intros)
├── achievements/ (Medallions showcase)
└── settings/ (Theme, language, font, daily goal, sound effects, local backup)
```

Onboarding is Splash → Language → Path (Learn & Test vs. Test-Only) → Font → Learning Style (Learn only) → Daily Goal → Home (or TestOnlyHome).

### `ExerciseContent` — the polymorphic exercise model

`core/domain/model/ExerciseContent.kt` is a `kotlinx.serialization` sealed interface with subtypes: `WordIntro`, `MultipleChoice`, `Matching`, `FillInTheBlank`, `WordOrderBuilder`, `TapWordInVerse`.

- **`isScored`**: `false` only for `WordIntro` (teach step) and `ChapterIntro`.
- **`OptionsBearing`**: `MultipleChoice`, `FillInTheBlank` (regenerated at runtime by `DistractorGenerator`).
- **`practicedItemId()`**: The word ID quizzed by the scored exercise.
- **Meaning resolved at read time**: `LessonViewModel.resolveCanonicalMeaning` overrides baked JSON fields with fresh lookups from `WordFrequencyEntity` across all 12 languages.


Adding a new `ExerciseContent` subtype means touching, at minimum: `isScored`, `practicedItemId()`, `LessonScreen.kt`'s content-dispatch `when` and check-button-visibility `when`, `LessonScreen.kt`'s `correctAnswerLabel()`, and `LessonViewModel.onCheckPressed()`'s correctness `when` (plus `finalizeCheck`'s `ExerciseType` mapping). A new Composable under `feature/lesson/exercise/` renders it; reuse `OptionCard` (from `MultipleChoiceExercise.kt`) for options-bearing types and `AudioPlayButton` for anything that plays audio.

### Room: currently a pre-launch destructive schema, not hand-written migrations yet

`QwDatabase` has no `Migration` objects yet (check `QwDatabase.version` for the current number, not this doc — it bumps freely pre-launch) — `core/di/DatabaseModule.kt` builds it with `.fallbackToDestructiveMigration(dropAllTables = true)`, a deliberate pre-launch choice (no installed base to preserve, so there's nothing to migrate). Once this schema needs to survive a real release, that call should be replaced with real hand-written `Migration(n, n+1)` objects whose raw SQL matches what Room would generate for each `@Entity` change — bump `QwDatabase.version`, write the migration, and register it via `.addMigrations(...)` in `DatabaseModule`. Until then, any schema change is a free version bump; don't add migration scaffolding preemptively for changes made before the first real release.

`ContentSeeder.CONTENT_VERSION` is a **separate** version counter (check `ContentSeeder.kt` for the current number, not this doc; tracked in `UserPreferencesDataStore`, not Room) gating `ContentSeeder.seedIfNeeded()`'s full wipe-and-reseed of the *content* tables from `app/src/main/assets/content/*.json` — check `ContentSeeder.kt` for the exact table list, since it's changing alongside the chapter/section restructuring. It never touches `user_progress`/`user_stats`/`exercise_attempts` — those are real user data. Bump `CONTENT_VERSION` whenever bundled content JSON changes shape *or values* in a way that needs a full reseed (a value-only change, like corrected example verses, still needs a bump — the seeder gates on the version flag alone, not a per-row diff, so a forgotten bump means already-seeded devices never see the correction).

### Per-item progress tracking (`ExerciseAttemptEntity`) and the Review session

Lesson-level progress (`UserProgressEntity`, pass/fail + best score per lesson) is the older mechanism. `ExerciseAttemptEntity`/`ExerciseAttemptDao` is a per-word attempt log — `getMissedItemIds(userId)` returns ids whose *most recent* attempt was wrong (an item drops out the next time it's answered correctly; not a full spaced-repetition scheduler). This log drives three things: `DistractorGenerator`'s soft tie-break toward past confusions, `AdaptiveSequencer`'s in-lesson reordering, and the dynamic **Review session** (`Route.Review`, a distinct type-safe nav destination from `Route.Lesson` — `LessonViewModel.lessonId` is nullable and `null` exactly when reached via `Route.Review`, no sentinel string involved). `ItemKind` is a single-value enum (`WORD`) today — kept as an enum rather than removed outright since `ExerciseAttemptEntity` persists it and `BackupRepository` round-trips it through `@Serializable`.

### There is no cloud backend

QuranicWords ships with no sign-in, no remote database, and no leaderboard — none of that code exists in `core/data/remote/` (the directory is empty) or anywhere else in this repository. Progress instead round-trips entirely through **`BackupRepository`** (`core/domain/repository/BackupRepository.kt` + `core/data/repository/BackupRepositoryImpl.kt`): `exportBackup`/`importBackup` take plain `OutputStream`/`InputStream` (kept `android.net.Uri` out of the interface — the Settings screen opens the stream from a Storage-Access-Framework-picked Uri via `ContentResolver`), serializing `UserStatsEntity`/`UserProgressEntity`/`ExerciseAttemptEntity` (all made `@Serializable` directly, reused rather than parallel DTOs) plus onboarding preferences into one JSON `BackupPayload` file. Import adopts the backup's `userId` as this device's local user id via `UserPreferencesDataStore.setLocalUserId`.

### Threading & reactivity

Room DAOs expose `Flow` for anything the UI observes live. `LessonViewModel.init` fires its independent reads (exercises, missed-item ids, word candidates) concurrently via `async`, awaiting only where one genuinely depends on another (e.g. a Review session's exercise fetch depends on the missed-ids fetch, awaited inside its own `async` block rather than serialized before it starts) — that concurrent-`async` pattern is worth reusing for any new multi-read/multi-write path.

### In-app language switching

`MainActivity` extends `AppCompatActivity` (not plain `ComponentActivity`) specifically because `AppCompatDelegate.setApplicationLocales()`'s pre-API-33 compat path needs an `AppCompatActivity`-registered delegate to mutate `Configuration.locales` — without it the call silently no-ops on API 24–32 and `values-bn/` resources never get selected. Screens must localize both static UI strings (`stringResource`, resource-qualifier driven) **and** JSON-sourced content fields (`LocalizedText` maps — `title`, `meaning`, `prompt`, etc. on entities/`ExerciseContent`, keyed by `Language.tag`) explicitly via `rememberSelectedLanguage()` — these are two separate mechanisms and both must be wired per screen. `Language` supports 12 tags (`en, bn, sq, zh, fa, fr, de, hi, in, ru, tr, ur`); only `en`/`bn` have translated content/UI strings so far, the rest fall back to English via `LocalizedText.get`'s fallback.

### R8/ProGuard gotcha

`app/proguard-rules.pro` keeps the `ExerciseContent` kotlinx.serialization hierarchy's `$$serializer`/`Companion` classes un-renamed — this is the one genuinely reflection-dependent part of the codebase (polymorphic serialization needs the `@SerialName`-annotated class names intact). Any *new* `ExerciseContent` subtype is automatically covered by the existing wildcard rule, but if a similar polymorphic-serialization pattern is introduced elsewhere, it needs its own keep rule — verify via `./gradlew :app:assembleRelease` and inspecting `app/build/outputs/mapping/release/mapping.txt`.

## Environment note

This repo has no Android SDK configured by default in a fresh environment (only a JDK is not sufficient) — `./gradlew` commands will fail with `SDK location not found` until `ANDROID_HOME`/`local.properties` point at one. Room schema/migration correctness in particular can only be verified against a real build/emulator, not by reading code.
