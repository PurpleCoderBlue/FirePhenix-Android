package net.thunderbird.components.ui.bolt.organism

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isTrue
import kotlin.test.Test
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.setContentWithTheme
import net.thunderbird.components.ui.testing.ComposeUiTestHarness

class ModalBottomSheetTest : ComposeUiTestHarness() {
    @Test
    fun `visible sheet choices accept input and can remove the dialog`() = runComposeTest {
        val open = mutableStateOf(true)
        var selected = false
        setContentWithTheme {
            if (open.value) {
                ModalBottomSheet(onDismissRequest = { open.value = false }) {
                    ButtonText(text = "Reply choice", onClick = {
                        selected = true
                        open.value = false
                    })
                }
            }
        }
        onNodeWithText("Reply choice").assertIsDisplayed().performClick()
        assertThat(selected).isTrue()
        onNodeWithText("Reply choice").assertDoesNotExist()
    }

    @Test
    fun `reply choices remain displayed when the dialog caller has no visible content`() = runComposeTest {
        setContent {
            Box(Modifier.fillMaxWidth().height(0.dp)) {
                net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme {
                    ModalBottomSheet(onDismissRequest = {}) {
                        TextTitleSmall("Reply")
                        TextTitleSmall("Reply all")
                    }
                }
            }
        }
        onNodeWithText("Reply").assertIsDisplayed()
        onNodeWithText("Reply all").assertIsDisplayed()
    }
}
