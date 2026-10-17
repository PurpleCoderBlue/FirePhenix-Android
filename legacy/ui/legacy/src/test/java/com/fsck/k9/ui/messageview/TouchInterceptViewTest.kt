package com.fsck.k9.ui.messageview

import android.app.Activity
import android.app.Application
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class TouchInterceptViewTest {
    @Test
    fun `reading owns the first move before an outer pager can intercept it`() {
        val parent = fixture(atBottom = false)
        dispatch(parent, MotionEvent.ACTION_DOWN, 300f)
        dispatch(parent, MotionEvent.ACTION_MOVE, 240f)
        dispatch(parent, MotionEvent.ACTION_MOVE, 180f)
        assertThat(parent.interceptedMoves).isEqualTo(0)
    }

    @Test
    fun `a new upward drag at the bottom releases interception to the outer pager`() {
        val parent = fixture(atBottom = true)
        dispatch(parent, MotionEvent.ACTION_DOWN, 300f)
        dispatch(parent, MotionEvent.ACTION_MOVE, 240f)
        dispatch(parent, MotionEvent.ACTION_MOVE, 180f)
        assertThat(parent.interceptedMoves).isEqualTo(1)
    }

    @Test
    fun `production hierarchy gates the pager from outside its message content`() {
        listOf(false, true).forEach { atBottom ->
            val pager = fixture(atBottom)
            val gate = pager.getChildAt(0) as TouchInterceptView
            val scroll = gate.getChildAt(0)
            gate.removeView(scroll)
            pager.removeView(gate)
            val host = pager.parent as FrameLayout
            host.removeView(pager)
            gate.addView(pager, FrameLayout.LayoutParams(400, 600))
            pager.addView(scroll, FrameLayout.LayoutParams(400, 600))
            host.addView(gate, FrameLayout.LayoutParams(400, 600))
            host.measure(exact(400), exact(600))
            host.layout(0, 0, 400, 600)
            gate.layout(0, 0, 400, 600)
            pager.layout(0, 0, 400, 600)
            scroll.layout(0, 0, 400, 600)

            dispatch(gate, MotionEvent.ACTION_DOWN, 300f)
            dispatch(gate, MotionEvent.ACTION_MOVE, 240f)
            dispatch(gate, MotionEvent.ACTION_MOVE, 180f)

            assertThat(pager.interceptedMoves).isEqualTo(if (atBottom) 1 else 0)
        }
    }

    @Test
    fun `inner body boundary allows outer reading scroll before paging`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible().get()
        val gate = TouchInterceptView(activity, null)
        val pager = PagingParent(activity)
        val outer = object : ScrollView(activity) {
            var interceptedMoves = 0
            override fun canScrollVertically(direction: Int): Boolean = true
            override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                    interceptedMoves++
                    return true
                }
                return false
            }
            override fun onTouchEvent(event: MotionEvent): Boolean = true
        }
        val inner = object : ScrollView(activity) {
            override fun canScrollVertically(direction: Int): Boolean = direction < 0
            override fun onTouchEvent(event: MotionEvent): Boolean = true
        }
        inner.addView(View(activity), FrameLayout.LayoutParams(400, 1000))
        outer.addView(inner, FrameLayout.LayoutParams(400, 600))
        pager.addView(outer, FrameLayout.LayoutParams(400, 600))
        gate.addView(pager, FrameLayout.LayoutParams(400, 600))
        activity.setContentView(gate)
        activity.window.decorView.measure(exact(400), exact(600))
        activity.window.decorView.layout(0, 0, 400, 600)
        gate.measure(exact(400), exact(600))
        gate.layout(0, 0, 400, 600)
        pager.layout(0, 0, 400, 600)
        outer.layout(0, 0, 400, 600)
        inner.layout(0, 0, 400, 600)

        dispatch(gate, MotionEvent.ACTION_DOWN, 300f)
        dispatch(gate, MotionEvent.ACTION_MOVE, 240f)
        dispatch(gate, MotionEvent.ACTION_MOVE, 180f)

        assertThat(outer.interceptedMoves).isEqualTo(1)
        assertThat(pager.interceptedMoves).isEqualTo(0)
    }

    private fun fixture(atBottom: Boolean): PagingParent {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible().get()
        val parent = PagingParent(activity)
        val gate = TouchInterceptView(activity, null)
        val scroll = object : ScrollView(activity) {
            override fun canScrollVertically(direction: Int): Boolean = direction < 0 || !atBottom
            override fun onTouchEvent(event: MotionEvent): Boolean {
                super.onTouchEvent(event)
                return true
            }
        }.apply {
            addView(View(activity), FrameLayout.LayoutParams(400, 5000))
        }
        gate.addView(scroll, FrameLayout.LayoutParams(400, 600))
        parent.addView(gate, FrameLayout.LayoutParams(400, 600))
        activity.setContentView(parent)
        activity.window.decorView.measure(exact(400), exact(600))
        activity.window.decorView.layout(0, 0, 400, 600)
        parent.measure(exact(400), exact(600))
        parent.layout(0, 0, 400, 600)
        scroll.layout(0, 0, 400, 600)
        assertThat(scroll.canScrollVertically(1)).isEqualTo(!atBottom)
        assertThat(scroll.getGlobalVisibleRect(android.graphics.Rect())).isEqualTo(true)
        assertThat(scroll.isShown).isEqualTo(true)
        return parent
    }

    private fun dispatch(parent: View, action: Int, y: Float) {
        val event = MotionEvent.obtain(0, 10, action, 100f, y, 0)
        parent.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private class PagingParent(context: Context) : FrameLayout(context) {
        var interceptedMoves = 0
        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                interceptedMoves++
                return true
            }
            return false
        }
    }
}
