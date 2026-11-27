package com.example.motion.export

import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.motion.audio.AudioDecoder
import com.example.motion.audio.AudioMixerAndEncoder
import com.example.motion.audio.StereoPcm
import com.example.motion.model.MotionManifest
import com.example.motion.model.RenderQuality
import com.example.motion.render.MotionRenderer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class VideoExportOutcome(
  val galleryUri: Uri,
  val fileName: String,
  val sizeBytes: Long,
  val durationSec: Float,
)

class VideoExportEngine(private val context: Context) {

  fun renderToGalleryMp4(
    projectDir: File,
    manifest: MotionManifest,
    quality: RenderQuality,
    onProgress: (frame: Int, totalFrames: Int, phaseLabel: String) -> Unit,
    isCancelled: () -> Boolean,
  ): VideoExportOutcome {
    preflightChecks(projectDir, manifest)

    val width = 1080
    val height = 1920
    val fps = 30
    val durationCeilSec = ceil(manifest.effectiveDurationSec().toDouble()).toInt().coerceIn(1, 60)
    val totalFrames = durationCeilSec * fps

    val tempDir = File(context.cacheDir, "render_tmp_${System.currentTimeMillis()}").apply { mkdirs() }
    val tempVideoFile = File(tempDir, "video_only.mp4")
    val tempAudioFile = File(tempDir, "audio_only.m4a")
    val tempMuxedFile = File(tempDir, "final_muxed.mp4")

    val renderer = MotionRenderer(context, projectDir, manifest)
    try {
      // 1. Decode & mix audio 48kHz stereo
      onProgress(0, totalFrames, "Memproses & mencampur audio…")
      val asetDir = File(projectDir, "aset")
      val voPcm = manifest.voiceover.takeIf { it.isNotBlank() }?.let { rel ->
        val f = File(asetDir, rel)
        if (f.exists()) AudioDecoder.decodeToStereo48k(f, maxSeconds = 60f) else null
      }
      val musPcm = manifest.musik.takeIf { it.isNotBlank() }?.let { rel ->
        val f = File(asetDir, rel)
        if (f.exists()) AudioDecoder.decodeToStereo48k(f, maxSeconds = 60f) else null
      }
      val mixedPcm = AudioMixerAndEncoder.mixTracks(
        voiceover = voPcm,
        music = musPcm,
        voiceOffsetSec = manifest.voiceOffsetDetik,
        totalDurationSec = durationCeilSec.toFloat(),
      )
      if (isCancelled()) throw InterruptedException("Render dibatalkan pengguna.")

      encodeAudioOnly(mixedPcm, tempAudioFile) {
        if (isCancelled()) throw InterruptedException("Render dibatalkan pengguna.")
      }

      // 2. Render frame-by-frame via H.264 InputSurface
      encodeVideoOnly(
        renderer = renderer,
        width = width,
        height = height,
        fps = fps,
        bitrateBps = quality.bitrateBps,
        totalFrames = totalFrames,
        outFile = tempVideoFile,
        onProgress = { f ->
          onProgress(f, totalFrames, "Merender video…")
        },
        isCancelled = isCancelled,
      )

      // 3. Gabungkan track H.264 + AAC ke MP4 final
      onProgress(totalFrames, totalFrames, "Menggabungkan video & audio…")
      muxVideoAndAudio(tempVideoFile, tempAudioFile, tempMuxedFile)

      // 4. Simpan ke MediaStore Galeri (Movies/KytheraStudio)
      onProgress(totalFrames, totalFrames, "Menyimpan ke Galeri…")
      val safeBase = manifest.judul.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Kythera" }
      val fileName = "${safeBase}_${System.currentTimeMillis()}.mp4"
      val uri = saveMp4ToMediaStore(tempMuxedFile, fileName)
      return VideoExportOutcome(
        galleryUri = uri,
        fileName = fileName,
        sizeBytes = tempMuxedFile.length(),
        durationSec = durationCeilSec.toFloat(),
      )
    } finally {
      renderer.release()
      tempDir.deleteRecursively()
    }
  }

  private fun preflightChecks(projectDir: File, manifest: MotionManifest) {
    val dur = manifest.effectiveDurationSec()
    if (dur > 60f) {
      throw IllegalStateException("Durasi proyek (${ceil(dur.toDouble()).toInt()} dtk) melebihi batas 60 detik.")
    }
    if (manifest.scenes.isEmpty()) {
      throw IllegalStateException("Tidak ada adegan untuk dirender.")
    }
    val freeBytes = projectDir.usableSpace
    if (freeBytes in 1 until 120L * 1024L * 1024L) {
      throw IllegalStateException("Ruang penyimpanan internal hampir penuh (butuh minimal 120 MB bebas).")
    }
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    if (am != null) {
      val mi = ActivityManager.MemoryInfo()
      am.getMemoryInfo(mi)
      if (mi.lowMemory) {
        throw IllegalStateException("Memori RAM perangkat terlalu rendah. Tutup aplikasi lain lalu coba lagi.")
      }
    }
  }

  private fun encodeAudioOnly(
    pcm: StereoPcm,
    outFile: File,
    onCheckCancel: () -> Unit,
  ) {
    val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var muxerStarted = false
    try {
      AudioMixerAndEncoder.encodePcmIntoMuxer(
        pcm = pcm,
        muxer = muxer,
        audioTrackIndexProvider = { fmt ->
          val idx = muxer.addTrack(fmt)
          muxer.start()
          muxerStarted = true
          idx
        },
        onCheckCancel = onCheckCancel,
      )
    } finally {
      runCatching {
        if (muxerStarted) muxer.stop()
        muxer.release()
      }
    }
  }

  private fun encodeVideoOnly(
    renderer: MotionRenderer,
    width: Int,
    height: Int,
    fps: Int,
    bitrateBps: Int,
    totalFrames: Int,
    outFile: File,
    onProgress: (Int) -> Unit,
    isCancelled: () -> Boolean,
  ) {
    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
      setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
      setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
      setInteger(MediaFormat.KEY_FRAME_RATE, fps)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    var surface: android.view.Surface? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    var trackIdx = -1
    var writtenFrames = 0L
    val bufferInfo = MediaCodec.BufferInfo()

    fun drain(eos: Boolean) {
      if (eos) codec.signalEndOfInputStream()
      while (true) {
        val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
        if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
          if (!eos) break
        } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          if (!muxerStarted) {
            trackIdx = muxer!!.addTrack(codec.outputFormat)
            muxer!!.start()
            muxerStarted = true
          }
        } else if (outIdx >= 0) {
          val encoded = codec.getOutputBuffer(outIdx)
          if (encoded != null && bufferInfo.size > 0 && muxerStarted) {
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
              encoded.position(bufferInfo.offset)
              encoded.limit(bufferInfo.offset + bufferInfo.size)
              // Pastikan PTS akurat 30 fps deterministik
              bufferInfo.presentationTimeUs = (writtenFrames * 1_000_000L) / fps.toLong()
              writtenFrames++
              muxer!!.writeSampleData(trackIdx, encoded, bufferInfo)
            }
          }
          codec.releaseOutputBuffer(outIdx, false)
          if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
        }
      }
    }

    try {
      codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      surface = codec.createInputSurface()
      codec.start()
      muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

      for (frameIdx in 0 until totalFrames) {
        if (isCancelled()) throw InterruptedException("Render dibatalkan pengguna.")
        val t = frameIdx.toFloat() / fps.toFloat()
        val canvas = try {
          surface.lockHardwareCanvas()
        } catch (_: Throwable) {
          surface.lockCanvas(null)
        }
        try {
          renderer.renderFrame(canvas, t, showSafeZones = false, selectedElementId = null)
        } finally {
          surface.unlockCanvasAndPost(canvas)
        }
        drain(false)
        onProgress(frameIdx + 1)
      }
      drain(true)
    } finally {
      runCatching { surface?.release() }
      runCatching {
        codec.stop()
        codec.release()
      }
      runCatching {
        if (muxerStarted) muxer?.stop()
        muxer?.release()
      }
    }
  }

  private fun muxVideoAndAudio(videoFile: File, audioFile: File, destFile: File) {
    val videoExt = MediaExtractor()
    val audioExt = MediaExtractor()
    val muxer = MediaMuxer(destFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var started = false
    try {
      videoExt.setDataSource(videoFile.absolutePath)
      val vTrackSrc = findTrack(videoExt, "video/")
      audioExt.setDataSource(audioFile.absolutePath)
      val aTrackSrc = findTrack(audioExt, "audio/")

      val vTrackDst = if (vTrackSrc >= 0) {
        videoExt.selectTrack(vTrackSrc)
        muxer.addTrack(videoExt.getTrackFormat(vTrackSrc))
      } else {
        -1
      }
      val aTrackDst = if (aTrackSrc >= 0) {
        audioExt.selectTrack(aTrackSrc)
        muxer.addTrack(audioExt.getTrackFormat(aTrackSrc))
      } else {
        -1
      }

      muxer.start()
      started = true

      val buf = ByteBuffer.allocateDirect(512 * 1024)
      val info = MediaCodec.BufferInfo()
      if (vTrackDst >= 0) copyTrack(videoExt, muxer, vTrackDst, buf, info)
      if (aTrackDst >= 0) copyTrack(audioExt, muxer, aTrackDst, buf, info)
    } finally {
      runCatching { videoExt.release() }
      runCatching { audioExt.release() }
      runCatching {
        if (started) muxer.stop()
        muxer.release()
      }
    }
  }

  private fun findTrack(ext: MediaExtractor, mimePrefix: String): Int {
    for (i in 0 until ext.trackCount) {
      val mime = ext.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
      if (mime.startsWith(mimePrefix)) return i
    }
    return -1
  }

  private fun copyTrack(
    ext: MediaExtractor,
    muxer: MediaMuxer,
    dstTrackIdx: Int,
    buf: ByteBuffer,
    info: MediaCodec.BufferInfo,
  ) {
    while (true) {
      buf.clear()
      val size = ext.readSampleData(buf, 0)
      if (size < 0) break
      info.offset = 0
      info.size = size
      info.presentationTimeUs = ext.sampleTime
      info.flags = if ((ext.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
        MediaCodec.BUFFER_FLAG_KEY_FRAME
      } else {
        0
      }
      buf.position(0)
      buf.limit(size)
      muxer.writeSampleData(dstTrackIdx, buf, info)
      ext.advance()
    }
  }

  private fun saveMp4ToMediaStore(sourceFile: File, fileName: String): Uri {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/KytheraStudio")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException("Gagal membuat entri MediaStore untuk video MP4.")
      resolver.openOutputStream(uri)?.use { out ->
        sourceFile.inputStream().use { inp -> inp.copyTo(out) }
      } ?: throw IllegalStateException("Gagal menulis file MP4 ke Galeri.")

      values.clear()
      values.put(MediaStore.Video.Media.IS_PENDING, 0)
      resolver.update(uri, values, null, null)
      return uri
    } else {
      val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "KytheraStudio").apply {
        mkdirs()
      }
      val dst = File(outDir, fileName)
      sourceFile.inputStream().use { inp ->
        FileOutputStream(dst).use { out -> inp.copyTo(out) }
      }
      return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dst)
    }
  }
}
