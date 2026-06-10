package com.example

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerStopwatchStateManager {

    private val job = SupervisorJob()
    private val stateScope = CoroutineScope(Dispatchers.Main + job)

    private var appContext: Context? = null

    // --- State Initialization ---
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // --- Sound Selection State ---
    private val _selectedSound = MutableStateFlow(AlarmSoundPreset.ETHEREAL_CHIME)
    val selectedSound: StateFlow<AlarmSoundPreset> = _selectedSound.asStateFlow()

    fun selectSound(preset: AlarmSoundPreset) {
        _selectedSound.value = preset
    }

    // --- Background Style State ---
    private val _isBackgroundAnimated = MutableStateFlow(true)
    val isBackgroundAnimated: StateFlow<Boolean> = _isBackgroundAnimated.asStateFlow()

    fun setBackgroundAnimated(animated: Boolean) {
        _isBackgroundAnimated.value = animated
    }

    // --- Always On Display Keep-Screen State ---
    private val _isAlwaysOn = MutableStateFlow(true)
    val isAlwaysOn: StateFlow<Boolean> = _isAlwaysOn.asStateFlow()

    fun toggleAlwaysOn() {
        _isAlwaysOn.value = !_isAlwaysOn.value
    }

    // --- Alarm trigger event ---
    private val _alarmTriggered = MutableStateFlow(false)
    val alarmTriggered: StateFlow<Boolean> = _alarmTriggered.asStateFlow()

    fun setAlarmTriggered(active: Boolean) {
        _alarmTriggered.value = active
        if (active) {
            triggerNotificationUpdate()
        }
    }

    // --- Picture in Picture State ---
    private val _isInPip = MutableStateFlow(false)
    val isInPip: StateFlow<Boolean> = _isInPip.asStateFlow()

    fun setInPip(active: Boolean) {
        _isInPip.value = active
    }

    // ==========================================
    // TIMER STATE & LOGIC
    // ==========================================
    private val _timerInput = MutableStateFlow("")
    val timerInput: StateFlow<String> = _timerInput.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _timerRemainingMs = MutableStateFlow(0L)
    val timerRemainingMs: StateFlow<Long> = _timerRemainingMs.asStateFlow()

    private val _timerMaxMs = MutableStateFlow(0L)
    val timerMaxMs: StateFlow<Long> = _timerMaxMs.asStateFlow()

    private var timerJob: Job? = null

    fun appendDigit(digit: Int) {
        if (_timerStatus.value != TimerStatus.IDLE) return
        val current = _timerInput.value
        if (current.length >= 6) return
        if (current.isEmpty() && digit == 0) return
        _timerInput.value = current + digit
    }

    fun deleteDigit() {
        if (_timerStatus.value != TimerStatus.IDLE) return
        val current = _timerInput.value
        if (current.isNotEmpty()) {
            _timerInput.value = current.dropLast(1)
        }
    }

    fun clearTimerInput() {
        if (_timerStatus.value != TimerStatus.IDLE) return
        _timerInput.value = ""
    }

    fun getParsedDurationSeconds(): Long {
        val input = _timerInput.value
        if (input.isEmpty()) return 0L
        val formatted = input.padStart(6, '0')
        val h = formatted.substring(0, 2).toInt()
        val m = formatted.substring(2, 4).toInt()
        val s = formatted.substring(4, 6).toInt()
        return (h * 3600L + m * 60L + s)
    }

    fun startTimer() {
        if (_timerStatus.value != TimerStatus.IDLE) return
        val totalSecs = getParsedDurationSeconds()
        if (totalSecs <= 0) return

        val durationMs = totalSecs * 1000L
        _timerMaxMs.value = durationMs
        _timerRemainingMs.value = durationMs
        _timerStatus.value = TimerStatus.RUNNING

        launchTimerJob(durationMs)
        notifyServiceOfStateChange()
    }

    fun startPresetTimer(presetSeconds: Long) {
        if (_timerStatus.value != TimerStatus.IDLE) return
        val durationMs = presetSeconds * 1000L
        _timerMaxMs.value = durationMs
        _timerRemainingMs.value = durationMs
        _timerStatus.value = TimerStatus.RUNNING
        launchTimerJob(durationMs)
        notifyServiceOfStateChange()
    }

    private fun launchTimerJob(initialRemainingMs: Long) {
        timerJob?.cancel()
        timerJob = stateScope.launch {
            var remaining = initialRemainingMs
            val startTime = SystemClock.elapsedRealtime()
            val targetTime = startTime + remaining

            var lastNotifyTime = 0L

            while (remaining > 0) {
                val now = SystemClock.elapsedRealtime()
                remaining = targetTime - now
                if (remaining < 0) remaining = 0L
                _timerRemainingMs.value = remaining
                
                // Update widgets and notifications periodically (every ~1s)
                if (now - lastNotifyTime >= 1000L) {
                    lastNotifyTime = now
                    triggerNotificationUpdate()
                    triggerWidgetUpdate()
                }

                delay(16)
            }

            _timerStatus.value = TimerStatus.FINISHED
            _alarmTriggered.value = true
            triggerNotificationUpdate()
            triggerWidgetUpdate()
            notifyServiceOfStateChange()
        }
    }

    fun pauseTimer() {
        if (_timerStatus.value != TimerStatus.RUNNING) return
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.PAUSED
        triggerNotificationUpdate()
        triggerWidgetUpdate()
        notifyServiceOfStateChange()
    }

    fun resumeTimer() {
        if (_timerStatus.value != TimerStatus.PAUSED) return
        _timerStatus.value = TimerStatus.RUNNING
        launchTimerJob(_timerRemainingMs.value)
        notifyServiceOfStateChange()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.IDLE
        _timerRemainingMs.value = 0L
        _timerMaxMs.value = 0L
        _timerInput.value = ""
        _alarmTriggered.value = false
        triggerNotificationUpdate()
        triggerWidgetUpdate()
        notifyServiceOfStateChange()
    }

    fun addOneMinute() {
        if (_timerStatus.value != TimerStatus.RUNNING && _timerStatus.value != TimerStatus.PAUSED) return
        val increment = 60_000L
        _timerMaxMs.value += increment
        val newRemaining = _timerRemainingMs.value + increment
        _timerRemainingMs.value = newRemaining

        if (_timerStatus.value == TimerStatus.RUNNING) {
            launchTimerJob(newRemaining)
        } else {
            triggerNotificationUpdate()
            triggerWidgetUpdate()
        }
    }

    fun dismissAlarm() {
        _alarmTriggered.value = false
        resetTimer()
    }

    fun snoozeAlarm() {
        _alarmTriggered.value = false
        timerJob?.cancel()
        _timerMaxMs.value = 60_000L
        _timerRemainingMs.value = 60_000L
        _timerStatus.value = TimerStatus.RUNNING
        launchTimerJob(60_000L)
        notifyServiceOfStateChange()
    }

    // ==========================================
    // STOPWATCH STATE & LOGIC
    // ==========================================
    private val _stopwatchStatus = MutableStateFlow(StopwatchStatus.IDLE)
    val stopwatchStatus: StateFlow<StopwatchStatus> = _stopwatchStatus.asStateFlow()

    private val _stopwatchElapsedMs = MutableStateFlow(0L)
    val stopwatchElapsedMs: StateFlow<Long> = _stopwatchElapsedMs.asStateFlow()

    private val _laps = MutableStateFlow<List<LapRecord>>(emptyList())
    val laps: StateFlow<List<LapRecord>> = _laps.asStateFlow()

    private var stopwatchJob: Job? = null

    fun startStopwatch() {
        if (_stopwatchStatus.value == StopwatchStatus.RUNNING) return
        _stopwatchStatus.value = StopwatchStatus.RUNNING

        stopwatchJob?.cancel()
        stopwatchJob = stateScope.launch {
            val baseTime = SystemClock.elapsedRealtime() - _stopwatchElapsedMs.value
            var lastNotifyTime = 0L

            while (_stopwatchStatus.value == StopwatchStatus.RUNNING) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - baseTime
                _stopwatchElapsedMs.value = elapsed

                // Update notification and widget every ~1s roughly
                if (now - lastNotifyTime >= 1000L) {
                    lastNotifyTime = now
                    triggerNotificationUpdate()
                    triggerWidgetUpdate()
                }

                delay(10)
            }
        }
        notifyServiceOfStateChange()
    }

    fun pauseStopwatch() {
        if (_stopwatchStatus.value != StopwatchStatus.RUNNING) return
        stopwatchJob?.cancel()
        _stopwatchStatus.value = StopwatchStatus.PAUSED
        triggerNotificationUpdate()
        triggerWidgetUpdate()
        notifyServiceOfStateChange()
    }

    fun addLap() {
        if (_stopwatchStatus.value != StopwatchStatus.RUNNING) return
        val currentElapsed = _stopwatchElapsedMs.value
        val currentLaps = _laps.value
        
        val lastTotalMs = currentLaps.firstOrNull()?.totalTimeMs ?: 0L
        val lapSplitMs = currentElapsed - lastTotalMs

        val newRecord = LapRecord(
            index = currentLaps.size + 1,
            lapTimeMs = lapSplitMs,
            totalTimeMs = currentElapsed
        )

        _laps.value = listOf(newRecord) + currentLaps
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _stopwatchStatus.value = StopwatchStatus.IDLE
        _stopwatchElapsedMs.value = 0L
        _laps.value = emptyList()
        triggerNotificationUpdate()
        triggerWidgetUpdate()
        notifyServiceOfStateChange()
    }

    // ==========================================
    // NOTIFICATION & SERVICE SIGNALING
    // ==========================================
    private fun notifyServiceOfStateChange() {
        val context = appContext ?: return
        val isTimerRunning = _timerStatus.value == TimerStatus.RUNNING || _timerStatus.value == TimerStatus.PAUSED || _timerStatus.value == TimerStatus.FINISHED
        val isStopwatchRunning = _stopwatchStatus.value == StopwatchStatus.RUNNING || _stopwatchStatus.value == StopwatchStatus.PAUSED

        if (isTimerRunning || isStopwatchRunning) {
            val serviceIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_UPDATE_SERVICE
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            // Stop the foreground service if both are idle
            val serviceIntent = Intent(context, TimerStopwatchService::class.java)
            context.stopService(serviceIntent)
        }
    }

    private fun triggerNotificationUpdate() {
        val context = appContext ?: return
        val isTimerRunningOrActive = _timerStatus.value != TimerStatus.IDLE
        val isStopwatchRunningOrActive = _stopwatchStatus.value != StopwatchStatus.IDLE

        if (isTimerRunningOrActive || isStopwatchRunningOrActive) {
            val serviceIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_TRIGGER_NOTIFICATION_REFRESH
            }
            context.startService(serviceIntent)
        }
    }

    fun triggerWidgetUpdate() {
        val context = appContext ?: return
        // Broadcast updates to widgets
        context.sendBroadcast(Intent(context, TimerWidgetProvider::class.java).apply {
            action = TimerWidgetProvider.ACTION_WIDGET_UPDATE
        })
        context.sendBroadcast(Intent(context, StopwatchWidgetProvider::class.java).apply {
            action = StopwatchWidgetProvider.ACTION_WIDGET_UPDATE
        })
    }
}
