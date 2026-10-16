package net.thunderbird.components.ui.bolt.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/** Adjacent page translation prevents translucent content from overlapping during navigation. */
data class GlassPageMotion(val enabled: Boolean = true) {
    fun enter(back: Boolean = false): EnterTransition = if (!enabled) {
        EnterTransition.None
    } else {
        slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) {
            if (back) -it else it
        }
    }

    fun exit(back: Boolean = false): ExitTransition = if (!enabled) {
        ExitTransition.None
    } else {
        slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) {
            if (back) it else -it
        }
    }

    companion object {
        const val DURATION = 220
    }
}
