package net.thunderbird.core.ui.theme.api

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Optional appearance entry point shared by onboarding and settings. */
interface AppearanceSettings {
    val title: String
    fun open(context: Context)

    @Composable
    fun Content(compact: Boolean = false, modifier: Modifier = Modifier)
}
