package com.quranicwords.app.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetUpdateScheduler.updateAllWidgets(context, advanceRotation = true)
                WidgetUpdateScheduler.scheduleNextUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}


object WidgetUpdateScheduler {
    const val INTERVAL_MILLIS = 30 * 60 * 1000L
    private const val REQUEST_CODE = 9021

    fun hasAnyActiveWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        val statsIds = manager.getAppWidgetIds(ComponentName(context, StatsWidgetProvider::class.java))
        val wordIds = manager.getAppWidgetIds(ComponentName(context, WordWidgetProvider::class.java))
        val combinedIds = manager.getAppWidgetIds(ComponentName(context, CombinedWidgetProvider::class.java))
        return (statsIds.isNotEmpty() || wordIds.isNotEmpty() || combinedIds.isNotEmpty())
    }

    /** Inexact, non-wakeup refresh: the rotating word and stats only need to be fresh when the
     * learner glances at the home screen, never worth waking the device for. Lesson completion
     * and the in-app refresh receiver push immediate updates on top of this. */
    fun scheduleNextUpdate(context: Context) {
        if (!hasAnyActiveWidgets(context)) {
            cancelSchedule(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WidgetAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + INTERVAL_MILLIS, pendingIntent)
    }

    fun cancelSchedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WidgetAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        alarmManager.cancel(pendingIntent)
    }

    suspend fun updateAllWidgets(context: Context, advanceRotation: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context) ?: return

        val statsIds = manager.getAppWidgetIds(ComponentName(context, StatsWidgetProvider::class.java))
        if (statsIds.isNotEmpty()) {
            StatsWidgetProvider.updateWidgets(context, manager, statsIds)
        }

        val wordIds = manager.getAppWidgetIds(ComponentName(context, WordWidgetProvider::class.java))
        if (wordIds.isNotEmpty()) {
            WordWidgetProvider.updateWidgets(context, manager, wordIds, advanceRotation = advanceRotation)
        }

        val combinedIds = manager.getAppWidgetIds(ComponentName(context, CombinedWidgetProvider::class.java))
        if (combinedIds.isNotEmpty()) {
            CombinedWidgetProvider.updateWidgets(context, manager, combinedIds, advanceRotation = advanceRotation)
        }
    }
}
