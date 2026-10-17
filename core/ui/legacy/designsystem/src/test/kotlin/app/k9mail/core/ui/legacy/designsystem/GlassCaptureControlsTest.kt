package app.k9mail.core.ui.legacy.designsystem

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Button
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
class GlassCaptureControlsTest {
    @Test
    fun `new disabled export button cannot enter a source before glass decoration registers it`() {
        val context = RuntimeEnvironment.getApplication()
        var draws = 0
        val button = object : Button(context) {
            override fun draw(canvas: Canvas) {
                draws++
            }
        }.apply { isEnabled = false }
        val root = FrameLayout(context).apply {
            addView(button, FrameLayout.LayoutParams(100, 48))
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 100)
        }
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(root).draw(canvas, emptySet())

        assertThat(draws).isEqualTo(0)
    }
}
