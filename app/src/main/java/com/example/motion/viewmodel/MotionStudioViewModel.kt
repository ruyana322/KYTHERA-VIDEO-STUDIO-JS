package com.example.motion.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motion.audio.AudioDecoder
import com.example.motion.io.BitmapAssetProcessor
import com.example.motion.io.ZipImportOutcome
import com.example.motion.io.ZipProjectStorage
import com.example.motion.model.CaptionLine
import com.example.motion.model.ElementType
import com.example.motion.model.KineticWord
import com.example.motion.model.ManifestSerializer
import com.example.motion.model.MotionManifest
import com.example.motion.model.ProjectStylePreset
import com.example.motion.model.ProjectSummary
import com.example.motion.model.RenderQuality
import com.example.motion.model.RenderStatus
import com.example.motion.model.SafeZones
import com.example.motion.model.SceneElement
import com.example.motion.model.SceneSpec
import com.example.motion.model.ValidationWarning
import com.example.motion.render.MotionRenderer
import com.example.motion.service.RenderForegroundService
import com.example.motion.service.RenderProgressBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class StudioScreen { PROJECT_LIST, WIZARD, EDITOR }
enum class EditorBottomTab(val label: String) {
  ADEGAN("Adegan"),
  TEKS("Teks"),
  ASET("Aset"),
  AUDIO("Audio"),
  GAYA("Gaya"),
}

enum class SaveStatus(val label: String) {
  TERSIMPAN("Tersimpan"),
  MENYIMPAN("Menyimpan…"),
  BELUM_DISIMPAN("Belum disimpan"),
}

data class WizardAssetItem(
  val id: String,
  val uri: Uri,
  val shortName: String,
  val savedFileName: String,
  val width: Int,
  val height: Int,
  val wasCropped: Boolean,
)

data class WizardDraftState(
  val tempProjectId: String = "",
  val step: Int = 1, // 1: Suara, 2: Aset, 3: Gaya
  val title: String = "Proyek Motion Baru",
  val voiceFileName: String = "",
  val voiceDurationSec: Float = 0f,
  val voicePeaks: FloatArray = FloatArray(0),
  val voicePausesSec: List<Float> = emptyList(),
  val sceneSplitSec: List<Float> = emptyList(),
  val musicFileName: String = "",
  val musicPeaks: FloatArray = FloatArray(0),
  val assets: List<WizardAssetItem> = emptyList(),
  val gaya: ProjectStylePreset = ProjectStylePreset.GELAP_TECH,
  val errorMessage: String? = null,
  val isBusy: Boolean = false,
)

data class MotionStudioUiState(
  val screen: StudioScreen = StudioScreen.PROJECT_LIST,
  val projects: List<ProjectSummary> = emptyList(),
  val activeProjectId: String? = null,
  val manifest: MotionManifest = MotionManifest(),
  val previewBitmap: Bitmap? = null,
  val previewRevision: Int = 0,
  val isPlaying: Boolean = false,
  val currentTimeSec: Float = 0f,
  val loopActiveSceneOnly: Boolean = false,
  val showSafeZones: Boolean = false,
  val selectedSceneId: String? = null,
  val selectedElementId: String? = null,
  val activeTab: EditorBottomTab = EditorBottomTab.ADEGAN,
  val saveStatus: SaveStatus = SaveStatus.TERSIMPAN,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val voicePeaks: FloatArray = FloatArray(0),
  val musicPeaks: FloatArray = FloatArray(0),
  val voicePausesSec: List<Float> = emptyList(),
  val wizard: WizardDraftState = WizardDraftState(),
  val renderQuality: RenderQuality = RenderQuality.NORMAL,
  val showRenderSheet: Boolean = false,
  val validationWarnings: List<ValidationWarning> = emptyList(),
  val renderStatus: RenderStatus = RenderStatus.Idle,
  val isImportingZip: Boolean = false,
  val darkGlassMode: Boolean = false,
  val toastMessage: String? = null,
) {
  val selectedScene: SceneSpec?
    get() = manifest.scenes.firstOrNull { it.id == selectedSceneId }
      ?: manifest.scenes.lastOrNull { currentTimeSec >= it.mulai }
      ?: manifest.scenes.firstOrNull()

  val selectedElement: SceneElement?
    get() = selectedScene?.elemen?.firstOrNull { it.id == selectedElementId }
}

class MotionStudioViewModel(application: Application) : AndroidViewModel(application) {

  private val storage = ZipProjectStorage(application)
  private val _uiState = MutableStateFlow(MotionStudioUiState())
  val uiState: StateFlow<MotionStudioUiState> = _uiState.asStateFlow()

  private var activeRenderer: MotionRenderer? = null
  private var playbackJob: Job? = null
  private var autosaveJob: Job? = null
  private var previewBitmapPaused: Bitmap? = null
  private var previewBitmapPlay: Bitmap? = null

  private val undoStack = ArrayDeque<MotionManifest>(MAX_UNDO)
  private val redoStack = ArrayDeque<MotionManifest>(MAX_UNDO)

  init {
    refreshProjectList()
    viewModelScope.launch {
      RenderProgressBus.status.collect { status ->
        _uiState.update { it.copy(renderStatus = status) }
      }
    }
  }

  fun refreshProjectList() {
    viewModelScope.launch(Dispatchers.IO) {
      val list = storage.listProjects()
      _uiState.update { it.copy(projects = list) }
    }
  }

  fun toggleThemeMode() {
    _uiState.update { it.copy(darkGlassMode = !it.darkGlassMode) }
  }

  fun consumeToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  fun showToast(msg: String) {
    _uiState.update { it.copy(toastMessage = msg) }
  }

  fun backToProjectList() {
    pausePlayback()
    flushAutosaveNow()
    activeRenderer?.release()
    activeRenderer = null
    refreshProjectList()
    _uiState.update {
      it.copy(
        screen = StudioScreen.PROJECT_LIST,
        showRenderSheet = false,
      )
    }
  }

  // ==================== LAYAR 1: DAFTAR PROYEK & ZIP ====================

  fun createAndOpenSample51s() {
    viewModelScope.launch(Dispatchers.IO) {
      val (id, manifest) = storage.createSampleProject51s()
      val list = storage.listProjects()
      withContext(Dispatchers.Main) {
        _uiState.update { it.copy(projects = list) }
        openProjectInEditor(id, manifest)
        showToast("Contoh proyek 51 detik (7 adegan) berhasil dimuat.")
      }
    }
  }

  fun importZipUri(uri: Uri) {
    viewModelScope.launch {
      _uiState.update { it.copy(isImportingZip = true) }
      val outcome = withContext(Dispatchers.IO) {
        storage.importZip(getApplication<Application>().contentResolver, uri)
      }
      _uiState.update { it.copy(isImportingZip = false) }
      when (outcome) {
        is ZipImportOutcome.Success -> {
          refreshProjectList()
          openProjectInEditor(outcome.projectId, outcome.manifest)
          showToast("Berhasil mengimpor '${outcome.manifest.judul}'.")
        }
        is ZipImportOutcome.Failure -> {
          showToast(outcome.errorMessage)
        }
      }
    }
  }

  fun exportProjectToZipUri(projectId: String, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      val ok = storage.exportProjectZip(projectId, getApplication<Application>().contentResolver, uri)
      withContext(Dispatchers.Main) {
        showToast(if (ok) "Proyek berhasil diekspor sebagai ZIP." else "Gagal mengekspor ZIP.")
      }
    }
  }

  fun renameProject(projectId: String, newTitle: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val updated = storage.renameProject(projectId, newTitle)
      val list = storage.listProjects()
      withContext(Dispatchers.Main) {
        _uiState.update { st ->
          st.copy(
            projects = list,
            manifest = if (st.activeProjectId == projectId && updated != null) updated else st.manifest,
          )
        }
        showToast("Judul proyek diperbarui.")
      }
    }
  }

  fun duplicateProject(projectId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val copyId = storage.duplicateProject(projectId)
      val list = storage.listProjects()
      withContext(Dispatchers.Main) {
        _uiState.update { it.copy(projects = list) }
        if (copyId != null) showToast("Proyek berhasil diduplikasi.")
      }
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      storage.deleteProject(projectId)
      val list = storage.listProjects()
      withContext(Dispatchers.Main) {
        _uiState.update { it.copy(projects = list) }
        showToast("Proyek dihapus.")
      }
    }
  }

  // ==================== LAYAR 2: WIZARD PROYEK BARU (3 LANGKAH) ====================

  fun startNewProjectWizard() {
    val tempId = storage.newProjectId()
    storage.projectDir(tempId).apply {
      mkdirs()
      File(this, "aset").mkdirs()
    }
    _uiState.update {
      it.copy(
        screen = StudioScreen.WIZARD,
        wizard = WizardDraftState(tempProjectId = tempId, step = 1),
      )
    }
  }

  fun cancelWizard() {
    val tid = _uiState.value.wizard.tempProjectId
    if (tid.isNotBlank()) {
      viewModelScope.launch(Dispatchers.IO) { storage.deleteProject(tid) }
    }
    _uiState.update { it.copy(screen = StudioScreen.PROJECT_LIST) }
  }

  fun setWizardStep(step: Int) {
    _uiState.update {
      it.copy(wizard = it.wizard.copy(step = step.coerceIn(1, 3), errorMessage = null))
    }
  }

  fun updateWizardTitle(title: String) {
    _uiState.update { it.copy(wizard = it.wizard.copy(title = title)) }
  }

  fun selectWizardVoiceover(uri: Uri, displayName: String) {
    val wiz = _uiState.value.wizard
    if (wiz.tempProjectId.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(wizard = it.wizard.copy(isBusy = true, errorMessage = null)) }
      val result = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val copied = storage.copyAudioUriToProject(resolver, wiz.tempProjectId, uri, displayName)
        val rawDur = AudioDecoder.probeDurationSec(copied)
        val pcm = AudioDecoder.decodeToStereo48k(copied, maxSeconds = 62f)
        val dur = if (pcm.durationSec > 0f) pcm.durationSec else rawDur
        if (dur <= 0.1f) {
          copied.delete()
          return@withContext Result.failure<Triple<String, VoiceAnalysisPayload, FloatArray>>(
            IllegalArgumentException("File audio tidak dapat didekode. Pastikan format WAV atau MP3 valid."),
          )
        }
        val totalWithPadding = ceil((dur + 1.0f + 1.5f).toDouble()).toFloat()
        if (totalWithPadding > 60f) {
          copied.delete()
          return@withContext Result.failure(
            IllegalArgumentException(
              "Durasi voiceover (${String.format("%.1f", dur)} dtk) + offset & tail melebihi batas maksimal 60 detik.",
            ),
          )
        }
        val peaks = AudioDecoder.computeWaveformPeaks(pcm, bucketsPerSec = 18)
        val analysis = AudioDecoder.detectSilenceAndPauses(pcm, offsetSec = 1.0f)
        Result.success(
          Triple(
            copied.name,
            VoiceAnalysisPayload(dur, analysis.pausesSec, analysis.sceneSplitSec),
            peaks,
          ),
        )
      }
      result.onSuccess { (fileName, info, peaks) ->
        _uiState.update {
          it.copy(
            wizard = it.wizard.copy(
              isBusy = false,
              voiceFileName = fileName,
              voiceDurationSec = info.dur,
              voicePeaks = peaks,
              voicePausesSec = info.pausesSec,
              sceneSplitSec = info.sceneSplitsSec,
              errorMessage = null,
            ),
          )
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(wizard = it.wizard.copy(isBusy = false, errorMessage = err.message))
        }
      }
    }
  }

  fun selectWizardMusic(uri: Uri, displayName: String) {
    val wiz = _uiState.value.wizard
    if (wiz.tempProjectId.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(wizard = it.wizard.copy(isBusy = true, errorMessage = null)) }
      val res = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val copied = storage.copyAudioUriToProject(resolver, wiz.tempProjectId, uri, displayName)
        val pcm = AudioDecoder.decodeToStereo48k(copied, maxSeconds = 60f)
        val peaks = AudioDecoder.computeWaveformPeaks(pcm, bucketsPerSec = 18)
        copied.name to peaks
      }
      _uiState.update {
        it.copy(
          wizard = it.wizard.copy(
            isBusy = false,
            musicFileName = res.first,
            musicPeaks = res.second,
          ),
        )
      }
    }
  }

  fun addWizardAssets(uris: List<Uri>) {
    val wiz = _uiState.value.wizard
    if (wiz.tempProjectId.isBlank() || uris.isEmpty()) return
    viewModelScope.launch {
      _uiState.update { it.copy(wizard = it.wizard.copy(isBusy = true)) }
      val added = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val asetDir = File(storage.projectDir(wiz.tempProjectId), "aset")
        uris.mapIndexedNotNull { idx, uri ->
          val base = "gambar_${wiz.assets.size + idx + 1}"
          val info = BitmapAssetProcessor.processAndSaveImageFromUri(resolver, uri, asetDir, base)
          info?.let {
            WizardAssetItem(
              id = UUID.randomUUID().toString().take(8),
              uri = uri,
              shortName = base,
              savedFileName = it.fileName,
              width = it.width,
              height = it.height,
              wasCropped = it.wasAlphaCropped,
            )
          }
        }
      }
      _uiState.update {
        it.copy(wizard = it.wizard.copy(isBusy = false, assets = it.wizard.assets + added))
      }
    }
  }

  fun updateWizardAssetName(assetId: String, shortName: String) {
    val cleaned = shortName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
    _uiState.update { st ->
      st.copy(
        wizard = st.wizard.copy(
          assets = st.wizard.assets.map { if (it.id == assetId) it.copy(shortName = cleaned) else it },
        ),
      )
    }
  }

  fun removeWizardAsset(assetId: String) {
    _uiState.update { st ->
      st.copy(
        wizard = st.wizard.copy(assets = st.wizard.assets.filterNot { it.id == assetId }),
      )
    }
  }

  fun setWizardStyle(gaya: ProjectStylePreset) {
    _uiState.update { it.copy(wizard = it.wizard.copy(gaya = gaya)) }
  }

  /**
   * Auto-Draft Wizard: menggunakan deteksi jeda suara (>= 0.6 dtk) untuk memecah adegan,
   * memasang slot teks judul & slot karakter otomatis, lalu membuka Editor.
   */
  fun finalizeWizardAutoDraft() {
    val wiz = _uiState.value.wizard
    if (wiz.voiceFileName.isBlank() || wiz.voiceDurationSec <= 0f) {
      _uiState.update {
        it.copy(wizard = it.wizard.copy(step = 1, errorMessage = "Voiceover wajib dipilih terlebih dahulu."))
      }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(wizard = it.wizard.copy(isBusy = true)) }
      val totalDur = ceil((wiz.voiceDurationSec + 1.0f + 1.5f).toDouble()).toFloat().coerceIn(2f, 60f)
      val asetMap = linkedMapOf<String, String>()
      wiz.assets.forEachIndexed { i, item ->
        val key = item.shortName.ifBlank { "aset_${i + 1}" }
        asetMap[key] = item.savedFileName
      }
      val firstAssetKey = asetMap.keys.firstOrNull()

      // Hitung batas adegan dari deteksi jeda >= 0.6 dtk (atau bagi rata ~6 dtk bila tidak ada jeda panjang)
      val splitCandidates = wiz.sceneSplitSec
        .filter { it > 1.8f && it < totalDur - 1.8f }
        .fold(mutableListOf<Float>()) { acc, t ->
          if (acc.isEmpty() || t - acc.last() >= 2.2f) acc.add(t)
          acc
        }

      val boundaries = mutableListOf(0f)
      if (splitCandidates.isNotEmpty()) {
        boundaries.addAll(splitCandidates)
      } else {
        val count = max(1, (totalDur / 6.5f).toInt()).coerceIn(1, 7)
        val step = totalDur / count.toFloat()
        for (k in 1 until count) boundaries.add(k * step)
      }
      boundaries.add(totalDur)

      val scenes = mutableListOf<SceneSpec>()
      val palette = linkedMapOf<String, String>()
      for (i in 0 until boundaries.size - 1) {
        val start = boundaries[i]
        val end = boundaries[i + 1]
        val id = "sc_${i + 1}"
        val accent = wiz.gaya.accentHexes[i % wiz.gaya.accentHexes.size]
        palette[id] = accent

        val elements = mutableListOf<SceneElement>()
        elements += SceneElement(
          id = "${id}_bg_part",
          tipe = ElementType.PARTIKEL,
          cue = 0f,
          durasi = (end - start),
          warna = accent,
          jumlah = 20,
        )
        elements += SceneElement(
          id = "${id}_panel",
          tipe = ElementType.PANEL,
          cue = 0.15f,
          durasi = 0.5f,
          x = 540f,
          y = 430f,
          lebar = 840f,
          tinggi = 250f,
          radius = 30f,
          warna = accent,
          isi = if (i == 0) wiz.title.trim().ifBlank { "Judul Video" } else "Adegan ${i + 1}",
        )
        elements += SceneElement(
          id = "${id}_teks",
          tipe = ElementType.TEKS,
          cue = 0.35f,
          durasi = 0.55f,
          x = 540f,
          y = 700f,
          ukuran = 62f,
          font = "inter",
          warna = wiz.gaya.textHex,
          isi = if (i == 0) "Ringkasan Pembuka" else "Poin Utama ${i + 1}",
        )
        if (firstAssetKey != null) {
          elements += SceneElement(
            id = "${id}_karakter",
            tipe = ElementType.KARAKTER,
            cue = 0.4f,
            durasi = 0.55f,
            x = 540f,
            y = 1180f,
            tinggi = 500f,
            bob = 12f,
            asetNama = firstAssetKey,
          )
        } else {
          elements += SceneElement(
            id = "${id}_ikon",
            tipe = ElementType.IKON,
            cue = 0.4f,
            durasi = 0.55f,
            x = 540f,
            y = 1140f,
            ukuran = 150f,
            warna = accent,
            ikonNama = if (i == 0) "shield" else "chip",
          )
        }
        if (i == 0) {
          elements += SceneElement(
            id = "${id}_wave",
            tipe = ElementType.GELOMBANG_SUARA,
            cue = 0.8f,
            durasi = (end - start),
            x = 540f,
            y = 1520f,
            lebar = 760f,
            tinggi = 120f,
            jumlah = 24,
            warna = accent,
          )
        }

        scenes += SceneSpec(
          id = id,
          nama = if (i == 0) "1. Pembuka" else "${i + 1}. Bagian ${i + 1}",
          mulai = start,
          selesai = end,
          aksen = accent,
          elemen = elements,
        )
      }

      val manifest = MotionManifest(
        judul = wiz.title.trim().ifBlank { "Proyek Motion Baru" },
        fps = 30,
        lebar = 1080,
        tinggi = 1920,
        voiceover = wiz.voiceFileName,
        musik = wiz.musicFileName,
        voiceOffsetDetik = 1.0f,
        tailDetik = 1.5f,
        durasiVoiceoverDetik = wiz.voiceDurationSec,
        gaya = wiz.gaya,
        transisiDurasiDetik = 0.5f,
        aset = asetMap,
        palette = palette,
        scenes = scenes,
      )

      withContext(Dispatchers.IO) {
        storage.saveProject(wiz.tempProjectId, manifest, generateThumb = true)
      }
      refreshProjectList()
      openProjectInEditor(wiz.tempProjectId, manifest)
      showToast("Draf otomatis dibuat dengan ${scenes.size} adegan.")
    }
  }

  // ==================== LAYAR 3: EDITOR PORTRAIT ====================

  fun openProjectInEditor(projectId: String, preloaded: MotionManifest? = null) {
    pausePlayback()
    undoStack.clear()
    redoStack.clear()
    viewModelScope.launch {
      val manifest = preloaded ?: withContext(Dispatchers.IO) { storage.loadProject(projectId) }
      if (manifest == null) {
        showToast("Gagal membuka proyek.")
        return@launch
      }
      val dir = storage.projectDir(projectId)
      activeRenderer?.release()
      activeRenderer = MotionRenderer(getApplication(), dir, manifest)

      _uiState.update {
        it.copy(
          screen = StudioScreen.EDITOR,
          activeProjectId = projectId,
          manifest = manifest,
          currentTimeSec = 0f,
          selectedSceneId = manifest.scenes.firstOrNull()?.id,
          selectedElementId = manifest.scenes.firstOrNull()?.elemen?.firstOrNull()?.id,
          saveStatus = SaveStatus.TERSIMPAN,
          canUndo = false,
          canRedo = false,
          showRenderSheet = false,
        )
      }

      renderSinglePreviewFrame(0f, highQuality = true)
      analyzeProjectAudioAsync(projectId, manifest)
    }
  }

  private fun analyzeProjectAudioAsync(projectId: String, manifest: MotionManifest) {
    viewModelScope.launch(Dispatchers.IO) {
      val asetDir = File(storage.projectDir(projectId), "aset")
      val voFile = manifest.voiceover.takeIf { it.isNotBlank() }?.let { File(asetDir, it) }
      val musFile = manifest.musik.takeIf { it.isNotBlank() }?.let { File(asetDir, it) }

      val voPcm = if (voFile?.exists() == true) AudioDecoder.decodeToStereo48k(voFile, 60f) else null
      val musPcm = if (musFile?.exists() == true) AudioDecoder.decodeToStereo48k(musFile, 60f) else null

      val voPeaks = voPcm?.let { AudioDecoder.computeWaveformPeaks(it, 18) } ?: FloatArray(0)
      val musPeaks = musPcm?.let { AudioDecoder.computeWaveformPeaks(it, 18) } ?: FloatArray(0)
      val analysis = voPcm?.let { AudioDecoder.detectSilenceAndPauses(it, manifest.voiceOffsetDetik) }

      withContext(Dispatchers.Main) {
        _uiState.update { st ->
          val updatedManifest = if (voPcm != null && voPcm.durationSec > 0f && st.manifest.durasiVoiceoverDetik <= 0f) {
            st.manifest.copy(durasiVoiceoverDetik = voPcm.durationSec)
          } else {
            st.manifest
          }
          st.copy(
            manifest = updatedManifest,
            voicePeaks = voPeaks,
            musicPeaks = musPeaks,
            voicePausesSec = analysis?.pausesSec ?: emptyList(),
          )
        }
      }
    }
  }

  fun togglePlayPause() {
    if (_uiState.value.isPlaying) {
      pausePlayback()
    } else {
      startPlayback()
    }
  }

  private fun startPlayback() {
    playbackJob?.cancel()
    _uiState.update { it.copy(isPlaying = true) }
    playbackJob = viewModelScope.launch {
      var lastNs = System.nanoTime()
      while (isActive && _uiState.value.isPlaying) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0f, 0.12f)
        lastNs = nowNs

        val st = _uiState.value
        val activeSc = st.selectedScene
        val minT = if (st.loopActiveSceneOnly && activeSc != null) activeSc.mulai else 0f
        val maxT = if (st.loopActiveSceneOnly && activeSc != null) activeSc.selesai else st.manifest.effectiveDurationSec()
        var nextT = st.currentTimeSec + dt
        if (nextT >= maxT) nextT = minT

        _uiState.update { it.copy(currentTimeSec = nextT) }
        renderSinglePreviewFrame(nextT, highQuality = false)
        delay(66L) // ~15 fps saat play agar responsif dan hemat baterai
      }
    }
  }

  fun pausePlayback() {
    playbackJob?.cancel()
    playbackJob = null
    if (_uiState.value.isPlaying) {
      _uiState.update { it.copy(isPlaying = false) }
      renderSinglePreviewFrame(_uiState.value.currentTimeSec, highQuality = true)
    }
  }

  fun scrubTo(timeSec: Float) {
    pausePlayback()
    val maxDur = _uiState.value.manifest.effectiveDurationSec()
    val clamped = timeSec.coerceIn(0f, maxDur)
    val sceneAtT = _uiState.value.manifest.scenes.lastOrNull { clamped >= it.mulai }
    _uiState.update {
      it.copy(
        currentTimeSec = clamped,
        selectedSceneId = sceneAtT?.id ?: it.selectedSceneId,
      )
    }
    renderSinglePreviewFrame(clamped, highQuality = true)
  }

  fun toggleLoopActiveScene() {
    _uiState.update { it.copy(loopActiveSceneOnly = !it.loopActiveSceneOnly) }
  }

  fun toggleSafeZones() {
    _uiState.update { it.copy(showSafeZones = !it.showSafeZones) }
    renderSinglePreviewFrame(_uiState.value.currentTimeSec, highQuality = !_uiState.value.isPlaying)
  }

  fun selectEditorTab(tab: EditorBottomTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun selectScene(sceneId: String, jumpToStart: Boolean = true) {
    val sc = _uiState.value.manifest.scenes.firstOrNull { it.id == sceneId } ?: return
    val newT = if (jumpToStart) sc.mulai + 0.05f else _uiState.value.currentTimeSec
    _uiState.update {
      it.copy(
        selectedSceneId = sc.id,
        selectedElementId = sc.elemen.firstOrNull()?.id,
        currentTimeSec = newT,
      )
    }
    renderSinglePreviewFrame(newT, highQuality = !_uiState.value.isPlaying)
  }

  fun selectElement(elementId: String?) {
    _uiState.update { it.copy(selectedElementId = elementId) }
    renderSinglePreviewFrame(_uiState.value.currentTimeSec, highQuality = !_uiState.value.isPlaying)
  }

  fun handlePreviewTap(normX: Float, normY: Float) {
    val renderer = activeRenderer ?: return
    val hit = renderer.findElementAt(_uiState.value.currentTimeSec, normX, normY)
    selectElement(hit?.id)
  }

  fun dragSelectedElement(deltaNormX: Float, deltaNormY: Float) {
    val st = _uiState.value
    val el = st.selectedElement ?: return
    val scId = st.selectedScene?.id ?: return
    val newX = (el.x + deltaNormX * SafeZones.WIDTH).coerceIn(40f, SafeZones.WIDTH - 40f)
    val newY = (el.y + deltaNormY * SafeZones.HEIGHT).coerceIn(60f, SafeZones.HEIGHT - 60f)
    mutateManifest(recordUndo = false) { m ->
      m.copy(
        scenes = m.scenes.map { sc ->
          if (sc.id != scId) sc
          else sc.copy(elemen = sc.elemen.map { if (it.id == el.id) it.copy(x = newX, y = newY) else it })
        },
      )
    }
  }

  fun pinchSelectedElement(scaleFactor: Float) {
    val st = _uiState.value
    val el = st.selectedElement ?: return
    val scId = st.selectedScene?.id ?: return
    val newScale = (el.skala * scaleFactor).coerceIn(0.25f, 3.2f)
    mutateManifest(recordUndo = false) { m ->
      m.copy(
        scenes = m.scenes.map { sc ->
          if (sc.id != scId) sc
          else sc.copy(elemen = sc.elemen.map { if (it.id == el.id) it.copy(skala = newScale) else it })
        },
      )
    }
  }

  fun recordManualUndoCheckpoint(before: MotionManifest) {
    pushUndo(before)
  }

  // ==================== MUTASI ADEGAN & ELEMEN + UNDO/REDO (30 LANGKAH) ====================

  private fun pushUndo(snapshot: MotionManifest) {
    if (undoStack.size >= MAX_UNDO) undoStack.removeFirst()
    undoStack.addLast(snapshot)
    redoStack.clear()
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val prev = undoStack.removeLast()
    redoStack.addLast(_uiState.value.manifest)
    applyManifestDirect(prev)
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val next = redoStack.removeLast()
    undoStack.addLast(_uiState.value.manifest)
    applyManifestDirect(next)
  }

  private fun mutateManifest(recordUndo: Boolean = true, transform: (MotionManifest) -> MotionManifest) {
    val current = _uiState.value.manifest
    val updated = transform(current)
    if (updated == current) return
    if (recordUndo) pushUndo(current)
    applyManifestDirect(updated)
  }

  private fun applyManifestDirect(updated: MotionManifest) {
    activeRenderer?.updateManifest(updated)
    _uiState.update {
      it.copy(
        manifest = updated,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        saveStatus = SaveStatus.BELUM_DISIMPAN,
      )
    }
    renderSinglePreviewFrame(_uiState.value.currentTimeSec, highQuality = !_uiState.value.isPlaying)
    scheduleDebouncedAutosave()
  }

  private fun scheduleDebouncedAutosave() {
    autosaveJob?.cancel()
    autosaveJob = viewModelScope.launch {
      delay(1000L)
      flushAutosaveNow()
    }
  }

  private fun flushAutosaveNow() {
    val pid = _uiState.value.activeProjectId ?: return
    val m = _uiState.value.manifest
    _uiState.update { it.copy(saveStatus = SaveStatus.MENYIMPAN) }
    viewModelScope.launch(Dispatchers.IO) {
      storage.saveProject(pid, m, generateThumb = true)
      withContext(Dispatchers.Main) {
        _uiState.update { it.copy(saveStatus = SaveStatus.TERSIMPAN) }
      }
    }
  }

  fun updateProjectTitleInEditor(newTitle: String) {
    mutateManifest { it.copy(judul = newTitle) }
  }

  fun updateStylePreset(gaya: ProjectStylePreset) {
    mutateManifest { m ->
      m.copy(
        gaya = gaya,
        scenes = m.scenes.mapIndexed { idx, sc ->
          sc.copy(aksen = gaya.accentHexes[idx % gaya.accentHexes.size])
        },
      )
    }
  }

  fun updateTransitionDuration(sec: Float) {
    mutateManifest { it.copy(transisiDurasiDetik = sec.coerceIn(0.1f, 1.2f)) }
  }

  fun updateAudioOffsets(voiceOffset: Float, tailSec: Float) {
    val voDur = _uiState.value.manifest.durasiVoiceoverDetik
    val newTotal = ceil((voDur + voiceOffset + tailSec).toDouble()).toFloat()
    if (newTotal > 60f) {
      showToast("Ditolak: total durasi melebihi batas 60 detik.")
      return
    }
    mutateManifest { m ->
      m.copy(
        voiceOffsetDetik = voiceOffset.coerceIn(0f, 5f),
        tailDetik = tailSec.coerceIn(0f, 5f),
      )
    }
  }

  fun replaceAudioInEditor(uri: Uri, fileName: String, isMusic: Boolean) {
    val pid = _uiState.value.activeProjectId ?: return
    viewModelScope.launch {
      val res = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val copied = storage.copyAudioUriToProject(resolver, pid, uri, fileName)
        val pcm = AudioDecoder.decodeToStereo48k(copied, 62f)
        val dur = pcm.durationSec
        if (!isMusic) {
          val offset = _uiState.value.manifest.voiceOffsetDetik
          val tail = _uiState.value.manifest.tailDetik
          val total = ceil((dur + offset + tail).toDouble()).toFloat()
          if (dur <= 0.1f || total > 60f) {
            copied.delete()
            return@withContext null to "Voiceover tidak valid atau total durasi > 60 detik."
          }
        }
        copied.name to null
      }
      val savedName = res.first
      val err = res.second
      if (savedName == null) {
        showToast(err ?: "Gagal memuat audio.")
        return@launch
      }
      mutateManifest { m ->
        if (isMusic) m.copy(musik = savedName)
        else m.copy(voiceover = savedName)
      }
      analyzeProjectAudioAsync(pid, _uiState.value.manifest)
      showToast(if (isMusic) "Musik latar diperbarui." else "Voiceover diperbarui.")
    }
  }

  fun removeBackgroundMusic() {
    mutateManifest { it.copy(musik = "") }
    _uiState.update { it.copy(musicPeaks = FloatArray(0)) }
  }

  fun addImagesToEditorAssets(uris: List<Uri>) {
    val pid = _uiState.value.activeProjectId ?: return
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val addedMap = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val asetDir = File(storage.projectDir(pid), "aset")
        val existingCount = _uiState.value.manifest.aset.size
        val map = linkedMapOf<String, String>()
        uris.forEachIndexed { idx, uri ->
          val key = "gambar_${existingCount + idx + 1}"
          val info = BitmapAssetProcessor.processAndSaveImageFromUri(resolver, uri, asetDir, key)
          if (info != null) map[key] = info.fileName
        }
        map
      }
      if (addedMap.isNotEmpty()) {
        mutateManifest { m -> m.copy(aset = m.aset + addedMap) }
        activeRenderer?.reloadCaches()
        renderSinglePreviewFrame(_uiState.value.currentTimeSec, highQuality = true)
        showToast("${addedMap.size} gambar di-alpha-crop & ditambahkan ke aset.")
      }
    }
  }

  fun splitActiveSceneAtPlayhead() {
    val st = _uiState.value
    val sc = st.selectedScene ?: return
    val splitT = st.currentTimeSec
    if (splitT <= sc.mulai + 0.6f || splitT >= sc.selesai - 0.6f) {
      showToast("Posisikan playhead minimal 0.6 dtk dari tepi adegan untuk memecah.")
      return
    }
    mutateManifest { m ->
      val idx = m.scenes.indexOfFirst { it.id == sc.id }
      if (idx < 0) return@mutateManifest m
      val left = sc.copy(selesai = splitT)
      val rightId = "sc_${UUID.randomUUID().toString().take(5)}"
      val right = sc.copy(
        id = rightId,
        nama = "${sc.nama} (B)",
        mulai = splitT,
        elemen = sc.elemen.map { it.copy(id = "${rightId}_${it.id.takeLast(5)}") },
      )
      val nextScenes = m.scenes.toMutableList()
      nextScenes[idx] = left
      nextScenes.add(idx + 1, right)
      m.copy(scenes = nextScenes)
    }
  }

  fun mergeSceneWithRight(sceneId: String) {
    mutateManifest { m ->
      val idx = m.scenes.indexOfFirst { it.id == sceneId }
      if (idx < 0 || idx >= m.scenes.lastIndex) return@mutateManifest m
      val left = m.scenes[idx]
      val right = m.scenes[idx + 1]
      val merged = left.copy(
        selesai = right.selesai,
        elemen = left.elemen + right.elemen,
      )
      val list = m.scenes.toMutableList()
      list[idx] = merged
      list.removeAt(idx + 1)
      m.copy(scenes = list)
    }
  }

  fun duplicateScene(sceneId: String) {
    val dur = _uiState.value.manifest.effectiveDurationSec()
    if (dur + 2f > 60f) {
      showToast("Tidak cukup sisa durasi (maks 60 detik).")
      return
    }
    mutateManifest { m ->
      val idx = m.scenes.indexOfFirst { it.id == sceneId }
      if (idx < 0) return@mutateManifest m
      val src = m.scenes[idx]
      val span = min(src.selesai - src.mulai, 60f - m.effectiveDurationSec()).coerceAtLeast(1f)
      val newId = "sc_${UUID.randomUUID().toString().take(5)}"
      val insertAt = src.selesai
      val cloned = src.copy(
        id = newId,
        nama = "${src.nama} (Duplikat)",
        mulai = insertAt,
        selesai = insertAt + span,
        elemen = src.elemen.map { it.copy(id = "${newId}_${it.id.takeLast(5)}") },
      )
      val shifted = m.scenes.mapIndexed { i, s ->
        if (i > idx) s.copy(mulai = s.mulai + span, selesai = s.selesai + span) else s
      }.toMutableList()
      shifted.add(idx + 1, cloned)
      m.copy(scenes = shifted)
    }
  }

  fun deleteScene(sceneId: String) {
    if (_uiState.value.manifest.scenes.size <= 1) {
      showToast("Minimal harus menyisakan 1 adegan.")
      return
    }
    mutateManifest { m ->
      val idx = m.scenes.indexOfFirst { it.id == sceneId }
      if (idx < 0) return@mutateManifest m
      val target = m.scenes[idx]
      val list = m.scenes.toMutableList()
      list.removeAt(idx)
      if (idx > 0) {
        list[idx - 1] = list[idx - 1].copy(selesai = target.selesai)
      } else if (list.isNotEmpty()) {
        list[0] = list[0].copy(mulai = target.mulai)
      }
      m.copy(scenes = list)
    }
  }

  fun adjustSceneBoundary(leftSceneId: String, deltaSec: Float) {
    mutateManifest(recordUndo = false) { m ->
      val idx = m.scenes.indexOfFirst { it.id == leftSceneId }
      if (idx < 0 || idx >= m.scenes.lastIndex) return@mutateManifest m
      val left = m.scenes[idx]
      val right = m.scenes[idx + 1]
      val newBoundary = (left.selesai + deltaSec).coerceIn(left.mulai + 0.8f, right.selesai - 0.8f)
      val list = m.scenes.toMutableList()
      list[idx] = left.copy(selesai = newBoundary)
      list[idx + 1] = right.copy(mulai = newBoundary)
      m.copy(scenes = list)
    }
  }

  fun updateSceneMeta(sceneId: String, nama: String? = null, aksen: String? = null) {
    mutateManifest { m ->
      m.copy(
        scenes = m.scenes.map { sc ->
          if (sc.id != sceneId) sc
          else sc.copy(
            nama = nama ?: sc.nama,
            aksen = aksen ?: sc.aksen,
          )
        },
      )
    }
  }

  fun addElementToSelectedScene(type: ElementType) {
    val sc = _uiState.value.selectedScene ?: return
    val newId = "el_${UUID.randomUUID().toString().take(6)}"
    val localCue = (_uiState.value.currentTimeSec - sc.mulai).coerceIn(0f, (sc.selesai - sc.mulai - 0.5f).coerceAtLeast(0f))
    val firstAsset = _uiState.value.manifest.aset.keys.firstOrNull().orEmpty()
    val elem = when (type) {
      ElementType.TEKS -> SceneElement(id = newId, tipe = type, cue = localCue, x = 540f, y = 620f, ukuran = 64f, warna = "#FFFFFF", isi = "Teks Baru")
      ElementType.KATA_KINETIK -> SceneElement(
        id = newId, tipe = type, cue = localCue, x = 540f, y = 680f, ukuran = 70f, warna = "#FFFFFF",
        kataList = listOf(KineticWord("KATA", localCue), KineticWord("KINETIK", localCue + 0.5f), KineticWord("BARU!", localCue + 1.0f)),
      )
      ElementType.IKON -> SceneElement(id = newId, tipe = type, cue = localCue, x = 540f, y = 460f, ukuran = 150f, warna = sc.aksen, ikonNama = "shield")
      ElementType.KARAKTER -> SceneElement(id = newId, tipe = type, cue = localCue, x = 540f, y = 1140f, tinggi = 480f, asetNama = firstAsset)
      ElementType.PANEL -> SceneElement(id = newId, tipe = type, cue = localCue, x = 540f, y = 440f, lebar = 840f, tinggi = 260f, warna = sc.aksen, isi = "Panel Sorotan")
      ElementType.KARTU -> SceneElement(id = newId, tipe = type, cue = localCue, x = 540f, y = 960f, lebar = 820f, tinggi = 220f, warna = sc.aksen, isi = "Kartu Informasi")
      ElementType.GELOMBANG_SUARA -> SceneElement(id = newId, tipe = type, cue = localCue, x = 540f, y = 1480f, lebar = 760f, tinggi = 120f, warna = sc.aksen)
      ElementType.PARTIKEL -> SceneElement(id = newId, tipe = type, cue = 0f, durasi = sc.selesai - sc.mulai, warna = sc.aksen, jumlah = 26)
      ElementType.HUJAN_DIGITAL -> SceneElement(id = newId, tipe = type, cue = 0f, durasi = sc.selesai - sc.mulai, warna = sc.aksen, jumlah = 22)
      ElementType.GARIS -> SceneElement(id = newId, tipe = type, cue = localCue, x = 180f, y = 1280f, x2 = 900f, y2 = 1280f, tebal = 10f, warna = sc.aksen)
      ElementType.CAPTION -> SceneElement(
        id = newId, tipe = type, cue = localCue, x = 540f, y = 1480f, ukuran = 44f, font = "poppins", warna = "#FFFFFF",
        captionList = listOf(CaptionLine("Teks caption pertama", localCue, localCue + 2.5f)),
      )
    }
    mutateManifest { m ->
      m.copy(
        scenes = m.scenes.map { if (it.id == sc.id) it.copy(elemen = it.elemen + elem) else it },
      )
    }
    selectElement(newId)
  }

  fun updateSelectedElement(recordUndo: Boolean = true, updater: (SceneElement) -> SceneElement) {
    val scId = _uiState.value.selectedScene?.id ?: return
    val elId = _uiState.value.selectedElement?.id ?: return
    mutateManifest(recordUndo = recordUndo) { m ->
      m.copy(
        scenes = m.scenes.map { sc ->
          if (sc.id != scId) sc
          else sc.copy(elemen = sc.elemen.map { if (it.id == elId) updater(it) else it })
        },
      )
    }
  }

  fun deleteSelectedElement() {
    val scId = _uiState.value.selectedScene?.id ?: return
    val elId = _uiState.value.selectedElement?.id ?: return
    mutateManifest { m ->
      m.copy(
        scenes = m.scenes.map { sc ->
          if (sc.id != scId) sc
          else sc.copy(elemen = sc.elemen.filterNot { it.id == elId })
        },
      )
    }
    _uiState.update { it.copy(selectedElementId = null) }
  }

  /**
   * Menggeser penanda cue elemen dengan magnet snap ke jeda suara atau playhead (< 0.25 dtk).
   */
  fun shiftElementCueWithSnap(sceneId: String, elementId: String, newAbsTimeSec: Float) {
    val st = _uiState.value
    val sc = st.manifest.scenes.firstOrNull { it.id == sceneId } ?: return
    var snappedAbs = newAbsTimeSec
    val snapTargets = st.voicePausesSec + st.currentTimeSec
    var bestDist = 0.25f
    for (target in snapTargets) {
      val d = abs(newAbsTimeSec - target)
      if (d < bestDist) {
        bestDist = d
        snappedAbs = target
      }
    }
    val localCue = (snappedAbs - sc.mulai).coerceIn(0f, max(0f, (sc.selesai - sc.mulai) - 0.2f))
    mutateManifest(recordUndo = false) { m ->
      m.copy(
        scenes = m.scenes.map { s ->
          if (s.id != sceneId) s
          else s.copy(elemen = s.elemen.map { if (it.id == elementId) it.copy(cue = localCue) else it })
        },
      )
    }
  }

  // ==================== RENDER SHEET & FOREGROUND SERVICE ====================

  fun openRenderSheet() {
    pausePlayback()
    flushAutosaveNow()
    val pid = _uiState.value.activeProjectId ?: return
    val warnings = ManifestSerializer.validateForRender(_uiState.value.manifest, storage.projectDir(pid))
    _uiState.update {
      it.copy(
        showRenderSheet = true,
        validationWarnings = warnings,
      )
    }
  }

  fun closeRenderSheet() {
    _uiState.update { it.copy(showRenderSheet = false) }
  }

  fun setRenderQuality(quality: RenderQuality) {
    _uiState.update { it.copy(renderQuality = quality) }
  }

  fun startMp4Render() {
    val pid = _uiState.value.activeProjectId ?: return
    val m = _uiState.value.manifest
    val warnings = ManifestSerializer.validateForRender(m, storage.projectDir(pid))
    if (warnings.any { it.isBlocking }) {
      _uiState.update { it.copy(validationWarnings = warnings) }
      showToast(warnings.first { it.isBlocking }.message)
      return
    }
    storage.saveProject(pid, m, generateThumb = false)
    RenderForegroundService.startRender(getApplication(), pid, _uiState.value.renderQuality)
  }

  fun cancelMp4Render() {
    RenderForegroundService.cancelRender(getApplication())
  }

  fun resetRenderStatus() {
    RenderProgressBus.clearFinished()
  }

  // ==================== BITMAP PREVIEW DETERMINISTIK ====================

  private fun renderSinglePreviewFrame(timeSec: Float, highQuality: Boolean) {
    val renderer = activeRenderer ?: return
    val w = if (highQuality) 540 else 360
    val h = if (highQuality) 960 else 640
    val targetBmp = if (highQuality) {
      if (previewBitmapPaused == null || previewBitmapPaused?.isRecycled == true) {
        previewBitmapPaused = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      }
      previewBitmapPaused!!
    } else {
      if (previewBitmapPlay == null || previewBitmapPlay?.isRecycled == true) {
        previewBitmapPlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      }
      previewBitmapPlay!!
    }

    val canvas = Canvas(targetBmp)
    val st = _uiState.value
    renderer.renderFrame(
      canvas = canvas,
      t = timeSec,
      showSafeZones = st.showSafeZones,
      selectedElementId = st.selectedElementId,
    )
    _uiState.update {
      it.copy(
        previewBitmap = targetBmp,
        previewRevision = it.previewRevision + 1,
      )
    }
  }

  override fun onCleared() {
    playbackJob?.cancel()
    autosaveJob?.cancel()
    activeRenderer?.release()
    previewBitmapPaused?.recycle()
    previewBitmapPlay?.recycle()
    super.onCleared()
  }

  companion object {
    private const val MAX_UNDO = 30
  }
}

data class VoiceAnalysisPayload(
  val dur: Float,
  val pausesSec: List<Float>,
  val sceneSplitsSec: List<Float>,
)
