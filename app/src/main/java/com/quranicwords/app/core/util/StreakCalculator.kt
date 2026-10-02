package com.quranicwords.app.core.util

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** [streakIncreased] is true only when the streak actually grew (0 -> 1, or extended by a new
 * day); [streakReset] is true when a non-zero streak was broken by a missed day and restarted at
 * 1. At most one of them is true - a same-day repeat leaves both false. */
data class StreakUpdateResult(
    val stats: UserStatsEntity,
    val streakIncreased: Boolean,
    val streakReset: Boolean = false
)

/**
 * Pure, unit-testable streak/points logic. [clock] is injected (rather than calling
 * `LocalDate.now()` directly) so tests are deterministic regardless of the machine's real date,
 * and so behavior is correct across timezone changes - streaks are compared by local calendar
 * date, never by UTC epoch, since "did the user practice today" is inherently a local-date
 * question.
 */
class StreakCalculator @Inject constructor(private val clock: Clock) {

    fun recordActivity(previous: UserStatsEntity?, userId: String, pointsToAdd: Int): StreakUpdateResult {
        val today = LocalDate.now(clock)
        val prevDate = previous?.lastActivityLocalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val prevStreak = previous?.currentStreak ?: 0

        // A last-activity date *after* today means the clock went backwards (manual change, or
        // travelling west across a date line) - treat it as the same day rather than a gap, and
        // keep the later date so the streak isn't broken once the clock catches up again.
        val clockRolledBack = prevDate != null && prevDate.isAfter(today)
        val continued = prevDate == today || clockRolledBack || prevDate == today.minusDays(1)
        val newStreak = when {
            prevDate == today || clockRolledBack -> if (prevStreak == 0) 1 else prevStreak
            prevDate == today.minusDays(1) -> prevStreak + 1
            else -> 1
        }

        // copy() rather than a fresh row, so columns this calculator doesn't own (hearts, best
        // combo, the hearts setting) survive every lesson.
        val base = previous ?: UserStatsEntity(userId, 0, 0, 0, null)
        val stats = base.copy(
            userId = userId,
            totalPoints = base.totalPoints + pointsToAdd,
            currentStreak = newStreak,
            longestStreak = maxOf(base.longestStreak, newStreak),
            lastActivityLocalDate = (if (clockRolledBack) prevDate else today).toString()
        )
        return StreakUpdateResult(
            stats,
            streakIncreased = newStreak > prevStreak,
            streakReset = !continued && prevStreak > 0
        )
    }
}
