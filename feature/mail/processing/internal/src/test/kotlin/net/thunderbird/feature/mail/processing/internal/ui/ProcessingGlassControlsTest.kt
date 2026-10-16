package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipe
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isSameInstanceAs
import java.util.concurrent.atomic.AtomicReference
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.atom.Slider
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.atom.button.ButtonFilled
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlined
import net.thunderbird.components.ui.bolt.atom.textfield.TextFieldOutlinedSelect
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import org.junit.Test
import org.robolectric.annotation.Config

/** GPU optics are verified on hardware; these tests cover shared input/accessibility behavior. */
@Config(sdk = [28])
class ProcessingGlassControlsTest : ComposeTest() {
    @Test
    fun `a context without a backdrop keeps the incoming modifier unchanged`() {
        val actual = AtomicReference<Modifier>()
        val original = Modifier.testTag("untouched")
        composeTestRule.setContent {
            val decorated = original.liquidGlass(interactive = true)
            SideEffect { actual.set(decorated) }
        }
        composeTestRule.waitForIdle()
        assertThat(actual.get()).isSameInstanceAs(original)
    }

    @Test
    fun `glass controls retain click edit switch and slider semantics`() {
        var clicks = 0
        var progress = 0f
        composeTestRule.setContent {
            var value by remember { mutableStateOf("") }
            var enabled by remember { mutableStateOf(false) }
            ThunderbirdBoltTheme(darkTheme = true) {
                Column {
                    ButtonFilled("Action", { clicks++ })
                    TextFieldOutlined(value, { value = it }, Modifier.testTag("input"))
                    Switch(enabled, { enabled = it }, Modifier.testTag("switch"))
                    Slider(progress, { progress = it }, Modifier.testTag("slider"))
                }
            }
        }
        composeTestRule.onNodeWithText("Action").performClick()
        composeTestRule.onNodeWithTag("input").performTextInput("Existing input")
        composeTestRule.onNodeWithText("Existing input").assertExists()
        composeTestRule.onNodeWithTag("switch").assertIsOff().performClick().assertIsOn()
        composeTestRule.onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(24f) }
        assertThat(clicks).isEqualTo(1)
        assertThat(progress).isEqualTo(24f)
    }

    @Test
    fun `disabled glass switch cannot invoke its callback`() {
        var changes = 0
        composeTestRule.setContent {
            ThunderbirdBoltTheme {
                Switch(false, { changes++ }, Modifier.testTag("disabled"), enabled = false)
            }
        }
        composeTestRule.onNodeWithTag("disabled").performClick().assertIsOff()
        assertThat(changes).isEqualTo(0)
    }

    @Test
    fun `changing light and dark theme preserves remembered form input`() {
        val dark = mutableStateOf(false)
        composeTestRule.setContent {
            ThunderbirdBoltTheme(darkTheme = dark.value) {
                Scaffold { _ ->
                    var text by remember { mutableStateOf("") }
                    TextFieldOutlined(text, { text = it }, Modifier.testTag("preserved-input"))
                }
            }
        }
        composeTestRule.onNodeWithTag("preserved-input").performTextInput("Preserved input")
        composeTestRule.runOnIdle { dark.value = true }
        composeTestRule.onNodeWithText("Preserved input").assertExists()
        composeTestRule.runOnIdle { dark.value = false }
        composeTestRule.onNodeWithText("Preserved input").assertExists()
    }

    @Test
    @Config(sdk = [35])
    fun `focused read only selector survives switching light and dark theme in both directions`() {
        val dark = mutableStateOf(false)
        composeTestRule.setContent {
            ThunderbirdBoltTheme(darkTheme = dark.value) {
                Scaffold { _ ->
                    TextFieldOutlinedSelect(
                        options = persistentListOf(false, true),
                        selectedOption = dark.value,
                        onValueChange = { dark.value = it },
                        modifier = Modifier.testTag("appearance-selector"),
                        optionToStringTransformation = { if (it) "Dark" else "Light" },
                    )
                }
            }
        }
        repeat(2) {
            composeTestRule.onNodeWithTag("appearance-selector").performClick()
            composeTestRule.onNodeWithText("Dark").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Dark").assertExists()
            composeTestRule.onNodeWithTag("appearance-selector").performClick()
            composeTestRule.onNodeWithText("Light").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Light").assertExists()
        }
    }

    @Test
    fun `glass controls retain hardware keyboard input`() {
        var progress = 16f
        composeTestRule.setContent {
            var checked by remember { mutableStateOf(false) }
            ThunderbirdBoltTheme {
                Column {
                    Switch(checked, { checked = it }, Modifier.testTag("keyboard-switch"))
                    Slider(progress, { progress = it }, Modifier.testTag("keyboard-slider"))
                }
            }
        }
        val toggle = composeTestRule.onNodeWithTag("keyboard-switch")
        toggle.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        toggle.performKeyInput { pressKey(Key.Spacebar) }.assertIsOn()
        val slider = composeTestRule.onNodeWithTag("keyboard-slider")
        slider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertThat(progress).isEqualTo(17f)
    }

    @Test
    fun `glass slider handles dragging in both directions on its full touch area`() {
        var progress = 16f
        composeTestRule.setContent {
            var value by remember { mutableFloatStateOf(16f) }
            ThunderbirdBoltTheme {
                Slider(value, {
                    value = it
                    progress = it
                }, Modifier.testTag("touch-slider"))
            }
        }
        val slider = composeTestRule.onNodeWithTag("touch-slider")
        slider.performTouchInput { swipe(center, centerLeft) }
        composeTestRule.waitForIdle()
        assertThat(progress).isLessThan(16f)
        slider.performTouchInput { swipe(centerLeft, centerRight) }
        composeTestRule.waitForIdle()
        assertThat(progress).isGreaterThan(16f)
    }
}
