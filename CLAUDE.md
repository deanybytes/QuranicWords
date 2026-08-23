# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

QuranicWords — a gamified, game-like Android app for learning Quranic vocabulary: Arabic words taught in the order they actually appear in the Qur'an, most frequent first. Single `:app` module, Kotlin + Jetpack Compose + Material 3, MVVM + Hilt, Room (offline-first local data). The curriculum is vocabulary-only — no alphabet stage, no grammar track. Content is organized as **chapter → section → lesson**, with section/chapter exams gating progression to the next unit; this hierarchy is being actively built out in code (entities, DAOs, seeding, and the unlock-gating rules can all be mid-change at any given moment), so treat `core/data/local/entity/` and `core/data/repository/ProgressRepositoryImpl` as the source of truth for the current shape rather than any specific schema described in `docs/` — see `MEMORY.md` for phase status. **Local-device-only**: there is no sign-in, no cloud backend, and no leaderboard in this codebase — that code was never carried over, not disabled or commented out. Progress carries across devices/reinstalls via `BackupRepository`'s JSON export/import (Settings screen), not cloud sync. See `README.md` for the product pitch and `docs/` for detailed docs (architecture, data model, algorithms, curriculum design, content sourcing, security, roadmap) — read the relevant one before making a non-trivial change in that area, and update it if the change makes it stale. `docs/` files describe the general shape and intent of the chapter/section restructuring, not an exact frozen schema, since that work is still landing.

Two hard product constraints baked into the codebase, not just documentation: **no human faces anywhere** in icons/illustrations (geometric/calligraphic/nature motifs only), and **no rendering of actual Ayat/Mushaf text as decoration** — scripture is never used as a loading-screen or gamification skin.

## Commands

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # run all unit tests (JVM, app/src/test)
./gradlew test --tests "com.quranicwords.app.core.domain.DistractorGeneratorTest"                     # single test class
./gradlew test --tests "com.quranicwords.app.core.domain.DistractorGeneratorTest.picks the closest*"  # single test method
./gradlew lint
./gradlew clean
```

The app builds and runs fully offline with zero configuration (language + font onboarding, the whole lesson loop — scoring, streaks, points — all work without any account). There is no sign-in and no leaderboard to disable — neither exists in this codebase.

Content-authoring pipeline (`tools/ingestion/*.py`, numbered stages `01_parse_lemmas.py` → `10_add_highlight_spans.py`, plus `root_bn_batch*.py` data files) is offline Python, not part of the Android build — it regenerates the JSON under `app/src/main/assets/content/` from external corpora (Quranic Arabic Corpus, Quran-bil-Quran). Not run as part of any Gradle task; see `docs/CONTENT_SOURCES.md`.

## Architecture

### Layers

Feature packages (`feature/<screen>/`) on top of a shared `core/` layer. No explicit domain "use case" layer — ViewModels call repository interfaces directly. `core/domain/` holds both the repository interfaces/models (`domain/model/`, `domain/repository/`) *and* a few standalone pure-logic classes at its top level (`AdaptiveSequencer`, `DistractorGenerator`) that don't belong to a ViewModel or a repository. `core/data/repository/*Impl` are the only classes that touch `QwDatabase`/DAOs directly — ViewModels and pure-logic classes always go through a repository interface, never a DAO.

```
core/
├── di/            Hilt modules (Database, Dispatcher, Clock, Repository) → SingletonComponent
├── data/
│   ├── local/     Room: QwDatabase, entity/, dao/, Converters
│   ├── datastore/ UserPreferencesDataStore — single source for onboarding + settings + reduce-motion + local user id
│   ├── assets/    ContentSeeder + seed DTOs (ContentSeedDtos.kt)
│   ├── repository/       *RepositoryImpl — the only classes touching QwDatabase directly (Content/Progress/BackupRepositoryImpl)
│   └── CurrentUserIdProvider.kt   resolves the effective progress-tracking user id (purely local — see UserPreferencesDataStore.getOrCreateLocalUserId)
├── domain/
│   ├── model/           Language, ItemKind, ExerciseType, ExerciseContent (+ OptionsBearing), BackupPayload, QuranFontStyle, ThemeMode, LessonResult, ReviewSession
│   ├── repository/       Content/Progress/BackupRepository interfaces
│   ├── AdaptiveSequencer.kt   pure reorder function, no DI
│   └── DistractorGenerator.kt + WordCandidatePool   pure selection logic, no DI
├── navigation/    Routes.kt (type-safe, kotlinx.serialization), QwNavHost.kt (per-route-category transitions)
├── ui/
│   ├── theme/     Color, Theme, Type, Shape, QuranFont
│   ├── motion/    Motion, ReducedMotion, Haptics — shared animation vocabulary
│   └── components/  reusable Composables (QwButton, StatBadges, Qw3DFlipCard, CelebrationBurst, StreakFlame, GeometricPatternBackground, QwLogo, ...)
└── util/          StreakCalculator, GamificationConfig, AudioPlayer, QuranPreviewText, AppJson

feature/
├── splash/, onboarding/{language,font}/, home/, settings/ (has the local backup export/import UI)
├── lesson/ (+ exercise/: WordIntro, MultipleChoice, TapWhatYouHear, Matching, FillInTheBlank, WordOrderBuilder, ListenAndType, AudioPlayButton)
└── lessonsummary/
```

There is no `leaderboard/` package. Onboarding today is Splash → Language → Path (Learn vs. Test/Quiz-only) → Font → Learning Style (Learn only) → Daily Goal → Home (see `core/navigation/Routes.kt`); `SplashViewModel` resumes mid-chain for a returning user by checking each step's own `*_choice_made` flag in that order, skipping the Learning Style check entirely for a Test/Quiz-only user since that route is unreachable for them.

### `ExerciseContent` — the polymorphic exercise model

`core/domain/model/ExerciseContent.kt` is a `kotlinx.serialization` sealed interface, one subtype per exercise shape, stored as a single JSON blob in `ExerciseEntity.contentJson` (shape genuinely varies by type — multiple choice needs options, matching needs pairs, the teach step needs a meaning/root/example verse — none of it overlaps cleanly into fixed columns). Two orthogonal groupings to know about:

- **`isScored`** (exhaustive `when`, not `!is`) — `false` only for `WordIntro` (the teach step); everything else (`MultipleChoice`, `TapWhatYouHear`, `Matching`, `FillInTheBlank`, `WordOrderBuilder`, `ListenAndType`) is scored. Deliberately exhaustive so a new subtype forces an explicit scoring decision at compile time.
- **`OptionsBearing`** — a sealed sub-interface implemented by `MultipleChoice`, `TapWhatYouHear`, `FillInTheBlank` (the three types whose `options`/`correctOptionId` get runtime-regenerated by `DistractorGenerator`, see below); all three also carry `OptionsBearing.wordId`, the word each exercise quizzes — `MultipleChoice`/`TapWhatYouHear`'s literal `promptArabic` text and per-exercise-local `correctOptionId` (e.g. `"o3"`) are not usable as a lookup key on their own. `Matching`, `WordOrderBuilder`, `ListenAndType`, and `WordIntro` are not options-bearing.
- **`practicedItemId()`** — the single word id a scored exercise quizzes, used for per-item attempt logging. Returns `OptionsBearing.wordId` for `MultipleChoice`/`TapWhatYouHear`/`FillInTheBlank` (not `correctOptionId`, which is only a per-exercise-local option id and would fragment logging for the same word across differently-numbered exercises), `wordId` for `WordOrderBuilder`/`ListenAndType`, and `null` for `Matching` (which quizzes several pairs at once — logged per-pair at the call site, `LessonViewModel.selectMatchingRight`, using `MatchPair.wordId`, **not** `MatchPair.id` — that field is a lesson-scoped presentation id like `"p1"`/`"p2"`, not a global word id, and will collide across lessons if used for attempt logging).
- **Meaning is resolved at read time, never trusted from the baked JSON as-is** — `LessonViewModel.resolveCanonicalMeaning`/`rebuildOptions` overwrite `WordIntro.meaning`/`TapWordInVerse.meaning`/`MatchPair.right`/the correct `ChoiceOption.label` with a fresh lookup against `WordFrequencyEntity` (via `WordCandidatePool`, keyed by each type's `wordId`) every time a lesson loads, falling back to the baked text only when the word isn't in the pool. This exists because the content pipeline independently copy-embeds a word's meaning into up to 5 places at authoring time (see `docs/CONTENT_SOURCES.md`'s "Fixing meaning-copy drift" section) and those copies can drift out of sync with each other; the app resolving from one canonical source at read time is what actually prevents the same word from showing different translations on different screens, regardless of what the shipped JSON currently says.

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
