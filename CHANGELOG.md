# Changelog

All notable changes to QuranicWords are documented here. This project does not yet follow strict semantic versioning (pre-1.0 tags mark development phases); from v1.1.0 onward, tags mark real GitHub releases.

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
