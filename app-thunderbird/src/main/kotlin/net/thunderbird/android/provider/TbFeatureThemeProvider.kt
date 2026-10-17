package net.thunderbird.android.provider

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.components.ui.bolt.theme.GlassAccent
import net.thunderbird.components.ui.bolt.theme.GlassBackgroundCoordinates
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.manager.GlassBackgroundStore
import net.thunderbird.core.ui.theme.manager.GlassColors

internal class TbFeatureThemeProvider(private val settings: GeneralSettingsManager) : FeatureThemeProvider {
    @Composable
    override fun WithControlTheme(content: @Composable () -> Unit) {
        val config by settings.getSettingsFlow().collectAsState(settings.getConfig())
        val dark = when (config.display.coreSettings.appTheme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
        RenderTheme(dark, content = content)
    }

    @Composable
    override fun WithContentTheme(content: @Composable () -> Unit) {
        val config by settings.getSettingsFlow().collectAsState(settings.getConfig())
        val dark = when (config.display.coreSettings.appTheme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
        RenderTheme(dark, content = content)
    }

    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        val config by settings.getSettingsFlow().collectAsState(settings.getConfig())
        val dark = when (config.display.coreSettings.appTheme) {
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        }
        WithTheme(dark, content)
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        RenderTheme(darkTheme, content = content)
    }

    @Composable
    private fun RenderTheme(
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        val config by settings.getSettingsFlow().collectAsState(settings.getConfig())
        val display = config.display.coreSettings
        val context = LocalContext.current.applicationContext
        val view = LocalView.current
        val coordinates = remember(view) {
            val location = IntArray(2)
            return@remember {
                view.getLocationInWindow(location)
                GlassBackgroundCoordinates(
                    IntSize(view.rootView.width, view.rootView.height),
                    IntOffset(location[0], location[1]),
                )
            }
        }
        val background = background(display, darkTheme)
        val accent by produceState<GlassAccent?>(
            null,
            display.glassBackground,
            display.glassBackgroundImage,
            display.glassAutomaticColor,
            display.glassThemeColor,
            darkTheme,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    val seed = if (display.glassAutomaticColor) {
                        GlassBackgroundStore.color(context, display.glassBackground, display.glassBackgroundImage)
                    } else {
                        display.glassThemeColor
                    }
                    GlassColors.accent(seed, darkTheme)
                }
        }
        ThunderbirdBoltTheme(
            darkTheme = darkTheme,

            // GlassActivityCallbacks owns the stationary wallpaper beneath every content host.
            glassBackgroundVisible = false,
            glassMotionEnabled = net.thunderbird.core.ui.theme.manager.GlassMotion.enabled(
                LocalContext.current,
                config.display.visualSettings.animationPreference,
            ),
            blur = config.display.coreSettings.glassBlur,
            glassBackground = background?.asImageBitmap(),
            glassBackgroundCoordinates = coordinates,
            glassAccent = accent,
            content = content,
        )
    }

    @Composable
    private fun background(
        display: net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettings,
        darkTheme: Boolean,
    ): Bitmap? {
        val context = LocalContext.current.applicationContext
        val background by produceState<Bitmap?>(
            initialValue = null,
            display.glassBackground,
            display.glassBackgroundImage,
            display.glassBackgroundBlur,
            darkTheme,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    GlassBackgroundStore.load(
                        context,
                        display.glassBackground,
                        display.glassBackgroundImage,
                        darkTheme,
                        display.glassBackgroundBlur,
                    )
                }
        }
        return background
    }
}
