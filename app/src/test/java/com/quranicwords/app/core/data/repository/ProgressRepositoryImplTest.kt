package com.quranicwords.app.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.util.StreakCalculator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Real Room-backed exam-scoring/gating coverage for [ProgressRepositoryImpl.completeLesson] -
 * the actual gap flagged by QW-10/QW-11 (the exam engine itself already existed via the shared
 * lesson pipeline; this is the dedicated test coverage that was missing). Fixture mirrors
 * [com.quranicwords.app.core.domain.CurriculumUnlockResolverTest]'s tree shape so the two stay
 * easy to cross-reference.
 */
// Robolectric 4.16's newest supported shadow SDK is API 36, one behind this app's targetSdk 37 -
// pin the simulated framework version explicitly rather than letting SDK-picker fail. This only
// affects which Android framework Robolectric shadows during the test run, not real app behavior.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProgressRepositoryImplTest {

    private lateinit var database: QwDatabase
    private lateinit var repository: ProgressRepositoryImpl
    private val userId = "test_user"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val clock = Clock.fixed(Instant.parse("2026-08-22T10:00:00Z"), ZoneOffset.UTC)
        repository = ProgressRepositoryImpl(database, StreakCalculator(clock))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun seedTree() = runTest {
        database.chapterDao().insertAll(
            listOf(
                ChapterEntity("chapter_1", mapOf("en" to "C1"), emptyMap(), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
                ChapterEntity("chapter_2", mapOf("en" to "C2"), emptyMap(), sortOrder = 2, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0)
            )
        )
        database.sectionDao().insertAll(
            listOf(
                SectionEntity("section_1_1", "chapter_1", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
                SectionEntity("section_2_1", "chapter_2", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0)
            )
        )
        database.lessonDao().insertAll(
            listOf(
                LessonEntity("l1", "chapter_1", "section_1_1", mapOf("en" to "L1"), sortOrder = 1, kind = LessonKind.REGULAR),
                LessonEntity("section_exam", "chapter_1", "section_1_1", mapOf("en" to "Exam"), sortOrder = 2, kind = LessonKind.SECTION_EXAM),
                LessonEntity("l2", "chapter_1", "section_1_1", mapOf("en" to "L2"), sortOrder = 3, kind = LessonKind.REGULAR),
                LessonEntity("chapter_1_exam", "chapter_1", null, mapOf("en" to "CE"), sortOrder = 1, kind = LessonKind.CHAPTER_EXAM),
                LessonEntity("chapter_2_l1", "chapter_2", "section_2_1", mapOf("en" to "L1"), sortOrder = 1, kind = LessonKind.REGULAR)
            )
        )
    }

    @Test
    fun `section exam at exactly the 80 percent threshold unlocks the next lesson`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "section_exam", correctCount = 8, totalCount = 10)

        assertEquals(80, result.accuracyPercent)
        assertEquals(LessonKind.SECTION_EXAM, result.lessonKind)
        assertEquals("l2", result.nextLessonId)
        assertEquals(LessonStatus.UNLOCKED, database.userProgressDao().get(userId, "l2")?.status)
    }

    @Test
    fun `section exam just under the threshold does not unlock the next lesson`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "section_exam", correctCount = 7, totalCount = 10)

        assertEquals(70, result.accuracyPercent)
        assertNull(result.nextLessonId)
        assertNull(database.userProgressDao().get(userId, "l2"))
    }

    @Test
    fun `chapter exam at the threshold unlocks what comes next`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "chapter_1_exam", correctCount = 9, totalCount = 10)

        assertEquals(LessonKind.CHAPTER_EXAM, result.lessonKind)
        // Chapter 1 has no CHAPTER_FLASHBACK row (nothing earlier to flash back to - see
        // LessonKind's doc comment), so this falls through straight to chapter 2's first lesson.
        assertEquals("chapter_2_l1", result.nextLessonId)
    }

    @Test
    fun `chapter exam under the threshold does not unlock what comes next`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "chapter_1_exam", correctCount = 1, totalCount = 10)

        assertEquals(10, result.accuracyPercent)
        assertNull(result.nextLessonId)
    }

    @Test
    fun `a REGULAR lesson at a low score still unlocks the next lesson`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "l1", correctCount = 1, totalCount = 10)

        assertEquals(LessonKind.REGULAR, result.lessonKind)
        assertEquals(10, result.accuracyPercent)
        assertEquals("section_exam", result.nextLessonId)
        assertEquals(LessonStatus.UNLOCKED, database.userProgressDao().get(userId, "section_exam")?.status)
    }

    @Test
    fun `bestScorePercent takes the max across repeated attempts`() = runTest {
        seedTree()

        repository.completeLesson(userId, "l1", correctCount = 3, totalCount = 10)
        repository.completeLesson(userId, "l1", correctCount = 1, totalCount = 10)

        assertEquals(30, database.userProgressDao().get(userId, "l1")?.bestScorePercent)
    }

    @Test
    fun `completeReviewSession reports a null lessonKind`() = runTest {
        seedTree()

        val result = repository.completeReviewSession(userId, correctCount = 5, totalCount = 5)

        assertNull(result.lessonKind)
        assertNull(result.nextLessonId)
    }
}
