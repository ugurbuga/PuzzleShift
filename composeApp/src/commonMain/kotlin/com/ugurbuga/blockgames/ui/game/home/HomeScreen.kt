package com.ugurbuga.blockgames.ui.game.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_banner_blocksort_bottom
import blockgames.composeapp.generated.resources.app_title_banner_blocksort_top
import blockgames.composeapp.generated.resources.app_title_banner_blockwise_bottom
import blockgames.composeapp.generated.resources.app_title_banner_blockwise_top
import blockgames.composeapp.generated.resources.app_title_banner_boomblocks_bottom
import blockgames.composeapp.generated.resources.app_title_banner_boomblocks_top
import blockgames.composeapp.generated.resources.app_title_banner_chainshift_bottom
import blockgames.composeapp.generated.resources.app_title_banner_chainshift_top
import blockgames.composeapp.generated.resources.app_title_banner_stackshift_bottom
import blockgames.composeapp.generated.resources.app_title_banner_stackshift_top
import blockgames.composeapp.generated.resources.high_score
import blockgames.composeapp.generated.resources.home_classic_cta
import blockgames.composeapp.generated.resources.home_time_attack_cta
import blockgames.composeapp.generated.resources.selection_switch_game
import blockgames.composeapp.generated.resources.settings_challenges
import blockgames.composeapp.generated.resources.settings_theme
import blockgames.composeapp.generated.resources.settings_tutorial
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.localization.appNameStringResource
import com.ugurbuga.blockgames.localization.appStringResource
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.NotificationManager
import com.ugurbuga.blockgames.platform.isDebugBuild
import com.ugurbuga.blockgames.platform.rememberNotificationManager
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.blockStyleIconTint
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.boardCellInsetDp
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.game.rememberBlockStylePulse
import com.ugurbuga.blockgames.ui.game.specialBlockIconTint
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.BlockGamesUiColors
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val HomeTitleBannerColumns = 6
private const val HomeTitleBannerRows = 2
private const val BlockSortHomeBannerColumns = 5
private const val BlockSortHomeBannerRows = 4

@Composable
fun HomeScreen(
    settings: AppSettings,
    classicHighScore: Int,
    timeAttackHighScore: Int,
    telemetry: AppTelemetry,
    onPlay: () -> Unit,
    onPlayTimeAttack: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenChallenges: () -> Unit,
    onSwitchGame: () -> Unit,
    notificationManager: NotificationManager,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val stylePulseTransition = rememberInfiniteTransition(label = "homeStylePulse")
    val stylePulse by stylePulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "homeStylePulseValue",
    )

    LogScreen(telemetry, TelemetryScreenNames.Home)

    notificationManager.RequestPermission()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.screenGradientTop,
                            uiColors.screenGradientBottom,
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = appNameStringResource(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                HomeTitleBanner(
                    settings = settings,
                    pulse = stylePulse,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.78f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeQuickActionButton(
                            text = appStringResource(Res.string.home_classic_cta),
                            icon = Icons.Filled.PlayArrow,
                            tone = CellTone.Cyan,
                            settings = settings,
                            pulse = stylePulse,
                            modifier = Modifier.weight(1f),
                            onClick = onPlay,
                            iconSize = 44.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                        )

                        HomeQuickActionButton(
                            text = appStringResource(Res.string.home_time_attack_cta),
                            icon = Icons.Filled.Timer,
                            tone = CellTone.Amber,
                            settings = settings,
                            pulse = stylePulse,
                            modifier = Modifier.weight(1f),
                            onClick = onPlayTimeAttack,
                            iconSize = 44.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HomeHighScoreCard(
                    classicHighScore = classicHighScore,
                    timeAttackHighScore = timeAttackHighScore,
                    onClick = {
                        if (isDebugBuild()) {
                            notificationManager.sendTestNotification()
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_challenges),
                        icon = Icons.Default.EmojiEvents,
                        tone = CellTone.Emerald,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenChallenges,
                    )
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_tutorial),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tone = CellTone.Gold,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTutorial,
                    )
                    HomeQuickActionButton(
                        text = stringResource(Res.string.selection_switch_game),
                        icon = Icons.Default.SwapHoriz,
                        tone = CellTone.Blue,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onSwitchGame,
                    )
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_theme),
                        icon = Icons.Filled.Palette,
                        tone = CellTone.Violet,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTheme,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    when (GlobalPlatformConfig.gameplayStyle) {
        GameplayStyle.StackShift -> StackShiftHomeTitleBanner(settings, pulse, modifier)
        GameplayStyle.BlockWise -> BlockWiseHomeTitleBanner(settings, pulse, modifier)
        GameplayStyle.ChainShift -> ChainShiftHomeTitleBanner(settings, pulse, modifier)
        GameplayStyle.MergeShift -> MergeShiftHomeTitleBanner(settings, pulse, modifier)
        GameplayStyle.BoomBlocks -> BoomBlocksHomeTitleBanner(settings, pulse, modifier)
        GameplayStyle.BlockSort -> BlockSortHomeTitleBanner(settings, pulse, modifier)
    }
}

@Composable
private fun ChainShiftHomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val topWord = stringResource(Res.string.app_title_banner_chainshift_top)
    val bottomWord = stringResource(Res.string.app_title_banner_chainshift_bottom)
    val accent = uiColors.selectionMergeShift
    Card(
        modifier = modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = 10.dp,
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.18f + (pulse * 0.08f)),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = topWord,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = bottomWord,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BlockSortHomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val bannerMotionTransition = rememberInfiniteTransition(label = "blockSortBannerMotion")
    val sequenceClock by bannerMotionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blockSortBannerSequenceClock",
    )
    val phase = normalizedPhase(sequenceClock)
    val topWord = stringResource(Res.string.app_title_banner_blocksort_top)
    val bottomWord = stringResource(Res.string.app_title_banner_blocksort_bottom)
    val moves = remember { blockSortHomeBannerMoves() }
    val moveCount = moves.size.coerceAtLeast(1)
    val scaledPhase = phase * moveCount
    val activeMoveIndex = scaledPhase.toInt().coerceIn(0, moves.lastIndex)
    val localPhase = (scaledPhase - activeMoveIndex).coerceIn(0f, 0.9999f)
    val activeMove = moves[activeMoveIndex]
    val sourceTapProgress = segmentProgress(localPhase, 0.00f, 0.18f)
    val handTravelProgress = segmentProgress(localPhase, 0.18f, 0.42f)
    val targetTapProgress = segmentProgress(localPhase, 0.42f, 0.60f)
    val pieceMoveProgress = segmentProgress(localPhase, 0.60f, 0.92f)
    val completedMoves = activeMoveIndex + if (localPhase >= 0.92f) 1 else 0
    val baseColumns = remember(topWord, bottomWord, activeMoveIndex) {
        blockSortHomeBannerColumns(
            topWord = topWord,
            bottomWord = bottomWord,
            completedMoves = activeMoveIndex,
        )
    }
    val displayedColumns = remember(topWord, bottomWord, baseColumns, activeMove, localPhase, completedMoves) {
        when {
            localPhase in 0.60f..0.92f -> baseColumns.mapIndexed { columnIndex, columnCells ->
                if (columnIndex == activeMove.sourceColumn) {
                    columnCells.toMutableList().apply { this[activeMove.sourceRow] = null }
                } else {
                    columnCells
                }
            }

            else -> blockSortHomeBannerColumns(
                topWord = topWord,
                bottomWord = bottomWord,
                completedMoves = completedMoves,
            )
        }
    }
    val sourceHighlight = when {
        localPhase < 0.18f -> sourceTapProgress
        localPhase < 0.42f -> 1f - handTravelProgress
        else -> 0f
    }
    val targetHighlight = when {
        localPhase < 0.18f -> 0f
        localPhase < 0.42f -> handTravelProgress
        localPhase < 0.60f -> targetTapProgress
        localPhase < 0.92f -> 1f - pieceMoveProgress
        else -> 0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = 10.dp,
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.82f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 3.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.20f),
                            uiColors.success.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        ) {
            val layout = remember(maxWidth, maxHeight) {
                calculateBlockSortHomeBannerLayout(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            val sourceCellTopLeft = layout.cellTopLeft(column = activeMove.sourceColumn, row = activeMove.sourceRow)
            val targetCellTopLeft = layout.cellTopLeft(column = activeMove.targetColumn, row = activeMove.targetRow)
            val movingCellX = lerpDp(sourceCellTopLeft.x, targetCellTopLeft.x, pieceMoveProgress)
            val movingCellY = lerpDp(sourceCellTopLeft.y, targetCellTopLeft.y, pieceMoveProgress)
            val handAlpha = if (localPhase < 0.60f) 1f else 0f
            val handPosition = when {
                localPhase < 0.18f -> sourceCellTopLeft
                localPhase < 0.42f -> androidx.compose.ui.unit.DpOffset(
                    x = lerpDp(sourceCellTopLeft.x, targetCellTopLeft.x, handTravelProgress),
                    y = lerpDp(sourceCellTopLeft.y, targetCellTopLeft.y, handTravelProgress),
                )
                else -> targetCellTopLeft
            }
            val tapCompression = maxOf(sourceTapProgress, targetTapProgress)

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier.size(layout.boardWidth, layout.boardHeight),
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.spacedBy(layout.columnGap),
                    ) {
                        displayedColumns.forEachIndexed { columnIndex, columnCells ->
                            BlockSortHomeTitleBannerColumn(
                                cells = columnCells,
                                settings = settings,
                                pulse = pulse,
                                slotSize = layout.slotSize,
                                cellGap = layout.cellGap,
                                sourceHighlight = if (columnIndex == activeMove.sourceColumn) sourceHighlight else 0f,
                                targetHighlight = if (columnIndex == activeMove.targetColumn) targetHighlight else 0f,
                                modifier = Modifier.size(layout.columnWidth, layout.columnHeight),
                            )
                        }
                    }

                    if (localPhase in 0.60f..0.92f) {
                        HomeTitleAnimatedCell(
                            letter = null,
                            tone = activeMove.tone,
                            settings = settings,
                            pulse = pulse * 0.7f,
                            alpha = 1f,
                            modifier = Modifier
                                .size(layout.slotSize)
                                .offset(x = movingCellX, y = movingCellY),
                        )
                    }

                    if (sourceTapProgress > 0.01f) {
                        HomeTitleTapEffect(
                            x = sourceCellTopLeft.x,
                            y = sourceCellTopLeft.y,
                            size = layout.slotSize,
                            progress = sourceTapProgress,
                            color = uiColors.launchGlow,
                        )
                    }

                    if (targetTapProgress > 0.01f) {
                        HomeTitleTapEffect(
                            x = targetCellTopLeft.x,
                            y = targetCellTopLeft.y,
                            size = layout.slotSize,
                            progress = targetTapProgress,
                            color = uiColors.guideAccent,
                        )
                    }

                    if (handAlpha > 0.01f) {
                        val isDark = isBlockGamesDarkTheme(settings)
                        val handColor = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
                        HomeTitleDemoHand(
                            x = handPosition.x,
                            y = handPosition.y,
                            size = layout.slotSize * (1f - (tapCompression * 0.18f)),
                            alpha = handAlpha,
                            color = handColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockSortHomeTitleBannerColumn(
    cells: List<HomeTitleCell?>,
    settings: AppSettings,
    pulse: Float,
    slotSize: Dp,
    cellGap: Dp,
    sourceHighlight: Float,
    targetHighlight: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val sourceStrength = sourceHighlight.coerceIn(0f, 1f)
    val targetStrength = targetHighlight.coerceIn(0f, 1f)
    val highlightStrength = maxOf(sourceStrength, targetStrength)
    val highlightColor = if (sourceStrength >= targetStrength) uiColors.launchGlow else uiColors.guideAccent
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.lerp(
                uiColors.gameSurface.copy(alpha = 0.84f),
                highlightColor.copy(alpha = 0.18f),
                highlightStrength * 0.42f,
            ),
        ),
        border = BorderStroke(
            width = if (highlightStrength > 0.01f) 1.5.dp else 1.dp,
            color = androidx.compose.ui.graphics.lerp(
                uiColors.panelStroke.copy(alpha = 0.72f),
                highlightColor.copy(alpha = 0.82f),
                highlightStrength,
            ),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(cellGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            cells.forEach { cell ->
                if (cell == null) {
                    HomeTitleEmptyCell(
                        settings = settings,
                        pulse = pulse,
                        modifier = Modifier.size(slotSize),
                    )
                } else {
                    HomeTitleAnimatedCell(
                        letter = cell.letter,
                        tone = cell.tone,
                        settings = settings,
                        pulse = pulse,
                        alpha = 1f,
                        modifier = Modifier.size(slotSize),
                    )
                }
            }
        }
    }
}

private data class BlockSortHomeBannerLayout(
    val slotSize: Dp,
    val cellGap: Dp,
    val columnGap: Dp,
    val columnWidth: Dp,
    val columnHeight: Dp,
    val boardWidth: Dp,
    val boardHeight: Dp,
) {
    fun cellTopLeft(column: Int, row: Int): androidx.compose.ui.unit.DpOffset {
        return androidx.compose.ui.unit.DpOffset(
            x = ((columnWidth + columnGap) * column) + 6.dp,
            y = 8.dp + ((slotSize + cellGap) * row),
        )
    }
}

private fun calculateBlockSortHomeBannerLayout(
    maxWidth: Dp,
    maxHeight: Dp,
): BlockSortHomeBannerLayout {
    val columnGap = 10.dp
    val cellGap = 6.dp
    val horizontalPadding = 12.dp
    val verticalPadding = 0.dp
    val widthBasedSlot = (
        maxWidth - horizontalPadding - (columnGap * (BlockSortHomeBannerColumns - 1)) - (6.dp * 2 * BlockSortHomeBannerColumns)
    ) / BlockSortHomeBannerColumns
    val heightBasedSlot = (
        maxHeight - verticalPadding - (cellGap * (BlockSortHomeBannerRows - 1)) - (8.dp * 2)
    ) / BlockSortHomeBannerRows
    val slotSize = minOf(widthBasedSlot, heightBasedSlot).coerceAtLeast(12.dp)
    val columnWidth = slotSize + (6.dp * 2)
    val columnHeight = (slotSize * BlockSortHomeBannerRows) + (cellGap * (BlockSortHomeBannerRows - 1)) + (8.dp * 2)
    val boardWidth = (columnWidth * BlockSortHomeBannerColumns) + (columnGap * (BlockSortHomeBannerColumns - 1))
    return BlockSortHomeBannerLayout(
        slotSize = slotSize,
        cellGap = cellGap,
        columnGap = columnGap,
        columnWidth = columnWidth,
        columnHeight = columnHeight,
        boardWidth = boardWidth,
        boardHeight = columnHeight,
    )
}

internal fun blockSortHomeBannerColumns(
    topWord: String,
    bottomWord: String,
    completedMoves: Int,
): List<List<HomeTitleCell?>> {
    val columns: MutableList<MutableList<HomeTitleCell?>> = mutableListOf(
        mutableListOf<HomeTitleCell?>(
            HomeTitleCell(tone = CellTone.Gold),
            HomeTitleCell(tone = CellTone.Blue),
            HomeTitleCell(letter = topWord.getOrNull(0)?.toString(), tone = CellTone.Cyan),
            HomeTitleCell(letter = bottomWord.getOrNull(0)?.toString(), tone = CellTone.Blue),
        ),
        mutableListOf<HomeTitleCell?>(
            null,
            HomeTitleCell(tone = CellTone.Gold),
            HomeTitleCell(letter = topWord.getOrNull(1)?.toString(), tone = CellTone.Gold),
            HomeTitleCell(letter = bottomWord.getOrNull(1)?.toString(), tone = CellTone.Lime),
        ),
        mutableListOf<HomeTitleCell?>(
            HomeTitleCell(tone = CellTone.Violet),
            HomeTitleCell(tone = CellTone.Emerald),
            HomeTitleCell(letter = topWord.getOrNull(2)?.toString(), tone = CellTone.Violet),
            HomeTitleCell(letter = bottomWord.getOrNull(2)?.toString(), tone = CellTone.Amber),
        ),
        mutableListOf<HomeTitleCell?>(
            null,
            HomeTitleCell(tone = CellTone.Violet),
            HomeTitleCell(letter = topWord.getOrNull(3)?.toString(), tone = CellTone.Emerald),
            HomeTitleCell(letter = bottomWord.getOrNull(3)?.toString(), tone = CellTone.Rose),
        ),
        mutableListOf<HomeTitleCell?>(
            HomeTitleCell(tone = CellTone.Blue),
            HomeTitleCell(tone = CellTone.Coral),
            HomeTitleCell(letter = topWord.getOrNull(4)?.toString(), tone = CellTone.Gold),
            HomeTitleCell(letter = bottomWord.getOrNull(4)?.toString(), tone = CellTone.Cyan),
        ),
    )

    blockSortHomeBannerMoves().take(completedMoves.coerceAtLeast(0)).forEach { move ->
        val movedCell = columns[move.sourceColumn][move.sourceRow]
        columns[move.sourceColumn][move.sourceRow] = null
        columns[move.targetColumn][move.targetRow] = movedCell
    }

    return columns.map { it.toList() }
}

internal fun blockSortHomeBannerMoves(): List<BlockSortHomeBannerMove> = listOf(
    BlockSortHomeBannerMove(sourceColumn = 0, sourceRow = 0, targetColumn = 1, targetRow = 0, tone = CellTone.Gold),
    BlockSortHomeBannerMove(sourceColumn = 2, sourceRow = 0, targetColumn = 3, targetRow = 0, tone = CellTone.Violet),
    BlockSortHomeBannerMove(sourceColumn = 4, sourceRow = 0, targetColumn = 0, targetRow = 0, tone = CellTone.Blue),
)

internal data class BlockSortHomeBannerMove(
    val sourceColumn: Int,
    val sourceRow: Int,
    val targetColumn: Int,
    val targetRow: Int,
    val tone: CellTone,
)

@Composable
private fun HomeTitleTapEffect(
    x: Dp,
    y: Dp,
    size: Dp,
    progress: Float,
    color: Color,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .graphicsLayer {
                alpha = 0.28f * (1f - (clamped * 0.35f))
                scaleX = 0.92f + (0.18f * clamped)
                scaleY = 0.92f + (0.18f * clamped)
            }
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.26f)),
    )
}

@Composable
private fun BoomBlocksHomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val columns = 6
    val rows = 4

    val bannerMotionTransition = rememberInfiniteTransition(label = "boomBlocksBannerMotion")
    val sequenceClock by bannerMotionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "boomBlocksBannerSequenceClock",
    )
    val sequencePhase = normalizedPhase(sequenceClock)

    // Sequential Explosions: TL, BR, TR, BL
    // Each phase takes 0.25 of the total timeline
    val p1 = segmentProgress(sequencePhase, 0.00f, 0.25f)
    val p2 = segmentProgress(sequencePhase, 0.25f, 0.50f)
    val p3 = segmentProgress(sequencePhase, 0.50f, 0.75f)
    val p4 = segmentProgress(sequencePhase, 0.75f, 1.00f)

    // Sub-phases for each sequence (relative to each 0.25 segment)
    fun subMove(p: Float) = segmentProgress(p, 0.00f, 0.48f)
    fun subTap(p: Float) = segmentProgress(p, 0.48f, 0.60f)
    fun subExplode(p: Float) = segmentProgress(p, 0.60f, 0.80f)
    fun subSettle(p: Float) = segmentProgress(p, 0.80f, 1.00f)

    val topWord = stringResource(Res.string.app_title_banner_boomblocks_top)
    val bottomWord = stringResource(Res.string.app_title_banner_boomblocks_bottom)

    val tones = listOf(
        CellTone.Cyan,
        CellTone.Gold,
        CellTone.Violet,
        CellTone.Emerald,
        CellTone.Coral,
        CellTone.Blue
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = 10.dp,
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.82f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(6f / 4.2f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.22f),
                            uiColors.launchGlow.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        ) {
            val cellWidth = maxWidth / columns
            val boardCellHeight = cellWidth

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .height(boardCellHeight)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (c in 0 until columns) {
                            val letter = when (r) {
                                1 -> if (c in 1..4) topWord[c - 1].toString() else null
                                2 -> if (c in 0..5) bottomWord[c].toString() else null
                                else -> null
                            }

                            val tone = when (r) {
                                0 -> if (c < 3) tones[0] else tones[1]
                                3 -> if (c < 3) tones[2] else tones[3]
                                1 -> when (c) {
                                    0 -> tones[(0 + 2) % tones.size] // Swapped with Row 2, Col 0
                                    2 -> tones[(3 + 4) % tones.size] // Swapped with Col 3
                                    3 -> tones[(2 + 4) % tones.size] // Swapped with Col 2
                                    else -> tones[(c + 4) % tones.size]
                                }
                                2 -> when (c) {
                                    0 -> tones[(0 + 4) % tones.size] // Swapped with Row 1, Col 0
                                    else -> tones[(c + 2) % tones.size]
                                }
                                else -> tones[0]
                            }

                            // Explosion logic for each quadrant
                            val isTL = r == 0 && c < 3
                            val isBR = r == 3 && c >= 3
                            val isTR = r == 0 && c >= 3
                            val isBL = r == 3 && c < 3

                            val (cellAlpha, cellOffsetY) = when {
                                isTL -> {
                                    val alpha = if (p1 < 0.60f) 1f else if (p1 < 0.80f) 1f - subExplode(p1) else subSettle(p1)
                                    val y = if (p1 >= 0.80f) lerpDp(-boardCellHeight * 2, 0.dp, subSettle(p1)) else 0.dp
                                    alpha to y
                                }
                                isBR -> {
                                    val alpha = if (p2 < 0.60f) 1f else if (p2 < 0.80f) 1f - subExplode(p2) else subSettle(p2)
                                    val y = if (p2 >= 0.80f) lerpDp(boardCellHeight * 3, 0.dp, subSettle(p2)) else 0.dp
                                    alpha to y
                                }
                                isTR -> {
                                    val alpha = if (p3 < 0.60f) 1f else if (p3 < 0.80f) 1f - subExplode(p3) else subSettle(p3)
                                    val y = if (p3 >= 0.80f) lerpDp(-boardCellHeight * 2, 0.dp, subSettle(p3)) else 0.dp
                                    alpha to y
                                }
                                isBL -> {
                                    val alpha = if (p4 < 0.60f) 1f else if (p4 < 0.80f) 1f - subExplode(p4) else subSettle(p4)
                                    val y = if (p4 >= 0.80f) lerpDp(boardCellHeight * 3, 0.dp, subSettle(p4)) else 0.dp
                                    alpha to y
                                }
                                else -> 1f to 0.dp
                            }

                            HomeTitleAnimatedCell(
                                letter = letter,
                                tone = tone,
                                settings = settings,
                                pulse = pulse,
                                alpha = cellAlpha,
                                modifier = Modifier.size(boardCellHeight),
                                offsetY = cellOffsetY
                            )
                        }
                    }
                }
            }

            // Flaş efektleri
            if (subExplode(p1) in 0.01f..0.99f) {
                HomeTitleRowClearOverlay(
                    alpha = (1f - subExplode(p1)) * 0.6f,
                    rowIndex = 0,
                    cellHeight = boardCellHeight,
                    modifier = Modifier.matchParentSize().padding(end = maxWidth / 2)
                )
            }
            if (subExplode(p2) in 0.01f..0.99f) {
                HomeTitleRowClearOverlay(
                    alpha = (1f - subExplode(p2)) * 0.6f,
                    rowIndex = 3,
                    cellHeight = boardCellHeight,
                    modifier = Modifier.matchParentSize().padding(start = maxWidth / 2)
                )
            }
            if (subExplode(p3) in 0.01f..0.99f) {
                HomeTitleRowClearOverlay(
                    alpha = (1f - subExplode(p3)) * 0.6f,
                    rowIndex = 0,
                    cellHeight = boardCellHeight,
                    modifier = Modifier.matchParentSize().padding(start = maxWidth / 2)
                )
            }
            if (subExplode(p4) in 0.01f..0.99f) {
                HomeTitleRowClearOverlay(
                    alpha = (1f - subExplode(p4)) * 0.6f,
                    rowIndex = 3,
                    cellHeight = boardCellHeight,
                    modifier = Modifier.matchParentSize().padding(end = maxWidth / 2)
                )
            }

            // El İkonu Animasyonu
            val (handX, handY, handAlpha, handP) = when {
                sequencePhase < 0.25f -> {
                    val x = lerpDp(maxWidth * 0.8f, cellWidth * 1.5f, subMove(p1))
                    val y = lerpDp(maxHeight, boardCellHeight * 0.5f, subMove(p1))
                    val alpha = if (p1 < 0.60f) 1f else 0f
                    val tap = subTap(p1)
                    val pVal = if (p1 in 0.48f..0.60f) tap else 0f
                    listOf(x, y, alpha, pVal)
                }
                sequencePhase < 0.50f -> {
                    val x = lerpDp(cellWidth * 1.5f, cellWidth * 4.5f, subMove(p2))
                    val y = lerpDp(boardCellHeight * 0.5f, boardCellHeight * 3.5f, subMove(p2))
                    val alpha = if (p2 < 0.60f) 1f else 0f
                    val tap = subTap(p2)
                    val pVal = if (p2 in 0.48f..0.60f) tap else 0f
                    listOf(x, y, alpha, pVal)
                }
                sequencePhase < 0.75f -> {
                    val x = lerpDp(cellWidth * 4.5f, cellWidth * 4.5f, subMove(p3))
                    val y = lerpDp(boardCellHeight * 3.5f, boardCellHeight * 0.5f, subMove(p3))
                    val alpha = if (p3 < 0.60f) 1f else 0f
                    val tap = subTap(p3)
                    val pVal = if (p3 in 0.48f..0.60f) tap else 0f
                    listOf(x, y, alpha, pVal)
                }
                else -> {
                    val x = lerpDp(cellWidth * 4.5f, cellWidth * 1.5f, subMove(p4))
                    val y = lerpDp(boardCellHeight * 0.5f, boardCellHeight * 3.5f, subMove(p4))
                    val alpha = if (p4 < 0.60f) 1f else 0f
                    val tap = subTap(p4)
                    val pVal = if (p4 in 0.48f..0.60f) tap else 0f
                    listOf(x, y, alpha, pVal)
                }
            }

            if ((handAlpha as Float) > 0f) {
                val isDark = isBlockGamesDarkTheme(settings)
                val handColor = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
                val tapScale = 1f - ((handP as Float) * 0.25f)

                HomeTitleDemoHand(
                    x = handX as Dp,
                    y = handY as Dp,
                    size = boardCellHeight * tapScale,
                    alpha = handAlpha,
                    color = handColor,
                )
            }
        }
    }
}


@Composable
private fun StackShiftHomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val bannerMotionTransition = rememberInfiniteTransition(label = "stackShiftBannerMotion")
    val sequenceClock by bannerMotionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stackShiftBannerSequenceClock",
    )
    val sequencePhase = normalizedPhase(sequenceClock)

    val lowerDrift = segmentProgress(sequencePhase, 0.00f, 0.10f)
    val lowerAlign = segmentProgress(sequencePhase, 0.10f, 0.15f)
    val lowerLaunch = segmentProgress(sequencePhase, 0.15f, 0.20f)
    val lowerExplode = segmentProgress(sequencePhase, 0.25f, 0.32f)

    val upperDrift = segmentProgress(sequencePhase, 0.34f, 0.44f)
    val upperAlign = segmentProgress(sequencePhase, 0.44f, 0.49f)
    val upperLaunch = segmentProgress(sequencePhase, 0.49f, 0.54f)
    val upperExplode = segmentProgress(sequencePhase, 0.59f, 0.66f)

    val stackAlign = segmentProgress(sequencePhase, 0.68f, 0.74f)
    val stackLaunch = segmentProgress(sequencePhase, 0.74f, 0.82f)

    val shiftAlign = segmentProgress(sequencePhase, 0.82f, 0.88f)
    val shiftLaunch = segmentProgress(sequencePhase, 0.88f, 0.96f)

    val topRow = rememberHomeTitleRow(
        word = stringResource(Res.string.app_title_banner_stackshift_top),
        startColumn = 0
    )
    val bottomRow = rememberHomeTitleRow(
        word = stringResource(Res.string.app_title_banner_stackshift_bottom),
        startColumn = 1
    )

    val topRowAlpha = titleTopRowAlpha(
        phase = sequencePhase,
        upperExplode = upperExplode,
        stackLaunch = stackLaunch,
    )
    val bottomRowAlpha = titleBottomRowAlpha(
        phase = sequencePhase,
        lowerExplode = lowerExplode,
        shiftLaunch = shiftLaunch,
    )

    HomeTitleBannerLayout(
        modifier = modifier,
        settings = settings,
        pulse = pulse,
        uiColors = uiColors,
        topRow = topRow,
        bottomRow = bottomRow,
        sequencePhase = sequencePhase,
        topRowAlpha = topRowAlpha,
        bottomRowAlpha = bottomRowAlpha,
        lowerExplode = lowerExplode,
        upperExplode = upperExplode,
        bottomTargetX = 1.dp // We will multiply by cellWidth inside layout
    ) { dockCellSize, dockPieceY, boardCellHeight, dockSingleStartX, dockSingleLeftX, dockSingleRightX, dockWordStartX, topTargetX, topGapTargetX, bottomTargetX, bottomGapTargetX ->

        if (sequencePhase < 0.15f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Gold)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockSingleStartX, dockSingleLeftX, lowerDrift).let { x ->
                        if (sequencePhase > 0.10f) lerpDp(x, bottomGapTargetX, lowerAlign) else x
                    },
                    y = dockPieceY,
                ),
            )
        } else if (sequencePhase < 0.25f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Gold)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = bottomGapTargetX,
                    y = lerpDp(dockPieceY, boardCellHeight, lowerLaunch),
                ),
            )
        } else if (sequencePhase < 0.32f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Gold)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier
                    .offset(x = bottomGapTargetX, y = boardCellHeight)
                    .graphicsLayer { alpha = 1f - lowerExplode },
            )
        }

        if (sequencePhase in 0.34f..0.49f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Violet)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockSingleStartX, dockSingleRightX, upperDrift).let { x ->
                        if (sequencePhase > 0.44f) lerpDp(x, topGapTargetX, upperAlign) else x
                    },
                    y = dockPieceY,
                ),
            )
        } else if (sequencePhase in 0.49f..0.59f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Violet)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = topGapTargetX,
                    y = lerpDp(dockPieceY, 0.dp, upperLaunch),
                ),
            )
        } else if (sequencePhase in 0.59f..0.66f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Violet)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier
                    .offset(x = topGapTargetX, y = 0.dp)
                    .graphicsLayer { alpha = 1f - upperExplode },
            )
        }

        if (sequencePhase in 0.68f..0.82f) {
            HomeTitleAnimatedPiece(
                cells = topRow.filterNotNull(),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockWordStartX, topTargetX, stackAlign),
                    y = lerpDp(dockPieceY, 0.dp, stackLaunch),
                ),
            )
        }

        if (sequencePhase in 0.82f..0.96f) {
            HomeTitleAnimatedPiece(
                cells = bottomRow.filterNotNull(),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockWordStartX, bottomTargetX, shiftAlign),
                    y = lerpDp(dockPieceY, boardCellHeight, shiftLaunch),
                ),
            )
        }

        val handAlpha = when {
            sequencePhase < 0.15f -> 1f
            sequencePhase in 0.34f..0.49f -> 1f
            sequencePhase in 0.68f..0.74f -> 1f
            sequencePhase in 0.82f..0.88f -> 1f
            else -> 0f
        }

        if (handAlpha > 0f) {
            val isDark = isBlockGamesDarkTheme(settings)
            val handColor = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
            val handX = when {
                sequencePhase < 0.15f -> lerpDp(
                    dockSingleStartX,
                    dockSingleLeftX,
                    lowerDrift
                ).let { x ->
                    if (sequencePhase > 0.10f) lerpDp(x, bottomGapTargetX, lowerAlign) else x
                }

                sequencePhase < 0.49f -> lerpDp(
                    dockSingleStartX,
                    dockSingleRightX,
                    upperDrift
                ).let { x ->
                    if (sequencePhase > 0.44f) lerpDp(x, topGapTargetX, upperAlign) else x
                }

                sequencePhase < 0.74f -> lerpDp(
                    dockWordStartX,
                    topTargetX,
                    stackAlign
                ) + (dockCellSize * 2f)

                else -> lerpDp(dockWordStartX, bottomTargetX, shiftAlign) + (dockCellSize * 2f)
            }
            HomeTitleDemoHand(
                x = handX,
                y = dockPieceY,
                size = dockCellSize,
                alpha = handAlpha,
                color = handColor,
            )
        }
    }
}

@Composable
private fun BlockWiseHomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val bannerMotionTransition = rememberInfiniteTransition(label = "blockWiseBannerMotion")
    val sequenceClock by bannerMotionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blockWiseBannerSequenceClock",
    )
    val sequencePhase = normalizedPhase(sequenceClock)

    val lowerLaunch = segmentProgress(sequencePhase, 0.00f, 0.20f)
    val lowerExplode = segmentProgress(sequencePhase, 0.25f, 0.32f)

    val upperLaunch = segmentProgress(sequencePhase, 0.34f, 0.54f)
    val upperExplode = segmentProgress(sequencePhase, 0.59f, 0.66f)

    val blockWordTravel = segmentProgress(sequencePhase, 0.68f, 0.82f)
    val wiseWordTravel = segmentProgress(sequencePhase, 0.82f, 0.96f)

    val topRow = rememberHomeTitleRow(
        word = stringResource(Res.string.app_title_banner_blockwise_top),
        startColumn = 0
    )
    val bottomRow = rememberHomeTitleRow(
        word = stringResource(Res.string.app_title_banner_blockwise_bottom),
        startColumn = 2
    )

    val topRowAlpha = if (sequencePhase in 0.66f..0.74f) 0f else if (sequencePhase in 0.74f..0.82f) blockWordTravel else 1f
    val bottomRowAlpha = if (sequencePhase in 0.32f..0.88f) 0f else if (sequencePhase in 0.88f..0.96f) wiseWordTravel else 1f

    val blockWiseBottomGapCells = remember {
        listOf(
            HomeTitleCell(tone = CellTone.Blue),
            HomeTitleCell(tone = CellTone.Lime),
        )
    }

    HomeTitleBannerLayout(
        modifier = modifier,
        settings = settings,
        pulse = pulse,
        uiColors = uiColors,
        topRow = topRow,
        bottomRow = bottomRow,
        sequencePhase = sequencePhase,
        topRowAlpha = topRowAlpha,
        bottomRowAlpha = bottomRowAlpha,
        lowerExplode = lowerExplode,
        upperExplode = upperExplode,
        bottomTargetX = 0.dp
    ) { dockCellSize, dockPieceY, boardCellHeight, _, _, _, _, topTargetX, topGapTargetX, bottomTargetX, bottomGapTargetX ->
        val dockDoubleStartX = (maxWidth - (dockCellSize * 2f)) / 2f
        val dockSingleStartX = (maxWidth - dockCellSize) / 2f
        val dockWordStartX = (maxWidth - (dockCellSize * 5f)) / 2f
        val dockBlockwiseBottomWordStartX = (maxWidth - (dockCellSize * HomeTitleBannerColumns)) / 2f

        if (sequencePhase < 0.20f) {
            HomeTitleAnimatedPiece(
                cells = blockWiseBottomGapCells,
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockDoubleStartX, bottomGapTargetX, lowerLaunch),
                    y = lerpDp(dockPieceY, boardCellHeight, lowerLaunch),
                ),
            )
        } else if (sequencePhase < 0.25f) {
            HomeTitleAnimatedPiece(
                cells = blockWiseBottomGapCells,
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(x = bottomGapTargetX, y = boardCellHeight),
            )
        } else if (sequencePhase < 0.32f) {
            HomeTitleAnimatedPiece(
                cells = blockWiseBottomGapCells,
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier
                    .offset(x = bottomGapTargetX, y = boardCellHeight)
                    .graphicsLayer { alpha = 1f - lowerExplode },
            )
        }

        if (sequencePhase in 0.34f..0.54f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Violet)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockSingleStartX, topGapTargetX, upperLaunch),
                    y = lerpDp(dockPieceY, 0.dp, upperLaunch),
                ),
            )
        } else if (sequencePhase in 0.54f..0.59f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Violet)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(x = topGapTargetX, y = 0.dp),
            )
        } else if (sequencePhase in 0.59f..0.66f) {
            HomeTitleAnimatedPiece(
                cells = listOf(HomeTitleCell(tone = CellTone.Violet)),
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier
                    .offset(x = topGapTargetX, y = 0.dp)
                    .graphicsLayer { alpha = 1f - upperExplode },
            )
        }

        if (sequencePhase in 0.68f..0.82f) {
            HomeTitleAnimatedPiece(
                cells = topRow,
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockWordStartX, topTargetX, blockWordTravel),
                    y = lerpDp(dockPieceY, 0.dp, blockWordTravel),
                ),
            )
        }

        if (sequencePhase in 0.82f..0.96f) {
            HomeTitleAnimatedPiece(
                cells = bottomRow,
                settings = settings,
                pulse = pulse,
                cellSize = dockCellSize,
                modifier = Modifier.offset(
                    x = lerpDp(dockBlockwiseBottomWordStartX, bottomTargetX, wiseWordTravel),
                    y = lerpDp(dockPieceY, boardCellHeight, wiseWordTravel),
                ),
            )
        }

        val handAlpha = when {
            sequencePhase < 0.20f -> 1f
            sequencePhase in 0.34f..0.54f -> 1f
            sequencePhase in 0.68f..0.96f -> 1f
            else -> 0f
        }

        if (handAlpha > 0f) {
            val isDark = isBlockGamesDarkTheme(settings)
            val handColor = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
            val (handX, handY) = when {
                sequencePhase < 0.20f -> {
                    val x = lerpDp(dockDoubleStartX, bottomGapTargetX, lowerLaunch) + homeTitleOccupiedCenterOffset(blockWiseBottomGapCells, dockCellSize)
                    val y = lerpDp(dockPieceY, boardCellHeight, lowerLaunch)
                    x to y
                }
                sequencePhase < 0.54f -> {
                    val x = lerpDp(dockSingleStartX, topGapTargetX, upperLaunch)
                    val y = lerpDp(dockPieceY, 0.dp, upperLaunch)
                    x to y
                }
                sequencePhase < 0.82f -> {
                    val x = lerpDp(dockWordStartX, topTargetX, blockWordTravel) + homeTitleOccupiedCenterOffset(topRow, dockCellSize)
                    val y = lerpDp(dockPieceY, 0.dp, blockWordTravel)
                    x to y
                }
                else -> {
                    val x = lerpDp(dockBlockwiseBottomWordStartX, bottomTargetX, wiseWordTravel) + homeTitleOccupiedCenterOffset(bottomRow, dockCellSize)
                    val y = lerpDp(dockPieceY, boardCellHeight, wiseWordTravel)
                    x to y
                }
            }
            HomeTitleDemoHand(
                x = handX,
                y = handY,
                size = dockCellSize,
                alpha = handAlpha,
                color = handColor,
            )
        }
    }
}

@Composable
private fun MergeShiftHomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val bannerMotionTransition = rememberInfiniteTransition(label = "mergeShiftBannerMotion")
    val sequenceClock by bannerMotionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mergeShiftBannerSequenceClock",
    )
    val sequencePhase = normalizedPhase(sequenceClock)

    // Demo phases (4 launches, all to bottom row)
    val p1Drift = segmentProgress(sequencePhase, 0.00f, 0.10f)
    val p1Align = segmentProgress(sequencePhase, 0.10f, 0.15f)
    val p1Launch = segmentProgress(sequencePhase, 0.15f, 0.20f)

    val p2Drift = segmentProgress(sequencePhase, 0.25f, 0.35f)
    val p2Align = segmentProgress(sequencePhase, 0.35f, 0.40f)
    val p2Launch = segmentProgress(sequencePhase, 0.40f, 0.45f)

    val p3Drift = segmentProgress(sequencePhase, 0.50f, 0.60f)
    val p3Align = segmentProgress(sequencePhase, 0.60f, 0.65f)
    val p3Launch = segmentProgress(sequencePhase, 0.65f, 0.70f)

    val p4Drift = segmentProgress(sequencePhase, 0.75f, 0.85f)
    val p4Align = segmentProgress(sequencePhase, 0.85f, 0.90f)
    val p4Launch = segmentProgress(sequencePhase, 0.90f, 0.95f)

    val topRow = remember {
        listOf(
            HomeTitleCell("256", CellTone.Gold),
            HomeTitleCell("4", CellTone.Amber),
            HomeTitleCell("32", CellTone.Emerald),
            HomeTitleCell("64", CellTone.Blue),
            HomeTitleCell("16", CellTone.Violet),
            HomeTitleCell("8", CellTone.Rose),
        )
    }
    val bottomRow = remember {
        listOf(
            HomeTitleCell("64", CellTone.Blue),
            null,
            null, // Merge Target A (Col 2)
            null, // Merge Target B (Col 3)
            null,
            HomeTitleCell("2", CellTone.Cyan),
        )
    }

    HomeTitleBannerLayout(
        modifier = modifier,
        settings = settings,
        pulse = pulse,
        uiColors = uiColors,
        topRow = topRow,
        bottomRow = bottomRow,
        sequencePhase = sequencePhase,
        topRowAlpha = 1f,
        bottomRowAlpha = 1f,
        lowerExplode = 0f,
        upperExplode = 0f,
        bottomTargetX = 0.dp
    ) { dockCellSize, dockPieceY, boardCellHeight, dockSingleStartX, dockSingleLeftX, dockSingleRightX, _, _, _, _, _ ->

        // Target A (Row 1, Col 2)
        HomeTitleAnimatedCell(
            letter = when {
                sequencePhase < 0.20f -> "2"
                sequencePhase < 0.45f -> "4"
                else -> "8"
            },
            tone = when {
                sequencePhase < 0.20f -> CellTone.Cyan
                sequencePhase < 0.45f -> CellTone.Amber
                else -> CellTone.Rose
            },
            settings = settings,
            pulse = when {
                sequencePhase in 0.20f..0.25f -> pulse * 2.5f
                sequencePhase in 0.45f..0.50f -> pulse * 2.5f
                else -> pulse
            },
            alpha = 1f,
            modifier = Modifier.size(dockCellSize).offset(x = dockCellSize * 2f, y = boardCellHeight)
        )

        // Target B (Row 1, Col 3)
        HomeTitleAnimatedCell(
            letter = when {
                sequencePhase < 0.70f -> "8"
                sequencePhase < 0.95f -> "16"
                else -> "32"
            },
            tone = when {
                sequencePhase < 0.70f -> CellTone.Rose
                sequencePhase < 0.95f -> CellTone.Violet
                else -> CellTone.Emerald
            },
            settings = settings,
            pulse = when {
                sequencePhase in 0.70f..0.75f -> pulse * 2.5f
                sequencePhase in 0.95f..1.0f -> pulse * 2.5f
                else -> pulse
            },
            alpha = 1f,
            modifier = Modifier.size(dockCellSize).offset(x = dockCellSize * 3f, y = boardCellHeight)
        )

        // Launching Pieces (all to bottom row)
        // P1: "2" launches to Col 2
        if (sequencePhase < 0.20f) {
            val targetX = dockCellSize * 2f
            val x = if (sequencePhase < 0.15f) {
                lerpDp(dockSingleStartX, dockSingleLeftX, p1Drift).let { xpos ->
                    if (sequencePhase > 0.10f) lerpDp(xpos, targetX, p1Align) else xpos
                }
            } else targetX
            val y = if (sequencePhase >= 0.15f) lerpDp(dockPieceY, boardCellHeight, p1Launch) else dockPieceY

            HomeTitleAnimatedCell(
                letter = "2",
                tone = CellTone.Cyan,
                settings = settings,
                pulse = pulse,
                alpha = 1f,
                modifier = Modifier.size(dockCellSize).offset(x = x, y = y)
            )
        }

        // P2: "4" launches to Col 2
        if (sequencePhase in 0.25f..0.45f) {
            val targetX = dockCellSize * 2f
            val x = if (sequencePhase < 0.40f) {
                lerpDp(dockSingleStartX, dockSingleLeftX, p2Drift).let { xpos ->
                    if (sequencePhase > 0.35f) lerpDp(xpos, targetX, p2Align) else xpos
                }
            } else targetX
            val y = if (sequencePhase >= 0.40f) lerpDp(dockPieceY, boardCellHeight, p2Launch) else dockPieceY

            HomeTitleAnimatedCell(
                letter = "4",
                tone = CellTone.Amber,
                settings = settings,
                pulse = pulse,
                alpha = 1f,
                modifier = Modifier.size(dockCellSize).offset(x = x, y = y)
            )
        }

        // P3: "8" launches to Col 3
        if (sequencePhase in 0.50f..0.70f) {
            val targetX = dockCellSize * 3f
            val x = if (sequencePhase < 0.65f) {
                lerpDp(dockSingleStartX, dockSingleRightX, p3Drift).let { xpos ->
                    if (sequencePhase > 0.60f) lerpDp(xpos, targetX, p3Align) else xpos
                }
            } else targetX
            val y = if (sequencePhase >= 0.65f) lerpDp(dockPieceY, boardCellHeight, p3Launch) else dockPieceY

            HomeTitleAnimatedCell(
                letter = "8",
                tone = CellTone.Rose,
                settings = settings,
                pulse = pulse,
                alpha = 1f,
                modifier = Modifier.size(dockCellSize).offset(x = x, y = y)
            )
        }

        // P4: "16" launches to Col 3
        if (sequencePhase in 0.75f..0.95f) {
            val targetX = dockCellSize * 3f
            val x = if (sequencePhase < 0.90f) {
                lerpDp(dockSingleStartX, dockSingleRightX, p4Drift).let { xpos ->
                    if (sequencePhase > 0.85f) lerpDp(xpos, targetX, p4Align) else xpos
                }
            } else targetX
            val y = if (sequencePhase >= 0.90f) lerpDp(dockPieceY, boardCellHeight, p4Launch) else dockPieceY

            HomeTitleAnimatedCell(
                letter = "16",
                tone = CellTone.Violet,
                settings = settings,
                pulse = pulse,
                alpha = 1f,
                modifier = Modifier.size(dockCellSize).offset(x = x, y = y)
            )
        }

        val handAlpha = when {
            sequencePhase < 0.15f -> 1f
            sequencePhase in 0.25f..0.40f -> 1f
            sequencePhase in 0.50f..0.65f -> 1f
            sequencePhase in 0.75f..0.90f -> 1f
            else -> 0f
        }

        if (handAlpha > 0f) {
            val isDark = isBlockGamesDarkTheme(settings)
            val handColor = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
            val handX = when {
                sequencePhase < 0.15f -> lerpDp(dockSingleStartX, dockSingleLeftX, p1Drift).let { xpos ->
                    if (sequencePhase > 0.10f) lerpDp(xpos, dockCellSize * 2f, p1Align) else xpos
                }
                sequencePhase < 0.40f -> lerpDp(dockSingleStartX, dockSingleLeftX, p2Drift).let { xpos ->
                    if (sequencePhase > 0.35f) lerpDp(xpos, dockCellSize * 2f, p2Align) else xpos
                }
                sequencePhase < 0.65f -> lerpDp(dockSingleStartX, dockSingleRightX, p3Drift).let { xpos ->
                    if (sequencePhase > 0.60f) lerpDp(xpos, dockCellSize * 3f, p3Align) else xpos
                }
                else -> lerpDp(dockSingleStartX, dockSingleRightX, p4Drift).let { xpos ->
                    if (sequencePhase > 0.85f) lerpDp(xpos, dockCellSize * 3f, p4Align) else xpos
                }
            }
            HomeTitleDemoHand(
                x = handX,
                y = dockPieceY,
                size = dockCellSize,
                alpha = handAlpha,
                color = handColor,
            )
        }
    }
}


@Composable
private fun HomeTitleBannerLayout(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    pulse: Float,
    uiColors: BlockGamesUiColors,
    topRow: List<HomeTitleCell?>,
    bottomRow: List<HomeTitleCell?>,
    sequencePhase: Float,
    topRowAlpha: Float,
    bottomRowAlpha: Float,
    lowerExplode: Float,
    upperExplode: Float,
    bottomTargetX: Dp,
    content: @Composable BoxWithConstraintsScope.(
        dockCellSize: Dp,
        dockPieceY: Dp,
        boardCellHeight: Dp,
        dockSingleStartX: Dp,
        dockSingleLeftX: Dp,
        dockSingleRightX: Dp,
        dockWordStartX: Dp,
        topTargetX: Dp,
        topGapTargetX: Dp,
        bottomTargetX: Dp,
        bottomGapTargetX: Dp
    ) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = 10.dp,
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.82f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(6f / 3.8f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.22f),
                            uiColors.launchGlow.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        ) {
            val cellWidth = maxWidth / HomeTitleBannerColumns
            val boardHeight = cellWidth * HomeTitleBannerRows
            val boardCellHeight = cellWidth
            val dockTop = boardHeight + 16.dp
            val dockHeight = maxHeight - dockTop
            val dockCellSize = boardCellHeight
            val dockSingleStartX = (maxWidth - dockCellSize) / 2f
            val dockSingleLeftX = maxWidth * 0.12f
            val dockSingleRightX = maxWidth - dockCellSize - (maxWidth * 0.12f)
            val dockWordStartX = (maxWidth - (dockCellSize * 5f)) / 2f
            val dockPieceY = dockTop + ((dockHeight - dockCellSize) / 2f)
            val topTargetX = 0.dp
            val topGapTargetX = cellWidth * 5f
            val actualBottomTargetX = bottomTargetX * cellWidth.value
            val bottomGapTargetX = 0.dp

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .height(boardHeight)
                        .fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.matchParentSize()) {
                        Row(
                            modifier = Modifier
                                .height(boardCellHeight)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            topRow.forEach { cell ->
                                val cellModifier = Modifier.size(boardCellHeight)
                                if (cell == null) {
                                    HomeTitleEmptyCell(
                                        settings = settings,
                                        pulse = pulse,
                                        modifier = cellModifier,
                                    )
                                } else {
                                    HomeTitleAnimatedCell(
                                        letter = cell.letter,
                                        tone = cell.tone,
                                        settings = settings,
                                        pulse = pulse,
                                        alpha = topRowAlpha,
                                        modifier = cellModifier,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .height(boardCellHeight)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            bottomRow.forEach { cell ->
                                val cellModifier = Modifier.size(boardCellHeight)
                                if (cell == null) {
                                    HomeTitleEmptyCell(
                                        settings = settings,
                                        pulse = pulse,
                                        modifier = cellModifier,
                                    )
                                } else {
                                    HomeTitleAnimatedCell(
                                        letter = cell.letter,
                                        tone = cell.tone,
                                        settings = settings,
                                        pulse = pulse,
                                        alpha = bottomRowAlpha,
                                        modifier = cellModifier,
                                    )
                                }
                            }
                        }
                    }

                    HomeTitleRowClearOverlay(
                        alpha = clearFlashAlpha(lowerExplode) * if (sequencePhase in 0.25f..0.32f) 1f else 0f,
                        rowIndex = 1,
                        cellHeight = boardCellHeight,
                        modifier = Modifier.matchParentSize(),
                    )
                    HomeTitleRowClearOverlay(
                        alpha = clearFlashAlpha(upperExplode) * if (sequencePhase in 0.59f..0.66f) 1f else 0f,
                        rowIndex = 0,
                        cellHeight = boardCellHeight,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dockHeight),
                ) {
                    HomeTitleMiniDock(
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            this.content(
                dockCellSize,
                dockPieceY,
                boardCellHeight,
                dockSingleStartX,
                dockSingleLeftX,
                dockSingleRightX,
                dockWordStartX,
                topTargetX,
                topGapTargetX,
                actualBottomTargetX,
                bottomGapTargetX
            )
        }
    }
}

@Composable
private fun HomeTitleDemoHand(
    x: Dp,
    y: Dp,
    size: Dp,
    alpha: Float,
    color: Color,
) {
    Icon(
        imageVector = Icons.Default.TouchApp,
        contentDescription = null,
        tint = color.copy(alpha = 0.72f * alpha),
        modifier = Modifier
            .offset(x = x + (size * 0.4f), y = y + (size * 0.6f))
            .size(size * 0.8f)
            .graphicsLayer {
                rotationZ = -15f
            },
    )
}

internal data class HomeTitleCell(
    val letter: String? = null,
    val tone: CellTone,
)

@Composable
private fun rememberHomeTitleRow(
    word: String,
    startColumn: Int,
): List<HomeTitleCell?> {
    val tones = if (startColumn == 0) {
        listOf(CellTone.Cyan, CellTone.Gold, CellTone.Violet, CellTone.Emerald, CellTone.Coral)
    } else {
        listOf(CellTone.Blue, CellTone.Lime, CellTone.Amber, CellTone.Rose, CellTone.Cyan)
    }
    return List(HomeTitleBannerColumns) { index ->
        val letterIndex = index - startColumn
        if (letterIndex in word.indices) {
            HomeTitleCell(
                letter = word[letterIndex].toString(),
                tone = tones[letterIndex],
            )
        } else {
            null
        }
    }
}

internal fun segmentProgress(
    phase: Float,
    start: Float,
    end: Float,
): Float = ((phase - start) / (end - start)).coerceIn(0f, 1f)

internal fun clearFlashAlpha(progress: Float): Float = 1f - abs((progress * 2f) - 1f)

internal fun titleTopRowAlpha(
    phase: Float,
    upperExplode: Float,
    stackLaunch: Float,
): Float = when {
    phase < 0.59f -> 1f
    phase < 0.66f -> 1f - upperExplode
    phase < 0.74f -> 0f
    phase < 0.82f -> stackLaunch
    else -> 1f
}

internal fun titleBottomRowAlpha(
    phase: Float,
    lowerExplode: Float,
    shiftLaunch: Float,
): Float = when {
    phase < 0.25f -> 1f
    phase < 0.32f -> 1f - lowerExplode
    phase < 0.88f -> 0f
    phase < 0.96f -> shiftLaunch
    else -> 1f
}

internal fun normalizedPhase(value: Float): Float {
    val remainder = value % 1f
    return if (remainder < 0f) remainder + 1f else remainder
}

private fun lerpDp(
    start: Dp,
    end: Dp,
    progress: Float,
): Dp = start + ((end - start) * progress.coerceIn(0f, 1f))

@Composable
private fun HomeTitleAnimatedCell(
    letter: String?,
    tone: CellTone,
    settings: AppSettings,
    pulse: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
) {
    if (alpha <= 0f) return
    val effectivePulse = rememberBlockStylePulse(
        style = settings.blockVisualStyle,
        pulse = pulse,
    )
    val letterTint = specialBlockIconTint(
        style = settings.blockVisualStyle,
        isDarkTheme = isBlockGamesDarkTheme(settings),
        palette = settings.blockColorPalette,
    )

    BoxWithConstraints(
        modifier = modifier.offset(x = offsetX, y = offsetY),
        contentAlignment = Alignment.Center,
    ) {
        val cellSize = maxWidth
        val letterFontSize = (cellSize.value * 0.40f).coerceIn(16f, 34f).sp
        BlockCellPreview(
            tone = tone,
            palette = settings.blockColorPalette,
            style = settings.blockVisualStyle,
            size = cellSize,
            alpha = alpha,
            pulse = effectivePulse,
        )
        if (letter != null) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = letterFontSize),
                color = letterTint.copy(alpha = alpha),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HomeTitleEmptyCell(
    settings: AppSettings,
    pulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val cellSize = maxWidth
        val corner =
            RoundedCornerShape(boardCellCornerRadiusDp(cellSize, settings.blockVisualStyle))
        val inset = boardCellInsetDp(cellSize) * 0.72f
        val pulseAlpha = if (settings.blockVisualStyle == BlockVisualStyle.DynamicLiquid) {
            0.10f + (0.15f * pulse)
        } else {
            0.20f
        }
        Box(
            modifier = Modifier
                .size(cellSize)
                .clip(corner)
                .background(uiColors.boardEmptyCellBorder.copy(alpha = pulseAlpha))
                .padding(inset),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(corner)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.boardEmptyCell.copy(alpha = 0.40f),
                                uiColors.boardEmptyCell.copy(alpha = 0.22f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun HomeTitleMiniDock(
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
            elevation = 8.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.68f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.launchGlow.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
                    .background(uiColors.panelMuted.copy(alpha = 0.48f))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.panelHighlight.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun HomeTitleAnimatedPiece(
    cells: List<HomeTitleCell?>,
    settings: AppSettings,
    pulse: Float,
    cellSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (cells.isEmpty()) return
    Row(modifier = modifier) {
        cells.forEach { cell ->
            if (cell == null) {
                Spacer(modifier = Modifier.size(cellSize))
            } else {
                HomeTitleAnimatedCell(
                    letter = cell.letter,
                    tone = cell.tone,
                    settings = settings,
                    pulse = pulse,
                    alpha = 1f,
                    modifier = Modifier.size(cellSize),
                )
            }
        }
    }
}

internal fun homeTitleOccupiedCenterOffset(
    cells: List<HomeTitleCell?>,
    cellSize: Dp,
): Dp {
    val occupiedIndices = cells.mapIndexedNotNull { index, cell -> index.takeIf { cell != null } }
    if (occupiedIndices.isEmpty()) return 0.dp
    val centerIndex = occupiedIndices.average().toFloat()
    return cellSize * centerIndex
}

@Composable
private fun HomeTitleRowClearOverlay(
    alpha: Float,
    rowIndex: Int,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f) return
    val uiColors = BlockGamesThemeTokens.uiColors
    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeight)
                .offset(y = cellHeight * rowIndex)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.warning.copy(alpha = 0.12f * alpha),
                            Color.White.copy(alpha = 0.36f * alpha),
                            uiColors.launchGlow.copy(alpha = 0.20f * alpha),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun HomeHighScoreCard(
    classicHighScore: Int,
    timeAttackHighScore: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Surface(
        modifier = modifier
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                elevation = 5.dp,
            )
            .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
            .clickable { onClick() },
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        color = uiColors.panelMuted.copy(alpha = 0.96f),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.76f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = appStringResource(Res.string.high_score),
                style = MaterialTheme.typography.labelMedium,
                color = uiColors.subtitle,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeHighScoreMetric(
                    title = appStringResource(Res.string.home_classic_cta),
                    value = classicHighScore.toString(),
                )
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 32.dp)
                        .height(32.dp)
                        .background(uiColors.panelStroke.copy(alpha = 0.48f)),
                )
                HomeHighScoreMetric(
                    title = appStringResource(Res.string.home_time_attack_cta),
                    value = timeAttackHighScore.toString(),
                )
            }
        }
    }
}

@Composable
private fun HomeHighScoreMetric(
    title: String,
    value: String,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeQuickActionButton(
    text: String,
    icon: ImageVector,
    tone: CellTone,
    settings: AppSettings,
    pulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val resolvedStyle = settings.blockVisualStyle
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val cellSize = maxWidth
        val cellInset = boardCellInsetDp(cellSize)
        val buttonShape = RoundedCornerShape(boardCellCornerRadiusDp(cellSize, resolvedStyle))
        val effectivePulse = rememberBlockStylePulse(
            style = resolvedStyle,
            pulse = pulse,
        )
        val contentColor = blockStyleIconTint(style = resolvedStyle)
        val isDark = isBlockGamesDarkTheme(settings)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(cellInset),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blockGamesSurfaceShadow(
                        shape = buttonShape,
                        elevation = 0.dp,
                    )
                    .clip(buttonShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cornerRadiusPx = boardCellCornerRadiusPx(
                        with(density) { cellSize.toPx() },
                        resolvedStyle
                    )
                    drawCellBody(
                        tone = tone,
                        palette = settings.blockColorPalette,
                        style = resolvedStyle,
                        isDark = isDark,
                        topLeft = Offset.Zero,
                        size = this.size,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        pulse = effectivePulse,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize),
                    )
                    Text(
                        text = text,
                        style = textStyle,
                        color = contentColor,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}


@Preview(name = "StackShift", showBackground = true)
@Composable
fun HomeScreenStackShiftPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.StackShift
    val settings = AppSettings(
        blockVisualStyle = BlockVisualStyle.Sharp3D
    )
    BlockGamesTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            classicHighScore = 1250,
            timeAttackHighScore = 860,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onPlayTimeAttack = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
            onOpenChallenges = {},
            onSwitchGame = {},
            notificationManager = rememberNotificationManager(),
        )
    }
}

@Preview(name = "BlockWise", showBackground = true)
@Composable
fun HomeScreenBlockWisePreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.BlockWise
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Bubble
    )
    BlockGamesTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            classicHighScore = 1250,
            timeAttackHighScore = 860,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onPlayTimeAttack = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
            onOpenChallenges = {},
            onSwitchGame = {},
            notificationManager = rememberNotificationManager(),
        )
    }
}

@Preview(name = "MergeShift", showBackground = true)
@Composable
fun HomeScreenMergeShiftPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.MergeShift
    val settings = AppSettings(
        blockVisualStyle = BlockVisualStyle.Cosmic
    )
    BlockGamesTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            classicHighScore = 1250,
            timeAttackHighScore = 860,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onPlayTimeAttack = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
            onOpenChallenges = {},
            onSwitchGame = {},
            notificationManager = rememberNotificationManager(),
        )
    }
}

@Preview(name = "BoomBlocks", showBackground = true)
@Composable
fun HomeScreenBoomBlocksPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.BoomBlocks
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Crystal
    )
    BlockGamesTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            classicHighScore = 1250,
            timeAttackHighScore = 860,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onPlayTimeAttack = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
            onOpenChallenges = {},
            onSwitchGame = {},
            notificationManager = rememberNotificationManager(),
        )
    }
}
