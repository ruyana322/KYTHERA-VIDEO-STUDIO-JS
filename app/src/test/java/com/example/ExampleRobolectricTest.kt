package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.media.MediaCodecCapabilityInspector
import com.example.media.RenderSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
