package net.thunderbird.components.ui.bolt.theme.thunderbird

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import net.thunderbird.components.ui.bolt.resources.Res
import net.thunderbird.components.ui.bolt.resources.bolt_firephenix_glass
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.GlassAccent
import net.thunderbird.components.ui.bolt.theme.GlassAppearance
import net.thunderbird.components.ui.bolt.theme.GlassBackgroundCoordinates
import net.thunderbird.components.ui.bolt.theme.GlassScene
import net.thunderbird.components.ui.bolt.theme.ThemeColorSchemeVariants
import net.thunderbird.components.ui.bolt.theme.ThemeConfig
import net.thunderbird.components.ui.bolt.theme.ThemeImageVariants
import net.thunderbird.components.ui.bolt.theme.ThemeImages
import net.thunderbird.components.ui.bolt.theme.asGlass
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeElevations
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeShapes
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeSizes
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeSpacings
import net.thunderbird.components.ui.bolt.theme.default.defaultTypography
import net.thunderbird.components.ui.bolt.theme.withGlassAccent

@Composable
fun ThunderbirdBoltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glassMotionEnabled: Boolean = true,
    glassBackgroundVisible: Boolean = true,
    blur: Int = 16,
    glassBackground: ImageBitmap? = null,
    glassBackgroundCoordinates: (() -> GlassBackgroundCoordinates)? = null,
    glassAccent: GlassAccent? = null,
    content: @Composable () -> Unit,
) {
    val images = ThemeImages(
        logo = Res.drawable.bolt_firephenix_glass,
    )

    val themeConfig = ThemeConfig(
        colors = ThemeColorSchemeVariants(
            dark = darkThemeColorScheme.asGlass().withGlassAccent(glassAccent),
            light = lightThemeColorScheme.asGlass().withGlassAccent(glassAccent),
        ),
        elevations = defaultThemeElevations,
        images = ThemeImageVariants(
            light = images,
            dark = images,
        ),
        sizes = defaultThemeSizes,
        spacings = defaultThemeSpacings,
        shapes = defaultThemeShapes,
        typography = defaultTypography,
    )

    BoltTheme(
        themeConfig = themeConfig,
        darkTheme = darkTheme,
        content = {
            GlassScene(
                GlassAppearance(
                    enabled = true,
                    motionEnabled = glassMotionEnabled,
                    backgroundVisible = glassBackgroundVisible,
                    blur = blur,
                    background = glassBackground,
                    backgroundCoordinates = glassBackgroundCoordinates,
                ),
                content = content,
            )
        },
    )
}
