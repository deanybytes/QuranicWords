package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The one definition of "the streak the learner sees" - Home, Progress, Test-only Home, the
 * widgets and the streak-reminder worker all go through here so they can never disagree.
 *
 * [UserStatsEntity.currentStreak] is only ever recomputed by `StreakCalculator` when a session
 * completes, so a stored streak from days ago would otherwise still show as "alive" even though
 * the learner missed a day - it only silently drops the next time they finish something. A last
 * activity older than yesterday therefore displays as 0. A last activity *after* [today] (clock
 * rolled back) is treated like today, matching `StreakCalculator`.
 */
object DisplayedStreak {
    fun of(stats: UserStatsEntity?, today: LocalDate): Int {
        val lastActivity = lastActivityDate(stats) ?: return 0
        val dayGap = ChronoUnit.DAYS.between(lastActivity, today)
        return if (dayGap <= 1) stats?.currentStreak ?: 0 else 0
    }

    /** True once any session has been completed today (or "later", after a clock rollback). */
    fun practicedToday(stats: UserStatsEntity?, today: LocalDate): Boolean {
        val lastActivity = lastActivityDate(stats) ?: return false
        return !lastActivity.isBefore(today)
    }

    private fun lastActivityDate(stats: UserStatsEntity?): LocalDate? =
        stats?.lastActivityLocalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
