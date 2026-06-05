package com.ugurbuga.blockgames.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.localization.AppEnvironment
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens

@Composable
fun BlockStylesGallery(
    modifier: Modifier = Modifier,
) {
    val styles = BlockVisualStyle.entries
    val uiColors = BlockGamesThemeTokens.uiColors

    Surface(
        modifier = modifier.fillMaxSize(),
        color = uiColors.gameSurface,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(styles) { style ->
                BlockStyleItem(style = style)
            }
        }
    }
}

@Composable
private fun BlockStyleItem(
    style: BlockVisualStyle,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Column(
        modifier = modifier
            .background(
                color = uiColors.panel.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = style.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BlockCellPreview(
                tone = CellTone.Cyan,
                palette = BlockColorPalette.Classic,
                style = style,
                size = 40.dp
            )
            BlockCellPreview(
                tone = CellTone.Gold,
                palette = BlockColorPalette.Classic,
                style = style,
                size = 40.dp
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BlockCellPreview(
                tone = CellTone.Coral,
                palette = BlockColorPalette.Classic,
                style = style,
                size = 40.dp
            )
            BlockCellPreview(
                tone = CellTone.Emerald,
                palette = BlockColorPalette.Classic,
                style = style,
                size = 40.dp
            )
        }
    }
}

@Preview(name = "Block Styles Gallery - Light")
@Composable
fun BlockStylesGalleryLightPreview() {
    val settings = AppSettings(themeMode = AppThemeMode.Light)
    BlockGamesTheme(settings = settings) {
        AppEnvironment(settings = settings) {
            BlockStylesGallery()
        }
    }
}

@Preview(name = "Block Styles Gallery - Dark")
@Composable
fun BlockStylesGalleryDarkPreview() {
    val settings = AppSettings(themeMode = AppThemeMode.Dark)
    BlockGamesTheme(settings = settings) {
        AppEnvironment(settings = settings) {
            BlockStylesGallery()
        }
    }
}
