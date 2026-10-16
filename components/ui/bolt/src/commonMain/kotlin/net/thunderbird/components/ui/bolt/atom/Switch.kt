package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.LocalGlassBackdrop
import net.thunderbird.components.ui.bolt.theme.liquid.LiquidToggle
import androidx.compose.material3.Switch as Material3Switch

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val backdrop = LocalGlassBackdrop.current
    if (LocalGlassAppearance.current.enabled && backdrop != null) {
        LiquidToggle(
            enabled = enabled,
            selected = {
                checked
            },
            onSelect = { if (enabled) onCheckedChange(it) },
            backdrop = backdrop,
            modifier = modifier.onKeyEvent { event ->
                val activate = event.key == Key.Spacebar || event.key == Key.Enter
                if (enabled && activate) {
                    if (event.type == KeyEventType.KeyUp) onCheckedChange(!checked)
                    true
                } else {
                    false
                }
            }.focusable(enabled).defaultMinSize(
                minHeight = 48.dp,
            ).alpha(if (enabled) 1f else DISABLED_ALPHA).semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                if (!enabled) disabled()
                onClick {
                    if (enabled) onCheckedChange(!checked)
                    enabled
                }
            },
        )
        return
    }
    Material3Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
@Preview(showBackground = true)
internal fun SwitchPreview() {
    PreviewWithThemes {
        Switch(
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SwitchDisabledPreview() {
    PreviewWithThemes {
        Switch(
            checked = true,
            onCheckedChange = {},
            enabled = false,
        )
    }
}

private const val DISABLED_ALPHA = 0.38f
