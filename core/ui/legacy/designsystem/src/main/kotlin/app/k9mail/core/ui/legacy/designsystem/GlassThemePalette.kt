package app.k9mail.core.ui.legacy.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.TypedValue
import android.view.View
import com.google.android.material.color.MaterialColors
import java.util.WeakHashMap

/** Contrast-aware accent roles shared by View and Compose glass controls. */
data class GlassThemePalette(val accent: Int, val onAccent: Int, val container: Int, val onContainer: Int) {
    companion object {
        private val activities = WeakHashMap<Activity, GlassThemePalette>()
        fun attach(activity: Activity, palette: GlassThemePalette) {
            activities[activity] = palette
        }
        fun detach(activity: Activity) {
            activities.remove(activity)
        }
        fun forContext(context: Context): GlassThemePalette? {
            var current = context
            while (true) {
                if (current is Activity) return activities[current]
                if (current !is ContextWrapper || current.baseContext === current) return null
                current = current.baseContext
            }
        }

        fun isAccent(view: View, color: Int): Boolean {
            val value = TypedValue()
            return listOf(androidx.appcompat.R.attr.colorPrimary, com.google.android.material.R.attr.colorSecondary)
                .any { view.context.theme.resolveAttribute(it, value, true) && color == value.data }
        }

        fun from(seed: Int, dark: Boolean): GlassThemePalette {
            val roles = MaterialColors.getColorRoles(seed, !dark)
            return GlassThemePalette(roles.accent, roles.onAccent, roles.accentContainer, roles.onAccentContainer)
        }
    }
}
