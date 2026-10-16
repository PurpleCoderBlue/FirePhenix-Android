package net.thunderbird.core.ui.theme.manager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import java.util.Locale
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.SwitchInput
import net.thunderbird.components.ui.bolt.molecule.input.TextInput

@Composable
internal fun GlassColorControls(
    automatic: Boolean,
    color: Int,
    onAutomatic: (Boolean) -> Unit,
    onColor: (Int) -> Unit,
) {
    var input by remember(color) { mutableStateOf("#%06X".format(Locale.ROOT, color and RGB_MASK)) }
    val valid = Regex("#[a-fA-F0-9]{6}").matches(input)
    Column {
        SwitchInput(stringResource(R.string.appearance_automatic_color), automatic, onAutomatic)
        if (!automatic) {
            TextInput(text = input, label = stringResource(R.string.appearance_theme_color), onTextChange = { value ->
                input = value
                if (Regex("#[a-fA-F0-9]{6}").matches(value)) {
                    onColor(value.drop(1).toInt(HEX_RADIX) or OPAQUE_ALPHA)
                }
            })
            if (!valid) TextBodyMedium(stringResource(R.string.appearance_theme_color_format))
            FlowRow {
                listOf("#3576A5", "#B83C32", "#8658B5", "#3B8064", "#A86B22").forEach { preset ->
                    ButtonText(preset, { onColor(preset.drop(1).toInt(HEX_RADIX) or OPAQUE_ALPHA) })
                }
            }
        }
    }
}

private const val RGB_MASK = 0x00FFFFFF
private val OPAQUE_ALPHA = 0xFF000000.toInt()

private const val HEX_RADIX = 16
