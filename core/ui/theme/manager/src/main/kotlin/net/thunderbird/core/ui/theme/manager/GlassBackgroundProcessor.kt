package net.thunderbird.core.ui.theme.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.roundToInt

/** Processes only the wallpaper. Text, mail HTML and controls never enter this bitmap. */
internal object GlassBackgroundProcessor {
    fun process(
        source: Bitmap,
        dark: Boolean,
        blur: Int,
        opacity: Int = if (dark) DARK_OPACITY else LIGHT_OPACITY,
    ): Bitmap {
        val level = blur.coerceIn(0, MAX_BLUR)
        val scale = if (level > 0) minOf(1f, MAX_BLURRED_EDGE.toFloat() / maxOf(source.width, source.height)) else 1f
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(if (dark) DARK_BASE else LIGHT_BASE)
            drawBitmap(scaled, 0f, 0f, null)
        }
        if (scaled !== source) scaled.recycle()
        if (level > 0) {
            val pixels = IntArray(width * height)
            output.getPixels(pixels, 0, width, 0, 0, width, height)
            val temporary = IntArray(pixels.size)
            val radius = (
                maxOf(
                    width,
                    height,
                ) * RADIUS_FRACTION * level / STRENGTH_REFERENCE
                ).roundToInt().coerceIn(1, maxOf(width, height))
            repeat(BLUR_PASSES) {
                pass(pixels, temporary, width, height, radius, horizontal = true)
                pass(temporary, pixels, width, height, radius, horizontal = false)
            }
            output.setPixels(pixels, 0, width, 0, 0, width, height)
        }
        // Bound both extremes of a photo so light/dark theme foreground roles remain readable.
        val alpha = (opacity.coerceIn(0, 100) * MAX_ALPHA / PERCENT).roundToInt()
        val base = if (dark) DARK_BASE else LIGHT_BASE
        Canvas(output).drawColor((base and RGB_MASK) or (alpha shl ALPHA_SHIFT))
        return output
    }

    private fun pass(input: IntArray, output: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val length = if (horizontal) width else height
        val lines = if (horizontal) height else width
        val step = if (horizontal) 1 else width
        val count = radius * 2 + 1
        repeat(lines) { line ->
            val origin = if (horizontal) line * width else line
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val pixel = input[origin + offset.coerceIn(0, length - 1) * step]
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
            }
            repeat(length) { position ->
                output[origin + position * step] = Color.rgb(red / count, green / count, blue / count)
                val leaving = input[origin + (position - radius).coerceIn(0, length - 1) * step]
                val entering = input[origin + (position + radius + 1).coerceIn(0, length - 1) * step]
                red += Color.red(entering) - Color.red(leaving)
                green += Color.green(entering) - Color.green(leaving)
                blue += Color.blue(entering) - Color.blue(leaving)
            }
        }
    }

    private const val RGB_MASK = 0x00FFFFFF
    private const val ALPHA_SHIFT = 24
    private const val DARK_OPACITY = 85
    private const val LIGHT_OPACITY = 78
    private const val MAX_ALPHA = 255f
    private const val PERCENT = 100f
    private const val MAX_BLUR = 512
    private const val STRENGTH_REFERENCE = 32
    private const val MAX_BLURRED_EDGE = 960
    private const val RADIUS_FRACTION = 0.035f
    private const val BLUR_PASSES = 3
    private val DARK_BASE = 0xFF101820.toInt()
    private val LIGHT_BASE = 0xFFF8FAFC.toInt()
}
