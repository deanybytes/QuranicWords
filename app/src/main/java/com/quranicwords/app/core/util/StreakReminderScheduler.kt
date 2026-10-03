package com.quranicwords.app.core.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quranicwords.app.core.data.sync.StreakReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
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
        // Only ever called from a Settings action (reminder switched on, or its time changed), so
        // always replace: UPDATE keeps an existing periodic request's original schedule, which left
        // a changed reminder time firing at the old time; CANCEL_AND_REENQUEUE applies the new
        // initial delay.
        workManager.enqueueUniquePeriodicWork(
            StreakReminderWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(StreakReminderWorker.UNIQUE_WORK_NAME)
    }

    /** Zoned (not LocalDateTime) arithmetic so a DST change between now and the next reminder
     * doesn't shift it by an hour; a wall time skipped by a spring-forward gap resolves to the
     * instant just after the gap. */
    private fun initialDelayMillis(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now(clock)
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute)).atZone(now.zone)
        if (!target.isAfter(now)) target = now.toLocalDate().plusDays(1).atTime(LocalTime.of(hour, minute)).atZone(now.zone)
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }
}
