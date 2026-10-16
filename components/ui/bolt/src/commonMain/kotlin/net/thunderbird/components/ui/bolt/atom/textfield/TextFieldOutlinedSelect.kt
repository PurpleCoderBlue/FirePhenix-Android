package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.glassField
import net.thunderbird.components.ui.bolt.theme.glassFieldColors
import net.thunderbird.components.ui.bolt.theme.glassFieldShape
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import androidx.compose.material3.DropdownMenu as Material3DropdownMenu
import androidx.compose.material3.DropdownMenuItem as Material3DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox as Material3ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField
import androidx.compose.material3.Text as Material3Text

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
fun <T> TextFieldOutlinedSelect(
    options: ImmutableList<T>,
    selectedOption: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionToStringTransformation: (T) -> String = { it.toString() },
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
    onOptionLongClick: ((T) -> Unit)? = null,
    optionLongClickLabel: String? = null,
    onAddOption: (() -> Unit)? = null,
    addOptionLabel: String? = null,
) {
    var isExpanded by remember {
        mutableStateOf(false)
    }

    val isReadOnlyOrDisabled = isReadOnly || !isEnabled

    Material3ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = {
            isExpanded = if (isReadOnlyOrDisabled) {
                false
            } else {
                isExpanded.not()
            }
        },
    ) {
        Material3OutlinedTextField(
            shape = glassFieldShape(),
            colors = glassFieldColors(),
            value = optionToStringTransformation(selectedOption),
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .then(modifier).glassField(),
            enabled = isEnabled,
            readOnly = true,
            label = selectLabel(label, isRequired),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            isError = hasError,
            singleLine = true,
            interactionSource = remember {
                MutableInteractionSource()
            },
        )

        if (isReadOnlyOrDisabled.not()) {
            Material3DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier.exposedDropdownSize().liquidGlass(RoundedCornerShape(20.dp)),
                containerColor = if (LocalGlassAppearance.current.enabled) {
                    Color.Transparent
                } else {
                    BoltTheme.colors.surfaceContainer.copy(alpha = 1f)
                },
            ) {
                options.forEach { option ->
                    val text = transformOptionWithSelectionHighlight(
                        option,
                        optionToStringTransformation(option),
                        selectedOption,
                    )
                    val selectOption = {
                        onValueChange(option)
                        isExpanded = false
                    }
                    if (onOptionLongClick == null) {
                        Material3DropdownMenuItem(
                            text = { Material3Text(text) },
                            onClick = selectOption,
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .semantics { selected = option == selectedOption }
                                .combinedClickable(
                                    role = Role.Button,
                                    onClick = selectOption,
                                    onLongClickLabel = optionLongClickLabel,
                                    onLongClick = {
                                        isExpanded = false
                                        onOptionLongClick(option)
                                    },
                                )
                                .padding(ExposedDropdownMenuDefaults.ItemContentPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Material3Text(text, color = BoltTheme.colors.onSurface)
                        }
                    }
                }
                if (onAddOption != null && addOptionLabel != null) {
                    if (options.isNotEmpty()) DividerHorizontal()
                    Material3DropdownMenuItem(
                        text = { Material3Text(addOptionLabel, color = BoltTheme.colors.primary) },
                        onClick = {
                            isExpanded = false
                            onAddOption()
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

private fun <T> transformOptionWithSelectionHighlight(
    option: T,
    optionString: String,
    selectedOption: T,
): AnnotatedString {
    return buildAnnotatedString {
        if (option == selectedOption) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(optionString)
            }
        } else {
            append(optionString)
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedSelectPreview() {
    PreviewWithThemes {
        TextFieldOutlinedSelect(
            options = persistentListOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedSelectWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedSelect(
            options = persistentListOf("Option 1", "Option 2", "Option 3"),
            selectedOption = "Option 1",
            onValueChange = {},
            label = "Label",
        )
    }
}
