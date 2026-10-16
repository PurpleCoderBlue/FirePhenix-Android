package net.thunderbird.feature.mail.processing.internal.ui

import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.mail.processing.api.RemoteImageRule
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.domain.RemoteImagePolicy

internal data class RemoteImageSettingsState(
    val rules: List<RemoteImageRule> = emptyList(),
    val draft: RemoteImageRule? = null,
    val invalidSender: Boolean = false,
    val storageFailed: Boolean = false,
)

internal sealed interface RemoteImageSettingsEvent {
    data class Edit(val rule: RemoteImageRule) : RemoteImageSettingsEvent
    data class Sender(val value: String) : RemoteImageSettingsEvent
    data class Enabled(val rule: RemoteImageRule, val enabled: Boolean) : RemoteImageSettingsEvent
    data class Delete(val id: String) : RemoteImageSettingsEvent
    data object Add : RemoteImageSettingsEvent
    data object Save : RemoteImageSettingsEvent
    data object Close : RemoteImageSettingsEvent
}

internal class RemoteImageSettingsViewModel(private val storage: ProcessingStorage) :
    BaseViewModel<RemoteImageSettingsState, RemoteImageSettingsEvent, Nothing>(
        RemoteImageSettingsState(storage.configuration.value.remoteImageRules, storageFailed = storage.loadFailed),
    ) {
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            storage.configuration.collect { config -> updateState { it.copy(rules = config.remoteImageRules) } }
        }
    }

    override fun event(event: RemoteImageSettingsEvent) {
        when (event) {
            is RemoteImageSettingsEvent.Edit -> updateState { it.copy(draft = event.rule, invalidSender = false) }

            is RemoteImageSettingsEvent.Sender -> updateState {
                it.copy(draft = it.draft?.copy(sender = event.value), invalidSender = false)
            }

            is RemoteImageSettingsEvent.Enabled -> change { rules ->
                rules.map { if (it.id == event.rule.id) it.copy(enabled = event.enabled) else it }
            }

            is RemoteImageSettingsEvent.Delete -> change { it.filterNot { rule -> rule.id == event.id } }

            RemoteImageSettingsEvent.Add -> updateState {
                it.copy(draft = RemoteImageRule(UUID.randomUUID().toString(), ""), invalidSender = false)
            }

            RemoteImageSettingsEvent.Save -> save()

            RemoteImageSettingsEvent.Close -> updateState { it.copy(draft = null, invalidSender = false) }
        }
    }

    private fun save() {
        val draft = state.value.draft ?: return
        val sender = RemoteImagePolicy.normalize(draft.sender)
        if (sender == null) {
            updateState { it.copy(invalidSender = true) }
        } else {
            change { rules ->
                rules.filterNot { it.id == draft.id || it.sender == sender } + draft.copy(sender = sender)
            }
        }
    }

    private fun change(transform: (List<RemoteImageRule>) -> List<RemoteImageRule>) {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val config = storage.configuration.value
                    storage.saveConfiguration(config.copy(remoteImageRules = transform(config.remoteImageRules)))
                    updateState { it.copy(draft = null, storageFailed = false) }
                } catch (_: java.io.IOException) {
                    updateState { it.copy(storageFailed = true) }
                } catch (_: net.thunderbird.feature.mail.processing.internal.data.ProcessingException) {
                    updateState { it.copy(storageFailed = true) }
                } catch (_: IllegalStateException) {
                    updateState { it.copy(storageFailed = true) }
                }
            }
        }
    }
}
