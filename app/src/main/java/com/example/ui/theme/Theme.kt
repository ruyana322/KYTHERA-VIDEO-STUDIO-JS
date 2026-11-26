package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class KytheraGlassColors(
  val isDark: Boolean,
  val bgTop: Color,
  val bgMid: Color,
  val bgBottom: Color,
  val blobPrimary: Color,
  val blobSecondary: Color,
  val blobTertiary: Color,
  val glassSurface: Color,
  val glassElevated: Color,
  val glassBorder: Color,
  val subtleBorder: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
)

private val LightGlassTokens = KytheraGlassColors(
  isDark = false,
  bgTop = GlassLightBgTop,
  bgMid = GlassLightBgMid,
  bgBottom = GlassLightBgBottom,
  blobPrimary = GlassLightBlobLavender,
  blobSecondary = GlassLightBlobSky,
  blobTertiary = GlassLightBlobPeach,
  glassSurface = GlassLightSurface,
  glassElevated = GlassLightSurfaceElevated,
  glassBorder = GlassLightBorder,
  subtleBorder = GlassLightSubtleBorder,
  textPrimary = InkPrimary,
  textSecondary = InkSecondary,
  textMuted = InkMuted,
)

private val DarkGlassTokens = KytheraGlassColors(
  isDark = true,
  bgTop = GlassDarkBgTop,
  bgMid = GlassDarkBgMid,
  bgBottom = GlassDarkBgBottom,
  blobPrimary = GlassDarkBlobIndigo,
  blobSecondary = GlassDarkBlobCyan,
  blobTertiary = GlassDarkBlobViolet,
  glassSurface = GlassDarkSurface,
  glassElevated = GlassDarkSurfaceElevated,
  glassBorder = GlassDarkBorder,
  subtleBorder = GlassDarkSubtleBorder,
  textPrimary = DarkTextPrimary,
  textSecondary = DarkTextSecondary,
  textMuted = Color(0xFF6B7894),
)

val LocalKytheraGlass = staticCompositionLocalOf { LightGlassTokens }

private val KytheraLightColorScheme = lightColorScheme(
  primary = KytheraPeriwinkle,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE2E6FF),
  onPrimaryContainer = InkPrimary,
  secondary = KytheraElectricBlue,
  onSecondary = Color.White,
  tertiary = KytheraViolet,
  background = GlassLightBgTop,
  onBackground = InkPrimary,
  surface = GlassLightSurfaceElevated,
  onSurface = InkPrimary,
  surfaceVariant = Color(0xFFEBEFFF),
  onSurfaceVariant = InkSecondary,
  outline = GlassLightSubtleBorder,
)

private val KytheraDarkColorScheme = darkColorScheme(
  primary = KytheraPeriwinkle,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF242E57),
  onPrimaryContainer = DarkTextPrimary,
  secondary = KytheraCyan,
  onSecondary = GlassDarkBgTop,
  tertiary = KytheraViolet,
  background = GlassDarkBgTop,
  onBackground = DarkTextPrimary,
  surface = GlassDarkSurfaceElevated,
  onSurface = DarkTextPrimary,
  surfaceVariant = Color(0xFF1A2236),
  onSurfaceVariant = DarkTextSecondary,
  outline = GlassDarkSubtleBorder,
)

private val KytheraShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KytheraStudioTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val glassTokens = if (darkTheme) DarkGlassTokens else LightGlassTokens
  val colorScheme = if (darkTheme) KytheraDarkColorScheme else KytheraLightColorScheme

  CompositionLocalProvider(LocalKytheraGlass provides glassTokens) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = KytheraTypography,
      shapes = KytheraShapes,
      content = content,
    )
  }
}
