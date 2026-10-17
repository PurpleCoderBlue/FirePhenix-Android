package net.thunderbird.feature.mail.processing.internal.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.mail.processing.api.SenderListEntry
import net.thunderbird.feature.mail.processing.api.SenderListKind
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.data.updateConfiguration
import net.thunderbird.feature.mail.processing.internal.domain.RemoteImagePolicy
import net.thunderbird.feature.mail.processing.internal.domain.withSenderListsMigration

internal data class RemoteImageSettingsState(
    val rules: List<SenderListEntry> = emptyList(),
    val kind: SenderListKind = SenderListKind.AUTO_IMAGES,
    val draft: SenderListEntry? = null,
    val original: SenderListEntry? = null,
    val invalidSender: Boolean = false,
    val storageFailed: Boolean = false,
)

internal sealed interface RemoteImageSettingsEvent {
    data class Select(val kind: SenderListKind) : RemoteImageSettingsEvent
    data class Edit(val rule: SenderListEntry) : RemoteImageSettingsEvent
    data class Sender(val value: String) : RemoteImageSettingsEvent
    data class Enabled(val rule: SenderListEntry, val enabled: Boolean) : RemoteImageSettingsEvent
    data class Delete(val rule: SenderListEntry) : RemoteImageSettingsEvent
    data object Add : RemoteImageSettingsEvent
    data object Save : RemoteImageSettingsEvent
    data object Close : RemoteImageSettingsEvent
}

internal class RemoteImageSettingsViewModel(private val storage: ProcessingStorage) :
    BaseViewModel<RemoteImageSettingsState, RemoteImageSettingsEvent, Nothing>(
        RemoteImageSettingsState(
            storage.configuration.value.withSenderListsMigration().senderLists,
            storageFailed = storage.loadFailed,
        ),
    ) {
    private var initializedKind = false

    fun initializeKind(kind: SenderListKind) {
        if (initializedKind) return
        initializedKind = true
        event(RemoteImageSettingsEvent.Select(kind))
    }

    init {
        viewModelScope.launch {
            storage.configuration.collect { config ->
                updateState { it.copy(rules = config.withSenderListsMigration().senderLists) }
            }
        }
    }

    override fun event(event: RemoteImageSettingsEvent) {
        when (event) {
            is RemoteImageSettingsEvent.Select -> updateState { it.copy(kind = event.kind, draft = null) }

            is RemoteImageSettingsEvent.Edit -> updateState {
                it.copy(draft = event.rule, original = event.rule, invalidSender = false)
            }

            is RemoteImageSettingsEvent.Sender -> updateState {
                it.copy(draft = it.draft?.copy(sender = event.value), invalidSender = false)
            }

            is RemoteImageSettingsEvent.Enabled -> change { rules ->
                rules.map {
                    if (it.kind == event.rule.kind && it.sender.equals(event.rule.sender, ignoreCase = true)) {
                        it.copy(enabled = event.enabled)
                    } else {
                        it
                    }
                }
            }

            is RemoteImageSettingsEvent.Delete -> change { rules ->
                rules.filterNot { it.kind == event.rule.kind && it.sender.equals(event.rule.sender, ignoreCase = true) }
            }

            RemoteImageSettingsEvent.Add -> updateState {
                it.copy(draft = SenderListEntry("", it.kind), original = null, invalidSender = false)
            }

            RemoteImageSettingsEvent.Save -> save()

            RemoteImageSettingsEvent.Close -> updateState {
                it.copy(draft = null, original = null, invalidSender = false)
            }
        }
    }

    private fun save() {
        val draft = state.value.draft ?: return
        val original = state.value.original
        val sender = RemoteImagePolicy.normalize(draft.sender)
        if (sender == null) {
            updateState { it.copy(invalidSender = true) }
        } else {
            change { rules ->
                rules.filterNot { it == original || (it.kind == draft.kind && it.sender == sender) } +
                    draft.copy(sender = sender)
            }
        }
    }

    private fun change(transform: (List<SenderListEntry>) -> List<SenderListEntry>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                storage.updateConfiguration { config ->
                    config.withSenderListsMigration().let { it.copy(senderLists = transform(it.senderLists)) }
                }
                updateState { it.copy(draft = null, original = null, storageFailed = false) }
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
