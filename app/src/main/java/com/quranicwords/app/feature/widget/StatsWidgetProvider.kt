package com.quranicwords.app.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.quranicwords.app.MainActivity
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.util.VerseReferenceFormatter
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
            val lang = snapshot.language
            val localizedContext = context.getLocalizedWidgetContext(lang)

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
                val streakDigits = VerseReferenceFormatter.formatDigits(stats.streakDays.toString(), lang)
                val streakUnit = when (lang) {
                    Language.BANGLA -> " দিন"
                    Language.URDU, Language.PERSIAN -> " دن"
                    Language.HINDI -> " দিন"
                    Language.FRENCH -> "j"
                    Language.TURKISH -> "g"
                    else -> "d"
                }
                views.setTextViewText(R.id.tv_streak, "$streakDigits$streakUnit")
                views.setContentDescription(
                    R.id.ll_streak_badge,
                    localizedContext.getString(
                        if (stats.streakDays > 0) R.string.widget_streak_days else R.string.widget_streak_zero,
                        stats.streakDays
                    )
                )

                // Words learned
                val wordsCountFormatted = VerseReferenceFormatter.formatDigits(stats.wordsLearnedCount.toString(), lang)
                views.setTextViewText(R.id.tv_words_count, wordsCountFormatted)
                val formattedPct = VerseReferenceFormatter.formatDigits(
                    String.format(Locale.forLanguageTag(lang.tag), "%.1f", stats.wordsLearnedPct),
                    lang
                )
                val wordsLearnedLabel = localizedContext.getString(R.string.widget_words_learned_label)
                views.setTextViewText(R.id.tv_words_pct, "$wordsLearnedLabel ($formattedPct%)")

                // Accuracy
                val accuracyFormatted = VerseReferenceFormatter.formatDigits(stats.accuracyPct.toString(), lang)
                views.setTextViewText(R.id.tv_accuracy, "$accuracyFormatted%")
                views.setTextViewText(R.id.tv_accuracy_label, localizedContext.getString(R.string.widget_accuracy_label))

                // Daily Goal
                val todayMinutesFormatted = VerseReferenceFormatter.formatDigits(stats.todayPracticeMinutes.toString(), lang)
                val goalMinutesFormatted = VerseReferenceFormatter.formatDigits(stats.dailyGoalMinutes.toString(), lang)
                val minUnit = when (lang) {
                    Language.BANGLA -> "মি"
                    Language.URDU, Language.PERSIAN -> "منٹ"
                    else -> "m"
                }
                views.setTextViewText(
                    R.id.tv_daily_goal,
                    "$todayMinutesFormatted / $goalMinutesFormatted$minUnit"
                )
                views.setProgressBar(R.id.pb_daily_goal, 100, stats.dailyGoalProgressPct, false)
                views.setTextViewText(R.id.tv_daily_goal_label, localizedContext.getString(R.string.widget_daily_goal_label))

                // To Review
                val reviewFormatted = VerseReferenceFormatter.formatDigits(stats.reviewCount.toString(), lang)
                views.setTextViewText(R.id.tv_to_review, reviewFormatted)
                views.setTextViewText(R.id.tv_review_label, localizedContext.getString(R.string.widget_review_label))

                // Click handlers
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                val refreshPendingIntent = WidgetRefreshReceiver.getRefreshPendingIntent(
                    context = context,
                    widgetId = widgetId,
                    providerClass = StatsWidgetProvider::class.java
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
                views.setContentDescription(R.id.btn_refresh, localizedContext.getString(R.string.widget_refresh))

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
