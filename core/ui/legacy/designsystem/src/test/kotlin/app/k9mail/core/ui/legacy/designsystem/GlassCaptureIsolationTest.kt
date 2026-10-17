package app.k9mail.core.ui.legacy.designsystem

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassCaptureIsolationTest {
    @Test
    fun `capturing one window cannot record another window source into it`() {
        val context = RuntimeEnvironment.getApplication()
        var foreignDraws = 0
        val foreign = scene(
            context,
            object : View(context) {
                override fun draw(canvas: Canvas) {
                    foreignDraws++
                }
            },
        )
        val foreignCapture = GlassBackdropCapture(foreign)
        val current = scene(
            context,
            object : View(context) {
                override fun draw(canvas: Canvas) {
                    foreignCapture.draw(canvas, emptySet())
                }
            },
        )
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(current).draw(canvas, emptySet())
        assertThat(foreignDraws).isEqualTo(0)
        foreignCapture.draw(canvas, emptySet())
        assertThat(foreignDraws).isEqualTo(1)
    }

    @Test
    fun `a failed capture releases isolation for the next window`() {
        val context = RuntimeEnvironment.getApplication()
        val broken = scene(
            context,
            object : View(context) {
                override fun draw(canvas: Canvas) {
                    error("fixture failure")
                }
            },
        )
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        runCatching { GlassBackdropCapture(broken).draw(canvas, emptySet()) }

        assertThat(GlassBackdropCapture.isCapturing).isEqualTo(false)
    }

    private fun scene(context: Context, content: View) = FrameLayout(context).apply {
        addView(content, FrameLayout.LayoutParams(100, 100))
        measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, 100, 100)
    }
}
