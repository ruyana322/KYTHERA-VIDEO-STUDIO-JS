package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KytheraDarkColorScheme = darkColorScheme(
  primary = KytheraElectricBlue,
  onPrimary = KytheraTextPrimary,
  secondary = KytheraCyan,
  onSecondary = KytheraVoid,
  background = KytheraVoid,
  onBackground = KytheraTextPrimary,
  surface = KytheraSurface,
  onSurface = KytheraTextPrimary,
  surfaceVariant = KytheraPanel,
  onSurfaceVariant = KytheraTextSecondary,
)

@Composable
fun KytheraStudioTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = KytheraDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
