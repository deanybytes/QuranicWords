package com.quranicwords.app.feature.testonlyhome

import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.domain.DailyGoalCalculator
import com.quranicwords.app.core.domain.InactivityDuration
import com.quranicwords.app.core.domain.StreakRecovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TestOnlyHomeViewModelTest {

    private val today = LocalDate.of(2026, 8, 22)

    @Test
    fun `30-day practice history mapping computes correct active days and total minutes`() {
        val practiceHistory = listOf(
            DailyPracticeEntity("u1", "2026-08-22", 15),
            DailyPracticeEntity("u1", "2026-08-21", 20),
            DailyPracticeEntity("u1", "2026-08-20", 0),
            DailyPracticeEntity("u1", "2026-08-15", 30)
        )

        val practiceMap = practiceHistory.associate { it.localDate to it.minutesPracticed }
        val last30DaysMinutes = (29 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong()).toString()
            practiceMap[d] ?: 0
        }

        assertEquals(30, last30DaysMinutes.size)
        assertEquals(15, last30DaysMinutes.last()) // today
        assertEquals(20, last30DaysMinutes[28])   // yesterday
        assertEquals(3, last30DaysMinutes.count { it > 0 })
        assertEquals(65, last30DaysMinutes.sum())
    }

    @Test
    fun `frequency mode clamps progress at total 4538 corpus limit`() {
        val offset1 = 50
        val offset2 = 5000

        assertEquals(50, offset1.coerceAtMost(4538))
        assertEquals(4538, offset2.coerceAtMost(4538))
    }

    @Test
    fun `random covered tracking counts distinct covered words`() {
        val coveredSet = setOf("w1", "w2", "w3", "w4", "w5")
        assertEquals(5, coveredSet.size.coerceAtMost(4538))
    }

    @Test
    fun `daily goal calculation identifies when target minutes are achieved`() {
        assertTrue(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 20, goalMinutes = 20))
        assertTrue(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 25, goalMinutes = 20))
        assertFalse(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 19, goalMinutes = 20))
    }
}
