package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.domain.model.ChapterWithSections

/**
 * The single definition of "Qur'an coverage %" - Home, Progress (and its coverage achievements),
 * the lesson summary and the chapter/section intros all read it from here so they always agree.
 *
 * Based on completed lessons and the content's precomputed occurrence percents: each section
 * contributes `(completed lessons / lessons) * section.quranOccurrencePercent`. Content built
 * without section percents falls back to whole chapters, credited once their chapter exam is
 * completed. A chapter's total is capped at its own percent.
 */
object CoverageCalculator {

    /** chapterId -> covered percent of the whole Qur'an within that chapter. */
    fun coverageByChapter(
        chapters: List<ChapterEntity>,
        sections: List<SectionEntity>,
        lessons: List<LessonEntity>,
        completedLessonIds: Set<String>
    ): Map<String, Double> {
        val useSections = sections.any { it.quranOccurrencePercent > 0.0 }
        val lessonsBySection = lessons.filter { it.sectionId != null }.groupBy { it.sectionId!! }
        val sectionsByChapter = sections.groupBy { it.chapterId }
        val completedExamChapters = lessons
            .filter { it.kind == LessonKind.CHAPTER_EXAM && it.id in completedLessonIds }
            .map { it.chapterId }
            .toSet()

        return chapters.associate { chapter ->
            val covered = if (useSections) {
                sectionsByChapter[chapter.id].orEmpty().sumOf { section ->
                    sectionCoverage(section, lessonsBySection[section.id].orEmpty(), completedLessonIds)
                }
            } else {
                if (chapter.id in completedExamChapters) chapter.quranOccurrencePercent else 0.0
            }
            val cap = chapter.quranOccurrencePercent
            chapter.id to if (cap > 0.0) covered.coerceIn(0.0, cap) else covered.coerceAtLeast(0.0)
        }
    }

    /** Total covered percent across the curriculum. */
    fun totalCoverage(
        chapters: List<ChapterEntity>,
        sections: List<SectionEntity>,
        lessons: List<LessonEntity>,
        completedLessonIds: Set<String>
    ): Double = coverageByChapter(chapters, sections, lessons, completedLessonIds).values.sum()

    /** Covered percent within one section. */
    fun sectionCoverage(section: SectionEntity, sectionLessons: List<LessonEntity>, completedLessonIds: Set<String>): Double {
        if (sectionLessons.isEmpty()) return 0.0
        val completed = sectionLessons.count { it.id in completedLessonIds }
        return (completed.toDouble() / sectionLessons.size) * section.quranOccurrencePercent
    }

    fun coverageByChapter(tree: List<ChapterWithSections>, completedLessonIds: Set<String>): Map<String, Double> =
        coverageByChapter(
            chapters = tree.map { it.chapter },
            sections = tree.flatMap { c -> c.sections.map { it.section } },
            lessons = tree.flatMap { c -> c.sections.flatMap { it.lessons } + c.chapterLevelLessons },
            completedLessonIds = completedLessonIds
        )
}
