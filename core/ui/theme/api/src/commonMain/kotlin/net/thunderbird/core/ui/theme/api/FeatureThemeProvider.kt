package net.thunderbird.core.ui.theme.api

import androidx.compose.runtime.Composable

/**
 * Provides the compose theme for a feature.
 */
interface FeatureThemeProvider {
    /** A small native-hosted control samples the wallpaper without painting an opaque wallpaper tile. */
    @Composable
    fun WithControlTheme(content: @Composable () -> Unit) {
        WithTheme(content)
    }

    @Composable
    fun WithContentTheme(content: @Composable () -> Unit) {
        WithTheme(content)
    }

    @Composable
    fun WithTheme(
        content: @Composable () -> Unit,
    )

    @Composable
    fun WithTheme(
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    )
}
