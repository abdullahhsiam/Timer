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
            "default glass" -> defaultConfig
            "amoled black", "amoled slate" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.98f, blur = 0f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.05f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#000000", opacity = 0.98f, blur = 0f, borderColor = "#2C2C2E", borderThickness = 1.0f, glowColor = "#CCCCCC", glowStrength = 0.05f, cornerRadius = 28, textColor = "#E5E5EA", accentColor = "#FFFFFF", shadowIntensity = 6f),
                expandedBubblePanel = ComponentStyle(bgColor = "#000000", opacity = 0.98f, blur = 0f, borderColor = "#2C2C2E", borderThickness = 1.2f, glowColor = "#CCCCCC", glowStrength = 0.05f, cornerRadius = 24, textColor = "#E5E5EA", accentColor = "#FFFFFF", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#222222", borderThickness = 1f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#FFFFFF", glowStrength = 0f),
                stopwatchWidget = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#222222", borderThickness = 1f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#FFFFFF", glowStrength = 0f),
                notificationControls = ComponentStyle(bgColor = "#000000", opacity = 1.0f, textColor = "#FFFFFF", accentColor = "#FFFFFF")
            )
            "visionos glass" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.1f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFFFFF", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#FFFFFF", opacity = 0.22f, blur = 32f, borderColor = "#FFFFFF", borderThickness = 1.5f, glowColor = "#FFFFFF", glowStrength = 0.35f, cornerRadius = 28, textColor = "#FFFFFF", accentColor = "#A5F3FC", shadowIntensity = 4f),
                expandedBubblePanel = ComponentStyle(bgColor = "#FFFFFF", opacity = 0.25f, blur = 35f, borderColor = "#FFFFFF", borderThickness = 1.5f, glowColor = "#FFFFFF", glowStrength = 0.40f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#A5F3FC", shadowIntensity = 5f),
                timerWidget = ComponentStyle(bgColor = "#FFFFFF", opacity = 0.30f, blur = 25f, borderColor = "#FFFFFF", borderThickness = 1.2f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#A5F3FC", shadowIntensity = 3f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#FFFFFF", opacity = 0.30f, blur = 25f, borderColor = "#FFFFFF", borderThickness = 1.2f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#A5F3FC", shadowIntensity = 3f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#FFFFFF", opacity = 0.40f, textColor = "#FFFFFF", accentColor = "#93C5FD")
            )
            "ios glass" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.1f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#E5E5EA", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#1C1C1E", opacity = 0.55f, blur = 25f, borderColor = "#545456", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.15f, cornerRadius = 28, textColor = "#FFFFFF", accentColor = "#0A84FF", shadowIntensity = 6f),
                expandedBubblePanel = ComponentStyle(bgColor = "#1C1C1E", opacity = 0.60f, blur = 28f, borderColor = "#545456", borderThickness = 1.2f, glowColor = "#FFFFFF", glowStrength = 0.15f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#0A84FF", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#1C1C1E", opacity = 0.65f, blur = 22f, borderColor = "#48484A", borderThickness = 1f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#0A84FF", shadowIntensity = 4f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#1C1C1E", opacity = 0.65f, blur = 22f, borderColor = "#48484A", borderThickness = 1f, cornerRadius = 20, textColor = "#FFFFFF", accentColor = "#0A84FF", shadowIntensity = 4f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#1C1C1E", opacity = 0.8f, textColor = "#FFFFFF", accentColor = "#0A84FF")
            )
            "material you" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.1f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FFD8E4", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#31101D", opacity = 0.70f, blur = 18f, borderColor = "#FFD8E4", borderThickness = 1.2f, glowColor = "#EC4899", glowStrength = 0.20f, cornerRadius = 28, textColor = "#FFD8E4", accentColor = "#F9A8D4", shadowIntensity = 6f),
                expandedBubblePanel = ComponentStyle(bgColor = "#31101D", opacity = 0.75f, blur = 20f, borderColor = "#FFD8E4", borderThickness = 1.2f, glowColor = "#EC4899", glowStrength = 0.20f, cornerRadius = 24, textColor = "#FFD8E4", accentColor = "#F9A8D4", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#2D1622", opacity = 0.85f, blur = 15f, borderColor = "#F472B6", borderThickness = 1f, cornerRadius = 22, textColor = "#FFF1F2", accentColor = "#F472B6", shadowIntensity = 4f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#2D1622", opacity = 0.85f, blur = 15f, borderColor = "#F472B6", borderThickness = 1f, cornerRadius = 22, textColor = "#FFF1F2", accentColor = "#F472B6", shadowIntensity = 4f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#2D1622", opacity = 1.0f, textColor = "#FFD8E4", accentColor = "#F472B6")
            )
            "hyperos" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.1f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#38BDF8", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#111827", opacity = 0.80f, blur = 14f, borderColor = "#374151", borderThickness = 1.0f, glowColor = "#0284C7", glowStrength = 0.15f, cornerRadius = 24, textColor = "#F3F4F6", accentColor = "#38BDF8", shadowIntensity = 5f),
                expandedBubblePanel = ComponentStyle(bgColor = "#111827", opacity = 0.85f, blur = 16f, borderColor = "#374151", borderThickness = 1.0f, glowColor = "#0284C7", glowStrength = 0.15f, cornerRadius = 20, textColor = "#F3F4F6", accentColor = "#38BDF8", shadowIntensity = 7f),
                timerWidget = ComponentStyle(bgColor = "#111827", opacity = 0.85f, blur = 12f, borderColor = "#1F2937", borderThickness = 1f, cornerRadius = 18, textColor = "#F9FAFB", accentColor = "#38BDF8", shadowIntensity = 3f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#111827", opacity = 0.85f, blur = 12f, borderColor = "#1F2937", borderThickness = 1f, cornerRadius = 18, textColor = "#F9FAFB", accentColor = "#38BDF8", shadowIntensity = 3f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#111827", opacity = 1.0f, textColor = "#F3F4F6", accentColor = "#38BDF8")
            )
            "one ui" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.1f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#60A5FA", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#1A1D26", opacity = 0.72f, blur = 24f, borderColor = "#3B82F6", borderThickness = 1.2f, glowColor = "#2563EB", glowStrength = 0.25f, cornerRadius = 28, textColor = "#F8FAFC", accentColor = "#60A5FA", shadowIntensity = 6f),
                expandedBubblePanel = ComponentStyle(bgColor = "#1A1D26", opacity = 0.76f, blur = 26f, borderColor = "#3B82F6", borderThickness = 1.2f, glowColor = "#2563EB", glowStrength = 0.25f, cornerRadius = 24, textColor = "#F8FAFC", accentColor = "#60A5FA", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#1E293B", opacity = 0.80f, blur = 18f, borderColor = "#475569", borderThickness = 1f, cornerRadius = 24, textColor = "#F1F5F9", accentColor = "#60A5FA", shadowIntensity = 4f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#1E293B", opacity = 0.80f, blur = 18f, borderColor = "#475569", borderThickness = 1f, cornerRadius = 24, textColor = "#F1F5F9", accentColor = "#60A5FA", shadowIntensity = 4f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#1E293B", opacity = 1.0f, textColor = "#F1F5F9", accentColor = "#3B82F6")
            )
            "nothing os" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.98f, blur = 0f, borderColor = "#FFFFFF", borderThickness = 1.0f, glowColor = "#000000", glowStrength = 0f, cornerRadius = 14, textColor = "#FFFFFF", accentColor = "#FF3333", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 0f, borderColor = "#FFFFFF", borderThickness = 1.0f, glowColor = "#000000", glowStrength = 0f, cornerRadius = 12, textColor = "#FFFFFF", accentColor = "#FF3333", shadowIntensity = 6f),
                expandedBubblePanel = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 0f, borderColor = "#FFFFFF", borderThickness = 1.0f, glowColor = "#000000", glowStrength = 0f, cornerRadius = 12, textColor = "#FFFFFF", accentColor = "#FF3333", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#FFFFFF", borderThickness = 1.2f, cornerRadius = 14, textColor = "#FFFFFF", accentColor = "#FF3333", shadowIntensity = 4f),
                stopwatchWidget = ComponentStyle(bgColor = "#000000", opacity = 1.0f, blur = 0f, borderColor = "#FFFFFF", borderThickness = 1.2f, cornerRadius = 14, textColor = "#FFFFFF", accentColor = "#FF3333", shadowIntensity = 4f),
                notificationControls = ComponentStyle(bgColor = "#000000", opacity = 1.0f, textColor = "#FFFFFF", accentColor = "#FF3333")
            )
            "frosted ice" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.0f, glowColor = "#FFFFFF", glowStrength = 0.1f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#38BDF8", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#E0F2FE", opacity = 0.38f, blur = 32f, borderColor = "#BAE6FD", borderThickness = 1.2f, glowColor = "#38BDF8", glowStrength = 0.45f, cornerRadius = 28, textColor = "#0369A1", accentColor = "#0EA5E9", shadowIntensity = 6f),
                expandedBubblePanel = ComponentStyle(bgColor = "#E0F2FE", opacity = 0.42f, blur = 35f, borderColor = "#BAE6FD", borderThickness = 1.2f, glowColor = "#38BDF8", glowStrength = 0.45f, cornerRadius = 24, textColor = "#0369A1", accentColor = "#0EA5E9", shadowIntensity = 8f),
                timerWidget = ComponentStyle(bgColor = "#F0F9FF", opacity = 0.45f, blur = 25f, borderColor = "#7DD3FC", borderThickness = 1f, cornerRadius = 20, textColor = "#0369A1", accentColor = "#0ea5e9", shadowIntensity = 3f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#F0F9FF", opacity = 0.45f, blur = 25f, borderColor = "#7DD3FC", borderThickness = 1f, cornerRadius = 20, textColor = "#0369A1", accentColor = "#0ea5e9", shadowIntensity = 3f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#E0F2FE", opacity = 0.6f, textColor = "#0369A1", accentColor = "#0EA5E9")
            )
            "emerald dream" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.2f, glowColor = "#FFFFFF", glowStrength = 0.15f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#34D399", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#042F1A", opacity = 0.72f, blur = 20f, borderColor = "#34D399", borderThickness = 1.0f, glowColor = "#10B981", glowStrength = 0.45f, cornerRadius = 28, textColor = "#ECFDF5", accentColor = "#34D399", shadowIntensity = 6f, gradientEnabled = true, gradientStartColor = "#022C1A", gradientEndColor = "#020B14"),
                expandedBubblePanel = ComponentStyle(bgColor = "#042F1A", opacity = 0.75f, blur = 25f, borderColor = "#34D399", borderThickness = 1.2f, glowColor = "#10B981", glowStrength = 0.45f, cornerRadius = 24, textColor = "#ECFDF5", accentColor = "#34D399", shadowIntensity = 8f, gradientEnabled = true, gradientStartColor = "#022C1A", gradientEndColor = "#020B14"),
                timerWidget = ComponentStyle(bgColor = "#022C1A", opacity = 0.85f, blur = 15f, borderColor = "#059669", borderThickness = 1f, cornerRadius = 20, textColor = "#ECFDF5", accentColor = "#34D399", shadowIntensity = 3f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#022C1A", opacity = 0.85f, blur = 15f, borderColor = "#059669", borderThickness = 1f, cornerRadius = 20, textColor = "#ECFDF5", accentColor = "#34D399", shadowIntensity = 3f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#022C1A", opacity = 1.0f, textColor = "#ECFDF5", accentColor = "#34D399")
            )
            "neon cyberpunk" -> AppAppearanceConfig(
                dockableIsland = ComponentStyle(bgColor = "#000000", opacity = 0.96f, blur = 4f, borderColor = "#1A1A1A", borderThickness = 1.2f, glowColor = "#FFFFFF", glowStrength = 0.15f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#FF007F", shadowIntensity = 8f),
                floatingBubble = ComponentStyle(bgColor = "#0B0214", opacity = 0.72f, blur = 22f, borderColor = "#FF2A6D", borderThickness = 1.5f, glowColor = "#FF2A6D", glowStrength = 0.55f, cornerRadius = 28, textColor = "#FFFFFF", accentColor = "#00E6FF", shadowIntensity = 10f, gradientEnabled = true, gradientStartColor = "#1B0230", gradientEndColor = "#010714"),
                expandedBubblePanel = ComponentStyle(bgColor = "#0B0214", opacity = 0.76f, blur = 26f, borderColor = "#FF2A6D", borderThickness = 1.5f, glowColor = "#FF2A6D", glowStrength = 0.60f, cornerRadius = 24, textColor = "#FFFFFF", accentColor = "#00E6FF", shadowIntensity = 12f, gradientEnabled = true, gradientStartColor = "#1B0230", gradientEndColor = "#010714"),
                timerWidget = ComponentStyle(bgColor = "#04001A", opacity = 0.90f, blur = 14f, borderColor = "#FF2A6D", borderThickness = 1.2f, glowColor = "#00E6FF", glowStrength = 0.45f, cornerRadius = 18, textColor = "#00FF87", accentColor = "#FF2A6D", shadowIntensity = 6f, wallpaperAware = true),
                stopwatchWidget = ComponentStyle(bgColor = "#04001A", opacity = 0.90f, blur = 14f, borderColor = "#FF2A6D", borderThickness = 1.2f, glowColor = "#00E6FF", glowStrength = 0.45f, cornerRadius = 18, textColor = "#00FF87", accentColor = "#FF2A6D", shadowIntensity = 6f, wallpaperAware = true),
                notificationControls = ComponentStyle(bgColor = "#04001A", opacity = 1.0f, textColor = "#00FF87", accentColor = "#FF2A6D")
            )
            else -> defaultConfig
        }

        // Fulfill AMOLED-black priority requirement:
        // "The Dockable Island must always default to an AMOLED-black design with minimal transparency, subtle glow, high contrast, and a premium Android 15/Dynamic Island aesthetic."
        // We override dockableIsland settings if it gets configured inappropriately to retain its essential status-bar notch premium aura
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
        
        saveState() // Keep states persisted in real time
        
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
