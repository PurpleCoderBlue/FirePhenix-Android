/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: package/import relocation and project Kotlin formatting.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull

internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)

        val down = awaitFirstDown(false)
        val drag = initialDown

        onDragStart(down)
        onDrag(drag, Offset.Zero)
        val upEvent =
            drag(
                pointerId = drag.id,
                onDrag = { onDrag(it, it.positionChange()) },
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    if (currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true) return null
    var pointer = pointerId
    var result: PointerInputChange? = null
    var active = true
    while (active) {
        val change = awaitDragOrUp(pointer)
        when {
            change == null || change.isConsumed -> active = false

            change.changedToUpIgnoreConsumed() -> {
                result = change
                active = false
            }

            else -> {
                onDrag(change)
                pointer = change.id
            }
        }
    }
    return result
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    var result: PointerInputChange? = null
    var active = true
    while (active) {
        val change = awaitPointerEvent().changes.fastFirstOrNull { it.id == pointer }
        when {
            change == null -> active = false

            change.changedToUpIgnoreConsumed() -> {
                val other = currentEvent.changes.fastFirstOrNull { it.pressed }
                if (other == null) {
                    result = change
                    active = false
                } else {
                    pointer = other.id
                }
            }

            change.previousPosition != change.position -> {
                result = change
                active = false
            }
        }
    }
    return result
}
