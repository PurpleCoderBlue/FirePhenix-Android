package net.thunderbird.feature.mail.processing.internal.data

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ServiceProfile

internal fun contentHash(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

@Serializable
private data class StoredProcessingConfiguration(
    val configuration: ProcessingConfiguration = ProcessingConfiguration(),
    val secrets: Map<String, String> = emptyMap(),
)

internal class EncryptedProcessingStorage(context: Context) : ProcessingStorage {
    private val directory = File(context.noBackupFilesDir, "mail-processing").apply { mkdirs() }
    private val cipher = ProcessingCipher()
    private val configurationFile = File(directory, "configuration.enc")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    override var loadFailed: Boolean = false
        private set
    private val cacheEpoch = java.util.concurrent.atomic.AtomicLong(0)
    override val cacheGeneration: Long get() = cacheEpoch.get()
    private var stored = readConfiguration()
    private val mutableConfiguration = MutableStateFlow(stored.configuration)
    override val configuration = mutableConfiguration.asStateFlow()

    @Synchronized
    override fun saveConfiguration(configuration: ProcessingConfiguration) {
        save(stored.copy(configuration = configuration))
    }

    @Synchronized
    override fun saveProfile(profile: ServiceProfile, secret: String) {
        val updated = profile.copy(revision = UUID.randomUUID().toString())
        val profiles = stored.configuration.profiles.filterNot { it.id == profile.id } + updated
        val secrets = stored.secrets + (profile.id to secret)
        save(stored.copy(configuration = stored.configuration.copy(profiles = profiles), secrets = secrets))
    }

    @Synchronized
    override fun removeProfile(id: String) {
        val config = stored.configuration
        val remaining = config.profiles.filterNot { it.id == id }
        save(
            stored.copy(
                configuration = config.copy(
                    profiles = remaining,
                    automaticTranslation =
                    config.automaticTranslation && remaining.any { it.action == ProcessingAction.TRANSLATE },
                    automaticSummary =
                    config.automaticSummary && remaining.any { it.action == ProcessingAction.SUMMARIZE },
                    defaultTranslationProfile = config.defaultTranslationProfile
                        .takeUnless { it == id }.orEmpty(),
                    defaultSummaryProfile = config.defaultSummaryProfile.takeUnless { it == id }.orEmpty(),
                ),
                secrets = stored.secrets - id,
            ),
        )
        clearCache()
    }

    @Synchronized
    override fun credentials(profileId: String): ProcessingCredentials? =
        stored.configuration.profiles.firstOrNull { it.id == profileId }?.let {
            ProcessingCredentials(it, stored.secrets[profileId].orEmpty())
        }

    @Synchronized
    override fun cached(accountId: String, key: String): String? {
        val file = cacheFile(accountId, key)
        if (!file.exists()) return null
        return try {
            cipher.read(file).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    @Synchronized
    override fun cache(accountId: String, key: String, content: String, generation: Long) {
        if (cacheEpoch.get() != generation) return
        cipher.write(cacheFile(accountId, key), content)
        directory.listFiles { file -> file.name.startsWith("cache_") }
            ?.sortedByDescending { it.lastModified() }?.drop(MAX_CACHE_ENTRIES)?.forEach { it.delete() }
    }

    @Synchronized
    override fun clearCache(accountId: String?) {
        cacheEpoch.incrementAndGet()
        val prefix = if (accountId == null) "cache_" else "cache_${contentHash(accountId)}_"
        directory.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    @Synchronized
    override fun clearAccount(accountId: String) {
        save(stored.copy(configuration = stored.configuration.withoutAccount(accountId)))
        clearCache(accountId)
    }

    private fun cacheFile(
        accountId: String,
        key: String,
    ) = File(
        directory,
        "cache_${contentHash(accountId)}_${contentHash(key)}.enc",
    )

    private fun readConfiguration(): StoredProcessingConfiguration {
        if (!configurationFile.exists()) return StoredProcessingConfiguration()
        return try {
            json.decodeFromString<StoredProcessingConfiguration>(
                cipher.read(configurationFile).toString(Charsets.UTF_8),
            )
        } catch (_: Exception) {
            loadFailed = true
            StoredProcessingConfiguration()
        }
    }

    private fun save(next: StoredProcessingConfiguration) {
        cipher.write(configurationFile, json.encodeToString(next))
        stored = next
        loadFailed = false
        mutableConfiguration.value = next.configuration
    }

    private companion object {
        const val MAX_CACHE_ENTRIES = 100
    }
}
