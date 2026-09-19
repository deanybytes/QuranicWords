package com.quranicwords.app.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.quranicwords.app.MainActivity
import com.quranicwords.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            updateWidgets(context, appWidgetManager, appWidgetIds)
            WidgetUpdateScheduler.scheduleNextUpdate(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (!WidgetUpdateScheduler.hasAnyActiveWidgets(context)) {
            WidgetUpdateScheduler.cancelSchedule(context)
        }
    }

    companion object {
        suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            isReflected: Boolean = false
        ) {
            val snapshot = WidgetDataProvider.getWidgetData(context, advanceRotation = false)
            val stats = snapshot.stats

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                101,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_stats)

                // Background state (normal glass vs reflection sheen)
                views.setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    if (isReflected) R.drawable.widget_glass_bg_reflected else R.drawable.widget_glass_bg
                )

                // Streak
                views.setTextViewText(R.id.tv_streak, "${stats.streakDays}d")

                // Words learned
                views.setTextViewText(R.id.tv_words_count, "${stats.wordsLearnedCount}")
                views.setTextViewText(
                    R.id.tv_words_pct,
                    context.getString(R.string.widget_words_learned_label) + " (${String.format(
                        Locale.US, "%.1f", stats.wordsLearnedPct)}%)"
                )

                // Accuracy
                views.setTextViewText(R.id.tv_accuracy, "${stats.accuracyPct}%")

                // Daily Goal
                views.setTextViewText(
                    R.id.tv_daily_goal,
                    "${stats.todayPracticeMinutes} / ${stats.dailyGoalMinutes}m"
                )
                views.setProgressBar(R.id.pb_daily_goal, 100, stats.dailyGoalProgressPct, false)

                // To Review
                views.setTextViewText(R.id.tv_to_review, "${stats.reviewCount}")

                // Click handlers
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                val refreshPendingIntent = WidgetRefreshReceiver.getRefreshPendingIntent(
                    context = context,
                    widgetId = widgetId,
                    providerClass = StatsWidgetProvider::class.java
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
