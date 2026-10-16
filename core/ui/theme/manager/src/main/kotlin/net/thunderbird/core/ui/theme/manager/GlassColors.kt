package net.thunderbird.core.ui.theme.manager

import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.legacy.designsystem.GlassThemePalette
import net.thunderbird.components.ui.bolt.theme.GlassAccent

object GlassColors {
    fun accent(seed: Int, dark: Boolean): GlassAccent = GlassThemePalette.from(seed, dark).let {
        GlassAccent(Color(it.accent), Color(it.onAccent), Color(it.container), Color(it.onContainer))
    }
}
