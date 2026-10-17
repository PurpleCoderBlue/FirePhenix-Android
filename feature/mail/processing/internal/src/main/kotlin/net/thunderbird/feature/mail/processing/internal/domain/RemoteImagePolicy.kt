package net.thunderbird.feature.mail.processing.internal.domain

import java.util.Locale
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess
import net.thunderbird.feature.mail.processing.api.SenderListKind

/** Only actual From mailboxes qualify; never infer contact trust from display names or Reply-To. */
internal object RemoteImagePolicy {
    private val mailbox = Regex("[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+")

    fun normalize(sender: String): String? = sender.trim().takeIf {
        '*' !in it && mailbox.matches(it)
    }?.lowercase(Locale.ROOT)

    fun access(
        configuration: ProcessingConfiguration,
        senders: List<String>,
        officialSenders: List<OfficialSender> = OfficialSenders.entries,
    ): RemoteImageAccess {
        val config = configuration.withSenderListsMigration()
        val manualBlock = senders.any { config.hasSender(it, SenderListKind.BLOCK_IMAGES) }
        val directoryBlock = senders.mapNotNull(::normalize).any { sender ->
            if (config.hasSender(sender, SenderListKind.AUTO_IMAGES)) {
                false
            } else {
                officialSenders.any { it.imageAccess == RemoteImageAccess.BLOCKED && it.matches(sender) }
            }
        }
        val sender = senders.singleOrNull()?.let(::normalize)
        return when {
            manualBlock || directoryBlock -> RemoteImageAccess.BLOCKED
            sender == null -> RemoteImageAccess.MANUAL
            config.hasSender(sender, SenderListKind.AUTO_IMAGES) -> RemoteImageAccess.AUTOMATIC
            else -> officialSenders.firstOrNull { it.matches(sender) }?.imageAccess ?: RemoteImageAccess.MANUAL
        }
    }
}
