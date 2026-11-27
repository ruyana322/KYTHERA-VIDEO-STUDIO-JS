package com.example.media

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipExtractResult(
  val textFiles: Map<String, String>,
  val binaryFiles: Map<String, Pair<String, ByteArray>>,
)

object ZipProjectHandler {

  private const val MAX_ENTRY_BYTES = 4 * 1024 * 1024
  private val TEXT_EXTENSIONS = setOf(
    "html", "htm", "css", "js", "mjs", "cjs", "ts", "jsx",
    "json", "svg", "txt", "md", "glsl", "vert", "frag", "xml",
  )
  private val BINARY_MIME_BY_EXT = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "webp" to "image/webp",
    "gif" to "image/gif",
    "woff2" to "font/woff2",
    "ttf" to "font/ttf",
  )

  fun extractZip(contentResolver: ContentResolver, uri: Uri): ZipExtractResult {
    val textFiles = linkedMapOf<String, String>()
    val binaryFiles = linkedMapOf<String, Pair<String, ByteArray>>()

    contentResolver.openInputStream(uri)?.use { rawStream ->
      ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
          val normalized = entry.name.replace('\\', '/').trimStart('/')
          if (!entry.isDirectory && normalized.isNotBlank() && !normalized.contains("..") && !normalized.startsWith("__MACOSX/")) {
            val ext = normalized.substringAfterLast('.', "").lowercase()
            if (ext in TEXT_EXTENSIONS || ext in BINARY_MIME_BY_EXT) {
              val buffer = ByteArray(8192)
              val out = ByteArrayOutputStream()
              var totalRead = 0
              var len = zis.read(buffer)
              while (len > 0 && totalRead < MAX_ENTRY_BYTES) {
                out.write(buffer, 0, len)
                totalRead += len
                len = zis.read(buffer)
              }
              val bytes = out.toByteArray()
              if (ext in TEXT_EXTENSIONS) {
                textFiles[normalized] = bytes.toString(StandardCharsets.UTF_8)
              } else {
                val mime = BINARY_MIME_BY_EXT[ext] ?: "application/octet-stream"
                binaryFiles[normalized] = mime to bytes
              }
            }
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
    }

    // Strip single common root folder (e.g., "my-project/index.html" -> "index.html")
    val cleanedText = stripCommonRootFolder(textFiles)
    val cleanedBinary = stripCommonRootFolder(binaryFiles)
    return ZipExtractResult(cleanedText, cleanedBinary)
  }

  private fun <T> stripCommonRootFolder(map: Map<String, T>): Map<String, T> {
    if (map.isEmpty()) return map
    val keys = map.keys.toList()
    val firstSeg = keys.first().substringBefore('/', "")
    if (firstSeg.isEmpty() || !keys.all { it.startsWith("$firstSeg/") }) {
      return map
    }
    val out = linkedMapOf<String, T>()
    map.forEach { (k, v) -> out[k.removePrefix("$firstSeg/")] = v }
    return out
  }

  fun extractZipToJson(contentResolver: ContentResolver, uri: Uri): String {
    val result = extractZip(contentResolver, uri)
    return JSONObject().apply {
      result.textFiles.forEach { (k, v) -> put(k, v) }
    }.toString()
  }

  fun writeZipFromJson(contentResolver: ContentResolver, uri: Uri, filesJsonString: String) {
    val filesJson = JSONObject(filesJsonString)
    contentResolver.openOutputStream(uri)?.use { outStream ->
      ZipOutputStream(outStream).use { zos ->
        val keys = filesJson.keys()
        while (keys.hasNext()) {
          val fileName = keys.next().replace("..", "").trimStart('/')
          if (fileName.isBlank()) continue
          val content = filesJson.optString(fileName, "")
          zos.putNextEntry(ZipEntry(fileName))
          zos.write(content.toByteArray(StandardCharsets.UTF_8))
          zos.closeEntry()
        }
      }
    }
  }
}
