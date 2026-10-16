package net.thunderbird.components.ui.bolt.organism

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.liquidGlass
import androidx.compose.material3.TopAppBar as Material3TopAppBar

/**
 * A top app bar with a title, subtitle, navigation icon, and actions.
 *
 * @param title The title of the top app bar.
 * @param navigationIcon The icon to use for the navigation icon.
 * @param actions The actions to display in the top app bar.
 * @param modifier The modifier to apply to the top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit) = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Material3TopAppBar(
        windowInsets = if (LocalGlassAppearance.current.enabled) WindowInsets(0) else TopAppBarDefaults.windowInsets,
        title = { TextTitleLarge(text = title) },
        modifier = if (LocalGlassAppearance.current.enabled) {
            modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .liquidGlass(RoundedCornerShape(28.dp))
        } else {
            modifier
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = topAppBarColors(
            containerColor = if (LocalGlassAppearance.current.enabled) {
                Color.Transparent
            } else {
                BoltTheme.colors.surfaceContainer
            },
        ),
    )
}

@Composable
fun TopAppBarWithMenuButton(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            ButtonIcon(
                onClick = onMenuClick,
                imageVector = Icons.Outlined.Menu,
                modifier = Modifier.testTag("TopAppBarMenuButton"),
            )
        },
        actions = actions,
    )
}

@Composable
fun TopAppBarWithBackButton(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            ButtonIcon(
                onClick = onBackClick,
                imageVector = Icons.Outlined.ArrowBack,
                modifier = Modifier.testTag("TopAppBarBackButton"),
            )
        },
        actions = actions,
    )
}

@Composable
@Preview(showBackground = true)
internal fun TopAppBarPreview() {
    PreviewWithThemes {
        TopAppBar(
            title = "Title",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TopAppBarWithActionsPreview() {
    PreviewWithThemes {
        TopAppBar(
            title = "Title",
            actions = {
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Info,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Check,
                )
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Visibility,
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TopAppBarWithMenuButtonPreview() {
    PreviewWithThemes {
        TopAppBarWithMenuButton(
            title = "Title",
            onMenuClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun TopAppBarWithBackButtonPreview() {
    PreviewWithThemes {
        TopAppBarWithBackButton(
            title = "Title",
            onBackClick = {},
        )
    }
}
