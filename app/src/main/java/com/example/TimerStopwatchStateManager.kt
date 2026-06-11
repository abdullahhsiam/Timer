package com.example

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat

object TimerStopwatchStateManager {

    private val job = SupervisorJob()
    private val stateScope = CoroutineScope(Dispatchers.Default + job)

    private var appContext: Context? = null

    // --- State Initialization ---
    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            restoreState()
        }
    }

    fun saveState() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("timer_stopwatch_states", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("timer_status", _timerStatus.value.name)
            putLong("timer_remaining_ms", _timerRemainingMs.value)
            putLong("timer_max_ms", _timerMaxMs.value)
            putLong("timer_target_wall_time", if (_timerStatus.value == TimerStatus.RUNNING) System.currentTimeMillis() + _timerRemainingMs.value else 0L)
            
            putString("stopwatch_status", _stopwatchStatus.value.name)
            putLong("stopwatch_elapsed_ms", _stopwatchElapsedMs.value)
            putLong("stopwatch_target_wall_time", if (_stopwatchStatus.value == StopwatchStatus.RUNNING) System.currentTimeMillis() - _stopwatchElapsedMs.value else 0L)
            
            // Pomodoro states
            putString("focus_mode_state", _focusModeState.value.name)
            putString("pomodoro_status", _pomodoroStatus.value.name)
            putLong("pomodoro_remaining_ms", _pomodoroRemainingMs.value)
            putLong("pomodoro_duration_ms", _pomodoroDurationMs.value)
            putInt("focus_default_min", _focusDefaultMin.value)
            putInt("short_break_default_min", _shortBreakDefaultMin.value)
            putInt("long_break_default_min", _longBreakDefaultMin.value)
            putLong("total_focus_time_ms", _totalFocusTimeMs.value)
            putInt("completed_focus_sessions", _completedFocusSessions.value)
            putInt("completed_break_sessions", _completedBreakSessions.value)
            putInt("manual_breaks_count", _manualBreaksCount.value)
            putInt("current_session_number", _currentSessionNumber.value)
            putLong("daily_total_focus_time_ms", _dailyTotalFocusTimeMs.value)
            putInt("daily_completed_focus_sessions", _dailyCompletedFocusSessions.value)
            putString("last_stats_day", lastStatsDay)
            putBoolean("pomodoro_notifications_enabled", _pomodoroNotificationsEnabled.value)

            // Premium custom styling params
            putFloat("glass_blur", _glassBlur.value)
            putFloat("glass_opacity", _glassOpacity.value)
            putFloat("glass_glow", _glassGlow.value)
            putInt("glass_corner_radius", _glassCornerRadius.value)
            putString("glass_tint", _glassTint.value)
            putFloat("glass_shadow", _glassShadow.value)
            putFloat("glass_animation_speed", _glassAnimationSpeed.value)
            putString("wallpaper_uri", _wallpaperUri.value)
            putInt("overlay_mode", _overlayMode.value)
            putInt("active_tab", _activeTab.value)

            // New multi-surface config saving
            putString("appearance_config_json", _appearanceConfig.value.toSerializedString())
            apply()
        }
    }

    fun restoreState() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("timer_stopwatch_states", Context.MODE_PRIVATE)
        
        _glassBlur.value = prefs.getFloat("glass_blur", 25f)
        _glassOpacity.value = prefs.getFloat("glass_opacity", 0.18f)
        _glassGlow.value = prefs.getFloat("glass_glow", 0.45f)
        _glassCornerRadius.value = prefs.getInt("glass_corner_radius", 24)
        _glassTint.value = prefs.getString("glass_tint", "lavender") ?: "lavender"
        _glassShadow.value = prefs.getFloat("glass_shadow", 4f)
        _glassAnimationSpeed.value = prefs.getFloat("glass_animation_speed", 1.0f)
        _wallpaperUri.value = prefs.getString("wallpaper_uri", "") ?: ""
        _overlayMode.value = prefs.getInt("overlay_mode", 0)
        _activeTab.value = prefs.getInt("active_tab", 0)

        // Restore appearance configs
        val appearanceJson = prefs.getString("appearance_config_json", "") ?: ""
        if (appearanceJson.isNotEmpty()) {
            _appearanceConfig.value = AppAppearanceConfig.fromSerializedString(appearanceJson)
        } else {
            _appearanceConfig.value = AppAppearanceConfig()
        }

        // Restore custom preset names list
        val presetPrefs = context.getSharedPreferences("appearance_presets", Context.MODE_PRIVATE)
        val namesStr = presetPrefs.getString("preset_names_list", "") ?: ""
        if (namesStr.isNotEmpty()) {
            _customPresetNames.value = namesStr.split(",").filter { it.isNotEmpty() }
        } else {
            _customPresetNames.value = emptyList()
        }

        // RESTORE POMODORO
        val fmsStr = prefs.getString("focus_mode_state", FocusModeState.OFF.name) ?: FocusModeState.OFF.name
        _focusModeState.value = FocusModeState.valueOf(fmsStr)

        val pomoStatusStr = prefs.getString("pomodoro_status", PomodoroStatus.IDLE.name) ?: PomodoroStatus.IDLE.name
        val savedPomoRemaining = prefs.getLong("pomodoro_remaining_ms", 0L)
        val savedPomoDuration = prefs.getLong("pomodoro_duration_ms", 0L)

        _focusDefaultMin.value = prefs.getInt("focus_default_min", 25)
        _shortBreakDefaultMin.value = prefs.getInt("short_break_default_min", 5)
        _longBreakDefaultMin.value = prefs.getInt("long_break_default_min", 15)

        _totalFocusTimeMs.value = prefs.getLong("total_focus_time_ms", 0L)
        _completedFocusSessions.value = prefs.getInt("completed_focus_sessions", 0)
        _completedBreakSessions.value = prefs.getInt("completed_break_sessions", 0)
        _manualBreaksCount.value = prefs.getInt("manual_breaks_count", 0)
        _currentSessionNumber.value = prefs.getInt("current_session_number", 1)
        
        _dailyTotalFocusTimeMs.value = prefs.getLong("daily_total_focus_time_ms", 0L)
        _dailyCompletedFocusSessions.value = prefs.getInt("daily_completed_focus_sessions", 0)
        lastStatsDay = prefs.getString("last_stats_day", "") ?: ""
        _pomodoroNotificationsEnabled.value = prefs.getBoolean("pomodoro_notifications_enabled", true)

        val pomoStatus = PomodoroStatus.valueOf(pomoStatusStr)
        _pomodoroDurationMs.value = savedPomoDuration

        if (pomoStatus == PomodoroStatus.RUNNING) {
            _pomodoroRemainingMs.value = savedPomoRemaining
            _pomodoroStatus.value = PomodoroStatus.RUNNING
            launchPomodoroJob(savedPomoRemaining)
        } else {
            _pomodoroRemainingMs.value = savedPomoRemaining
            _pomodoroStatus.value = if (pomoStatus == PomodoroStatus.RUNNING) PomodoroStatus.PAUSED else pomoStatus
        }

        // RESTORE TIMER
        val timerStatusStr = prefs.getString("timer_status", TimerStatus.IDLE.name) ?: TimerStatus.IDLE.name
        val savedTimerRemaining = prefs.getLong("timer_remaining_ms", 0L)
        val savedTimerMax = prefs.getLong("timer_max_ms", 0L)
        val targetWallTime = prefs.getLong("timer_target_wall_time", 0L)
        
        val timerStatus = TimerStatus.valueOf(timerStatusStr)
        _timerMaxMs.value = savedTimerMax

        if (timerStatus == TimerStatus.RUNNING && targetWallTime > 0L) {
            val calculatedRemaining = targetWallTime - System.currentTimeMillis()
            if (calculatedRemaining > 0L) {
                _timerRemainingMs.value = calculatedRemaining
                _timerStatus.value = TimerStatus.RUNNING
                launchTimerJob(calculatedRemaining)
            } else {
                _timerRemainingMs.value = 0L
                _timerStatus.value = TimerStatus.FINISHED
                _alarmTriggered.value = true
            }
        } else {
            _timerRemainingMs.value = savedTimerRemaining
            _timerStatus.value = if (timerStatus == TimerStatus.RUNNING) TimerStatus.PAUSED else timerStatus
        }

        // RESTORE STOPWATCH
        val swStatusStr = prefs.getString("stopwatch_status", StopwatchStatus.IDLE.name) ?: StopwatchStatus.IDLE.name
        val savedSwElapsed = prefs.getLong("stopwatch_elapsed_ms", 0L)
        val targetSwWallTime = prefs.getLong("stopwatch_target_wall_time", 0L)
        
        val swStatus = StopwatchStatus.valueOf(swStatusStr)
        if (swStatus == StopwatchStatus.RUNNING && targetSwWallTime > 0L) {
            val elapsed = System.currentTimeMillis() - targetSwWallTime
            _stopwatchElapsedMs.value = if (elapsed > 0) elapsed else 0L
            startStopwatch()
        } else {
            _stopwatchElapsedMs.value = savedSwElapsed
            _stopwatchStatus.value = swStatus
        }
    }

    // --- Glassmorphic Styling State Flows & Unified Engine ---
    private val _appearanceConfig = MutableStateFlow(AppAppearanceConfig())
    val appearanceConfig: StateFlow<AppAppearanceConfig> = _appearanceConfig.asStateFlow()

    private val _customPresetNames = MutableStateFlow<List<String>>(emptyList())
    val customPresetNames: StateFlow<List<String>> = _customPresetNames.asStateFlow()

    private val _glassBlur = MutableStateFlow(25f)
    val glassBlur: StateFlow<Float> = _glassBlur.asStateFlow()

    private val _glassOpacity = MutableStateFlow(0.18f)
    val glassOpacity: StateFlow<Float> = _glassOpacity.asStateFlow()

    private val _glassGlow = MutableStateFlow(0.45f)
    val glassGlow: StateFlow<Float> = _glassGlow.asStateFlow()

    private val _glassCornerRadius = MutableStateFlow(24)
    val glassCornerRadius: StateFlow<Int> = _glassCornerRadius.asStateFlow()

    private val _glassTint = MutableStateFlow("lavender")
    val glassTint: StateFlow<String> = _glassTint.asStateFlow()

    private val _glassShadow = MutableStateFlow(4f)
    val glassShadow: StateFlow<Float> = _glassShadow.asStateFlow()

    private val _glassAnimationSpeed = MutableStateFlow(1.0f)
    val glassAnimationSpeed: StateFlow<Float> = _glassAnimationSpeed.asStateFlow()

    private val _wallpaperUri = MutableStateFlow("")
    val wallpaperUri: StateFlow<String> = _wallpaperUri.asStateFlow()

    private val _overlayMode = MutableStateFlow(0) // 0: Auto/Dockable, 1: Floating Bubble, 2: Dynamic Island
    val overlayMode: StateFlow<Int> = _overlayMode.asStateFlow()

    fun updateAppearanceConfig(newConfig: AppAppearanceConfig) {
        _appearanceConfig.value = newConfig
        
        // Backward-compatible sync
        _glassBlur.value = newConfig.expandedBubblePanel.blur
        _glassOpacity.value = newConfig.expandedBubblePanel.opacity
        _glassGlow.value = newConfig.expandedBubblePanel.glowStrength
        _glassCornerRadius.value = newConfig.expandedBubblePanel.cornerRadius
        _glassTint.value = if (newConfig.expandedBubblePanel.borderColor == "#FF2A6D") "pink" else if (newConfig.expandedBubblePanel.borderColor == "#00E6FF") "cyan" else "lavender"
        _glassShadow.value = newConfig.expandedBubblePanel.shadowIntensity

        saveState()
        triggerWidgetUpdate()
        triggerNotificationUpdate()
    }

    fun updateComponentStyle(componentName: String, style: ComponentStyle) {
        val current = _appearanceConfig.value
        val updated = when (componentName) {
            "dockableIsland" -> current.copy(dockableIsland = style)
            "floatingBubble" -> current.copy(floatingBubble = style)
            "expandedBubblePanel" -> current.copy(expandedBubblePanel = style)
            "timerWidget" -> current.copy(timerWidget = style)
            "stopwatchWidget" -> current.copy(stopwatchWidget = style)
            "notificationControls" -> current.copy(notificationControls = style)
            else -> current
        }
        updateAppearanceConfig(updated)
    }

    fun applyPreset(presetName: String) {
        val defaultConfig = AppAppearanceConfig()
        val config = when (presetName.lowercase()) {
            "system default" -> defaultConfig
            "amoled black" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#22FFFFFF", borderThickness = 1.0f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#33FFFFFF", borderThickness = 1.0f, cornerRadius = 28, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                expandedBubblePanel = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#33FFFFFF", borderThickness = 1.0f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#33FFFFFF", borderThickness = 1.0f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                stopwatchWidget = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#33FFFFFF", borderThickness = 1.0f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                notificationControls = ComponentStyle(bgColor = "#000000", opacity = 1.0f, textColor = "#FFFFFF", accentColor = "#FFFFFF")
            )
            "use wallpaper background" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.95f, blur = 0f, borderColor = "#40FFFFFF", borderThickness = 1.0f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#1A1A24", opacity = 0.35f, blur = 25f, borderColor = "#60FFFFFF", borderThickness = 1.0f, cornerRadius = 28, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 6f, wallpaperAware = true),
                expandedBubblePanel = ComponentStyle(bgColor = "#1A1A24", opacity = 0.40f, blur = 30f, borderColor = "#60FFFFFF", borderThickness = 1.0f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f, wallpaperAware = true),
                timerWidget = ComponentStyle(bgColor = "#1A1A24", opacity = 0.35f, blur = 20f, borderColor = "#50FFFFFF", borderThickness = 1.0f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 4f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#1A1A24", opacity = 0.35f, blur = 20f, borderColor = "#50FFFFFF", borderThickness = 1.0f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 4f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#0A0A0E", opacity = 0.50f, textColor = "#FFFFFF", accentColor = "#FFFFFF")
            )
            else -> defaultConfig
        }

        // Fulfill AMOLED-black priority requirement:
        // "The Dockable Island must always default to an AMOLED-black design with minimal transparency, subtle glow, high contrast, and a premium Android 15/Dynamic Island aesthetic."
        val finalConfig = config.copy(
            dockableIsland = config.dockableIsland.copy(
                bgColor = "#000000",
                opacity = config.dockableIsland.opacity.coerceAtLeast(0.92f), // Must be high contrast AMOLED black
                gradientEnabled = false
            )
        )
        updateAppearanceConfig(finalConfig)
    }

    fun saveAsCustomPreset(name: String) {
        val context = appContext ?: return
        val currentJson = _appearanceConfig.value.toSerializedString()
        val prefs = context.getSharedPreferences("appearance_presets", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("preset_config_$name", currentJson)
            
            // Update indices
            val currentNames = _customPresetNames.value.toMutableList()
            if (!currentNames.contains(name)) {
                currentNames.add(name)
                putString("preset_names_list", currentNames.joinToString(","))
                _customPresetNames.value = currentNames
            }
            apply()
        }
    }

    fun deleteCustomPreset(name: String) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("appearance_presets", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("preset_config_$name")
            val currentNames = _customPresetNames.value.toMutableList()
            if (currentNames.remove(name)) {
                putString("preset_names_list", currentNames.joinToString(","))
                _customPresetNames.value = currentNames
            }
            apply()
        }
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
        _glassBlur.value = blur
        _glassOpacity.value = opacity
        _glassGlow.value = glow
        _glassCornerRadius.value = cornerRadius
        _glassTint.value = tint
        _glassShadow.value = shadow
        _glassAnimationSpeed.value = animSpeed
        saveState()
        triggerWidgetUpdate()
    }

    fun setWallpaperUri(uri: String) {
        _wallpaperUri.value = uri
        saveState()
    }

    fun setOverlayMode(mode: Int) {
        _overlayMode.value = mode
        saveState()
        triggerWidgetUpdate()
        val context = appContext
        if (context != null) {
            val intent = Intent(context, OverlayBubbleService::class.java)
            if (mode == 2) {
                context.stopService(intent)
            } else {
                if (android.provider.Settings.canDrawOverlays(context)) {
                    try {
                        context.startService(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun selectTab(index: Int) {
        _activeTab.value = index
        saveState()
        triggerWidgetUpdate()
    }

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

    // --- Floating Bubble Overlay State ---
    private val _overlayActive = MutableStateFlow(false)
    val overlayActive: StateFlow<Boolean> = _overlayActive.asStateFlow()

    fun setOverlayActive(active: Boolean) {
        _overlayActive.value = active
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
        if (_timerStatus.value == TimerStatus.IDLE || _timerStatus.value == TimerStatus.FINISHED) {
            startPresetTimer(300L) // Start standard default 5m timer
            return
        }
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

    fun addMinutes(minutes: Int) {
        val increment = minutes * 60_000L
        if (_timerStatus.value == TimerStatus.IDLE || _timerStatus.value == TimerStatus.FINISHED) {
            if (_alarmTriggered.value) {
                _alarmTriggered.value = false
            }
            _timerMaxMs.value = increment
            _timerRemainingMs.value = increment
            _timerStatus.value = TimerStatus.RUNNING
            launchTimerJob(increment)
            notifyServiceOfStateChange()
        } else {
            _timerMaxMs.value += increment
            val newRemaining = _timerRemainingMs.value + increment
            _timerRemainingMs.value = newRemaining
            if (_timerStatus.value == TimerStatus.FINISHED) {
                _timerStatus.value = TimerStatus.RUNNING
                _alarmTriggered.value = false
                launchTimerJob(newRemaining)
                notifyServiceOfStateChange()
            } else if (_timerStatus.value == TimerStatus.RUNNING) {
                launchTimerJob(newRemaining)
            } else {
                triggerNotificationUpdate()
                triggerWidgetUpdate()
                notifyServiceOfStateChange()
            }
        }
    }

    fun addOneMinute() {
        addMinutes(1)
    }

    fun addFiveMinutes() {
        addMinutes(5)
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

                delay(16)
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
    // POMODORO STATE & ENGINE
    // ==========================================
    private val _focusModeState = MutableStateFlow(FocusModeState.OFF)
    val focusModeState: StateFlow<FocusModeState> = _focusModeState.asStateFlow()

    private val _pomodoroStatus = MutableStateFlow(PomodoroStatus.IDLE)
    val pomodoroStatus: StateFlow<PomodoroStatus> = _pomodoroStatus.asStateFlow()

    private val _pomodoroRemainingMs = MutableStateFlow(0L)
    val pomodoroRemainingMs: StateFlow<Long> = _pomodoroRemainingMs.asStateFlow()

    private val _pomodoroDurationMs = MutableStateFlow(0L)
    val pomodoroDurationMs: StateFlow<Long> = _pomodoroDurationMs.asStateFlow()

    // Configurable defaults (minutes)
    private val _focusDefaultMin = MutableStateFlow(25)
    val focusDefaultMin: StateFlow<Int> = _focusDefaultMin.asStateFlow()

    private val _shortBreakDefaultMin = MutableStateFlow(5)
    val shortBreakDefaultMin: StateFlow<Int> = _shortBreakDefaultMin.asStateFlow()

    private val _longBreakDefaultMin = MutableStateFlow(15)
    val longBreakDefaultMin: StateFlow<Int> = _longBreakDefaultMin.asStateFlow()

    // Statistics
    private val _totalFocusTimeMs = MutableStateFlow(0L)
    val totalFocusTimeMs: StateFlow<Long> = _totalFocusTimeMs.asStateFlow()

    private val _completedFocusSessions = MutableStateFlow(0)
    val completedFocusSessions: StateFlow<Int> = _completedFocusSessions.asStateFlow()

    private val _completedBreakSessions = MutableStateFlow(0)
    val completedBreakSessions: StateFlow<Int> = _completedBreakSessions.asStateFlow()

    private val _manualBreaksCount = MutableStateFlow(0)
    val manualBreaksCount: StateFlow<Int> = _manualBreaksCount.asStateFlow()

    private val _currentSessionNumber = MutableStateFlow(1) // 1 to 4
    val currentSessionNumber: StateFlow<Int> = _currentSessionNumber.asStateFlow()

    private val _dailyTotalFocusTimeMs = MutableStateFlow(0L)
    val dailyTotalFocusTimeMs: StateFlow<Long> = _dailyTotalFocusTimeMs.asStateFlow()

    private val _dailyCompletedFocusSessions = MutableStateFlow(0)
    val dailyCompletedFocusSessions: StateFlow<Int> = _dailyCompletedFocusSessions.asStateFlow()

    private var lastStatsDay = ""

    private val _pomodoroNotificationsEnabled = MutableStateFlow(true)
    val pomodoroNotificationsEnabled: StateFlow<Boolean> = _pomodoroNotificationsEnabled.asStateFlow()

    private var pomodoroJob: Job? = null

    fun setPomodoroNotificationsEnabled(enabled: Boolean) {
        _pomodoroNotificationsEnabled.value = enabled
        saveState()
    }

    fun setPomodoroDurations(focus: Int, shortBreak: Int, longBreak: Int) {
        _focusDefaultMin.value = focus
        _shortBreakDefaultMin.value = shortBreak
        _longBreakDefaultMin.value = longBreak
        saveState()
        if (_pomodoroStatus.value == PomodoroStatus.IDLE || _pomodoroStatus.value == PomodoroStatus.PAUSED) {
            resetPomodoro()
        }
    }

    fun getCycleDurationMs(state: FocusModeState): Long {
        return when (state) {
            FocusModeState.OFF -> 0L
            FocusModeState.FOCUS -> _focusDefaultMin.value * 60_000L
            FocusModeState.BREAK -> {
                val completed = _completedFocusSessions.value
                val isLongBreak = completed > 0 && completed % 4 == 0
                if (isLongBreak) {
                    _longBreakDefaultMin.value * 60_000L
                } else {
                    _shortBreakDefaultMin.value * 60_000L
                }
            }
        }
    }

    private fun checkAndResetDailyStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (lastStatsDay != today) {
            lastStatsDay = today
            _dailyTotalFocusTimeMs.value = 0L
            _dailyCompletedFocusSessions.value = 0
            saveState()
        }
    }

    fun setFocusModeState(newState: FocusModeState) {
        if (_focusModeState.value == newState) return
        pomodoroJob?.cancel()
        _focusModeState.value = newState
        
        if (newState == FocusModeState.OFF) {
            _pomodoroStatus.value = PomodoroStatus.IDLE
            _pomodoroRemainingMs.value = 0L
            _pomodoroDurationMs.value = 0L
        } else {
            val duration = getCycleDurationMs(newState)
            _pomodoroRemainingMs.value = duration
            _pomodoroDurationMs.value = duration
            _pomodoroStatus.value = PomodoroStatus.IDLE
        }
        saveState()
        notifyServiceOfStateChange()
    }

    fun startPomodoro() {
        if (_focusModeState.value == FocusModeState.OFF) {
            setFocusModeState(FocusModeState.FOCUS)
        }
        _pomodoroStatus.value = PomodoroStatus.RUNNING
        launchPomodoroJob(_pomodoroRemainingMs.value)
        notifyServiceOfStateChange()
    }

    fun pausePomodoro() {
        if (_pomodoroStatus.value != PomodoroStatus.RUNNING) return
        pomodoroJob?.cancel()
        _pomodoroStatus.value = PomodoroStatus.PAUSED
        notifyServiceOfStateChange()
    }

    fun resumePomodoro() {
        if (_pomodoroStatus.value != PomodoroStatus.PAUSED) return
        _pomodoroStatus.value = PomodoroStatus.RUNNING
        launchPomodoroJob(_pomodoroRemainingMs.value)
        notifyServiceOfStateChange()
        _completedFocusSessions.let {
            sendOptionalNotification("Session Resumed", "Your pomodoro timer is active again.")
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        _pomodoroStatus.value = PomodoroStatus.IDLE
        val duration = getCycleDurationMs(_focusModeState.value)
        _pomodoroRemainingMs.value = duration
        _pomodoroDurationMs.value = duration
        notifyServiceOfStateChange()
    }

    fun skipBreak() {
        if (_focusModeState.value != FocusModeState.BREAK) return
        pomodoroJob?.cancel()
        
        _focusModeState.value = FocusModeState.FOCUS
        val duration = _focusDefaultMin.value * 60_000L
        _pomodoroRemainingMs.value = duration
        _pomodoroDurationMs.value = duration
        _pomodoroStatus.value = PomodoroStatus.RUNNING
        
        launchPomodoroJob(duration)
        saveState()
        notifyServiceOfStateChange()
        sendOptionalNotification("Focus Started", "Ready to focus for " + _focusDefaultMin.value + " minutes.")
    }

    fun startBreakManually() {
        pomodoroJob?.cancel()
        _manualBreaksCount.value += 1
        
        _focusModeState.value = FocusModeState.BREAK
        val duration = getCycleDurationMs(FocusModeState.BREAK)
        _pomodoroRemainingMs.value = duration
        _pomodoroDurationMs.value = duration
        _pomodoroStatus.value = PomodoroStatus.RUNNING
        
        launchPomodoroJob(duration)
        saveState()
        notifyServiceOfStateChange()
        sendOptionalNotification("Break Started", "Manual break entered successfully.")
    }

    private fun launchPomodoroJob(initialMs: Long) {
        pomodoroJob?.cancel()
        pomodoroJob = stateScope.launch {
            var remaining = initialMs
            val startTime = SystemClock.elapsedRealtime()
            val targetTime = startTime + remaining
            var lastNotifyTime = startTime

            while (remaining > 0) {
                if (_pomodoroStatus.value != PomodoroStatus.RUNNING) break
                val now = SystemClock.elapsedRealtime()
                remaining = targetTime - now
                if (remaining < 0) remaining = 0L
                _pomodoroRemainingMs.value = remaining

                if (now - lastNotifyTime >= 1000L) {
                    lastNotifyTime = now
                    triggerWidgetUpdate()
                }
                delay(16)
            }

            if (remaining <= 0) {
                onPomodoroSessionComplete()
            }
        }
    }

    private fun onPomodoroSessionComplete() {
        val currentType = _focusModeState.value
        if (currentType == FocusModeState.FOCUS) {
            val focusCompletedCount = _completedFocusSessions.value + 1
            _completedFocusSessions.value = focusCompletedCount
            
            val focusSessionMs = _focusDefaultMin.value * 60_000L
            _totalFocusTimeMs.value += focusSessionMs
            _currentSessionNumber.value = (focusCompletedCount % 4) + 1

            checkAndResetDailyStats()
            _dailyCompletedFocusSessions.value += 1
            _dailyTotalFocusTimeMs.value += focusSessionMs

            val isLongBreak = focusCompletedCount > 0 && focusCompletedCount % 4 == 0
            
            _focusModeState.value = FocusModeState.BREAK
            val breakDuration = if (isLongBreak) {
                _longBreakDefaultMin.value * 60_000L
            } else {
                _shortBreakDefaultMin.value * 60_000L
            }
            
            _pomodoroRemainingMs.value = breakDuration
            _pomodoroDurationMs.value = breakDuration
            _pomodoroStatus.value = PomodoroStatus.RUNNING
            
            if (isLongBreak) {
                sendOptionalNotification("Long Break Started", "Amazing job! Enjoy a " + _longBreakDefaultMin.value + "-minute rest.")
            } else {
                sendOptionalNotification("Focus Complete", "Great focus! Take a short " + _shortBreakDefaultMin.value + "-minute break.")
            }

            launchPomodoroJob(breakDuration)
        } else if (currentType == FocusModeState.BREAK) {
            _completedBreakSessions.value += 1
            
            _focusModeState.value = FocusModeState.FOCUS
            val focusDuration = _focusDefaultMin.value * 60_000L
            _pomodoroRemainingMs.value = focusDuration
            _pomodoroDurationMs.value = focusDuration
            _pomodoroStatus.value = PomodoroStatus.RUNNING
            
            sendOptionalNotification("Break Complete", "Time to focus! Let's get " + _focusDefaultMin.value + " minutes of work done.")
            
            launchPomodoroJob(focusDuration)
        }
        saveState()
        notifyServiceOfStateChange()
    }

    private fun sendOptionalNotification(title: String, text: String) {
        if (!_pomodoroNotificationsEnabled.value) return
        val context = appContext ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "pomodoro_notification_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pomodoro Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for Pomodoro state transitions"
            }
            manager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            
        manager.notify(2027, builder.build())
    }

    // ==========================================
    // NOTIFICATION & SERVICE SIGNALING
    // ==========================================
    private fun notifyServiceOfStateChange() {
        val context = appContext ?: return
        
        saveState() // Keep states persisted in real time
        
        val isTimerRunning = _timerStatus.value == TimerStatus.RUNNING || _timerStatus.value == TimerStatus.PAUSED || _timerStatus.value == TimerStatus.FINISHED
        val isStopwatchRunning = _stopwatchStatus.value == StopwatchStatus.RUNNING || _stopwatchStatus.value == StopwatchStatus.PAUSED
        val isPomodoroRunning = _pomodoroStatus.value == PomodoroStatus.RUNNING || _pomodoroStatus.value == PomodoroStatus.PAUSED

        if (isTimerRunning || isStopwatchRunning || isPomodoroRunning) {
            val serviceIntent = Intent(context, TimerStopwatchService::class.java).apply {
                action = TimerStopwatchService.ACTION_UPDATE_SERVICE
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            // Stop the foreground service if all are idle
            val serviceIntent = Intent(context, TimerStopwatchService::class.java)
            context.stopService(serviceIntent)
        }
    }

    private fun triggerNotificationUpdate() {
        // No-op. The TimerStopwatchService now drives its own periodic notification updates
        // directly from its active foreground instance, completely securing background limits.
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
