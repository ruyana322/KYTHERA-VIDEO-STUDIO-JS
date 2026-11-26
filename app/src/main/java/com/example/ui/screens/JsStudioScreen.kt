package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JsPresetKind
import com.example.ui.components.GlassCard
import com.example.ui.theme.CinemaStageVoid
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun JsStudioScreen(
  preset: JsPresetKind,
  files: Map<String, String>,
  activeFile: String,
  consoleLogs: List<String>,
  isPlaying: Boolean,
  onBack: () -> Unit,
  onSelectFile: (String) -> Unit,
  onEditContent: (String) -> Unit,
  onAddNewFile: () -> Unit,
  onTogglePreset: () -> Unit,
  onImportZip: () -> Unit,
  onExportZip: () -> Unit,
  onRunRender: () -> Unit,
  onClearConsole: () -> Unit,
  onSendToTimeline: () -> Unit,
  bottomContentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val glass = LocalKytheraGlass.current
  var animTime by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(isPlaying) {
    while (isActive && isPlaying) {
      delay(16L)
      animTime += 0.016f
    }
  }

  val activeCode = files[activeFile].orEmpty()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = 10.dp,
      bottom = bottomContentPadding.calculateBottomPadding() + 20.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("js_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glass.textPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("JS Animation Studio", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
          Text(preset.title, style = MaterialTheme.typography.bodySmall, color = KytheraPeriwinkle)
        }
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onTogglePreset)
            .testTag("js_preset_switch"),
          color = KytheraViolet.copy(alpha = 0.16f),
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = KytheraViolet, modifier = Modifier.size(14.dp))
            Text("Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KytheraViolet)
          }
        }
      }
    }

    // ZIP Import/Export & Send to Timeline Actions
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onImportZip,
          modifier = Modifier.weight(1f).testTag("js_import_zip"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(" Import ZIP", fontSize = 11.5.sp)
        }
        OutlinedButton(
          onClick = onExportZip,
          modifier = Modifier.weight(1f).testTag("js_export_zip"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Filled.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(" Export ZIP", fontSize = 11.5.sp)
        }
        Button(
          onClick = onSendToTimeline,
          colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.testTag("js_send_timeline"),
        ) {
          Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
          Text(" Timeline", fontSize = 11.5.sp)
        }
      }
    }

    // Live Native Canvas Preview of Active Animation Script
    item {
      GlassCard(cornerRadius = 20.dp, elevated = true, contentPadding = 10.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("LIVE PREVIEW (60 FPS NATIVE)", fontFamily = JetBrainsMonoFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = glass.textSecondary)
          Button(
            onClick = onRunRender,
            colors = ButtonDefaults.buttonColors(containerColor = KytheraElectricBlue),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp).testTag("js_render_button"),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(" Render", fontSize = 11.sp)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CinemaStageVoid),
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            if (preset == JsPresetKind.CYBER_PULSE) {
              val ringCount = 5
              for (i in 0 until ringCount) {
                val phase = ((animTime * 0.7f + i * 0.2f) % 1f)
                val rad = 18.dp.toPx() + phase * (size.minDimension * 0.42f)
                val alpha = (1f - phase).coerceIn(0f, 1f)
                drawCircle(
                  brush = Brush.sweepGradient(listOf(KytheraPeriwinkle.copy(alpha = alpha), KytheraCyan.copy(alpha = alpha))),
                  radius = rad,
                  center = center,
                  style = Stroke(width = 4.dp.toPx()),
                )
              }
            } else {
              val bars = 18
              val slotW = (size.width * 0.84f) / bars
              val startX = size.width * 0.08f
              for (i in 0 until bars) {
                val amp = abs(sin(animTime * 4.2f + i * 0.55f))
                val bh = (size.height * 0.72f) * amp + 10f
                drawRoundRect(
                  brush = Brush.verticalGradient(listOf(KytheraCyan, KytheraPeriwinkle, KytheraViolet)),
                  topLeft = Offset(startX + i * slotW + 2f, size.height - 16.dp.toPx() - bh),
                  size = Size((slotW - 6f).coerceAtLeast(4f), bh),
                  cornerRadius = CornerRadius(6f, 6f),
                )
              }
            }
          }
        }
      }
    }

    // File Browser Tabs + Code Editor
    item {
      GlassCard(cornerRadius = 20.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          files.keys.forEach { fileName ->
            val sel = fileName == activeFile
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectFile(fileName) },
              color = if (sel) KytheraPeriwinkle else glass.glassSurface,
              border = BorderStroke(1.dp, glass.subtleBorder),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                text = fileName,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (sel) Color.White else glass.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onAddNewFile),
            color = KytheraMint.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Add, contentDescription = "New File", tint = KytheraMint, modifier = Modifier.size(14.dp))
              Text(" New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KytheraMint)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = activeCode,
          onValueChange = onEditContent,
          textStyle = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = glass.textPrimary,
          ),
          minLines = 7,
          maxLines = 11,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("js_code_editor"),
        )
      }
    }

    // Console Output
    item {
      GlassCard(cornerRadius = 18.dp, contentPadding = 12.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("CONSOLE", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = glass.textSecondary)
          TextButton(onClick = onClearConsole) { Text("Clear", fontSize = 11.sp) }
        }
        if (consoleLogs.isEmpty()) {
          Text("Console empty", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = glass.textMuted)
        } else {
          consoleLogs.forEach { line ->
            Text(
              text = line,
              fontFamily = JetBrainsMonoFamily,
              fontSize = 11.sp,
              color = KytheraElectricBlue,
            )
          }
        }
      }
    }
  }
}
