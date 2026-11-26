package com.example.data

import android.content.Context
import com.example.model.AppliedEffect
import com.example.model.AspectRatioOption
import com.example.model.EasingCurve
import com.example.model.EffectCategory
import com.example.model.JsPresetKind
import com.example.model.Keyframe1D
import com.example.model.Keyframe2D
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.ShapeKind
import com.example.model.StudioJsonSerializer
import com.example.model.StudioProject
import java.io.File

class ProjectRepository(private val context: Context) {

  private val storageFile: File
    get() = File(context.filesDir, "kythera_projects_v2.json")

  fun loadProjects(): List<StudioProject> {
    val saved = runCatching {
      if (storageFile.exists()) {
        StudioJsonSerializer.projectsFromJson(storageFile.readText())
      } else {
        emptyList()
      }
    }.getOrDefault(emptyList())

    return saved.ifEmpty {
      val defaults = createSampleProjects()
      saveProjects(defaults)
      defaults
    }
  }

  fun saveProjects(projects: List<StudioProject>) {
    runCatching {
      storageFile.writeText(StudioJsonSerializer.projectsToJson(projects))
    }
  }

  fun defaultJsFiles(preset: JsPresetKind): Map<String, String> {
    return when (preset) {
      JsPresetKind.CYBER_PULSE -> linkedMapOf(
        "index.html" to """<!DOCTYPE html>
<html>
<head>
  <link rel="stylesheet" href="style.css" />
</head>
<body>
  <canvas id="stage"></canvas>
  <div class="hud">KYTHERA // SHADER PULSE</div>
  <script src="app.js"></script>
</body>
</html>""",
        "style.css" to """body {
  margin: 0;
  background: radial-gradient(circle, #182342, #07090e);
  overflow: hidden;
  font-family: "Plus Jakarta Sans", sans-serif;
}
.hud {
  position: fixed;
  bottom: 18px;
  left: 18px;
  color: #00E5FF;
  letter-spacing: 2px;
  font-size: 12px;
}""",
        "app.js" to """const pulseSpeed = 2.4;
const ringCount = 5;
const accentHue = 218;

function renderFrame(t) {
  for (let i = 0; i < ringCount; i++) {
    const phase = (t * pulseSpeed + i * 0.45) % 3.0;
    drawGlowRing(phase * 90, accentHue + i * 16);
  }
}
console.log("Kythera Pulse Shader ready @ 60 FPS");""",
      )
      JsPresetKind.KINETIC_BARS -> linkedMapOf(
        "index.html" to """<!DOCTYPE html>
<html>
<body>
  <div class="bars-stage"></div>
  <script src="app.js"></script>
</body>
</html>""",
        "style.css" to """body {
  margin: 0;
  background: #090C14;
}""",
        "app.js" to """const barCount = 18;
const tempoBpm = 128;

function renderSpectrum(t) {
  for (let i = 0; i < barCount; i++) {
    const amp = Math.abs(Math.sin(t * 4.2 + i * 0.55));
    drawSpectrumBar(i, amp);
  }
}
console.log("Kinetic Neon Spectrum active (" + tempoBpm + " BPM)");""",
      )
    }
  }

  companion object {
    fun createSampleProjects(): List<StudioProject> {
      return listOf(
        StudioProject(
          id = "proj_cyber_intro",
          name = "Aurora Motion Intro",
          aspectRatio = AspectRatioOption.PORTRAIT_9_16,
          fps = 60,
          resolutionLabel = "1080p",
          durationSec = 8.0f,
          updatedLabel = "Today, 09:20",
          accentColorHex = 0xFF5B6BFF,
          layers = listOf(
            LayerTrack(
              id = "l_bg_orb",
              name = "Aura Glow Orb",
              type = LayerType.SHAPE,
              startSec = 0f,
              durationSec = 8.0f,
              colorHex = 0xFF5B6BFF,
              shapeKind = ShapeKind.CIRCLE,
              motionBlurEnabled = true,
              easing = EasingCurve("Ease In-Out", 0.42f, 0.0f, 0.58f, 1.0f),
              positionKeys = listOf(
                Keyframe2D(0f, -40f, -60f),
                Keyframe2D(3.5f, 35f, 40f),
                Keyframe2D(7.5f, -10f, -20f),
              ),
              scaleKeys = listOf(
                Keyframe1D(0f, 90f),
                Keyframe1D(4.0f, 130f),
                Keyframe1D(8.0f, 95f),
              ),
              effects = listOf(
                AppliedEffect("electric_glow", "Electric Glow", EffectCategory.GLOW, "Intensity", 0f, 40f, 24f),
                AppliedEffect("cyber_particles", "Cyber Sparks", EffectCategory.PARTICLES, "Density", 5f, 60f, 26f),
              ),
            ),
            LayerTrack(
              id = "l_pulse_ring",
              name = "Pulse Ring Vector",
              type = LayerType.SHAPE,
              startSec = 0.4f,
              durationSec = 6.8f,
              colorHex = 0xFF00C6FF,
              shapeKind = ShapeKind.RING,
              easing = EasingCurve("Spring", 0.34f, 1.35f, 0.64f, 1.0f),
              rotationKeys = listOf(
                Keyframe1D(0f, 0f),
                Keyframe1D(6.5f, 360f),
              ),
              scaleKeys = listOf(
                Keyframe1D(0f, 65f),
                Keyframe1D(2.0f, 112f),
                Keyframe1D(6.5f, 100f),
              ),
              effects = listOf(
                AppliedEffect("chromatic_shift", "RGB Split", EffectCategory.DISTORTION, "Offset", 0f, 24f, 6f),
              ),
            ),
            LayerTrack(
              id = "l_hero_title",
              name = "KYTHERA Title",
              type = LayerType.TEXT,
              startSec = 0.6f,
              durationSec = 7.0f,
              colorHex = 0xFFF3F6FF,
              textContent = "KYTHERA",
              fontSizeSp = 36f,
              motionBlurEnabled = true,
              positionKeys = listOf(
                Keyframe2D(0f, 0f, 45f),
                Keyframe2D(1.4f, 0f, 0f),
              ),
              opacityKeys = listOf(
                Keyframe1D(0f, 0f),
                Keyframe1D(0.9f, 100f),
                Keyframe1D(6.5f, 100f),
                Keyframe1D(7.0f, 0f),
              ),
              effects = listOf(
                AppliedEffect("drop_shadow", "Drop Shadow", EffectCategory.SHADOW, "Depth", 0f, 36f, 18f),
              ),
            ),
            LayerTrack(
              id = "l_synth_audio",
              name = "Future Synth Bed.wav",
              type = LayerType.AUDIO,
              startSec = 0f,
              durationSec = 8.0f,
              colorHex = 0xFF00C48C,
            ),
          ),
        ),
        StudioProject(
          id = "proj_kinetic_reel",
          name = "Kinetic Glass Reel",
          aspectRatio = AspectRatioOption.SOCIAL_4_5,
          fps = 60,
          resolutionLabel = "1440p",
          durationSec = 6.0f,
          updatedLabel = "Yesterday",
          accentColorHex = 0xFF9C6BFF,
          layers = listOf(
            LayerTrack(
              id = "l_wave_bg",
              name = "Spectrum Wave",
              type = LayerType.SHAPE,
              startSec = 0f,
              durationSec = 6.0f,
              colorHex = 0xFF9C6BFF,
              shapeKind = ShapeKind.WAVE,
              effects = listOf(
                AppliedEffect("wave_warp", "Wave Warp", EffectCategory.DISTORTION, "Amplitude", 0f, 25f, 12f),
              ),
            ),
            LayerTrack(
              id = "l_kinetic_txt",
              name = "Create Without Limits",
              type = LayerType.TEXT,
              startSec = 0.3f,
              durationSec = 5.4f,
              colorHex = 0xFF00C6FF,
              textContent = "CREATE MOTION",
              fontSizeSp = 28f,
              rotationKeys = listOf(
                Keyframe1D(0f, -8f),
                Keyframe1D(2.5f, 0f),
              ),
            ),
          ),
        ),
        StudioProject(
          id = "proj_horizon_4k",
          name = "Widescreen Horizon 4K",
          aspectRatio = AspectRatioOption.LANDSCAPE_16_9,
          fps = 30,
          resolutionLabel = "4K",
          durationSec = 10.0f,
          updatedLabel = "2 days ago",
          accentColorHex = 0xFFFF7A64,
          layers = listOf(
            LayerTrack(
              id = "l_star_shape",
              name = "Starburst Prism",
              type = LayerType.SHAPE,
              startSec = 0f,
              durationSec = 10.0f,
              colorHex = 0xFFFF7A64,
              shapeKind = ShapeKind.STAR,
              rotationKeys = listOf(
                Keyframe1D(0f, 0f),
                Keyframe1D(10f, 270f),
              ),
            ),
            LayerTrack(
              id = "l_video_main",
              name = "B-Roll Cinema Clip",
              type = LayerType.VIDEO,
              startSec = 0.5f,
              durationSec = 8.5f,
              colorHex = 0xFF2979FF,
            ),
          ),
        ),
      )
    }
  }
}
