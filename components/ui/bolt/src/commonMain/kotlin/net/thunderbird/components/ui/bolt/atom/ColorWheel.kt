package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Hue/saturation selector. Brightness and confirmation belong to the containing color dialog. */
@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    description: String,
    onChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val change by rememberUpdatedState(onChange)
    Canvas(
        modifier.aspectRatio(1f).semantics { contentDescription = description }.pointerInput(Unit) {
            fun select(position: Offset) {
                val x = position.x - size.width / 2f
                val y = position.y - size.height / 2f
                val angle = ((atan2(y, x) * HALF_CIRCLE / PI.toFloat()) + FULL_CIRCLE) % FULL_CIRCLE
                change(angle, (sqrt(x * x + y * y) / (size.width / 2f)).coerceIn(0f, 1f))
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                select(down.position)
                down.consume()
                drag(down.id) { event ->
                    select(event.position)
                    event.consume()
                }
            }
        },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(
            Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
            ),
            radius,
        )
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), radius = radius), radius)
        drawCircle(Color.Black.copy(alpha = 1f - brightness.coerceIn(0f, 1f)), radius)
        val radians = hue * PI.toFloat() / HALF_CIRCLE
        val position = center + Offset(cos(radians), sin(radians)) * (saturation * (radius - 2f))
        drawCircle(Color.Black, MARKER_RADIUS, position, style = Stroke(MARKER_OUTER_STROKE))
        drawCircle(Color.White, MARKER_RADIUS, position, style = Stroke(2f))
    }
}

private const val HALF_CIRCLE = 180f
private const val FULL_CIRCLE = 360f
private const val MARKER_RADIUS = 9f
private const val MARKER_OUTER_STROKE = 4f
