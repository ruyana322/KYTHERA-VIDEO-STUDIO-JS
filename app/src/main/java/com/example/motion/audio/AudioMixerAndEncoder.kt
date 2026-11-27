package com.example.motion.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tanh

object AudioMixerAndEncoder {

  const val SAMPLE_RATE = 48_000
  const val CHANNELS = 2
  const val AAC_BITRATE = 192_000
  private const val MUSIC_BASE_GAIN = 0.24f
  private const val DUCK_MIN_MULTIPLIER = 0.25f
  private const val END_FADE_SEC = 2.5f

  /**
   * Mix manual PCM 48kHz stereo:
   * - Voiceover diletakkan pada [voiceOffsetSec]
   * - Musik di-loop berulang pada volume dasar rendah
   * - Auto-ducking halus mengikuti amplop RMS voiceover
   * - Fade-out 2.5 detik di akhir audio
   * - Limiter anti-clipping (soft knee + tanh ceiling)
   */
  fun mixTracks(
    voiceover: StereoPcm?,
    music: StereoPcm?,
    voiceOffsetSec: Float,
    totalDurationSec: Float,
  ): StereoPcm {
    val totalFrames = max(SAMPLE_RATE, (totalDurationSec * SAMPLE_RATE).roundToInt())
    val out = ShortArray(totalFrames * CHANNELS)
    val voFrames = voiceover?.frameCount ?: 0
    val voOffsetFrames = max(0, (voiceOffsetSec * SAMPLE_RATE).roundToInt())
    val musFrames = music?.frameCount ?: 0
    val fadeFrames = (END_FADE_SEC * SAMPLE_RATE).roundToInt().coerceIn(1, totalFrames)
    val fadeStartFrame = totalFrames - fadeFrames

    var voiceEnvelope = 0f
    val attackCoeff = 0.004f
    val releaseCoeff = 0.00035f

    for (f in 0 until totalFrames) {
      val voIdx = f - voOffsetFrames
      var voL = 0f
      var voR = 0f
      if (voiceover != null && voIdx in 0 until voFrames) {
        voL = voiceover.samples[voIdx * 2] / 32767f
        voR = voiceover.samples[voIdx * 2 + 1] / 32767f
      }
      val voMag = max(abs(voL), abs(voR))
      val coeff = if (voMag > voiceEnvelope) attackCoeff else releaseCoeff
      voiceEnvelope += (voMag - voiceEnvelope) * coeff

      var musL = 0f
      var musR = 0f
      if (music != null && musFrames > 0) {
        val mIdx = f % musFrames
        val duckMult = (1f - (voiceEnvelope * 4.5f).coerceIn(0f, 1f) * (1f - DUCK_MIN_MULTIPLIER))
        val gain = MUSIC_BASE_GAIN * duckMult
        musL = (music.samples[mIdx * 2] / 32767f) * gain
        musR = (music.samples[mIdx * 2 + 1] / 32767f) * gain
      }

      val fadeGain = if (f >= fadeStartFrame) {
        ((totalFrames - f).toFloat() / fadeFrames.toFloat()).coerceIn(0f, 1f)
      } else {
        1f
      }

      val rawL = (voL + musL) * fadeGain
      val rawR = (voR + musR) * fadeGain

      val limitedL = softLimit(rawL)
      val limitedR = softLimit(rawR)

      out[f * 2] = (limitedL * 32767f).roundToInt().coerceIn(-32767, 32767).toShort()
      out[f * 2 + 1] = (limitedR * 32767f).roundToInt().coerceIn(-32767, 32767).toShort()
    }

    return StereoPcm(out, SAMPLE_RATE, CHANNELS)
  }

  private fun softLimit(sample: Float): Float {
    val absS = abs(sample)
    if (absS <= 0.85f) return sample
    return tanh(sample.toDouble()).toFloat() * 0.98f
  }

  /**
   * Encode PCM 48kHz stereo ke AAC 192 kbps lalu tulis ke track audio pada MediaMuxer.
   * Dipanggil setelah MediaMuxer.start() atau memakai track yang sudah terdaftar sebelum start().
   */
  fun encodePcmIntoMuxer(
    pcm: StereoPcm,
    muxer: MediaMuxer,
    audioTrackIndexProvider: (MediaFormat) -> Int,
    onCheckCancel: () -> Unit,
  ) {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS).apply {
      setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
      setInteger(MediaFormat.KEY_BIT_RATE, AAC_BITRATE)
      setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    val info = MediaCodec.BufferInfo()
    var trackIdx = -1
    var totalSamplesFed = 0L
    var cursorShorts = 0
    var inputEosSent = false
    var outputEos = false

    try {
      codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      codec.start()

      while (!outputEos) {
        onCheckCancel()

        if (!inputEosSent) {
          val inIdx = codec.dequeueInputBuffer(10_000L)
          if (inIdx >= 0) {
            val inBuf = codec.getInputBuffer(inIdx)
            if (inBuf != null) {
              inBuf.clear()
              inBuf.order(ByteOrder.LITTLE_ENDIAN)
              val maxShorts = min(inBuf.capacity() / 2, pcm.samples.size - cursorShorts)
              if (maxShorts <= 0) {
                val ptsUs = (totalSamplesFed * 1_000_000L) / SAMPLE_RATE.toLong()
                codec.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputEosSent = true
              } else {
                for (i in 0 until maxShorts) {
                  inBuf.putShort(pcm.samples[cursorShorts + i])
                }
                val ptsUs = (totalSamplesFed * 1_000_000L) / SAMPLE_RATE.toLong()
                val framesInBatch = maxShorts / CHANNELS
                totalSamplesFed += framesInBatch.toLong()
                cursorShorts += maxShorts
                codec.queueInputBuffer(inIdx, 0, maxShorts * 2, ptsUs, 0)
              }
            }
          }
        }

        val outIdx = codec.dequeueOutputBuffer(info, 10_000L)
        if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          if (trackIdx < 0) {
            trackIdx = audioTrackIndexProvider(codec.outputFormat)
          }
        } else if (outIdx >= 0) {
          val outBuf = codec.getOutputBuffer(outIdx)
          if (outBuf != null && info.size > 0 && trackIdx >= 0) {
            if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
              outBuf.position(info.offset)
              outBuf.limit(info.offset + info.size)
              muxer.writeSampleData(trackIdx, outBuf, info)
            }
          }
          codec.releaseOutputBuffer(outIdx, false)
          if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            outputEos = true
          }
        }
      }
    } finally {
      runCatching {
        codec.stop()
        codec.release()
      }
    }
  }
}
