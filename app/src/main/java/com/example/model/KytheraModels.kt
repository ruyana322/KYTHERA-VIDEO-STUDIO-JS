package com.example.model

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow

enum class StudioTab {
  HOME,
  EDITOR,
  SCRIPTS,
  EXPORT,
}

enum class AspectRatioOption(val label: String, val wRatio: Int, val hRatio: Int, val baseW: Int, val baseH: Int) {
  PORTRAIT_9_16("9:16", 9, 16, 1080, 1920),
  LANDSCAPE_16_9("16:9", 16, 9, 1920, 1080),
  SQUARE_1_1("1:1", 1, 1, 1080, 1080),
  SOCIAL_4_5("4:5", 4, 5, 1080, 1350);

  companion object {
    fun fromLabel(label: String): AspectRatioOption =
      entries.firstOrNull { it.label == label } ?: PORTRAIT_9_16
  }
}

enum class PreviewQuality(val label: String, val stepScale: Float) {
  FULL("Full", 1.0f),
  HALF("Half", 0.65f),
  DRAFT("Draft", 0.4f),
}

enum class LayerType(val label: String, val accentHex: Long) {
  VIDEO("Video", 0xFF2979FF),
  AUDIO("Audio", 0xFF00C48C),
  IMAGE("Image", 0xFF9C6BFF),
  TEXT("Text", 0xFFFF7A64),
  SHAPE("Shape", 0xFF00C6FF),
  DRAW("Vector", 0xFFFFB020),
}

enum class ShapeKind(val label: String) {
  ROUNDED_RECT("Rounded Card"),
  CIRCLE("Glow Orb"),
  RING("Pulse Ring"),
  STAR("Starburst"),
  WAVE("Spectrum Wave"),
}

enum class AnimationProperty(val label: String) {
  POSITION("Position"),
  ROTATION("Rotation"),
  SCALE("Scale"),
  OPACITY("Opacity"),
  ANCHOR("Anchor"),
}

enum class EffectCategory(val label: String) {
  ALL("All"),
  BLUR("Blur"),
  GLOW("Glow"),
  SHADOW("Shadow"),
  DISTORTION("Distortion"),
  COLOR_CORRECTION("Color Correction"),
  CHROMA_KEY("Chroma Key"),
  MOTION("Motion Effects"),
  PARTICLES("Particle Effects"),
  TRANSITIONS("Transitions"),
}

data class Keyframe2D(
  val timeSec: Float,
  val x: Float,
  val y: Float,
)

data class Keyframe1D(
  val timeSec: Float,
  val value: Float,
)

data class EasingCurve(
  val label: String = "Ease In-Out",
  val p1x: Float = 0.42f,
  val p1y: Float = 0.0f,
  val p2x: Float = 0.58f,
  val p2y: Float = 1.0f,
) {
  companion object {
    val PRESETS = listOf(
      EasingCurve("Linear", 0.25f, 0.25f, 0.75f, 0.75f),
      EasingCurve("Ease In-Out", 0.42f, 0.0f, 0.58f, 1.0f),
      EasingCurve("Ease Out", 0.16f, 1.0f, 0.30f, 1.0f),
      EasingCurve("Ease In", 0.55f, 0.05f, 0.75f, 0.2f),
      EasingCurve("Spring", 0.34f, 1.35f, 0.64f, 1.0f),
    )
  }

  fun evaluate(tInput: Float): Float {
    val t = tInput.coerceIn(0f, 1f)
    // Iterative solve for x(u) = t on cubic Bezier, then evaluate y(u)
    var u = t
    repeat(6) {
      val omt = 1f - u
      val x = 3f * omt * omt * u * p1x + 3f * omt * u * u * p2x + u * u * u
      val dx = 3f * omt * omt * p1x + 6f * omt * u * (p2x - p1x) + 3f * u * u * (1f - p2x)
      if (kotlin.math.abs(dx) > 1e-4f) {
        u = (u - (x - t) / dx).coerceIn(0f, 1f)
      }
    }
    val omt = 1f - u
    return (3f * omt * omt * u * p1y + 3f * omt * u * u * p2y + u * u * u)
  }
}

data class EffectDef(
  val id: String,
  val category: EffectCategory,
  val name: String,
  val description: String,
  val paramLabel: String,
  val minVal: Float,
  val maxVal: Float,
  val defaultVal: Float,
)

data class AppliedEffect(
  val id: String,
  val name: String,
  val category: EffectCategory,
  val paramLabel: String,
  val minVal: Float,
  val maxVal: Float,
  val value: Float,
)

object EffectsCatalog {
  val ALL = listOf(
    EffectDef("gaussian_blur", EffectCategory.BLUR, "Gaussian Blur", "Smooth isotropic lens blur", "Radius", 0f, 30f, 8f),
    EffectDef("motion_blur_fx", EffectCategory.BLUR, "Directional Blur", "High-speed directional streak", "Amount", 0f, 30f, 10f),
    EffectDef("electric_glow", EffectCategory.GLOW, "Electric Glow", "Cyan-blue neon bloom aura", "Intensity", 0f, 40f, 20f),
    EffectDef("neon_bloom", EffectCategory.GLOW, "Soft Bloom", "Thresholded highlight bloom", "Spread", 0f, 40f, 16f),
    EffectDef("drop_shadow", EffectCategory.SHADOW, "Drop Shadow", "Elevated soft depth shadow", "Depth", 0f, 36f, 16f),
    EffectDef("vignette", EffectCategory.SHADOW, "Cinema Vignette", "Edge framing shadow falloff", "Strength", 0f, 100f, 45f),
    EffectDef("chromatic_shift", EffectCategory.DISTORTION, "RGB Split", "Chromatic lens aberration", "Offset", 0f, 24f, 8f),
    EffectDef("wave_warp", EffectCategory.DISTORTION, "Wave Warp", "Dynamic sinusoidal displacement", "Amplitude", 0f, 25f, 9f),
    EffectDef("color_grade", EffectCategory.COLOR_CORRECTION, "Contrast & Grade", "Punchy tonal contrast curve", "Contrast", 50f, 180f, 125f),
    EffectDef("hue_shift", EffectCategory.COLOR_CORRECTION, "Hue Shift", "Spectrum color rotation", "Degrees", 0f, 360f, 45f),
    EffectDef("chroma_key", EffectCategory.CHROMA_KEY, "Chroma Key", "Key out green/blue backdrop", "Tolerance", 0f, 100f, 35f),
    EffectDef("oscillate", EffectCategory.MOTION, "Oscillate", "Continuous harmonic float", "Speed", 1f, 10f, 4f),
    EffectDef("camera_shake", EffectCategory.MOTION, "Handheld Shake", "Organic micro-jitter impulse", "Jitter", 0f, 20f, 5f),
    EffectDef("cyber_particles", EffectCategory.PARTICLES, "Cyber Sparks", "Floating glowing micro particles", "Density", 5f, 60f, 24f),
    EffectDef("cross_dissolve", EffectCategory.TRANSITIONS, "Edge Dissolve", "Smooth opacity fade curve", "Fade", 0f, 100f, 60f),
  )
}

data class DrawPointNorm(val x: Float, val y: Float)

data class LayerTrack(
  val id: String,
  val name: String,
  val type: LayerType,
  val startSec: Float,
  val durationSec: Float,
  val colorHex: Long = 0xFF5B6BFF,
  val visible: Boolean = true,
  val locked: Boolean = false,
  val textContent: String = "KYTHERA",
  val fontSizeSp: Float = 32f,
  val shapeKind: ShapeKind = ShapeKind.ROUNDED_RECT,
  val mediaUri: String? = null,
  val drawStrokes: List<List<DrawPointNorm>> = emptyList(),
  val anchorX: Float = 0.5f,
  val anchorY: Float = 0.5f,
  val motionBlurEnabled: Boolean = false,
  val motionBlurShutter: Float = 180f,
  val easing: EasingCurve = EasingCurve(),
  val positionKeys: List<Keyframe2D> = listOf(Keyframe2D(0f, 0f, 0f)),
  val rotationKeys: List<Keyframe1D> = listOf(Keyframe1D(0f, 0f)),
  val scaleKeys: List<Keyframe1D> = listOf(Keyframe1D(0f, 100f)),
  val opacityKeys: List<Keyframe1D> = listOf(Keyframe1D(0f, 100f)),
  val effects: List<AppliedEffect> = emptyList(),
) {
  val endSec: Float get() = startSec + durationSec

  fun allKeyframeTimes(): List<Float> {
    return (positionKeys.map { it.timeSec } +
      rotationKeys.map { it.timeSec } +
      scaleKeys.map { it.timeSec } +
      opacityKeys.map { it.timeSec }).distinct().sorted()
  }
}

data class EvaluatedLayer(
  val posX: Float,
  val posY: Float,
  val rotationDeg: Float,
  val scalePct: Float,
  val opacityPct: Float,
)

fun interpolate1D(keys: List<Keyframe1D>, localTime: Float, easing: EasingCurve, fallback: Float): Float {
  if (keys.isEmpty()) return fallback
  val sorted = keys.sortedBy { it.timeSec }
  if (localTime <= sorted.first().timeSec) return sorted.first().value
  if (localTime >= sorted.last().timeSec) return sorted.last().value
  for (i in 0 until sorted.lastIndex) {
    val a = sorted[i]
    val b = sorted[i + 1]
    if (localTime in a.timeSec..b.timeSec) {
      val span = (b.timeSec - a.timeSec).coerceAtLeast(0.0001f)
      val factor = easing.evaluate((localTime - a.timeSec) / span)
      return a.value + (b.value - a.value) * factor
    }
  }
  return sorted.last().value
}

fun interpolate2D(keys: List<Keyframe2D>, localTime: Float, easing: EasingCurve): Pair<Float, Float> {
  if (keys.isEmpty()) return 0f to 0f
  val sorted = keys.sortedBy { it.timeSec }
  if (localTime <= sorted.first().timeSec) return sorted.first().x to sorted.first().y
  if (localTime >= sorted.last().timeSec) return sorted.last().x to sorted.last().y
  for (i in 0 until sorted.lastIndex) {
    val a = sorted[i]
    val b = sorted[i + 1]
    if (localTime in a.timeSec..b.timeSec) {
      val span = (b.timeSec - a.timeSec).coerceAtLeast(0.0001f)
      val factor = easing.evaluate((localTime - a.timeSec) / span)
      return (a.x + (b.x - a.x) * factor) to (a.y + (b.y - a.y) * factor)
    }
  }
  return sorted.last().x to sorted.last().y
}

fun evaluateLayerAt(layer: LayerTrack, timelineTimeSec: Float): EvaluatedLayer {
  val localTime = (timelineTimeSec - layer.startSec).coerceAtLeast(0f)
  val (px, py) = interpolate2D(layer.positionKeys, localTime, layer.easing)
  return EvaluatedLayer(
    posX = px,
    posY = py,
    rotationDeg = interpolate1D(layer.rotationKeys, localTime, layer.easing, 0f),
    scalePct = interpolate1D(layer.scaleKeys, localTime, layer.easing, 100f),
    opacityPct = interpolate1D(layer.opacityKeys, localTime, layer.easing, 100f).coerceIn(0f, 100f),
  )
}

data class StudioProject(
  val id: String,
  val name: String,
  val aspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_9_16,
  val fps: Int = 60,
  val resolutionLabel: String = "1080p",
  val durationSec: Float = 8.0f,
  val updatedLabel: String = "Just now",
  val accentColorHex: Long = 0xFF5B6BFF,
  val layers: List<LayerTrack> = emptyList(),
)

enum class ExportResolution(val label: String, val heightPx: Int) {
  HD_720("720p", 720),
  FHD_1080("1080p", 1080),
  QHD_1440("1440p", 1440),
  UHD_4K("4K", 2160),
}

enum class ExportFormat(val label: String, val codecSummary: String) {
  MP4("MP4", "H.264 / AAC"),
  WEBM("WebM", "VP9 / Opus"),
}

data class ExportConfig(
  val resolution: ExportResolution = ExportResolution.FHD_1080,
  val fps: Int = 60,
  val bitrateMbps: Int = 24,
  val format: ExportFormat = ExportFormat.MP4,
)

enum class JsPresetKind(val title: String) {
  CYBER_PULSE("Cyberpunk Pulse Ring"),
  KINETIC_BARS("Kinetic Neon Spectrum"),
}

object StudioJsonSerializer {
  fun projectsToJson(projects: List<StudioProject>): String {
    val arr = JSONArray()
    projects.forEach { arr.put(projectToJson(it)) }
    return arr.toString()
  }

  fun projectsFromJson(raw: String): List<StudioProject> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return emptyList()
    return if (trimmed.startsWith("[")) {
      val arr = JSONArray(trimmed)
      buildList {
        for (i in 0 until arr.length()) {
          add(projectFromJson(arr.getJSONObject(i)))
        }
      }
    } else {
      listOf(projectFromJson(JSONObject(trimmed)))
    }
  }

  fun projectToJson(p: StudioProject): JSONObject {
    return JSONObject().apply {
      put("id", p.id)
      put("name", p.name)
      put("aspectRatio", p.aspectRatio.label)
      put("fps", p.fps)
      put("resolutionLabel", p.resolutionLabel)
      put("durationSec", p.durationSec.toDouble())
      put("updatedLabel", p.updatedLabel)
      put("accentColorHex", p.accentColorHex)
      val layersArr = JSONArray()
      p.layers.forEach { layer ->
        layersArr.put(JSONObject().apply {
          put("id", layer.id)
          put("name", layer.name)
          put("type", layer.type.name)
          put("startSec", layer.startSec.toDouble())
          put("durationSec", layer.durationSec.toDouble())
          put("colorHex", layer.colorHex)
          put("visible", layer.visible)
          put("locked", layer.locked)
          put("textContent", layer.textContent)
          put("fontSizeSp", layer.fontSizeSp.toDouble())
          put("shapeKind", layer.shapeKind.name)
          put("mediaUri", layer.mediaUri ?: JSONObject.NULL)
          put("anchorX", layer.anchorX.toDouble())
          put("anchorY", layer.anchorY.toDouble())
          put("motionBlurEnabled", layer.motionBlurEnabled)
          put("motionBlurShutter", layer.motionBlurShutter.toDouble())
          put("easing", JSONObject().apply {
            put("label", layer.easing.label)
            put("p1x", layer.easing.p1x.toDouble())
            put("p1y", layer.easing.p1y.toDouble())
            put("p2x", layer.easing.p2x.toDouble())
            put("p2y", layer.easing.p2y.toDouble())
          })
          put("positionKeys", JSONArray().apply {
            layer.positionKeys.forEach { k ->
              put(JSONObject().put("t", k.timeSec.toDouble()).put("x", k.x.toDouble()).put("y", k.y.toDouble()))
            }
          })
          put("rotationKeys", JSONArray().apply {
            layer.rotationKeys.forEach { k ->
              put(JSONObject().put("t", k.timeSec.toDouble()).put("v", k.value.toDouble()))
            }
          })
          put("scaleKeys", JSONArray().apply {
            layer.scaleKeys.forEach { k ->
              put(JSONObject().put("t", k.timeSec.toDouble()).put("v", k.value.toDouble()))
            }
          })
          put("opacityKeys", JSONArray().apply {
            layer.opacityKeys.forEach { k ->
              put(JSONObject().put("t", k.timeSec.toDouble()).put("v", k.value.toDouble()))
            }
          })
          put("drawStrokes", JSONArray().apply {
            layer.drawStrokes.forEach { stroke ->
              put(JSONArray().apply {
                stroke.forEach { pt ->
                  put(JSONObject().put("x", pt.x.toDouble()).put("y", pt.y.toDouble()))
                }
              })
            }
          })
          put("effects", JSONArray().apply {
            layer.effects.forEach { fx ->
              put(JSONObject().apply {
                put("id", fx.id)
                put("name", fx.name)
                put("category", fx.category.name)
                put("paramLabel", fx.paramLabel)
                put("minVal", fx.minVal.toDouble())
                put("maxVal", fx.maxVal.toDouble())
                put("value", fx.value.toDouble())
              })
            }
          })
        })
      }
      put("layers", layersArr)
    }
  }

  fun projectFromJson(obj: JSONObject): StudioProject {
    val layersArr = obj.optJSONArray("layers") ?: JSONArray()
    val layers = buildList {
      for (i in 0 until layersArr.length()) {
        val lo = layersArr.getJSONObject(i)
        val eObj = lo.optJSONObject("easing")
        val easing = if (eObj != null) {
          EasingCurve(
            label = eObj.optString("label", "Ease In-Out"),
            p1x = eObj.optDouble("p1x", 0.42).toFloat(),
            p1y = eObj.optDouble("p1y", 0.0).toFloat(),
            p2x = eObj.optDouble("p2x", 0.58).toFloat(),
            p2y = eObj.optDouble("p2y", 1.0).toFloat(),
          )
        } else {
          EasingCurve()
        }
        val posArr = lo.optJSONArray("positionKeys") ?: JSONArray()
        val posKeys = buildList {
          for (j in 0 until posArr.length()) {
            val k = posArr.getJSONObject(j)
            add(Keyframe2D(k.optDouble("t", 0.0).toFloat(), k.optDouble("x", 0.0).toFloat(), k.optDouble("y", 0.0).toFloat()))
          }
        }.ifEmpty { listOf(Keyframe2D(0f, 0f, 0f)) }

        fun read1D(key: String, def: Float): List<Keyframe1D> {
          val a = lo.optJSONArray(key) ?: JSONArray()
          return buildList {
            for (j in 0 until a.length()) {
              val k = a.getJSONObject(j)
              add(Keyframe1D(k.optDouble("t", 0.0).toFloat(), k.optDouble("v", def.toDouble()).toFloat()))
            }
          }.ifEmpty { listOf(Keyframe1D(0f, def)) }
        }

        val strokesArr = lo.optJSONArray("drawStrokes") ?: JSONArray()
        val strokes = buildList {
          for (j in 0 until strokesArr.length()) {
            val sa = strokesArr.getJSONArray(j)
            add(buildList {
              for (k in 0 until sa.length()) {
                val pt = sa.getJSONObject(k)
                add(DrawPointNorm(pt.optDouble("x", 0.5).toFloat(), pt.optDouble("y", 0.5).toFloat()))
              }
            })
          }
        }

        val fxArr = lo.optJSONArray("effects") ?: JSONArray()
        val fxList = buildList {
          for (j in 0 until fxArr.length()) {
            val fo = fxArr.getJSONObject(j)
            val cat = runCatching { EffectCategory.valueOf(fo.optString("category", "GLOW")) }.getOrDefault(EffectCategory.GLOW)
            add(
              AppliedEffect(
                id = fo.optString("id", "electric_glow"),
                name = fo.optString("name", "Electric Glow"),
                category = cat,
                paramLabel = fo.optString("paramLabel", "Intensity"),
                minVal = fo.optDouble("minVal", 0.0).toFloat(),
                maxVal = fo.optDouble("maxVal", 40.0).toFloat(),
                value = fo.optDouble("value", 16.0).toFloat(),
              ),
            )
          }
        }

        val layerType = runCatching { LayerType.valueOf(lo.optString("type", "SHAPE")) }.getOrDefault(LayerType.SHAPE)
        val shapeKind = runCatching { ShapeKind.valueOf(lo.optString("shapeKind", "ROUNDED_RECT")) }.getOrDefault(ShapeKind.ROUNDED_RECT)
        add(
          LayerTrack(
            id = lo.optString("id", "layer_$i"),
            name = lo.optString("name", "Layer ${i + 1}"),
            type = layerType,
            startSec = lo.optDouble("startSec", 0.0).toFloat(),
            durationSec = lo.optDouble("durationSec", 4.0).toFloat(),
            colorHex = lo.optLong("colorHex", 0xFF5B6BFF),
            visible = lo.optBoolean("visible", true),
            locked = lo.optBoolean("locked", false),
            textContent = lo.optString("textContent", "KYTHERA"),
            fontSizeSp = lo.optDouble("fontSizeSp", 32.0).toFloat(),
            shapeKind = shapeKind,
            mediaUri = lo.optString("mediaUri", "").takeIf { it.isNotBlank() && it != "null" },
            drawStrokes = strokes,
            anchorX = lo.optDouble("anchorX", 0.5).toFloat(),
            anchorY = lo.optDouble("anchorY", 0.5).toFloat(),
            motionBlurEnabled = lo.optBoolean("motionBlurEnabled", false),
            motionBlurShutter = lo.optDouble("motionBlurShutter", 180.0).toFloat(),
            easing = easing,
            positionKeys = posKeys,
            rotationKeys = read1D("rotationKeys", 0f),
            scaleKeys = read1D("scaleKeys", 100f),
            opacityKeys = read1D("opacityKeys", 100f),
            effects = fxList,
          ),
        )
      }
    }

    return StudioProject(
      id = obj.optString("id", "proj_${System.currentTimeMillis()}"),
      name = obj.optString("name", "Imported Project"),
      aspectRatio = AspectRatioOption.fromLabel(obj.optString("aspectRatio", "9:16")),
      fps = obj.optInt("fps", 60),
      resolutionLabel = obj.optString("resolutionLabel", "1080p"),
      durationSec = obj.optDouble("durationSec", 8.0).toFloat().coerceAtLeast(2f),
      updatedLabel = obj.optString("updatedLabel", "Imported"),
      accentColorHex = obj.optLong("accentColorHex", 0xFF5B6BFF),
      layers = layers,
    )
  }
}

fun Color.Companion.fromHexLong(hex: Long): Color = Color(hex)
