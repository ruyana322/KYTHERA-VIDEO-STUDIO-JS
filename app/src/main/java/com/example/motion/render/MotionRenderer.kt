package com.example.motion.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.motion.model.ElementType
import com.example.motion.model.MotionManifest
import com.example.motion.model.ProjectStylePreset
import com.example.motion.model.SafeZones
import com.example.motion.model.SceneElement
import com.example.motion.model.SceneSpec
import java.io.File
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Mesin render deterministik berbasis android.graphics.Canvas.
 * Seluruh gambar setiap frame dihitung murni dari waktu t = nomorFrame / fps
 * tanpa state tersembunyi, memakai seed acak tetap, dan Paint/Path/Bitmap ter-cache.
 */
class MotionRenderer(
  private val context: Context,
  private val projectDir: File,
  private var manifest: MotionManifest,
) {

  private data class ParticleSeed(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val phase: Float,
  )

  private data class RainColumnSeed(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val length: Float,
  )

  // Font ter-cache
  private val interBlack: Typeface
  private val poppinsMedium: Typeface
  private val customFonts = HashMap<String, Typeface>()

  // Bitmap aset ter-cache
  private val cachedBitmaps = HashMap<String, Bitmap>()

  // Seed acak tetap untuk partikel & hujan digital
  private val particleSeeds: List<ParticleSeed>
  private val rainSeeds: List<RainColumnSeed>
  private val glitchOffsets: FloatArray

  // Reusable Paint & Geometry (nol alokasi per frame)
  private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }
  private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
  }
  private val textStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 2f
    color = Color.parseColor("#080C14")
    textAlign = Paint.Align.CENTER
    strokeJoin = Paint.Join.ROUND
  }
  private val textFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
  }
  private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
  }
  private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val safeZonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 3f
    color = Color.argb(180, 0, 229, 255)
    pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
  }
  private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 4f
    color = Color.parseColor("#FFCC00")
  }
  private val subtleGlowFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
  private val wipePath = Path()
  private val iconPath = Path()
  private val workPath = Path()
  private val workMatrix = Matrix()
  private val workRect = RectF()
  private val safeRect = RectF(
    SafeZones.SIDE_PX,
    SafeZones.TOP_PX,
    SafeZones.WIDTH - SafeZones.SIDE_PX,
    SafeZones.HEIGHT - SafeZones.BOTTOM_PX,
  )

  private var lastBgKey: String? = null
  private var vignetteReady = false

  init {
    val baseInter = runCatching { ResourcesCompat.getFont(context, R.font.inter) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    interBlack = Typeface.create(baseInter, Typeface.BOLD)
    val basePoppins = runCatching { ResourcesCompat.getFont(context, R.font.poppins) }.getOrNull() ?: Typeface.SANS_SERIF
    poppinsMedium = Typeface.create(basePoppins, Typeface.NORMAL)

    val rng = Random(0x4B595448L)
    particleSeeds = List(80) {
      ParticleSeed(
        x0 = rng.nextFloat() * 1080f,
        y0 = rng.nextFloat() * 1920f,
        vx = (rng.nextFloat() - 0.5f) * 38f,
        vy = -18f - rng.nextFloat() * 44f,
        radius = 4f + rng.nextFloat() * 8f,
        phase = rng.nextFloat() * 6.283f,
      )
    }
    rainSeeds = List(36) { idx ->
      RainColumnSeed(
        x = (idx + 0.5f) * (1080f / 36f),
        speed = 260f + rng.nextFloat() * 340f,
        phase = rng.nextFloat() * 1920f,
        length = 90f + rng.nextFloat() * 160f,
      )
    }
    glitchOffsets = FloatArray(64) { (rng.nextFloat() - 0.5f) * 24f }

    reloadCaches()
  }

  fun updateManifest(newManifest: MotionManifest) {
    val assetsChanged = newManifest.aset != manifest.aset
    manifest = newManifest
    if (assetsChanged) {
      reloadCaches()
    }
  }

  fun reloadCaches() {
    val asetDir = File(projectDir, "aset")
    val keepKeys = manifest.aset.keys
    val iter = cachedBitmaps.entries.iterator()
    while (iter.hasNext()) {
      val e = iter.next()
      if (e.key !in keepKeys) {
        e.value.recycle()
        iter.remove()
      }
    }

    manifest.aset.forEach { (logicalName, relPath) ->
      if (!cachedBitmaps.containsKey(logicalName)) {
        val f = File(asetDir, relPath)
        if (f.exists()) {
          decodeSampledAsset(f, 1440)?.let { bmp ->
            cachedBitmaps[logicalName] = bmp
          }
        }
      }
    }

    if (asetDir.exists()) {
      asetDir.listFiles()?.forEach { f ->
        if (f.extension.equals("ttf", ignoreCase = true) || f.extension.equals("otf", ignoreCase = true)) {
          val key = f.nameWithoutExtension.lowercase()
          if (!customFonts.containsKey(key)) {
            runCatching { Typeface.createFromFile(f) }.getOrNull()?.let { tf ->
              customFonts[key] = tf
            }
          }
        }
      }
    }
  }

  fun release() {
    cachedBitmaps.values.forEach { runCatching { it.recycle() } }
    cachedBitmaps.clear()
  }

  private fun decodeSampledAsset(file: File, maxDim: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    var sample = 1
    while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
      sample *= 2
    }
    return BitmapFactory.decodeFile(
      file.absolutePath,
      BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
      },
    )
  }

  /**
   * Fungsi render tunggal berbasis waktu absolut t (detik).
   */
  fun renderFrame(
    canvas: Canvas,
    t: Float,
    showSafeZones: Boolean = false,
    selectedElementId: String? = null,
  ) {
    val w = canvas.width.toFloat().coerceAtLeast(1f)
    val h = canvas.height.toFloat().coerceAtLeast(1f)
    canvas.save()
    canvas.scale(w / SafeZones.WIDTH, h / SafeZones.HEIGHT)

    val scenes = manifest.scenes
    if (scenes.isEmpty()) {
      drawBackgroundAndGrid(canvas, null, t)
      drawVignette(canvas)
      canvas.restore()
      return
    }

    val clampedT = t.coerceIn(0f, max(0.01f, manifest.effectiveDurationSec()))
    var currentIdx = scenes.indexOfLast { clampedT >= it.mulai }.coerceAtLeast(0)
    if (currentIdx >= scenes.size) currentIdx = scenes.lastIndex
    val currentScene = scenes[currentIdx]
    val nextScene = scenes.getOrNull(currentIdx + 1)
    val transDur = manifest.transisiDurasiDetik.coerceIn(0.05f, 1.5f)

    // Cek apakah berada di jendela transisi wipe diagonal 0.5 detik menjelang adegan berikutnya
    val wipeStart = currentScene.selesai - transDur
    if (nextScene != null && clampedT >= wipeStart && clampedT < currentScene.selesai) {
      val p = easeInOut(prog(clampedT, wipeStart, transDur))
      drawSceneFull(canvas, currentScene, clampedT, selectedElementId)

      canvas.save()
      buildDiagonalWipeClip(p, wipePath)
      canvas.clipPath(wipePath)
      drawSceneFull(canvas, nextScene, clampedT, selectedElementId)
      canvas.restore()
    } else {
      drawSceneFull(canvas, currentScene, clampedT, selectedElementId)
    }

    if (showSafeZones) {
      canvas.drawRect(safeRect, safeZonePaint)
      textStrokePaint.textSize = 24f
      textStrokePaint.strokeWidth = 3f
      textFillPaint.textSize = 24f
      textFillPaint.color = Color.argb(210, 0, 229, 255)
      textFillPaint.maskFilter = null
      textFillPaint.typeface = poppinsMedium
      canvas.drawText("ZONA AMAN (120 / 300 / 60 px)", 540f, SafeZones.TOP_PX - 22f, textFillPaint)
    }

    canvas.restore()
  }

  private fun buildDiagonalWipeClip(progress: Float, out: Path) {
    out.reset()
    val shift = -SafeZones.HEIGHT + progress * (SafeZones.WIDTH + SafeZones.HEIGHT * 1.45f)
    out.moveTo(0f, 0f)
    out.lineTo(shift + SafeZones.HEIGHT * 0.45f, 0f)
    out.lineTo(shift, SafeZones.HEIGHT)
    out.lineTo(0f, SafeZones.HEIGHT)
    out.close()
  }

  private fun drawSceneFull(
    canvas: Canvas,
    scene: SceneSpec,
    t: Float,
    selectedElementId: String?,
  ) {
    drawBackgroundAndGrid(canvas, scene, t)
    val localT = (t - scene.mulai).coerceAtLeast(0f)

    val ordered = scene.elemen.sortedBy { layerOrder(it.tipe) }
    for (el in ordered) {
      drawElement(canvas, scene, el, t, localT, el.id == selectedElementId)
    }

    drawVignette(canvas)
  }

  private fun layerOrder(type: ElementType): Int = when (type) {
    ElementType.HUJAN_DIGITAL -> 0
    ElementType.PARTIKEL -> 1
    ElementType.PANEL -> 2
    ElementType.KARTU -> 3
    ElementType.GARIS -> 4
    ElementType.KARAKTER -> 5
    ElementType.IKON -> 6
    ElementType.GELOMBANG_SUARA -> 7
    ElementType.TEKS -> 8
    ElementType.KATA_KINETIK -> 9
    ElementType.CAPTION -> 10
  }

  private fun drawBackgroundAndGrid(canvas: Canvas, scene: SceneSpec?, t: Float) {
    val bgTop = parseColorSafe(scene?.latar ?: manifest.gaya.backgroundHex, Color.parseColor("#080C18"))
    val bgBot = parseColorSafe(manifest.gaya.secondaryBgHex, Color.parseColor("#101A33"))
    val bgKey = "$bgTop:$bgBot"
    if (lastBgKey != bgKey) {
      bgPaint.shader = LinearGradient(
        0f, 0f, SafeZones.WIDTH, SafeZones.HEIGHT,
        intArrayOf(bgTop, bgBot),
        null,
        Shader.TileMode.CLAMP,
      )
      lastBgKey = bgKey
    }
    canvas.drawRect(0f, 0f, SafeZones.WIDTH, SafeZones.HEIGHT, bgPaint)

    // Grid latar bergerak deterministik
    val accent = parseColorSafe(scene?.aksen ?: manifest.gaya.accentHexes.first(), Color.CYAN)
    val isLight = manifest.gaya == ProjectStylePreset.TERANG_BERSIH
    gridPaint.color = withAlpha(accent, if (isLight) 28 else 36)
    val cell = 90f
    val scrollY = (t * 28f) % cell
    val scrollX = (t * 14f) % cell
    var x = -cell + scrollX
    while (x <= SafeZones.WIDTH + cell) {
      canvas.drawLine(x, 0f, x, SafeZones.HEIGHT, gridPaint)
      x += cell
    }
    var y = -cell + scrollY
    while (y <= SafeZones.HEIGHT + cell) {
      canvas.drawLine(0f, y, SafeZones.WIDTH, y, gridPaint)
      y += cell
    }
  }

  private fun drawVignette(canvas: Canvas) {
    if (!vignetteReady) {
      vignettePaint.shader = RadialGradient(
        SafeZones.WIDTH / 2f,
        SafeZones.HEIGHT / 2f,
        SafeZones.HEIGHT * 0.72f,
        intArrayOf(Color.TRANSPARENT, Color.argb(145, 4, 6, 14)),
        floatArrayOf(0.55f, 1.0f),
        Shader.TileMode.CLAMP,
      )
      vignetteReady = true
    }
    canvas.drawRect(0f, 0f, SafeZones.WIDTH, SafeZones.HEIGHT, vignettePaint)
  }

  private fun drawElement(
    canvas: Canvas,
    scene: SceneSpec,
    el: SceneElement,
    t: Float,
    localT: Float,
    isSelected: Boolean,
  ) {
    val enterP = easeOutCubic(prog(localT, el.cue, el.durasi.coerceAtLeast(0.15f)))
    if (enterP <= 0.005f && el.tipe != ElementType.PARTIKEL && el.tipe != ElementType.HUJAN_DIGITAL && el.tipe != ElementType.CAPTION) {
      return
    }

    val color = parseColorSafe(el.warna, parseColorSafe(scene.aksen, Color.WHITE))
    val glitchX = if (el.glitch) computeGlitchShift(t) else 0f

    when (el.tipe) {
      ElementType.PARTIKEL -> drawParticles(canvas, color, el.jumlah, t)
      ElementType.HUJAN_DIGITAL -> drawDigitalRain(canvas, color, el.jumlah, t)
      ElementType.PANEL -> {
        val alpha = (enterP * 230).roundToInt().coerceIn(0, 255)
        val scale = 0.92f + 0.08f * enterP
        drawPanelOrCard(canvas, el, color, alpha, scale, isCard = false)
      }
      ElementType.KARTU -> {
        val pop = easeOutBack(prog(localT, el.cue, el.durasi.coerceAtLeast(0.15f)))
        val alpha = (enterP * 245).roundToInt().coerceIn(0, 255)
        drawPanelOrCard(canvas, el, color, alpha, 0.85f + 0.15f * pop, isCard = true)
      }
      ElementType.GARIS -> {
        val grow = easeOutCubic(prog(localT, el.cue, el.durasi.coerceAtLeast(0.15f)))
        strokePaint.color = color
        strokePaint.strokeWidth = el.tebal
        strokePaint.maskFilter = if (isWhiteLike(color)) null else subtleGlowFilter
        val curX2 = el.x + (el.x2 - el.x) * grow
        val curY2 = el.y + (el.y2 - el.y) * grow
        canvas.drawLine(el.x + glitchX, el.y, curX2 + glitchX, curY2, strokePaint)
        strokePaint.maskFilter = null
      }
      ElementType.KARAKTER -> {
        val pop = easeOutBack(prog(localT, el.cue, el.durasi.coerceAtLeast(0.15f)))
        val alpha = (enterP * 255).roundToInt().coerceIn(0, 255)
        val bobY = sin(t * 2.5f) * el.bob
        val bmp = cachedBitmaps[el.asetNama]
        if (bmp != null) {
          val baseScale = (el.tinggi / max(1, bmp.height).toFloat()) * el.skala * (0.85f + 0.15f * pop)
          drawSprite(canvas, bmp, el.x + glitchX, el.y + bobY, baseScale, alpha, el.rotasi)
        } else {
          drawCharacterPlaceholder(canvas, el.x + glitchX, el.y + bobY, el.tinggi * el.skala, color, alpha)
        }
      }
      ElementType.IKON -> {
        val pop = easeOutBack(prog(localT, el.cue, el.durasi.coerceAtLeast(0.15f)))
        val halfSize = (el.ukuran * 0.5f) * el.skala * pop.coerceAtLeast(0.01f)
        VectorIconPaths.fillTransformedPath(el.ikonNama, el.x + glitchX, el.y, halfSize, iconPath, workMatrix)
        strokePaint.color = withAlpha(color, (enterP * 255).roundToInt())
        strokePaint.strokeWidth = (el.ukuran * 0.08f).coerceIn(5f, 18f)
        strokePaint.maskFilter = if (isWhiteLike(color)) null else subtleGlowFilter
        canvas.drawPath(iconPath, strokePaint)
        strokePaint.maskFilter = null
      }
      ElementType.GELOMBANG_SUARA -> {
        drawSoundWave(canvas, el, color, t, enterP)
      }
      ElementType.TEKS -> {
        val slideY = (1f - enterP) * 38f
        val alpha = (enterP * 255).roundToInt().coerceIn(0, 255)
        drawStyledText(
          canvas = canvas,
          text = el.isi,
          cx = el.x + glitchX,
          cy = el.y + slideY,
          size = el.ukuran * el.skala,
          color = withAlpha(color, alpha),
          fontKey = el.font,
          maxWidth = SafeZones.WIDTH - SafeZones.SIDE_PX * 2f,
        )
      }
      ElementType.KATA_KINETIK -> {
        drawKineticWords(canvas, el, color, localT, glitchX)
      }
      ElementType.CAPTION -> {
        val activeLine = el.captionList.firstOrNull { localT in it.masuk..it.keluar }
        if (activeLine != null) {
          val pIn = easeOutCubic(prog(localT, activeLine.masuk, 0.25f))
          val pOut = 1f - prog(localT, (activeLine.keluar - 0.2f).coerceAtLeast(activeLine.masuk), 0.2f)
          val alpha = (min(pIn, pOut) * 255).roundToInt().coerceIn(0, 255)
          if (alpha > 8) {
            drawStyledText(
              canvas = canvas,
              text = activeLine.teks,
              cx = el.x,
              cy = el.y + (1f - pIn) * 18f,
              size = el.ukuran * el.skala,
              color = withAlpha(color, alpha),
              fontKey = el.font,
              maxWidth = SafeZones.WIDTH - SafeZones.SIDE_PX * 2f,
            )
          }
        }
      }
    }

    if (isSelected) {
      val bw = max(180f, el.lebar * el.skala * 0.65f)
      val bh = max(120f, el.tinggi * el.skala * 0.65f)
      workRect.set(el.x - bw / 2f, el.y - bh / 2f, el.x + bw / 2f, el.y + bh / 2f)
      canvas.drawRoundRect(workRect, 18f, 18f, selectionPaint)
    }
  }

  private fun drawParticles(canvas: Canvas, color: Int, count: Int, t: Float) {
    val n = count.coerceIn(4, particleSeeds.size)
    fillPaint.maskFilter = null
    for (i in 0 until n) {
      val s = particleSeeds[i]
      val px = ((s.x0 + s.vx * t) % SafeZones.WIDTH + SafeZones.WIDTH) % SafeZones.WIDTH
      val py = ((s.y0 + s.vy * t) % SafeZones.HEIGHT + SafeZones.HEIGHT) % SafeZones.HEIGHT
      val pulse = 0.55f + 0.45f * sin(t * 2.2f + s.phase)
      fillPaint.color = withAlpha(color, (pulse * 165).roundToInt())
      canvas.drawCircle(px, py, s.radius, fillPaint)
    }
  }

  private fun drawDigitalRain(canvas: Canvas, color: Int, count: Int, t: Float) {
    val n = count.coerceIn(6, rainSeeds.size)
    strokePaint.maskFilter = null
    strokePaint.strokeWidth = 3f
    for (i in 0 until n) {
      val col = rainSeeds[i]
      val yHead = (col.phase + t * col.speed) % (SafeZones.HEIGHT + col.length)
      strokePaint.color = withAlpha(color, 115)
      canvas.drawLine(col.x, yHead - col.length, col.x, yHead, strokePaint)
      fillPaint.color = withAlpha(color, 195)
      canvas.drawCircle(col.x, yHead, 4.5f, fillPaint)
    }
  }

  private fun drawPanelOrCard(
    canvas: Canvas,
    el: SceneElement,
    accent: Int,
    alpha: Int,
    scale: Float,
    isCard: Boolean,
  ) {
    val w = el.lebar * el.skala * scale
    val h = el.tinggi * el.skala * scale
    workRect.set(el.x - w / 2f, el.y - h / 2f, el.x + w / 2f, el.y + h / 2f)

    val isLight = manifest.gaya == ProjectStylePreset.TERANG_BERSIH
    val bgBase = if (isLight) Color.argb((alpha * 0.88f).roundToInt(), 255, 255, 255)
    else Color.argb((alpha * 0.78f).roundToInt(), 16, 24, 46)

    fillPaint.maskFilter = null
    fillPaint.color = bgBase
    canvas.drawRoundRect(workRect, el.radius, el.radius, fillPaint)

    strokePaint.color = withAlpha(accent, alpha)
    strokePaint.strokeWidth = if (isCard) 4f else 3f
    strokePaint.maskFilter = subtleGlowFilter
    canvas.drawRoundRect(workRect, el.radius, el.radius, strokePaint)
    strokePaint.maskFilter = null

    if (el.isi.isNotBlank()) {
      val txtColor = if (isLight) Color.argb(alpha, 20, 26, 46) else Color.argb(alpha, 245, 248, 255)
      drawWrappedText(
        canvas = canvas,
        text = el.isi,
        cx = el.x,
        cy = el.y,
        size = if (isCard) 38f else 32f,
        color = txtColor,
        fontKey = "poppins",
        maxWidth = w - 64f,
      )
    }
  }

  private fun drawSoundWave(canvas: Canvas, el: SceneElement, color: Int, t: Float, enterP: Float) {
    val bars = el.jumlah.coerceIn(8, 48)
    val totalW = el.lebar * el.skala
    val barW = (totalW / bars) * 0.62f
    val gap = totalW / bars
    val startX = el.x - totalW * 0.5f + gap * 0.5f
    fillPaint.color = withAlpha(color, (enterP * 235).roundToInt())
    fillPaint.maskFilter = if (isWhiteLike(color)) null else subtleGlowFilter

    for (i in 0 until bars) {
      val wave = 0.22f + 0.78f * abs(sin(t * 5.2f + i * 0.55f) * sin(t * 2.1f + i * 0.23f))
      val h = (el.tinggi * el.skala) * wave * enterP
      val cx = startX + i * gap
      workRect.set(cx - barW * 0.5f, el.y - h * 0.5f, cx + barW * 0.5f, el.y + h * 0.5f)
      canvas.drawRoundRect(workRect, barW * 0.5f, barW * 0.5f, fillPaint)
    }
    fillPaint.maskFilter = null
  }

  private fun drawKineticWords(
    canvas: Canvas,
    el: SceneElement,
    color: Int,
    localT: Float,
    glitchX: Float,
  ) {
    val words = el.kataList
    if (words.isEmpty()) return
    val lineSpacing = el.ukuran * el.skala * 1.24f
    val startY = el.y - ((words.size - 1) * lineSpacing) * 0.5f

    for (i in words.indices) {
      val kw = words[i]
      val wp = prog(localT, kw.cue, 0.36f)
      if (wp <= 0.01f) continue
      val pop = easeOutBack(wp)
      val slide = (1f - easeOutCubic(wp)) * 42f
      val alpha = (easeOutCubic(wp) * 255).roundToInt().coerceIn(0, 255)
      val size = el.ukuran * el.skala * (0.78f + 0.22f * pop)
      drawStyledText(
        canvas = canvas,
        text = kw.teks,
        cx = el.x + glitchX,
        cy = startY + i * lineSpacing + slide,
        size = size,
        color = withAlpha(color, alpha),
        fontKey = el.font,
        maxWidth = SafeZones.WIDTH - SafeZones.SIDE_PX * 2f,
      )
    }
  }

  private fun drawCharacterPlaceholder(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    heightPx: Float,
    color: Int,
    alpha: Int,
  ) {
    val half = (heightPx * 0.32f).coerceIn(60f, 220f)
    VectorIconPaths.fillTransformedPath("human", cx, cy, half, iconPath, workMatrix)
    strokePaint.color = withAlpha(color, alpha)
    strokePaint.strokeWidth = 10f
    strokePaint.maskFilter = subtleGlowFilter
    canvas.drawPath(iconPath, strokePaint)
    strokePaint.maskFilter = null
  }

  /**
   * Gaya teks:
   * - Teks putih TANPA glow dengan outline gelap 2px
   * - Teks berwarna boleh glow tipis (BlurMaskFilter radius 6px)
   */
  private fun drawStyledText(
    canvas: Canvas,
    text: String,
    cx: Float,
    cy: Float,
    size: Float,
    color: Int,
    fontKey: String,
    maxWidth: Float,
  ) {
    if (text.isBlank()) return
    val tf = resolveFont(fontKey)
    val whiteLike = isWhiteLike(color)
    val alpha = Color.alpha(color)

    textFillPaint.typeface = tf
    textFillPaint.textSize = size
    textFillPaint.color = color
    textFillPaint.maskFilter = if (whiteLike) null else subtleGlowFilter

    textStrokePaint.typeface = tf
    textStrokePaint.textSize = size
    textStrokePaint.color = Color.argb(alpha, 8, 12, 20)
    textStrokePaint.strokeWidth = 2f

    val measured = textFillPaint.measureText(text)
    if (measured <= maxWidth || !text.contains(' ')) {
      val baseline = cy + size * 0.34f
      if (whiteLike) {
        canvas.drawText(text, cx, baseline, textStrokePaint)
      }
      canvas.drawText(text, cx, baseline, textFillPaint)
    } else {
      drawWrappedLinesInternal(canvas, text, cx, cy, size, maxWidth, whiteLike)
    }
    textFillPaint.maskFilter = null
  }

  private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    cx: Float,
    cy: Float,
    size: Float,
    color: Int,
    fontKey: String,
    maxWidth: Float,
  ) {
    val tf = resolveFont(fontKey)
    val whiteLike = isWhiteLike(color)
    textFillPaint.typeface = tf
    textFillPaint.textSize = size
    textFillPaint.color = color
    textFillPaint.maskFilter = null

    textStrokePaint.typeface = tf
    textStrokePaint.textSize = size
    textStrokePaint.color = Color.argb(Color.alpha(color), 8, 12, 20)
    textStrokePaint.strokeWidth = 2f

    drawWrappedLinesInternal(canvas, text, cx, cy, size, maxWidth, whiteLike)
  }

  private fun drawWrappedLinesInternal(
    canvas: Canvas,
    text: String,
    cx: Float,
    cy: Float,
    size: Float,
    maxWidth: Float,
    drawDarkOutline: Boolean,
  ) {
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val lines = ArrayList<String>(4)
    val sb = StringBuilder()
    for (w in words) {
      val cand = if (sb.isEmpty()) w else "${sb} $w"
      if (textFillPaint.measureText(cand) <= maxWidth || sb.isEmpty()) {
        sb.clear()
        sb.append(cand)
      } else {
        lines.add(sb.toString())
        sb.clear()
        sb.append(w)
      }
    }
    if (sb.isNotEmpty()) lines.add(sb.toString())

    val lineHeight = size * 1.25f
    val startY = cy - ((lines.size - 1) * lineHeight) * 0.5f + size * 0.34f
    for (i in lines.indices) {
      val ly = startY + i * lineHeight
      if (drawDarkOutline) canvas.drawText(lines[i], cx, ly, textStrokePaint)
      canvas.drawText(lines[i], cx, ly, textFillPaint)
    }
  }

  fun drawSprite(
    canvas: Canvas,
    bitmap: Bitmap,
    cx: Float,
    cy: Float,
    scale: Float,
    alpha: Int,
    rotDeg: Float,
  ) {
    if (alpha <= 2 || scale <= 0.01f) return
    bitmapPaint.alpha = alpha.coerceIn(0, 255)
    workMatrix.reset()
    workMatrix.postTranslate(-bitmap.width * 0.5f, -bitmap.height * 0.5f)
    workMatrix.postScale(scale, scale)
    if (rotDeg != 0f) workMatrix.postRotate(rotDeg)
    workMatrix.postTranslate(cx, cy)
    canvas.drawBitmap(bitmap, workMatrix, bitmapPaint)
  }

  fun findElementAt(t: Float, normX: Float, normY: Float): SceneElement? {
    val scenes = manifest.scenes
    if (scenes.isEmpty()) return null
    val scene = scenes.lastOrNull { t >= it.mulai } ?: scenes.first()
    val vx = normX * SafeZones.WIDTH
    val vy = normY * SafeZones.HEIGHT
    return scene.elemen.asReversed().firstOrNull { el ->
      if (el.tipe == ElementType.PARTIKEL || el.tipe == ElementType.HUJAN_DIGITAL) return@firstOrNull false
      val hw = max(130f, el.lebar * el.skala * 0.5f)
      val hh = max(110f, el.tinggi * el.skala * 0.5f)
      vx in (el.x - hw)..(el.x + hw) && vy in (el.y - hh)..(el.y + hh)
    }
  }

  private fun resolveFont(fontKey: String): Typeface {
    val k = fontKey.trim().lowercase()
    return customFonts[k] ?: when {
      k.contains("poppins") -> poppinsMedium
      else -> interBlack
    }
  }

  private fun computeGlitchShift(t: Float): Float {
    val frame = (t * 30f).toInt()
    return if (frame % 9 == 0 || frame % 17 == 0) {
      glitchOffsets[abs(frame) % glitchOffsets.size]
    } else {
      0f
    }
  }

  private fun isWhiteLike(color: Int): Boolean {
    val r = Color.red(color) / 255f
    val g = Color.green(color) / 255f
    val b = Color.blue(color) / 255f
    val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
    return lum >= 0.88f
  }

  private fun withAlpha(color: Int, alpha: Int): Int {
    return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
  }

  private fun parseColorSafe(hex: String?, fallback: Int): Int {
    if (hex.isNullOrBlank()) return fallback
    return try {
      Color.parseColor(hex.trim())
    } catch (_: Throwable) {
      fallback
    }
  }

  companion object {
    fun prog(t: Float, t0: Float, durasi: Float): Float {
      if (durasi <= 0.0001f) return if (t >= t0) 1f else 0f
      return ((t - t0) / durasi).coerceIn(0f, 1f)
    }

    fun easeOutCubic(x: Float): Float {
      val t = x.coerceIn(0f, 1f)
      return 1f - (1f - t).pow(3)
    }

    fun easeInOut(x: Float): Float {
      val t = x.coerceIn(0f, 1f)
      return if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
    }

    fun easeOutBack(x: Float): Float {
      val t = x.coerceIn(0f, 1f)
      val c1 = 1.70158f
      val c3 = c1 + 1f
      return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
    }
  }
}
