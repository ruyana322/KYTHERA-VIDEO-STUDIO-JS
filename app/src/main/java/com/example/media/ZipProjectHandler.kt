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

object ZipProjectHandler {

  private const val MAX_ENTRY_BYTES = 2 * 1024 * 1024
  private val ALLOWED_EXTENSIONS = setOf("html", "htm", "css", "js", "json", "svg", "txt", "md")

  fun extractZipToJson(contentResolver: ContentResolver, uri: Uri): String {
    val filesJson = JSONObject()
    contentResolver.openInputStream(uri)?.use { rawStream ->
      ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
          val entryName = entry.name
          if (!entry.isDirectory && !entryName.contains("..") && !entryName.startsWith("/")) {
            val ext = entryName.substringAfterLast('.', "").lowercase()
            if (ext in ALLOWED_EXTENSIONS) {
              val buffer = ByteArray(4096)
              val out = ByteArrayOutputStream()
              var totalRead = 0
              var len = zis.read(buffer)
              while (len > 0 && totalRead < MAX_ENTRY_BYTES) {
                out.write(buffer, 0, len)
                totalRead += len
                len = zis.read(buffer)
              }
              val cleanName = entryName.substringAfterLast('/')
              if (cleanName.isNotBlank()) {
                filesJson.put(cleanName, out.toString(StandardCharsets.UTF_8.name()))
              }
            }
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
    }
    return filesJson.toString()
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
