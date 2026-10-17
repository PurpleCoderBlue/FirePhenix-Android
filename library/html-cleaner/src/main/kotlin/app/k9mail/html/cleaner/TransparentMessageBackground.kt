package app.k9mail.html.cleaner

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Presentation pass for already sanitized mail. Preserves links, image sources and authored background images. */
object TransparentMessageBackground {
    fun apply(html: String, dark: Boolean): String {
        val document = Jsoup.parse(html)
        apply(document, dark)
        document.outputSettings().prettyPrint(false)
        return document.html()
    }

    internal fun apply(document: Document, dark: Boolean) {
        val foreground = if (dark) "#F3F3F3" else "#1B1B1F"
        val link = if (dark) "#A6D1FF" else "#004B85"
        val style = "html,body,body *{background-color:transparent!important;color:$foreground!important;}" +
            "body a,body a *{color:$link!important;}" +
            "*{scrollbar-width:none!important;}*::-webkit-scrollbar{display:none!important;width:0;height:0;}"
        document.head().appendElement("style").appendText(style)
        // Inline !important declarations outrank any stylesheet. Append after the original declaration.
        document.select("html,body,body *").forEach { element ->
            val color = if (element.closest("a") != null) link else foreground
            val original = element.attr("style").trimEnd(';')
            val prefix = if (original.isEmpty()) "" else "$original;"
            element.attr("style", prefix + "background-color:transparent!important;color:$color!important")
        }
    }
}
