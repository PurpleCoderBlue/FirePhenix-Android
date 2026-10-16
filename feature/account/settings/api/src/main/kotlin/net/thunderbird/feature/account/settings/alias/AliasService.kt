package net.thunderbird.feature.account.settings.alias

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import net.thunderbird.core.android.account.Identity

data class AliasAccount(val id: String, val label: String)

data class SenderAlias(
    val identity: Identity,
    val isDefault: Boolean,
    val isFavorite: Boolean,
    val isPrimary: Boolean = false,
)

data class AliasRequest(
    val accountId: String,
    val explicit: String?,
    val originalFrom: List<String>,
    val replyRecipients: List<String>,
    val historyRecipients: List<String>,
)

data class AliasResolution(
    val selected: String? = null,
    val choices: List<String> = emptyList(),
    val invalid: String? = null,
) {
    val requiresChoice: Boolean get() = selected == null
}

fun interface AliasResolutionListener {
    fun onResolved(resolution: AliasResolution)
}

/** Aliases are existing account identities; this interface never creates Microsoft account aliases. */
interface AliasService {
    fun accounts(): List<AliasAccount>
    fun aliases(accountId: String): List<SenderAlias>
    fun defaultIdentity(accountId: String): Identity?
    fun openManagement(context: Context, accountId: String)
    fun add(accountId: String, address: String)
    fun remove(accountId: String, address: String)
    fun setDefault(accountId: String, address: String)
    fun favorite(accountId: String, address: String, favorite: Boolean)
    fun clearAccount(accountId: String)
    fun resolve(owner: LifecycleOwner, request: AliasRequest, listener: AliasResolutionListener)
    suspend fun searchSent(accountId: String, recipient: String): Set<String>
}

/** Legacy sent-mail access is adapted in the application wiring layer. */
interface SentAliasHistory {
    fun candidates(accountId: String, recipients: Set<String>): Set<String>
    suspend fun searchRemote(accountId: String, recipient: String)
}
