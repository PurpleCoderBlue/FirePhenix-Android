package net.thunderbird.feature.mail.processing.internal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

internal class ProcessingSettingsActivity : ComponentActivity() {
    private val themeProvider: FeatureThemeProvider by inject()
    private val viewModel: ProcessingSettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            themeProvider.WithTheme {
                ProcessingSettingsContent(viewModel, onBack = ::finish)
            }
        }
    }
}
