package com.quranicwords.app.feature.widget

import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * Chooses the word of the moment. Strict priority: a word due for review always wins (the widget
 * then doubles as a nudge to do the Daily Review), then a practised word that isn't due, then -
 * for a learner who hasn't practised anything yet - the curriculum's next new word. [step] is the
 * widget's own rotation counter (advanced by its "next word" button and the half-hourly refresh),
 * so two widgets on one home screen can show different words.
 */
object WidgetWordPicker {
    fun pick(pools: WordPools, step: Int): WordPick? {
        val safeStep = step.coerceAtLeast(0)
        return when {
            pools.due.isNotEmpty() -> WordPick(pools.due[safeStep % pools.due.size], WordKind.DUE)
            pools.learned.isNotEmpty() -> WordPick(pools.learned[safeStep % pools.learned.size], WordKind.LEARNED)
            pools.nextNew != null -> WordPick(pools.nextNew, WordKind.NEW)
            else -> null
        }
    }

    /** A due word is an active-recall prompt: its meaning stays hidden until the learner taps to
     * reveal it (remembered per widget, for that word only). Other words show their meaning. */
    fun isMeaningHidden(kind: WordKind, wordId: String, revealedWordId: String?): Boolean =
        kind == WordKind.DUE && revealedWordId != wordId
}

enum class WidgetSizeClass { COMPACT, SMALL, MEDIUM, LARGE }

/**
 * Responsive layout choice. On API 31+ the launcher picks from a `RemoteViews(Map<SizeF, ...>)`
 * itself; [bestFit] reproduces that platform rule (closest ideal size that fits, else the
 * smallest) so older launchers - which only report the widget's size in its options - get the
 * same layout for the same size.
 */
object WidgetSizing {
    data class Breakpoint(val sizeClass: WidgetSizeClass, val widthDp: Float, val heightDp: Float)

    fun breakpoints(kind: WidgetKind): List<Breakpoint> = when (kind) {
        WidgetKind.WORD -> listOf(
            Breakpoint(WidgetSizeClass.COMPACT, 110f, 40f),
            Breakpoint(WidgetSizeClass.SMALL, 110f, 100f),
            Breakpoint(WidgetSizeClass.MEDIUM, 180f, 100f),
            Breakpoint(WidgetSizeClass.LARGE, 220f, 250f)
        )
        WidgetKind.STATS -> listOf(
            Breakpoint(WidgetSizeClass.COMPACT, 110f, 40f),
            Breakpoint(WidgetSizeClass.SMALL, 110f, 100f),
            Breakpoint(WidgetSizeClass.MEDIUM, 220f, 100f),
            Breakpoint(WidgetSizeClass.LARGE, 220f, 240f)
        )
        WidgetKind.COMBINED -> listOf(
            Breakpoint(WidgetSizeClass.COMPACT, 160f, 40f),
            Breakpoint(WidgetSizeClass.SMALL, 110f, 100f),
            Breakpoint(WidgetSizeClass.MEDIUM, 220f, 100f),
            Breakpoint(WidgetSizeClass.LARGE, 220f, 260f)
        )
    }

    /** Layout used when the launcher reports no size at all (some pre-12 launchers, first bind). */
    val DEFAULT: WidgetSizeClass = WidgetSizeClass.MEDIUM

    fun bestFit(widthDp: Float, heightDp: Float, breakpoints: List<Breakpoint>): WidgetSizeClass {
        require(breakpoints.isNotEmpty())
        if (widthDp <= 0f || heightDp <= 0f) return DEFAULT
        val fitting = breakpoints.filter { it.widthDp <= widthDp && it.heightDp <= heightDp }
        val chosen = fitting.minByOrNull { sq(it.widthDp - widthDp) + sq(it.heightDp - heightDp) }
            ?: breakpoints.minBy { it.widthDp * it.heightDp }
        return chosen.sizeClass
    }

    private fun sq(v: Float) = v * v
}

/** Number formatting for the widgets - always the learner's own digits (Bangla, Devanagari,
 * Eastern Arabic) with their grouping, via the app's single formatter. */
object WidgetFormat {
    fun number(value: Int, language: Language): String = VerseReferenceFormatter.formatNumber(value, language)

    /** 0..10000, the Drawable level range the ring and bar images are driven by. */
    fun level(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 10_000).toInt()

    fun goalFraction(todayMinutes: Int, goalMinutes: Int): Float =
        if (goalMinutes <= 0) 0f else (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
}

/** Where a widget tap lands in the app - see [WidgetDeepLink]. */
enum class WidgetDestination { HOME, DAILY_REVIEW, PRACTICE_WORD }

data class WidgetTarget(val destination: WidgetDestination, val wordId: String? = null)

/** The single call to action each state gets. */
object WidgetActions {
    /** Tapping the word: a due word goes to the Daily Review, a learned one to a focused review
     * of that word, a new one to the learning path on Home. */
    fun forWord(kind: WordKind, wordId: String): WidgetTarget = when (kind) {
        WordKind.DUE -> WidgetTarget(WidgetDestination.DAILY_REVIEW)
        WordKind.LEARNED -> WidgetTarget(WidgetDestination.PRACTICE_WORD, wordId)
        WordKind.NEW -> WidgetTarget(WidgetDestination.HOME)
    }

    /** The progress widgets' primary button: review whenever anything is due, else keep learning. */
    fun forStats(contentReady: Boolean, dueCount: Int): WidgetTarget = when {
        !contentReady -> WidgetTarget(WidgetDestination.HOME)
        dueCount > 0 -> WidgetTarget(WidgetDestination.DAILY_REVIEW)
        else -> WidgetTarget(WidgetDestination.HOME)
    }
}
