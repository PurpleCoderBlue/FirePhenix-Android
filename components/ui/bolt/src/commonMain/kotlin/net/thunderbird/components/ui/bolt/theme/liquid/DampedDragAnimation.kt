/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: package/import relocation and project Kotlin formatting.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float = 1f,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDragCancelled: DampedDragAnimation.() -> Unit = {},
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec =
        spring(dampingRatio = 1f, stiffness = VALUE_STIFFNESS, visibilityThreshold = visibilityThreshold)
    private val velocityAnimationSpec =
        spring(
            dampingRatio = VELOCITY_DAMPING,
            stiffness = VELOCITY_STIFFNESS,
            visibilityThreshold =
            visibilityThreshold * VELOCITY_THRESHOLD_FACTOR,
        )
    private val pressProgressAnimationSpec =
        spring(dampingRatio = 1f, stiffness = VALUE_STIFFNESS, visibilityThreshold = ANIMATION_THRESHOLD)
    private val scaleXAnimationSpec =
        spring(dampingRatio = WIDTH_DAMPING, stiffness = SCALE_STIFFNESS, visibilityThreshold = ANIMATION_THRESHOLD)
    private val scaleYAnimationSpec =
        spring(dampingRatio = HEIGHT_DAMPING, stiffness = SCALE_STIFFNESS, visibilityThreshold = ANIMATION_THRESHOLD)

    private val valueAnimation =
        Animatable(initialValue = initialValue, visibilityThreshold = visibilityThreshold)
    private val velocityAnimation =
        Animatable(initialValue = 0f, visibilityThreshold = INITIAL_VELOCITY_THRESHOLD)
    private val pressProgressAnimation =
        Animatable(initialValue = 0f, visibilityThreshold = ANIMATION_THRESHOLD)
    private val scaleXAnimation =
        Animatable(initialValue = initialScale, visibilityThreshold = ANIMATION_THRESHOLD)
    private val scaleYAnimation =
        Animatable(initialValue = initialScale, visibilityThreshold = ANIMATION_THRESHOLD)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragCancelled()
                release()
            },
        ) { change, dragAmount ->
            onDrag(size, dragAmount)
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * RELEASE_THRESHOLD_FACTOR
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    private var snapJob: Job? = null

    /** Slider movement follows the finger; obsolete pointer updates must never queue animations. */
    fun snapToValue(value: Float) {
        snapJob?.cancel()
        snapJob = animationScope.launch { valueAnimation.snapTo(value.coerceIn(valueRange)) }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            Clock.System.now().toEpochMilliseconds(),
            Offset(x = value, y = 0f),
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}

private const val VALUE_STIFFNESS = 1000f
private const val VELOCITY_DAMPING = 0.5f
private const val VELOCITY_STIFFNESS = 300f
private const val VELOCITY_THRESHOLD_FACTOR = 10f
private const val WIDTH_DAMPING = 0.6f
private const val HEIGHT_DAMPING = 0.7f
private const val SCALE_STIFFNESS = 250f
private const val ANIMATION_THRESHOLD = 0.001f
private const val INITIAL_VELOCITY_THRESHOLD = 5f
private const val RELEASE_THRESHOLD_FACTOR = 0.025f
