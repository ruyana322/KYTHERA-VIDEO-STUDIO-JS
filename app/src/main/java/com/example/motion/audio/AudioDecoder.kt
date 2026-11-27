package com.example.motion.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class StereoPcm(
  val samples: ShortArray,
  val sampleRate: Int = 48_000,
  val channels: Int = 2,
) {
  val frameCount: Int get() = samples.size / channels
  val durationSec: Float get() = frameCount.toFloat() / sampleRate.toFloat()
}

data class VoiceAnalysisResult(
  val pausesSec: List<Float>,
  val sceneSplitSec: List<Float>,
  val totalVoiceDurationSec: Float,
)

object AudioDecoder {

  const val TARGET_SAMPLE_RATE = 48_000
  const val TARGET_CHANNELS = 2

  fun probeDurationSec(file: File): Float {
    if (!file.exists() || file.length() <= 16) return 0f
    parseWavHeaderInfo(file)?.let { info ->
      if (info.dataSizeBytes > 0 && info.byteRate > 0) {
        return info.dataSizeBytes.toFloat() / info.byteRate.toFloat()
      }
    }
    val extractor = MediaExtractor()
    return try {
      extractor.setDataSource(file.absolutePath)
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith("audio/")) {
          val durUs = if (fmt.containsKey(MediaFormat.KEY_DURATION)) fmt.getLong(MediaFormat.KEY_DURATION) else 0L
          if (durUs > 0L) return durUs / 1_000_000f
        }
      }
      0f
    } catch (_: Throwable) {
      0f
    } finally {
      runCatching { extractor.release() }
    }
  }

  fun decodeToStereo48k(file: File, maxSeconds: Float = 62f): StereoPcm {
    if (!file.exists()) return StereoPcm(ShortArray(0))
    parseWavAndDecode(file, maxSeconds)?.let { return it }
    return decodeWithMediaCodec(file, maxSeconds)
  }

  private data class WavHeaderInfo(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val byteRate: Int,
    val dataOffset: Long,
    val dataSizeBytes: Int,
  )

  private fun parseWavHeaderInfo(file: File): WavHeaderInfo? {
    return try {
      RandomAccessFile(file, "r").use { raf ->
        if (raf.length() < 44) return null
        val riff = ByteArray(12)
        raf.readFully(riff)
        val riffTag = String(riff, 0, 4, Charsets.US_ASCII)
        val waveTag = String(riff, 8, 4, Charsets.US_ASCII)
        if (riffTag != "RIFF" || waveTag != "WAVE") return null

        var audioFormat = 1
        var channels = 1
        var sampleRate = 44_100
        var bitsPerSample = 16
        var byteRate = sampleRate * channels * 2
        var dataOffset = -1L
        var dataSize = 0

        val hdr = ByteArray(8)
        while (raf.filePointer + 8 <= raf.length()) {
          raf.readFully(hdr)
          val chunkId = String(hdr, 0, 4, Charsets.US_ASCII)
          val chunkSize = ByteBuffer.wrap(hdr, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
          if (chunkSize < 0) break
          if (chunkId == "fmt ") {
            val fmtBuf = ByteArray(min(chunkSize, 64))
            raf.readFully(fmtBuf)
            val bb = ByteBuffer.wrap(fmtBuf).order(ByteOrder.LITTLE_ENDIAN)
            audioFormat = bb.short.toInt() and 0xFFFF
            channels = (bb.short.toInt() and 0xFFFF).coerceIn(1, 2)
            sampleRate = bb.int.coerceIn(8000, 192000)
            byteRate = bb.int
            bb.short // blockAlign
            bitsPerSample = bb.short.toInt() and 0xFFFF
            if (chunkSize > fmtBuf.size) raf.seek(raf.filePointer + (chunkSize - fmtBuf.size))
          } else if (chunkId == "data") {
            dataOffset = raf.filePointer
            dataSize = min(chunkSize.toLong(), raf.length() - dataOffset).toInt()
            break
          } else {
            val next = raf.filePointer + chunkSize + (chunkSize % 2)
            if (next >= raf.length()) break
            raf.seek(next)
          }
        }
        if (audioFormat != 1 || dataOffset < 0 || dataSize <= 0) return null
        WavHeaderInfo(sampleRate, channels, bitsPerSample, max(1, byteRate), dataOffset, dataSize)
      }
    } catch (_: Throwable) {
      null
    }
  }

  private fun parseWavAndDecode(file: File, maxSeconds: Float): StereoPcm? {
    val info = parseWavHeaderInfo(file) ?: return null
    val bytesPerFrame = (info.bitsPerSample / 8) * info.channels
    if (bytesPerFrame <= 0) return null
    val maxFramesIn = (maxSeconds * info.sampleRate).toInt().coerceAtLeast(1)
    val availFrames = info.dataSizeBytes / bytesPerFrame
    val framesToRead = min(availFrames, maxFramesIn)
    val rawBytes = ByteArray(framesToRead * bytesPerFrame)
    RandomAccessFile(file, "r").use { raf ->
      raf.seek(info.dataOffset)
      raf.readFully(rawBytes)
    }

    val stereoSrc = ShortArray(framesToRead * 2)
    val bb = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until framesToRead) {
      val left: Short
      val right: Short
      when (info.bitsPerSample) {
        16 -> {
          left = bb.short
          right = if (info.channels >= 2) bb.short else left
        }
        8 -> {
          val lByte = (bb.get().toInt() and 0xFF) - 128
          left = (lByte shl 8).toShort()
          right = if (info.channels >= 2) {
            val rByte = (bb.get().toInt() and 0xFF) - 128
            (rByte shl 8).toShort()
          } else {
            left
          }
        }
        else -> {
          left = 0
          right = 0
        }
      }
      stereoSrc[i * 2] = left
      stereoSrc[i * 2 + 1] = right
    }
    return resampleStereo(stereoSrc, framesToRead, info.sampleRate, TARGET_SAMPLE_RATE)
  }

  private fun decodeWithMediaCodec(file: File, maxSeconds: Float): StereoPcm {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
      extractor.setDataSource(file.absolutePath)
      var audioTrackIdx = -1
      var trackFormat: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith("audio/")) {
          audioTrackIdx = i
          trackFormat = fmt
          break
        }
      }
      if (audioTrackIdx < 0 || trackFormat == null) return StereoPcm(ShortArray(0))
      extractor.selectTrack(audioTrackIdx)

      val mime = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
      var srcSampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
        trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      } else {
        44_100
      }
      var srcChannels = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      } else {
        2
      }

      codec = MediaCodec.createDecoderByType(mime)
      codec.configure(trackFormat, null, null, 0)
      codec.start()

      val maxOutFrames = (maxSeconds * srcSampleRate).toInt().coerceAtLeast(1)
      val collected = ArrayList<Short>(min(maxOutFrames * 2, 48_000 * 10 * 2))
      val info = MediaCodec.BufferInfo()
      var inputDone = false
      var outputDone = false
      val maxUs = (maxSeconds * 1_000_000L).toLong()

      while (!outputDone) {
        if (!inputDone) {
          val inIdx = codec.dequeueInputBuffer(5_000L)
          if (inIdx >= 0) {
            val inBuf = codec.getInputBuffer(inIdx)
            if (inBuf != null) {
              val read = extractor.readSampleData(inBuf, 0)
              val pts = extractor.sampleTime
              if (read < 0 || pts > maxUs) {
                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputDone = true
              } else {
                codec.queueInputBuffer(inIdx, 0, read, pts, 0)
                extractor.advance()
              }
            }
          }
        }

        val outIdx = codec.dequeueOutputBuffer(info, 5_000L)
        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val outFmt = codec.outputFormat
          if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            srcSampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          }
          if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            srcChannels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)
          }
        } else if (outIdx >= 0) {
          val outBuf = codec.getOutputBuffer(outIdx)
          if (outBuf != null && info.size > 0) {
            outBuf.position(info.offset)
            outBuf.limit(info.offset + info.size)
            val sb = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val frameCount = sb.remaining() / max(1, srcChannels)
            for (f in 0 until frameCount) {
              val l = sb.get()
              val r = if (srcChannels >= 2) sb.get() else l
              collected.add(l)
              collected.add(r)
              if (collected.size / 2 >= maxOutFrames) {
                outputDone = true
                break
              }
            }
          }
          codec.releaseOutputBuffer(outIdx, false)
          if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            outputDone = true
          }
        }
      }

      val srcArr = ShortArray(collected.size) { collected[it] }
      return resampleStereo(srcArr, srcArr.size / 2, srcSampleRate, TARGET_SAMPLE_RATE)
    } catch (_: Throwable) {
      return StereoPcm(ShortArray(0))
    } finally {
      runCatching {
        codec?.stop()
        codec?.release()
      }
      runCatching { extractor.release() }
    }
  }

  fun resampleStereo(
    srcStereo: ShortArray,
    srcFrames: Int,
    srcRate: Int,
    dstRate: Int = TARGET_SAMPLE_RATE,
  ): StereoPcm {
    if (srcFrames <= 0 || srcStereo.size < 2) return StereoPcm(ShortArray(0), dstRate, 2)
    if (srcRate == dstRate) {
      return StereoPcm(srcStereo.copyOf(srcFrames * 2), dstRate, 2)
    }
    val ratio = dstRate.toDouble() / srcRate.toDouble()
    val dstFrames = max(1, (srcFrames * ratio).roundToInt())
    val out = ShortArray(dstFrames * 2)
    for (i in 0 until dstFrames) {
      val srcPos = i / ratio
      val idx0 = srcPos.toInt().coerceIn(0, srcFrames - 1)
      val idx1 = min(srcFrames - 1, idx0 + 1)
      val frac = (srcPos - idx0).toFloat()
      val l0 = srcStereo[idx0 * 2].toFloat()
      val r0 = srcStereo[idx0 * 2 + 1].toFloat()
      val l1 = srcStereo[idx1 * 2].toFloat()
      val r1 = srcStereo[idx1 * 2 + 1].toFloat()
      out[i * 2] = (l0 + (l1 - l0) * frac).roundToInt().coerceIn(-32767, 32767).toShort()
      out[i * 2 + 1] = (r0 + (r1 - r0) * frac).roundToInt().coerceIn(-32767, 32767).toShort()
    }
    return StereoPcm(out, dstRate, 2)
  }

  fun computeWaveformPeaks(pcm: StereoPcm, bucketsPerSec: Int = 20): FloatArray {
    if (pcm.frameCount == 0) return FloatArray(0)
    val framesPerBucket = max(1, pcm.sampleRate / bucketsPerSec)
    val totalBuckets = max(1, (pcm.frameCount + framesPerBucket - 1) / framesPerBucket)
    val peaks = FloatArray(totalBuckets)
    for (b in 0 until totalBuckets) {
      val start = b * framesPerBucket
      val end = min(pcm.frameCount, start + framesPerBucket)
      var peak = 0
      for (f in start until end) {
        val v = max(abs(pcm.samples[f * 2].toInt()), abs(pcm.samples[f * 2 + 1].toInt()))
        if (v > peak) peak = v
      }
      peaks[b] = (peak / 32767f).coerceIn(0f, 1f)
    }
    return peaks
  }

  /**
   * Deteksi jeda suara pada PCM 48kHz:
   * - Ambang: -35 dB relatif terhadap full-scale (32767)
   * - Jeda terdeteksi: >= 0.25 detik
   * - Pemecahan adegan Auto-Draft: jeda >= 0.6 detik
   */
  fun detectSilenceAndPauses(
    pcm: StereoPcm,
    offsetSec: Float = 1.0f,
    thresholdDb: Float = -35f,
    minPauseSec: Float = 0.25f,
    sceneSplitMinSec: Float = 0.6f,
  ): VoiceAnalysisResult {
    if (pcm.frameCount == 0) return VoiceAnalysisResult(emptyList(), emptyList(), 0f)
    val winFrames = max(1, (pcm.sampleRate * 0.02f).toInt()) // 20 ms window
    val totalWins = pcm.frameCount / winFrames
    val pauses = mutableListOf<Float>()
    val sceneSplits = mutableListOf<Float>()

    var inSilence = false
    var silenceStartFrame = 0

    for (w in 0 until totalWins) {
      val start = w * winFrames
      val end = min(pcm.frameCount, start + winFrames)
      var sumSq = 0.0
      for (f in start until end) {
        val mono = (pcm.samples[f * 2].toDouble() + pcm.samples[f * 2 + 1].toDouble()) * 0.5
        sumSq += mono * mono
      }
      val rms = sqrt(sumSq / max(1, end - start))
      val db = if (rms < 1.0) -96f else (20.0 * log10(rms / 32767.0)).toFloat()
      val isSilent = db < thresholdDb

      if (isSilent && !inSilence) {
        inSilence = true
        silenceStartFrame = start
      } else if (!isSilent && inSilence) {
        inSilence = false
        val silenceDur = (start - silenceStartFrame).toFloat() / pcm.sampleRate.toFloat()
        val midTimeSec = offsetSec + ((silenceStartFrame + start) * 0.5f) / pcm.sampleRate.toFloat()
        if (silenceDur >= minPauseSec) {
          pauses += midTimeSec
        }
        if (silenceDur >= sceneSplitMinSec && midTimeSec > offsetSec + 1.5f) {
          sceneSplits += midTimeSec
        }
      }
    }

    return VoiceAnalysisResult(
      pausesSec = pauses,
      sceneSplitSec = sceneSplits,
      totalVoiceDurationSec = pcm.durationSec,
    )
  }

  /**
   * Membuat file WAV demo valid untuk proyek contoh 51 detik agar dapat langsung di-preview dan di-render.
   */
  fun writeDemoWavFile(
    targetFile: File,
    durationSec: Float,
    isMusicPad: Boolean,
    sceneCueBoundaries: List<Float> = listOf(6.0f, 13.5f, 21.0f, 28.5f, 36.0f, 43.0f),
  ) {
    val sampleRate = 22_050
    val totalFrames = (durationSec * sampleRate).roundToInt().coerceAtLeast(sampleRate)
    val dataBytes = totalFrames * 2 // mono 16-bit
    val pcm = ByteArray(dataBytes)
    val bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until totalFrames) {
      val t = i.toFloat() / sampleRate.toFloat()
      val sample: Float = if (isMusicPad) {
        val f1 = 220f
        val f2 = 277.18f
        val f3 = 329.63f
        val env = 0.25f + 0.1f * sin(2.0 * PI * 0.2 * t).toFloat()
        (sin(2.0 * PI * f1 * t) + 0.6 * sin(2.0 * PI * f2 * t) + 0.5 * sin(2.0 * PI * f3 * t)).toFloat() * 0.14f * env
      } else {
        // Simulasi pola bicara berjeda (jeda >0.65 dtk di tiap batas adegan)
        val nearSplit = sceneCueBoundaries.any { b -> abs(t - b) < 0.38f }
        if (nearSplit || t > durationSec - 0.3f) {
          0f
        } else {
          val syllableMod = abs(sin(2.0 * PI * 2.6 * t)).toFloat().coerceIn(0f, 1f)
          val pitch = 185f + 35f * sin(2.0 * PI * 0.45 * t).toFloat()
          (sin(2.0 * PI * pitch * t) + 0.35 * sin(2.0 * PI * pitch * 2.0 * t)).toFloat() * 0.34f * syllableMod
        }
      }
      bb.putShort((sample * 32767f).roundToInt().coerceIn(-32767, 32767).toShort())
    }

    targetFile.parentFile?.mkdirs()
    RandomAccessFile(targetFile, "rw").use { raf ->
      raf.setLength(0)
      val hdr = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
      hdr.put("RIFF".toByteArray(Charsets.US_ASCII))
      hdr.putInt(36 + dataBytes)
      hdr.put("WAVE".toByteArray(Charsets.US_ASCII))
      hdr.put("fmt ".toByteArray(Charsets.US_ASCII))
      hdr.putInt(16)
      hdr.putShort(1) // PCM
      hdr.putShort(1) // Mono
      hdr.putInt(sampleRate)
      hdr.putInt(sampleRate * 2)
      hdr.putShort(2)
      hdr.putShort(16)
      hdr.put("data".toByteArray(Charsets.US_ASCII))
      hdr.putInt(dataBytes)
      raf.write(hdr.array())
      raf.write(pcm)
    }
  }
}
