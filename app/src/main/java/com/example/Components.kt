package com.example

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
    isAnimated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val wallpaperUri by TimerStopwatchStateManager.wallpaperUri.collectAsState()
    val glassBlur by TimerStopwatchStateManager.glassBlur.collectAsState()
    val glassOpacity by TimerStopwatchStateManager.glassOpacity.collectAsState()
    val glassGlow by TimerStopwatchStateManager.glassGlow.collectAsState()
    val glassTint by TimerStopwatchStateManager.glassTint.collectAsState()
    val glassAnimationSpeed by TimerStopwatchStateManager.glassAnimationSpeed.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    
    // Animate rotation angle, adjusting speed based on user preference
    val speedFactor = if (glassAnimationSpeed > 0f) glassAnimationSpeed else 1.0f
    val animDuration = (12000 / speedFactor).toInt().coerceIn(2000, 120000)
    
    val angleRad by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Drift offsets with speed scaling
    val driftOffset1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween((8000 / speedFactor).toInt().coerceIn(1000, 80000), easing = SineCrossingEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_1"
    )

    val driftOffset2 by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween((10000 / speedFactor).toInt().coerceIn(1000, 100000), easing = SineCrossingEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_2"
    )

    // Alarm pulsing
    val alarmPulseColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF6B0E23),
        targetValue = Color(0xFF140D24),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarm_color"
    )

    val tintOverlayColor = when (glassTint.lowercase()) {
        "cyan" -> Color(0x1F00E6FF)
        "pink" -> Color(0x1FFF2A6D)
        "yellow" -> Color(0x1FFFD600)
        "green" -> Color(0x1F00FF87)
        else -> Color(0x22D0BCFF) // Default lavender
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Wallpaper Backdrop Layer
        if (wallpaperUri.isNotEmpty()) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(wallpaperUri)
                    .crossfade(true)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = "User Selected Backdrop",
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            Modifier.blur(glassBlur.dp)
                        } else {
                            Modifier
                        }
                    ),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Ambient Gradient Overlay Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val sizeVal = size
                    if (isPulsingAlarm) {
                        drawRect(color = alarmPulseColor)
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
                    } else if (wallpaperUri.isNotEmpty()) {
                        // Blend wall-paper, apply configuring glass opacity & tint
                        drawRect(color = Color.Black.copy(alpha = glassOpacity.coerceIn(0f, 1f)))
                        drawRect(color = tintOverlayColor)
                    } else if (isRunningActive) {
                        val activeAngle = if (isAnimated && glassAnimationSpeed > 0f) angleRad else 0f
                        val activeDrift1 = if (isAnimated && glassAnimationSpeed > 0f) driftOffset1 else 0f
                        val activeDrift2 = if (isAnimated && glassAnimationSpeed > 0f) driftOffset2 else 0f

                        val cosVal = cos(activeAngle) * 0.45f
                        val sinVal = sin(activeAngle) * 0.45f
                        
                        val activeBrush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF090812),
                                Color(0xFF160E2E),
                                Color(0xFF0F1E28),
                                Color(0xFF20113B),
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

                        // Glowing ambient circles based on users tint selection
                        val glowTintAccent = when (glassTint.lowercase()) {
                            "cyan" -> Color(0x3E00E6FF)
                            "pink" -> Color(0x3EFF2A6D)
                            "yellow" -> Color(0x3EFFD600)
                            "green" -> Color(0x3E00FF87)
                            else -> Color(0x3E312E81) // lavender
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowTintAccent, Color.Transparent),
                                center = Offset(
                                    x = sizeVal.width * -0.05f + activeDrift1 * 1.5f,
                                    y = sizeVal.height * -0.1f + activeDrift2 * 1.5f
                                ),
                                radius = sizeVal.width * 1.0f
                            ),
                            radius = sizeVal.width * 1.0f,
                            center = Offset(
                                x = sizeVal.width * -0.05f + activeDrift1 * 1.5f,
                                y = sizeVal.height * -0.1f + activeDrift2 * 1.5f
                            )
                        )
                    } else {
                        val passiveDrift1 = if (isAnimated && glassAnimationSpeed > 0f) driftOffset1 else 0f
                        val passiveDrift2 = if (isAnimated && glassAnimationSpeed > 0f) driftOffset2 else 0f

                        drawRect(color = Color(0xFF0F0F12))

                        val primaryGlowAccent = when (glassTint.lowercase()) {
                            "cyan" -> Color(0x2300E6FF)
                            "pink" -> Color(0x23FF2A6D)
                            "yellow" -> Color(0x23FFD600)
                            "green" -> Color(0x2300FF87)
                            else -> Color(0x33312E81) // purple/indigo
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryGlowAccent, Color.Transparent),
                                center = Offset(
                                    x = sizeVal.width * -0.1f + passiveDrift1,
                                    y = sizeVal.height * -0.15f + passiveDrift2
                                ),
                                radius = sizeVal.width * 0.9f
                            ),
                            radius = sizeVal.width * 0.9f,
                            center = Offset(
                                x = sizeVal.width * -0.1f + passiveDrift1,
                                y = sizeVal.height * -0.15f + passiveDrift2
                            )
                        )
                    }
                }
        ) {
            content()
        }
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

    val isInPip by TimerStopwatchStateManager.isInPip.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val sizeD = if (isInPip) {
        140.dp
    } else if (isLandscape) {
        200.dp
    } else {
        310.dp
    }
    
    val textFontSize = if (isInPip) {
        22.sp
    } else if (isLandscape) {
        36.sp
    } else {
        52.sp
    }
    
    val subtitleTopPadding = if (isInPip) {
        1.dp
    } else if (isLandscape) {
        2.dp
    } else {
        10.dp
    }

    Box(
        modifier = modifier
            .size(sizeD)
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
                fontSize = textFontSize,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp
            )
            
            Spacer(modifier = Modifier.height(subtitleTopPadding))
            
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
    buttonSize: androidx.compose.ui.unit.Dp = 72.dp,
    spacing: androidx.compose.ui.unit.Dp = 12.dp,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    actionFontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
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
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.CenterHorizontally)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val label = items[index]
                    
                    KeypadButton(
                        label = label,
                        modifier = Modifier.size(buttonSize),
                        fontSize = fontSize,
                        actionFontSize = actionFontSize,
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
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    actionFontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
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
        val resolvedFontSize = if (isAction) {
            if (actionFontSize != androidx.compose.ui.unit.TextUnit.Unspecified) actionFontSize else 16.sp
        } else {
            if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) fontSize else 22.sp
        }
        Text(
            text = label,
            fontSize = resolvedFontSize,
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
