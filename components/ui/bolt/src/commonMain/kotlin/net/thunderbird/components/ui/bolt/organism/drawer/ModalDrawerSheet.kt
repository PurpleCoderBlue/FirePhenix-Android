package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import androidx.compose.material3.ModalDrawerSheet as Material3ModalDrawerSheet

@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalGlassAppearance.current.enabled) {
        val shape = RoundedCornerShape(28.dp)
        Material3ModalDrawerSheet(
            modifier = modifier.liquidGlass(shape),
            drawerShape = shape,
            drawerContainerColor = Color.Transparent,
            content = content,
        )
    } else {
        Material3ModalDrawerSheet(modifier = modifier, content = content)
    }
}
