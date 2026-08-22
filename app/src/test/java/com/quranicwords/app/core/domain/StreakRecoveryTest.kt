package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakRecoveryTest {

    // --- recoveryQuestionCount ---

    @Test
    fun `recoveryQuestionCount is null under the 7-day floor`() {
        assertNull(StreakRecovery.recoveryQuestionCount(6))
    }

    @Test
    fun `recoveryQuestionCount is 3 right at the 7-day floor`() {
        assertEquals(3, StreakRecovery.recoveryQuestionCount(7))
    }

    @Test
    fun `recoveryQuestionCount is 3 just under the 30-day tier`() {
        assertEquals(3, StreakRecovery.recoveryQuestionCount(29))
    }

    @Test
    fun `recoveryQuestionCount is 5 right at the 30-day tier`() {
        assertEquals(5, StreakRecovery.recoveryQuestionCount(30))
    }

    @Test
    fun `recoveryQuestionCount is 5 just under the 365-day tier`() {
        assertEquals(5, StreakRecovery.recoveryQuestionCount(364))
    }

    @Test
    fun `recoveryQuestionCount is 10 right at the 365-day tier`() {
        assertEquals(10, StreakRecovery.recoveryQuestionCount(365))
    }

    // --- isLocked ---

    private val today = LocalDate.of(2026, 1, 10)

    private fun stats(currentStreak: Int, lastActivityLocalDate: String?) = UserStatsEntity(
        userId = "u",
        totalPoints = 0,
        currentStreak = currentStreak,
        longestStreak = currentStreak,
        lastActivityLocalDate = lastActivityLocalDate
    )

    @Test
    fun `isLocked is false with null stats`() {
        assertFalse(StreakRecovery.isLocked(null, today))
    }

    @Test
    fun `isLocked is false when the streak is under 7 days even after a 2-day gap`() {
        val s = stats(currentStreak = 6, lastActivityLocalDate = today.minusDays(2).toString())
        assertFalse(StreakRecovery.isLocked(s, today))
    }

    @Test
    fun `isLocked is false with only a 1-day gap, matching StreakCalculator's own grace`() {
        val s = stats(currentStreak = 10, lastActivityLocalDate = today.minusDays(1).toString())
        assertFalse(StreakRecovery.isLocked(s, today))
    }

    @Test
    fun `isLocked is false when the streak is current (activity today)`() {
        val s = stats(currentStreak = 10, lastActivityLocalDate = today.toString())
        assertFalse(StreakRecovery.isLocked(s, today))
    }

    @Test
    fun `isLocked is true with a 7-plus day streak and a 2-day gap`() {
        val s = stats(currentStreak = 10, lastActivityLocalDate = today.minusDays(2).toString())
        assertTrue(StreakRecovery.isLocked(s, today))
    }

    @Test
    fun `isLocked is true with a longer gap too`() {
        val s = stats(currentStreak = 40, lastActivityLocalDate = today.minusDays(10).toString())
        assertTrue(StreakRecovery.isLocked(s, today))
    }

    @Test
    fun `isLocked is false with a null lastActivityLocalDate`() {
        val s = stats(currentStreak = 10, lastActivityLocalDate = null)
        assertFalse(StreakRecovery.isLocked(s, today))
    }
}
