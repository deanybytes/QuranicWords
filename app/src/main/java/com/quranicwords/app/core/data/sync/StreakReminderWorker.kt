package com.quranicwords.app.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quranicwords.app.core.data.CurrentUserIdProvider
import com.quranicwords.app.core.data.datastore.UserPreferencesDataStore
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.util.StreakReminderNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/**
 * Fires roughly once a day (see [com.quranicwords.app.core.util.StreakReminderScheduler]) while
 * the user has the streak reminder enabled. Only notifies when there's an actual streak at risk -
 * `currentStreak > 0` and today's activity hasn't happened yet - never a generic "come back" nag
 * for a learner with no streak to protect, matching QW-24's "streak-at-risk" scope exactly.
 */
@HiltWorker
class StreakReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore,
    private val currentUserIdProvider: CurrentUserIdProvider,
    private val progressRepository: ProgressRepository,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Defensive re-check: the periodic work is cancelled the moment the setting is turned
        // off, but a run already queued at that exact moment could still land here.
        if (!preferences.streakReminderEnabledFlow.first()) return Result.success()

        val userId = currentUserIdProvider.get()
        val stats = progressRepository.observeStats(userId).first() ?: return Result.success()
        if (stats.currentStreak <= 0) return Result.success()

        val today = LocalDate.now(clock)
        val lastActivity = stats.lastActivityLocalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (lastActivity == today) return Result.success()

        StreakReminderNotifications.notifyStreakAtRisk(applicationContext, stats.currentStreak)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "streak_reminder"
    }
}
