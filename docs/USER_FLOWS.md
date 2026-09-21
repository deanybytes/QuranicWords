# 🧭 User Flows

## 1️⃣ Onboarding & Learning Path Setup

Every step **persists to DataStore the moment it's chosen**, so a killed/restarted process resumes exactly where the learner left off (`SplashViewModel` re-derives the start destination from what's already saved).

```mermaid
sequenceDiagram
    actor User
    participant Splash
    participant Lang as Language Select
    participant Path as Learning Path
    participant Font as Font Select
    participant Style as Learning Style
    participant Goal as Daily Goal
    participant Home

    User->>Splash: Cold start
    Splash->>Splash: ContentSeeder.seedIfNeeded()
    alt language not set
        Splash->>Lang: navigate
        User->>Lang: Select Language
        Lang->>Path: navigate
    end
    alt path not chosen
        User->>Path: Choose Learn & Test vs Test Only
        alt Test Only
            Path->>Font: navigate
            Font->>Goal: navigate
            Goal->>Home: Open Test-Only Hub (6 Modes)
        else Learn & Test
            Path->>Font: navigate
            Font->>Style: Choose Reinforcement Level
            Style->>Goal: Set Daily Words Target
            Goal->>Home: Open Curriculum Map (10 Chapters)
        end
    end
```

### Font Selection
Learners preview **Surah Al-Kawthar** in a clean interface displaying the script name and live Arabic sample (Amiri, Scheherazade, Noto Naskh, Lateef IndoPak, Noto Nastaliq Urdu).

## 2️⃣ 6-Mode Test-Only Flow

For test-focused learning:
```mermaid
flowchart TD
    TestHome[Test-Only Hub] --> Mode1[1. Ism Mode: 3,057 Nouns]
    TestHome --> Mode2[2. Fi'l Mode: 1,479 Verbs]
    TestHome --> Mode3[3. Ḥarf Mode: 173 Particles]
    TestHome --> Mode4[4. Mix / Random Mode: All 4,709 Words with Grammar Badges]
    TestHome --> Mode5[5. Mistaken Words Review: Adaptive Error Revision]
    TestHome --> Mode6[6. Chapterwise Practice & Test Mode: 10 Quranic Chapters]
    Mode1 & Mode2 & Mode3 & Mode4 & Mode5 & Mode6 --> DynQuiz[Dynamic In-Verse Quizzes:<br/>• Reverse Verse Tap<br/>• Verse Completion<br/>• Contextual Multiple Choice]
    DynQuiz --> Summary[Lesson Summary with Full Vocabulary Breakdown]
```

## 3️⃣ Lesson Gameplay & Summary Loop

```mermaid
flowchart TD
    Start([Open lesson or test mode]) --> Load[Load + decode exercises]
    Load --> Show[Show exercise N]
    Show --> Type{Exercise type}
    Type -->|Teach step<br/>non-scored| Teach["Word Intro:<br/>Multi-meaning tabs + example verses"]
    Teach --> ContinueTeach["Tap Continue"] --> More
    Type -->|Multiple choice /<br/>Fill in the blank| Select[User selects an option<br/>Live verse context & highlighted translation]
    Select --> Check[User taps Check]
    Check --> Feedback1[Feedback banner:<br/>correct ✅ / incorrect ❌ + answer]
    Type -->|Matching| Match[User pairs left ↔ right tiles<br/>Highlighted glass translation on tile select]
    Match -->|all pairs matched| Feedback1
    Type -->|Word order / Verse tap| Build[User builds sequence or taps target verse word<br/>Highlighted glass translation displayed]
    Build --> Check
    Feedback1 --> More{More exercises?}
    More -->|yes| Continue[Tap Continue] --> Show
    More -->|no| Summary[Lesson Summary Screen]
    Summary --> Stats["Alhamdulillah!<br/>Words Covered · Mistakes · Accuracy %"]
    Summary --> WordList["Vocabulary Breakdown:<br/>Arabic words + Grammar category badges + Meanings"]
    Summary --> Preview["Next Lesson Preview:<br/>Upcoming words & context (curriculum mode)"]
    Summary --> NextAction{Learner Choice}
    NextAction -->|Proceed| NextLesson([Next Lesson in Curriculum])
    NextAction -->|Back| HomeMap([Curriculum Map or Test Hub])
```

## 4️⃣ Local Backup Export & Import

Progress is 100% on-device and offline. Learners can export a full JSON backup to their local storage via Android Storage Access Framework (SAF) and restore it at any time on another device.
