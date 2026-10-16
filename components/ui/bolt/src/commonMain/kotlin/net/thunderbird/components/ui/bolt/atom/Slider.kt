package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.LocalGlassBackdrop
import net.thunderbird.components.ui.bolt.theme.liquid.LiquidSlider

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..MAX_BLUR,
    onValueChangeFinished: () -> Unit = {},
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val backdrop = LocalGlassBackdrop.current
    if (LocalGlassAppearance.current.enabled && backdrop != null) {
        LiquidSlider(
            value = { value },
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            visibilityThreshold = 0.01f,
            backdrop = backdrop,
            modifier = modifier.onKeyEvent {
                handleSliderKey(
                    event = it,
                    value = value,
                    range = valueRange,
                    isRtl = isRtl,
                    onChange = onValueChange,
                    onFinished = onValueChangeFinished,
                )
            }.focusable().semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress {
                    onValueChange(it.coerceIn(valueRange))
                    onValueChangeFinished()
                    true
                }
            },
        )
        return
    }
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
    )
}

private const val MAX_BLUR = 32f

private fun handleSliderKey(
    event: KeyEvent,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    isRtl: Boolean,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
): Boolean {
    val direction = when (event.key) {
        Key.DirectionRight -> if (isRtl) -1 else 1
        Key.DirectionLeft -> if (isRtl) 1 else -1
        Key.DirectionUp -> 1
        Key.DirectionDown -> -1
        else -> 0
    }
    if (direction == 0) return false
    if (event.type == KeyEventType.KeyDown) {
        val step = (range.endInclusive - range.start) / KEYBOARD_STEPS
        onChange((value + step * direction).coerceIn(range))
    }
    if (event.type == KeyEventType.KeyUp) onFinished()
    return true
}

private const val KEYBOARD_STEPS = 32f
