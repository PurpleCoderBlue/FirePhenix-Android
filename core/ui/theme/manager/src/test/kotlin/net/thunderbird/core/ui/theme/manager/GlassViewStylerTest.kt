package net.thunderbird.core.ui.theme.manager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.legacy.designsystem.GlassSurfaceRenderer
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import com.google.android.material.appbar.AppBarLayout
import java.time.Duration
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassViewStylerTest {
    @Test
    fun `unchanged animation frames do not traverse the native child hierarchy again`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = CountingFrame(activity).apply { addView(TextView(activity)) }
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassWindowStyler(GlassViewStyler())
        testSubject.refreshWindow(root, root, renderer)
        root.childReads = 0

        repeat(20) { testSubject.refreshWindow(root, root, renderer) }

        assertThat(root.childReads).isEqualTo(0)
        testSubject.invalidateWindow()
        testSubject.refreshWindow(root, root, renderer)
        assertThat(root.childReads > 0).isEqualTo(true)
        testSubject.close()
        renderer.close()
    }

    @Test
    fun `cached window frames still restore a replaced black header`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val toolbar = android.widget.Toolbar(activity).apply { background = ColorDrawable(Color.BLACK) }
        val root = FrameLayout(activity).apply { addView(toolbar) }
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassWindowStyler(GlassViewStyler())
        testSubject.refreshWindow(root, root, renderer)
        val glass = toolbar.background

        toolbar.background = ColorDrawable(Color.BLACK)
        testSubject.refreshWindow(root, root, renderer)

        assertThat(toolbar.background).isSameInstanceAs(glass)
        testSubject.close()
        renderer.close()
    }

    @Test
    fun `controls added by a new layout receive their glass decoration`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassWindowStyler(GlassViewStyler())
        testSubject.refreshWindow(root, root, renderer)
        val original = ColorDrawable(Color.BLUE)
        val button = Button(activity).apply { background = original }

        root.addView(button)
        testSubject.invalidateWindow()
        testSubject.refreshWindow(root, root, renderer)

        assertThat(button.background === original).isFalse()
        testSubject.close()
        assertThat(button.background).isSameInstanceAs(original)
        renderer.close()
    }

    private class CountingFrame(context: Context) : FrameLayout(context) {
        var childReads = 0
        override fun getChildAt(index: Int): View {
            childReads++
            return super.getChildAt(index)
        }
    }

    @Test
    fun `repeated frame visits skip missing ids and resolve unchanged resource ids once`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val resources = CountingResources(activity.resources)
        val context = object : ContextWrapper(activity) {
            override fun getResources(): Resources = resources
        }
        val child = View(context)
        val root = FrameLayout(context).apply { addView(child) }
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassViewStyler()
        val image = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        repeat(3) { testSubject.visit(root, renderer, image) }
        assertThat(resources.lookups[View.NO_ID] ?: 0).isEqualTo(0)
        child.id = android.R.id.content
        repeat(3) { testSubject.visit(root, renderer, image) }
        assertThat(resources.lookups[android.R.id.content]).isEqualTo(1)
        child.id = android.R.id.title
        testSubject.visit(root, renderer, image)
        assertThat(resources.lookups[android.R.id.title]).isEqualTo(1)
        testSubject.close()
        renderer.close()
    }

    @Test
    fun `contextual toolbar outside android content receives the window glass surface`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context =
            ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar)
        val content = FrameLayout(context).apply { id = android.R.id.content }
        val original = ColorDrawable(Color.BLACK)
        val contextual = androidx.appcompat.widget.ActionBarContextView(context).apply { background = original }
        val decor = FrameLayout(context).apply {
            addView(content)
            addView(contextual)
        }
        val renderer = GlassSurfaceRenderer(content)
        val styler = GlassViewStyler()
        styler.visitWindow(decor, content, renderer)
        assertThat(contextual.background === original).isFalse()
        val glass = contextual.background
        contextual.background = ColorDrawable(Color.BLACK)
        styler.visitWindow(decor, content, renderer)
        assertThat(contextual.background).isSameInstanceAs(glass)
        styler.close()
        assertThat(contextual.background).isSameInstanceAs(original)
        renderer.close()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `Material AppBar stays transparent after actual lift animation and restores Original`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context =
            ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar)
        val appBar = AppBarLayout(context).apply { isLiftOnScroll = true }
        val original = appBar.background
        val root = FrameLayout(context).apply { addView(appBar, FrameLayout.LayoutParams(200, 80)) }
        root.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 200, 200)
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassViewStyler()
        testSubject.visit(root, renderer)

        appBar.setLifted(true)
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        val bitmap = Bitmap.createBitmap(200, 80, Bitmap.Config.ARGB_8888)
        appBar.draw(Canvas(bitmap))

        assertThat(Color.alpha(bitmap.getPixel(100, 40))).isEqualTo(0)
        testSubject.close()
        assertThat(appBar.background).isSameInstanceAs(original)
        renderer.close()
    }

    @Test
    fun `scroll lift replacement cannot leave the toolbar black and Original restores its original backing`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val original = ColorDrawable(Color.WHITE)
        val toolbar = android.widget.Toolbar(activity).apply { background = original }
        val root = FrameLayout(activity).apply { addView(toolbar) }
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassViewStyler()
        testSubject.visit(root, renderer)
        val glass = toolbar.background
        toolbar.background = ColorDrawable(Color.BLACK)
        testSubject.visit(root, renderer)
        assertThat(toolbar.background).isSameInstanceAs(glass)
        testSubject.close()
        assertThat(toolbar.background).isSameInstanceAs(original)
        renderer.close()
    }

    @Test
    fun `switching back restores native background padding and elevation`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        val original = ColorDrawable(Color.WHITE)
        val button = Button(activity).apply {
            background = original
            setPadding(11, 13, 17, 19)
            elevation = 2f
        }
        root.addView(button)
        val renderer = GlassSurfaceRenderer(root)
        val testSubject = GlassViewStyler()

        testSubject.visit(root, renderer)
        assertThat(button.background === original).isFalse()
        testSubject.visit(root, null)

        assertThat(button.background).isSameInstanceAs(original)
        assertThat(listOf(button.paddingLeft, button.paddingTop, button.paddingRight, button.paddingBottom))
            .isEqualTo(listOf(11, 13, 17, 19))
        assertThat(button.elevation).isEqualTo(2f)
        renderer.close()
    }

    @Test
    fun `glass toggle preserves the original listener state and restores its native widget`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var calls = 0
        val button = CheckBox(activity).apply {
            id = android.R.id.checkbox
            setOnCheckedChangeListener { _, _ -> calls++ }
        }
        val widget = FrameLayout(activity).apply {
            id = android.R.id.widget_frame
            addView(button)
        }
        val root = FrameLayout(activity).apply { addView(widget) }
        val original = button.buttonDrawable
        val theme = object : FeatureThemeProvider {
            @Composable override fun WithTheme(content: @Composable () -> Unit) {
                content()
            }

            @Composable override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
                content()
            }
        }
        val styler = GlassViewStyler(theme)
        val renderer = GlassSurfaceRenderer(root)

        styler.visit(root, renderer)
        assertThat(widget.childCount).isEqualTo(2)
        assertThat(button.visibility).isEqualTo(View.GONE)
        button.performClick()
        styler.close()

        assertThat(calls).isEqualTo(1)
        assertThat(button.isChecked).isEqualTo(true)
        assertThat(button.visibility).isEqualTo(View.VISIBLE)
        assertThat(button.buttonDrawable).isSameInstanceAs(original)
        assertThat(widget.childCount).isEqualTo(1)
        renderer.close()
    }

    @Test
    fun `glass section backing is transparent and original backing is restored`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val original = ColorDrawable(Color.BLACK)
        val header = TextView(activity).apply {
            tag = "firephenix-settings-section"
            background = original
        }
        val root = FrameLayout(activity).apply { addView(header) }
        val styler = GlassViewStyler()
        val renderer = GlassSurfaceRenderer(root)

        styler.visit(root, renderer)
        assertThat((header.background as ColorDrawable).color).isEqualTo(Color.TRANSPARENT)
        styler.close()
        assertThat(header.background).isSameInstanceAs(original)
        renderer.close()
    }

    @Test
    fun `original appearance does not decorate an untouched native control`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val original = ColorDrawable(Color.BLUE)
        val button = Button(activity).apply { background = original }
        val root = FrameLayout(activity).apply { addView(button) }
        val testSubject = GlassViewStyler()

        testSubject.visit(root, null)

        assertThat(button.background).isSameInstanceAs(original)
    }

    @Suppress("DEPRECATION")
    private class CountingResources(base: Resources) : Resources(base.assets, base.displayMetrics, base.configuration) {
        val lookups = mutableMapOf<Int, Int>()

        override fun getResourceEntryName(id: Int): String {
            lookups[id] = (lookups[id] ?: 0) + 1
            return super.getResourceEntryName(id)
        }
    }

    @Test
    fun `native lists and webviews hide both scrollbars without disabling scrolling`() {
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = android.widget.FrameLayout(context)
        val scroll = android.widget.ScrollView(context)
        val web = android.webkit.WebView(context)
        root.addView(scroll)
        scroll.addView(web)
        listOf(scroll, web).forEach {
            it.isVerticalScrollBarEnabled = true
            it.isHorizontalScrollBarEnabled = true
        }
        GlassViewStyler().visit(root, GlassSurfaceRenderer(root))
        listOf(scroll, web).forEach {
            assertThat(it.isVerticalScrollBarEnabled).isEqualTo(false)
            assertThat(it.isHorizontalScrollBarEnabled).isEqualTo(false)
        }
    }

    @Test
    @Config(sdk = [35])
    fun `independent dialog window also hides its native scrollbars`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val scroll = android.widget.ScrollView(activity).apply { isVerticalScrollBarEnabled = true }
        val dialog = android.app.Dialog(activity)
        dialog.setContentView(scroll)
        dialog.show()
        GlassViewStyler().visitWindow(activity.window.decorView, content, GlassSurfaceRenderer(content))
        assertThat(scroll.isVerticalScrollBarEnabled).isEqualTo(false)
        dialog.dismiss()
    }
}
