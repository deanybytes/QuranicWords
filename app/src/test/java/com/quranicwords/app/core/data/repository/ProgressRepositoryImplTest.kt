package com.quranicwords.app.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.data.local.QwDatabase
import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserStatsEntity
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
    private lateinit var preferences: UserPreferencesDataStore
    private lateinit var repository: ProgressRepositoryImpl
    private val userId = "test_user"
    private val clock = Clock.fixed(Instant.parse("2026-08-22T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), QwDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferencesDataStore(ApplicationProvider.getApplicationContext())
        repository = ProgressRepositoryImpl(
            database,
            StreakCalculator(clock),
            clock,
            preferences,
            ApplicationProvider.getApplicationContext()
        )
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
    fun `getOpenPracticeExercises in MISTAKES mode draws only from mistaken words`() = runTest {
        seedTree()
        seedExercise("ex_a", "word_a")
        seedExercise("ex_b", "word_b")
        seedExercise("ex_c", "word_c")
        database.wordFrequencyDao().insertAll(
            listOf(
                WordFrequencyEntity("word_a", "ا", 1, 100, mapOf("en" to "a"), null, 1),
                WordFrequencyEntity("word_b", "ب", 2, 90, mapOf("en" to "b"), null, 1),
                WordFrequencyEntity("word_c", "ت", 3, 80, mapOf("en" to "c"), null, 1)
            )
        )
        database.exerciseAttemptDao().insert(
            ExerciseAttemptEntity(userId = userId, itemId = "word_a", itemKind = ItemKind.WORD, exerciseType = ExerciseType.MULTIPLE_CHOICE, wasCorrect = false, attemptedAtEpochMillis = 1L)
        )

        val result = repository.getOpenPracticeExercises(userId, mode = "MISTAKES", batchSize = 18)

        assertEquals(listOf("word_a"), result.map { it.practicedItemId })
    }

    @Test
    fun `getOpenPracticeExercises in RANDOM mode falls back cleanly and samples full corpus`() = runTest {
        seedTree()
        seedExercise("ex_a", "word_a")
        seedExercise("ex_b", "word_b")
        database.wordFrequencyDao().insertAll(
            listOf(
                WordFrequencyEntity("word_a", "ا", 1, 100, mapOf("en" to "a"), null, 1),
                WordFrequencyEntity("word_b", "ب", 2, 90, mapOf("en" to "b"), null, 1)
            )
        )

        val result = repository.getOpenPracticeExercises(userId, mode = "RANDOM", batchSize = 18)

        assertEquals(setOf("word_a", "word_b"), result.map { it.practicedItemId }.toSet())
    }

    @Test
    fun `getOpenPracticeExercises caps at batchSize`() = runTest {
        seedTree()
        database.wordFrequencyDao().insertAll(
            (1..25).map { WordFrequencyEntity("word_$it", "w$it", it, 100 - it, mapOf("en" to "m$it"), null, 1) }
        )
        (1..25).forEach { seedExercise("ex_$it", "word_$it") }

        val result = repository.getOpenPracticeExercises(userId, mode = "RANDOM", batchSize = 10)

        assertEquals(10, result.size)
        assertTrue(result.map { it.practicedItemId }.toSet().all { it!!.startsWith("word_") })
    }

    @Test
    fun `getStreakRecoveryExercises never falls back to the full corpus`() = runTest {
        seedTree()
        seedExercise("ex_a", "word_a")
        database.wordFrequencyDao().insertAll(listOf(WordFrequencyEntity("word_a", "ا", 1, 100, mapOf("en" to "a"), null, 1)))
        // No attempt history at all for this user - unlike getOpenPracticeExercises, this must
        // stay empty rather than falling back to word_frequency's full corpus.

        val result = repository.getStreakRecoveryExercises(userId, count = 3)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `attemptStreakRecovery on a passing score restores lastActivityLocalDate without touching currentStreak`() = runTest {
        seedTree()
        database.userStatsDao().upsert(UserStatsEntity(userId, totalPoints = 100, currentStreak = 15, longestStreak = 15, lastActivityLocalDate = "2020-01-01"))

        val passed = repository.attemptStreakRecovery(userId, correctCount = 4, totalCount = 5)

        assertTrue(passed)
        val stats = database.userStatsDao().get(userId)
        assertEquals(15, stats?.currentStreak)
        assertEquals(java.time.LocalDate.now(clock).toString(), stats?.lastActivityLocalDate)
    }

    @Test
    fun `attemptStreakRecovery under the passing threshold leaves stats untouched`() = runTest {
        seedTree()
        database.userStatsDao().upsert(UserStatsEntity(userId, totalPoints = 100, currentStreak = 15, longestStreak = 15, lastActivityLocalDate = "2020-01-01"))

        val passed = repository.attemptStreakRecovery(userId, correctCount = 2, totalCount = 5)

        assertEquals(false, passed)
        val stats = database.userStatsDao().get(userId)
        assertEquals("2020-01-01", stats?.lastActivityLocalDate)
        assertEquals(15, stats?.currentStreak)
    }

    @Test
    fun `observeMissedItemIds tracks mistaken words and removes them once answered correctly`() = runTest {
        assertEquals(emptyList<String>(), repository.getMissedItemIds(userId))

        // User makes a mistake on word_1 and word_2
        repository.logAttempt(userId, "word_1", ItemKind.WORD, ExerciseType.MULTIPLE_CHOICE, wasCorrect = false)
        repository.logAttempt(userId, "word_2", ItemKind.WORD, ExerciseType.TAP_WHAT_YOU_HEAR, wasCorrect = false)

        val missedBefore = repository.getMissedItemIds(userId)
        assertEquals(setOf("word_1", "word_2"), missedBefore.toSet())

        // User corrects word_1 in review
        repository.logAttempt(userId, "word_1", ItemKind.WORD, ExerciseType.MULTIPLE_CHOICE, wasCorrect = true)

        val missedAfter = repository.getMissedItemIds(userId)
        assertEquals(listOf("word_2"), missedAfter)

        // User corrects word_2
        repository.logAttempt(userId, "word_2", ItemKind.WORD, ExerciseType.MULTIPLE_CHOICE, wasCorrect = true)

        val missedFinal = repository.getMissedItemIds(userId)
        assertEquals(emptyList<String>(), missedFinal)
    }

    @Test
    fun `getOpenPracticeExercises in FREQUENCY mode yields sequential items in order`() = runTest {
        seedTree()
        preferences.setTestFrequencyOffset(0)
        database.wordFrequencyDao().insertAll(
            listOf(
                WordFrequencyEntity("wf_1", "w1", 1, 100, mapOf("en" to "m1"), null, 1),
                WordFrequencyEntity("wf_2", "w2", 2, 90, mapOf("en" to "m2"), null, 1),
                WordFrequencyEntity("wf_3", "w3", 3, 80, mapOf("en" to "m3"), null, 1)
            )
        )
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity("ex_1", "l1", 1, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_1"),
                ExerciseEntity("ex_2", "l1", 2, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_2"),
                ExerciseEntity("ex_3", "l1", 3, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_3")
            )
        )

        val batch1 = repository.getOpenPracticeExercises(userId, mode = "FREQUENCY", batchSize = 2)
        assertEquals(2, batch1.size)
        assertEquals("wf_1", batch1[0].practicedItemId)
        assertEquals("wf_2", batch1[1].practicedItemId)

        val batch2 = repository.getOpenPracticeExercises(userId, mode = "FREQUENCY", batchSize = 2)
        assertEquals(1, batch2.size)
        assertEquals("wf_3", batch2[0].practicedItemId)
    }

    @Test
    fun `getOpenPracticeExercises in RANDOM mode covers items without immediate repetition`() = runTest {
        seedTree()
        preferences.resetTestRandomCoveredWordIds()
        database.wordFrequencyDao().insertAll(
            listOf(
                WordFrequencyEntity("wf_1", "w1", 1, 100, mapOf("en" to "m1"), null, 1),
                WordFrequencyEntity("wf_2", "w2", 2, 90, mapOf("en" to "m2"), null, 1),
                WordFrequencyEntity("wf_3", "w3", 3, 80, mapOf("en" to "m3"), null, 1),
                WordFrequencyEntity("wf_4", "w4", 4, 70, mapOf("en" to "m4"), null, 1)
            )
        )
        database.exerciseDao().insertAll(
            listOf(
                ExerciseEntity("ex_1", "l1", 1, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_1"),
                ExerciseEntity("ex_2", "l1", 2, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_2"),
                ExerciseEntity("ex_3", "l1", 3, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_3"),
                ExerciseEntity("ex_4", "l1", 4, ExerciseType.MULTIPLE_CHOICE, "{}", "wf_4")
            )
        )

        val batch1 = repository.getOpenPracticeExercises(userId, mode = "RANDOM", batchSize = 2)
        assertEquals(2, batch1.size)
        val items1 = batch1.mapNotNull { it.practicedItemId }.toSet()

        val batch2 = repository.getOpenPracticeExercises(userId, mode = "RANDOM", batchSize = 2)
        assertEquals(2, batch2.size)
        val items2 = batch2.mapNotNull { it.practicedItemId }.toSet()

        // Batch 1 and Batch 2 must have no overlap since 4 items total and 2 items sampled per batch
        assertTrue(items1.intersect(items2).isEmpty())
    }
}
