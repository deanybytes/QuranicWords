package com.quranicwords.app.core.domain.model

import com.quranicwords.app.core.util.GamificationConfig

data class LessonResult(
    val lessonId: String,
    val correctCount: Int,
    val totalCount: Int,
    val pointsAwarded: Int,
    val newTotalPoints: Int,
    val currentStreak: Int,
    val streakIncreased: Boolean,
    /** The next lesson in this module by sortOrder, if any - lets the summary screen flow
     * straight into it instead of always dropping the learner back at Home. */
    val nextLessonId: String? = null
) {
    val accuracyPercent: Int
        get() = GamificationConfig.percentOf(correctCount, totalCount)
}
