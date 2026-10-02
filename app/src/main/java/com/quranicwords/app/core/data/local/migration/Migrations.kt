package com.quranicwords.app.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written Room migrations. v5 is the schema both released builds (v1.0.0 and v1.0.1) shipped
 * with, so every migration from here on must preserve learner data - see QwDatabase's builder,
 * which deliberately allows destructive fallback only from the never-released versions 1-4.
 */
object Migrations {

    /** 5 → 6: first-try flag on attempts (retries stop counting toward mastery/review), plus the
     * missing index behind every Review / Open Practice exercise lookup by practiced word. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `exercise_attempts` ADD COLUMN `isFirstTry` INTEGER NOT NULL DEFAULT 1"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_exercises_practicedItemId` ON `exercises` (`practicedItemId`)"
            )
        }
    }

    /**
     * 6 → 7: spaced repetition and gamification in one bump. Purely additive - two new tables
     * plus defaulted `user_stats` columns - so every existing row survives untouched.
     * `heartsEnabled` defaults to 0 here on purpose: learners who upgrade keep playing without
     * hearts unless they opt in, while a brand-new install's first stats row is created with it
     * on. `word_memory` starts empty and is backfilled once from attempt history at runtime (see
     * `ProgressRepositoryImpl.seedWordMemoryIfNeeded`) - replaying FSRS needs Kotlin, not SQL.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `word_memory` (`userId` TEXT NOT NULL, `itemId` TEXT NOT NULL, " +
                    "`state` TEXT NOT NULL, `stability` REAL NOT NULL, `difficulty` REAL NOT NULL, " +
                    "`dueAtEpochMillis` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `lapses` INTEGER NOT NULL, " +
                    "`lastGrade` INTEGER NOT NULL, `lastReviewedAtEpochMillis` INTEGER, `lastReviewLocalDate` TEXT, " +
                    "PRIMARY KEY(`userId`, `itemId`))"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_word_memory_userId_dueAtEpochMillis` ON `word_memory` (`userId`, `dueAtEpochMillis`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_word_memory_userId_lastGrade` ON `word_memory` (`userId`, `lastGrade`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_quests` (`userId` TEXT NOT NULL, `localDate` TEXT NOT NULL, " +
                    "`questId` TEXT NOT NULL, `metric` TEXT NOT NULL, `target` INTEGER NOT NULL, " +
                    "`progress` INTEGER NOT NULL, `rewardXp` INTEGER NOT NULL, `completedAtEpochMillis` INTEGER, " +
                    "PRIMARY KEY(`userId`, `localDate`, `questId`))"
            )
            db.execSQL("ALTER TABLE `user_stats` ADD COLUMN `hearts` INTEGER NOT NULL DEFAULT 5")
            db.execSQL("ALTER TABLE `user_stats` ADD COLUMN `heartsUpdatedAtEpochMillis` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `user_stats` ADD COLUMN `bestCombo` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `user_stats` ADD COLUMN `heartsEnabled` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7)
}
