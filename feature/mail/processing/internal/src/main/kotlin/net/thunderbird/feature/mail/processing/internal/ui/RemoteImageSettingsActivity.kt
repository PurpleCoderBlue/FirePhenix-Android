package net.thunderbird.feature.mail.processing.internal.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

internal class RemoteImageSettingsActivity : ComponentActivity() {
    private val theme: FeatureThemeProvider by inject()
    private val viewModel: RemoteImageSettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent.getStringExtra("senderListKind")?.let { value ->
            net.thunderbird.feature.mail.processing.api.SenderListKind.entries.firstOrNull { it.name == value }?.let {
                viewModel.initializeKind(it)
            }
        }
        setContent { theme.WithTheme { RemoteImageSettingsContent(viewModel, ::finish) } }
    }
}
