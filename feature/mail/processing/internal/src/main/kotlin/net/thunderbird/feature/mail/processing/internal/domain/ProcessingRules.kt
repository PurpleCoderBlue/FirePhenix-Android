package net.thunderbird.feature.mail.processing.internal.domain

import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ProcessingRule

internal const val AUTOMATIC_CHARACTER_LIMIT = 30_000
internal const val REQUEST_CHARACTER_LIMIT = 4_000

internal fun ProcessingConfiguration.matchingRule(
    message: ProcessingMessage,
    action: ProcessingAction,
    characterCount: Int,
): ProcessingRule? {
    val enabled = when (action) {
        ProcessingAction.TRANSLATE -> automaticTranslation
        ProcessingAction.SUMMARIZE -> automaticSummary
    }
    val unsafeForAutomaticProcessing = message.isSpam || !message.isComplete
    if (!enabled || unsafeForAutomaticProcessing || characterCount > AUTOMATIC_CHARACTER_LIMIT) return null
    return rules.firstOrNull { rule ->
        rule.enabled && rule.action == action &&
            profiles.any { it.id == rule.profileId && it.action == action } && rule.matches(message)
    }
}

internal fun ProcessingRule.matches(message: ProcessingMessage): Boolean {
    val conditions = buildList {
        if (accountId.isNotBlank()) add(accountId == message.accountId)
        if (folder.isNotBlank()) add(folder.equals(message.folder, ignoreCase = true))
        if (sender.isNotBlank()) {
            val expected = sender.trim().lowercase()
            val address = message.sender.trim().lowercase()
            add(if (expected.startsWith("@")) address.endsWith(expected) else address == expected)
        }
        if (subject.isNotBlank()) add(message.subject.contains(subject, ignoreCase = true))
        if (sourceLanguage.isNotBlank()) {
            add(
                message.sourceLanguage?.let {
                    it.equals(sourceLanguage, ignoreCase = true) || it.startsWith("$sourceLanguage-", ignoreCase = true)
                } == true,
            )
        }
    }
    return conditions.isEmpty() || if (matchAll) conditions.all { it } else conditions.any { it }
}

/** Splits without dropping whitespace or dividing a surrogate pair. */
internal fun splitText(text: String, limit: Int = REQUEST_CHARACTER_LIMIT): List<String> {
    require(limit > 0)
    if (text.isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        val count = text.codePointCount(start, text.length).coerceAtMost(limit)
        var end = text.offsetByCodePoints(start, count)
        if (end < text.length) {
            val boundary = text.lastIndexOfAny(charArrayOf('\n', ' ', '。', '.', '!', '?'), end - 1)
            if (boundary >= start + (end - start) / 2) end = boundary + 1
        }
        result += text.substring(start, end)
        start = end
    }
    return result
}
