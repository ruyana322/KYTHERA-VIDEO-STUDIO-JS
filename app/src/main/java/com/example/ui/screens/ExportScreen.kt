package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExportFormat
import com.example.model.ExportResolution
import com.example.ui.components.GlassCard
import com.example.ui.components.SquircleBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass
import com.example.viewmodel.StudioUiState
import kotlin.math.roundToInt

@Composable
fun ExportScreen(
  uiState: StudioUiState,
  onBack: () -> Unit,
  onUpdateResolution: (ExportResolution) -> Unit,
  onUpdateFps: (Int) -> Unit,
  onUpdateBitrate: (Int) -> Unit,
  onUpdateFormat: (ExportFormat) -> Unit,
  onStartExport: () -> Unit,
  onCancelExport: () -> Unit,
  onExportJson: () -> Unit,
  bottomContentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val glass = LocalKytheraGlass.current
  val proj = uiState.activeProject
  val cfg = uiState.exportConfig
  val estMb = ((proj?.durationSec ?: 8f) * cfg.bitrateMbps / 8f).roundToInt().coerceAtLeast(1)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 18.dp,
      end = 18.dp,
      top = 10.dp,
      bottom = bottomContentPadding.calculateBottomPadding() + 20.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("export_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glass.textPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Render & Export", style = MaterialTheme.typography.headlineSmall, color = glass.textPrimary)
          Text(
            text = proj?.name ?: "Kythera Project",
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary,
          )
        }
        SquircleBadge(
          icon = Icons.Filled.MovieCreation,
          contentDescription = "Export",
          startColor = KytheraPeriwinkle,
          endColor = KytheraCyan,
        )
      }
    }

    // Progress Ring Glass Card (inspired by reference 2 & 4 circular chart)
    item {
      GlassCard(cornerRadius = 22.dp, elevated = true, contentPadding = 16.dp) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val strokeW = 10.dp.toPx()
              drawArc(
                color = glass.textPrimary.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
              )
              val pct = if (uiState.isExporting) uiState.exportProgress else if (uiState.exportCompleteMessage != null) 1f else 0.72f
              drawArc(
                brush = Brush.sweepGradient(listOf(KytheraPeriwinkle, KytheraCyan, KytheraViolet, KytheraPeriwinkle)),
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = if (uiState.isExporting) "${(uiState.exportProgress * 100).roundToInt()}%"
                else if (uiState.exportCompleteMessage != null) "100%"
                else cfg.resolution.label,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary,
              )
              Text(
                text = if (uiState.isExporting) "Rendering" else "${cfg.fps} FPS",
                fontSize = 10.sp,
                color = glass.textSecondary,
              )
            }
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${cfg.resolution.label} · ${cfg.format.label} (${cfg.format.codecSummary})",
              style = MaterialTheme.typography.titleMedium,
              color = glass.textPrimary,
            )
            Text(
              text = "Bitrate ${cfg.bitrateMbps} Mbps · Est. ~$estMb MB",
              fontFamily = JetBrainsMonoFamily,
              fontSize = 11.sp,
              color = glass.textSecondary,
            )
            Text(
              text = uiState.exportCompleteMessage ?: (uiState.diagnostics?.hardwareEncoders?.firstOrNull() ?: "MediaCodec Hardware Ready"),
              fontSize = 11.5.sp,
              color = if (uiState.exportCompleteMessage != null) KytheraMint else KytheraPeriwinkle,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }

    // Resolution & FPS Selection
    item {
      GlassCard(cornerRadius = 20.dp, contentPadding = 14.dp) {
        Text("Resolution", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ExportResolution.entries.forEach { res ->
            val sel = res == cfg.resolution
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onUpdateResolution(res) }
                .testTag("export_res_${res.label}"),
              color = if (sel) KytheraPeriwinkle else glass.glassSurface,
              border = BorderStroke(1.dp, if (sel) KytheraPeriwinkle else glass.subtleBorder),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = res.label,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (sel) Color.White else glass.textPrimary,
                modifier = Modifier.padding(vertical = 10.dp).let { it },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Frame Rate", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(24, 30, 60).forEach { fps ->
            val sel = fps == cfg.fps
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onUpdateFps(fps) }
                .testTag("export_fps_$fps"),
              color = if (sel) KytheraElectricBlue else glass.glassSurface,
              border = BorderStroke(1.dp, if (sel) KytheraElectricBlue else glass.subtleBorder),
              shape = RoundedCornerShape(12.dp),
            ) {
              Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(
                  text = "$fps FPS",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (sel) Color.White else glass.textPrimary,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Target Bitrate", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
          Text(
            text = "${cfg.bitrateMbps} Mbps",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = KytheraPeriwinkle,
          )
        }
        Slider(
          value = cfg.bitrateMbps.toFloat(),
          onValueChange = { onUpdateBitrate(it.roundToInt()) },
          valueRange = 8f..50f,
          colors = SliderDefaults.colors(thumbColor = KytheraPeriwinkle, activeTrackColor = KytheraPeriwinkle),
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text("Container Format", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ExportFormat.entries.forEach { fmt ->
            val sel = fmt == cfg.format
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onUpdateFormat(fmt) }
                .testTag("export_fmt_${fmt.name.lowercase()}"),
              color = if (sel) KytheraViolet else glass.glassSurface,
              border = BorderStroke(1.dp, if (sel) KytheraViolet else glass.subtleBorder),
              shape = RoundedCornerShape(12.dp),
            ) {
              Column(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = fmt.label,
                  fontWeight = FontWeight.Bold,
                  color = if (sel) Color.White else glass.textPrimary,
                )
                Text(
                  text = fmt.codecSummary,
                  fontSize = 10.sp,
                  color = if (sel) Color.White.copy(alpha = 0.85f) else glass.textSecondary,
                )
              }
            }
          }
        }
      }
    }

    // Action Buttons
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (uiState.isExporting) {
          OutlinedButton(
            onClick = onCancelExport,
            border = BorderStroke(1.dp, KytheraCrimson),
            modifier = Modifier.weight(1f).height(50.dp).testTag("export_cancel_button"),
          ) {
            Text("Cancel Render", color = KytheraCrimson, fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = onStartExport,
            colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("export_start_button"),
          ) {
            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Render Video", fontWeight = FontWeight.Bold)
          }
        }
        OutlinedButton(
          onClick = onExportJson,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.height(50.dp).testTag("export_json_button"),
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(17.dp))
          Text(" JSON")
        }
      }
    }
  }
}
