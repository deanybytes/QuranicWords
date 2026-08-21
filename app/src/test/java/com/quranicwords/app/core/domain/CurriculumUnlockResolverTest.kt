package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.SectionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fixture: 2 chapters x 2 sections x 2 regular lessons, matching the real shape
 * `tools/ingestion/11_build_curriculum.py` emits (lesson flashback after lesson 2, section exam,
 * section flashback for every section but the first in its chapter, chapter exam, chapter
 * flashback for every chapter but the first).
 *
 *   chapter_1 (sortOrder 1)
 *     section_1_1 (sortOrder 1): l1, l2, flashback_after_2, exam
 *     section_1_2 (sortOrder 2): l1, l2, flashback_after_2, exam, section_flashback
 *     chapter_1_exam                                            (no chapter_1_flashback - it's chapter 1)
 *   chapter_2 (sortOrder 2)
 *     section_2_1 (sortOrder 1): l1, l2, flashback_after_2, exam
 *     section_2_2 (sortOrder 2): l1, l2, flashback_after_2, exam, section_flashback
 *     chapter_2_exam, chapter_2_flashback
 */
class CurriculumUnlockResolverTest {

    private val chapters = listOf(
        ChapterEntity("chapter_1", mapOf("en" to "C1"), emptyMap(), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
        ChapterEntity("chapter_2", mapOf("en" to "C2"), emptyMap(), sortOrder = 2, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0)
    )

    private val sections = listOf(
        SectionEntity("section_1_1", "chapter_1", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
        SectionEntity("section_1_2", "chapter_1", mapOf("en" to "S2"), sortOrder = 2, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
        SectionEntity("section_2_1", "chapter_2", mapOf("en" to "S1"), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0),
        SectionEntity("section_2_2", "chapter_2", mapOf("en" to "S2"), sortOrder = 2, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = 0.0)
    )

    private fun sectionLessons(sectionId: String, chapterId: String, withSectionFlashback: Boolean) = buildList {
        add(LessonEntity("${sectionId}_l1", chapterId, sectionId, mapOf("en" to "L1"), sortOrder = 1, kind = LessonKind.REGULAR))
        add(LessonEntity("${sectionId}_l2", chapterId, sectionId, mapOf("en" to "L2"), sortOrder = 2, kind = LessonKind.REGULAR))
        add(LessonEntity("${sectionId}_flashback", chapterId, sectionId, mapOf("en" to "F"), sortOrder = 3, kind = LessonKind.LESSON_FLASHBACK))
        add(LessonEntity("${sectionId}_exam", chapterId, sectionId, mapOf("en" to "Exam"), sortOrder = 4, kind = LessonKind.SECTION_EXAM))
        if (withSectionFlashback) {
            add(LessonEntity("${sectionId}_secflash", chapterId, sectionId, mapOf("en" to "SF"), sortOrder = 5, kind = LessonKind.SECTION_FLASHBACK))
        }
    }

    private val lessons = sectionLessons("section_1_1", "chapter_1", withSectionFlashback = false) +
        sectionLessons("section_1_2", "chapter_1", withSectionFlashback = true) +
        sectionLessons("section_2_1", "chapter_2", withSectionFlashback = false) +
        sectionLessons("section_2_2", "chapter_2", withSectionFlashback = true) +
        listOf(
            LessonEntity("chapter_1_exam", "chapter_1", null, mapOf("en" to "CE"), sortOrder = 1, kind = LessonKind.CHAPTER_EXAM),
            LessonEntity("chapter_2_exam", "chapter_2", null, mapOf("en" to "CE"), sortOrder = 1, kind = LessonKind.CHAPTER_EXAM),
            LessonEntity("chapter_2_flashback", "chapter_2", null, mapOf("en" to "CF"), sortOrder = 2, kind = LessonKind.CHAPTER_FLASHBACK)
        )

    private fun resolve(completedId: String): String? =
        CurriculumUnlockResolver.resolveNextLessonId(completedId, lessons, sections, chapters)

    @Test
    fun `next lesson within a section follows sortOrder`() {
        assertEquals("section_1_1_l2", resolve("section_1_1_l1"))
        assertEquals("section_1_1_flashback", resolve("section_1_1_l2"))
        assertEquals("section_1_1_exam", resolve("section_1_1_flashback"))
    }

    @Test
    fun `finishing a section with no trailing flashback jumps to the next section's first lesson`() {
        // section_1_1 is the first section in chapter_1, so it has no SECTION_FLASHBACK row -
        // its exam is the section's last item.
        assertEquals("section_1_2_l1", resolve("section_1_1_exam"))
    }

    @Test
    fun `finishing a section's own flashback jumps to the next section's first lesson`() {
        assertEquals("section_2_2_l1", resolve("section_2_1_exam"))
    }

    @Test
    fun `finishing the last section in a chapter moves to the chapter exam`() {
        assertEquals("chapter_1_exam", resolve("section_1_2_secflash"))
        assertEquals("chapter_2_exam", resolve("section_2_2_secflash"))
    }

    @Test
    fun `chapter exam with no chapter flashback (chapter 1) falls through to the next chapter`() {
        assertEquals("section_2_1_l1", resolve("chapter_1_exam"))
    }

    @Test
    fun `chapter exam with a chapter flashback unlocks the flashback, not the next chapter yet`() {
        assertEquals("chapter_2_flashback", resolve("chapter_2_exam"))
    }

    @Test
    fun `finishing the last chapter's flashback ends the curriculum`() {
        assertNull(resolve("chapter_2_flashback"))
    }

    @Test
    fun `unknown lesson id resolves to null`() {
        assertNull(resolve("does_not_exist"))
    }

    @Test
    fun `only REGULAR lessons skip the passing-score requirement`() {
        LessonKind.entries.forEach { kind ->
            assertEquals(kind != LessonKind.REGULAR, kind.requiresPassingScore())
        }
    }
}
