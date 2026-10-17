package app.k9mail.core.ui.legacy.designsystem

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Covers independent dialog and popup windows without rescanning every hierarchy on every frame. */
object GlassScrollbars {
    private val observed = WeakHashMap<View, Unit>()

    fun observeWindows() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowInspector.getGlobalWindowViews().forEach(::observe)
        }
    }

    fun observe(root: View) {
        if (observed.put(root, Unit) != null) return
        hide(root)
        val reference = WeakReference(root)
        root.viewTreeObserver.addOnGlobalLayoutListener { reference.get()?.let(::hide) }
    }

    private fun hide(view: View) {
        if (view.isVerticalScrollBarEnabled) view.isVerticalScrollBarEnabled = false
        if (view.isHorizontalScrollBarEnabled) view.isHorizontalScrollBarEnabled = false
        if (view is ViewGroup) for (index in 0 until view.childCount) hide(view.getChildAt(index))
    }
}
