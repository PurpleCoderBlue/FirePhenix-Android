package net.thunderbird.core.ui.theme.manager

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.view.children
import app.k9mail.core.ui.legacy.designsystem.GlassBackdropView
import app.k9mail.core.ui.legacy.designsystem.GlassControlColors
import app.k9mail.core.ui.legacy.designsystem.GlassSurfaceRenderer
import app.k9mail.core.ui.legacy.designsystem.GlassThemePalette
import app.k9mail.core.ui.legacy.designsystem.GlassWallpaperDrawable
import java.util.WeakHashMap
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

/** Owns reversible View decoration; Compose and email HTML keep their own rendering. */
internal class GlassViewStyler(private val theme: FeatureThemeProvider? = null) {
    private val toggles = WeakHashMap<CompoundButton, GlassPreferenceToggle>()
    private val originals = WeakHashMap<View, Drawable?>()
    private val decorations = WeakHashMap<View, Drawable?>()
    private val paddings = WeakHashMap<View, IntArray>()
    private val elevations = WeakHashMap<View, Float>()
    private val contentImages = WeakHashMap<View, Bitmap>()
    private val accents = GlassControlColors()
    private val resourceNames = WeakHashMap<Resources, MutableMap<Int, String>>()
    private val materialColors = WeakHashMap<View, GlassThemePalette>()

    fun visit(
        view: View,
        renderer: GlassSurfaceRenderer?,
        content: Bitmap? = null,
        palette: GlassThemePalette? = null,
    ) {
        if (view is GlassBackdropView || view is WebView || view.javaClass.name.startsWith("androidx.compose.")) return
        if (renderer == null) {
            restore(view)
        } else {
            style(view, renderer)
            if (content != null && resourceName(view) in CONTENT_HOSTS && contentImages[view] !== content) {
                if (!originals.containsKey(view)) saveOriginal(view)
                view.background = GlassWallpaperDrawable(view, content)
                contentImages[view] = content
            }
            if (palette != null) applyPalette(view, renderer, palette)
        }
        if (view is ViewGroup) {
            view.children.toList().forEach { visit(it, renderer, content, palette) }
        }
    }

    private fun applyPalette(view: View, renderer: GlassSurfaceRenderer, palette: GlassThemePalette) {
        val button = view is Button && view !is CompoundButton
        val floating = view.javaClass.simpleName.contains("FloatingActionButton")
        if ((button || floating) && materialColors[view] != palette) {
            view.background = renderer.material(view, palette.container, true)
            decorations[view] = view.background
            materialColors[view] = palette
        }
        accents.apply(view, palette)
    }

    private fun style(view: View, renderer: GlassSurfaceRenderer) {
        val name = view.javaClass.simpleName
        if (originals.containsKey(view)) {
            // AppBar lift-on-scroll may replace its backing after initial layout.
            val toolbar = name.endsWith("Toolbar") || name.endsWith("ActionBarContextView")
            val container = name.endsWith("AppBarLayout") || name.endsWith("ActionBarContainer")
            if ((toolbar || container) && decorations.containsKey(view)) {
                if (view.background !== decorations[view]) view.background = decorations[view]
            }
            return
        }
        if (!view.isSelected && !view.isActivated) styleNew(view, renderer, name)
    }

    private fun styleNew(view: View, renderer: GlassSurfaceRenderer, name: String) {
        when {
            view.tag == "firephenix-settings-section" -> {
                saveOriginal(view)
                view.background = ColorDrawable(Color.TRANSPARENT)
            }

            view is CompoundButton && isPreferenceToggle(view, resourceName(view)) && theme != null -> {
                toggles.getOrPut(view) { GlassPreferenceToggle(view, theme) }
            }

            isControl(
                view,
                name,
                if (view is EditText) resourceName(view) else "",
            ) -> applyControl(view, name, renderer)

            name.endsWith("AppBarLayout") || name.endsWith("ActionBarContainer") -> {
                saveOriginal(view)
                // Material 1.14 converts even a transparent ColorDrawable into a lift-on-scroll
                // MaterialShapeDrawable. A null backing keeps the shared wallpaper visible and
                // cannot be recolored by its lift animator; the Toolbar owns the optical surface.
                view.background = null
            }

            else -> (view.background as? ColorDrawable)?.let { clearNeutralContainer(view, it) }
        }
        if (originals.containsKey(view)) decorations[view] = view.background
    }

    private fun applyControl(view: View, name: String, renderer: GlassSurfaceRenderer) {
        saveOriginal(view)
        val color = view.backgroundTintList?.defaultColor ?: (view.background as? ColorDrawable)?.color
            ?: resolveSurface(view)
        view.background =
            renderer.material(
                view,
                color,
                name.endsWith("Toolbar") || name.endsWith("ActionBarContextView") || view is Button,
            )
        view.elevation = maxOf(view.elevation, GLASS_ELEVATION_DP * view.resources.displayMetrics.density)
        paddings[view]?.let { view.setPadding(it[0], it[1], it[2], it.last()) }
    }

    private fun clearNeutralContainer(view: View, background: ColorDrawable) {
        if (view !is ViewGroup) return
        val channels = listOf(Color.red(background.color), Color.green(background.color), Color.blue(background.color))
        if (channels.max() - channels.min() >= NEUTRAL_TOLERANCE) return
        saveOriginal(view)
        view.background = ColorDrawable(Color.TRANSPARENT)
        paddings[view]?.let { view.setPadding(it[0], it[1], it[2], it.last()) }
    }

    private fun saveOriginal(view: View) {
        originals[view] = view.background
        elevations[view] = view.elevation
        paddings[view] = intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
    }

    private fun restore(view: View) {
        accents.restore(view)
        contentImages.remove(view)
        materialColors.remove(view)
        if (view is CompoundButton) toggles.remove(view)?.close()
        if (!originals.containsKey(view)) return
        view.background = originals.remove(view)
        decorations.remove(view)
        paddings.remove(view)?.let { view.setPadding(it[0], it[1], it[2], it.last()) }
        elevations.remove(view)?.let { view.elevation = it }
    }

    fun close() {
        accents.close()
        toggles.values.toList().forEach { it.close() }
        toggles.clear()
        originals.keys.toList().forEach { restore(it) }
        resourceNames.clear()
    }

    private fun resolveSurface(view: View): Int {
        val typed = TypedValue()
        view.context.theme.resolveAttribute(android.R.attr.colorBackground, typed, true)
        return typed.data
    }

    private companion object {
        val CONTENT_HOSTS = setOf("message_list_coordinator", "message_view")
        const val GLASS_ELEVATION_DP = 4f
        const val NEUTRAL_TOLERANCE = 35
    }

    private fun resourceName(view: View): String {
        val id = view.id
        if (id == View.NO_ID || id == 0) return ""
        return resourceNames.getOrPut(view.resources) { mutableMapOf() }.getOrPut(id) {
            runCatching { view.resources.getResourceEntryName(id) }.getOrDefault("")
        }
    }
}

private fun isPreferenceToggle(view: CompoundButton, resourceName: String): Boolean {
    var parent = view.parent as? View
    var widgetFrame = false
    while (parent != null && !widgetFrame) {
        widgetFrame = parent.id == android.R.id.widget_frame
        parent = parent.parent as? View
    }
    return resourceName in setOf("checkbox", "switchWidget") && widgetFrame
}

private fun isControl(view: View, name: String, resourceName: String): Boolean = when {
    view is CompoundButton -> false

    name.endsWith("Toolbar") || name.endsWith("ActionBarContextView") || name.contains("FloatingActionButton") ||
        view is Button -> true

    view is EditText -> {
        !resourceName.contains("message_content") && !resourceName.contains("message_body")
    }

    else -> false
}

/** AppCompat contextual bars are siblings of android.R.id.content, outside the content traversal. */
internal fun GlassViewStyler.visitWindow(
    decor: View,
    contentRoot: ViewGroup,
    renderer: GlassSurfaceRenderer,
    content: Bitmap? = null,
    palette: GlassThemePalette? = null,
) {
    visit(contentRoot, renderer, content, palette)
    visitWindowBars(decor, contentRoot) { visit(it, renderer, palette = palette) }
}

private fun visitWindowBars(view: View, contentRoot: ViewGroup, decorate: (View) -> Unit) {
    if (view === contentRoot) return
    val name = view.javaClass.simpleName
    if (name.endsWith("ActionBarContextView") || name.endsWith("ActionBarContainer")) {
        decorate(view)
    } else if (view is ViewGroup) {
        view.children.forEach { visitWindowBars(it, contentRoot, decorate) }
    }
}
