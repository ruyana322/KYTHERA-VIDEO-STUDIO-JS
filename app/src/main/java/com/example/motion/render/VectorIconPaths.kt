package com.example.motion.render

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF

/**
 * Koleksi ikon vektor deterministik berbasis android.graphics.Path
 * dalam ruang koordinat ternormalisasi [-1f..1f] (ukuran dasar 2x2) tanpa alokasi per frame.
 */
object VectorIconPaths {

  private val rawPaths: Map<String, Path> = mapOf(
    "image" to buildImagePath(),
    "warn" to buildWarnPath(),
    "lock" to buildLockPath(),
    "shield" to buildShieldPath(),
    "key" to buildKeyPath(),
    "pin" to buildPinPath(),
    "phone" to buildPhonePath(),
    "bubble" to buildBubblePath(),
    "human" to buildHumanPath(),
    "chip" to buildChipPath(),
    "check" to buildCheckPath(),
    "cross" to buildCrossPath(),
  )

  fun fillTransformedPath(
    iconName: String,
    cx: Float,
    cy: Float,
    halfSize: Float,
    outPath: Path,
    matrix: Matrix,
  ) {
    val src = rawPaths[iconName.trim().lowercase()] ?: rawPaths.getValue("shield")
    outPath.reset()
    outPath.set(src)
    matrix.reset()
    matrix.setScale(halfSize, halfSize)
    matrix.postTranslate(cx, cy)
    outPath.transform(matrix)
  }

  private fun buildImagePath(): Path = Path().apply {
    addRoundRect(RectF(-0.85f, -0.68f, 0.85f, 0.68f), 0.16f, 0.16f, Path.Direction.CW)
    addCircle(-0.42f, -0.26f, 0.14f, Path.Direction.CW)
    moveTo(-0.7f, 0.52f)
    lineTo(-0.22f, 0.04f)
    lineTo(0.14f, 0.34f)
    lineTo(0.42f, 0.08f)
    lineTo(0.72f, 0.52f)
  }

  private fun buildWarnPath(): Path = Path().apply {
    moveTo(0f, -0.84f)
    lineTo(0.86f, 0.72f)
    lineTo(-0.86f, 0.72f)
    close()
    moveTo(0f, -0.32f)
    lineTo(0f, 0.22f)
    addCircle(0f, 0.45f, 0.07f, Path.Direction.CW)
  }

  private fun buildLockPath(): Path = Path().apply {
    addRoundRect(RectF(-0.62f, -0.1f, 0.62f, 0.78f), 0.18f, 0.18f, Path.Direction.CW)
    moveTo(-0.36f, -0.1f)
    lineTo(-0.36f, -0.45f)
    cubicTo(-0.36f, -0.86f, 0.36f, -0.86f, 0.36f, -0.45f)
    lineTo(0.36f, -0.1f)
    addCircle(0f, 0.32f, 0.11f, Path.Direction.CW)
  }

  private fun buildShieldPath(): Path = Path().apply {
    moveTo(0f, -0.86f)
    lineTo(0.74f, -0.54f)
    lineTo(0.74f, 0.06f)
    cubicTo(0.74f, 0.48f, 0.38f, 0.76f, 0f, 0.90f)
    cubicTo(-0.38f, 0.76f, -0.74f, 0.48f, -0.74f, 0.06f)
    lineTo(-0.74f, -0.54f)
    close()
    moveTo(-0.26f, 0.02f)
    lineTo(-0.04f, 0.26f)
    lineTo(0.34f, -0.22f)
  }

  private fun buildKeyPath(): Path = Path().apply {
    addCircle(-0.36f, -0.12f, 0.34f, Path.Direction.CW)
    moveTo(-0.02f, -0.12f)
    lineTo(0.84f, -0.12f)
    lineTo(0.84f, 0.22f)
    moveTo(0.54f, -0.12f)
    lineTo(0.54f, 0.16f)
  }

  private fun buildPinPath(): Path = Path().apply {
    moveTo(0f, 0.88f)
    cubicTo(0.45f, 0.36f, 0.64f, 0.05f, 0.64f, -0.24f)
    cubicTo(0.64f, -0.64f, 0.34f, -0.88f, 0f, -0.88f)
    cubicTo(-0.34f, -0.88f, -0.64f, -0.64f, -0.64f, -0.24f)
    cubicTo(-0.64f, 0.05f, -0.45f, 0.36f, 0f, 0.88f)
    close()
    addCircle(0f, -0.26f, 0.22f, Path.Direction.CW)
  }

  private fun buildPhonePath(): Path = Path().apply {
    addRoundRect(RectF(-0.46f, -0.84f, 0.46f, 0.84f), 0.16f, 0.16f, Path.Direction.CW)
    moveTo(-0.2f, -0.64f)
    lineTo(0.2f, -0.64f)
    addCircle(0f, 0.62f, 0.08f, Path.Direction.CW)
  }

  private fun buildBubblePath(): Path = Path().apply {
    addRoundRect(RectF(-0.78f, -0.66f, 0.78f, 0.42f), 0.24f, 0.24f, Path.Direction.CW)
    moveTo(-0.28f, 0.42f)
    lineTo(-0.48f, 0.80f)
    lineTo(0.06f, 0.42f)
    moveTo(-0.36f, -0.12f)
    lineTo(0.36f, -0.12f)
  }

  private fun buildHumanPath(): Path = Path().apply {
    addCircle(0f, -0.44f, 0.28f, Path.Direction.CW)
    moveTo(-0.62f, 0.78f)
    cubicTo(-0.62f, 0.12f, 0.62f, 0.12f, 0.62f, 0.78f)
  }

  private fun buildChipPath(): Path = Path().apply {
    addRoundRect(RectF(-0.52f, -0.52f, 0.52f, 0.52f), 0.12f, 0.12f, Path.Direction.CW)
    addRect(RectF(-0.24f, -0.24f, 0.24f, 0.24f), Path.Direction.CW)
    for (offset in listOf(-0.28f, 0f, 0.28f)) {
      moveTo(offset, -0.52f)
      lineTo(offset, -0.82f)
      moveTo(offset, 0.52f)
      lineTo(offset, 0.82f)
      moveTo(-0.52f, offset)
      lineTo(-0.82f, offset)
      moveTo(0.52f, offset)
      lineTo(0.82f, offset)
    }
  }

  private fun buildCheckPath(): Path = Path().apply {
    addCircle(0f, 0f, 0.82f, Path.Direction.CW)
    moveTo(-0.38f, 0.02f)
    lineTo(-0.08f, 0.34f)
    lineTo(0.42f, -0.28f)
  }

  private fun buildCrossPath(): Path = Path().apply {
    addCircle(0f, 0f, 0.82f, Path.Direction.CW)
    moveTo(-0.34f, -0.34f)
    lineTo(0.34f, 0.34f)
    moveTo(0.34f, -0.34f)
    lineTo(-0.34f, 0.34f)
  }
}
