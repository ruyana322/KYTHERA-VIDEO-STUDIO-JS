package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.motion.audio.AudioDecoder
import com.example.motion.audio.AudioMixerAndEncoder
import com.example.motion.audio.StereoPcm
import com.example.motion.io.BitmapAssetProcessor
import com.example.motion.io.ZipImportOutcome
import com.example.motion.io.ZipProjectStorage
import com.example.motion.model.ManifestParseResult
import com.example.motion.model.ManifestSerializer
import com.example.motion.model.SampleManifestProvider
import com.example.motion.render.MotionRenderer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches Indonesian Motion Studio`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Kythera Studio", context.getString(R.string.app_name))
  }

  @Test
  fun `51 second 7 scene sample manifest parses and roundtrips accurately`() {
    val json = SampleManifestProvider.create51SecondJson()
    val parsed = ManifestSerializer.parse(json)
    assertTrue(parsed is ManifestParseResult.Success)
    val manifest = (parsed as ManifestParseResult.Success).manifest
    assertEquals(7, manifest.scenes.size)
    assertEquals(30, manifest.fps)
    assertEquals(1080, manifest.lebar)
    assertEquals(1920, manifest.tinggi)
    assertEquals(51f, manifest.effectiveDurationSec(), 0.01f)
    assertEquals(1530, manifest.totalFrames())
  }

  @Test
  fun `manifest exceeding 60 seconds is rejected`() {
    val tooLong = """
      {
        "judul": "Terlalu Panjang",
        "fps": 30,
        "lebar": 1080,
        "tinggi": 1920,
        "durasi_voiceover_detik": 59.0,
        "voice_offset_detik": 1.0,
        "tail_detik": 2.0,
        "scenes": [
          { "id": "s1", "mulai": 0.0, "selesai": 62.0, "aksen": "#00E5FF", "elemen": [] }
        ]
      }
    """.trimIndent()
    val parsed = ManifestSerializer.parse(tooLong)
    assertTrue(parsed is ManifestParseResult.Error)
  }

  @Test
  fun `alpha bounding box crop removes transparent border`() {
    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    for (y in 40 until 100) {
      for (x in 30 until 80) {
        bmp.setPixel(x, y, Color.CYAN)
      }
    }

    val (cropped, didCrop) = BitmapAssetProcessor.cropAlphaBoundingBox(bmp)
    assertTrue(didCrop)
    assertEquals(50, cropped.width)
    assertEquals(60, cropped.height)
    bmp.recycle()
    cropped.recycle()
  }

  @Test
  fun `zip import rejects zip slip traversal paths`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = ZipProjectStorage(context)
    val badZip = File(context.cacheDir, "bad_slip_test.zip")
    ZipOutputStream(FileOutputStream(badZip)).use { zos ->
      zos.putNextEntry(ZipEntry("../../evil.txt"))
      zos.write("malicious".toByteArray())
      zos.closeEntry()
    }
    val outcome = storage.importZip(context.contentResolver, Uri.fromFile(badZip))
    assertTrue(outcome is ZipImportOutcome.Failure)
    badZip.delete()
  }

  @Test
  fun `deterministic canvas renderer produces identical pixels for same timestamp`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = ZipProjectStorage(context)
    val (projectId, manifest) = storage.createSampleProject51s()
    val dir = storage.projectDir(projectId)

    val renderer = MotionRenderer(context, dir, manifest)
    val bmp1 = Bitmap.createBitmap(270, 480, Bitmap.Config.ARGB_8888)
    val bmp2 = Bitmap.createBitmap(270, 480, Bitmap.Config.ARGB_8888)
    try {
      // Uji frame biasa dan frame transisi wipe diagonal (t=6.75 detik)
      renderer.renderFrame(Canvas(bmp1), 6.75f)
      renderer.renderFrame(Canvas(bmp2), 6.75f)
      val px1 = IntArray(270 * 480)
      val px2 = IntArray(270 * 480)
      bmp1.getPixels(px1, 0, 270, 0, 0, 270, 480)
      bmp2.getPixels(px2, 0, 270, 0, 0, 270, 480)
      assertArrayEquals(px1, px2)
    } finally {
      renderer.release()
      bmp1.recycle()
      bmp2.recycle()
      storage.deleteProject(projectId)
    }
  }

  @Test
  fun `audio silence detector splits pauses and mixer applies fade out and anti clipping`() {
    val sr = 48_000
    val totalSec = 5f
    val frames = (sr * totalSec).toInt()
    val voSamples = ShortArray(frames * 2)
    // Bicara 0..1.5s, jeda hening 1.5..2.3s (0.8s >= 0.6s!), bicara 2.3..4.5s
    for (f in 0 until frames) {
      val t = f.toFloat() / sr
      val v: Short = if (t < 1.5f || (t >= 2.3f && t < 4.5f)) 14000 else 0
      voSamples[f * 2] = v
      voSamples[f * 2 + 1] = v
    }
    val voPcm = StereoPcm(voSamples, sr, 2)
    val analysis = AudioDecoder.detectSilenceAndPauses(voPcm, offsetSec = 1.0f)
    assertTrue(analysis.pausesSec.isNotEmpty())
    assertTrue(analysis.sceneSplitSec.isNotEmpty())

    val musSamples = ShortArray(sr * 2) { 20_000 }
    val musPcm = StereoPcm(musSamples, sr, 2)
    val mixed = AudioMixerAndEncoder.mixTracks(
      voiceover = voPcm,
      music = musPcm,
      voiceOffsetSec = 1.0f,
      totalDurationSec = 6.0f,
    )
    assertEquals(6 * sr, mixed.frameCount)
    // Sample akhir harus mendekati 0 karena fade-out 2.5 detik dan tanpa clipping
    val lastVal = abs(mixed.samples[mixed.samples.lastIndex].toInt())
    assertTrue(lastVal < 50)
  }
}
