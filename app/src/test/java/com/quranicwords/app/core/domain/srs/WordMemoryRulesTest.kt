package com.quranicwords.app.core.domain.srs

import com.quranicwords.app.core.data.local.entity.ExerciseAttemptEntity
import com.quranicwords.app.core.data.local.entity.ExerciseEntity
import com.quranicwords.app.core.domain.model.ExerciseType
import com.quranicwords.app.core.domain.model.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class WordMemoryRulesTest {

    private val scheduler = FsrsScheduler()
    private val day = FsrsScheduler.DAY_MILLIS
    private val t0 = Instant.parse("2026-09-01T08:00:00Z").toEpochMilli()

    private fun apply(existing: com.quranicwords.app.core.data.local.entity.WordMemoryEntity?, correct: Boolean, at: Long, date: String) =
        WordMemoryRules.applyFirstTry(existing, "u", "w1", correct, at, date, scheduler)

    @Test
    fun `the scheduler runs only once per local day`() {
        val first = apply(null, correct = true, at = t0, date = "2026-09-01")
        val sameDay = apply(first, correct = true, at = t0 + 60_000, date = "2026-09-01")

        assertEquals(first.stability, sameDay.stability, 1e-9)
        assertEquals(first.reps, sameDay.reps)
        assertEquals(first.dueAtEpochMillis, sameDay.dueAtEpochMillis)
    }

    @Test
    fun `a later same-day first try still records its grade`() {
        val first = apply(null, correct = true, at = t0, date = "2026-09-01")
        val missedLater = apply(first, correct = false, at = t0 + 3_600_000, date = "2026-09-01")

        assertEquals(ReviewGrade.AGAIN.value, missedLater.lastGrade)
        assertEquals(first.stability, missedLater.stability, 1e-9)
    }

    @Test
    fun `a correct same-day answer to a lapsed word defers it to tomorrow`() {
        val missed = apply(null, correct = false, at = t0, date = "2026-09-01")
        val later = t0 + 30 * 60_000
        assertTrue("still due in relearning", missed.dueAtEpochMillis <= later)

        val fixed = apply(missed, correct = true, at = later, date = "2026-09-01")

        assertEquals(later + day, fixed.dueAtEpochMillis)
        assertEquals(ReviewGrade.GOOD.value, fixed.lastGrade)
    }

    @Test
    fun `a new day runs the scheduler again`() {
        val first = apply(null, correct = true, at = t0, date = "2026-09-01")
        val nextDay = apply(first, correct = true, at = t0 + day, date = "2026-09-02")

        assertEquals(2, nextDay.reps)
        assertEquals(MemoryState.REVIEW, nextDay.state)
        assertEquals("2026-09-02", nextDay.lastReviewLocalDate)
    }

    @Test
    fun `a local date before the stored one is treated as the same day`() {
        val first = apply(null, correct = true, at = t0, date = "2026-09-05")
        val rolledBack = apply(first, correct = true, at = t0 - 2 * day, date = "2026-09-03")

        assertEquals(first.reps, rolledBack.reps)
    }

    private fun attempt(id: Long, item: String, correct: Boolean, at: Long, firstTry: Boolean = true) =
        ExerciseAttemptEntity(id, "u", item, ItemKind.WORD, ExerciseType.MULTIPLE_CHOICE, correct, at, firstTry)

    @Test
    fun `backfill replays first tries per day, caps stability and spreads due dates`() {
        val now = t0 + 60 * day
        val attempts = listOf(
            attempt(1, "w1", true, t0),
            attempt(2, "w1", true, t0 + 1000), // same day - grade only
            attempt(3, "w1", true, t0 + day),
            attempt(4, "w1", true, t0 + 8 * day),
            attempt(5, "w1", false, t0 + 9 * day, firstTry = false), // retry - ignored
            attempt(6, "w2", false, t0)
        )

        val rows = WordMemoryRules.backfill("u", attempts, ZoneOffset.UTC, now, scheduler).associateBy { it.itemId }

        val w1 = checkNotNull(rows["w1"])
        assertEquals("one scheduler run per day, retries ignored", 3, w1.reps)
        assertTrue(w1.stability <= WordMemoryRules.BACKFILL_MAX_STABILITY_DAYS)
        assertEquals(now + WordMemoryRules.spreadDays("w1") * day, w1.dueAtEpochMillis)
        assertEquals(ReviewGrade.GOOD.value, w1.lastGrade)

        val w2 = checkNotNull(rows["w2"])
        assertEquals(ReviewGrade.AGAIN.value, w2.lastGrade)
        assertTrue(w2.dueAtEpochMillis in now..(now + 6 * day))
    }

    @Test
    fun `backfill is deterministic and ignores attempt order`() {
        val attempts = (0 until 20).map { attempt(it.toLong(), "w${it % 5}", it % 3 != 0, t0 + it * day) }
        val a = WordMemoryRules.backfill("u", attempts, ZoneOffset.UTC, t0 + 30 * day, scheduler)
        val b = WordMemoryRules.backfill("u", attempts.shuffled(), ZoneOffset.UTC, t0 + 30 * day, scheduler)
        assertEquals(a, b)
        assertTrue(a.all { WordMemoryRules.spreadDays(it.itemId) in 0 until WordMemoryRules.BACKFILL_SPREAD_DAYS })
    }

    @Test
    fun `backfill of a learner with only retries produces nothing`() {
        assertTrue(WordMemoryRules.backfill("u", listOf(attempt(1, "w1", true, t0, firstTry = false)), ZoneOffset.UTC, t0, scheduler).isEmpty())
    }

    private fun ex(id: String, item: String, type: ExerciseType) = ExerciseEntity(id, "l", 0, type, "{}", item)

    @Test
    fun `review exercises get harder as memory strengthens`() {
        val exercises = listOf("weak", "mid", "strong", "strongNoAudio").associateWith { item ->
            buildList {
                add(ex("${item}_mc", item, ExerciseType.MULTIPLE_CHOICE))
                add(ex("${item}_fill", item, ExerciseType.FILL_IN_THE_BLANK))
                if (item != "strongNoAudio") add(ex("${item}_hear", item, ExerciseType.TAP_WHAT_YOU_HEAR))
            }
        }
        val strengths = mapOf(
            "weak" to WordStrength.LEARNING,
            "mid" to WordStrength.FAMILIAR,
            "strong" to WordStrength.MASTERED,
            "strongNoAudio" to WordStrength.STRONG
        )

        val picked = ReviewExercisePicker.pick(listOf("weak", "mid", "strong", "strongNoAudio"), exercises, strengths, listeningEnabled = true)

        assertEquals(
            listOf(ExerciseType.MULTIPLE_CHOICE, ExerciseType.FILL_IN_THE_BLANK, ExerciseType.TAP_WHAT_YOU_HEAR, ExerciseType.MULTIPLE_CHOICE),
            picked.map { it.type }
        )
    }

    @Test
    fun `listening is never picked while audio is off and familiar words alternate context types`() {
        val items = listOf("a", "b")
        val exercises = items.associateWith { item ->
            listOf(
                ex("${item}_fill", item, ExerciseType.FILL_IN_THE_BLANK),
                ex("${item}_tap", item, ExerciseType.WORD_IN_VERSE_TAP),
                ex("${item}_hear", item, ExerciseType.TAP_WHAT_YOU_HEAR)
            )
        }
        val familiar = ReviewExercisePicker.pick(items, exercises, items.associateWith { WordStrength.FAMILIAR }, listeningEnabled = false)
        assertEquals(listOf(ExerciseType.FILL_IN_THE_BLANK, ExerciseType.WORD_IN_VERSE_TAP), familiar.map { it.type })

        val onlyListening = mapOf("x" to listOf(ex("x_hear", "x", ExerciseType.TAP_WHAT_YOU_HEAR)))
        assertTrue(ReviewExercisePicker.pick(listOf("x"), onlyListening, mapOf("x" to WordStrength.MASTERED), listeningEnabled = false).isEmpty())
        assertNull(ReviewExercisePicker.pick(listOf("missing"), emptyMap(), emptyMap(), true).firstOrNull())
    }
}
