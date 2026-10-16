package net.thunderbird.components.ui.bolt.theme

import androidx.compose.ui.graphics.Color

data class GlassAccent(val primary: Color, val onPrimary: Color, val container: Color, val onContainer: Color)

internal fun ThemeColorScheme.withGlassAccent(accent: GlassAccent?): ThemeColorScheme = if (accent ==
    null
) {
    this
} else {
    copy(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.container,
        onPrimaryContainer = accent.onContainer,
        secondary = accent.primary,
        onSecondary = accent.onPrimary,
        secondaryContainer = accent.container,
        onSecondaryContainer = accent.onContainer,
        tertiary = accent.primary,
        onTertiary = accent.onPrimary,
        tertiaryContainer = accent.container,
        onTertiaryContainer = accent.onContainer,
        inversePrimary = accent.container,
    )
}
