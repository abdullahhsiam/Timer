package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun FlipCardDigit(
    digit: Char,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    width: Dp = 68.dp,
    textSize: Float = 70f
) {
    var previousDigit by remember { mutableStateOf(digit) }
    var currentDigit by remember { mutableStateOf(digit) }
    
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
            rotation.snapTo(0f)
            launch {
                rotation.animateTo(
                    targetValue = 180f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val rotAnim = rotation.asState() // Read only when needed
    // Use derived state just for the threshold if really needed, but it's better to just put both flaps in UI and toggle their alpha in graphicsLayer!

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(Color(0xFF0F1522), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Base static layers
        Column(modifier = Modifier.fillMaxSize()) {
            // Top half: always shows current digit (revealed as flap drops)
            DigitHalfStatic(digit = currentDigit, isTop = true, height = height / 2, width = width, textSize = textSize)
            // Bottom half: always shows previous digit (covered as flap drops)
            DigitHalfStatic(digit = previousDigit, isTop = false, height = height / 2, width = width, textSize = textSize)
        }
        
        // Add static shadows for depth
        Box(modifier = Modifier.fillMaxSize()) {
            // Shadow over bottom half as flap comes down
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .graphicsLayer {
                        val r = rotation.value
                        val staticShadowAlpha = if (r < 90f) (r / 90f) * 0.5f else 0.5f
                        alpha = staticShadowAlpha
                    }
                    .background(Color.Black)
            )
        }

        // Top Flap falling forward (0 to 90 degrees)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    val r = rotation.value
                    alpha = if (r < 90f) 1f else 0f
                    rotationX = -r
                    transformOrigin = TransformOrigin(0.5f, 1f) // Pivot at bottom edge (center hinge)
                    cameraDistance = 12f * density
                }
        ) {
            DigitHalfStatic(digit = previousDigit, isTop = true, height = height / 2, width = width, textSize = textSize)
            
            // Shadow on the flap itself as it faces down
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .graphicsLayer {
                        val r = rotation.value
                        alpha = (r / 90f) * 0.4f
                    }
                    .background(Color.Black)
            )
        }

        // Bottom Flap finishing the fall (90 to 180 degrees)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val r = rotation.value
                    alpha = if (r >= 90f) 1f else 0f
                    rotationX = 180f - r
                    transformOrigin = TransformOrigin(0.5f, 0f) // Pivot at top edge (center hinge)
                    cameraDistance = 12f * density
                }
        ) {
            DigitHalfStatic(digit = currentDigit, isTop = false, height = height / 2, width = width, textSize = textSize)
            
            // Shadow on the flap dissipating as it lands
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .graphicsLayer {
                        val r = rotation.value
                        alpha = ((180f - r) / 90f) * 0.4f
                    }
                    .background(Color.Black)
            )
        }

        // Center split line to sell the mechanical look
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Black.copy(alpha = 0.8f))
        )
    }
}

// A simple static half-digit component.
// We use a fixed-size Canvas/Box approach to prevent Text layout shifting or clipping.
@Composable
fun DigitHalfStatic(
    digit: Char,
    isTop: Boolean,
    height: Dp,
    width: Dp,
    textSize: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(
                if (isTop) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                else RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .background(Color(0xFF0F1522)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            fontSize = textSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeight = textSize.sp
            ),
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .graphicsLayer {
                    translationY = if (isTop) (height.toPx() / 2f) else -(height.toPx() / 2f)
                }
                .padding(horizontal = 4.dp)
        )
        
        // Inner gradient for 3D realism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = if (isTop) {
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                        } else {
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                        }
                    )
                )
        )
    }
}

@Composable
fun FlipClockDisplay(
    timeString: String,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    width: Dp = 68.dp,
    textSize: Float = 70f
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val digitCount = timeString.count { it.isDigit() }
        val separatorCount = timeString.length - digitCount
        var internalSpacers = 0
        for (i in 0 until timeString.length - 1) {
            if (timeString[i].isDigit() && timeString[i+1].isDigit()) {
                internalSpacers++
            }
        }

        // Approximate separator width + padding (4.dp on each side)
        val separatorWidth = (textSize * 0.7f * 0.5f).dp + 8.dp 
        
        val unscaledTotalWidth = (width * digitCount) + 
                                 (4.dp * internalSpacers) + 
                                 (separatorWidth * separatorCount)
                                 
        val maxW = if (maxWidth != Dp.Infinity && maxWidth > 0.dp) {
             maxWidth
        } else {
             androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 32.dp
        }
        
        val scale = if (unscaledTotalWidth > maxW) {
            maxW / unscaledTotalWidth
        } else {
            1f
        }
        
        val scaledHeight = height * scale
        val scaledWidth = width * scale
        val scaledTextSize = textSize * scale
        val scaledSpacer = 4.dp * scale
        val scaledPadding = 4.dp * scale

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in timeString.indices) {
                val c = timeString[i]
                if (c.isDigit()) {
                    FlipCardDigit(
                        digit = c,
                        height = scaledHeight,
                        width = scaledWidth,
                        textSize = scaledTextSize
                    )
                    if (i < timeString.lastIndex && timeString[i+1].isDigit()) {
                        Spacer(modifier = Modifier.width(scaledSpacer))
                    }
                } else {
                    Text(
                        text = c.toString(),
                        fontSize = (scaledTextSize * 0.7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = scaledPadding)
                    )
                }
            }
        }
    }
}
