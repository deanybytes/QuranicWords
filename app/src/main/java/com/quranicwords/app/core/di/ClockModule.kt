package com.quranicwords.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    /** Injected so [com.quranicwords.app.core.util.StreakCalculator] is
     * deterministically testable with a fixed [Clock] instead of calling [java.time.LocalDate.now] directly.
     * A [SystemZoneClock] rather than `Clock.systemDefaultZone()`: that one captures the zone once,
     * so after the learner travels or changes their timezone "today" stayed in the old zone until
     * the process died - the singleton lives as long as the app does. */
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemZoneClock()
}

/** The system clock in whatever the device's default zone is *right now* - re-read on every call,
 * so streak/daily-goal "today" follows timezone changes without an app restart. */
class SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
