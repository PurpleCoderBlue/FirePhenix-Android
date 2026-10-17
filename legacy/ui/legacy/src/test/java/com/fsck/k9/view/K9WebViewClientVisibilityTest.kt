package com.fsck.k9.view

import android.app.Application
import android.webkit.WebView
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class K9WebViewClientVisibilityTest {
    @Test
    fun `reading can reveal committed content before remote images finish`() {
        var visible = 0
        var finished = 0
        val client = K9WebViewClient(mock(), null) { finished++ }
        client.onPageVisible = { visible++ }
        val view = WebView(RuntimeEnvironment.getApplication())

        client.onPageCommitVisible(view, "about:blank")

        assertThat(visible).isEqualTo(1)
        assertThat(finished).isEqualTo(0)
        client.onPageFinished(view, "about:blank")
        assertThat(finished).isEqualTo(1)
    }

    @Test
    fun `printing and other consumers still wait for complete page loading`() {
        var finished = 0
        val client = K9WebViewClient(mock(), null) { finished++ }
        val view = WebView(RuntimeEnvironment.getApplication())

        client.onPageCommitVisible(view, "about:blank")
        assertThat(finished).isEqualTo(0)
        client.onPageFinished(view, "about:blank")
        assertThat(finished).isEqualTo(1)
    }
}
