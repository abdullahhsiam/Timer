package com.example

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import android.view.animation.DecelerateInterpolator
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

    private var isIslandExpanded = false
    private var islandWidthAnimator: ValueAnimator? = null

    // Fine-grained state cache for performance optimization (eliminates redundant UI redraw and findViewById lookups)
    private var lastFormattedTime = ""
    private var lastTimerStatus: TimerStatus? = null
    private var lastSwStatus: StopwatchStatus? = null
    private var lastIsTimerActive = false
    private var lastIsSwActive = false
    private var lastIsPaused = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        TimerStopwatchStateManager.setOverlayActive(true)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
        
        if (mode == 0) {
            // Apply AMOLED Black aesthetic to Dockable Island capsule (with extremely thin elegant glowing border)
            val islandBgDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(android.graphics.Color.parseColor("#050510")) // Premium black matte acrylic
                val density = root.context.resources.displayMetrics.density
                cornerRadius = 23f * density // pill
                setStroke((1f * density).toInt(), android.graphics.Color.parseColor("#1CFFFFFF")) // Subtle glowing bezel
            }
            root.findViewById<View>(R.id.dockable_island_container)?.background = islandBgDrawable
            return
        }

        val collapsedContainer = root.findViewById<View>(R.id.collapsed_container) ?: return
        val expandedContainer = root.findViewById<View>(R.id.expanded_container) ?: return

        val collapsedStyle = config.floatingBubble
        val expandedStyle = config.expandedBubblePanel

        collapsedContainer.setPadding(24, 15, 24, 15)

        // Apply backdrops to Free Floating Bubble surfaces
        applyComponentStyleToView(collapsedContainer, collapsedStyle)
        applyComponentStyleToView(expandedContainer, expandedStyle)

        // Text Colors
        val collapsedTimeText = root.findViewById<TextView>(R.id.collapsed_time_text)
        val expandedTimeText = root.findViewById<TextView>(R.id.expanded_time_text)
        val expandedTitle = root.findViewById<TextView>(R.id.expanded_title)

        val isTimerActive = TimerStopwatchStateManager.timerStatus.value != TimerStatus.IDLE
        val isSwActive = TimerStopwatchStateManager.stopwatchStatus.value != StopwatchStatus.IDLE
        val isPaused = (isTimerActive && TimerStopwatchStateManager.timerStatus.value == TimerStatus.PAUSED) || (isSwActive && TimerStopwatchStateManager.stopwatchStatus.value == StopwatchStatus.PAUSED)
        val timeColor = if (isPaused) android.graphics.Color.RED else android.graphics.Color.WHITE

        collapsedTimeText?.setTextColor(timeColor)
        expandedTimeText?.setTextColor(timeColor)
        expandedTitle?.setTextColor(android.graphics.Color.WHITE)

        // Apply styling to individual buttons inside expanded overlay card
        val btnPausePlay = root.findViewById<TextView>(R.id.btn_overlay_pause_play)
        val btnAddTime = root.findViewById<TextView>(R.id.btn_overlay_add_time)
        val btnReset = root.findViewById<TextView>(R.id.btn_overlay_reset)

        listOf(btnPausePlay, btnAddTime, btnReset).forEach { btn ->
            if (btn != null) {
                btn.setTextColor(android.graphics.Color.WHITE)
                val btnStyle = ComponentStyle(
                    bgColor = expandedStyle.bgColor,
                    opacity = 0.15f,
                    borderColor = "#33FFFFFF",
                    borderThickness = 1.0f,
                    cornerRadius = 10
                )
                applyComponentStyleToView(btn, btnStyle)
            }
        }
    }

    private fun reconfigureLayoutParams(mode: Int) {
        isIslandExpanded = false
        islandWidthAnimator?.cancel()

        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            overlayView = null
        }

        setupOverlayBubble(mode)
    }

    private fun toggleIslandExpansion() {
        val root = overlayView ?: return
        val islandContainer = root.findViewById<View>(R.id.dockable_island_container) ?: return
        val btnPlayPause = root.findViewById<View>(R.id.island_btn_play_pause) ?: return
        val btnReset = root.findViewById<View>(R.id.island_btn_reset) ?: return
        val btnClose = root.findViewById<View>(R.id.island_btn_close) ?: return
        val density = resources.displayMetrics.density

        isIslandExpanded = !isIslandExpanded
        islandWidthAnimator?.cancel()

        // Deterministic morphing widths
        val startWidth = if (isIslandExpanded) (130 * density).toInt() else (240 * density).toInt()
        val endWidth = if (isIslandExpanded) (240 * density).toInt() else (130 * density).toInt()
        val fixedCapsuleHeight = (46 * density).toInt()

        if (isIslandExpanded) {
            btnPlayPause.visibility = View.VISIBLE
            btnReset.visibility = View.VISIBLE
            btnClose.visibility = View.VISIBLE
            btnPlayPause.alpha = 0f
            btnReset.alpha = 0f
            btnClose.alpha = 0f
        }

        // Custom premium path interpolator with spring overshoot (BackEaseOut feel)
        val customInterpolator = if (isIslandExpanded) {
            android.view.animation.PathInterpolator(0.15f, 0.9f, 0.2f, 1.08f)
        } else {
            android.view.animation.PathInterpolator(0.25f, 1f, 0.2f, 1f)
        }

        islandWidthAnimator = ValueAnimator.ofInt(startWidth, endWidth).apply {
            duration = 350
            interpolator = customInterpolator
            addUpdateListener { animator ->
                val currentWidth = animator.animatedValue as Int
                val currentParams = params ?: return@addUpdateListener
                currentParams.width = currentWidth
                currentParams.height = fixedCapsuleHeight
                try {
                    windowManager.updateViewLayout(root, currentParams)
                } catch (e: Exception) {}

                val progress = animator.animatedFraction
                
                // Opacity Transitions & staggered fade for buttons to feel like a single cohesive surface transforming
                if (isIslandExpanded) {
                    val fadeProgress = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                    btnPlayPause.alpha = fadeProgress
                    btnReset.alpha = fadeProgress
                    btnClose.alpha = fadeProgress
                    
                    // Subtle dynamic physical scale compression for the active center time text
                    root.findViewById<View>(R.id.island_time_text)?.let {
                        val textScale = 1.0f - (0.08f * kotlin.math.sin(progress * Math.PI).toFloat())
                        it.scaleX = textScale
                        it.scaleY = textScale
                    }
                } else {
                    val fadeProgress = (1f - progress * 1.5f).coerceIn(0f, 1f)
                    btnPlayPause.alpha = fadeProgress
                    btnReset.alpha = fadeProgress
                    btnClose.alpha = fadeProgress
                }

                // Blur-based morphing effect (Android 12+) using a bell curve for organic liquid tension feel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val bellCurve = kotlin.math.sin(progress * Math.PI).toFloat()
                    val blurRadius = bellCurve * 8f * density
                    if (blurRadius > 1.2f) {
                        try {
                            islandContainer.setRenderEffect(
                                android.graphics.RenderEffect.createBlurEffect(
                                    blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP
                                )
                            )
                        } catch (e: Exception) {}
                    } else {
                        try {
                            islandContainer.setRenderEffect(null)
                        } catch (e: Exception) {}
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val currentParams = params
                    if (currentParams != null) {
                        currentParams.width = endWidth
                        currentParams.height = fixedCapsuleHeight
                        try {
                            windowManager.updateViewLayout(root, currentParams)
                        } catch (e: Exception) {}
                    }
                    if (!isIslandExpanded) {
                        btnPlayPause.visibility = View.GONE
                        btnReset.visibility = View.GONE
                        btnClose.visibility = View.GONE
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            islandContainer.setRenderEffect(null)
                        } catch (e: Exception) {}
                    }
                    root.findViewById<View>(R.id.island_time_text)?.let {
                        it.scaleX = 1f
                        it.scaleY = 1f
                    }
                }
            })
            start()
        }
    }

    private fun setupOverlayBubble(mode: Int) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = layoutInflater.inflate(R.layout.overlay_bubble, null)

        val density = resources.displayMetrics.density
        val layoutParams = WindowManager.LayoutParams(
            if (mode == 0) (130 * density).toInt() else WindowManager.LayoutParams.WRAP_CONTENT,
            if (mode == 0) (46 * density).toInt() else WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (mode == 0) {
                // Dynamic Island Mode: Fixed center-aligned notch bar, sitting directly below status bar
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = (10 * density).toInt()
            } else {
                // Free Bubble Mode: Top-Left floating anchor, user-draggable
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 200
            }
        }

        this.params = layoutParams
        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return
        }

        val dockableContainer = overlayView!!.findViewById<View>(R.id.dockable_island_container)
        val collapsedContainer = overlayView!!.findViewById<View>(R.id.collapsed_container)
        val expandedContainer = overlayView!!.findViewById<View>(R.id.expanded_container)

        if (mode == 0) {
            dockableContainer?.visibility = View.VISIBLE
            collapsedContainer?.visibility = View.GONE
            expandedContainer?.visibility = View.GONE
            
            // Start narrow/collapsed with hidden icons and reset alpha
            overlayView!!.findViewById<View>(R.id.island_btn_play_pause)?.let {
                it.visibility = View.GONE
                it.alpha = 0f
            }
            overlayView!!.findViewById<View>(R.id.island_btn_reset)?.let {
                it.visibility = View.GONE
                it.alpha = 0f
            }
            overlayView!!.findViewById<View>(R.id.island_btn_close)?.let {
                it.visibility = View.GONE
                it.alpha = 0f
            }
        } else {
            dockableContainer?.visibility = View.GONE
            collapsedContainer?.visibility = View.VISIBLE
            expandedContainer?.visibility = View.GONE
        }

        // Apply appearance styling (AMOLED black etc.)
        val config = TimerStopwatchStateManager.appearanceConfig.value
        applyAppearanceConfigToOverlays(mode, config)

        // Dragging & Clicking Helpers
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val clickThreshold = 15 // Pixels
        var isMoving = false

        overlayView!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = params ?: return false

                if (mode == 0) {
                    // Fixed center-aligned Dockable Island (Never draggable)
                    if (event.action == MotionEvent.ACTION_UP) {
                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)
                        if (deltaX < clickThreshold && deltaY < clickThreshold) {
                            toggleIslandExpansion()
                        }
                    } else if (event.action == MotionEvent.ACTION_DOWN) {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    return true
                }

                // Normal free dragging mechanics for Floating Bubble
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)
                        if (deltaX > clickThreshold || deltaY > clickThreshold) {
                            isMoving = true
                        }
                        currentParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        currentParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, currentParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
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

        // 1. DOCKABLE ISLAND EVENT CLICK LISTENERS
        overlayView!!.findViewById<View>(R.id.island_btn_play_pause)?.setOnClickListener {
            toggleActiveTimerState()
        }
        overlayView!!.findViewById<View>(R.id.island_btn_reset)?.setOnClickListener {
            resetActiveTimerState()
        }
        overlayView!!.findViewById<View>(R.id.island_btn_close)?.setOnClickListener {
            stopSelf()
        }

        // 2. FLOATING BUBBLE CARD EVENT LISTENERS
        overlayView!!.findViewById<View>(R.id.btn_close_overlay)?.setOnClickListener {
            stopSelf()
        }
        overlayView!!.findViewById<View>(R.id.btn_overlay_pause_play)?.setOnClickListener {
            toggleActiveTimerState()
        }
        overlayView!!.findViewById<View>(R.id.btn_overlay_add_time)?.setOnClickListener {
            addOneMinuteToActiveTimer()
        }
        overlayView!!.findViewById<View>(R.id.btn_overlay_reset)?.setOnClickListener {
            resetActiveTimerState()
        }
    }

    private fun toggleActiveTimerState() {
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

    private fun resetActiveTimerState() {
        val isTimerActive = TimerStopwatchStateManager.timerStatus.value != TimerStatus.IDLE
        if (isTimerActive) {
            TimerStopwatchStateManager.resetTimer()
        } else {
            TimerStopwatchStateManager.resetStopwatch()
        }
    }

    private fun addOneMinuteToActiveTimer() {
        val isTimerActive = TimerStopwatchStateManager.timerStatus.value != TimerStatus.IDLE
        if (isTimerActive) {
            TimerStopwatchStateManager.addOneMinute()
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

        val isTimerActive = state.timerStatus != TimerStatus.IDLE
        val isSwActive = state.swStatus != StopwatchStatus.IDLE
        val isPaused = (isTimerActive && state.timerStatus == TimerStatus.PAUSED) || (isSwActive && state.swStatus == StopwatchStatus.PAUSED)
        val timeColor = if (isPaused) android.graphics.Color.RED else android.graphics.Color.WHITE

        var formattedTime = "00:00"
        if (isTimerActive) {
            val totalSecs = state.timerRemainingMs / 1000
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            formattedTime = if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        } else if (isSwActive) {
            val totalSecs = state.swElapsed / 1000
            val m = (totalSecs / 60) % 60
            val s = totalSecs % 60
            val cc = (state.swElapsed / 10) % 100
            formattedTime = String.format("%02d:%02d.%02d", m, s, cc)
        }

        // Cache hit check: skip layout traversal if visual representation hasn't changed
        if (formattedTime == lastFormattedTime &&
            state.timerStatus == lastTimerStatus &&
            state.swStatus == lastSwStatus &&
            isTimerActive == lastIsTimerActive &&
            isSwActive == lastIsSwActive &&
            isPaused == lastIsPaused
        ) {
            return
        }

        // Update cached values
        lastFormattedTime = formattedTime
        lastTimerStatus = state.timerStatus
        lastSwStatus = state.swStatus
        lastIsTimerActive = isTimerActive
        lastIsSwActive = isSwActive
        lastIsPaused = isPaused

        // 1. UPDATE DOCKABLE ISLAND UI
        val islandTimeText = view.findViewById<TextView>(R.id.island_time_text)
        if (islandTimeText != null) {
            islandTimeText.text = formattedTime
            islandTimeText.setTextColor(timeColor)
        }
        val islandPlayPause = view.findViewById<ImageView>(R.id.island_btn_play_pause)
        if (islandPlayPause != null) {
            val isRunning = (isTimerActive && state.timerStatus == TimerStatus.RUNNING) || (isSwActive && state.swStatus == StopwatchStatus.RUNNING)
            islandPlayPause.setImageResource(
                if (isRunning) R.drawable.ic_pause_symbol else R.drawable.ic_play_symbol
            )
            islandPlayPause.setColorFilter(android.graphics.Color.WHITE)
        }
        val islandReset = view.findViewById<ImageView>(R.id.island_btn_reset)
        islandReset?.setColorFilter(android.graphics.Color.WHITE)
        val islandClose = view.findViewById<ImageView>(R.id.island_btn_close)
        islandClose?.setColorFilter(android.graphics.Color.WHITE)

        // 2. UPDATE FLOATING BUBBLE UI
        val collapsedTimeText = view.findViewById<TextView>(R.id.collapsed_time_text)
        val expandedTimeText = view.findViewById<TextView>(R.id.expanded_time_text)
        val expandedTitle = view.findViewById<TextView>(R.id.expanded_title)
        val btnPausePlay = view.findViewById<TextView>(R.id.btn_overlay_pause_play)
        val btnAddTime = view.findViewById<TextView>(R.id.btn_overlay_add_time)
        val glowingDot = view.findViewById<View>(R.id.collapsed_glowing_dot)

        collapsedTimeText?.text = formattedTime
        collapsedTimeText?.setTextColor(timeColor)
        expandedTimeText?.text = formattedTime
        expandedTimeText?.setTextColor(timeColor)

        expandedTitle?.setTextColor(android.graphics.Color.WHITE)
        btnPausePlay?.setTextColor(android.graphics.Color.WHITE)
        btnAddTime?.setTextColor(android.graphics.Color.WHITE)

        if (isTimerActive) {
            expandedTitle?.text = "ACTIVE TIMER"
            btnAddTime?.visibility = View.VISIBLE
            if (btnPausePlay != null) {
                btnPausePlay.text = if (state.timerStatus == TimerStatus.RUNNING) "Pause" else "Resume"
            }
            glowingDot?.visibility = if (state.timerStatus == TimerStatus.RUNNING) View.VISIBLE else View.GONE
        } else if (isSwActive) {
            expandedTitle?.text = "STOPWATCH"
            btnAddTime?.visibility = View.GONE
            if (btnPausePlay != null) {
                btnPausePlay.text = if (state.swStatus == StopwatchStatus.RUNNING) "Pause" else "Resume"
            }
            glowingDot?.visibility = if (state.swStatus == StopwatchStatus.RUNNING) View.VISIBLE else View.GONE
        } else {
            expandedTitle?.text = "SYSTEM IDLE"
            glowingDot?.visibility = View.GONE
            btnPausePlay?.text = "Start"
            btnAddTime?.visibility = View.GONE
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
            } catch (e: Exception) {}
        }
    }

    private data class OverlayState(
        val timerRemainingMs: Long,
        val timerStatus: TimerStatus,
        val swElapsed: Long,
        val swStatus: StopwatchStatus
    )
}
