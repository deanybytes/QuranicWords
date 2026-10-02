package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.SectionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverageCalculatorTest {

    private fun chapter(id: String, percent: Double, order: Int) =
        ChapterEntity(id, mapOf("en" to id), emptyMap(), sortOrder = order, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = percent)

    private fun section(id: String, chapterId: String, percent: Double) =
        SectionEntity(id, chapterId, mapOf("en" to id), sortOrder = 1, wordCount = 0, quranOccurrenceCount = 0, quranOccurrencePercent = percent)

    private fun lesson(id: String, chapterId: String, sectionId: String?, kind: LessonKind = LessonKind.REGULAR) =
        LessonEntity(id, chapterId, sectionId, mapOf("en" to id), sortOrder = 1, kind = kind)

    @Test
    fun `sections contribute their share of completed lessons`() {
        val chapters = listOf(chapter("c1", 40.0, 1))
        val sections = listOf(section("s1", "c1", 30.0), section("s2", "c1", 10.0))
        val lessons = listOf(lesson("a", "c1", "s1"), lesson("b", "c1", "s1"), lesson("c", "c1", "s2"))

        assertEquals(15.0, CoverageCalculator.totalCoverage(chapters, sections, lessons, setOf("a")), 0.001)
        assertEquals(40.0, CoverageCalculator.totalCoverage(chapters, sections, lessons, setOf("a", "b", "c")), 0.001)
    }

    @Test
    fun `content without section percents credits chapters by their exam`() {
        val chapters = listOf(chapter("c1", 25.0, 1), chapter("c2", 20.0, 2))
        val sections = listOf(section("s1", "c1", 0.0), section("s2", "c2", 0.0))
        val lessons = listOf(
            lesson("a", "c1", "s1"),
            lesson("c1_exam", "c1", null, LessonKind.CHAPTER_EXAM),
            lesson("c2_exam", "c2", null, LessonKind.CHAPTER_EXAM)
        )

        val byChapter = CoverageCalculator.coverageByChapter(chapters, sections, lessons, setOf("a", "c1_exam"))

        assertEquals(25.0, byChapter["c1"]!!, 0.001)
        assertEquals(0.0, byChapter["c2"]!!, 0.001)
    }

    @Test
    fun `a chapter's coverage never exceeds its own percent`() {
        val chapters = listOf(chapter("c1", 10.0, 1))
        val sections = listOf(section("s1", "c1", 30.0)) // inconsistent content
        val lessons = listOf(lesson("a", "c1", "s1"))

        assertEquals(10.0, CoverageCalculator.totalCoverage(chapters, sections, lessons, setOf("a")), 0.001)
    }
}
