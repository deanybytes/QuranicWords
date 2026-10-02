package com.quranicwords.app.feature.lesson

import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.util.GamificationConfig

/** One row [LessonViewModel] should hand to `ProgressRepository.logAttempt`. */
data class AttemptLog(
    val itemId: String,
    val exerciseType: ExerciseType,
    val correct: Boolean,
    val isFirstTry: Boolean
)

/**
 * Pure, immutable first-try scoring for one lesson session - kept out of [LessonViewModel] so the
 * rules are unit-testable without Hilt/Room/a main dispatcher.
 *
 * Two independent "first" notions, on purpose:
 * - **Per exercise index** ([gradedIndices]): only the first check of a given exercise counts
 *   toward [correctCount]. "Try again", going back with "Previous" and re-answering, or retrying a
 *   Matching exercise never re-scores it - so an exam can't be passed by retrying until correct.
 * - **Per practiced item** ([loggedItemIds]): only the first logged attempt of a word in this
 *   session is an honest recall signal (`isFirstTry = true`); retries and in-session repeats (see
 *   `LessonContentRepeater`) are still logged for history but flagged `isFirstTry = false`, which
 *   the missed/mastered queries ignore - a retry can't clear a mistake or mint mastery.
 */
data class LessonScoring(
    val gradedIndices: Set<Int> = emptySet(),
    val correctCount: Int = 0,
    val loggedItemIds: Set<String> = emptySet()
) {
    fun isGraded(index: Int): Boolean = index in gradedIndices

    /** Grades exercise [index]; a no-op once that index has been graded. */
    fun grade(index: Int, correct: Boolean): LessonScoring =
        if (isGraded(index)) this
        else copy(gradedIndices = gradedIndices + index, correctCount = correctCount + if (correct) 1 else 0)

    /** Builds the attempt row for [itemId], flagging it first-try only the first time this session
     * sees that item. */
    fun log(itemId: String, exerciseType: ExerciseType, correct: Boolean): Pair<LessonScoring, AttemptLog> {
        val firstTry = itemId !in loggedItemIds
        val next = if (firstTry) copy(loggedItemIds = loggedItemIds + itemId) else this
        return next to AttemptLog(itemId, exerciseType, correct, isFirstTry = firstTry)
    }

    fun scorePercent(totalCount: Int): Int = GamificationConfig.percentOf(correctCount, totalCount)

    /** The same threshold `ProgressRepository.completeLesson` gates exams/flashbacks on, applied
     * to the first-try [correctCount] this session will report. */
    fun meetsPassingScore(totalCount: Int): Boolean =
        scorePercent(totalCount) >= GamificationConfig.PASSING_SCORE_PERCENT
}
