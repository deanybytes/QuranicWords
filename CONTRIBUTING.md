# Contributing to QuranicWords

Thanks for your interest in contributing. This is a single-module Android app (Kotlin + Jetpack Compose + Material 3, MVVM + Hilt, Room, offline-first) — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the architecture overview and [`docs/`](docs/) for detailed docs on each area.

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

## Content pipeline

All vocabulary content under `app/src/main/assets/content/` and `data/` is generated. **Never
hand-edit it.** CI rebuilds it and fails on any difference.

```bash
python3 tools/pipeline/run.py               # rebuild + validate + write assets
python3 -m pytest tools/pipeline/tests      # golden words, validator, determinism
python3 tools/export/build_web_data.py      # web data from the app assets
python3 tools/export/generate_dictionary_html.py   # standalone HTML dictionary
```

To fix a meaning, add a reviewed entry to `tools/pipeline/overrides/` (function words live in
`function_words.tsv`) or improve the extraction rules, then rebuild. If the output changes, bump
`ContentSeeder.CONTENT_VERSION` in the same change so installed apps reseed. Bump it for a
value-only change too. See [`tools/pipeline/README.md`](tools/pipeline/README.md) and
[`docs/CONTENT_SOURCES.md`](docs/CONTENT_SOURCES.md).

`tools/legacy/` holds the old one-off scripts that produced the corrupted v1.0 content. Don't run
them.

## Two hard product constraints

These are enforced by design, not just convention — PRs that violate them won't be merged:

- **No human faces anywhere** in icons/illustrations (geometric/calligraphic/nature motifs only).
- **No rendering of actual Ayat/Mushaf text as decoration** — scripture is never used as a loading-screen or gamification skin. (The one narrow, deliberate exception — the opening invocation — is documented inline where it's implemented.)

## Pull requests

- Keep PRs focused — one logical change per PR.
- Follow the existing code style (default to clear, self-documenting code; only explain non-obvious *why*).
- Update the relevant doc under `docs/` if your change makes it stale.
- Describe what you tested (unit tests run, device/emulator manual testing) in the PR description.

## Open Source License & DEANY TALKS Ecosystem

QuranicWords is licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE). By contributing to QuranicWords, you agree that your contributions will be licensed under its GPL-3.0 terms.

- **Dawah Platform Ecosystem**: QuranicWords is part of the **DEANY TALKS** digital Islamic education initiative.
- **Contact & Inquiries**: Reach out to the core team via email at `contact.deanstalks@gmail.com` or join community discussions on GitHub.

## Reporting bugs / requesting features

Use the issue templates under [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/) or email `contact.deanstalks@gmail.com`.

