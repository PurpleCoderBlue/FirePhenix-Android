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
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.SwitchInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.feature.mail.processing.internal.R

@Composable
internal fun RemoteImageSettingsContent(viewModel: RemoteImageSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::event
    val back = { if (state.draft == null) onBack() else dispatch(RemoteImageSettingsEvent.Close) }
    BackHandler(onBack = back)
    Scaffold(topBar = { TopAppBarWithBackButton(stringResource(R.string.remote_images_title), onBackClick = back) }) {
        Column(
            Modifier.fillMaxSize().padding(it).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextBodyMedium(stringResource(R.string.remote_images_policy))
                TextBodyMedium(stringResource(R.string.remote_images_contacts_unavailable))
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
        state.rules.forEach { rule ->
            SwitchInput(rule.sender, rule.enabled, { dispatch(RemoteImageSettingsEvent.Enabled(rule, it)) })
            FlowRow {
                ButtonText(stringResource(R.string.processing_edit), { dispatch(RemoteImageSettingsEvent.Edit(rule)) })
                ButtonText(stringResource(R.string.processing_delete), {
                    dispatch(RemoteImageSettingsEvent.Delete(rule.id))
                })
            }
            DividerHorizontal()
        }
        ButtonText(stringResource(R.string.remote_images_add), { dispatch(RemoteImageSettingsEvent.Add) })
    }
}
