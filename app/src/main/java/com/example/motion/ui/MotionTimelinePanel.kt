package com.example.motion.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motion.model.MotionManifest
import com.example.motion.model.SceneElement
import com.example.motion.model.SceneSpec
import com.example.ui.theme.KytheraAmber
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MotionTimelinePanel(
  manifest: MotionManifest,
  currentTimeSec: Float,
  selectedScene: SceneSpec?,
  selectedElementId: String?,
  voicePeaks: FloatArray,
  musicPeaks: FloatArray,
  voicePausesSec: List<Float>,
  onScrub: (Float) -> Unit,
  onSelectScene: (String) -> Unit,
  onAdjustSceneBoundary: (String, Float) -> Unit,
  onSplitSceneAtPlayhead: () -> Unit,
  onMergeSceneRight: (String) -> Unit,
  onDuplicateScene: (String) -> Unit,
  onDeleteScene: (String) -> Unit,
  onSelectElement: (String) -> Unit,
  onShiftElementCue: (String, String, Float) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val totalDur = max(2f, manifest.effectiveDurationSec())
  val pxPerSecDp = 34.dp
  val density = LocalDensity.current
  val pxPerSec = with(density) { pxPerSecDp.toPx() }
  val trackWidthDp = pxPerSecDp * totalDur
  val scrollState = rememberScrollState()
  var contextScene by remember { mutableStateOf<SceneSpec?>(null) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(if (glass.isDark) Color(0xFF0E1424) else Color(0xFFEAF0FF))
      .border(1.dp, glass.glassBorder, RoundedCornerShape(16.dp))
      .padding(8.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState),
    ) {
      Column(modifier = Modifier.width(trackWidthDp + 24.dp)) {
        // 1. Penggaris waktu + scrub playhead
        Box(
          modifier = Modifier
            .width(trackWidthDp)
            .height(22.dp)
            .pointerInput(totalDur, pxPerSec) {
              detectTapGestures { pos ->
                onScrub((pos.x / pxPerSec).coerceIn(0f, totalDur))
              }
            }
            .pointerInput(totalDur, pxPerSec) {
              detectDragGestures { change, _ ->
                change.consume()
                onScrub((change.position.x / pxPerSec).coerceIn(0f, totalDur))
              }
            }
            .testTag("timeline_ruler"),
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val maxSec = ceil(totalDur.toDouble()).toInt()
            for (s in 0..maxSec) {
              val x = s * pxPerSec
              val tall = s % 5 == 0
              drawLine(
                color = if (tall) KytheraPeriwinkle else Color.Gray.copy(alpha = 0.5f),
                start = Offset(x, if (tall) 2f else 10f),
                end = Offset(x, size.height),
                strokeWidth = if (tall) 2f else 1f,
              )
            }
          }
          val maxSec = ceil(totalDur.toDouble()).toInt()
          for (s in 0..maxSec step 5) {
            Text(
              text = "${s}d",
              fontSize = 9.sp,
              color = glass.textSecondary,
              modifier = Modifier.offset { IntOffset((s * pxPerSec).roundToInt() + 3, 0) },
            )
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 2. Track Suara (Waveform + Penanda Jeda)
        WaveformTimelineRow(
          label = "Suara",
          widthDp = trackWidthDp,
          heightDp = 24.dp,
          peaks = voicePeaks,
          barColor = KytheraCyan,
          pausesSec = voicePausesSec,
          pxPerSec = pxPerSec,
          totalDur = totalDur,
          offsetSec = manifest.voiceOffsetDetik,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3. Track Musik (Waveform tipis)
        WaveformTimelineRow(
          label = "Musik",
          widthDp = trackWidthDp,
          heightDp = 16.dp,
          peaks = musicPeaks,
          barColor = KytheraMint.copy(alpha = 0.7f),
          pausesSec = emptyList(),
          pxPerSec = pxPerSec,
          totalDur = totalDur,
          offsetSec = 0f,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Track Adegan (ketuk pilih, seret tepi kanan, tekan-lama pecah/gabung/duplikat/hapus)
        Box(
          modifier = Modifier
            .width(trackWidthDp)
            .height(32.dp),
        ) {
          manifest.scenes.forEachIndexed { idx, scene ->
            val leftPx = (scene.mulai * pxPerSec).roundToInt()
            val widthPx = max(28, ((scene.selesai - scene.mulai) * pxPerSec).roundToInt())
            val isSel = scene.id == selectedScene?.id
            val accentColor = remember(scene.aksen) {
              runCatching { Color(AndroidColor.parseColor(scene.aksen)) }.getOrDefault(KytheraPeriwinkle)
            }

            Box(
              modifier = Modifier
                .offset { IntOffset(leftPx, 0) }
                .width(with(density) { widthPx.toDp() })
                .fillMaxHeight()
                .padding(end = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = if (isSel) 0.34f else 0.18f))
                .border(
                  width = if (isSel) 2.dp else 1.dp,
                  color = if (isSel) Color.White else accentColor.copy(alpha = 0.7f),
                  shape = RoundedCornerShape(8.dp),
                )
                .combinedClickable(
                  onClick = { onSelectScene(scene.id) },
                  onLongClick = { contextScene = scene },
                )
                .testTag("timeline_scene_${scene.id}"),
            ) {
              Text(
                text = scene.nama,
                fontSize = 10.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                color = glass.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                  .align(Alignment.CenterStart)
                  .padding(horizontal = 6.dp, vertical = 2.dp),
              )

              // Handle seret tepi kanan untuk menggeser batas antar adegan
              if (idx < manifest.scenes.lastIndex) {
                Box(
                  modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.35f))
                    .pointerInput(scene.id, pxPerSec) {
                      detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        onAdjustSceneBoundary(scene.id, dragAmount / pxPerSec)
                      }
                    },
                )
              }

              DropdownMenu(
                expanded = contextScene?.id == scene.id,
                onDismissRequest = { contextScene = null },
              ) {
                DropdownMenuItem(
                  text = { Text("Pecah di Playhead") },
                  onClick = {
                    contextScene = null
                    onSelectScene(scene.id)
                    onSplitSceneAtPlayhead()
                  },
                )
                if (idx < manifest.scenes.lastIndex) {
                  DropdownMenuItem(
                    text = { Text("Gabung ke Kanan") },
                    onClick = {
                      contextScene = null
                      onMergeSceneRight(scene.id)
                    },
                  )
                }
                DropdownMenuItem(
                  text = { Text("Duplikat Adegan") },
                  onClick = {
                    contextScene = null
                    onDuplicateScene(scene.id)
                  },
                )
                DropdownMenuItem(
                  text = { Text("Hapus Adegan", color = KytheraCrimson) },
                  onClick = {
                    contextScene = null
                    onDeleteScene(scene.id)
                  },
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 5. Baris Elemen (Penanda Cue geser & snap ke jeda suara/playhead)
        if (selectedScene != null) {
          val elems = selectedScene.elemen.take(4)
          elems.forEach { el ->
            ElementCueRow(
              scene = selectedScene,
              element = el,
              trackWidthDp = trackWidthDp,
              pxPerSec = pxPerSec,
              isSelected = el.id == selectedElementId,
              onSelect = { onSelectElement(el.id) },
              onShiftCue = { newAbs -> onShiftElementCue(selectedScene.id, el.id, newAbs) },
            )
          }
        }

        // Garis Playhead vertikal menembus seluruh track
        Box(
          modifier = Modifier
            .offset { IntOffset((currentTimeSec * pxPerSec).roundToInt(), 0) }
            .width(2.dp)
            .height(0.dp),
        )
      }

      // Garis Playhead overlay penuh tinggi
      Box(
        modifier = Modifier
          .offset { IntOffset((currentTimeSec * pxPerSec).roundToInt(), 0) }
          .width(2.dp)
          .height(140.dp)
          .background(KytheraCrimson),
      )
    }
  }
}

@Composable
private fun WaveformTimelineRow(
  label: String,
  widthDp: androidx.compose.ui.unit.Dp,
  heightDp: androidx.compose.ui.unit.Dp,
  peaks: FloatArray,
  barColor: Color,
  pausesSec: List<Float>,
  pxPerSec: Float,
  totalDur: Float,
  offsetSec: Float,
) {
  Box(
    modifier = Modifier
      .width(widthDp)
      .height(heightDp)
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF0A0F1E).copy(alpha = 0.72f)),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      if (peaks.isNotEmpty()) {
        val midY = size.height / 2f
        val startPx = offsetSec * pxPerSec
        val usableW = (size.width - startPx).coerceAtLeast(10f)
        val dx = usableW / peaks.size.toFloat()
        val step = max(1, (peaks.size / 180))
        for (i in peaks.indices step step) {
          val x = startPx + i * dx
          if (x > size.width) break
          val amp = (peaks[i] * size.height * 0.45f).coerceAtLeast(1.5f)
          drawLine(
            color = barColor,
            start = Offset(x, midY - amp),
            end = Offset(x, midY + amp),
            strokeWidth = 2f,
          )
        }
      }
      // Penanda jeda suara untuk snapping
      pausesSec.forEach { p ->
        if (p in 0f..totalDur) {
          val px = p * pxPerSec
          drawLine(
            color = KytheraAmber.copy(alpha = 0.85f),
            start = Offset(px, 0f),
            end = Offset(px, size.height),
            strokeWidth = 2f,
          )
        }
      }
    }
    Text(
      text = label,
      fontSize = 9.sp,
      color = Color.White.copy(alpha = 0.7f),
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 4.dp),
    )
  }
}

@Composable
private fun ElementCueRow(
  scene: SceneSpec,
  element: SceneElement,
  trackWidthDp: androidx.compose.ui.unit.Dp,
  pxPerSec: Float,
  isSelected: Boolean,
  onSelect: () -> Unit,
  onShiftCue: (Float) -> Unit,
) {
  val absCue = (scene.mulai + element.cue).coerceIn(scene.mulai, scene.selesai - 0.1f)
  val cuePx = (absCue * pxPerSec).roundToInt()
  val barPx = max(68, (element.durasi * pxPerSec).roundToInt())

  Box(
    modifier = Modifier
      .width(trackWidthDp)
      .height(22.dp)
      .padding(vertical = 1.dp),
  ) {
    Box(
      modifier = Modifier
        .offset { IntOffset(cuePx, 0) }
        .width(with(LocalDensity.current) { barPx.toDp() })
        .fillMaxHeight()
        .clip(RoundedCornerShape(6.dp))
        .background(if (isSelected) KytheraPeriwinkle else KytheraPeriwinkle.copy(alpha = 0.45f))
        .pointerInput(scene.id, element.id, absCue, pxPerSec) {
          detectTapGestures { onSelect() }
        }
        .pointerInput(scene.id, element.id, absCue, pxPerSec) {
          var runningAbs = absCue
          detectHorizontalDragGestures(
            onDragStart = { runningAbs = scene.mulai + element.cue },
          ) { change, dragAmount ->
            change.consume()
            runningAbs += dragAmount / pxPerSec
            onShiftCue(runningAbs)
          }
        }
        .padding(horizontal = 6.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      Text(
        text = element.displayLabel(),
        fontSize = 9.5.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
