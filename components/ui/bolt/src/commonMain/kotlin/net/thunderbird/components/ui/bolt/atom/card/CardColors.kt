package net.thunderbird.components.ui.bolt.atom.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import androidx.compose.material3.CardColors as Material3CardColors

/**
 * Represents the colors used by a card.
 *
 * See [CardDefaults.cardColors], [CardDefaults.outlinedCardColors], and [CardDefaults.elevatedCardColors].
 *
 * @property containerColor The color used for the background of this card.
 * @property contentColor The preferred color for content inside this card.
 * @property disabledContainerColor The color used for the background of this card when it is not enabled.
 * @property disabledContentColor The preferred color for content inside this card when it is not enabled.
 */
data class CardColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

/**
 * Converts a [Material3CardColors] to a [CardColors].
 */
internal fun Material3CardColors.toCardColors(): CardColors =
    CardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

/**
 * Converts a [CardColors] to a Material 3 [Material3CardColors].
 */
internal fun CardColors.toMaterial3CardColors(): Material3CardColors =
    Material3CardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

/** The optical material owns the backing; Material must not paint an opaque card over it. */
@Composable
internal fun CardColors.forGlass(): CardColors = if (LocalGlassAppearance.current.enabled) {
    copy(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        contentColor = BoltTheme.colors.onSurface,
        disabledContentColor = BoltTheme.colors.onSurface.copy(alpha = CardDefaults.DISABLED_ALPHA),
    )
} else {
    this
}
