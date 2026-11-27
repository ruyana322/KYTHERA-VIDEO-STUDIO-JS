package com.example.motion.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.ceil

sealed class ManifestParseResult {
  data class Success(val manifest: MotionManifest) : ManifestParseResult()
  data class Error(val message: String) : ManifestParseResult()
}

object ManifestSerializer {

  fun parse(jsonRaw: String): ManifestParseResult {
    return try {
      val root = JSONObject(jsonRaw)
      val judul = root.optString("judul", "").trim().ifBlank { "Proyek Motion" }
      val fps = root.optInt("fps", 30)
      if (fps != 30) {
        return ManifestParseResult.Error("FPS harus 30 (terbaca $fps fps). Sesuaikan manifest.json ke 30 fps.")
      }
      val lebar = root.optInt("lebar", 1080)
      val tinggi = root.optInt("tinggi", 1920)
      if (lebar != 1080 || tinggi != 1920) {
        return ManifestParseResult.Error(
          "Resolusi wajib portrait 1080x1920 (terbaca ${lebar}x${tinggi}).",
        )
      }

      val voiceover = root.optString("voiceover", "").trim()
      val musik = root.optString("musik", "").trim()
      val voiceOffset = root.optDouble("voice_offset_detik", 1.0).toFloat().coerceAtLeast(0f)
      val tailDetik = root.optDouble("tail_detik", 1.5).toFloat().coerceAtLeast(0f)
      val voiceDuration = root.optDouble("durasi_voiceover_detik", 0.0).toFloat().coerceAtLeast(0f)
      val gaya = ProjectStylePreset.fromId(root.optString("gaya", "gelap_tech"))
      val transisiDurasi = root.optDouble("transisi_durasi_detik", 0.5).toFloat().coerceIn(0f, 1.5f)

      val asetMap = linkedMapOf<String, String>()
      root.optJSONObject("aset")?.let { asetObj ->
        val keys = asetObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val v = asetObj.optString(k, "").trim()
          if (k.isNotBlank() && v.isNotBlank()) asetMap[k] = v
        }
      }

      val paletteMap = linkedMapOf<String, String>()
      root.optJSONObject("palette")?.let { palObj ->
        val keys = palObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val v = palObj.optString(k, "").trim()
          if (k.isNotBlank() && v.isNotBlank()) paletteMap[k] = v
        }
      }

      val scenesArr = root.optJSONArray("scenes")
      if (scenesArr == null || scenesArr.length() == 0) {
        return ManifestParseResult.Error("Daftar 'scenes' kosong di manifest.json. Minimal harus ada 1 adegan.")
      }

      val scenes = mutableListOf<SceneSpec>()
      for (i in 0 until scenesArr.length()) {
        val sObj = scenesArr.optJSONObject(i) ?: continue
        val sId = sObj.optString("id", "sc_${i + 1}").ifBlank { "sc_${i + 1}" }
        val sName = sObj.optString("nama", "Adegan ${i + 1}").ifBlank { "Adegan ${i + 1}" }
        val mulai = sObj.optDouble("mulai", 0.0).toFloat().coerceAtLeast(0f)
        val selesai = sObj.optDouble("selesai", (mulai + 3f).toDouble()).toFloat()
        if (selesai <= mulai) {
          return ManifestParseResult.Error("Adegan '$sId' memiliki waktu selesai <= mulai.")
        }
        val aksen = sObj.optString("aksen", "").ifBlank {
          paletteMap[sId] ?: gaya.accentHexes[i % gaya.accentHexes.size]
        }
        val latar = if (sObj.has("latar") && !sObj.isNull("latar")) {
          sObj.optString("latar", "").ifBlank { null }
        } else {
          null
        }

        val elemArr = sObj.optJSONArray("elemen") ?: JSONArray()
        val elements = mutableListOf<SceneElement>()
        for (j in 0 until elemArr.length()) {
          val eObj = elemArr.optJSONObject(j) ?: continue
          elements += parseElement(eObj, sId, j, aksen)
        }
        scenes += SceneSpec(
          id = sId,
          nama = sName,
          mulai = mulai,
          selesai = selesai,
          aksen = aksen,
          latar = latar,
          elemen = elements,
        )
      }

      val sortedScenes = scenes.sortedBy { it.mulai }
      val manifest = MotionManifest(
        judul = judul,
        fps = fps,
        lebar = lebar,
        tinggi = tinggi,
        voiceover = voiceover,
        musik = musik,
        voiceOffsetDetik = voiceOffset,
        tailDetik = tailDetik,
        durasiVoiceoverDetik = voiceDuration,
        gaya = gaya,
        transisiDurasiDetik = transisiDurasi,
        aset = asetMap,
        palette = paletteMap,
        scenes = sortedScenes,
      )

      val maxDuration = manifest.effectiveDurationSec()
      if (maxDuration > 60f) {
        return ManifestParseResult.Error(
          "Durasi total (${ceil(maxDuration.toDouble()).toInt()} detik) melebihi batas maksimal 60 detik.",
        )
      }

      ManifestParseResult.Success(manifest)
    } catch (e: Exception) {
      ManifestParseResult.Error("Gagal membaca manifest.json: ${e.message ?: "format JSON tidak valid"}")
    }
  }

  private fun parseElement(obj: JSONObject, sceneId: String, index: Int, sceneAccent: String): SceneElement {
    val tipe = ElementType.fromJson(obj.optString("tipe", "teks"))
    val id = obj.optString("id", "${sceneId}_el_${index + 1}").ifBlank { "${sceneId}_el_${index + 1}" }
    val posArr = obj.optJSONArray("posisi")
    val rawX = posArr?.optDouble(0, obj.optDouble("x", 540.0))?.toFloat()
      ?: obj.optDouble("x", 540.0).toFloat()
    val rawY = posArr?.optDouble(1, obj.optDouble("y", 960.0))?.toFloat()
      ?: obj.optDouble("y", 960.0).toFloat()
    val normX = if (rawX > 0f && rawX <= 1.0f) rawX * 1080f else rawX
    val normY = if (rawY > 0f && rawY <= 1.0f) rawY * 1920f else rawY

    val ukArr = obj.optJSONArray("ukuran_kotak")
    val defaultW = when (tipe) {
      ElementType.PANEL, ElementType.KARTU -> 840f
      ElementType.GELOMBANG_SUARA -> 780f
      else -> 820f
    }
    val defaultH = when (tipe) {
      ElementType.PANEL -> 300f
      ElementType.KARTU -> 240f
      ElementType.GELOMBANG_SUARA -> 140f
      else -> 260f
    }
    val lebar = (ukArr?.optDouble(0)?.toFloat() ?: obj.optDouble("lebar", defaultW.toDouble()).toFloat())
    val tinggi = (ukArr?.optDouble(1)?.toFloat() ?: obj.optDouble("tinggi", defaultH.toDouble()).toFloat())

    val kataArr = obj.optJSONArray("kata") ?: obj.optJSONArray("daftar_kata")
    val kataList = mutableListOf<KineticWord>()
    if (kataArr != null) {
      for (k in 0 until kataArr.length()) {
        val kw = kataArr.optJSONObject(k)
        if (kw != null) {
          kataList += KineticWord(
            teks = kw.optString("teks", kw.optString("kata", "")),
            cue = kw.optDouble("cue", 0.0).toFloat(),
          )
        } else {
          val str = kataArr.optString(k, "")
          if (str.isNotBlank()) kataList += KineticWord(str, k * 0.35f)
        }
      }
    }

    val capArr = obj.optJSONArray("daftar") ?: obj.optJSONArray("caption")
    val captionList = mutableListOf<CaptionLine>()
    if (capArr != null) {
      for (c in 0 until capArr.length()) {
        val cp = capArr.optJSONObject(c) ?: continue
        captionList += CaptionLine(
          teks = cp.optString("teks", ""),
          masuk = cp.optDouble("masuk", 0.0).toFloat(),
          keluar = cp.optDouble("keluar", 2.0).toFloat(),
        )
      }
    }

    return SceneElement(
      id = id,
      tipe = tipe,
      cue = obj.optDouble("cue", 0.0).toFloat().coerceAtLeast(0f),
      durasi = obj.optDouble("durasi", 0.55).toFloat().coerceIn(0.1f, 30f),
      x = normX,
      y = normY,
      lebar = lebar,
      tinggi = tinggi,
      skala = obj.optDouble("skala", 1.0).toFloat().coerceIn(0.1f, 5f),
      rotasi = obj.optDouble("rotasi", 0.0).toFloat(),
      warna = obj.optString("warna", "").ifBlank {
        when (tipe) {
          ElementType.TEKS, ElementType.KATA_KINETIK, ElementType.CAPTION -> "#FFFFFF"
          else -> sceneAccent
        }
      },
      isi = obj.optString("isi", obj.optString("teks", "")),
      font = obj.optString("font", "inter").ifBlank { "inter" },
      ukuran = obj.optDouble("ukuran", 62.0).toFloat().coerceIn(16f, 220f),
      ikonNama = obj.optString("ikon", obj.optString("nama", "shield")).ifBlank { "shield" },
      asetNama = obj.optString("aset", "").trim(),
      bob = obj.optDouble("bob", 14.0).toFloat().coerceIn(0f, 80f),
      radius = obj.optDouble("radius", 28.0).toFloat().coerceIn(0f, 120f),
      jumlah = obj.optInt("jumlah", 24).coerceIn(4, 120),
      x2 = obj.optDouble("x2", (normX + 340f).toDouble()).toFloat(),
      y2 = obj.optDouble("y2", normY.toDouble()).toFloat(),
      tebal = obj.optDouble("tebal", 8.0).toFloat().coerceIn(1f, 40f),
      glitch = obj.optBoolean("glitch", false),
      kataList = kataList,
      captionList = captionList,
    )
  }

  fun toJsonString(manifest: MotionManifest): String {
    val root = JSONObject().apply {
      put("judul", manifest.judul)
      put("fps", manifest.fps)
      put("lebar", manifest.lebar)
      put("tinggi", manifest.tinggi)
      put("voiceover", manifest.voiceover)
      put("musik", manifest.musik)
      put("voice_offset_detik", manifest.voiceOffsetDetik.toDouble())
      put("tail_detik", manifest.tailDetik.toDouble())
      if (manifest.durasiVoiceoverDetik > 0f) {
        put("durasi_voiceover_detik", manifest.durasiVoiceoverDetik.toDouble())
      }
      put("gaya", manifest.gaya.id)
      put("transisi_durasi_detik", manifest.transisiDurasiDetik.toDouble())

      put("aset", JSONObject().apply {
        manifest.aset.forEach { (k, v) -> put(k, v) }
      })
      put("palette", JSONObject().apply {
        manifest.palette.forEach { (k, v) -> put(k, v) }
      })
      put("scenes", JSONArray().apply {
        manifest.scenes.forEach { scene ->
          put(JSONObject().apply {
            put("id", scene.id)
            put("nama", scene.nama)
            put("mulai", scene.mulai.toDouble())
            put("selesai", scene.selesai.toDouble())
            put("aksen", scene.aksen)
            if (!scene.latar.isNullOrBlank()) put("latar", scene.latar)
            put("elemen", JSONArray().apply {
              scene.elemen.forEach { el ->
                put(elementToJson(el))
              }
            })
          })
        }
      })
    }
    return root.toString(2)
  }

  private fun elementToJson(el: SceneElement): JSONObject = JSONObject().apply {
    put("id", el.id)
    put("tipe", el.tipe.jsonKey)
    put("cue", el.cue.toDouble())
    put("durasi", el.durasi.toDouble())
    put("posisi", JSONArray().apply {
      put(el.x.toDouble())
      put(el.y.toDouble())
    })
    put("skala", el.skala.toDouble())
    if (el.rotasi != 0f) put("rotasi", el.rotasi.toDouble())
    put("warna", el.warna)
    if (el.glitch) put("glitch", true)

    when (el.tipe) {
      ElementType.TEKS -> {
        put("isi", el.isi)
        put("ukuran", el.ukuran.toDouble())
        put("font", el.font)
      }
      ElementType.KATA_KINETIK -> {
        put("ukuran", el.ukuran.toDouble())
        put("font", el.font)
        put("kata", JSONArray().apply {
          el.kataList.forEach { kw ->
            put(JSONObject().apply {
              put("teks", kw.teks)
              put("cue", kw.cue.toDouble())
            })
          }
        })
      }
      ElementType.IKON -> {
        put("ikon", el.ikonNama)
        put("ukuran", el.ukuran.toDouble())
      }
      ElementType.KARAKTER -> {
        put("aset", el.asetNama)
        put("tinggi", el.tinggi.toDouble())
        put("bob", el.bob.toDouble())
      }
      ElementType.PANEL, ElementType.KARTU -> {
        put("isi", el.isi)
        put("lebar", el.lebar.toDouble())
        put("tinggi", el.tinggi.toDouble())
        put("radius", el.radius.toDouble())
      }
      ElementType.GELOMBANG_SUARA -> {
        put("lebar", el.lebar.toDouble())
        put("tinggi", el.tinggi.toDouble())
        put("jumlah", el.jumlah)
      }
      ElementType.PARTIKEL, ElementType.HUJAN_DIGITAL -> {
        put("jumlah", el.jumlah)
      }
      ElementType.GARIS -> {
        put("x2", el.x2.toDouble())
        put("y2", el.y2.toDouble())
        put("tebal", el.tebal.toDouble())
      }
      ElementType.CAPTION -> {
        put("ukuran", el.ukuran.toDouble())
        put("font", el.font)
        put("daftar", JSONArray().apply {
          el.captionList.forEach { cp ->
            put(JSONObject().apply {
              put("teks", cp.teks)
              put("masuk", cp.masuk.toDouble())
              put("keluar", cp.keluar.toDouble())
            })
          }
        })
      }
    }
  }

  fun validateForRender(manifest: MotionManifest, projectDir: File): List<ValidationWarning> {
    val warnings = mutableListOf<ValidationWarning>()
    val totalDur = manifest.effectiveDurationSec()
    if (totalDur > 60f) {
      warnings += ValidationWarning(
        isBlocking = true,
        message = "Durasi proyek (${ceil(totalDur.toDouble()).toInt()} dtk) melebihi batas maksimal 60 detik.",
      )
    }
    if (manifest.scenes.isEmpty()) {
      warnings += ValidationWarning(
        isBlocking = true,
        message = "Tidak ada adegan di timeline. Tambahkan minimal 1 adegan.",
      )
    }
    if (manifest.voiceover.isNotBlank()) {
      val voFile = File(File(projectDir, "aset"), manifest.voiceover)
      if (!voFile.exists()) {
        warnings += ValidationWarning(
          isBlocking = true,
          message = "File voiceover '${manifest.voiceover}' tidak ditemukan di folder aset/.",
        )
      }
    }
    if (manifest.musik.isNotBlank()) {
      val musFile = File(File(projectDir, "aset"), manifest.musik)
      if (!musFile.exists()) {
        warnings += ValidationWarning(
          isBlocking = false,
          message = "File musik '${manifest.musik}' tidak ditemukan; video akan dirender tanpa musik latar.",
        )
      }
    }

    manifest.aset.forEach { (key, relName) ->
      val f = File(File(projectDir, "aset"), relName)
      if (!f.exists()) {
        warnings += ValidationWarning(
          isBlocking = false,
          message = "Aset '$key' ($relName) belum ada di folder aset/.",
        )
      }
    }

    manifest.scenes.forEach { sc ->
      sc.elemen.forEach { el ->
        if (el.tipe == ElementType.KARAKTER && el.asetNama.isNotBlank() && !manifest.aset.containsKey(el.asetNama)) {
          warnings += ValidationWarning(
            isBlocking = false,
            message = "Karakter di '${sc.nama}' merujuk aset '${el.asetNama}' yang belum terdaftar.",
          )
        }
        if (el.tipe == ElementType.TEKS || el.tipe == ElementType.KATA_KINETIK || el.tipe == ElementType.CAPTION) {
          val outOfSafeZone = el.y < SafeZones.TOP_PX ||
            el.y > (SafeZones.HEIGHT - SafeZones.BOTTOM_PX) ||
            el.x < SafeZones.SIDE_PX ||
            el.x > (SafeZones.WIDTH - SafeZones.SIDE_PX)
          if (outOfSafeZone) {
            warnings += ValidationWarning(
              isBlocking = false,
              message = "Teks di '${sc.nama}' mendekati tepi zona aman (atas 120px / bawah 300px / samping 60px).",
            )
          }
        }
      }
    }

    return warnings
  }
}
