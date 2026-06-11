package com.example

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
                ClockFlipView(currentTimeMillis, isTablet, isLandscape)
            } else {
                ClockAnalogView(currentTimeMillis, isTablet, isLandscape)
            }
        }
    }
}

@Composable
fun ClockAnalogView(currentTimeMillis: Long, isTablet: Boolean, isLandscape: Boolean) {
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val clockSize = if (isTablet) 320.dp else if (isLandscape) 220.dp else 260.dp
        
        Box(
            modifier = Modifier
                .size(clockSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f))
                .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFF4C8DFF).copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val center = this.size.center
                val radius = this.size.width / 2f
                
                // Draw ticks
                for (i in 0 until 60) {
                    val angle = Math.PI * i / 30.0 - Math.PI / 2.0
                    val isHour = i % 5 == 0
                    val tickLength = if (isHour) radius * 0.15f else radius * 0.05f
                    val strokeWidth = if (isHour) 3.dp.toPx() else 1.dp.toPx()
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
                    strokeWidth = 6.dp.toPx(),
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
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Draw second hand
                val secondAngle = Math.PI * smoothSecond / 30.0 - Math.PI / 2.0
                val secLength = radius * 0.85f
                drawLine(
                    color = com.example.ui.theme.NeonPink,
                    start = center,
                    end = Offset(
                        x = center.x + cos(secondAngle).toFloat() * secLength,
                        y = center.y + sin(secondAngle).toFloat() * secLength
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Draw center dot
                drawCircle(color = com.example.ui.theme.NeonPink, radius = 6.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = timeStr,
                color = Color.White,
                fontSize = if (isTablet) 56.sp else 48.sp,
                fontWeight = FontWeight.Light,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amPmStr,
                color = com.example.ui.theme.NeonPink,
                fontSize = if (isTablet) 24.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Text(
            text = dateStr,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ClockFlipView(currentTimeMillis: Long, isTablet: Boolean, isLandscape: Boolean) {
    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    
    // We use a 12 hour format display, so hour = 0 translates to 12
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val pad = { n: Int -> if (n < 10) "0$n" else n.toString() }
    
    val timeString = "${pad(displayHour)}:${pad(minute)}"
    
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        FlipClockDisplay(
            timeString = timeString,
            height = if (isTablet) 120.dp else if (isLandscape) 90.dp else 110.dp,
            width = if (isTablet) 80.dp else if (isLandscape) 60.dp else 70.dp,
            textSize = if (isTablet) 80f else if (isLandscape) 64f else 72f,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateFormat.format(cal.time).uppercase(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
