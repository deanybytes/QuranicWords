# Changelog

All notable changes to QuranicWords are documented here.

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
