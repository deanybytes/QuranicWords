# 🗺️ Roadmap

QuranicWords is a focused, single-curriculum app: **Quranic vocabulary, ordered by word frequency**. There's no alphabet stage and no grammar track — the app assumes a learner who can already read Arabic script and takes them straight into meaning.

## ✅ Built so far

- [x] Jetpack Compose + Material 3, green brand theme (day/night, dynamic color disabled)
- [x] MVVM + Hilt DI, feature-based package structure
- [x] Room offline cache (curriculum content, lessons, exercises, word-frequency, progress, stats)
- [x] DataStore for settings/onboarding state
- [x] Local backup: JSON export/import of progress (`BackupRepository`, Settings screen) — the only way progress carries across devices; see [`docs/BACKUP_AND_SYNC.md`](BACKUP_AND_SYNC.md)
- [x] Onboarding: language → **Qur'an font style picker** → home
- [x] **Quranic Vocabulary curriculum — the full frequency curve**, not a sample: 3,680 words (every lemma in the Quranic Arabic Corpus's public frequency table), teach-then-quiz per word, real sourced meanings/roots/example verses — see [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) for the full ingestion methodology
- [x] Points, streak (timezone-safe), lesson unlock progression
- [x] Bangla + English UI, user-chosen at setup, applied both to static resources (`AppCompatDelegate` + explicit recreate on API < 33) and to JSON-sourced content — see [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)
- [x] Home skill path with motion (winding node layout, animated point/streak counters, lesson-complete celebration)
- [x] **Highlighted verse words + meanings** — the word-intro teach step visually highlights the taught word within its example verse (Arabic) and, where a confident best-effort match exists, its gloss within the translation. See [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) for the real (partial) coverage numbers and a content-quality gap this surfaced.
- [x] **R8/ProGuard enabled for release builds** — `optimization { enable = true }` + keep rules for the `ExerciseContent` polymorphic serialization hierarchy; verified via a successful `assembleRelease` and inspecting the R8 mapping file, though full on-device runtime verification still needs a signed build — see [`SECURITY.md`](../SECURITY.md)
- [x] Unit tests for streak/scoring/teach-step-scoring logic + a content-parsing regression test that decodes the full generated vocabulary content through the real Kotlin serializers
- [🚧] **Chapter → section → lesson restructuring**, with section/chapter exams gating progression — actively landing in code; treat `core/data/local/entity/` and `MEMORY.md`'s status notes as current, not this doc

> ⚠️ **`meaning["bn"]` is independently verified against quran.gtaf.org for 1,526/3,680 (41.5%) vocabulary words** (`WordIntro.meaningReviewed["bn"] = true`); the remaining 2,154 stay AI-drafted and flagged `false`, honestly reflecting that no independent source covers them yet. See [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) for exactly which parts are sourced versus AI-drafted.

## 🔜 Explicitly deferred

| Item | Why it's not built yet |
|---|---|
| **Exam engine (section/chapter exams)** | Lands alongside the chapter/section restructuring above |
| **Re-verify root-matched example verses** | ~58% of root-matched vocabulary lemmas' "example verse" comes from the root's own curated list and was never confirmed to literally contain that exact lemma (discovered while building highlight spans — see [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md)); affects both pedagogical accuracy and highlight coverage |
| **Real branching curriculum-tree graph** | `HomeScreen` ships a winding linear path (motion, staggered node layout), not a *branching* visual tree |
| **Admin/content-authoring tooling** | A CMS so lessons can be added without an app release |
| ~~**Push notifications / streak reminders**~~ | ✅ Done — opt-in, local-only (`StreakReminderWorker`/`StreakReminderScheduler`, WorkManager `PeriodicWorkRequest`, no `AlarmManager`/`BOOT_COMPLETED` receiver needed). Configurable in Settings → Notifications (time-of-day picker), only fires when there's an actual streak at risk of breaking. Respects the Android 13+ `POST_NOTIFICATIONS` runtime permission flow. |
| **Achievements/badges beyond raw points** | Not built |
| **Word-pronunciation audio (TTS)** | `tools/ingestion/12_generate_word_audio.py` synthesizes one clip per word via Google Cloud Text-to-Speech (`ar-XA-Wavenet-B`, male), bundled directly in the APK at `app/src/main/assets/audio/words/` — see [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) |
| **Independently-verified Bangla vocabulary meanings for the remaining 58.5%** | 41.5% now verified against gtaf.org, tracked via `meaningReviewed["bn"]` — see the callout above |
| **7 of 10 Qur'an font styles** | IndoPak, IndoPak Nastaleeq, Nurani, Taha, Al-Qalam, KFGQPC, Madani Simple — selectable in the picker, render with system fallback until their real licensed font files are sourced |
| **Analytics/crash reporting** | Not integrated, by design — the app makes no network requests at all |
| **Release signing keystore** | R8/ProGuard is enabled and verified via `assembleRelease` + mapping-file inspection (see [`SECURITY.md`](../SECURITY.md)), but there's still no signing config — a keystore is a secret only the developer should generate/hold |
| ~~**CI/CD**~~ | ✅ Done — `.github/workflows/android-ci.yml` runs unit tests, lint (fails on errors), and `assembleDebug`/`assembleRelease` on every push/PR to `main` |
| **Play Store listing / Data Safety form** | Business/account tasks outside repo scope — see [`SECURITY.md`](../SECURITY.md) |

## 📖 Open content dependencies (flagged, not fabricated)

Real open-licensed sources are cataloged in [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md), which also states plainly which parts are genuinely sourced versus AI-drafted-and-tracked:

1. **The real Qur'an Arabic text + Bangla/English translations** — sourced and **ingested**: Quran-bil-Quran (MIT, Arabic + English) and risan/quran-json (CC BY-SA 4.0, Bangla) back every vocabulary example verse.
2. **The real word-frequency table with meanings and example verses** — sourced and **ingested**: the full 3,680-lemma Quranic Arabic Corpus frequency table, cross-matched against Quran-bil-Quran's root data. `meaningEn` is sourced/derived; `meaning["bn"]` is independently verified against quran.gtaf.org for 41.5% of words (tracked via `meaningReviewed["bn"]`), AI-drafted and flagged for the rest.
3. **Word-pronunciation audio** — synthesized (not a licensed recitation corpus): gTTS-generated clips bundled per word, see `docs/CONTENT_SOURCES.md`.
4. **7 proprietary Qur'an font files** — still unsourced; typically not freely redistributable.

## 🧭 Suggested next steps

1. Finish the chapter/section/exam restructuring and reseed content around the new hierarchy
2. Close the remaining 58.5% of `meaning["bn"]` gap — either extend verse-span coverage beyond the current 1,582/3,680 so more words become gtaf.org-checkable, or have a Bangla-fluent reviewer verify the rest by hand, flipping `meaningReviewed["bn"]` as each is confirmed
3. Full illustrated UI redesign, once art direction/asset licensing is decided (same discipline as fonts)
4. Re-verify the ~58% of root-matched example verses that don't literally contain their word (see the open content dependencies above) — improves both pedagogical accuracy and highlight coverage
6. CI/CD, Play Store readiness, and a hosted privacy policy ahead of a real release
