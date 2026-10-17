package net.thunderbird.core.ui.theme.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.template.Scaffold
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettings
import net.thunderbird.core.preference.update
import net.thunderbird.core.ui.theme.api.AppearanceSettings
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

private const val MAX_BLUR = 32

class DefaultAppearanceSettings(
    private val context: Context,
    private val settings: GeneralSettingsManager,
    private val coreSettings: net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager,
) : AppearanceSettings {
    override val title: String get() = context.getString(R.string.appearance_title)

    override fun open(context: Context) {
        context.startActivity(Intent(context, AppearanceSettingsActivity::class.java))
    }

    @Composable
    override fun Content(compact: Boolean, modifier: Modifier) {
        val config by settings.getSettingsFlow().collectAsState(settings.getConfig())
        val display = config.display.coreSettings
        val localContext = LocalContext.current
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!compact) {
                ThemeMode(display.appTheme)
                BlurControls(display.glassBlur)
                GlassBackgroundControls(
                    style = display.glassBackground,
                    imageName = display.glassBackgroundImage,
                    blur = display.glassBackgroundBlur,
                    onBlurChange = { blur -> updateDisplay { it.copy(glassBackgroundBlur = blur) } },
                    onChange = { style, image ->
                        updateDisplay { it.copy(glassBackground = style, glassBackgroundImage = image) }
                    },
                )
                GlassColorControls(
                    display.glassAutomaticColor,
                    display.glassThemeColor,
                    onAutomatic = { value -> updateDisplay { it.copy(glassAutomaticColor = value) } },
                    onColor = { value -> updateDisplay { it.copy(glassThemeColor = value) } },
                )
            } else {
                ButtonText(stringResource(R.string.appearance_more), { open(localContext) })
            }
        }
    }

    @Composable
    private fun ThemeMode(mode: AppTheme) {
        val system = stringResource(R.string.appearance_system)
        val light = stringResource(R.string.appearance_light)
        val dark = stringResource(R.string.appearance_dark)
        SelectInput(
            options = AppTheme.entries.toImmutableList(),
            selectedOption = mode,
            onOptionChange = { selected ->
                updateDisplay {
                    it.copy(
                        appTheme = selected,
                        messageViewTheme = SubTheme.USE_GLOBAL,
                        messageComposeTheme = SubTheme.USE_GLOBAL,
                    )
                }
            },
            optionToStringTransformation = {
                when (it) {
                    AppTheme.LIGHT -> light
                    AppTheme.DARK -> dark
                    AppTheme.FOLLOW_SYSTEM -> system
                }
            },
            label = stringResource(R.string.appearance_mode),
        )
    }

    @Composable
    private fun BlurControls(blur: Int, modifier: Modifier = Modifier) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassValueControl(
                value = blur,
                label = R.string.appearance_glass_blur_label,
                onChange = { value -> updateDisplay { it.copy(glassBlur = value) } },
                maximum = MAX_BLUR,
                sliderLimit = MAX_BLUR,
            )
        }
    }

    private fun updateDisplay(change: (DisplayCoreSettings) -> DisplayCoreSettings) {
        coreSettings.update(change)
    }
}

internal class AppearanceSettingsActivity : ComponentActivity() {
    private val appearance: AppearanceSettings by inject()
    private val theme: FeatureThemeProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            theme.WithTheme {
                Scaffold(topBar = { TopAppBarWithBackButton(appearance.title, ::finish) }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                        Column(
                            Modifier.widthIn(max = 840.dp).fillMaxWidth()
                                .verticalScroll(rememberScrollState()).padding(16.dp),
                        ) { appearance.Content() }
                    }
                }
            }
        }
    }
}
