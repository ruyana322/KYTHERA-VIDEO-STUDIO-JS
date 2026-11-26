package com.example.media

import android.media.MediaCodecList
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

data class RenderSpec(
  val projectId: String,
  val width: Int,
  val height: Int,
  val fps: Int,
  val bitrateMbps: Int,
  val containerFormat: String,
)

data class DeviceDiagnostics(
  val runtime: String,
  val abis: List<String>,
  val isArm64Ready: Boolean,
  val hardwareEncoders: List<String>,
) {
  fun toJsonString(): String {
    return JSONObject().apply {
      put("runtime", runtime)
      put("abis", JSONArray(abis))
      put("isArm64Ready", isArm64Ready)
      put("hardwareEncoders", JSONArray(hardwareEncoders))
    }.toString()
  }
}

/**
 * Architectural boundary for future native FFmpeg and Android MediaCodec rendering integration.
 */
interface MediaEngineGateway {
  fun inspectDiagnostics(): DeviceDiagnostics
  fun prepareRenderSession(spec: RenderSpec): Boolean
}

class MediaCodecCapabilityInspector : MediaEngineGateway {

  override fun inspectDiagnostics(): DeviceDiagnostics {
    val supportedAbis = Build.SUPPORTED_ABIS.toList()
    val arm64Supported = supportedAbis.any { it.equals("arm64-v8a", ignoreCase = true) }

    val targetMimes = setOf(
      "video/avc",
      "video/hevc",
      "video/x-vnd.on2.vp9",
      "audio/mp4a-latm",
    )

    val encoders = runCatching {
      MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        .asSequence()
        .filter { it.isEncoder }
        .filter { info -> info.supportedTypes.any { it in targetMimes } }
        .map { info ->
          val mimes = info.supportedTypes.filter { it in targetMimes }.joinToString("/")
          "${info.name} ($mimes)"
        }
        .distinct()
        .take(6)
        .toList()
    }.getOrDefault(emptyList())

    return DeviceDiagnostics(
      runtime = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) · ${Build.MODEL}",
      abis = supportedAbis.ifEmpty { listOf("arm64-v8a") },
      isArm64Ready = arm64Supported,
      hardwareEncoders = encoders.ifEmpty { listOf("MediaCodec fallback ready") },
    )
  }

  override fun prepareRenderSession(spec: RenderSpec): Boolean {
    // Prototype mode: future native MediaCodec/FFmpeg pipeline entry point.
    return spec.width > 0 && spec.height > 0 && spec.fps > 0
  }
}
