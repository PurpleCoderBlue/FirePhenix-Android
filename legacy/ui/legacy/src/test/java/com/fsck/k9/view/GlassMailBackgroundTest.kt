package com.fsck.k9.view

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.Test

class GlassMailBackgroundTest {
    @Test
    fun `inline important backgrounds are overridden without changing links or image sources`() {
        val html = "<html style='background:#000!important'><body>" +
            "<table><tr><td style='background-color:#000!important;color:#fff!important'>" +
            "<a href='https://example.invalid'>Hello</a><img src='cid:logo'></td></tr></table></body></html>"
        val result = GlassMailBackground.apply(html, dark = true)
        assertThat(result).contains("background:#000!important;background-color:transparent!important")
        assertThat(result).contains("https://example.invalid")
        assertThat(result).contains("cid:logo")
    }

    @Test
    fun `background override follows existing CSS while preserving links images and message markup`() {
        val source = "<html><head><style>*{background:#121212!important}</style></head>" +
            "<body><table><tr><td><a href=\"https://example.com\">Hello</a>" +
            "<img src=\"cid:logo\"></td></tr></table></body></html>"
        val result = GlassMailBackground.apply(source)
        assertThat(result).contains("background-color:transparent!important;")
        assertThat(result).contains("href=\"https://example.com\"")
        assertThat(result).contains(">Hello</a>")
        assertThat(result).contains("src=\"cid:logo\"")
        assertThat(
            result.lastIndexOf("background-color:transparent") > result.indexOf("background:#121212"),
        ).isEqualTo(true)
    }
}
