package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.tooling.preview.Preview
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevices
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.LocalGlassAppearance
import net.thunderbird.components.ui.bolt.theme.LocalGlassBackdrop
import net.thunderbird.components.ui.bolt.theme.LocalGlassToolbar
import androidx.compose.material3.FabPosition as Material3FabPosition
import androidx.compose.material3.Scaffold as Material3Scaffold

@Suppress("LongParameterList")
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: ScaffoldFabPosition = ScaffoldFabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    val glass = LocalGlassAppearance.current.enabled
    val scene = LocalGlassBackdrop.current as? LayerBackdrop
    val pageBackdrop = rememberLayerBackdrop(onDraw = {
        scene?.let { drawLayer(it.graphicsLayer) }
        drawContent()
    })
    Material3Scaffold(
        modifier = modifier,
        contentColor = BoltTheme.colors.onSurface,
        containerColor = if (LocalGlassAppearance.current.enabled) {
            Color.Transparent
        } else {
            BoltTheme.colors.surface
        },
        topBar = {
            CompositionLocalProvider(
                LocalGlassBackdrop provides if (glass) pageBackdrop else scene,
                LocalGlassToolbar provides glass,
            ) {
                topBar()
            }
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition.toMaterialFabPosition(),
        content = { padding ->
            Box(Modifier.fillMaxSize().then(if (glass) Modifier.layerBackdrop(pageBackdrop) else Modifier)) {
                content(padding)
            }
        },
    )
}

enum class ScaffoldFabPosition {
    Start,
    Center,
    End,
    EndOverlay,
}

private fun ScaffoldFabPosition.toMaterialFabPosition(): Material3FabPosition {
    return when (this) {
        ScaffoldFabPosition.Start -> Material3FabPosition.Start
        ScaffoldFabPosition.Center -> Material3FabPosition.Center
        ScaffoldFabPosition.End -> Material3FabPosition.End
        ScaffoldFabPosition.EndOverlay -> Material3FabPosition.EndOverlay
    }
}

@Composable
@PreviewDevices
internal fun ScaffoldPreview() {
    PreviewWithTheme {
        Scaffold(
            topBar = {
                Surface(
                    color = BoltTheme.colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BoltTheme.sizes.topBarHeight),
                ) {}
            },
            bottomBar = {
                Surface(
                    color = BoltTheme.colors.warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BoltTheme.sizes.bottomBarHeight),
                ) {}
            },
        ) { contentPadding ->
            Surface(
                color = BoltTheme.colors.info,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {}
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun ScaffoldWitFabPreview() {
    PreviewWithTheme {
        Scaffold(
            topBar = {
                Surface(
                    color = BoltTheme.colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BoltTheme.sizes.topBarHeight),
                ) {}
            },
            bottomBar = {
                Surface(
                    color = BoltTheme.colors.warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BoltTheme.sizes.bottomBarHeight),
                ) {}
            },
            floatingActionButton = {
                ButtonIcon(
                    onClick = { },
                    imageVector = Icons.Outlined.Check,
                )
            },
        ) { contentPadding ->
            Surface(
                color = BoltTheme.colors.surface,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {}
        }
    }
}
