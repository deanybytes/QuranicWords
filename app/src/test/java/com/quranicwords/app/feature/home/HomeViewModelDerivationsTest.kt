package com.quranicwords.app.feature.home

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-logic coverage for [findCurrentPosition]/[aggregateStatus], the collapse/expand tree's
 * (QW-22) "where is the learner right now" and "what color should this summary node be"
 * derivations - fixture mirrors [com.quranicwords.app.core.domain.CurriculumUnlockResolverTest]'s
 * shape.
 */
class HomeViewModelDerivationsTest {

    private val chapters = listOf(
        ChapterWithSections(
            chapter = ChapterEntity("chapter_1", mapOf("en" to "C1"), emptyMap(), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
            sections = listOf(
                SectionWithLessons(
                    section = SectionEntity("section_1_1", "chapter_1", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
                    lessons = listOf(
                        LessonEntity("l1", "chapter_1", "section_1_1", mapOf("en" to "L1"), sortOrder = 1, kind = LessonKind.REGULAR),
                        LessonEntity("l2", "chapter_1", "section_1_1", mapOf("en" to "L2"), sortOrder = 2, kind = LessonKind.REGULAR)
                    )
                )
            ),
            chapterLevelLessons = listOf(
                LessonEntity("chapter_1_exam", "chapter_1", null, mapOf("en" to "CE"), sortOrder = 1, kind = LessonKind.CHAPTER_EXAM)
            )
        ),
        ChapterWithSections(
            chapter = ChapterEntity("chapter_2", mapOf("en" to "C2"), emptyMap(), sortOrder = 2, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
            sections = listOf(
                SectionWithLessons(
                    section = SectionEntity("section_2_1", "chapter_2", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
                    lessons = listOf(LessonEntity("l3", "chapter_2", "section_2_1", mapOf("en" to "L3"), sortOrder = 1, kind = LessonKind.REGULAR))
                )
            )
        )
    )

    private fun progress(vararg pairs: Pair<String, LessonStatus>): Map<String, UserProgressEntity> =
        pairs.associate { (id, status) -> id to UserProgressEntity(userId = "u", lessonId = id, status = status, bestScorePercent = 0, completedAtEpochMillis = null) }

    @Test
    fun `no progress at all resolves to no current position`() {
        val (chapterId, sectionId) = findCurrentPosition(chapters, emptyMap())
        assertNull(chapterId)
        assertNull(sectionId)
    }

    @Test
    fun `first unlocked lesson determines the current chapter and section`() {
        val (chapterId, sectionId) = findCurrentPosition(chapters, progress("l1" to LessonStatus.UNLOCKED))
        assertEquals("chapter_1", chapterId)
        assertEquals("section_1_1", sectionId)
    }

    @Test
    fun `an unlocked chapter-level lesson resolves to that chapter with a null section`() {
        val (chapterId, sectionId) = findCurrentPosition(
            chapters,
            progress("l1" to LessonStatus.COMPLETED, "l2" to LessonStatus.COMPLETED, "chapter_1_exam" to LessonStatus.UNLOCKED)
        )
        assertEquals("chapter_1", chapterId)
        assertNull(sectionId)
    }

    @Test
    fun `chapter 1 fully complete moves current position into chapter 2`() {
        val (chapterId, sectionId) = findCurrentPosition(
            chapters,
            progress(
                "l1" to LessonStatus.COMPLETED,
                "l2" to LessonStatus.COMPLETED,
                "chapter_1_exam" to LessonStatus.COMPLETED,
                "l3" to LessonStatus.UNLOCKED
            )
        )
        assertEquals("chapter_2", chapterId)
        assertEquals("section_2_1", sectionId)
    }

    @Test
    fun `uncompleted preceding chapter intro does not block active lesson advancement`() {
        val p = progress(
            "l1" to LessonStatus.UNLOCKED,
            "l2" to LessonStatus.COMPLETED,
            "chapter_1_exam" to LessonStatus.UNLOCKED
        )
        val (chapterId, sectionId) = findCurrentPosition(chapters, p)
        assertEquals("chapter_1", chapterId)
        assertNull(sectionId)
        assertEquals("chapter_1_exam", findCurrentLessonId(chapters, p))
    }

    @Test
    fun `reviewing an earlier lesson does not drag current position backwards`() {
        // Learner has completed l1 and l2, and chapter_1_exam is UNLOCKED.
        // Even if l1 is completed again for review, current lesson remains chapter_1_exam.
        val p = progress(
            "l1" to LessonStatus.COMPLETED,
            "l2" to LessonStatus.COMPLETED,
            "chapter_1_exam" to LessonStatus.UNLOCKED
        )
        assertEquals("chapter_1_exam", findCurrentLessonId(chapters, p))
    }

    @Test
    fun `all curriculum lessons completed resolves current position and id to null`() {
        val p = progress(
            "l1" to LessonStatus.COMPLETED,
            "l2" to LessonStatus.COMPLETED,
            "chapter_1_exam" to LessonStatus.COMPLETED,
            "l3" to LessonStatus.COMPLETED
        )
        assertNull(findCurrentLessonId(chapters, p))
        val (chapterId, sectionId) = findCurrentPosition(chapters, p)
        assertNull(chapterId)
        assertNull(sectionId)
    }

    @Test
    fun `aggregateStatus is LOCKED when no lesson has been reached`() {
        assertEquals(LessonStatus.LOCKED, aggregateStatus(listOf("l1", "l2"), emptyMap()))
    }

    @Test
    fun `aggregateStatus is UNLOCKED when at least one lesson has been reached but not all completed`() {
        val result = aggregateStatus(listOf("l1", "l2"), progress("l1" to LessonStatus.COMPLETED, "l2" to LessonStatus.UNLOCKED))
        assertEquals(LessonStatus.UNLOCKED, result)
    }

    @Test
    fun `aggregateStatus is COMPLETED only when every lesson is completed`() {
        val result = aggregateStatus(listOf("l1", "l2"), progress("l1" to LessonStatus.COMPLETED, "l2" to LessonStatus.COMPLETED))
        assertEquals(LessonStatus.COMPLETED, result)
    }

    @Test
    fun `aggregateStatus of an empty lesson list is LOCKED, not a crash`() {
        assertEquals(LessonStatus.LOCKED, aggregateStatus(emptyList(), emptyMap()))
    }

    // --- completedLessonsHistory ---

    private fun completedProgress(lessonId: String, completedAtEpochMillis: Long, bestScorePercent: Int = 100) =
        UserProgressEntity(
            userId = "u",
            lessonId = lessonId,
            status = LessonStatus.COMPLETED,
            bestScorePercent = bestScorePercent,
            completedAtEpochMillis = completedAtEpochMillis
        )

    @Test
    fun `completedLessonsHistory orders completed lessons most-recent-first`() {
        val progressByLessonId = mapOf(
            "l1" to completedProgress("l1", completedAtEpochMillis = 1000),
            "l2" to completedProgress("l2", completedAtEpochMillis = 3000),
            "chapter_1_exam" to completedProgress("chapter_1_exam", completedAtEpochMillis = 2000)
        )

        val result = completedLessonsHistory(chapters, progressByLessonId)

        assertEquals(listOf("l2", "chapter_1_exam", "l1"), result.map { it.first.id })
    }

    @Test
    fun `completedLessonsHistory excludes lessons that are unlocked but not completed`() {
        val progressByLessonId = mapOf(
            "l1" to completedProgress("l1", completedAtEpochMillis = 1000),
            "l2" to UserProgressEntity("u", "l2", LessonStatus.UNLOCKED, 0, completedAtEpochMillis = null)
        )

        val result = completedLessonsHistory(chapters, progressByLessonId)

        assertEquals(listOf("l1"), result.map { it.first.id })
    }

    @Test
    fun `completedLessonsHistory is empty when nothing has been completed`() {
        assertEquals(emptyList<Any>(), completedLessonsHistory(chapters, emptyMap()))
    }

    // --- stepHistoryIndex ---

    @Test
    fun `stepHistoryIndex advances within bounds`() {
        assertEquals(1, stepHistoryIndex(currentIndex = 0, size = 3, delta = 1))
    }

    @Test
    fun `stepHistoryIndex clamps at the last index instead of wrapping`() {
        assertEquals(2, stepHistoryIndex(currentIndex = 2, size = 3, delta = 1))
    }

    @Test
    fun `stepHistoryIndex clamps at zero instead of going negative`() {
        assertEquals(0, stepHistoryIndex(currentIndex = 0, size = 3, delta = -1))
    }

    @Test
    fun `stepHistoryIndex on an empty history is always zero`() {
        assertEquals(0, stepHistoryIndex(currentIndex = 0, size = 0, delta = 1))
    }

    // --- isCurriculumComplete ---

    @Test
    fun `isCurriculumComplete is true when every lesson across every chapter is completed`() {
        val allComplete = progress(
            "l1" to LessonStatus.COMPLETED,
            "l2" to LessonStatus.COMPLETED,
            "chapter_1_exam" to LessonStatus.COMPLETED,
            "l3" to LessonStatus.COMPLETED
        )
        assertEquals(true, isCurriculumComplete(chapters, allComplete))
    }

    @Test
    fun `isCurriculumComplete is false when one lesson is still unlocked`() {
        val almostComplete = progress(
            "l1" to LessonStatus.COMPLETED,
            "l2" to LessonStatus.COMPLETED,
            "chapter_1_exam" to LessonStatus.COMPLETED,
            "l3" to LessonStatus.UNLOCKED
        )
        assertEquals(false, isCurriculumComplete(chapters, almostComplete))
    }

    @Test
    fun `isCurriculumComplete is false with no progress at all`() {
        assertEquals(false, isCurriculumComplete(chapters, emptyMap()))
    }

    @Test
    fun `isCurriculumComplete is false for an empty curriculum tree, not vacuously true`() {
        assertEquals(false, isCurriculumComplete(emptyList(), emptyMap()))
    }

    // --- userCoveragePercentByChapter & calculateTotalUserCoveragePercent ---

    @Test
    fun `userCoveragePercentByChapter is zero when no lessons are completed`() {
        val customChapters = listOf(
            ChapterWithSections(
                chapter = ChapterEntity("c1", emptyMap(), emptyMap(), 1, 0, 0, 80.0),
                sections = listOf(
                    SectionWithLessons(
                        section = SectionEntity("s1", "c1", emptyMap(), 1, 0, 0, 80.0),
                        lessons = listOf(
                            LessonEntity("l1", "c1", "s1", emptyMap(), 1, LessonKind.REGULAR),
                            LessonEntity("l2", "c1", "s1", emptyMap(), 2, LessonKind.REGULAR)
                        )
                    )
                )
            )
        )
        val coverage = userCoveragePercentByChapter(customChapters, emptyMap())
        assertEquals(0.0, coverage["c1"] ?: -1.0, 0.001)
        assertEquals(0.0, calculateTotalUserCoveragePercent(coverage), 0.001)
    }

    @Test
    fun `userCoveragePercentByChapter calculates proportional real coverage based on completed lessons`() {
        val customChapters = listOf(
            ChapterWithSections(
                chapter = ChapterEntity("c1", emptyMap(), emptyMap(), 1, 0, 0, 80.0),
                sections = listOf(
                    SectionWithLessons(
                        section = SectionEntity("s1", "c1", emptyMap(), 1, 0, 0, 50.0),
                        lessons = listOf(
                            LessonEntity("l1", "c1", "s1", emptyMap(), 1, LessonKind.REGULAR),
                            LessonEntity("l2", "c1", "s1", emptyMap(), 2, LessonKind.REGULAR)
                        )
                    ),
                    SectionWithLessons(
                        section = SectionEntity("s2", "c1", emptyMap(), 2, 0, 0, 30.0),
                        lessons = listOf(
                            LessonEntity("l3", "c1", "s2", emptyMap(), 1, LessonKind.REGULAR)
                        )
                    )
                )
            )
        )
        // 1 of 2 lessons completed in s1 (25.0%), s2 not completed (0%) -> c1 coverage = 25.0%
        val p1 = progress("l1" to LessonStatus.COMPLETED)
        val coverage1 = userCoveragePercentByChapter(customChapters, p1)
        assertEquals(25.0, coverage1["c1"] ?: -1.0, 0.001)
        assertEquals(25.0, calculateTotalUserCoveragePercent(coverage1), 0.001)

        // all lessons completed in s1 (50.0%) and s2 (30.0%) -> c1 coverage = 80.0%
        val p2 = progress("l1" to LessonStatus.COMPLETED, "l2" to LessonStatus.COMPLETED, "l3" to LessonStatus.COMPLETED)
        val coverage2 = userCoveragePercentByChapter(customChapters, p2)
        assertEquals(80.0, coverage2["c1"] ?: -1.0, 0.001)
        assertEquals(80.0, calculateTotalUserCoveragePercent(coverage2), 0.001)
    }

    @Test
    fun `findHomeTargetItemIndex locates item when chapter and section are expanded`() {
        // Chapters layout:
        // No top cards (currentIndex = 0)
        // chapter_1 summary -> index 0
        // section_1_1 summary -> index 1
        // l1 -> index 2
        // l2 -> index 3
        // chapter_1_exam -> index 4
        // chapter_2 summary -> index 5
        val index = findHomeTargetItemIndex(
            chapters = chapters,
            isCurriculumComplete = false,
            hasReviewableItems = false,
            hasCompletedHistory = false,
            expandedChapterIds = setOf("chapter_1"),
            expandedSectionIds = setOf("section_1_1"),
            targetLessonId = "l2"
        )
        assertEquals(3, index)
    }

    @Test
    fun `findHomeTargetItemIndex locates section node when section is collapsed`() {
        // chapter_1 summary -> index 0
        // section_1_1 summary -> index 1 (collapsed, l1 and l2 not rendered)
        // chapter_1_exam -> index 2
        val index = findHomeTargetItemIndex(
            chapters = chapters,
            isCurriculumComplete = false,
            hasReviewableItems = false,
            hasCompletedHistory = false,
            expandedChapterIds = setOf("chapter_1"),
            expandedSectionIds = emptySet(),
            targetLessonId = "l2"
        )
        assertEquals(1, index)
    }

    @Test
    fun `findHomeTargetItemIndex locates chapter node when chapter is collapsed`() {
        // history_card -> index 0
        // chapter_1 summary -> index 1 (collapsed)
        // chapter_2 summary -> index 2 (collapsed)
        val index = findHomeTargetItemIndex(
            chapters = chapters,
            isCurriculumComplete = false,
            hasReviewableItems = false,
            hasCompletedHistory = true,
            expandedChapterIds = emptySet(),
            expandedSectionIds = emptySet(),
            targetLessonId = "l3"
        )
        assertEquals(2, index)
    }

    @Test
    fun `findHomeTargetItemIndex returns null when target not found or null`() {
        val nullIndex = findHomeTargetItemIndex(
            chapters = chapters,
            isCurriculumComplete = false,
            hasReviewableItems = false,
            hasCompletedHistory = false,
            expandedChapterIds = setOf("chapter_1"),
            expandedSectionIds = setOf("section_1_1"),
            targetLessonId = null
        )
        assertNull(nullIndex)

        val notFoundIndex = findHomeTargetItemIndex(
            chapters = chapters,
            isCurriculumComplete = false,
            hasReviewableItems = false,
            hasCompletedHistory = false,
            expandedChapterIds = setOf("chapter_1"),
            expandedSectionIds = setOf("section_1_1"),
            targetLessonId = "non_existent"
        )
        assertNull(notFoundIndex)
    }
}

