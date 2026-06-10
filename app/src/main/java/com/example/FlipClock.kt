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

    val rot = rotation.value

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
            val staticShadowAlpha = if (rot < 90f) (rot / 90f) * 0.5f else 0.5f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color.Black.copy(alpha = staticShadowAlpha))
            )
        }

        // The flipping flap logic
        // We only animate the flap. Rotating forward means negative rotationX.
        if (rot < 90f) {
            // Top Flap falling forward (0 to -90 degrees)
            // Shows the top half of the previous digit
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        rotationX = -rot
                        transformOrigin = TransformOrigin(0.5f, 1f) // Pivot at bottom edge (center hinge)
                        cameraDistance = 12f * density
                    }
            ) {
                DigitHalfStatic(digit = previousDigit, isTop = true, height = height / 2, width = width, textSize = textSize)
                
                // Shadow on the flap itself as it faces down
                val flapShadow = (rot / 90f) * 0.4f
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color.Black.copy(alpha = flapShadow))
                )
            }
        } else {
            // Bottom Flap finishing the fall (-90 to 0 degrees)
            // Shows bottom half of the current digit
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        // Start at 90 degrees rotated forward (which means -90 relative to the bottom flap's top-edge pivot)
                        // Actually, if we pivot at top edge of bottom half, rotating +90 goes into screen, -90 comes out.
                        // We want it to swing down from the user towards flat.
                        // So it starts at +90 (pointing right at user) and goes to 0 (flat).
                        rotationX = 180f - rot
                        transformOrigin = TransformOrigin(0.5f, 0f) // Pivot at top edge (center hinge)
                        cameraDistance = 12f * density
                    }
            ) {
                DigitHalfStatic(digit = currentDigit, isTop = false, height = height / 2, width = width, textSize = textSize)
                
                // Shadow on the flap dissipating as it lands
                val flapShadow = ((180f - rot) / 90f) * 0.4f
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(Color.Black.copy(alpha = flapShadow))
                )
            }
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
            .background(Color(0xFF0F1522))
    ) {
        // Draw the full text centered in a double-height box, and just clip half of it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height * 2)
                // Offset perfectly to show top or bottom half
                .offset(y = if (isTop) 0.dp else -height),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = textSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    // Add subtle horizontal padding to prevent clip on wide font strokes
                    .padding(horizontal = 4.dp)
                    // Avoid font metrics pushing the text down
                    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
            )
        }
        
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
    height: Dp = 100.dp,
    width: Dp = 68.dp,
    textSize: Float = 70f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in timeString.indices) {
            val c = timeString[i]
            if (c.isDigit()) {
                FlipCardDigit(
                    digit = c,
                    height = height,
                    width = width,
                    textSize = textSize
                )
                if (i < timeString.lastIndex && timeString[i+1].isDigit()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            } else {
                Text(
                    text = c.toString(),
                    fontSize = (textSize * 0.7f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
