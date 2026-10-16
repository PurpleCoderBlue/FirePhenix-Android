package app.k9mail.html.cleaner

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.jsoup.Jsoup
import org.junit.Test

class TransparentMessageBackgroundTest {
    @Test
    fun `specific important styles cannot conceal wallpaper and images links tables remain intact`() {
        val source = "<html><head><style>#mail{background:black!important;color:black!important}</style></head>" +
            "<body><table><tr><td id='mail' style='background:#000!important'>" +
            "<a href='https://example.invalid'><span>Hello</span></a><img src='cid:logo'></td></tr></table></body></html>"
        val testSubject = TransparentMessageBackground
        val document = Jsoup.parse(testSubject.apply(source, true))
        assertThat(document.selectFirst("#mail")!!.attr("style")).contains("background-color:transparent!important")
        assertThat(document.selectFirst("a span")!!.attr("style")).contains("color:#A6D1FF!important")
        assertThat(document.selectFirst("a")!!.attr("href")).isEqualTo("https://example.invalid")
        assertThat(document.selectFirst("img")!!.attr("src")).isEqualTo("cid:logo")
        assertThat(document.select("table tr td").size).isEqualTo(1)
        assertThat(document.body().text()).isEqualTo("Hello")
    }
}
