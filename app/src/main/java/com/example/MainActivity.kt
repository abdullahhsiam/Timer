package com.example

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.os.Bundle
import android.view.WindowManager
import android.provider.Settings
import android.net.Uri
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
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    var menuExpanded by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }

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

    // Wrap entire layout in standard animated background
    AnimatedGradientBackground(
        isPulsingAlarm = alarmTriggered,
        isRunningActive = timerStatus == TimerStatus.RUNNING || stopwatchStatus == StopwatchStatus.RUNNING,
        isAnimated = isBackgroundAnimated
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
                    val readableTime = String.format("%02d:%02d.%02d", m, s, cc)

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
                    val displayString = if (h > 0) {
                        String.format("%02d:%02d:%02d", h, m, s)
                    } else {
                        String.format("%02d:%02d", m, s)
                    }

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
            // Main Standard Navigation and Layout Structure
            val isFullScreenDisplay = (timerStatus == TimerStatus.RUNNING && activeTab == 0) || 
                                      (stopwatchStatus == StopwatchStatus.RUNNING && activeTab == 1)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Hidden in immersive Full-Screen Display Mode to keep design clean and non-distracting)
                AnimatedVisibility(
                    visible = !isFullScreenDisplay,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF141419))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Alarm Sound Settings", color = Color.White, fontSize = 14.sp) },
                                        onClick = {
                                            menuExpanded = false
                                            showSoundDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = if (isBackgroundAnimated) "Background: Animated" else "Background: Static Fluid", 
                                                color = Color.White, 
                                                fontSize = 14.sp
                                            ) 
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.setBackgroundAnimated(!isBackgroundAnimated)
                                        }
                                    )
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Low-Opacity Pill Tab Switcher (bg-white/5, expanded to fit 3 options)
                        Row(
                            modifier = Modifier
                                .width(310.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TabItem(
                                label = "Timer",
                                selected = activeTab == 0,
                                icon = Icons.Default.Timer,
                                onClick = { viewModel.selectTab(0) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("tab_timer")
                            )
                            TabItem(
                                label = "Watch",
                                selected = activeTab == 1,
                                icon = Icons.Default.Schedule,
                                onClick = { viewModel.selectTab(1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("tab_stopwatch")
                            )
                            TabItem(
                                label = "Design",
                                selected = activeTab == 2,
                                icon = Icons.Filled.Layers,
                                onClick = { viewModel.selectTab(2) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("tab_styling")
                            )
                        }
                    }
                }

                // Main Display Center content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeTab == 0) {
                        TimerTabContent(viewModel = viewModel)
                    } else if (activeTab == 1) {
                        StopwatchTabContent(viewModel = viewModel)
                    } else {
                        DesignTabContent(viewModel = viewModel)
                    }
                }

                // Global Settings Bottom Footer (Hidden during immersive lock to satisfy Always On requirements)
                AnimatedVisibility(
                    visible = !isFullScreenDisplay,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
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
                                                context.startService(Intent(context, OverlayBubbleService::class.java))
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
}

@Composable
fun TabItem(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
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
                tint = if (selected) Color.White else Color.White.copy(alpha = 0.40f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.40f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
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
    val timerRemainingMs by viewModel.timerRemainingMs.collectAsState()
    val timerMaxMs by viewModel.timerMaxMs.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Crossfade(targetState = timerStatus, label = "timer_status_transition") { status ->
        when (status) {
            TimerStatus.IDLE -> {
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
                                            PurpleGlow
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
                                    .clickable(enabled = timerInput.isNotEmpty()) { viewModel.startTimer() },
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
                            ModernKeypad(
                                buttonSize = 58.dp,
                                spacing = 6.dp,
                                horizontalSpacing = 14.dp,
                                fontSize = 24.sp,
                                actionFontSize = 18.sp,
                                onDigitClicked = { viewModel.appendDigit(it) },
                                onDeleteClicked = { viewModel.deleteDigit() },
                                onClearAllClicked = { viewModel.clearTimerInput() }
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
                        ModernKeypad(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onDigitClicked = { viewModel.appendDigit(it) },
                            onDeleteClicked = { viewModel.deleteDigit() },
                            onClearAllClicked = { viewModel.clearTimerInput() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Giant minimalist START button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (timerInput.isNotEmpty()) {
                                        PurpleGlow
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
                                .clickable(enabled = timerInput.isNotEmpty()) { viewModel.startTimer() },
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
            TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                // Countdown radial displaying Always On look
                val remainingSeconds = (timerRemainingMs + 999) / 1000
                val h = remainingSeconds / 3600
                val m = (remainingSeconds % 3600) / 60
                val s = remainingSeconds % 60
                val readableTime = String.format("%02d:%02d:%02d", h, m, s)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Simple small digital clock displaying at the upper middle side
                    LiveDigitalClock()

                    CircleProgressTimer(
                        remainingMs = timerRemainingMs,
                        totalMs = timerMaxMs,
                        displayString = readableTime,
                        statusText = if (status == TimerStatus.RUNNING) "RUNNING" else "PAUSED",
                        onProgressColor = if (status == TimerStatus.RUNNING) PurpleGlow else Color.White.copy(alpha = 0.15f),
                        glowEnabled = status == TimerStatus.RUNNING
                    )

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
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .width(if (isLandscape) 100.dp else 110.dp)
                                .testTag("timer_reset_btn")
                        )

                        // Pause / Resume Toggle
                        ActionButton(
                            text = if (status == TimerStatus.RUNNING) "Pause" else "Resume",
                            isPrimary = true,
                            accentColor = if (status == TimerStatus.RUNNING) PurpleGlow else PurpleGlow,
                            onClick = {
                                if (status == TimerStatus.RUNNING) viewModel.pauseTimer() else viewModel.resumeTimer()
                            },
                            modifier = Modifier
                                .width(if (isLandscape) 120.dp else 130.dp)
                                .testTag("timer_toggle_btn")
                        )

                        // +1 MIN helper trigger
                        ActionButton(
                            text = "+1m",
                            isPrimary = false,
                            onClick = { viewModel.addOneMinute() },
                            modifier = Modifier
                                .width(if (isLandscape) 90.dp else 100.dp)
                                .testTag("timer_add_1m_btn")
                        )
                    }
                }
            }
            else -> {}
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


// ==========================================
// STOPWATCH TAB CONTENT VIEWS
// ==========================================
@Composable
fun StopwatchTabContent(viewModel: TimerStopwatchViewModel) {
    val elapsedMs by viewModel.stopwatchElapsedMs.collectAsState()
    val stopwatchStatus by viewModel.stopwatchStatus.collectAsState()
    val laps by viewModel.laps.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Formatter centered on centisecond precision
    val totalSeconds = elapsedMs / 1000
    val m = (totalSeconds / 60) % 60
    val s = totalSeconds % 60
    val cc = (elapsedMs / 10) % 100
    val readableTime = String.format("%02d:%02d.%02d", m, s, cc)

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: CircleProgressTimer
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircleProgressTimer(
                    remainingMs = if (stopwatchStatus == StopwatchStatus.RUNNING) 1L else 0L,
                    totalMs = 1L,
                    displayString = readableTime,
                    statusText = when (stopwatchStatus) {
                        StopwatchStatus.IDLE -> "STOPWATCH"
                        StopwatchStatus.RUNNING -> "RUNNING"
                        StopwatchStatus.PAUSED -> "PAUSED"
                    },
                    onProgressColor = PurpleGlow,
                    glowEnabled = stopwatchStatus == StopwatchStatus.RUNNING
                )
            }

            // Right Column: Laps list and Controls below
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lap Times list section (Takes remaining vertical scroll frame)
                Box(
                    modifier = Modifier
                        .weight(1f)
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
                        LazyColumn(
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
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    when (stopwatchStatus) {
                        StopwatchStatus.IDLE -> {
                            ActionButton(
                                text = "Start",
                                isPrimary = true,
                                accentColor = PurpleGlow,
                                onClick = { viewModel.startStopwatch() },
                                modifier = Modifier
                                    .width(140.dp)
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
                                accentColor = NeonPink,
                                onClick = { viewModel.pauseStopwatch() },
                                modifier = Modifier
                                    .width(110.dp)
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
                                accentColor = PurpleGlow,
                                onClick = { viewModel.startStopwatch() },
                                modifier = Modifier
                                    .width(110.dp)
                                    .testTag("stopwatch_resume_btn")
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Display massive counting stopwatch layout
            CircleProgressTimer(
                remainingMs = if (stopwatchStatus == StopwatchStatus.RUNNING) 1L else 0L,
                totalMs = 1L,
                displayString = readableTime,
                statusText = when (stopwatchStatus) {
                    StopwatchStatus.IDLE -> "STOPWATCH"
                    StopwatchStatus.RUNNING -> "RUNNING"
                    StopwatchStatus.PAUSED -> "PAUSED"
                },
                onProgressColor = PurpleGlow,
                glowEnabled = stopwatchStatus == StopwatchStatus.RUNNING
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lap Times list section (Takes remaining vertical scroll frame)
            Box(
                modifier = Modifier
                    .weight(1f)
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
                    LazyColumn(
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

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                                .width(180.dp)
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
                                .width(120.dp)
                                .testTag("stopwatch_lap_btn")
                        )

                        ActionButton(
                            text = "Pause",
                            isPrimary = true,
                            accentColor = NeonPink,
                            onClick = { viewModel.pauseStopwatch() },
                            modifier = Modifier
                                .width(150.dp)
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
                                .width(120.dp)
                                .testTag("stopwatch_reset_btn")
                        )

                        ActionButton(
                            text = "Resume",
                            isPrimary = true,
                            accentColor = PurpleGlow,
                            onClick = { viewModel.startStopwatch() },
                            modifier = Modifier
                                .width(150.dp)
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
    val formattedLap = String.format("%02d:%02d.%02d", lapMin, lapSec, lapCc)

    val overallMin = (lap.totalTimeMs / 60000) % 60
    val overallSec = (lap.totalTimeMs / 1000) % 60
    val overallCc = (lap.totalTimeMs / 10) % 100
    val formattedOverall = String.format("%02d:%02d.%02d", overallMin, overallSec, overallCc)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x08FFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("#%02d", lap.index),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = PurpleGlow,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Delta: $formattedLap",
                fontSize = 13.sp,
                color = Color.LightGray
            )
        }
        
        Text(
            text = formattedOverall,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
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
fun LiveDigitalClock() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
    }

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

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .testTag("live_digital_clock")
    ) {
        Text(
            text = timeString,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(3.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = secondsString,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = PurpleGlow,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = amPm,
                fontSize = 10.sp,
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
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF87))
                            )
                            Text(
                                text = "DOCKABLE NOTCH • 24:18",
                                color = accentColorVal,
                                fontSize = 11.sp,
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
fun DesignTabContent(viewModel: TimerStopwatchViewModel) {
    val config by viewModel.appearanceConfigState.collectAsState()
    val customPresets by viewModel.customPresetNames.collectAsState()
    val wallpaperUri by viewModel.wallpaperUri.collectAsState()
    val overlayMode by viewModel.overlayMode.collectAsState()

    val context = LocalContext.current
    var selectedComponent by remember { mutableStateOf("dockableIsland") } // "dockableIsland", "floatingBubble", "expandedBubblePanel", "timerWidget", "stopwatchWidget", "notificationControls"

    val activeStyle = when (selectedComponent) {
        "dockableIsland" -> config.dockableIsland
        "floatingBubble" -> config.floatingBubble
        "expandedBubblePanel" -> config.expandedBubblePanel
        "timerWidget" -> config.timerWidget
        "stopwatchWidget" -> config.stopwatchWidget
        "notificationControls" -> config.notificationControls
        else -> config.dockableIsland
    }

    val updateStyle = { updated: ComponentStyle ->
        viewModel.updateComponentStyle(selectedComponent, updated)
    }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var savePresetName by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    // Wallpaper / Background Photo Picker launcher
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
        // 1. Wallpaper and Overlay System Mode Selection
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
                    text = "BACKDROP & DOCK SYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleGlow,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            pickMedia.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f))
                    ) {
                        Text("Set Wallpaper Behind UI", color = Color.White, fontSize = 12.sp)
                    }
                    if (wallpaperUri.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.setWallpaperUri("") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3E610115))
                        ) {
                            Text("Reset", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SYSTEM OVERLAY CONTROLLER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(2.dp)
                ) {
                    val overlayLabels = listOf("Dockable Island", "Free Bubble", "Disabled")
                    overlayLabels.forEachIndexed { idx, label ->
                        val isSel = overlayMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (isSel) PurpleGlow else Color.Transparent)
                                .clickable { viewModel.setOverlayMode(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSel) Color.Black else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Active Master Styling Theme presets
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
                    text = "VISUAL CLASS PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleGlow,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Horizontal scrollable flow of master theme designs
                val masterPresets = listOf(
                    "Default Glass", "Amoled Slate", "Neon Cyberpunk", "Emerald Dream", "Material Pink"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    masterPresets.take(3).forEach { p ->
                        Button(
                            onClick = { viewModel.applyPreset(p) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f))
                        ) {
                            Text(p, color = Color.White, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    masterPresets.drop(3).forEach { p ->
                        Button(
                            onClick = { viewModel.applyPreset(p) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f))
                        ) {
                            Text(p, color = Color.White, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 3. User Custom Preset Studio management
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
                    text = "THEME STUDIO STORAGE & PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleGlow,
                    letterSpacing = 1.5.sp
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
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24))
                    ) {
                        Text("Import/Paste Design JSON", color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Ecosystem Config", config.toSerializedString())
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Config JSON copied! Paste anywhere to share design presets.", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Text("Copy Active Config JSON to Clipboard 📋", color = Color.White, fontSize = 12.sp)
                }

                if (customPresets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "YOUR SAVED CUSTOM PRESETS:",
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
                            Text(name, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1.0f).clickable {
                                val prefs = context.getSharedPreferences("appearance_presets", android.content.Context.MODE_PRIVATE)
                                val json = prefs.getString("preset_config_$name", "") ?: ""
                                if (json.isNotEmpty()) {
                                    viewModel.updateAppearanceConfig(AppAppearanceConfig.fromSerializedString(json))
                                    android.widget.Toast.makeText(context, "$name Applied!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                            Button(
                                onClick = { viewModel.deleteCustomPreset(name) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x6A9D1C2A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Delete", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. APPEARANCE STUDIO (COSMIC STYLE EDITORS PER SURFACE)
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
                    text = "UNIFIED APPEARANCE STUDIO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleGlow,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure parameters individually per system surface layer below:",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(14.dp))
                // Surface Layer Customizer selector buttons
                val surfaceComponents = listOf(
                    "dockableIsland" to "🏝️ Dock Island",
                    "floatingBubble" to "🫧 Collapsed Bubble",
                    "expandedBubblePanel" to "📋 Expanded Bubble",
                    "timerWidget" to "⏱️ Timer Widget",
                    "stopwatchWidget" to "🏁 SW Widget",
                    "notificationControls" to "🔔 Notification"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    surfaceComponents.take(3).forEach { (id, label) ->
                        val isSelected = selectedComponent == id
                        Button(
                            onClick = { selectedComponent = id },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.04f))
                        ) {
                            Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    surfaceComponents.drop(3).forEach { (id, label) ->
                        val isSelected = selectedComponent == id
                        Button(
                            onClick = { selectedComponent = id },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PurpleGlow else Color.White.copy(alpha = 0.04f))
                        ) {
                            Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Live preview render card
                LivePreviewCard(componentName = selectedComponent, style = activeStyle)

                Spacer(modifier = Modifier.height(16.dp))
                // Color hex controllers with quick selection palettes
                Text(
                    text = "BACKGROUND HEX CODE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Predefined hex code colors (AMOLED Black, Cyber Dark Violet, Deep Slate, Lavender Glass, Hot Pink, Mint Green)
                    val bgPresets = listOf("#000000", "#0B0214", "#1C1C1D", "#FFFFFF", "#FF2A6D", "#00FF87")
                    bgPresets.forEach { colorStr ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(android.graphics.Color.parseColor(colorStr)))
                                .border(
                                    width = if (activeStyle.bgColor == colorStr) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    updateStyle(activeStyle.copy(bgColor = colorStr))
                                }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1.0f))
                    // Manual background string input field
                    OutlinedTextField(
                        value = activeStyle.bgColor,
                        onValueChange = { updateStyle(activeStyle.copy(bgColor = it)) },
                        modifier = Modifier.width(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PurpleGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Dual-Gradient support toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ENABLE DUAL-GRADIENT MIX",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Blends background using 2 linear hex endpoints",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = activeStyle.gradientEnabled,
                        onCheckedChange = { updateStyle(activeStyle.copy(gradientEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleGlow)
                    )
                }

                if (activeStyle.gradientEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
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

                Spacer(modifier = Modifier.height(14.dp))
                // TEXT and ACCENT row selectors
                Text(
                    text = "TEXT AND ACCENT COLORS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = activeStyle.textColor,
                        onValueChange = { updateStyle(activeStyle.copy(textColor = it)) },
                        label = { Text("Main Text Color", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                    OutlinedTextField(
                        value = activeStyle.accentColor,
                        onValueChange = { updateStyle(activeStyle.copy(accentColor = it)) },
                        label = { Text("Accent Neon Color", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                // BORDERS & OUTLINE TINTS
                Text(
                    text = "BORDER OUTLINES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = activeStyle.borderColor,
                        onValueChange = { updateStyle(activeStyle.copy(borderColor = it)) },
                        label = { Text("Border Color Hex", fontSize = 9.sp) },
                        modifier = Modifier.weight(1.3f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thickness", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                        Slider(
                            value = activeStyle.borderThickness,
                            onValueChange = { updateStyle(activeStyle.copy(borderThickness = it)) },
                            valueRange = 0f..4f,
                            colors = SliderDefaults.colors(thumbColor = PurpleGlow)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Master customization sliders (Opacity, Corner, Glow strength, Blur, Shadow, Neon)
                CustomSettingSlider(
                    title = "SURFACE DECORATION OPACITY",
                    value = activeStyle.opacity * 100f,
                    valueRange = 0f..100f,
                    displaySuffix = "%",
                    onValueChange = { updateStyle(activeStyle.copy(opacity = it / 100f)) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                CustomSettingSlider(
                    title = "GLASS ACRYLIC BLUR",
                    value = activeStyle.blur,
                    valueRange = 0f..50f,
                    displaySuffix = "px",
                    onValueChange = { updateStyle(activeStyle.copy(blur = it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                CustomSettingSlider(
                    title = "GLASS CORNER BOUND RADIUS",
                    value = activeStyle.cornerRadius.toFloat(),
                    valueRange = 8f..36f,
                    displaySuffix = "dp",
                    onValueChange = { updateStyle(activeStyle.copy(cornerRadius = it.toInt())) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                CustomSettingSlider(
                    title = "NEON GLOW INTEGRATED STRENGTH",
                    value = activeStyle.glowStrength * 100f,
                    valueRange = 0f..100f,
                    displaySuffix = "%",
                    onValueChange = { updateStyle(activeStyle.copy(glowStrength = it / 100f)) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                CustomSettingSlider(
                    title = "FROSTED SHADOW DEPTH",
                    value = activeStyle.shadowIntensity,
                    valueRange = 0f..20f,
                    displaySuffix = "dp",
                    onValueChange = { updateStyle(activeStyle.copy(shadowIntensity = it)) }
                )
            }
        }
    }

    // dialogs for loading and pasting configurations
    if (showSavePresetDialog) {
        Dialog(onDismissRequest = { showSavePresetDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Save Custom Theme Preset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = savePresetName,
                        onValueChange = { savePresetName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        label = { Text("Preset name (e.g., Cyberpunk Dark)", fontSize = 11.sp) }
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
                    Text("Paste Theme Configuration JSON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        label = { Text("Paste configuration string here", fontSize = 10.sp) }
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
                                        android.widget.Toast.makeText(context, "Preset imported and applied in real time!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Invalid Format! Make sure you copied the correct JSON string.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow)
                        ) {
                            Text("Import Preset", color = Color.Black, fontWeight = FontWeight.Bold)
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

