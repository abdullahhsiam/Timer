package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PurpleGlow

@Composable
fun FocusModeSelector(
    currentState: FocusModeState,
    onStateSelected: (FocusModeState) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(19.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val states = listOf(FocusModeState.OFF, FocusModeState.FOCUS, FocusModeState.BREAK)
        states.forEach { state ->
            val isSelected = currentState == state
            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    when (state) {
                        FocusModeState.FOCUS -> GlowGreen.copy(alpha = 0.2f)
                        FocusModeState.BREAK -> CyanGlow.copy(alpha = 0.2f)
                        FocusModeState.OFF -> Color.White.copy(alpha = 0.12f)
                    }
                } else Color.Transparent,
                label = "pomo_selector_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    when (state) {
                        FocusModeState.FOCUS -> GlowGreen
                        FocusModeState.BREAK -> CyanGlow
                        FocusModeState.OFF -> Color.White
                    }
                } else Color.White.copy(alpha = 0.5f),
                label = "pomo_selector_text"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(17.dp))
                    .background(animatedBgColor)
                    .clickable { onStateSelected(state) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (state) {
                        FocusModeState.OFF -> "OFF"
                        FocusModeState.FOCUS -> "FOCUS"
                        FocusModeState.BREAK -> "BREAK"
                    },
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PomodoroDashboard(
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
    val currentSessionNum by viewModel.currentSessionNumber.collectAsState()
    
    val dailyFocusMs by viewModel.dailyTotalFocusTimeMs.collectAsState()
    val dailyCompletedFocus by viewModel.dailyCompletedFocusSessions.collectAsState()
    
    val focusMin by viewModel.focusDefaultMin.collectAsState()
    val shortMin by viewModel.shortBreakDefaultMin.collectAsState()
    val longMin by viewModel.longBreakDefaultMin.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Focus Mode Controller Selector
        FocusModeSelector(
            currentState = focusState,
            onStateSelected = { state -> viewModel.setFocusModeState(state) },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(bottom = 12.dp)
        )

        AnimatedVisibility(
            visible = focusState != FocusModeState.OFF,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val activeColor = if (focusState == FocusModeState.FOCUS) GlowGreen else CyanGlow

                // 2. Countdown Display (Digital style)
                val totalSecs = remainingMs / 1000
                val m = totalSecs / 60
                val s = totalSecs % 60
                val timeString = String.format("%02d:%02d", m, s)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = timeString,
                        color = activeColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
                    val badgeAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "badge_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(activeColor.copy(alpha = 0.15f * if (pomoStatus == PomodoroStatus.RUNNING) badgeAlpha else 1.0f))
                            .border(1.dp, activeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (focusState == FocusModeState.FOCUS) "FOCUS #$currentSessionNum" else "BREAK",
                            color = activeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 3. Compact Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    // Start / Pause
                    IconButton(
                        onClick = {
                            if (pomoStatus == PomodoroStatus.RUNNING) {
                                viewModel.pausePomodoro()
                            } else {
                                viewModel.startPomodoro()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeColor.copy(alpha = 0.15f))
                            .border(1.dp, activeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = if (pomoStatus == PomodoroStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (pomoStatus == PomodoroStatus.RUNNING) "Pause" else "Resume",
                            tint = activeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = { viewModel.resetPomodoro() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Action: Manual Break or Skip Break based on state
                    if (focusState == FocusModeState.FOCUS) {
                        Button(
                            onClick = { viewModel.startBreakManually() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanGlow.copy(alpha = 0.15f),
                                contentColor = CyanGlow
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Break", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.skipBreak() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowGreen.copy(alpha = 0.15f),
                                contentColor = GlowGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlowGreen.copy(alpha = 0.3f)),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skip Break", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Configuration Button
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Pomodoro",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Stats Dashboard Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "POMODORO COMPANION",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                StatLabelValue(label = "Today's Focus", value = formatMinutes(dailyFocusMs) + " ($dailyCompletedFocus sessions)")
                                Spacer(modifier = Modifier.height(4.dp))
                                StatLabelValue(label = "Cycles Completed", value = "${completedFocus / 4} cycles")
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                StatLabelValue(label = "Completed Focus", value = "$completedFocus sessions", alignEnd = true)
                                Spacer(modifier = Modifier.height(4.dp))
                                StatLabelValue(label = "Manual Breaks", value = "$manualBreaks sessions", alignEnd = true)
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
    val currentSessionNum by viewModel.currentSessionNumber.collectAsState()

    val dailyFocusMs by viewModel.dailyTotalFocusTimeMs.collectAsState()
    val dailyCompletedFocus by viewModel.dailyCompletedFocusSessions.collectAsState()

    val focusMin by viewModel.focusDefaultMin.collectAsState()
    val shortMin by viewModel.shortBreakDefaultMin.collectAsState()
    val longMin by viewModel.longBreakDefaultMin.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Heading Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "FOCUS CYCLES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "Zen Pomodoro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        // Segmented state buttons (OFF, FOCUS, BREAK)
        FocusModeSelector(
            currentState = focusState,
            onStateSelected = { state -> viewModel.setFocusModeState(state) },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 4.dp)
        )

        val activeColor = when (focusState) {
            FocusModeState.FOCUS -> GlowGreen
            FocusModeState.BREAK -> CyanGlow
            else -> Color.White.copy(alpha = 0.3f)
        }

        // Circular Display or Empty Zen Space
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.02f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Draw active arc line inside
            val progress = if (durationMs > 0) remainingMs.toFloat() / durationMs.toFloat() else 0f
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val strokeWidthPx = 6.dp.toPx()
                // Background Track
                drawArc(
                    color = Color.White.copy(alpha = 0.04f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                if (focusState != FocusModeState.OFF) {
                    // Running Arc
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }

            // Central time text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (focusState == FocusModeState.OFF) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Zen Status",
                        tint = PurpleGlow.copy(alpha = 0.7f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "READY",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                } else {
                    val totalSecs = remainingMs / 1000
                    val m = totalSecs / 60
                    val s = totalSecs % 60
                    val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                    
                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 42.sp,
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
                        text = if (focusState == FocusModeState.FOCUS) "FOCUS #$currentSessionNum" else "REST BREAK",
                        color = activeColor.copy(alpha = if (pomoStatus == PomodoroStatus.RUNNING) opacity else 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Active Buttons Controls
        if (focusState != FocusModeState.OFF) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Config Button
                IconButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Durations",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Primary Start / Pause
                val runIcon = if (pomoStatus == PomodoroStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow
                Button(
                    onClick = {
                        if (pomoStatus == PomodoroStatus.RUNNING) {
                            viewModel.pausePomodoro()
                        } else {
                            viewModel.startPomodoro()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 120.dp)
                ) {
                    Icon(imageVector = runIcon, contentDescription = "Hold control", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pomoStatus == PomodoroStatus.RUNNING) "Pause" else "Resume",
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
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // OFF State Options presets to easily trigger Pomodoro sessions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "QUICK START PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Focus block standard (25m)
                    PresetClickCard(
                        title = "Classic Focus",
                        details = "$focusMin min study block",
                        icon = Icons.Default.Spa,
                        badgeColor = GlowGreen,
                        modifier = Modifier.weight(1.5f),
                        onClick = {
                            viewModel.setFocusModeState(FocusModeState.FOCUS)
                            viewModel.startPomodoro() // Auto start
                        }
                    )
                    
                    // Relax rest preset (5m)
                    PresetClickCard(
                        title = "Short Break",
                        details = "$shortMin min breath time",
                        icon = Icons.Default.Coffee,
                        badgeColor = CyanGlow,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.setFocusModeState(FocusModeState.BREAK)
                            viewModel.startPomodoro() // Auto start
                        }
                    )
                }
            }
        }

        // Stats Companion Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOCUS STATISTICS COMPANION",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    
                    // Trigger durations settings directly when off
                    if (focusState == FocusModeState.OFF) {
                        Text(
                            text = "Durations Setting",
                            color = PurpleGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showConfigDialog = true }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        StatLabelValue(label = "Today's Focus", value = formatMinutes(dailyFocusMs) + " ($dailyCompletedFocus sessions)")
                        Spacer(modifier = Modifier.height(6.dp))
                        StatLabelValue(label = "Completed Session Count", value = "$completedFocus focus blocks")
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        StatLabelValue(label = "Cycles Completed", value = "${completedFocus / 4} cycles", alignEnd = true)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatLabelValue(label = "Total Active Hours", value = String.format(java.util.Locale.getDefault(), "%.1fh", totalFocusMs / 3600000.0), alignEnd = true)
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
fun PresetClickCard(
    title: String,
    details: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = details, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}
