package com.example

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.bridge.KytheraHostActions
import com.example.bridge.KytheraNativeBridge
import com.example.media.MediaCodecCapabilityInspector
import com.example.media.ZipProjectHandler
import com.example.ui.theme.KytheraStudioTheme
import com.example.webview.KytheraWebViewHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(), KytheraHostActions {

  private var webView: WebView? = null
  private var pendingZipFilesJson: String? = null
  private var pendingJsonContent: String? = null
  private var externalStartupError by mutableStateOf<String?>(null)
  private var externalBootConfirmed by mutableStateOf(false)

  private val mediaEngine = MediaCodecCapabilityInspector()

  private val mediaPickerLauncher = registerForActivityResult(
    ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) handlePickedMedia(uri)
  }

  private val zipOpenLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) handleImportedZip(uri)
  }

  private val projectOpenLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) handleImportedProject(uri)
  }

  private val zipCreateLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/zip"),
  ) { uri ->
    val payload = pendingZipFilesJson
    pendingZipFilesJson = null
    if (uri != null && payload != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        runCatching { ZipProjectHandler.writeZipFromJson(contentResolver, uri, payload) }
          .onSuccess {
            withContext(Dispatchers.Main) {
              dispatchToastToWeb("Exported ZIP archive")
            }
          }
      }
    }
  }

  private val jsonCreateLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    val payload = pendingJsonContent
    pendingJsonContent = null
    if (uri != null && payload != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        runCatching {
          contentResolver.openOutputStream(uri)?.use { out ->
            out.write(payload.toByteArray(StandardCharsets.UTF_8))
          }
        }.onSuccess {
          withContext(Dispatchers.Main) {
            dispatchToastToWeb("Exported project JSON")
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.i(TAG, "MainActivity.onCreate")
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.parseColor("#07090E")),
      navigationBarStyle = SystemBarStyle.dark(Color.parseColor("#07090E")),
    )

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          val wv = webView
          if (wv == null) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            return
          }
          wv.evaluateJavascript("window.kytheraHandleBack && window.kytheraHandleBack()") { result ->
            if (result != "true") {
              isEnabled = false
              onBackPressedDispatcher.onBackPressed()
            }
          }
        }
      },
    )

    val bridge = KytheraNativeBridge(
      context = applicationContext,
      mediaEngine = mediaEngine,
      hostActions = this,
    )

    setContent {
      KytheraStudioTheme {
        KytheraWebViewHost(
          nativeBridge = bridge,
          externalStartupError = externalStartupError,
          externalBootConfirmed = externalBootConfirmed,
          onClearExternalError = {
            externalStartupError = null
            externalBootConfirmed = false
          },
          onWebViewCreated = { created -> webView = created },
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    webView?.onResume()
    webView?.resumeTimers()
  }

  override fun onPause() {
    webView?.onPause()
    super.onPause()
  }

  override fun onWebAppReady() {
    Log.i(TAG, "WebApp confirmed ready")
    externalBootConfirmed = true
  }

  override fun onWebStartupError(errorMessage: String) {
    Log.e(TAG, "WebApp startup error: $errorMessage")
    externalStartupError = errorMessage
  }

  override fun onRequestMediaPick() {
    mediaPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
    )
  }

  override fun onRequestZipImport() {
    zipOpenLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
  }

  override fun onRequestProjectImport() {
    projectOpenLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
  }

  override fun onRequestZipExport(suggestedFilename: String, filesJson: String) {
    pendingZipFilesJson = filesJson
    zipCreateLauncher.launch(suggestedFilename.ifBlank { "kythera-project.zip" })
  }

  override fun onRequestJsonExport(suggestedFilename: String, content: String) {
    pendingJsonContent = content
    jsonCreateLauncher.launch(suggestedFilename.ifBlank { "project.kythera.json" })
  }

  private fun handlePickedMedia(uri: Uri) {
    lifecycleScope.launch(Dispatchers.IO) {
      val mime = contentResolver.getType(uri) ?: "image/jpeg"
      val isVideo = mime.startsWith("video/")
      val displayName = queryDisplayName(uri) ?: if (isVideo) "Clip.mp4" else "Image.png"
      var dataUrl: String? = null

      if (!isVideo) {
        runCatching {
          contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size <= 3 * 1024 * 1024) {
              val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
              dataUrl = "data:$mime;base64,$b64"
            }
          }
        }
      }

      val json = JSONObject().apply {
        put("name", displayName)
        put("isVideo", isVideo)
        put("dataUrl", dataUrl ?: JSONObject.NULL)
      }.toString()

      withContext(Dispatchers.Main) {
        webView?.evaluateJavascript(
          "window.__kytheraNativeMediaPicked(${JSONObject.quote(json)})",
          null,
        )
      }
    }
  }

  private fun handleImportedZip(uri: Uri) {
    lifecycleScope.launch(Dispatchers.IO) {
      val extractedJson = runCatching {
        ZipProjectHandler.extractZipToJson(contentResolver, uri)
      }.getOrDefault("{}")

      withContext(Dispatchers.Main) {
        webView?.evaluateJavascript(
          "window.__kytheraNativeZipImported(${JSONObject.quote(extractedJson)})",
          null,
        )
      }
    }
  }

  private fun handleImportedProject(uri: Uri) {
    lifecycleScope.launch(Dispatchers.IO) {
      val content = runCatching {
        contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
      }.getOrNull() ?: return@launch

      withContext(Dispatchers.Main) {
        webView?.evaluateJavascript(
          "window.__kytheraNativeProjectImported(${JSONObject.quote(content)})",
          null,
        )
      }
    }
  }

  private fun queryDisplayName(uri: Uri): String? {
    return runCatching {
      contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
      }
    }.getOrNull()
  }

  private fun dispatchToastToWeb(message: String) {
    webView?.evaluateJavascript("androidToast && androidToast(${JSONObject.quote(message)})", null)
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
  }

  override fun onDestroy() {
    webView?.stopLoading()
    webView?.destroy()
    webView = null
    super.onDestroy()
  }

  private companion object {
    const val TAG = "KytheraMain"
  }
}
