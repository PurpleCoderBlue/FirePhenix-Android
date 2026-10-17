package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.molecule.input.PasswordInput
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.components.ui.bolt.molecule.input.SwitchInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.AlertDialog
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingProvider
import net.thunderbird.feature.mail.processing.api.SenderListKind
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.R

@Composable
internal fun ProcessingSettingsContent(viewModel: ProcessingSettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::event
    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                stringResource(R.string.processing_title),
                onBackClick = {
                    if (state.profile != null) {
                        dispatch(ProcessingSettingsEvent.CloseEditor)
                    } else {
                        onBack()
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).imePadding()) {
            val wide = maxWidth >= 720.dp && maxHeight >= 600.dp
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.profile == null) {
                    state.error?.let { TextBodyMedium(processingErrorText(it)) }
                }
                if (state.loadFailed) TextBodyMedium(stringResource(R.string.processing_storage_warning))
                val profile = state.profile
                when {
                    profile != null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        ProfileEditor(state, profile, dispatch, Modifier.widthIn(max = 720.dp))
                    }

                    else -> {
                        SettingsOverview(state, wide, dispatch)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsOverview(
    state: ProcessingSettingsState,
    wide: Boolean,
    dispatch: (ProcessingSettingsEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextBodyMedium(stringResource(R.string.processing_unverified))
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ServiceSection(
                    ProcessingAction.TRANSLATE,
                    state.configuration,
                    dispatch,
                    Modifier.weight(1f),
                )
                ServiceSection(
                    ProcessingAction.SUMMARIZE,
                    state.configuration,
                    dispatch,
                    Modifier.weight(1f),
                )
            }
        } else {
            ServiceSection(ProcessingAction.TRANSLATE, state.configuration, dispatch)
            DividerHorizontal()
            ServiceSection(ProcessingAction.SUMMARIZE, state.configuration, dispatch)
        }
        DividerHorizontal()
        ButtonText(stringResource(R.string.processing_clear_cache), {
            dispatch(ProcessingSettingsEvent.ClearCache)
        })
        if (state.cacheCleared) TextBodyMedium(stringResource(R.string.processing_cache_cleared))
    }
}

@Composable
private fun ProfileEditor(
    state: ProcessingSettingsState,
    profile: ServiceProfile,
    dispatch: (ProcessingSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun change(next: ServiceProfile) = dispatch(ProcessingSettingsEvent.DraftProfile(next))
    val traditional = profile.provider in setOf(ProcessingProvider.MICROSOFT, ProcessingProvider.GOOGLE)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileCopySource(state, profile, dispatch)
        TextInput(
            { change(profile.copy(name = it)) },
            text = profile.name,
            label = stringResource(R.string.processing_profile_name),
        )
        ProfileProvider(profile, ::change)
        TextInput(
            { change(profile.copy(endpoint = it)) },
            text = profile.endpoint,
            label = stringResource(R.string.processing_endpoint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
        )
        if (!traditional) {
            TextInput(
                { change(profile.copy(model = it)) },
                text = profile.model,
                label = stringResource(R.string.processing_model),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
            )
        }
        if (profile.provider == ProcessingProvider.MICROSOFT) {
            TextInput(
                { change(profile.copy(region = it)) },
                text = profile.region,
                label = stringResource(R.string.processing_region),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
            )
        }
        PasswordInput(
            { dispatch(ProcessingSettingsEvent.Secret(it)) },
            password = state.secret,
            label = stringResource(R.string.processing_key),
        )
        TextBodyMedium(stringResource(R.string.processing_keep_key))
        if (!traditional) {
            TextInput(
                { dispatch(ProcessingSettingsEvent.Parameters(it)) },
                text = state.parameters,
                label = stringResource(R.string.processing_parameters),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
                isSingleLine = false,
            )
        }
        if (!traditional) ProfileDiagnostics(state, dispatch)
        SaveFeedback(
            state,
            { dispatch(ProcessingSettingsEvent.SaveProfile) },
            enabled = !state.checkingConnection && !state.testingModel,
        )
    }
}

@Composable
private fun ProfileDiagnostics(state: ProcessingSettingsState, dispatch: (ProcessingSettingsEvent) -> Unit) {
    val label = if (state.checkingConnection) {
        R.string.processing_checking
    } else {
        R.string.processing_check_connection
    }
    ButtonText(
        stringResource(label),
        { dispatch(ProcessingSettingsEvent.CheckConnection) },
        enabled = !state.checkingConnection && !state.testingModel && !state.saving,
    )
    if (state.connectionChecked) TextBodyMedium(stringResource(R.string.processing_connection_ok))
    TextBodyMedium(stringResource(R.string.processing_connection_notice))
    TextBodyMedium(stringResource(R.string.processing_model_test_notice))
    ButtonText(
        stringResource(
            if (state.testingModel) R.string.processing_testing_model else R.string.processing_test_model,
        ),
        { dispatch(ProcessingSettingsEvent.TestModel) },
        enabled = !state.checkingConnection && !state.testingModel && !state.saving,
    )
    state.modelTestOutput?.let { TextBodyMedium(stringResource(R.string.processing_model_test_output, it)) }
}

@Composable
private fun ProfileCopySource(
    state: ProcessingSettingsState,
    profile: ServiceProfile,
    dispatch: (ProcessingSettingsEvent) -> Unit,
) {
    Column {
        val sources = state.configuration.profiles.filter {
            it.action != profile.action &&
                it.provider !in setOf(ProcessingProvider.MICROSOFT, ProcessingProvider.GOOGLE)
        }
        if (sources.isNotEmpty()) {
            val none = stringResource(R.string.processing_copy_optional)
            SelectInput<ServiceProfile?>(
                options = sources.toImmutableList(),
                selectedOption = sources.firstOrNull { it.id == state.copiedFromProfileId },
                label = stringResource(
                    if (profile.action ==
                        ProcessingAction.TRANSLATE
                    ) {
                        R.string.processing_copy_summary
                    } else {
                        R.string.processing_copy_translation
                    },
                ),
                optionToStringTransformation = { it?.name ?: none },
                onOptionChange = { it?.let { source -> dispatch(ProcessingSettingsEvent.CopyProfile(source.id)) } },
            )
            if (state.copiedFromProfileId != null) TextBodyMedium(stringResource(R.string.processing_copy_notice))
        }
    }
}

@Composable
private fun SaveFeedback(state: ProcessingSettingsState, onSave: () -> Unit, enabled: Boolean = true) {
    val feedback = remember { BringIntoViewRequester() }
    LaunchedEffect(state.error) { if (state.error != null) feedback.bringIntoView() }
    Column(Modifier.bringIntoViewRequester(feedback), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.error?.let { error ->
            val message = when (state.profileField) {
                ProfileField.ENDPOINT -> stringResource(R.string.processing_invalid_endpoint)
                ProfileField.MODEL -> stringResource(R.string.processing_missing_model)
                ProfileField.KEY -> stringResource(R.string.processing_missing_key)
                ProfileField.PARAMETERS -> stringResource(R.string.processing_invalid_parameters)
                null -> processingErrorText(error)
            }
            TextBodyMedium(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        ButtonFilled(
            stringResource(if (state.saving) R.string.processing_saving else R.string.processing_save),
            onSave,
            enabled = enabled && !state.saving,
        )
    }
}

@Composable
private fun ServiceSection(
    action: ProcessingAction,
    configuration: ProcessingConfiguration,
    dispatch: (ProcessingSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val translation = action == ProcessingAction.TRANSLATE
    val profiles = configuration.profiles.filter { it.action == action }
    val defaultId = if (translation) configuration.defaultTranslationProfile else configuration.defaultSummaryProfile
    var managedProfile by remember { mutableStateOf<ServiceProfile?>(null) }
    val noService = stringResource(R.string.processing_not_configured)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextTitleLarge(stringResource(if (translation) R.string.processing_translate else R.string.processing_summary))
        SelectInput<ServiceProfile?>(
            options = profiles.toImmutableList(),
            selectedOption = profiles.firstOrNull { it.id == defaultId } ?: profiles.firstOrNull(),
            onOptionChange = { profile ->
                profile?.let { dispatch(ProcessingSettingsEvent.DefaultProfile(action, it.id)) }
            },
            optionToStringTransformation = { it?.name ?: noService },
            label = stringResource(
                if (translation) R.string.processing_default_translation else R.string.processing_default_summary,
            ),
            onOptionLongClick = { managedProfile = it },
            optionLongClickLabel = stringResource(R.string.processing_manage_service),
            onAddOption = { dispatch(ProcessingSettingsEvent.AddProfile(action)) },
            addOptionLabel = stringResource(
                if (translation) R.string.processing_add_translation else R.string.processing_add_summary,
            ),
        )
        TextBodyMedium(stringResource(R.string.processing_service_long_press))
        SenderListSettingsButton(translation)
    }
    managedProfile?.let { profile ->
        AlertDialog(
            title = profile.name,
            text = profile.provider.label,
            confirmText = stringResource(R.string.processing_edit),
            onConfirmClick = {
                managedProfile = null
                dispatch(ProcessingSettingsEvent.EditProfile(profile))
            },
            dismissText = stringResource(R.string.processing_delete),
            onDismissClick = {
                managedProfile = null
                dispatch(ProcessingSettingsEvent.DeleteProfile(profile.id))
            },
            onDismissRequest = { managedProfile = null },
        )
    }
}

@Composable
private fun SenderListSettingsButton(translation: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ButtonText(
        stringResource(if (translation) R.string.sender_list_translate else R.string.sender_list_summary),
        {
            context.startActivity(
                android.content.Intent(context, RemoteImageSettingsActivity::class.java).putExtra(
                    "senderListKind",
                    if (translation) {
                        SenderListKind.AUTO_TRANSLATE.name
                    } else {
                        SenderListKind.AUTO_SUMMARIZE.name
                    },
                ),
            )
        },
    )
}

@Composable
private fun ProfileProvider(profile: ServiceProfile, change: (ServiceProfile) -> Unit) {
    val traditional = profile.provider in setOf(ProcessingProvider.MICROSOFT, ProcessingProvider.GOOGLE)
    if (profile.action == ProcessingAction.TRANSLATE) {
        val aiLabel = stringResource(R.string.processing_ai)
        val machineLabel = stringResource(R.string.processing_machine)
        SelectInput(
            options = listOf(false, true).toImmutableList(),
            selectedOption = traditional,
            onOptionChange = {
                val provider = if (it) ProcessingProvider.MICROSOFT else ProcessingProvider.QWEN
                change(profile.copy(provider = provider, endpoint = provider.endpoint, parameters = emptyMap()))
            },
            optionToStringTransformation = { if (it) machineLabel else aiLabel },
            label = stringResource(R.string.processing_service_type),
        )
    }
    val providers = ProcessingProvider.entries.filter {
        val machine = it in setOf(ProcessingProvider.MICROSOFT, ProcessingProvider.GOOGLE)
        if (traditional) machine else !machine
    }
    SelectInput(
        options = providers.toImmutableList(),
        selectedOption = profile.provider,
        onOptionChange = { change(profile.copy(provider = it, endpoint = it.endpoint, parameters = emptyMap())) },
        optionToStringTransformation = { it.label },
        label = stringResource(R.string.processing_provider),
    )
}
