package net.thunderbird.feature.mail.processing.internal.data

import net.thunderbird.feature.mail.processing.api.ProcessingMessage

internal fun ProcessingMessage.sourceKey(): String = contentHash(
    listOf(accountId, folder, messageId, html, isComplete.toString(), isSpam.toString()).joinToString("\u0000"),
)
