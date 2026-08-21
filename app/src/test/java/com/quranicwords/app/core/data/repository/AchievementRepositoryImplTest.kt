package com.quranicwords.app.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AchievementRepositoryImplTest {

    private lateinit var database: QwDatabase
    private lateinit var repository: AchievementRepositoryImpl
    private val userId = "test_user"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AchievementRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun seedTree() = runTest {
        database.chapterDao().insertAll(
            listOf(
                ChapterEntity("chapter_1", mapOf("en" to "C1"), emptyMap(), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 30.0),
                ChapterEntity("chapter_2", mapOf("en" to "C2"), emptyMap(), sortOrder = 2, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 20.0)
            )
        )
        database.sectionDao().insertAll(
            listOf(SectionEntity("section_1_1", "chapter_1", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0))
        )
        database.lessonDao().insertAll(
            listOf(
                LessonEntity("l1", "chapter_1", "section_1_1", mapOf("en" to "L1"), sortOrder = 1, kind = LessonKind.REGULAR),
                LessonEntity("section_exam", "chapter_1", "section_1_1", mapOf("en" to "Exam"), sortOrder = 2, kind = LessonKind.SECTION_EXAM),
                LessonEntity("chapter_1_exam", "chapter_1", null, mapOf("en" to "CE"), sortOrder = 1, kind = LessonKind.CHAPTER_EXAM),
                LessonEntity("chapter_2_exam", "chapter_2", null, mapOf("en" to "CE"), sortOrder = 1, kind = LessonKind.CHAPTER_EXAM)
            )
        )
    }

    private suspend fun completeLesson(lessonId: String, scorePercent: Int) {
        database.userProgressDao().upsert(
            UserProgressEntity(userId = userId, lessonId = lessonId, status = LessonStatus.COMPLETED, bestScorePercent = scorePercent, completedAtEpochMillis = 0L)
        )
    }

    @Test
    fun `no achievements unlock with no progress at all`() = runTest {
        seedTree()
        assertTrue(repository.checkAndUnlock(userId).isEmpty())
    }

    @Test
    fun `completing a REGULAR lesson unlocks first_lesson`() = runTest {
        seedTree()
        completeLesson("l1", 30)

        val unlocked = repository.checkAndUnlock(userId)

        assertTrue(unlocked.any { it.id == "first_lesson" })
    }

    @Test
    fun `passing an exam unlocks first_exam_passed but a failed one does not`() = runTest {
        seedTree()
        completeLesson("section_exam", 50) // below the 80% pass threshold

        assertTrue(repository.checkAndUnlock(userId).none { it.id == "first_exam_passed" })

        completeLesson("section_exam", 80)

        assertTrue(repository.checkAndUnlock(userId).any { it.id == "first_exam_passed" })
    }

    @Test
    fun `completing a chapter exam unlocks that chapter's completion achievement`() = runTest {
        seedTree()
        completeLesson("chapter_1_exam", 90)

        val unlocked = repository.checkAndUnlock(userId)

        assertTrue(unlocked.any { it.id == "chapter_1_complete" })
        assertTrue(unlocked.none { it.id == "chapter_2_complete" })
    }

    @Test
    fun `cumulative coverage from completed chapters crosses coverage bands`() = runTest {
        seedTree()
        // chapter_1 alone is 30% coverage - crosses the 25% band, not the 50% one.
        completeLesson("chapter_1_exam", 90)
        val afterChapter1 = repository.checkAndUnlock(userId)
        assertTrue(afterChapter1.any { it.id == "coverage_25" })
        assertTrue(afterChapter1.none { it.id == "coverage_50" })

        // chapter_1 (30%) + chapter_2 (20%) = 50% - now crosses the 50% band too.
        completeLesson("chapter_2_exam", 90)
        val afterChapter2 = repository.checkAndUnlock(userId)
        assertTrue(afterChapter2.any { it.id == "coverage_50" })
    }

    @Test
    fun `streak achievements unlock from UserStatsEntity longestStreak`() = runTest {
        seedTree()
        database.userStatsDao().upsert(
            UserStatsEntity(userId = userId, totalPoints = 100, currentStreak = 7, longestStreak = 7, lastActivityLocalDate = "2026-08-22")
        )

        val unlocked = repository.checkAndUnlock(userId)

        assertTrue(unlocked.any { it.id == "streak_7" })
        assertTrue(unlocked.none { it.id == "streak_30" })
    }

    @Test
    fun `an already-unlocked achievement is never returned again`() = runTest {
        seedTree()
        completeLesson("l1", 30)
        val firstCall = repository.checkAndUnlock(userId)
        assertEquals(1, firstCall.count { it.id == "first_lesson" })

        val secondCall = repository.checkAndUnlock(userId)

        assertTrue(secondCall.none { it.id == "first_lesson" })
    }
}
