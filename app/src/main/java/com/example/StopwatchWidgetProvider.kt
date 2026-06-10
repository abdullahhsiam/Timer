package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class StopwatchWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.example.StopwatchWidgetProvider.ACTION_WIDGET_UPDATE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val stopwatchStatus = TimerStopwatchStateManager.stopwatchStatus.value
        val stopwatchElapsedMs = TimerStopwatchStateManager.stopwatchElapsedMs.value

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.stopwatch_widget)

            // Format Stopwatch display text (m:s.cc)
            val displayStr = formatStopwatch(stopwatchElapsedMs)
            views.setTextViewText(R.id.widget_stopwatch_text, displayStr)

            // Play/Pause button labels
            val toggleLabel = if (stopwatchStatus == StopwatchStatus.RUNNING) "Pause" else "Play"
            views.setTextViewText(R.id.btn_widget_stopwatch_toggle, toggleLabel)

            // Contextual Lap vs Reset Button labels
            val resetLabel = if (stopwatchStatus == StopwatchStatus.RUNNING) "Lap" else "Reset"
            views.setTextViewText(R.id.btn_widget_stopwatch_reset, resetLabel)

            // Setup PendingIntents
            // 1. Title/Click to open main activity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pOpenApp = PendingIntent.getActivity(
                context, appWidgetId * 10 + 1, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_stopwatch_text, pOpenApp)
            views.setOnClickPendingIntent(R.id.widget_stopwatch_title, pOpenApp)

            // 2. Play/Pause toggle
            val toggleIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = if (stopwatchStatus == StopwatchStatus.RUNNING) {
                    TimerStopwatchService.ACTION_PAUSE_STOPWATCH
                } else {
                    TimerStopwatchService.ACTION_RESUME_STOPWATCH
                }
            }
            val pToggle = PendingIntent.getService(
                context, appWidgetId * 10 + 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_stopwatch_toggle, pToggle)

            // 3. Reset/Lap button
            val resetAction = if (stopwatchStatus == StopwatchStatus.RUNNING) {
                TimerStopwatchService.ACTION_LAP_STOPWATCH
            } else {
                TimerStopwatchService.ACTION_RESET_STOPWATCH
            }
            val resetIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = resetAction
            }
            val pReset = PendingIntent.getService(
                context, appWidgetId * 10 + 3, resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_stopwatch_reset, pReset)

            // Update app widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_UPDATE || AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, StopwatchWidgetProvider::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun formatStopwatch(ms: Long): String {
        val totalSecs = ms / 1000
        val m = (totalSecs / 60) % 60
        val s = totalSecs % 60
        val cc = (ms / 10) % 100
        return String.format("%02d:%02d.%02d", m, s, cc)
    }
}
