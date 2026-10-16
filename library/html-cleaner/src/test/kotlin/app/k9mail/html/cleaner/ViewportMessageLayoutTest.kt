package app.k9mail.html.cleaner

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import org.jsoup.Jsoup
import org.junit.Test

class ViewportMessageLayoutTest {
    private val longMail = "<html><body><table><tr><td>" + (1..160).joinToString("") {
        "<p style='font-size:17px;background:#abc'>Paragraph $it " + "visible words ".repeat(30) +
            "<a href='https://example.invalid/link'>link</a><img src='cid:image-$it'></p>"
    } + "</td></tr></table></body></html>"

    @Test
    fun `long Original mail keeps all paragraphs tables links and CID sources while allowing offscreen rendering`() {
        val before = Jsoup.parse(longMail)
        val after = Jsoup.parse(ViewportMessageLayout.apply(longMail))
        assertThat(after.body().text()).isEqualTo(before.body().text())
        assertThat(after.select("table,tr,td,p,a,img").map { it.tagName() })
            .isEqualTo(before.select("table,tr,td,p,a,img").map { it.tagName() })
        assertThat(after.select("a").map { it.attr("href") }).isEqualTo(before.select("a").map { it.attr("href") })
        assertThat(after.select("img").map { it.attr("src") }).isEqualTo(before.select("img").map { it.attr("src") })
        assertThat(after.select("[data-fp-viewport]")).isNotEmpty()
        assertThat(after.select("p").all { "font-size:17px;background:#abc" in it.attr("style") }).isTrue()
        assertThat(after.select("script").isEmpty()).isTrue()
    }

    @Test
    fun `Glass color treatment and viewport optimization both apply without deleting body content`() {
        val after = Jsoup.parse(ViewportMessageLayout.apply(longMail, true))
        assertThat(after.select("[data-fp-viewport]").size).isEqualTo(160)
        assertThat(after.select("p").all { "background-color:transparent!important" in it.attr("style") }).isTrue()
        assertThat(after.body().text()).isEqualTo(Jsoup.parse(longMail).body().text())
    }

    @Test
    fun `compact Original mail is unchanged and repeated viewport optimization is idempotent`() {
        val compact = "<p style='color:red'>Short original message</p>"
        assertThat(ViewportMessageLayout.apply(compact)).isEqualTo(compact)
        val optimized = ViewportMessageLayout.apply(longMail)
        assertThat(ViewportMessageLayout.apply(optimized)).isEqualTo(optimized)
    }
}
