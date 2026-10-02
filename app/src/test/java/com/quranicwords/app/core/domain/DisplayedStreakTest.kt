package com.quranicwords.app.core.domain

import com.quranicwords.app.core.data.local.entity.UserStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DisplayedStreakTest {

    private val today = LocalDate.parse("2026-08-22")
    private fun stats(lastActivity: String?) = UserStatsEntity("u", totalPoints = 0, currentStreak = 9, longestStreak = 9, lastActivityLocalDate = lastActivity)

    @Test
    fun `a streak active today or yesterday is shown as stored`() {
        assertEquals(9, DisplayedStreak.of(stats("2026-08-22"), today))
        assertEquals(9, DisplayedStreak.of(stats("2026-08-21"), today))
    }

    @Test
    fun `a stored streak older than yesterday displays as zero`() {
        assertEquals(0, DisplayedStreak.of(stats("2026-08-20"), today))
        assertEquals(0, DisplayedStreak.of(null, today))
        assertEquals(0, DisplayedStreak.of(stats(null), today))
    }

    @Test
    fun `a future last-activity date (clock rollback) still counts as alive and practiced`() {
        assertEquals(9, DisplayedStreak.of(stats("2026-08-24"), today))
        assertTrue(DisplayedStreak.practicedToday(stats("2026-08-24"), today))
    }

    @Test
    fun `practicedToday is false until something is completed today`() {
        assertFalse(DisplayedStreak.practicedToday(stats("2026-08-21"), today))
        assertTrue(DisplayedStreak.practicedToday(stats("2026-08-22"), today))
    }
}
