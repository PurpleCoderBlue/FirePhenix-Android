package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ReaderContentSlot
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import net.thunderbird.feature.mail.processing.internal.domain.ProcessingLanguageDetector
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Config(sdk = [28], qualifiers = "en-w600dp-h900dp")
class ProcessingReaderButtonsTest : ComposeTest() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reader: ProcessingReaderViewModel? = null

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun after() {
        reader?.viewModelScope?.cancel()
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `busy translation uses its button and does not add a status row`() {
        val pending = CompletableDeferred<List<String>>()
        val testSubject = render { _, _ -> pending.await() }
        composeTestRule.onNodeWithText("Translate").performClick()
        composeTestRule.waitUntil { testSubject.state.value.translation.busy }
        composeTestRule.onNodeWithText("Translating…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Processing 0 / 1").assertDoesNotExist()
    }

    @Test
    fun `busy summary stays in its button and clicking it cancels the same request`() {
        val pending = CompletableDeferred<List<String>>()
        val testSubject = render { _, _ -> pending.await() }
        composeTestRule.onNodeWithText("Summarize").performClick()
        composeTestRule.waitUntil { testSubject.state.value.summary.busy }
        composeTestRule.onNodeWithText("Summarizing…").assertIsDisplayed().performClick()
        composeTestRule.waitUntil { testSubject.state.value.summary.cancelled }
        composeTestRule.onNodeWithText("Summarize").assertIsDisplayed()
        composeTestRule.onNodeWithText("Processing 0 / 1").assertDoesNotExist()
    }

    @Test
    fun `completed summary toggles in the original button without generating again`() {
        val requests = AtomicInteger()
        val testSubject = render { _, texts ->
            requests.incrementAndGet()
            texts.map { "Summary result" }
        }
        composeTestRule.onNodeWithText("Summarize").performClick()
        composeTestRule.waitUntil(5_000) {
            testSubject.state.value.summary.result != null || testSubject.state.value.summary.error != null
        }
        assertThat(testSubject.state.value.summary.error).isEqualTo(null)
        composeTestRule.onNodeWithText("Collapse summary").assertDoesNotExist()
        composeTestRule.onNodeWithText("Hide summary").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Summary result").assertDoesNotExist()
        composeTestRule.onNodeWithText("Show summary").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Summary result").assertIsDisplayed()
        assertThat(requests.get()).isEqualTo(1)
    }

    @Test
    fun `Chinese original hides translate but keeps summary available`() {
        val testSubject = render(language = "zh-Hans") { _, texts -> texts }
        composeTestRule.waitUntil { testSubject.state.value.detectedLanguage == "zh-Hans" }
        composeTestRule.onNodeWithText("Translate").assertDoesNotExist()
        composeTestRule.onNodeWithText("Summarize").assertIsDisplayed()
    }

    private fun render(
        language: String = "en",
        execute: suspend (ServiceProfile, List<String>) -> List<String>,
    ): ProcessingReaderViewModel {
        val storage = FakeProcessingStorage()
        storage.configuration.value = storage.configuration.value.copy(
            profiles = storage.configuration.value.profiles + ServiceProfile(
                "summary",
                "Summary",
                ProcessingAction.SUMMARIZE,
                model = "fixture",
            ),
            defaultSummaryProfile = "summary",
        )
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ) =
                execute(profile, texts)
        }
        val testSubject = ProcessingReaderViewModel(
            storage,
            MailProcessingManager(storage, service, scope),
            ProcessingLanguageDetector { language },
        )
        reader = testSubject
        val message = ProcessingMessage("fixture", "buttons", "inbox", "sender@example.org", "", "<p>Hello</p>", false)
        composeTestRule.setContent {
            val configuration by storage.configuration.collectAsState()
            ThunderbirdBoltTheme {
                Column {
                    ReaderContentSlot.entries.filter { it != ReaderContentSlot.INLINE }.forEach { slot ->
                        ProcessingReaderContent(message, configuration, testSubject, {}, {}, slot = slot)
                    }
                }
            }
        }
        composeTestRule.waitUntil { testSubject.state.value.languageDetectionComplete }
        return testSubject
    }
}
