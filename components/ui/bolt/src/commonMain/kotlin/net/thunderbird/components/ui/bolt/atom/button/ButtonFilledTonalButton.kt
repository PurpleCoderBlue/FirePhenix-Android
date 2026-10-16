package net.thunderbird.components.ui.bolt.atom.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.glassAction
import androidx.compose.material3.FilledTonalButton as Material3FilledTonalButton
import androidx.compose.material3.Text as Material3Text

@Composable
fun ButtonFilledTonal(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Material3FilledTonalButton(
        onClick = onClick,
        modifier = modifier.glassAction(interactive = enabled),
        enabled = enabled,
        colors = if (LocalGlassAppearance.current.enabled) {
            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            )
        } else {
            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
        },
    ) {
        Material3Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledTonalPreview() {
    PreviewWithThemes {
        ButtonFilledTonal(
            text = "Button Filled Tonal",
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledTonalDisabledPreview() {
    PreviewWithThemes {
        ButtonFilledTonal(
            text = "Button Filled Tonal Disabled",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ButtonFilledTonalMultiLinePreview() {
    PreviewWithThemes {
        ButtonFilledTonal(
            text = "First\nSecond line",
            onClick = {},
        )
    }
}
