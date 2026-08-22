package com.quranicwords.app.core.domain

/** Pure "did the learner meet today's daily goal" check - kept as a named, tested function rather
 * than an inline comparison in a ViewModel, matching this codebase's [com.quranicwords.app.core.util.StreakCalculator]/
 * [com.quranicwords.app.core.util.GamificationConfig] convention. */
object DailyGoalCalculator {
    fun isGoalMetToday(minutesPracticedToday: Int, goalMinutes: Int): Boolean =
        minutesPracticedToday >= goalMinutes
}
