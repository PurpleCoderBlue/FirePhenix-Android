package net.thunderbird.feature.mail.processing.internal.ui

import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingProvider
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingConnectionTester
import net.thunderbird.feature.mail.processing.internal.data.ProcessingException
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import net.thunderbird.feature.mail.processing.internal.data.ProcessingProtocol
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal enum class ProfileField { ENDPOINT, MODEL, KEY, PARAMETERS }

internal class ProfileValidationException(val field: ProfileField, val failure: ProcessingFailure) : Exception()

internal data class ProcessingSettingsState(
    val configuration: ProcessingConfiguration = ProcessingConfiguration(),
    val profile: ServiceProfile? = null,
    val secret: String = "",
    val parameters: String = "{}",
    val error: ProcessingFailure? = null,
    val cacheCleared: Boolean = false,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val profileField: ProfileField? = null,
    val copiedFromProfileId: String? = null,
    val checkingConnection: Boolean = false,
    val connectionChecked: Boolean = false,
    val testingModel: Boolean = false,
    val modelTestOutput: String? = null,
)

internal sealed interface ProcessingSettingsEvent {
    sealed interface ProfileEvent : ProcessingSettingsEvent
    data class EditProfile(val profile: ServiceProfile) : ProfileEvent
    data class AddProfile(val action: ProcessingAction) : ProfileEvent
    data class DraftProfile(val profile: ServiceProfile) : ProfileEvent
    data class Secret(val value: String) : ProfileEvent
    data class Parameters(val value: String) : ProfileEvent
    data class CopyProfile(val sourceId: String) : ProfileEvent
    data object CheckConnection : ProfileEvent
    data object TestModel : ProfileEvent
    data object SaveProfile : ProfileEvent
    data class DeleteProfile(val id: String) : ProfileEvent
    data class DefaultProfile(val action: ProcessingAction, val id: String) : ProfileEvent
    data object ClearCache : ProcessingSettingsEvent
    data object CloseEditor : ProcessingSettingsEvent
}

// One settings state owner handles explicit profile and diagnostic events.
@Suppress("TooManyFunctions")
internal class ProcessingSettingsViewModel(
    private val storage: ProcessingStorage,
    private val protocol: ProcessingProtocol,
    private val connectionTester: ProcessingConnectionTester? = null,
    private val modelService: ProcessingService? = null,
) : BaseViewModel<ProcessingSettingsState, ProcessingSettingsEvent, Nothing>(
    ProcessingSettingsState(configuration = storage.configuration.value, loadFailed = storage.loadFailed),
) {
    private var connectionJob: Job? = null
    private val mutationMutex = Mutex()
    init {
        viewModelScope.launch {
            storage.configuration.collect { config -> updateState { it.copy(configuration = config) } }
        }
    }

    override fun event(event: ProcessingSettingsEvent) {
        if (event is ProcessingSettingsEvent.ProfileEvent || event == ProcessingSettingsEvent.CloseEditor) {
            connectionJob?.cancel()
            updateState {
                it.copy(
                    checkingConnection = false,
                    connectionChecked = false,
                    testingModel = false,
                    modelTestOutput = null,
                )
            }
        }
        when (event) {
            is ProcessingSettingsEvent.ProfileEvent -> profileEvent(event)

            ProcessingSettingsEvent.ClearCache -> mutate {
                storage.clearCache()
                updateState { it.copy(cacheCleared = true) }
            }

            ProcessingSettingsEvent.CloseEditor -> updateState {
                it.copy(
                    profile = null,
                    secret = "",
                    parameters = "{}",
                    error = null,
                    profileField = null,
                    copiedFromProfileId = null,
                )
            }
        }
    }

    private fun profileEvent(event: ProcessingSettingsEvent.ProfileEvent) {
        when (event) {
            is ProcessingSettingsEvent.EditProfile -> edit(event.profile)

            is ProcessingSettingsEvent.AddProfile -> edit(
                ServiceProfile(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    action = event.action,
                ),
            )

            is ProcessingSettingsEvent.DraftProfile -> updateState {
                val sameDestination =
                    it.profile?.let { before ->
                        before.provider == event.profile.provider &&
                            before.endpoint == event.profile.endpoint
                    } ==
                        true
                it.copy(
                    profile = event.profile,
                    error = null,
                    profileField = null,
                    copiedFromProfileId = it.copiedFromProfileId.takeIf {
                        sameDestination
                    },
                )
            }

            is ProcessingSettingsEvent.Secret -> updateState {
                it.copy(secret = event.value, error = null, profileField = null)
            }

            is ProcessingSettingsEvent.Parameters -> updateState {
                it.copy(parameters = event.value, error = null, profileField = null)
            }

            is ProcessingSettingsEvent.CopyProfile -> copyProfile(event.sourceId)

            ProcessingSettingsEvent.CheckConnection -> checkConnection()

            ProcessingSettingsEvent.TestModel -> testModel()

            ProcessingSettingsEvent.SaveProfile -> saveProfile()

            is ProcessingSettingsEvent.DeleteProfile -> mutate { storage.removeProfile(event.id) }

            is ProcessingSettingsEvent.DefaultProfile -> mutate {
                val config = storage.configuration.value
                storage.saveConfiguration(
                    if (event.action == ProcessingAction.TRANSLATE) {
                        config.copy(defaultTranslationProfile = event.id)
                    } else {
                        config.copy(defaultSummaryProfile = event.id)
                    },
                )
            }
        }
    }

    private fun edit(profile: ServiceProfile) {
        val params = kotlinx.serialization.json.buildJsonObject {
            profile.parameters.forEach { (key, value) -> put(key, Json.parseToJsonElement(value)) }
        }.toString()
        updateState {
            it.copy(
                profile = profile,
                secret = "",
                parameters = params,
                error = null,
                profileField = null,
                copiedFromProfileId = null,
            )
        }
    }

    private fun copyProfile(sourceId: String) {
        val draft = state.value.profile ?: return
        val source = storage.credentials(sourceId)?.profile
        val machine = source?.provider in setOf(ProcessingProvider.MICROSOFT, ProcessingProvider.GOOGLE)
        if (source == null || source.action == draft.action || machine) return
        edit(
            source.copy(
                id = draft.id,
                action = draft.action,
                name = draft.name.ifBlank {
                    source.name
                },
                revision = "",
            ),
        )
        // Keep only the source ID in UI state; the secret is copied internally when saving.
        updateState { it.copy(copiedFromProfileId = sourceId) }
    }

    private fun checkConnection() {
        val current = state.value
        val draft = current.profile ?: return
        val tester = connectionTester ?: return
        updateState { it.copy(checkingConnection = true, connectionChecked = false, error = null, profileField = null) }
        connectionJob = viewModelScope.launch {
            try {
                val profile = normalizedProfile(draft, current.parameters)
                validateProfile(profile)
                val secret = withContext(Dispatchers.IO) { profileSecret(profile, current) }
                tester.check(profile, secret)
                updateState { it.copy(connectionChecked = true) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ProfileValidationException) {
                updateState { it.copy(error = exception.failure, profileField = exception.field) }
            } catch (exception: ProcessingException) {
                updateState { it.copy(error = exception.failure) }
            } catch (_: Exception) {
                updateState { it.copy(error = ProcessingFailure.NETWORK) }
            } finally {
                updateState { it.copy(checkingConnection = false) }
            }
        }
    }

    private fun testModel() {
        val current = state.value
        val draft = current.profile ?: return
        val service = modelService ?: return
        updateState { it.copy(testingModel = true, error = null, profileField = null) }
        connectionJob = viewModelScope.launch {
            try {
                val profile = normalizedProfile(draft, current.parameters)
                validateProfile(profile)
                val secret = withContext(Dispatchers.IO) { profileSecret(profile, current) }
                // Fixed public fixture only: this path has no reference to any ProcessingMessage or mail body.
                val text = if (profile.action == ProcessingAction.TRANSLATE) {
                    ProcessingModelTestFixture.translation
                } else {
                    "A meeting is scheduled for tomorrow. No action is required today."
                }
                val result = service.execute(profile, secret, listOf(text), "zh-Hans")
                updateState { it.copy(modelTestOutput = result.single().take(TEST_OUTPUT_LIMIT)) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ProfileValidationException) {
                updateState { it.copy(error = exception.failure, profileField = exception.field) }
            } catch (exception: ProcessingException) {
                updateState { it.copy(error = exception.failure) }
            } catch (_: Exception) {
                updateState { it.copy(error = ProcessingFailure.NETWORK) }
            } finally {
                updateState { it.copy(testingModel = false) }
            }
        }
    }

    private fun saveProfile() {
        val current = state.value
        val draft = current.profile ?: return
        mutate {
            val profile = normalizedProfile(draft, current.parameters)
            validateProfile(profile)
            val secret = profileSecret(profile, current)
            protocol.request(profile, secret, listOf("configuration validation"), "zh-Hans")
            storage.saveProfile(profile, secret)
            val config = storage.configuration.value
            if (profile.action == ProcessingAction.TRANSLATE && config.defaultTranslationProfile.isBlank()) {
                storage.saveConfiguration(config.copy(defaultTranslationProfile = profile.id))
            }
            if (profile.action == ProcessingAction.SUMMARIZE && config.defaultSummaryProfile.isBlank()) {
                storage.saveConfiguration(config.copy(defaultSummaryProfile = profile.id))
            }
            updateState {
                it.copy(
                    profile = null,
                    secret = "",
                    parameters = "{}",
                    loadFailed = false,
                    profileField = null,
                    copiedFromProfileId = null,
                )
            }
        }
    }

    private fun profileSecret(profile: ServiceProfile, current: ProcessingSettingsState): String {
        val saved = storage.credentials(profile.id)
        val copied = current.copiedFromProfileId?.let(storage::credentials)
        val reusable = listOfNotNull(copied, saved).firstOrNull {
            it.profile.provider == profile.provider && it.profile.endpoint == profile.endpoint
        }
        val secret = current.secret.trim().ifBlank { reusable?.secret.orEmpty() }
        if (secret.isBlank()) throw ProfileValidationException(ProfileField.KEY, ProcessingFailure.NOT_CONFIGURED)
        return secret
    }

    private fun mutate(block: () -> Unit) {
        updateState { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { mutationMutex.withLock { block() } }
            } catch (exception: ProfileValidationException) {
                updateState { it.copy(error = exception.failure, profileField = exception.field) }
            } catch (exception: ProcessingException) {
                updateState { it.copy(error = exception.failure) }
            } catch (_: Exception) {
                updateState { it.copy(error = ProcessingFailure.STORAGE) }
            } finally {
                updateState { it.copy(saving = false) }
            }
        }
    }
    private companion object {
        const val TEST_OUTPUT_LIMIT = 500
    }
}
