package net.thunderbird.feature.mail.processing.internal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.data.sourceKey
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import net.thunderbird.feature.mail.processing.internal.domain.ProcessingLanguageDetector
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProcessingReaderViewModelTest {
    private val viewModels = mutableListOf<ViewModel>()

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun after() {
        runBlocking { viewModels.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } }
        Dispatchers.resetMain()
    }

    @Test
    fun `translation button translates then toggles the same result without another service call`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        var requests = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                requests++
                return texts.map { "Translated $it" }
            }
        }
        val testSubject = ProcessingReaderViewModel(
            storage,
            MailProcessingManager(storage, service, backgroundScope),
            ProcessingLanguageDetector { "en" },
        )
        viewModels.add(testSubject)
        testSubject.event(
            ProcessingReaderEvent.Open(
                ProcessingMessage("account", "toggle-mail", "inbox", "", "", "<p>Hello</p>", false),
            ),
        )

        // Act / Assert: first click translates in place; the next two only change presentation.
        testSubject.event(ProcessingReaderEvent.ToggleTranslation)
        val translated = testSubject.state.first { it.translation.result != null }
        assertThat(translated.mode).isEqualTo(ReadingMode.TRANSLATED)
        testSubject.event(ProcessingReaderEvent.ToggleTranslation)
        assertThat(testSubject.state.value.mode).isEqualTo(ReadingMode.ORIGINAL)
        testSubject.event(ProcessingReaderEvent.ToggleTranslation)
        assertThat(testSubject.state.value.mode).isEqualTo(ReadingMode.TRANSLATED)
        assertThat(testSubject.state.value.translation.result).isEqualTo(translated.translation.result)
        assertThat(requests).isEqualTo(1)
    }

    @Test
    fun `summary options do not discard a completed translation`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ) = texts
        }
        val testSubject = ProcessingReaderViewModel(
            storage,
            MailProcessingManager(storage, service, backgroundScope),
            ProcessingLanguageDetector { "en" },
        )
        viewModels.add(testSubject)
        testSubject.event(
            ProcessingReaderEvent.Open(
                ProcessingMessage("account", "mail", "inbox", "", "", "<p>Hello</p>", false),
            ),
        )
        testSubject.event(ProcessingReaderEvent.Process(ProcessingAction.TRANSLATE))
        val before = testSubject.state.first { it.translation.result != null }
        // Act
        testSubject.event(ProcessingReaderEvent.Language(ProcessingAction.SUMMARIZE, "ko"))
        testSubject.event(ProcessingReaderEvent.QuotedContext(true))
        // Assert
        assertThat(testSubject.state.value.translation).isEqualTo(before.translation)
        assertThat(testSubject.state.value.mode).isEqualTo(before.mode)
        assertThat(testSubject.state.value.summaryLanguage).isEqualTo("ko")
    }

    @Test
    fun `first configured service becomes available in an already open unconfigured reader`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val configured = storage.configuration.value
        storage.configuration.value = configured.copy(profiles = emptyList())
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ) = texts
        }
        val testSubject = ProcessingReaderViewModel(
            storage,
            MailProcessingManager(storage, service, backgroundScope),
            ProcessingLanguageDetector { "en" },
        )
        viewModels.add(testSubject)
        testSubject.event(
            ProcessingReaderEvent.Open(
                ProcessingMessage("account", "mail", "inbox", "", "", "<p>Hello</p>", false),
            ),
        )
        // Act
        storage.configuration.value = configured
        testSubject.event(ProcessingReaderEvent.ConfigurationChanged(configured))
        // Assert
        assertThat(testSubject.state.value.translationProfile).isEqualTo("service")
        assertThat(testSubject.state.value.translation.result).isNull()
    }

    @Test
    fun `changing a profile invalidates the visible result and restores the original`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ) = texts.map { "Translated $it" }
        }
        val manager = MailProcessingManager(storage, service, backgroundScope)
        val testSubject = ProcessingReaderViewModel(storage, manager, ProcessingLanguageDetector { "en" })
        viewModels.add(testSubject)
        val message =
            ProcessingMessage("account", "mail", "inbox", "", "", "<p>Hello</p>", false, sourceLanguage = "en")
        testSubject.event(ProcessingReaderEvent.Open(message))
        testSubject.event(ProcessingReaderEvent.Process(ProcessingAction.TRANSLATE))
        testSubject.state.first { it.translation.result != null }
        // Act
        val changed = storage.configuration.value.copy(
            profiles = storage.configuration.value.profiles.map { it.copy(revision = "changed") },
        )
        storage.configuration.value = changed
        testSubject.event(ProcessingReaderEvent.ConfigurationChanged(changed))
        // Assert
        assertThat(testSubject.state.value.translation.result).isNull()
        assertThat(testSubject.state.value.mode).isEqualTo(ReadingMode.ORIGINAL)
    }

    @Test
    fun `folder identity and full-message availability are part of the reader source identity`() {
        // Arrange
        val message = ProcessingMessage("account", "same-uid", "inbox", "", "", "<p>Hello</p>", false)
        // Act / Assert
        assertThat(message.sourceKey() == message.copy(folder = "archive").sourceKey()).isEqualTo(false)
        assertThat(message.sourceKey() == message.copy(isComplete = false).sourceKey()).isEqualTo(false)
    }
}
