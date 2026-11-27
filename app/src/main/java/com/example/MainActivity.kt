package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motion.ui.MotionEditorScreen
import com.example.motion.ui.NewProjectWizardScreen
import com.example.motion.ui.ProjectListScreen
import com.example.motion.ui.RenderExportSheet
import com.example.motion.viewmodel.MotionStudioViewModel
import com.example.motion.viewmodel.StudioScreen
import com.example.ui.components.AmbientGlassBackground
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraStudioTheme

class MainActivity : ComponentActivity() {

  private val viewModel: MotionStudioViewModel by viewModels()

  private var pendingAudioMode: AudioPickTarget = AudioPickTarget.WIZARD_VOICEOVER
  private var pendingZipExportProjectId: String? = null

  private enum class AudioPickTarget {
    WIZARD_VOICEOVER,
    WIZARD_MUSIC,
    EDITOR_VOICEOVER,
    EDITOR_MUSIC,
  }

  private val notificationPermLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) {
    // Tetap lanjutkan render meskipun notifikasi ditolak pengguna
    viewModel.startMp4Render()
  }

  private val zipImportLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) viewModel.importZipUri(uri)
  }

  private val zipExportLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/zip"),
  ) { uri ->
    val pid = pendingZipExportProjectId
    pendingZipExportProjectId = null
    if (uri != null && pid != null) {
      viewModel.exportProjectToZipUri(pid, uri)
    }
  }

  private val audioOpenLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      val name = queryDisplayName(uri) ?: "audio.wav"
      when (pendingAudioMode) {
        AudioPickTarget.WIZARD_VOICEOVER -> viewModel.selectWizardVoiceover(uri, name)
        AudioPickTarget.WIZARD_MUSIC -> viewModel.selectWizardMusic(uri, name)
        AudioPickTarget.EDITOR_VOICEOVER -> viewModel.replaceAudioInEditor(uri, name, isMusic = false)
        AudioPickTarget.EDITOR_MUSIC -> viewModel.replaceAudioInEditor(uri, name, isMusic = true)
      }
    }
  }

  private val multiImagePickerLauncher = registerForActivityResult(
    ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
  ) { uris ->
    if (uris.isNotEmpty()) {
      if (viewModel.uiState.value.screen == StudioScreen.WIZARD) {
        viewModel.addWizardAssets(uris)
      } else {
        viewModel.addImagesToEditorAssets(uris)
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
        KytheraMotionApp(
          viewModel = viewModel,
          onImportZip = {
            zipImportLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
          },
          onExportZip = { projectId ->
            pendingZipExportProjectId = projectId
            zipExportLauncher.launch("proyek_motion_${projectId}.zip")
          },
          onPickAudio = { target ->
            pendingAudioMode = target
            audioOpenLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/mpeg", "audio/*"))
          },
          onPickImages = {
            multiImagePickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
          },
          onStartRenderWithPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
              ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
              notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              viewModel.startMp4Render()
            }
          },
          onOpenVideo = { uriString ->
            runCatching {
              val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(uriString), "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              startActivity(intent)
            }.onFailure {
              viewModel.showToast("Tidak ada pemutar video tersedia; buka langsung dari Galeri.")
            }
          },
          onShareVideo = { uriString ->
            runCatching {
              val share = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(uriString))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              startActivity(Intent.createChooser(share, "Bagikan Video MP4"))
            }
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

  @Composable
  private fun KytheraMotionApp(
    viewModel: MotionStudioViewModel,
    onImportZip: () -> Unit,
    onExportZip: (String) -> Unit,
    onPickAudio: (AudioPickTarget) -> Unit,
    onPickImages: () -> Unit,
    onStartRenderWithPermission: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onShareVideo: (String) -> Unit,
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
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing),
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          when (uiState.screen) {
            StudioScreen.PROJECT_LIST -> ProjectListScreen(
              projects = uiState.projects,
              isImportingZip = uiState.isImportingZip,
              darkMode = uiState.darkGlassMode,
              onToggleTheme = viewModel::toggleThemeMode,
              onNewProject = viewModel::startNewProjectWizard,
              onImportZip = onImportZip,
              onLoadSample51s = viewModel::createAndOpenSample51s,
              onOpenProject = { id -> viewModel.openProjectInEditor(id) },
              onRenameProject = viewModel::renameProject,
              onDuplicateProject = viewModel::duplicateProject,
              onExportZip = onExportZip,
              onDeleteProject = viewModel::deleteProject,
            )

            StudioScreen.WIZARD -> NewProjectWizardScreen(
              wizard = uiState.wizard,
              onCancel = viewModel::cancelWizard,
              onSetStep = viewModel::setWizardStep,
              onTitleChange = viewModel::updateWizardTitle,
              onPickVoiceover = { onPickAudio(AudioPickTarget.WIZARD_VOICEOVER) },
              onPickMusic = { onPickAudio(AudioPickTarget.WIZARD_MUSIC) },
              onPickAssets = onPickImages,
              onRenameAsset = viewModel::updateWizardAssetName,
              onRemoveAsset = viewModel::removeWizardAsset,
              onSelectStyle = viewModel::setWizardStyle,
              onFinishAutoDraft = viewModel::finalizeWizardAutoDraft,
            )

            StudioScreen.EDITOR -> MotionEditorScreen(
              uiState = uiState,
              onBack = viewModel::backToProjectList,
              onUndo = viewModel::undo,
              onRedo = viewModel::redo,
              onOpenRender = viewModel::openRenderSheet,
              onTogglePlay = viewModel::togglePlayPause,
              onToggleLoopScene = viewModel::toggleLoopActiveScene,
              onToggleSafeZones = viewModel::toggleSafeZones,
              onPreviewTap = viewModel::handlePreviewTap,
              onPreviewDrag = viewModel::dragSelectedElement,
              onPreviewPinch = viewModel::pinchSelectedElement,
              onScrub = viewModel::scrubTo,
              onSelectScene = { sid -> viewModel.selectScene(sid, jumpToStart = true) },
              onAdjustSceneBoundary = viewModel::adjustSceneBoundary,
              onSplitSceneAtPlayhead = viewModel::splitActiveSceneAtPlayhead,
              onMergeSceneRight = viewModel::mergeSceneWithRight,
              onDuplicateScene = viewModel::duplicateScene,
              onDeleteScene = viewModel::deleteScene,
              onSelectElement = viewModel::selectElement,
              onShiftElementCue = viewModel::shiftElementCueWithSnap,
              onSelectTab = viewModel::selectEditorTab,
              onRenameScene = { sid, nm -> viewModel.updateSceneMeta(sid, nama = nm) },
              onUpdateSceneAccent = { sid, ax -> viewModel.updateSceneMeta(sid, aksen = ax) },
              onAddElement = viewModel::addElementToSelectedScene,
              onUpdateElement = { fn -> viewModel.updateSelectedElement(recordUndo = true, updater = fn) },
              onDeleteElement = viewModel::deleteSelectedElement,
              onAddAssets = onPickImages,
              onPickVoiceover = { onPickAudio(AudioPickTarget.EDITOR_VOICEOVER) },
              onPickMusic = { onPickAudio(AudioPickTarget.EDITOR_MUSIC) },
              onRemoveMusic = viewModel::removeBackgroundMusic,
              onUpdateOffsets = viewModel::updateAudioOffsets,
              onUpdateTitle = viewModel::updateProjectTitleInEditor,
              onSelectStyle = viewModel::updateStylePreset,
              onUpdateTransitionDur = viewModel::updateTransitionDuration,
            )
          }
        }
      }

      if (uiState.showRenderSheet) {
        RenderExportSheet(
          selectedQuality = uiState.renderQuality,
          warnings = uiState.validationWarnings,
          status = uiState.renderStatus,
          onSelectQuality = viewModel::setRenderQuality,
          onStartRender = onStartRenderWithPermission,
          onCancelRender = viewModel::cancelMp4Render,
          onOpenVideo = onOpenVideo,
          onShareVideo = onShareVideo,
          onDismiss = {
            viewModel.resetRenderStatus()
            viewModel.closeRenderSheet()
          },
        )
      }
    }
  }
}
