package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawPointNorm
import com.example.ui.components.GlassCard
import com.example.ui.components.PreviewCanvas
import com.example.ui.components.TimelinePanel
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass
import com.example.viewmodel.StudioUiState
import java.util.Locale

@Composable
fun EditorScreen(
  uiState: StudioUiState,
  onBackHome: () -> Unit,
  onRenameProject: (String) -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onCycleQuality: () -> Unit,
  onCycleAspect: () -> Unit,
  onOpenExport: () -> Unit,
  onTogglePlay: () -> Unit,
  onScrub: (Float) -> Unit,
  onZoomDelta: (Float) -> Unit,
  onSelectLayer: (String) -> Unit,
  onToggleVisibility: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onShiftLayerStart: (String, Float) -> Unit,
  onPickMedia: () -> Unit,
  onAddAudio: () -> Unit,
  onAddText: () -> Unit,
  onAddShape: () -> Unit,
  onToggleDrawMode: () -> Unit,
  onOpenEffects: () -> Unit,
  onOpenAnimation: () -> Unit,
  onOpenLayerSettings: () -> Unit,
  onRequestMediaLoad: (String, Boolean) -> Unit,
  onCommitDrawStroke: (List<DrawPointNorm>) -> Unit,
  onDragSelectedLayer: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackHome)

  val glass = LocalKytheraGlass.current
  val project = uiState.activeProject ?: return
  var showRenameDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 12.dp, vertical = 6.dp),
  ) {
    // TOP BAR
    GlassCard(
      cornerRadius = 18.dp,
      elevated = true,
      contentPadding = 6.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        IconButton(
          onClick = onBackHome,
          modifier = Modifier.testTag("editor_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Home",
            tint = glass.textPrimary,
          )
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable { showRenameDialog = true }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
          Text(
            text = project.name,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${project.resolutionLabel} · ${project.fps} FPS · ${project.aspectRatio.label}",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = glass.textSecondary,
          )
        }

        IconButton(
          onClick = onUndo,
          enabled = uiState.canUndo,
          modifier = Modifier.testTag("editor_undo_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (uiState.canUndo) glass.textPrimary else glass.textMuted,
          )
        }

        IconButton(
          onClick = onRedo,
          enabled = uiState.canRedo,
          modifier = Modifier.testTag("editor_redo_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            tint = if (uiState.canRedo) glass.textPrimary else glass.textMuted,
          )
        }

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onCycleQuality)
            .testTag("editor_quality_pill"),
          color = KytheraPeriwinkle.copy(alpha = 0.14f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = uiState.previewQuality.label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = KytheraPeriwinkle,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          )
        }

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onOpenExport)
            .testTag("editor_export_button"),
          color = KytheraElectricBlue,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Filled.Upload, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(14.dp))
            Text("Export", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // ADAPTIVE WORKSPACE (Portrait vertical split vs Tablet landscape split)
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
    ) {
      val isWide = maxWidth >= 680.dp
      if (isWide) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Column(
            modifier = Modifier
              .weight(0.48f)
              .fillMaxHeight(),
          ) {
            PreviewCanvas(
              project = project,
              currentTimeSec = uiState.currentTimeSec,
              selectedLayerId = uiState.selectedLayer?.id,
              previewQuality = uiState.previewQuality,
              isDrawMode = uiState.isDrawMode,
              loadedImages = uiState.loadedImages,
              onRequestMediaLoad = onRequestMediaLoad,
              onCommitDrawStroke = onCommitDrawStroke,
              onDragSelectedLayer = onDragSelectedLayer,
              modifier = Modifier.weight(1f),
            )
            TransportControlsBar(
              currentTimeSec = uiState.currentTimeSec,
              totalDurationSec = project.durationSec,
              aspectLabel = project.aspectRatio.label,
              isPlaying = uiState.isPlaying,
              onRewind = { onScrub(0f) },
              onTogglePlay = onTogglePlay,
              onCycleAspect = onCycleAspect,
            )
          }

          TimelinePanel(
            project = project,
            currentTimeSec = uiState.currentTimeSec,
            selectedLayerId = uiState.selectedLayer?.id,
            zoom = uiState.timelineZoom,
            onScrub = onScrub,
            onZoomDelta = onZoomDelta,
            onSelectLayer = onSelectLayer,
            onToggleVisibility = onToggleVisibility,
            onToggleLock = onToggleLock,
            onShiftLayerStart = onShiftLayerStart,
            modifier = Modifier
              .weight(0.52f)
              .fillMaxHeight(),
          )
        }
      } else {
        Column(modifier = Modifier.fillMaxSize()) {
          PreviewCanvas(
            project = project,
            currentTimeSec = uiState.currentTimeSec,
            selectedLayerId = uiState.selectedLayer?.id,
            previewQuality = uiState.previewQuality,
            isDrawMode = uiState.isDrawMode,
            loadedImages = uiState.loadedImages,
            onRequestMediaLoad = onRequestMediaLoad,
            onCommitDrawStroke = onCommitDrawStroke,
            onDragSelectedLayer = onDragSelectedLayer,
            modifier = Modifier.weight(0.50f),
          )
          TransportControlsBar(
            currentTimeSec = uiState.currentTimeSec,
            totalDurationSec = project.durationSec,
            aspectLabel = project.aspectRatio.label,
            isPlaying = uiState.isPlaying,
            onRewind = { onScrub(0f) },
            onTogglePlay = onTogglePlay,
            onCycleAspect = onCycleAspect,
          )
          Spacer(modifier = Modifier.height(6.dp))
          TimelinePanel(
            project = project,
            currentTimeSec = uiState.currentTimeSec,
            selectedLayerId = uiState.selectedLayer?.id,
            zoom = uiState.timelineZoom,
            onScrub = onScrub,
            onZoomDelta = onZoomDelta,
            onSelectLayer = onSelectLayer,
            onToggleVisibility = onToggleVisibility,
            onToggleLock = onToggleLock,
            onShiftLayerStart = onShiftLayerStart,
            modifier = Modifier.weight(0.50f),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // EDITOR BOTTOM TOOL DOCK (Media, Audio, Text, Shape, Draw, Effects, Animation, Settings)
    GlassCard(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars),
      cornerRadius = 22.dp,
      elevated = true,
      contentPadding = 6.dp,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        EditorToolButton("Media", Icons.Filled.PermMedia, testTag = "tool_media", onClick = onPickMedia)
        EditorToolButton("Audio", Icons.Filled.Audiotrack, testTag = "tool_audio", onClick = onAddAudio)
        EditorToolButton("Text", Icons.Filled.TextFields, testTag = "tool_text", onClick = onAddText)
        EditorToolButton("Shape", Icons.Filled.Category, testTag = "tool_shape", onClick = onAddShape)
        EditorToolButton("Draw", Icons.Filled.Brush, highlighted = uiState.isDrawMode, testTag = "tool_draw", onClick = onToggleDrawMode)
        EditorToolButton("Effects", Icons.Filled.AutoAwesome, testTag = "tool_effects", onClick = onOpenEffects)
        EditorToolButton("Animate", Icons.Filled.Animation, testTag = "tool_animate", onClick = onOpenAnimation)
        EditorToolButton("Settings", Icons.Filled.Tune, testTag = "tool_layer_settings", onClick = onOpenLayerSettings)
      }
    }
  }

  if (showRenameDialog) {
    var draftName by remember(project.name) { mutableStateOf(project.name) }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text("Rename Project") },
      text = {
        OutlinedTextField(
          value = draftName,
          onValueChange = { draftName = it },
          singleLine = true,
          label = { Text("Project Name") },
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onRenameProject(draftName)
            showRenameDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun TransportControlsBar(
  currentTimeSec: Float,
  totalDurationSec: Float,
  aspectLabel: String,
  isPlaying: Boolean,
  onRewind: () -> Unit,
  onTogglePlay: () -> Unit,
  onCycleAspect: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  GlassCard(
    cornerRadius = 16.dp,
    contentPadding = 6.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        IconButton(onClick = onRewind, modifier = Modifier.size(36.dp).testTag("transport_rewind")) {
          Icon(Icons.Filled.SkipPrevious, contentDescription = "Jump to start", tint = glass.textPrimary)
        }
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(KytheraPeriwinkle, KytheraCyan)))
            .clickable(onClick = onTogglePlay)
            .testTag("transport_play_pause"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
          )
        }
        Text(
          text = "${formatTimecode(currentTimeSec)} / ${formatTimecode(totalDurationSec)}",
          fontFamily = JetBrainsMonoFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = glass.textPrimary,
        )
      }

      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .clickable(onClick = onCycleAspect)
          .testTag("transport_aspect_button"),
        color = glass.glassElevated,
        border = BorderStroke(1.dp, glass.glassBorder),
        shape = RoundedCornerShape(50),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect Ratio", tint = KytheraPeriwinkle, modifier = Modifier.size(14.dp))
          Text(
            text = aspectLabel,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = glass.textPrimary,
          )
        }
      }
    }
  }
}

@Composable
private fun EditorToolButton(
  label: String,
  icon: ImageVector,
  highlighted: Boolean = false,
  testTag: String,
  onClick: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val fg = if (highlighted) Color.White else glass.textPrimary
  Column(
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .width(64.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(
        if (highlighted) KytheraPeriwinkle else glass.textPrimary.copy(alpha = 0.04f),
      )
      .clickable(onClick = onClick)
      .padding(vertical = 7.dp, horizontal = 4.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(imageVector = icon, contentDescription = label, tint = fg, modifier = Modifier.size(19.dp))
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      fontSize = 10.5.sp,
      fontWeight = FontWeight.SemiBold,
      color = fg,
      maxLines = 1,
    )
  }
}

private fun formatTimecode(sec: Float): String {
  val s = sec.coerceAtLeast(0f)
  val mins = (s / 60f).toInt()
  val rem = s - mins * 60f
  return String.format(Locale.US, "%02d:%05.2f", mins, rem)
}
