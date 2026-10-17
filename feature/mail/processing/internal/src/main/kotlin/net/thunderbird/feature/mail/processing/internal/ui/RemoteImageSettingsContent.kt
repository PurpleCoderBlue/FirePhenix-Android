package net.thunderbird.feature.mail.processing.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.components.ui.bolt.molecule.input.SwitchInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess
import net.thunderbird.feature.mail.processing.api.SenderListKind
import net.thunderbird.feature.mail.processing.internal.R
import net.thunderbird.feature.mail.processing.internal.domain.OfficialSenders

@Composable
internal fun RemoteImageSettingsContent(viewModel: RemoteImageSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::event
    val back = { if (state.draft == null) onBack() else dispatch(RemoteImageSettingsEvent.Close) }
    BackHandler(onBack = back)
    Scaffold(topBar = { TopAppBarWithBackButton(stringResource(R.string.sender_lists_title), onBackClick = back) }) {
        Column(
            Modifier.fillMaxSize().padding(it).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val labels = SenderListKind.entries.associateWith { kind -> senderListLabel(kind) }
                SelectInput(
                    options = SenderListKind.entries.toImmutableList(),
                    selectedOption = state.kind,
                    onOptionChange = { dispatch(RemoteImageSettingsEvent.Select(it)) },
                    optionToStringTransformation = { kind -> labels.getValue(kind) },
                    label = stringResource(R.string.sender_lists_title),
                )
                TextBodyMedium(stringResource(R.string.sender_lists_policy))
                if (state.storageFailed) TextBodyMedium(stringResource(R.string.processing_storage_warning))
                if (state.draft != null) {
                    RemoteImageEditor(state, dispatch)
                } else {
                    RemoteImageRules(state, dispatch)
                }
            }
        }
    }
}

@Composable
private fun RemoteImageEditor(state: RemoteImageSettingsState, dispatch: (RemoteImageSettingsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextInput(
            { dispatch(RemoteImageSettingsEvent.Sender(it)) },
            text = state.draft?.sender.orEmpty(),
            label = stringResource(R.string.remote_images_sender),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
        )
        if (state.invalidSender) TextBodyMedium(stringResource(R.string.remote_images_invalid_sender))
        ButtonFilled(stringResource(R.string.processing_save), { dispatch(RemoteImageSettingsEvent.Save) })
    }
}

@Composable
private fun RemoteImageRules(state: RemoteImageSettingsState, dispatch: (RemoteImageSettingsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.rules.filter { it.kind == state.kind }.forEach { rule ->
            SwitchInput(rule.sender, rule.enabled, { dispatch(RemoteImageSettingsEvent.Enabled(rule, it)) })
            FlowRow {
                ButtonText(stringResource(R.string.processing_edit), { dispatch(RemoteImageSettingsEvent.Edit(rule)) })
                ButtonText(stringResource(R.string.processing_delete), {
                    dispatch(RemoteImageSettingsEvent.Delete(rule))
                })
            }
            DividerHorizontal()
        }
        ButtonText(stringResource(R.string.sender_list_add), { dispatch(RemoteImageSettingsEvent.Add) })
        if (state.kind == SenderListKind.AUTO_IMAGES || state.kind == SenderListKind.BLOCK_IMAGES) {
            DividerHorizontal()
            TextBodyMedium(stringResource(R.string.sender_lists_builtin))
            val access = if (state.kind == SenderListKind.BLOCK_IMAGES) {
                RemoteImageAccess.BLOCKED
            } else {
                RemoteImageAccess.AUTOMATIC
            }
            OfficialSenders.entries.filter { it.imageAccess == access }.forEach { sender ->
                TextBodyMedium(sender.name)
                TextBodyMedium(sender.patterns)
            }
        }
    }
}

@Composable
private fun senderListLabel(kind: SenderListKind): String = stringResource(
    when (kind) {
        SenderListKind.AUTO_TRANSLATE -> R.string.sender_list_translate
        SenderListKind.AUTO_SUMMARIZE -> R.string.sender_list_summary
        SenderListKind.AUTO_IMAGES -> R.string.sender_list_images
        SenderListKind.BLOCK_IMAGES -> R.string.sender_list_block_images
    },
)
