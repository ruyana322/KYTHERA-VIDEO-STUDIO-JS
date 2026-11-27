package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.media.DeviceDiagnostics
import com.example.media.MediaCodecCapabilityInspector
import com.example.media.NativeMp4Exporter
import com.example.media.RenderSpec
import com.example.media.VideoFrameExtractor
import com.example.media.ZipExtractResult
import com.example.model.AnimationProperty
import com.example.model.AppliedEffect
import com.example.model.AspectRatioOption
import com.example.model.DrawPointNorm
import com.example.model.EasingCurve
import com.example.model.EffectCategory
import com.example.model.EffectDef
import com.example.model.ExportConfig
import com.example.model.ExportFormat
import com.example.model.ExportResolution
import com.example.model.JsPresetKind
import com.example.model.Keyframe1D
import com.example.model.Keyframe2D
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.PreviewQuality
import com.example.model.ShapeKind
import com.example.model.StudioJsonSerializer
import com.example.model.StudioProject
import com.example.model.StudioTab
import com.example.model.evaluateLayerAt
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
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

data class StudioUiState(
  val projects: List<StudioProject> = emptyList(),
  val activeProjectId: String = "",
  val activeTab: StudioTab = StudioTab.HOME,
  val selectedLayerId: String? = null,
  val currentTimeSec: Float = 1.2f,
  val isPlaying: Boolean = false,
  val timelineZoom: Float = 1.0f,
  val previewQuality: PreviewQuality = PreviewQuality.FULL,
  val isDrawMode: Boolean = false,
  val darkGlassMode: Boolean = false,
  val showNewProjectDialog: Boolean = false,
  val showAddTextDialog: Boolean = false,
  val showShapePickerDialog: Boolean = false,
  val showAnimationSheet: Boolean = false,
  val showEffectsSheet: Boolean = false,
  val showLayerSettingsDialog: Boolean = false,
  val showDiagnosticsDialog: Boolean = false,
  val activeAnimProp: AnimationProperty = AnimationProperty.POSITION,
  val activeEffectCategory: EffectCategory = EffectCategory.ALL,
  val effectSearchQuery: String = "",
  val exportConfig: ExportConfig = ExportConfig(),
  val isExporting: Boolean = false,
  val exportProgress: Float = 0f,
  val exportFrameStatus: String = "",
  val exportCompleteMessage: String? = null,
  val renderedMp4Path: String? = null,
  val jsPreset: JsPresetKind = JsPresetKind.CYBER_PULSE,
  val jsFiles: Map<String, String> = emptyMap(),
  val jsBinaryFiles: Map<String, Pair<String, ByteArray>> = emptyMap(),
  val jsActiveFile: String = "app.js",
  val jsRunRevision: Int = 1,
  val jsConsoleLogs: List<String> = listOf("[Kythera JS] Runtime sandbox ready @ 60 FPS"),
  val jsPlaying: Boolean = true,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val toastMessage: String? = null,
  val diagnostics: DeviceDiagnostics? = null,
  val loadedImages: Map<String, ImageBitmap> = emptyMap(),
) {
  val activeProject: StudioProject?
    get() = projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()

  val selectedLayer: LayerTrack?
    get() = activeProject?.layers?.firstOrNull { it.id == selectedLayerId }
      ?: activeProject?.layers?.firstOrNull()
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ProjectRepository(application.applicationContext)
  private val mediaEngine = MediaCodecCapabilityInspector()
  private val mp4Exporter = NativeMp4Exporter(application.applicationContext)

  private val rawBitmaps = mutableMapOf<String, Bitmap>()
  private var audioPlayer: MediaPlayer? = null
  private var activeAudioUri: String? = null

  private val undoStack = ArrayDeque<StudioProject>()
  private val redoStack = ArrayDeque<StudioProject>()

  private var playbackJob: Job? = null
  private var exportJob: Job? = null
  @Volatile private var exportCancelRequested = false

  private val _uiState = MutableStateFlow(StudioUiState())
  val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

  init {
    val initialProjects = repository.loadProjects()
    val firstProj = initialProjects.firstOrNull()
    val defaultFiles = repository.defaultJsFiles(JsPresetKind.CYBER_PULSE)
    val diagnostics = mediaEngine.inspectDiagnostics()
    _uiState.value = StudioUiState(
      projects = initialProjects,
      activeProjectId = firstProj?.id.orEmpty(),
      selectedLayerId = firstProj?.layers?.firstOrNull()?.id,
      jsFiles = defaultFiles,
      diagnostics = diagnostics,
    )
  }

  override fun onCleared() {
    super.onCleared()
    releaseAudioPlayer()
  }

  private fun releaseAudioPlayer() {
    runCatching { audioPlayer?.stop() }
    runCatching { audioPlayer?.release() }
    audioPlayer = null
    activeAudioUri = null
  }

  fun selectTab(tab: StudioTab) {
    if (tab != StudioTab.EDITOR) {
      pausePlayback()
    }
    _uiState.update { it.copy(activeTab = tab, isDrawMode = false) }
  }

  fun openProjectInEditor(projectId: String) {
    pausePlayback()
    undoStack.clear()
    redoStack.clear()
    val proj = _uiState.value.projects.firstOrNull { it.id == projectId } ?: return
    _uiState.update {
      it.copy(
        activeProjectId = proj.id,
        selectedLayerId = proj.layers.firstOrNull()?.id,
        currentTimeSec = (proj.durationSec * 0.18f).coerceAtMost(proj.durationSec),
        activeTab = StudioTab.EDITOR,
        isDrawMode = false,
        canUndo = false,
        canRedo = false,
      )
    }
  }

  fun toggleThemeMode() {
    _uiState.update { it.copy(darkGlassMode = !it.darkGlassMode) }
  }

  fun setShowNewProjectDialog(show: Boolean) {
    _uiState.update { it.copy(showNewProjectDialog = show) }
  }

  fun setShowAddTextDialog(show: Boolean) {
    _uiState.update { it.copy(showAddTextDialog = show) }
  }

  fun setShowShapePickerDialog(show: Boolean) {
    _uiState.update { it.copy(showShapePickerDialog = show) }
  }

  fun setShowAnimationSheet(show: Boolean) {
    _uiState.update { it.copy(showAnimationSheet = show) }
  }

  fun setShowEffectsSheet(show: Boolean) {
    _uiState.update { it.copy(showEffectsSheet = show) }
  }

  fun setShowLayerSettingsDialog(show: Boolean) {
    _uiState.update { it.copy(showLayerSettingsDialog = show) }
  }

  fun setShowDiagnosticsDialog(show: Boolean) {
    _uiState.update { it.copy(showDiagnosticsDialog = show) }
  }

  fun showToast(message: String) {
    _uiState.update { it.copy(toastMessage = message) }
  }

  fun consumeToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  fun createProject(name: String, aspect: AspectRatioOption, fps: Int, resolutionLabel: String) {
    val cleanName = name.trim().ifEmpty { "Untitled Motion" }
    val newProj = StudioProject(
      id = "proj_${System.currentTimeMillis()}",
      name = cleanName,
      aspectRatio = aspect,
      fps = fps,
      resolutionLabel = resolutionLabel,
      durationSec = 8.0f,
      updatedLabel = "Just now",
      accentColorHex = listOf(0xFF5B6BFF, 0xFF00C6FF, 0xFF9C6BFF, 0xFFFF7A64).random(),
      layers = listOf(
        LayerTrack(
          id = "l_title_${System.currentTimeMillis()}",
          name = "Title Layer",
          type = LayerType.TEXT,
          startSec = 0f,
          durationSec = 8.0f,
          colorHex = 0xFFF3F6FF,
          textContent = cleanName.uppercase(),
          fontSizeSp = 32f,
        ),
        LayerTrack(
          id = "l_orb_${System.currentTimeMillis() + 1}",
          name = "Ambient Glow",
          type = LayerType.SHAPE,
          startSec = 0f,
          durationSec = 8.0f,
          colorHex = 0xFF5B6BFF,
          shapeKind = ShapeKind.CIRCLE,
          effects = listOf(
            AppliedEffect("electric_glow", "Electric Glow", EffectCategory.GLOW, "Intensity", 0f, 40f, 18f),
          ),
        ),
      ),
    )
    val nextList = listOf(newProj) + _uiState.value.projects
    repository.saveProjects(nextList)
    undoStack.clear()
    redoStack.clear()
    _uiState.update {
      it.copy(
        projects = nextList,
        activeProjectId = newProj.id,
        selectedLayerId = newProj.layers.firstOrNull()?.id,
        currentTimeSec = 0f,
        showNewProjectDialog = false,
        activeTab = StudioTab.EDITOR,
        canUndo = false,
        canRedo = false,
        toastMessage = "Created $cleanName",
      )
    }
  }

  fun duplicateProject(projectId: String) {
    val src = _uiState.value.projects.firstOrNull { it.id == projectId } ?: return
    val copy = src.copy(
      id = "proj_${System.currentTimeMillis()}",
      name = "${src.name} Copy",
      updatedLabel = "Just now",
    )
    val nextList = listOf(copy) + _uiState.value.projects
    repository.saveProjects(nextList)
    _uiState.update { it.copy(projects = nextList, toastMessage = "Duplicated ${src.name}") }
  }

  fun deleteProject(projectId: String) {
    val current = _uiState.value.projects
    if (current.size <= 1) {
      showToast("At least one project is required")
      return
    }
    val nextList = current.filterNot { it.id == projectId }
    repository.saveProjects(nextList)
    _uiState.update { state ->
      val nextActive = if (state.activeProjectId == projectId) nextList.first().id else state.activeProjectId
      state.copy(
        projects = nextList,
        activeProjectId = nextActive,
        toastMessage = "Project deleted",
      )
    }
  }

  private fun mutateActiveProject(recordUndo: Boolean = true, transform: (StudioProject) -> StudioProject) {
    val state = _uiState.value
    val active = state.activeProject ?: return
    if (recordUndo) {
      undoStack.addLast(active)
      if (undoStack.size > 30) undoStack.removeFirst()
      redoStack.clear()
    }
    val updated = transform(active).copy(updatedLabel = "Just now")
    val nextProjects = state.projects.map { if (it.id == updated.id) updated else it }
    repository.saveProjects(nextProjects)
    _uiState.update {
      it.copy(
        projects = nextProjects,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
      )
    }
  }

  fun undo() {
    val active = _uiState.value.activeProject ?: return
    if (undoStack.isEmpty()) return
    val prev = undoStack.removeLast()
    redoStack.addLast(active)
    val nextProjects = _uiState.value.projects.map { if (it.id == prev.id) prev else it }
    repository.saveProjects(nextProjects)
    _uiState.update {
      it.copy(
        projects = nextProjects,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        toastMessage = "Undo",
      )
    }
  }

  fun redo() {
    val active = _uiState.value.activeProject ?: return
    if (redoStack.isEmpty()) return
    val next = redoStack.removeLast()
    undoStack.addLast(active)
    val nextProjects = _uiState.value.projects.map { if (it.id == next.id) next else it }
    repository.saveProjects(nextProjects)
    _uiState.update {
      it.copy(
        projects = nextProjects,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        toastMessage = "Redo",
      )
    }
  }

  fun renameActiveProject(newName: String) {
    val clean = newName.trim()
    if (clean.isEmpty()) return
    mutateActiveProject { it.copy(name = clean) }
  }

  fun cycleAspectRatio() {
    val options = AspectRatioOption.entries
    val current = _uiState.value.activeProject?.aspectRatio ?: AspectRatioOption.PORTRAIT_9_16
    val next = options[(options.indexOf(current) + 1) % options.size]
    mutateActiveProject { it.copy(aspectRatio = next) }
    showToast("Canvas aspect ${next.label}")
  }

  fun cyclePreviewQuality() {
    val options = PreviewQuality.entries
    val next = options[(options.indexOf(_uiState.value.previewQuality) + 1) % options.size]
    _uiState.update { it.copy(previewQuality = next, toastMessage = "Preview: ${next.label}") }
  }

  fun selectLayer(layerId: String) {
    _uiState.update { it.copy(selectedLayerId = layerId) }
  }

  fun scrubTo(timeSec: Float) {
    val maxDur = _uiState.value.activeProject?.durationSec ?: 8f
    val clamped = timeSec.coerceIn(0f, maxDur)
    _uiState.update { it.copy(currentTimeSec = clamped) }
    runCatching {
      audioPlayer?.let { mp ->
        if (!mp.isPlaying) {
          mp.seekTo((clamped * 1000).toInt().coerceIn(0, mp.duration.coerceAtLeast(1)))
        }
      }
    }
  }

  fun adjustTimelineZoom(delta: Float) {
    _uiState.update { it.copy(timelineZoom = (it.timelineZoom + delta).coerceIn(0.5f, 3.0f)) }
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
    syncAudioPlaybackStart()
    _uiState.update { it.copy(isPlaying = true) }
    playbackJob = viewModelScope.launch {
      var lastNs = System.nanoTime()
      while (isActive && _uiState.value.isPlaying) {
        delay(16L)
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0f, 0.08f)
        lastNs = nowNs
        val maxDur = _uiState.value.activeProject?.durationSec ?: 8f
        val nextTime = _uiState.value.currentTimeSec + dt
        val wrapped = if (nextTime >= maxDur) 0f else nextTime
        if (wrapped == 0f) {
          runCatching { audioPlayer?.seekTo(0) }
        }
        _uiState.update { it.copy(currentTimeSec = wrapped) }
      }
    }
  }

  private fun syncAudioPlaybackStart() {
    val audioLayer = _uiState.value.activeProject?.layers?.firstOrNull {
      it.type == LayerType.AUDIO && it.visible && !it.mediaUri.isNullOrBlank()
    } ?: return
    val uriStr = audioLayer.mediaUri ?: return
    runCatching {
      if (activeAudioUri != uriStr || audioPlayer == null) {
        releaseAudioPlayer()
        val mp = MediaPlayer().apply {
          setDataSource(getApplication(), Uri.parse(uriStr))
          isLooping = true
          prepare()
        }
        audioPlayer = mp
        activeAudioUri = uriStr
      }
      audioPlayer?.let { mp ->
        val targetMs = ((_uiState.value.currentTimeSec - audioLayer.startSec).coerceAtLeast(0f) * 1000).toInt()
        mp.seekTo(targetMs.coerceIn(0, mp.duration.coerceAtLeast(1)))
        mp.start()
      }
    }
  }

  fun pausePlayback() {
    playbackJob?.cancel()
    playbackJob = null
    runCatching { if (audioPlayer?.isPlaying == true) audioPlayer?.pause() }
    _uiState.update { it.copy(isPlaying = false) }
  }

  fun addLayer(
    type: LayerType,
    name: String,
    textContent: String = "MOTION",
    fontSizeSp: Float = 32f,
    shapeKind: ShapeKind = ShapeKind.ROUNDED_RECT,
    mediaUri: String? = null,
    colorHex: Long = type.accentHex,
    initialOffsetY: Float = 0f,
  ) {
    val projDur = _uiState.value.activeProject?.durationSec ?: 8f
    val start = 0f
    val dur = projDur.coerceAtLeast(2f)
    val newLayer = LayerTrack(
      id = "layer_${System.currentTimeMillis()}",
      name = name,
      type = type,
      startSec = start,
      durationSec = dur,
      colorHex = colorHex,
      textContent = textContent,
      fontSizeSp = fontSizeSp,
      shapeKind = shapeKind,
      mediaUri = mediaUri,
      positionKeys = listOf(Keyframe2D(0f, 0f, initialOffsetY)),
    )
    mutateActiveProject { proj ->
      proj.copy(layers = listOf(newLayer) + proj.layers)
    }
    _uiState.update {
      it.copy(
        selectedLayerId = newLayer.id,
        isDrawMode = type == LayerType.DRAW,
        showAddTextDialog = false,
        showShapePickerDialog = false,
        toastMessage = "Added ${type.label}: $name",
      )
    }
  }

  fun addPickedMediaLayer(uri: Uri, displayName: String, isVideo: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      val bmp = if (isVideo) {
        VideoFrameExtractor.extractThumbnail(getApplication(), uri)
      } else {
        decodeBitmapThumbnail(uri)
      }
      if (bmp != null) {
        rawBitmaps[uri.toString()] = bmp
        withContext(Dispatchers.Main) {
          _uiState.update {
            it.copy(loadedImages = it.loadedImages + (uri.toString() to bmp.asImageBitmap()))
          }
        }
      }
    }
    addLayer(
      type = if (isVideo) LayerType.VIDEO else LayerType.IMAGE,
      name = displayName,
      mediaUri = uri.toString(),
    )
  }

  fun addPickedAudioLayer(uri: Uri, displayName: String) {
    addLayer(
      type = LayerType.AUDIO,
      name = displayName,
      mediaUri = uri.toString(),
      colorHex = 0xFF00C48C,
    )
  }

  private fun decodeBitmapThumbnail(uri: Uri): Bitmap? {
    return runCatching {
      getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeStream(stream, null, opts)
      }
    }.getOrNull()
  }

  fun ensureMediaLoaded(uriString: String, isVideo: Boolean) {
    if (_uiState.value.loadedImages.containsKey(uriString)) return
    viewModelScope.launch(Dispatchers.IO) {
      val uri = Uri.parse(uriString)
      val bmp = if (isVideo) {
        VideoFrameExtractor.extractThumbnail(getApplication(), uri)
      } else {
        decodeBitmapThumbnail(uri)
      } ?: return@launch
      rawBitmaps[uriString] = bmp
      withContext(Dispatchers.Main) {
        _uiState.update {
          it.copy(loadedImages = it.loadedImages + (uriString to bmp.asImageBitmap()))
        }
      }
    }
  }

  fun nudgeSelectedLayerPosition(dxPx: Float, dyPx: Float) {
    val state = _uiState.value
    val layer = state.selectedLayer ?: return
    if (layer.locked) return
    val eval = evaluateLayerAt(layer, state.currentTimeSec)
    val localT = ((state.currentTimeSec - layer.startSec).coerceAtLeast(0f) * 100f).toInt() / 100f
    val newX = (eval.posX + dxPx).coerceIn(-300f, 300f)
    val newY = (eval.posY + dyPx).coerceIn(-300f, 300f)

    mutateActiveProject(recordUndo = false) { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id != layer.id) return@map l
          val updated = l.positionKeys.toMutableList()
          val idx = updated.indexOfFirst { abs(it.timeSec - localT) < 0.08f }
          if (idx >= 0) updated[idx] = Keyframe2D(localT, newX, newY) else updated.add(Keyframe2D(localT, newX, newY))
          l.copy(positionKeys = updated.sortedBy { it.timeSec })
        },
      )
    }
  }

  fun toggleDrawMode() {
    val activeDraw = _uiState.value.isDrawMode
    if (!activeDraw) {
      val sel = _uiState.value.selectedLayer
      if (sel == null || sel.type != LayerType.DRAW) {
        addLayer(LayerType.DRAW, "Freehand Stroke", colorHex = 0xFF00C6FF)
      } else {
        _uiState.update { it.copy(isDrawMode = true, toastMessage = "Draw directly on preview") }
      }
    } else {
      _uiState.update { it.copy(isDrawMode = false) }
    }
  }

  fun appendDrawStroke(points: List<DrawPointNorm>) {
    if (points.size < 2) return
    val targetId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id == targetId) l.copy(drawStrokes = l.drawStrokes + listOf(points)) else l
        },
      )
    }
  }

  fun toggleLayerVisibility(layerId: String) {
    mutateActiveProject { proj ->
      proj.copy(layers = proj.layers.map { if (it.id == layerId) it.copy(visible = !it.visible) else it })
    }
  }

  fun toggleLayerLock(layerId: String) {
    mutateActiveProject { proj ->
      proj.copy(layers = proj.layers.map { if (it.id == layerId) it.copy(locked = !it.locked) else it })
    }
  }

  fun shiftLayerStart(layerId: String, deltaSec: Float) {
    mutateActiveProject(recordUndo = false) { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id == layerId && !l.locked) {
            val maxStart = (proj.durationSec - l.durationSec).coerceAtLeast(0f)
            l.copy(startSec = (l.startSec + deltaSec).coerceIn(0f, maxStart))
          } else l
        },
      )
    }
  }

  fun deleteSelectedLayer() {
    val selId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject { proj ->
      proj.copy(layers = proj.layers.filterNot { it.id == selId })
    }
    val nextFirst = _uiState.value.activeProject?.layers?.firstOrNull()?.id
    _uiState.update { it.copy(selectedLayerId = nextFirst, showLayerSettingsDialog = false, toastMessage = "Layer deleted") }
  }

  fun updateSelectedLayerMeta(name: String, textContent: String, colorHex: Long, shapeKind: ShapeKind) {
    val selId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id == selId) {
            l.copy(
              name = name.ifBlank { l.name },
              textContent = textContent,
              colorHex = colorHex,
              shapeKind = shapeKind,
            )
          } else l
        },
      )
    }
  }

  fun setActiveAnimationProperty(prop: AnimationProperty) {
    _uiState.update { it.copy(activeAnimProp = prop) }
  }

  fun setLayerKeyframeValue(
    prop: AnimationProperty,
    v1: Float,
    v2: Float = 0f,
  ) {
    val state = _uiState.value
    val layer = state.selectedLayer ?: return
    val localT = ((state.currentTimeSec - layer.startSec).coerceAtLeast(0f) * 100f).toInt() / 100f

    mutateActiveProject { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id != layer.id) return@map l
          when (prop) {
            AnimationProperty.POSITION -> {
              val updated = l.positionKeys.toMutableList()
              val idx = updated.indexOfFirst { abs(it.timeSec - localT) < 0.08f }
              if (idx >= 0) updated[idx] = Keyframe2D(localT, v1, v2) else updated.add(Keyframe2D(localT, v1, v2))
              l.copy(positionKeys = updated.sortedBy { it.timeSec })
            }
            AnimationProperty.ROTATION -> l.copy(rotationKeys = upsert1D(l.rotationKeys, localT, v1))
            AnimationProperty.SCALE -> l.copy(scaleKeys = upsert1D(l.scaleKeys, localT, v1))
            AnimationProperty.OPACITY -> l.copy(opacityKeys = upsert1D(l.opacityKeys, localT, v1))
            AnimationProperty.ANCHOR -> l.copy(anchorX = v1.coerceIn(0f, 1f), anchorY = v2.coerceIn(0f, 1f))
          }
        },
      )
    }
  }

  fun toggleKeyframeAtPlayhead(prop: AnimationProperty) {
    val state = _uiState.value
    val layer = state.selectedLayer ?: return
    val localT = ((state.currentTimeSec - layer.startSec).coerceAtLeast(0f) * 100f).toInt() / 100f
    val eval = evaluateLayerAt(layer, state.currentTimeSec)

    mutateActiveProject { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id != layer.id) return@map l
          when (prop) {
            AnimationProperty.POSITION -> {
              val hit = l.positionKeys.indexOfFirst { abs(it.timeSec - localT) < 0.08f }
              val next = if (hit >= 0 && l.positionKeys.size > 1) {
                l.positionKeys.filterIndexed { i, _ -> i != hit }
              } else {
                (l.positionKeys + Keyframe2D(localT, eval.posX, eval.posY)).sortedBy { it.timeSec }
              }
              l.copy(positionKeys = next)
            }
            AnimationProperty.ROTATION -> l.copy(rotationKeys = toggle1D(l.rotationKeys, localT, eval.rotationDeg))
            AnimationProperty.SCALE -> l.copy(scaleKeys = toggle1D(l.scaleKeys, localT, eval.scalePct))
            AnimationProperty.OPACITY -> l.copy(opacityKeys = toggle1D(l.opacityKeys, localT, eval.opacityPct))
            AnimationProperty.ANCHOR -> l
          }
        },
      )
    }
  }

  private fun upsert1D(list: List<Keyframe1D>, t: Float, v: Float): List<Keyframe1D> {
    val mutable = list.toMutableList()
    val idx = mutable.indexOfFirst { abs(it.timeSec - t) < 0.08f }
    if (idx >= 0) mutable[idx] = Keyframe1D(t, v) else mutable.add(Keyframe1D(t, v))
    return mutable.sortedBy { it.timeSec }
  }

  private fun toggle1D(list: List<Keyframe1D>, t: Float, currentVal: Float): List<Keyframe1D> {
    val hit = list.indexOfFirst { abs(it.timeSec - t) < 0.08f }
    return if (hit >= 0 && list.size > 1) {
      list.filterIndexed { i, _ -> i != hit }
    } else {
      (list + Keyframe1D(t, currentVal)).sortedBy { it.timeSec }
    }
  }

  fun updateLayerEasing(easing: EasingCurve, recordUndo: Boolean = false) {
    val selId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject(recordUndo = recordUndo) { proj ->
      proj.copy(layers = proj.layers.map { if (it.id == selId) it.copy(easing = easing) else it })
    }
  }

  fun toggleLayerMotionBlur(enabled: Boolean, shutter: Float) {
    val selId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject { proj ->
      proj.copy(
        layers = proj.layers.map {
          if (it.id == selId) it.copy(motionBlurEnabled = enabled, motionBlurShutter = shutter) else it
        },
      )
    }
  }

  fun setEffectCategory(cat: EffectCategory) {
    _uiState.update { it.copy(activeEffectCategory = cat) }
  }

  fun setEffectSearch(query: String) {
    _uiState.update { it.copy(effectSearchQuery = query) }
  }

  fun addEffectToSelectedLayer(def: EffectDef) {
    val selId = _uiState.value.selectedLayer?.id ?: return
    val applied = AppliedEffect(
      id = def.id,
      name = def.name,
      category = def.category,
      paramLabel = def.paramLabel,
      minVal = def.minVal,
      maxVal = def.maxVal,
      value = def.defaultVal,
    )
    mutateActiveProject { proj ->
      proj.copy(layers = proj.layers.map { if (it.id == selId) it.copy(effects = it.effects + applied) else it })
    }
    showToast("Applied ${def.name}")
  }

  fun updateEffectValue(index: Int, value: Float) {
    val selId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject(recordUndo = false) { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id != selId) return@map l
          val next = l.effects.mapIndexed { idx, fx -> if (idx == index) fx.copy(value = value) else fx }
          l.copy(effects = next)
        },
      )
    }
  }

  fun removeEffect(index: Int) {
    val selId = _uiState.value.selectedLayer?.id ?: return
    mutateActiveProject { proj ->
      proj.copy(
        layers = proj.layers.map { l ->
          if (l.id != selId) return@map l
          l.copy(effects = l.effects.filterIndexed { idx, _ -> idx != index })
        },
      )
    }
  }

  fun updateExportConfig(
    resolution: ExportResolution = _uiState.value.exportConfig.resolution,
    fps: Int = _uiState.value.exportConfig.fps,
    bitrateMbps: Int = _uiState.value.exportConfig.bitrateMbps,
    format: ExportFormat = _uiState.value.exportConfig.format,
  ) {
    _uiState.update {
      it.copy(
        exportConfig = ExportConfig(resolution, fps, bitrateMbps, format),
        exportCompleteMessage = null,
      )
    }
  }

  fun startRealMp4Export() {
    val proj = _uiState.value.activeProject ?: return
    val cfg = _uiState.value.exportConfig
    val aspect = proj.aspectRatio
    val heightPx = cfg.resolution.heightPx.coerceAtMost(1920)
    val widthPx = (heightPx * aspect.wRatio / aspect.hRatio).coerceIn(320, 1920)

    val prepared = mediaEngine.prepareRenderSession(
      RenderSpec(
        projectId = proj.id,
        width = widthPx,
        height = heightPx,
        fps = cfg.fps,
        bitrateMbps = cfg.bitrateMbps,
        containerFormat = cfg.format.label,
      ),
    )
    if (!prepared) {
      showToast("Invalid render specification")
      return
    }

    exportJob?.cancel()
    exportCancelRequested = false
    _uiState.update {
      it.copy(
        isExporting = true,
        exportProgress = 0f,
        exportFrameStatus = "Initializing MediaCodec H.264...",
        exportCompleteMessage = null,
        renderedMp4Path = null,
      )
    }

    exportJob = viewModelScope.launch(Dispatchers.Default) {
      runCatching {
        mp4Exporter.exportProjectToMp4(
          project = proj,
          widthPx = widthPx,
          heightPx = heightPx,
          fps = cfg.fps,
          bitrateMbps = cfg.bitrateMbps,
          loadedBitmaps = rawBitmaps.toMap(),
          onProgress = { pct, frame, total ->
            _uiState.update {
              it.copy(
                exportProgress = pct,
                exportFrameStatus = "Encoding frame $frame / $total (${cfg.fps} FPS)",
              )
            }
          },
          isCancelled = { exportCancelRequested },
        )
      }.onSuccess { mp4File ->
        val sizeKb = (mp4File.length() / 1024L).coerceAtLeast(1L)
        withContext(Dispatchers.Main) {
          _uiState.update {
            it.copy(
              isExporting = false,
              exportProgress = 1f,
              exportFrameStatus = "MP4 ready ($sizeKb KB)",
              renderedMp4Path = mp4File.absolutePath,
              exportCompleteMessage = "Encoded ${proj.name} (${mp4File.name} · ${sizeKb} KB). Tap Save MP4 to export to storage.",
              toastMessage = "MP4 render complete ($sizeKb KB)",
            )
          }
        }
      }.onFailure { err ->
        withContext(Dispatchers.Main) {
          _uiState.update {
            it.copy(
              isExporting = false,
              exportProgress = 0f,
              exportFrameStatus = "",
              toastMessage = if (exportCancelRequested) "Render cancelled" else "Render error: ${err.message}",
            )
          }
        }
      }
    }
  }

  fun cancelExport() {
    exportCancelRequested = true
    exportJob?.cancel()
    _uiState.update {
      it.copy(
        isExporting = false,
        exportProgress = 0f,
        exportFrameStatus = "",
        toastMessage = "Render cancelled",
      )
    }
  }

  fun getRenderedMp4File(): File? {
    val path = _uiState.value.renderedMp4Path ?: return null
    val f = File(path)
    return if (f.exists()) f else null
  }

  fun selectJsFile(fileName: String) {
    _uiState.update { it.copy(jsActiveFile = fileName) }
  }

  fun updateJsFileContent(content: String) {
    val activeFile = _uiState.value.jsActiveFile
    _uiState.update {
      it.copy(jsFiles = it.jsFiles + (activeFile to content))
    }
  }

  fun addNewJsFile() {
    val idx = _uiState.value.jsFiles.size + 1
    val name = "module_$idx.js"
    val body = "// Kythera Shader Module $idx\nconsole.log(\"Loaded $name\");\n"
    _uiState.update {
      it.copy(
        jsFiles = it.jsFiles + (name to body),
        jsActiveFile = name,
        jsConsoleLogs = it.jsConsoleLogs + "[File] Created $name",
      )
    }
  }

  fun toggleJsPreset() {
    val nextPreset = if (_uiState.value.jsPreset == JsPresetKind.CYBER_PULSE) {
      JsPresetKind.KINETIC_BARS
    } else {
      JsPresetKind.CYBER_PULSE
    }
    val nextFiles = repository.defaultJsFiles(nextPreset)
    _uiState.update {
      it.copy(
        jsPreset = nextPreset,
        jsFiles = nextFiles,
        jsBinaryFiles = emptyMap(),
        jsActiveFile = "app.js",
        jsRunRevision = it.jsRunRevision + 1,
        jsConsoleLogs = it.jsConsoleLogs + "[Preset] Switched to ${nextPreset.title}",
      )
    }
  }

  fun runJsRenderPass() {
    val active = _uiState.value.jsActiveFile
    _uiState.update {
      it.copy(
        jsPlaying = true,
        jsRunRevision = it.jsRunRevision + 1,
        jsConsoleLogs = (it.jsConsoleLogs + "[Render] Executing live preview ($active)").takeLast(30),
        toastMessage = "Running live JS preview",
      )
    }
  }

  fun appendJsConsoleLog(line: String) {
    _uiState.update {
      it.copy(jsConsoleLogs = (it.jsConsoleLogs + line).takeLast(30))
    }
  }

  fun clearJsConsole() {
    _uiState.update { it.copy(jsConsoleLogs = emptyList()) }
  }

  fun sendJsAnimationToTimeline() {
    val timelineKey = _uiState.value.jsFiles.keys.firstOrNull { it.endsWith("timeline.json", ignoreCase = true) }
    val timelineRaw = timelineKey?.let { _uiState.value.jsFiles[it] }
    if (!timelineRaw.isNullOrBlank()) {
      val parsed = StudioJsonSerializer.parseExternalTimelineJson(timelineRaw, "Imported Timeline")
      if (parsed != null) {
        val next = listOf(parsed) + _uiState.value.projects
        repository.saveProjects(next)
        _uiState.update {
          it.copy(
            projects = next,
            activeProjectId = parsed.id,
            selectedLayerId = parsed.layers.firstOrNull()?.id,
            activeTab = StudioTab.EDITOR,
            toastMessage = "Imported ${parsed.layers.size} layers from timeline.json",
          )
        }
        return
      }
    }

    val preset = _uiState.value.jsPreset
    val kind = if (preset == JsPresetKind.CYBER_PULSE) ShapeKind.RING else ShapeKind.WAVE
    addLayer(
      type = LayerType.SHAPE,
      name = "JS: ${preset.title}",
      shapeKind = kind,
      colorHex = 0xFF00C6FF,
    )
    selectTab(StudioTab.EDITOR)
  }

  fun importProjectJson(rawJson: String) {
    val direct = runCatching { StudioJsonSerializer.projectsFromJson(rawJson) }.getOrDefault(emptyList())
    val resolved = if (direct.isNotEmpty() && direct.first().layers.isNotEmpty()) {
      direct
    } else {
      listOfNotNull(StudioJsonSerializer.parseExternalTimelineJson(rawJson))
    }
    if (resolved.isEmpty()) {
      showToast("Invalid project / timeline JSON file")
      return
    }
    val next = resolved + _uiState.value.projects
    repository.saveProjects(next)
    _uiState.update {
      it.copy(
        projects = next,
        activeProjectId = resolved.first().id,
        selectedLayerId = resolved.first().layers.firstOrNull()?.id,
        activeTab = StudioTab.EDITOR,
        toastMessage = "Imported ${resolved.first().name}",
      )
    }
  }

  fun importZipExtractResult(result: ZipExtractResult) {
    if (result.textFiles.isEmpty() && result.binaryFiles.isEmpty()) {
      showToast("ZIP archive contained no supported HTML/JS/JSON/media files")
      return
    }
    val preferred = result.textFiles.keys.firstOrNull { it.endsWith("index.html", ignoreCase = true) }
      ?: result.textFiles.keys.firstOrNull { it.endsWith(".js", ignoreCase = true) }
      ?: result.textFiles.keys.firstOrNull { it.endsWith("timeline.json", ignoreCase = true) }
      ?: result.textFiles.keys.firstOrNull()
      ?: ""

    val fileSummary = (result.textFiles.keys + result.binaryFiles.keys).joinToString(", ")
    _uiState.update {
      it.copy(
        jsFiles = result.textFiles,
        jsBinaryFiles = result.binaryFiles,
        jsActiveFile = preferred,
        jsRunRevision = it.jsRunRevision + 1,
        activeTab = StudioTab.SCRIPTS,
        jsConsoleLogs = (it.jsConsoleLogs + "[ZIP] Loaded ${result.textFiles.size} code/JSON & ${result.binaryFiles.size} asset files: $fileSummary").takeLast(30),
        toastMessage = "Imported ${result.textFiles.size + result.binaryFiles.size} files from ZIP",
      )
    }
  }

  fun buildActiveProjectJsonForExport(): Pair<String, String> {
    val proj = _uiState.value.activeProject ?: StudioProject("proj", "Kythera")
    val filename = "${proj.name.replace(Regex("\\s+"), "_")}.kythera.json"
    return filename to StudioJsonSerializer.projectToJson(proj).toString(2)
  }

  fun buildJsFilesJsonForZipExport(): Pair<String, String> {
    val json = JSONObject().apply {
      _uiState.value.jsFiles.forEach { (k, v) -> put(k, v) }
    }.toString()
    return "kythera-js-animation.zip" to json
  }
}
