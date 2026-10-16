/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: shared rendering helpers, controlled state and project coding conventions.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
internal fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val light = BoltTheme.colors.surface.luminance() >= THEME_MIDPOINT
    val accent = BoltTheme.colors.primary
    val trackColor = if (light) {
        Color(0xFF787878).copy(TRACK_LIGHT_ALPHA)
    } else {
        Color(0xFF787880).copy(TRACK_DARK_ALPHA)
    }
    val dragWidth = with(LocalDensity.current) { 20.dp.toPx() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animation =
        rememberToggleAnimation(
            selected = selected,
            onSelect = onSelect,
            enabled = enabled,
            dragWidth = dragWidth,
            isLtr = isLtr,
        )
    val track = rememberLayerBackdrop()
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier.layerBackdrop(track).clip(Capsule()).drawBehind {
                drawRect(lerp(start = trackColor, stop = accent, fraction = animation.value))
            }.size(width = 64.dp, height = 28.dp),
        )
        Box(
            Modifier.graphicsLayer {
                val padding = 2.dp.toPx()
                translationX = if (isLtr) {
                    lerp(start = padding, stop = padding + dragWidth, fraction = animation.value)
                } else {
                    lerp(start = -padding, stop = -(padding + dragWidth), fraction = animation.value)
                }
            }.then(if (enabled) animation.modifier else Modifier)
                .liquidControlThumb(
                    backdrop = backdrop,
                    track = track,
                    animation = animation,
                    style = LiquidThumbStyles.Toggle,
                )
                .size(width = 40.dp, height = 24.dp),
        )
    }
}

@Composable
private fun rememberToggleAnimation(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    enabled: Boolean,
    dragWidth: Float,
    isLtr: Boolean,
): DampedDragAnimation {
    val selectedState = rememberUpdatedState(selected)
    val onSelectState = rememberUpdatedState(onSelect)
    val enabledState = rememberUpdatedState(enabled)
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val gestures = remember(dragWidth, isLtr, touchSlop) {
        ToggleDragState(
            selected = selectedState,
            onSelect = onSelectState,
            enabled = enabledState,
            dragWidth = dragWidth,
            isLtr = isLtr,
            touchSlop = touchSlop,
        )
    }
    val scope = rememberCoroutineScope()
    val animation = remember(scope, gestures) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = gestures.fraction,
            valueRange = 0f..1f,
            visibilityThreshold = TOGGLE_THRESHOLD,
            pressedScale = PRESSED_SCALE,
            onDragStarted = { gestures.begin() },
            onDragStopped = { gestures.stop(this) },
            onDragCancelled = { gestures.cancel() },
            onDrag = { _, amount -> gestures.drag(amount) },
        )
    }
    LaunchedEffect(animation) {
        snapshotFlow { gestures.fraction }.collectLatest { animation.updateValue(it) }
    }
    LaunchedEffect(gestures, animation) {
        snapshotFlow {
            selectedState.value()
        }.collectLatest { gestures.synchronize(selected = it, animation = animation) }
    }
    return animation
}

private const val THEME_MIDPOINT = 0.5f
private const val TRACK_LIGHT_ALPHA = 0.2f
private const val TRACK_DARK_ALPHA = 0.36f
private const val TOGGLE_THRESHOLD = 0.001f
private const val PRESSED_SCALE = 1.5f
