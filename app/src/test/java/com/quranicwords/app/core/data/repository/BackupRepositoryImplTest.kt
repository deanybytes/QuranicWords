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
}
