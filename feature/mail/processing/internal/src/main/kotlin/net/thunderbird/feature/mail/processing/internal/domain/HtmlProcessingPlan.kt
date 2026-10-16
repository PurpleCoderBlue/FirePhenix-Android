package net.thunderbird.feature.mail.processing.internal.domain

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

/** Only text nodes are sent to services; output is assigned as text, never trusted markup. */
internal class HtmlProcessingPlan(html: String) {
    private val document: Document = Jsoup.parse(html).apply { outputSettings().prettyPrint(false) }
    private val nodes: List<TextNode> = document.body().getAllElements()
        .filterNot { it.tagName() in setOf("script", "style", "noscript", "textarea") }
        .flatMap { it.textNodes() }
        .filter { it.wholeText.isNotBlank() }
    private val parts = nodes.map { splitText(it.wholeText) }
    val texts: List<String> = parts.flatten()
    val characterCount: Int = texts.sumOf { it.codePointCount(0, it.length) }

    fun batches(): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var length = 0
        for (text in texts) {
            val count = text.codePointCount(0, text.length)
            if (current.isNotEmpty() && (length + count > REQUEST_CHARACTER_LIMIT || current.size >= MAX_BATCH_ITEMS)) {
                batches += current
                current = mutableListOf()
                length = 0
            }
            current += text
            length += count
        }
        if (current.isNotEmpty()) batches += current
        return batches
    }

    fun translatedHtml(translations: List<String>): String {
        require(translations.size == texts.size) { "Translation item count mismatch" }
        var index = 0
        nodes.forEachIndexed { nodeIndex, node ->
            val count = parts[nodeIndex].size
            node.text(translations.subList(index, index + count).joinToString(""))
            index += count
        }
        return document.outerHtml()
    }

    companion object {
        private const val MAX_BATCH_ITEMS = 100

        fun summaryText(html: String, includeQuotedContext: Boolean): String {
            val document = cleanDocument(html)
            val quote = document.selectFirst("blockquote,.gmail_quote")?.clone()
            val outlookHistory = takeOutlookHistory(document)
            document.select("blockquote,.gmail_quote,.moz-cite-prefix").remove()
            val current = currentText(document)
            val context = if (includeQuotedContext) quotedContext(quote, outlookHistory) else ""
            return if (context.isBlank()) current else current + "\n\n[Referenced conversation]\n" + context
        }

        private fun quotedContext(quote: org.jsoup.nodes.Element?, outlookHistory: Document?): String {
            val contextDocument = quote?.let { cleanDocument(it.outerHtml()) } ?: outlookHistory ?: return ""
            val firstQuote = contextDocument.selectFirst("blockquote,.gmail_quote")
            contextDocument.select("blockquote,.gmail_quote").filter { it !== firstQuote }.forEach { it.remove() }
            takeOutlookHistory(contextDocument)
            return currentText(contextDocument)
        }

        private fun cleanDocument(html: String): Document = Jsoup.parse(html).apply {
            select("script,style,noscript").remove()
        }

        /** Outlook places history after a separator rather than inside a blockquote. */
        private fun takeOutlookHistory(document: Document): Document? {
            val marker = document.selectFirst("#divRplyFwdMsg") ?: return null
            val history = StringBuilder()
            var cursor: org.jsoup.nodes.Node? = marker
            while (cursor != null && cursor !== document.body()) {
                var sibling = cursor.nextSibling()
                while (sibling != null) {
                    val next = sibling.nextSibling()
                    history.append(sibling.outerHtml())
                    sibling.remove()
                    sibling = next
                }
                cursor = cursor.parent()
            }
            marker.remove()
            return cleanDocument(history.toString())
        }

        private fun currentText(document: Document): String = document.body().wholeText().lines().takeWhile {
            val line = it.trim()
            !line.startsWith(">") && !line.matches(Regex("(?i)^On .+wrote:.*")) &&
                !line.contains("-----Original Message-----", ignoreCase = true) &&
                !line.startsWith("发件人：")
        }.joinToString("\n").trim()

        fun bilingualHtml(original: String, translated: String): String {
            val document = Jsoup.parse(original).apply { outputSettings().prettyPrint(false) }
            val originalBody = document.body().html()
            document.body().empty()
            document.body().appendElement("section").html(originalBody)
            document.body().appendElement("hr")
            document.body().appendElement("section").html(Jsoup.parse(translated).body().html())
            return document.outerHtml()
        }
    }
}
