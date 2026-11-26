package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectRepository
import com.example.media.MediaCodecCapabilityInspector
import com.example.media.RenderSpec
import com.example.model.EasingCurve
import com.example.model.Keyframe1D
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.StudioJsonSerializer
import com.example.model.evaluateLayerAt
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
  fun `project repository persists and roundtrips json`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ProjectRepository(context)
    val projects = repo.loadProjects()
    assertTrue(projects.isNotEmpty())

    val serialized = StudioJsonSerializer.projectsToJson(projects)
    val decoded = StudioJsonSerializer.projectsFromJson(serialized)
    assertEquals(projects.size, decoded.size)
    assertEquals(projects.first().name, decoded.first().name)
  }

  @Test
  fun `keyframe interpolation evaluates midpoint accurately`() {
    val layer = LayerTrack(
      id = "test_layer",
      name = "Test Shape",
      type = LayerType.SHAPE,
      startSec = 0f,
      durationSec = 4f,
      easing = EasingCurve("Linear", 0.25f, 0.25f, 0.75f, 0.75f),
      scaleKeys = listOf(Keyframe1D(0f, 100f), Keyframe1D(2f, 200f)),
    )
    val eval = evaluateLayerAt(layer, 1f)
    assertEquals(150f, eval.scalePct, 2.0f)
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
