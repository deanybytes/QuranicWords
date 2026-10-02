package com.quranicwords.app.feature.lessonsummary

import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryCelebrationTest {

    private fun route(previousXp: Int, newXp: Int, streak: Int = 3, increased: Boolean = false) = Route.LessonSummary(
        lessonId = "l1",
        correctCount = 8,
        totalCount = 10,
        pointsAwarded = newXp - previousXp,
        newTotalPoints = newXp,
        currentStreak = streak,
        streakIncreased = increased,
        previousTotalPoints = previousXp
    )

    @Test
    fun `crossing a level boundary is a level-up`() {
        assertEquals(2, celebrationFor(route(previousXp = 90, newXp = 120)).leveledUpTo)
        assertNull(celebrationFor(route(previousXp = 110, newXp = 190)).leveledUpTo)
    }

    @Test
    fun `only a streak that just reached 7, 30 or 100 is a milestone`() {
        assertEquals(7, celebrationFor(route(0, 10, streak = 7, increased = true)).streakMilestone)
        assertNull(celebrationFor(route(0, 10, streak = 7, increased = false)).streakMilestone)
        assertNull(celebrationFor(route(0, 10, streak = 8, increased = true)).streakMilestone)
        assertEquals(100, celebrationFor(route(0, 10, streak = 100, increased = true)).streakMilestone)
    }

    @Test
    fun `gated lessons pass at the threshold`() {
        assertTrue(summaryPassed(route(0, 10).copy(lessonKind = LessonKind.SECTION_EXAM)))
        assertFalse(summaryPassed(route(0, 10).copy(lessonKind = LessonKind.SECTION_EXAM, correctCount = 7)))
        assertTrue(summaryPassed(route(0, 10).copy(lessonKind = LessonKind.REGULAR, correctCount = 1)))
    }
}
