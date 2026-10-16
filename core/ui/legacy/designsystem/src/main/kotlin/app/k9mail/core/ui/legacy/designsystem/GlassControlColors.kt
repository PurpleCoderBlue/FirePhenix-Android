package app.k9mail.core.ui.legacy.designsystem

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import java.util.WeakHashMap

/** Changes only accent roles and keeps original colors for switching back to Original. */
class GlassControlColors {
    private val texts = WeakHashMap<TextView, ColorStateList>()
    private val buttons = WeakHashMap<CompoundButton, ColorStateList?>()
    private val images = WeakHashMap<ImageView, ColorStateList?>()

    fun visit(view: View, palette: GlassThemePalette) {
        if (view is WebView || view.javaClass.name.startsWith("androidx.compose.")) return
        apply(view, palette)
        if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index), palette)
    }

    fun apply(view: View, palette: GlassThemePalette) {
        if (view is TextView) {
            if (texts.containsKey(view) || view is Button || GlassThemePalette.isAccent(view, view.currentTextColor)) {
                texts.getOrPut(view) { view.textColors }
                val desired = accentStates(palette.accent)
                if (view.currentTextColor !=
                    desired.getColorForState(view.drawableState, palette.accent)
                ) {
                    view.setTextColor(desired)
                }
            }
        }
        if (view is CompoundButton) {
            if (!buttons.containsKey(view)) buttons[view] = view.buttonTintList
            val tint = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(palette.accent, palette.onContainer),
            )
            if (view.buttonTintList?.defaultColor != tint.defaultColor ||
                view.buttonTintList?.getColorForState(intArrayOf(android.R.attr.state_checked), 0) != palette.accent
            ) {
                view.buttonTintList = tint
            }
        }
        if (view is ImageView && view.javaClass.simpleName.contains("FloatingActionButton")) {
            if (!images.containsKey(view)) images[view] = view.imageTintList
            if (view.imageTintList?.defaultColor !=
                palette.onContainer
            ) {
                view.imageTintList = accentStates(palette.onContainer)
            }
        }
    }

    private fun accentStates(color: Int) = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
        intArrayOf((color and RGB_MASK) or DISABLED_ALPHA, color),
    )

    fun restore(view: View) {
        if (view is TextView) texts.remove(view)?.let(view::setTextColor)
        if (view is CompoundButton && buttons.containsKey(view)) view.buttonTintList = buttons.remove(view)
        if (view is ImageView && images.containsKey(view)) view.imageTintList = images.remove(view)
    }

    private companion object {
        const val RGB_MASK = 0x00FFFFFF
        const val DISABLED_ALPHA = 0x61000000
    }

    fun close() {
        (texts.keys.toList() + buttons.keys.toList() + images.keys.toList()).forEach(::restore)
    }
}
