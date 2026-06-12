package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.PurpleGlow

@Composable
fun PomodoroTabContent(
    viewModel: TimerStopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val focusState by viewModel.focusModeState.collectAsState()
    val pomoStatus by viewModel.pomodoroStatus.collectAsState()
    val remainingMs by viewModel.pomodoroRemainingMs.collectAsState()
    val durationMs by viewModel.pomodoroDurationMs.collectAsState()

    val completedFocus by viewModel.completedFocusSessions.collectAsState()
    val completedBreak by viewModel.completedBreakSessions.collectAsState()
    val manualBreaks by viewModel.manualBreaksCount.collectAsState()
    val totalFocusMs by viewModel.totalFocusTimeMs.collectAsState()

    val dailyFocusMs by viewModel.dailyTotalFocusTimeMs.collectAsState()
    val dailyCompletedFocus by viewModel.dailyCompletedFocusSessions.collectAsState()
    val dailyBreakMs by viewModel.dailyTotalBreakTimeMs.collectAsState()

    val focusMin by viewModel.focusDefaultMin.collectAsState()
    val shortMin by viewModel.shortBreakDefaultMin.collectAsState()
    val longMin by viewModel.longBreakDefaultMin.collectAsState()

    val activeVisualMode by viewModel.activeVisualMode.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    val actualRemainingMs = if (remainingMs == 0L && focusState == FocusModeState.OFF) {
        focusMin * 60_000L
    } else {
        remainingMs
    }
    val actualDurationMs = if (durationMs == 0L && focusState == FocusModeState.OFF) {
        focusMin * 60_000L
    } else {
        durationMs
    }

    val activeColor = if (focusState == FocusModeState.BREAK) CyanGlow else GlowGreen

    // Dynamic scale and spacing rules
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600 && configuration.screenHeightDp >= 600
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val isCompactWidth = configuration.screenWidthDp < 480

    // Avoid overlap with top bar sliding pill switcher
    val topBarPadding = if (isCompactWidth && isPortrait) {
        155.dp
    } else {
        105.dp
    }

    val themeSpacing = if (isTablet) 16.dp else 10.dp
    val ringSize = if (isTablet) 180.dp else 145.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(themeSpacing)
    ) {
        // Space above matching the fixed header height to prevent overlapping layers
        Spacer(modifier = Modifier.height(topBarPadding))

        // Main Visual Centerpiece Selection Box (Circle / Flip view)
        Box(
            modifier = Modifier
                .padding(vertical = if (isTablet) 12.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = activeVisualMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) + 
                     scaleIn(initialScale = 0.92f, animationSpec = tween(220, easing = LinearOutSlowInEasing))) togetherWith 
                    (fadeOut(animationSpec = tween(150)) + 
                     scaleOut(targetScale = 1.05f, animationSpec = tween(150)))
                },
                label = "pomo_visual_mode_transition"
            ) { mode ->
                // Shared Status Definitions
                val primarySessionText = when (pomoStatus) {
                    PomodoroStatus.IDLE -> "Ready to Start"
                    else -> when (focusState) {
                        FocusModeState.FOCUS -> "Focused Session"
                        FocusModeState.BREAK -> {
                            val isLong = completedFocus > 0 && completedFocus % 4 == 0
                            if (isLong) "Long Break" else "Short Break"
                        }
                        FocusModeState.OFF -> "Ready to Start"
                    }
                }

                val cycleProgressText = when (focusState) {
                    FocusModeState.FOCUS -> {
                        val num = (completedFocus % 4) + 1
                        "Focus #$num of 4"
                    }
                    FocusModeState.BREAK -> {
                        val isLong = completedFocus > 0 && completedFocus % 4 == 0
                        val num = completedFocus % 4
                        if (isLong) "Cycle Complete" else "Break #$num of 3"
                    }
                    FocusModeState.OFF -> "Focus #1 of 4"
                }

                if (mode == 1) {
                    // FLIP MODE COUNTER
                    val totalSecs = actualRemainingMs / 1000
                    val m = totalSecs / 60
                    val s = totalSecs % 60
                    val timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            FlipClockDisplay(
                                timeString = timeString,
                                width = 48.dp,
                                height = 68.dp,
                                textSize = 42f
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val pulse = rememberInfiniteTransition(label = "badge_pulse_flip")
                            val opacity by pulse.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_opacity"
                            )
                            
                            Text(
                                text = primarySessionText.uppercase(),
                                color = activeColor.copy(alpha = if (pomoStatus == PomodoroStatus.RUNNING) opacity else 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            
                            Text(
                                text = cycleProgressText,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                } else {
                    // CIRCLE MODE TIMER: Centered, reduced by 15-20% (145.dp)
                    Box(
                        modifier = Modifier
                            .size(ringSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.015f))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (actualDurationMs > 0) actualRemainingMs.toFloat() / actualDurationMs.toFloat() else 0f
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            val strokeWidthPx = 4.5.dp.toPx()
                            // Track
                            drawArc(
                                color = Color.White.copy(alpha = 0.03f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            // Animated Running Arc
                            drawArc(
                                color = activeColor,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            val totalSecs = actualRemainingMs / 1000
                            val m = totalSecs / 60
                            val s = totalSecs % 60
                            val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                            
                            // 1. COUNTDOWN TIME
                            Text(
                                text = formattedTime,
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // 2. SESSION STATE TYPE (PULSING)
                            val pulse = rememberInfiniteTransition(label = "badge_pulse")
                            val opacity by pulse.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_opacity"
                            )

                            Text(
                                text = primarySessionText.uppercase(),
                                color = activeColor.copy(alpha = if (pomoStatus == PomodoroStatus.RUNNING) opacity else 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            
                            Spacer(modifier = Modifier.height(1.dp))

                            // 3. CYCLE PROGRESS SPECIFICS
                            Text(
                                text = cycleProgressText,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Simplified Control Actions Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Config Button (Always visible)
            IconButton(
                onClick = { showConfigDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configure Pomodoro",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Start / Pause Primary Button
            val isRunning = pomoStatus == PomodoroStatus.RUNNING
            val actionIcon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow
            val actionText = if (isRunning) "Pause" else if (pomoStatus == PomodoroStatus.PAUSED) "Resume" else "Start"
            
            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.pausePomodoro()
                    } else {
                        viewModel.startPomodoro()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .height(44.dp)
                    .widthIn(min = 120.dp)
            ) {
                Icon(imageVector = actionIcon, contentDescription = actionText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = actionText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Show reset and manual coffee breaks action only if the Pomodoro session has started / is active
            if (pomoStatus != PomodoroStatus.IDLE) {
                // Reset Button
                IconButton(
                    onClick = { viewModel.resetPomodoro() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Action: Manual Break or Skip Break based on state
                if (focusState == FocusModeState.FOCUS || focusState == FocusModeState.OFF) {
                    IconButton(
                        onClick = { viewModel.startBreakManually() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyanGlow.copy(alpha = 0.15f))
                            .border(1.dp, CyanGlow.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = "Start Break",
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.skipBreak() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlowGreen.copy(alpha = 0.15f))
                            .border(1.dp, GlowGreen.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Break",
                            tint = GlowGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Smart Empty State Collapse logic
        val totalFocusHours = totalFocusMs / 3600000.0
        val focusHoursFormatted = String.format(java.util.Locale.getDefault(), "%.1fh", totalFocusHours)
        
        val isStatsEmpty = (dailyFocusMs == 0L && completedFocus == 0 && manualBreaks == 0 && dailyBreakMs == 0L && completedBreak == 0)

        AnimatedContent(
            targetState = isStatsEmpty,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "pomo_stats_display_transition"
        ) { empty ->
            if (empty) {
                // Compact Placeholder Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .clickable { showConfigDialog = true }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = PurpleGlow.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "No Pomodoro sessions recorded today",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Premium Insights Refactored 2-Column Grid Dashboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "POMODORO INSIGHTS",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            
                            Text(
                                text = "Target Config",
                                color = PurpleGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showConfigDialog = true }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }

                        // Compact Responsive 2-Column Dashboard Grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                StatGridCell(
                                    label = "Focus Time",
                                    value = formatMinutes(dailyFocusMs),
                                    icon = Icons.Default.Spa,
                                    iconColor = GlowGreen
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                StatGridCell(
                                    label = "Break Time",
                                    value = formatMinutes(dailyBreakMs),
                                    icon = Icons.Default.Coffee,
                                    iconColor = CyanGlow
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                StatGridCell(
                                    label = "Focus Progress",
                                    value = "$completedFocus sessions",
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = GlowGreen
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                StatGridCell(
                                    label = "Break Progress",
                                    value = "$completedBreak sessions",
                                    icon = Icons.Default.Coffee,
                                    iconColor = CyanGlow
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                StatGridCell(
                                    label = "Manual Breaks",
                                    value = "$manualBreaks sessions",
                                    icon = Icons.Default.SkipNext,
                                    iconColor = CyanGlow
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                StatGridCell(
                                    label = "Total Focus Hours",
                                    value = focusHoursFormatted,
                                    icon = Icons.Default.AccessTime,
                                    iconColor = PurpleGlow
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfigDialog) {
        PomodoroConfigDialog(
            focusVal = focusMin,
            shortVal = shortMin,
            longVal = longMin,
            onSave = { f, s, l ->
                viewModel.setPomodoroDurations(f, s, l)
                showConfigDialog = false
            },
            onDismiss = { showConfigDialog = false }
        )
    }
}

@Composable
fun StatGridCell(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun formatMinutes(ms: Long): String {
    val mins = ms / 60_000L
    return "${mins}m"
}

@Composable
fun PomodoroConfigDialog(
    focusVal: Int,
    shortVal: Int,
    longVal: Int,
    onSave: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var focus by remember { mutableIntStateOf(focusVal) }
    var short by remember { mutableIntStateOf(shortVal) }
    var long by remember { mutableIntStateOf(longVal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Configure Durations",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DurationSelector(title = "Focus Time", value = focus, min = 5, max = 120, unit = "min", onValueChange = { focus = it })
                DurationSelector(title = "Short Break", value = short, min = 1, max = 30, unit = "min", onValueChange = { short = it })
                DurationSelector(title = "Long Break", value = long, min = 5, max = 60, unit = "min", onValueChange = { long = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(focus, short, long) },
                colors = ButtonDefaults.buttonColors(containerColor = GlowGreen, contentColor = Color.Black)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF141419),
        textContentColor = Color.White
    )
}

@Composable
fun DurationSelector(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Text(text = title, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (value > min) Color.White else Color.White.copy(alpha = 0.2f))
            }
            
            Text(text = "$value $unit", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            
            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = if (value < max) Color.White else Color.White.copy(alpha = 0.2f))
            }
        }
    }
}
