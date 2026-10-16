package net.thunderbird.core.ui.theme.manager

import android.app.Activity
import android.content.res.Resources
import app.k9mail.core.ui.legacy.designsystem.R
import java.util.WeakHashMap

/** Saves the original theme before applying glass-only popup overlays. */
internal class GlassPopupThemes {
    private val originals = WeakHashMap<Activity, Resources.Theme>()
    private val modes = WeakHashMap<Activity, Pair<Boolean, Boolean>>()

    fun apply(activity: Activity, dark: Boolean, animated: Boolean = true) {
        if (modes[activity] == (dark to animated)) return
        val original = originals.getOrPut(activity) {
            activity.resources.newTheme().apply { setTo(activity.theme) }
        }
        activity.theme.setTo(original)
        val overlay = when {
            dark && !animated -> R.style.FirePhenixGlassPopupsDarkNoMotion
            !animated -> R.style.FirePhenixGlassPopupsNoMotion
            dark -> R.style.FirePhenixGlassPopupsDark
            else -> R.style.FirePhenixGlassPopups
        }
        activity.theme.applyStyle(overlay, true)
        activity.window.setWindowAnimations(if (animated) R.style.FirePhenixGlassWindowAnimations else 0)
        modes[activity] = dark to animated
    }

    fun remove(activity: Activity) {
        originals.remove(activity)
        modes.remove(activity)
    }
}
