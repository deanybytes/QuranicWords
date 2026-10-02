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

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6)
}
