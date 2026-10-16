package net.thunderbird.components.ui.bolt.organism

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.GlassPopupScene
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    val glass = LocalGlassAppearance.current.enabled
    MaterialModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = if (glass) {
            Color.Transparent
        } else {
            BottomSheetDefaults.ContainerColor
        },
        contentColor = BoltTheme.colors.onSurface,
        dragHandle = null,
    ) {
        if (glass) {
            // The outer modifier participates in Material's anchored placement. Keep optical layers inside it.
            GlassPopupScene(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().liquidGlass(BottomSheetDefaults.ExpandedShape),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BottomSheetDefaults.DragHandle()
                    content()
                }
            }
        } else {
            BottomSheetDefaults.DragHandle()
            content()
        }
    }
}
