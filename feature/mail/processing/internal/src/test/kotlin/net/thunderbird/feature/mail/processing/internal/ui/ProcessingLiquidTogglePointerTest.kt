package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [28])
class ProcessingLiquidTogglePointerTest : ComposeTest() {
    @Test
    fun `glass switch accepts repeated pointer taps`() {
        composeTestRule.setContent {
            var checked by remember { mutableStateOf(false) }
            ThunderbirdBoltTheme {
                Switch(checked, { checked = it }, Modifier.testTag("switch"))
            }
        }
        val control = composeTestRule.onNodeWithTag("switch")
        control.assertIsOff().performTouchInput { click() }
        composeTestRule.waitForIdle()
        control.assertIsOn().performTouchInput { click() }
        composeTestRule.waitForIdle()
        control.assertIsOff()
    }

    @Test
    fun `disabled glass switch ignores pointer taps`() {
        var changes = 0
        composeTestRule.setContent {
            ThunderbirdBoltTheme {
                Switch(false, { changes++ }, Modifier.testTag("switch"), enabled = false)
            }
        }
        composeTestRule.onNodeWithTag("switch").performTouchInput { click() }.assertIsOff()
        assertThat(changes).isEqualTo(0)
    }

    @Test
    fun `scroll cancellation does not toggle the glass switch`() {
        var changes = 0
        composeTestRule.setContent {
            ThunderbirdBoltTheme {
                Column(Modifier.height(180.dp).verticalScroll(rememberScrollState())) {
                    Switch(false, { changes++ }, Modifier.testTag("switch"))
                    Spacer(Modifier.height(800.dp))
                }
            }
        }
        composeTestRule.onNodeWithTag("switch").performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        assertThat(changes).isEqualTo(0)
    }
}
