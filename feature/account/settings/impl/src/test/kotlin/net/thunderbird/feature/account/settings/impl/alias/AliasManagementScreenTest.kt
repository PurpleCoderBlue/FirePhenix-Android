package net.thunderbird.feature.account.settings.impl.alias

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [28], qualifiers = "en-w600dp-h900dp")
class AliasManagementScreenTest : ComposeTest() {
    @Test
    fun `one editable dropdown selects a local candidate without automatically adding it`() {
        var state by mutableStateOf(AliasManagementState(candidates = listOf("candidate@example.org")))
        val events = mutableListOf<AliasManagementEvent>()
        setContentWithTheme {
            AliasManagementScreen(state, { event ->
                events += event
                if (event is AliasManagementEvent.Address) state = state.copy(address = event.value)
            }, {})
        }
        composeTestRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        composeTestRule.onNodeWithText("Existing account alias address").performClick()
        composeTestRule.onNodeWithText("candidate@example.org").performClick()
        assertThat(events).isEqualTo(listOf(AliasManagementEvent.Address("candidate@example.org")))
        composeTestRule.onNodeWithText("Search sent mail on server").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add existing alias").performClick()
        assertThat(events.last()).isEqualTo(AliasManagementEvent.Add)
    }
}
