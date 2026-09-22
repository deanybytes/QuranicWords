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
            val word = snapshot.currentWord
            val lang = snapshot.language
            val localizedContext = context.getLocalizedWidgetContext(lang)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                102,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_word)

                // Background state (normal glass vs reflection sheen)
                views.setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    if (isReflected) R.drawable.widget_glass_bg_reflected else R.drawable.widget_glass_bg
                )

                if (word == null) {
                    // Empty state
                    views.setViewVisibility(R.id.ll_empty_state, View.VISIBLE)
                    views.setViewVisibility(R.id.ll_word_content, View.GONE)
                    views.setViewVisibility(R.id.tv_status_badge, View.GONE)
                    views.setTextViewText(R.id.tv_empty_words_title, localizedContext.getString(R.string.widget_empty_words_title))
                    views.setTextViewText(R.id.tv_empty_words_desc, localizedContext.getString(R.string.widget_empty_words_desc))
                } else {
                    views.setViewVisibility(R.id.ll_empty_state, View.GONE)
                    views.setViewVisibility(R.id.ll_word_content, View.VISIBLE)
                    views.setViewVisibility(R.id.tv_status_badge, View.VISIBLE)

                    // Badge: Needs Review vs Mastered
                    if (word.isMistaken) {
                        views.setTextViewText(R.id.tv_status_badge, localizedContext.getString(R.string.widget_mistake_badge))
                        views.setTextColor(R.id.tv_status_badge, context.getColor(R.color.widget_mistake_tag_text))
                    } else {
                        views.setTextViewText(R.id.tv_status_badge, localizedContext.getString(R.string.widget_mastered_badge))
                        views.setTextColor(R.id.tv_status_badge, context.getColor(R.color.widget_mastered_tag_text))
                    }

                    // Word and Meaning
                    views.setTextViewText(R.id.tv_arabic_word, word.arabicWord)
                    views.setTextViewText(R.id.tv_word_meaning, word.meaning)

                    // Stats: occurrences, % of Quran, frequency rank
                    val occText = localizedContext.getString(R.string.widget_occurrences_format, word.occurrenceCount)
                    views.setTextViewText(
                        R.id.tv_occurrences,
                        VerseReferenceFormatter.formatDigits(occText, lang)
                    )
                    val quranPctText = localizedContext.getString(R.string.widget_quran_pct_format, word.quranPercentage)
                    views.setTextViewText(
                        R.id.tv_quran_pct,
                        VerseReferenceFormatter.formatDigits(quranPctText, lang)
                    )
                    val rankText = localizedContext.getString(R.string.widget_rank_format, word.frequencyRank)
                    views.setTextViewText(
                        R.id.tv_freq_rank,
                        VerseReferenceFormatter.formatDigits(rankText, lang)
                    )

                    // Example Verse
                    if (word.exampleVerseArabic != null && word.exampleVerseReference != null) {
                        views.setViewVisibility(R.id.ll_verse_box, View.VISIBLE)
                        views.setTextViewText(
                            R.id.tv_verse_ref,
                            VerseReferenceFormatter.format(word.exampleVerseReference, lang)
                        )
                        views.setTextViewText(R.id.tv_verse_arabic, word.exampleVerseArabic)
                    } else {
                        views.setViewVisibility(R.id.ll_verse_box, View.GONE)
                    }
                }

                // Click handlers
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                val refreshPendingIntent = WidgetRefreshReceiver.getRefreshPendingIntent(
                    context = context,
                    widgetId = widgetId,
                    providerClass = WordWidgetProvider::class.java
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
                views.setContentDescription(R.id.btn_refresh, localizedContext.getString(R.string.widget_refresh))

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
