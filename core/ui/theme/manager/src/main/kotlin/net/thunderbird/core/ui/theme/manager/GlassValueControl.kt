package net.thunderbird.core.ui.theme.manager

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.Slider
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.TextInput
import net.thunderbird.components.ui.bolt.organism.BasicDialog

@Composable
internal fun GlassValueControl(
    value: Int,
    @StringRes label: Int,
    onChange: (Int) -> Unit,
    maximum: Int = MAX_BLUR_VALUE,
    sliderLimit: Int = INITIAL_SLIDER_LIMIT,
) {
    val draft = remember(maximum) { BlurControlState(value, maximum) }
    var rangeEnd by remember { mutableIntStateOf(maxOf(sliderLimit, value).coerceAtMost(maximum)) }
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    LaunchedEffect(value) {
        draft.external(value)
        rangeEnd = maxOf(rangeEnd, value).coerceAtMost(maximum)
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextBodyMedium(stringResource(label))
        Slider(
            value = draft.value.toFloat(),
            valueRange = 0f..rangeEnd.toFloat(),
            onValueChange = { draft.drag(it.roundToInt()) },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            onValueChangeFinished = { onChange(draft.finish()) },
        )
        TextBodyMedium(
            draft.value.toString(),
            modifier = Modifier.clickable {
                input = draft.value.toString()
                editing =
                    true
            },
        )
    }
    if (editing) {
        val parsed = input.toIntOrNull()?.takeIf { it in 0..maximum }
        BasicDialog(
            headlineText = stringResource(label),
            onDismissRequest = { editing = false },
            content = {
                TextInput(
                    text = input,
                    onTextChange = {
                        input = it
                    },
                    label = stringResource(R.string.appearance_value_range, maximum),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            buttons = {
                ButtonText(stringResource(R.string.appearance_cancel), { editing = false })
                ButtonText(stringResource(R.string.appearance_apply), {
                    parsed?.let {
                        draft.type(input)
                        rangeEnd = maxOf(rangeEnd, it)
                        onChange(it)
                        editing = false
                    }
                }, enabled = parsed != null)
            },
        )
    }
}

private const val MAX_BLUR_VALUE = 512
private const val INITIAL_SLIDER_LIMIT = 128
