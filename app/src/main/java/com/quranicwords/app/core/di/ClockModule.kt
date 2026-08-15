package com.quranicwords.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    /** Injected so [com.quranicwords.app.core.util.StreakCalculator] is
     * deterministically testable with a fixed [Clock] instead of calling [java.time.LocalDate.now] directly. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
