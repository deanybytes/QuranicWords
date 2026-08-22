package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Streak-lock/recovery rules. Deliberately computed lazily from [UserStatsEntity]'s existing
 * fields rather than a new persisted "locked" flag - [StreakCalculator.recordActivity] never
 * actually zeroes [UserStatsEntity.currentStreak] until the *next* real activity completes (see
 * its own doc comment), so the un-reset value is naturally still sitting there for as long as the
 * learner hasn't practiced - "locked" is just "stale by 2+ days and was worth protecting",
 * matching the same staleness idea [com.quranicwords.app.feature.home.HomeViewModel
 * .displayedStreak] already uses for its own 1-day display grace.
 *
 * A recovery attempt must never be routed through [StreakCalculator.recordActivity] itself - that
 * would immediately reset the streak to 1 via its own "more than a day gap" branch, clobbering
 * the very value this is trying to restore. See `ProgressRepository.attemptStreakRecovery`.
 */
object StreakRecovery {
    /** Streaks shorter than 7 days aren't worth a recovery quiz - they just reset silently, same
     * as before this feature existed. Longer streaks need proportionally more proof: 3 questions
     * for a week-plus streak, 5 for a month-plus, 10 for a year-plus. */
    fun recoveryQuestionCount(brokenStreakLength: Int): Int? = when {
        brokenStreakLength < 7 -> null
        brokenStreakLength < 30 -> 3
        brokenStreakLength < 365 -> 5
        else -> 10
    }

    /** True when [stats] holds a streak worth protecting (>= 7, the [recoveryQuestionCount]
     * floor) that's gone stale for 2+ days - one day of grace beyond [StreakCalculator]'s own
     * "yesterday still counts" tolerance, so a locked state only appears once a day has been
     * fully missed, not merely late. */
    fun isLocked(stats: UserStatsEntity?, today: LocalDate): Boolean {
        if (stats == null || stats.currentStreak < 7) return false
        val lastActivity = stats.lastActivityLocalDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return false
        return ChronoUnit.DAYS.between(lastActivity, today) >= 2
    }
}
