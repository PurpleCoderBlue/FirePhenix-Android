/*
 * Slider input adaptation for the Apache-2.0 AndroidLiquidGlass sample.
 * Binds input to the stable track while preserving the upstream thumb animation.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.liquidSliderInput(
    animation: DampedDragAnimation,
    range: ClosedFloatingPointRange<Float>,
    isLtr: Boolean,
    onChange: State<(Float) -> Unit>,
    onFinished: State<() -> Unit>,
): Modifier = pointerInput(animation, range, isLtr) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var dragged = false
        animation.press()
        try {
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                dragged = true
                change.consume()
                updatePosition(change.position.x, size.width, animation, range, isLtr, onChange)
            }
            if (drag != null) {
                horizontalDrag(drag.id) { change ->
                    change.consume()
                    updatePosition(change.position.x, size.width, animation, range, isLtr, onChange)
                }
            }
            // MOVE events may be coalesced during a busy frame. UP is the authoritative final position.
            val up = currentEvent.changes.lastOrNull { !it.pressed && !it.isConsumed }
            if (up != null) {
                updatePosition(up.position.x, size.width, animation, range, isLtr, onChange)
                up.consume()
                onFinished.value()
            } else if (dragged) {
                onFinished.value()
            }
        } finally {
            animation.release()
        }
    }
}

private fun updatePosition(
    x: Float,
    width: Int,
    animation: DampedDragAnimation,
    range: ClosedFloatingPointRange<Float>,
    isLtr: Boolean,
    onChange: State<(Float) -> Unit>,
) {
    val fraction = (x / width.coerceAtLeast(1)).coerceIn(minimumValue = 0f, maximumValue = 1f)
    val target = range.start + (range.endInclusive - range.start) * if (isLtr) fraction else 1f - fraction
    animation.snapToValue(target)
    onChange.value(target)
}
