# Changelog

All notable changes to QuranicWords are documented here.

## [1.0.0] - 2026-09-21 (versionCode 27)

### Added
- **Dynamic Exercise Generation in Test Modes**: Test modes (Ism, Fi'l, Ḥarf, Mix/Random, Mistaken Words Review) now dynamically synthesize a rich alternating exercise mix:
  - Reverse Verse Quizzes (`TapWordInVerse`: "Tap the Arabic word in the verse")
  - Verse Completion (`FillInTheBlank`: "Complete the verse")
  - Contextual Multiple Choice (`MultipleChoice`: "Choose the correct meaning" with verse context and translation)
  - Full 11-language localized prompts (`TAP_WORD_PROMPT`, `FILL_BLANK_PROMPT`, `MULTIPLE_CHOICE_PROMPT`).
- **Test-Only & Review Session Word Breakdown in Lesson Summary**: Forwarded `practicedWordIds` via `LessonSummaryRoute` so that test and review mode completions display a comprehensive breakdown of all practiced words with Arabic text, grammatical category (`GrammarCategoryBadge`), and localized meaning.
- **Contextual In-Verse Meaning Highlights & Glass Translations**: Added `HighlightedGlassTranslation` with precise meaning highlight ranges (`HighlightUtils.findMeaningHighlightRange`) across `MatchingExercise`, `MultipleChoiceExercise`, and `TapWordInVerseExercise`. Added Quranic font (`LocalQuranFontFamily`) support for Arabic option choices in Multiple Choice (`OptionCard`).
- **Universal Multi-Language Digit Localization**: Implemented locale-aware number and percentage formatting using `VerseReferenceFormatter.formatDigits(..., language)` across all UI components, badges, charts, and screens (`StatBadges`, `Home30DayActivityTrendChart`, `LearnedWordsScreen`, `ProgressScreen`, `RoadmapScreen`, `TestOnlyHomeScreen`, `WordBrowseScreen`, `WordIntroExercise`).
- **New Localization Strings**: Added `home_chart_days_ago`, `home_chart_day_mins_format`, `progress_day_practiced`, `test_mode_start_chapter_btn`, and updated string placeholders across all locales (`en`, `bn`, `ur`, `fr`, `in`, `tr`).

### Fixed & Improved
- **Corpus & Part-of-Speech Counts Harmonization**: Standardized test mode counts to 4,709 total words: 173 particles (Ḥarf, `w_1..173`), 1,479 verbs (Fi'l, `w_174..1652`), and 3,057 nouns (Ism, `w_1653..4709`).
- **Grammar Category Resolution for Numeric IDs**: Updated `resolveCategoryFromWordId` to support numeric word IDs (`w_1..4709`) and wired into `WordIntroExercise` and `LessonSummaryViewModel`.
- **Mistakes Review Action Routing**: Fixed card click routing on Mistakes Review banner in `TestOnlyHomeScreen` to correctly trigger review action.
- **Scored Exercise Filtering**: `ExerciseDao.getScoredExercisesForItems` now explicitly excludes `WORD_INTRO` and `CHAPTER_INTRO` so only scored quizzes are pulled for review and open practice.
- **Content Seeding Fixes**: Fixed `practicedItemId` assignment in `ContentSeedDtos.kt` to fall back to `(content as? ExerciseContent.WordIntro)?.wordId`.
- **Content Repository Optimization**: Optimized `getWordIntrosForItems` in `ContentRepositoryImpl.kt` to use cached all-word-intros.
- **Days Practiced Pluralization & Localization**: Localized 28-day practice count with language-specific digits and singular/plural string formatting in `ProgressScreen`.

### Changed
- **Release Version**: Bumped `versionCode` to `27` (`versionName = "1.0.0"`).
- **Production Artifacts**: Built signed release app bundle `QuranicWords-v1.0.0.aab`, release APK `QuranicWords-v1.0.0.apk`, and native debug symbols `QuranicWords-v1.0.0-native-debug-symbols.zip`.

## [1.0.0] - 2026-09-19 (versionCode 26)

### Added
- **Surah Names in Verse References**: Enriched Quran verse citations throughout the app with canonical Surah names (e.g. "Al-Baqarah 2:255" instead of raw numbers) across all 114 Surahs via `SurahNames.kt` and `VerseReferenceFormatter.kt`.
- **Roadmap Learning & Review Navigation**: Enabled direct navigation to both completed and unlocked lessons on the Roadmap screen, with clear active indicator (`PlayArrow`) and accent styling for the learner's current lesson.
- **Automated Home Tree Navigation**: Added auto-scrolling to the active lesson upon completion/advancement (`findHomeTargetItemIndex`) and clean auto-collapsing of completed units to keep the curriculum view organized and uncluttered.

### Fixed
- **Curriculum Auto-Unlock Progression**: Resolved edge cases in `ProgressRepositoryImpl` where completing a chapter intro or transition node now immediately unlocks the subsequent section lessons.
- **Widget Formatting Across Locales**: Enforced `Locale.US` in float percentage formatting for home screen widgets (`StatsWidgetProvider`), resolving potential layout rendering errors in international locales.
- **Multi-language String Resources**: Consolidated duplicate Indonesian localization resources under Android standard `values-in` and harmonized string definitions across English, Bengali, French, Indonesian, Turkish, and Urdu.

### Changed
- **Compiler & Lint Quality Gates**: Configured `warningsAsErrors = true` and `allWarningsAsErrors = true` across Kotlin compiler and Android Lint to guarantee strict code hygiene.
- **Test Runner Compatibility**: Optimized unit test JVM arguments in `build.gradle.kts` and `gradle.properties` for smooth execution on modern JDK runtimes.
- **Release Version**: Bumped `versionCode` to `26` (`versionName = "1.0.0"`).
- **Production Artifacts**: Built signed release app bundle `QuranicWords-v1.0.0.aab`, release APK `QuranicWords-v1.0.0.apk`, and native debug symbols `QuranicWords-v1.0.0-native-debug-symbols.zip`.

## [1.0.1] - 2026-09-19 (versionCode 22)

### Added
- Added "Next Sense" navigation for words with multiple senses (polysemy), ensuring users review all meanings before proceeding.
- Added a "Previous" button during exercises to allow revisiting and reviewing completed words.

### Fixed
- Fixed an intermittent crash (`IndexOutOfBoundsException`) during the splash screen opening invocation sequence when tapped multiple times rapidly.

### Changed
- Bumped `versionCode` to `22`.

## [1.0.1] - 2026-09-18 (versionCode 21)

### Fixed
- **Matching Exercise Shuffling & Derangement**: Implemented a derangement algorithm (`derangeRightEntries`) ensuring that right tiles are strictly non-aligned with left tiles on the initial row layout, and randomized input pair ordering on distractor regeneration.
- **Home & Roadmap Lesson Status Icons**: Aligned lesson node icons across `HomeScreen.kt`, `RoadmapScreen.kt`, and `LessonStatusStyle.kt` so that completed lessons unconditionally display `Icons.Filled.CheckCircle` (tick button), only the active current lesson displays `Icons.Filled.PlayArrow`, and unlocked non-current lessons display their specific category/kind icon (never `PlayArrow`).
- **Stability & Crash Fixes on Consecutive Lessons**:
  - Added `@Volatile private var isFinishing` re-entrancy protection and `try-catch` exception handling in `LessonViewModel.finishLesson()`.
  - Moved `LessonSummaryViewModel.loadSummaryDetails()` database and JSON computations to `Dispatchers.IO` with `try-catch` safety.
  - Added click debouncing (`continueClicked` state) on `QwPrimaryButton` and `QwSecondaryButton` in `LessonSummaryScreen.kt` to prevent concurrent `NavController.navigate()` invocations from crashing Navigation Compose.
  - Removed per-correct-answer `CelebrationBurst` from `AnswerFeedbackOverlay.kt` to prevent animator accumulation and memory exhaustion during extended lesson runs.

### Changed
- **Release Version Bump**: Bumped `versionCode` to `21` (`versionName = "1.0.1"`).
- **Production Artifacts**: Built signed release app bundle `QuranicWords-v1.0.1.aab`, release APK `QuranicWords-v1.0.1.apk`, and native debug symbols `QuranicWords-v1.0.1-native-debug-symbols.zip`.

## [1.0.1] - 2026-09-17 (versionCode 20)

### Fixed
- **End-of-Lesson Matching Quizzes**: Synthesized and restored interactive 4–5 pair `MATCHING` exercises at the end of all 997 regular lessons (plus all 100 flashbacks, 100 section exams, and 10 chapter exams), totaling 1,207 matching exercises.
- **Curriculum Progression & Lesson Gating**: Populated all 210 previously empty `SECTION_FLASHBACK`, `SECTION_EXAM`, and `CHAPTER_EXAM` lessons with complete question suites (14,358 exercises total across 1,217 lessons, with 0 empty lessons).
- **Auto-Healing & Home Unlocking**: Implemented automated progress repair in `ProgressRepositoryImpl.ensureCurriculumStarted` to immediately unlock next lessons for any completed lessons and ensure `les_0002` unlocks for the first section; updated `HomeScreen.kt` to auto-expand newly active sections upon progression.
- **Polysemy Sense Clean-up**: Audited all polysemy words. Collapsed duplicate/redundant senses into clean single canonical definitions for pseudo-polysemous words, eliminating redundant tabs and slash-repetitions (`not / not`), while rigorously preserving distinct classical senses for the 19 authentic *Wujūh al-Qur'an* lemmas.
- **Empty Lesson Fallback UI**: Added graceful fallback UI card in `LessonScreen.kt` with a return button in case any empty lesson state is ever encountered.

### Changed
- **Database Content Reseeding**: Bumped `ContentSeeder.CONTENT_VERSION` from 33 to 34 to cleanly reseed Room content tables on app update without disturbing user progress or statistics.
- **Production Artifacts & Dictionary**: Regenerated `QuranicWords_Dictionary.html` (57.3 MB) reflecting 100% verified senses; built signed release app bundle `QuranicWords-v1.0.1.aab` (`versionCode 20`, `versionName 1.0.1`), release APK `QuranicWords-v1.0.1.apk`, and unstripped native debug symbols `QuranicWords-v1.0.1-native-debug-symbols.zip`.


## [1.0.0] - 2026-09-16 (Google Play Store Official Release)

### Changed
- **Target SDK 36 Upgrade**: Updated targetSdk and compileSdk to 36 (Android 16 compatibility) fulfilling Google Play Console publishing guidelines.
- **Native Debug Symbols Generation**: Automated `extractReleaseNativeDebugMetadata` and `mergeReleaseNativeDebugMetadata` tasks extracting unstripped native symbols into `QuranicWords-v1.0.0-native-debug-symbols.zip`.
- **Language Split Bundle Optimization**: Disabled Dynamic Feature language splits (`bundle.language.enableSplit = false`) to guarantee instant in-app language switching offline across all 11 supported languages without requiring Play Store feature downloads.
- **Version Number Alignment**: Realigned public production versioning to `v1.0.0` (internal `versionCode 18`) for Google Play Store launch.

## [3.1.0] - 2026-09-16

### Added
- **Complete Arabic Vocalization (Tashkīl) Overhaul**: Restored 100% of missing Jajam (sukūn), Jabar (fatḥah), Jer (kasrah), Pesh (ḍammah), and Tasdid (shaddah) on standalone particles, prefixes, and suffixes in TargetArabicWord and in-verse highlights across all 4,709 lemmas.
- **Canonical Dataset Synchronization**: Synchronized `harf_canonical_192.json`, `harf_rows.json`, `ism_canonical_3091.json`, and `fil_canonical_1505.json`.
- **Clean Database Reseeding Pipeline**: Added `deleteAll()` methods across `SectionDao`, `LessonDao`, and `ExerciseDao` in Room; updated `ContentSeeder.kt` to clean and reseed all 5 content tables, bumping `CONTENT_VERSION` to 32.
- **Interactive HTML Dictionary Typography**: Refactored `mark.ar-hl` inline styling in `QuranicWords_Dictionary.html`, expanded Arabic font stack, increased verse line-height to 2.2, and enlarged target word badges.

### Fixed
- Fixed target word mismatches: Rank 93 مِيكَال -> وَمِيكَىٰلَ, Rank 122 صَالِح -> ٱلصَّـٰلِحَـٰتِ, and 28 خ-ض-ع nominal entries -> خَـٰضِعِينَ.

## [3.0.0] - 2026-09-05

### Added
- **10-Language Master Curriculum (4,709 Quranic Lemmas)**: Expanded vocabulary to 4,709 lemmas across 10 Chapters, 100 Sections, 1,217 Lessons, and 9,428 Exercises, covering 59,888 total Quranic occurrences (~80%+ of the Qur'an).
- **11 Aligned Global Languages**: Complete translation and in-verse span alignment for English, Bengali, Urdu, Hindi, Indonesian, Malay, Turkish, Persian, Hausa, Swahili, and French.
- **Exact In-Verse Highlights (0 Mismatches)**: Deep semantic audit ensuring zero truncated highlights, zero stop-word false positives, and 100% token-grounded sequential spans in example verses.
- **Chapterwise Practice & Test Mode**: Added dedicated 6th test mode in Test-Only Hub allowing learners to test recall chapter-by-chapter across all 10 Quranic chapters.
- **Interactive Offline HTML Dictionary**: Shipped `QuranicWords_Dictionary.html` with instant search, multi-language toggles, and audio-visual root mapping.

## [2.2.0] - 2026-09-01

### Added
- **Parts of Speech Curriculum (4,616 words)**: Full architectural division into the 3 Quranic parts of speech (**Fi'l / Verbs**: 1,450 words, 145 lessons, 15 sections; **Ḥarf / Particles**: 109 words, 11 lessons, 2 sections; **Ism / Nouns**: 3,057 words, 306 lessons, 31 sections).
- **Contextual Polysemy (Wujūh al-Qur'an)**: Multi-meaning tabs and dedicated verse examples per word with 100% verified glosses across 12 languages.
- **5-Mode Test Hub**: Dedicated testing modes for Ism, Fi'l, Ḥarf, Mix/Random (with live grammar tags), and adaptive Mistaken Words Review.
- **Grammar Category Badges**: Color-coded badges (`GrammarCategoryBadge`) in Forest Green (Ism `#2E7D32`), Warm Gold (Fi'l `#D4AF37`), and Sky Blue (Ḥarf `#0288D1`) across exercise headers, quiz options, test modes, and learned words dictionary.
- **Post-Lesson Performance Summary**: Rich end-of-lesson stats displaying total words covered (*Alhamdulillah*), mistake count, accuracy percentage, time spent, and next lesson preview.
- **Open Source & DEANY TALKS info**: GPL-3.0 licensing notices, GitHub repository links, DEANY TALKS Dawah ecosystem platform links, and contact email.

### Changed
- **Typography & Verse Highlighting**: Preserved 100% of Tashkīl, Ḥarakāt, Sukūn, Tashdīd, and Tanwīn. Replaced 3D glass box borders with clean inline text spans to eliminate line breaking and ensure verse continuity.
- **Streamlined Font Selection**: Simplified font selection UI to show only font name and live Surah Al-Kawthar Arabic preview.
- **Audio Architecture**: Removed word pronunciation audio playback and audio-dependent exercises (`TapWhatYouHear`, `ListenAndType`) to focus on reading comprehension and Qur'anic context, while retaining low-latency UI sound effects (`SfxPlayer.kt`) for correct/incorrect/lesson complete feedback.

## [1.2.0] - 2026-08-22

### Added
- **Achievements system**: 17 unlockable badges (streak milestones, chapter completions, vocabulary-coverage bands, first-lesson/first-exam), custom-drawn medallion badges, a dedicated Achievements screen, and a flip-card reveal on the lesson summary screen when a new one unlocks. Round-trips through backup export/import (`BACKUP_SCHEMA_VERSION` 1→2).
- **Illustrated visual redesign**: four custom-drawn (zero external asset) Islamic motifs — crescent moon, starfield, mosque silhouette, abstract book — applied across Splash, Lesson Summary, Settings, and Home, plus a chrome refresh (elevation/press-depth on the primary Home CTA, pill-shaped lesson progress bar, serif section titles).
- **Branching curriculum tree on Home**: chapters and sections now collapse/expand instead of showing a flat lesson list, auto-expanding to the learner's current position, with a fan-out connector between an expanded chapter and its sections.
- **Internal content-authoring CLI** (`tools/ingestion/16_cms.py`): interactive tool to look up, edit, and validate word content directly, with a built-in reminder to bump `ContentSeeder.CONTENT_VERSION` on any content-shape change.

### Changed
- Lesson summary screen now correctly distinguishes lesson kinds: only section/chapter exams and flashbacks gate on the 80% passing score (`LessonKind.requiresPassingScore()`); a regular lesson or review session always shows completion, not a false "try again."
- Re-verified all 3,680 words' example verses against the actual word-boundary-matched Arabic text (99.7% now machine-verified, `exampleVerseVerified` flag added), and improved English highlight-span coverage with phrase-matching and light stemming.

### Fixed
- **Accessibility**: TalkBack couldn't skip the every-launch opening invocation at all (raw gesture detector produced no accessibility node); language/font onboarding cards and multiple-choice/tap-what-you-hear/fill-in-the-blank exercise options were announced as unlabeled "Button" (missing `mergeDescendants`); correct/incorrect exercise feedback was conveyed only by color, invisible to a screen reader. All fixed and verified via an emulator-based TalkBack audit pass.

## [1.1.0] - 2026-08-22

### Added
- **12-language architecture**: UI strings and word-by-word meanings translated into 10 new languages (Albanian, Chinese, Farsi, French, German, Hindi, Indonesian, Russian, Turkish, Urdu), alongside the existing English/Bangla. Word-by-word source data credited to [quran.gtaf.org](https://quran.gtaf.org/).
- **Opening invocation animation**: every launch now opens with the Ta'awwudh, Basmala, and "Rabbi zidni ilma" (Qur'an 20:114), tap-to-skip, respecting reduced-motion settings.
- **Bundled word-pronunciation audio**: per-word Arabic pronunciation clips, synthesized via Google Cloud Text-to-Speech (male WaveNet voice), bundled directly in the APK — no network request, no separate download.
- **Opt-in local streak-reminder notifications** (WorkManager, no network).
- CI/CD pipeline (`.github/workflows/android-ci.yml`): unit tests, lint, and debug/release builds on every push/PR.

### Changed
- Content model refactored from flat `*En`/`*Bn` fields to a general `LocalizedText` map across all exercise types and entities, enabling the 12-language expansion.
- Project relicensed to **GPL-3.0** and prepared for public release: standard OSS files added (CONTRIBUTING, CODE_OF_CONDUCT, issue/PR templates), `.idea/` untracked, `SECURITY.md` promoted to the repo root.

### Fixed
- In-app language switching silently failed to apply on modern Android versions (API 33+) due to a stale SDK-version guard around activity recreation.
- Multiple-choice quiz options and matching-exercise pairs weren't inheriting translated text — they were separate embedded copies of a word's meaning, not references.
- Room database crash (`IllegalStateException`) on any device with a pre-existing install after the content-model refactor changed entity columns without a schema version bump.

### Removed
- The old remote audio-download pipeline (`WordAudioRepository`, `AudioBulkDownloadWorker`, `AudioConfig`) — superseded by bundled TTS audio, which needs no runtime download or hosting.

## [1.0.0] and earlier

See git history (tags `v0.1.0-phase0` through `v1.0.0`) for the initial build-out: core lesson loop, gamification (points/streaks), Room offline cache, English/Bangla localization, the full 3,680-word vocabulary curriculum, R8/ProGuard release hardening, and CI/CD.
