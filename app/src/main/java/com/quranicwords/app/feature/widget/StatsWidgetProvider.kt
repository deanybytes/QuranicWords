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
import com.quranicwords.app.core.domain.model.ThemeMode
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

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                101,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val systemIsNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            for (widgetId in appWidgetIds) {
                val refreshPendingIntent = WidgetRefreshReceiver.getRefreshPendingIntent(
                    context = context,
                    widgetId = widgetId,
                    providerClass = StatsWidgetProvider::class.java
                )

                val views = when (snapshot.themeMode) {
                    ThemeMode.DARK -> buildRemoteViews(context, snapshot, isNight = true, isReflected, openAppPendingIntent, refreshPendingIntent)
                    ThemeMode.LIGHT -> buildRemoteViews(context, snapshot, isNight = false, isReflected, openAppPendingIntent, refreshPendingIntent)
                    ThemeMode.SYSTEM -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val light = buildRemoteViews(context, snapshot, isNight = false, isReflected, openAppPendingIntent, refreshPendingIntent)
                            val dark = buildRemoteViews(context, snapshot, isNight = true, isReflected, openAppPendingIntent, refreshPendingIntent)
                            RemoteViews(light, dark)
                        } else {
                            buildRemoteViews(context, snapshot, isNight = systemIsNight, isReflected, openAppPendingIntent, refreshPendingIntent)
                        }
                    }
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            snapshot: WidgetSnapshot,
            isNight: Boolean,
            isReflected: Boolean,
            openAppPendingIntent: PendingIntent,
            refreshPendingIntent: PendingIntent
        ): RemoteViews {
            val stats = snapshot.stats
            val lang = snapshot.language
            val themeMode = if (isNight) ThemeMode.DARK else ThemeMode.LIGHT
            val themedContext = context.getThemedAndLocalizedWidgetContext(lang, themeMode)

            val views = RemoteViews(context.packageName, R.layout.widget_stats)

            // Background state
            val bgRes = if (isReflected) {
                if (isNight) R.drawable.widget_glass_bg_reflected_dark else R.drawable.widget_glass_bg_reflected_light
            } else {
                if (isNight) R.drawable.widget_glass_bg_dark else R.drawable.widget_glass_bg_light
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

            // Header styling
            views.setTextColor(R.id.tv_app_title, themedContext.getColor(R.color.widget_text_primary))
            views.setInt(
                R.id.ll_streak_badge,
                "setBackgroundResource",
                if (isNight) R.drawable.widget_badge_bg_dark else R.drawable.widget_badge_bg_light
            )
            views.setInt(
                R.id.btn_refresh,
                "setBackgroundResource",
                if (isNight) R.drawable.widget_button_glass_dark else R.drawable.widget_button_glass_light
            )

            // Card backgrounds
            val cardBg = if (isNight) R.drawable.widget_surface_card_dark else R.drawable.widget_surface_card_light
            views.setInt(R.id.ll_words_card, "setBackgroundResource", cardBg)
            views.setInt(R.id.ll_accuracy_card, "setBackgroundResource", cardBg)
            views.setInt(R.id.ll_daily_goal_card, "setBackgroundResource", cardBg)
            views.setInt(R.id.ll_review_card, "setBackgroundResource", cardBg)

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
            views.setTextColor(R.id.tv_streak, themedContext.getColor(R.color.widget_accent_gold))
            views.setContentDescription(
                R.id.ll_streak_badge,
                themedContext.getString(
                    if (stats.streakDays > 0) R.string.widget_streak_days else R.string.widget_streak_zero,
                    stats.streakDays
                )
            )

            // Words learned
            val wordsCountFormatted = VerseReferenceFormatter.formatDigits(stats.wordsLearnedCount.toString(), lang)
            views.setTextViewText(R.id.tv_words_count, wordsCountFormatted)
            views.setTextColor(R.id.tv_words_count, themedContext.getColor(R.color.widget_text_primary))

            val formattedPct = VerseReferenceFormatter.formatDigits(
                String.format(Locale.forLanguageTag(lang.tag), "%.1f", stats.wordsLearnedPct),
                lang
            )
            val wordsLearnedLabel = themedContext.getString(R.string.widget_words_learned_label)
            views.setTextViewText(R.id.tv_words_pct, "$wordsLearnedLabel ($formattedPct%)")
            views.setTextColor(R.id.tv_words_pct, themedContext.getColor(R.color.widget_text_secondary))

            // Accuracy
            val accuracyFormatted = VerseReferenceFormatter.formatDigits(stats.accuracyPct.toString(), lang)
            views.setTextViewText(R.id.tv_accuracy, "$accuracyFormatted%")
            views.setTextColor(R.id.tv_accuracy, themedContext.getColor(R.color.widget_accent_primary))
            views.setTextViewText(R.id.tv_accuracy_label, themedContext.getString(R.string.widget_accuracy_label))
            views.setTextColor(R.id.tv_accuracy_label, themedContext.getColor(R.color.widget_text_secondary))

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
            views.setTextColor(R.id.tv_daily_goal, themedContext.getColor(R.color.widget_text_primary))
            views.setProgressBar(R.id.pb_daily_goal, 100, stats.dailyGoalProgressPct, false)
            views.setTextViewText(R.id.tv_daily_goal_label, themedContext.getString(R.string.widget_daily_goal_label))
            views.setTextColor(R.id.tv_daily_goal_label, themedContext.getColor(R.color.widget_text_secondary))

            // To Review
            val reviewFormatted = VerseReferenceFormatter.formatDigits(stats.reviewCount.toString(), lang)
            views.setTextViewText(R.id.tv_to_review, reviewFormatted)
            views.setTextColor(R.id.tv_to_review, themedContext.getColor(R.color.widget_mistake_tag_text))
            views.setTextViewText(R.id.tv_review_label, themedContext.getString(R.string.widget_review_label))
            views.setTextColor(R.id.tv_review_label, themedContext.getColor(R.color.widget_text_secondary))

            // Click handlers
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            views.setContentDescription(R.id.btn_refresh, themedContext.getString(R.string.widget_refresh))

            return views
        }
    }
}

