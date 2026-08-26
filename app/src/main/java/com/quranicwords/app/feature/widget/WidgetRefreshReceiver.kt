package com.quranicwords.app.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val providerClassName = intent.getStringExtra(EXTRA_PROVIDER_CLASS)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || providerClassName == null) {
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Phase 1: Briefly show the reflection sheen animation background
                when (providerClassName) {
                    StatsWidgetProvider::class.java.name -> {
                        StatsWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(widgetId), isReflected = true)
                    }
                    WordWidgetProvider::class.java.name -> {
                        WordWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(widgetId), advanceRotation = false, isReflected = true)
                    }
                    CombinedWidgetProvider::class.java.name -> {
                        CombinedWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(widgetId), advanceRotation = false, isReflected = true)
                    }
                }

                // Brief delay for the sheen reflection effect
                delay(350L)

                // Phase 2: Advance word rotation and restore normal glass background
                when (providerClassName) {
                    StatsWidgetProvider::class.java.name -> {
                        StatsWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(widgetId), isReflected = false)
                    }
                    WordWidgetProvider::class.java.name -> {
                        WordWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(widgetId), advanceRotation = true, isReflected = false)
                    }
                    CombinedWidgetProvider::class.java.name -> {
                        CombinedWidgetProvider.updateWidgets(context, appWidgetManager, intArrayOf(widgetId), advanceRotation = true, isReflected = false)
                    }
                }

                // Reschedule the 3-minute periodic update timer
                WidgetUpdateScheduler.scheduleNextUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.quranicwords.app.ACTION_WIDGET_REFRESH"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        const val EXTRA_PROVIDER_CLASS = "extra_provider_class"

        fun getRefreshPendingIntent(
            context: Context,
            widgetId: Int,
            providerClass: Class<*>
        ): PendingIntent {
            val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
                action = ACTION_REFRESH
                putExtra(EXTRA_WIDGET_ID, widgetId)
                putExtra(EXTRA_PROVIDER_CLASS, providerClass.name)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId + 5000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
