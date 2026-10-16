package net.thunderbird.components.ui.bolt.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassBackdrop
import net.thunderbird.components.ui.bolt.theme.liquidGlass

/** Compares a detailed source with the same source through the actual shared glass material. */
@Composable
fun GlassPreview(label: String, sourceLabel: String, modifier: Modifier = Modifier) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier.clip(RoundedCornerShape(24.dp))) {
        Column(
            Modifier.matchParentSize().layerBackdrop(backdrop)
                .background(BoltTheme.colors.surfaceContainerLow).padding(16.dp).clearAndSetSemantics {},
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(PREVIEW_ROWS) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(PREVIEW_COLUMNS) {
                        Icon(
                            imageVector = if (row == 0) Icons.Outlined.MarkEmailUnread else Icons.Filled.Star,
                            modifier = Modifier.size(32.dp),
                            tint = if (row == 0) BoltTheme.colors.primary else BoltTheme.colors.tertiary,
                        )
                        TextBodyLarge(sourceLabel, color = BoltTheme.colors.onSurface)
                    }
                }
            }
        }
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            Box(
                Modifier.align(Alignment.CenterEnd).fillMaxWidth(GLASS_PREVIEW_FRACTION).fillMaxHeight()
                    .padding(8.dp).liquidGlass(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                TextBodyLarge(label, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

private const val PREVIEW_ROWS = 2
private const val PREVIEW_COLUMNS = 4
private const val GLASS_PREVIEW_FRACTION = 0.72f
