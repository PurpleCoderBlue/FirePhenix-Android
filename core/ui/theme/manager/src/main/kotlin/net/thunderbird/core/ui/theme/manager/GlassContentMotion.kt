package net.thunderbird.core.ui.theme.manager

import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.children
import app.k9mail.core.ui.legacy.designsystem.GlassBackdropView

/** Animates only page content. The single wallpaper sibling never receives a transformation. */
internal object GlassContentMotion {
    fun start(root: ViewGroup, motion: GlassTransitions?, entering: Boolean, returning: Boolean) {
        val resource = motion?.let {
            if (entering) {
                if (returning) it.popEnter else it.enter
            } else {
                if (returning) it.popExit else it.exit
            }
        }
        root.children.filterNot { it is GlassBackdropView }.forEach { content ->
            content.clearAnimation()
            if (resource != null) {
                val animation = AnimationUtils.loadAnimation(root.context, resource)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) = Unit
                    override fun onAnimationRepeat(animation: Animation) = Unit
                    override fun onAnimationEnd(animation: Animation) {
                        // A later navigation must not be cleared by this animation's completion.
                        content.post { if (content.animation === animation) content.clearAnimation() }
                    }
                })
                content.startAnimation(animation)
            }
        }
    }
}
