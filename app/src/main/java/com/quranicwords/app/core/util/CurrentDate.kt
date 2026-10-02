package com.quranicwords.app.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/** Longest single wait between re-checks - bounds how stale "today" can get after a manual clock
 * or timezone change, which no midnight computed in advance can anticipate. */
private const val MAX_RECHECK_MILLIS = 15 * 60_000L

/**
 * The learner's local date, re-emitted when it changes (local midnight, or a clock/timezone
 * change noticed on the next re-check) - for screens that would otherwise capture
 * `LocalDate.now()` once and keep showing "today" as yesterday if left open past midnight.
 */
fun currentDateFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(clock)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        val untilMidnight = Duration.between(clock.instant(), nextMidnight).toMillis()
        // +1s so we wake just after midnight rather than a hair before it.
        delay((untilMidnight + 1_000L).coerceIn(1_000L, MAX_RECHECK_MILLIS))
    }
}.distinctUntilChanged()
