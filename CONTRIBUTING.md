# Contributing to QuranicWords

Thanks for your interest in contributing. This is a single-module Android app (Kotlin + Jetpack Compose + Material 3, MVVM + Hilt, Room, offline-first) — see [`CLAUDE.md`](CLAUDE.md) for the architecture overview and [`docs/`](docs/) for detailed docs on each area.

## Getting set up

```bash
git clone https://github.com/rmrashahriar/QuranicWords.git
cd QuranicWords
./gradlew :app:assembleDebug
```

You'll need an Android SDK installed and `local.properties` pointing at it (`sdk.dir=...`) — Android Studio creates this for you automatically on first open. The app builds and runs fully offline with zero configuration; there's no account, no API key, and no backend to stand up.

## Building and testing

```bash
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # run all unit tests (JVM, app/src/test)
./gradlew test --tests "com.quranicwords.app.core.domain.DistractorGeneratorTest"   # single test class
./gradlew lint
```

Run the relevant unit tests before opening a PR. For UI/feature changes, actually exercise the change on a device or emulator — passing tests verify code correctness, not feature correctness.

## Content ingestion pipeline

`tools/ingestion/*.py` (numbered stages `01_...` through `16_...`) is a separate, offline Python pipeline that regenerates the bundled JSON under `app/src/main/assets/content/` from external corpora. It is not part of the Android build or CI. See [`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md) before touching it — it documents exactly which fields are sourced from real data versus AI-drafted-and-flagged, and that discipline (never silently fabricate content) is a hard expectation for any change in this area.

If you change the *shape* of any file under `app/src/main/assets/content/`, bump `ContentSeeder.CONTENT_VERSION` (`app/src/main/java/com/quranicwords/app/core/data/assets/ContentSeeder.kt`) in the same change — otherwise existing installs silently skip reseeding. **A value-only change needs the same bump** — the seeder gates on the version flag alone, not a per-row diff.

### Editing a single word without a full pipeline re-run

For a small, one-off fix (a wrong meaning, a bad example verse) that doesn't warrant re-running a whole pipeline stage, use `tools/ingestion/16_cms.py` — a local interactive CLI (`python 16_cms.py`) to look up a word by id or Arabic text, edit its meaning or example verse directly, and validate the content set before saving. It keeps `word_frequency.json` in sync automatically and flags whatever you edit as `meaningReviewed[lang] = false`/`exampleVerseVerified = false` (a human edit isn't the same as passing through the pipeline's own independent-source cross-checks). It prints the `CONTENT_VERSION` bump reminder when you save — follow it. This is deliberately a local CLI, not a hosted web tool: the app makes zero network requests by design, and a web CMS would be this project's first server.

## Two hard product constraints

These are enforced by design, not just convention — PRs that violate them won't be merged:

- **No human faces anywhere** in icons/illustrations (geometric/calligraphic/nature motifs only).
- **No rendering of actual Ayat/Mushaf text as decoration** — scripture is never used as a loading-screen or gamification skin. (The one narrow, deliberate exception — the every-launch opening invocation — is documented inline where it's implemented.)

## Pull requests

- Keep PRs focused — one logical change per PR.
- Follow the existing code style (see `CLAUDE.md`'s guidance on comments: default to none, only explain non-obvious *why*).
- Update the relevant doc under `docs/` if your change makes it stale.
- Describe what you tested (unit tests run, device/emulator manual testing) in the PR description.

## Reporting bugs / requesting features

Use the issue templates under [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/).

## License

By contributing, you agree that your contributions will be licensed under the project's [GPL-3.0 license](LICENSE).
