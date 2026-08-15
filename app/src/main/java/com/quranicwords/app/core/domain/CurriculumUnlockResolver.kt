package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.SectionEntity

/** Every [LessonKind] except [LessonKind.REGULAR] must be passed at
 * [com.quranicwords.app.core.util.GamificationConfig.PASSING_SCORE_PERCENT] or better to unlock
 * what comes next - see [com.quranicwords.app.core.data.repository.ProgressRepositoryImpl
 * .completeLesson]. */
fun LessonKind.requiresPassingScore(): Boolean = this != LessonKind.REGULAR

/**
 * Pure "what unlocks next" resolver over the whole chapter -> section -> lesson tree - given
 * every lesson/section/chapter as a flat list (the curriculum is small, a few hundred lessons
 * total, so loading it whole is cheap - see `LessonDao.getAll`/`SectionDao.getAll`) and the id of
 * the lesson just completed, returns the id of the next thing to unlock, or `null` if the whole
 * curriculum is finished. Caller ([com.quranicwords.app.core.data.repository
 * .ProgressRepositoryImpl]) is responsible for deciding *whether* to call this at all - a failed
 * exam/flashback shouldn't advance anything, and this resolver has no notion of pass/fail, only
 * of tree structure.
 *
 * Three-tier fallback from the completed lesson: (1) the next lesson by `sortOrder` within the
 * same section; (2) failing that, the first lesson of the next section within the same chapter;
 * (3) failing that (the chapter's last section just finished), the chapter's own
 * [LessonKind.CHAPTER_EXAM]/[LessonKind.CHAPTER_FLASHBACK] row, or - once those are done too -
 * the first lesson of the next chapter.
 */
object CurriculumUnlockResolver {
    fun resolveNextLessonId(
        completedLessonId: String,
        lessons: List<LessonEntity>,
        sections: List<SectionEntity>,
        chapters: List<ChapterEntity>
    ): String? {
        val completed = lessons.find { it.id == completedLessonId } ?: return null
        val sectionId = completed.sectionId

        if (sectionId != null) {
            val nextInSection = lessons
                .filter { it.sectionId == sectionId && it.sortOrder > completed.sortOrder }
                .minByOrNull { it.sortOrder }
            if (nextInSection != null) return nextInSection.id

            val section = sections.find { it.id == sectionId } ?: return null
            val nextSection = sections
                .filter { it.chapterId == section.chapterId && it.sortOrder > section.sortOrder }
                .minByOrNull { it.sortOrder }
            if (nextSection != null) {
                return lessons.filter { it.sectionId == nextSection.id }.minByOrNull { it.sortOrder }?.id
            }

            // Last section in the chapter just finished - move to the chapter-level exam.
            return lessons.find {
                it.chapterId == section.chapterId && it.sectionId == null && it.kind == LessonKind.CHAPTER_EXAM
            }?.id
        }

        // completed is chapter-scoped (CHAPTER_EXAM or CHAPTER_FLASHBACK, sectionId == null).
        if (completed.kind == LessonKind.CHAPTER_EXAM) {
            val flashback = lessons.find {
                it.chapterId == completed.chapterId && it.sectionId == null && it.kind == LessonKind.CHAPTER_FLASHBACK
            }
            if (flashback != null) return flashback.id
            // No flashback for this chapter (chapter 1) - fall through to the next chapter.
        }

        val chapter = chapters.find { it.id == completed.chapterId } ?: return null
        val nextChapter = chapters.filter { it.sortOrder > chapter.sortOrder }.minByOrNull { it.sortOrder }
            ?: return null
        val nextSection = sections.filter { it.chapterId == nextChapter.id }.minByOrNull { it.sortOrder }
            ?: return null
        return lessons.filter { it.sectionId == nextSection.id }.minByOrNull { it.sortOrder }?.id
    }
}
