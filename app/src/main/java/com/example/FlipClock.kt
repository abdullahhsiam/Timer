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
        DigitHalf(digit = previousDigit, isTop = false, height = height, width = width, textSize = textSize)

        // The flipping flap
        if (rot < 90f) {
            // Flap represents the top half of the *previous* digit flipping down
            DigitHalf(
                digit = previousDigit,
                isTop = true,
                height = height,
                width = width,
                textSize = textSize,
                modifier = Modifier.graphicsLayer {
                    rotationX = rot
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    cameraDistance = 8f * density
                }
            )
        } else {
            // Flap represents the bottom half of the *current* digit landing down
            DigitHalf(
                digit = currentDigit,
                isTop = false,
                height = height,
                width = width,
                textSize = textSize,
                modifier = Modifier.graphicsLayer {
                    rotationX = rot - 180f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    cameraDistance = 8f * density
                }
            )
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
    val align = if (isTop) Alignment.TopCenter else Alignment.BottomCenter
    val clipShape = if (isTop) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp) else RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    
    Box(
        modifier = modifier
            .width(width)
            .height(height / 2)
            .clip(clipShape)
            .background(Color(0xFF0F1522)),
        contentAlignment = align
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .fillMaxWidth(),
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
