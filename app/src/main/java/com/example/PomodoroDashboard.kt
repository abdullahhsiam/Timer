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

    val sessionLabel = when (focusState) {
        FocusModeState.FOCUS -> {
            val num = (completedFocus % 4) + 1
            "Focus #$num"
        }
        FocusModeState.BREAK -> {
            val num = completedFocus % 4
            if (num == 0) "Long Break" else "Break #$num"
        }
        FocusModeState.OFF -> {
            "Focus #1"
        }
    }

    val activeColor = if (focusState == FocusModeState.BREAK) CyanGlow else GlowGreen

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Visual Centerpiece Selection Box (Circle / Flip view)
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp),
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
                if (mode == 1) {
                    // FLIP MODE COUNTER
                    val totalSecs = actualRemainingMs / 1000
                    val m = totalSecs / 60
                    val s = totalSecs % 60
                    val timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            FlipClockDisplay(
                                timeString = timeString,
                                width = 52.dp,
                                height = 76.dp,
                                textSize = 50f
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val pulse = rememberInfiniteTransition(label = "badge_pulse_flip")
                            val opacity by pulse.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_opacity"
                            )
                            
                            Text(
                                text = sessionLabel.uppercase(),
                                color = activeColor.copy(alpha = if (pomoStatus == PomodoroStatus.RUNNING) opacity else 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    // CIRCLE MODE TIMER: Reduced diameter by ~27% from 220dp to 160dp
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (actualDurationMs > 0) actualRemainingMs.toFloat() / actualDurationMs.toFloat() else 0f
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            val strokeWidthPx = 5.dp.toPx()
                            // Track
                            drawArc(
                                color = Color.White.copy(alpha = 0.04f),
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
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (focusState == FocusModeState.OFF && pomoStatus == PomodoroStatus.IDLE) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = "Zen Status",
                                    tint = PurpleGlow.copy(alpha = 0.7f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "READY",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            } else {
                                val totalSecs = actualRemainingMs / 1000
                                val m = totalSecs / 60
                                val s = totalSecs % 60
                                val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                                
                                Text(
                                    text = formattedTime,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Light,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                
                                val pulse = rememberInfiniteTransition(label = "badge_pulse")
                                val opacity by pulse.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse_opacity"
                                )

                                Text(
                                    text = sessionLabel.uppercase(),
                                    color = activeColor.copy(alpha = if (pomoStatus == PomodoroStatus.RUNNING) opacity else 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Primary Compact Control Actions Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Config Button
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

        // Premium Metrics Stats Panel
        val totalFocusHours = totalFocusMs / 3600000.0
        val focusHoursFormatted = String.format(java.util.Locale.getDefault(), "%.1fh", totalFocusHours)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(2.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POMODORO STATE MONITOR",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    
                    Text(
                        text = "Config",
                        color = PurpleGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showConfigDialog = true }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        StatLabelValue(label = "Today's Focus Time", value = formatMinutes(dailyFocusMs))
                        StatLabelValue(label = "Focus Blocks Completed", value = "$completedFocus sessions")
                        StatLabelValue(label = "Manual Break Count", value = "$manualBreaks sessions")
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        StatLabelValue(label = "Today's Break Time", value = formatMinutes(dailyBreakMs), alignEnd = true)
                        StatLabelValue(label = "Break Blocks Completed", value = "$completedBreak sessions", alignEnd = true)
                        StatLabelValue(label = "Total Focus Hours", value = focusHoursFormatted, alignEnd = true)
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
fun StatLabelValue(
    label: String,
    value: String,
    alignEnd: Boolean = false
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
