package com.quranicwords.app.core.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.quranicwords.app.core.data.local.migration.Migrations
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v5 is what v1.0.0 and v1.0.1 shipped, so every learner table must survive the upgrade with
 * its rows intact - this is the guard against ever going back to destructive migration.
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
}
