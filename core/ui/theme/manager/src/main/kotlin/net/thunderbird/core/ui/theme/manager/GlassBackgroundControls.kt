package net.thunderbird.core.ui.theme.manager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.SelectInput
import net.thunderbird.core.preference.display.coreSettings.GlassBackground

@Composable
internal fun GlassBackgroundControls(
    style: GlassBackground,
    imageName: String,
    blur: Int,
    onBlurChange: (Int) -> Unit,
    onChange: (GlassBackground, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                importing = true
                failed = false
                try {
                    val name = GlassBackgroundStore.importImage(context, uri)
                    onChange(GlassBackground.CUSTOM, name)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed = true
                } finally {
                    importing = false
                }
            }
        }
    }
    val labels = backgroundLabels()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectInput(
            options = GlassBackground.entries.toImmutableList(),
            selectedOption = style,
            label = stringResource(R.string.appearance_background),
            optionToStringTransformation = { labels.getValue(it) },
            onOptionChange = { selected ->
                if (!importing) {
                    if (selected == GlassBackground.CUSTOM && !GlassBackgroundStore.hasImage(context, imageName)) {
                        picker.launch(arrayOf("image/*"))
                    } else {
                        onChange(selected, imageName)
                    }
                }
            },
        )
        ButtonText(
            text = stringResource(R.string.appearance_background_pick),
            onClick = { picker.launch(arrayOf("image/*")) },
            enabled = !importing,
        )
        GlassValueControl(
            value = blur,
            label = R.string.appearance_background_blur_label,
            onChange = onBlurChange,
        )
        ImportStatus(importing, failed)
        if (style == GlassBackground.CUSTOM && !GlassBackgroundStore.hasImage(context, imageName)) {
            TextBodyMedium(stringResource(R.string.appearance_background_missing))
        }
    }
}

@Composable
private fun backgroundLabels() = mapOf(
    GlassBackground.BLUE to stringResource(R.string.appearance_background_blue),
    GlassBackground.WARM to stringResource(R.string.appearance_background_warm),
    GlassBackground.PHOENIX to stringResource(R.string.appearance_background_phoenix),
    GlassBackground.CUSTOM to stringResource(R.string.appearance_background_custom),
)

@Composable
private fun ImportStatus(importing: Boolean, failed: Boolean) {
    Column {
        if (importing) TextBodyMedium(stringResource(R.string.appearance_background_importing))
        if (failed) TextBodyMedium(stringResource(R.string.appearance_background_error))
    }
}
