package app.k9mail.core.ui.legacy.designsystem

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassBackdropCaptureTest {
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `fully clipped offscreen containers do not redraw their descendants for glass`() {
        val context = RuntimeEnvironment.getApplication()
        val leaf = CountingView(context)
        val offscreen = FrameLayout(context).apply { addView(leaf, FrameLayout.LayoutParams(100, 100)) }
        val root = FrameLayout(context).apply {
            addView(offscreen, FrameLayout.LayoutParams(100, 100).apply { topMargin = 200 })
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 100)
        }
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(root).draw(canvas, emptySet())

        assertThat(leaf.draws).isEqualTo(0)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `an offscreen parent without child clipping still captures its visible overflow`() {
        val context = RuntimeEnvironment.getApplication()
        var draws = 0
        val leaf = object : View(context) {
            override fun draw(canvas: Canvas) {
                draws++
                canvas.drawColor(Color.BLUE)
            }
        }.apply { translationY = -200f }
        val overflow = FrameLayout(context).apply {
            clipChildren = false
            addView(leaf, FrameLayout.LayoutParams(100, 100))
        }
        val root = FrameLayout(context).apply {
            addView(overflow, FrameLayout.LayoutParams(100, 100).apply { topMargin = 200 })
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 100)
        }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        GlassBackdropCapture(root).draw(Canvas(bitmap), emptySet())

        assertThat(draws).isEqualTo(1)
        assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.BLUE)
    }

    @Test
    fun `sampling does not dispatch fragment animations or Compose hosts for a second draw pass`() {
        val context = RuntimeEnvironment.getApplication()
        val leaf = CountingView(context)
        val fragmentHost = DrawingContainer(context).apply { addView(leaf) }
        val control = CountingView(context)
        val root = layout(context, fragmentHost, control)
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        fragmentHost.draw(canvas)
        val before = fragmentHost.draws

        GlassBackdropCapture(root).draw(canvas, setOf(control))

        assertThat(fragmentHost.draws).isEqualTo(before)
        assertThat(leaf.draws).isEqualTo(2)
    }

    @Test
    fun `backdrop does not draw nested email WebView a second time`() {
        val context = RuntimeEnvironment.getApplication()
        val email = CountingWebView(context)
        val body = DrawingContainer(context).apply { addView(email) }
        val control = CountingView(context)
        val root = layout(context, body, control)
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val testSubject = GlassBackdropCapture(root)
        body.draw(canvas)
        assertThat(email.draws).isEqualTo(1)

        testSubject.draw(canvas, setOf(control))

        assertThat(email.draws).isEqualTo(1)
    }

    @Test
    fun `backdrop still draws ordinary content and excludes glass foreground`() {
        val context = RuntimeEnvironment.getApplication()
        val content = CountingView(context)
        val control = CountingView(context)
        val root = layout(context, content, control)
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val testSubject = GlassBackdropCapture(root)

        testSubject.draw(canvas, setOf(control))

        assertThat(content.draws).isEqualTo(1)
        assertThat(control.draws).isEqualTo(0)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `scrolled child drawing cannot paint outside parent viewport into toolbar`() {
        val context = RuntimeEnvironment.getApplication()
        val control = CountingView(context)
        val body = FrameLayout(context).apply {
            addView(BleedingView(context), FrameLayout.LayoutParams(100, 40))
            addView(control, FrameLayout.LayoutParams(100, 10))
        }
        val root = FrameLayout(context).apply {
            addView(body, FrameLayout.LayoutParams(100, 40).apply { topMargin = 60 })
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 100)
        }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.RED) }
        val testSubject = GlassBackdropCapture(root)

        testSubject.draw(canvas, setOf(control))

        assertThat(bitmap.getPixel(50, 20)).isEqualTo(Color.RED)
        assertThat(bitmap.getPixel(50, 80)).isEqualTo(Color.BLACK)
    }

    @Test
    fun `recycled optical background is excluded when transient registry is empty`() {
        val context = RuntimeEnvironment.getApplication()
        val background = CountingGlassBackground()
        val recycled = CountingView(context).apply { this.background = background }
        val body = DrawingContainer(context).apply { addView(recycled) }
        val ordinary = CountingView(context)
        val root = layout(context, body, ordinary)
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(root).draw(canvas, emptySet())

        assertThat(background.draws).isEqualTo(0)
        assertThat(recycled.draws).isEqualTo(0)
        assertThat(ordinary.draws).isEqualTo(1)
    }

    @Test
    fun `wrapped and foreground optical materials never enter their own backdrop after recycling`() {
        val context = RuntimeEnvironment.getApplication()
        val background = CountingGlassBackground()
        val wrapped = android.graphics.drawable.InsetDrawable(
            android.graphics.drawable.LayerDrawable(arrayOf(background)),
            2,
        )
        val recycled = CountingView(context).apply { this.background = wrapped }
        val foreground = CountingGlassBackground()
        val reusedForeground = CountingView(context).apply {
            this.foreground = android.graphics.drawable.InsetDrawable(foreground, 1)
        }
        val root = layout(context, recycled, reusedForeground)
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(root).draw(canvas, emptySet())

        assertThat(background.draws).isEqualTo(0)
        assertThat(recycled.draws).isEqualTo(0)
        assertThat(reusedForeground.draws).isEqualTo(0)
    }

    @Test
    fun `AppCompat wrappers and popup materials are excluded from a recycled backdrop`() {
        val context = RuntimeEnvironment.getApplication()
        val background = CountingGlassBackground()
        val wrapped = CountingView(context).apply {
            this.background = androidx.appcompat.graphics.drawable.DrawableWrapperCompat(background)
        }
        val popup = CountingView(context).apply { this.background = GlassPopupDrawable() }
        val root = layout(context, wrapped, popup)
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        GlassBackdropCapture(root).draw(canvas, emptySet())

        assertThat(background.draws).isEqualTo(0)
        assertThat(wrapped.draws).isEqualTo(0)
        assertThat(popup.draws).isEqualTo(0)
    }

    private class CountingGlassBackground : Drawable(), GlassOpticalMaterial {
        var draws = 0
        override fun draw(canvas: Canvas) {
            draws++
        }
        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private class BleedingView(context: Context) : View(context) {
        override fun draw(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
        }
    }

    private fun layout(context: Context, content: View, control: View): FrameLayout =
        FrameLayout(context).apply {
            addView(content, FrameLayout.LayoutParams(100, 100))
            addView(control, FrameLayout.LayoutParams(100, 10))
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 100)
        }

    // Drive child drawing explicitly: Robolectric does not attach this fixture to a window.
    private class DrawingContainer(context: Context) : FrameLayout(context) {
        var draws = 0
        override fun draw(canvas: Canvas) {
            draws++
            for (index in 0 until childCount) getChildAt(index).draw(canvas)
        }
    }

    private class CountingWebView(context: Context) : WebView(context) {
        var draws = 0
        override fun draw(canvas: Canvas) {
            draws++
            canvas.drawColor(Color.BLUE)
        }
    }

    private class CountingView(context: Context) : View(context) {
        var draws = 0
        override fun draw(canvas: Canvas) {
            draws++
        }
    }
}
