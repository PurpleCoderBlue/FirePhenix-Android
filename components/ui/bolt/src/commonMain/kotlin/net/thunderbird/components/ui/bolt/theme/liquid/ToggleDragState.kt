/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: shared rendering helpers, controlled state and project coding conventions.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.abs

/** Keeps the sample's horizontal drag behavior separate from parent scrolling. */
internal class ToggleDragState(
    private val selected: State<() -> Boolean>,
    private val onSelect: State<(Boolean) -> Unit>,
    private val enabled: State<Boolean>,
    private val dragWidth: Float,
    private val isLtr: Boolean,
    private val touchSlop: Float,
) {
    var fraction by mutableFloatStateOf(if (selected.value()) 1f else 0f)
        private set
    private var didDrag = false
    private var dragOffset = Offset.Zero
    private var verticalGesture = false

    fun begin() {
        dragOffset = Offset.Zero
        verticalGesture = false
        didDrag = false
    }

    fun cancel() {
        fraction = if (selected.value()) 1f else 0f
        didDrag = false
    }

    fun stop(animation: DampedDragAnimation) {
        if (!enabled.value || verticalGesture) {
            cancel()
            return
        }
        fraction = if (didDrag) {
            if (animation.targetValue >= MIDPOINT) 1f else 0f
        } else {
            if (selected.value()) 0f else 1f
        }
        onSelect.value(fraction == 1f)
        didDrag = false
    }

    fun drag(amount: Offset) {
        dragOffset += amount
        if (abs(dragOffset.y) > touchSlop && abs(dragOffset.y) > abs(dragOffset.x)) verticalGesture = true
        if (verticalGesture) return
        if (!didDrag) didDrag = amount.x != 0f
        val delta = amount.x / dragWidth
        fraction = (if (isLtr) fraction + delta else fraction - delta)
            .fastCoerceIn(minimumValue = 0f, maximumValue = 1f)
    }

    fun synchronize(selected: Boolean, animation: DampedDragAnimation) {
        val target = if (selected) 1f else 0f
        if (target != fraction) {
            fraction = target
            animation.animateToValue(target)
        }
    }

    private companion object {
        const val MIDPOINT = 0.5f
    }
}
