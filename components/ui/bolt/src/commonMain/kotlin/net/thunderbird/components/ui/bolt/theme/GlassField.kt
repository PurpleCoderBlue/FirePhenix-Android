package net.thunderbird.components.ui.bolt.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.glassField(): Modifier = liquidGlass(RoundedCornerShape(18.dp))

@Composable
internal fun glassFieldShape() =
    if (LocalGlassAppearance.current.enabled) RoundedCornerShape(18.dp) else OutlinedTextFieldDefaults.shape

@Composable
internal fun glassFieldColors() = if (LocalGlassAppearance.current.enabled) {
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = BoltTheme.colors.primary.copy(alpha = 0.55f),
        disabledBorderColor = BoltTheme.colors.outline.copy(alpha = 0.12f),
    )
} else {
    OutlinedTextFieldDefaults.colors()
}
