package net.thunderbird.feature.account.settings.impl.alias

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.settings.alias.AliasService
import net.thunderbird.feature.account.settings.alias.SenderAlias

internal data class AliasManagementState(
    val aliases: List<SenderAlias> = emptyList(),
    val accounts: List<net.thunderbird.feature.account.settings.alias.AliasAccount> = emptyList(),
    val address: String = "",
    val candidates: List<String> = emptyList(),
    val busy: Boolean = false,
    val failed: Boolean = false,
)

internal sealed interface AliasManagementEvent {
    data class Address(val value: String) : AliasManagementEvent
    data object Add : AliasManagementEvent
    data class Remove(val address: String) : AliasManagementEvent
    data class Default(val address: String) : AliasManagementEvent
    data class Favorite(val address: String, val favorite: Boolean) : AliasManagementEvent
}

internal class AliasManagementViewModel(
    private val accountId: String,
    private val service: AliasService,
) : BaseViewModel<AliasManagementState, AliasManagementEvent, Nothing>(AliasManagementState()) {
    private var loadedCandidates: Set<String>? = null

    init {
        mutate {}
    }

    override fun event(event: AliasManagementEvent) {
        when (event) {
            is AliasManagementEvent.Address -> updateState { it.copy(address = event.value, failed = false) }

            AliasManagementEvent.Add -> mutate {
                service.add(accountId, state.value.address)
                updateState { it.copy(address = "") }
            }

            is AliasManagementEvent.Remove -> mutate { service.remove(accountId, event.address) }

            is AliasManagementEvent.Default -> mutate { service.setDefault(accountId, event.address) }

            is AliasManagementEvent.Favorite -> mutate { service.favorite(accountId, event.address, event.favorite) }
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        if (state.value.busy) return
        updateState { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                val aliases = withContext(Dispatchers.IO) {
                    if (accountId.isBlank()) emptyList() else service.aliases(accountId)
                }
                val accounts = withContext(Dispatchers.IO) { service.accounts() }
                val candidates = withContext(Dispatchers.IO) {
                    if (accountId.isBlank()) {
                        emptySet()
                    } else {
                        loadedCandidates ?: service.localCandidates(accountId).also { loadedCandidates = it }
                    }
                }.filter { candidate -> aliases.none { it.identity.email.equals(candidate, ignoreCase = true) } }
                    .distinctBy { it.lowercase(java.util.Locale.ROOT) }.sorted()
                updateState { it.copy(aliases = aliases, accounts = accounts, candidates = candidates) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                updateState { it.copy(failed = true) }
            } finally {
                updateState { it.copy(busy = false) }
            }
        }
    }
}
