package app.k9mail.core.ui.legacy.designsystem

import android.app.Activity
import android.app.Application
import android.widget.FrameLayout
import android.widget.ScrollView
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassScrollbarsTest {
    @Test
    fun `newly inserted native scrollers also lose scrollbars on the next layout`() {
        val context = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(context)
        GlassScrollbars.observe(root)
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
        }
        root.addView(scroll)
        root.viewTreeObserver.dispatchOnGlobalLayout()
        assertThat(scroll.isVerticalScrollBarEnabled).isEqualTo(false)
        assertThat(scroll.isHorizontalScrollBarEnabled).isEqualTo(false)
    }
}
