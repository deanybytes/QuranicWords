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

    /** The first-try bonus: [perfectBonus] only on a perfect, non-empty session. */
    fun perfectBonus(correctCount: Int, totalCount: Int): Int =
        if (totalCount > 0 && correctCount == totalCount) PERFECT_LESSON_BONUS else 0

    // Combo: consecutive first-try correct answers within one session. Each correct answer
    // that lands the combo at or above a tier earns that tier's bonus on top of its base points.
    const val COMBO_TIER_ONE = 5
    const val COMBO_TIER_ONE_BONUS = 2
    const val COMBO_TIER_TWO = 10
    const val COMBO_TIER_TWO_BONUS = 5

    /** Bonus XP for the answer that brought the combo to [combo]. */
    fun comboBonusFor(combo: Int): Int = when {
        combo >= COMBO_TIER_TWO -> COMBO_TIER_TWO_BONUS
        combo >= COMBO_TIER_ONE -> COMBO_TIER_ONE_BONUS
        else -> 0
    }

    /** Replaying an already-completed lesson still pays, at this share - practice is welcome,
     * farming XP off the easiest lesson isn't. Failed exams/flashbacks pay nothing at all. */
    const val REPLAY_POINTS_PERCENT = 50

    /** Hearts restored by finishing a review or practice session. */
    const val HEARTS_PER_REVIEW_SESSION = 1
}

/** Shared streak-day tiers - both [com.quranicwords.app.core.ui.components.StreakFlame]'s
 * flicker/size intensity and [com.quranicwords.app.core.domain.AchievementCatalog]'s streak
 * milestones key off the same three numbers, so they can't drift apart. */
object StreakTiers {
    const val BRONZE = 7
    const val SILVER = 30
    const val GOLD = 100
}
