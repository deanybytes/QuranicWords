package com.quranicwords.app.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Fired by [WidgetUpdateScheduler]'s alarms: the half-hourly word rotation and the local-midnight
 * refresh that resets the day's streak flame, goal ring and quests. */
class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val advance = intent.action == WidgetUpdateScheduler.ACTION_ROTATE
        WidgetUpdateScheduler.runAsync(this, context) {
            WidgetUpdateScheduler.updateAllWidgets(context, advanceRotation = advance)
            WidgetUpdateScheduler.scheduleNextUpdate(context)
        }
    }
}

/** Clock, timezone, locale and app-update events: the "day" or the strings on screen changed. */
class WidgetSystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        WidgetUpdateScheduler.runAsync(this, context) {
            WidgetUpdateScheduler.updateAllWidgets(context, advanceRotation = false)
            WidgetUpdateScheduler.scheduleNextUpdate(context)
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}

object WidgetUpdateScheduler {
    private const val TAG = "WidgetUpdate"
    const val INTERVAL_MILLIS = 30 * 60 * 1000L
    const val ACTION_ROTATE = "com.quranicwords.app.widget.action.ROTATE"
    const val ACTION_MIDNIGHT = "com.quranicwords.app.widget.action.MIDNIGHT"
    private const val REQUEST_ROTATE = 9021
    private const val REQUEST_MIDNIGHT = 9022
    /** Allowed lateness of the midnight refresh: a window this wide is not an exact alarm. */
    private const val MIDNIGHT_WINDOW_MILLIS = 10 * 60 * 1000L

    /** One refresh at a time: snapshot + rotation state are read and written as a unit. */
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val PROVIDERS: Map<WidgetKind, Class<*>> = mapOf(
        WidgetKind.WORD to WordWidgetProvider::class.java,
        WidgetKind.STATS to StatsWidgetProvider::class.java,
        WidgetKind.COMBINED to CombinedWidgetProvider::class.java
    )

    fun widgetIds(context: Context, manager: AppWidgetManager, kind: WidgetKind): IntArray =
        manager.getAppWidgetIds(ComponentName(context, PROVIDERS.getValue(kind))) ?: IntArray(0)

    fun hasAnyActiveWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return WidgetKind.entries.any { widgetIds(context, manager, it).isNotEmpty() }
    }

    /** Runs [block] off the main thread while keeping [receiver]'s broadcast (and so the
     * process) alive until it finishes - the try/finally guarantees the broadcast is released. */
    fun runAsync(receiver: BroadcastReceiver, context: Context, block: suspend () -> Unit) {
        val pending = receiver.goAsync()
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Widget refresh failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    /** Fire-and-forget refresh for callers outside a broadcast (e.g. the app going to background). */
    fun refreshInBackground(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching { if (hasAnyActiveWidgets(app)) updateAllWidgets(app, advanceRotation = false) }
                .onFailure { Log.w(TAG, "Background widget refresh failed", it) }
        }
    }

    /**
     * Inexact, non-wakeup alarms: the rotating word and stats only need to be fresh when the
     * learner glances at the home screen, never worth waking the device for (a non-wakeup alarm
     * that comes due while asleep is delivered as soon as the screen turns on). Lesson completion,
     * settings changes and the in-app refresh paths push immediate updates on top of this.
     */
    fun scheduleNextUpdate(context: Context) {
        if (!hasAnyActiveWidgets(context)) {
            cancelSchedule(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        alarmManager.set(AlarmManager.RTC, now + INTERVAL_MILLIS, alarmIntent(context, ACTION_ROTATE, REQUEST_ROTATE))
        alarmManager.setWindow(
            AlarmManager.RTC,
            nextLocalMidnightMillis(now, ZoneId.systemDefault()),
            MIDNIGHT_WINDOW_MILLIS,
            alarmIntent(context, ACTION_MIDNIGHT, REQUEST_MIDNIGHT)
        )
    }

    fun cancelSchedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(alarmIntent(context, ACTION_ROTATE, REQUEST_ROTATE))
        alarmManager.cancel(alarmIntent(context, ACTION_MIDNIGHT, REQUEST_MIDNIGHT))
    }

    /** A few seconds past the next local midnight, so "today" has certainly rolled over. */
    fun nextLocalMidnightMillis(nowMillis: Long, zone: ZoneId): Long {
        val today: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 5_000L
    }

    private fun alarmIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, WidgetAlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Refreshes every placed widget from one shared snapshot. Signature kept for the app's
     * existing callers (progress repository, preferences). */
    suspend fun updateAllWidgets(context: Context, advanceRotation: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        mutex.withLock {
            val ids = WidgetKind.entries.associateWith { widgetIds(context, manager, it) }
            if (ids.values.all { it.isEmpty() }) return
            val snapshot = WidgetDataProvider.loadSnapshot(context)
            val store = WidgetStateStore(context)
            ids.forEach { (kind, kindIds) ->
                kindIds.forEach { id ->
                    if (advanceRotation && kind != WidgetKind.STATS) store.advance(id)
                    render(context, manager, kind, id, snapshot, store)
                }
            }
        }
    }

    /** Refreshes just [widgetIds] of one [kind] (a provider callback or a tap on one widget). */
    suspend fun updateWidgets(context: Context, kind: WidgetKind, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        val manager = AppWidgetManager.getInstance(context) ?: return
        mutex.withLock {
            val snapshot = WidgetDataProvider.loadSnapshot(context)
            val store = WidgetStateStore(context)
            widgetIds.forEach { render(context, manager, kind, it, snapshot, store) }
        }
    }

    private suspend fun render(
        context: Context,
        manager: AppWidgetManager,
        kind: WidgetKind,
        widgetId: Int,
        snapshot: WidgetSnapshot,
        store: WidgetStateStore
    ) {
        try {
            val word = if (kind == WidgetKind.STATS) null else WidgetDataProvider.wordFor(context, snapshot, store.step(widgetId))
            WidgetRenderer.push(context, manager, kind, widgetId, snapshot, word, store.revealedWordId(widgetId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // One broken instance must not stop the others from refreshing.
            Log.w(TAG, "Could not render widget $widgetId ($kind)", e)
        }
    }
}
