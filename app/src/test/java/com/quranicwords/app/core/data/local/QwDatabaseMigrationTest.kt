package com.quranicwords.app.core.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.quranicwords.app.core.data.local.migration.Migrations
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v5 is what v1.0.0 and v1.0.1 shipped, so every learner table must survive the upgrade with
 * its rows intact - this is the guard against ever going back to destructive migration. Every
 * later version gets the same treatment, chained from v5 so the real upgrade path is exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QwDatabaseMigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), QwDatabase::class.java)

    @Test
    fun migrate5To6_keepsAllLearnerData() = runTest {
        helper.createDatabase(dbName, 5).apply {
            execSQL("INSERT INTO user_stats VALUES ('u1', 420, 9, 12, '2026-09-30')")
            execSQL("INSERT INTO user_progress VALUES ('u1', 'les_1', 'COMPLETED', 90, 1700000000000, 60000)")
            execSQL("INSERT INTO exercise_attempts (userId, itemId, itemKind, exerciseType, wasCorrect, attemptedAtEpochMillis) VALUES ('u1', 'w_0001', 'WORD', 'MULTIPLE_CHOICE', 0, 1700000000000)")
            execSQL("INSERT INTO achievements VALUES ('u1', 'first_lesson', 1700000000000)")
            execSQL("INSERT INTO daily_practice VALUES ('u1', '2026-09-30', 14)")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 6, true, *Migrations.ALL).close()

        // Open through Room itself so entity decoding (incl. the new defaulted column) is exercised.
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(420, db.userStatsDao().get("u1")?.totalPoints)
            assertEquals(12, db.userStatsDao().get("u1")?.longestStreak)
            assertEquals(90, db.userProgressDao().get("u1", "les_1")?.bestScorePercent)
            val attempts = db.exerciseAttemptDao().getAllForUser("u1")
            assertEquals(1, attempts.size)
            assertTrue("pre-v6 attempts default to first-try", attempts.single().isFirstTry)
            assertEquals(1, db.achievementDao().getAllForUserOnce("u1").size)
            assertEquals(14, db.dailyPracticeDao().getAllForUserOnce("u1").single().minutesPracticed)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate5To6To7_keepsAllLearnerDataAndAddsGamificationDefaults() = runTest {
        val name = "migration-test-7.db"
        helper.createDatabase(name, 5).apply {
            execSQL("INSERT INTO user_stats VALUES ('u1', 420, 9, 12, '2026-09-30')")
            execSQL("INSERT INTO user_progress VALUES ('u1', 'les_1', 'COMPLETED', 90, 1700000000000, 60000)")
            execSQL("INSERT INTO exercise_attempts (userId, itemId, itemKind, exerciseType, wasCorrect, attemptedAtEpochMillis) VALUES ('u1', 'w_0001', 'WORD', 'MULTIPLE_CHOICE', 1, 1700000000000)")
            execSQL("INSERT INTO achievements VALUES ('u1', 'first_lesson', 1700000000000)")
            execSQL("INSERT INTO daily_practice VALUES ('u1', '2026-09-30', 14)")
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, *Migrations.ALL).apply {
            // A row written by a v6 build, between the two upgrades.
            execSQL("INSERT INTO exercise_attempts (userId, itemId, itemKind, exerciseType, wasCorrect, attemptedAtEpochMillis, isFirstTry) VALUES ('u1', 'w_0002', 'WORD', 'MULTIPLE_CHOICE', 0, 1700000500000, 0)")
            close()
        }

        helper.runMigrationsAndValidate(name, 7, true, *Migrations.ALL).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val stats = checkNotNull(db.userStatsDao().get("u1"))
            assertEquals(420, stats.totalPoints)
            assertEquals(9, stats.currentStreak)
            assertEquals(12, stats.longestStreak)
            assertEquals("2026-09-30", stats.lastActivityLocalDate)
            assertEquals(5, stats.hearts)
            assertEquals(0, stats.bestCombo)
            assertFalse("migrated learners keep playing without hearts", stats.heartsEnabled)
            assertEquals(90, db.userProgressDao().get("u1", "les_1")?.bestScorePercent)
            val attempts = db.exerciseAttemptDao().getAllForUser("u1").sortedBy { it.attemptedAtEpochMillis }
            assertEquals(2, attempts.size)
            assertTrue(attempts[0].isFirstTry)
            assertFalse(attempts[1].isFirstTry)
            assertEquals(1, db.achievementDao().getAllForUserOnce("u1").size)
            assertEquals(14, db.dailyPracticeDao().getAllForUserOnce("u1").single().minutesPracticed)
            assertEquals("memory is backfilled at runtime, not by SQL", 0, db.wordMemoryDao().countForUser("u1"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate7To8_addsAnEmptyVersesTableAndKeepsLearnerData() = runTest {
        val name = "migration-test-8.db"
        helper.createDatabase(name, 5).apply {
            execSQL("INSERT INTO user_stats VALUES ('u1', 420, 9, 12, '2026-09-30')")
            execSQL("INSERT INTO user_progress VALUES ('u1', 'les_1', 'COMPLETED', 90, 1700000000000, 60000)")
            execSQL("INSERT INTO exercise_attempts (userId, itemId, itemKind, exerciseType, wasCorrect, attemptedAtEpochMillis) VALUES ('u1', 'wp_1', 'WORD', 'MULTIPLE_CHOICE', 1, 1700000000000)")
            close()
        }
        helper.runMigrationsAndValidate(name, 7, true, *Migrations.ALL).apply {
            // Rows written by a v7 build.
            execSQL("INSERT INTO word_memory VALUES ('u1', 'wp_1', 'REVIEW', 3.5, 5.0, 1700000900000, 2, 0, 3, 1700000000000, '2026-09-30')")
            execSQL("INSERT INTO daily_quests VALUES ('u1', '2026-09-30', 'q1', 'LESSONS', 2, 1, 20, NULL)")
            close()
        }

        helper.runMigrationsAndValidate(name, 8, true, *Migrations.ALL).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java, name)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("verses are filled by the content reseed, not by SQL", 0, db.verseDao().count())
            db.verseDao().insertAll(
                listOf(
                    com.quranicwords.app.core.data.local.entity.VerseEntity(
                        key = "2:8", reference = "Al-Baqarah 2:8", arabic = "وَمِنَ ٱلنَّاسِ",
                        wordByWord = mapOf("en" to "And of the people"), translation = mapOf("en" to "And of the people")
                    )
                )
            )
            assertEquals("Al-Baqarah 2:8", db.verseDao().get("2:8")?.reference)
            assertEquals(mapOf("en" to "And of the people"), db.verseDao().getAll(listOf("2:8", "9:9")).single().wordByWord)
            assertEquals(420, db.userStatsDao().get("u1")?.totalPoints)
            assertEquals(90, db.userProgressDao().get("u1", "les_1")?.bestScorePercent)
            assertEquals(1, db.exerciseAttemptDao().getAllForUser("u1").size)
            assertEquals(1, db.wordMemoryDao().countForUser("u1"))
        } finally {
            db.close()
        }
    }
}
