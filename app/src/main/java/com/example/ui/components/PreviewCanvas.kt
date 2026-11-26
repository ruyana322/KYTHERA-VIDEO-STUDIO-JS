package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawPointNorm
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.PreviewQuality
import com.example.model.ShapeKind
import com.example.model.StudioProject
import com.example.model.evaluateLayerAt
import com.example.ui.theme.CinemaStageGrid
import com.example.ui.theme.CinemaStageVoid
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.PlusJakartaSansFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PreviewCanvas(
  project: StudioProject,
  currentTimeSec: Float,
  selectedLayerId: String?,
  previewQuality: PreviewQuality,
  isDrawMode: Boolean,
  loadedImages: Map<String, ImageBitmap>,
  onRequestImageLoad: (String) -> Unit,
  onCommitDrawStroke: (List<DrawPointNorm>) -> Unit,
  modifier: Modifier = Modifier,
) {
  val textMeasurer = rememberTextMeasurer()
  val activeStroke = remember { mutableStateListOf<DrawPointNorm>() }

  LaunchedEffect(project.layers) {
    project.layers.forEach { l ->
      if (l.type == LayerType.IMAGE && !l.mediaUri.isNullOrBlank()) {
        onRequestImageLoad(l.mediaUri)
      }
    }
  }

  val ratio = project.aspectRatio.wRatio.toFloat() / project.aspectRatio.hRatio.toFloat()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    val stageShape = RoundedCornerShape(20.dp)
    val fillWidthFirst = maxWidth / maxHeight < ratio
    val frameModifier = if (fillWidthFirst) {
      Modifier
        .fillMaxWidth()
        .aspectRatio(ratio)
    } else {
      Modifier
        .fillMaxHeight()
        .aspectRatio(ratio)
    }

    Box(
      modifier = frameModifier
        .clip(stageShape)
        .background(CinemaStageVoid)
        .border(1.5.dp, Color.White.copy(alpha = 0.25f), stageShape),
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .then(
            if (isDrawMode) {
              Modifier.pointerInput(isDrawMode) {
                detectDragGestures(
                  onDragStart = { offset ->
                    activeStroke.clear()
                    activeStroke.add(
                      DrawPointNorm(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        (offset.y / size.height).coerceIn(0f, 1f),
                      ),
                    )
                  },
                  onDrag = { change, _ ->
                    change.consume()
                    val pos = change.position
                    activeStroke.add(
                      DrawPointNorm(
                        (pos.x / size.width).coerceIn(0f, 1f),
                        (pos.y / size.height).coerceIn(0f, 1f),
                      ),
                    )
                  },
                  onDragEnd = {
                    if (activeStroke.size >= 2) {
                      onCommitDrawStroke(activeStroke.toList())
                    }
                    activeStroke.clear()
                  },
                  onDragCancel = { activeStroke.clear() },
                )
              }
            } else {
              Modifier
            },
          ),
      ) {
        drawStageBackdrop()

        // Draw layers bottom-to-top (reverse list order so index 0 is foreground)
        project.layers.asReversed().forEach { layer ->
          if (!layer.visible) return@forEach
          if (currentTimeSec < layer.startSec || currentTimeSec > layer.endSec) return@forEach

          drawStudioLayer(
            layer = layer,
            timeSec = currentTimeSec,
            isSelected = layer.id == selectedLayerId,
            qualityScale = previewQuality.stepScale,
            textMeasurer = textMeasurer,
            imageBitmap = layer.mediaUri?.let { loadedImages[it] },
          )
        }

        // Live in-progress freehand stroke while drawing
        if (activeStroke.size >= 2) {
          val livePath = Path().apply {
            moveTo(activeStroke.first().x * size.width, activeStroke.first().y * size.height)
            for (i in 1 until activeStroke.size) {
              lineTo(activeStroke[i].x * size.width, activeStroke[i].y * size.height)
            }
          }
          drawPath(
            path = livePath,
            color = KytheraCyan,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
        }
      }

      if (isDrawMode) {
        Surface(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp),
          color = KytheraPeriwinkle.copy(alpha = 0.88f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "DRAW MODE · Drag to sketch",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

private fun DrawScope.drawStageBackdrop() {
  drawRect(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFF151E34), CinemaStageVoid),
      center = center,
      radius = size.maxDimension * 0.7f,
    ),
  )
  val step = size.minDimension / 6f
  var x = step
  while (x < size.width) {
    drawLine(CinemaStageGrid.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    x += step
  }
  var y = step
  while (y < size.height) {
    drawLine(CinemaStageGrid.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    y += step
  }
}

private fun DrawScope.drawStudioLayer(
  layer: LayerTrack,
  timeSec: Float,
  isSelected: Boolean,
  qualityScale: Float,
  textMeasurer: TextMeasurer,
  imageBitmap: ImageBitmap?,
) {
  val eval = evaluateLayerAt(layer, timeSec)
  val alpha = (eval.opacityPct / 100f).coerceIn(0f, 1f)
  if (alpha <= 0.01f) return

  val glowAmt = layer.effects.firstOrNull { it.id == "electric_glow" || it.id == "neon_bloom" }?.value ?: 0f
  val rgbSplit = layer.effects.firstOrNull { it.id == "chromatic_shift" }?.value ?: 0f
  val dropShadow = layer.effects.firstOrNull { it.id == "drop_shadow" }?.value ?: 0f
  val waveAmt = layer.effects.firstOrNull { it.id == "wave_warp" }?.value ?: 0f
  val oscSpeed = layer.effects.firstOrNull { it.id == "oscillate" }?.value ?: 0f
  val shakeAmt = layer.effects.firstOrNull { it.id == "camera_shake" }?.value ?: 0f
  val particleDensity = layer.effects.firstOrNull { it.id == "cyber_particles" }?.value ?: 0f
  val vignetteAmt = layer.effects.firstOrNull { it.id == "vignette" }?.value ?: 0f

  val oscY = if (oscSpeed > 0f) sin(timeSec * oscSpeed * 2.2f) * 14f else 0f
  val shakeX = if (shakeAmt > 0f) sin(timeSec * 37f) * shakeAmt else 0f
  val shakeY = if (shakeAmt > 0f) cos(timeSec * 29f) * shakeAmt else 0f

  val pivot = Offset(
    size.width * layer.anchorX + eval.posX + shakeX,
    size.height * layer.anchorY + eval.posY + oscY + shakeY,
  )
  val layerScale = (eval.scalePct / 100f).coerceIn(0.1f, 4f)
  val baseColor = Color(layer.colorHex).copy(alpha = alpha)

  if (particleDensity > 0f) {
    val count = (particleDensity * qualityScale).roundToInt().coerceIn(4, 50)
    for (i in 0 until count) {
      val px = ((sin(timeSec * 1.1f + i * 2.13f) * 0.5f + 0.5f) * size.width)
      val py = ((cos(timeSec * 0.85f + i * 1.71f) * 0.5f + 0.5f) * size.height)
      drawCircle(
        color = KytheraCyan.copy(alpha = alpha * 0.55f),
        radius = (2f + (i % 3)) * qualityScale,
        center = Offset(px, py),
      )
    }
  }

  translate(left = eval.posX + shakeX, top = eval.posY + oscY + shakeY) {
    rotate(degrees = eval.rotationDeg, pivot = Offset(size.width * layer.anchorX, size.height * layer.anchorY)) {
      scale(scale = layerScale, pivot = Offset(size.width * layer.anchorX, size.height * layer.anchorY)) {
        when (layer.type) {
          LayerType.SHAPE -> {
            drawShapeLayer(layer.shapeKind, baseColor, glowAmt, rgbSplit, waveAmt, timeSec, alpha)
          }
          LayerType.TEXT -> {
            val style = TextStyle(
              fontFamily = PlusJakartaSansFamily,
              fontWeight = FontWeight.Bold,
              fontSize = (layer.fontSizeSp * 0.85f).sp,
              color = baseColor,
              letterSpacing = 1.5.sp,
            )
            val measured = textMeasurer.measure(layer.textContent, style = style)
            val topLeft = Offset(
              center.x - measured.size.width / 2f,
              center.y - measured.size.height / 2f,
            )
            if (dropShadow > 0f) {
              drawText(
                textMeasurer = textMeasurer,
                text = layer.textContent,
                topLeft = topLeft + Offset(dropShadow * 0.35f, dropShadow * 0.45f),
                style = style.copy(color = Color.Black.copy(alpha = 0.55f * alpha)),
              )
            }
            if (rgbSplit > 0f) {
              drawText(
                textMeasurer = textMeasurer,
                text = layer.textContent,
                topLeft = topLeft - Offset(rgbSplit, 0f),
                style = style.copy(color = Color(0xFFFF3D71).copy(alpha = 0.55f * alpha)),
              )
              drawText(
                textMeasurer = textMeasurer,
                text = layer.textContent,
                topLeft = topLeft + Offset(rgbSplit, 0f),
                style = style.copy(color = KytheraCyan.copy(alpha = 0.55f * alpha)),
              )
            }
            if (glowAmt > 0f) {
              drawCircle(
                brush = Brush.radialGradient(
                  listOf(baseColor.copy(alpha = 0.35f * alpha), Color.Transparent),
                  center = center,
                  radius = measured.size.width * 0.75f + glowAmt * 2f,
                ),
                radius = measured.size.width * 0.75f + glowAmt * 2f,
                center = center,
              )
            }
            drawText(textMeasurer = textMeasurer, text = layer.textContent, topLeft = topLeft, style = style)
          }
          LayerType.IMAGE -> {
            val boxW = size.width * 0.56f
            val boxH = size.height * 0.42f
            val tl = Offset(center.x - boxW / 2f, center.y - boxH / 2f)
            if (imageBitmap != null) {
              drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                dstSize = IntSize(boxW.roundToInt(), boxH.roundToInt()),
                alpha = alpha,
              )
            } else {
              drawRoundRect(
                brush = Brush.linearGradient(listOf(KytheraPeriwinkle.copy(alpha = alpha), KytheraCyan.copy(alpha = alpha))),
                topLeft = tl,
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(18f, 18f),
              )
            }
          }
          LayerType.VIDEO -> {
            val w = size.width * 0.72f
            val h = size.height * 0.46f
            val tl = Offset(center.x - w / 2f, center.y - h / 2f)
            drawRoundRect(
              brush = Brush.linearGradient(
                listOf(KytheraElectricBlue.copy(alpha = 0.45f * alpha), KytheraCyan.copy(alpha = 0.3f * alpha)),
              ),
              topLeft = tl,
              size = Size(w, h),
              cornerRadius = CornerRadius(20f, 20f),
            )
            drawRoundRect(
              color = KytheraCyan.copy(alpha = 0.65f * alpha),
              topLeft = tl,
              size = Size(w, h),
              cornerRadius = CornerRadius(20f, 20f),
              style = Stroke(width = 2.dp.toPx()),
            )
          }
          LayerType.DRAW -> {
            layer.drawStrokes.forEach { stroke ->
              if (stroke.size >= 2) {
                val p = Path().apply {
                  moveTo(stroke.first().x * size.width, stroke.first().y * size.height)
                  for (i in 1 until stroke.size) {
                    lineTo(stroke[i].x * size.width, stroke[i].y * size.height)
                  }
                }
                drawPath(
                  path = p,
                  color = baseColor,
                  style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
              }
            }
          }
          LayerType.AUDIO -> {
            val bars = 18
            val barW = size.width * 0.65f / bars
            val startX = center.x - (bars * barW) / 2f
            for (i in 0 until bars) {
              val amp = (kotlin.math.abs(sin(timeSec * 6f + i * 0.7f)) * 28f + 6f)
              drawRoundRect(
                color = baseColor.copy(alpha = 0.75f * alpha),
                topLeft = Offset(startX + i * barW + 2f, size.height * 0.86f - amp),
                size = Size((barW - 4f).coerceAtLeast(2f), amp),
                cornerRadius = CornerRadius(4f, 4f),
              )
            }
          }
        }
      }
    }
  }

  if (vignetteAmt > 0f) {
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = (vignetteAmt / 100f) * 0.72f)),
        center = center,
        radius = size.maxDimension * 0.68f,
      ),
    )
  }

  if (isSelected && layer.type != LayerType.AUDIO) {
    val boxSize = size.minDimension * 0.44f
    drawRoundRect(
      color = KytheraCyan.copy(alpha = 0.75f),
      topLeft = Offset(pivot.x - boxSize / 2f, pivot.y - boxSize / 2f),
      size = Size(boxSize, boxSize),
      cornerRadius = CornerRadius(10f, 10f),
      style = Stroke(width = 1.5.dp.toPx()),
    )
  }
}

private fun DrawScope.drawShapeLayer(
  kind: ShapeKind,
  color: Color,
  glowAmt: Float,
  rgbSplit: Float,
  waveAmt: Float,
  timeSec: Float,
  alpha: Float,
) {
  val r = size.minDimension * 0.24f
  if (glowAmt > 0f) {
    drawCircle(
      brush = Brush.radialGradient(
        listOf(color.copy(alpha = 0.48f * alpha), Color.Transparent),
        center = center,
        radius = r * 1.6f + glowAmt * 3f,
      ),
      radius = r * 1.6f + glowAmt * 3f,
      center = center,
    )
  }

  when (kind) {
    ShapeKind.CIRCLE -> {
      drawCircle(
        brush = Brush.linearGradient(listOf(color, KytheraCyan.copy(alpha = alpha))),
        radius = r,
        center = center,
      )
    }
    ShapeKind.ROUNDED_RECT -> {
      val s = r * 1.85f
      drawRoundRect(
        brush = Brush.linearGradient(listOf(color, KytheraPeriwinkle.copy(alpha = alpha))),
        topLeft = Offset(center.x - s / 2f, center.y - s / 2f),
        size = Size(s, s * 0.72f),
        cornerRadius = CornerRadius(24f, 24f),
      )
    }
    ShapeKind.RING -> {
      if (rgbSplit > 0f) {
        drawCircle(
          color = Color(0xFFFF3D71).copy(alpha = 0.5f * alpha),
          radius = r * 1.1f,
          center = center - Offset(rgbSplit, 0f),
          style = Stroke(width = 6.dp.toPx()),
        )
      }
      drawCircle(
        brush = Brush.sweepGradient(listOf(color, KytheraCyan.copy(alpha = alpha), color)),
        radius = r * 1.1f,
        center = center,
        style = Stroke(width = 8.dp.toPx()),
      )
    }
    ShapeKind.STAR -> {
      val path = Path()
      val pts = 10
      for (i in 0 until pts) {
        val rad = if (i % 2 == 0) r * 1.15f else r * 0.52f
        val angle = (i * PI / 5.0 - PI / 2.0)
        val x = center.x + (cos(angle) * rad).toFloat()
        val y = center.y + (sin(angle) * rad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      path.close()
      drawPath(path, brush = Brush.linearGradient(listOf(color, KytheraCyan.copy(alpha = alpha))))
    }
    ShapeKind.WAVE -> {
      val path = Path()
      val amp = 18f + waveAmt * 1.6f
      val steps = 40
      val startX = size.width * 0.12f
      val width = size.width * 0.76f
      for (i in 0..steps) {
        val frac = i / steps.toFloat()
        val x = startX + frac * width
        val y = center.y + sin(frac * 10f + timeSec * 4.5f) * amp
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      drawPath(
        path = path,
        brush = Brush.horizontalGradient(listOf(color, KytheraCyan.copy(alpha = alpha))),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}
