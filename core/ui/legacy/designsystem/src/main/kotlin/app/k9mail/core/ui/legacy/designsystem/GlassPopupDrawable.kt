package app.k9mail.core.ui.legacy.designsystem

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import org.xmlpull.v1.XmlPullParser

/** Optical popup background; foreground menus/dialog content retain their native input handling. */
@Keep
class GlassPopupDrawable : Drawable() {
    private var density = 1f
    private var delegate: Drawable? = null
    private val fallback = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accents = GlassControlColors()

    override fun inflate(resources: Resources, parser: XmlPullParser, attrs: AttributeSet, theme: Resources.Theme?) {
        super.inflate(resources, parser, attrs, theme)
        density = resources.displayMetrics.density
    }

    override fun draw(canvas: Canvas) {
        val view = callbackView()
        if (view != null) GlassThemePalette.forContext(view.context)?.let { accents.visit(view.rootView, it) }
        if (delegate == null && view != null) {
            delegate = GlassSurfaceRenderer.popupMaterial(view)
        }
        val background = delegate
        if (background != null) {
            background.bounds = bounds
            background.state = state
            background.draw(canvas)
        } else {
            val color = android.util.TypedValue()
            view?.context?.theme?.resolveAttribute(android.R.attr.colorBackground, color, true)
            fallback.color = if (color.data != 0) color.data else FALLBACK_COLOR
            canvas.drawRoundRect(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat(),
                CORNER_RADIUS_DP * density,
                CORNER_RADIUS_DP * density,
                fallback,
            )
        }
    }

    private fun callbackView(): View? {
        var owner = callback
        while (owner is Drawable) owner = owner.callback
        return owner as? View
    }

    override fun getPadding(padding: Rect): Boolean {
        val inset = (CONTENT_INSET_DP * density).toInt()
        padding.set(inset, inset, inset, inset)
        return true
    }

    override fun getOutline(outline: Outline) = outline.setRoundRect(bounds, CORNER_RADIUS_DP * density)
    override fun isStateful(): Boolean = true
    override fun setAlpha(alpha: Int) {
        delegate?.alpha = alpha
    }
    override fun setColorFilter(colorFilter: ColorFilter?) {
        delegate?.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    private companion object {
        const val CORNER_RADIUS_DP = 24f
        const val CONTENT_INSET_DP = 8f
        val FALLBACK_COLOR = 0xFFF8F8FC.toInt()
    }
}
