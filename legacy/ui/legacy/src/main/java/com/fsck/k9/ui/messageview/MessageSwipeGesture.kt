package com.fsck.k9.ui.messageview

import kotlin.math.abs

/** Locks a drag to reading or navigation using scrollability at the beginning of that drag. */
internal class MessageSwipeGesture(private val touchSlop: Int) {
    private var x = 0f
    private var y = 0f
    private var canScrollUp = false
    private var canScrollDown = false
    private var reading: Boolean? = null

    fun start(x: Float, y: Float, canScrollUp: Boolean, canScrollDown: Boolean) {
        this.x = x
        this.y = y
        this.canScrollUp = canScrollUp
        this.canScrollDown = canScrollDown
        reading = null
    }

    fun multiplePointers() { reading = true }

    fun blocksPager(x: Float, y: Float): Boolean {
        reading?.let { return it }
        val dx = this.x - x
        val dy = this.y - y
        return if (maxOf(abs(dx), abs(dy)) <= touchSlop) {
            true
        } else {
            val owns = abs(dx) >= abs(dy) || if (dy > 0) canScrollDown else canScrollUp
            reading = owns
            owns
        }
    }
}
