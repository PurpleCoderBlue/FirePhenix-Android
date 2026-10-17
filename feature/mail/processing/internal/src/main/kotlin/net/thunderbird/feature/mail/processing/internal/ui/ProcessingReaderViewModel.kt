package net.thunderbird.feature.mail.processing.internal.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.internal.data.ProcessingException
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.data.sourceKey
import net.thunderbird.feature.mail.processing.internal.domain.AUTOMATIC_CHARACTER_LIMIT
import net.thunderbird.feature.mail.processing.internal.domain.HtmlProcessingPlan
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import net.thunderbird.feature.mail.processing.internal.domain.ProcessingLanguageDetector
import net.thunderbird.feature.mail.processing.internal.domain.ProcessingSelection
import net.thunderbird.feature.mail.processing.internal.domain.isChineseLanguage
import net.thunderbird.feature.mail.processing.internal.domain.matchingRule

internal enum class ReadingMode { ORIGINAL, TRANSLATED, BILINGUAL }

internal data class ProcessingActionState(
    val busy: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val result: String? = null,
    val error: ProcessingFailure? = null,
    val cancelled: Boolean = false,
    val outputLanguage: String? = null,
)

internal data class ProcessingReaderState(
    val sourceKey: String = "",
    val translation: ProcessingActionState = ProcessingActionState(),
    val summary: ProcessingActionState = ProcessingActionState(),
    val translationProfile: String = "",
    val summaryProfile: String = "",
    val translationLanguage: String = "zh-Hans",
    val summaryLanguage: String = "zh-Hans",
    val detectedLanguage: String? = null,
    val languageDetectionComplete: Boolean = false,
    val mode: ReadingMode = ReadingMode.ORIGINAL,
    val includeQuotedContext: Boolean = false,
    val summaryExpanded: Boolean = true,
    val pending: Pair<ProcessingSelection, Boolean>? = null,
    val automaticSkipped: Boolean = false,
)

internal sealed interface ProcessingReaderEvent {
    data class Open(val message: ProcessingMessage) : ProcessingReaderEvent
    data class ConfigurationChanged(val configuration: ProcessingConfiguration) : ProcessingReaderEvent
    data class Process(val action: ProcessingAction, val force: Boolean = false) : ProcessingReaderEvent
    data class Profile(val action: ProcessingAction, val id: String) : ProcessingReaderEvent
    data class Mode(val mode: ReadingMode) : ProcessingReaderEvent
    data class Language(val action: ProcessingAction, val language: String) : ProcessingReaderEvent
    data class QuotedContext(val include: Boolean) : ProcessingReaderEvent
    data class Cancel(val action: ProcessingAction) : ProcessingReaderEvent
    data object ConfirmLong : ProcessingReaderEvent
    data object DismissLong : ProcessingReaderEvent
    data object ToggleSummary : ProcessingReaderEvent
    data object ToggleTranslation : ProcessingReaderEvent
    data object QuickSummary : ProcessingReaderEvent
}

internal class ProcessingReaderViewModel(
    private val storage: ProcessingStorage,
    private val manager: MailProcessingManager,
    private val languageDetector: ProcessingLanguageDetector,
) : BaseViewModel<ProcessingReaderState, ProcessingReaderEvent, Nothing>(ProcessingReaderState()) {
    private var message: ProcessingMessage? = null
    private var openedKey: String? = null
    private val keys = mutableMapOf<ProcessingAction, String>()
    private val epochs = mutableMapOf<ProcessingAction, Long>()
    private var profileSnapshot = storage.configuration.value.profiles.associateBy { it.id }

    override fun event(event: ProcessingReaderEvent) {
        when (event) {
            is ProcessingReaderEvent.Open -> open(event.message)

            is ProcessingReaderEvent.ConfigurationChanged -> refresh(event.configuration)

            is ProcessingReaderEvent.Process -> manual(event.action, event.force)

            is ProcessingReaderEvent.Mode -> updateState { it.copy(mode = event.mode) }

            is ProcessingReaderEvent.Language -> changeLanguage(event)

            is ProcessingReaderEvent.Profile -> changeProfile(event)

            is ProcessingReaderEvent.QuotedContext -> updateState { it.withQuotedContext(event.include) }

            is ProcessingReaderEvent.Cancel -> viewModelScope.launch {
                manager.cancel(keys[event.action].orEmpty())
            }

            ProcessingReaderEvent.ConfirmLong -> {
                val pending = state.value.pending
                updateState { it.copy(pending = null) }
                if (pending != null) start(pending.first, pending.second)
            }

            ProcessingReaderEvent.DismissLong -> updateState { it.copy(pending = null) }

            ProcessingReaderEvent.ToggleSummary -> updateState { it.copy(summaryExpanded = !it.summaryExpanded) }

            ProcessingReaderEvent.ToggleTranslation -> quickAction(ProcessingAction.TRANSLATE)

            ProcessingReaderEvent.QuickSummary -> quickAction(ProcessingAction.SUMMARIZE)
        }
    }

    private fun quickAction(action: ProcessingAction) {
        if (action == ProcessingAction.TRANSLATE && state.value.detectedLanguage.isChineseLanguage()) return
        val current = state.value
        val status = if (action == ProcessingAction.TRANSLATE) current.translation else current.summary
        when {
            status.busy -> event(ProcessingReaderEvent.Cancel(action))

            action == ProcessingAction.SUMMARIZE -> {
                if (status.result != null) event(ProcessingReaderEvent.ToggleSummary) else manual(action, force = false)
            }

            current.mode != ReadingMode.ORIGINAL -> updateState { it.copy(mode = ReadingMode.ORIGINAL) }

            status.result != null -> updateState { it.copy(mode = ReadingMode.TRANSLATED) }

            else -> manual(action, force = false)
        }
    }

    private fun changeLanguage(event: ProcessingReaderEvent.Language) {
        if (isBusy(event.action)) return
        updateState {
            if (event.action == ProcessingAction.TRANSLATE) {
                it.copy(
                    translationLanguage = event.language,
                    translation = ProcessingActionState(),
                    mode = ReadingMode.ORIGINAL,
                )
            } else {
                it.copy(summaryLanguage = event.language, summary = ProcessingActionState())
            }
        }
    }

    private fun changeProfile(event: ProcessingReaderEvent.Profile) {
        if (isBusy(event.action)) return
        updateState {
            if (event.action == ProcessingAction.TRANSLATE) {
                it.copy(
                    translationProfile = event.id,
                    translation = ProcessingActionState(),
                    mode = ReadingMode.ORIGINAL,
                )
            } else {
                it.copy(summaryProfile = event.id, summary = ProcessingActionState())
            }
        }
    }

    private fun open(input: ProcessingMessage) {
        val key = input.sourceKey()
        if (openedKey == key) return
        openedKey = key
        message = input
        val config = storage.configuration.value
        updateState {
            ProcessingReaderState(
                sourceKey = key,
                translationProfile = config.defaultTranslationProfile.ifBlank {
                    config.profiles.firstOrNull { it.action == ProcessingAction.TRANSLATE }?.id.orEmpty()
                },
                summaryProfile = config.defaultSummaryProfile.ifBlank {
                    config.profiles.firstOrNull { it.action == ProcessingAction.SUMMARIZE }?.id.orEmpty()
                },
            )
        }
        viewModelScope.launch {
            val plan = withContext(Dispatchers.Default) { HtmlProcessingPlan(input.html) }
            val language = input.sourceLanguage ?: withContext(Dispatchers.Default) {
                languageDetector.detect(plan.texts.joinToString(" "))
            }
            if (openedKey != key) return@launch
            val detected = input.copy(sourceLanguage = language)
            message = detected
            updateState {
                it.copy(
                    detectedLanguage = language,
                    languageDetectionComplete = true,
                    automaticSkipped = plan.characterCount > AUTOMATIC_CHARACTER_LIMIT,
                )
            }
            ProcessingAction.entries.forEach { action ->
                config.matchingRule(detected, action, plan.characterCount)?.let { rule ->
                    start(ProcessingSelection(rule.profileId, action, rule.model, rule.targetLanguage), force = false)
                }
            }
        }
    }

    private fun manual(action: ProcessingAction, force: Boolean) {
        if (isBusy(action) ||
            (action == ProcessingAction.TRANSLATE && state.value.detectedLanguage.isChineseLanguage())
        ) {
            return
        }
        val input = message ?: return
        val current = state.value
        val selectedId = when (action) {
            ProcessingAction.TRANSLATE -> current.translationProfile
            ProcessingAction.SUMMARIZE -> current.summaryProfile
        }
        val config = storage.configuration.value
        val profileId = selectedId.ifBlank {
            (
                when (action) {
                    ProcessingAction.TRANSLATE -> config.defaultTranslationProfile
                    ProcessingAction.SUMMARIZE -> config.defaultSummaryProfile
                }
                )
                .ifBlank { config.profiles.firstOrNull { it.action == action }?.id.orEmpty() }
        }
        val selection = ProcessingSelection(
            profileId = profileId,
            action = action,
            language = if (action ==
                ProcessingAction.TRANSLATE
            ) {
                current.translationLanguage
            } else {
                current.summaryLanguage
            },
            includeQuotedContext = current.includeQuotedContext,
        )
        if (HtmlProcessingPlan(input.html).characterCount > AUTOMATIC_CHARACTER_LIMIT) {
            updateState { it.copy(pending = selection to force) }
        } else {
            start(selection, force)
        }
    }

    private data class RequestToken(
        val source: String?,
        val action: ProcessingAction,
        val epoch: Long,
        val language: String,
    )

    private fun start(selection: ProcessingSelection, force: Boolean) {
        val input = message ?: return
        val action = selection.action
        if (isBusy(action)) return
        updateState {
            if (action == ProcessingAction.TRANSLATE) {
                it.copy(translationProfile = selection.profileId)
            } else {
                it.copy(summaryProfile = selection.profileId)
            }
        }
        val token = RequestToken(openedKey, action, (epochs[action] ?: 0L) + 1, selection.language)
        epochs[action] = token.epoch
        setAction(action, ProcessingActionState(busy = true, outputLanguage = token.language))
        viewModelScope.launch {
            try {
                val result = manager.process(
                    input,
                    selection,
                    force,
                    onStarted = { key ->
                        if (openedKey == token.source && epochs[action] == token.epoch) keys[action] = key
                    },
                ) { completed, total ->
                    setCurrent(token, ProcessingActionState(true, completed, total))
                }
                setCurrent(token, ProcessingActionState(result = result))
            } catch (_: CancellationException) {
                setCurrent(token, ProcessingActionState(cancelled = true))
            } catch (exception: ProcessingException) {
                setCurrent(token, ProcessingActionState(error = exception.failure))
            } catch (_: Exception) {
                setCurrent(token, ProcessingActionState(error = ProcessingFailure.OUTPUT))
            }
        }
    }

    private fun setCurrent(token: RequestToken, status: ProcessingActionState) {
        if (openedKey != token.source || epochs[token.action] != token.epoch) return
        val next = status.copy(outputLanguage = token.language)
        updateState {
            if (token.action == ProcessingAction.TRANSLATE) {
                it.copy(translation = next, mode = if (next.result != null) ReadingMode.TRANSLATED else it.mode)
            } else {
                it.copy(summary = next)
            }
        }
    }

    private fun refresh(configuration: ProcessingConfiguration) {
        val next = configuration.profiles.associateBy { it.id }
        ProcessingAction.entries.forEach { action ->
            val id = if (action == ProcessingAction.TRANSLATE) {
                state.value.translationProfile
            } else {
                state.value.summaryProfile
            }
            if (id.isBlank()) {
                val defaultId = if (action == ProcessingAction.TRANSLATE) {
                    configuration.defaultTranslationProfile
                } else {
                    configuration.defaultSummaryProfile
                }
                val initialId = defaultId.ifBlank {
                    configuration.profiles.firstOrNull { it.action == action }?.id.orEmpty()
                }
                updateState {
                    if (action == ProcessingAction.TRANSLATE) {
                        it.copy(translationProfile = initialId)
                    } else {
                        it.copy(summaryProfile = initialId)
                    }
                }
            }
            if (profileSnapshot[id] != next[id]) {
                epochs[action] = (epochs[action] ?: 0L) + 1
                val previousKey = keys.remove(action)
                viewModelScope.launch { previousKey?.let { manager.cancel(it) } }
                setAction(action, ProcessingActionState())
                if (action == ProcessingAction.TRANSLATE) updateState { it.copy(mode = ReadingMode.ORIGINAL) }
            }
        }
        profileSnapshot = next
    }

    private fun isBusy(action: ProcessingAction): Boolean =
        if (action == ProcessingAction.TRANSLATE) state.value.translation.busy else state.value.summary.busy

    private fun setAction(action: ProcessingAction, status: ProcessingActionState) {
        updateState {
            if (action == ProcessingAction.TRANSLATE) it.copy(translation = status) else it.copy(summary = status)
        }
    }
}

private fun ProcessingReaderState.withQuotedContext(include: Boolean): ProcessingReaderState =
    if (summary.busy) this else copy(includeQuotedContext = include, summary = ProcessingActionState())
