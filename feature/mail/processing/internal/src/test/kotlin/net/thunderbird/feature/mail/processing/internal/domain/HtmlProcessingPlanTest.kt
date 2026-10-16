package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.jsoup.Jsoup
import org.junit.Test

class HtmlProcessingPlanTest {
    @Test
    fun `Outlook sibling history is excluded by default and only nearest reply is optional`() {
        // Arrange
        val html = "<p>Current request</p><div id='divRplyFwdMsg'>From: previous</div>" +
            "<p>Nearest reply</p><div id='divRplyFwdMsg'>From: oldest</div><p>Ancient history</p>"
        // Act
        val current = HtmlProcessingPlan.summaryText(html, false)
        val context = HtmlProcessingPlan.summaryText(html, true)
        // Assert
        assertThat(current).isEqualTo("Current request")
        assertThat(context.contains("Nearest reply")).isTrue()
        assertThat(context.contains("Ancient history")).isEqualTo(false)
    }

    @Test
    fun `many short HTML nodes respect provider item limits without losing text`() {
        // Arrange
        val html = (1..250).joinToString("") { "<span>$it</span>" }
        val testSubject = HtmlProcessingPlan(html)
        // Act
        val batches = testSubject.batches()
        // Assert
        assertThat(batches.map { it.size }).isEqualTo(listOf(100, 100, 50))
        assertThat(batches.flatten()).isEqualTo(testSubject.texts)
    }

    @Test
    fun `translation preserves tables links and cid images and treats model markup as text`() {
        // Arrange
        val testSubject = HtmlProcessingPlan(
            """<table><tr><td>Hello</td><td><a href="https://example.org">World</a>""" +
                """<img src="cid:photo"></td></tr></table>""",
        )
        // Act
        val result = Jsoup.parse(testSubject.translatedHtml(listOf("你好", "<script>alert(1)</script>")))
        // Assert
        assertThat(result.select("table tr td").size).isEqualTo(2)
        assertThat(result.selectFirst("a")!!.attr("href")).isEqualTo("https://example.org")
        assertThat(result.selectFirst("img")!!.attr("src")).isEqualTo("cid:photo")
        assertThat(result.select("script").isEmpty()).isTrue()
        assertThat(result.selectFirst("a")!!.text()).isEqualTo("<script>alert(1)</script>")
    }

    @Test
    fun `service payload excludes scripts styles and image data`() {
        // Arrange
        val testSubject = HtmlProcessingPlan(
            """<style>body{color:red}</style><p>Hello</p><script>secret()</script>""" +
                """<img src="data:image/png;base64,hidden">""",
        )
        // Act / Assert
        assertThat(testSubject.texts).isEqualTo(listOf("Hello"))
    }

    @Test
    fun `summary excludes quoted history unless explicitly selected`() {
        // Arrange
        val html = "<p>New deadline is Friday.</p><blockquote>Old deadline was Tuesday.</blockquote>"
        // Act
        val result = HtmlProcessingPlan.summaryText(html, includeQuotedContext = false)
        // Assert
        assertThat(result).isEqualTo("New deadline is Friday.")
        assertThat(HtmlProcessingPlan.summaryText(html, true).contains("Tuesday")).isTrue()
    }

    @Test
    fun `explicit conversation context includes only the nearest quote and excludes nested history`() {
        // Arrange
        val html = "<p>Latest</p><blockquote>Relevant context<blockquote>Old private history</blockquote></blockquote>"
        // Act
        val result = HtmlProcessingPlan.summaryText(html, true)
        // Assert
        assertThat(result.contains("Relevant context")).isTrue()
        assertThat(result.contains("Old private history")).isEqualTo(false)
    }
}
