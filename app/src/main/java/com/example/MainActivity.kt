package com.example

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.os.Bundle
import android.view.WindowManager
import android.provider.Settings
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DoNotDisturbAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.OffWhite
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TimerStopwatchViewModel by viewModels()
    private lateinit var alarmController: AlarmController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TimerStopwatchStateManager.initialize(applicationContext)
        enableEdgeToEdge()
        alarmController = AlarmController(applicationContext)
        viewModel.selectSound(alarmController.getSelectedSound())
        
        // Load custom background preferences
        val prefs = getSharedPreferences("focus_sound_prefs", android.content.Context.MODE_PRIVATE)
        viewModel.setBackgroundAnimated(prefs.getBoolean("is_background_animated", true))

        setContent {
            MyApplicationTheme {
                val alarmTriggered by viewModel.alarmTriggered.collectAsState()
                val selectedSound by viewModel.selectedSound.collectAsState()
                val isBackgroundAnimated by viewModel.isBackgroundAnimated.collectAsState()

                // Trigger or close alarm audio/vibe relative to ViewModel reactive states
                LaunchedEffect(alarmTriggered) {
                    if (alarmTriggered) {
                        alarmController.startAlarm()
                    } else {
                        alarmController.stopAlarm()
                    }
                }

                // Keep sound controller synchronized with state flow changes
                LaunchedEffect(selectedSound) {
                    alarmController.saveSelectedSound(selectedSound)
                }

                // Persist background styling preferences
                LaunchedEffect(isBackgroundAnimated) {
                    val editPrefs = getSharedPreferences("focus_sound_prefs", android.content.Context.MODE_PRIVATE)
                    editPrefs.edit().putBoolean("is_background_animated", isBackgroundAnimated).apply()
                }

                var showSplash by remember { mutableStateOf(true) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            viewModel = viewModel,
                            alarmTriggered = alarmTriggered,
                            onPlayPreview = { preset -> alarmController.playPreview(preset) },
                            onStopPreview = { alarmController.stopAlarm() }
                        )

                        if (showSplash) {
                            IntroSplashAnimation(
                                onFinished = { showSplash = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val isTimerRunning = TimerStopwatchStateManager.timerStatus.value == TimerStatus.RUNNING
        val isStopwatchRunning = TimerStopwatchStateManager.stopwatchStatus.value == StopwatchStatus.RUNNING
        if (isTimerRunning || isStopwatchRunning) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pipParams = android.app.PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(pipParams)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        TimerStopwatchStateManager.setInPip(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure no stray alarm/vibration resource leakage on teardown
        if (::alarmController.isInitialized) {
            alarmController.stopAlarm()
        }
    }
}

@Composable
fun MainScreen(
    viewModel: TimerStopwatchViewModel,
    alarmTriggered: Boolean,
    onPlayPreview: (AlarmSoundPreset) -> Unit,
    onStopPreview: () -> Unit
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isAlwaysOn by viewModel.isAlwaysOn.collectAsState()
    val timerStatus by viewModel.timerStatus.collectAsState()
    val stopwatchStatus by viewModel.stopwatchStatus.collectAsState()
    val isBackgroundAnimated by viewModel.isBackgroundAnimated.collectAsState()
    val overlayMode by viewModel.overlayMode.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    var popupVisibleState by remember { mutableStateOf(false) }

    LaunchedEffect(menuExpanded) {
        if (menuExpanded) {
            popupVisibleState = true
        } else {
            delay(180)
            popupVisibleState = false
        }
    }

    var showSoundDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Active Always-on-Display Wake Lock toggle rule
    // Keeps screen awake during running/active modes or if AOD is overall enforced
    val screenShouldBeOn = isAlwaysOn && (
            timerStatus == TimerStatus.RUNNING || 
            timerStatus == TimerStatus.PAUSED || 
            timerStatus == TimerStatus.FINISHED ||
            stopwatchStatus == StopwatchStatus.RUNNING || 
            stopwatchStatus == StopwatchStatus.PAUSED || 
            alarmTriggered
    )

    val view = LocalView.current
    DisposableEffect(screenShouldBeOn) {
        val window = (context as? Activity)?.window
        if (screenShouldBeOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val activeVisualMode by viewModel.activeVisualMode.collectAsState()

    // Wrap entire layout in standard animated background
    AnimatedGradientBackground(
        isPulsingAlarm = alarmTriggered,
        isRunningActive = timerStatus == TimerStatus.RUNNING || stopwatchStatus == StopwatchStatus.RUNNING,
        isAnimated = isBackgroundAnimated,
        visualMode = activeVisualMode
    ) {
        val isInPip by TimerStopwatchStateManager.isInPip.collectAsState()

        if (isInPip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F12)),
                contentAlignment = Alignment.Center
            ) {
                val isSwActive = stopwatchStatus != StopwatchStatus.IDLE
                if (isSwActive) {
                    val elapsedMs by viewModel.stopwatchElapsedMs.collectAsState()
                    val totalSeconds = elapsedMs / 1000
                    val m = (totalSeconds / 60) % 60
                    val s = totalSeconds % 60
                    val cc = (elapsedMs / 10) % 100
                    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
                    val readableTime = "${pad(m)}:${pad(s)}.${pad(cc)}"

                    CircleProgressTimer(
                        remainingMs = if (stopwatchStatus == StopwatchStatus.RUNNING) 1L else 0L,
                        totalMs = 1L,
                        displayString = readableTime,
                        statusText = "SW",
                        onProgressColor = NeonPink,
                        glowEnabled = stopwatchStatus == StopwatchStatus.RUNNING,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    val timerRemainingMs by viewModel.timerRemainingMs.collectAsState()
                    val timerMaxMs by viewModel.timerMaxMs.collectAsState()
                    val totalSecs = timerRemainingMs / 1000
                    val h = totalSecs / 3600
                    val m = (totalSecs % 3600) / 60
                    val s = totalSecs % 60
                    
                    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
                    val displayString = if (h > 0) "${pad(h)}:${pad(m)}:${pad(s)}" else "${pad(m)}:${pad(s)}"

                    CircleProgressTimer(
                        remainingMs = timerRemainingMs,
                        totalMs = timerMaxMs,
                        displayString = displayString,
                        statusText = "TIMER",
                        onProgressColor = PurpleGlow,
                        glowEnabled = timerStatus == TimerStatus.RUNNING,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        } else if (alarmTriggered) {
            // Full screen active Alert dialog overlay
            ActiveAlarmOverlay(viewModel = viewModel)
        } else {
            // Main Standard Navigation and Layout Structure with stable coordinates
            val isFullScreenDisplay = (timerStatus == TimerStatus.RUNNING && activeTab == 0) || 
                                      (stopwatchStatus == StopwatchStatus.RUNNING && activeTab == 1)

            val fullScreenTransitionProgress by animateFloatAsState(
                targetValue = if (isFullScreenDisplay) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "full_screen_transition"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                var swipeOffset by remember { mutableFloatStateOf(0f) }
                // 1. CENTER CONTAINER (DIALER & TIMERS): Perfect geometric anchoring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeTab) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipeOffset = 0f },
                                onDragEnd = {
                                    if (swipeOffset > 100f && activeTab == 1) {
                                        viewModel.selectTab(0)
                                    } else if (swipeOffset < -100f && activeTab == 0) {
                                        viewModel.selectTab(1)
                                    }
                                    swipeOffset = 0f
                                },
                                onDragCancel = { swipeOffset = 0f }
                            ) { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                                swipeOffset += dragAmount
                            }
                        }
                        .graphicsLayer {
                            // Symmetrical dynamic scale kept at 1.0f to prevent controls from scaling out of screen bounds
                            val scale = 1.0f
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            val duration = 550
                            (fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.94f, animationSpec = tween(duration, easing = FastOutSlowInEasing))) togetherWith
                                    (fadeOut(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = tween(duration, easing = FastOutSlowInEasing))) using SizeTransform(clip = false)
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "tab_switch_transition"
                    ) { targetTab ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (targetTab == 0) {
                                TimerTabContent(viewModel = viewModel)
                            } else {
                                StopwatchTabContent(viewModel = viewModel)
                            }
                        }
                    }
                }

                // 2. TOP CONTAINER (OPTIONS HEADER & SLIDING PILL TAB SELECTOR)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = 1f - fullScreenTransitionProgress
                            translationY = -35.dp.toPx() * fullScreenTransitionProgress
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Focus Mode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp,
                                color = Color.White.copy(alpha = 0.60f)
                            )
                            // Active status pulse green dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GlowGreen)
                            )
                        }
                        
                        // Options Trigger & Dropdown Menu
                        Box {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { menuExpanded = true }
                            )
                            if (popupVisibleState) {
                                Popup(
                                    alignment = Alignment.TopEnd,
                                    offset = IntOffset(0, 30),
                                    onDismissRequest = { menuExpanded = false },
                                    properties = PopupProperties(focusable = true)
                                ) {
                                    val selectedSound by viewModel.selectedSound.collectAsState()
                                    val visibleState = remember { androidx.compose.animation.core.MutableTransitionState(false) }
                                    LaunchedEffect(menuExpanded) {
                                        visibleState.targetState = menuExpanded
                                    }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visibleState = visibleState,
                                        enter = fadeIn(tween(180)) + scaleIn(spring(0.70f, Spring.StiffnessMediumLow), initialScale = 0.8f, transformOrigin = TransformOrigin(1f, 0f)),
                                        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f, transformOrigin = TransformOrigin(1f, 0f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(280.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF141419))
                                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Column {
                                            Text(
                                                text = "CONTROL CENTRE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                                color = Color(0xFFAAAAAA),
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )

                                            SettingsMenuItem(
                                                icon = Icons.Default.MusicNote,
                                                title = "Alarm sound",
                                                subtitle = selectedSound.name,
                                                onClick = {
                                                    menuExpanded = false
                                                    showSoundDialog = true
                                                }
                                            )

                                            SettingsMenuItem(
                                                icon = if (isBackgroundAnimated) Icons.Default.Layers else Icons.Default.LayersClear,
                                                title = "Fluid background",
                                                subtitle = if (isBackgroundAnimated) "Animated colors enabled" else "Static blur only",
                                                onClick = { },
                                                trailing = {
                                                    Switch(
                                                        checked = isBackgroundAnimated,
                                                        onCheckedChange = { viewModel.setBackgroundAnimated(it) },
                                                        modifier = Modifier.scale(0.85f),
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = PurpleGlow,
                                                            checkedTrackColor = PurpleGlow.copy(alpha = 0.4f)
                                                        )
                                                    )
                                                }
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Color(0x1AFFFFFF))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "OVERLAY MODE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                                color = Color(0xFFAAAAAA),
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )

                                            SettingsRadioItem(
                                                icon = Icons.Default.DesktopWindows,
                                                title = "Dockable Island",
                                                isSelected = overlayMode == 0,
                                                onClick = { 
                                                    viewModel.setOverlayMode(0) 
                                                    menuExpanded = false
                                                }
                                            )

                                            SettingsRadioItem(
                                                icon = Icons.Default.ChatBubbleOutline,
                                                title = "Floating Bubble",
                                                isSelected = overlayMode == 1,
                                                onClick = { 
                                                    viewModel.setOverlayMode(1) 
                                                    menuExpanded = false
                                                }
                                            )

                                            SettingsRadioItem(
                                                icon = Icons.Default.DoNotDisturbAlt,
                                                title = "Disable Overlays",
                                                isSelected = overlayMode == 2,
                                                onClick = { 
                                                    viewModel.setOverlayMode(2) 
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                        }
                                    }
                                }
                            }
                        }

                        if (showSoundDialog) {
                            val selectedSound by viewModel.selectedSound.collectAsState()
                            SoundSelectionDialog(
                                currentSelection = selectedSound,
                                onSelect = { preset -> viewModel.selectSound(preset) },
                                onDismiss = { showSoundDialog = false },
                                onPlayPreview = onPlayPreview,
                                onStopPreview = onStopPreview
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sliding Premium Glassmorphic Tab Switchers
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SlidingTabSwitcher(
                            activeTab = activeTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                        val activeVisualMode by viewModel.activeVisualMode.collectAsState()
                        VisualModeSwitcher(
                            activeMode = activeVisualMode,
                            onModeSelected = { viewModel.selectVisualMode(it) }
                        )
                    }
                }

                // 3. BOTTOM CONTAINER (ALWAYS-ON & BUBBLE CONFIG FOOTER ACTIONS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .graphicsLayer {
                            alpha = 1f - fullScreenTransitionProgress
                            translationY = 35.dp.toPx() * fullScreenTransitionProgress
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val overlayActive by viewModel.overlayActive.collectAsState()
                    val hasOverlayPermission = Settings.canDrawOverlays(context)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x06FFFFFF))
                                .clickable { viewModel.toggleAlwaysOn() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlwaysOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "AOD State",
                                tint = if (isAlwaysOn) PurpleGlow else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAlwaysOn) "Always-On Enforced" else "Screen Sleep Allowed",
                                fontSize = 11.sp,
                                color = if (isAlwaysOn) Color.White else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x06FFFFFF))
                                .clickable {
                                    if (Settings.canDrawOverlays(context)) {
                                        if (overlayActive) {
                                            context.stopService(Intent(context, OverlayBubbleService::class.java))
                                        } else {
                                            val serviceIntent = Intent(context, OverlayBubbleService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(serviceIntent)
                                            } else {
                                                context.startService(serviceIntent)
                                            }
                                        }
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (overlayActive) Icons.Default.Layers else Icons.Default.LayersClear,
                                contentDescription = "Overlay State",
                                tint = if (overlayActive) PurpleGlow else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!hasOverlayPermission) "Grant Overlay Permission" else if (overlayActive) "Floating Bubble Active" else "Enable Floating Bubble",
                                fontSize = 11.sp,
                                color = if (overlayActive) Color.White else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "v1.0.0 Stable",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SlidingTabSwitcher(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth tab selection index progress (0f to 1f)
    val tabProgress by animateFloatAsState(
        targetValue = activeTab.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 300f
        ),
        label = "sliding_tab_progress"
    )

    // Liquid organic stretch: pill temporarily elongates as it slides across the divide
    val stretchWidth = 80.dp + (32.dp * Math.max(0.0, kotlin.math.sin(tabProgress * Math.PI)).toFloat())

    Box(
        modifier = modifier
            .width(160.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp))
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding glassmorphic indicator capsule
        Box(
            modifier = Modifier
                .offset(x = (tabProgress * 74).dp)
                .width(stretchWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .blur(
                    radius = (8.dp * Math.max(0.0, kotlin.math.sin(tabProgress * Math.PI)).toFloat())
                )
                .background(Color.White.copy(alpha = 0.12f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(
                label = "Timer",
                selected = activeTab == 0,
                icon = Icons.Default.Timer,
                onClick = { onTabSelected(0) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("tab_timer")
            )
            TabItem(
                label = "Watch",
                selected = activeTab == 1,
                icon = Icons.Default.Schedule,
                onClick = { onTabSelected(1) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("tab_stopwatch")
            )
        }
    }
}

@Composable
fun VisualModeSwitcher(
    activeMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabProgress by animateFloatAsState(
        targetValue = activeMode.toFloat(),
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "sliding_mode_progress"
    )

    val stretchWidth = 75.dp + (32.dp * Math.max(0.0, kotlin.math.sin(tabProgress * Math.PI)).toFloat())

    Box(
        modifier = modifier
            .width(150.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp))
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = (tabProgress * 69).dp)
                .width(stretchWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .blur(radius = (8.dp * Math.max(0.0, kotlin.math.sin(tabProgress * Math.PI)).toFloat()))
                .background(Color.White.copy(alpha = 0.12f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(
                label = "Circle",
                selected = activeMode == 0,
                icon = androidx.compose.material.icons.Icons.Default.Timer,
                onClick = { onModeSelected(0) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            TabItem(
                label = "Flip",
                selected = activeMode == 1,
                icon = androidx.compose.material.icons.Icons.Default.Layers,
                onClick = { onModeSelected(1) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
fun TabItem(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth selection background transparency transition (reserved for potential press feedback)
    val animAlpha by animateFloatAsState(
        targetValue = if (selected) 0.12f else 0.0f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "tab_item_bg"
    )
    
    // Soft color crossfade for text & icons
    val animTextColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.40f),
        animationSpec = tween(320),
        label = "tab_item_text"
    )

    // Dynamic elastic micro-scaling for the pill element
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 0.98f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "tab_item_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animTextColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = animTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun TimerFlipView(viewModel: TimerStopwatchViewModel, isTablet: Boolean, isLandscape: Boolean) {
    val timerRemainingMs by viewModel.timerRemainingMs.collectAsState()
    val remainingSeconds = (timerRemainingMs + 999) / 1000
    val h = remainingSeconds / 3600
    val m = (remainingSeconds % 3600) / 60
    val s = remainingSeconds % 60
    
    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
    val readableTime = "${pad(h)}:${pad(m)}:${pad(s)}"

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        FlipClockDisplay(
            timeString = readableTime,
            height = if (isTablet) 95.dp else if (isLandscape) 70.dp else 80.dp,
            width = if (isTablet) 65.dp else if (isLandscape) 48.dp else 54.dp,
            textSize = if (isTablet) 68f else if (isLandscape) 50f else 56f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 24.dp))
    }
}

@Composable
fun TimerCircleView(viewModel: TimerStopwatchViewModel, timerStatus: TimerStatus, accentColor: Color) {
    val timerRemainingMs by viewModel.timerRemainingMs.collectAsState()
    val timerMaxMs by viewModel.timerMaxMs.collectAsState()
    val remainingSeconds = (timerRemainingMs + 999) / 1000
    val h = remainingSeconds / 3600
    val m = (remainingSeconds % 3600) / 60
    val s = remainingSeconds % 60
    
    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
    val readableTime = "${pad(h)}:${pad(m)}:${pad(s)}"
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleProgressTimer(
            remainingMs = timerRemainingMs,
            totalMs = timerMaxMs,
            displayString = readableTime,
            statusText = if (timerStatus == TimerStatus.RUNNING) "RUNNING" else "PAUSED",
            onProgressColor = if (timerStatus == TimerStatus.RUNNING) accentColor else Color.White.copy(alpha = 0.15f),
            glowEnabled = timerStatus == TimerStatus.RUNNING
        )
    }
}

@Composable
fun ReactiveTimerFace(viewModel: TimerStopwatchViewModel, isTablet: Boolean, isLandscape: Boolean) {
    val timerStatus by viewModel.timerStatus.collectAsState()
    val activeVisualMode by viewModel.activeVisualMode.collectAsState()
    val accentColor = if (activeVisualMode == 1) Color(0xFF4C8DFF) else PurpleGlow

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LiveDigitalClock()
        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 24.dp))
        
        AnimatedContent(
            targetState = activeVisualMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, easing = LinearOutSlowInEasing))) togetherWith 
                (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 1.05f, animationSpec = tween(150)))
            },
            label = "visual_mode_transition"
        ) { mode ->
            if (mode == 1) {
                TimerFlipView(viewModel, isTablet, isLandscape)
            } else {
                TimerCircleView(viewModel, timerStatus, accentColor)
            }
        }
    }
}

// ==========================================
// TIMER TAB CONTENT VIEWS
// ==========================================
@Composable
fun TimerTabContent(viewModel: TimerStopwatchViewModel) {
    val timerInput by viewModel.timerInput.collectAsState()
    val timerStatus by viewModel.timerStatus.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600 && configuration.screenHeightDp >= 600

    val isTimerActive = timerStatus == TimerStatus.RUNNING || timerStatus == TimerStatus.PAUSED
    val activeVisualMode by viewModel.activeVisualMode.collectAsState()
    val accentColor = if (activeVisualMode == 1) Color(0xFF4C8DFF) else PurpleGlow

    val transition = updateTransition(targetState = isTimerActive, label = "dialer_to_timer")
    val transitionProgress by transition.animateFloat(
        transitionSpec = { tween(600, easing = FastOutSlowInEasing) },
        label = "transition_progress"
    ) { active ->
        if (active) 1f else 0f
    }

    // Layout transform parameters
    val dialerAlpha = 1f - transitionProgress
    val dialerScale = 1f - (0.06f * transitionProgress)

    val timerAlpha = transitionProgress
    val timerScale = 0.94f + (0.06f * transitionProgress)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- DIALER LAYER (IDLE INPUT KEYPAD VIEW) ---
        if (transitionProgress < 0.99f || !isTimerActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = dialerAlpha
                        scaleX = dialerScale
                        scaleY = dialerScale
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val formatted = timerInput.padStart(6, '0')
                                val h = formatted.substring(0, 2)
                                val m = formatted.substring(2, 4)
                                val s = formatted.substring(4, 6)

                                DigitGroup(h, "h", active = timerInput.length >= 5)
                                Spacer(modifier = Modifier.width(6.dp))
                                DigitGroup(m, "m", active = timerInput.length >= 3)
                                Spacer(modifier = Modifier.width(6.dp))
                                DigitGroup(s, "s", active = timerInput.isNotEmpty())
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                            ) {
                                ModernPresetButton(label = "+30s", onClick = { viewModel.startPresetTimer(30L) })
                                ModernPresetButton(label = "1 Min", onClick = { viewModel.startPresetTimer(60L) })
                                ModernPresetButton(label = "5 Min", onClick = { viewModel.startPresetTimer(300L) })
                                ModernPresetButton(label = "10 Min", onClick = { viewModel.startPresetTimer(600L) })
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (timerInput.isNotEmpty()) {
                                            accentColor
                                        } else {
                                            Color.White.copy(alpha = 0.03f)
                                        }
                                    )
                                    .then(
                                        if (timerInput.isEmpty()) {
                                            Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("timer_start_button")
                                    .clickable(enabled = timerInput.isNotEmpty() && transitionProgress < 0.1f) { viewModel.startTimer() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Timer",
                                    tint = if (timerInput.isNotEmpty()) com.example.ui.theme.DarkPurple else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Right column Keypad (more weight, larger buttons)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1.1f)
                        ) {
                            val isCompactLand = configuration.screenHeightDp < 600
                            ModernKeypad(
                                buttonSize = if (isCompactLand) 48.dp else 58.dp,
                                spacing = if (isCompactLand) 4.dp else 6.dp,
                                horizontalSpacing = if (isCompactLand) 8.dp else 14.dp,
                                fontSize = if (isCompactLand) 20.sp else 24.sp,
                                actionFontSize = if (isCompactLand) 16.sp else 18.sp,
                                onDigitClicked = { if (transitionProgress < 0.1f) viewModel.appendDigit(it) },
                                onDeleteClicked = { if (transitionProgress < 0.1f) viewModel.deleteDigit() },
                                onClearAllClicked = { if (transitionProgress < 0.1f) viewModel.clearTimerInput() }
                            )
                        }
                    }
                } else {
                    // Setup Input layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Display text field representing entered details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val formatted = timerInput.padStart(6, '0')
                            val h = formatted.substring(0, 2)
                            val m = formatted.substring(2, 4)
                            val s = formatted.substring(4, 6)

                            DigitGroup(h, "h", active = timerInput.length >= 5)
                            Spacer(modifier = Modifier.width(6.dp))
                            DigitGroup(m, "m", active = timerInput.length >= 3)
                            Spacer(modifier = Modifier.width(6.dp))
                            DigitGroup(s, "s", active = timerInput.isNotEmpty())
                        }

                        // Preset Pill list row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            ModernPresetButton(label = "+30s", onClick = { viewModel.startPresetTimer(30L) })
                            ModernPresetButton(label = "1 Min", onClick = { viewModel.startPresetTimer(60L) })
                            ModernPresetButton(label = "5 Min", onClick = { viewModel.startPresetTimer(300L) })
                            ModernPresetButton(label = "10 Min", onClick = { viewModel.startPresetTimer(600L) })
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tactical numeric layout keys overlay
                        val isCompactScreen = configuration.screenWidthDp < 600
                        ModernKeypad(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            buttonSize = if (isCompactScreen) 56.dp else 72.dp,
                            spacing = if (isCompactScreen) 8.dp else 12.dp,
                            horizontalSpacing = if (isCompactScreen) 16.dp else 24.dp,
                            fontSize = if (isCompactScreen) 22.sp else 26.sp,
                            actionFontSize = if (isCompactScreen) 16.sp else 18.sp,
                            onDigitClicked = { if (transitionProgress < 0.1f) viewModel.appendDigit(it) },
                            onDeleteClicked = { if (transitionProgress < 0.1f) viewModel.deleteDigit() },
                            onClearAllClicked = { if (transitionProgress < 0.1f) viewModel.clearTimerInput() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Giant minimalist START button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (timerInput.isNotEmpty()) {
                                        accentColor
                                    } else {
                                        Color.White.copy(alpha = 0.03f)
                                    }
                                )
                                .then(
                                    if (timerInput.isEmpty()) {
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("timer_start_button")
                                .clickable(enabled = timerInput.isNotEmpty() && transitionProgress < 0.1f) { viewModel.startTimer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Timer",
                                tint = if (timerInput.isNotEmpty()) com.example.ui.theme.DarkPurple else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- TIMER LAYER (ACTIVE TIMER COUNTDOWN VIEW) ---
        if (transitionProgress > 0.01f || isTimerActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = timerAlpha
                        scaleX = timerScale
                        scaleY = timerScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ReactiveTimerFace(viewModel, isTablet, isLandscape)

                    Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 40.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 20.dp, Alignment.CenterHorizontally)
                    ) {
                        // Reset button
                        ActionButton(
                            text = "Reset",
                            isPrimary = false,
                            onClick = { if (transitionProgress > 0.9f) viewModel.resetTimer() },
                            modifier = Modifier
                                .width(if (isLandscape) 100.dp else 110.dp)
                                .testTag("timer_reset_btn")
                        )

                        // Pause / Resume Toggle
                        ActionButton(
                            text = if (timerStatus == TimerStatus.RUNNING) "Pause" else "Resume",
                            isPrimary = true,
                            accentColor = accentColor,
                            onClick = {
                                if (transitionProgress > 0.9f) {
                                    if (timerStatus == TimerStatus.RUNNING) viewModel.pauseTimer() else viewModel.resumeTimer()
                                }
                            },
                            modifier = Modifier
                                .width(if (isLandscape) 120.dp else 130.dp)
                                .testTag("timer_toggle_btn")
                        )

                        // +1 MIN helper trigger
                        ActionButton(
                            text = "+1m",
                            isPrimary = false,
                            onClick = { if (transitionProgress > 0.9f) viewModel.addOneMinute() },
                            modifier = Modifier
                                .width(if (isLandscape) 90.dp else 100.dp)
                                .testTag("timer_add_1m_btn")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DigitGroup(digits: String, label: String, active: Boolean) {
    val highlightColor = if (active) PurpleGlow else Color.White.copy(alpha = 0.15f)
    val unitColor = if (active) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f)

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = digits,
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            color = if (active) Color.White else Color(0x33FFFFFF),
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-1).sp
        )
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = highlightColor,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsRadioItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = PurpleGlow,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// ==========================================
// STOPWATCH TAB CONTENT VIEWS
// ==========================================
@Composable
fun StopwatchFlipView(viewModel: TimerStopwatchViewModel, isTablet: Boolean, isLandscape: Boolean) {
    val elapsedMs by viewModel.stopwatchElapsedMs.collectAsState()
    val totalSeconds = elapsedMs / 1000
    val m = (totalSeconds / 60) % 60
    val s = totalSeconds % 60
    val cc = (elapsedMs / 10) % 100
    
    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
    val flipTime = "${pad(m)}:${pad(s)}"
    val fractionTime = ".${pad(cc)}"

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            FlipClockDisplay(
                timeString = flipTime,
                height = if (isTablet) 68.dp else if (isLandscape) 48.dp else 56.dp,
                width = if (isTablet) 45.dp else if (isLandscape) 33.dp else 39.dp,
                textSize = if (isTablet) 45f else if (isLandscape) 33f else 39f,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = fractionTime, color = Color.White, fontSize = if (isTablet) 18.sp else if (isLandscape) 15.sp else 16.sp, fontWeight = FontWeight.Bold, modifier = if (!isLandscape) Modifier.padding(bottom = 12.dp) else Modifier)
        }
        if (!isLandscape) Spacer(modifier = Modifier.height(if (isTablet) 15.dp else 9.dp))
    }
}

@Composable
fun StopwatchCircleView(viewModel: TimerStopwatchViewModel, stopwatchStatus: StopwatchStatus, accentColorPrimary: Color) {
    val elapsedMs by viewModel.stopwatchElapsedMs.collectAsState()
    val totalSeconds = elapsedMs / 1000
    val m = (totalSeconds / 60) % 60
    val s = totalSeconds % 60
    val cc = (elapsedMs / 10) % 100
    
    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
    val readableTime = "${pad(m)}:${pad(s)}.${pad(cc)}"

    CircleProgressTimer(
        remainingMs = if (stopwatchStatus == StopwatchStatus.RUNNING) 1L else 0L,
        totalMs = 1L,
        displayString = readableTime,
        statusText = when (stopwatchStatus) {
            StopwatchStatus.IDLE -> "STOPWATCH"
            StopwatchStatus.RUNNING -> "RUNNING"
            StopwatchStatus.PAUSED -> "PAUSED"
        },
        onProgressColor = accentColorPrimary,
        glowEnabled = stopwatchStatus == StopwatchStatus.RUNNING,
        sizeFraction = 0.75f
    )
}

@Composable
fun ReactiveStopwatchFace(viewModel: TimerStopwatchViewModel, isTablet: Boolean, isCompactHeightScreen: Boolean, isLandscape: Boolean) {
    val stopwatchStatus by viewModel.stopwatchStatus.collectAsState()
    val activeVisualMode by viewModel.activeVisualMode.collectAsState()
    val accentColorPrimary = if (activeVisualMode == 1) Color(0xFF4C8DFF) else PurpleGlow

    AnimatedContent(
        targetState = activeVisualMode,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, easing = LinearOutSlowInEasing))) togetherWith 
            (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 1.05f, animationSpec = tween(150)))
        },
        label = "sw_visual_mode_transition"
    ) { mode ->
        if (mode == 1) {
            StopwatchFlipView(viewModel, isTablet, isLandscape)
        } else {
            StopwatchCircleView(viewModel, stopwatchStatus, accentColorPrimary)
        }
    }
}

@Composable
fun StopwatchTabContent(viewModel: TimerStopwatchViewModel) {
    val stopwatchStatus by viewModel.stopwatchStatus.collectAsState()
    val laps by viewModel.laps.collectAsState()
    val activeVisualMode by viewModel.activeVisualMode.collectAsState()
    val accentColorPrimary = if (activeVisualMode == 1) Color(0xFF4C8DFF) else PurpleGlow
    val accentColorSecondary = if (activeVisualMode == 1) Color(0xFF00E6FF) else NeonPink

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600 && configuration.screenHeightDp >= 600
    val isCompactHeightScreen = configuration.screenHeightDp < 600

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .scale(0.88f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: CircleProgressTimer
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LiveDigitalClock(scale = 0.75f)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ReactiveStopwatchFace(viewModel, isTablet, isCompactHeightScreen, isLandscape = true)
                }
            }

            // Right Column: Laps list and Controls below
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lap Times list section (Takes remaining vertical scroll frame)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = if (isTablet) 240.dp else 140.dp)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x06FFFFFF))
                        .border(width = 0.5.dp, color = Color(0x0AFFFFFF), shape = RoundedCornerShape(18.dp))
                ) {
                    if (laps.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO LAPS RECORDED",
                                color = Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(laps.size) {
                            if (laps.isNotEmpty()) {
                                listState.animateScrollToItem(0)
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(laps, key = { it.index }) { lap ->
                                LapRowItem(lap = lap)
                            }
                        }
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .scale(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    when (stopwatchStatus) {
                        StopwatchStatus.IDLE -> {
                            ActionButton(
                                text = "Start",
                                isPrimary = true,
                                accentColor = accentColorPrimary,
                                onClick = { viewModel.startStopwatch() },
                                modifier = Modifier
                                    .width(130.dp)
                                    .testTag("stopwatch_start_btn")
                            )
                        }
                        StopwatchStatus.RUNNING -> {
                            ActionButton(
                                text = "Lap",
                                isPrimary = false,
                                onClick = { viewModel.addLap() },
                                modifier = Modifier
                                    .width(100.dp)
                                    .testTag("stopwatch_lap_btn")
                            )

                            ActionButton(
                                text = "Pause",
                                isPrimary = true,
                                accentColor = accentColorSecondary,
                                onClick = { viewModel.pauseStopwatch() },
                                modifier = Modifier
                                    .width(120.dp)
                                    .testTag("stopwatch_pause_btn")
                            )
                        }
                        StopwatchStatus.PAUSED -> {
                            ActionButton(
                                text = "Reset",
                                isPrimary = false,
                                onClick = { viewModel.resetStopwatch() },
                                modifier = Modifier
                                    .width(100.dp)
                                    .testTag("stopwatch_reset_btn")
                            )

                            ActionButton(
                                text = "Resume",
                                isPrimary = true,
                                accentColor = accentColorPrimary,
                                onClick = { viewModel.startStopwatch() },
                                modifier = Modifier
                                    .width(120.dp)
                                    .testTag("stopwatch_resume_btn")
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 32.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display counting stopwatch layout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LiveDigitalClock(scale = 0.75f)
                Spacer(modifier = Modifier.height(if (isCompactHeightScreen) 8.dp else 16.dp))
                ReactiveStopwatchFace(viewModel, isTablet, isCompactHeightScreen, isLandscape = false)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lap Times list section (Takes remaining vertical scroll frame)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = if (isTablet) 240.dp else 160.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x06FFFFFF))
                    .border(width = 0.5.dp, color = Color(0x0AFFFFFF), shape = RoundedCornerShape(18.dp))
            ) {
                if (laps.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO LAPS RECORDED",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record laps on running stopwatch.",
                            color = Color.DarkGray,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(laps.size) {
                        if (laps.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(laps, key = { it.index }) { lap ->
                            LapRowItem(lap = lap)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 8.dp))

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .scale(0.8f),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                when (stopwatchStatus) {
                    StopwatchStatus.IDLE -> {
                        // Start button only
                        ActionButton(
                            text = "Start",
                            isPrimary = true,
                            accentColor = PurpleGlow,
                            onClick = { viewModel.startStopwatch() },
                            modifier = Modifier
                                .width(130.dp)
                                .testTag("stopwatch_start_btn")
                        )
                    }
                    StopwatchStatus.RUNNING -> {
                        // Lap and Pause
                        ActionButton(
                            text = "Lap",
                            isPrimary = false,
                            onClick = { viewModel.addLap() },
                            modifier = Modifier
                                .width(110.dp)
                                .testTag("stopwatch_lap_btn")
                        )

                        ActionButton(
                            text = "Pause",
                            isPrimary = true,
                            accentColor = NeonPink,
                            onClick = { viewModel.pauseStopwatch() },
                            modifier = Modifier
                                .width(130.dp)
                                .testTag("stopwatch_pause_btn")
                        )
                    }
                    StopwatchStatus.PAUSED -> {
                        // Reset and Resume
                        ActionButton(
                            text = "Reset",
                            isPrimary = false,
                            onClick = { viewModel.resetStopwatch() },
                            modifier = Modifier
                                .width(110.dp)
                                .testTag("stopwatch_reset_btn")
                        )

                        ActionButton(
                            text = "Resume",
                            isPrimary = true,
                            accentColor = PurpleGlow,
                            onClick = { viewModel.startStopwatch() },
                            modifier = Modifier
                                .width(130.dp)
                                .testTag("stopwatch_resume_btn")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LapRowItem(lap: LapRecord) {
    val lapMin = (lap.lapTimeMs / 60000) % 60
    val lapSec = (lap.lapTimeMs / 1000) % 60
    val lapCc = (lap.lapTimeMs / 10) % 100
    
    val pad = { n: Long -> if (n < 10) "0$n" else n.toString() }
    val formattedLap = "${pad(lapMin)}:${pad(lapSec)}.${pad(lapCc)}"

    val overallMin = (lap.totalTimeMs / 60000) % 60
    val overallSec = (lap.totalTimeMs / 1000) % 60
    val overallCc = (lap.totalTimeMs / 10) % 100
    val formattedOverall = "${pad(overallMin)}:${pad(overallSec)}.${pad(overallCc)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x08FFFFFF))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val padInt = { n: Int -> if (n < 10) "0$n" else n.toString() }
            Text(
                text = "#${padInt(lap.index)}",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = PurpleGlow,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delta: $formattedLap",
                fontSize = 10.sp,
                color = Color.LightGray
            )
        }
        
        Text(
            text = formattedOverall,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}


// ==========================================
// ACTIVE ALARM SYSTEM DIALOG OVERLAY
// ==========================================
@Composable
fun ActiveAlarmOverlay(viewModel: TimerStopwatchViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Large animated warning icon and title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "shiver")
            
            // Icon Shiver animation represents heavy ringing feedback
            val shiverAngle by infiniteTransition.animateFloat(
                initialValue = -12f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(120, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shiver_rot"
            )

            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Lock Alert Active",
                tint = Color.White,
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer { rotationZ = shiverAngle }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TIMER COMPLETED!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "The countdown duration has fully elapsed.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        // colosssal dismiss & snooze actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionButton(
                text = "Dismiss Alarm",
                isPrimary = true,
                accentColor = Color.White,
                onClick = { viewModel.dismissAlarm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("alarm_dismiss_btn")
            )

            ActionButton(
                text = "Snooze +1 Min",
                isPrimary = false,
                onClick = { viewModel.snoozeAlarm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("alarm_snooze_btn")
            )
        }
    }
}


// ==========================================
// CUSTOM ALARM SOUND SELECTOR DIALOG
// ==========================================
@Composable
fun SoundSelectionDialog(
    currentSelection: AlarmSoundPreset,
    onSelect: (AlarmSoundPreset) -> Unit,
    onDismiss: () -> Unit,
    onPlayPreview: (AlarmSoundPreset) -> Unit,
    onStopPreview: () -> Unit
) {
    var previewingId by remember { mutableStateOf<String?>(null) }

    // Terminate preview playback on dialog dismiss
    DisposableEffect(Unit) {
        onDispose {
            onStopPreview()
        }
    }

    Dialog(onDismissRequest = {
        onStopPreview()
        onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PurpleGlow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sound Profiles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Choose ringtone or wellness chime",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable choices
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AlarmSoundPreset.values()) { preset ->
                        val isSelected = preset == currentSelection
                        val isCurrentlyPreviewing = previewingId == preset.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PurpleGlow.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onSelect(preset)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                                // Elegant description text
                                val desc = when (preset) {
                                    AlarmSoundPreset.ETHEREAL_CHIME -> "528Hz – Cellular energy & healing"
                                    AlarmSoundPreset.ZEN_RESONANCE -> "432Hz – Therapeutic cosmic tuning"
                                    AlarmSoundPreset.MINIMAL_PULSE -> "880Hz – High-efficiency pulse beep"
                                    AlarmSoundPreset.SYSTEM_ALARM -> "Standard default alarm sound"
                                    AlarmSoundPreset.SYSTEM_RINGTONE -> "Your phone's standard ringtone"
                                    AlarmSoundPreset.SYSTEM_NOTIFICATION -> "Compact systemic alert chime"
                                }
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f)
                                )
                            }

                            // Preview floating action trigger
                            IconButton(
                                onClick = {
                                    if (isCurrentlyPreviewing) {
                                        onStopPreview()
                                        previewingId = null
                                    } else {
                                        onPlayPreview(preset)
                                        previewingId = preset.id
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrentlyPreviewing) PurpleGlow else Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview",
                                    tint = if (isCurrentlyPreviewing) Color(0xFF381E72) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close button
                ActionButton(
                    text = "Confirm Choice",
                    isPrimary = true,
                    onClick = {
                        onStopPreview()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// LIVE DIGITAL CLOCK COMPOSABLE
// ==========================================
@Composable
fun LiveDigitalClock(modifier: Modifier = Modifier, scale: Float = 1f) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            if (now / 1000 != currentTime / 1000) {
                currentTime = now
            }
            kotlinx.coroutines.delay(200)
        }
    }

    // derivedStateOf prevents recomposition of the whole component if the exact text hasn't changed.
    val timeState by remember(currentTime) {
        derivedStateOf {
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTime
            }

            val hour = calendar.get(java.util.Calendar.HOUR)
            val displayHour = if (hour == 0) 12 else hour
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val second = calendar.get(java.util.Calendar.SECOND)
            val amPm = if (calendar.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"

            val timeString = String.format("%d:%02d", displayHour, minute)
            val secondsString = String.format("%02d", second)
            Triple(timeString, secondsString, amPm)
        }
    }

    val (timeString, secondsString, amPm) = timeState

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(bottom = (12 * scale).dp)
            .testTag("live_digital_clock")
    ) {
        Text(
            text = timeString,
            fontSize = (22 * scale).sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = (0.5f * scale).sp
        )
        Spacer(modifier = Modifier.width((3 * scale).dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = (2 * scale).dp)
        ) {
            Text(
                text = secondsString,
                fontSize = (12 * scale).sp,
                fontWeight = FontWeight.Light,
                color = PurpleGlow,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width((3 * scale).dp))
            Text(
                text = amPm,
                fontSize = (10 * scale).sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

// ==========================================
// PREMIUM APP OPENING SPLASH ANIMATION
// ==========================================
@Composable
fun IntroSplashAnimation(onFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val glowScaling = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Step 1: Smoothly present the white dot from the center
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        
        // Step 2: Zoom out dot aggressively while expanding/blurring gradient aura and fading screen
        launch {
            scale.animateTo(
                targetValue = 26f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
        }
        launch {
            glowScaling.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
        }
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 950, easing = LinearEasing)
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E).copy(alpha = alpha.value)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseDotRadius = 14.dp.toPx()
            
            // Premium background bloom gradient behind the dot as it expands
            if (glowScaling.value > 0f) {
                // Decay the alpha as glow expands to simulate organic blur spread fading out
                val bloomAlpha = ((1f - glowScaling.value) * alpha.value * 0.75f).coerceIn(0f, 1f)
                val bloomRadius = size.width * 0.85f * glowScaling.value
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = bloomAlpha * 0.85f),
                            Color(0xFF5B21B6).copy(alpha = bloomAlpha * 0.55f), // Premium Royal Purple Inner Glow
                            Color(0xFF00E6FF).copy(alpha = bloomAlpha * 0.25f), // Shifting Cyan Outer Glow
                            Color.Transparent
                        ),
                        center = center,
                        radius = bloomRadius
                    ),
                    center = center,
                    radius = bloomRadius
                )
            }

            // Draw primary middle white dot representing app icon
            if (alpha.value > 0f) {
                val dotRadius = baseDotRadius * scale.value
                drawCircle(
                    color = Color.White.copy(alpha = alpha.value),
                    radius = dotRadius,
                    center = center
                )
            }
        }
    }
}

@Composable
fun LivePreviewCard(componentName: String, style: ComponentStyle) {
    val bgColorVal = try {
        Color(android.graphics.Color.parseColor(style.bgColor))
    } catch (e: Exception) {
        Color.DarkGray
    }
    
    val borderColorVal = try {
        Color(android.graphics.Color.parseColor(style.borderColor))
    } catch (e: Exception) {
        Color.White
    }

    val textColorVal = try {
        Color(android.graphics.Color.parseColor(style.textColor))
    } catch (e: Exception) {
        Color.White
    }

    val accentColorVal = try {
        Color(android.graphics.Color.parseColor(style.accentColor))
    } catch (e: Exception) {
        Color.Yellow
    }

    val bgBrush = if (style.gradientEnabled) {
        val startColor = try {
            Color(android.graphics.Color.parseColor(style.gradientStartColor))
        } catch (e: Exception) {
            Color.Black
        }
        val endColor = try {
            Color(android.graphics.Color.parseColor(style.gradientEndColor))
        } catch (e: Exception) {
            Color.DarkGray
        }
        Brush.linearGradient(colors = listOf(startColor, endColor))
    } else {
        Brush.linearGradient(colors = listOf(bgColorVal.copy(alpha = style.opacity), bgColorVal.copy(alpha = style.opacity)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LIVE ECOSYSTEM STYLE PREVIEW",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(style.cornerRadius.dp))
                .background(bgBrush)
                .border(
                    width = style.borderThickness.dp,
                    color = borderColorVal,
                    shape = RoundedCornerShape(style.cornerRadius.dp)
                )
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (componentName) {
                    "dockableIsland" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Timer,
                                contentDescription = "Timer Indicator",
                                tint = accentColorVal,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "24:18",
                                color = accentColorVal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                    "floatingBubble" -> {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColorVal.copy(alpha = 0.15f))
                                .border(1.dp, accentColorVal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "12:05",
                                color = textColorVal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "expandedBubblePanel" -> {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "ACTIVE COOP TIMER",
                                color = accentColorVal,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "15:20",
                                color = textColorVal,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(textColorVal.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Pause", color = accentColorVal, fontSize = 9.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(textColorVal.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Reset", color = textColorVal, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                    "timerWidget" -> {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Live Timer Widget (2x2)",
                                color = accentColorVal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "00:45:12",
                                color = textColorVal,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(textColorVal.copy(alpha = 0.08f))
                                        .padding(4.dp)
                                ) {
                                    Text("Add +1m", color = textColorVal, fontSize = 8.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(textColorVal.copy(alpha = 0.08f))
                                        .padding(4.dp)
                                ) {
                                    Text("Pause", color = accentColorVal, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                    "stopwatchWidget" -> {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                "Stopwatch Widget (2x1)",
                                color = accentColorVal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "04:18.25",
                                color = textColorVal,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "notificationControls" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = accentColorVal,
                                modifier = Modifier.size(14.dp)
                            )
                            Column {
                                Text(
                                    "Ecosystem Running",
                                    color = textColorVal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tap to toggle notification active status",
                                    color = textColorVal.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivePreviewWidgetCard(componentName: String, style: ComponentStyle) {
    Box(modifier = Modifier.scale(0.85f)) {
        LivePreviewCard(componentName = componentName, style = style)
    }
}

@Composable
fun MockSmartphoneEcosystem(config: AppAppearanceConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0C091A))
            .border(3.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mock Status Bar / Bezel Notch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "10:10",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
            )
            // Center Dockable Island Preview
            Box(modifier = Modifier.width(160.dp)) {
                LivePreviewWidgetCard(componentName = "dockableIsland", style = config.dockableIsland)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center space representing Android Home Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF1E0E3D), Color(0xFF0F081D)))),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Star Field mesh simulated background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0x228B5CF6), radius = 300f, center = Offset(size.width / 2, size.height / 2))
                drawCircle(color = Color(0x1106B6D4), radius = 200f, center = Offset(size.width * 0.2f, size.height * 0.3f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Collapsed overlay bubble floating in space
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(60.dp)) {
                        LivePreviewWidgetCard(componentName = "floatingBubble", style = config.floatingBubble)
                    }
                    Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        LivePreviewWidgetCard(componentName = "expandedBubblePanel", style = config.expandedBubblePanel)
                    }
                }

                // Widgets placed on Home Screen Desktop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LivePreviewWidgetCard(componentName = "timerWidget", style = config.timerWidget)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        LivePreviewWidgetCard(componentName = "stopwatchWidget", style = config.stopwatchWidget)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Notification center item at the bottom of ecosystems
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "System Active Notification Layer:",
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LivePreviewWidgetCard(componentName = "notificationControls", style = config.notificationControls)
            }
        }
    }
}

@Composable
fun DesignTabContent(viewModel: TimerStopwatchViewModel) {
    val config by viewModel.appearanceConfigState.collectAsState()
    val customPresets by viewModel.customPresetNames.collectAsState()
    val wallpaperUri by viewModel.wallpaperUri.collectAsState()
    val overlayMode by viewModel.overlayMode.collectAsState()

    val context = LocalContext.current
    
    // Choose active Studio Section
    var activeStudioTab by remember { mutableStateOf("island") } // "island", "bubble", "widget", "ecosystem"
    var isExpertMode by remember { mutableStateOf(false) }

    // Floating Bubble sub tab
    var bubbleSubTab by remember { mutableStateOf("floatingBubble") } // "floatingBubble", "expandedBubblePanel"
    // Widget sub tab
    var widgetSubTab by remember { mutableStateOf("timerWidget") } // "timerWidget", "stopwatchWidget"

    val activeComponentId = when (activeStudioTab) {
        "island" -> "dockableIsland"
        "bubble" -> bubbleSubTab
        "widget" -> widgetSubTab
        else -> "dockableIsland"
    }

    val activeStyle = when (activeComponentId) {
        "dockableIsland" -> config.dockableIsland
        "floatingBubble" -> config.floatingBubble
        "expandedBubblePanel" -> config.expandedBubblePanel
        "timerWidget" -> config.timerWidget
        "stopwatchWidget" -> config.stopwatchWidget
        else -> config.dockableIsland
    }

    val updateStyle = { updated: ComponentStyle ->
        viewModel.updateComponentStyle(activeComponentId, updated)
    }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    // Photo/Wallpaper Picker
    val pickMedia = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            viewModel.setWallpaperUri(uri.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- 1. STUDIO TITLE AND GLOBAL PRESET CHEST ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APPEARANCE STUDIO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurpleGlow,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Craft gorgeous visual themes for overlays & widgets",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Expert Mode Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .clickable { isExpertMode = !isExpertMode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EXPERT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpertMode) PurpleGlow else Color.Gray
                        )
                        Switch(
                            checked = isExpertMode,
                            onCheckedChange = { isExpertMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PurpleGlow,
                                checkedTrackColor = PurpleGlow.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.DarkGray,
                                uncheckedTrackColor = Color.Black
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
        }

        // --- 2. THEME BRAND PRESET BOARD (1-TAP DESIGN CODES) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "PROFESSIONAL DESIGN CODES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                val presetCols = listOf(
                    listOf("VisionOS Glass", "iOS Glass", "Frosted Ice"),
                    listOf("AMOLED Black", "Neon Cyberpunk", "Emerald Dream"),
                    listOf("Material You", "HyperOS", "One UI", "Nothing OS")
                )

                presetCols.forEachIndexed { rowIdx, cols ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cols.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.applyPreset(preset)
                                        android.widget.Toast.makeText(context, "$preset Theme Loaded!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset,
                                    color = PurpleGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. STUDIO SECTOR SWITCHER (DOCK, BUBBLE, WIDGET, UNIFIED ECOSYSTEM) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(3.dp)
            ) {
                val studios = listOf(
                    "island" to "🏝️ Dock Notch",
                    "bubble" to "🫧 Floating",
                    "widget" to "⏱️ Desktop",
                    "ecosystem" to "🌐 Unified Preview"
                )
                studios.forEach { (tabId, label) ->
                    val isSel = activeStudioTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSel) PurpleGlow else Color.Transparent)
                            .clickable { activeStudioTab = tabId }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.Black else Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- 4. GRAPHIC PREVIEWS & STUDIO SPECIFIC EDITORS ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                when (activeStudioTab) {
                    "island" -> {
                        Text(
                            text = "🏝️ DOCKABLE ISLAND NOTCH STUDIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleGlow
                        )
                        Text(
                            text = "The status-bar notch defaults to a premium AMOLED-black for Dynamic Island mimicry. Customize highlights below.",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Live Preview Box over a mockup wallpaper background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.radialGradient(colors = listOf(Color(0xFF2E0854), Color(0xFF0F0B1E))))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "AMOLED Wallpaper Surface Mock",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                            LivePreviewWidgetCard(componentName = "dockableIsland", style = config.dockableIsland)
                        }
                    }

                    "bubble" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🫧 FLOATING BUBBLE STUDIO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleGlow
                                )
                                Text(
                                    text = "Customize collapsed & expanded overlay panels separately.",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            
                            // Link style button to easily copy look!
                            Button(
                                onClick = {
                                    val reference = if (bubbleSubTab == "floatingBubble") config.floatingBubble else config.expandedBubblePanel
                                    viewModel.updateComponentStyle("floatingBubble", reference)
                                    viewModel.updateComponentStyle("expandedBubblePanel", reference)
                                    android.widget.Toast.makeText(context, "Bubble & Panel styles synchronized!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("🔗 Sync Both", fontSize = 9.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Sub selector inside bubble
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(2.dp)
                        ) {
                            listOf("floatingBubble" to "💬 Collapsed State", "expandedBubblePanel" to "📋 Expanded Board").forEach { (subId, label) ->
                                val isSubSelected = bubbleSubTab == subId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSubSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { bubbleSubTab = subId }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSubSelected) PurpleGlow else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Double Real-Time Previews (Satisfies requirement of displaying BOTH states at once)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B))))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Collapsed Preview
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("COLLAPSED BUBBLE", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LivePreviewWidgetCard(componentName = "floatingBubble", style = config.floatingBubble)
                            }
                            
                            // Divider line
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.1f)))

                            // Expanded Panel Preview
                            Column(
                                modifier = Modifier.weight(1.3f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("EXPANDED ACRYLIC PANEL", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LivePreviewWidgetCard(componentName = "expandedBubblePanel", style = config.expandedBubblePanel)
                            }
                        }
                    }

                    "widget" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⏱️ DESKTOP APP WIDGET STUDIO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleGlow
                                )
                                Text(
                                    text = "Design wallpaper-aware glassmorphic widgets.",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Button(
                                onClick = {
                                    val reference = if (widgetSubTab == "timerWidget") config.timerWidget else config.stopwatchWidget
                                    viewModel.updateComponentStyle("timerWidget", reference)
                                    viewModel.updateComponentStyle("stopwatchWidget", reference)
                                    android.widget.Toast.makeText(context, "Widgets synchronized!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("🔗 Sync Both", fontSize = 9.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub selectors inside widgets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(2.dp)
                        ) {
                            listOf("timerWidget" to "⏱️ Timer Widget", "stopwatchWidget" to "🏁 Stopwatch Widget").forEach { (subId, label) ->
                                val isSubSelected = widgetSubTab == subId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSubSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { widgetSubTab = subId }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSubSelected) PurpleGlow else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Joint Widget previews over colorful grid to simulate Android desktop
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(colors = listOf(Color(0xFF14213d), Color(0xFF000000))))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ANDROID LAUNCHER WIDGET PREVIEW CLASS", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Timer (2x2 Grid)", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LivePreviewWidgetCard(componentName = "timerWidget", style = config.timerWidget)
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Stopwatch (2x1 Grid)", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LivePreviewWidgetCard(componentName = "stopwatchWidget", style = config.stopwatchWidget)
                                }
                            }
                        }
                    }

                    "ecosystem" -> {
                        Text(
                            text = "🌐 UNIFIED ECOSYSTEM PREVIEW PANEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleGlow
                        )
                        Text(
                            text = "A live layout containing all visual layers synchronized together in real time.",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Render full smartphone frame mockup
                        MockSmartphoneEcosystem(config = config)
                    }
                }

                // If not eco tab, render the customizable slider parameters
                if (activeStudioTab != "ecosystem") {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Standard color palette chips
                    Text(
                        text = "VISUAL INTUITION COLOR CHIPS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Quick Palette chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colorChips = listOf(
                            "#000000" to "Amoled",
                            "#0C0414" to "Tech",
                            "#1A1D26" to "Slate",
                            "#FFFFFF" to "Glass",
                            "#FF2A6D" to "Neon",
                            "#00E6FF" to "Cyan",
                            "#34D399" to "Mint"
                        )
                        colorChips.forEach { (hex, alias) ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (activeStyle.bgColor.lowercase() == hex.lowercase() || 
                                                   activeStyle.accentColor.lowercase() == hex.lowercase()) 2.dp else 0.5.dp,
                                        color = if (activeStyle.bgColor.lowercase() == hex.lowercase()) Color.White else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        // Quick set background or accent based on what tab is chosen
                                        if (activeComponentId == "dockableIsland") {
                                            updateStyle(activeStyle.copy(accentColor = hex))
                                        } else {
                                            updateStyle(activeStyle.copy(bgColor = hex))
                                        }
                                        android.widget.Toast.makeText(context, "$alias Color Seed Selected", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- SLIDERS FOR VISUAL PARAMS (BASIC MODE / STANDARD EXPERT MERGE) ---
                    CustomSettingSlider(
                        title = "BACKDROP TRANSPARENCY",
                        value = activeStyle.opacity * 100f,
                        valueRange = 5f..100f,
                        displaySuffix = "%",
                        onValueChange = { updateStyle(activeStyle.copy(opacity = it / 100f)) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CustomSettingSlider(
                        title = "GLASS BLUR FROSTING",
                        value = activeStyle.blur,
                        valueRange = 0f..40f,
                        displaySuffix = "dp",
                        onValueChange = { updateStyle(activeStyle.copy(blur = it)) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CustomSettingSlider(
                        title = "GLASS EDGE CORNER SPEED",
                        value = activeStyle.cornerRadius.toFloat(),
                        valueRange = 4f..32f,
                        displaySuffix = "dp",
                        onValueChange = { updateStyle(activeStyle.copy(cornerRadius = it.toInt())) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CustomSettingSlider(
                        title = "NEON GLOW STRENGTH",
                        value = activeStyle.glowStrength * 100f,
                        valueRange = 0f..100f,
                        displaySuffix = "%",
                        onValueChange = { updateStyle(activeStyle.copy(glowStrength = it / 100f)) }
                    )

                    // EXTREME DETAIL EXPERT MODE (Revealed only if isExpertMode is toggled)
                    AnimatedVisibility(
                        visible = isExpertMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "EXPERT LAYOUT STRATEGIC DECORATOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleGlow,
                                letterSpacing = 1.sp
                            )
                            
                            // Manual Input color boxes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = activeStyle.bgColor,
                                    onValueChange = { updateStyle(activeStyle.copy(bgColor = it)) },
                                    label = { Text("Bg Hex", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                                OutlinedTextField(
                                    value = activeStyle.textColor,
                                    onValueChange = { updateStyle(activeStyle.copy(textColor = it)) },
                                    label = { Text("Text Hex", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = activeStyle.borderColor,
                                    onValueChange = { updateStyle(activeStyle.copy(borderColor = it)) },
                                    label = { Text("Border Hex", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                                OutlinedTextField(
                                    value = activeStyle.glowColor,
                                    onValueChange = { updateStyle(activeStyle.copy(glowColor = it)) },
                                    label = { Text("Glow Hex", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = activeStyle.accentColor,
                                    onValueChange = { updateStyle(activeStyle.copy(accentColor = it)) },
                                    label = { Text("Accent Hex", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Border Thickness", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    Slider(
                                        value = activeStyle.borderThickness,
                                        onValueChange = { updateStyle(activeStyle.copy(borderThickness = it)) },
                                        valueRange = 0f..4f,
                                        colors = SliderDefaults.colors(thumbColor = PurpleGlow)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            CustomSettingSlider(
                                title = "FROSTED SHADOW DEPTH",
                                value = activeStyle.shadowIntensity,
                                valueRange = 0f..18f,
                                displaySuffix = "dp",
                                onValueChange = { updateStyle(activeStyle.copy(shadowIntensity = it)) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Dual Gradient setup
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CUSTOM DUAL-GRADIENT BLEND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Uses custom multi-color start/end points", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                                Switch(
                                    checked = activeStyle.gradientEnabled,
                                    onCheckedChange = { updateStyle(activeStyle.copy(gradientEnabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PurpleGlow)
                                )
                            }

                            if (activeStyle.gradientEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = activeStyle.gradientStartColor,
                                        onValueChange = { updateStyle(activeStyle.copy(gradientStartColor = it)) },
                                        label = { Text("Start Hex", fontSize = 9.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                    OutlinedTextField(
                                        value = activeStyle.gradientEndColor,
                                        onValueChange = { updateStyle(activeStyle.copy(gradientEndColor = it)) },
                                        label = { Text("End Hex", fontSize = 9.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. THEME SAVING AND IMPORT PORTABILITY HUB ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "THEME EXPORT & SHARING SUITE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleGlow,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showSavePresetDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                    ) {
                        Text("Save Current Style", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24))
                    ) {
                        Text("Import/Paste JSON Preset", color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Ecosystem Config", config.toSerializedString())
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Full preset config copied! Share anywhere.", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f))
                ) {
                    Text("Copy Active Config JSON 📋", color = Color.White, fontSize = 11.sp)
                }

                if (customPresets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "YOUR SAVED CUSTOM VISUALS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    customPresets.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clickable {
                                        val prefs = context.getSharedPreferences("appearance_presets", android.content.Context.MODE_PRIVATE)
                                        val json = prefs.getString("preset_config_$name", "") ?: ""
                                        if (json.isNotEmpty()) {
                                            viewModel.updateAppearanceConfig(AppAppearanceConfig.fromSerializedString(json))
                                            android.widget.Toast.makeText(context, "Custom Preset '$name' Applied!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            )
                            Button(
                                onClick = { viewModel.deleteCustomPreset(name) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x6A9D1C2A)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Delete", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // dialogs for saving themes
    if (showSavePresetDialog) {
        Dialog(onDismissRequest = { showSavePresetDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Name Custom Visual Style", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = savePresetName,
                        onValueChange = { savePresetName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        label = { Text("E.g., Emerald Aura", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showSavePresetDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                if (savePresetName.trim().isNotEmpty()) {
                                    viewModel.saveAsCustomPreset(savePresetName.trim())
                                    savePresetName = ""
                                    showSavePresetDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Import Preset Configuration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        label = { Text("Paste theme JSON config string", fontSize = 10.sp) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showImportDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val text = importJsonText.trim()
                                if (text.isNotEmpty()) {
                                    try {
                                        val parsed = AppAppearanceConfig.fromSerializedString(text)
                                        viewModel.updateAppearanceConfig(parsed)
                                        importJsonText = ""
                                        showImportDialog = false
                                        android.widget.Toast.makeText(context, "Theme Applied Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Invalid theme signature format!", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displaySuffix: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
            Text(
                text = String.format("%.0f%s", value, displaySuffix),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PurpleGlow
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PurpleGlow,
                activeTrackColor = PurpleGlow.copy(alpha = 0.7f),
                inactiveTrackColor = Color.White.copy(alpha = 0.08f)
            )
        )
    }
}

@Composable
fun AppearanceSettingsDialog(
    viewModel: TimerStopwatchViewModel,
    onDismiss: () -> Unit
) {
    val config by viewModel.appearanceConfigState.collectAsState()
    val overlayMode by viewModel.overlayMode.collectAsState()
    
    // Choose active preview tab
    var previewTab by remember { mutableStateOf("overlays") } // "overlays" or "widgets"
    
    // Simulated state for previewing paused vs running
    var previewPaused by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
                .border(2.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                
                               // Dialog Body in LazyColumn for perfect scrollability
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // PRESET SELECTION SECTION
                    item {
                        Column {
                            Text(
                                text = "THEME DESIGN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val presets = listOf(
                                "System Default" to "default",
                                "AMOLED Black" to "black",
                                "Use Wallpaper Background" to "wallpaper"
                            )
                            
                            presets.chunked(1).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { (label, key) ->
                                        val isSelected = when (key) {
                                            "default" -> config.floatingBubble.bgColor == "#0C0C12" && config.floatingBubble.opacity == 0.45f
                                            "black" -> config.floatingBubble.bgColor == "#000000" && config.floatingBubble.opacity == 1.0f
                                            "wallpaper" -> config.floatingBubble.wallpaperAware
                                            else -> false
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) Color.White.copy(alpha = 0.12f)
                                                    else Color.White.copy(alpha = 0.04f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    viewModel.applyPreset(label)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // OVERLAY MODE SELECTION SECTION
                    item {
                        Column {
                            Text(
                                text = "OVERLAY MODE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Dockable Island" to 0,
                                    "Floating Bubble" to 1,
                                    "No Overlays" to 2
                                ).forEach { (label, mode) ->
                                    val isSelected = overlayMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.12f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.setOverlayMode(mode)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // PREVIEW STATE CONTROLLER SECTION
                    item {
                        Column {
                            Text(
                                text = "SIMULATED TIMER STATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Running (White Time)" to false,
                                    "Paused (Red Time)" to true
                                ).forEach { (label, isPausedState) ->
                                    val isSelected = previewPaused == isPausedState
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.10f)
                                                else Color.White.copy(alpha = 0.03f)
                                            )
                                            .border(
                                                width = 0.8.dp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                previewPaused = isPausedState
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // PREVIEW TAB SELECTOR SECTION
                    item {
                        Column {
                            Text(
                                text = "LIVE EXTERNAL SURFACE PREVIEWS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Overlays Preview" to "overlays",
                                    "Widgets Preview" to "widgets"
                                ).forEach { (label, tabKey) ->
                                    val isSelected = previewTab == tabKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.10f)
                                                else Color.White.copy(alpha = 0.03f)
                                            )
                                            .border(
                                                width = 0.8.dp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                previewTab = tabKey
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // VISUAL CONTENT CONTAINER
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            val timeValColor = if (previewPaused) Color.Red else Color.White
                            
                            if (previewTab == "overlays") {
                                // 1. Dockable Island Preview Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DOCKABLE ISLAND", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Simulated Black Capsule Status bar notch
                                        Row(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF000000))
                                                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                                .padding(horizontal = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (previewPaused) Color.Gray else Color.Green)
                                                )
                                                Text("ACTIVE TIMER", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                            }
                                            Text("12:34", fontSize = 11.sp, color = timeValColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                // 2. Floating Bubble & Expanded Panel Preview
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("FLOATING BUBBLE OVERLAY", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Floating Bubble
                                            val bubbleBg = config.floatingBubble.getComposeBgColor()
                                            val bubbleBorder = config.floatingBubble.getComposeBorderColor()
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(27.dp))
                                                        .background(bubbleBg)
                                                        .border(0.8.dp, bubbleBorder, RoundedCornerShape(27.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("12:34", fontSize = 11.sp, color = timeValColor, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Bubble", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                                            }
                                            
                                            // Expanded Bubble Panel Card
                                            val expandBg = config.expandedBubblePanel.getComposeBgColor()
                                            val expandBorder = config.expandedBubblePanel.getComposeBorderColor()
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = expandBg),
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .width(160.dp)
                                                        .border(0.8.dp, expandBorder, RoundedCornerShape(14.dp))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text("ACTIVE TIMER", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("12:34", fontSize = 16.sp, color = timeValColor, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            listOf("Pause", "+1m", "Reset").forEach { bLabel ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(18.dp)
                                                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(bLabel, fontSize = 7.sp, color = Color.White)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Expanded Panel", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 3. Widgets Previews: Timer Widget & Stopwatch Widget
                                val amoledOverride = config.timerWidget.bgColor == "#000000" && config.timerWidget.opacity == 1.0f
                                
                                // Timer Widget Card
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (amoledOverride) Color.Black else Color(0xFF0C0C12).copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (amoledOverride) Color(0xFF333333) else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.HourglassEmpty,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                                Text("Timer: Active", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Text("WIDGET PREVIEW", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f))
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "12:34",
                                            fontSize = 28.sp,
                                            color = timeValColor,
                                            fontWeight = FontWeight.ExtraLight,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+1 Min", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Pause", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Stopwatch Widget Card
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (amoledOverride) Color.Black else Color(0xFF0C0C12).copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (amoledOverride) Color(0xFF333333) else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                                Text("Stopwatch", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Text("WIDGET PREVIEW", fontSize = 8.sp, color = Color.White.copy(alpha = 0.4f))
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "12:34.56",
                                            fontSize = 28.sp,
                                            color = timeValColor,
                                            fontWeight = FontWeight.ExtraLight,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Reset", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Pause", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

