package com.quranicwords.app.core.domain

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * XP -> level. XP is the learner's existing `user_stats.totalPoints`; reaching level L costs
 * `50·L·(L−1)` XP in total, so each level needs 100 XP more than the one before (level 2 at 100,
 * level 3 at 300, level 4 at 600...). Inverting that quadratic gives
 * `level = floor((1 + sqrt(1 + 0.08·xp)) / 2)`. Pure and total - negative XP is treated as zero.
 */
object LevelCurve {

    fun levelFor(xp: Int): Int {
        val safeXp = xp.coerceAtLeast(0)
        var level = floor((1 + sqrt(1 + 0.08 * safeXp)) / 2).toInt().coerceAtLeast(1)
        // Guard the floating-point floor at exact boundaries in both directions.
        while (xpForLevel(level + 1) <= safeXp) level++
        while (level > 1 && xpForLevel(level) > safeXp) level--
        return level
    }

    /** Total XP at which [level] is reached (level 1 = 0). */
    fun xpForLevel(level: Int): Int {
        val l = level.coerceAtLeast(1).toLong()
        return (50L * l * (l - 1)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /** Where [xp] sits inside its current level, for the XP progress bar. */
    fun progressFor(xp: Int): LevelProgress {
        val level = levelFor(xp)
        val floorXp = xpForLevel(level)
        val nextXp = xpForLevel(level + 1)
        return LevelProgress(level = level, xpIntoLevel = xp.coerceAtLeast(0) - floorXp, xpForNextLevel = nextXp - floorXp)
    }
}

data class LevelProgress(val level: Int, val xpIntoLevel: Int, val xpForNextLevel: Int) {
    val fraction: Float get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}
