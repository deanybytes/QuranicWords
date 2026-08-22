package com.quranicwords.app.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.WordFrequencyEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.util.StreakCalculator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    private val clock = Clock.fixed(Instant.parse("2026-08-22T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProgressRepositoryImpl(database, StreakCalculator(clock), clock)
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

        val result = repository.completeLesson(userId, "section_exam", correctCount = 8, totalCount = 10, durationMillis = 60_000L)

        assertEquals(80, result.accuracyPercent)
        assertEquals(LessonKind.SECTION_EXAM, result.lessonKind)
        assertEquals("l2", result.nextLessonId)
        assertEquals(LessonStatus.UNLOCKED, database.userProgressDao().get(userId, "l2")?.status)
    }

    @Test
    fun `section exam just under the threshold does not unlock the next lesson`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "section_exam", correctCount = 7, totalCount = 10, durationMillis = 60_000L)

        assertEquals(70, result.accuracyPercent)
        assertNull(result.nextLessonId)
        assertNull(database.userProgressDao().get(userId, "l2"))
    }

    @Test
    fun `chapter exam at the threshold unlocks what comes next`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "chapter_1_exam", correctCount = 9, totalCount = 10, durationMillis = 60_000L)

        assertEquals(LessonKind.CHAPTER_EXAM, result.lessonKind)
        // Chapter 1 has no CHAPTER_FLASHBACK row (nothing earlier to flash back to - see
        // LessonKind's doc comment), so this falls through straight to chapter 2's first lesson.
        assertEquals("chapter_2_l1", result.nextLessonId)
    }

    @Test
    fun `chapter exam under the threshold does not unlock what comes next`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "chapter_1_exam", correctCount = 1, totalCount = 10, durationMillis = 60_000L)

        assertEquals(10, result.accuracyPercent)
        assertNull(result.nextLessonId)
    }

    @Test
    fun `a REGULAR lesson at a low score still unlocks the next lesson`() = runTest {
        seedTree()

        val result = repository.completeLesson(userId, "l1", correctCount = 1, totalCount = 10, durationMillis = 60_000L)

        assertEquals(LessonKind.REGULAR, result.lessonKind)
        assertEquals(10, result.accuracyPercent)
        assertEquals("section_exam", result.nextLessonId)
        assertEquals(LessonStatus.UNLOCKED, database.userProgressDao().get(userId, "section_exam")?.status)
    }

    @Test
    fun `bestScorePercent takes the max across repeated attempts`() = runTest {
        seedTree()

        repository.completeLesson(userId, "l1", correctCount = 3, totalCount = 10, durationMillis = 60_000L)
        repository.completeLesson(userId, "l1", correctCount = 1, totalCount = 10, durationMillis = 60_000L)

        assertEquals(30, database.userProgressDao().get(userId, "l1")?.bestScorePercent)
    }

    @Test
    fun `completeReviewSession reports a null lessonKind`() = runTest {
        seedTree()

        val result = repository.completeReviewSession(userId, correctCount = 5, totalCount = 5, durationMillis = 60_000L)

        assertNull(result.lessonKind)
        assertNull(result.nextLessonId)
    }

    @Test
    fun `completeLesson persists durationMillis on the progress row`() = runTest {
        seedTree()

        repository.completeLesson(userId, "l1", correctCount = 5, totalCount = 10, durationMillis = 125_000L)

        assertEquals(125_000L, database.userProgressDao().get(userId, "l1")?.durationMillis)
    }

    @Test
    fun `completeLesson rounds a session's duration up into today's daily practice minutes`() = runTest {
        seedTree()

        // 90 seconds should round up to 2 minutes, not truncate to 1.
        repository.completeLesson(userId, "l1", correctCount = 5, totalCount = 10, durationMillis = 90_000L)

        val today = java.time.LocalDate.now(clock).toString()
        assertEquals(2, database.dailyPracticeDao().get(userId, today)?.minutesPracticed)
    }

    @Test
    fun `daily practice minutes accumulate across multiple sessions the same day`() = runTest {
        seedTree()

        repository.completeLesson(userId, "l1", correctCount = 5, totalCount = 10, durationMillis = 60_000L)
        repository.completeReviewSession(userId, correctCount = 5, totalCount = 5, durationMillis = 60_000L)

        val today = java.time.LocalDate.now(clock).toString()
        assertEquals(2, database.dailyPracticeDao().get(userId, today)?.minutesPracticed)
    }

    @Test
    fun `a zero-duration session does not create a daily practice row`() = runTest {
        seedTree()

        repository.completeLesson(userId, "l1", correctCount = 5, totalCount = 10, durationMillis = 0L)

        val today = java.time.LocalDate.now(clock).toString()
        assertNull(database.dailyPracticeDao().get(userId, today))
    }

    @Test
    fun `completeReviewSession defaults to REVIEW sessionType`() = runTest {
        seedTree()

        val result = repository.completeReviewSession(userId, correctCount = 5, totalCount = 5, durationMillis = 60_000L)

        assertEquals(LessonSessionType.REVIEW, result.sessionType)
    }

    @Test
    fun `completeReviewSession reports whatever sessionType the caller passes`() = runTest {
        seedTree()

        val result = repository.completeReviewSession(
            userId, correctCount = 5, totalCount = 5, durationMillis = 60_000L, sessionType = LessonSessionType.OPEN_PRACTICE
        )

        assertEquals(LessonSessionType.OPEN_PRACTICE, result.sessionType)
    }

    private suspend fun seedExercise(id: String, practicedItemId: String) {
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity(
                    id = id,
                    lessonId = "l1",
                    orderIndex = 0,
                    type = ExerciseType.MULTIPLE_CHOICE,
                    contentJson = "{}",
                    practicedItemId = practicedItemId
                )
            )
        )
    }

    @Test
    fun `getOpenPracticeExercises draws only from words the user has already practiced`() = runTest {
        seedTree()
        seedExercise("ex_a", "word_a")
        seedExercise("ex_b", "word_b")
        seedExercise("ex_c", "word_c")
        database.exerciseAttemptDao().insert(
            ExerciseAttemptEntity(userId = userId, itemId = "word_a", itemKind = ItemKind.WORD, exerciseType = ExerciseType.MULTIPLE_CHOICE, wasCorrect = true, attemptedAtEpochMillis = 1L)
        )

        val result = repository.getOpenPracticeExercises(userId, batchSize = 18)

        assertEquals(listOf("word_a"), result.map { it.practicedItemId })
    }

    @Test
    fun `getOpenPracticeExercises falls back to the full corpus when nothing has been practiced yet`() = runTest {
        seedTree()
        seedExercise("ex_a", "word_a")
        seedExercise("ex_b", "word_b")
        database.wordFrequencyDao().insertAll(
            listOf(
                WordFrequencyEntity("word_a", "ا", 1, 100, mapOf("en" to "a"), null, 1),
                WordFrequencyEntity("word_b", "ب", 2, 90, mapOf("en" to "b"), null, 1)
            )
        )

        val result = repository.getOpenPracticeExercises(userId, batchSize = 18)

        assertEquals(setOf("word_a", "word_b"), result.map { it.practicedItemId }.toSet())
    }

    @Test
    fun `getOpenPracticeExercises caps at batchSize`() = runTest {
        seedTree()
        (1..25).forEach { seedExercise("ex_$it", "word_$it") }
        (1..25).forEach {
            database.exerciseAttemptDao().insert(
                ExerciseAttemptEntity(userId = userId, itemId = "word_$it", itemKind = ItemKind.WORD, exerciseType = ExerciseType.MULTIPLE_CHOICE, wasCorrect = true, attemptedAtEpochMillis = it.toLong())
            )
        }

        val result = repository.getOpenPracticeExercises(userId, batchSize = 10)

        assertEquals(10, result.size)
        assertTrue(result.map { it.practicedItemId }.toSet().all { it!!.startsWith("word_") })
    }
}
