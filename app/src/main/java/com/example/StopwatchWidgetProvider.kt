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
        // Initialize State Manager to ensure states are restored and active
        TimerStopwatchStateManager.initialize(context.applicationContext)

        val stopwatchStatus = TimerStopwatchStateManager.stopwatchStatus.value
        val stopwatchElapsedMs = TimerStopwatchStateManager.stopwatchElapsedMs.value
        val appearanceConfig = TimerStopwatchStateManager.appearanceConfig.value
        val widgetStyle = appearanceConfig.stopwatchWidget

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val isSmall = minHeight < 80

            val layoutId = if (isSmall) R.layout.stopwatch_widget_small else R.layout.stopwatch_widget
            val views = RemoteViews(context.packageName, layoutId)

            // Dynamic Styling Injection
            try {
                val baseBgColor = android.graphics.Color.parseColor(widgetStyle.bgColor)
                val bgAlpha = (widgetStyle.opacity * 255).toInt().coerceIn(0, 255)
                val colorWithAlpha = (bgAlpha shl 24) or (baseBgColor and 0x00FFFFFF)
                views.setInt(android.R.id.background, "setBackgroundColor", colorWithAlpha)

                // Paused -> RED, Running/Idle -> WHITE
                val isPaused = stopwatchStatus != StopwatchStatus.RUNNING && stopwatchStatus != StopwatchStatus.IDLE
                val displayTextColor = if (isPaused) android.graphics.Color.RED else android.graphics.Color.WHITE
                val staticWhite = android.graphics.Color.WHITE

                views.setTextColor(R.id.widget_stopwatch_text, displayTextColor)

                if (!isSmall) {
                    views.setTextColor(R.id.widget_stopwatch_title, staticWhite)
                    val btnBgVal = (35 shl 24) or (staticWhite and 0x00FFFFFF)
                    views.setTextColor(R.id.btn_widget_stopwatch_toggle, staticWhite)
                    views.setInt(R.id.btn_widget_stopwatch_toggle, "setBackgroundColor", btnBgVal)

                    views.setTextColor(R.id.btn_widget_stopwatch_reset, staticWhite)
                    views.setInt(R.id.btn_widget_stopwatch_reset, "setBackgroundColor", btnBgVal)
                }
            } catch (e: Exception) {}

            // Format Stopwatch display text (m:s.cc)
            val displayStr = formatStopwatch(stopwatchElapsedMs)
            views.setTextViewText(R.id.widget_stopwatch_text, displayStr)

            if (!isSmall) {
                // Play/Pause/Reset text labels
                val toggleLabel = if (stopwatchStatus == StopwatchStatus.RUNNING) "Pause" else "Play"
                views.setTextViewText(R.id.btn_widget_stopwatch_toggle, toggleLabel)
                
                val resetLabel = if (stopwatchStatus == StopwatchStatus.RUNNING) "Lap" else "Reset"
                views.setTextViewText(R.id.btn_widget_stopwatch_reset, resetLabel)
            } else {
                val toggleIcon = if (stopwatchStatus == StopwatchStatus.RUNNING) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                views.setImageViewResource(R.id.btn_widget_stopwatch_toggle, toggleIcon)

                // For small format, lap isn't available, just standard reset icon vs lap icon
                val resetIcon = if (stopwatchStatus == StopwatchStatus.RUNNING) android.R.drawable.ic_menu_report_image else android.R.drawable.ic_menu_close_clear_cancel
                views.setImageViewResource(R.id.btn_widget_stopwatch_reset, resetIcon)
            }

            // Setup PendingIntents
            // 1. Title/Click to open main activity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pOpenApp = PendingIntent.getActivity(
                context, appWidgetId * 10 + 1, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_stopwatch_text, pOpenApp)
            if (!isSmall) views.setOnClickPendingIntent(R.id.widget_stopwatch_title, pOpenApp)

            // Helper for pending intents
            fun getActionPendingIntent(actionCode: Int, actionText: String): PendingIntent {
                val intent = Intent(context, TimerStopwatchService::class.java).apply {
                    action = actionText
                }
                return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    PendingIntent.getForegroundService(
                        context, appWidgetId * 10 + actionCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getService(
                        context, appWidgetId * 10 + actionCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            // 2. Play/Pause toggle
            val toggleAction = if (stopwatchStatus == StopwatchStatus.RUNNING) {
                TimerStopwatchService.ACTION_PAUSE_STOPWATCH
            } else {
                TimerStopwatchService.ACTION_RESUME_STOPWATCH
            }
            views.setOnClickPendingIntent(R.id.btn_widget_stopwatch_toggle, getActionPendingIntent(2, toggleAction))

            // 3. Reset/Lap button
            val resetAction = if (stopwatchStatus == StopwatchStatus.RUNNING) {
                TimerStopwatchService.ACTION_LAP_STOPWATCH
            } else {
                TimerStopwatchService.ACTION_RESET_STOPWATCH
            }
            views.setOnClickPendingIntent(R.id.btn_widget_stopwatch_reset, getActionPendingIntent(3, resetAction))

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun formatStopwatch(ms: Long): String {
        val totalSecs = ms / 1000
        val m = (totalSecs / 60) % 60
        val s = totalSecs % 60
        val cc = (ms / 10) % 100
        return String.format("%02d:%02d.%02d", m, s, cc)
    }
}
