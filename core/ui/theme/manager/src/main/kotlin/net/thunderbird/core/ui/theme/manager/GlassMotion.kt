package net.thunderbird.core.ui.theme.manager

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import app.k9mail.core.ui.legacy.designsystem.R
import net.thunderbird.core.preference.AnimationPreference

/** Respects both the app preference and the system's disabled-animation settings. */
object GlassMotion {
    /** Installed only by the debug app; event strings are fixed UI stage names, never mail data. */
    var diagnostics: ((String) -> Unit)? = null

    fun trace(context: Context, stage: String, elapsedMs: Long = 0) {
        diagnostics?.invoke("screen=${context.javaClass.simpleName} stage=$stage elapsedMs=$elapsedMs")
    }

    fun transitions(
        context: Context,
        preference: AnimationPreference,
    ): GlassTransitions? {
        val animate = enabled(context, preference)
        val duration = context.resources.getInteger(R.integer.firephenix_page_motion_duration)
        trace(context, "policy-enabled-$animate-duration-$duration")
        return if (animate) animationResources else null
    }

    val animationResources: GlassTransitions
        get() = GlassTransitions(
            R.anim.firephenix_forward_enter,
            R.anim.firephenix_forward_exit,
            R.anim.firephenix_back_enter,
            R.anim.firephenix_back_exit,
        )

    /** Window-level motion would move the wallpaper; the lifecycle callback animates content instead. */
    @Suppress("DEPRECATION")
    fun configureActivity(activity: Activity, preference: AnimationPreference) {
        transitions(activity, preference)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        // Pending overrides take precedence over an OEM window transition, including on API 34+.
        activity.overridePendingTransition(0, 0)
    }

    fun enabled(context: Context, preference: AnimationPreference): Boolean {
        if (preference == AnimationPreference.OFF) return false
        val resolver = context.contentResolver
        return listOf(
            Settings.Global.ANIMATOR_DURATION_SCALE,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.WINDOW_ANIMATION_SCALE,
        ).all { Settings.Global.getFloat(resolver, it, 1f) > 0f }
    }
}

data class GlassTransitions(val enter: Int, val exit: Int, val popEnter: Int, val popExit: Int)
