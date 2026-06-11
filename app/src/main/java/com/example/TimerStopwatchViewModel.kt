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

    // --- Tab Selection State ---
    val activeTab: StateFlow<Int> = TimerStopwatchStateManager.activeTab

    fun selectTab(index: Int) {
        TimerStopwatchStateManager.selectTab(index)
    }

    // --- Visual Mode State (0 = Circular, 1 = Flip Clock) ---
    private val _activeVisualMode = MutableStateFlow(0)
    val activeVisualMode: StateFlow<Int> = _activeVisualMode.asStateFlow()

    fun selectVisualMode(index: Int) {
        _activeVisualMode.value = index
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

    // ==========================================
    // POMODORO DELEGATIONS
    // ==========================================
    val focusModeState: StateFlow<FocusModeState> = TimerStopwatchStateManager.focusModeState
    val pomodoroStatus: StateFlow<PomodoroStatus> = TimerStopwatchStateManager.pomodoroStatus
    val pomodoroRemainingMs: StateFlow<Long> = TimerStopwatchStateManager.pomodoroRemainingMs
    val pomodoroDurationMs: StateFlow<Long> = TimerStopwatchStateManager.pomodoroDurationMs
    
    val focusDefaultMin: StateFlow<Int> = TimerStopwatchStateManager.focusDefaultMin
    val shortBreakDefaultMin: StateFlow<Int> = TimerStopwatchStateManager.shortBreakDefaultMin
    val longBreakDefaultMin: StateFlow<Int> = TimerStopwatchStateManager.longBreakDefaultMin

    val totalFocusTimeMs: StateFlow<Long> = TimerStopwatchStateManager.totalFocusTimeMs
    val completedFocusSessions: StateFlow<Int> = TimerStopwatchStateManager.completedFocusSessions
    val completedBreakSessions: StateFlow<Int> = TimerStopwatchStateManager.completedBreakSessions
    val manualBreaksCount: StateFlow<Int> = TimerStopwatchStateManager.manualBreaksCount
    val currentSessionNumber: StateFlow<Int> = TimerStopwatchStateManager.currentSessionNumber
    val dailyTotalFocusTimeMs: StateFlow<Long> = TimerStopwatchStateManager.dailyTotalFocusTimeMs
    val dailyCompletedFocusSessions: StateFlow<Int> = TimerStopwatchStateManager.dailyCompletedFocusSessions
    val dailyTotalBreakTimeMs: StateFlow<Long> = TimerStopwatchStateManager.dailyTotalBreakTimeMs
    val totalPomodoroCyclesCompleted: StateFlow<Int> = TimerStopwatchStateManager.totalPomodoroCyclesCompleted
    val pomodoroNotificationsEnabled: StateFlow<Boolean> = TimerStopwatchStateManager.pomodoroNotificationsEnabled

    fun setFocusModeState(state: FocusModeState) {
        TimerStopwatchStateManager.setFocusModeState(state)
    }

    fun startPomodoro() {
        TimerStopwatchStateManager.startPomodoro()
    }

    fun pausePomodoro() {
        TimerStopwatchStateManager.pausePomodoro()
    }

    fun resumePomodoro() {
        TimerStopwatchStateManager.resumePomodoro()
    }

    fun resetPomodoro() {
        TimerStopwatchStateManager.resetPomodoro()
    }

    fun skipBreak() {
        TimerStopwatchStateManager.skipBreak()
    }

    fun startBreakManually() {
        TimerStopwatchStateManager.startBreakManually()
    }

    fun setPomodoroNotificationsEnabled(enabled: Boolean) {
        TimerStopwatchStateManager.setPomodoroNotificationsEnabled(enabled)
    }

    fun setPomodoroDurations(focus: Int, shortBreak: Int, longBreak: Int) {
        TimerStopwatchStateManager.setPomodoroDurations(focus, shortBreak, longBreak)
    }

    // ==========================================
    // STYLING & ECOSYSTEM DELEGATIONS
    // ==========================================
    val glassBlur: StateFlow<Float> = TimerStopwatchStateManager.glassBlur
    val glassOpacity: StateFlow<Float> = TimerStopwatchStateManager.glassOpacity
    val glassGlow: StateFlow<Float> = TimerStopwatchStateManager.glassGlow
    val glassCornerRadius: StateFlow<Int> = TimerStopwatchStateManager.glassCornerRadius
    val glassTint: StateFlow<String> = TimerStopwatchStateManager.glassTint
    val glassShadow: StateFlow<Float> = TimerStopwatchStateManager.glassShadow
    val glassAnimationSpeed: StateFlow<Float> = TimerStopwatchStateManager.glassAnimationSpeed
    val wallpaperUri: StateFlow<String> = TimerStopwatchStateManager.wallpaperUri
    val overlayMode: StateFlow<Int> = TimerStopwatchStateManager.overlayMode

    val appearanceConfigState: StateFlow<AppAppearanceConfig> = TimerStopwatchStateManager.appearanceConfig
    val customPresetNames: StateFlow<List<String>> = TimerStopwatchStateManager.customPresetNames

    fun updateAppearanceConfig(config: AppAppearanceConfig) {
        TimerStopwatchStateManager.updateAppearanceConfig(config)
    }

    fun updateComponentStyle(componentName: String, style: ComponentStyle) {
        TimerStopwatchStateManager.updateComponentStyle(componentName, style)
    }

    fun applyPreset(presetName: String) {
        TimerStopwatchStateManager.applyPreset(presetName)
    }

    fun saveAsCustomPreset(name: String) {
        TimerStopwatchStateManager.saveAsCustomPreset(name)
    }

    fun deleteCustomPreset(name: String) {
        TimerStopwatchStateManager.deleteCustomPreset(name)
    }

    fun updateStyleOptions(
        blur: Float,
        opacity: Float,
        glow: Float,
        cornerRadius: Int,
        tint: String,
        shadow: Float,
        animSpeed: Float
    ) {
        TimerStopwatchStateManager.updateStyleOptions(
            blur = blur,
            opacity = opacity,
            glow = glow,
            cornerRadius = cornerRadius,
            tint = tint,
            shadow = shadow,
            animSpeed = animSpeed
        )
    }

    fun setWallpaperUri(uri: String) {
        TimerStopwatchStateManager.setWallpaperUri(uri)
    }

    fun setOverlayMode(mode: Int) {
        TimerStopwatchStateManager.setOverlayMode(mode)
    }
}
