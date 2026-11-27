package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass

@Composable
fun AmbientGlassBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val glass = LocalKytheraGlass.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(glass.bgTop, glass.bgMid, glass.bgBottom),
        ),
      )
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(glass.blobPrimary.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.14f),
            radius = size.maxDimension * 0.48f,
          ),
          radius = size.maxDimension * 0.48f,
          center = Offset(size.width * 0.15f, size.height * 0.14f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(glass.blobSecondary.copy(alpha = 0.50f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.24f),
            radius = size.maxDimension * 0.42f,
          ),
          radius = size.maxDimension * 0.42f,
          center = Offset(size.width * 0.88f, size.height * 0.24f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(glass.blobTertiary.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(size.width * 0.72f, size.height * 0.88f),
            radius = size.maxDimension * 0.45f,
          ),
          radius = size.maxDimension * 0.45f,
          center = Offset(size.width * 0.72f, size.height * 0.88f),
        )
      },
    content = content,
  )
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 20.dp,
  elevated: Boolean = false,
  onClick: (() -> Unit)? = null,
  contentPadding: Dp = 14.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val shape = RoundedCornerShape(cornerRadius)
  val bg = if (elevated) glass.glassElevated else glass.glassSurface

  Surface(
    modifier = modifier
      .shadow(
        elevation = if (glass.isDark) 10.dp else 12.dp,
        shape = shape,
        ambientColor = KytheraPeriwinkle.copy(alpha = if (glass.isDark) 0.22f else 0.14f),
        spotColor = KytheraPeriwinkle.copy(alpha = if (glass.isDark) 0.24f else 0.12f),
      )
      .clip(shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = shape,
    color = bg,
    border = BorderStroke(1.dp, glass.glassBorder),
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = if (glass.isDark) 0.06f else 0.36f),
              Color.Transparent,
            ),
          ),
        )
        .padding(contentPadding),
      content = content,
    )
  }
}

@Composable
fun SquircleBadge(
  icon: ImageVector,
  contentDescription: String,
  startColor: Color,
  endColor: Color,
  size: Dp = 40.dp,
  iconSize: Dp = 20.dp,
) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(13.dp))
      .background(Brush.linearGradient(listOf(startColor, endColor)))
      .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = Color.White,
      modifier = Modifier.size(iconSize),
    )
  }
}
