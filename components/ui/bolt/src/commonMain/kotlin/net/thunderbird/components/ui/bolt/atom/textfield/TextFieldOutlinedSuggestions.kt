package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.glassField
import net.thunderbird.components.ui.bolt.theme.glassFieldColors
import net.thunderbird.components.ui.bolt.theme.glassFieldShape
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import androidx.compose.material3.DropdownMenu as MaterialDropdownMenu
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox as MaterialDropdownMenuBox
import androidx.compose.material3.OutlinedTextField as MaterialTextField
import androidx.compose.material3.Text as MaterialText

/** Editable address field. Selecting a suggestion changes the draft, never commits it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldOutlinedSuggestions(
    value: String,
    onValueChange: (String) -> Unit,
    options: ImmutableList<String>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(options, value) {
        options.asSequence().filter { it.contains(value.trim(), ignoreCase = true) }
            .take(MAX_VISIBLE_SUGGESTIONS).toList()
    }
    MaterialDropdownMenuBox(
        expanded = expanded && matches.isNotEmpty(),
        onExpandedChange = { expanded = enabled && it },
        modifier = modifier,
    ) {
        MaterialTextField(
            value = value,
            onValueChange = stripLineBreaks(onValueChange),
            label = { MaterialText(label) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = glassFieldShape(),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).glassField(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
                )
            },
        )
        MaterialDropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize().liquidGlass(RoundedCornerShape(20.dp)),
            containerColor = if (LocalGlassAppearance.current.enabled) Color.Transparent else BoltTheme.colors.surface,
        ) {
            matches.forEach { candidate ->
                MaterialDropdownMenuItem(
                    text = { MaterialText(candidate) },
                    onClick = {
                        onValueChange(candidate)
                        expanded = false
                    },
                )
            }
        }
    }
}

private const val MAX_VISIBLE_SUGGESTIONS = 50
