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
}
