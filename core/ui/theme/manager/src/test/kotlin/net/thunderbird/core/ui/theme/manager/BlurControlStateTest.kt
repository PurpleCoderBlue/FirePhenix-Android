package net.thunderbird.core.ui.theme.manager

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class BlurControlStateTest {
    @Test
    fun `old preference echoes cannot move a dragging or newly committed thumb`() {
        val testSubject = BlurControlState(100, 512)
        testSubject.drag(30)
        testSubject.external(90)
        assertThat(testSubject.value).isEqualTo(30)
        testSubject.drag(2)
        assertThat(testSubject.finish()).isEqualTo(2)
        testSubject.external(50)
        assertThat(testSubject.value).isEqualTo(2)
        testSubject.external(2)
        assertThat(testSubject.value).isEqualTo(2)
    }

    @Test
    fun `a second gesture supersedes the previous final value without replaying intermediate numbers`() {
        val testSubject = BlurControlState(100, 512)
        testSubject.drag(1)
        testSubject.finish()
        testSubject.drag(80)
        testSubject.external(1)
        testSubject.drag(7)
        assertThat(testSubject.finish()).isEqualTo(7)
        testSubject.external(1)
        assertThat(testSubject.value).isEqualTo(7)
        testSubject.external(7)
        assertThat(testSubject.input).isEqualTo("7")
    }
}
