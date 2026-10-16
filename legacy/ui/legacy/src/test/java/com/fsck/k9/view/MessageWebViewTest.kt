package com.fsck.k9.view

import android.view.View
import android.widget.FrameLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class MessageWebViewTest : RobolectricTest() {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `reader viewport is capped even when an outer scroll container requests full mail height`() {
        val testSubject = MessageWebView(context).apply { bodyViewportEnabled = true }
        val root = FrameLayout(context).apply {
            addView(testSubject)
            layout(0, 0, 1000, 800)
        }
        testSubject.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50_000, View.MeasureSpec.EXACTLY),
        )
        assertThat(testSubject.measuredHeight).isEqualTo(root.height)
    }

    @Test
    fun `printing and signature previews keep the caller requested height`() {
        val testSubject = MessageWebView(context)
        FrameLayout(context).apply {
            addView(testSubject)
            layout(0, 0, 1000, 800)
        }
        testSubject.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50_000, View.MeasureSpec.EXACTLY),
        )
        assertThat(testSubject.measuredHeight).isEqualTo(50_000)
    }

    @Test
    fun configure_shouldNotUseViewSpecificHardwareLayer() {
        val testSubject = MessageWebView(context).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        try {
            testSubject.configure(
                WebViewConfig(
                    useDarkMode = false,
                    autoFitWidth = false,
                    textZoom = 100,
                ),
            )
        } catch (_: UnsupportedOperationException) {
            // Robolectric's WebView provider can reject the AndroidX dark-mode WebSettings API.
        }

        assertThat(testSubject.layerType).isEqualTo(View.LAYER_TYPE_NONE)
    }
}
