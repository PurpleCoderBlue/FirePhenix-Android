/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: shared rendering helpers, controlled state and project coding conventions.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
internal fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val light = BoltTheme.colors.surface.luminance() >= THEME_MIDPOINT
    val accent = net.thunderbird.components.ui.bolt.theme.BoltTheme.colors.primary
    val trackColor = if (light) {
        Color(0xFF787878).copy(TRACK_LIGHT_ALPHA)
    } else {
        Color(0xFF787880).copy(TRACK_DARK_ALPHA)
    }
    val track = rememberLayerBackdrop()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animation = rememberSliderAnimation(value = value, range = valueRange, threshold = visibilityThreshold)
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val finished = rememberUpdatedState(onValueChangeFinished)
    val progress = { ((value() - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f) }
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).liquidSliderInput(
            animation = animation,
            range = valueRange,
            isLtr = isLtr,
            onChange = currentOnValueChange,
            onFinished = finished,
        ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val width = constraints.maxWidth
        SliderTrack(progress = progress, backdrop = track, accent = accent, track = trackColor)
        Box(
            Modifier.graphicsLayer {
                translationX = (-size.width / 2f + width * progress()).fastCoerceIn(
                    minimumValue = -size.width / THUMB_EDGE_DIVISOR,
                    maximumValue = width - size.width * THUMB_END_FACTOR,
                ) * if (isLtr) 1f else -1f
            }
                .liquidControlThumb(
                    backdrop = backdrop,
                    track = track,
                    animation = animation,
                    style = LiquidThumbStyles.Slider,
                )
                .size(width = 40.dp, height = 24.dp),
        )
    }
}

@Composable
private fun rememberSliderAnimation(
    value: () -> Float,
    range: ClosedFloatingPointRange<Float>,
    threshold: Float,
): DampedDragAnimation {
    val currentValue = rememberUpdatedState(value)
    val scope = rememberCoroutineScope()
    val animation = remember(scope, range) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = currentValue.value(),
            valueRange = range,
            visibilityThreshold = threshold,
            pressedScale = PRESSED_SCALE,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> },
        )
    }
    LaunchedEffect(animation) {
        snapshotFlow { currentValue.value() }.collectLatest { updated ->
            if (animation.targetValue != updated) animation.snapToValue(updated)
        }
    }
    return animation
}

@Composable
private fun SliderTrack(
    progress: () -> Float,
    backdrop: LayerBackdrop,
    accent: Color,
    track: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.layerBackdrop(backdrop)) {
        Box(
            Modifier.clip(Capsule()).background(track).height(6.dp).fillMaxWidth(),
        )
        Box(
            Modifier.clip(Capsule()).background(accent).height(6.dp).layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = (constraints.maxWidth * progress()).fastRoundToInt()
                layout(width, placeable.height) { placeable.place(0, 0) }
            },
        )
    }
}

private const val THEME_MIDPOINT = 0.5f
private const val TRACK_LIGHT_ALPHA = 0.2f
private const val TRACK_DARK_ALPHA = 0.36f
private const val THUMB_EDGE_DIVISOR = 4f
private const val THUMB_END_FACTOR = 3f / 4f
private const val PRESSED_SCALE = 1.5f
