package com.example.motion.io

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ProcessedAssetInfo(
  val fileName: String,
  val width: Int,
  val height: Int,
  val wasAlphaCropped: Boolean,
  val wasDownscaled: Boolean,
)

object BitmapAssetProcessor {

  const val MAX_SIDE_PX = 2000

  fun processAndSaveImageFromUri(
    contentResolver: ContentResolver,
    uri: Uri,
    targetDir: File,
    desiredBaseName: String,
  ): ProcessedAssetInfo? {
    targetDir.mkdirs()
    val safeName = desiredBaseName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "aset" }
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOpts)
    } ?: return null

    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null
    val longest = max(boundsOpts.outWidth, boundsOpts.outHeight)
    var sample = 1
    while (longest / sample > MAX_SIDE_PX * 2) sample *= 2

    val decoded = contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(
        stream,
        null,
        BitmapFactory.Options().apply {
          inSampleSize = sample
          inPreferredConfig = Bitmap.Config.ARGB_8888
        },
      )
    } ?: return null

    val outFile = File(targetDir, "$safeName.png")
    return processBitmapAndWrite(decoded, outFile).also {
      decoded.recycle()
    }
  }

  fun processExistingAssetFile(file: File): ProcessedAssetInfo? {
    if (!file.exists()) return null
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

    val longest = max(boundsOpts.outWidth, boundsOpts.outHeight)
    var sample = 1
    while (longest / sample > MAX_SIDE_PX * 2) sample *= 2

    val decoded = BitmapFactory.decodeFile(
      file.absolutePath,
      BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
      },
    ) ?: return null

    return processBitmapAndWrite(decoded, file).also {
      decoded.recycle()
    }
  }

  fun processBitmapAndWrite(source: Bitmap, outFile: File): ProcessedAssetInfo {
    var working = source
    var wasDownscaled = false
    val longest = max(working.width, working.height)
    if (longest > MAX_SIDE_PX) {
      val scale = MAX_SIDE_PX.toFloat() / longest.toFloat()
      val nw = max(1, (working.width * scale).roundToInt())
      val nh = max(1, (working.height * scale).roundToInt())
      working = Bitmap.createScaledBitmap(working, nw, nh, true)
      wasDownscaled = true
    }

    val croppedResult = cropAlphaBoundingBox(working)
    if (working !== source && working !== croppedResult.first) {
      working.recycle()
    }
    val finalBmp = croppedResult.first
    val wasCropped = croppedResult.second

    val isPng = outFile.extension.equals("png", ignoreCase = true) || wasCropped
    val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    outFile.parentFile?.mkdirs()
    FileOutputStream(outFile).use { fos ->
      finalBmp.compress(format, 92, fos)
    }

    val info = ProcessedAssetInfo(
      fileName = outFile.name,
      width = finalBmp.width,
      height = finalBmp.height,
      wasAlphaCropped = wasCropped,
      wasDownscaled = wasDownscaled,
    )
    if (finalBmp !== source) finalBmp.recycle()
    return info
  }

  /**
   * Memotong PNG transparan tepat ke bounding box piksel ber-alpha > 8.
   */
  fun cropAlphaBoundingBox(bmp: Bitmap): Pair<Bitmap, Boolean> {
    if (!bmp.hasAlpha()) return bmp to false
    val w = bmp.width
    val h = bmp.height
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1
    var foundTransparent = false

    for (y in 0 until h) {
      val rowOffset = y * w
      for (x in 0 until w) {
        val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
        if (alpha <= 8) {
          foundTransparent = true
        } else {
          if (x < minX) minX = x
          if (x > maxX) maxX = x
          if (y < minY) minY = y
          if (y > maxY) maxY = y
        }
      }
    }

    if (!foundTransparent || maxX < minX || maxY < minY) return bmp to false
    if (minX == 0 && minY == 0 && maxX == w - 1 && maxY == h - 1) return bmp to false

    val croppedW = max(1, maxX - minX + 1)
    val croppedH = max(1, maxY - minY + 1)
    val cropped = Bitmap.createBitmap(bmp, minX, minY, croppedW, croppedH)
    return cropped to true
  }

  /**
   * Membuat aset PNG transparan contoh karakter vektor untuk proyek demo 51 detik.
   */
  fun createSampleCharacterPng(targetFile: File) {
    val bmp = Bitmap.createBitmap(420, 560, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Lingkaran aura belakang (dengan tepi transparan agar menguji alpha bbox crop)
    paint.color = Color.argb(65, 0, 229, 255)
    canvas.drawCircle(210f, 260f, 160f, paint)
    // Kepala & visor
    paint.color = Color.parseColor("#E6EEFF")
    canvas.drawCircle(210f, 170f, 68f, paint)
    paint.color = Color.parseColor("#0B1426")
    canvas.drawRoundRect(162f, 148f, 258f, 188f, 18f, 18f, paint)
    paint.color = Color.parseColor("#00E5FF")
    canvas.drawCircle(186f, 168f, 8f, paint)
    canvas.drawCircle(234f, 168f, 8f, paint)
    // Badan jaket tech
    paint.color = Color.parseColor("#5B6BFF")
    canvas.drawRoundRect(118f, 252f, 302f, 468f, 44f, 44f, paint)
    paint.color = Color.parseColor("#00E5FF")
    canvas.drawRoundRect(174f, 294f, 246f, 362f, 14f, 14f, paint)

    processBitmapAndWrite(bmp, targetFile)
    bmp.recycle()
  }
}
