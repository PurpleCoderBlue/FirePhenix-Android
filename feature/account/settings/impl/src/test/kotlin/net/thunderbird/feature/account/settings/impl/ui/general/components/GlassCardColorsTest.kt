package net.thunderbird.feature.account.settings.impl.ui.general.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import app.k9mail.core.ui.compose.testing.ComposeTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.components.ui.bolt.atom.card.CardColors
import net.thunderbird.components.ui.bolt.atom.card.CardDefaults
import net.thunderbird.components.ui.bolt.theme.GlassAppearance
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [28])
class GlassCardColorsTest : ComposeTest() {
    @Test
    fun `dark account and release cards let the glass background through`() = checkColors(dark = true)

    @Test
    fun `light account and release cards let the glass background through`() = checkColors(dark = false)

    private fun checkColors(dark: Boolean) {
        var glassColors = emptyList<CardColors>()
        var plainColors = emptyList<CardColors>()
        composeTestRule.setContent {
            ThunderbirdBoltTheme(darkTheme = dark) {
                val glass = listOf(
                    CardDefaults.cardColors(),
                    CardDefaults.elevatedCardColors(),
                    CardDefaults.outlinedCardColors(),
                )
                CompositionLocalProvider(LocalGlassAppearance provides GlassAppearance(enabled = false)) {
                    val plain = listOf(
                        CardDefaults.cardColors(),
                        CardDefaults.elevatedCardColors(),
                        CardDefaults.outlinedCardColors(),
                    )
                    SideEffect {
                        glassColors = glass
                        plainColors = plain
                    }
                }
            }
        }
        composeTestRule.runOnIdle {
            glassColors.forEach { assertThat(it.containerColor.alpha).isEqualTo(0f) }
            plainColors.forEach { assertThat(it.containerColor.alpha).isEqualTo(1f) }
        }
    }
}
