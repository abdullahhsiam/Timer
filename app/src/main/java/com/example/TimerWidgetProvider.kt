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
            val views = RemoteViews(context.packageName, R.layout.timer_widget)

            // Dynamic Styling Injection
            try {
                val baseBgColor = android.graphics.Color.parseColor(widgetStyle.bgColor)
                val bgAlpha = (widgetStyle.opacity * 255).toInt().coerceIn(0, 255)
                val colorWithAlpha = (bgAlpha shl 24) or (baseBgColor and 0x00FFFFFF)
                views.setInt(android.R.id.background, "setBackgroundColor", colorWithAlpha)

                val textColorVal = android.graphics.Color.parseColor(widgetStyle.textColor)
                val accentColorVal = android.graphics.Color.parseColor(widgetStyle.accentColor)

                views.setTextColor(R.id.widget_timer_title, accentColorVal)
                views.setTextColor(R.id.widget_timer_text, textColorVal)

                val btnBgVal = (35 shl 24) or (accentColorVal and 0x00FFFFFF)
                views.setTextColor(R.id.btn_widget_timer_toggle, accentColorVal)
                views.setInt(R.id.btn_widget_timer_toggle, "setBackgroundColor", btnBgVal)

                views.setTextColor(R.id.btn_widget_timer_reset, textColorVal)
                views.setInt(R.id.btn_widget_timer_reset, "setBackgroundColor", btnBgVal)

                views.setTextColor(R.id.btn_widget_timer_add_1, accentColorVal)
                views.setInt(R.id.btn_widget_timer_add_1, "setBackgroundColor", btnBgVal)

                views.setTextColor(R.id.btn_widget_timer_add_5, accentColorVal)
                views.setInt(R.id.btn_widget_timer_add_5, "setBackgroundColor", btnBgVal)
            } catch (e: Exception) {}

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

            // 2. Play/Pause toggle (Service Action)
            val toggleIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = if (timerStatus == TimerStatus.RUNNING) {
                    TimerStopwatchService.ACTION_PAUSE_TIMER
                } else {
                    TimerStopwatchService.ACTION_RESUME_TIMER
                }
            }
            val pToggle = PendingIntent.getService(
                context, appWidgetId * 10 + 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_timer_toggle, pToggle)

            // 3. Reset Button
            val resetIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_RESET_TIMER
            }
            val pReset = PendingIntent.getService(
                context, appWidgetId * 10 + 3, resetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_timer_reset, pReset)

            // 4. Add 1 Minute Button
            val addOneIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_ADD_MINUTE
            }
            val pAddOne = PendingIntent.getService(
                context, appWidgetId * 10 + 4, addOneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_timer_add_1, pAddOne)

            // 5. Add 5 Minutes Button
            val addFiveIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_ADD_FIVE_MINUTES
            }
            val pAddFive = PendingIntent.getService(
                context, appWidgetId * 10 + 5, addFiveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_timer_add_5, pAddFive)

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
