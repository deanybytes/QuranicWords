package com.quranicwords.app.core.util

/** Tunable point rules for lesson scoring - kept as plain constants/functions, no config file. */
object GamificationConfig {
    const val POINTS_PER_CORRECT_ANSWER = 10
    const val PERFECT_LESSON_BONUS = 20

    /** Minimum score to pass an exam or flashback review and unlock what comes next - see
     * [com.quranicwords.app.core.data.local.entity.LessonKind]. Regular lessons have no
     * threshold at all; every other kind is gated at this bar. */
    const val PASSING_SCORE_PERCENT = 80

    fun pointsForLesson(correctCount: Int, totalCount: Int): Int {
        val base = correctCount * POINTS_PER_CORRECT_ANSWER
        val isPerfect = totalCount > 0 && correctCount == totalCount
        return base + if (isPerfect) PERFECT_LESSON_BONUS else 0
    }

    /** Shared `correct/total` percentage formula so every scoring surface (lesson result,
     * summary screen, stored best score) agrees on the same zero-total handling. */
    fun percentOf(correctCount: Int, totalCount: Int): Int =
        if (totalCount == 0) 0 else (correctCount * 100) / totalCount
}
