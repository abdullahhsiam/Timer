package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored glow gradients
  content: @Composable () -> Unit,
) {
  val colorScheme = darkColorScheme(
    primary = PurpleGlow,
    onPrimary = DarkPurple,
    secondary = PurpleGlow,
    onSecondary = OffWhite,
    tertiary = NeonPink,
    background = DarkBackground,
    surface = CardBackground,
    onBackground = OffWhite,
    onSurface = OffWhite,
    error = AlertRed,
    onError = OffWhite
  )

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
