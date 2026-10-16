package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.glassAction
import androidx.compose.material3.Button as Material3Button
import androidx.compose.material3.Text as Material3Text

@Composable
fun ButtonFilled(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.filledButtonColors(),
    shape: ButtonShape = ButtonDefaults.filledShape(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    Material3Button(
        onClick = onClick,
        modifier = modifier.glassAction(interactive = enabled),
        enabled = enabled,
        shape = shape.shape,
        colors = if (LocalGlassAppearance.current.enabled) {
            colors.toMaterial3Colors().copy(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = BoltTheme.colors.onSurface,
            )
        } else {
            colors.toMaterial3Colors()
        },
        border = shape.borderStroke?.toMaterial3BorderStroke(),
        contentPadding = contentPadding,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(end = BoltTheme.spacings.default),
                tint = if (LocalGlassAppearance.current.enabled) BoltTheme.colors.onSurface else colors.iconColor,
            )
        }
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledPreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "Button Filled",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledDisabledPreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "Button Filled Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledMultiLinePreview() {
    PreviewWithThemes {
        ButtonFilled(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
