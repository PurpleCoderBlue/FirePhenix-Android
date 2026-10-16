package net.thunderbird.feature.mail.processing.internal.domain

import java.util.Locale
import net.thunderbird.feature.mail.processing.api.RemoteImageRule

/** Only actual From mailboxes qualify; never infer contact trust from display names or Reply-To. */
internal object RemoteImagePolicy {
    private val mailbox = Regex("[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+")

    fun normalize(sender: String): String? = sender.trim().takeIf {
        '*' !in it && mailbox.matches(it)
    }?.lowercase(Locale.ROOT)

    fun allows(rules: List<RemoteImageRule>, senders: List<String>, isOutlookContact: (String) -> Boolean): Boolean {
        val sender = senders.singleOrNull()?.let(::normalize) ?: return false
        return rules.any { it.enabled && normalize(it.sender) == sender } || isOutlookContact(sender)
    }
}
