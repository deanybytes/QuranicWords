package com.quranicwords.app.core.domain.model

import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.util.GamificationConfig

data class LessonResult(
    val lessonId: String,
    val correctCount: Int,
    val totalCount: Int,
    val pointsAwarded: Int,
    val newTotalPoints: Int,
    val currentStreak: Int,
    val streakIncreased: Boolean,
    /** The next thing to unlock in the curriculum (another lesson, an exam, or a flashback
     * review - see [com.quranicwords.app.core.domain.CurriculumUnlockResolver]), if any - lets
     * the summary screen flow straight into it instead of always dropping the learner back at
     * Home. */
    val nextLessonId: String? = null,
    /** The kind of lesson just completed - null for a Review session (no single lesson row to
     * report a kind for). Lets the summary screen show pass/fail messaging tied to the real
     * per-kind gating rule ([com.quranicwords.app.core.domain.requiresPassingScore]) instead of a
     * hardcoded accuracy threshold - see [com.quranicwords.app.core.data.repository
     * .ProgressRepositoryImpl.completeLesson], which already gates on exactly this. */
    val lessonKind: LessonKind? = null,
    /** Wall-clock time spent on this lesson/session, from the moment its ViewModel was
     * constructed to the moment it finished - shown on completed lesson/summary UI and rolled
     * into daily practice-minutes tracking (see [com.quranicwords.app.core.data.local.entity.DailyPracticeEntity]). */
    val durationMillis: Long = 0L,
    /** See [LessonSessionType] - which of the four session kinds this result came from, so the
     * summary screen can pick the right primary-button behavior. */
    val sessionType: LessonSessionType = LessonSessionType.LESSON
) {
    val accuracyPercent: Int
        get() = GamificationConfig.percentOf(correctCount, totalCount)
}
