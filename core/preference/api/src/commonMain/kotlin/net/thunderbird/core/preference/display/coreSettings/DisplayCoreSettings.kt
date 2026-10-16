package net.thunderbird.core.preference.display.coreSettings

import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.SplitViewMode
import net.thunderbird.core.preference.SubTheme

const val DISPLAY_SETTINGS_DEFAULT_APP_LANGUAGE = ""
const val DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME = true
val DISPLAY_SETTINGS_DEFAULT_APP_THEME = AppTheme.FOLLOW_SYSTEM
val DISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME = SubTheme.USE_GLOBAL
val DISPLAY_SETTINGS_DEFAULT_SPLIT_VIEW_MODE = SplitViewMode.AUTOMATIC
val DISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME = SubTheme.USE_GLOBAL

data class DisplayCoreSettings(
    val glassBlur: Int = 16,
    val glassBackground: GlassBackground = GlassBackground.BLUE,
    val glassBackgroundImage: String = "",
    val glassBackgroundBlur: Int = 12,
    val glassAutomaticColor: Boolean = true,
    val glassThemeColor: Int = 0xFF3576A5.toInt(),
    val fixedMessageViewTheme: Boolean = DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME,
    val appTheme: AppTheme = DISPLAY_SETTINGS_DEFAULT_APP_THEME,
    val messageViewTheme: SubTheme = DISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME,
    val messageComposeTheme: SubTheme = DISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME,
    val appLanguage: String = DISPLAY_SETTINGS_DEFAULT_APP_LANGUAGE,
    val splitViewMode: SplitViewMode = DISPLAY_SETTINGS_DEFAULT_SPLIT_VIEW_MODE,
)
