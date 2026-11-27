package com.example

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.media.ZipExtractResult
import com.example.media.ZipProjectHandler
import com.example.model.LayerType
import com.example.model.StudioTab
import com.example.ui.components.AmbientGlassBackground
import com.example.ui.components.FrostedFloatingBottomBar
import com.example.ui.components.FrostedNavigationRail
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JsStudioScreen
import com.example.ui.sheets.AddTextDialog
import com.example.ui.sheets.AnimationSheet
import com.example.ui.sheets.DiagnosticsDialog
import com.example.ui.sheets.EffectsSheet
import com.example.ui.sheets.LayerSettingsDialog
import com.example.ui.sheets.NewProjectDialog
import com.example.ui.sheets.ShapePickerDialog
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraStudioTheme
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

  private val viewModel: StudioViewModel by viewModels()
  private var pendingZipPayload: String? = null
  private var pendingJsonPayload: String? = null

  private val mediaPickerLauncher = registerForActivityResult(
    ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      val mime = contentResolver.getType(uri) ?: "image/jpeg"
      val isVideo = mime.startsWith("video/")
      val displayName = queryDisplayName(uri) ?: if (isVideo) "Clip.mp4" else "Image.png"
      viewModel.addPickedMediaLayer(uri, displayName, isVideo)
    }
  }

  private val audioOpenLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      val displayName = queryDisplayName(uri) ?: "Audio Track.mp3"
      viewModel.addPickedAudioLayer(uri, displayName)
    }
  }

  private val zipOpenLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        val result = runCatching {
          ZipProjectHandler.extractZip(contentResolver, uri)
        }.getOrDefault(ZipExtractResult(emptyMap(), emptyMap()))
        withContext(Dispatchers.Main) {
          viewModel.importZipExtractResult(result)
        }
      }
    }
  }

  private val projectOpenLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        val content = runCatching {
          contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
        }.getOrNull()
        if (!content.isNullOrBlank()) {
          withContext(Dispatchers.Main) {
            viewModel.importProjectJson(content)
          }
        }
      }
    }
  }

  private val zipCreateLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/zip"),
  ) { uri ->
    val payload = pendingZipPayload
    pendingZipPayload = null
    if (uri != null && payload != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        runCatching { ZipProjectHandler.writeZipFromJson(contentResolver, uri, payload) }
          .onSuccess {
            withContext(Dispatchers.Main) {
              viewModel.showToast("Exported ZIP bundle")
            }
          }
      }
    }
  }

  private val jsonCreateLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    val payload = pendingJsonPayload
    pendingJsonPayload = null
    if (uri != null && payload != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        runCatching {
          contentResolver.openOutputStream(uri)?.use { out ->
            out.write(payload.toByteArray(StandardCharsets.UTF_8))
          }
        }.onSuccess {
          withContext(Dispatchers.Main) {
            viewModel.showToast("Exported .kythera.json project")
          }
        }
      }
    }
  }

  private val mp4CreateLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("video/mp4"),
  ) { uri ->
    val srcFile = viewModel.getRenderedMp4File()
    if (uri != null && srcFile != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        runCatching {
          contentResolver.openOutputStream(uri)?.use { out ->
            srcFile.inputStream().use { input -> input.copyTo(out) }
          }
        }.onSuccess {
          withContext(Dispatchers.Main) {
            viewModel.showToast("Saved MP4 to device storage")
          }
        }.onFailure { err ->
          withContext(Dispatchers.Main) {
            viewModel.showToast("Failed to save MP4: ${err.message}")
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
    )

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      KytheraStudioTheme(darkTheme = uiState.darkGlassMode) {
        KytheraStudioApp(
          viewModel = viewModel,
          onPickMedia = {
            mediaPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
          },
          onPickAudio = {
            audioOpenLauncher.launch(arrayOf("audio/*"))
          },
          onImportProject = {
            projectOpenLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
          },
          onImportZip = {
            zipOpenLauncher.launch(
              arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"),
            )
          },
          onExportZip = {
            val (name, payload) = viewModel.buildJsFilesJsonForZipExport()
            pendingZipPayload = payload
            zipCreateLauncher.launch(name)
          },
          onExportJson = {
            val (name, payload) = viewModel.buildActiveProjectJsonForExport()
            pendingJsonPayload = payload
            jsonCreateLauncher.launch(name)
          },
          onSaveMp4 = {
            val projName = viewModel.uiState.value.activeProject?.name?.replace(Regex("\\s+"), "_") ?: "kythera"
            mp4CreateLauncher.launch("$projName.mp4")
          },
        )
      }
    }
  }

  override fun onPause() {
    viewModel.pausePlayback()
    super.onPause()
  }

  private fun queryDisplayName(uri: Uri): String? {
    return runCatching {
      contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
      }
    }.getOrNull()
  }
}

@Composable
private fun KytheraStudioApp(
  viewModel: StudioViewModel,
  onPickMedia: () -> Unit,
  onPickAudio: () -> Unit,
  onImportProject: () -> Unit,
  onImportZip: () -> Unit,
  onExportZip: () -> Unit,
  onExportJson: () -> Unit,
  onSaveMp4: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.toastMessage) {
    val msg = uiState.toastMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeToast()
    }
  }

  AmbientGlassBackground {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
      val useNavRail = maxWidth >= 680.dp

      Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
          SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
              snackbarData = data,
              shape = RoundedCornerShape(16.dp),
              containerColor = KytheraPeriwinkle,
              contentColor = Color.White,
            )
          }
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          if (useNavRail) {
            FrostedNavigationRail(
              activeTab = uiState.activeTab,
              onSelectTab = viewModel::selectTab,
              onNewProjectClick = { viewModel.setShowNewProjectDialog(true) },
            )
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            val bottomNavPad = if (!useNavRail && uiState.activeTab != StudioTab.EDITOR) {
              PaddingValues(bottom = 86.dp)
            } else {
              PaddingValues(0.dp)
            }

            when (uiState.activeTab) {
              StudioTab.HOME -> HomeScreen(
                uiState = uiState,
                onOpenProject = viewModel::openProjectInEditor,
                onNewProject = { viewModel.setShowNewProjectDialog(true) },
                onImportProject = onImportProject,
                onImportZip = onImportZip,
                onOpenJsStudio = { viewModel.selectTab(StudioTab.SCRIPTS) },
                onDuplicateProject = viewModel::duplicateProject,
                onDeleteProject = viewModel::deleteProject,
                onToggleTheme = viewModel::toggleThemeMode,
                onOpenDiagnostics = { viewModel.setShowDiagnosticsDialog(true) },
                bottomContentPadding = bottomNavPad,
              )

              StudioTab.EDITOR -> EditorScreen(
                uiState = uiState,
                onBackHome = { viewModel.selectTab(StudioTab.HOME) },
                onRenameProject = viewModel::renameActiveProject,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onCycleQuality = viewModel::cyclePreviewQuality,
                onCycleAspect = viewModel::cycleAspectRatio,
                onOpenExport = { viewModel.selectTab(StudioTab.EXPORT) },
                onTogglePlay = viewModel::togglePlayPause,
                onScrub = viewModel::scrubTo,
                onZoomDelta = viewModel::adjustTimelineZoom,
                onSelectLayer = viewModel::selectLayer,
                onToggleVisibility = viewModel::toggleLayerVisibility,
                onToggleLock = viewModel::toggleLayerLock,
                onShiftLayerStart = viewModel::shiftLayerStart,
                onPickMedia = onPickMedia,
                onAddAudio = onPickAudio,
                onAddText = { viewModel.setShowAddTextDialog(true) },
                onAddShape = { viewModel.setShowShapePickerDialog(true) },
                onToggleDrawMode = viewModel::toggleDrawMode,
                onOpenEffects = { viewModel.setShowEffectsSheet(true) },
                onOpenAnimation = { viewModel.setShowAnimationSheet(true) },
                onOpenLayerSettings = { viewModel.setShowLayerSettingsDialog(true) },
                onRequestMediaLoad = viewModel::ensureMediaLoaded,
                onCommitDrawStroke = viewModel::appendDrawStroke,
                onDragSelectedLayer = viewModel::nudgeSelectedLayerPosition,
              )

              StudioTab.SCRIPTS -> JsStudioScreen(
                preset = uiState.jsPreset,
                files = uiState.jsFiles,
                binaryFiles = uiState.jsBinaryFiles,
                activeFile = uiState.jsActiveFile,
                runRevision = uiState.jsRunRevision,
                consoleLogs = uiState.jsConsoleLogs,
                isPlaying = uiState.jsPlaying,
                onBack = { viewModel.selectTab(StudioTab.HOME) },
                onSelectFile = viewModel::selectJsFile,
                onEditContent = viewModel::updateJsFileContent,
                onAddNewFile = viewModel::addNewJsFile,
                onTogglePreset = viewModel::toggleJsPreset,
                onImportZip = onImportZip,
                onExportZip = onExportZip,
                onRunRender = viewModel::runJsRenderPass,
                onConsoleLog = viewModel::appendJsConsoleLog,
                onClearConsole = viewModel::clearJsConsole,
                onSendToTimeline = viewModel::sendJsAnimationToTimeline,
                bottomContentPadding = bottomNavPad,
              )

              StudioTab.EXPORT -> ExportScreen(
                uiState = uiState,
                onBack = { viewModel.selectTab(StudioTab.EDITOR) },
                onUpdateResolution = { viewModel.updateExportConfig(resolution = it) },
                onUpdateFps = { viewModel.updateExportConfig(fps = it) },
                onUpdateBitrate = { viewModel.updateExportConfig(bitrateMbps = it) },
                onUpdateFormat = { viewModel.updateExportConfig(format = it) },
                onStartExport = viewModel::startRealMp4Export,
                onCancelExport = viewModel::cancelExport,
                onSaveMp4 = onSaveMp4,
                onExportJson = onExportJson,
                bottomContentPadding = bottomNavPad,
              )
            }

            if (!useNavRail && uiState.activeTab != StudioTab.EDITOR) {
              FrostedFloatingBottomBar(
                activeTab = uiState.activeTab,
                onSelectTab = viewModel::selectTab,
                onNewProjectClick = { viewModel.setShowNewProjectDialog(true) },
                modifier = Modifier.align(Alignment.BottomCenter),
              )
            }
          }
        }
      }
    }

    if (uiState.showNewProjectDialog) {
      NewProjectDialog(
        onCreate = viewModel::createProject,
        onDismiss = { viewModel.setShowNewProjectDialog(false) },
      )
    }

    if (uiState.showAddTextDialog) {
      AddTextDialog(
        onAdd = { text, fontSize, colorHex ->
          viewModel.addLayer(
            type = LayerType.TEXT,
            name = text.take(18),
            textContent = text,
            fontSizeSp = fontSize,
            colorHex = colorHex,
            initialOffsetY = -28f,
          )
        },
        onDismiss = { viewModel.setShowAddTextDialog(false) },
      )
    }

    if (uiState.showShapePickerDialog) {
      ShapePickerDialog(
        onPickShape = { shapeKind, colorHex ->
          viewModel.addLayer(
            type = LayerType.SHAPE,
            name = shapeKind.label,
            shapeKind = shapeKind,
            colorHex = colorHex,
          )
        },
        onDismiss = { viewModel.setShowShapePickerDialog(false) },
      )
    }

    val selectedLayer = uiState.selectedLayer
    if (uiState.showAnimationSheet && selectedLayer != null) {
      AnimationSheet(
        layer = selectedLayer,
        currentTimeSec = uiState.currentTimeSec,
        activeProperty = uiState.activeAnimProp,
        onSelectProperty = viewModel::setActiveAnimationProperty,
        onSetKeyframeValue = viewModel::setLayerKeyframeValue,
        onToggleKeyframe = viewModel::toggleKeyframeAtPlayhead,
        onUpdateEasing = { viewModel.updateLayerEasing(it, recordUndo = false) },
        onToggleMotionBlur = viewModel::toggleLayerMotionBlur,
        onDismiss = { viewModel.setShowAnimationSheet(false) },
      )
    }

    if (uiState.showEffectsSheet && selectedLayer != null) {
      EffectsSheet(
        layer = selectedLayer,
        selectedCategory = uiState.activeEffectCategory,
        searchQuery = uiState.effectSearchQuery,
        onSelectCategory = viewModel::setEffectCategory,
        onSearchChange = viewModel::setEffectSearch,
        onAddEffect = viewModel::addEffectToSelectedLayer,
        onUpdateEffectValue = viewModel::updateEffectValue,
        onRemoveEffect = viewModel::removeEffect,
        onDismiss = { viewModel.setShowEffectsSheet(false) },
      )
    }

    if (uiState.showLayerSettingsDialog && selectedLayer != null) {
      LayerSettingsDialog(
        layer = selectedLayer,
        onSave = viewModel::updateSelectedLayerMeta,
        onDelete = viewModel::deleteSelectedLayer,
        onDismiss = { viewModel.setShowLayerSettingsDialog(false) },
      )
    }

    if (uiState.showDiagnosticsDialog) {
      DiagnosticsDialog(
        diagnostics = uiState.diagnostics,
        onDismiss = { viewModel.setShowDiagnosticsDialog(false) },
      )
    }
  }
}
