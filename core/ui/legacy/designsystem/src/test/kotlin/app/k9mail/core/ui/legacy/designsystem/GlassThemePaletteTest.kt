package app.k9mail.core.ui.legacy.designsystem

import android.app.Application
import androidx.core.graphics.ColorUtils
import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassThemePaletteTest {
    @Test
    fun `automatic or manual accent roles keep foreground contrast in both modes`() {
        for (seed in listOf(0xFF3576A5.toInt(), 0xFFB83C32.toInt(), 0xFFEEEEEE.toInt(), 0xFF111111.toInt())) {
            for (dark in listOf(true, false)) {
                val palette = GlassThemePalette.from(seed, dark)
                assertThat(ColorUtils.calculateContrast(palette.onAccent, palette.accent) >= 4.5).isTrue()
                assertThat(ColorUtils.calculateContrast(palette.onContainer, palette.container) >= 4.5).isTrue()
            }
        }
    }
}
