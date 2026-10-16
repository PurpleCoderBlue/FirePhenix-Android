package net.thunderbird.components.ui.bolt.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.shapes.RoundedRectangularShape
import net.thunderbird.components.ui.bolt.theme.liquid.InteractiveHighlight

internal val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalGlassToolbar = staticCompositionLocalOf { false }

/**
 * Uses Backdrop's published optical renderer, rather than drawing a glass texture.
 * Foreground content is drawn after the refracted backdrop and remains sharp.
 * The original appearance returns the exact incoming modifier.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.Unspecified,
    interactive: Boolean = false,
    showHighlight: Boolean = true,
): Modifier {
    val appearance = LocalGlassAppearance.current
    val backdrop = LocalGlassBackdrop.current
    if (!appearance.enabled || backdrop == null) return this
    val materialTint = if (tint == Color.Unspecified) BoltTheme.colors.surface else tint
    val opticalShape = when (shape) {
        RectangleShape -> RoundedCornerShape(0.dp)
        is CornerBasedShape, is RoundedRectangularShape -> shape
        else -> RoundedCornerShape(24.dp)
    }
    val scope = rememberCoroutineScope()
    val interaction = if (interactive) remember(scope) { InteractiveHighlight(scope) } else null
    val dark = BoltTheme.colors.surface.luminance() < 0.5f
    return drawBackdrop(
        backdrop = backdrop,
        shape = { opticalShape },
        effects = {
            vibrancy()
            val level =
                appearance.blur.coerceIn(minimumValue = 0, maximumValue = MAX_GLASS_BLUR) / MAX_GLASS_BLUR.toFloat()
            blur((MAX_BLUR_RADIUS_DP * level * level).dp.toPx())
            lens(
                refractionHeight = minOf(16.dp.toPx(), size.minDimension / 2f),
                refractionAmount = 24.dp.toPx(),
                depthEffect = true,
                chromaticAberration = true,
            )
        },
        highlight = {
            if (showHighlight) {
                Highlight(
                    width = 0.8.dp,
                    alpha = if (dark) 0.8f else 0.65f,
                    style = HighlightStyle.Default(
                        angle = 45f + (interaction?.offset?.x ?: 0f) * 0.12f,
                        falloff = 1.4f,
                    ),
                )
            } else {
                null
            }
        },
        layerBlock = interaction?.let { highlight ->
            val transform: GraphicsLayerScope.() -> Unit = {
                val scale = 1f + highlight.pressProgress * 0.035f
                scaleX = scale
                scaleY = scale
            }
            transform
        },
        // Large blur kernels can leave translucent pixels; prevent the sharp source showing through them.
        onDrawBehind = { drawRect(materialTint.copy(alpha = 1f)) },
        // Bound the recorded source to this material instead of exporting the whole scene's display list.
        onDrawBackdrop = { drawBackdrop -> clipRect { drawBackdrop() } },
        onDrawSurface = {
            val level =
                appearance.blur.coerceIn(minimumValue = 0, maximumValue = MAX_GLASS_BLUR) / MAX_GLASS_BLUR.toFloat()
            drawRect(materialTint.copy(alpha = CLEAR_TINT_ALPHA + FROST_TINT_ALPHA_RANGE * level))
        },
    ).then(interaction?.modifier ?: Modifier)
        .then(interaction?.gestureModifier ?: Modifier)
}

/** A pill for actions; actions within a glass toolbar share its material. */
@Composable
internal fun Modifier.glassAction(interactive: Boolean = true): Modifier =
    if (LocalGlassToolbar.current) {
        this
    } else {
        liquidGlass(
            shape = RoundedCornerShape(PILL_CORNER_PERCENT),
            interactive = interactive,
        )
    }

private const val MAX_GLASS_BLUR = 32
private const val MAX_BLUR_RADIUS_DP = 96f
private const val CLEAR_TINT_ALPHA = 0.08f
private const val FROST_TINT_ALPHA_RANGE = 0.70f
private const val PILL_CORNER_PERCENT = 50
