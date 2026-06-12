package com.example

import com.example.persistence.PomodoroDailySummary
import com.example.persistence.PomodoroSessionLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
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

    val streaks by viewModel.streaksState.collectAsState(initial = PomodoroStreaks(0, 0))
    val dailySummaries by viewModel.allDailySummaries.collectAsState(initial = emptyList())
    val sessionLogs by viewModel.allSessionLogs.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showConfigDialog by remember { mutableStateOf(false) }
    val showFullHistory by viewModel.showPomoHistory.collectAsState()
    var expandedDates by remember { mutableStateOf(setOf<String>()) }

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

    // Dynamic responsive size classes with explicit min/max bounds
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val screenWidth = configuration.screenWidthDp
    val isCompactPhone = screenWidth < 600
    val isMediumDevice = screenWidth in 600..840
    val isLargeTablet = screenWidth > 840
    val isTablet = screenWidth >= 600

    // Prevent scrolling overlays behind sticky top-bar
    val topBarPadding = if (isCompactPhone && isPortrait) {
        155.dp
    } else {
        105.dp
    }

    // Proportional spacings
    val themeSpacing = when {
        isCompactPhone -> 8.dp
        isMediumDevice -> 12.dp
        else -> 18.dp
    }

    // Precise physical bounding for dynamic circular timer
    val ringSize = when {
        isCompactPhone -> 130.dp
        isMediumDevice -> 160.dp
        else -> 195.dp
    }

    val ctrlBtnSize = if (isCompactPhone) 36.dp else 44.dp
    val primaryBtnHeight = if (isCompactPhone) 36.dp else 44.dp
    val primaryBtnMinWidth = if (isCompactPhone) 100.dp else 120.dp
    val ctrlIconSize = if (isCompactPhone) 15.dp else 18.dp
    val ctrlFontSize = if (isCompactPhone) 11.sp else 13.sp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .fadingEdgeMask(
                    topFadeHeight = topBarPadding + 15.dp,
                    bottomFadeHeight = 100.dp
                )
                .padding(horizontal = if (isLargeTablet) 24.dp else 16.dp, vertical = 2.dp)
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
                    // CIRCLE MODE TIMER: Centered
                    Box(
                        modifier = Modifier
                            .size(ringSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.015f))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val totalSecs = actualRemainingMs / 1000
                            val m = totalSecs / 60
                            val s = totalSecs % 60
                            val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
                            
                            val isRunningMode = pomoStatus == PomodoroStatus.RUNNING

                            CircleProgressTimer(
                                remainingMs = actualRemainingMs,
                                totalMs = if (actualDurationMs > 0) actualDurationMs else 1L,
                                displayString = formattedTime,
                                statusText = primarySessionText,
                                onProgressColor = activeColor,
                                glowEnabled = isRunningMode,
                                sizeFraction = if (isCompactPhone) 0.55f else 0.70f
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = cycleProgressText.uppercase(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }

        // Simplified Control Actions Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 10.dp else 14.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Config Button (Always visible)
            IconButton(
                onClick = { showConfigDialog = true },
                modifier = Modifier
                    .size(ctrlBtnSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configure Pomodoro",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(ctrlIconSize)
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
                    .height(primaryBtnHeight)
                    .widthIn(min = primaryBtnMinWidth)
            ) {
                Icon(imageVector = actionIcon, contentDescription = actionText, modifier = Modifier.size(ctrlIconSize))
                Spacer(modifier = Modifier.width(if (isCompactPhone) 4.dp else 6.dp))
                Text(
                    text = actionText,
                    fontWeight = FontWeight.Bold,
                    fontSize = ctrlFontSize
                )
            }

            // Show reset and manual coffee breaks action only if the Pomodoro session has started / is active
            if (pomoStatus != PomodoroStatus.IDLE) {
                // Reset Button
                IconButton(
                    onClick = { viewModel.resetPomodoro() },
                    modifier = Modifier
                        .size(ctrlBtnSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(ctrlIconSize)
                    )
                }

                // Action: Manual Break or Skip Break based on state
                if (focusState == FocusModeState.FOCUS || focusState == FocusModeState.OFF) {
                    IconButton(
                        onClick = { viewModel.startBreakManually() },
                        modifier = Modifier
                            .size(ctrlBtnSize)
                            .clip(CircleShape)
                            .background(CyanGlow.copy(alpha = 0.15f))
                            .border(1.dp, CyanGlow.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = "Start Break",
                            tint = CyanGlow,
                            modifier = Modifier.size(ctrlIconSize)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.skipBreak() },
                        modifier = Modifier
                            .size(ctrlBtnSize)
                            .clip(CircleShape)
                            .background(GlowGreen.copy(alpha = 0.15f))
                            .border(1.dp, GlowGreen.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Break",
                            tint = GlowGreen,
                            modifier = Modifier.size(ctrlIconSize)
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

        // Space between Insights and History
        Spacer(modifier = Modifier.height(6.dp))

        // POMODORO HISTORY AND STREAKS PANEL
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                .clickable {
                    viewModel.setPomoHistoryVisibility(true)
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with Streak Indicators & PDF Export Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "POMODORO HISTORY",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        // Streak Badges row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            // Current Streak
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Current Streak",
                                    tint = Color(0xFFFF5722), // Flame color
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${streaks.currentStreak}d streak",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            // Divider Dot
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 10.sp
                            )

                            // Best Streak
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Best Streak",
                                    tint = Color(0xFFFFC107), // Gold star
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "best ${streaks.bestStreak}d",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Share / Export PDF icon button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                PomodoroPdfExporter.exportToPdfAndShare(context, dailySummaries, sessionLogs)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleGlow.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export PDF Report",
                            tint = PurpleGlow,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (dailySummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No focus records logged yet",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Display up to 30 elements
                        dailySummaries.take(30).forEach { day ->
                            val isExpanded = expandedDates.contains(day.date)
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = if (isExpanded) 0.025f else 0.012f))
                                    .border(1.dp, Color.White.copy(alpha = if (isExpanded) 0.05f else 0.03f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        expandedDates = if (isExpanded) expandedDates - day.date else expandedDates + day.date
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                // Day Entry Summary Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Expand details",
                                            tint = Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = formatHistoryDate(day.date),
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    
                                    // Focus summary stats
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Focus: ${formatMinutes(day.focusTimeMs)}",
                                            color = GlowGreen.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "•",
                                            color = Color.White.copy(alpha = 0.15f),
                                            fontSize = 9.sp
                                        )
                                        Text(
                                            text = "Breaks: ${formatMinutes(day.breakTimeMs)}",
                                            color = CyanGlow.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Expanded Chronicle detail panel
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)),
                                    exit = shrinkVertically(animationSpec = tween(120)) + fadeOut(animationSpec = tween(120))
                                ) {
                                    val dayLogs = sessionLogs.filter { it.date == day.date }.sortedBy { it.startTime }
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, start = 20.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Header label
                                        Text(
                                            text = "SESSION TIMESTAMPS",
                                            color = Color.White.copy(alpha = 0.25f),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )

                                        if (dayLogs.isEmpty()) {
                                            Text(
                                                text = "No detailed session periods recorded for this date",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        } else {
                                            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                            
                                            dayLogs.forEach { log ->
                                                val startStr = sdfTime.format(Date(log.startTime))
                                                val endStr = sdfTime.format(Date(log.endTime))
                                                val durationStr = formatMinutes(log.durationMs)
                                                
                                                val labelText = when (log.sessionType) {
                                                    "FOCUS" -> "Focus Session"
                                                    "BREAK" -> "Standard Break"
                                                    "MANUAL_BREAK" -> "Manual Break"
                                                    else -> log.sessionType
                                                }
                                                val badgeColor = when (log.sessionType) {
                                                    "FOCUS" -> GlowGreen
                                                    "BREAK" -> CyanGlow
                                                    else -> PurpleGlow
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(badgeColor)
                                                        )
                                                        Text(
                                                            text = "$startStr–$endStr",
                                                            color = Color.White.copy(alpha = 0.65f),
                                                            fontSize = 10.sp,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        text = "$labelText ($durationStr)",
                                                        color = Color.White.copy(alpha = 0.5f),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Divider(color = Color.White.copy(alpha = 0.05f))
                                        Spacer(modifier = Modifier.height(1.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Completed Focus: ${day.focusSessionsCompleted}",
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = "Completed Breaks: ${day.breakSessionsCompleted}",
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = "Manual Breaks: ${day.manualBreakCount}",
                                                color = Color.White.copy(alpha = 0.45f),
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
        }

        // Adequate bottom spacing so that no entries are hidden behind the bottom settings area
        Spacer(modifier = Modifier.height(130.dp))
    }

        // Floating Full History Overlay Screen with smooth transitions
        AnimatedVisibility(
            visible = showFullHistory,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullHistoryScreen(
                dailySummaries = dailySummaries,
                sessionLogs = sessionLogs,
                streaks = streaks,
                onBack = { viewModel.setPomoHistoryVisibility(false) },
                onExportPdf = {
                    coroutineScope.launch {
                        PomodoroPdfExporter.exportToPdfAndShare(context, dailySummaries, sessionLogs)
                    }
                }
            )
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    val boxSize = if (isCompact) 24.dp else 28.dp
    val iconSize = if (isCompact) 12.dp else 14.dp
    val labelSize = if (isCompact) 9.sp else 10.sp
    val valueSize = if (isCompact) 11.sp else 12.sp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 2.dp else 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = labelSize,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = valueSize,
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

fun formatHistoryDate(dateStr: String): String {
    val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    return try {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        when (dateStr) {
            todayStr -> "Today"
            yesterdayStr -> "Yesterday"
            else -> {
                val date = sdfSource.parse(dateStr)
                date?.let { sdfDisplay.format(it) } ?: dateStr
            }
        }
    } catch (e: Exception) {
        dateStr
    }
}

/**
 * Custom Modifier extending drawWithContent. This applies a vertical transparency gradient mapping using Destination-In blend mode,
 * enabling scrolling components to merge beautifully into standard background surfaces without sharp clip lines.
 */
fun Modifier.fadingEdgeMask(
    topFadeHeight: Dp,
    bottomFadeHeight: Dp
): Modifier = this.graphicsLayer {
    // Force offscreen compositing strategy to allow blending modes on translucent canvas
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val topFadePx = topFadeHeight.toPx()
    val bottomFadePx = bottomFadeHeight.toPx()
    val height = size.height

    if (topFadePx > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = topFadePx
            ),
            blendMode = BlendMode.DstIn
        )
    }

    if (bottomFadePx > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = height - bottomFadePx,
                endY = height
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

@Composable
fun FullHistoryScreen(
    dailySummaries: List<PomodoroDailySummary>,
    sessionLogs: List<PomodoroSessionLog>,
    streaks: PomodoroStreaks,
    onBack: () -> Unit,
    onExportPdf: () -> Unit
) {
    val context = LocalContext.current
    var expandedDates by remember { mutableStateOf(setOf<String>()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B11)) // deep dark charcoal base
    ) {
        // Decorative ambient background glow (upper-right and center-left) matching cyan-blue theme
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1900F0FF), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x127C4DFF), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f),
                    radius = size.width * 0.6f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp, start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to main dashboard",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Full Focus History",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleGlow.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export PDF Report",
                        tint = PurpleGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Streak overview board (premium glass card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00F0FF).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Current Streak",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "${streaks.currentStreak} Days",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All-Time Best",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "${streaks.bestStreak} Days",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable logs grouped by date (Unlimited Scrolling - LazyColumn)
            if (dailySummaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No focus records logged yet",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dailySummaries) { day ->
                        val isExpanded = expandedDates.contains(day.date)
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = if (isExpanded) 0.04f else 0.02f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            if (isExpanded) Color(0xFF00F0FF).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    expandedDates = if (isExpanded) expandedDates - day.date else expandedDates + day.date
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand details",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = formatHistoryDate(day.date),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                Text(
                                    text = "Focus: ${formatMinutes(day.focusTimeMs)}",
                                    color = GlowGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Collapsed preview info details row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 22.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cycles: ${day.focusSessionsCompleted}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.2f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Breaks: ${formatMinutes(day.breakTimeMs)}",
                                    color = CyanGlow.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }

                            // Expanded details section
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                val dayLogs = sessionLogs.filter { it.date == day.date }.sortedBy { it.startTime }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, start = 22.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SESSION CHRONICLE TIMESTAMPS",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )

                                    if (dayLogs.isEmpty()) {
                                        Text(
                                            text = "No detailed session logs captured",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 10.sp
                                        )
                                    } else {
                                        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        dayLogs.forEach { log ->
                                            val startStr = sdfTime.format(Date(log.startTime))
                                            val endStr = sdfTime.format(Date(log.endTime))
                                            val durationStr = formatMinutes(log.durationMs)
                                            val labelText = when (log.sessionType) {
                                                "FOCUS" -> "Focus"
                                                "BREAK" -> "Break"
                                                "MANUAL_BREAK" -> "Manual Break"
                                                else -> log.sessionType
                                            }
                                            val badgeColor = when (log.sessionType) {
                                                "FOCUS" -> GlowGreen
                                                "BREAK" -> CyanGlow
                                                else -> PurpleGlow
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(badgeColor)
                                                    )
                                                    Text(
                                                        text = "$startStr – $endStr",
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontSize = 11.sp,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    )
                                                }
                                                Text(
                                                    text = "$labelText ($durationStr)",
                                                    color = Color.White.copy(alpha = 0.45f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
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


