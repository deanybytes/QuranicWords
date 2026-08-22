package com.quranicwords.app.feature.progress

import com.quranicwords.app.core.data.local.entity.DailyPracticeEntity
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Pure-logic coverage for the Progress tab's three chart-data derivations - the highest-value
 * test surface in the whole charts feature, since the Canvas drawing itself isn't meaningfully
 * testable (see [ProgressViewModel]'s doc comment).
 */
class ProgressViewModelTest {

    private val zone = ZoneOffset.UTC
    private val day0 = LocalDate.of(2026, 1, 1)
    private val days = (0..6).map { day0.plusDays(it.toLong()) }

    private fun progressRow(lessonId: String, date: LocalDate) = UserProgressEntity(
        userId = "u1",
        lessonId = lessonId,
        status = LessonStatus.COMPLETED,
        bestScorePercent = 100,
        completedAtEpochMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    private fun practiceRow(date: LocalDate, minutes: Int) =
        DailyPracticeEntity(userId = "u1", localDate = date.toString(), minutesPracticed = minutes)

    // --- lessonsCompletedPerDay ---

    @Test
    fun `lessonsCompletedPerDay counts completions per day and zero-fills days with none`() {
        val progress = listOf(
            progressRow("l1", days[0]),
            progressRow("l2", days[0]),
            progressRow("l3", days[3])
        )

        val result = lessonsCompletedPerDay(progress, days, zone)

        assertEquals(listOf(2, 0, 0, 1, 0, 0, 0), result)
    }

    @Test
    fun `lessonsCompletedPerDay ignores rows with a null completedAt`() {
        val progress = listOf(
            UserProgressEntity("u1", "l1", LessonStatus.UNLOCKED, 0, completedAtEpochMillis = null)
        )

        val result = lessonsCompletedPerDay(progress, days, zone)

        assertEquals(List(7) { 0 }, result)
    }

    @Test
    fun `lessonsCompletedPerDay returns all zeros for an empty progress list`() {
        assertEquals(List(7) { 0 }, lessonsCompletedPerDay(emptyList(), days, zone))
    }

    // --- practiceDaysGrid ---

    @Test
    fun `practiceDaysGrid marks only days with a positive-minute practice row`() {
        val dailyPractice = listOf(
            practiceRow(days[1], minutes = 20),
            practiceRow(days[2], minutes = 0)
        )

        val result = practiceDaysGrid(dailyPractice, days)

        assertEquals(listOf(false, true, false, false, false, false, false), result)
    }

    @Test
    fun `practiceDaysGrid returns all false when there is no practice history`() {
        assertEquals(List(7) { false }, practiceDaysGrid(emptyList(), days))
    }

    // --- countGoalMetDays ---

    @Test
    fun `countGoalMetDays counts days at or above the goal threshold`() {
        val dailyPractice = listOf(
            practiceRow(days[0], minutes = 20), // exactly at goal
            practiceRow(days[1], minutes = 19), // just under goal
            practiceRow(days[2], minutes = 45)  // well above goal
        )

        assertEquals(2, countGoalMetDays(dailyPractice, days, goalMinutes = 20))
    }

    @Test
    fun `countGoalMetDays treats days with no practice row as zero minutes`() {
        val dailyPractice = listOf(practiceRow(days[0], minutes = 20))

        assertEquals(1, countGoalMetDays(dailyPractice, days, goalMinutes = 20))
    }

    @Test
    fun `countGoalMetDays is zero when the goal is unreachable by any tracked day`() {
        val dailyPractice = listOf(practiceRow(days[0], minutes = 10))

        assertEquals(0, countGoalMetDays(dailyPractice, days, goalMinutes = 30))
    }
}
