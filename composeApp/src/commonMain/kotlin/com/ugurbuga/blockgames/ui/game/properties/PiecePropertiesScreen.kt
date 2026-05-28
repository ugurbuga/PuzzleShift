package com.ugurbuga.blockgames.ui.game.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.piece_properties_active
import blockgames.composeapp.generated.resources.piece_properties_cells
import blockgames.composeapp.generated.resources.piece_properties_kind
import blockgames.composeapp.generated.resources.piece_properties_none
import blockgames.composeapp.generated.resources.piece_properties_size
import blockgames.composeapp.generated.resources.piece_properties_special
import blockgames.composeapp.generated.resources.piece_properties_title
import blockgames.composeapp.generated.resources.piece_properties_unlock_level
import blockgames.composeapp.generated.resources.piece_size_format
import blockgames.composeapp.generated.resources.queue_empty
import blockgames.composeapp.generated.resources.special_column_clearer
import blockgames.composeapp.generated.resources.special_ghost
import blockgames.composeapp.generated.resources.special_heavy
import blockgames.composeapp.generated.resources.special_row_clearer
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PiecePropertiesScreen(
    modifier: Modifier = Modifier,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    activePiece: Piece?,
    onBack: () -> Unit,
) {
    LogScreen(telemetry, TelemetryScreenNames.PieceProperties)
    val uiColors = BlockGamesThemeTokens.uiColors
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.piece_properties_title),
                        onClick = onBack,
                        size = 40.dp,
                    )
                    Text(
                        text = stringResource(Res.string.piece_properties_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.piece_properties_active),
                        style = MaterialTheme.typography.labelLarge,
                        color = uiColors.subtitle,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted),
                border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (activePiece == null) {
                        Text(
                            text = stringResource(Res.string.queue_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        return@Column
                    }

                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_kind),
                        value = activePiece.kind.name,
                    )
                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_size),
                        value = stringResource(Res.string.piece_size_format, activePiece.width, activePiece.height),
                    )

                    val specialLabel = if (activePiece.special == SpecialBlockType.None) {
                        stringResource(Res.string.piece_properties_none)
                    } else {
                        resolveSpecialLabel(activePiece.special)
                    }
                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_special),
                        value = specialLabel,
                    )

                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_cells),
                        value = activePiece.cells.size.toString(),
                    )

                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_unlock_level),
                        value = activePiece.kind.unlockLevel.toString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = uiColors.subtitle,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.Medium,
    )
}

internal fun resolveSpecialLabelRes(type: SpecialBlockType): StringResource = when (type) {
    SpecialBlockType.None -> Res.string.piece_properties_none
    SpecialBlockType.ColumnClearer -> Res.string.special_column_clearer
    SpecialBlockType.RowClearer -> Res.string.special_row_clearer
    SpecialBlockType.Ghost -> Res.string.special_ghost
    SpecialBlockType.Heavy -> Res.string.special_heavy
}

@Composable
private fun resolveSpecialLabel(type: SpecialBlockType): String = stringResource(resolveSpecialLabelRes(type))

@Preview(name = "Piece Properties", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun PiecePropertiesScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        PiecePropertiesScreen(
            telemetry = NoOpAppTelemetry,
            activePiece = Piece(
                id = 1L,
                kind = PieceKind.T,
                tone = CellTone.Violet,
                cells = listOf(
                    GridPoint(0, 0),
                    GridPoint(1, 0),
                    GridPoint(2, 0),
                    GridPoint(1, 1),
                ),
                width = 3,
                height = 2,
                special = SpecialBlockType.RowClearer,
            ),
            onBack = {},
        )
    }
}

