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
        // Initialize State Manager to ensure states are restored and active
        TimerStopwatchStateManager.initialize(context.applicationContext)

        val timerStatus = TimerStopwatchStateManager.timerStatus.value
        val timerRemainingMs = TimerStopwatchStateManager.timerRemainingMs.value
        val appearanceConfig = TimerStopwatchStateManager.appearanceConfig.value
        val widgetStyle = appearanceConfig.timerWidget

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val isSmall = minHeight < 80

            val layoutId = if (isSmall) R.layout.timer_widget_small else R.layout.timer_widget
            val views = RemoteViews(context.packageName, layoutId)

            // Dynamic Styling Injection
            try {
                val baseBgColor = android.graphics.Color.parseColor(widgetStyle.bgColor)
                val bgAlpha = (widgetStyle.opacity * 255).toInt().coerceIn(0, 255)
                val colorWithAlpha = (bgAlpha shl 24) or (baseBgColor and 0x00FFFFFF)
                views.setInt(android.R.id.background, "setBackgroundColor", colorWithAlpha)

                // Pauesd -> RED, Running/Idle -> WHITE
                val isPaused = timerStatus == TimerStatus.PAUSED
                val displayTextColor = if (isPaused) android.graphics.Color.RED else android.graphics.Color.WHITE
                val staticWhite = android.graphics.Color.WHITE

                views.setTextColor(R.id.widget_timer_text, displayTextColor)

                if (!isSmall) {
                    views.setTextColor(R.id.widget_timer_title, staticWhite)

                    val btnBgVal = (35 shl 24) or (staticWhite and 0x00FFFFFF)
                    views.setTextColor(R.id.btn_widget_timer_toggle, staticWhite)
                    views.setInt(R.id.btn_widget_timer_toggle, "setBackgroundColor", btnBgVal)

                    views.setTextColor(R.id.btn_widget_timer_reset, staticWhite)
                    views.setInt(R.id.btn_widget_timer_reset, "setBackgroundColor", btnBgVal)

                    views.setTextColor(R.id.btn_widget_timer_add_1, staticWhite)
                    views.setInt(R.id.btn_widget_timer_add_1, "setBackgroundColor", btnBgVal)

                    views.setTextColor(R.id.btn_widget_timer_add_5, staticWhite)
                    views.setInt(R.id.btn_widget_timer_add_5, "setBackgroundColor", btnBgVal)
                }
            } catch (e: Exception) {}

            // Format Timer display text
            val displayStr = formatTime(timerRemainingMs)
            views.setTextViewText(R.id.widget_timer_text, displayStr)

            if (!isSmall) {
                // Play/Pause button state representation
                val toggleLabel = if (timerStatus == TimerStatus.RUNNING) "Pause" else "Play"
                views.setTextViewText(R.id.btn_widget_timer_toggle, toggleLabel)
            } else {
                val toggleIcon = if (timerStatus == TimerStatus.RUNNING) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                views.setImageViewResource(R.id.btn_widget_timer_toggle, toggleIcon)
            }

            // Setup PendingIntents
            // 1. Click text or title to open MainActivity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pOpenApp = PendingIntent.getActivity(
                context, appWidgetId * 10 + 1, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_timer_text, pOpenApp)
            if (!isSmall) views.setOnClickPendingIntent(R.id.widget_timer_title, pOpenApp)

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

            // 2. Play/Pause toggle (Service Action)
            val toggleAction = if (timerStatus == TimerStatus.RUNNING) {
                TimerStopwatchService.ACTION_PAUSE_TIMER
            } else {
                TimerStopwatchService.ACTION_RESUME_TIMER
            }
            views.setOnClickPendingIntent(R.id.btn_widget_timer_toggle, getActionPendingIntent(2, toggleAction))

            // 3. Reset Button
            views.setOnClickPendingIntent(R.id.btn_widget_timer_reset, getActionPendingIntent(3, TimerStopwatchService.ACTION_RESET_TIMER))

            // 4. Add 1 Minute Button
            if (isSmall) {
                views.setOnClickPendingIntent(R.id.btn_widget_timer_add, getActionPendingIntent(4, TimerStopwatchService.ACTION_ADD_MINUTE))
            } else {
                views.setOnClickPendingIntent(R.id.btn_widget_timer_add_1, getActionPendingIntent(4, TimerStopwatchService.ACTION_ADD_MINUTE))
                views.setOnClickPendingIntent(R.id.btn_widget_timer_add_5, getActionPendingIntent(5, TimerStopwatchService.ACTION_ADD_FIVE_MINUTES))
            }

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
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
