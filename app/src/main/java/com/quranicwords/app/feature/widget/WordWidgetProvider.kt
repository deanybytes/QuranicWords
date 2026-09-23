package com.quranicwords.app.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.quranicwords.app.MainActivity
import com.quranicwords.app.R
import com.quranicwords.app.core.util.VerseReferenceFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import com.quranicwords.app.core.domain.model.ThemeMode

class WordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            updateWidgets(context, appWidgetManager, appWidgetIds, advanceRotation = false)
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
            advanceRotation: Boolean = false,
            isReflected: Boolean = false
        ) {
            val snapshot = WidgetDataProvider.getWidgetData(context, advanceRotation = advanceRotation)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                102,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val systemIsNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            for (widgetId in appWidgetIds) {
                val refreshPendingIntent = WidgetRefreshReceiver.getRefreshPendingIntent(
                    context = context,
                    widgetId = widgetId,
                    providerClass = WordWidgetProvider::class.java
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
            val word = snapshot.currentWord
            val lang = snapshot.language
            val themeMode = if (isNight) ThemeMode.DARK else ThemeMode.LIGHT
            val themedContext = context.getThemedAndLocalizedWidgetContext(lang, themeMode)

            val views = RemoteViews(context.packageName, R.layout.widget_word)

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
                R.id.btn_refresh,
                "setBackgroundResource",
                if (isNight) R.drawable.widget_button_glass_dark else R.drawable.widget_button_glass_light
            )

            if (word == null) {
                // Empty state
                views.setViewVisibility(R.id.ll_empty_state, View.VISIBLE)
                views.setViewVisibility(R.id.ll_word_content, View.GONE)
                views.setViewVisibility(R.id.tv_status_badge, View.GONE)
                views.setTextViewText(R.id.tv_empty_words_title, themedContext.getString(R.string.widget_empty_words_title))
                views.setTextColor(R.id.tv_empty_words_title, themedContext.getColor(R.color.widget_text_primary))
                views.setTextViewText(R.id.tv_empty_words_desc, themedContext.getString(R.string.widget_empty_words_desc))
                views.setTextColor(R.id.tv_empty_words_desc, themedContext.getColor(R.color.widget_text_secondary))
            } else {
                views.setViewVisibility(R.id.ll_empty_state, View.GONE)
                views.setViewVisibility(R.id.ll_word_content, View.VISIBLE)
                views.setViewVisibility(R.id.tv_status_badge, View.VISIBLE)

                // Badge styling
                views.setInt(
                    R.id.tv_status_badge,
                    "setBackgroundResource",
                    if (isNight) R.drawable.widget_badge_bg_dark else R.drawable.widget_badge_bg_light
                )
                if (word.isMistaken) {
                    views.setTextViewText(R.id.tv_status_badge, themedContext.getString(R.string.widget_mistake_badge))
                    views.setTextColor(R.id.tv_status_badge, themedContext.getColor(R.color.widget_mistake_tag_text))
                } else {
                    views.setTextViewText(R.id.tv_status_badge, themedContext.getString(R.string.widget_mastered_badge))
                    views.setTextColor(R.id.tv_status_badge, themedContext.getColor(R.color.widget_mastered_tag_text))
                }

                // Hero Card styling
                views.setInt(
                    R.id.ll_hero_card,
                    "setBackgroundResource",
                    if (isNight) R.drawable.widget_surface_card_dark else R.drawable.widget_surface_card_light
                )
                views.setTextViewText(R.id.tv_arabic_word, word.arabicWord)
                views.setTextColor(R.id.tv_arabic_word, themedContext.getColor(R.color.widget_text_primary))
                views.setTextViewText(R.id.tv_word_meaning, word.meaning)
                views.setTextColor(R.id.tv_word_meaning, themedContext.getColor(R.color.widget_accent_primary))

                // Stats Pills
                val pillBg = if (isNight) R.drawable.widget_badge_bg_dark else R.drawable.widget_badge_bg_light
                views.setInt(R.id.tv_occurrences, "setBackgroundResource", pillBg)
                views.setInt(R.id.tv_quran_pct, "setBackgroundResource", pillBg)
                views.setInt(R.id.tv_freq_rank, "setBackgroundResource", pillBg)

                val occText = themedContext.getString(R.string.widget_occurrences_format, word.occurrenceCount)
                views.setTextViewText(R.id.tv_occurrences, VerseReferenceFormatter.formatDigits(occText, lang))
                views.setTextColor(R.id.tv_occurrences, themedContext.getColor(R.color.widget_text_secondary))

                val quranPctText = themedContext.getString(R.string.widget_quran_pct_format, word.quranPercentage)
                views.setTextViewText(R.id.tv_quran_pct, VerseReferenceFormatter.formatDigits(quranPctText, lang))
                views.setTextColor(R.id.tv_quran_pct, themedContext.getColor(R.color.widget_text_secondary))

                val rankText = themedContext.getString(R.string.widget_rank_format, word.frequencyRank)
                views.setTextViewText(R.id.tv_freq_rank, VerseReferenceFormatter.formatDigits(rankText, lang))
                views.setTextColor(R.id.tv_freq_rank, themedContext.getColor(R.color.widget_text_secondary))

                // Example Verse Box
                if (word.exampleVerseArabic != null && word.exampleVerseReference != null) {
                    views.setViewVisibility(R.id.ll_verse_box, View.VISIBLE)
                    views.setInt(
                        R.id.ll_verse_box,
                        "setBackgroundResource",
                        if (isNight) R.drawable.widget_surface_card_dark else R.drawable.widget_surface_card_light
                    )
                    views.setTextViewText(
                        R.id.tv_verse_ref,
                        VerseReferenceFormatter.format(word.exampleVerseReference, lang)
                    )
                    views.setTextColor(R.id.tv_verse_ref, themedContext.getColor(R.color.widget_accent_primary))
                    views.setTextViewText(R.id.tv_verse_arabic, word.exampleVerseArabic)
                    views.setTextColor(R.id.tv_verse_arabic, themedContext.getColor(R.color.widget_text_primary))
                } else {
                    views.setViewVisibility(R.id.ll_verse_box, View.GONE)
                }
            }

            // Click handlers
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            views.setContentDescription(R.id.btn_refresh, themedContext.getString(R.string.widget_refresh))

            return views
        }
    }
}
