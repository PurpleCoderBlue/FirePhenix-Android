package net.thunderbird.core.ui.theme.manager

import android.app.Activity
import android.app.Application
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.Transformation
import app.k9mail.core.ui.legacy.designsystem.R
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
class GlassPageMotionTest {
    @Test
    fun `transparent pages remain adjacent instead of overlapping during navigation`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val directions = listOf(
            R.anim.firephenix_forward_enter to R.anim.firephenix_forward_exit,
            R.anim.firephenix_back_enter to R.anim.firephenix_back_exit,
        )
        directions.forEach { (enterResource, exitResource) ->
            val enter = AnimationUtils.loadAnimation(activity, enterResource)
            val exit = AnimationUtils.loadAnimation(activity, exitResource)
            val width = 1000
            listOf(enter, exit).forEach {
                it.initialize(width, 800, width, 800)
                it.startTime = 0L
            }
            listOf(0L, 90L, 180L, 270L, 359L).forEach { time ->
                val incoming = Transformation()
                val outgoing = Transformation()
                enter.getTransformation(time, incoming)
                exit.getTransformation(time, outgoing)
                val incomingPoint = floatArrayOf(0f, 0f)
                val outgoingPoint = floatArrayOf(0f, 0f)
                incoming.matrix.mapPoints(incomingPoint)
                outgoing.matrix.mapPoints(outgoingPoint)
                // Both pages are translucent: their content rectangles must never overlap.
                assertThat(abs(incomingPoint[0] - outgoingPoint[0])).isGreaterThanOrEqualTo(width - 1f)
            }
        }
    }

    @Test
    fun `all page directions use one duration without full page alpha compositing`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val resources = listOf(
            R.anim.firephenix_forward_enter,
            R.anim.firephenix_forward_exit,
            R.anim.firephenix_back_enter,
            R.anim.firephenix_back_exit,
        )
        resources.forEach { resource ->
            val animation = AnimationUtils.loadAnimation(activity, resource) as AnimationSet
            assertThat(animation.duration).isEqualTo(360L)
            assertThat(animation.animations.any { it is AlphaAnimation }).isFalse()
        }
    }
}
