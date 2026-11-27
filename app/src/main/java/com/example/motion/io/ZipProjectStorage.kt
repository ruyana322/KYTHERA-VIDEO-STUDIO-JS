package com.example.motion.io

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import com.example.motion.audio.AudioDecoder
import com.example.motion.model.ManifestParseResult
import com.example.motion.model.ManifestSerializer
import com.example.motion.model.MotionManifest
import com.example.motion.model.ProjectSummary
import com.example.motion.model.SampleManifestProvider
import com.example.motion.render.MotionRenderer
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.ceil

sealed class ZipImportOutcome {
  data class Success(val projectId: String, val manifest: MotionManifest) : ZipImportOutcome()
  data class Failure(val errorMessage: String) : ZipImportOutcome()
}

class ZipProjectStorage(private val context: Context) {

  private val projectsRoot: File
    get() = File(context.filesDir, "projects").apply { mkdirs() }

  fun projectDir(projectId: String): File = File(projectsRoot, projectId)

  fun listProjects(): List<ProjectSummary> {
    val dirs = projectsRoot.listFiles()?.filter { it.isDirectory } ?: return emptyList()
    return dirs.mapNotNull { dir ->
      val manifestFile = File(dir, "manifest.json")
      if (!manifestFile.exists()) return@mapNotNull null
      val raw = runCatching { manifestFile.readText(StandardCharsets.UTF_8) }.getOrNull() ?: return@mapNotNull null
      val parsed = ManifestSerializer.parse(raw)
      if (parsed !is ManifestParseResult.Success) return@mapNotNull null
      val m = parsed.manifest
      val metaFile = File(dir, "meta.json")
      val updatedAt = runCatching {
        if (metaFile.exists()) JSONObject(metaFile.readText()).optLong("updatedAt", manifestFile.lastModified())
        else manifestFile.lastModified()
      }.getOrDefault(manifestFile.lastModified())
      val thumbFile = File(dir, "thumbnail.jpg")
      ProjectSummary(
        id = dir.name,
        judul = m.judul,
        durasiDetik = m.effectiveDurationSec(),
        updatedAtMs = updatedAt,
        thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else null,
        sceneCount = m.scenes.size,
        assetCount = m.aset.size,
        gaya = m.gaya,
      )
    }.sortedByDescending { it.updatedAtMs }
  }

  fun loadProject(projectId: String): MotionManifest? {
    val f = File(projectDir(projectId), "manifest.json")
    if (!f.exists()) return null
    val raw = runCatching { f.readText(StandardCharsets.UTF_8) }.getOrNull() ?: return null
    return (ManifestSerializer.parse(raw) as? ManifestParseResult.Success)?.manifest
  }

  fun saveProject(projectId: String, manifest: MotionManifest, generateThumb: Boolean = true) {
    val dir = projectDir(projectId).apply { mkdirs() }
    File(dir, "aset").mkdirs()
    val json = ManifestSerializer.toJsonString(manifest)
    File(dir, "manifest.json").writeText(json, StandardCharsets.UTF_8)
    val meta = JSONObject().put("updatedAt", System.currentTimeMillis())
    File(dir, "meta.json").writeText(meta.toString(), StandardCharsets.UTF_8)

    if (generateThumb) {
      generateProjectThumbnail(projectId, manifest)
    }
  }

  fun generateProjectThumbnail(projectId: String, manifest: MotionManifest) {
    val dir = projectDir(projectId)
    val thumbFile = File(dir, "thumbnail.jpg")
    val targetT = minOf(3.0f, (manifest.effectiveDurationSec() * 0.4f).coerceAtLeast(0.5f))
    val bmp = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
    val renderer = MotionRenderer(context, dir, manifest)
    try {
      val canvas = Canvas(bmp)
      renderer.renderFrame(canvas, targetT, showSafeZones = false, selectedElementId = null)
      FileOutputStream(thumbFile).use { fos ->
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
      }
    } catch (_: Throwable) {
      // Abaikan kegagalan thumbnail non-kritis
    } finally {
      renderer.release()
      bmp.recycle()
    }
  }

  fun renameProject(projectId: String, newTitle: String): MotionManifest? {
    val cur = loadProject(projectId) ?: return null
    val updated = cur.copy(judul = newTitle.trim().ifBlank { cur.judul })
    saveProject(projectId, updated, generateThumb = false)
    return updated
  }

  fun duplicateProject(projectId: String): String? {
    val srcDir = projectDir(projectId)
    if (!srcDir.exists()) return null
    val cur = loadProject(projectId) ?: return null
    val newId = newProjectId()
    val dstDir = projectDir(newId)
    srcDir.copyRecursively(dstDir, overwrite = true)
    val copyManifest = cur.copy(judul = "${cur.judul} (Salinan)")
    saveProject(newId, copyManifest, generateThumb = true)
    return newId
  }

  fun deleteProject(projectId: String) {
    projectDir(projectId).deleteRecursively()
  }

  fun createSampleProject51s(): Pair<String, MotionManifest> {
    val id = newProjectId()
    val dir = projectDir(id)
    val asetDir = File(dir, "aset").apply { mkdirs() }
    val manifest = SampleManifestProvider.create51SecondManifest()

    BitmapAssetProcessor.createSampleCharacterPng(File(asetDir, "karakter_host.png"))
    AudioDecoder.writeDemoWavFile(
      targetFile = File(asetDir, "voiceover_demo.wav"),
      durationSec = 48.5f,
      isMusicPad = false,
    )
    AudioDecoder.writeDemoWavFile(
      targetFile = File(asetDir, "musik_demo.wav"),
      durationSec = 16.0f,
      isMusicPad = true,
    )
    saveProject(id, manifest, generateThumb = true)
    return id to manifest
  }

  /**
   * Mengimpor ZIP proyek dari Storage Access Framework dengan perlindungan Zip-Slip,
   * batas ukuran ekstraksi, alpha-crop PNG, dan validasi durasi <= 60 detik.
   */
  fun importZip(contentResolver: ContentResolver, uri: Uri): ZipImportOutcome {
    val tempDir = File(context.cacheDir, "zip_import_${System.currentTimeMillis()}").apply { mkdirs() }
    return try {
      val canonicalTemp = tempDir.canonicalFile
      var totalBytes = 0L
      var fileCount = 0

      contentResolver.openInputStream(uri)?.use { rawIn ->
        ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
          val buf = ByteArray(16_384)
          while (true) {
            val entry = zis.nextEntry ?: break
            val entryName = entry.name.replace('\\', '/')
            if (entryName.contains("..") || entryName.startsWith("/")) {
              return ZipImportOutcome.Failure("ZIP ditolak: jalur berkas tidak aman ($entryName).")
            }
            val dest = File(canonicalTemp, entryName).canonicalFile
            if (!dest.path.startsWith(canonicalTemp.path + File.separator) && dest != canonicalTemp) {
              return ZipImportOutcome.Failure("ZIP ditolak: terdeteksi upaya zip-slip ($entryName).")
            }

            if (entry.isDirectory) {
              dest.mkdirs()
            } else {
              // Tolak skrip / biner executable untuk kepatuhan keamanan
              val ext = dest.extension.lowercase()
              if (ext in FORBIDDEN_SCRIPT_EXTS) {
                zis.closeEntry()
                continue
              }
              fileCount++
              if (fileCount > MAX_ZIP_FILES) {
                return ZipImportOutcome.Failure("ZIP ditolak: jumlah file melebihi batas $MAX_ZIP_FILES.")
              }
              dest.parentFile?.mkdirs()
              FileOutputStream(dest).use { out ->
                while (true) {
                  val n = zis.read(buf)
                  if (n <= 0) break
                  totalBytes += n
                  if (totalBytes > MAX_EXTRACTED_BYTES) {
                    return ZipImportOutcome.Failure("ZIP ditolak: ukuran ekstraksi melebihi 120 MB.")
                  }
                  out.write(buf, 0, n)
                }
              }
            }
            zis.closeEntry()
          }
        }
      } ?: return ZipImportOutcome.Failure("Gagal membuka file ZIP.")

      // Cari manifest.json di root atau satu level subfolder
      val manifestFile = findManifestFile(canonicalTemp)
        ?: return ZipImportOutcome.Failure("Berkas manifest.json tidak ditemukan di dalam ZIP.")
      val bundleRoot = manifestFile.parentFile ?: canonicalTemp

      val parsed = ManifestSerializer.parse(manifestFile.readText(StandardCharsets.UTF_8))
      if (parsed is ManifestParseResult.Error) {
        return ZipImportOutcome.Failure(parsed.message)
      }
      var manifest = (parsed as ManifestParseResult.Success).manifest

      // Normalisasi folder aset/ & proses PNG/JPG
      val asetDir = File(bundleRoot, "aset").apply { mkdirs() }
      manifest.aset.values.forEach { rel ->
        val imgFile = File(asetDir, rel)
        if (imgFile.exists() && imgFile.extension.lowercase() in IMAGE_EXTS) {
          BitmapAssetProcessor.processExistingAssetFile(imgFile)
        }
      }

      // Hitung durasi voiceover asli bila ada
      if (manifest.voiceover.isNotBlank()) {
        val voFile = File(asetDir, manifest.voiceover)
        if (voFile.exists()) {
          val measuredVo = AudioDecoder.probeDurationSec(voFile)
          if (measuredVo > 0f) {
            val total = ceil((measuredVo + manifest.voiceOffsetDetik + manifest.tailDetik).toDouble()).toFloat()
            if (total > 60f) {
              return ZipImportOutcome.Failure(
                "Proyek ditolak: durasi total (${total.toInt()} detik) melebihi batas maksimal 60 detik.",
              )
            }
            manifest = manifest.copy(durasiVoiceoverDetik = measuredVo)
          }
        }
      }

      val projectId = newProjectId()
      val targetDir = projectDir(projectId)
      bundleRoot.copyRecursively(targetDir, overwrite = true)
      saveProject(projectId, manifest, generateThumb = true)
      ZipImportOutcome.Success(projectId, manifest)
    } catch (e: Exception) {
      ZipImportOutcome.Failure("Gagal mengimpor ZIP: ${e.message ?: "berkas rusak"}")
    } finally {
      tempDir.deleteRecursively()
    }
  }

  private fun findManifestFile(root: File): File? {
    val direct = File(root, "manifest.json")
    if (direct.exists()) return direct
    return root.listFiles()?.firstOrNull { it.isDirectory }?.let { sub ->
      File(sub, "manifest.json").takeIf { it.exists() }
    }
  }

  fun exportProjectZip(projectId: String, contentResolver: ContentResolver, destUri: Uri): Boolean {
    val dir = projectDir(projectId)
    if (!dir.exists()) return false
    return runCatching {
      contentResolver.openOutputStream(destUri)?.use { rawOut ->
        ZipOutputStream(BufferedOutputStream(rawOut)).use { zos ->
          val manifestFile = File(dir, "manifest.json")
          if (manifestFile.exists()) {
            zos.putNextEntry(ZipEntry("manifest.json"))
            manifestFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
          }
          val asetDir = File(dir, "aset")
          if (asetDir.exists()) {
            asetDir.walkTopDown().filter { it.isFile }.forEach { f ->
              val rel = "aset/" + f.relativeTo(asetDir).path.replace('\\', '/')
              zos.putNextEntry(ZipEntry(rel))
              f.inputStream().use { it.copyTo(zos) }
              zos.closeEntry()
            }
          }
        }
      }
      true
    }.getOrDefault(false)
  }

  fun copyAudioUriToProject(
    contentResolver: ContentResolver,
    projectId: String,
    uri: Uri,
    fileName: String,
  ): File {
    val asetDir = File(projectDir(projectId), "aset").apply { mkdirs() }
    val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "audio.wav" }
    val dst = File(asetDir, cleanName)
    contentResolver.openInputStream(uri)?.use { input ->
      FileOutputStream(dst).use { out -> input.copyTo(out) }
    }
    return dst
  }

  fun newProjectId(): String = "mg_${UUID.randomUUID().toString().take(8)}"

  companion object {
    private const val MAX_EXTRACTED_BYTES = 120L * 1024L * 1024L
    private const val MAX_ZIP_FILES = 200
    private val IMAGE_EXTS = setOf("png", "jpg", "jpeg", "webp")
    private val FORBIDDEN_SCRIPT_EXTS = setOf("js", "mjs", "html", "htm", "dex", "jar", "so", "apk", "sh")
  }
}
