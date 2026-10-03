package com.quranicwords.app.feature.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.os.Bundle

/**
 * Shared lifecycle for the three widgets. Every callback does its work through
 * [WidgetUpdateScheduler.runAsync] (goAsync + try/finally), so the process stays alive until the
 * RemoteViews are pushed and the broadcast is always released.
 */
abstract class QwWidgetProvider(private val kind: WidgetKind) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateScheduler.runAsync(this, context) {
            WidgetUpdateScheduler.updateWidgets(context, kind, appWidgetIds)
            WidgetUpdateScheduler.scheduleNextUpdate(context)
        }
    }

    /** Before API 31 the layout is picked from the reported size, so a resize needs a rebuild;
     * from 31 the launcher chooses among the size-mapped layouts itself. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        WidgetUpdateScheduler.runAsync(this, context) {
            WidgetUpdateScheduler.updateWidgets(context, kind, intArrayOf(appWidgetId))
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetStateStore(context).forget(appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (!WidgetUpdateScheduler.hasAnyActiveWidgets(context)) {
            WidgetUpdateScheduler.cancelSchedule(context)
        }
    }
}

/** Word of the Moment. */
class WordWidgetProvider : QwWidgetProvider(WidgetKind.WORD)

/** Progress: streak, goal, level, hearts, quests, review CTA. */
class StatsWidgetProvider : QwWidgetProvider(WidgetKind.STATS)

/** Word & Progress. */
class CombinedWidgetProvider : QwWidgetProvider(WidgetKind.COMBINED)
