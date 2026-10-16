/*
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 * Adapted from AndroidLiquidGlass catalog examples.
 * Changes: package/import relocation and project Kotlin formatting.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
) {

    private val pressProgressAnimationSpec =
        spring(dampingRatio = SPRING_DAMPING, stiffness = SPRING_STIFFNESS, visibilityThreshold = PRESS_THRESHOLD)
    private val positionAnimationSpec =
        spring(
            dampingRatio = SPRING_DAMPING,
            stiffness = SPRING_STIFFNESS,
            visibilityThreshold = Offset.VisibilityThreshold,
        )

    private val pressProgressAnimation =
        Animatable(initialValue = 0f, visibilityThreshold = PRESS_THRESHOLD)
    private val positionAnimation =
        Animatable(
            initialValue = Offset.Zero,
            typeConverter = Offset.VectorConverter,
            visibilityThreshold = Offset.VisibilityThreshold,
        )

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    private val shader =
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}""",
            )
        } else {
            null
        }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                if (shader != null) {
                    drawRect(
                        Color.White.copy(BASE_PRESS_ALPHA * progress),
                        blendMode = BlendMode.Plus,
                    )
                    shader.apply {
                        val position = position(size, positionAnimation.value)
                        setFloatUniform(name = "size", value1 = size.width, value2 = size.height)
                        setColorUniform("color", Color.White.copy(HIGHLIGHT_ALPHA * progress))
                        setFloatUniform("radius", size.minDimension * LIGHT_RADIUS_SCALE)
                        setFloatUniform(
                            name = "position",
                            value1 = position.x.fastCoerceIn(minimumValue = 0f, maximumValue = size.width),
                            value2 = position.y.fastCoerceIn(minimumValue = 0f, maximumValue = size.height),
                        )
                    }
                    drawRect(
                        ShaderBrush(shader.asComposeShader()),
                        blendMode = BlendMode.Plus,
                    )
                } else {
                    drawRect(
                        Color.White.copy(FALLBACK_PRESS_ALPHA * progress),
                        blendMode = BlendMode.Plus,
                    )
                }
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}

private const val SPRING_DAMPING = 0.5f
private const val SPRING_STIFFNESS = 300f
private const val PRESS_THRESHOLD = 0.001f
private const val BASE_PRESS_ALPHA = 0.08f
private const val HIGHLIGHT_ALPHA = 0.15f
private const val LIGHT_RADIUS_SCALE = 1.5f
private const val FALLBACK_PRESS_ALPHA = 0.25f
