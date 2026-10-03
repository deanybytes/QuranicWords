package com.quranicwords.app.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * In-widget interactions that don't open the app: "next word" (advances this widget's rotation)
 * and "reveal meaning" (toggles a due word's hidden meaning). Both update only the tapped widget.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val kind = WidgetKind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || kind == null) return

        val store = WidgetStateStore(context)
        when (intent.action) {
            ACTION_NEXT_WORD -> store.advance(widgetId)
            ACTION_TOGGLE_REVEAL -> store.toggleReveal(widgetId, intent.getStringExtra(EXTRA_WORD_ID) ?: return)
            else -> return
        }
        WidgetUpdateScheduler.runAsync(this, context) {
            WidgetUpdateScheduler.updateWidgets(context, kind, intArrayOf(widgetId))
        }
    }

    companion object {
        const val ACTION_NEXT_WORD = "com.quranicwords.app.widget.action.NEXT_WORD"
        const val ACTION_TOGGLE_REVEAL = "com.quranicwords.app.widget.action.TOGGLE_REVEAL"
        private const val EXTRA_WIDGET_ID = "com.quranicwords.app.widget.extra.WIDGET_ID"
        private const val EXTRA_KIND = "com.quranicwords.app.widget.extra.KIND"
        private const val EXTRA_WORD_ID = "com.quranicwords.app.widget.extra.ACTION_WORD_ID"

        fun next(context: Context, kind: WidgetKind, widgetId: Int): PendingIntent =
            broadcast(context, ACTION_NEXT_WORD, kind, widgetId, wordId = null)

        fun reveal(context: Context, kind: WidgetKind, widgetId: Int, wordId: String): PendingIntent =
            broadcast(context, ACTION_TOGGLE_REVEAL, kind, widgetId, wordId)

        private fun broadcast(context: Context, action: String, kind: WidgetKind, widgetId: Int, wordId: String?): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                this.action = action
                // Distinct data per widget and action, so each instance gets its own PendingIntent.
                data = "quranicwords-widget://action/$action/$widgetId".toUri()
                putExtra(EXTRA_WIDGET_ID, widgetId)
                putExtra(EXTRA_KIND, kind.name)
                wordId?.let { putExtra(EXTRA_WORD_ID, it) }
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
