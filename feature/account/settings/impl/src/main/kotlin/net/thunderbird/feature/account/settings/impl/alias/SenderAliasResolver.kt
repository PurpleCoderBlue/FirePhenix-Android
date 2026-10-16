package net.thunderbird.feature.account.settings.impl.alias

import net.thunderbird.feature.account.settings.alias.AliasRequest
import net.thunderbird.feature.account.settings.alias.AliasResolution

internal class SenderAliasResolver {
    fun resolve(
        request: AliasRequest,
        available: List<String>,
        default: String,
        history: Set<String>,
    ): AliasResolution {
        val aliases = available.associateBy { it.lowercase() }
        return request.explicit?.let {
            decision(listOf(it), available, true) ?: AliasResolution(choices = available)
        }
            ?: decision(request.originalFrom, available, true)
            ?: decision(request.replyRecipients, available)
            ?: decision(history, available)
            ?: aliases[default.lowercase()]?.let { AliasResolution(selected = it) }
            ?: AliasResolution(invalid = default, choices = available)
    }

    private fun decision(
        addresses: Collection<String>,
        available: List<String>,
        rejectMissing: Boolean = false,
    ): AliasResolution? {
        val aliases = available.associateBy { it.lowercase() }
        val nonEmpty = addresses.filter { it.isNotBlank() }
        val matches = nonEmpty.mapNotNull { aliases[it.lowercase()] }.distinct()
        return when {
            nonEmpty.isEmpty() -> null

            rejectMissing && nonEmpty.any { it.lowercase() !in aliases } -> AliasResolution(
                invalid = nonEmpty.first { it.lowercase() !in aliases },
                choices = available,
            )

            matches.isEmpty() -> null

            matches.size == 1 -> AliasResolution(selected = matches.single())

            else -> AliasResolution(choices = matches)
        }
    }
}
