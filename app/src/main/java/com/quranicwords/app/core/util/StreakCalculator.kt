package com.quranicwords.app.core.util

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class StreakUpdateResult(val stats: UserStatsEntity, val streakIncreased: Boolean)

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

        val newStreak = when (prevDate) {
            today -> if (prevStreak == 0) 1 else prevStreak
            today.minusDays(1) -> prevStreak + 1
            else -> 1
        }

        val stats = UserStatsEntity(
            userId = userId,
            totalPoints = (previous?.totalPoints ?: 0) + pointsToAdd,
            currentStreak = newStreak,
            longestStreak = maxOf(previous?.longestStreak ?: 0, newStreak),
            lastActivityLocalDate = today.toString()
        )
        return StreakUpdateResult(stats, streakIncreased = newStreak != prevStreak)
    }
}
