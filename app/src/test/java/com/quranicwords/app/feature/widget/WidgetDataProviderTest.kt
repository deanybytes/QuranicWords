package com.quranicwords.app.feature.widget

import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.util.VerseReferenceFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** Pure widget logic: word selection, empty states, sizing, formatting and deep-link parsing. */
class WidgetDataProviderTest {

    private val pools = WordPools(due = listOf("d1", "d2"), learned = listOf("l1", "l2", "l3"), nextNew = "n1")

    // --- Word selection -------------------------------------------------------------------

    @Test
    fun `a due word always wins over learned and new words`() {
        for (step in 0..10) {
            val pick = WidgetWordPicker.pick(pools, step)!!
            assertEquals(WordKind.DUE, pick.kind)
            assertTrue(pick.wordId in pools.due)
        }
    }

    @Test
    fun `rotation cycles through the due pool in order and wraps`() {
        assertEquals(listOf("d1", "d2", "d1", "d2"), (0..3).map { WidgetWordPicker.pick(pools, it)!!.wordId })
    }

    @Test
    fun `with nothing due a learned word is shown, rotating`() {
        val noDue = pools.copy(due = emptyList())
        assertEquals(listOf("l1", "l2", "l3", "l1"), (0..3).map { WidgetWordPicker.pick(noDue, it)!!.wordId })
        assertEquals(WordKind.LEARNED, WidgetWordPicker.pick(noDue, 0)!!.kind)
    }

    @Test
    fun `a learner with nothing practised sees the next word to learn`() {
        val fresh = WordPools(emptyList(), emptyList(), nextNew = "n1")
        assertEquals(WordPick("n1", WordKind.NEW), WidgetWordPicker.pick(fresh, 7))
    }

    @Test
    fun `no pools at all is the empty state`() {
        val empty = WordPools(emptyList(), emptyList(), null)
        assertTrue(empty.isEmpty)
        assertNull(WidgetWordPicker.pick(empty, 0))
        assertTrue(WidgetSnapshot.notReady().pools.isEmpty)
        assertFalse(WidgetSnapshot.notReady().contentReady)
    }

    @Test
    fun `a negative step never crashes`() {
        assertEquals("d1", WidgetWordPicker.pick(pools, -5)!!.wordId)
    }

    @Test
    fun `only a due word hides its meaning, until that word is revealed`() {
        assertTrue(WidgetWordPicker.isMeaningHidden(WordKind.DUE, "d1", revealedWordId = null))
        assertFalse(WidgetWordPicker.isMeaningHidden(WordKind.DUE, "d1", revealedWordId = "d1"))
        // Revealing one word doesn't reveal the next one.
        assertTrue(WidgetWordPicker.isMeaningHidden(WordKind.DUE, "d2", revealedWordId = "d1"))
        assertFalse(WidgetWordPicker.isMeaningHidden(WordKind.LEARNED, "l1", null))
        assertFalse(WidgetWordPicker.isMeaningHidden(WordKind.NEW, "n1", null))
    }

    // --- Tap targets ----------------------------------------------------------------------

    @Test
    fun `word taps route by kind`() {
        assertEquals(WidgetTarget(WidgetDestination.DAILY_REVIEW), WidgetActions.forWord(WordKind.DUE, "d1"))
        assertEquals(WidgetTarget(WidgetDestination.PRACTICE_WORD, "l1"), WidgetActions.forWord(WordKind.LEARNED, "l1"))
        assertEquals(WidgetTarget(WidgetDestination.HOME), WidgetActions.forWord(WordKind.NEW, "n1"))
    }

    @Test
    fun `the progress call to action is review only when something is due`() {
        assertEquals(WidgetDestination.DAILY_REVIEW, WidgetActions.forStats(contentReady = true, dueCount = 3).destination)
        assertEquals(WidgetDestination.HOME, WidgetActions.forStats(contentReady = true, dueCount = 0).destination)
        assertEquals(WidgetDestination.HOME, WidgetActions.forStats(contentReady = false, dueCount = 3).destination)
    }

    @Test
    fun `deep link extras are parsed strictly`() {
        assertEquals(WidgetTarget(WidgetDestination.DAILY_REVIEW), WidgetDeepLink.parse("DAILY_REVIEW", null))
        assertEquals(WidgetTarget(WidgetDestination.PRACTICE_WORD, "wf_12"), WidgetDeepLink.parse("PRACTICE_WORD", "wf_12"))
        assertNull(WidgetDeepLink.parse("PRACTICE_WORD", null))
        assertNull(WidgetDeepLink.parse("PRACTICE_WORD", "bad id; drop table"))
        assertNull(WidgetDeepLink.parse("SETTINGS", null))
        assertNull(WidgetDeepLink.parse(null, null))
    }

    // --- Responsive layout choice ---------------------------------------------------------

    private fun fit(kind: WidgetKind, w: Int, h: Int) =
        WidgetSizing.bestFit(w.toFloat(), h.toFloat(), WidgetSizing.breakpoints(kind))

    @Test
    fun `layout follows the widget's size`() {
        // One row tall, any width.
        assertEquals(WidgetSizeClass.COMPACT, fit(WidgetKind.WORD, 320, 60))
        // Two cells square.
        assertEquals(WidgetSizeClass.SMALL, fit(WidgetKind.WORD, 130, 130))
        // Narrow but tall stays small rather than squeezing a wide layout.
        assertEquals(WidgetSizeClass.SMALL, fit(WidgetKind.STATS, 150, 300))
        // The 3x2 / 4x2 defaults.
        assertEquals(WidgetSizeClass.MEDIUM, fit(WidgetKind.WORD, 260, 150))
        assertEquals(WidgetSizeClass.MEDIUM, fit(WidgetKind.STATS, 320, 170))
        assertEquals(WidgetSizeClass.MEDIUM, fit(WidgetKind.COMBINED, 320, 170))
        // 4x3 and up.
        assertEquals(WidgetSizeClass.LARGE, fit(WidgetKind.WORD, 330, 300))
        assertEquals(WidgetSizeClass.LARGE, fit(WidgetKind.STATS, 330, 300))
        assertEquals(WidgetSizeClass.LARGE, fit(WidgetKind.COMBINED, 330, 300))
    }

    @Test
    fun `an unreported size uses the default layout and a too-small one the smallest`() {
        assertEquals(WidgetSizing.DEFAULT, fit(WidgetKind.WORD, 0, 0))
        assertEquals(WidgetSizeClass.COMPACT, fit(WidgetKind.COMBINED, 100, 30))
    }

    @Test
    fun `every widget kind offers all four layouts`() {
        for (kind in WidgetKind.entries) {
            assertEquals(WidgetSizeClass.entries.toSet(), WidgetSizing.breakpoints(kind).map { it.sizeClass }.toSet())
        }
    }

    // --- Formatting -----------------------------------------------------------------------

    @Test
    fun `widget numbers use each language's own digits`() {
        val expected = mapOf(
            Language.ENGLISH to "507",
            Language.BANGLA to "৫০৭",
            Language.URDU to "۵۰۷",
            Language.PERSIAN to "۵۰۷",
            Language.HINDI to "५०७",
            Language.INDONESIAN to "507",
            Language.TURKISH to "507",
            Language.FRENCH to "507"
        )
        assertEquals(8, Language.entries.size)
        for (lang in Language.entries) {
            assertEquals("digits for $lang", expected[lang], WidgetFormat.number(507, lang))
        }
        assertEquals("1,234", WidgetFormat.number(1234, Language.ENGLISH))
    }

    @Test
    fun `verse citations on widgets are fully localized`() {
        val bn = VerseReferenceFormatter.format("Taha 20:34", Language.BANGLA)
        assertTrue(bn, bn.startsWith(VerseReferenceFormatter.surahWord(Language.BANGLA)))
        assertTrue(bn, bn.endsWith("২০:৩৪"))
        assertFalse(bn, bn.contains("Taha"))
        assertTrue(VerseReferenceFormatter.format("20:34", Language.URDU).endsWith("۲۰:۳۴"))
    }

    @Test
    fun `goal and level fractions map to drawable levels`() {
        assertEquals(0, WidgetFormat.level(WidgetFormat.goalFraction(0, 10)))
        assertEquals(5_000, WidgetFormat.level(WidgetFormat.goalFraction(5, 10)))
        assertEquals(10_000, WidgetFormat.level(WidgetFormat.goalFraction(25, 10)))
        assertEquals(0, WidgetFormat.level(WidgetFormat.goalFraction(5, 0)))
        assertEquals(10_000, WidgetFormat.level(2f))
    }

    @Test
    fun `goal met needs a real goal`() {
        assertTrue(WidgetStats.EMPTY.copy(todayMinutes = 10, goalMinutes = 10).goalMet)
        assertFalse(WidgetStats.EMPTY.copy(todayMinutes = 9, goalMinutes = 10).goalMet)
        assertFalse(WidgetStats.EMPTY.copy(todayMinutes = 0, goalMinutes = 0).goalMet)
    }

    // --- Scheduling -----------------------------------------------------------------------

    @Test
    fun `the midnight refresh lands just after the next local midnight`() {
        val zone = ZoneId.of("Asia/Dhaka")
        val now = ZonedDateTime.of(2026, 10, 3, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        val next = ZonedDateTime.of(2026, 10, 4, 0, 0, 5, 0, zone).toInstant().toEpochMilli()
        assertEquals(next, WidgetUpdateScheduler.nextLocalMidnightMillis(now, zone))
        val justAfter = ZonedDateTime.of(2026, 10, 4, 0, 0, 1, 0, zone).toInstant().toEpochMilli()
        assertEquals(
            ZonedDateTime.of(2026, 10, 5, 0, 0, 5, 0, zone).toInstant().toEpochMilli(),
            WidgetUpdateScheduler.nextLocalMidnightMillis(justAfter, zone)
        )
    }
}
