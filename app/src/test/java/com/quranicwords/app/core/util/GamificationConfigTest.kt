package com.quranicwords.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GamificationConfigTest {

    @Test
    fun `awards ten points per correct answer`() {
        assertEquals(30, GamificationConfig.pointsForLesson(correctCount = 3, totalCount = 5))
    }

    @Test
    fun `awards a perfect-lesson bonus only at one hundred percent accuracy`() {
        val perfect = GamificationConfig.pointsForLesson(correctCount = 5, totalCount = 5)
        val imperfect = GamificationConfig.pointsForLesson(correctCount = 4, totalCount = 5)

        assertEquals(5 * 10 + 20, perfect)
        assertEquals(4 * 10, imperfect)
    }

    @Test
    fun `zero correct answers awards zero points`() {
        assertEquals(0, GamificationConfig.pointsForLesson(correctCount = 0, totalCount = 5))
    }

    @Test
    fun `an empty lesson never awards the perfect bonus`() {
        assertEquals(0, GamificationConfig.pointsForLesson(correctCount = 0, totalCount = 0))
    }
}
