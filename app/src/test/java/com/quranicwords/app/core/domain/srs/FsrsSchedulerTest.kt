package com.quranicwords.app.core.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FsrsSchedulerTest {

    private val start = Instant.parse("2026-09-01T08:00:00Z").toEpochMilli()
    private val scheduler = FsrsScheduler(Clock.fixed(Instant.ofEpochMilli(start), ZoneOffset.UTC))
    private val day = FsrsScheduler.DAY_MILLIS

    @Test
    fun `retrievability is 1 at review time and 90 percent after one stability`() {
        assertEquals(1.0, FsrsScheduler.retrievability(0.0, 5.0), 1e-9)
        assertEquals(0.9, FsrsScheduler.retrievability(5.0, 5.0), 1e-9)
        assertTrue(FsrsScheduler.retrievability(20.0, 5.0) < 0.9)
    }

    @Test
    fun `a new card answered Good enters learning and is due in one day`() {
        val card = scheduler.review(MemoryCard(), ReviewGrade.GOOD, start)

        assertEquals(MemoryState.LEARNING, card.state)
        assertEquals(FsrsScheduler.DEFAULT_WEIGHTS[2], card.stability, 1e-9)
        assertEquals(start + day, card.dueAtEpochMillis)
        assertEquals(1, card.reps)
        assertEquals(0, card.lapses)
    }

    @Test
    fun `a new card answered Again relearns in ten minutes with low stability`() {
        val card = scheduler.review(MemoryCard(), ReviewGrade.AGAIN, start)

        assertEquals(FsrsScheduler.DEFAULT_WEIGHTS[0], card.stability, 1e-9)
        assertEquals(start + 10 * 60 * 1000L, card.dueAtEpochMillis)
        assertTrue(card.difficulty > scheduler.review(MemoryCard(), ReviewGrade.GOOD, start).difficulty)
    }

    @Test
    fun `stability grows on every on-time Good and intervals never shrink`() {
        var card = scheduler.review(MemoryCard(), ReviewGrade.GOOD, start)
        var now = start
        var previousStability = card.stability
        var previousInterval = 0L
        repeat(8) {
            now = card.dueAtEpochMillis
            card = scheduler.review(card, ReviewGrade.GOOD, now)
            val interval = card.dueAtEpochMillis - now
            assertEquals(MemoryState.REVIEW, card.state)
            assertTrue("stability must grow", card.stability > previousStability)
            assertTrue("interval must not shrink", interval >= previousInterval)
            assertEquals("whole days only", 0L, interval % day)
            previousStability = card.stability
            previousInterval = interval
        }
    }

    @Test
    fun `intervals are capped at the maximum`() {
        var card = scheduler.review(MemoryCard(), ReviewGrade.GOOD, start)
        repeat(30) { card = scheduler.review(card, ReviewGrade.GOOD, card.dueAtEpochMillis) }
        val lastReview = checkNotNull(card.lastReviewedAtEpochMillis)
        assertEquals(365L * day, card.dueAtEpochMillis - lastReview)
    }

    @Test
    fun `Again on a review card is a lapse that drops stability and relearns soon`() {
        var card = scheduler.review(MemoryCard(), ReviewGrade.GOOD, start)
        card = scheduler.review(card, ReviewGrade.GOOD, card.dueAtEpochMillis)
        card = scheduler.review(card, ReviewGrade.GOOD, card.dueAtEpochMillis)
        val beforeLapse = card

        val now = beforeLapse.dueAtEpochMillis
        val lapsed = scheduler.review(beforeLapse, ReviewGrade.AGAIN, now)

        assertEquals(MemoryState.RELEARNING, lapsed.state)
        assertEquals(1, lapsed.lapses)
        assertTrue(lapsed.stability < beforeLapse.stability)
        assertTrue(lapsed.difficulty > beforeLapse.difficulty)
        assertEquals(now + FsrsScheduler.RELEARNING_STEP_MILLIS, lapsed.dueAtEpochMillis)

        val relearned = scheduler.review(lapsed, ReviewGrade.GOOD, now + day)
        assertEquals(MemoryState.REVIEW, relearned.state)
        assertEquals(1, relearned.lapses)
    }

    @Test
    fun `an overdue Good grows stability more than an on-time one`() {
        var card = scheduler.review(MemoryCard(), ReviewGrade.GOOD, start)
        card = scheduler.review(card, ReviewGrade.GOOD, card.dueAtEpochMillis)

        val onTime = scheduler.review(card, ReviewGrade.GOOD, card.dueAtEpochMillis)
        val overdue = scheduler.review(card, ReviewGrade.GOOD, card.dueAtEpochMillis + 20 * day)

        assertTrue(overdue.stability > onTime.stability)
    }

    @Test
    fun `a clock that went backwards is treated as no elapsed time`() {
        val card = scheduler.review(MemoryCard(), ReviewGrade.GOOD, start)

        val rolledBack = scheduler.review(card, ReviewGrade.GOOD, start - 5 * day)
        val sameInstant = scheduler.review(card, ReviewGrade.GOOD, start)

        assertEquals(sameInstant.stability, rolledBack.stability, 1e-9)
    }

    @Test
    fun `the same history always produces the same schedule`() {
        fun run(): MemoryCard {
            var card = MemoryCard()
            listOf(ReviewGrade.GOOD, ReviewGrade.AGAIN, ReviewGrade.GOOD, ReviewGrade.GOOD).forEachIndexed { i, grade ->
                card = scheduler.review(card, grade, start + i * 3 * day)
            }
            return card
        }
        assertEquals(run(), run())
    }

    @Test
    fun `the clock overload reviews at the injected instant`() {
        assertEquals(scheduler.review(MemoryCard(), ReviewGrade.GOOD, start), scheduler.review(MemoryCard(), ReviewGrade.GOOD))
    }

    @Test
    fun `strength buckets follow stability thresholds`() {
        assertEquals(WordStrength.NEW, WordStrength.fromStability(null))
        assertEquals(WordStrength.NEW, WordStrength.fromStability(0.0))
        assertEquals(WordStrength.LEARNING, WordStrength.fromStability(1.9))
        assertEquals(WordStrength.FAMILIAR, WordStrength.fromStability(2.0))
        assertEquals(WordStrength.FAMILIAR, WordStrength.fromStability(6.9))
        assertEquals(WordStrength.STRONG, WordStrength.fromStability(7.0))
        assertEquals(WordStrength.MASTERED, WordStrength.fromStability(30.0))
        assertTrue(WordStrength.STRONG.isStrongOrBetter && WordStrength.MASTERED.isStrongOrBetter)
        assertTrue(!WordStrength.FAMILIAR.isStrongOrBetter)
    }
}
