package net.thunderbird.core.ui.theme.manager

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassBackgroundProcessorTest {
    @Test
    fun `whole background blur removes image detail without changing the imported source`() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                source.setPixel(x, y, if ((x / 4 + y / 4) % 2 == 0) Color.WHITE else Color.BLACK)
            }
        }
        val clear = GlassBackgroundProcessor.process(source, dark = true, blur = 0)
        val blurred = GlassBackgroundProcessor.process(source, dark = true, blur = 32)

        assertThat(edgeEnergy(blurred) < edgeEnergy(clear) / 5).isTrue()
        assertThat(source.getPixel(0, 0) == Color.WHITE && source.getPixel(4, 0) == Color.BLACK).isTrue()
    }

    @Test
    fun `theme overlay keeps secondary text readable over both extreme image colors`() {
        for (dark in listOf(true, false)) {
            for (background in listOf(Color.BLACK, Color.WHITE)) {
                val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(background) }
                val output = GlassBackgroundProcessor.process(source, dark, blur = 0)
                val foreground = Color.parseColor(if (dark) "#C5C6CC" else "#45474A")

                assertThat(ColorUtils.calculateContrast(foreground, output.getPixel(4, 4)) >= 4.5).isTrue()
            }
        }
    }

    private fun edgeEnergy(image: Bitmap): Long {
        var energy = 0L
        for (y in 0 until image.height) {
            for (x in 1 until image.width) {
                energy += abs(Color.red(image.getPixel(x, y)) - Color.red(image.getPixel(x - 1, y)))
            }
        }
        return energy
    }

    @Test
    fun `content opacity can show the wallpaper or fully cover it without changing source pixels`() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        val clear = GlassBackgroundProcessor.process(source, true, 0, 0)
        val covered = GlassBackgroundProcessor.process(source, true, 0, 100)
        assertThat(clear.getPixel(4, 4)).isEqualTo(Color.MAGENTA)
        assertThat(covered.getPixel(4, 4)).isEqualTo(0xFF101820.toInt())
        assertThat(source.getPixel(4, 4)).isEqualTo(Color.MAGENTA)
    }

    @Test
    fun `custom blur above the old maximum further removes broad image detail`() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                source.setPixel(x, y, if (x < 128) Color.BLACK else Color.WHITE)
            }
        }
        val medium = GlassBackgroundProcessor.process(source, false, 32, 0)
        val maximum = GlassBackgroundProcessor.process(source, false, 512, 0)
        val mediumContrast = abs(Color.red(medium.getPixel(32, 128)) - Color.red(medium.getPixel(224, 128)))
        val maximumContrast = abs(Color.red(maximum.getPixel(32, 128)) - Color.red(maximum.getPixel(224, 128)))
        assertThat(maximumContrast < mediumContrast / 2).isTrue()
    }
}
