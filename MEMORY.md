# MEMORY.md

Durable project-context notes for QuranicWords, kept in the repo (not in `.gitignore`) so the record survives across sessions and contributors. The full phased implementation plan lives outside this repo at `/home/rafi/.claude/plans/clone-the-full-project-serene-iverson.md` — this file is a shorter in-repo companion, not a replacement.

## What this is

QuranicWords is a Kotlin + Jetpack Compose + Material 3 app, MVVM + Hilt, Room, offline-first. Scope is deliberately narrow — **Quranic vocabulary learning only**, ordered by word frequency, with no alphabet stage and no grammar track. Content is being restructured from a flat module→lesson list into **chapter → section → lesson**, with section/chapter exams gating progression (80% pass threshold to unlock the next unit). There is no sign-in, no cloud backend, and no leaderboard — none of that exists in this codebase. Intended for real Google Play Store publish.

## Status

- **Phase 0 (foundation)** — done. Package set to `com.quranicwords.app`, `Qw` identifier prefix used throughout, the curriculum scoped to vocabulary-only from the start, Room DB at schema version 1 with no migration history to carry forward.
- **Phase 1 (chapter/section data model)** — done. `ChapterEntity`/`SectionEntity`/`LessonEntity.kind` (`REGULAR`/`LESSON_FLASHBACK`/`SECTION_EXAM`/`SECTION_FLASHBACK`/`CHAPTER_EXAM`/`CHAPTER_FLASHBACK`) replace the old flat `ModuleEntity`→`LessonEntity` shape. `tools/ingestion/11_build_curriculum.py` re-slices the 3,680-word corpus into 8 chapters × 10 sections × ~10-word lessons (887 lessons total, 17,540 exercises), interleaving lesson/section/chapter flashback checkpoints per the three-level design. Unlock/gating logic lives in the pure `CurriculumUnlockResolver` (unit-tested) plus `ProgressRepositoryImpl` (80% pass threshold on every non-`REGULAR` kind). `HomeScreen`/`HomeViewModel` render the chapter→section→lesson tree (functional, visual polish deferred to Phase 3).
- **Phases 2-6** — not started. See the plan file for the full breakdown (licensing/audio pipeline, UI/animation overhaul, copy pass, verification, Play Store readiness).

Verified after every phase: `./gradlew :app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` all pass.

## Key decisions (see the plan file for the complete table and rationale)

- Chapter/section split: 8 chapters × ~10 sections, contiguous frequency-rank slices, never reordered.
- Exam pass threshold: 80%, at lesson/section/chapter flashback and exam levels alike.
- Word audio: single-word clips segmented from EveryAyah.com (Alafasy) + quran-align timing, both CC BY 4.0; streamed on demand by default, with a Settings bulk-download-to-device option.
- Content licensing: NOTICE file credits Quranic Arabic Corpus (GPL), Quran-bil-Quran (MIT), risan/quran-json (CC BY-SA 4.0); app's own code is proprietary/all-rights-reserved, `Copyright © rmrashahriar 2026`.
- Mid-stream additions folded into the plan's addendum: reverse-direction (meaning-shown, Arabic-blanked) quiz variants, gradually increasing cloze difficulty, three-level flashback review exams (lesson/section/chapter, each scoped to earlier siblings under the same parent).

## Working agreements

- Keep this file and `CLAUDE.md` tracked in git — never add either to `.gitignore`.
- Commit and push work to `git@github.com:rmrashahriar/QuranicWords.git` regularly rather than letting it sit local-only; cut a GitHub release after each clean build milestone.
