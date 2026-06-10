package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class TimerStopwatchService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var alarmController: AlarmController
    private var isFirstStart = true

    companion object {
        const val CHANNEL_ID = "timer_stopwatch_fgs_channel"
        const val NOTIFICATION_ID = 2026

        // Actions
        const val ACTION_UPDATE_SERVICE = "com.example.ACTION_UPDATE_SERVICE"
        const val ACTION_TRIGGER_NOTIFICATION_REFRESH = "com.example.ACTION_TRIGGER_NOTIFICATION_REFRESH"

        const val ACTION_PAUSE_TIMER = "com.example.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.example.ACTION_RESUME_TIMER"
        const val ACTION_ADD_MINUTE = "com.example.ACTION_ADD_MINUTE"
        const val ACTION_ADD_FIVE_MINUTES = "com.example.ACTION_ADD_FIVE_MINUTES"
        const val ACTION_RESET_TIMER = "com.example.ACTION_RESET_TIMER"

        const val ACTION_PAUSE_STOPWATCH = "com.example.ACTION_PAUSE_STOPWATCH"
        const val ACTION_RESUME_STOPWATCH = "com.example.ACTION_RESUME_STOPWATCH"
        const val ACTION_LAP_STOPWATCH = "com.example.ACTION_LAP_STOPWATCH"
        const val ACTION_RESET_STOPWATCH = "com.example.ACTION_RESET_STOPWATCH"
    }

    override fun onCreate() {
        super.onCreate()
        alarmController = AlarmController(this)

        createNotificationChannel()

        // Start Foreground immediately to comply with Android 14+ rules
        val initialNotification = buildStatusNotification()
        startForeground(NOTIFICATION_ID, initialNotification)

        // Observe alarm triggers to play sound in background
        serviceScope.launch {
            TimerStopwatchStateManager.alarmTriggered.collectLatest { triggered ->
                if (triggered) {
                    alarmController.startAlarm()
                } else {
                    alarmController.stopAlarm()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PAUSE_TIMER -> TimerStopwatchStateManager.pauseTimer()
                ACTION_RESUME_TIMER -> TimerStopwatchStateManager.resumeTimer()
                ACTION_ADD_MINUTE -> TimerStopwatchStateManager.addOneMinute()
                ACTION_ADD_FIVE_MINUTES -> TimerStopwatchStateManager.addFiveMinutes()
                ACTION_RESET_TIMER -> TimerStopwatchStateManager.resetTimer()

                ACTION_PAUSE_STOPWATCH -> TimerStopwatchStateManager.pauseStopwatch()
                ACTION_RESUME_STOPWATCH -> TimerStopwatchStateManager.startStopwatch()
                ACTION_LAP_STOPWATCH -> TimerStopwatchStateManager.addLap()
                ACTION_RESET_STOPWATCH -> TimerStopwatchStateManager.resetStopwatch()
                
                ACTION_UPDATE_SERVICE, ACTION_TRIGGER_NOTIFICATION_REFRESH -> {
                    // Handled inside flow/triggers, just rebuild status notification below
                }
            }
        }

        val notification = buildStatusNotification()
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    private fun buildStatusNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timerStatus = TimerStopwatchStateManager.timerStatus.value
        val timerRemainingMs = TimerStopwatchStateManager.timerRemainingMs.value
        val stopwatchStatus = TimerStopwatchStateManager.stopwatchStatus.value
        val stopwatchElapsedMs = TimerStopwatchStateManager.stopwatchElapsedMs.value

        val isTimerActive = timerStatus != TimerStatus.IDLE
        val isStopwatchActive = stopwatchStatus != StopwatchStatus.IDLE

        var title = "Minimalist Timer"
        var contentText = "Ticking in the background"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // Material style fallback or matching Launcher
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true) // We manage audio separately via AlarmController for fine-grained synthetic chimes
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)

        if (isTimerActive && isStopwatchActive) {
            val timerFmt = formatTime(timerRemainingMs)
            val swFmt = formatStopwatch(stopwatchElapsedMs)
            title = "Timer: $timerFmt  |  Stopwatch: $swFmt"
            contentText = "Active Background Operations"
            
            // Shared controls: Pause stopwatch & Pause Timer
            addTimerActions(builder, timerStatus)
            addStopwatchActions(builder, stopwatchStatus)
        } else if (isTimerActive) {
            val timerFmt = formatTime(timerRemainingMs)
            title = "Timer $timerFmt"
            contentText = if (timerStatus == TimerStatus.RUNNING) "Remaining time" else "Paused"
            
            // Progress Bar
            val maxMs = TimerStopwatchStateManager.timerMaxMs.value
            if (maxMs > 0 && timerStatus == TimerStatus.RUNNING) {
                builder.setProgress(100, ((1.0 - (timerRemainingMs.toDouble() / maxMs.toDouble())) * 100).toInt(), false)
            }
            
            addTimerActions(builder, timerStatus)
        } else if (isStopwatchActive) {
            val swFmt = formatStopwatch(stopwatchElapsedMs)
            title = "Stopwatch $swFmt"
            contentText = if (stopwatchStatus == StopwatchStatus.RUNNING) "Running..." else "Paused"

            addStopwatchActions(builder, stopwatchStatus)
        }

        builder.setContentTitle(title)
        builder.setContentText(contentText)

        return builder.build()
    }

    private fun addTimerActions(builder: NotificationCompat.Builder, status: TimerStatus) {
        if (status == TimerStatus.RUNNING) {
            val pauseIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_PAUSE_TIMER }
            val pPause = PendingIntent.getService(this, 11, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pPause)

            val addMinIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_ADD_MINUTE }
            val pAdd = PendingIntent.getService(this, 12, addMinIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_input_add, "+1 Min", pAdd)

            val addFiveMinIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_ADD_FIVE_MINUTES }
            val pAddFive = PendingIntent.getService(this, 15, addFiveMinIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_input_add, "+5 Min", pAddFive)
        } else if (status == TimerStatus.PAUSED) {
            val resumeIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_RESUME_TIMER }
            val pResume = PendingIntent.getService(this, 13, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pResume)

            val resetIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_RESET_TIMER }
            val pReset = PendingIntent.getService(this, 14, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", pReset)
        }
    }

    private fun addStopwatchActions(builder: NotificationCompat.Builder, status: StopwatchStatus) {
        if (status == StopwatchStatus.RUNNING) {
            val pauseIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_PAUSE_STOPWATCH }
            val pPause = PendingIntent.getService(this, 21, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause SW", pPause)

            val lapIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_LAP_STOPWATCH }
            val pLap = PendingIntent.getService(this, 22, lapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_menu_report_image, "Lap", pLap)
        } else if (status == StopwatchStatus.PAUSED) {
            val resumeIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_RESUME_STOPWATCH }
            val pResume = PendingIntent.getService(this, 23, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pResume)

            val resetIntent = Intent(this, TimerStopwatchService::class.java).apply { action = ACTION_RESET_STOPWATCH }
            val pReset = PendingIntent.getService(this, 24, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", pReset)
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

    private fun formatStopwatch(ms: Long): String {
        val totalSecs = ms / 1000
        val m = (totalSecs / 60) % 60
        val s = totalSecs % 60
        val cc = (ms / 10) % 100
        return String.format("%02d:%02d.%02d", m, s, cc)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer & Stopwatch Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps active timing processes running cleanly in the background and status bar."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        alarmController.stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
