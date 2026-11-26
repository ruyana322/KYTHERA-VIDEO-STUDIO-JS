package com.example.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.media.MediaEngineGateway

interface KytheraHostActions {
  fun onRequestMediaPick()
  fun onRequestZipImport()
  fun onRequestProjectImport()
  fun onRequestZipExport(suggestedFilename: String, filesJson: String)
  fun onRequestJsonExport(suggestedFilename: String, content: String)
}

class KytheraNativeBridge(
  private val context: Context,
  private val mediaEngine: MediaEngineGateway,
  private val hostActions: KytheraHostActions,
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun getDeviceDiagnosticsJson(): String {
    return mediaEngine.inspectDiagnostics().toJsonString()
  }

  @JavascriptInterface
  fun pickMedia(@Suppress("UNUSED_PARAMETER") mediaType: String) {
    mainHandler.post { hostActions.onRequestMediaPick() }
  }

  @JavascriptInterface
  fun importZipProject() {
    mainHandler.post { hostActions.onRequestZipImport() }
  }

  @JavascriptInterface
  fun importKytheraProject() {
    mainHandler.post { hostActions.onRequestProjectImport() }
  }

  @JavascriptInterface
  fun exportZipProject(filename: String, filesJson: String) {
    mainHandler.post { hostActions.onRequestZipExport(filename, filesJson) }
  }

  @JavascriptInterface
  fun exportJsonFile(filename: String, content: String) {
    mainHandler.post { hostActions.onRequestJsonExport(filename, content) }
  }

  @JavascriptInterface
  fun showNativeToast(message: String) {
    mainHandler.post {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
  }
}
