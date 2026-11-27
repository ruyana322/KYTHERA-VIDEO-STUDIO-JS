package com.example.motion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.motion.model.ElementType
import com.example.motion.model.MotionManifest
import com.example.motion.model.ProjectStylePreset
import com.example.motion.model.SUPPORTED_VECTOR_ICONS
import com.example.motion.model.SceneElement
import com.example.motion.model.SceneSpec
import com.example.motion.viewmodel.EditorBottomTab
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass

@Composable
fun MotionEditorTabsPanel(
  activeTab: EditorBottomTab,
  manifest: MotionManifest,
  selectedScene: SceneSpec?,
  selectedElement: SceneElement?,
  voicePausesCount: Int,
  onSelectTab: (EditorBottomTab) -> Unit,
  onRenameScene: (String, String) -> Unit,
  onUpdateSceneAccent: (String, String) -> Unit,
  onSplitScene: () -> Unit,
  onAddElement: (ElementType) -> Unit,
  onSelectElement: (String) -> Unit,
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
  val glass = LocalKytheraGlass.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(glass.glassElevated)
      .border(1.dp, glass.glassBorder, RoundedCornerShape(16.dp))
      .padding(10.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      EditorBottomTab.entries.forEach { tab ->
        val sel = tab == activeTab
        FilterChip(
          selected = sel,
          onClick = { onSelectTab(tab) },
          label = { Text(tab.label, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium) },
          modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      when (activeTab) {
        EditorBottomTab.ADEGAN -> TabAdeganContent(
          scene = selectedScene,
          selectedElement = selectedElement,
          onRenameScene = onRenameScene,
          onUpdateAccent = onUpdateSceneAccent,
          onSplitScene = onSplitScene,
          onAddElement = onAddElement,
          onSelectElement = onSelectElement,
        )
        EditorBottomTab.TEKS -> TabTeksContent(
          element = selectedElement,
          onUpdateElement = onUpdateElement,
          onDeleteElement = onDeleteElement,
          onAddTextQuick = { onAddElement(ElementType.TEKS) },
        )
        EditorBottomTab.ASET -> TabAsetContent(
          manifest = manifest,
          selectedElement = selectedElement,
          onAddAssets = onAddAssets,
          onUpdateElement = onUpdateElement,
          onAddCharacter = { onAddElement(ElementType.KARAKTER) },
          onAddIcon = { onAddElement(ElementType.IKON) },
        )
        EditorBottomTab.AUDIO -> TabAudioContent(
          manifest = manifest,
          voicePausesCount = voicePausesCount,
          onPickVoiceover = onPickVoiceover,
          onPickMusic = onPickMusic,
          onRemoveMusic = onRemoveMusic,
          onUpdateOffsets = onUpdateOffsets,
        )
        EditorBottomTab.GAYA -> TabGayaContent(
          manifest = manifest,
          onUpdateTitle = onUpdateTitle,
          onSelectStyle = onSelectStyle,
          onUpdateTransitionDur = onUpdateTransitionDur,
        )
      }
    }
  }
}

@Composable
private fun TabAdeganContent(
  scene: SceneSpec?,
  selectedElement: SceneElement?,
  onRenameScene: (String, String) -> Unit,
  onUpdateAccent: (String, String) -> Unit,
  onSplitScene: () -> Unit,
  onAddElement: (ElementType) -> Unit,
  onSelectElement: (String) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  if (scene == null) {
    Text("Belum ada adegan aktif.", color = glass.textSecondary)
    return
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedTextField(
      value = scene.nama,
      onValueChange = { onRenameScene(scene.id, it) },
      label = { Text("Nama Adegan") },
      singleLine = true,
      modifier = Modifier.weight(1f),
    )
    OutlinedButton(onClick = onSplitScene, modifier = Modifier.testTag("btn_pecah_adegan")) {
      Text("Pecah")
    }
  }

  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Aksen:", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
    listOf("#00E5FF", "#5B6BFF", "#00E676", "#FF5252", "#FFB300", "#AA00FF").forEach { hex ->
      val col = Color(android.graphics.Color.parseColor(hex))
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(col)
          .border(if (scene.aksen.equals(hex, true)) 2.5.dp else 1.dp, Color.White, CircleShape)
          .clickable { onUpdateAccent(scene.id, hex) },
      )
    }
  }

  Text("Tambah Elemen ke Adegan:", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    ElementType.entries.forEach { type ->
      OutlinedButton(
        onClick = { onAddElement(type) },
        modifier = Modifier.testTag("add_el_${type.jsonKey}"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(type.label)
      }
    }
  }

  if (scene.elemen.isNotEmpty()) {
    Text("Elemen di Adegan (${scene.elemen.size}):", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      scene.elemen.forEach { el ->
        FilterChip(
          selected = el.id == selectedElement?.id,
          onClick = { onSelectElement(el.id) },
          label = { Text(el.displayLabel()) },
        )
      }
    }
  }
}

@Composable
private fun TabTeksContent(
  element: SceneElement?,
  onUpdateElement: ((SceneElement) -> SceneElement) -> Unit,
  onDeleteElement: () -> Unit,
  onAddTextQuick: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  if (element == null) {
    Button(onClick = onAddTextQuick, modifier = Modifier.fillMaxWidth()) {
      Text("Tambah Elemen Teks")
    }
    return
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text("Edit: ${element.tipe.label} (${element.displayLabel()})", style = MaterialTheme.typography.labelLarge, color = glass.textPrimary)
    IconButton(onClick = onDeleteElement, modifier = Modifier.testTag("btn_hapus_elemen")) {
      Icon(Icons.Filled.Delete, contentDescription = "Hapus Elemen", tint = KytheraCrimson)
    }
  }

  if (element.tipe == ElementType.TEKS || element.tipe == ElementType.PANEL || element.tipe == ElementType.KARTU) {
    OutlinedTextField(
      value = element.isi,
      onValueChange = { txt -> onUpdateElement { it.copy(isi = txt) } },
      label = { Text("Isi Teks") },
      modifier = Modifier.fillMaxWidth().testTag("input_isi_teks"),
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Ukuran ${element.ukuran.toInt()}px", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
    Slider(
      value = element.ukuran,
      onValueChange = { sz -> onUpdateElement { it.copy(ukuran = sz) } },
      valueRange = 24f..140f,
      modifier = Modifier.weight(1f),
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilterChip(
      selected = element.font.equals("inter", true),
      onClick = { onUpdateElement { it.copy(font = "inter") } },
      label = { Text("Inter Black") },
    )
    FilterChip(
      selected = element.font.equals("poppins", true),
      onClick = { onUpdateElement { it.copy(font = "poppins") } },
      label = { Text("Poppins Medium") },
    )
    Spacer(modifier = Modifier.weight(1f))
    Text("Glitch", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
    Switch(
      checked = element.glitch,
      onCheckedChange = { g -> onUpdateElement { it.copy(glitch = g) } },
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    listOf("#FFFFFF", "#00E5FF", "#5B6BFF", "#00E676", "#FFB300", "#FF5252").forEach { hex ->
      val col = Color(android.graphics.Color.parseColor(hex))
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(col)
          .border(if (element.warna.equals(hex, true)) 2.5.dp else 1.dp, KytheraPeriwinkle, CircleShape)
          .clickable { onUpdateElement { it.copy(warna = hex) } },
      )
    }
  }
}

@Composable
private fun TabAsetContent(
  manifest: MotionManifest,
  selectedElement: SceneElement?,
  onAddAssets: () -> Unit,
  onUpdateElement: ((SceneElement) -> SceneElement) -> Unit,
  onAddCharacter: () -> Unit,
  onAddIcon: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onAddAssets, modifier = Modifier.weight(1f).testTag("btn_editor_tambah_gambar")) {
      Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Tambah Gambar")
    }
    OutlinedButton(onClick = onAddCharacter) { Text("+ Karakter") }
    OutlinedButton(onClick = onAddIcon) { Text("+ Ikon") }
  }

  if (manifest.aset.isNotEmpty()) {
    Text("Aset Tersedia (${manifest.aset.size}):", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      manifest.aset.keys.forEach { key ->
        val assigned = selectedElement?.tipe == ElementType.KARAKTER && selectedElement.asetNama == key
        FilterChip(
          selected = assigned,
          onClick = {
            if (selectedElement?.tipe == ElementType.KARAKTER) {
              onUpdateElement { it.copy(asetNama = key) }
            }
          },
          label = { Text(key) },
        )
      }
    }
  }

  if (selectedElement?.tipe == ElementType.IKON) {
    Text("Pilih Ikon Vektor:", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      SUPPORTED_VECTOR_ICONS.forEach { iconName ->
        FilterChip(
          selected = selectedElement.ikonNama == iconName,
          onClick = { onUpdateElement { it.copy(ikonNama = iconName) } },
          label = { Text(iconName) },
        )
      }
    }
  }
}

@Composable
private fun TabAudioContent(
  manifest: MotionManifest,
  voicePausesCount: Int,
  onPickVoiceover: () -> Unit,
  onPickMusic: () -> Unit,
  onRemoveMusic: () -> Unit,
  onUpdateOffsets: (Float, Float) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  Text(
    text = "Voiceover: ${manifest.voiceover.ifBlank { "Belum ada" }} · Jeda suara terdeteksi: $voicePausesCount",
    style = MaterialTheme.typography.bodySmall,
    color = glass.textPrimary,
  )
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onPickVoiceover, modifier = Modifier.weight(1f)) {
      Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Ganti Voiceover")
    }
    OutlinedButton(onClick = onPickMusic, modifier = Modifier.weight(1f)) {
      Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(if (manifest.musik.isBlank()) "Tambah Musik" else "Ganti Musik")
    }
  }
  if (manifest.musik.isNotBlank()) {
    OutlinedButton(onClick = onRemoveMusic) { Text("Hapus Musik (${manifest.musik})") }
  }
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("Offset: ${String.format("%.1f", manifest.voiceOffsetDetik)}d", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
    Slider(
      value = manifest.voiceOffsetDetik,
      onValueChange = { v -> onUpdateOffsets(v, manifest.tailDetik) },
      valueRange = 0f..3f,
      modifier = Modifier.weight(1f),
    )
    Text("Tail: ${String.format("%.1f", manifest.tailDetik)}d", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
    Slider(
      value = manifest.tailDetik,
      onValueChange = { tl -> onUpdateOffsets(manifest.voiceOffsetDetik, tl) },
      valueRange = 0.5f..4f,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun TabGayaContent(
  manifest: MotionManifest,
  onUpdateTitle: (String) -> Unit,
  onSelectStyle: (ProjectStylePreset) -> Unit,
  onUpdateTransitionDur: (Float) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  OutlinedTextField(
    value = manifest.judul,
    onValueChange = onUpdateTitle,
    label = { Text("Judul Proyek") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
  )
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ProjectStylePreset.entries.forEach { preset ->
      FilterChip(
        selected = manifest.gaya == preset,
        onClick = { onSelectStyle(preset) },
        label = { Text(preset.label) },
      )
    }
  }
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("Transisi Wipe: ${String.format("%.2f", manifest.transisiDurasiDetik)} dtk", style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
    Slider(
      value = manifest.transisiDurasiDetik,
      onValueChange = onUpdateTransitionDur,
      valueRange = 0.1f..1.0f,
      modifier = Modifier.weight(1f),
    )
  }
}
