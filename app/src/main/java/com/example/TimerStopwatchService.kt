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
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

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

        const val ACTION_PAUSE_POMODORO = "com.example.ACTION_PAUSE_POMODORO"
        const val ACTION_RESUME_POMODORO = "com.example.ACTION_RESUME_POMODORO"
        const val ACTION_SKIP_BREAK_POMODORO = "com.example.ACTION_SKIP_BREAK_POMODORO"
        const val ACTION_RESET_POMODORO = "com.example.ACTION_RESET_POMODORO"
    }

    override fun onCreate() {
        super.onCreate()
        TimerStopwatchStateManager.initialize(this.applicationContext)
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

        // Ticking loop to update notification periodically (every ~1s) without startService IPC
        serviceScope.launch {
            while (isActive) {
                delay(1000)
                updateNotificationImmediately()
            }
        }
    }

    private fun updateNotificationImmediately() {
        try {
            val notification = buildStatusNotification()
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
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

                ACTION_PAUSE_POMODORO -> TimerStopwatchStateManager.pausePomodoro()
                ACTION_RESUME_POMODORO -> TimerStopwatchStateManager.resumePomodoro()
                ACTION_SKIP_BREAK_POMODORO -> TimerStopwatchStateManager.skipBreak()
                ACTION_RESET_POMODORO -> TimerStopwatchStateManager.resetPomodoro()
            }
        }

        updateNotificationImmediately()

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
        val pomoStatus = TimerStopwatchStateManager.pomodoroStatus.value
        val pomoRemainingMs = TimerStopwatchStateManager.pomodoroRemainingMs.value
        val focusState = TimerStopwatchStateManager.focusModeState.value
        val completedFocus = TimerStopwatchStateManager.completedFocusSessions.value

        val isTimerActive = timerStatus != TimerStatus.IDLE
        val isStopwatchActive = stopwatchStatus != StopwatchStatus.IDLE
        val isPomoActive = pomoStatus != PomodoroStatus.IDLE

        var title = "Minimalist Timer"
        var contentText = "Ticking in the background"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // Material style fallback or matching Launcher
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true) // We manage audio separately via AlarmController for fine-grained synthetic chimes
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)

        val pomoFmt = formatTime(pomoRemainingMs)
        val sessionLabel = when (focusState) {
            FocusModeState.FOCUS -> {
                val num = (completedFocus % 4) + 1
                "Focus #$num"
            }
            FocusModeState.BREAK -> {
                val num = completedFocus % 4
                if (num == 0) "Long Break" else "Break #$num"
            }
            else -> "Pomodoro"
        }

        if (isPomoActive && (isTimerActive || isStopwatchActive)) {
            val builderTextList = mutableListOf<String>()
            if (isPomoActive) builderTextList.add("$sessionLabel: $pomoFmt")
            if (isTimerActive) builderTextList.add("Timer: ${formatTime(timerRemainingMs)}")
            if (isStopwatchActive) builderTextList.add("SW: ${formatStopwatch(stopwatchElapsedMs)}")
            title = builderTextList.joinToString("  |  ")
            contentText = "Active Background Operations"
            addPomodoroActions(builder, pomoStatus, focusState)
        } else if (isPomoActive) {
            title = "$sessionLabel $pomoFmt"
            contentText = if (pomoStatus == PomodoroStatus.RUNNING) "Remaining session time" else "Paused"
            val maxMs = TimerStopwatchStateManager.pomodoroDurationMs.value
            if (maxMs > 0 && pomoStatus == PomodoroStatus.RUNNING) {
                builder.setProgress(100, ((1.0 - (pomoRemainingMs.toDouble() / maxMs.toDouble())) * 100).toInt(), false)
            }
            addPomodoroActions(builder, pomoStatus, focusState)
        } else if (isTimerActive && isStopwatchActive) {
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

    private fun getActionPendingIntent(requestCode: Int, actionText: String): PendingIntent {
        val intent = Intent(this, TimerStopwatchService::class.java).apply { action = actionText }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun addTimerActions(builder: NotificationCompat.Builder, status: TimerStatus) {
        if (status == TimerStatus.RUNNING) {
            val pPause = getActionPendingIntent(11, ACTION_PAUSE_TIMER)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pPause)

            val pAdd = getActionPendingIntent(12, ACTION_ADD_MINUTE)
            builder.addAction(android.R.drawable.ic_input_add, "+1 Min", pAdd)

            val pAddFive = getActionPendingIntent(15, ACTION_ADD_FIVE_MINUTES)
            builder.addAction(android.R.drawable.ic_input_add, "+5 Min", pAddFive)
        } else if (status == TimerStatus.PAUSED) {
            val pResume = getActionPendingIntent(13, ACTION_RESUME_TIMER)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pResume)

            val pReset = getActionPendingIntent(14, ACTION_RESET_TIMER)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", pReset)
        }
    }

    private fun addStopwatchActions(builder: NotificationCompat.Builder, status: StopwatchStatus) {
        if (status == StopwatchStatus.RUNNING) {
            val pPause = getActionPendingIntent(21, ACTION_PAUSE_STOPWATCH)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause SW", pPause)

            val pLap = getActionPendingIntent(22, ACTION_LAP_STOPWATCH)
            builder.addAction(android.R.drawable.ic_menu_report_image, "Lap", pLap)
        } else if (status == StopwatchStatus.PAUSED) {
            val pResume = getActionPendingIntent(23, ACTION_RESUME_STOPWATCH)
            builder.addAction(android.R.drawable.ic_media_play, "Resume SW", pResume)

            val pReset = getActionPendingIntent(24, ACTION_RESET_STOPWATCH)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", pReset)
        }
    }

    private fun addPomodoroActions(builder: NotificationCompat.Builder, status: PomodoroStatus, focusState: FocusModeState) {
        if (status == PomodoroStatus.RUNNING) {
            val pPause = getActionPendingIntent(31, ACTION_PAUSE_POMODORO)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pPause)
            
            if (focusState == FocusModeState.BREAK) {
                val pSkip = getActionPendingIntent(32, ACTION_SKIP_BREAK_POMODORO)
                builder.addAction(android.R.drawable.ic_media_next, "Skip Break", pSkip)
            }
        } else if (status == PomodoroStatus.PAUSED) {
            val pResume = getActionPendingIntent(33, ACTION_RESUME_POMODORO)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pResume)

            val pReset = getActionPendingIntent(34, ACTION_RESET_POMODORO)
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
