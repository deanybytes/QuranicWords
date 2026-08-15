# 🗺️ Roadmap

This is now a **full architectural foundation + two complete, playable tiers**: Novice (Arabic Alphabet, isolated + joined letter forms) and Reader (Quranic Vocabulary, the full 3,680-word frequency curve). Tier 3 (Scholar-track grammar) remains scaffolded at the schema/navigation level only — hand-authoring a grammar curriculum from the Corpus's syntactic treebank is a genuinely separate, later effort, tracked here.

> **Product decision:** Google Sign-In / Firebase Auth / Firestore (progress sync + leaderboard) have been removed — the app is local-device-only now, with a local JSON backup export/import (`BackupRepository`) replacing cloud sync. The removed code is commented out (not deleted) throughout the codebase, in case a cloud path is reinstated later.

## ✅ Built this increment

- [x] Jetpack Compose + Material 3, green brand theme (day/night, dynamic color disabled)
- [x] MVVM + Hilt DI, feature-based package structure
- [x] Room offline cache (letters, modules, lessons, exercises, word-frequency, progress, stats)
- [x] DataStore for settings/onboarding state
- [x] ~~Firebase Auth (Google + Anonymous, with guest→Google linking)~~ — **since removed**, see below
- [x] ~~Cloud Firestore progress sync + leaderboard~~ — **since removed**, see below
- [x] Local backup: JSON export/import of progress (`BackupRepository`, Settings screen) — replaces the cloud sync above with a local-device-only model
- [x] Full onboarding: language → tier → **Qur'an font style picker** → home
- [x] Novice tier Arabic Alphabet module — **teach-then-quiz per letter**, all 28 letters × isolated + joined (INITIAL/MEDIAL/FINAL) forms, 14 lessons, 214 exercises — see [`docs/CURRICULUM_DESIGN.md`](CURRICULUM_DESIGN.md)
- [x] **Reader tier Quranic Vocabulary module — the full frequency curve**, not a sample: 3,680 words (every lemma in the Quranic Arabic Corpus's public frequency table), teach-then-quiz per word, 368 lessons, 8,096 exercises, real sourced meanings/roots/example verses — see [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) for the full ingestion methodology
- [x] Points, streak (timezone-safe), lesson unlock progression, now generalized across every implemented module (not hardcoded to one)
- [x] Bangla + English UI, user-chosen at setup, applied both to static resources (`AppCompatDelegate` + explicit recreate on API < 33) and to JSON-sourced content — see [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)
- [x] Home skill path with motion (winding node layout, animated point/streak counters, lesson-complete celebration)
- [x] **Ranked, paginated, tier-filtered leaderboard** — rank numbers, cursor-based "load more" past the live first page, an explicit self-placement-not-skill-rank disclaimer, `firestore.indexes.json`/`firebase.json` for the composite index the tier filter needs (see [`docs/FIREBASE_SETUP.md`](FIREBASE_SETUP.md) for the one-time `firebase deploy` step)
- [x] **WorkManager-backed Firestore sync retry queue** — a durable, exponential-backoff retry job (`FirestoreMirrorRetryWorker`, Hilt-injected) enqueued only when the immediate synchronous mirror attempt in `completeLesson` fails; the local-first architecture (Room write is instant, Firestore is best-effort) is unchanged, this only replaces "silently dropped on failure" with "retried until it lands." Retries are now bounded (`MAX_RETRY_ATTEMPTS = 10`) instead of indefinite.
- [x] **Real, deployed-from-repo Firestore security rules** (`firestore.rules`) — fixes a reported "leaderboard isn't working" bug whose root cause was that the rules had only ever existed as documentation, never actually attached to the live project, so every leaderboard read/write silently hit Firestore's default deny-all. Firestore query failures are now logged and surfaced to the UI as a distinct error state (with retry) instead of rendering identically to "genuinely empty."
- [x] **Highlighted verse words + meanings** — `WordIntroExerciseContent` now visually highlights the taught word within its example verse (Arabic) and, where a confident best-effort match exists, its gloss within the translation. See [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) for the real (partial) coverage numbers and a content-quality gap this surfaced.
- [x] **Home screen lag fix** — the vocabulary module's lesson path (368 lessons) now renders as a single flat, fully virtualized `LazyColumn` item per lesson instead of eagerly composing every node up front; this was previously listed below as an untested/deferred risk and has since been fixed and confirmed.
- [x] **R8/ProGuard enabled for release builds** — `optimization { enable = true }` + keep rules for the `ExerciseContent` polymorphic serialization hierarchy (the one genuinely reflection-dependent part of this codebase); verified via a successful `assembleRelease` and inspecting the R8 mapping file, though full on-device runtime verification still needs a signed build — see [`docs/SECURITY.md`](SECURITY.md).
- [x] Unit tests for streak/scoring/teach-step-scoring logic + a content-parsing regression test that decodes the full generated vocabulary content through the real Kotlin serializers

> ⚠️ **One-time progress reset on this content restructure.** The Alphabet module's lesson boundaries and ids changed when it moved to teach-then-quiz (3 lessons → 7 → 14). Any existing install's `user_progress` rows for the old lesson ids become orphaned/inert on the next launch (see [`docs/DATA_MODEL.md`](DATA_MODEL.md)) — accepted as a pre-launch, no-real-users tradeoff rather than building a lesson-id migration.

> ⚠️ **`meaningBn` is AI-drafted, not independently verified, for nearly all 3,680 vocabulary words.** Tracked honestly via `WordIntro.meaningBnReviewed` (currently `false` everywhere) and disclosed once in Settings → About — a deliberate tradeoff the user chose explicitly (full coverage now, verification as a tracked follow-up) rather than a smaller, fully-reviewed slice. See [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md) for exactly which parts are sourced versus AI-drafted.

## 🔜 Explicitly deferred

| Item | Why it's not in this increment |
|---|---|
| **Tier 3 (Scholar-track) grammar module** | Sources identified (Corpus treebank/ontology), no package scaffolded yet |
| **Exams engine** | Unit/tier-gating exams, exam-specific scoring rules |
| **Cloud Function leaderboard fan-out** | Client writes its own `leaderboard/{uid}` doc directly for now |
| **Real leaderboard anti-cheat** | `totalPoints` is entirely client-computed; Firestore rules only enforce monotonic non-decrease + a sanity range, not real validation (see [`docs/SECURITY.md`](SECURITY.md) for why a per-write delta cap wasn't a real fix). Needs a Cloud Function recomputing `totalPoints` server-side from the `progress` subcollection — no Cloud Functions setup exists in this project yet |
| **Re-verify root-matched example verses** | ~58% of root-matched vocabulary lemmas' "example verse" comes from the root's own curated list and was never confirmed to literally contain that exact lemma (discovered while building highlight spans — see [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md)); affects both pedagogical accuracy and highlight coverage |
| **Real branching skill-tree graph** | `HomeScreen` ships a winding linear path (motion, staggered node layout), not a *branching* visual tree across modules |
| **Admin/content-authoring tooling** | A CMS so lessons can be added without an app release |
| **Push notifications / streak reminders** | Not built |
| **Achievements/badges beyond raw points** | Not built |
| **Letter tracing exercise type** | Built as a checkpoint-order approximation in an earlier pass, then pulled — true handwriting recognition needs ML infrastructure this increment doesn't build; a real approach can be designed and reintroduced later |
| **Tier 1 stages 3-4 (harakat, simple words)** | Isolated + joined letter forms are built; diacritics and word-reading need a new data model, not yet designed |
| **Full male-voice audio pipeline** | `AudioPlayer`/`WordIntroExercise`/`TapWhatYouHear` all exist and degrade gracefully; `audioAssetPath` fields are populated as intentional forward references but no real audio clips are bundled — EveryAyah + quran-align (CC BY 4.0) are sourced and ready to ingest, not yet done |
| **Independently-verified Bangla vocabulary meanings** | AI-drafted for now, tracked via `meaningBnReviewed` — see the callout above |
| **7 of 10 Qur'an font styles** | IndoPak, IndoPak Nastaleeq, Nurani, Taha, Al-Qalam, KFGQPC, Madani Simple — selectable in the picker, render with system fallback until their real licensed font files are sourced (see below) |
| **Analytics/Crashlytics** | Not integrated |
| **Release signing keystore** | R8/ProGuard is now enabled and verified via `assembleRelease` + mapping-file inspection (see [`docs/SECURITY.md`](SECURITY.md)), but there's still no signing config — a keystore is a secret only the developer should generate/hold, so full on-device release verification remains a manual follow-up |
| **CI/CD** | No pipeline configured yet |
| **Play Store listing / Data Safety form** | Business/account tasks outside repo scope — see [`docs/SECURITY.md`](SECURITY.md) |

## 📖 Open content dependencies (flagged, not fabricated)

Real open-licensed sources are cataloged in [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md), which also states plainly which parts are genuinely sourced versus AI-drafted-and-tracked:

1. **The real Qur'an Arabic text + Bangla/English translations** — sourced and **ingested**: Quran-bil-Quran (MIT, Arabic + English) and risan/quran-json (CC BY-SA 4.0, Bangla) now back every Tier 2 vocabulary example verse.
2. **The real word-frequency table with meanings and example verses** — sourced and **ingested**: the full 3,680-lemma Quranic Arabic Corpus frequency table, cross-matched against Quran-bil-Quran's root data. `meaningEn` is sourced/derived; `meaningBn` is AI-drafted (tracked via `meaningBnReviewed`, not silently presented as verified).
3. **Real audio clips** (male voice only) — sourced but **not yet ingested**: EveryAyah.com + quran-align (CC BY 4.0, word-timestamped, all-male reciters) for word/verse audio. **Letter-name pronunciation audio has no identified source** and needs dedicated recording.
4. **7 proprietary Qur'an font files** — still unsourced; typically not freely redistributable.

## 🧭 Suggested next increment

1. Independently verify (or have a Bangla-fluent reviewer verify) the AI-drafted `meaningBn` values, flipping `meaningBnReviewed` as each is confirmed — start with the highest-frequency bands (0–25%, 25–50%) since they're seen most often
2. Real audio ingestion (EveryAyah + quran-align) for Tier 2 word/verse playback; letter-name audio still needs sourcing/recording
3. Tier 1 stages 3-4: harakat/diacritics and simple words (needs new data model — no entity for diacritic marks exists yet)
4. Exams engine (unlocks tier-gating)
5. Tier 3 grammar module, built on the Corpus's syntactic treebank/ontology
6. Full illustrated UI redesign, once art direction/asset licensing is decided (same discipline as fonts/audio)
7. Re-verify the ~58% of root-matched example verses that don't literally contain their word (see the open content dependencies above) — improves both pedagogical accuracy and highlight coverage
8. A Cloud Function to recompute leaderboard `totalPoints` server-side, closing the client-authoritative points gap noted in [`docs/SECURITY.md`](SECURITY.md)
