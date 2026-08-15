# MEMORY.md

Durable project-context notes for QuranicWords, kept in the repo (not in `.gitignore`) so the record survives across sessions and contributors. The full phased implementation plan lives outside this repo at `/home/rafi/.claude/plans/clone-the-full-project-serene-iverson.md` — this file is a shorter in-repo companion, not a replacement.

## What this is

QuranicWords is a fork of `UnderstandingHolyQuran` (Kotlin + Jetpack Compose + Material 3, MVVM + Hilt, Room, offline-first). Scope is deliberately narrowed to **Tier 2 only** — Quranic vocabulary learning, ordered by word frequency — with Tier 1 (alphabet) and Tier 3 (grammar) removed entirely. Content is restructured from a flat module→lesson list into **chapter → section → lesson**, with section/chapter exams gating progression (80% pass threshold to unlock the next unit). Google Sign-In, Firebase, and the leaderboard are not just disabled but deleted outright (the source app kept them commented for its own history; this fork ships none of it). Intended for real Google Play Store publish.

## Status

- **Phase 0 (fork & strip)** — done. Package renamed `com.example.understandingholyquran` → `com.quranicwords.app`, `Uhq` identifier prefix renamed to `Qw` throughout, Tier-1/Tier-3 code and content deleted, Firebase/auth/leaderboard dead code purged for real, Room DB reset to schema version 1 (no fake migration continuity from the source app). Verified: `./gradlew :app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` all pass.
- **Phases 1-6** — not started. See the plan file for the full breakdown (data model restructuring, licensing/audio pipeline, UI/animation overhaul, copy pass, verification, Play Store readiness).

## Key decisions (see the plan file for the complete table and rationale)

- Chapter/section split: 8 chapters × ~10 sections, contiguous frequency-rank slices, never reordered.
- Exam pass threshold: 80%, at lesson/section/chapter flashback and exam levels alike.
- Word audio: single-word clips segmented from EveryAyah.com (Alafasy) + quran-align timing, both CC BY 4.0; streamed on demand by default, with a Settings bulk-download-to-device option.
- Content licensing: NOTICE file credits Quranic Arabic Corpus (GPL), Quran-bil-Quran (MIT), risan/quran-json (CC BY-SA 4.0); app's own code is proprietary/all-rights-reserved, `Copyright © rmrashahriar 2026`.
- Mid-stream additions folded into the plan's addendum: reverse-direction (meaning-shown, Arabic-blanked) quiz variants, gradually increasing cloze difficulty, three-level flashback review exams (lesson/section/chapter, each scoped to earlier siblings under the same parent).

## Working agreements

- Keep this file and `CLAUDE.md` tracked in git — never add either to `.gitignore`.
- Commit and push work to `git@github.com:rmrashahriar/QuranicWords.git` regularly rather than letting it sit local-only; cut a GitHub release after each clean build milestone.
