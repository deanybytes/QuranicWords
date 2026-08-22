package com.quranicwords.app.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalCalculatorTest {

    @Test
    fun `goal is met when minutes practiced exactly equal the goal`() {
        assertTrue(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 20, goalMinutes = 20))
    }

    @Test
    fun `goal is met when minutes practiced exceed the goal`() {
        assertTrue(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 45, goalMinutes = 20))
    }

    @Test
    fun `goal is not met one minute under the target`() {
        assertFalse(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 19, goalMinutes = 20))
    }

    @Test
    fun `zero minutes practiced never meets a nonzero goal`() {
        assertFalse(DailyGoalCalculator.isGoalMetToday(minutesPracticedToday = 0, goalMinutes = 10))
    }
}
