package net.thunderbird.feature.navigation.drawer.dropdown

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout

/** Programmatic drawer motion shares page timing without retaining a full-screen hardware layer. */
internal class DrawerMotion(
    private val view: View,
    private val isOpen: () -> Boolean,
    private val setOpen: (Boolean) -> Unit,
    private val durationMillis: () -> Long,
    private val trace: (String) -> Unit,
) {
    private var animation: ValueAnimator? = null
    private var generation = 0

    fun show(open: Boolean) {
        val wasOpen = isOpen()
        if (animation == null && open == wasOpen) return
        val from = view.translationX
        val reversing = animation != null
        cancel()
        val token = generation
        if (open) setOpen(true)
        if (open && view.width == 0) {
            val parentWidth = (view.parent as? View)?.width ?: 0
            view.translationX = -parentWidth.toFloat() * direction()
            view.doOnLayout { if (token == generation) start(open, closedOffset(), token) }
        } else {
            start(open, if (open && !wasOpen && !reversing) closedOffset() else from, token)
        }
    }

    fun cancel() {
        generation++
        animation?.cancel()
        animation = null
        view.translationX = 0f
    }

    private fun start(open: Boolean, from: Float, token: Int) {
        val duration = durationMillis()
        trace("drawer-motion-begin")
        if (duration <= 0 || view.width == 0) {
            finish(open, token)
        } else {
            view.translationX = from
            animation = ValueAnimator.ofFloat(from, if (open) 0f else closedOffset()).apply {
                this.duration = duration
                interpolator = AnimationUtils.loadInterpolator(view.context, android.R.interpolator.fast_out_slow_in)
                addUpdateListener { view.translationX = it.animatedValue as Float }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (token == generation) finish(open, token)
                    }
                })
                start()
            }
        }
    }

    private fun finish(open: Boolean, token: Int) {
        if (token != generation) return
        animation = null
        if (!open) setOpen(false)
        view.translationX = 0f
        trace("drawer-motion-end")
    }

    private fun closedOffset(): Float = -view.width.toFloat() * direction()

    private fun direction(): Float = if (ViewCompat.getLayoutDirection(view) ==
        ViewCompat.LAYOUT_DIRECTION_RTL
    ) {
        -1f
    } else {
        1f
    }
}
