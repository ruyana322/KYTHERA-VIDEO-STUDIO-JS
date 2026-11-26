package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.WebViewAssetLoader
import com.example.bridge.KytheraHostActions
import com.example.bridge.KytheraNativeBridge
import com.example.media.MediaCodecCapabilityInspector
import com.example.media.RenderSpec
import com.example.webview.KYTHERA_ASSET_URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kythera Studio", appName)
  }

  @Test
  fun `webview assets are packaged`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetList = context.assets.list("")?.toSet() ?: emptySet()
    assertTrue(assetList.contains("index.html"))
    assertTrue(assetList.contains("style.css"))
    assertTrue(assetList.contains("app.js"))
  }

  @Test
  fun `webview asset loader resolves html css and js`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val loader = WebViewAssetLoader.Builder()
      .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
      .build()

    val htmlResp = loader.shouldInterceptRequest(Uri.parse(KYTHERA_ASSET_URL))
    val cssResp = loader.shouldInterceptRequest(Uri.parse("https://appassets.androidplatform.net/assets/style.css"))
    val jsResp = loader.shouldInterceptRequest(Uri.parse("https://appassets.androidplatform.net/assets/app.js"))

    assertNotNull("Expected index.html to resolve via WebViewAssetLoader", htmlResp)
    assertNotNull("Expected style.css to resolve via WebViewAssetLoader", cssResp)
    assertNotNull("Expected app.js to resolve via WebViewAssetLoader", jsResp)
  }

  @Test
  fun `native bridge reports startup ready and error callbacks`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var readyCount = 0
    var lastError: String? = null

    val actions = object : KytheraHostActions {
      override fun onRequestMediaPick() {}
      override fun onRequestZipImport() {}
      override fun onRequestProjectImport() {}
      override fun onRequestZipExport(suggestedFilename: String, filesJson: String) {}
      override fun onRequestJsonExport(suggestedFilename: String, content: String) {}
      override fun onWebAppReady() { readyCount++ }
      override fun onWebStartupError(errorMessage: String) { lastError = errorMessage }
    }

    val bridge = KytheraNativeBridge(context, MediaCodecCapabilityInspector(), actions)
    bridge.reportAppReady()
    bridge.reportStartupError("ReferenceError: test")
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(1, readyCount)
    assertEquals("ReferenceError: test", lastError)
  }

  @Test
  fun `media engine gateway validates render spec`() {
    val engine = MediaCodecCapabilityInspector()
    val diagnostics = engine.inspectDiagnostics()
    assertTrue(diagnostics.abis.isNotEmpty())
    assertTrue(
      engine.prepareRenderSession(
        RenderSpec(
          projectId = "proj_test",
          width = 1920,
          height = 1080,
          fps = 60,
          bitrateMbps = 24,
          containerFormat = "MP4",
        ),
      ),
    )
  }
}
