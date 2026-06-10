package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayBubbleService : Service() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)
    private var updateJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        TimerStopwatchStateManager.setOverlayActive(true)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlayBubble()
        observeTimerStopwatch()

        // Sync and reconfigure layout based on dynamic style settings
        serviceScope.launch {
            TimerStopwatchStateManager.overlayMode.collectLatest { mode ->
                if (mode == 2) {
                    stopSelf()
                } else {
                    reconfigureLayoutParams(mode)
                }
            }
        }

        // Live Dynamic Appearance synchronization for both Collapsed & Expanded states
        serviceScope.launch {
            combine(
                TimerStopwatchStateManager.overlayMode,
                TimerStopwatchStateManager.appearanceConfig
            ) { mode, config ->
                Pair(mode, config)
            }.collectLatest { (mode, config) ->
                applyAppearanceConfigToOverlays(mode, config)
            }
        }
    }

    private fun applyComponentStyleToView(view: View, style: ComponentStyle) {
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            
            if (style.gradientEnabled) {
                val startColor = android.graphics.Color.parseColor(style.gradientStartColor)
                val endColor = android.graphics.Color.parseColor(style.gradientEndColor)
                colors = intArrayOf(startColor, endColor)
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
            } else {
                val baseColor = android.graphics.Color.parseColor(style.bgColor)
                val alpha = (style.opacity * 255).toInt().coerceIn(0, 255)
                val colorWithAlpha = (alpha shl 24) or (baseColor and 0x00FFFFFF)
                setColor(colorWithAlpha)
            }
            
            val radiusPx = (style.cornerRadius * view.context.resources.displayMetrics.density)
            cornerRadius = radiusPx
            
            if (style.borderThickness > 0) {
                val strokeWidthPx = (style.borderThickness * view.context.resources.displayMetrics.density).toInt()
                val strokeColor = android.graphics.Color.parseColor(style.borderColor)
                setStroke(strokeWidthPx, strokeColor)
            }
        }
        
        view.background = drawable
        view.elevation = style.shadowIntensity * view.context.resources.displayMetrics.density
    }

    private fun applyAppearanceConfigToOverlays(mode: Int, config: AppAppearanceConfig) {
        val root = overlayView ?: return
        val collapsedContainer = root.findViewById<View>(R.id.collapsed_container) ?: return
        val expandedContainer = root.findViewById<View>(R.id.expanded_container) ?: return

        val isIsland = mode == 0
        val collapsedStyle = if (isIsland) config.dockableIsland else config.floatingBubble
        val expandedStyle = if (isIsland) config.dockableIsland else config.expandedBubblePanel

        // Format and size for notch overlay vs bubble positioning
        if (isIsland) {
            collapsedContainer.setPadding(35, 12, 35, 12)
        } else {
            collapsedContainer.setPadding(24, 15, 24, 15)
        }

        // Apply backdrops
        applyComponentStyleToView(collapsedContainer, collapsedStyle)
        applyComponentStyleToView(expandedContainer, expandedStyle)

        // Text Colors
        val collapsedTimeText = root.findViewById<TextView>(R.id.collapsed_time_text)
        if (collapsedTimeText != null) {
            collapsedTimeText.setTextColor(android.graphics.Color.parseColor(collapsedStyle.accentColor))
        }

        val expandedTimeText = root.findViewById<TextView>(R.id.expanded_time_text)
        if (expandedTimeText != null) {
            expandedTimeText.setTextColor(android.graphics.Color.parseColor(expandedStyle.textColor))
        }

        val expandedTitle = root.findViewById<TextView>(R.id.expanded_title)
        if (expandedTitle != null) {
            expandedTitle.setTextColor(android.graphics.Color.parseColor(expandedStyle.accentColor))
        }

        // Apply styling to individual buttons inside expanded overlay
        val btnPausePlay = root.findViewById<TextView>(R.id.btn_overlay_pause_play)
        val btnAddTime = root.findViewById<TextView>(R.id.btn_overlay_add_time)
        val btnReset = root.findViewById<TextView>(R.id.btn_overlay_reset)

        listOf(btnPausePlay, btnAddTime, btnReset).forEach { btn ->
            if (btn != null) {
                btn.setTextColor(android.graphics.Color.parseColor(expandedStyle.textColor))
                val btnStyle = ComponentStyle(
                    bgColor = expandedStyle.bgColor,
                    opacity = 0.15f,
                    borderColor = expandedStyle.accentColor,
                    borderThickness = 1.0f,
                    cornerRadius = 10
                )
                applyComponentStyleToView(btn, btnStyle)
            }
        }
    }

    private fun reconfigureLayoutParams(mode: Int) {
        val currentParams = params ?: return
        val currentView = overlayView ?: return

        if (mode == 0) {
            // Dynamic Island Mode: Horizontal center-aligned narrow notch bar
            currentParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            currentParams.x = 0
            currentParams.y = 20
        } else {
            // Free Bubble Mode: Top-Left floating draggable anchor
            currentParams.gravity = Gravity.TOP or Gravity.START
            currentParams.x = 100
            currentParams.y = 200
        }

        try {
            windowManager.updateViewLayout(currentView, currentParams)
        } catch (e: Exception) {}
    }

    private fun setupOverlayBubble() {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = layoutInflater.inflate(R.layout.overlay_bubble, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        this.params = layoutParams
        windowManager.addView(overlayView, layoutParams)

        val collapsedContainer = overlayView!!.findViewById<View>(R.id.collapsed_container)
        val expandedContainer = overlayView!!.findViewById<View>(R.id.expanded_container)

        // Dragging & Click Helper Logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var clickThreshold = 10 // Max pixel moves to still be labeled a click

        overlayView!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = params ?: return false
                val activeMode = TimerStopwatchStateManager.overlayMode.value

                if (activeMode == 0) {
                    // Dynamic Island Mode: static notch at top screen center, touches toggle expansion directly!
                    if (event.action == MotionEvent.ACTION_UP) {
                        if (collapsedContainer.visibility == View.VISIBLE) {
                            collapsedContainer.visibility = View.GONE
                            expandedContainer.visibility = View.VISIBLE
                        } else {
                            collapsedContainer.visibility = View.VISIBLE
                            expandedContainer.visibility = View.GONE
                        }
                    }
                    return true
                }

                // Dragging mechanics for Free Bubble Mode
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        currentParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, currentParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)
                        if (deltaX < clickThreshold && deltaY < clickThreshold) {
                            // Registered click - Expand or collapse bubble
                            if (collapsedContainer.visibility == View.VISIBLE) {
                                collapsedContainer.visibility = View.GONE
                                expandedContainer.visibility = View.VISIBLE
                            } else {
                                collapsedContainer.visibility = View.VISIBLE
                                expandedContainer.visibility = View.GONE
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Config buttons inside Expanded card
        val btnClose = overlayView!!.findViewById<ImageView>(R.id.btn_close_overlay)
        btnClose.setOnClickListener {
            stopSelf()
        }

        val btnPausePlay = overlayView!!.findViewById<TextView>(R.id.btn_overlay_pause_play)
        btnPausePlay.setOnClickListener {
            val isTimerActive = TimerStopwatchStateManager.timerStatus.value != TimerStatus.IDLE
            if (isTimerActive) {
                if (TimerStopwatchStateManager.timerStatus.value == TimerStatus.RUNNING) {
                    TimerStopwatchStateManager.pauseTimer()
                } else {
                    TimerStopwatchStateManager.resumeTimer()
                }
            } else {
                if (TimerStopwatchStateManager.stopwatchStatus.value == StopwatchStatus.RUNNING) {
                    TimerStopwatchStateManager.pauseStopwatch()
                } else {
                    TimerStopwatchStateManager.startStopwatch()
                }
            }
        }

        val btnAddTime = overlayView!!.findViewById<TextView>(R.id.btn_overlay_add_time)
        btnAddTime.setOnClickListener {
            val isTimerActive = TimerStopwatchStateManager.timerStatus.value != TimerStatus.IDLE
            if (isTimerActive) {
                TimerStopwatchStateManager.addOneMinute()
            }
        }

        val btnReset = overlayView!!.findViewById<TextView>(R.id.btn_overlay_reset)
        btnReset.setOnClickListener {
            val isTimerActive = TimerStopwatchStateManager.timerStatus.value != TimerStatus.IDLE
            if (isTimerActive) {
                TimerStopwatchStateManager.resetTimer()
            } else {
                TimerStopwatchStateManager.resetStopwatch()
            }
        }
    }

    private fun observeTimerStopwatch() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            combine(
                TimerStopwatchStateManager.timerRemainingMs,
                TimerStopwatchStateManager.timerStatus,
                TimerStopwatchStateManager.stopwatchElapsedMs,
                TimerStopwatchStateManager.stopwatchStatus
            ) { timerRemaining, timerStatus, swElapsed, swStatus ->
                OverlayState(timerRemaining, timerStatus, swElapsed, swStatus)
            }.collectLatest { state ->
                updateOverlayContent(state)
            }
        }
    }

    private fun updateOverlayContent(state: OverlayState) {
        val view = overlayView ?: return

        val collapsedTimeText = view.findViewById<TextView>(R.id.collapsed_time_text)
        val expandedTimeText = view.findViewById<TextView>(R.id.expanded_time_text)
        val expandedTitle = view.findViewById<TextView>(R.id.expanded_title)
        val btnPausePlay = view.findViewById<TextView>(R.id.btn_overlay_pause_play)
        val btnAddTime = view.findViewById<TextView>(R.id.btn_overlay_add_time)
        val glowingDot = view.findViewById<View>(R.id.collapsed_glowing_dot)

        val isTimerActive = state.timerStatus != TimerStatus.IDLE
        val isSwActive = state.swStatus != StopwatchStatus.IDLE

        if (isTimerActive) {
            expandedTitle.text = "ACTIVE TIMER"
            val totalSecs = state.timerRemainingMs / 1000
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            val readableText = if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }

            collapsedTimeText.text = readableText
            expandedTimeText.text = readableText
            collapsedTimeText.setTextColor(resources.getColor(R.color.purple_glow, null))

            btnAddTime.visibility = View.VISIBLE

            if (state.timerStatus == TimerStatus.RUNNING) {
                btnPausePlay.text = "Pause"
                glowingDot.visibility = View.VISIBLE
            } else {
                btnPausePlay.text = "Resume"
                glowingDot.visibility = View.GONE
            }
        } else if (isSwActive) {
            expandedTitle.text = "STOPWATCH"
            val totalSecs = state.swElapsed / 1000
            val m = (totalSecs / 60) % 60
            val s = totalSecs % 60
            val cc = (state.swElapsed / 10) % 100
            val readableText = String.format("%02d:%02d.%02d", m, s, cc)

            collapsedTimeText.text = readableText
            expandedTimeText.text = readableText
            collapsedTimeText.setTextColor(resources.getColor(R.color.neon_pink, null))

            btnAddTime.visibility = View.GONE

            if (state.swStatus == StopwatchStatus.RUNNING) {
                btnPausePlay.text = "Pause"
                glowingDot.visibility = View.VISIBLE
            } else {
                btnPausePlay.text = "Resume"
                glowingDot.visibility = View.GONE
            }
        } else {
            // Both are idle
            expandedTitle.text = "SYSTEM IDLE"
            collapsedTimeText.text = "00:00"
            expandedTimeText.text = "00:00"
            collapsedTimeText.setTextColor(resources.getColor(R.color.off_white, null))
            glowingDot.visibility = View.GONE
            btnPausePlay.text = "Start"
            btnAddTime.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TimerStopwatchStateManager.setOverlayActive(false)
        updateJob?.cancel()
        job.cancel()
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore removal errors on process death
            }
        }
    }

    private data class OverlayState(
        val timerRemainingMs: Long,
        val timerStatus: TimerStatus,
        val swElapsed: Long,
        val swStatus: StopwatchStatus
    )
}
