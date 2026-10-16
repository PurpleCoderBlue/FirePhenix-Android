package app.k9mail.core.ui.legacy.designsystem

import android.graphics.Canvas
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

/** An optical material must never be recorded into its own source. */
internal interface GlassOpticalMaterial

/** Samples native UI without submitting external renderers or glass foreground twice. */
internal class GlassBackdropCapture(private val root: ViewGroup) {
    fun draw(canvas: Canvas, targets: Set<View>) {
        capture(canvas, root, targets)
    }

    private fun isExternalSurface(view: View): Boolean =
        view is WebView || view is SurfaceView || view is TextureView

    private fun capture(canvas: Canvas, view: View, targets: Set<View>) {
        val excluded = targets.contains(view) || view.background is GlassOpticalMaterial
        if (view.visibility != View.VISIBLE || view.alpha == 0f || excluded) {
            return
        }
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
