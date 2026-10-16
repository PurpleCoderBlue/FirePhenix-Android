package net.thunderbird.core.preference.display.coreSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayCoreSettingKey(val value: String) {

    FixedMessageViewTheme("fixedMessageViewTheme"),
    MessageViewTheme("messageViewTheme"),
    MessageComposeTheme("messageComposeTheme"),
    AppLanguage("language"),
    SplitViewMode("splitViewMode"),
    Theme("theme"),
    GlassBlur("firephenixGlassBlur"),
    GlassBackground("firephenixGlassBackground"),
    GlassBackgroundImage("firephenixGlassBackgroundImage"),
    GlassBackgroundBlur("firephenixGlassBackgroundBlur"),
    GlassAutomaticColor("firephenixGlassAutomaticColor"),
    GlassThemeColor("firephenixGlassThemeColor"),
}

interface DisplayCoreSettingsPreferenceManager : PreferenceManager<DisplayCoreSettings>
