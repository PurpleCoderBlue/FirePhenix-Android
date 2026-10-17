package net.thunderbird.core.ui.theme.manager

import android.app.Application
import android.provider.Settings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import net.thunderbird.core.preference.AnimationPreference
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GlassMotionTest {
    @Test
    fun `native and Compose pages use the same duration contract`() {
        val context = RuntimeEnvironment.getApplication()
        assertk.assertThat(
            context.resources.getInteger(
                app.k9mail.core.ui.legacy.designsystem.R.integer.firephenix_page_motion_duration,
            ),
        )
            .isEqualTo(net.thunderbird.components.ui.bolt.theme.GlassPageMotion.DURATION)
    }

    @Test
    fun `disabled system animation scale or app preference prevents native glass transitions`() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        assertThat(GlassMotion.enabled(context, AnimationPreference.FOLLOW_SYSTEM)).isFalse()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        assertThat(GlassMotion.enabled(context, AnimationPreference.OFF)).isFalse()
    }

    @Test
    fun `activity window stays stationary so its wallpaper cannot slide`() {
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        GlassMotion.configureActivity(activity, AnimationPreference.FOLLOW_SYSTEM)
        val resource = org.robolectric.Shadows.shadowOf(activity).pendingTransitionEnterAnimationResourceId
        assertThat(resource).isEqualTo(0)
    }
}
