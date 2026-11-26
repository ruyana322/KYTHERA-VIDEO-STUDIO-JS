package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimationProperty
import com.example.model.EasingCurve
import com.example.model.LayerTrack
import com.example.model.evaluateLayerAt
import com.example.ui.components.GlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationSheet(
  layer: LayerTrack,
  currentTimeSec: Float,
  activeProperty: AnimationProperty,
  onSelectProperty: (AnimationProperty) -> Unit,
  onSetKeyframeValue: (AnimationProperty, Float, Float) -> Unit,
  onToggleKeyframe: (AnimationProperty) -> Unit,
  onUpdateEasing: (EasingCurve) -> Unit,
  onToggleMotionBlur: (Boolean, Float) -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val eval = evaluateLayerAt(layer, currentTimeSec)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = glass.glassElevated,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text("Keyframe & Motion Graph", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
          Text(
            text = "${layer.name} · t=${String.format("%.2fs", currentTimeSec)}",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = glass.textSecondary,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close Animation Editor", tint = glass.textPrimary)
        }
      }

      // Property Selector Tabs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AnimationProperty.entries.forEach { prop ->
          val active = prop == activeProperty
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable { onSelectProperty(prop) }
              .testTag("anim_prop_${prop.name.lowercase()}"),
            color = if (active) KytheraPeriwinkle else glass.glassSurface,
            border = BorderStroke(1.dp, if (active) KytheraPeriwinkle else glass.subtleBorder),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = prop.label,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (active) Color.White else glass.textPrimary,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
          }
        }
      }

      // Active Property Sliders + Keyframe Toggle
      GlassCard(cornerRadius = 18.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${activeProperty.label} Value",
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
          )
          if (activeProperty != AnimationProperty.ANCHOR) {
            Button(
              onClick = { onToggleKeyframe(activeProperty) },
              colors = ButtonDefaults.buttonColors(containerColor = KytheraElectricBlue),
              modifier = Modifier.testTag("anim_toggle_keyframe"),
            ) {
              Icon(Icons.Filled.Diamond, contentDescription = null, modifier = Modifier.size(15.dp))
              Text(" Keyframe", fontSize = 11.5.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeProperty) {
          AnimationProperty.POSITION -> {
            PropertySlider("X Offset", eval.posX, -180f..180f, "px") {
              onSetKeyframeValue(AnimationProperty.POSITION, it, eval.posY)
            }
            PropertySlider("Y Offset", eval.posY, -180f..180f, "px") {
              onSetKeyframeValue(AnimationProperty.POSITION, eval.posX, it)
            }
          }
          AnimationProperty.ROTATION -> {
            PropertySlider("Angle", eval.rotationDeg, -360f..360f, "°") {
              onSetKeyframeValue(AnimationProperty.ROTATION, it, 0f)
            }
          }
          AnimationProperty.SCALE -> {
            PropertySlider("Scale", eval.scalePct, 20f..220f, "%") {
              onSetKeyframeValue(AnimationProperty.SCALE, it, 0f)
            }
          }
          AnimationProperty.OPACITY -> {
            PropertySlider("Opacity", eval.opacityPct, 0f..100f, "%") {
              onSetKeyframeValue(AnimationProperty.OPACITY, it, 0f)
            }
          }
          AnimationProperty.ANCHOR -> {
            PropertySlider("Anchor X", layer.anchorX * 100f, 0f..100f, "%") {
              onSetKeyframeValue(AnimationProperty.ANCHOR, it / 100f, layer.anchorY)
            }
            PropertySlider("Anchor Y", layer.anchorY * 100f, 0f..100f, "%") {
              onSetKeyframeValue(AnimationProperty.ANCHOR, layer.anchorX, it / 100f)
            }
          }
        }

        // Keyframe Mini-Timeline Strip
        val kfTimes = layer.allKeyframeTimes()
        Text(
          text = "Keyframes: " + kfTimes.joinToString(" · ") { "${String.format("%.2f", it)}s" },
          fontFamily = JetBrainsMonoFamily,
          fontSize = 10.5.sp,
          color = KytheraCyan,
        )
      }

      // Interactive Cubic-Bezier Graph Editor
      GlassCard(cornerRadius = 18.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Easing Graph Editor", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
          Text(
            text = layer.easing.label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = KytheraPeriwinkle,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          EasingCurve.PRESETS.forEach { preset ->
            val selected = layer.easing.label == preset.label
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onUpdateEasing(preset) },
              color = if (selected) KytheraPeriwinkle else glass.glassSurface,
              border = BorderStroke(1.dp, glass.subtleBorder),
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = preset.label,
                fontSize = 11.sp,
                color = if (selected) Color.White else glass.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        BezierCurveGraphEditor(
          easing = layer.easing,
          onUpdateEasing = onUpdateEasing,
        )
      }

      // Motion Blur Controls
      GlassCard(cornerRadius = 18.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text("Motion Blur", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
            Text("Velocity-based frame shutter blend", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
          }
          Switch(
            checked = layer.motionBlurEnabled,
            onCheckedChange = { onToggleMotionBlur(it, layer.motionBlurShutter) },
          )
        }
        if (layer.motionBlurEnabled) {
          PropertySlider("Shutter Angle", layer.motionBlurShutter, 45f..360f, "°") {
            onToggleMotionBlur(true, it)
          }
        }
      }
    }
  }
}

@Composable
private fun PropertySlider(
  label: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  unit: String,
  onValueChange: (Float) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary)
    Text(
      text = "${value.roundToInt()}$unit",
      fontFamily = JetBrainsMonoFamily,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = glass.textPrimary,
    )
  }
  Slider(
    value = value.coerceIn(range.start, range.endInclusive),
    onValueChange = onValueChange,
    valueRange = range,
    colors = SliderDefaults.colors(thumbColor = KytheraPeriwinkle, activeTrackColor = KytheraPeriwinkle),
  )
}

@Composable
private fun BezierCurveGraphEditor(
  easing: EasingCurve,
  onUpdateEasing: (EasingCurve) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  var activeHandle = ""
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(170.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFF0C101C)),
  ) {
    Canvas(
      modifier = Modifier
        .matchParentSize()
        .pointerInput(easing) {
          val pad = 22.dp.toPx()
          val gw = (size.width - pad * 2).coerceAtLeast(1f)
          val gh = (size.height - pad * 2).coerceAtLeast(1f)
          detectDragGestures(
            onDragStart = { pos ->
              val nx = ((pos.x - pad) / gw).coerceIn(0f, 1f)
              val ny = ((size.height - pad - pos.y) / gh).coerceIn(-0.35f, 1.35f)
              val d1 = hypot(nx - easing.p1x, ny - easing.p1y)
              val d2 = hypot(nx - easing.p2x, ny - easing.p2y)
              activeHandle = if (d1 <= d2) "p1" else "p2"
            },
            onDrag = { change, _ ->
              change.consume()
              val nx = ((change.position.x - pad) / gw).coerceIn(0f, 1f)
              val ny = ((size.height - pad - change.position.y) / gh).coerceIn(-0.35f, 1.35f)
              val next = if (activeHandle == "p1") {
                easing.copy(label = "Custom", p1x = nx, p1y = ny)
              } else {
                easing.copy(label = "Custom", p2x = nx, p2y = ny)
              }
              onUpdateEasing(next)
            },
          )
        },
    ) {
      val pad = 22.dp.toPx()
      val gw = size.width - pad * 2
      val gh = size.height - pad * 2

      for (i in 0..4) {
        val gx = pad + gw * i / 4f
        val gy = pad + gh * i / 4f
        drawLine(Color.White.copy(alpha = 0.08f), Offset(gx, pad), Offset(gx, size.height - pad))
        drawLine(Color.White.copy(alpha = 0.08f), Offset(pad, gy), Offset(size.width - pad, gy))
      }

      val p0 = Offset(pad, size.height - pad)
      val p3 = Offset(size.width - pad, pad)
      val p1 = Offset(pad + easing.p1x * gw, size.height - pad - easing.p1y * gh)
      val p2 = Offset(pad + easing.p2x * gw, size.height - pad - easing.p2y * gh)

      val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
      drawLine(KytheraCyan.copy(alpha = 0.55f), p0, p1, strokeWidth = 2f, pathEffect = dash)
      drawLine(KytheraCyan.copy(alpha = 0.55f), p3, p2, strokeWidth = 2f, pathEffect = dash)

      val path = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
      }
      drawPath(path, color = KytheraPeriwinkle, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

      drawCircle(color = KytheraCyan, radius = 7.dp.toPx(), center = p1)
      drawCircle(color = KytheraCyan, radius = 7.dp.toPx(), center = p2)
      drawCircle(color = Color.White, radius = 3.dp.toPx(), center = p1)
      drawCircle(color = Color.White, radius = 3.dp.toPx(), center = p2)
    }
    Text(
      text = "Drag P1 / P2 handles",
      fontFamily = JetBrainsMonoFamily,
      fontSize = 10.sp,
      color = glass.textMuted,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(8.dp),
    )
  }
}
