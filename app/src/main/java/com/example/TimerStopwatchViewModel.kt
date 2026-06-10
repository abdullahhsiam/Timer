package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerStopwatchViewModel : ViewModel() {

    // --- Sound Selection State ---
    val selectedSound: StateFlow<AlarmSoundPreset> = TimerStopwatchStateManager.selectedSound

    fun selectSound(preset: AlarmSoundPreset) {
        TimerStopwatchStateManager.selectSound(preset)
    }

    // --- Background Style State ---
    val isBackgroundAnimated: StateFlow<Boolean> = TimerStopwatchStateManager.isBackgroundAnimated

    fun setBackgroundAnimated(animated: Boolean) {
        TimerStopwatchStateManager.setBackgroundAnimated(animated)
    }

    // --- Tab Selection State (UI View State only) ---
    private val _activeTab = MutableStateFlow(0) // 0: Timer, 1: Stopwatch
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun selectTab(index: Int) {
        _activeTab.value = index
    }

    // --- Always On Display Keep-Screen State ---
    val isAlwaysOn: StateFlow<Boolean> = TimerStopwatchStateManager.isAlwaysOn

    fun toggleAlwaysOn() {
        TimerStopwatchStateManager.toggleAlwaysOn()
    }

    // --- Floating Bubble Overlay State ---
    val overlayActive: StateFlow<Boolean> = TimerStopwatchStateManager.overlayActive

    fun setOverlayActive(active: Boolean) {
        TimerStopwatchStateManager.setOverlayActive(active)
    }

    // --- Alarm trigger event ---
    val alarmTriggered: StateFlow<Boolean> = TimerStopwatchStateManager.alarmTriggered

    fun setAlarmTriggered(active: Boolean) {
        TimerStopwatchStateManager.setAlarmTriggered(active)
    }

    // ==========================================
    // TIMER DELEGATIONS
    // ==========================================
    val timerInput: StateFlow<String> = TimerStopwatchStateManager.timerInput
    val timerStatus: StateFlow<TimerStatus> = TimerStopwatchStateManager.timerStatus
    val timerRemainingMs: StateFlow<Long> = TimerStopwatchStateManager.timerRemainingMs
    val timerMaxMs: StateFlow<Long> = TimerStopwatchStateManager.timerMaxMs

    fun appendDigit(digit: Int) {
        TimerStopwatchStateManager.appendDigit(digit)
    }

    fun deleteDigit() {
        TimerStopwatchStateManager.deleteDigit()
    }

    fun clearTimerInput() {
        TimerStopwatchStateManager.clearTimerInput()
    }

    fun startTimer() {
        TimerStopwatchStateManager.startTimer()
    }

    fun startPresetTimer(presetSeconds: Long) {
        TimerStopwatchStateManager.startPresetTimer(presetSeconds)
    }

    fun pauseTimer() {
        TimerStopwatchStateManager.pauseTimer()
    }

    fun resumeTimer() {
        TimerStopwatchStateManager.resumeTimer()
    }

    fun resetTimer() {
        TimerStopwatchStateManager.resetTimer()
    }

    fun addOneMinute() {
        TimerStopwatchStateManager.addOneMinute()
    }

    fun addFiveMinutes() {
        TimerStopwatchStateManager.addFiveMinutes()
    }

    fun dismissAlarm() {
        TimerStopwatchStateManager.dismissAlarm()
    }

    fun snoozeAlarm() {
        TimerStopwatchStateManager.snoozeAlarm()
    }

    // ==========================================
    // STOPWATCH DELEGATIONS
    // ==========================================
    val stopwatchStatus: StateFlow<StopwatchStatus> = TimerStopwatchStateManager.stopwatchStatus
    val stopwatchElapsedMs: StateFlow<Long> = TimerStopwatchStateManager.stopwatchElapsedMs
    val laps: StateFlow<List<LapRecord>> = TimerStopwatchStateManager.laps

    fun startStopwatch() {
        TimerStopwatchStateManager.startStopwatch()
    }

    fun pauseStopwatch() {
        TimerStopwatchStateManager.pauseStopwatch()
    }

    fun addLap() {
        TimerStopwatchStateManager.addLap()
    }

    fun resetStopwatch() {
        TimerStopwatchStateManager.resetStopwatch()
    }
}
