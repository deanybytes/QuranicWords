# 🔐 Security

> **Note:** QuranicWords has no sign-in, no remote database, and no leaderboard — none of that code exists in this repository. There's nothing to secure on a server, because there is no server. The local backup file (`BackupRepository`, exported via Settings) contains a device's full progress in plaintext JSON — treat it like any other user data export (no secrets embedded, but not encrypted either).

## 🔑 Secrets handling

| Secret | Where it lives | Committed to git? |
|---|---|---|
| Signing keystore (`*.jks`, `*.keystore`) | Local / CI secret store | ❌ `.gitignore`d |
| `keystore.properties`, `signing.properties` | Local / CI secret store | ❌ `.gitignore`d |

There are no API keys, backend credentials, or third-party service tokens anywhere in this app — it makes no network requests at all.

## 🕵️ Data privacy

- **No PII collected.** A locally-generated UUID (`UserPreferencesDataStore.getOrCreateLocalUserId()`) keys a learner's local progress; nothing personally identifying is generated or requested, and nothing ever leaves the device.
- **No analytics or crash reporting is wired up** — nothing is sent anywhere, ever, in this build. There is no network permission usage beyond what the OS itself requires.
- **No PII is ever logged.** The small amount of `Log.w` logging that exists (e.g. `AudioPlayer` logging the asset path when playback fails) is deliberately limited to exception objects and asset paths, never user identifiers or displayed content.
- **The local backup file is plaintext JSON, not encrypted.** A learner who exports it and shares the file would be sharing their own progress data (points, streak, per-word attempt history) — worth a line in Settings' backup UI, but not a secret-handling concern the app itself needs to solve.
- Room DB and DataStore preferences are excluded from Android Auto Backup (`app/src/main/res/xml/backup_rules.xml` / `data_extraction_rules.xml`) as defense-in-depth, even though current local data has no PII beyond a random UUID.

## 🧾 Play Store / production compliance — what's outside this repo's reach

| Item | Status |
|---|---|
| Privacy Policy (hosted, public URL) | ⬜ Not created — required before Play Store submission, must be drafted/reviewed and hosted by the developer |
| Play Console "Data Safety" form | ⬜ Business/account task, can't be filled from source code — should be straightforward given the app collects no data |
| Release signing keystore | ⬜ Must be generated and held privately by the developer — never something an agent should generate and hand back in plaintext |
| R8/ProGuard tuning for the `release` build type | ✅ Enabled (`optimization { enable = true }` + [`app/proguard-rules.pro`](../app/proguard-rules.pro) keeping the `ExerciseContent` kotlinx.serialization hierarchy's `$$serializer`/`Companion` classes — Room and Hilt bundle their own consumer rules automatically). Verify via `./gradlew :app:assembleRelease` and inspecting `app/build/outputs/mapping/release/mapping.txt`, confirming the serializer/companion classes for every `ExerciseContent` subtype survived un-renamed while the data classes themselves were properly obfuscated. ⚠️ **What this does *not* verify**: actual runtime behavior on a signed, installed release build — there's no signing config in this repo (by design, a keystore is a secret only the developer should hold), so that remains a manual follow-up before the first real release. |
| Accessibility deep audit (TalkBack pass on a real device) | ⬜ Semantic groundwork is in (content descriptions, live regions, 48dp touch targets) but not device-verified |

## ✅ What *is* covered today

- No secrets committed (`.gitignore` covers keystores and `*.properties` signing files).
- No hardcoded credentials anywhere in source — there's nothing to authenticate to.
- R8/ProGuard code shrinking and obfuscation enabled for release builds, with keep rules verified against the one genuinely reflection-dependent part of this codebase (`ExerciseContent`'s polymorphic serialization).
- Room DB and DataStore preferences excluded from Android Auto Backup as defense-in-depth.
- The app makes zero network requests, which removes an entire category of attack surface (interception, server compromise, credential leakage) by construction rather than by hardening.
