package net.thunderbird.feature.mail.processing.internal.domain

import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.SenderListEntry
import net.thunderbird.feature.mail.processing.api.SenderListKind

/** Version zero is the old rule system. Never widen old rules into permission to send mail to AI. */
internal fun ProcessingConfiguration.withSenderListsMigration(): ProcessingConfiguration =
    if (senderListsVersion >= 1) {
        this
    } else {
        copy(
            senderListsVersion = 1,
            senderLists = emptyList(),
            rules = rules.map { it.copy(enabled = false) },
            remoteImageRules = remoteImageRules.map { it.copy(enabled = false) },
            automaticTranslation = false,
            automaticSummary = false,
        )
    }

internal fun ProcessingConfiguration.hasSender(sender: String, kind: SenderListKind): Boolean {
    val normalized = RemoteImagePolicy.normalize(sender) ?: return false
    return senderLists.any { it.enabled && it.kind == kind && RemoteImagePolicy.normalize(it.sender) == normalized }
}

internal fun ProcessingConfiguration.withSender(
    sender: String,
    kind: SenderListKind,
    enabled: Boolean,
): ProcessingConfiguration {
    val normalized = requireNotNull(RemoteImagePolicy.normalize(sender))
    val current = withSenderListsMigration()
    val remaining = current.senderLists.filterNot {
        it.kind == kind && RemoteImagePolicy.normalize(it.sender) == normalized
    }
    return current.copy(senderLists = if (enabled) remaining + SenderListEntry(normalized, kind) else remaining)
}
