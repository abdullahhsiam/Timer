package com.example

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkPurple
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.OffWhite
import kotlin.math.cos
import kotlin.math.sin

/**
 * AnimatedGradientBackground draws key ambient dynamic dark-colored shifting radial gradients
 * mimicking the exact HTML specification's blur layers (indigo-900/20 & purple-900/10 glows).
 * If isRunningActive is true, it triggers a fully dynamic fluid rotating multi-colored gradient sequence.
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    isPulsingAlarm: Boolean = false,
    isRunningActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    
    // Rotating gradient angle animation for the active running state
    val angleRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Animate fluid movement coordinates for the ambient blurry radial indicators
    val driftOffset1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = SineCrossingEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_1"
    )

    val driftOffset2 by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = SineCrossingEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_2"
    )

    // Alarm active flashing visual colors pulse
    val alarmPulseColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF6B0E23), // Deep crimson red
        targetValue = Color(0xFF140D24), // Dark deep purple
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarm_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val sizeVal = size
                if (isPulsingAlarm) {
                    // Dramatic full-screen flashing layout
                    drawRect(color = alarmPulseColor)
                    
                    // Added radial glow underneath
                    val center = Offset(sizeVal.width / 2f, sizeVal.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF1E56), Color.Transparent),
                            center = center,
                            radius = sizeVal.minDimension * 0.9f
                        ),
                        radius = sizeVal.minDimension * 0.9f,
                        center = center
                    )
                } else if (isRunningActive) {
                    // Gorgeous, highly polished rotating cosmic fluid dark multi-color gradient
                    val cosVal = cos(angleRad) * 0.45f
                    val sinVal = sin(angleRad) * 0.45f
                    
                    val activeBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF090812), // Deep space black base
                            Color(0xFF160E2E), // Low-luminance velvet cosmic purple
                            Color(0xFF0F1E28), // Deep therapeutic teal
                            Color(0xFF20113B), // Mystic amethyst violet
                            Color(0xFF090812)
                        ),
                        start = Offset(
                            x = sizeVal.width * (0.5f + cosVal),
                            y = sizeVal.height * (0.5f - sinVal)
                        ),
                        end = Offset(
                            x = sizeVal.width * (0.5f - cosVal),
                            y = sizeVal.height * (0.5f + sinVal)
                        )
                    )
                    drawRect(brush = activeBrush)

                    // Draw layered active glowing radial accents on top for extra visual depth!
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3E312E81), Color.Transparent), // Stronger indigo layer
                            center = Offset(
                                x = sizeVal.width * -0.05f + driftOffset1 * 1.5f,
                                y = sizeVal.height * -0.1f + driftOffset2 * 1.5f
                            ),
                            radius = sizeVal.width * 1.0f
                        ),
                        radius = sizeVal.width * 1.0f,
                        center = Offset(
                            x = sizeVal.width * -0.05f + driftOffset1 * 1.5f,
                            y = sizeVal.height * -0.1f + driftOffset2 * 1.5f
                        )
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2B581C87), Color.Transparent), // Stronger lavender purple layer
                            center = Offset(
                                x = sizeVal.width * 1.05f - driftOffset2 * 1.5f,
                                y = sizeVal.height * 1.0f - driftOffset1 * 1.5f
                            ),
                            radius = sizeVal.width * 0.9f
                        ),
                        radius = sizeVal.width * 0.9f,
                        center = Offset(
                            x = sizeVal.width * 1.05f - driftOffset2 * 1.5f,
                            y = sizeVal.height * 1.0f - driftOffset1 * 1.5f
                        )
                    )
                } else {
                    // Clean Minimalism Dark solid canvas base
                    drawRect(color = Color(0xFF0F0F12))

                    // Draw Top-Left Blur Circle of Indigo-900 (Color #312E81 with 20% opacity)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x33312E81), Color.Transparent),
                            center = Offset(
                                x = sizeVal.width * -0.1f + driftOffset1,
                                y = sizeVal.height * -0.15f + driftOffset2
                            ),
                            radius = sizeVal.width * 0.9f
                        ),
                        radius = sizeVal.width * 0.9f,
                        center = Offset(
                            x = sizeVal.width * -0.1f + driftOffset1,
                            y = sizeVal.height * -0.15f + driftOffset2
                        )
                    )

                    // Draw Bottom-Right Blur Circle of Purple-900 (Color #581C87 with 10% opacity)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1B581C87), Color.Transparent),
                            center = Offset(
                                x = sizeVal.width * 1.1f - driftOffset2,
                                y = sizeVal.height * 1.05f - driftOffset1
                            ),
                            radius = sizeVal.width * 0.82f
                        ),
                        radius = sizeVal.width * 0.82f,
                        center = Offset(
                            x = sizeVal.width * 1.1f - driftOffset2,
                            y = sizeVal.height * 1.05f - driftOffset1
                        )
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Custom Easing for soft rhythmic oscillation.
 */
class SineCrossingEasing : Easing {
    override fun transform(fraction: Float): Float {
        return (sin(fraction * Math.PI - Math.PI / 2) + 1).toFloat() / 2f
    }
}

/**
 * CircleProgressTimer displays the concentric outline circles with giant
 * extralight typography from the Focus Mode Clean Minimalism layout.
 */
@Composable
fun CircleProgressTimer(
    modifier: Modifier = Modifier,
    remainingMs: Long,
    totalMs: Long,
    displayString: String,
    statusText: String,
    onProgressColor: Color = PurpleGlow,
    glowEnabled: Boolean = true
) {
    // Smooth progress fraction anim
    val progressFraction = if (totalMs > 0) remainingMs.toFloat() / totalMs.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "elapsed_radial"
    )

    Box(
        modifier = modifier
            .size(310.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Concentric Clean Borders (absolute -inset-12 and -inset-24)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            
            // Outermost Concentric border (border-white/[0.02])
            drawCircle(
                color = Color.White.copy(alpha = 0.02f),
                radius = size.minDimension * 0.48f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Middle Concentric border (border-white/5)
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.minDimension * 0.41f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Extremely thin and clean elegant circular stopwatch progress arc overlay
            if (totalMs > 0L) {
                drawArc(
                    color = onProgressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width * 0.82f, size.height * 0.82f),
                    topLeft = Offset(size.width * 0.09f, size.height * 0.09f)
                )
            }
        }

        // Timer Details Block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Giant extralight time tracking string
            Text(
                text = displayString,
                color = OffWhite,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Elegant sub-title text tracking state
            Text(
                text = statusText.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.5.sp,
                    color = Color.White.copy(alpha = 0.4f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * ModernKeypad provides a number keyboard layout designed with the
 * clean minimalist look.
 */
@Composable
fun ModernKeypad(
    modifier: Modifier = Modifier,
    onDigitClicked: (Int) -> Unit,
    onDeleteClicked: () -> Unit,
    onClearAllClicked: () -> Unit
) {
    val items = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "C", "0", "⌫"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val label = items[index]
                    
                    KeypadButton(
                        label = label,
                        modifier = Modifier.size(72.dp),
                        onClick = {
                            when (label) {
                                "C" -> onClearAllClicked()
                                "⌫" -> onDeleteClicked()
                                else -> onDigitClicked(label.toInt())
                            }
                        },
                        isAction = label == "C" || label == "⌫"
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isAction: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isAction) Color.White.copy(alpha = 0.03f) else Color.Transparent)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple()
            )
            .border(
                width = 1.dp,
                color = if (isAction) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label == "C" || label == "⌫") 16.sp else 22.sp,
            fontWeight = FontWeight.Light,
            color = if (isAction) PurpleGlow else OffWhite
        )
    }
}

/**
 * ModernPresetButton provides pill-shaped preset shortcuts
 * for quick starting timers instantly.
 */
@Composable
fun ModernPresetButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = OffWhite.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * ActionButton is designed directly to match the Clean Minimalism aesthetic.
 * - Primary uses the Lavender rounded-[32px] design (#D0BCFF, text #381E72)
 * - Secondary uses the clean outline round button.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    accentColor: Color = PurpleGlow,
    enabled: Boolean = true
) {
    val bgBrush = if (isPrimary) {
        Brush.horizontalGradient(listOf(accentColor, accentColor))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val contentColor = if (isPrimary) DarkPurple else OffWhite

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(if (isPrimary) RoundedCornerShape(32.dp) else CircleShape)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(brush = bgBrush)
            .border(
                width = 1.dp,
                color = if (isPrimary) Color.Transparent else Color.White.copy(alpha = 0.1f),
                shape = if (isPrimary) RoundedCornerShape(32.dp) else CircleShape
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            letterSpacing = 0.5.sp
        )
    }
}
