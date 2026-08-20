package com.quranicwords.app.core.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quranicwords.app.core.data.sync.StreakReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules/cancels [StreakReminderWorker] as a daily [androidx.work.PeriodicWorkRequest] rather
 * than `AlarmManager` + a `BOOT_COMPLETED` receiver - WorkManager persists periodic work across
 * process death and reboots on its own, so no receiver/manifest entry is needed. Trade-off worth
 * being explicit about: WorkManager guarantees a *minimum* interval, not a wall-clock-exact fire
 * time (Doze/battery optimization can shift it by minutes), which is an acceptable fit for a
 * "reminder" and avoids the `SCHEDULE_EXACT_ALARM` permission a stricter guarantee would need.
 */
@Singleton
class StreakReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .build()
        // UPDATE (not KEEP) so changing the reminder time in Settings actually reschedules the
        // next run instead of leaving the old time in effect until the work happens to re-enqueue.
        workManager.enqueueUniquePeriodicWork(
            StreakReminderWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(StreakReminderWorker.UNIQUE_WORK_NAME)
    }

    private fun initialDelayMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now(clock)
        var target = LocalDateTime.of(LocalDate.now(clock), LocalTime.of(hour, minute))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }
}
