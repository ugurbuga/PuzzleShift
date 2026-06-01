package com.ugurbuga.blockgames.ui.game.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.block_properties_column_clearer_desc
import blockgames.composeapp.generated.resources.block_properties_column_clearer_title
import blockgames.composeapp.generated.resources.block_properties_ghost_desc
import blockgames.composeapp.generated.resources.block_properties_ghost_title
import blockgames.composeapp.generated.resources.block_properties_heavy_desc
import blockgames.composeapp.generated.resources.block_properties_heavy_title
import blockgames.composeapp.generated.resources.block_properties_normal_desc
import blockgames.composeapp.generated.resources.block_properties_normal_title
import blockgames.composeapp.generated.resources.block_properties_row_clearer_desc
import blockgames.composeapp.generated.resources.block_properties_row_clearer_title
import blockgames.composeapp.generated.resources.block_properties_select_hint
import blockgames.composeapp.generated.resources.block_properties_title
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.blockStyleIconTint
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val BlockSampleSize = 78.dp
@Composable
fun BlockPropertiesScreen(
    modifier: Modifier = Modifier,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    onBack: () -> Unit,
) {
    LogScreen(telemetry, TelemetryScreenNames.BlockProperties)
    var selected by remember { mutableStateOf(SpecialBlockType.None) }
    val uiColors = BlockGamesThemeTokens.uiColors
    
    val stylePulse = LocalBlockStylePulse.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Card(
                modifier = Modifier,
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                colors = CardDefaults.cardColors(containerColor = uiColors.panel),
                border = BorderStroke(1.dp, uiColors.panelStroke),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.block_properties_title),
                        onClick = onBack,
                        size = 40.dp,
                        pulse = stylePulse,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.block_properties_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.block_properties_select_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = uiColors.subtitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Card(
                    modifier = Modifier
                        .width(132.dp),
                    shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                    colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted),
                    border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BlockSample(
                            tone = sampleToneFor(selected),
                            special = selected,
                            pulse = stylePulse,
                        )
                        BlockTitleAndDesc(special = selected)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpecialBlockType.entries.forEach { type ->
                        BlockTypeRow(
                            special = type,
                            selected = type == selected,
                            onClick = { selected = type },
                            pulse = stylePulse,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockTypeRow(
    special: SpecialBlockType,
    selected: Boolean,
    onClick: () -> Unit,
    pulse: Float = 0f,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val contentColor = blockStyleIconTint(style = settings.blockVisualStyle)
    val background = if (selected) {
        uiColors.panelHighlight
    } else {
        uiColors.panelMuted
    }
    val rowShape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner)

    Card(
        modifier = Modifier
            .fillMaxWidth()

            .clip(rowShape)
            .clickable(onClick = onClick),
        shape = rowShape,
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = if (selected) 0.92f else 0.68f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BlockMiniIcon(tone = sampleToneFor(special), special = special, pulse = pulse)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolveBlockTitle(special),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resolveBlockDesc(special),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.82f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BlockSample(
    tone: CellTone,
    special: SpecialBlockType,
    pulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    BlockCellPreview(
        tone = tone,
        palette = settings.blockColorPalette,
        style = settings.blockVisualStyle,
        size = BlockSampleSize,
        special = special,
        pulse = pulse,
    )
}

@Composable
private fun BlockMiniIcon(
    tone: CellTone,
    special: SpecialBlockType,
    pulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    BlockCellPreview(
        tone = tone,
        palette = settings.blockColorPalette,
        style = settings.blockVisualStyle,
        size = 28.dp,
        special = special,
        pulse = pulse,
    )
}

@Composable
private fun BlockTitleAndDesc(special: SpecialBlockType) {
    val settings = LocalAppSettings.current
    val contentColor = blockStyleIconTint(style = settings.blockVisualStyle)
    Text(
        text = resolveBlockTitle(special),
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = resolveBlockDesc(special),
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.82f),
    )
}

internal fun resolveBlockTitleRes(special: SpecialBlockType): StringResource = when (special) {
    SpecialBlockType.None -> Res.string.block_properties_normal_title
    SpecialBlockType.ColumnClearer -> Res.string.block_properties_column_clearer_title
    SpecialBlockType.RowClearer -> Res.string.block_properties_row_clearer_title
    SpecialBlockType.Ghost -> Res.string.block_properties_ghost_title
    SpecialBlockType.Heavy -> Res.string.block_properties_heavy_title
}

@Composable
private fun resolveBlockTitle(special: SpecialBlockType): String = stringResource(resolveBlockTitleRes(special))

internal fun resolveBlockDescRes(special: SpecialBlockType): StringResource = when (special) {
    SpecialBlockType.None -> Res.string.block_properties_normal_desc
    SpecialBlockType.ColumnClearer -> Res.string.block_properties_column_clearer_desc
    SpecialBlockType.RowClearer -> Res.string.block_properties_row_clearer_desc
    SpecialBlockType.Ghost -> Res.string.block_properties_ghost_desc
    SpecialBlockType.Heavy -> Res.string.block_properties_heavy_desc
}

@Composable
private fun resolveBlockDesc(special: SpecialBlockType): String = stringResource(resolveBlockDescRes(special))

internal fun sampleToneFor(special: SpecialBlockType): CellTone {
    return when (special) {
        SpecialBlockType.None -> CellTone.Cyan
        SpecialBlockType.ColumnClearer -> CellTone.Emerald
        SpecialBlockType.RowClearer -> CellTone.Gold
        SpecialBlockType.Ghost -> CellTone.Violet
        SpecialBlockType.Heavy -> CellTone.Coral
    }
}

@Preview(name = "Block Properties", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun BlockPropertiesScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        BlockPropertiesScreen(
            telemetry = NoOpAppTelemetry,
            onBack = {},
        )
    }
}
