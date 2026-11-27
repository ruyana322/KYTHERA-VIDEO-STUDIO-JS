package com.example.motion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.motion.model.ProjectStylePreset
import com.example.motion.viewmodel.WizardDraftState
import com.example.ui.components.GlassCard
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass

@Composable
fun NewProjectWizardScreen(
  wizard: WizardDraftState,
  onCancel: () -> Unit,
  onSetStep: (Int) -> Unit,
  onTitleChange: (String) -> Unit,
  onPickVoiceover: () -> Unit,
  onPickMusic: () -> Unit,
  onPickAssets: () -> Unit,
  onRenameAsset: (String, String) -> Unit,
  onRemoveAsset: (String) -> Unit,
  onSelectStyle: (ProjectStylePreset) -> Unit,
  onFinishAutoDraft: () -> Unit,
) {
  BackHandler {
    if (wizard.step > 1) onSetStep(wizard.step - 1) else onCancel()
  }

  val glass = LocalKytheraGlass.current
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCancel, modifier = Modifier.testTag("btn_wizard_batal")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = glass.textPrimary)
        }
        Column {
          Text("Proyek Baru", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
          Text("Langkah ${wizard.step} dari 3", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        }
      }
      Text(
        text = "1080×1920 · 30 fps",
        style = MaterialTheme.typography.labelSmall,
        color = KytheraPeriwinkle,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Indikator langkah 1-2-3
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(1 to "1. Suara", 2 to "2. Aset", 3 to "3. Gaya").forEach { (stepNum, label) ->
        val active = wizard.step == stepNum
        val done = wizard.step > stepNum
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
              when {
                active -> KytheraPeriwinkle
                done -> KytheraMint.copy(alpha = 0.22f)
                else -> glass.glassSurface
              },
            )
            .clickable {
              if (stepNum == 1 || wizard.voiceFileName.isNotBlank()) onSetStep(stepNum)
            }
            .padding(vertical = 10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) Color.White else glass.textPrimary,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (!wizard.errorMessage.isNullOrBlank()) {
      GlassCard(contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Text(
          text = wizard.errorMessage,
          color = KytheraCrimson,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      when (wizard.step) {
        1 -> WizardStepVoice(
          wizard = wizard,
          onTitleChange = onTitleChange,
          onPickVoiceover = onPickVoiceover,
          onPickMusic = onPickMusic,
        )
        2 -> WizardStepAssets(
          wizard = wizard,
          onPickAssets = onPickAssets,
          onRenameAsset = onRenameAsset,
          onRemoveAsset = onRemoveAsset,
        )
        else -> WizardStepStyle(
          wizard = wizard,
          onSelectStyle = onSelectStyle,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (wizard.step > 1) {
        OutlinedButton(
          onClick = { onSetStep(wizard.step - 1) },
          modifier = Modifier.weight(1f),
        ) {
          Text("Sebelumnya")
        }
      }
      if (wizard.step == 2) {
        TextButton(
          onClick = { onSetStep(3) },
          modifier = Modifier.testTag("btn_wizard_lewati_aset"),
        ) {
          Text("Lewati")
        }
      }
      if (wizard.step < 3) {
        Button(
          onClick = { onSetStep(wizard.step + 1) },
          enabled = wizard.voiceFileName.isNotBlank() && !wizard.isBusy,
          modifier = Modifier
            .weight(1f)
            .testTag("btn_wizard_lanjut"),
        ) {
          Text("Lanjut")
        }
      } else {
        Button(
          onClick = onFinishAutoDraft,
          enabled = wizard.voiceFileName.isNotBlank() && !wizard.isBusy,
          modifier = Modifier
            .weight(1f)
            .testTag("btn_wizard_buat_draf"),
        ) {
          if (wizard.isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(6.dp))
          } else {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text("Buat Draf Otomatis")
        }
      }
    }
  }
}

@Composable
private fun WizardStepVoice(
  wizard: WizardDraftState,
  onTitleChange: (String) -> Unit,
  onPickVoiceover: () -> Unit,
  onPickMusic: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  GlassCard(elevated = true) {
    OutlinedTextField(
      value = wizard.title,
      onValueChange = onTitleChange,
      label = { Text("Judul Proyek") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_judul_wizard"),
    )
    Spacer(modifier = Modifier.height(12.dp))

    Text("Voiceover (Wajib · WAV / MP3 · maks 60 dtk)", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    Button(
      onClick = onPickVoiceover,
      enabled = !wizard.isBusy,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("btn_pilih_voiceover"),
    ) {
      Icon(Icons.Filled.AudioFile, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(if (wizard.voiceFileName.isBlank()) "Pilih File Voiceover" else "Ganti: ${wizard.voiceFileName}")
    }

    if (wizard.voiceFileName.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Durasi: ${String.format("%.1f", wizard.voiceDurationSec)} dtk · Jeda terdeteksi: ${wizard.voicePausesSec.size} · Pecah adegan: ${wizard.sceneSplitSec.size + 1} bagian",
        style = MaterialTheme.typography.bodySmall,
        color = KytheraMint,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.height(6.dp))
      MiniWaveformStrip(peaks = wizard.voicePeaks, barColor = KytheraPeriwinkle)
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text("Musik Latar (Opsional · auto-ducking & fade-out)", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedButton(
      onClick = onPickMusic,
      enabled = !wizard.isBusy,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("btn_pilih_musik"),
    ) {
      Icon(Icons.Filled.MusicNote, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(if (wizard.musicFileName.isBlank()) "Pilih Musik Latar (Opsional)" else "Musik: ${wizard.musicFileName}")
    }
    if (wizard.musicPeaks.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      MiniWaveformStrip(peaks = wizard.musicPeaks, barColor = KytheraMint)
    }
  }
}

@Composable
private fun WizardStepAssets(
  wizard: WizardDraftState,
  onPickAssets: () -> Unit,
  onRenameAsset: (String, String) -> Unit,
  onRemoveAsset: (String) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  GlassCard(elevated = true) {
    Text("Aset Gambar / Karakter (Opsional)", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
    Text(
      text = "PNG transparan otomatis dipotong ke bounding box alpha dan diperkecil jika > 2000 px.",
      style = MaterialTheme.typography.bodySmall,
      color = glass.textSecondary,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = onPickAssets,
      enabled = !wizard.isBusy,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("btn_pilih_aset_wizard"),
    ) {
      Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Pilih Beberapa Gambar")
    }
    Spacer(modifier = Modifier.height(10.dp))

    wizard.assets.forEach { item ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = item.shortName,
          onValueChange = { onRenameAsset(item.id, it) },
          label = { Text("Nama pendek (${item.width}×${item.height})") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onRemoveAsset(item.id) }) {
          Icon(Icons.Filled.Delete, contentDescription = "Hapus aset", tint = KytheraCrimson)
        }
      }
    }
  }
}

@Composable
private fun WizardStepStyle(
  wizard: WizardDraftState,
  onSelectStyle: (ProjectStylePreset) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  GlassCard(elevated = true) {
    Text("Pilih Gaya Visual", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
    Text(
      text = "Resolusi terkunci 1080×1920 portrait, 30 fps. Auto-Draft akan membagi adegan otomatis dari jeda suara >= 0.6 detik.",
      style = MaterialTheme.typography.bodySmall,
      color = glass.textSecondary,
    )
    Spacer(modifier = Modifier.height(10.dp))

    ProjectStylePreset.entries.forEach { preset ->
      val selected = wizard.gaya == preset
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(if (selected) KytheraPeriwinkle.copy(alpha = 0.14f) else Color.Transparent)
          .border(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) KytheraPeriwinkle else glass.subtleBorder,
            shape = RoundedCornerShape(14.dp),
          )
          .clickable { onSelectStyle(preset) }
          .padding(12.dp)
          .testTag("gaya_${preset.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(preset.label, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
          Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
        }
        if (selected) {
          Icon(Icons.Filled.CheckCircle, contentDescription = "Terpilih", tint = KytheraPeriwinkle)
        }
      }
    }
  }
}

@Composable
private fun MiniWaveformStrip(peaks: FloatArray, barColor: Color) {
  Canvas(
    modifier = Modifier
      .fillMaxWidth()
      .height(42.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFF0B1020)),
  ) {
    if (peaks.isEmpty()) return@Canvas
    val step = maxOf(1, peaks.size / 90)
    val count = peaks.size / step
    if (count <= 0) return@Canvas
    val dx = size.width / count.toFloat()
    val midY = size.height / 2f
    for (i in 0 until count) {
      val amp = (peaks[i * step] * size.height * 0.44f).coerceAtLeast(3f)
      val x = i * dx + dx * 0.5f
      drawLine(
        color = barColor,
        start = Offset(x, midY - amp),
        end = Offset(x, midY + amp),
        strokeWidth = (dx * 0.6f).coerceIn(1.5f, 4f),
      )
    }
  }
}
