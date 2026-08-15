# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

QuranicWords — a gamified, Duolingo-style Android app for learning Quranic vocabulary, forked from `UnderstandingHolyQuran` and narrowed to **Tier 2 (word-meaning vocabulary) only** — the alphabet and grammar tiers were removed entirely, not just hidden. Single `:app` module, Kotlin + Jetpack Compose + Material 3, MVVM + Hilt, Room (offline-first local data). Content is organized as chapter → section → lesson (not the source app's flat module → lesson), with section/chapter exams gating progression. **Local-device-only, permanently**: Google Sign-In, Firebase, and the leaderboard are not disabled-and-commented like in the source app — they're deleted outright, since this fork has no history to preserve. Progress carries across devices/reinstalls via `BackupRepository`'s JSON export/import (Settings screen). See `MEMORY.md` for current project status and the full implementation plan's location. `docs/` still carries most of the source app's documentation structure and is due a Tier-2-only/chapter-section rewrite pass — treat any doc still describing Tier 1/3, Firebase, or a flat module list as stale until that pass lands.

Two hard product constraints baked into the codebase, not just documentation: **no human faces anywhere** in icons/illustrations (geometric/calligraphic/nature motifs only), and **no rendering of actual Ayat/Mushaf text as decoration** — scripture is never used as a loading-screen or gamification skin.

## Commands

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # run all unit tests (JVM, app/src/test)
./gradlew test --tests "com.example.understandingholyquran.core.domain.DistractorGeneratorTest"                     # single test class
./gradlew test --tests "com.example.understandingholyquran.core.domain.DistractorGeneratorTest.picks the closest*"  # single test method
./gradlew lint
./gradlew clean
# firebase deploy --only firestore:rules,firestore:indexes   # DISABLED with Firebase - see docs/FIREBASE_SETUP.md
```

The app builds and runs fully offline with zero configuration (language/tier/font onboarding, the whole lesson loop — scoring, streaks, points — all work without any account). Google Sign-In and the leaderboard are removed (commented out, not deleted — see `## What this is` above); the `google-services` Gradle plugin application is commented out in `app/build.gradle.kts` and `google-services.json` is no longer read at all.

Content-authoring pipeline (`tools/ingestion/*.py`, numbered stages `01_parse_lemmas.py` → `10_add_highlight_spans.py`, plus `root_bn_batch*.py` data files) is offline Python, not part of the Android build — it regenerates the JSON under `assets/content/` from external corpora (Quranic Arabic Corpus, Quran-bil-Quran). Not run as part of any Gradle task; see `docs/CONTENT_SOURCES.md`.

## Architecture

### Layers

Feature packages (`feature/<screen>/`) on top of a shared `core/` layer. No explicit domain "use case" layer — ViewModels call repository interfaces directly. `core/domain/` holds both the repository interfaces/models (`domain/model/`, `domain/repository/`) *and* a few standalone pure-logic classes at its top level (`AdaptiveSequencer`, `DistractorGenerator`) that don't belong to a ViewModel or a repository. `core/data/repository/*Impl` are the only classes that touch `UhqDatabase`/DAOs directly — ViewModels and pure-logic classes always go through a repository interface, never a DAO.

```
core/
├── di/            Hilt modules (Database, Dispatcher, Clock, Repository) → SingletonComponent (FirebaseModule.kt DISABLED, kept commented)
├── data/
│   ├── local/     Room: UhqDatabase, Migrations.kt, entity/, dao/, Converters
│   ├── datastore/ UserPreferencesDataStore — single source for onboarding + settings + reduce-motion + local user id
│   ├── assets/    ContentSeeder + seed DTOs (ContentSeedDtos.kt)
│   ├── remote/firebase/  DISABLED (kept commented, not deleted) — FirebaseAuthDataSource, Firestore{Progress,Leaderboard}DataSource
│   ├── repository/       *RepositoryImpl — the only classes touching UhqDatabase directly (BackupRepositoryImpl added; AuthRepositoryImpl/LeaderboardRepositoryImpl DISABLED)
│   └── CurrentUserIdProvider.kt   resolves the effective progress-tracking user id (now purely local, see UserPreferencesDataStore.getOrCreateLocalUserId)
├── domain/
│   ├── model/           Language, Tier, ItemKind, ExerciseType, ExerciseContent (+ OptionsBearing), BackupPayload, ... (AuthState/LeaderboardEntry DISABLED)
│   ├── repository/       Content/Progress/BackupRepository interfaces (Auth/LeaderboardRepository DISABLED)
│   ├── AdaptiveSequencer.kt   pure reorder function, no DI
│   └── DistractorGenerator.kt + WordCandidatePool   pure selection logic, no DI
├── navigation/    Routes.kt (type-safe, kotlinx.serialization), UhqNavHost.kt (per-route-category transitions)
├── ui/
│   ├── theme/     Color, Theme, Type, Shape, QuranFont
│   ├── motion/    MotionSpecs, rememberReducedMotion(), UhqHaptics — shared animation vocabulary
│   └── components/  reusable Composables (UhqButton, PointsBadge/StreakBadge, Uhq3DFlipCard, CelebrationBurst, RiveStreakFlame, LottieOneShot, GeometricPatternBackground, ...)
└── util/          StreakCalculator, GamificationConfig, AudioPlayer, ArabicText, AppJson

feature/
├── splash/, onboarding/{language,tier,font}/ (onboarding/auth/ DISABLED, no longer wired into nav), home/, settings/ (now has the local backup export/import UI), leaderboard/ (DISABLED, no longer wired into nav)
├── lesson/ (+ exercise/: LetterIntro, WordIntro, MultipleChoice, TapWhatYouHear, Matching, FillInTheBlank, WordOrderBuilder, ListenAndType, AudioPlayButton)
└── lessonsummary/
```

### `ExerciseContent` — the polymorphic exercise model

`core/domain/model/ExerciseContent.kt` is a `kotlinx.serialization` sealed interface, one subtype per exercise shape, stored as a single JSON blob in `ExerciseEntity.contentJson` (shape genuinely varies by type — multiple choice needs options, matching needs pairs, a teach step needs a glyph/meaning/example verse — none of it overlaps cleanly into fixed columns). Two orthogonal groupings to know about:

- **`isScored`** (exhaustive `when`, not `!is`) — `false` only for `LetterIntro`/`WordIntro` (teach steps); everything else is scored. Deliberately exhaustive so a new subtype forces an explicit scoring decision at compile time.
- **`OptionsBearing`** — a sealed sub-interface implemented by `MultipleChoice`, `TapWhatYouHear`, `FillInTheBlank` (the three types whose `options`/`correctOptionId` get runtime-regenerated by `DistractorGenerator`, see below). `Matching`, `WordOrderBuilder`, `ListenAndType`, and the two teach types are not options-bearing.
- **`practicedItemId()`** — the single word/letter id a scored exercise quizzes, used for per-item attempt logging. Returns `correctOptionId` for `MultipleChoice`/`TapWhatYouHear` (these carry no domain tag of their own — see `ItemKind` below), `wordId` for the three newer scored types, and `null` for `Matching` (which quizzes several pairs at once — logged per-pair at the call site, `LessonViewModel.selectMatchingRight`, using `MatchPair.wordId`, **not** `MatchPair.id` — that field is a lesson-scoped presentation id like `"p1"`/`"p2"`, not a global word id, and will collide across lessons if used for attempt logging).

Adding a new `ExerciseContent` subtype means touching, at minimum: `isScored`, `practicedItemId()`, `LessonScreen.kt`'s content-dispatch `when` and check-button-visibility `when`, `LessonScreen.kt`'s `correctAnswerLabel()`, and `LessonViewModel.onCheckPressed()`'s correctness `when` (plus `finalizeCheck`'s `ExerciseType` mapping). A new Composable under `feature/lesson/exercise/` renders it; reuse `OptionCard` (from `MultipleChoiceExercise.kt`) for options-bearing types and `AudioPlayButton` for anything that plays audio.

### Room: migrations are hand-written, not generated

`UhqDatabase` is at version 4 with three explicit `Migration` objects in `core/data/local/Migrations.kt` (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`), each adding exactly one thing (the `exercise_attempts` table, `exercises.practicedItemId`, `modules.contentKind`) with raw SQL that must match what Room would generate for the corresponding `@Entity` — there's no `fallbackToDestructiveMigration()`, so a mismatch throws at runtime on affected installs rather than silently wiping data. When adding an entity/column: bump `UhqDatabase.version`, write the matching `Migration(n, n+1)`, and register it in `core/di/DatabaseModule.kt`'s `.addMigrations(...)`.

`ContentSeeder.CONTENT_VERSION` is a **separate** version counter (currently 7, in `UserPreferencesDataStore`, not Room) gating `ContentSeeder.seedIfNeeded()`'s full wipe-and-reseed of the *content* tables (`modules`, `letters`, `word_frequency`, cascading to `lessons`/`exercises`) from `assets/content/*.json`. It never touches `user_progress`/`user_stats`/`exercise_attempts` — those are real user data. Bump `CONTENT_VERSION` (not just the Room schema version) whenever a Room migration adds a column that needs backfilling from content (e.g. `exercises.practicedItemId`, `modules.contentKind`) — a schema migration alone leaves existing rows at their bare SQL `DEFAULT` until the next reseed.

### Per-item progress tracking (`ExerciseAttemptEntity`) and the Review session

Lesson-level progress (`UserProgressEntity`, pass/fail + best score per lesson) is the older mechanism. `ExerciseAttemptEntity`/`ExerciseAttemptDao` (added alongside the new exercise types) is a per-word/letter attempt log — `getMissedItemIds(userId)` returns ids whose *most recent* attempt was wrong (an item drops out the next time it's answered correctly; not a full spaced-repetition scheduler). This log drives three things: `DistractorGenerator`'s soft tie-break toward past confusions, `AdaptiveSequencer`'s in-lesson reordering, and the dynamic **Review session** (`Route.Review`, a distinct type-safe nav destination from `Route.Lesson` — `LessonViewModel.lessonId` is nullable and `null` exactly when reached via `Route.Review`, no sentinel string involved). `ItemKind` (LETTER vs WORD, since `MultipleChoice`/`TapWhatYouHear` are reused across both alphabet and vocabulary quizzes) comes from `ModuleEntity.contentKind`, seeded from `modules.json` — not inferred from a hardcoded module id.

### Firebase: DISABLED, kept commented not deleted

Google Sign-In / Firebase Auth / Firestore (progress mirror + leaderboard) were removed by product decision in favor of a local-device-only model. Every touched file is commented out rather than deleted (search `DISABLED:`), so the original design intent (`FirebaseModule.kt`'s nullable-at-the-boundary pattern, `firestore.rules`' security model, etc.) is still readable for future reference — but none of it compiles or runs anymore. `google-services` plugin application is commented out in `app/build.gradle.kts`, and the Firebase/Credential Manager/Google ID dependencies are commented out too. `firestore.rules`/`firestore.indexes.json`/`docs/FIREBASE_SETUP.md` are now historical documentation of the disabled cloud path, not deployed or enforced.

Progress instead round-trips through **`BackupRepository`** (`core/domain/repository/BackupRepository.kt` + `core/data/repository/BackupRepositoryImpl.kt`): `exportBackup`/`importBackup` take plain `OutputStream`/`InputStream` (kept `android.net.Uri` out of the interface — the Settings screen opens the stream from a Storage-Access-Framework-picked Uri via `ContentResolver`), serializing `UserStatsEntity`/`UserProgressEntity`/`ExerciseAttemptEntity` (all made `@Serializable` directly, reused rather than parallel DTOs, matching this project's existing pattern of domain repositories returning Room entities directly) plus onboarding preferences into one JSON `BackupPayload` file. Import adopts the backup's `userId` as this device's local user id via `UserPreferencesDataStore.setLocalUserId`.

### Threading & reactivity

Room DAOs expose `Flow` for anything the UI observes live. (The old `AuthRepository.authState` `StateFlow` pattern is DISABLED along with Firebase — see above.) `LessonViewModel.init` fires its independent reads (exercises, missed-item ids, word candidates) concurrently via `async`, awaiting only where one genuinely depends on another (e.g. a Review session's exercise fetch depends on the missed-ids fetch, awaited inside its own `async` block rather than serialized before it starts) — that concurrent-`async` pattern is worth reusing for any new multi-read/multi-write path.

### In-app language switching

`MainActivity` extends `AppCompatActivity` (not plain `ComponentActivity`) specifically because `AppCompatDelegate.setApplicationLocales()`'s pre-API-33 compat path needs an `AppCompatActivity`-registered delegate to mutate `Configuration.locales` — without it the call silently no-ops on API 24–32 and `values-bn/` resources never get selected. Screens must localize both static UI strings (`stringResource`, resource-qualifier driven) **and** JSON-sourced content fields (`titleEn`/`titleBn`, `promptEn`/`promptBn`, etc.) explicitly via `rememberIsBanglaSelected()` — these are two separate mechanisms and both must be wired per screen.

### R8/ProGuard gotcha

`app/proguard-rules.pro` keeps the `ExerciseContent` kotlinx.serialization hierarchy's `$$serializer`/`Companion` classes un-renamed — this is the one genuinely reflection-dependent part of the codebase (polymorphic serialization needs the `@SerialName`-annotated class names intact). Any *new* `ExerciseContent` subtype is automatically covered by the existing wildcard rule, but if a similar polymorphic-serialization pattern is introduced elsewhere, it needs its own keep rule — verify via `./gradlew :app:assembleRelease` and inspecting `app/build/outputs/mapping/release/mapping.txt`.

## Environment note

This repo has no Android SDK configured by default in a fresh environment (only a JDK is not sufficient) — `./gradlew` commands will fail with `SDK location not found` until `ANDROID_HOME`/`local.properties` point at one. Room migration correctness in particular can only be verified against a real build/emulator, not by reading code.
