package com.example.motion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.motion.model.RenderQuality
import com.example.motion.model.RenderStatus
import com.example.motion.model.ValidationWarning
import com.example.ui.theme.KytheraAmber
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderExportSheet(
  selectedQuality: RenderQuality,
  warnings: List<ValidationWarning>,
  status: RenderStatus,
  onSelectQuality: (RenderQuality) -> Unit,
  onStartRender: () -> Unit,
  onCancelRender: () -> Unit,
  onOpenVideo: (String) -> Unit,
  onShareVideo: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val hasBlocker = warnings.any { it.isBlocking }
  val isRunning = status is RenderStatus.Preparing || status is RenderStatus.Rendering

  ModalBottomSheet(onDismissRequest = { if (!isRunning) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Filled.Movie, contentDescription = null, tint = KytheraPeriwinkle)
        Column {
          Text("Render Video MP4", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
          Text("1080×1920 Portrait · 30 fps · H.264 + AAC 192 kbps", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        }
      }

      if (warnings.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KytheraAmber.copy(alpha = 0.14f))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          warnings.forEach { w ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = if (w.isBlocking) KytheraCrimson else KytheraAmber,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = w.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (w.isBlocking) KytheraCrimson else glass.textPrimary,
              )
            }
          }
        }
      }

      if (!isRunning && status !is RenderStatus.Completed) {
        Text("Kualitas Bitrate", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
        RenderQuality.entries.forEach { q ->
          val active = selectedQuality == q
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (active) KytheraPeriwinkle.copy(alpha = 0.14f) else Color.Transparent)
              .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) KytheraPeriwinkle else glass.subtleBorder,
                shape = RoundedCornerShape(12.dp),
              )
              .clickable { onSelectQuality(q) }
              .padding(12.dp)
              .testTag("quality_${q.name}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(q.label, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
              Text(q.subtitle, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
            }
            if (active) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KytheraPeriwinkle)
          }
        }
      }

      when (status) {
        is RenderStatus.Preparing -> {
          Text(status.message, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary)
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          OutlinedButton(
            onClick = onCancelRender,
            modifier = Modifier.fillMaxWidth().testTag("btn_batal_render"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Batalkan")
          }
        }
        is RenderStatus.Rendering -> {
          val etaTxt = if (status.etaSeconds > 0) " · sisa ~${status.etaSeconds} dtk" else ""
          Text(
            text = "${status.phaseLabel} Frame ${status.frame}/${status.totalFrames}$etaTxt",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = glass.textPrimary,
          )
          LinearProgressIndicator(
            progress = { status.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedButton(
            onClick = onCancelRender,
            modifier = Modifier.fillMaxWidth().testTag("btn_batal_render"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Batalkan")
          }
        }
        is RenderStatus.Completed -> {
          val mb = String.format("%.1f", status.sizeBytes / (1024f * 1024f))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KytheraMint)
            Text(
              text = "Tersimpan ke Galeri (Movies/KytheraStudio): ${status.fileName} ($mb MB)",
              style = MaterialTheme.typography.bodyMedium,
              color = KytheraMint,
              fontWeight = FontWeight.SemiBold,
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = { onOpenVideo(status.uriString) },
              modifier = Modifier.weight(1f).testTag("btn_buka_video"),
            ) {
              Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Buka")
            }
            OutlinedButton(
              onClick = { onShareVideo(status.uriString) },
              modifier = Modifier.weight(1f).testTag("btn_bagikan_video"),
            ) {
              Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Bagikan")
            }
          }
          OutlinedButton(onClick = onStartRender, modifier = Modifier.fillMaxWidth()) {
            Text("Render Ulang")
          }
        }
        is RenderStatus.Cancelled -> {
          Text(
            text = "Render dibatalkan. Tekan Mulai Render jika ingin memulai ulang dari awal.",
            style = MaterialTheme.typography.bodySmall,
            color = KytheraAmber,
          )
          Button(
            onClick = onStartRender,
            enabled = !hasBlocker,
            modifier = Modifier.fillMaxWidth().testTag("btn_mulai_render"),
          ) {
            Text("Mulai Render MP4")
          }
        }
        is RenderStatus.Failed -> {
          Text(status.message, style = MaterialTheme.typography.bodySmall, color = KytheraCrimson)
          Button(
            onClick = onStartRender,
            enabled = !hasBlocker,
            modifier = Modifier.fillMaxWidth().testTag("btn_mulai_render"),
          ) {
            Text("Coba Render Lagi")
          }
        }
        RenderStatus.Idle -> {
          Button(
            onClick = onStartRender,
            enabled = !hasBlocker,
            colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
            modifier = Modifier.fillMaxWidth().testTag("btn_mulai_render"),
          ) {
            Text("Mulai Render MP4", fontWeight = FontWeight.Bold)
          }
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
    }
  }
}
