package net.thunderbird.core.ui.theme.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale
import net.thunderbird.components.ui.bolt.atom.ColorWheel
import net.thunderbird.components.ui.bolt.atom.Slider
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.SwitchInput
import net.thunderbird.components.ui.bolt.organism.BasicDialog
import android.graphics.Color as AndroidColor

@Composable
internal fun GlassColorControls(
    automatic: Boolean,
    color: Int,
    onAutomatic: (Boolean) -> Unit,
    onColor: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    Column {
        SwitchInput(stringResource(R.string.appearance_automatic_color), automatic, onAutomatic)
        if (!automatic) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(Color(color)))
                ButtonText(stringResource(R.string.appearance_customize_color), { editing = true })
                TextBodyMedium("#%06X".format(Locale.ROOT, color and RGB_MASK))
            }
        }
    }
    if (editing) {
        ColorPickerDialog(color, { editing = false }) { selected ->
            onColor(selected)
            editing = false
        }
    }
}

@Composable
private fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var brightness by remember { mutableFloatStateOf(hsv[2]) }
    val selected = Color.hsv(hue, saturation, brightness)
    BasicDialog(
        headlineText = stringResource(R.string.appearance_theme_color),
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 420.dp),
        buttons = {
            ButtonText(stringResource(R.string.appearance_cancel), onDismiss)
            ButtonText(stringResource(R.string.appearance_apply), { onConfirm(selected.toArgb()) })
        },
        content = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorWheel(
                    hue,
                    saturation,
                    brightness,
                    stringResource(R.string.appearance_color_wheel),
                    onChange = { h, s ->
                        hue = h
                        saturation = s
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                ColorComponent(stringResource(R.string.appearance_color_hue), hue, HUE_RANGE) {
                    hue =
                        it.coerceAtMost(MAXIMUM_HUE)
                }
                ColorComponent(stringResource(R.string.appearance_color_saturation), saturation, 1f) { saturation = it }
                ColorComponent(stringResource(R.string.appearance_color_brightness), brightness, 1f) { brightness = it }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(32.dp).background(selected))
                    TextBodyMedium("#%06X".format(Locale.ROOT, selected.toArgb() and RGB_MASK))
                }
            }
        },
    )
}

@Composable
private fun ColorComponent(label: String, value: Float, maximum: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextBodyMedium(label)
        Slider(
            value,
            onChange,
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
            valueRange =
            0f..maximum,
        )
    }
}

private const val RGB_MASK = 0x00FFFFFF

private const val HUE_RANGE = 360f
private const val MAXIMUM_HUE = 359.99f
