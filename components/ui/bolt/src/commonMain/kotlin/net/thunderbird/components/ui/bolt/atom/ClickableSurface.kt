package net.thunderbird.components.ui.bolt.atom

import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import androidx.compose.material3.Surface as Material3Surface

/**
 * A UI atom for a Surface with a click listener.
 *
 * This is a convenience wrapper around [Material3Surface] that provides default values from [BoltTheme].
 *
 * @param onClick The lambda to be executed when the surface is clicked.
 * @param modifier The modifier to be applied to the surface.
 * @param shape The shape of the surface.
 * @param color The color of the surface.
 * @param tonalElevation The tonal elevation of the surface.
 * @param content The content to be displayed inside the surface.
 */
@Composable
fun ClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = BoltTheme.colors.surface,
    tonalElevation: Dp = BoltTheme.elevations.level0,
    content: @Composable () -> Unit,
) {
    val glass = LocalGlassAppearance.current.enabled
    val page = color == BoltTheme.colors.surface && shape == RectangleShape &&
        tonalElevation == BoltTheme.elevations.level0
    Material3Surface(
        onClick = onClick,
        modifier = if (glass && !page) modifier.liquidGlass(shape, color, interactive = true) else modifier,
        shape = shape,
        content = content,
        tonalElevation = tonalElevation,
        color = if (glass) Color.Transparent else color,
        contentColor = contentColorFor(color),
    )
}
