package net.thunderbird.core.ui.theme.manager

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.File
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.preference.display.coreSettings.GlassBackground
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassBackgroundStoreTest {
    @Test
    fun `default is flat and all built in backgrounds load independently`() {
        val context = RuntimeEnvironment.getApplication()
        val blue = GlassBackgroundStore.load(context, GlassBackground.BLUE, "", true)
        val warm = GlassBackgroundStore.load(context, GlassBackground.WARM, "", true)
        val phoenix = GlassBackgroundStore.load(context, GlassBackground.PHOENIX, "", false)
        assertThat(blue.width).isEqualTo(1)
        assertThat(blue.height).isEqualTo(1)
        assertThat(blue.getPixel(0, 0) != warm.getPixel(0, 0)).isTrue()
        assertThat(phoenix.width > 1).isTrue()
    }

    @Test
    fun `selected image is normalized and bounded in private no backup storage`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val input = File(context.cacheDir, "wallpaper.png")
        val bitmap = Bitmap.createBitmap(3000, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        input.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val name = GlassBackgroundStore.importImage(context, Uri.fromFile(input))
        assertThat(Regex("[a-f0-9]{64}\\.png").matches(name)).isTrue()
        assertThat(GlassBackgroundStore.hasImage(context, name)).isTrue()
        val output = GlassBackgroundStore.load(context, GlassBackground.CUSTOM, name, true, blur = 0)
        assertThat(output.width).isEqualTo(2560)
        assertThat(output.height).isEqualTo(512)
        val stored = android.graphics.BitmapFactory.decodeFile(
            File(context.noBackupFilesDir, "glass-backgrounds/$name").absolutePath,
        )
        assertThat(stored.getPixel(100, 100)).isEqualTo(Color.MAGENTA)
        stored.recycle()
        assertThat(GlassBackgroundStore.hasImage(context, "../wallpaper.png")).isEqualTo(false)
    }

    @Test
    fun `invalid image import fails without replacing a saved background`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val input = File(context.cacheDir, "invalid-image").apply { writeText("invalid") }
        assertFailsWith<IllegalArgumentException> {
            GlassBackgroundStore.importImage(context, Uri.fromFile(input))
        }
        val fallback = GlassBackgroundStore.load(context, GlassBackground.CUSTOM, "missing.png", true)
        assertThat(fallback.width).isEqualTo(1)
        assertThat(GlassBackgroundStore.hasImage(context, "missing.png")).isEqualTo(false)
    }

    @Test
    fun `automatic color samples the dominant original color and ignores transparent pixels`() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        for (y in 0 until 10) source.setPixel(0, y, Color.TRANSPARENT)
        source.setPixel(1, 1, Color.RED)
        assertThat(GlassBackgroundColors.dominantColor(source)).isEqualTo(Color.BLUE)
        assertThat(source.getPixel(1, 1)).isEqualTo(Color.RED)
    }
}
