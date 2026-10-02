package com.quranicwords.app.core.util

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class StreakCalculatorTest {

    private val today = Instant.parse("2026-08-12T10:00:00Z")
    private val clock = Clock.fixed(today, ZoneOffset.UTC)
    private val calculator = StreakCalculator(clock)

    @Test
    fun `first ever activity starts a streak of one`() {
        val result = calculator.recordActivity(previous = null, userId = "u1", pointsToAdd = 10)

        assertEquals(1, result.stats.currentStreak)
        assertEquals(1, result.stats.longestStreak)
        assertEquals(10, result.stats.totalPoints)
        assertEquals("2026-08-12", result.stats.lastActivityLocalDate)
        assertTrue(result.streakIncreased)
    }

    @Test
    fun `repeating on the same local day does not change the streak`() {
        val previous = UserStatsEntity(
            userId = "u1",
            totalPoints = 50,
            currentStreak = 3,
            longestStreak = 5,
            lastActivityLocalDate = "2026-08-12"
        )

        val result = calculator.recordActivity(previous, "u1", pointsToAdd = 10)

        assertEquals(3, result.stats.currentStreak)
        assertEquals(5, result.stats.longestStreak)
        assertEquals(60, result.stats.totalPoints)
        assertFalse(result.streakIncreased)
    }

    @Test
    fun `activity on the very next local day extends the streak`() {
        val previous = UserStatsEntity(
            userId = "u1",
            totalPoints = 50,
            currentStreak = 3,
            longestStreak = 3,
            lastActivityLocalDate = "2026-08-11"
        )

        val result = calculator.recordActivity(previous, "u1", pointsToAdd = 10)

        assertEquals(4, result.stats.currentStreak)
        assertEquals(4, result.stats.longestStreak)
        assertTrue(result.streakIncreased)
    }

    @Test
    fun `a gap of more than one day resets the streak to one`() {
        val previous = UserStatsEntity(
            userId = "u1",
            totalPoints = 50,
            currentStreak = 7,
            longestStreak = 9,
            lastActivityLocalDate = "2026-08-05"
        )

        val result = calculator.recordActivity(previous, "u1", pointsToAdd = 10)

        assertEquals(1, result.stats.currentStreak)
        // Longest streak is a high-water mark - a reset never lowers it.
        assertEquals(9, result.stats.longestStreak)
        // A reset is not an increase - the summary must not celebrate a broken streak.
        assertFalse(result.streakIncreased)
        assertTrue(result.streakReset)
    }

    @Test
    fun `extending or repeating a streak never reports a reset`() {
        val yesterday = UserStatsEntity("u1", totalPoints = 0, currentStreak = 3, longestStreak = 3, lastActivityLocalDate = "2026-08-11")
        val sameDay = yesterday.copy(lastActivityLocalDate = "2026-08-12")

        assertFalse(calculator.recordActivity(yesterday, "u1", 0).streakReset)
        assertFalse(calculator.recordActivity(sameDay, "u1", 0).streakReset)
        assertFalse(calculator.recordActivity(null, "u1", 0).streakReset)
    }

    @Test
    fun `a stale zero streak restarting is an increase, not a reset`() {
        val previous = UserStatsEntity("u1", totalPoints = 0, currentStreak = 0, longestStreak = 4, lastActivityLocalDate = "2026-07-01")

        val result = calculator.recordActivity(previous, "u1", 0)

        assertEquals(1, result.stats.currentStreak)
        assertTrue(result.streakIncreased)
        assertFalse(result.streakReset)
    }

    @Test
    fun `a last-activity date in the future is treated as the same day`() {
        // Clock rolled back two days (or the learner travelled west across the date line).
        val previous = UserStatsEntity("u1", totalPoints = 0, currentStreak = 5, longestStreak = 5, lastActivityLocalDate = "2026-08-14")

        val result = calculator.recordActivity(previous, "u1", pointsToAdd = 10)

        assertEquals(5, result.stats.currentStreak)
        assertEquals(10, result.stats.totalPoints)
        assertFalse(result.streakIncreased)
        assertFalse(result.streakReset)
        // The later date is kept, so the streak survives once the clock catches up.
        assertEquals("2026-08-14", result.stats.lastActivityLocalDate)
    }

    @Test
    fun `longest streak is updated when the current streak surpasses it`() {
        val previous = UserStatsEntity(
            userId = "u1",
            totalPoints = 50,
            currentStreak = 9,
            longestStreak = 9,
            lastActivityLocalDate = "2026-08-11"
        )

        val result = calculator.recordActivity(previous, "u1", pointsToAdd = 10)

        assertEquals(10, result.stats.currentStreak)
        assertEquals(10, result.stats.longestStreak)
    }
}
