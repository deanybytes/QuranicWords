package com.quranicwords.app.core.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quranicwords.app.MainActivity
import com.quranicwords.app.R

/**
 * Posts the "streak at risk" local reminder - see [StreakReminderWorker]/[StreakReminderScheduler].
 * Entirely on-device: no remote push service, no server-side trigger, consistent with the app's
 * offline-first design. Channel creation is idempotent (recreating with unchanged settings is a
 * documented no-op), so callers don't need to track whether it's already been created.
 */
object StreakReminderNotifications {
    private const val CHANNEL_ID = "streak_reminder"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_streak_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_streak_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Returns false if notifications can't actually be shown (permission denied on API 33+, or
     * the user disabled the channel/app notifications at the OS level) - callers use this to skip
     * the rest of a worker run's cost rather than posting into the void. */
    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** [canNotify] is checked here (not just left to callers) so this function is safe to call
     * unconditionally - the permission check IS the guard `NotificationManagerCompat.notify`'s
     * lint annotation asks for, just not one lint's simple pattern-matching can see across this
     * function boundary. */
    @SuppressLint("MissingPermission")
    fun notifyStreakAtRisk(context: Context, currentStreak: Int) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_streak)
            .setContentTitle(context.getString(R.string.notification_streak_title))
            .setContentText(context.getString(R.string.notification_streak_body, currentStreak))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
