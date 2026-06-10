package com.example

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerStatus {
    IDLE, RUNNING, PAUSED, FINISHED
}

enum class StopwatchStatus {
    IDLE, RUNNING, PAUSED
}

data class LapRecord(
    val index: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long
)

class TimerStopwatchViewModel : ViewModel() {

    // --- Sound Selection State ---
    private val _selectedSound = MutableStateFlow(AlarmSoundPreset.ETHEREAL_CHIME)
    val selectedSound: StateFlow<AlarmSoundPreset> = _selectedSound.asStateFlow()

    fun selectSound(preset: AlarmSoundPreset) {
        _selectedSound.value = preset
    }

    // --- Tab Selection State ---
    private val _activeTab = MutableStateFlow(0) // 0: Timer, 1: Stopwatch
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun selectTab(index: Int) {
        _activeTab.value = index
    }

    // --- Always On Display Keep-Screen State ---
    private val _isAlwaysOn = MutableStateFlow(true) // Default true for full convenience
    val isAlwaysOn: StateFlow<Boolean> = _isAlwaysOn.asStateFlow()

    fun toggleAlwaysOn() {
        _isAlwaysOn.value = !_isAlwaysOn.value
    }

    // --- Alarm trigger event for external notification in MainActivity ---
    private val _alarmTriggered = MutableStateFlow(false)
    val alarmTriggered: StateFlow<Boolean> = _alarmTriggered.asStateFlow()

    fun setAlarmTriggered(active: Boolean) {
        _alarmTriggered.value = active
    }


    // ==========================================
    // TIMER LOGIC & STATES
    // ==========================================
    private val _timerInput = MutableStateFlow("") // e.g. "1030" -> 10m 30s
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
        // Max 6 digits (HHMMSS)
        if (current.length >= 6) return
        // Prevent leading zeros if empty
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
    }

    fun startPresetTimer(presetSeconds: Long) {
        if (_timerStatus.value != TimerStatus.IDLE) return
        val durationMs = presetSeconds * 1000L
        _timerMaxMs.value = durationMs
        _timerRemainingMs.value = durationMs
        _timerStatus.value = TimerStatus.RUNNING
        launchTimerJob(durationMs)
    }

    private fun launchTimerJob(initialRemainingMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = initialRemainingMs
            val startTime = SystemClock.elapsedRealtime()
            val targetTime = startTime + remaining

            while (remaining > 0) {
                val now = SystemClock.elapsedRealtime()
                remaining = targetTime - now
                if (remaining < 0) remaining = 0L
                _timerRemainingMs.value = remaining
                // Small sleep to keep CPU cool and animation smooth
                delay(16)
            }

            // Timer completes
            _timerStatus.value = TimerStatus.FINISHED
            _alarmTriggered.value = true
        }
    }

    fun pauseTimer() {
        if (_timerStatus.value != TimerStatus.RUNNING) return
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.PAUSED
    }

    fun resumeTimer() {
        if (_timerStatus.value != TimerStatus.PAUSED) return
        _timerStatus.value = TimerStatus.RUNNING
        launchTimerJob(_timerRemainingMs.value)
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.IDLE
        _timerRemainingMs.value = 0L
        _timerMaxMs.value = 0L
        _timerInput.value = ""
        _alarmTriggered.value = false
    }

    fun addOneMinute() {
        if (_timerStatus.value != TimerStatus.RUNNING && _timerStatus.value != TimerStatus.PAUSED) return
        
        // Add 60 seconds (60,000 ms)
        val increment = 60_000L
        _timerMaxMs.value += increment
        val newRemaining = _timerRemainingMs.value + increment
        _timerRemainingMs.value = newRemaining

        // If running, restart job to recalculate the target screen time correctly
        if (_timerStatus.value == TimerStatus.RUNNING) {
            launchTimerJob(newRemaining)
        }
    }

    fun dismissAlarm() {
        _alarmTriggered.value = false
        resetTimer()
    }

    fun snoozeAlarm() {
        _alarmTriggered.value = false
        // Dismisses active screen and instantly schedules a 1-minute countdown timer
        timerJob?.cancel()
        _timerMaxMs.value = 60_000L
        _timerRemainingMs.value = 60_000L
        _timerStatus.value = TimerStatus.RUNNING
        launchTimerJob(60_000L)
    }


    // ==========================================
    // STOPWATCH LOGIC & STATES
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
        stopwatchJob = viewModelScope.launch {
            val baseTime = SystemClock.elapsedRealtime() - _stopwatchElapsedMs.value
            while (_stopwatchStatus.value == StopwatchStatus.RUNNING) {
                _stopwatchElapsedMs.value = SystemClock.elapsedRealtime() - baseTime
                delay(10) // Update every 10 milliseconds (centisecond precision)
            }
        }
    }

    fun pauseStopwatch() {
        if (_stopwatchStatus.value != StopwatchStatus.RUNNING) return
        stopwatchJob?.cancel()
        _stopwatchStatus.value = StopwatchStatus.PAUSED
    }

    fun addLap() {
        if (_stopwatchStatus.value != StopwatchStatus.RUNNING) return
        val currentElapsed = _stopwatchElapsedMs.value
        val currentLaps = _laps.value
        
        // Split is: current overall elapsed time minus the overall elapsed time at the previous lap
        val lastTotalMs = currentLaps.firstOrNull()?.totalTimeMs ?: 0L
        val lapSplitMs = currentElapsed - lastTotalMs

        val newRecord = LapRecord(
            index = currentLaps.size + 1,
            lapTimeMs = lapSplitMs,
            totalTimeMs = currentElapsed
        )

        // Store newest laps first (top of screen scroll)
        _laps.value = listOf(newRecord) + currentLaps
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _stopwatchStatus.value = StopwatchStatus.IDLE
        _stopwatchElapsedMs.value = 0L
        _laps.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopwatchJob?.cancel()
    }
}
