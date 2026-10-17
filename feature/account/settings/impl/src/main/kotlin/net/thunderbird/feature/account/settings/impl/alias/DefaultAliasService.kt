package net.thunderbird.feature.account.settings.impl.alias

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.account.settings.alias.AliasRequest
import net.thunderbird.feature.account.settings.alias.AliasResolutionListener
import net.thunderbird.feature.account.settings.alias.AliasService
import net.thunderbird.feature.account.settings.alias.SenderAlias
import net.thunderbird.feature.account.settings.alias.SentAliasHistory

internal class DefaultAliasService(
    private val context: Context,
    private val accounts: LegacyAccountDtoManager,
    private val history: SentAliasHistory,
    private val resolver: SenderAliasResolver,
) : AliasService {
    private val preferences = context.getSharedPreferences("firephenix-alias-preferences", Context.MODE_PRIVATE)

    override fun accounts(): List<net.thunderbird.feature.account.settings.alias.AliasAccount> =
        accounts.getAccounts().map { net.thunderbird.feature.account.settings.alias.AliasAccount(it.uuid, it.email) }

    override fun aliases(accountId: String): List<SenderAlias> {
        val account = requireNotNull(accounts.getAccount(accountId))
        val favorites = preferences.getStringSet("$accountId.favorites", emptySet()).orEmpty()
        val default = preferences.getString("$accountId.default", null) ?: account.identities.firstOrNull()?.email
        return account.identities.map { identity ->
            SenderAlias(
                identity,
                identity.email.equals(
                    default,
                    true,
                ),
                identity.email?.lowercase() in favorites,
                identity === account.identities.first(),
            )
        }.sortedByDescending { it.isFavorite }
    }

    override fun defaultIdentity(accountId: String): Identity? {
        val aliases = aliases(accountId)
        return aliases.firstOrNull { it.isDefault }?.identity
    }

    override fun openManagement(context: Context, accountId: String) {
        context.startActivity(Intent(context, AliasManagementActivity::class.java).putExtra("accountId", accountId))
    }

    override fun add(accountId: String, address: String) {
        val normalized = address.trim()
        require(normalized.none { it == '\r' || it == '\n' })
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalized).matches())
        val account = requireNotNull(accounts.getAccount(accountId))
        require(account.identities.none { it.email.equals(normalized, true) })
        val template = account.identities.first()
        account.identities.add(template.copy(email = normalized, description = normalized, replyTo = null))
        accounts.saveAccount(account)
    }

    override fun remove(accountId: String, address: String) {
        val account = requireNotNull(accounts.getAccount(accountId))
        require(account.identities.size > 1)
        val identity = requireNotNull(account.identities.firstOrNull { it.email.equals(address, true) })
        require(identity !== account.identities.first()) { "Primary account identity cannot be removed here" }
        account.identities.remove(identity)
        accounts.saveAccount(account)
        favorite(accountId, address, false)
        if (preferences.getString("$accountId.default", null).equals(address, true)) {
            preferences.edit().remove("$accountId.default").apply()
        }
    }

    override fun setDefault(accountId: String, address: String) {
        require(aliases(accountId).any { it.identity.email.equals(address, true) })
        preferences.edit().putString("$accountId.default", address).apply()
    }

    override fun favorite(accountId: String, address: String, favorite: Boolean) {
        require(aliases(accountId).any { it.identity.email.equals(address, true) } || !favorite)
        val key = "$accountId.favorites"
        val values = preferences.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        if (favorite) values.add(address.lowercase()) else values.remove(address.lowercase())
        preferences.edit().putStringSet(key, values).apply()
    }

    override fun clearAccount(accountId: String) {
        preferences.edit().remove("$accountId.favorites").remove("$accountId.default").apply()
    }

    override fun resolve(owner: LifecycleOwner, request: AliasRequest, listener: AliasResolutionListener) {
        owner.lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    val available = aliases(request.accountId).mapNotNull { it.identity.email }
                    val noPriorSelection = request.explicit == null && request.originalFrom.isEmpty()
                    val newRecipients = request.replyRecipients.isEmpty() && request.historyRecipients.isNotEmpty()
                    val candidates = if (noPriorSelection && newRecipients) {
                        history.candidates(request.accountId, request.historyRecipients.map { it.lowercase() }.toSet())
                    } else {
                        emptySet()
                    }
                    val defaultAddress = defaultIdentity(request.accountId)?.email.orEmpty()
                    resolver.resolve(request, available, defaultAddress, candidates)
                }
            } catch (exception: kotlinx.coroutines.CancellationException) {
                throw exception
            } catch (_: Exception) {
                net.thunderbird.feature.account.settings.alias.AliasResolution(
                    choices = emptyList(),
                )
            }
            listener.onResolved(result)
        }
    }

    override suspend fun localCandidates(accountId: String): Set<String> = withContext(Dispatchers.IO) {
        history.localCandidates(accountId).map { it.trim().lowercase(java.util.Locale.ROOT) }
            .filter { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }.toSet()
    }
}
