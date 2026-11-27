package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.ShapeKind
import com.example.model.StudioProject
import com.example.model.evaluateLayerAt
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object VideoFrameExtractor {
  fun extractThumbnail(context: Context, uri: Uri, timeUs: Long = 500_000L): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return runCatching {
      retriever.setDataSource(context, uri)
      val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: retriever.getFrameAtTime(0L)
      raw
    }.getOrNull().also {
      runCatching { retriever.release() }
    }
  }
}

class NativeMp4Exporter(private val context: Context) {

  fun exportProjectToMp4(
    project: StudioProject,
    widthPx: Int,
    heightPx: Int,
    fps: Int,
    bitrateMbps: Int,
    loadedBitmaps: Map<String, Bitmap>,
    onProgress: (Float, Int, Int) -> Unit,
    isCancelled: () -> Boolean,
  ): File {
    // Hardware H.264 encoders require even dimensions divisible by 16
    val encW = ((widthPx / 16) * 16).coerceIn(320, 1920)
    val encH = ((heightPx / 16) * 16).coerceIn(320, 1920)
    val safeFps = fps.coerceIn(15, 60)
    val totalFrames = (project.durationSec * safeFps).roundToInt().coerceAtLeast(safeFps)

    val outFile = File(context.cacheDir, "kythera_export_${System.currentTimeMillis()}.mp4")
    if (outFile.exists()) outFile.delete()

    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, encW, encH).apply {
      setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
      setInteger(MediaFormat.KEY_BIT_RATE, bitrateMbps.coerceIn(2, 40) * 1_000_000)
      setInteger(MediaFormat.KEY_FRAME_RATE, safeFps)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    var muxer: MediaMuxer? = null
    var inputSurface: android.view.Surface? = null
    var muxerStarted = false
    var trackIndex = -1

    try {
      codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      inputSurface = codec.createInputSurface()
      codec.start()

      muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      val bufferInfo = MediaCodec.BufferInfo()

      fun drainEncoder(endOfStream: Boolean) {
        if (endOfStream) codec.signalEndOfInputStream()
        while (true) {
          val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
          if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (!endOfStream) break
          } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (!muxerStarted) {
              trackIndex = muxer.addTrack(codec.outputFormat)
              muxer.start()
              muxerStarted = true
            }
          } else if (outIdx >= 0) {
            val encodedBuf = codec.getOutputBuffer(outIdx)
            if (encodedBuf != null && bufferInfo.size > 0 && muxerStarted) {
              if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                encodedBuf.position(bufferInfo.offset)
                encodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                muxer.writeSampleData(trackIndex, encodedBuf, bufferInfo)
              }
            }
            codec.releaseOutputBuffer(outIdx, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
          }
        }
      }

      for (frameIdx in 0 until totalFrames) {
        if (isCancelled()) {
          outFile.delete()
          error("Export cancelled")
        }
        val tSec = frameIdx.toFloat() / safeFps.toFloat()
        val canvas = inputSurface.lockCanvas(null)
        try {
          drawExportFrame(canvas, encW.toFloat(), encH.toFloat(), project, tSec, loadedBitmaps)
        } finally {
          inputSurface.unlockCanvasAndPost(canvas)
        }
        drainEncoder(false)
        onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat(), frameIdx + 1, totalFrames)
      }

      drainEncoder(true)
    } finally {
      runCatching { inputSurface?.release() }
      runCatching {
        codec.stop()
        codec.release()
      }
      runCatching {
        if (muxerStarted) muxer?.stop()
        muxer?.release()
      }
    }

    return outFile
  }

  private fun drawExportFrame(
    canvas: Canvas,
    w: Float,
    h: Float,
    project: StudioProject,
    timeSec: Float,
    bitmaps: Map<String, Bitmap>,
  ) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f, 0f, w, h,
        intArrayOf(Color.parseColor("#090C14"), Color.parseColor("#141B2E")),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, w, h, bgPaint)

    val scaleFactor = min(w, h) / 360f
    project.layers.asReversed().forEach { layer ->
      if (!layer.visible) return@forEach
      if (timeSec < layer.startSec || timeSec > layer.endSec) return@forEach
      drawLayerToAndroidCanvas(canvas, w, h, scaleFactor, layer, timeSec, bitmaps[layer.mediaUri])
    }
  }

  private fun drawLayerToAndroidCanvas(
    canvas: Canvas,
    w: Float,
    h: Float,
    scaleUnit: Float,
    layer: LayerTrack,
    timeSec: Float,
    bitmap: Bitmap?,
  ) {
    val eval = evaluateLayerAt(layer, timeSec)
    val alphaInt = ((eval.opacityPct / 100f).coerceIn(0f, 1f) * 255).roundToInt()
    if (alphaInt <= 2) return

    val cx = w * layer.anchorX
    val cy = h * layer.anchorY
    val baseColor = (layer.colorHex and 0x00FFFFFFL).toInt() or (alphaInt shl 24)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = baseColor
    }

    val particles = layer.effects.firstOrNull { it.id == "cyber_particles" }?.value ?: 0f
    if (particles > 0f) {
      val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((alphaInt * 0.6f).roundToInt(), 0, 198, 255) }
      val count = particles.roundToInt().coerceIn(4, 40)
      for (i in 0 until count) {
        val px = (sin(timeSec * 1.1f + i * 2.13f) * 0.5f + 0.5f) * w
        val py = (cos(timeSec * 0.85f + i * 1.71f) * 0.5f + 0.5f) * h
        canvas.drawCircle(px, py, (2.5f + (i % 3)) * scaleUnit, pPaint)
      }
    }

    canvas.save()
    canvas.translate(eval.posX * scaleUnit, eval.posY * scaleUnit)
    canvas.rotate(eval.rotationDeg, cx, cy)
    val s = (eval.scalePct / 100f).coerceIn(0.1f, 4f)
    canvas.scale(s, s, cx, cy)

    when (layer.type) {
      LayerType.SHAPE -> {
        val r = min(w, h) * 0.24f
        when (layer.shapeKind) {
          ShapeKind.CIRCLE -> canvas.drawCircle(cx, cy, r, paint)
          ShapeKind.ROUNDED_RECT -> {
            val rect = RectF(cx - r, cy - r * 0.72f, cx + r, cy + r * 0.72f)
            canvas.drawRoundRect(rect, 24f * scaleUnit, 24f * scaleUnit, paint)
          }
          ShapeKind.RING -> {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f * scaleUnit
            canvas.drawCircle(cx, cy, r * 1.08f, paint)
          }
          ShapeKind.STAR -> {
            val path = Path()
            for (i in 0 until 10) {
              val rad = if (i % 2 == 0) r * 1.15f else r * 0.52f
              val ang = i * PI / 5.0 - PI / 2.0
              val x = cx + (cos(ang) * rad).toFloat()
              val y = cy + (sin(ang) * rad).toFloat()
              if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, paint)
          }
          ShapeKind.WAVE -> {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 7f * scaleUnit
            paint.strokeCap = Paint.Cap.ROUND
            val path = Path()
            for (i in 0..40) {
              val frac = i / 40f
              val x = w * 0.12f + frac * w * 0.76f
              val y = cy + sin(frac * 10f + timeSec * 4.5f) * (20f * scaleUnit)
              if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)
          }
        }
      }
      LayerType.TEXT -> {
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = layer.fontSizeSp * scaleUnit * 1.25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(layer.textContent, cx, cy + paint.textSize * 0.35f, paint)
      }
      LayerType.IMAGE, LayerType.VIDEO -> {
        val bw = w * 0.62f
        val bh = h * 0.44f
        val dst = RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
        if (bitmap != null) {
          paint.alpha = alphaInt
          canvas.drawBitmap(bitmap, null, dst, paint)
        } else {
          canvas.drawRoundRect(dst, 18f * scaleUnit, 18f * scaleUnit, paint)
        }
      }
      LayerType.DRAW -> {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f * scaleUnit
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        layer.drawStrokes.forEach { stroke ->
          if (stroke.size >= 2) {
            val path = Path()
            path.moveTo(stroke.first().x * w, stroke.first().y * h)
            for (i in 1 until stroke.size) {
              path.lineTo(stroke[i].x * w, stroke[i].y * h)
            }
            canvas.drawPath(path, paint)
          }
        }
      }
      LayerType.AUDIO -> {
        val bars = 18
        val barW = w * 0.65f / bars
        val startX = cx - (bars * barW) / 2f
        for (i in 0 until bars) {
          val amp = abs(sin(timeSec * 6f + i * 0.7f)) * 28f * scaleUnit + 6f * scaleUnit
          canvas.drawRoundRect(
            RectF(startX + i * barW + 2f, h * 0.86f - amp, startX + (i + 1) * barW - 2f, h * 0.86f),
            4f, 4f, paint,
          )
        }
      }
    }
    canvas.restore()
  }
}
