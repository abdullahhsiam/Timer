package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.example.TimerWidgetProvider.ACTION_WIDGET_UPDATE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val timerStatus = TimerStopwatchStateManager.timerStatus.value
        val timerRemainingMs = TimerStopwatchStateManager.timerRemainingMs.value

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.timer_widget)

            // Format Timer display text
            val displayStr = formatTime(timerRemainingMs)
            views.setTextViewText(R.id.widget_timer_text, displayStr)

            // Play/Pause button state representation
            val toggleLabel = if (timerStatus == TimerStatus.RUNNING) "Pause" else "Play"
            views.setTextViewText(R.id.btn_widget_timer_toggle, toggleLabel)

            // Setup PendingIntents
            // 1. Click text or title to open MainActivity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pOpenApp = PendingIntent.getActivity(
                context, appWidgetId * 10 + 1, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_timer_text, pOpenApp)
            views.setOnClickPendingIntent(R.id.widget_timer_title, pOpenApp)

            // 2. Play/Pause toggle
            val toggleIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = if (timerStatus == TimerStatus.RUNNING) {
                    TimerStopwatchService.ACTION_PAUSE_TIMER
                } else if (timerStatus == TimerStatus.PAUSED) {
                    TimerStopwatchService.ACTION_RESUME_TIMER
                } else {
                    // Start standard default 5m countdown if clicked while idle
                    TimerStopwatchService.ACTION_RESUME_TIMER
                }
            }
            
            // If idle, clicking play sends a special default preset start inside the Service/StateManager
            val pToggle = if (timerStatus == TimerStatus.IDLE) {
                // If IDLE, clicking Play will launch the app or we can start a default timer
                val startPresetIntent = Intent(context, TimerStopwatchService::class.java).apply {
                    action = TimerStopwatchService.ACTION_UPDATE_SERVICE
                }
                // Custom trigger to start a standard 5 min timer
                views.setOnClickPendingIntent(R.id.btn_widget_timer_toggle, pOpenApp) // Open app to let them enter time
                pOpenApp
            } else {
                val pendingToggle = PendingIntent.getService(
                    context, appWidgetId * 10 + 2, toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_timer_toggle, pendingToggle)
                pendingToggle
            }

            // 3. Reset Button
            val resetIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_RESET_TIMER
            }
            val pReset = PendingIntent.getService(
                context, appWidgetId * 10 + 3, resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_timer_reset, pReset)

            // Instruct manager to update unit
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_UPDATE || AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, TimerWidgetProvider::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}
