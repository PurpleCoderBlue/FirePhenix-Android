package com.fsck.k9.ui.messageview

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewParent
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ScrollView

/** A new vertical drag pages only if all touched reading containers were already at that boundary. */
class TouchInterceptView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val gesture = MessageSwipeGesture(ViewConfiguration.get(context).scaledTouchSlop)
    private val hit = Rect()
    private var readingParent: ViewParent? = null
    private var paging = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                paging = false
                val readers = mutableListOf<View>()
                collectReaders(this, event.rawX.toInt(), event.rawY.toInt(), readers)
                // Protect the pager, while allowing the outer reading container to take over
                // when the nested WebView reaches its edge.
                readingParent = readers.firstOrNull()?.parent
                gesture.start(
                    event.x,
                    event.y,
                    readers.any { it.canScrollVertically(-1) },
                    readers.any { it.canScrollVertically(1) },
                )
                readingParent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                paging = false
                gesture.multiplePointers()
                readingParent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                paging = !gesture.blocksPager(event.x, event.y)
                readingParent?.requestDisallowInterceptTouchEvent(!paging)
            }
        }
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            paging = false
        }
        return handled
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // A reader can request ownership again while overscrolling. Keep the edge decision locked.
        val block = disallowIntercept && !paging
        super.requestDisallowInterceptTouchEvent(block)
        // dispatchTouchEvent resets this container's flag after ACTION_DOWN; its parent may still
        // hold the old flag. Propagate even when ViewGroup short-circuits an unchanged local flag.
        parent?.requestDisallowInterceptTouchEvent(block)
    }

    private fun collectReaders(view: View, x: Int, y: Int, result: MutableList<View>) {
        if (!view.isShown || !view.getGlobalVisibleRect(hit) || !hit.contains(x, y)) return
        if (view is ScrollView || view is WebView) result.add(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) collectReaders(view.getChildAt(index), x, y, result)
        }
    }
}
