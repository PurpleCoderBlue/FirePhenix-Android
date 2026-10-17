package net.thunderbird.core.ui.theme.manager

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import app.k9mail.core.ui.legacy.designsystem.GlassScrollbars
import app.k9mail.core.ui.legacy.designsystem.GlassSurfaceRenderer
import app.k9mail.core.ui.legacy.designsystem.GlassThemePalette

/** Only layout or material changes need a full native hierarchy visit during page motion. */
internal class GlassWindowStyler(private val styler: GlassViewStyler) {
    private var dirty = true
    private var image: Bitmap? = null
    private var colors: GlassThemePalette? = null

    fun invalidateWindow() {
        dirty = true
    }

    fun refreshWindow(
        decor: View,
        root: ViewGroup,
        renderer: GlassSurfaceRenderer,
        content: Bitmap? = null,
        palette: GlassThemePalette? = null,
    ) {
        GlassScrollbars.observeWindows()
        if (dirty || image !== content || colors != palette) {
            dirty = false
            image = content
            colors = palette
            styler.visitWindow(decor, root, renderer, content, palette)
        } else {
            styler.restoreWindowBars()
        }
    }

    fun close() {
        styler.close()
        image = null
        colors = null
    }
}
