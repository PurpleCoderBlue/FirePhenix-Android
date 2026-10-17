package net.thunderbird.core.ui.theme.manager

import android.app.Activity
import android.app.Application
import android.view.View
import android.widget.FrameLayout
import app.k9mail.core.ui.legacy.designsystem.GlassBackdropView
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassContentMotionTest {
    @Test
    fun `all four activity directions animate content while leaving wallpaper untouched`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = FrameLayout(activity)
        val background = GlassBackdropView(activity)
        val content = View(activity)
        root.addView(background)
        root.addView(content)
        listOf(true, false).forEach { entering ->
            listOf(true, false).forEach { returning ->
                GlassContentMotion.start(root, GlassMotion.animationResources, entering, returning)
                assertThat(background.animation).isNull()
                assertThat(content.animation).isNotNull()
                assertThat(content.animation.duration).isEqualTo(360L)
            }
        }
        GlassContentMotion.start(root, null, true, false)
        assertThat(content.animation).isNull()
        assertThat(background.animation).isNull()
    }
}
