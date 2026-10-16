package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.cancel
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.feature.mail.processing.internal.R
import net.thunderbird.feature.mail.processing.internal.data.ProcessingProtocol
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import org.junit.After
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@Config(qualifiers = "w600dp-h800dp")
class ProcessingProfileSaveTest : ComposeTest() {
    private val storage = FakeProcessingStorage()
    private lateinit var testSubject: ProcessingSettingsViewModel

    @After
    fun after() {
        if (::testSubject.isInitialized) testSubject.viewModelScope.cancel()
    }

    @Test
    fun `glass save button persists a valid profile with blank optional parameters`() {
        saveProfile(dark = true)
    }

    @Test
    fun `light save button persists a valid profile with blank optional parameters`() {
        saveProfile(dark = false)
    }

    private fun saveProfile(dark: Boolean) {
        // Arrange: exercise the real form and event chain, keeping network and credentials fictitious.
        testSubject = ProcessingSettingsViewModel(storage, ProcessingProtocol())
        composeTestRule.setContent {
            ThunderbirdBoltTheme(darkTheme = dark) {
                ProcessingSettingsContent(testSubject, {})
            }
        }
        composeTestRule.onNodeWithText(getString(R.string.processing_default_translation))
            .performScrollTo().performTouchInput { click() }
        composeTestRule.onNodeWithText(getString(R.string.processing_add_translation)).performClick()
        replaceField(R.string.processing_profile_name, "Local QA")
        replaceField(R.string.processing_endpoint, "https://example.invalid/v1")
        replaceField(R.string.processing_model, "test-model")
        replaceField(R.string.processing_key, "not-a-real-key")
        replaceField(R.string.processing_parameters, "")
        val id = testSubject.state.value.profile!!.id

        // Act: click the same Save button used on the device, rather than dispatching SaveProfile directly.
        composeTestRule.onNodeWithText(getString(R.string.processing_save)).performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            ShadowLooper.idleMainLooper()
            !testSubject.state.value.saving
        }

        // Assert: success closes the editor and stores the profile and credential independently of the UI.
        assertThat(testSubject.state.value.error).isEqualTo(null)
        assertThat(testSubject.state.value.profile).isEqualTo(null)
        assertThat(storage.credentials(id)?.profile?.model).isEqualTo("test-model")
        assertThat(storage.credentials(id)?.profile?.parameters).isEqualTo(emptyMap())
        assertThat(storage.credentials(id)?.secret).isEqualTo("not-a-real-key")
    }

    private fun replaceField(label: Int, value: String) {
        composeTestRule.onNode(hasSetTextAction() and hasText(getString(label)))
            .performScrollTo().performTextReplacement(value)
    }
}
