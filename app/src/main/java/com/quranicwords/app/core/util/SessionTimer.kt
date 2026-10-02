package com.quranicwords.app.core.util

/**
 * Active-time stopwatch for a practice session: starts on [start] (first content render, not
 * screen/ViewModel creation - loading time isn't practice), and stops accumulating between
 * [pause]/[resume] (app backgrounded), so a lesson left open overnight doesn't credit hours of
 * daily-goal minutes. [now] is injected for tests. Not thread-safe; drive it from the main thread.
 */
class SessionTimer(private val now: () -> Long) {
    private var accumulatedMillis = 0L
    private var runningSince: Long? = null
    private var started = false
    private var paused = false

    fun start() {
        if (started) return
        started = true
        if (!paused) runningSince = now()
    }

    fun pause() {
        paused = true
        runningSince?.let { accumulatedMillis += (now() - it).coerceAtLeast(0L) }
        runningSince = null
    }

    fun resume() {
        paused = false
        if (started && runningSince == null) runningSince = now()
    }

    fun elapsedMillis(): Long =
        accumulatedMillis + (runningSince?.let { (now() - it).coerceAtLeast(0L) } ?: 0L)
}
