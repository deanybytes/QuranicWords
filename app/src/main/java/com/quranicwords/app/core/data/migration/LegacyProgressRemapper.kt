package com.quranicwords.app.core.data.migration

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.SectionEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.util.GamificationConfig
import kotlinx.serialization.Serializable

/** One shipped content version's old-id -> new-id map, emitted by tools/pipeline (legacy.py). */
@Serializable
data class LegacyContentMap(
    val words: Map<String, String> = emptyMap(),
    /** Old lesson id -> the *new* word ids that lesson taught. */
    val lessonWords: Map<String, List<String>> = emptyMap()
)

/**
 * Carries a learner's progress from the corrupted pre-rebuild curriculum (content versions
 * 34/35, shipped in v1.0.0/v1.0.1) onto the rebuilt one, whose word and lesson ids are all new.
 *
 * - Attempts follow their word to its new id (old duplicates of one word simply merge); attempts
 *   on words with no counterpart are dropped.
 * - Lessons: every word taught by an old *completed* lesson counts as studied. Progress becomes the
 *   longest prefix of the new curriculum in which the learner had studied at least
 *   [KNOWN_SHARE] of the words overall and that ends on a lesson they mostly knew
 *   ([LAST_LESSON_SHARE]) - one contiguous, unlockable run, so a handful of words the old
 *   curriculum never taught (وَ, بِ...) can't erase a finished chapter. Checkpoints inside it
 *   count as passed and the lesson right after it is unlocked.
 * - Stats, streak, daily practice and achievements are untouched by design.
 *
 * Pure, so it is unit-tested without a database.
 */
object LegacyProgressRemapper {
    const val KNOWN_SHARE = 0.6
    const val LAST_LESSON_SHARE = 0.4

    data class Result(val progress: List<UserProgressEntity>, val attempts: List<ExerciseAttemptEntity>)

    fun remap(
        oldProgress: List<UserProgressEntity>,
        oldAttempts: List<ExerciseAttemptEntity>,
        map: LegacyContentMap,
        newLessonsInOrder: List<LessonEntity>,
        newLessonWords: Map<String, List<String>>
    ): Result {
        val attempts = oldAttempts.mapNotNull { a ->
            map.words[a.itemId]?.let { a.copy(id = 0, itemId = it) }
        }
        val progress = oldProgress.groupBy { it.userId }.flatMap { (userId, rows) ->
            remapUser(userId, rows, map, newLessonsInOrder, newLessonWords)
        }
        return Result(progress, attempts)
    }

    private fun remapUser(
        userId: String,
        rows: List<UserProgressEntity>,
        map: LegacyContentMap,
        lessons: List<LessonEntity>,
        lessonWords: Map<String, List<String>>
    ): List<UserProgressEntity> {
        val completed = rows.filter { it.status == LessonStatus.COMPLETED }
        val studied = completed.flatMap { map.lessonWords[it.lessonId].orEmpty() }.toSet()
        if (studied.isEmpty()) return emptyList()
        val scores = completed.map { it.bestScorePercent }
        val meanScore = if (scores.isEmpty()) 100 else scores.average().toInt().coerceIn(0, 100)
        val completedAt = completed.mapNotNull { it.completedAtEpochMillis }.maxOrNull()

        var frontier = -1
        var known = 0
        var total = 0
        lessons.forEachIndexed { i, lesson ->
            if (lesson.kind != LessonKind.REGULAR) return@forEachIndexed
            val words = lessonWords[lesson.id].orEmpty()
            val k = words.count { it in studied }
            known += k
            total += words.size
            if (words.isNotEmpty() && known >= KNOWN_SHARE * total && k >= LAST_LESSON_SHARE * words.size) {
                frontier = i
            }
        }
        if (frontier < 0) return emptyList()

        val out = mutableListOf<UserProgressEntity>()
        for (i in 0..frontier) {
            val lesson = lessons[i]
            out += if (lesson.kind == LessonKind.REGULAR) {
                UserProgressEntity(userId, lesson.id, LessonStatus.COMPLETED, meanScore, completedAt)
            } else {
                checkpoint(userId, lesson, completedAt)
            }
        }
        lessons.getOrNull(frontier + 1)?.let { out += UserProgressEntity(userId, it.id, LessonStatus.UNLOCKED, 0, null) }
        return out
    }

    private fun checkpoint(userId: String, lesson: LessonEntity, completedAt: Long?) = UserProgressEntity(
        userId = userId,
        lessonId = lesson.id,
        status = LessonStatus.COMPLETED,
        bestScorePercent = if (lesson.kind == LessonKind.CHAPTER_INTRO) 100 else GamificationConfig.PASSING_SCORE_PERCENT,
        completedAtEpochMillis = completedAt
    )

    /** Learner-facing curriculum order: chapters, then each chapter's sections (and their
     * lessons) in order, then the chapter-level lessons (the chapter exam). */
    fun curriculumOrder(
        chapters: List<ChapterEntity>,
        sections: List<SectionEntity>,
        lessons: List<LessonEntity>
    ): List<LessonEntity> {
        val bySection = lessons.filter { it.sectionId != null }.groupBy { it.sectionId }
        val chapterLevel = lessons.filter { it.sectionId == null }.groupBy { it.chapterId }
        return chapters.sortedBy { it.sortOrder }.flatMap { ch ->
            sections.filter { it.chapterId == ch.id }.sortedBy { it.sortOrder }
                .flatMap { s -> bySection[s.id].orEmpty().sortedBy { it.sortOrder } } +
                chapterLevel[ch.id].orEmpty().sortedBy { it.sortOrder }
        }
    }
}
