# 🗺️ Roadmap

## ✅ Built

- **A verified curriculum, rebuilt from the Quranic Arabic Corpus.** 3,833 distinct lemmas in
  10 part-of-speech chapters, ordered by frequency, covering 97.4% of the Qur'an's lexical
  segments. Meanings in 8 languages come from each word's own GTAF word-by-word occurrences.
  Exact verse highlights, roots, verb forms and contextual senses.
- **A reproducible content pipeline.** `tools/pipeline` uses pinned sources, a validator that
  fails the build on corrupt content, and golden-word tests. CI checks that committed assets
  match a fresh build.
- **Safe upgrades.** Real Room migrations replace destructive ones, the database is snapshotted
  before every upgrade, and lesson completion and backup import are transactional. Progress
  from v1.0.0/v1.0.1 is migrated onto the rebuilt curriculum.
- **Honest scoring.** Only first tries count. Matching mistakes count, failed exams stay open,
  and the 80% exam gate is enforced.
- **Spaced repetition (FSRS).** A Daily Review with due counts, and word strength from New to
  Mastered.
- **Gamification:** XP and levels, combos, optional hearts, daily quests, streaks with recovery,
  a daily-goal ring, achievements with progress, and celebrations.
- **Exercises:** meaning, verse completion, tap-the-word-in-verse and matching.
- **Accessibility and localization:** TalkBack semantics, RTL verse layout, localized digits,
  plurals and dates.
- **Web edition** at quranicwords.vercel.app: offline-capable PWA, learn path, spaced review
  and quizzes, all on-device. Also a standalone HTML dictionary.
- **Privacy:** 100% offline, no telemetry, local backup export/import.

## 🔜 Next

| Item | Description |
|---|---|
| Native-speaker review | Work through `tools/pipeline/reports/review_queue.tsv` (low-confidence meanings) with reviewers per language |
| Remaining rare words | Hand-curated meanings for the ~880 excluded hapax lemmas |
| Tajweed visualizer | Colour-coded tajweed in verse examples |
