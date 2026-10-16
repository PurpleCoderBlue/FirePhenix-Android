/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: shared rendering helpers, controlled state and project coding conventions.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

internal data class LiquidThumbStyle(
    val overlayScale: Float,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val velocityDivisor: Float,
)

internal object LiquidThumbStyles {
    private const val TOGGLE_OVERLAY_SCALE = 0.75f
    private const val TOGGLE_VELOCITY_DIVISOR = 50f
    private const val SLIDER_VELOCITY_DIVISOR = 10f
    val Toggle =
        LiquidThumbStyle(
            overlayScale = TOGGLE_OVERLAY_SCALE,
            refractionHeight = 5.dp,
            refractionAmount = 10.dp,
            velocityDivisor = TOGGLE_VELOCITY_DIVISOR,
        )
    val Slider =
        LiquidThumbStyle(
            overlayScale = 1f,
            refractionHeight = 10.dp,
            refractionAmount = 14.dp,
            velocityDivisor = SLIDER_VELOCITY_DIVISOR,
        )
}

@Composable
internal fun Modifier.liquidControlThumb(
    backdrop: Backdrop,
    track: LayerBackdrop,
    animation: DampedDragAnimation,
    style: LiquidThumbStyle,
): Modifier {
    val combined = rememberCombinedBackdrop(
        backdrop1 = backdrop,
        backdrop2 = rememberBackdrop(track) { drawBackdrop ->
            val progress = animation.pressProgress
            scale(
                scaleX = lerp(start = REST_OVERLAY_SCALE, stop = style.overlayScale, fraction = progress),
                scaleY = lerp(start = 0f, stop = style.overlayScale, fraction = progress),
            ) { drawBackdrop() }
        },
    )
    return drawBackdrop(
        backdrop = combined,
        shape = { Capsule() },
        effects = {
            val progress = animation.pressProgress
            blur(8.dp.toPx() * (1f - progress))
            lens(
                refractionHeight = style.refractionHeight.toPx() * progress,
                refractionAmount = style.refractionAmount.toPx() * progress,
                chromaticAberration = true,
            )
        },
        highlight = {
            Highlight.Ambient.copy(
                width = Highlight.Ambient.width / HIGHLIGHT_DIVISOR,
                blurRadius = Highlight.Ambient.blurRadius / HIGHLIGHT_DIVISOR,
                alpha = animation.pressProgress,
            )
        },
        shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = SHADOW_ALPHA)) },
        innerShadow = { InnerShadow(radius = 4.dp * animation.pressProgress, alpha = animation.pressProgress) },
        layerBlock = { applyLiquidDeformation(animation = animation, style = style) },
        // Bound the recorded source to this material instead of exporting the whole scene's display list.
        onDrawBackdrop = { drawBackdrop -> clipRect { drawBackdrop() } },
        onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - animation.pressProgress)) },
    )
}

private fun GraphicsLayerScope.applyLiquidDeformation(animation: DampedDragAnimation, style: LiquidThumbStyle) {
    scaleX = animation.scaleX
    scaleY = animation.scaleY
    val velocity = animation.velocity / style.velocityDivisor
    scaleX /= 1f - (velocity * WIDTH_VELOCITY_FACTOR).fastCoerceIn(
        minimumValue = -MAX_DEFORMATION,
        maximumValue = MAX_DEFORMATION,
    )
    scaleY *= 1f - (velocity * HEIGHT_VELOCITY_FACTOR).fastCoerceIn(
        minimumValue = -MAX_DEFORMATION,
        maximumValue = MAX_DEFORMATION,
    )
}

private const val REST_OVERLAY_SCALE = 2f / 3f
private const val HIGHLIGHT_DIVISOR = 1.5f
private const val SHADOW_ALPHA = 0.05f
private const val WIDTH_VELOCITY_FACTOR = 0.75f
private const val HEIGHT_VELOCITY_FACTOR = 0.25f
private const val MAX_DEFORMATION = 0.2f
