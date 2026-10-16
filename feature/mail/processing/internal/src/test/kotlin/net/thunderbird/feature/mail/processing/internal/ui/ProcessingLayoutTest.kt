package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ReaderContentSlot
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.R
import net.thunderbird.feature.mail.processing.internal.data.ProcessingProtocol
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import net.thunderbird.feature.mail.processing.internal.domain.ProcessingLanguageDetector
import org.junit.After
import org.junit.Test
import org.robolectric.annotation.Config

@Config(qualifiers = "w1280dp-h900dp")
class ProcessingLayoutTest : ComposeTest() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reader: ProcessingReaderViewModel? = null
    private var settings: ProcessingSettingsViewModel? = null

    @After
    fun after() {
        reader?.viewModelScope?.cancel()
        settings?.viewModelScope?.cancel()
        scope.cancel()
    }

    @Test
    fun `glass dark scaffold and nested surfaces use readable theme text`() {
        // Arrange
        val expectedColor = AtomicReference<Color>()
        composeTestRule.setContent {
            ThunderbirdBoltTheme(darkTheme = true) {
                val foreground = BoltTheme.colors.onSurface
                SideEffect { expectedColor.set(foreground) }
                Scaffold { padding ->
                    Column(Modifier.padding(padding)) {
                        TextBodyLarge("Scaffold label")
                        Surface { TextBodyLarge("Surface label") }
                    }
                }
            }
        }
        // Act / Assert
        listOf("Scaffold label", "Surface label").forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            composeTestRule.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layouts)
            }
            assertThat(layouts.single().layoutInput.style.color).isEqualTo(expectedColor.get())
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h560dp")
    fun `compact glass reader at large font keeps actions reachable and original mode accessible`() {
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
        val viewModel = ProcessingReaderViewModel(
            storage,
            MailProcessingManager(storage, service, scope),
            ProcessingLanguageDetector { "en" },
        ).also { reader = it }
        val displayed = AtomicReference<String?>()
        val message = ProcessingMessage("account", "mail", "inbox", "", "", "<p>Hello</p>", false)
        composeTestRule.setContent {
            val density = LocalDensity.current
            val configuration by storage.configuration.collectAsState()
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ThunderbirdBoltTheme(darkTheme = true) {
                    Box(Modifier.width(320.dp).height(560.dp)) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            ProcessingReaderContent(
                                message,
                                configuration,
                                viewModel,
                                {},
                                displayed::set,
                                slot = ReaderContentSlot.RESULTS,
                            )
                            ProcessingReaderContent(
                                message,
                                configuration,
                                viewModel,
                                {},
                                displayed::set,
                                slot = ReaderContentSlot.ACTIONS,
                            )
                        }
                    }
                }
            }
        }
        // Act / Assert
        composeTestRule.onNodeWithContentDescription(getString(R.string.processing_options)).performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()
        val quoted = composeTestRule.onNodeWithText(
            getString(R.string.processing_quoted),
            useUnmergedTree = true,
        ).performScrollTo()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        quoted.assertIsDisplayed()
        composeTestRule.onNode(
            hasText(getString(R.string.processing_translate)) and hasAnyAncestor(isDialog()),
        ).performScrollTo().performClick()
        awaitTranslated(displayed, viewModel)
        composeTestRule.onNodeWithText(getString(R.string.processing_bilingual)).performScrollTo().performClick()
        composeTestRule.waitUntil { displayed.get()?.contains("<section>") == true }
        composeTestRule.onNode(
            hasText(getString(R.string.processing_original)) and hasAnyAncestor(isDialog()),
        ).performScrollTo().performClick()
        composeTestRule.waitUntil { displayed.get() == null }
    }

    private fun awaitTranslated(displayed: AtomicReference<String?>, viewModel: ProcessingReaderViewModel) {
        try {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                // Android handler continuations need the Robolectric looper as well as the Compose clock.
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
                displayed.get()?.contains("Translated") == true
            }
        } catch (exception: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Reader state: " + viewModel.state.value.toString(), exception)
        }
    }

    @Test
    fun `wide settings put translation and summary in distinct action columns`() {
        // Arrange
        val viewModel = ProcessingSettingsViewModel(FakeProcessingStorage(), ProcessingProtocol()).also {
            settings = it
        }
        composeTestRule.setContent {
            ThunderbirdBoltTheme(darkTheme = false) {
                Box(Modifier.width(1000.dp).height(700.dp)) { ProcessingSettingsContent(viewModel, {}) }
            }
        }
        // Act / Assert
        val translation = composeTestRule.onNodeWithText(getString(R.string.processing_default_translation))
        val summary = composeTestRule.onNodeWithText(getString(R.string.processing_default_summary))
        translation.assertIsDisplayed()
        summary.assertIsDisplayed()
        assertThat(summary.fetchSemanticsNode().boundsInRoot.left)
            .isGreaterThan(translation.fetchSemanticsNode().boundsInRoot.right)
        assertThat(
            composeTestRule.onNodeWithText(getString(R.string.processing_automatic_translation))
                .fetchSemanticsNode().boundsInRoot.top,
        )
            .isGreaterThan(translation.fetchSemanticsNode().boundsInRoot.bottom)
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun `phone settings stack translation and summary vertically`() {
        val viewModel = ProcessingSettingsViewModel(FakeProcessingStorage(), ProcessingProtocol()).also {
            settings = it
        }
        composeTestRule.setContent {
            ThunderbirdBoltTheme { ProcessingSettingsContent(viewModel, {}) }
        }
        val translation = composeTestRule.onNodeWithText(getString(R.string.processing_default_translation))
            .fetchSemanticsNode().boundsInRoot
        val summary = composeTestRule.onNodeWithText(getString(R.string.processing_default_summary))
            .fetchSemanticsNode().boundsInRoot
        assertThat(summary.left).isEqualTo(translation.left)
        assertThat(summary.top).isGreaterThan(translation.bottom)
    }
}
