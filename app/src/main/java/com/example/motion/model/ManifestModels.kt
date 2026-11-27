package com.example.motion.model

import kotlin.math.ceil

enum class ProjectStylePreset(
  val id: String,
  val label: String,
  val subtitle: String,
  val backgroundHex: String,
  val secondaryBgHex: String,
  val textHex: String,
  val accentHexes: List<String>,
) {
  GELAP_TECH(
    id = "gelap_tech",
    label = "Gelap-Tech",
    subtitle = "Latar gelap modern, grid neon & aksen siber",
    backgroundHex = "#080C18",
    secondaryBgHex = "#101A33",
    textHex = "#FFFFFF",
    accentHexes = listOf("#00E5FF", "#5B6BFF", "#00E676", "#FF5252", "#FFB300", "#AA00FF", "#2979FF"),
  ),
  TERANG_BERSIH(
    id = "terang_bersih",
    label = "Terang-Bersih",
    subtitle = "Latar terang minimalis dengan aksen editorial",
    backgroundHex = "#F4F7FF",
    secondaryBgHex = "#E4ECFF",
    textHex = "#141A2E",
    accentHexes = listOf("#2962FF", "#00BFA5", "#FF6D00", "#6200EA", "#0091EA", "#D500F9", "#304FFE"),
  ),
  NETRAL(
    id = "netral",
    label = "Netral",
    subtitle = "Kontras seimbang untuk presentasi & edukasi",
    backgroundHex = "#141824",
    secondaryBgHex = "#20273B",
    textHex = "#FFFFFF",
    accentHexes = listOf("#38BDF8", "#818CF8", "#34D399", "#FBBF24", "#F472B6", "#60A5FA", "#A78BFA"),
  );

  companion object {
    fun fromId(raw: String?): ProjectStylePreset {
      val norm = raw?.trim()?.lowercase().orEmpty()
      return entries.firstOrNull {
        it.id == norm || it.name.lowercase() == norm || it.label.lowercase() == norm
      } ?: GELAP_TECH
    }
  }
}

enum class ElementType(val jsonKey: String, val label: String) {
  TEKS("teks", "Teks"),
  KATA_KINETIK("kataKinetik", "Kata Kinetik"),
  IKON("ikon", "Ikon Vektor"),
  KARAKTER("karakter", "Karakter"),
  PANEL("panel", "Panel"),
  GELOMBANG_SUARA("gelombangSuara", "Gelombang Suara"),
  PARTIKEL("partikel", "Partikel"),
  HUJAN_DIGITAL("hujanDigital", "Hujan Digital"),
  KARTU("kartu", "Kartu"),
  GARIS("garis", "Garis Tumbuh"),
  CAPTION("caption", "Caption");

  companion object {
    fun fromJson(raw: String?): ElementType {
      val key = raw?.trim().orEmpty()
      return entries.firstOrNull {
        it.jsonKey.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
      } ?: TEKS
    }
  }
}

data class KineticWord(
  val teks: String,
  val cue: Float,
)

data class CaptionLine(
  val teks: String,
  val masuk: Float,
  val keluar: Float,
)

data class SceneElement(
  val id: String,
  val tipe: ElementType,
  val cue: Float = 0f,
  val durasi: Float = 0.6f,
  val x: Float = 540f,
  val y: Float = 960f,
  val lebar: Float = 820f,
  val tinggi: Float = 260f,
  val skala: Float = 1.0f,
  val rotasi: Float = 0f,
  val warna: String = "#FFFFFF",
  val isi: String = "",
  val font: String = "inter",
  val ukuran: Float = 64f,
  val ikonNama: String = "shield",
  val asetNama: String = "",
  val bob: Float = 14f,
  val radius: Float = 28f,
  val jumlah: Int = 24,
  val x2: Float = 880f,
  val y2: Float = 960f,
  val tebal: Float = 8f,
  val glitch: Boolean = false,
  val kataList: List<KineticWord> = emptyList(),
  val captionList: List<CaptionLine> = emptyList(),
) {
  fun displayLabel(): String = when (tipe) {
    ElementType.TEKS -> isi.take(22).ifBlank { "Teks" }
    ElementType.KATA_KINETIK -> kataList.joinToString(" ") { it.teks }.take(22).ifBlank { "Kata Kinetik" }
    ElementType.IKON -> "Ikon: $ikonNama"
    ElementType.KARAKTER -> "Karakter: ${asetNama.ifBlank { "-" }}"
    ElementType.CAPTION -> "Caption (${captionList.size})"
    else -> tipe.label
  }
}

data class SceneSpec(
  val id: String,
  val nama: String,
  val mulai: Float,
  val selesai: Float,
  val aksen: String = "#00E5FF",
  val latar: String? = null,
  val elemen: List<SceneElement> = emptyList(),
)

data class MotionManifest(
  val judul: String = "Proyek Baru",
  val fps: Int = 30,
  val lebar: Int = 1080,
  val tinggi: Int = 1920,
  val voiceover: String = "",
  val musik: String = "",
  val voiceOffsetDetik: Float = 1.0f,
  val tailDetik: Float = 1.5f,
  val durasiVoiceoverDetik: Float = 0f,
  val gaya: ProjectStylePreset = ProjectStylePreset.GELAP_TECH,
  val transisiDurasiDetik: Float = 0.5f,
  val aset: Map<String, String> = emptyMap(),
  val palette: Map<String, String> = emptyMap(),
  val scenes: List<SceneSpec> = emptyList(),
) {
  fun effectiveDurationSec(): Float {
    val sceneEnd = scenes.maxOfOrNull { it.selesai } ?: 0f
    val audioTotal = if (durasiVoiceoverDetik > 0f) {
      ceil((durasiVoiceoverDetik + voiceOffsetDetik + tailDetik).toDouble()).toFloat()
    } else {
      0f
    }
    return maxOf(audioTotal, sceneEnd, 1f)
  }

  fun totalFrames(): Int = (ceil(effectiveDurationSec().toDouble()).toInt().coerceIn(1, 60)) * fps
}

data class ProjectSummary(
  val id: String,
  val judul: String,
  val durasiDetik: Float,
  val updatedAtMs: Long,
  val thumbnailPath: String?,
  val sceneCount: Int,
  val assetCount: Int,
  val gaya: ProjectStylePreset,
)

enum class RenderQuality(val bitrateBps: Int, val label: String, val subtitle: String) {
  NORMAL(12_000_000, "Normal (12 Mbps)", "Tajam & jernih untuk unggahan Reels/Shorts"),
  HEMAT(8_000_000, "Hemat (8 Mbps)", "Ukuran file lebih ringkas, tetap 1080×1920 30 fps"),
}

sealed interface RenderStatus {
  data object Idle : RenderStatus
  data class Preparing(val message: String) : RenderStatus
  data class Rendering(
    val frame: Int,
    val totalFrames: Int,
    val etaSeconds: Int,
    val phaseLabel: String,
  ) : RenderStatus {
    val progress: Float get() = if (totalFrames > 0) frame.toFloat() / totalFrames.toFloat() else 0f
  }

  data class Completed(
    val uriString: String,
    val fileName: String,
    val sizeBytes: Long,
    val durationSec: Float,
  ) : RenderStatus

  data object Cancelled : RenderStatus
  data class Failed(val message: String) : RenderStatus
}

data class ValidationWarning(
  val isBlocking: Boolean,
  val message: String,
)

object SafeZones {
  const val WIDTH = 1080f
  const val HEIGHT = 1920f
  const val TOP_PX = 120f
  const val BOTTOM_PX = 300f
  const val SIDE_PX = 60f
}

val SUPPORTED_VECTOR_ICONS = listOf(
  "image", "warn", "lock", "shield", "key", "pin",
  "phone", "bubble", "human", "chip", "check", "cross",
)
