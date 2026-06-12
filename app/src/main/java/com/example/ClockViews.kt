package com.example

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockTabContent(viewModel: TimerStopwatchViewModel) {
    val activeVisualMode by viewModel.activeVisualMode.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600 && configuration.screenHeightDp >= 600
    
    // We update current time every second, optimized to only recompose this component.
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMillis = now
            val delayToNextSecond = 1000L - (now % 1000L)
            delay(delayToNextSecond)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = activeVisualMode,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) + 
                androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.LinearOutSlowInEasing))) togetherWith 
                (androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) + 
                androidx.compose.animation.scaleOut(targetScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(150)))
            },
            label = "clock_visual_mode_transition"
        ) { mode ->
            if (mode == 1) {
                ClockFlipView(viewModel, currentTimeMillis, isTablet, isLandscape)
            } else {
                ClockAnalogView(viewModel, currentTimeMillis, isTablet, isLandscape)
            }
        }
    }
}

@Composable
fun ClockAnalogView(viewModel: TimerStopwatchViewModel, currentTimeMillis: Long, isTablet: Boolean, isLandscape: Boolean) {
    // Beautiful live analog clock as the main centerpiece
    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val ms = cal.get(Calendar.MILLISECOND)

    // Smooth seconds (optional if updating every second, but ms will be 0 typically due to delay alignment)
    val smoothSecond = second + ms / 1000f
    val smoothMinute = minute + smoothSecond / 60f
    val smoothHour = hour + smoothMinute / 60f

    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
    val format12 = SimpleDateFormat("hh:mm", Locale.getDefault())

    val dateStr = dateFormat.format(cal.time).uppercase()
    val amPmStr = amPmFormat.format(cal.time)
    val timeStr = format12.format(cal.time)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // Device-class aware bounding calculations
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp

        // Define bounds for each device class
        val baseMin = if (isTablet) {
            260.dp
        } else if (isLandscape) {
            130.dp // Allow landscape on phones to shrink nicely without clipping
        } else if (screenWidth < 360) { // Compact phone
            135.dp
        } else if (screenWidth in 360..411) { // Standard phone
            165.dp
        } else { // Large phone (412 to 599 dp)
            185.dp
        }

        val baseMax = if (isTablet) {
            350.dp
        } else if (isLandscape) {
            180.dp
        } else if (screenWidth < 360) { // Compact phone
            155.dp
        } else if (screenWidth in 360..411) { // Standard phone
            195.dp
        } else { // Large phone
            220.dp
        }

        val paddingSpace = if (isLandscape) 70.dp else 115.dp
        val safeDiameter = if (totalWidth < totalHeight - paddingSpace) totalWidth else totalHeight - paddingSpace
        val clockSize = safeDiameter.coerceIn(baseMin, baseMax)
        val scaleFactor = clockSize.value / 260f

        val rawTimeFontSize = (48f * scaleFactor).coerceAtLeast(32f)
        val rawAmPmFontSize = (20f * scaleFactor).coerceAtLeast(14f)
        val rawDateFontSize = (13f * scaleFactor).coerceAtLeast(10f)
        val rawLetterSpacing = (2f * scaleFactor).coerceAtLeast(1f)
        val rawSpacerHeight = (24f * scaleFactor).coerceAtLeast(8f)
        val rawMarginWidth = (8f * scaleFactor).coerceAtLeast(4f)
        val rawAmPmBottomPadding = (8f * scaleFactor).coerceAtLeast(4f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .size(clockSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(2.dp * scaleFactor, Color.White.copy(alpha = 0.1f), CircleShape)
                    .shadow(
                        elevation = 16.dp * scaleFactor,
                        shape = CircleShape,
                        spotColor = Color(0xFF4C8DFF).copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val neonPink = com.example.ui.theme.NeonPink
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp * scaleFactor)) {
                    val center = this.size.center
                    val radius = this.size.width / 2f
                    
                    // Draw ticks
                    for (i in 0 until 60) {
                        val angle = Math.PI * i / 30.0 - Math.PI / 2.0
                        val isHour = i % 5 == 0
                        val tickLength = if (isHour) radius * 0.15f else radius * 0.05f
                        val strokeWidth = if (isHour) (3.dp * scaleFactor).toPx() else (1.dp * scaleFactor).toPx()
                        val color = if (isHour) Color.White else Color.White.copy(alpha = 0.3f)
                        
                        val start = Offset(
                            x = center.x + cos(angle).toFloat() * radius,
                            y = center.y + sin(angle).toFloat() * radius
                        )
                        val end = Offset(
                            x = center.x + cos(angle).toFloat() * (radius - tickLength),
                            y = center.y + sin(angle).toFloat() * (radius - tickLength)
                        )
                        drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    }

                    // Draw hour hand
                    val hourAngle = Math.PI * smoothHour / 6.0 - Math.PI / 2.0
                    val hourLength = radius * 0.5f
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(
                            x = center.x + cos(hourAngle).toFloat() * hourLength,
                            y = center.y + sin(hourAngle).toFloat() * hourLength
                        ),
                        strokeWidth = (6.dp * scaleFactor).toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw minute hand
                    val minuteAngle = Math.PI * smoothMinute / 30.0 - Math.PI / 2.0
                    val minLength = radius * 0.75f
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = center,
                        end = Offset(
                            x = center.x + cos(minuteAngle).toFloat() * minLength,
                            y = center.y + sin(minuteAngle).toFloat() * minLength
                        ),
                        strokeWidth = (4.dp * scaleFactor).toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw second hand
                    val secondAngle = Math.PI * smoothSecond / 30.0 - Math.PI / 2.0
                    val secLength = radius * 0.85f
                    drawLine(
                        color = neonPink,
                        start = center,
                        end = Offset(
                            x = center.x + cos(secondAngle).toFloat() * secLength,
                            y = center.y + sin(secondAngle).toFloat() * secLength
                        ),
                        strokeWidth = (2.dp * scaleFactor).toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw center dot
                    drawCircle(color = neonPink, radius = (6.dp * scaleFactor).toPx(), center = center)
                    drawCircle(color = Color.White, radius = (3.dp * scaleFactor).toPx(), center = center)
                }
            }
            
            Spacer(modifier = Modifier.height(rawSpacerHeight.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = rawTimeFontSize.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(rawMarginWidth.dp))
                Text(
                    text = amPmStr,
                    color = com.example.ui.theme.NeonPink,
                    fontSize = rawAmPmFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = rawAmPmBottomPadding.dp)
                )
            }
            Text(
                text = dateStr,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = rawDateFontSize.sp,
                letterSpacing = rawLetterSpacing.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ClockFlipView(viewModel: TimerStopwatchViewModel, currentTimeMillis: Long, isTablet: Boolean, isLandscape: Boolean) {
    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    
    // We use a 12 hour format display, so hour = 0 translates to 12
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val pad = { n: Int -> if (n < 10) "0$n" else n.toString() }
    
    val timeString = "${pad(displayHour)}:${pad(minute)}:${pad(second)}"
    
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val scaleFactor = (if (isTablet) 1.25f else if (isLandscape) 0.85f else 1.0f)

        val rawDateFontSize = (13f * scaleFactor).coerceAtLeast(10f)
        val rawAmPmFontSize = (11f * scaleFactor).coerceAtLeast(10f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            FlipClockDisplay(
                timeString = timeString,
                height = (if (isTablet) 115.dp else if (isLandscape) 85.dp else 105.dp) * scaleFactor,
                width = (if (isTablet) 72.dp else if (isLandscape) 55.dp else 65.dp) * scaleFactor,
                textSize = (if (isTablet) 76f else if (isLandscape) 58f else 68f) * scaleFactor,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height((if (isLandscape) 12.dp else 24.dp) * scaleFactor))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dateFormat.format(cal.time).uppercase(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = rawDateFontSize.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(com.example.ui.theme.NeonPink.copy(alpha = 0.2f))
                        .border(1.dp, com.example.ui.theme.NeonPink.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = amPmFormat.format(cal.time),
                        color = com.example.ui.theme.NeonPink,
                        fontSize = rawAmPmFontSize.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
