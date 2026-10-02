package com.quranicwords.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTimerTest {

    private var now = 0L
    private val timer = SessionTimer { now }

    @Test
    fun `nothing is counted before the first content render`() {
        now = 5_000L // loading time
        assertEquals(0L, timer.elapsedMillis())

        timer.start()
        now = 65_000L
        assertEquals(60_000L, timer.elapsedMillis())
    }

    @Test
    fun `backgrounded time is excluded`() {
        timer.start()
        now = 30_000L
        timer.pause()
        now = 3_600_000L // an hour in the background
        timer.resume()
        now = 3_630_000L

        assertEquals(60_000L, timer.elapsedMillis())
    }

    @Test
    fun `starting while backgrounded waits for resume`() {
        timer.pause()
        timer.start()
        now = 10_000L
        assertEquals(0L, timer.elapsedMillis())

        timer.resume()
        now = 25_000L
        assertEquals(15_000L, timer.elapsedMillis())
    }
}
