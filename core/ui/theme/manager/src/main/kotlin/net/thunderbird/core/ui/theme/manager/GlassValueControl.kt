package net.thunderbird.core.ui.theme.manager

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.Slider
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.molecule.input.TextInput

@Composable
internal fun GlassValueControl(
    value: Int,
    @StringRes label: Int,
    @StringRes numberLabel: Int,
    onChange: (Int) -> Unit,
    maximum: Int = MAX_BLUR_VALUE,
    sliderLimit: Int = INITIAL_SLIDER_LIMIT,
) {
    val draft = remember(maximum) { BlurControlState(value, maximum) }
    var rangeEnd by remember { mutableIntStateOf(maxOf(sliderLimit, value).coerceAtMost(maximum)) }
    LaunchedEffect(value) {
        draft.external(value)
        rangeEnd = maxOf(rangeEnd, value).coerceAtMost(maximum)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextBodyLarge(stringResource(label, draft.value))
        Slider(
            value = draft.value.toFloat(),
            valueRange = 0f..rangeEnd.toFloat(),
            onValueChange = {
                draft.drag(it.roundToInt())
            },
            modifier = Modifier.fillMaxWidth(),
            onValueChangeFinished = { onChange(draft.finish()) },
        )
        TextInput(
            text = draft.input,
            onTextChange = { text ->
                draft.type(text)?.let { number ->
                    rangeEnd = maxOf(rangeEnd, number)
                    onChange(number)
                }
            },
            label = stringResource(numberLabel, draft.value),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        val parsed = draft.input.toIntOrNull()
        if (parsed == null || parsed !in 0..maximum) {
            TextBodyMedium(stringResource(R.string.appearance_value_range, maximum))
        }
    }
}

private const val MAX_BLUR_VALUE = 512
private const val INITIAL_SLIDER_LIMIT = 128
