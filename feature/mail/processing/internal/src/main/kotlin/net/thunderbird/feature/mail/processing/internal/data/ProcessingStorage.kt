package net.thunderbird.feature.mail.processing.internal.data

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ServiceProfile

internal class ProcessingCredentials(val profile: ServiceProfile, val secret: String)

internal interface ProcessingStorage {
    val configuration: StateFlow<ProcessingConfiguration>
    val loadFailed: Boolean
    val cacheGeneration: Long
    fun saveConfiguration(configuration: ProcessingConfiguration)
    fun saveProfile(profile: ServiceProfile, secret: String)
    fun removeProfile(id: String)
    fun credentials(profileId: String): ProcessingCredentials?
    fun cached(accountId: String, key: String): String?
    fun cache(accountId: String, key: String, content: String, generation: Long)
    fun clearCache(accountId: String? = null)
    fun clearAccount(accountId: String)
}

internal fun ProcessingConfiguration.withoutAccount(accountId: String): ProcessingConfiguration =
    copy(rules = rules.filterNot { it.accountId == accountId && it.accountId.isNotBlank() })
