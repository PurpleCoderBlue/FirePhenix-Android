package com.fsck.k9.view

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test

class MessageHtmlRendererTest {
    @Test
    fun `glass parsing is dispatched away from the caller before delivering the prepared HTML`() = runTest {
        val worker = StandardTestDispatcher(testScheduler, "HTML worker")
        val received = mutableListOf<String>()
        val testSubject = MessageHtmlRenderer(worker) { html, glass, dark ->
            assertThat(glass).isEqualTo(true)
            assertThat(currentCoroutineContext()[ContinuationInterceptor]).isEqualTo(worker)
            assertThat(dark).isEqualTo(true)
            "prepared:$html"
        }
        testSubject.render(backgroundScope, "mail", true, true, received::add)
        assertThat(received).isEmpty()
        runCurrent()
        assertThat(received).isEqualTo(listOf("prepared:mail"))
    }

    @Test
    fun `an older result cannot overwrite newer content even if parsing ignores cancellation`() = runTest {
        val oldResult = CompletableDeferred<Unit>()
        val received = mutableListOf<String>()
        val testSubject = MessageHtmlRenderer(StandardTestDispatcher(testScheduler)) { html, _, _ ->
            if (html == "old") withContext(NonCancellable) { oldResult.await() }
            "prepared:$html"
        }
        testSubject.render(backgroundScope, "old", true, false, received::add)
        runCurrent()
        testSubject.render(backgroundScope, "new", true, false, received::add)
        runCurrent()
        oldResult.complete(Unit)
        runCurrent()
        assertThat(received).isEqualTo(listOf("prepared:new"))
    }

    @Test
    fun `clearing cancels old work and Original also uses background presentation preparation`() = runTest {
        val received = mutableListOf<String>()
        val testSubject = MessageHtmlRenderer(StandardTestDispatcher(testScheduler)) { html, _, _ -> "prepared:$html" }
        testSubject.render(backgroundScope, "old", true, false, received::add)
        testSubject.render(backgroundScope, "", true, false, received::add)
        testSubject.render(backgroundScope, "Original", false, false, received::add)
        runCurrent()
        assertThat(received).isEqualTo(listOf("", "prepared:Original"))
    }

    @Test
    fun `destroying the renderer suppresses pending delivery`() = runTest {
        val received = mutableListOf<String>()
        val testSubject = MessageHtmlRenderer(StandardTestDispatcher(testScheduler)) { html, _, _ -> html }
        testSubject.render(backgroundScope, "mail", true, false, received::add)
        testSubject.cancel()
        runCurrent()
        assertThat(received).isEmpty()
    }
}
