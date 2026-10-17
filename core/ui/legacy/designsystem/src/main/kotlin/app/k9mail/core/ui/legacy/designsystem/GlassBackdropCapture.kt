package app.k9mail.core.ui.legacy.designsystem

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer
import android.graphics.drawable.DrawableWrapper
import android.graphics.drawable.LayerDrawable
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat

/** An optical material must never be recorded into its own source. */
internal interface GlassOpticalMaterial

/** Samples native UI without submitting external renderers or glass foreground twice. */
internal class GlassBackdropCapture(private val root: ViewGroup) {
    fun draw(canvas: Canvas, targets: Set<View>) {
        if (isCapturing) return
        isCapturing = true
        try {
            capture(canvas, root, targets)
        } finally {
            isCapturing = false
        }
    }

    companion object {
        // All sources are recorded on the UI thread. A foreign renderer must never enter this source.
        var isCapturing: Boolean = false
            private set
    }

    private fun isExternalSurface(view: View): Boolean {
        val surface = view is WebView || view is SurfaceView || view is TextureView
        return surface || view.javaClass.name.startsWith("androidx.compose.")
    }

    private fun isControlForeground(view: View): Boolean {
        val input = view is Button || view is EditText
        return input || view is ImageButton || view is Toolbar
    }

    private fun capture(canvas: Canvas, view: View, targets: Set<View>) {
        // Newly attached controls can be sampled before the next styling listener runs.
        // Exclude their foreground by role, before a Ripple becomes a source-backed glass drawable.
        val control = isControlForeground(view) || targets.contains(view)
        val excluded = control || view.background.containsOpticalMaterial() ||
            view.foreground.containsOpticalMaterial()
        if (view.visibility != View.VISIBLE || view.alpha == 0f || excluded) {
            return
        }
        if (isClippedOutsideViewport(canvas, view)) return
        when {
            // A WebView's hardware functor must stay in its normal window draw pass.
            // Its background can still fill the material; HTML is never redrawn here.
            isExternalSurface(view) -> view.background?.draw(canvas)

            view !is ViewGroup -> view.draw(canvas)

            // Never dispatch ViewGroup.draw again: dispatchDraw advances child animations and
            // submits Compose/external renderer hosts a second time during the same frame.
            else -> captureChildren(canvas, view, targets)
        }
    }

    @Suppress("DEPRECATION") // Keep the Canvas overload available on API 23.
    private fun isClippedOutsideViewport(canvas: Canvas, view: View): Boolean {
        if (view !is ViewGroup || !view.clipChildren) return false
        return view.width > 0 && view.height > 0 &&
            canvas.quickReject(0f, 0f, view.width.toFloat(), view.height.toFloat(), Canvas.EdgeType.AA)
    }

    private fun captureChildren(canvas: Canvas, view: ViewGroup, targets: Set<View>) {
        view.background?.draw(canvas)
        val groupSave = canvas.save()
        if (view.clipChildren) canvas.clipRect(0, 0, view.width, view.height)
        val hasPadding =
            view.paddingLeft != 0 || view.paddingTop != 0 || view.paddingRight != 0 || view.paddingBottom != 0
        if (view.clipToPadding && hasPadding) {
            canvas.clipRect(
                view.paddingLeft,
                view.paddingTop,
                view.width - view.paddingRight,
                view.height - view.paddingBottom,
            )
        }
        for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            val childSave = canvas.save()
            canvas.translate((child.left - view.scrollX).toFloat(), (child.top - view.scrollY).toFloat())
            canvas.concat(child.matrix)
            capture(canvas, child, targets)
            canvas.restoreToCount(childSave)
        }
        canvas.restoreToCount(groupSave)
    }
}

/** Material/Ripple/Inset wrappers can retain an optical drawable after its View is recycled. */
private fun Drawable?.containsOpticalMaterial(): Boolean = when (this) {
    null -> false
    is GlassOpticalMaterial -> true
    is DrawableWrapper -> drawable.containsOpticalMaterial()
    is DrawableWrapperCompat -> drawable.containsOpticalMaterial()
    is LayerDrawable -> (0 until numberOfLayers).any { getDrawable(it).containsOpticalMaterial() }
    is DrawableContainer -> current.takeUnless { it === this }.containsOpticalMaterial()
    else -> DrawableCompat.unwrap<Drawable>(this).takeUnless { it === this }.containsOpticalMaterial()
}
