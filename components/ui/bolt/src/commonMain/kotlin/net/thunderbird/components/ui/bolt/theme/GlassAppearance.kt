package net.thunderbird.components.ui.bolt.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

data class GlassAppearance(
    val enabled: Boolean = false,
    val motionEnabled: Boolean = true,
    val backgroundVisible: Boolean = true,
    val blur: Int = DEFAULT_GLASS_BLUR,
    val background: ImageBitmap? = null,
    val backgroundCoordinates: (() -> GlassBackgroundCoordinates)? = null,
)

/** Places backgrounds shared by separate native and Compose hosts in the same window. */
data class GlassBackgroundCoordinates(val size: IntSize, val offset: IntOffset)

val LocalGlassAppearance = staticCompositionLocalOf { GlassAppearance() }

/** Records the scene for optical surfaces, preserving content state when appearance changes. */
@Composable
fun GlassScene(appearance: GlassAppearance, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val base = BoltTheme.colors.surface.copy(alpha = 1f)
    val backdrop = if (appearance.enabled) {
        // Backdrop's onDraw records the material source independently from visible scene paint.
        // Embedded native preference controls must not paint a rectangular wallpaper of their own.
        rememberLayerBackdrop(onDraw = {
            drawGlassBackground(base, appearance.background, appearance.backgroundCoordinates?.invoke())
        })
    } else {
        null
    }
    CompositionLocalProvider(
        LocalGlassAppearance provides appearance,
        LocalGlassBackdrop provides backdrop,
        LocalContentColor provides if (appearance.enabled) BoltTheme.colors.onSurface else LocalContentColor.current,
    ) {
        // Keep input nodes under the same parent when switching appearance. Moving a focused
        // TextField can update its platform magnifier while detached on Android 35/36.
        Box(modifier, propagateMinConstraints = true) {
            if (backdrop != null) {
                Canvas(Modifier.matchParentSize().layerBackdrop(backdrop)) {
                    if (appearance.backgroundVisible) {
                        drawGlassBackground(base, appearance.background, appearance.backgroundCoordinates?.invoke())
                    }
                }
            }
            content()
        }
    }
}

/** Owns backdrop coordinates inside the popup window instead of borrowing its zero-height caller. */
@Composable
internal fun GlassPopupScene(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val appearance = LocalGlassAppearance.current
    var coordinates by remember { mutableStateOf(GlassBackgroundCoordinates(IntSize.Zero, IntOffset.Zero)) }
    GlassScene(
        appearance.copy(backgroundVisible = false, backgroundCoordinates = { coordinates }),
        modifier.onGloballyPositioned {
            coordinates = GlassBackgroundCoordinates(it.findRootCoordinates().size, it.positionInWindow().round())
        },
        content,
    )
}

private fun DrawScope.drawGlassBackground(base: Color, image: ImageBitmap?, coordinates: GlassBackgroundCoordinates?) {
    drawRect(if (base.luminance() < 0.5f) Color(BLUE_DARK) else Color(BLUE_LIGHT))
    if (image == null || size.width <= 0f || size.height <= 0f) return
    val windowSize = coordinates?.size?.takeIf { it.width > 0 && it.height > 0 }
        ?: IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
    val offset = coordinates?.offset ?: IntOffset.Zero
    val aspect = windowSize.width.toFloat() / windowSize.height
    val wide = image.width.toFloat() / image.height > aspect
    val cropWidth = if (wide) (image.height * aspect).roundToInt().coerceIn(1, image.width) else image.width
    val cropHeight = if (wide) image.height else (image.width / aspect).roundToInt().coerceIn(1, image.height)
    clipRect {
        translate(-offset.x.toFloat(), -offset.y.toFloat()) {
            drawImage(
                image = image,
                srcOffset = IntOffset((image.width - cropWidth) / 2, (image.height - cropHeight) / 2),
                srcSize = IntSize(cropWidth, cropHeight),
                dstSize = windowSize,
            )
        }
    }
}

// Content keeps opaque color roles; only glass controls use transparency.
internal fun ThemeColorScheme.asGlass(): ThemeColorScheme = this

private const val DEFAULT_GLASS_BLUR = 16
private const val BLUE_DARK = 0xFF132D45
private const val BLUE_LIGHT = 0xFFE8F0F8
