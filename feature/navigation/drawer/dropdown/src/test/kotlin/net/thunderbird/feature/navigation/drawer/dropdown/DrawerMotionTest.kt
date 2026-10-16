package net.thunderbird.feature.navigation.drawer.dropdown

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class DrawerMotionTest {
    @Test
    fun `open and close end at the shared duration and keep the host open until exit ends`() {
        val fixture = Fixture()
        fixture.motion.show(true)
        assertThat(fixture.open).isEqualTo(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(260))
        assertThat(fixture.view.translationX).isEqualTo(0f)
        fixture.motion.show(false)
        assertThat(fixture.open).isEqualTo(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(260))
        assertThat(fixture.open).isEqualTo(false)
        assertThat(fixture.events).isEqualTo(
            listOf(
                "drawer-motion-begin",
                "drawer-motion-end",
                "drawer-motion-begin",
                "drawer-motion-end",
            ),
        )
    }

    @Test
    fun `reversing a close cancels its stale completion instead of hiding the reopened drawer`() {
        val fixture = Fixture().apply { open = true }
        fixture.motion.show(false)
        // Keep the old animation callback pending while a new user action reverses it.
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(80))
        fixture.motion.show(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
        assertThat(fixture.open).isEqualTo(true)
        assertThat(fixture.closed).isEqualTo(0)
        assertThat(fixture.view.translationX).isEqualTo(0f)
    }

    @Test
    fun `disabled motion opens and closes immediately without a pending animation`() {
        val fixture = Fixture(duration = 0)
        fixture.motion.show(true)
        assertThat(fixture.view.translationX).isEqualTo(0f)
        fixture.motion.show(false)
        assertThat(fixture.open).isEqualTo(false)
    }

    private class Fixture(duration: Long = 220) {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = View(activity).apply { layout(0, 0, 400, 800) }
        var open = false
        var closed = 0
        val events = mutableListOf<String>()
        val motion = DrawerMotion(view, { open }, {
            open = it
            if (!it) closed++
        }, { duration }, events::add)
        init {
            activity.setContentView(FrameLayout(activity).apply { addView(view, FrameLayout.LayoutParams(400, 800)) })
            view.layout(0, 0, 400, 800)
        }
    }
}
