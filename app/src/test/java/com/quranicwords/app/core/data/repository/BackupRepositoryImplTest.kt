package com.quranicwords.app.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRepositoryImplTest {

    private lateinit var database: QwDatabase
    private lateinit var preferences: UserPreferencesDataStore
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferencesDataStore(context)
        repository = BackupRepositoryImpl(database, preferences)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `export and import round trip restores progress stats and preferences`() = runTest {
        val userId = preferences.getOrCreateLocalUserId()

        // 1. Seed database state
        val stats = UserStatsEntity(
            userId = userId,
            totalPoints = 750,
            currentStreak = 14,
            longestStreak = 20,
            lastActivityLocalDate = "2026-09-22"
        )
        database.userStatsDao().upsert(stats)

        val progress = listOf(
            UserProgressEntity(
                userId = userId,
                lessonId = "lesson_01",
                status = LessonStatus.COMPLETED,
                bestScorePercent = 100,
                completedAtEpochMillis = 1700000000000L
            ),
            UserProgressEntity(
                userId = userId,
                lessonId = "lesson_02",
                status = LessonStatus.UNLOCKED,
                bestScorePercent = 0,
                completedAtEpochMillis = null
            )
        )
        database.userProgressDao().upsertAll(progress)

        // 2. Set user preferences
        preferences.setLanguage(Language.TURKISH)
        preferences.setThemeMode(ThemeMode.DARK)
        preferences.setFontStyle(QuranFontStyle.MADANI_KFGQPC)
        preferences.setReduceMotion(true)

        // 3. Export backup
        val out = ByteArrayOutputStream()
        val exportResult = repository.exportBackup(out)
        assertTrue("Export must succeed", exportResult.isSuccess)
        val backupBytes = out.toByteArray()
        assertTrue("Backup payload must not be empty", backupBytes.isNotEmpty())

        // 4. Clear/mutate state
        preferences.setLanguage(Language.ENGLISH)
        preferences.setThemeMode(ThemeMode.LIGHT)
        preferences.setReduceMotion(false)
        database.userStatsDao().upsert(
            UserStatsEntity(
                userId = userId,
                totalPoints = 0,
                currentStreak = 0,
                longestStreak = 0,
                lastActivityLocalDate = null
            )
        )
        database.userProgressDao().upsertAll(emptyList())

        // 5. Import backup from byte stream
        val input = ByteArrayInputStream(backupBytes)
        val importResult = repository.importBackup(input)
        assertTrue("Import must succeed", importResult.isSuccess)

        // 6. Verify restored state
        val restoredStats = database.userStatsDao().get(userId)
        assertNotNull(restoredStats)
        assertEquals(750, restoredStats?.totalPoints)
        assertEquals(14, restoredStats?.currentStreak)
        assertEquals(20, restoredStats?.longestStreak)
        assertEquals("2026-09-22", restoredStats?.lastActivityLocalDate)

        val restoredProgress = database.userProgressDao().getAllForUserOnce(userId)
        assertEquals(2, restoredProgress.size)
        val p1 = restoredProgress.find { it.lessonId == "lesson_01" }
        assertEquals(LessonStatus.COMPLETED, p1?.status)
        assertEquals(100, p1?.bestScorePercent)

        assertEquals(Language.TURKISH, preferences.languageFlow.first())
        assertEquals(ThemeMode.DARK, preferences.themeModeFlow.first())
        assertEquals(QuranFontStyle.MADANI_KFGQPC, preferences.fontStyleFlow.first())
        assertEquals(true, preferences.reduceMotionFlow.first())
    }

    @Test
    fun `importing backup with foreign userId remaps all rows to current target userId`() = runTest {
        val currentUserId = preferences.getOrCreateLocalUserId()

        // Foreign backup with foreign userId
        val foreignUserId = "foreign-device-user-999"
        val foreignBackupPayload = com.quranicwords.app.core.domain.model.BackupPayload(
            schemaVersion = com.quranicwords.app.core.domain.model.BACKUP_SCHEMA_VERSION,
            exportedAtEpochMillis = 1700000000000L,
            userId = foreignUserId,
            stats = UserStatsEntity(
                userId = foreignUserId,
                totalPoints = 1200,
                currentStreak = 30,
                longestStreak = 45,
                lastActivityLocalDate = "2026-09-23"
            ),
            progress = listOf(
                UserProgressEntity(
                    userId = foreignUserId,
                    lessonId = "c1_s1_l1",
                    status = LessonStatus.COMPLETED,
                    bestScorePercent = 100,
                    completedAtEpochMillis = 1700000000000L
                )
            ),
            attempts = listOf(
                com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity(
                    id = 42,
                    userId = foreignUserId,
                    itemId = "w_1",
                    itemKind = com.quranicwords.app.core.domain.model.ItemKind.WORD,
                    exerciseType = com.quranicwords.app.core.domain.model.ExerciseType.MULTIPLE_CHOICE,
                    wasCorrect = true,
                    attemptedAtEpochMillis = 1700000000000L
                )
            ),
            achievements = listOf(
                com.quranicwords.app.core.data.local.entity.AchievementEntity(
                    userId = foreignUserId,
                    achievementId = "first_step",
                    unlockedAtEpochMillis = 1700000000000L
                )
            ),
            dailyPractices = listOf(
                com.quranicwords.app.core.data.local.entity.DailyPracticeEntity(
                    userId = foreignUserId,
                    localDate = "2026-09-23",
                    minutesPracticed = 15
                )
            ),
            preferences = com.quranicwords.app.core.domain.model.BackupPreferences(
                languageTag = "bn",
                themeMode = "DARK",
                fontStyle = "UTHMANI_HAFS",
                reduceMotion = true,
                learningPath = "FULL_CURRICULUM",
                learningStyle = "BALANCED",
                dailyGoalLevel = "STANDARD",
                reduceGlassEffects = false,
                soundEnabled = true
            )
        )

        val json = com.quranicwords.app.core.util.AppJson.encodeToString(
            com.quranicwords.app.core.domain.model.BackupPayload.serializer(),
            foreignBackupPayload
        )
        val importResult = repository.importBackup(ByteArrayInputStream(json.toByteArray()))
        assertTrue("Import must succeed", importResult.isSuccess)

        // Verify targetUserId has the restored data
        val restoredStats = database.userStatsDao().get(currentUserId)
        assertNotNull(restoredStats)
        assertEquals(1200, restoredStats?.totalPoints)
        assertEquals(30, restoredStats?.currentStreak)

        val restoredProgress = database.userProgressDao().getAllForUserOnce(currentUserId)
        assertEquals(1, restoredProgress.size)
        assertEquals("c1_s1_l1", restoredProgress[0].lessonId)
        assertEquals(LessonStatus.COMPLETED, restoredProgress[0].status)
        assertEquals(currentUserId, restoredProgress[0].userId)

        val restoredAttempts = database.exerciseAttemptDao().getAllForUser(currentUserId)
        assertEquals(1, restoredAttempts.size)
        assertEquals("w_1", restoredAttempts[0].itemId)
        assertEquals(currentUserId, restoredAttempts[0].userId)

        val restoredAchievements = database.achievementDao().getAllForUserOnce(currentUserId)
        assertEquals(1, restoredAchievements.size)
        assertEquals("first_step", restoredAchievements[0].achievementId)
        assertEquals(currentUserId, restoredAchievements[0].userId)

        val restoredDailyPractice = database.dailyPracticeDao().getAllForUserOnce(currentUserId)
        assertEquals(1, restoredDailyPractice.size)
        assertEquals("2026-09-23", restoredDailyPractice[0].localDate)
        assertEquals(15, restoredDailyPractice[0].minutesPracticed)
        assertEquals(currentUserId, restoredDailyPractice[0].userId)

        // Verify foreignUserId has no leftover rows
        assertTrue(database.userProgressDao().getAllForUserOnce(foreignUserId).isEmpty())
        assertTrue(database.exerciseAttemptDao().getAllForUser(foreignUserId).isEmpty())
    }
}
