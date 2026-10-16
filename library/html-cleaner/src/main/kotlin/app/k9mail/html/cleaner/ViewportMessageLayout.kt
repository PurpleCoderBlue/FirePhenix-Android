package app.k9mail.html.cleaner

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Presentation only, after sanitization. All text/links/CIDs remain in the document and no script is added. */
object ViewportMessageLayout {
    fun apply(html: String, glassDark: Boolean? = null): String {
        if (html.isBlank() || (glassDark == null && html.length < LONG_MESSAGE_CHARS)) return html
        val document = Jsoup.parse(html)
        if (glassDark != null) TransparentMessageBackground.apply(document, glassDark)
        if (html.length >= LONG_MESSAGE_CHARS) optimize(document)
        document.outputSettings().prettyPrint(false)
        return document.html()
    }

    private fun optimize(document: Document) {
        if (document.getElementById(STYLE_ID) != null) return
        // Leaf blocks avoid splitting tables, lists or authored inline markup into invalid fragments.
        val blocks = document.body().select(BLOCKS.joinToString(",")).filter { block ->
            block.children().none { it.tagName() in BLOCKS || it.tagName() == "table" } &&
                !UNSAFE_POSITION.containsMatchIn(block.attr("style"))
        }
        blocks.forEach { block ->
            val text = block.text()
            if (text.isNotBlank() && text.length <= MAX_BLOCK_CHARS) {
                val estimate = ((text.length / CHARS_PER_LINE + 1) * LINE_HEIGHT_PX).coerceIn(24, 1200)
                block.attr("data-fp-viewport", "")
                val style = block.attr("style").trimEnd(';')
                block.attr("style", "$style;--fp-viewport-height:${estimate}px")
            }
        }
        document.head().appendElement("style").attr("id", STYLE_ID).appendText(
            "@supports (content-visibility:auto) and (contain-intrinsic-block-size:auto 24px){" +
                "[data-fp-viewport]{content-visibility:auto;" +
                "contain-intrinsic-block-size:auto var(--fp-viewport-height,240px);}}",
        )
        document.select("img").drop(EAGER_IMAGES).forEach { image ->
            if (!image.hasAttr("loading")) image.attr("loading", "lazy")
        }
    }

    private const val LONG_MESSAGE_CHARS = 16_384
    private const val MAX_BLOCK_CHARS = 6000
    private const val CHARS_PER_LINE = 72
    private const val LINE_HEIGHT_PX = 24
    private const val EAGER_IMAGES = 2
    private const val STYLE_ID = "firephenix-viewport-style"
    private val BLOCKS = setOf("p", "div", "section", "article", "blockquote", "pre", "ul", "ol")
    private val UNSAFE_POSITION = Regex(
        """(?:position\s*:\s*(?:absolute|fixed)|float\s*:\s*(?:left|right))""",
        RegexOption.IGNORE_CASE,
    )
}
