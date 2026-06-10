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
    val scope = rememberCoroutineScope()

    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
            rotation.snapTo(0f)
            launch {
                rotation.animateTo(
                    targetValue = 180f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
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
        // Base digit (Top shows current, Bottom shows previous but will be covered soon)
        val splitColor = Color.Black.copy(alpha = 0.4f)

        // Draw top and bottom parts.
        // It's a bit complex to do standard canvas split, so we simulate it with overlapping boxes.
        
        // Static back halves:
        // Top static = current digit
        DigitHalf(digit = currentDigit, isTop = true, height = height, width = width, textSize = textSize)
        
        // Bottom static = previous digit
        Box(modifier = Modifier.width(width).height(height)) {
            DigitHalf(digit = previousDigit, isTop = false, height = height, width = width, textSize = textSize)
            // Shadow over the bottom static half cast by the top flap coming down
            val staticShadowAlpha = if (rot < 90f) (rot / 90f) * 0.5f else 0f
            if (staticShadowAlpha > 0f) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .offset(y = height / 2)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color.Black.copy(alpha = staticShadowAlpha))
                )
            }
        }

        // The flipping flap
        if (rot < 90f) {
            // Flap represents the top half of the *previous* digit flipping down
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .graphicsLayer {
                        rotationX = rot
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        cameraDistance = 8f * density
                    }
            ) {
                DigitHalf(digit = previousDigit, isTop = true, height = height, width = width, textSize = textSize)
                
                // Shadow on the flipping top half as it tilts away from the light
                val flapShadow = (rot / 90f) * 0.6f
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Black.copy(alpha = flapShadow))
                )
            }
        } else {
            // Flap represents the bottom half of the *current* digit landing down
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .graphicsLayer {
                        rotationX = rot - 180f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        cameraDistance = 8f * density
                    }
            ) {
                DigitHalf(digit = currentDigit, isTop = false, height = height, width = width, textSize = textSize)
                
                // Shadow on the flipping bottom half as it is coming down (it gets lighter as it lands)
                val flapShadow = ((180f - rot) / 90f) * 0.6f
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .offset(y = height / 2)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color.Black.copy(alpha = flapShadow))
                )
            }
        }

        // Middle shadow split line
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        )
    }
}

@Composable
fun DigitHalf(
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
    ) {
        if (isTop) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color(0xFF0F1522))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit.toString(),
                        fontSize = textSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .offset(y = height / 2)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFF0F1522))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .offset(y = -height / 2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit.toString(),
                        fontSize = textSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
        
        // Add a subtle gradient/shadow inside for depth
        if (isTop) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .offset(y = height / 2)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
fun FlipClockDisplay(
    timeString: String, // e.g. "05:23:10"
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
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
