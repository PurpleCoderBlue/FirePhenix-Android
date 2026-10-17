package com.fsck.k9.ui.messageview

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class MessageSwipeGestureTest {
    @Test
    fun `middle of body retains reading even when it later reaches an edge`() {
        val testSubject = MessageSwipeGesture(8)
        testSubject.start(100f, 100f, canScrollUp = true, canScrollDown = true)
        assertThat(testSubject.blocksPager(100f, 50f)).isEqualTo(true)
        assertThat(testSubject.blocksPager(100f, -300f)).isEqualTo(true)
    }

    @Test
    fun `new upward drag at bottom and downward drag at top can page`() {
        val testSubject = MessageSwipeGesture(8)
        testSubject.start(100f, 100f, true, false)
        assertThat(testSubject.blocksPager(100f, 50f)).isEqualTo(false)
        testSubject.start(100f, 100f, false, true)
        assertThat(testSubject.blocksPager(100f, 150f)).isEqualTo(false)
    }

    @Test
    fun `horizontal drag pinches and reading away from an edge never page`() {
        val testSubject = MessageSwipeGesture(8)
        testSubject.start(100f, 100f, false, true)
        assertThat(testSubject.blocksPager(50f, 100f)).isEqualTo(true)
        testSubject.start(100f, 100f, false, true)
        assertThat(testSubject.blocksPager(100f, 50f)).isEqualTo(true)
        testSubject.start(100f, 100f, false, false)
        testSubject.multiplePointers()
        assertThat(testSubject.blocksPager(100f, 50f)).isEqualTo(true)
    }
}
