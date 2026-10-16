package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.components.ui.bolt.molecule.input.SwitchInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.AlertDialog
import net.thunderbird.components.ui.bolt.organism.BasicDialog
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ReaderContentSlot
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.R
import net.thunderbird.feature.mail.processing.internal.data.sourceKey
import net.thunderbird.feature.mail.processing.internal.domain.HtmlProcessingPlan

@Composable
internal fun ProcessingReaderContent(
    message: ProcessingMessage,
    configuration: ProcessingConfiguration,
    viewModel: ProcessingReaderViewModel,
    onSettings: () -> Unit,
    onDisplayHtml: (String?) -> Unit,
    modifier: Modifier = Modifier,
    slot: ReaderContentSlot = ReaderContentSlot.INLINE,
) {
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::event
    if (slot == ReaderContentSlot.ACTIONS) {
        ReaderActions(state, configuration, dispatch, onSettings, modifier)
        return
    }
    ReaderEffects(message, configuration, state, dispatch, onDisplayHtml)
    ReaderResults(state, configuration, dispatch, onSettings, modifier, slot)
}

@Composable
private fun ReaderEffects(
    message: ProcessingMessage,
    configuration: ProcessingConfiguration,
    state: ProcessingReaderState,
    dispatch: (ProcessingReaderEvent) -> Unit,
    onDisplayHtml: (String?) -> Unit,
) {
    LaunchedEffect(message) { dispatch(ProcessingReaderEvent.Open(message)) }
    LaunchedEffect(configuration) { dispatch(ProcessingReaderEvent.ConfigurationChanged(configuration)) }
    LaunchedEffect(state.mode, state.translation.result, state.sourceKey, message.html) {
        val translated = state.translation.result.takeIf { state.sourceKey == message.sourceKey() }
        onDisplayHtml(
            when {
                translated == null || state.mode == ReadingMode.ORIGINAL -> null
                state.mode == ReadingMode.BILINGUAL -> HtmlProcessingPlan.bilingualHtml(message.html, translated)
                else -> translated
            },
        )
    }
}

@Composable
private fun ReaderResults(
    state: ProcessingReaderState,
    configuration: ProcessingConfiguration,
    dispatch: (ProcessingReaderEvent) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    slot: ReaderContentSlot = ReaderContentSlot.INLINE,
) {
    val busy = state.translation.busy || state.summary.busy
    val failed = state.translation.error != null || state.summary.error != null
    val hasStatus = busy || failed || state.automaticSkipped
    val visible = slot == ReaderContentSlot.INLINE || state.summary.result != null || hasStatus
    if (visible) {
        Surface(modifier) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (slot == ReaderContentSlot.INLINE) {
                    ReadingToolbar(state, dispatch, onSettings)
                    if (configuration.profiles.isEmpty()) {
                        TextBodyMedium(stringResource(R.string.processing_not_configured))
                    } else {
                        ConfiguredProcessing(state, configuration, dispatch)
                    }
                }
                if (slot == ReaderContentSlot.RESULTS) {
                    ActionProgress(state)
                }
                if (state.automaticSkipped) TextBodyMedium(stringResource(R.string.processing_auto_skipped))
                SummaryResult(state, dispatch)
            }
        }
    }
    if (state.pending != null) {
        AlertDialog(
            title = stringResource(R.string.processing_long_title),
            text = stringResource(R.string.processing_long_text),
            confirmText = stringResource(R.string.processing_confirm),
            onConfirmClick = { dispatch(ProcessingReaderEvent.ConfirmLong) },
            dismissText = stringResource(R.string.processing_cancel),
            onDismissClick = { dispatch(ProcessingReaderEvent.DismissLong) },
            onDismissRequest = { dispatch(ProcessingReaderEvent.DismissLong) },
        )
    }
}

@Composable
private fun ActionProgress(state: ProcessingReaderState) {
    Column {
        ProcessingAction.entries.forEach { action ->
            val status = if (action == ProcessingAction.TRANSLATE) state.translation else state.summary
            if (status.busy) {
                TextBodyMedium(
                    stringResource(R.string.processing_progress, status.completed, status.total),
                )
            }
            status.error?.let { TextBodyMedium(processingErrorText(it)) }
        }
    }
}

@Composable
private fun ReaderActions(
    state: ProcessingReaderState,
    configuration: ProcessingConfiguration,
    dispatch: (ProcessingReaderEvent) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ButtonText(
                stringResource(
                    when {
                        state.translation.busy -> R.string.processing_cancel
                        state.mode != ReadingMode.ORIGINAL -> R.string.processing_original
                        else -> R.string.processing_translate
                    },
                ),
                { dispatch(ProcessingReaderEvent.ToggleTranslation) },
                modifier = Modifier.weight(1f),
            )
            ButtonText(
                stringResource(if (state.summary.busy) R.string.processing_cancel else R.string.processing_summary),
                { dispatch(ProcessingReaderEvent.QuickSummary) },
                modifier = Modifier.weight(1f),
            )
            ButtonIcon(
                onClick = { expanded = true },
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.processing_options),
            )
        }
        if (expanded) {
            BasicDialog(
                headlineText = stringResource(R.string.processing_options),
                onDismissRequest = { expanded = false },
                buttons = {
                    ButtonText(stringResource(R.string.processing_back), { expanded = false })
                },
                modifier = Modifier.widthIn(max = 420.dp),
                content = {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReadingToolbar(state, dispatch, onSettings)
                        if (configuration.profiles.isEmpty()) {
                            TextBodyMedium(stringResource(R.string.processing_not_configured))
                        } else {
                            ConfiguredProcessing(state, configuration, dispatch)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ReadingToolbar(
    state: ProcessingReaderState,
    dispatch: (ProcessingReaderEvent) -> Unit,
    onSettings: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ReadingMode.entries.forEach { mode ->
            val label = stringResource(
                when (mode) {
                    ReadingMode.ORIGINAL -> R.string.processing_original
                    ReadingMode.TRANSLATED -> R.string.processing_translated
                    ReadingMode.BILINGUAL -> R.string.processing_bilingual
                },
            )
            if (state.mode == mode) {
                ButtonFilled(label, { dispatch(ProcessingReaderEvent.Mode(mode)) })
            } else {
                ButtonText(
                    label,
                    { dispatch(ProcessingReaderEvent.Mode(mode)) },
                    enabled = mode == ReadingMode.ORIGINAL || state.translation.result != null,
                )
            }
        }
        ButtonText(stringResource(R.string.processing_title), onSettings)
    }
}

@Composable
private fun ConfiguredProcessing(
    state: ProcessingReaderState,
    configuration: ProcessingConfiguration,
    dispatch: (ProcessingReaderEvent) -> Unit,
) {
    TextBodyMedium(
        stringResource(
            R.string.processing_detected,
            state.detectedLanguage ?: stringResource(R.string.processing_unknown),
        ),
    )
    ProcessingAction.entries.forEach { action ->
        val status = if (action == ProcessingAction.TRANSLATE) state.translation else state.summary
        val selected = if (action == ProcessingAction.TRANSLATE) state.translationProfile else state.summaryProfile
        ActionControls(
            action,
            status,
            selected,
            if (action == ProcessingAction.TRANSLATE) state.translationLanguage else state.summaryLanguage,
            configuration.profiles.filter {
                it.action == action
            }.toImmutableList(),
            dispatch,
        )
    }
    SwitchInput(
        stringResource(R.string.processing_quoted),
        state.includeQuotedContext,
        { dispatch(ProcessingReaderEvent.QuotedContext(it)) },
    )
}

@Composable
private fun ActionControls(
    action: ProcessingAction,
    status: ProcessingActionState,
    selected: String,
    language: String,
    profiles: ImmutableList<ServiceProfile>,
    dispatch: (ProcessingReaderEvent) -> Unit,
) {
    if (profiles.isEmpty()) return
    val profile = profiles.firstOrNull { it.id == selected }
    val missingService = stringResource(R.string.processing_service_removed)
    SelectInput<ServiceProfile?>(
        options = profiles.toImmutableList(),
        selectedOption = profile,
        onOptionChange = {
            it?.let { selectedProfile -> dispatch(ProcessingReaderEvent.Profile(action, selectedProfile.id)) }
        },
        optionToStringTransformation = { it?.name ?: missingService },
        label = stringResource(
            if (action == ProcessingAction.TRANSLATE) {
                R.string.processing_translation_service
            } else {
                R.string.processing_summary_service
            },
        ),
    )
    TextBodyMedium(
        if (profile ==
            null
        ) {
            missingService
        } else {
            stringResource(R.string.processing_destination, profile.endpoint)
        },
    )
    TextInput(
        text = language,
        label = stringResource(R.string.processing_manual_language),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
        onTextChange = { dispatch(ProcessingReaderEvent.Language(action, it)) },
    )
    ProcessingButtons(action, status, profile != null, dispatch)
    status.outputLanguage?.let { TextBodyMedium(stringResource(R.string.processing_result_language, it)) }
    if (status.busy) TextBodyMedium(stringResource(R.string.processing_progress, status.completed, status.total))
    if (status.cancelled) TextBodyMedium(stringResource(R.string.processing_cancelled))
    status.error?.let { TextBodyMedium(processingErrorText(it)) }
}

@Composable
private fun ProcessingButtons(
    action: ProcessingAction,
    status: ProcessingActionState,
    available: Boolean,
    dispatch: (ProcessingReaderEvent) -> Unit,
) {
    FlowRow {
        ButtonText(
            stringResource(
                if (action == ProcessingAction.TRANSLATE) {
                    R.string.processing_translate
                } else {
                    R.string.processing_summary
                },
            ),
            { dispatch(ProcessingReaderEvent.Process(action)) },
            enabled = !status.busy && available,
        )
        if (status.result != null || status.error != null) {
            ButtonText(
                stringResource(R.string.processing_again),
                { dispatch(ProcessingReaderEvent.Process(action, force = true)) },
                enabled = !status.busy && available,
            )
        }
        if (status.busy) {
            ButtonText(
                stringResource(R.string.processing_cancel),
                { dispatch(ProcessingReaderEvent.Cancel(action)) },
            )
        }
    }
}

@Composable
private fun SummaryResult(state: ProcessingReaderState, dispatch: (ProcessingReaderEvent) -> Unit) {
    state.summary.result?.let { summary ->
        ButtonText(
            stringResource(if (state.summaryExpanded) R.string.processing_collapse else R.string.processing_expand),
            { dispatch(ProcessingReaderEvent.ToggleSummary) },
        )
        if (state.summaryExpanded) TextBodyMedium(summary)
    }
}
