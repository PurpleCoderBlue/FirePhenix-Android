package net.thunderbird.components.ui.bolt.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.theme.glassField
import net.thunderbird.components.ui.bolt.theme.glassFieldColors
import net.thunderbird.components.ui.bolt.theme.glassFieldShape
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField

@Suppress("LongParameterList")
@Composable
fun TextFieldOutlinedNumber(
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,
) {
    Material3OutlinedTextField(
        shape = glassFieldShape(),
        colors = glassFieldColors(),
        value = value?.toString() ?: "",
        onValueChange = {
            onValueChange(
                it.takeIf { it.isNotBlank() }?.toLongOrNull(),
            )
        },
        modifier = modifier.glassField(),
        enabled = isEnabled,
        label = selectLabel(label, isRequired),
        readOnly = isReadOnly,
        isError = hasError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
        ),
        singleLine = true,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberWithLabelPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            label = "Label",
            onValueChange = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberDisabledPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
            isEnabled = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TextFieldOutlinedNumberErrorPreview() {
    PreviewWithThemes {
        TextFieldOutlinedNumber(
            value = 123L,
            onValueChange = {},
            hasError = true,
        )
    }
}
