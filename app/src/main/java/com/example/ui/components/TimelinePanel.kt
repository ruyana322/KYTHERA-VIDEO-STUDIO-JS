package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.StudioProject
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass
import kotlin.math.ceil

@Composable
fun TimelinePanel(
  project: StudioProject,
  currentTimeSec: Float,
  selectedLayerId: String?,
  zoom: Float,
  onScrub: (Float) -> Unit,
  onZoomDelta: (Float) -> Unit,
  onSelectLayer: (String) -> Unit,
  onToggleVisibility: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onShiftLayerStart: (String, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val glass = LocalKytheraGlass.current
  val hScroll = rememberScrollState()
  val pxPerSecDp = 64f * zoom
  val trackTotalWidthDp = (project.durationSec * pxPerSecDp).coerceAtLeast(280f)
  val density = LocalDensity.current
  val pxPerSecPx = with(density) { pxPerSecDp.dp.toPx() }

  GlassCard(
    modifier = modifier.fillMaxWidth(),
    cornerRadius = 20.dp,
    contentPadding = 10.dp,
  ) {
    // Timeline Toolbar + Time Ruler Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.width(126.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { onZoomDelta(-0.25f) },
          modifier = Modifier.size(32.dp).testTag("timeline_zoom_out"),
        ) {
          Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = glass.textSecondary, modifier = Modifier.size(18.dp))
        }
        Text(
          text = "${(zoom * 100).toInt()}%",
          fontFamily = JetBrainsMonoFamily,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = glass.textPrimary,
        )
        IconButton(
          onClick = { onZoomDelta(0.25f) },
          modifier = Modifier.size(32.dp).testTag("timeline_zoom_in"),
        ) {
          Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = glass.textSecondary, modifier = Modifier.size(18.dp))
        }
      }

      // Scrollable Ruler
      Box(
        modifier = Modifier
          .weight(1f)
          .height(28.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(glass.textPrimary.copy(alpha = 0.05f))
          .horizontalScroll(hScroll),
      ) {
        Canvas(
          modifier = Modifier
            .width(trackTotalWidthDp.dp)
            .fillMaxHeight()
            .pointerInput(project.durationSec, pxPerSecPx) {
              detectTapGestures { pos ->
                onScrub(pos.x / pxPerSecPx)
              }
            }
            .pointerInput(project.durationSec, pxPerSecPx) {
              detectDragGestures { change, _ ->
                change.consume()
                onScrub(change.position.x / pxPerSecPx)
              }
            },
        ) {
          val totalSecs = ceil(project.durationSec).toInt()
          for (s in 0..totalSecs) {
            val x = s * pxPerSecPx
            drawLine(
              color = glass.textSecondary.copy(alpha = 0.55f),
              start = Offset(x, size.height * 0.35f),
              end = Offset(x, size.height),
              strokeWidth = 2f,
            )
            if (s < totalSecs) {
              val halfX = (s + 0.5f) * pxPerSecPx
              drawLine(
                color = glass.textMuted.copy(alpha = 0.35f),
                start = Offset(halfX, size.height * 0.65f),
                end = Offset(halfX, size.height),
                strokeWidth = 1.5f,
              )
            }
          }
          val playX = (currentTimeSec * pxPerSecPx).coerceIn(0f, size.width)
          drawLine(
            color = KytheraCyan,
            start = Offset(playX, 0f),
            end = Offset(playX, size.height),
            strokeWidth = 3f,
          )
          drawCircle(color = KytheraCyan, radius = 5.dp.toPx(), center = Offset(playX, 6.dp.toPx()))
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Multi-Layer Rows
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(project.layers, key = { it.id }) { layer ->
        val isSelected = layer.id == selectedLayerId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (isSelected) KytheraPeriwinkle.copy(alpha = 0.14f)
              else glass.textPrimary.copy(alpha = 0.03f),
            )
            .border(
              width = if (isSelected) 1.2.dp else 0.6.dp,
              color = if (isSelected) KytheraPeriwinkle else glass.subtleBorder,
              shape = RoundedCornerShape(12.dp),
            )
            .clickable { onSelectLayer(layer.id) }
            .padding(horizontal = 6.dp)
            .testTag("timeline_layer_${layer.id}"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Track header
          Row(
            modifier = Modifier.width(120.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = iconForLayerType(layer.type),
              contentDescription = layer.type.label,
              tint = Color(layer.colorHex),
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = layer.name,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = glass.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Icon(
              imageVector = if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
              contentDescription = "Toggle visibility",
              tint = if (layer.visible) glass.textSecondary else glass.textMuted,
              modifier = Modifier
                .size(16.dp)
                .clickable { onToggleVisibility(layer.id) },
            )
            Icon(
              imageVector = if (layer.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
              contentDescription = "Toggle lock",
              tint = if (layer.locked) KytheraPeriwinkle else glass.textMuted,
              modifier = Modifier
                .size(15.dp)
                .clickable { onToggleLock(layer.id) },
            )
          }

          // Synchronized Clip Bar & Keyframe Diamonds
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .horizontalScroll(hScroll),
          ) {
            Canvas(
              modifier = Modifier
                .width(trackTotalWidthDp.dp)
                .fillMaxHeight()
                .pointerInput(layer.id, layer.locked, pxPerSecPx) {
                  detectDragGestures { change, dragAmount ->
                    change.consume()
                    onSelectLayer(layer.id)
                    if (!layer.locked) {
                      onShiftLayerStart(layer.id, dragAmount.x / pxPerSecPx)
                    }
                  }
                },
            ) {
              val startX = layer.startSec * pxPerSecPx
              val clipW = (layer.durationSec * pxPerSecPx).coerceAtLeast(24f)
              val barTop = 6.dp.toPx()
              val barH = size.height - 12.dp.toPx()
              val accent = Color(layer.colorHex)

              drawRoundRect(
                color = accent.copy(alpha = if (isSelected) 0.34f else 0.22f),
                topLeft = Offset(startX, barTop),
                size = Size(clipW, barH),
                cornerRadius = CornerRadius(10f, 10f),
              )
              drawRoundRect(
                color = accent,
                topLeft = Offset(startX, barTop),
                size = Size(clipW, barH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = if (isSelected) 2.2f else 1.2f),
              )

              // Keyframe diamond indicators
              layer.allKeyframeTimes().forEach { kt ->
                val kx = startX + kt * pxPerSecPx
                val ky = size.height / 2f
                val d = 4.5.dp.toPx()
                val diamond = Path().apply {
                  moveTo(kx, ky - d)
                  lineTo(kx + d, ky)
                  lineTo(kx, ky + d)
                  lineTo(kx - d, ky)
                  close()
                }
                drawPath(diamond, color = KytheraCyan)
              }

              // Playhead line across tracks
              val playX = (currentTimeSec * pxPerSecPx).coerceIn(0f, size.width)
              drawLine(
                color = KytheraCyan.copy(alpha = 0.85f),
                start = Offset(playX, 0f),
                end = Offset(playX, size.height),
                strokeWidth = 2.2f,
              )
            }
          }
        }
      }
    }
  }
}

private fun iconForLayerType(type: LayerType) = when (type) {
  LayerType.VIDEO -> Icons.Filled.Movie
  LayerType.AUDIO -> Icons.Filled.Audiotrack
  LayerType.IMAGE -> Icons.Filled.Image
  LayerType.TEXT -> Icons.Filled.TextFields
  LayerType.SHAPE -> Icons.Filled.AutoAwesome
  LayerType.DRAW -> Icons.Filled.Brush
}
