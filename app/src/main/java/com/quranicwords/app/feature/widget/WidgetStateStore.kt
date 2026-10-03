package com.quranicwords.app.feature.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Per-widget-instance UI state: the rotation step (which word of its pool the widget shows) and
 * which word's meaning the learner has revealed. Device-local presentation state only - never
 * learner progress - so plain SharedPreferences, outside the backup/progress model.
 */
class WidgetStateStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun step(widgetId: Int): Int = prefs.getInt(stepKey(widgetId), 0)

    fun advance(widgetId: Int) {
        prefs.edit { putInt(stepKey(widgetId), (step(widgetId) + 1) % STEP_WRAP) }
    }

    fun revealedWordId(widgetId: Int): String? = prefs.getString(revealKey(widgetId), null)

    /** Reveals [wordId] on [widgetId], or hides it again if it is already revealed. */
    fun toggleReveal(widgetId: Int, wordId: String) {
        prefs.edit {
            if (revealedWordId(widgetId) == wordId) remove(revealKey(widgetId)) else putString(revealKey(widgetId), wordId)
        }
    }

    fun forget(widgetIds: IntArray) {
        prefs.edit {
            widgetIds.forEach {
                remove(stepKey(it))
                remove(revealKey(it))
            }
        }
    }

    private fun stepKey(id: Int) = "step_$id"
    private fun revealKey(id: Int) = "revealed_$id"

    private companion object {
        const val PREFS_NAME = "quranic_words_widget_state"
        const val STEP_WRAP = 1_000_000
    }
}
