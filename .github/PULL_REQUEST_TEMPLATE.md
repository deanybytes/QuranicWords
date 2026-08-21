## What does this PR do?

<!-- Briefly describe the change and why it's needed. -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Content/data change (`app/src/main/assets/content/`, `tools/ingestion/`)
- [ ] Documentation
- [ ] Refactor / chore

## How was this tested?

- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew lint` passes
- [ ] Manually verified on a device/emulator (describe what you checked below)

<!-- What did you actually do to confirm this works? -->

## Checklist

- [ ] I bumped `ContentSeeder.CONTENT_VERSION` if I changed the shape of bundled content JSON
- [ ] I updated the relevant doc under `docs/` if this change makes it stale
- [ ] This change doesn't render human faces or use scripture as decoration (see `CLAUDE.md`)
