package com.example.motion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.motion.model.ElementType
import com.example.motion.model.ProjectStylePreset
import com.example.motion.model.SceneElement
import com.example.motion.viewmodel.EditorBottomTab
import com.example.motion.viewmodel.MotionStudioUiState
import com.example.ui.theme.CinemaStageVoid
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass

@Composable
fun MotionEditorScreen(
  uiState: MotionStudioUiState,
  onBack: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onOpenRender: () -> Unit,
  onTogglePlay: () -> Unit,
  onToggleLoopScene: () -> Unit,
  onToggleSafeZones: () -> Unit,
  onPreviewTap: (Float, Float) -> Unit,
  onPreviewDrag: (Float, Float) -> Unit,
  onPreviewPinch: (Float) -> Unit,
  onScrub: (Float) -> Unit,
  onSelectScene: (String) -> Unit,
  onAdjustSceneBoundary: (String, Float) -> Unit,
  onSplitSceneAtPlayhead: () -> Unit,
  onMergeSceneRight: (String) -> Unit,
  onDuplicateScene: (String) -> Unit,
  onDeleteScene: (String) -> Unit,
  onSelectElement: (String) -> Unit,
  onShiftElementCue: (String, String, Float) -> Unit,
  onSelectTab: (EditorBottomTab) -> Unit,
  onRenameScene: (String, String) -> Unit,
  onUpdateSceneAccent: (String, String) -> Unit,
  onAddElement: (ElementType) -> Unit,
  onUpdateElement: ((SceneElement) -> SceneElement) -> Unit,
  onDeleteElement: () -> Unit,
  onAddAssets: () -> Unit,
  onPickVoiceover: () -> Unit,
  onPickMusic: () -> Unit,
  onRemoveMusic: () -> Unit,
  onUpdateOffsets: (Float, Float) -> Unit,
  onUpdateTitle: (String) -> Unit,
  onSelectStyle: (ProjectStylePreset) -> Unit,
  onUpdateTransitionDur: (Float) -> Unit,
) {
  BackHandler(onBack = onBack)

  val glass = LocalKytheraGlass.current
  val totalDur = uiState.manifest.effectiveDurationSec()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // (E) Toolbar atas: Kembali, Judul & Status Simpan, Undo, Redo, Render
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("btn_editor_kembali")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = glass.textPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = uiState.manifest.judul,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = uiState.saveStatus.label,
            style = MaterialTheme.typography.labelSmall,
            color = KytheraMint,
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onUndo, enabled = uiState.canUndo, modifier = Modifier.testTag("btn_undo")) {
          Icon(
            Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (uiState.canUndo) glass.textPrimary else glass.textMuted,
          )
        }
        IconButton(onClick = onRedo, enabled = uiState.canRedo, modifier = Modifier.testTag("btn_redo")) {
          Icon(
            Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            tint = if (uiState.canRedo) glass.textPrimary else glass.textMuted,
          )
        }
        Button(
          onClick = onOpenRender,
          colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
          modifier = Modifier.testTag("btn_buka_render"),
        ) {
          Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Render", fontWeight = FontWeight.Bold)
        }
      }
    }

    // (A) Preview 9:16 + (D) Manipulasi langsung + Kontrol Play / Loop Adegan / Zona Aman
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      val bmp = uiState.previewBitmap
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .aspectRatio(9f / 16f)
          .clip(RoundedCornerShape(18.dp))
          .background(CinemaStageVoid)
          .border(1.5.dp, glass.glassBorder, RoundedCornerShape(18.dp))
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              val nx = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
              val ny = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
              onPreviewTap(nx, ny)
            }
          }
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              if (pan.x != 0f || pan.y != 0f) {
                onPreviewDrag(pan.x / size.width.toFloat(), pan.y / size.height.toFloat())
              }
              if (zoom != 1f) {
                onPreviewPinch(zoom)
              }
            }
          }
          .testTag("canvas_preview_9_16"),
      ) {
        if (bmp != null && !bmp.isRecycled) {
          key(uiState.previewRevision) {
            val imgBmp = remember(uiState.previewRevision) { bmp.asImageBitmap() }
            Image(
              bitmap = imgBmp,
              contentDescription = "Preview Frame",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }

    // Kontrol Transport Preview
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(onClick = onTogglePlay, modifier = Modifier.testTag("btn_play_pause")) {
          Icon(
            imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (uiState.isPlaying) "Jeda" else "Putar",
            tint = KytheraPeriwinkle,
          )
        }
        Text(
          text = "${formatTime(uiState.currentTimeSec)} / ${formatTime(totalDur)}",
          style = MaterialTheme.typography.labelSmall,
          color = glass.textPrimary,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = uiState.loopActiveSceneOnly,
          onClick = onToggleLoopScene,
          leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text("Loop") },
          modifier = Modifier.testTag("chip_loop_adegan"),
        )
        FilterChip(
          selected = uiState.showSafeZones,
          onClick = onToggleSafeZones,
          leadingIcon = { Icon(Icons.Filled.CropFree, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text("Zona Aman") },
          modifier = Modifier.testTag("chip_zona_aman"),
        )
      }
    }

    // (B) Timeline Multi-Track
    MotionTimelinePanel(
      manifest = uiState.manifest,
      currentTimeSec = uiState.currentTimeSec,
      selectedScene = uiState.selectedScene,
      selectedElementId = uiState.selectedElementId,
      voicePeaks = uiState.voicePeaks,
      musicPeaks = uiState.musicPeaks,
      voicePausesSec = uiState.voicePausesSec,
      onScrub = onScrub,
      onSelectScene = onSelectScene,
      onAdjustSceneBoundary = onAdjustSceneBoundary,
      onSplitSceneAtPlayhead = onSplitSceneAtPlayhead,
      onMergeSceneRight = onMergeSceneRight,
      onDuplicateScene = onDuplicateScene,
      onDeleteScene = onDeleteScene,
      onSelectElement = onSelectElement,
      onShiftElementCue = onShiftElementCue,
    )

    // (C) Panel Bawah 5 Tab (Adegan | Teks | Aset | Audio | Gaya)
    MotionEditorTabsPanel(
      activeTab = uiState.activeTab,
      manifest = uiState.manifest,
      selectedScene = uiState.selectedScene,
      selectedElement = uiState.selectedElement,
      voicePausesCount = uiState.voicePausesSec.size,
      onSelectTab = onSelectTab,
      onRenameScene = onRenameScene,
      onUpdateSceneAccent = onUpdateSceneAccent,
      onSplitScene = onSplitSceneAtPlayhead,
      onAddElement = onAddElement,
      onSelectElement = onSelectElement,
      onUpdateElement = onUpdateElement,
      onDeleteElement = onDeleteElement,
      onAddAssets = onAddAssets,
      onPickVoiceover = onPickVoiceover,
      onPickMusic = onPickMusic,
      onRemoveMusic = onRemoveMusic,
      onUpdateOffsets = onUpdateOffsets,
      onUpdateTitle = onUpdateTitle,
      onSelectStyle = onSelectStyle,
      onUpdateTransitionDur = onUpdateTransitionDur,
    )
  }
}

private fun formatTime(sec: Float): String {
  val clamped = sec.coerceAtLeast(0f)
  val mins = (clamped / 60f).toInt()
  val rem = clamped % 60f
  return String.format("%02d:%04.1f", mins, rem)
}
