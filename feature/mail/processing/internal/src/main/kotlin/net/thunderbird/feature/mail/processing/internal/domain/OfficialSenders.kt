package net.thunderbird.feature.mail.processing.internal.domain

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess

/** An address-directory match does not authenticate a message. */
@Serializable
internal data class OfficialSender(
    val name: String,
    val addresses: Set<String>,
    val domains: Set<String>,
    val source: String,
    val imageAccess: RemoteImageAccess,
) {
    fun matches(sender: String): Boolean = sender in addresses || sender.substringAfter('@') in domains
    val patterns: String get() = (addresses + domains.map { "@$it" }).joinToString(", ")
}

@Serializable
internal data class OfficialSenderCatalogue(
    val schemaVersion: Int,
    val revision: Long,
    val entries: List<OfficialSender>,
)

/** Local JSON shipped with the APK. Directory changes only when the application is updated. */
internal object OfficialSenders {
    private const val MAX_ENTRIES = 500
    private const val MAX_NAME_LENGTH = 80
    private const val MAX_PATTERNS = 100
    private val domain = Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+")
    private val publicDomains =
        setOf("gmail.com", "googlemail.com", "outlook.com", "hotmail.com", "live.com", "yahoo.com")
    val bundled: OfficialSenderCatalogue by lazy {
        val text = requireNotNull(javaClass.getResourceAsStream("/official-senders.json")).bufferedReader().use {
            it.readText()
        }
        decode(text)
    }
    val entries: List<OfficialSender> get() = bundled.entries

    fun decode(text: String): OfficialSenderCatalogue {
        val catalogue = Json.decodeFromString<OfficialSenderCatalogue>(text)
        require(catalogue.schemaVersion == 1 && catalogue.revision > 0)
        require(catalogue.entries.size in 1..MAX_ENTRIES)
        val names = mutableSetOf<String>()
        val patterns = mutableSetOf<String>()
        catalogue.entries.forEach { sender ->
            require(sender.name.isNotBlank() && sender.name.length <= MAX_NAME_LENGTH && names.add(sender.name))
            require(sender.name.none { it.isISOControl() })
            require(sender.addresses.size + sender.domains.size in 1..MAX_PATTERNS)
            require(
                sender.imageAccess == RemoteImageAccess.AUTOMATIC || sender.imageAccess == RemoteImageAccess.BLOCKED,
            )
            val source = requireNotNull(runCatching { URI(sender.source) }.getOrNull())
            require(source.scheme == "https" && !source.host.isNullOrBlank() && source.userInfo == null)
            sender.addresses.forEach {
                require(RemoteImagePolicy.normalize(it) == it && patterns.add(it))
            }
            sender.domains.forEach {
                require(domain.matches(it) && it !in publicDomains && patterns.add("@$it"))
            }
        }
        return catalogue
    }
}
