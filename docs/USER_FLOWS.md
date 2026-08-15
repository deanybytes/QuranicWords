# 🧭 User Flows

## 1️⃣ Onboarding — first launch

Every step **persists to DataStore the moment it's chosen**, so a killed/restarted process resumes exactly where the learner left off (`SplashViewModel` re-derives the start destination from what's already saved — see the decision table below).

> Google Sign-In / Firebase Auth were removed (commented out, not deleted) — onboarding no longer has an Auth Choice step, and there's no guest/Google distinction. A single local user id (`UserPreferencesDataStore.getOrCreateLocalUserId()`) is generated on first launch and used for all Room progress/stats.

```mermaid
sequenceDiagram
    actor User
    participant Splash
    participant Lang as Language Select
    participant Tier as Tier Select
    participant Font as Font Select
    participant Home

    User->>Splash: Cold start
    Splash->>Splash: ContentSeeder.seedIfNeeded()
    Splash->>Splash: Read language / tier / fontChoiceMade
    alt language not set
        Splash->>Lang: navigate
        User->>Lang: Tap English or বাংলা
        Lang->>Lang: DataStore.setLanguage() [saved instantly]
        Lang->>Tier: navigate
    end
    alt tier not set
        User->>Tier: Pick Novice / Reader / Scholar-track
        Tier->>Tier: DataStore.setTier() [saved instantly]
        Tier->>Font: navigate
    end
    alt font not chosen
        User->>Font: Preview Al-Kawthar in each style, tap one
        Font->>Font: DataStore.setFontStyle() [saved instantly]
        Font->>Home: navigate
    end
    Home->>User: Skill tree, ready to learn
```

### Splash's resume decision table

| `language` | `tier` | `fontChoiceMade` | → routes to |
|---|---|---|---|
| `null` | — | — | Language Select |
| set | `null` | — | Tier Select |
| set | set | `false` | Font Select |
| set | set | `true` | **Home** |

## 2️⃣ Local backup export / import

Progress carries across a reinstall or a new device via a JSON file the learner explicitly exports/imports from Settings — there's no account and no background sync:

```mermaid
sequenceDiagram
    actor User
    participant Settings
    participant SAF as Storage Access Framework
    participant Backup as BackupRepository
    participant Room

    Note over User,Room: Export
    User->>Settings: Tap "Export backup"
    Settings->>SAF: ActivityResultContracts.CreateDocument
    SAF-->>Settings: destination Uri
    Settings->>Backup: exportBackup(outputStream)
    Backup->>Room: read UserStats/UserProgress/ExerciseAttempt for local user id
    Backup-->>Settings: BackupPayload JSON written to Uri

    Note over User,Room: Import (fresh install or another device)
    User->>Settings: Tap "Import backup"
    Settings->>SAF: ActivityResultContracts.OpenDocument
    SAF-->>Settings: source Uri
    Settings->>Backup: importBackup(inputStream)
    Backup->>Room: upsert UserStats/UserProgress/ExerciseAttempt from payload
    Backup->>Settings: DataStore.setLocalUserId(payload.userId)
    Note over Backup: Restored userId becomes this device's local user id,<br/>so every screen reads the restored progress immediately
```

## 3️⃣ Lesson gameplay loop

```mermaid
flowchart TD
    Start([Open lesson]) --> Load[Load + decode exercises]
    Load --> Show[Show exercise N]
    Show --> Type{Exercise type}
    Type -->|Teach step<br/>non-scored| Teach["Show glyph + transliteration<br/>+ pronunciation hint"]
    Teach --> Got["Tap 'Got it'"] --> More
    Type -->|Multiple choice /<br/>Tap-what-you-hear| Select[User selects an option]
    Select --> Check[User taps Check]
    Check --> Feedback1[Feedback banner:<br/>correct ✅ / incorrect ❌ + answer]
    Type -->|Matching| Match[User pairs left ↔ right tiles]
    Match -->|all pairs matched| Feedback1
    Feedback1 --> More{More exercises?}
    More -->|yes| Continue[Tap Continue] --> Show
    More -->|no| Finish[Tap Finish lesson]
    Finish --> Complete["ProgressRepository.completeLesson<br/>(totalCount excludes teach steps)"]
    Complete --> Summary([Lesson Summary:<br/>points, accuracy, streak - animated])
```

A teach step never shows the Check button or the feedback banner - it self-advances via "Got it" straight back into the "more exercises?" branch, the same self-advance pattern `Matching` uses once solved (see [`docs/ALGORITHMS.md`](ALGORITHMS.md) for why it's also excluded from scoring).

**Exit-mid-lesson**: tapping the close (✕) icon shows a confirm dialog — progress for the *in-progress* lesson isn't saved on exit, matching the "no partial credit for abandoned lessons" rule (points are only awarded on `completeLesson`).

## 4️⃣ What `completeLesson` actually does

```mermaid
flowchart LR
    A[correctCount, totalCount] --> B["GamificationConfig.pointsForLesson()"]
    B --> C["StreakCalculator.recordActivity()"]
    C --> D[(Room: UserStatsEntity upsert)]
    C --> E[(Room: UserProgressEntity upsert<br/>status=COMPLETED)]
    E --> F[Unlock next lesson<br/>in the module]
    D & F --> G[Stays local-only —<br/>exportable via BackupRepository]
```

See [`docs/ALGORITHMS.md`](ALGORITHMS.md) for the exact scoring/streak formulas.

## 5️⃣ Switching language after onboarding

Picking a language (at onboarding, or later in Settings) writes to `UserPreferencesDataStore` immediately, same as every other onboarding choice - but unlike those, this one also has to change what's on screen *right now*, not just gate navigation. `MainActivity` observes the stored language and calls `AppCompatDelegate.setApplicationLocales()`, following with an explicit `recreate()` on API < 33 so the change actually applies (see [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) for why that's needed at all). JSON-sourced content (lesson/module titles, exercise prompts) re-renders immediately via `rememberIsBanglaSelected()` without needing a recreate, since it reads the already-updated `Configuration` directly rather than going through resource-qualifier resolution.
