package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val LocalThemeTemperature = compositionLocalOf { 0.0f } // 0f = Cold, 1f = Warm

// Polished Warm Glow & Dark Mode Colors
private val DarkBackgroundWarm = Color(0xFF140A06)
private val CardBackgroundWarm = Color(0xFF1F110B)
private val CyanGlowWarm = Color(0xFFFF9900) // Replacing Cyan with Warm Amber
private val PurpleGlowWarm = Color(0xFFFF5E3A) // Replacing Purple with Warm Coral/Orange
private val DarkPurpleWarm = Color(0xFF5A1C0E) // Replacing Dark Purple with deep brick red
private val NeonPinkWarm = Color(0xFFFF2A6D) // Keeping pink but is warm
private val OffWhiteWarm = Color(0xFFF2EBE9) // Warmer offwhite

private val DarkBackgroundCold = Color(0xFF0F0F12)
private val CardBackgroundCold = Color(0xFF141419)
private val CyanGlowCold = Color(0xFF00E6FF)
private val PurpleGlowCold = Color(0xFFD0BCFF)
private val DarkPurpleCold = Color(0xFF381E72)
private val NeonPinkCold = Color(0xFFFF2A6D)
private val OffWhiteCold = Color(0xFFE6E1E5)

val DarkBackground: Color @Composable get() = lerp(DarkBackgroundCold, DarkBackgroundWarm, LocalThemeTemperature.current)
val CardBackground: Color @Composable get() = lerp(CardBackgroundCold, CardBackgroundWarm, LocalThemeTemperature.current)
val CyanGlow: Color @Composable get() = lerp(CyanGlowCold, CyanGlowWarm, LocalThemeTemperature.current)
val PurpleGlow: Color @Composable get() = lerp(PurpleGlowCold, PurpleGlowWarm, LocalThemeTemperature.current)
val DarkPurple: Color @Composable get() = lerp(DarkPurpleCold, DarkPurpleWarm, LocalThemeTemperature.current)
val NeonPink: Color @Composable get() = lerp(NeonPinkCold, NeonPinkWarm, LocalThemeTemperature.current)
val OffWhite: Color @Composable get() = lerp(OffWhiteCold, OffWhiteWarm, LocalThemeTemperature.current)

val GlowGreen = Color(0xFF10B981) // Crisp focus mode emerald green
val WarmCoral = Color(0xFFFC5185)
val AlertRed = Color(0xFFFF1E56)
val GlassWhite = Color(0x1BFFFFFF)
val ConcentricWhiteLow = Color(0x05FFFFFF)
val ConcentricWhiteMid = Color(0x0DFFFFFF)

