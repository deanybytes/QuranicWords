package com.quranicwords.app.core.domain

/** A learner's hearts as of [anchorMillis] - the instant regeneration is measured from. */
data class HeartsState(val hearts: Int, val anchorMillis: Long) {
    val isFull: Boolean get() = hearts >= HeartsCalculator.MAX_HEARTS
}

/**
 * Hearts: a soft limit on starting new Learn lessons after many first-try mistakes. Stored as a
 * count plus the instant it was last true ([HeartsState.anchorMillis]); regeneration since then
 * is derived on read, never written on a timer. Pure and clock-free (callers pass `now`).
 *
 * Clock-rollback safe: a `now` earlier than the anchor grants nothing and re-anchors at `now`, so
 * setting the clock back can't mint hearts and setting it forward again only refills at the
 * normal rate from that point - and a full set never banks time towards hearts beyond the max.
 */
object HeartsCalculator {
    const val MAX_HEARTS = 5
    const val REGEN_MILLIS = 30L * 60 * 1000

    /** [stored] hearts anchored at [anchorMillis], brought forward to [nowMillis]. */
    fun current(stored: Int, anchorMillis: Long, nowMillis: Long): HeartsState {
        val clamped = stored.coerceIn(0, MAX_HEARTS)
        if (nowMillis < anchorMillis) return HeartsState(clamped, nowMillis)
        if (clamped >= MAX_HEARTS) return HeartsState(MAX_HEARTS, nowMillis)
        val regenerated = ((nowMillis - anchorMillis) / REGEN_MILLIS).toInt()
        val hearts = (clamped + regenerated).coerceAtMost(MAX_HEARTS)
        val anchor = if (hearts >= MAX_HEARTS) nowMillis else anchorMillis + regenerated * REGEN_MILLIS
        return HeartsState(hearts, anchor)
    }

    /** One first-try mistake. The regeneration clock starts at the moment the first heart goes. */
    fun lose(stored: Int, anchorMillis: Long, nowMillis: Long): HeartsState {
        val now = current(stored, anchorMillis, nowMillis)
        if (now.hearts <= 0) return now
        return HeartsState(now.hearts - 1, if (now.isFull) nowMillis else now.anchorMillis)
    }

    /** A completed review/practice session's refill, never above [MAX_HEARTS]. */
    fun gain(stored: Int, anchorMillis: Long, nowMillis: Long, amount: Int = 1): HeartsState {
        val now = current(stored, anchorMillis, nowMillis)
        val hearts = (now.hearts + amount).coerceAtMost(MAX_HEARTS)
        return HeartsState(hearts, if (hearts >= MAX_HEARTS) nowMillis else now.anchorMillis)
    }

    /** Time until the next heart regenerates, or null when already full. */
    fun millisUntilNext(stored: Int, anchorMillis: Long, nowMillis: Long): Long? {
        val now = current(stored, anchorMillis, nowMillis)
        if (now.isFull) return null
        return (now.anchorMillis + REGEN_MILLIS - nowMillis).coerceIn(0L, REGEN_MILLIS)
    }
}
