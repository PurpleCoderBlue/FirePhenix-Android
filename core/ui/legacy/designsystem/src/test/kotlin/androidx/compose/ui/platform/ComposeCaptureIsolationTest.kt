package androidx.compose.ui.platform

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import app.k9mail.core.ui.legacy.designsystem.GlassBackdropCapture
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class ComposeCaptureIsolationTest {
    @Test
    fun `native capture never descends into a Compose owned layer tree`() {
        val context = RuntimeEnvironment.getApplication()
        var layerDraws = 0
        val host = CaptureFixtureComposeView(context).apply {
            addView(
                object : View(context) {
                    override fun draw(canvas: Canvas) {
                        layerDraws++
                    }
                },
                FrameLayout.LayoutParams(100, 100),
            )
        }
        val root = FrameLayout(context).apply {
            addView(host, FrameLayout.LayoutParams(100, 100))
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 100)
        }
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(root).draw(canvas, emptySet())

        assertThat(layerDraws).isEqualTo(0)
    }
}

private class CaptureFixtureComposeView(context: Context) : FrameLayout(context)
