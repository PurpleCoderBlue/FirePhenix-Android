package net.thunderbird.core.ui.theme.manager

import android.graphics.Bitmap
import android.graphics.Color

internal object GlassBackgroundColors {
    fun dominantColor(bitmap: Bitmap): Int {
        val counts = IntArray(4096)
        val reds = IntArray(counts.size)
        val greens = IntArray(counts.size)
        val blues = IntArray(counts.size)
        val stepX = (bitmap.width / 64).coerceAtLeast(1)
        val stepY = (bitmap.height / 64).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) < MIN_VISIBLE_ALPHA) continue
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val bucket = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                counts[bucket]++
                reds[bucket] += r
                greens[bucket] += g
                blues[bucket] += b
            }
        }
        val bucket = counts.indices.maxBy { counts[it] }
        val count = counts[bucket]
        return if (count ==
            0
        ) {
            DEFAULT_SEED
        } else {
            Color.rgb(reds[bucket] / count, greens[bucket] / count, blues[bucket] / count)
        }
    }

    private const val MIN_VISIBLE_ALPHA = 128
    private val DEFAULT_SEED = 0xFF132D45.toInt()
}
