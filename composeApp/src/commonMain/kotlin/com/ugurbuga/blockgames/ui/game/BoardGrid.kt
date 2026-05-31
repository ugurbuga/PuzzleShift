package com.ugurbuga.blockgames.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.BoardCell
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.PressureLevel
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.boardSpecialIcon
import com.ugurbuga.blockgames.game.model.formatMergeValue
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.BlockGamesUiColors
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val DefaultBoardClearFlashDurationMillis = 420
private const val DefaultBoardShiftDurationMillis = 220
private const val CosmicStarCount = 20
private const val CosmicFieldSeed = 0x51A71C
private val BoardFrameInset = 1.dp

@Composable
fun BoardGrid(
    modifier: Modifier = Modifier,
    gameState: GameState,
    preview: PlacementPreview?,
    impactedPreviewCells: Set<GridPoint>,
    guidedColumns: Set<Int> = emptySet(),
    activeColumn: Int?,
    activePiece: Piece?,
    isDragging: Boolean,
    gameOverClearProgressProvider: () -> Float = { 0f },
    showClearFlash: Boolean = true,
    showPreviewClearGuides: Boolean = true,
    stylePulse: Float = 0f,
    clearFlashDurationMillis: Int = DefaultBoardClearFlashDurationMillis,
    boardShiftDurationMillis: Int = DefaultBoardShiftDurationMillis,
) {
    val settings = LocalAppSettings.current
    val colorScheme = MaterialTheme.colorScheme
    val uiColors = BlockGamesThemeTokens.uiColors
    val textMeasurer = rememberTextMeasurer()
    val boardBlockStyle = settings.blockVisualStyle
    val isDarkTheme = isBlockGamesDarkTheme(settings)
    val showDangerDecorations = gameState.status != GameStatus.GameOver
    val mergeShiftBoardFontFamily = MaterialTheme.typography.titleMedium.fontFamily
    val hasDangerPulse = showDangerDecorations && (
            gameState.criticalColumns.isNotEmpty() ||
                    gameState.columnPressure.any { pressure ->
                        pressure.level == PressureLevel.Critical || pressure.level == PressureLevel.Overflow
                    }
            )
    val hasImpactPulse =
        impactedPreviewCells.isNotEmpty() || (preview?.clearedRows?.isNotEmpty() == true) || (preview?.clearedColumns?.isNotEmpty() == true)
    var clearFlashAlpha by remember { mutableFloatStateOf(0f) }
    var clearFlashSweepProgress by remember { mutableFloatStateOf(1f) }
    var mergeAlpha by remember { mutableFloatStateOf(0f) }
    var mergeScale by remember { mutableFloatStateOf(1f) }
    var boardTransitionSource by remember { mutableStateOf(gameState.board) }
    var boardTransitionToken by remember { mutableLongStateOf(gameState.clearAnimationToken) }
    val boardShiftProgress = remember { Animatable(1f) }
    val shouldAnimateBoardShift =
        gameState.status == GameStatus.Running &&
                gameState.gameplayStyle == GameplayStyle.StackShift &&
                gameState.clearAnimationToken != boardTransitionToken
    val animatedBoardCells =
        remember(boardTransitionSource, gameState.board, shouldAnimateBoardShift) {
            if (shouldAnimateBoardShift) {
                buildAnimatedBoardCells(boardTransitionSource, gameState.board)
            } else {
                emptyList()
            }
        }
    val shouldRenderAnimatedBoardShift = shouldAnimateBoardShift && animatedBoardCells.isNotEmpty()
    val animatedBoardCellKeys = remember(animatedBoardCells) {
        animatedBoardCells.mapTo(mutableSetOf()) { animatedCell ->
            boardPointKey(animatedCell.point.column, animatedCell.point.row)
        }
    }

    val boardEffectsTransition = if (hasDangerPulse || hasImpactPulse) {
        rememberInfiniteTransition(label = "boardEffectsTransition")
    } else {
        null
    }
    val dangerPulseState = if (hasDangerPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 900,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "dangerPulseAlpha",
        )
    } else {
        remember { mutableFloatStateOf(0.2f) }
    }
    val dangerSweepState = if (hasDangerPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1600,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "dangerSweep",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val impactPulseState = if (hasImpactPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "impactPulse",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val impactSweepState = if (hasImpactPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 920,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "impactSweep",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val shouldAnimateStylePulse = stylePulse == 0f && (
            boardBlockStyle == BlockVisualStyle.DynamicLiquid ||
                    boardBlockStyle == BlockVisualStyle.Tornado ||
                    boardBlockStyle == BlockVisualStyle.Prism ||
                    boardBlockStyle == BlockVisualStyle.Flame
            )
    val stylePulseTransition = if (shouldAnimateStylePulse) {
        rememberInfiniteTransition(label = "stylePulse")
    } else {
        null
    }
    val stylePulseState = if (shouldAnimateStylePulse) {
        stylePulseTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "stylePulse",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    LaunchedEffect(gameState.clearAnimationToken) {
        if (!showClearFlash) {
            clearFlashAlpha = 0f
            clearFlashSweepProgress = 1f
            return@LaunchedEffect
        }
        if (gameState.recentlyClearedRows.isEmpty() && gameState.recentlyClearedColumns.isEmpty()) {
            if (gameState.recentlyMergedPoints.isNotEmpty()) {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    mergeAlpha = if (value < 0.5f) value * 2 else (1f - value) * 2
                    mergeScale = 1f + 0.3f * sin(value * PI.toFloat())
                }
            }
            return@LaunchedEffect
        }
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = clearFlashDurationMillis,
                easing = FastOutSlowInEasing
            ),
        ) { value, _ ->
            clearFlashSweepProgress = value
            clearFlashAlpha = 0.9f * (1f - value)
        }
    }

    LaunchedEffect(gameState.clearAnimationToken, gameState.status) {
        if (gameState.status != GameStatus.Running) {
            boardShiftProgress.snapTo(1f)
            boardTransitionSource = gameState.board
            boardTransitionToken = gameState.clearAnimationToken
            return@LaunchedEffect
        }

        if (!shouldRenderAnimatedBoardShift) {
            boardShiftProgress.snapTo(1f)
            boardTransitionSource = gameState.board
            boardTransitionToken = gameState.clearAnimationToken
            return@LaunchedEffect
        }

        boardShiftProgress.snapTo(0f)
        boardShiftProgress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = boardShiftDurationMillis,
                easing = FastOutSlowInEasing
            ),
        )
        boardTransitionSource = gameState.board
        boardTransitionToken = gameState.clearAnimationToken
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(BoardFrameInset)
            .clip(RoundedCornerShape(boardFrameCornerRadiusDp(boardBlockStyle)))
    ) {
        val density = LocalDensity.current
        val cellWidth = maxWidth / gameState.config.columns
        val cellHeight = maxHeight / gameState.config.rows
        val cellWidthPx = with(density) { cellWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }
        val boardCornerRadiusPx = with(density) { boardFrameCornerRadiusDp(boardBlockStyle).toPx() }
        val cellVisual = boardCellVisual(
            cellWidth = cellWidthPx,
            cellHeight = cellHeightPx,
            style = boardBlockStyle,
        )
        val emptySlotVisual = remember(cellWidthPx, cellHeightPx) {
            boardCellVisual(
                cellWidth = cellWidthPx,
                cellHeight = cellHeightPx,
                style = BlockVisualStyle.Flat,
            )
        }
        val hasCriticalColumns = gameState.criticalColumns.isNotEmpty()
        val pieceToneColor = remember(activePiece?.id, settings.blockColorPalette, isDarkTheme) {
            activePiece?.tone?.paletteColor(settings.blockColorPalette, isDarkTheme)
        }
        val effectivePreviewColor = pieceToneColor ?: uiColors.success
        val previewCellInsetPx = cellVisual.fillInsetPx
        val previewCellSize = remember(cellWidthPx, cellHeightPx, previewCellInsetPx) {
            Size(
                width = cellWidthPx - (previewCellInsetPx * 2),
                height = cellHeightPx - (previewCellInsetPx * 2),
            )
        }
        val previewCellInsetOffset = remember(previewCellInsetPx) {
            Offset(previewCellInsetPx, previewCellInsetPx)
        }
        val previewIconPadding = boardCellInsetDp(minOf(cellWidth, cellHeight))
        val previewAlpha = remember(isDarkTheme, isDragging) {
            when {
                isDarkTheme && isDragging -> 0.24f
                isDarkTheme -> 0.18f
                isDragging -> 0.28f
                else -> 0.24f
            }
        }
        val previewColumnBounds = remember(preview, cellHeightPx, cellVisual.previewInsetPx) {
            preview?.occupiedCells
                ?.groupBy(GridPoint::column)
                ?.mapValues { (_, points) ->
                    val highestOccupiedRow = points.minOf(GridPoint::row)
                    val lowestOccupiedRow = points.maxOf(GridPoint::row)
                    val top = (highestOccupiedRow * cellHeightPx) + cellVisual.previewInsetPx
                    val bottom =
                        ((lowestOccupiedRow + 1) * cellHeightPx) - cellVisual.previewInsetPx
                    top to bottom
                }
        }
        val highlightedColumns =
            remember(preview, activePiece?.id, activeColumn, gameState.config.columns) {
                preview?.coveredColumns ?: activePiece?.let { piece ->
                    activeColumn?.coerceIn(
                        0,
                        (gameState.config.columns - piece.width).coerceAtLeast(0)
                    )?.let { column ->
                        column..<(column + piece.width)
                    }
                }
            }
        val guideColumns = remember(guidedColumns, highlightedColumns) {
            guidedColumns.ifEmpty { highlightedColumns?.toSet().orEmpty() }
        }
        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
        val useStaticSlotLayer = clearProgress <= 0f && !hasCriticalColumns
        val currentBoardSpecialCells = remember(gameState.board) {
            buildList {
                for (row in 0 until gameState.board.rows) {
                    for (column in 0 until gameState.board.columns) {
                        val cell = gameState.board.cellAt(column, row) ?: continue
                        if (cell.special != SpecialBlockType.None) {
                            add(Triple(column, row, cell))
                        }
                    }
                }
            }
        }
        val animatedSpecialCells = remember(animatedBoardCells) {
            animatedBoardCells.filter { it.cell.special != SpecialBlockType.None }
        }

        BoardGridBackgroundLayer(
            uiColors = uiColors,
            boardDecorAlphaProvider = {
                (1f - clearProgress).coerceIn(0f, 1f)
            },
            boardCornerRadiusPx = boardCornerRadiusPx,
            modifier = Modifier.fillMaxSize(),
        )

        if (useStaticSlotLayer) {
            BoardGridSlotLayer(
                rows = gameState.config.rows,
                columns = gameState.config.columns,
                cellWidthPx = cellWidthPx,
                cellHeightPx = cellHeightPx,
                slotVisual = emptySlotVisual,
                uiColors = uiColors,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val clearRows = clearProgress * gameState.config.rows
            val fullyClearedRows = clearRows.toInt().coerceIn(0, gameState.config.rows)
            val partialClearAlpha = 1f - (clearRows - fullyClearedRows).coerceIn(0f, 1f)
            val boardDecorAlpha = (1f - clearProgress).coerceIn(0f, 1f)

            val dangerPulse = dangerPulseState.value
            val dangerSweep = dangerSweepState.value
            val impactPulse = impactPulseState.value
            val impactSweep = impactSweepState.value
            val stylePulseInternal = stylePulseState.value
            val effectiveStylePulse = if (stylePulse != 0f) stylePulse else stylePulseInternal

            val previewGuideAlpha = when {
                isDarkTheme && isDragging -> 0.12f
                isDarkTheme -> 0.10f
                isDragging -> 0.18f
                else -> 0.15f
            }
            val previewLineColor =
                effectivePreviewColor.copy(alpha = previewGuideAlpha * boardDecorAlpha)
            val previewTone = activePiece?.tone ?: CellTone.Cyan

            val previewBeamAlpha = when {
                isDarkTheme && isDragging -> 0.24f
                isDarkTheme -> 0.14f
                isDragging -> 0.30f
                else -> 0.22f
            }
            val cornerRadius = cellVisual.cornerRadius
            val boardWidthPx = size.width
            val boardHeightPx = size.height
            val boardFrameCornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx)

            highlightedColumns?.forEach { column ->
                val beamAlpha = previewBeamAlpha * boardDecorAlpha
                if (beamAlpha <= 0f || !showDangerDecorations) return@forEach
                val (barTop, barBottom) = previewColumnBounds?.get(column) ?: return@forEach
                val barHeight = (barBottom - barTop).coerceAtLeast(0f)
                if (barHeight <= 0f) return@forEach

                drawRoundRect(
                    color = previewLineColor,
                    topLeft = Offset(x = column * cellWidthPx, y = barTop),
                    size = Size(width = cellWidthPx, height = barHeight),
                    cornerRadius = cornerRadius,
                )
            }

            guideColumns.forEach { column ->
                if (column !in 0 until gameState.config.columns) return@forEach
                val (barTop, barBottom) = previewColumnBounds?.get(column) ?: return@forEach
                val barHeight = (barBottom - barTop).coerceAtLeast(0f)
                if (barHeight <= 0f) return@forEach
                drawRoundRect(
                    color = effectivePreviewColor.copy(alpha = 0.14f * boardDecorAlpha),
                    topLeft = Offset(x = column * cellWidthPx, y = barTop),
                    size = Size(width = cellWidthPx, height = barHeight),
                    cornerRadius = cornerRadius,
                )
            }

            if (showPreviewClearGuides) {
                preview?.clearedRows?.forEach { row ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Row,
                        index = row,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = effectivePreviewColor,
                        fillAlpha = (0.08f + 0.12f * impactPulse) * boardDecorAlpha,
                        outlineAlpha = (0.18f + 0.16f * impactPulse) * boardDecorAlpha,
                        sweepProgress = impactSweep,
                        sweepAlpha = (0.16f + 0.26f * impactPulse) * boardDecorAlpha,
                    )
                }

                preview?.clearedColumns?.forEach { column ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Column,
                        index = column,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = effectivePreviewColor,
                        fillAlpha = (0.08f + 0.12f * impactPulse) * boardDecorAlpha,
                        outlineAlpha = (0.18f + 0.16f * impactPulse) * boardDecorAlpha,
                        sweepProgress = impactSweep,
                        sweepAlpha = (0.16f + 0.26f * impactPulse) * boardDecorAlpha,
                    )
                }
            }

            gameState.columnPressure.forEach { pressure ->
                val alpha = when (pressure.level) {
                    PressureLevel.Calm -> 0f
                    PressureLevel.Warning -> 0.08f
                    PressureLevel.Critical -> dangerPulse
                    PressureLevel.Overflow -> dangerPulse + 0.12f
                }
                if (alpha <= 0f || !showDangerDecorations) return@forEach
                val color = when (pressure.level) {
                    PressureLevel.Warning -> uiColors.warning
                    PressureLevel.Critical -> uiColors.danger.copy(alpha = 0.92f)
                    PressureLevel.Overflow -> uiColors.danger
                    PressureLevel.Calm -> Color.Transparent
                }
                drawRoundRect(
                    color = color.copy(alpha = alpha.coerceAtMost(0.72f)),
                    topLeft = Offset(x = pressure.column * cellWidthPx, y = 0f),
                    size = Size(width = cellWidthPx, height = boardHeightPx),
                    cornerRadius = cornerRadius,
                )

                if (pressure.level == PressureLevel.Critical || pressure.level == PressureLevel.Overflow) {
                    val sweepTop = (boardHeightPx * dangerSweep) - cellHeightPx
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0f),
                                color.copy(alpha = (alpha * 0.35f).coerceAtMost(0.3f)),
                                color.copy(alpha = 0f),
                            ),
                        ),
                        topLeft = Offset(
                            x = pressure.column * cellWidthPx,
                            y = sweepTop.coerceIn(0f, boardHeightPx - cellHeightPx)
                        ),
                        size = Size(width = cellWidthPx, height = cellHeightPx * 1.6f),
                        cornerRadius = cornerRadius,
                    )
                }
            }

            if (hasCriticalColumns) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            uiColors.danger.copy(alpha = dangerPulse * boardDecorAlpha),
                            uiColors.warning.copy(alpha = (dangerPulse * 0.9f).coerceAtMost(0.65f) * boardDecorAlpha),
                            colorScheme.secondary.copy(
                                alpha = (dangerPulse * 0.8f).coerceAtMost(
                                    0.55f
                                ) * boardDecorAlpha
                            ),
                            uiColors.danger.copy(alpha = dangerPulse * boardDecorAlpha),
                        ),
                        start = Offset(x = boardWidthPx * dangerSweep, y = 0f),
                        end = Offset(x = boardWidthPx * (1f - dangerSweep), y = boardHeightPx),
                    ),
                    cornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx),
                    style = Stroke(width = 2.6f),
                )
            } else {
                drawRoundRect(
                    color = uiColors.boardOutlineGlow.copy(alpha = 0.48f * boardDecorAlpha),
                    cornerRadius = boardFrameCornerRadius,
                    style = Stroke(width = 4f),
                )
                drawRoundRect(
                    color = uiColors.boardOutline.copy(alpha = 0.44f * boardDecorAlpha),
                    cornerRadius = boardFrameCornerRadius,
                    style = Stroke(width = 2f),
                )
            }

            for (row in 0 until gameState.config.rows) {
                for (column in 0 until gameState.config.columns) {
                    val topLeft = Offset(x = column * cellWidthPx, y = row * cellHeightPx)
                    val cellSize = Size(width = cellWidthPx, height = cellHeightPx)
                    val rowFromBottom = (gameState.config.rows - 1 - row).coerceAtLeast(0)
                    val cellClearAlpha = when {
                        clearProgress <= 0f -> 1f
                        rowFromBottom < fullyClearedRows -> 0f
                        rowFromBottom == fullyClearedRows -> partialClearAlpha
                        else -> 1f
                    }
                    val renderedCellAlpha = cellClearAlpha * boardDecorAlpha
                    if (renderedCellAlpha <= 0f) continue
                    if (!useStaticSlotLayer) {
                        drawFlatBoardEmptySlot(
                            topLeft = topLeft,
                            cellSize = cellSize,
                            slotVisual = emptySlotVisual,
                            uiColors = uiColors,
                            alpha = renderedCellAlpha,
                        )
                        val gridLineColor = if (showDangerDecorations && hasCriticalColumns) {
                            uiColors.danger.copy(alpha = (0.10f + (dangerPulse * 0.26f)) * renderedCellAlpha)
                        } else {
                            uiColors.boardGridLine.copy(alpha = 0.12f * renderedCellAlpha)
                        }
                        drawRoundRect(
                            color = gridLineColor,
                            topLeft = topLeft,
                            size = cellSize,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1f),
                        )

                        if (showDangerDecorations && hasCriticalColumns) {
                            drawRoundRect(
                                color = uiColors.danger.copy(alpha = (0.04f + (dangerPulse * 0.18f)) * renderedCellAlpha),
                                topLeft = topLeft,
                                size = cellSize,
                                cornerRadius = cornerRadius,
                                style = Stroke(width = 1.4f),
                            )
                        }
                    }

                    if (!shouldRenderAnimatedBoardShift || boardPointKey(column, row) !in animatedBoardCellKeys) {
                        gameState.board.cellAt(column, row)?.let { cell ->
                            val isImpactedByPreview = GridPoint(column, row) in impactedPreviewCells
                            val isRecentlyMerged =
                                GridPoint(column, row) in gameState.recentlyMergedPoints
                            val scale = if (isRecentlyMerged) mergeScale else 1f

                            withTransform({
                                if (isRecentlyMerged) {
                                    scale(
                                        scale,
                                        scale,
                                        pivot = topLeft + Offset(
                                            cellWidthPx / 2f,
                                            cellHeightPx / 2f
                                        )
                                    )
                                }
                                if (isImpactedByPreview && gameState.gameplayStyle == GameplayStyle.MergeShift) {
                                    val pulseScale = 1f + (impactPulseState.value - 0.4f) * 0.25f
                                    scale(
                                        pulseScale,
                                        pulseScale,
                                        pivot = topLeft + Offset(
                                            cellWidthPx / 2f,
                                            cellHeightPx / 2f
                                        )
                                    )
                                }
                            }) {
                                drawCellBody(
                                    tone = cell.tone,
                                    palette = settings.blockColorPalette,
                                    style = boardBlockStyle,
                                    isDark = isDarkTheme,
                                    topLeft = topLeft + Offset(
                                        cellVisual.fillInsetPx,
                                        cellVisual.fillInsetPx
                                    ),
                                    size = Size(
                                        width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                                        height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                                    ),
                                    cornerRadius = cornerRadius,
                                    alpha = renderedCellAlpha,
                                    pulse = effectiveStylePulse,
                                )

                                if (gameState.gameplayStyle == GameplayStyle.MergeShift) {
                                    val text = formatMergeValue(cell.value)
                                    val fontSize = (minOf(cellWidthPx, cellHeightPx) * 0.44f)
                                    val textStyle = TextStyle(
                                        color = Color.White.copy(alpha = boardDecorAlpha),
                                        fontFamily = mergeShiftBoardFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = with(density) { fontSize.toSp() },
                                        textAlign = TextAlign.Center,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.75f * boardDecorAlpha),
                                            offset = Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                    val measuredText = textMeasurer.measure(text, textStyle)
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = text,
                                        style = textStyle,
                                        topLeft = topLeft + Offset(
                                            (cellWidthPx - measuredText.size.width) / 2f,
                                            (cellHeightPx - measuredText.size.height) / 2f
                                        )
                                    )
                                }

                                if (isRecentlyMerged && mergeAlpha > 0f) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.45f * mergeAlpha),
                                        topLeft = topLeft + Offset(
                                            cellVisual.fillInsetPx,
                                            cellVisual.fillInsetPx
                                        ),
                                        size = Size(
                                            width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                                            height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                                        ),
                                        cornerRadius = cornerRadius,
                                        style = Stroke(width = (4f * mergeAlpha).coerceAtLeast(1f))
                                    )
                                }
                            }

                            if (isImpactedByPreview && gameState.gameplayStyle != GameplayStyle.MergeShift) {
                                val warningInset =
                                    (cellVisual.previewInsetPx - (impactPulse * 0.8f)).coerceAtLeast(
                                        0.7f
                                    )
                                val diagonalInset =
                                    warningInset + (minOf(cellWidthPx, cellHeightPx) * 0.18f)
                                val crossStroke = 1.8f + (impactPulse * 1.8f)
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            uiColors.warning.copy(alpha = 0.22f + (impactPulse * 0.16f)),
                                            uiColors.danger.copy(alpha = 0.16f + (impactPulse * 0.22f)),
                                            Color.Transparent,
                                        ),
                                        center = topLeft + Offset(
                                            cellWidthPx / 2f,
                                            cellHeightPx / 2f
                                        ),
                                        radius = minOf(
                                            cellWidthPx,
                                            cellHeightPx
                                        ) * (0.98f + impactPulse * 0.18f),
                                    ),
                                    topLeft = topLeft,
                                    size = cellSize,
                                    cornerRadius = cornerRadius,
                                )
                                drawRoundRect(
                                    color = uiColors.danger.copy(alpha = (0.10f + impactPulse * 0.20f) * renderedCellAlpha),
                                    topLeft = topLeft + Offset(warningInset, warningInset),
                                    size = Size(
                                        width = cellWidthPx - (warningInset * 2),
                                        height = cellHeightPx - (warningInset * 2),
                                    ),
                                    cornerRadius = cornerRadius,
                                )
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            uiColors.warning.copy(alpha = 0.95f * renderedCellAlpha),
                                            uiColors.danger.copy(alpha = 1f * renderedCellAlpha),
                                        ),
                                        start = topLeft + Offset(cellWidthPx * impactSweep, 0f),
                                        end = topLeft + Offset(
                                            cellWidthPx * (1f - impactSweep),
                                            cellHeightPx
                                        ),
                                    ),
                                    topLeft = topLeft + Offset(warningInset, warningInset),
                                    size = Size(
                                        width = cellWidthPx - (warningInset * 2),
                                        height = cellHeightPx - (warningInset * 2),
                                    ),
                                    cornerRadius = cornerRadius,
                                    style = Stroke(width = 1.8f + impactPulse * 2.2f),
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.40f + impactPulse * 0.34f),
                                    start = topLeft + Offset(diagonalInset, diagonalInset),
                                    end = topLeft + Offset(
                                        cellWidthPx - diagonalInset,
                                        cellHeightPx - diagonalInset
                                    ),
                                    strokeWidth = crossStroke,
                                )
                                drawLine(
                                    color = uiColors.warning.copy(alpha = 0.42f + impactPulse * 0.32f),
                                    start = topLeft + Offset(
                                        cellWidthPx - diagonalInset,
                                        diagonalInset
                                    ),
                                    end = topLeft + Offset(
                                        diagonalInset,
                                        cellHeightPx - diagonalInset
                                    ),
                                    strokeWidth = crossStroke,
                                )
                            } else if (isImpactedByPreview && gameState.gameplayStyle == GameplayStyle.MergeShift) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = (0.2f + impactPulse * 0.4f) * renderedCellAlpha),
                                    topLeft = topLeft + Offset(
                                        cellVisual.fillInsetPx,
                                        cellVisual.fillInsetPx
                                    ),
                                    size = Size(
                                        width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                                        height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                                    ),
                                    cornerRadius = cornerRadius,
                                    style = Stroke(width = (2f + impactPulse * 4f).coerceAtLeast(1f))
                                )
                            }
                        }
                    }

                }
            }

            preview?.let { landing ->
                landing.occupiedCells.forEach { point ->
                    val topLeft = Offset(
                        x = point.column * cellWidthPx,
                        y = point.row * cellHeightPx,
                    )
                    drawCellBody(
                        tone = previewTone,
                        palette = settings.blockColorPalette,
                        style = boardBlockStyle,
                        isDark = isDarkTheme,
                        topLeft = topLeft + previewCellInsetOffset,
                        size = previewCellSize,
                        cornerRadius = cornerRadius,
                        alpha = previewAlpha * boardDecorAlpha,
                        pulse = effectiveStylePulse,
                    )

                    if (gameState.gameplayStyle == GameplayStyle.MergeShift && activePiece != null) {
                        val text = formatMergeValue(activePiece.value)
                        val fontSize = (minOf(cellWidthPx, cellHeightPx) * 0.44f)
                        val textStyle = TextStyle(
                            color = Color.White.copy(alpha = boardDecorAlpha),
                            fontFamily = mergeShiftBoardFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = with(density) { fontSize.toSp() },
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.75f * boardDecorAlpha),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                        val measuredText = textMeasurer.measure(text, textStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = text,
                            style = textStyle,
                            topLeft = topLeft + Offset(
                                (cellWidthPx - measuredText.size.width) / 2f,
                                (cellHeightPx - measuredText.size.height) / 2f
                            )
                        )
                    }
                }
            }

            if (showClearFlash && clearFlashAlpha > 0f) {
                gameState.recentlyClearedRows.forEach { row ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Row,
                        index = row,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = Color.White,
                        fillAlpha = clearFlashAlpha * boardDecorAlpha,
                        outlineAlpha = (clearFlashAlpha * 0.84f) * boardDecorAlpha,
                        sweepProgress = clearFlashSweepProgress,
                        sweepAlpha = (clearFlashAlpha * 0.9f) * boardDecorAlpha,
                    )
                }
                gameState.recentlyClearedColumns.forEach { column ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Column,
                        index = column,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = Color.White,
                        fillAlpha = clearFlashAlpha * boardDecorAlpha,
                        outlineAlpha = (clearFlashAlpha * 0.84f) * boardDecorAlpha,
                        sweepProgress = clearFlashSweepProgress,
                        sweepAlpha = (clearFlashAlpha * 0.9f) * boardDecorAlpha,
                    )
                }
            }

            if (shouldRenderAnimatedBoardShift) {
                animatedBoardCells.forEach { animatedCell ->
                    val sourceRow = animatedCell.sourceRow.toFloat()
                    val targetRow = animatedCell.point.row.toFloat()
                    val animatedRow =
                        sourceRow + ((targetRow - sourceRow) * boardShiftProgress.value)
                    val animatedTopLeft = Offset(
                        x = animatedCell.point.column * cellWidthPx,
                        y = animatedRow * cellHeightPx,
                    )
                    drawCellBody(
                        tone = animatedCell.cell.tone,
                        palette = settings.blockColorPalette,
                        style = boardBlockStyle,
                        isDark = isDarkTheme,
                        topLeft = animatedTopLeft + Offset(
                            cellVisual.fillInsetPx,
                            cellVisual.fillInsetPx
                        ),
                        size = Size(
                            width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                            height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                        ),
                        cornerRadius = cornerRadius,
                        alpha = boardDecorAlpha,
                        pulse = effectiveStylePulse,
                    )

                    if (gameState.gameplayStyle == GameplayStyle.MergeShift) {
                        val text = formatMergeValue(animatedCell.cell.value)
                        val fontSize = (minOf(cellWidthPx, cellHeightPx) * 0.44f)
                        val textStyle = TextStyle(
                            color = Color.White.copy(alpha = boardDecorAlpha),
                            fontFamily = mergeShiftBoardFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = with(density) { fontSize.toSp() },
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.75f * boardDecorAlpha),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                        val measuredText = textMeasurer.measure(text, textStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = text,
                            style = textStyle,
                            topLeft = animatedTopLeft + Offset(
                                (cellWidthPx - measuredText.size.width) / 2f,
                                (cellHeightPx - measuredText.size.height) / 2f
                            )
                        )
                    }
                }
            }
        }

        if (shouldRenderAnimatedBoardShift) {
            currentBoardSpecialCells.forEach { (column, row, cell) ->
                if (boardPointKey(column, row) in animatedBoardCellKeys) return@forEach
                BoardSpecialIconOverlay(
                    cell = cell,
                    column = column,
                    row = row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                )
            }
            animatedSpecialCells.forEach { animatedCell ->
                if (animatedCell.cell.special == SpecialBlockType.None) return@forEach
                BoardSpecialIconOverlay(
                    cell = animatedCell.cell,
                    column = animatedCell.point.column,
                    row = animatedCell.point.row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    translationYProvider = {
                        val sourceRow = animatedCell.sourceRow.toFloat()
                        val targetRow = animatedCell.point.row.toFloat()
                        val animatedRow =
                            sourceRow + ((targetRow - sourceRow) * boardShiftProgress.value)
                        (animatedRow - animatedCell.point.row) * cellHeightPx
                    },
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                )
            }
        } else {
            currentBoardSpecialCells.forEach { (column, row, cell) ->
                BoardSpecialIconOverlay(
                    cell = cell,
                    column = column,
                    row = row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                )
            }
        }

        val previewSpecial = activePiece?.special
        if (previewSpecial != null && previewSpecial != SpecialBlockType.None) {
            preview?.occupiedCells?.forEach { point ->
                BoardSpecialIconOverlay(
                    cell = BoardCell(tone = CellTone.Blue, special = previewSpecial),
                    column = point.column,
                    row = point.row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    contentPadding = previewIconPadding,
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                    iconAlpha = previewAlpha,
                    iconSizeRatio = 0.40f,
                )
            }
        }
    }
}

private enum class ClearEffectOrientation {
    Row,
    Column,
}

private fun DrawScope.drawClearEffectBand(
    orientation: ClearEffectOrientation,
    index: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    boardWidthPx: Float,
    boardHeightPx: Float,
    cornerRadius: CornerRadius,
    accentColor: Color,
    fillAlpha: Float,
    outlineAlpha: Float,
    sweepProgress: Float,
    sweepAlpha: Float,
) {
    when (orientation) {
        ClearEffectOrientation.Row -> {
            val top = index * cellHeightPx
            val bandSize = Size(width = boardWidthPx, height = cellHeightPx)
            val sweepWidth = maxOf(boardWidthPx * 0.28f, cellHeightPx * 1.8f)
            val sweepLeft = ((boardWidthPx + sweepWidth) * sweepProgress) - sweepWidth
            drawRoundRect(
                color = accentColor.copy(alpha = fillAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = 0f, y = top),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = (sweepAlpha * 0.84f).coerceAtMost(0.84f)),
                        accentColor.copy(alpha = sweepAlpha.coerceIn(0f, 0.92f)),
                        Color.Transparent,
                    ),
                    startX = sweepLeft,
                    endX = sweepLeft + sweepWidth,
                ),
                topLeft = Offset(x = 0f, y = top),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = outlineAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = 0f, y = top),
                size = bandSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.6f),
            )
        }

        ClearEffectOrientation.Column -> {
            val left = index * cellWidthPx
            val bandSize = Size(width = cellWidthPx, height = boardHeightPx)
            val sweepHeight = maxOf(boardHeightPx * 0.28f, cellWidthPx * 1.8f)
            val sweepTop = ((boardHeightPx + sweepHeight) * sweepProgress) - sweepHeight
            drawRoundRect(
                color = accentColor.copy(alpha = fillAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = left, y = 0f),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = (sweepAlpha * 0.84f).coerceAtMost(0.84f)),
                        accentColor.copy(alpha = sweepAlpha.coerceIn(0f, 0.92f)),
                        Color.Transparent,
                    ),
                    startY = sweepTop,
                    endY = sweepTop + sweepHeight,
                ),
                topLeft = Offset(x = left, y = 0f),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = outlineAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = left, y = 0f),
                size = bandSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.6f),
            )
        }
    }
}

@Composable
private fun BoardGridBackgroundLayer(
    uiColors: BlockGamesUiColors,
    boardDecorAlphaProvider: () -> Float,
    boardCornerRadiusPx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer { alpha = boardDecorAlphaProvider() }
    ) {
        val boardWidthPx = size.width
        val boardHeightPx = size.height
        val boardFrameCornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    uiColors.boardGradientTop.copy(alpha = 0.98f),
                    uiColors.gameSurface.copy(alpha = 0.94f),
                    uiColors.boardGradientBottom.copy(alpha = 0.98f),
                ),
            ),
            size = Size(boardWidthPx, boardHeightPx),
            cornerRadius = boardFrameCornerRadius,
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    uiColors.boardSignaturePrimary.copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                center = Offset(boardWidthPx * 0.22f, boardHeightPx * 0.14f),
                radius = boardWidthPx * 0.72f,
            ),
            size = Size(boardWidthPx, boardHeightPx),
            cornerRadius = boardFrameCornerRadius,
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    uiColors.boardSignatureSecondary.copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                center = Offset(boardWidthPx * 0.78f, boardHeightPx * 0.88f),
                radius = boardWidthPx * 0.84f,
            ),
            size = Size(boardWidthPx, boardHeightPx),
            cornerRadius = boardFrameCornerRadius,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.12f),
                ),
            ),
            size = Size(boardWidthPx, boardHeightPx),
            cornerRadius = boardFrameCornerRadius,
        )
    }
}

@Composable
private fun BoardGridSlotLayer(
    rows: Int,
    columns: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    slotVisual: BoardCellVisual,
    uiColors: BlockGamesUiColors,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cornerRadius = slotVisual.cornerRadius

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val topLeft = Offset(x = column * cellWidthPx, y = row * cellHeightPx)
                drawFlatBoardEmptySlot(
                    topLeft = topLeft,
                    cellSize = Size(cellWidthPx, cellHeightPx),
                    slotVisual = slotVisual,
                    uiColors = uiColors,
                )
                drawRoundRect(
                    color = uiColors.boardGridLine.copy(alpha = 0.12f),
                    topLeft = topLeft,
                    size = Size(cellWidthPx, cellHeightPx),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1f),
                )
            }
        }
    }
}

private fun DrawScope.drawFlatBoardEmptySlot(
    topLeft: Offset,
    cellSize: Size,
    slotVisual: BoardCellVisual,
    uiColors: BlockGamesUiColors,
    alpha: Float = 1f,
) {
    val slotInset = slotVisual.fillInsetPx * 0.72f
    val slotTopLeft = topLeft + Offset(slotInset, slotInset)
    val slotSize = Size(
        width = cellSize.width - (slotInset * 2),
        height = cellSize.height - (slotInset * 2),
    )
    val slotCornerRadius =
        CornerRadius(slotVisual.innerCornerRadiusPx, slotVisual.innerCornerRadiusPx)
    drawRoundRect(
        color = uiColors.boardEmptyCell.copy(alpha = 0.44f * alpha),
        topLeft = slotTopLeft,
        size = slotSize,
        cornerRadius = slotCornerRadius,
    )
    drawRoundRect(
        color = uiColors.boardEmptyCellBorder.copy(alpha = 0.16f * alpha),
        topLeft = slotTopLeft,
        size = slotSize,
        cornerRadius = slotCornerRadius,
        style = Stroke(width = 1f),
    )
}

@Composable
private fun BoardSpecialIconOverlay(
    cell: BoardCell,
    column: Int,
    row: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    translationX: Float = 0f,
    translationYProvider: () -> Float = { 0f },
    alphaProvider: () -> Float = { 1f },
    contentPadding: Dp = 2.dp,
    iconAlpha: Float = 1f,
    iconSizeRatio: Float = 0.36f,
) {
    Box(
        modifier = Modifier
            .offset(x = cellWidth * column, y = cellHeight * row)
            .size(cellWidth, cellHeight)
            .padding(contentPadding)
            .graphicsLayer {
                alpha = alphaProvider()
                this.translationX = translationX
                this.translationY = translationYProvider()
            },
        contentAlignment = Alignment.Center,
    ) {
        val settings = LocalAppSettings.current
        val isDarkTheme = isBlockGamesDarkTheme(settings)
        val resolvedBoardStyle = settings.blockVisualStyle
        Icon(
            imageVector = boardSpecialIcon(cell.special),
            contentDescription = null,
            tint = specialBlockIconTint(
                style = resolvedBoardStyle,
                isDarkTheme = isDarkTheme,
                palette = settings.blockColorPalette,
            ).copy(alpha = iconAlpha),
            modifier = Modifier.size(minOf(cellWidth, cellHeight) * iconSizeRatio),
        )
    }
}

private data class AnimatedBoardCell(
    val point: GridPoint,
    val cell: BoardCell,
    val sourceRow: Int,
)

private fun buildAnimatedBoardCells(
    previousBoard: BoardMatrix,
    currentBoard: BoardMatrix,
): List<AnimatedBoardCell> {
    val previousByColumnAndKey = buildMap {
        for (column in 0 until previousBoard.columns) {
            val grouped = mutableMapOf<Int, ArrayDeque<Int>>()
            for (row in 0 until previousBoard.rows) {
                val cell = previousBoard.cellAt(column, row) ?: continue
                val key = boardCellKey(cell)
                grouped.getOrPut(key) { ArrayDeque() }.addLast(row)
            }
            if (grouped.isNotEmpty()) put(column, grouped)
        }
    }

    return buildList {
        for (column in 0 until currentBoard.columns) {
            for (row in 0 until currentBoard.rows) {
                val cell = currentBoard.cellAt(column, row) ?: continue
                val key = boardCellKey(cell)
                val sourceRow = previousByColumnAndKey[column]?.get(key)?.removeFirstOrNull() ?: row
                if (sourceRow == row) continue
                add(
                    AnimatedBoardCell(
                        point = GridPoint(column = column, row = row),
                        cell = cell,
                        sourceRow = sourceRow
                    )
                )
            }
        }
    }
}

private fun boardPointKey(column: Int, row: Int): Int = (column shl 16) or (row and 0xFFFF)

private fun boardCellKey(cell: BoardCell): Int = (cell.tone.ordinal shl 8) or cell.special.ordinal

internal fun specialBlockIconTint(
    style: BlockVisualStyle,
    isDarkTheme: Boolean,
    palette: BlockColorPalette,
): Color {
    if (palette == BlockColorPalette.Monochrome) {
        return Color.Black
    }

    return when (style) {
        BlockVisualStyle.Outline,
        BlockVisualStyle.DynamicLiquid,
        BlockVisualStyle.SoundWave,
        BlockVisualStyle.Cyberpunk -> {
            if (isDarkTheme) Color.White else Color.Black
        }

        BlockVisualStyle.SpiderWeb,
        BlockVisualStyle.Cosmic -> {
            Color.Black
        }

        else -> Color.White
    }
}

@Composable
internal fun blockStyleIconTint(
    style: BlockVisualStyle,
    enabled: Boolean = true,
): Color {
    val settings = LocalAppSettings.current
    return specialBlockIconTint(
        style = style,
        isDarkTheme = isBlockGamesDarkTheme(settings),
        palette = settings.blockColorPalette,
    ).copy(alpha = if (enabled) 1f else 0.58f)
}

internal fun DrawScope.drawWoodGrain(
    topLeft: Offset,
    size: Size,
    alpha: Float,
) {
    val minDim = minOf(size.width, size.height)
    val colorAlpha = 0.35f * alpha // Increased visibility as requested
    val lineCount = 8
    val verticalStep = size.height / (lineCount + 1)

    repeat(lineCount) { i ->
        val yBase = topLeft.y + verticalStep * (i + 1)
        val path = Path().apply {
            moveTo(topLeft.x, yBase)
            val segments = 4
            val segmentWidth = size.width / segments
            for (j in 1..segments) {
                val x = topLeft.x + j * segmentWidth
                val waveOffset = (if (j % 2 == 0) -1f else 1f) * minDim * 0.02f
                val cpX = x - segmentWidth / 2f
                quadraticTo(cpX, yBase + waveOffset, x, yBase)
            }
        }

        val strokeWidth = (minDim * 0.012f).coerceAtLeast(1.0f) // Thicker as requested

        // Shadow line
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = colorAlpha * 0.5f),
            style = Stroke(width = strokeWidth),
        )

        // Highlight line with slight offset
        clipRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height) {
            withTransform({
                translate(0f, -1.2f) // Slightly more offset for thicker lines
            }) {
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = colorAlpha * 0.8f),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

private fun DrawScope.drawStonePattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
) {
    val minDimension = minOf(size.width, size.height)

    // Base stone body with more rocky gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                tintColor(toneColor, 0.15f).copy(alpha = alpha),
                toneColor.copy(alpha = alpha),
                shadeColor(toneColor, 0.22f).copy(alpha = alpha),
            ),
            start = topLeft,
            end = topLeft + Offset(size.width, size.height)
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
    )

    // Irregular polygonal cracks matching the visual reference
    val strokeColor = Color.Black.copy(alpha = 0.28f * alpha)
    val highlightColor = Color.White.copy(alpha = 0.22f * alpha)
    val strokeWidth = (minDimension * 0.038f).coerceAtLeast(1.2f)

    val crackPaths = listOf(
        // Major structural cracks
        listOf(Offset(0.25f, 0f), Offset(0.35f, 0.3f), Offset(0f, 0.45f)),
        listOf(Offset(0.35f, 0.3f), Offset(0.65f, 0.35f), Offset(0.75f, 0f)),
        listOf(Offset(0.65f, 0.35f), Offset(0.85f, 0.6f), Offset(1f, 0.5f)),
        listOf(Offset(0f, 0.75f), Offset(0.3f, 0.7f), Offset(0.45f, 1f)),
        listOf(Offset(0.3f, 0.7f), Offset(0.6f, 0.65f), Offset(0.85f, 0.6f)),
        listOf(Offset(0.6f, 0.65f), Offset(0.75f, 1f)),
        // Minor detail cracks
        listOf(Offset(0.35f, 0.3f), Offset(0.3f, 0.7f)),
        listOf(Offset(0.65f, 0.35f), Offset(0.6f, 0.65f))
    )

    crackPaths.forEach { points ->
        val path = Path().apply {
            moveTo(topLeft.x + size.width * points[0].x, topLeft.y + size.height * points[0].y)
            for (i in 1 until points.size) {
                lineTo(topLeft.x + size.width * points[i].x, topLeft.y + size.height * points[i].y)
            }
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth))

        // Depth highlight on one side of the crack
        val highlightPath = Path().apply {
            moveTo(
                topLeft.x + size.width * points[0].x + 0.8f,
                topLeft.y + size.height * points[0].y + 0.8f
            )
            for (i in 1 until points.size) {
                lineTo(
                    topLeft.x + size.width * points[i].x + 0.8f,
                    topLeft.y + size.height * points[i].y + 0.8f
                )
            }
        }
        drawPath(
            path = highlightPath,
            color = highlightColor,
            style = Stroke(width = strokeWidth * 0.4f)
        )
    }

    // Outer rock bevel/border
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = strokeWidth * 1.2f)
    )
}

@Composable
internal fun BlockCellPreview(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    special: SpecialBlockType = SpecialBlockType.None,
    pulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val isDarkTheme = isBlockGamesDarkTheme(settings)
    val density = LocalDensity.current
    val effectivePulse = rememberBlockStylePulse(style = style, pulse = pulse)
    val cellInset = boardCellInsetDp(size)
    val cellCornerRadius = boardCellCornerRadiusDp(size, style)
    val cellCornerRadiusPx = with(density) { cellCornerRadius.toPx() }
    Box(
        modifier = modifier
            .size(size)
            .padding(cellInset),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCellBody(
                tone = tone,
                palette = palette,
                style = style,
                isDark = isDarkTheme,
                topLeft = Offset.Zero,
                size = this.size,
                cornerRadius = CornerRadius(cellCornerRadiusPx, cellCornerRadiusPx),
                alpha = alpha,
                pulse = effectivePulse,
            )
        }
        if (special != SpecialBlockType.None) {
            Icon(
                imageVector = boardSpecialIcon(special),
                contentDescription = null,
                tint = specialBlockIconTint(
                    style = style,
                    isDarkTheme = isDarkTheme,
                    palette = palette,
                ).copy(alpha = alpha),
                modifier = Modifier
                    .size((size.value * 0.36f).dp),
            )
        }
    }
}

@Composable
internal fun BlockCellPreview(
    baseColor: Color,
    style: BlockVisualStyle,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    pulse: Float = 0f,
) {
    val density = LocalDensity.current
    val settings = LocalAppSettings.current
    val effectivePulse = rememberBlockStylePulse(style = style, pulse = pulse)
    val cellInset = boardCellInsetDp(size)
    val cellCornerRadius = boardCellCornerRadiusDp(size, style)
    val cellCornerRadiusPx = with(density) { cellCornerRadius.toPx() }
    Box(
        modifier = modifier
            .size(size)
            .padding(cellInset),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCellBody(
                baseColor = baseColor,
                palette = settings.blockColorPalette,
                style = style,
                topLeft = Offset.Zero,
                size = this.size,
                cornerRadius = CornerRadius(cellCornerRadiusPx, cellCornerRadiusPx),
                alpha = alpha,
                pulse = effectivePulse,
            )
        }
    }
}

@Composable
fun PieceBlocks(
    piece: Piece,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    cellInset: Dp? = null,
    cellCornerRadius: Dp? = null,
    pulse: Float = 0f,
    blockContent: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.size(
            width = cellSize * piece.width,
            height = cellSize * piece.height,
        ),
    ) {
        val settings = LocalAppSettings.current
        val density = LocalDensity.current
        val visualStyle = settings.blockVisualStyle
        val isDarkTheme = isBlockGamesDarkTheme(settings)
        val resolvedCellInset = cellInset ?: boardCellInsetDp(cellSize)
        val resolvedCellCornerRadius =
            cellCornerRadius ?: boardCellCornerRadiusDp(cellSize, visualStyle)
        val cellCornerRadiusPx = with(density) { resolvedCellCornerRadius.toPx() }
        piece.cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .offset(x = cellSize * cell.column, y = cellSize * cell.row)
                    .size(cellSize)
                    .padding(resolvedCellInset),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCellBody(
                        tone = piece.tone,
                        palette = settings.blockColorPalette,
                        style = visualStyle,
                        isDark = isDarkTheme,
                        topLeft = Offset.Zero,
                        size = this.size,
                        cornerRadius = CornerRadius(cellCornerRadiusPx, cellCornerRadiusPx),
                        alpha = alpha,
                        pulse = pulse,
                    )
                }

                if (piece.value > 0) {
                    Text(
                        text = formatMergeValue(piece.value),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = with(density) { (cellSize.toPx() * 0.44f).toSp() },
                            color = Color.White,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.75f * alpha),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        maxLines = 1,
                    )
                }

                if (piece.special != SpecialBlockType.None) {
                    Icon(
                        imageVector = boardSpecialIcon(piece.special),
                        contentDescription = null,
                        tint = specialBlockIconTint(
                            style = visualStyle,
                            isDarkTheme = isDarkTheme,
                            palette = settings.blockColorPalette,
                        ),
                        modifier = Modifier
                            .size((cellSize.value * 0.40f).dp),
                    )
                }
            }
        }
        blockContent()
    }
}


internal fun DrawScope.drawCellBody(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    isDark: Boolean,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float = 1f,
    pulse: Float = 0f,
) {
    drawCellBody(
        baseColor = tone.paletteColor(palette, isDark),
        palette = palette,
        style = style,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        alpha = alpha,
        pulse = pulse,
    )
}

internal fun DrawScope.drawCellBody(
    baseColor: Color,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float = 1f,
    pulse: Float = 0f,
) {
    val constrainedAlpha = alpha.coerceIn(0f, 1f)
    if (constrainedAlpha <= 0f) return

    val rawBaseColor = baseColor
    val baseColor = rawBaseColor.copy(alpha = constrainedAlpha)
    val minDimension = minOf(size.width, size.height)

    when (style) {
        BlockVisualStyle.Flat -> drawRoundRect(
            color = baseColor,
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )

        BlockVisualStyle.Bubble -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 1f),
                        baseColor.copy(alpha = 0.88f),
                        baseColor.copy(alpha = 0.62f),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.22f, cornerRadius.y * 1.22f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.34f, size.height * 0.24f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.94f, cornerRadius.y * 0.94f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.12f, size.height * 0.24f),
                size = Size(size.width * 0.60f, size.height * 0.10f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.42f, cornerRadius.y * 0.42f),
            )
        }

        BlockVisualStyle.Outline -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.28f),
                        baseColor.copy(alpha = 0.14f),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.96f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.2f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.88f, size.height * 0.88f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.86f),
                style = Stroke(width = 0.9f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.30f, size.height * 0.16f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.7f, cornerRadius.y * 0.7f),
            )
        }

        BlockVisualStyle.Sharp3D -> {
            val bottomLayer = angularPanelPath(
                topLeft = topLeft,
                size = size,
                insetFraction = 0.03f,
                notchFraction = 0.08f,
            )
            val topInset = minDimension * 0.11f
            val topLayerTopLeft = topLeft + Offset(topInset, topInset)
            val topLayerSize = Size(
                width = (size.width - (topInset * 2f)).coerceAtLeast(0f),
                height = (size.height - (topInset * 2f)).coerceAtLeast(0f),
            )
            val topLayer = angularPanelPath(
                topLeft = topLayerTopLeft,
                size = topLayerSize,
                insetFraction = 0.04f,
                notchFraction = 0.09f,
            )

            drawPath(
                path = bottomLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadeColor(rawBaseColor, 0.14f).copy(alpha = constrainedAlpha),
                        rawBaseColor.copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.30f).copy(alpha = constrainedAlpha),
                    ),
                    start = topLeft,
                    end = topLeft + Offset(size.width, size.height),
                ),
            )
            drawPath(
                path = bottomLayer,
                color = Color.Black.copy(alpha = 0.18f * constrainedAlpha),
                style = Stroke(width = (minDimension * 0.038f).coerceAtLeast(1.2f)),
            )

            drawPath(
                path = topLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.22f).copy(alpha = constrainedAlpha),
                        tintColor(rawBaseColor, 0.08f).copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.22f).copy(alpha = constrainedAlpha),
                    ),
                    start = topLayerTopLeft,
                    end = topLayerTopLeft + Offset(topLayerSize.width, topLayerSize.height),
                ),
            )
            drawPath(
                path = topLayer,
                color = Color.White.copy(alpha = 0.16f * constrainedAlpha),
                style = Stroke(width = (minDimension * 0.024f).coerceAtLeast(1f)),
            )

            drawPath(
                path = Path().apply {
                    moveTo(topLeft.x + size.width * 0.14f, topLeft.y + size.height * 0.18f)
                    lineTo(topLeft.x + size.width * 0.56f, topLeft.y + size.height * 0.18f)
                    lineTo(topLeft.x + size.width * 0.38f, topLeft.y + size.height * 0.36f)
                    lineTo(topLeft.x + size.width * 0.14f, topLeft.y + size.height * 0.36f)
                    close()
                },
                color = Color.White.copy(alpha = 0.10f * constrainedAlpha),
            )
            drawPath(
                path = Path().apply {
                    moveTo(topLeft.x + size.width * 0.24f, topLeft.y + size.height * 0.74f)
                    lineTo(topLeft.x + size.width * 0.84f, topLeft.y + size.height * 0.74f)
                    lineTo(topLeft.x + size.width * 0.72f, topLeft.y + size.height * 0.86f)
                    lineTo(topLeft.x + size.width * 0.14f, topLeft.y + size.height * 0.86f)
                    close()
                },
                color = Color.Black.copy(alpha = 0.16f * constrainedAlpha),
            )
        }

        BlockVisualStyle.Wood -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.12f).copy(alpha = constrainedAlpha),
                        baseColor,
                        shadeColor(rawBaseColor, 0.10f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.80f),
            )
            drawWoodGrain(topLeft = topLeft, size = size, alpha = constrainedAlpha)
        }

        BlockVisualStyle.GridSplit -> {
            drawGridSplitPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
            )
        }

        BlockVisualStyle.Crystal -> {
            drawRect(color = baseColor, topLeft = topLeft, size = size)
            val center = topLeft + Offset(size.width / 2f, size.height / 2f)
            drawRect(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f * constrainedAlpha),
                        Color.Black.copy(alpha = 0.20f * constrainedAlpha),
                        Color.White.copy(alpha = 0.24f * constrainedAlpha),
                        Color.Black.copy(alpha = 0.22f * constrainedAlpha),
                        Color.White.copy(alpha = 0.30f * constrainedAlpha),
                    ),
                    center = center,
                ),
                topLeft = topLeft,
                size = size,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.32f * constrainedAlpha),
                start = topLeft,
                end = topLeft + Offset(size.width, size.height),
                strokeWidth = 1.2f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.32f * constrainedAlpha),
                start = topLeft + Offset(size.width, 0f),
                end = topLeft + Offset(0f, size.height),
                strokeWidth = 1.2f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f * constrainedAlpha),
                        Color.Transparent
                    ),
                    center = topLeft + Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = size.width * 0.35f,
                ),
                center = topLeft + Offset(size.width * 0.3f, size.height * 0.3f),
                radius = size.width * 0.35f,
            )
            drawRect(
                color = Color.White.copy(alpha = 0.42f * constrainedAlpha),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 1.6f),
            )
        }

        BlockVisualStyle.DynamicLiquid -> {
            val constrainedPulse = pulse.coerceIn(0f, 1f)
            val glassColor = Color.White.copy(alpha = 0.16f * constrainedAlpha)
            val frameWidth = (minOf(size.width, size.height) * 0.08f).coerceIn(1.4f, 3.4f)
            val liquidInset = frameWidth * 0.92f
            val liquidAreaTopLeft = topLeft + Offset(liquidInset, liquidInset)
            val liquidAreaSize = Size(
                width = (size.width - (liquidInset * 2f)).coerceAtLeast(0f),
                height = (size.height - (liquidInset * 2f)).coerceAtLeast(0f),
            )
            val liquidHeightFraction = 0.10f + (0.90f * constrainedPulse)
            val liquidHeight = liquidAreaSize.height * liquidHeightFraction
            val liquidTopY = liquidAreaTopLeft.y + (liquidAreaSize.height - liquidHeight)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassColor,
                        Color.White.copy(alpha = 0.06f * constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.34f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = frameWidth),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.34f * constrainedAlpha),
                topLeft = topLeft + Offset(frameWidth * 0.38f, frameWidth * 0.38f),
                size = Size(
                    width = (size.width - (frameWidth * 0.76f)).coerceAtLeast(0f),
                    height = (size.height - (frameWidth * 0.76f)).coerceAtLeast(0f),
                ),
                cornerRadius = CornerRadius(
                    (cornerRadius.x - (frameWidth * 0.38f)).coerceAtLeast(0f),
                    (cornerRadius.y - (frameWidth * 0.38f)).coerceAtLeast(0f),
                ),
                style = Stroke(width = (frameWidth * 0.34f).coerceAtLeast(0.9f)),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.98f),
                        baseColor.copy(alpha = 0.82f),
                        baseColor.copy(alpha = 0.62f),
                    ),
                    startY = liquidTopY,
                    endY = liquidAreaTopLeft.y + liquidAreaSize.height,
                ),
                topLeft = Offset(liquidAreaTopLeft.x, liquidTopY),
                size = Size(liquidAreaSize.width, liquidHeight),
                cornerRadius = CornerRadius(cornerRadius.x * 0.68f, cornerRadius.y * 0.68f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f * constrainedAlpha),
                topLeft = Offset(liquidAreaTopLeft.x, liquidTopY),
                size = Size(
                    liquidAreaSize.width,
                    liquidHeight.coerceAtMost(liquidAreaSize.height * 0.20f)
                ),
                cornerRadius = CornerRadius(cornerRadius.x * 0.58f, cornerRadius.y * 0.58f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f * constrainedAlpha),
                topLeft = topLeft + Offset(size.width * 0.1f, size.height * 0.1f),
                size = Size(size.width * 0.15f, size.height * 0.3f),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }

        BlockVisualStyle.Tornado -> {
            drawTornadoPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                pulse = pulse,
            )
        }

        BlockVisualStyle.HoneycombTexture -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.16f).copy(alpha = constrainedAlpha),
                        baseColor,
                        shadeColor(rawBaseColor, 0.12f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.92f, cornerRadius.y * 0.92f),
            )
            drawHoneycombPattern(
                topLeft = topLeft,
                size = size,
                alpha = constrainedAlpha,
                toneColor = rawBaseColor
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f * constrainedAlpha),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.92f, cornerRadius.y * 0.92f),
                style = Stroke(width = (minDimension * 0.025f).coerceAtLeast(1f)),
            )
        }

        BlockVisualStyle.SpiderWeb -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.18f).copy(alpha = constrainedAlpha),
                        baseColor,
                        shadeColor(rawBaseColor, 0.10f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
            )
            drawSpiderWebPattern(
                topLeft = topLeft,
                size = size,
                alpha = constrainedAlpha,
                lineColor = Color.White.copy(alpha = 0.44f * constrainedAlpha),
            )
        }

        BlockVisualStyle.Cosmic -> {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.14f).copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.42f).copy(alpha = constrainedAlpha),
                        Color(0xFF140F1E).copy(alpha = constrainedAlpha),
                    ),
                    center = topLeft + Offset(size.width * 0.48f, size.height * 0.46f),
                    radius = minDimension * 0.86f,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
            )
            drawCosmicField(
                topLeft = topLeft,
                size = size,
                baseGlow = tintColor(rawBaseColor, 0.22f),
                alpha = constrainedAlpha,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Brick -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.14f).copy(alpha = constrainedAlpha),
                        baseColor.copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.18f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.52f, cornerRadius.y * 0.52f),
            )
            drawBrickPattern(
                topLeft = topLeft,
                size = size,
                baseTone = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
            )
        }

        BlockVisualStyle.SoundWave -> {
            drawSoundWavePattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Prism -> {
            drawPrismRefraction(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Flame -> {
            drawFlamePattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Gears -> {
            drawGearsPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Pixel -> {
            drawPixelPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Cyberpunk -> {
            drawCyberpunkPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.NeonGlow -> {
            drawNeonGlowPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.LiquidMarble -> {
            drawLiquidMarblePattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Holographic -> {
            drawHolographicPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.GlitchTech -> {
            drawGlitchTechPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.AuraEnergy -> {
            drawAuraEnergyPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.CircuitBoard -> {
            drawCircuitBoardPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }

        BlockVisualStyle.LightBurst -> {
            drawLightBurstPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }
    }
}

private fun DrawScope.drawCyberpunkPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val accentColor = toneColor.copy(alpha = alpha)

    clipPath(roundedClipPath(topLeft, size, cornerRadius)) {
        // Grid lines
        val lineCount = 4
        for (i in 1 until lineCount) {
            val ratio = i.toFloat() / lineCount
            drawLine(
                color = accentColor.copy(alpha = 0.15f * alpha),
                start = Offset(topLeft.x + size.width * ratio, topLeft.y),
                end = Offset(topLeft.x + size.width * ratio, topLeft.y + size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = accentColor.copy(alpha = 0.15f * alpha),
                start = Offset(topLeft.x, topLeft.y + size.height * ratio),
                end = Offset(topLeft.x + size.width, topLeft.y + size.height * ratio),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Animated scanline
        val scanlineY = (pulse * size.height * 2) % (size.height * 1.5f)
        if (scanlineY < size.height) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.5f to accentColor.copy(alpha = 0.3f * alpha),
                    1.0f to Color.Transparent,
                    startY = topLeft.y + scanlineY - 10.dp.toPx(),
                    endY = topLeft.y + scanlineY + 10.dp.toPx()
                ),
                topLeft = Offset(topLeft.x, topLeft.y + scanlineY - 10.dp.toPx()),
                size = Size(size.width, 20.dp.toPx())
            )
        }

        // Random digital "glitch" pixels
        val rand = kotlin.random.Random((pulse * 10).toInt())
        if (rand.nextFloat() > 0.7f) {
            val pxSize = 4.dp.toPx()
            val pxX = rand.nextFloat() * (size.width - pxSize)
            val pxY = rand.nextFloat() * (size.height - pxSize)
            drawRect(
                color = accentColor.copy(alpha = 0.8f * alpha),
                topLeft = Offset(topLeft.x + pxX, topLeft.y + pxY),
                size = Size(pxSize, pxSize)
            )
        }
    }

    // Outer glow border
    drawRoundRect(
        color = accentColor.copy(alpha = 0.6f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun roundedClipPath(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
): Path = Path().apply {
    addRoundRect(
        RoundRect(
            rect = Rect(topLeft, size),
            cornerRadius = cornerRadius,
        )
    )
}

private fun DrawScope.drawFlamePattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val clipShape = roundedClipPath(topLeft, size, cornerRadius)
    val pulseAmount = pulse.coerceIn(0f, 1f)
    val cycle = sin(pulseAmount * PI.toFloat()).coerceIn(0f, 1f)
    val sway = sin(pulseAmount * PI.toFloat() * 2f) * size.width * 0.014f
    val widthScale = 1f + (cycle * 0.20f)
    val heightCompression = 1f - (cycle * 0.22f)
    val centerX = topLeft.x + size.width * 0.5f + sway
    val bottomY = topLeft.y + size.height * (0.94f + (cycle * 0.01f))
    val topY = topLeft.y + size.height * (0.10f + (cycle * 0.16f))
    val innerTopY = topLeft.y + size.height * (0.25f + (cycle * 0.14f))
    val coreTopY = topLeft.y + size.height * (0.39f + (cycle * 0.10f))
    fun x(fraction: Float): Float = centerX + ((size.width * (fraction - 0.5f)) * widthScale)
    fun y(baseFraction: Float): Float =
        topLeft.y + size.height * (baseFraction * heightCompression + (1f - heightCompression) * 0.96f)

    val outerTop = shadeColor(toneColor, 0.14f)
    val outerMid = toneColor
    val outerBottom = tintColor(toneColor, 0.10f)
    val innerTop = tintColor(toneColor, 0.14f)
    val innerBottom = tintColor(toneColor, 0.04f)
    val coreTop = tintColor(toneColor, 0.22f)
    val coreBottom = tintColor(toneColor, 0.10f)

    val outerFlame = Path().apply {
        moveTo(centerX, topY)
        cubicTo(
            x(0.78f),
            y(0.18f),
            x(0.96f),
            y(0.56f),
            x(0.72f),
            bottomY,
        )
        cubicTo(
            x(0.63f),
            y(0.98f),
            x(0.37f),
            y(0.98f),
            x(0.28f),
            bottomY,
        )
        cubicTo(
            x(0.04f),
            y(0.56f),
            x(0.22f),
            y(0.18f),
            centerX,
            topY,
        )
        close()
    }
    val innerFlame = Path().apply {
        moveTo(centerX, innerTopY)
        cubicTo(
            x(0.70f),
            y(0.33f),
            x(0.80f),
            y(0.61f),
            x(0.65f),
            y(0.84f),
        )
        cubicTo(
            x(0.58f),
            y(0.92f),
            x(0.42f),
            y(0.92f),
            x(0.35f),
            y(0.84f),
        )
        cubicTo(
            x(0.20f),
            y(0.61f),
            x(0.30f),
            y(0.33f),
            centerX,
            innerTopY,
        )
        close()
    }
    val coreFlame = Path().apply {
        moveTo(centerX, coreTopY)
        cubicTo(
            x(0.62f),
            y(0.46f),
            x(0.69f),
            y(0.65f),
            x(0.58f),
            y(0.78f),
        )
        cubicTo(
            x(0.54f),
            y(0.84f),
            x(0.46f),
            y(0.84f),
            x(0.42f),
            y(0.78f),
        )
        cubicTo(
            x(0.31f),
            y(0.65f),
            x(0.38f),
            y(0.46f),
            centerX,
            coreTopY,
        )
        close()
    }

    clipPath(clipShape) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    shadeColor(toneColor, 0.34f).copy(alpha = 0.12f * alpha),
                    toneColor.copy(alpha = (0.16f + (cycle * 0.16f)) * alpha),
                    Color.Transparent,
                ),
                startY = topLeft.y,
                endY = topLeft.y + size.height,
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )
        drawPath(
            path = outerFlame,
            brush = Brush.verticalGradient(
                colors = listOf(
                    outerTop.copy(alpha = 0.96f * alpha),
                    outerMid.copy(alpha = 0.98f * alpha),
                    outerBottom.copy(alpha = 0.90f * alpha),
                ),
                startY = bottomY,
                endY = topLeft.y,
            ),
        )
        drawPath(
            path = innerFlame,
            brush = Brush.verticalGradient(
                colors = listOf(
                    innerTop.copy(alpha = 0.82f * alpha),
                    innerBottom.copy(alpha = 0.94f * alpha),
                ),
                startY = topLeft.y + size.height,
                endY = topLeft.y,
            ),
        )
        drawPath(
            path = coreFlame,
            brush = Brush.verticalGradient(
                colors = listOf(
                    coreTop.copy(alpha = 0.74f * alpha),
                    coreBottom.copy(alpha = 0.86f * alpha),
                ),
                startY = topLeft.y + size.height,
                endY = topLeft.y,
            ),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f * alpha),
            topLeft = Offset(
                x(0.22f),
                y(0.24f),
            ),
            size = Size(size.width * 0.26f * widthScale, size.height * 0.07f),
            cornerRadius = CornerRadius(cornerRadius.x * 0.60f, cornerRadius.y * 0.60f),
        )
        drawRoundRect(
            color = tintColor(toneColor, 0.18f).copy(alpha = (0.12f + (cycle * 0.10f)) * alpha),
            topLeft = Offset(
                x(0.57f),
                y(0.31f),
            ),
            size = Size(
                size.width * 0.08f * widthScale,
                size.height * (0.23f * heightCompression + 0.03f)
            ),
            cornerRadius = CornerRadius(cornerRadius.x * 0.46f, cornerRadius.y * 0.46f),
        )
    }
}

private fun DrawScope.drawGearsPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val clipShape = roundedClipPath(topLeft, size, cornerRadius)
    val minDimension = minOf(size.width, size.height)
    val rotationPhase = pulse.coerceIn(0f, 1f)
    val spinDegrees = rotationPhase * 360f

    clipPath(clipShape) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    tintColor(toneColor, 0.24f).copy(alpha = 0.94f * alpha),
                    shadeColor(toneColor, 0.08f).copy(alpha = 0.98f * alpha),
                    shadeColor(toneColor, 0.34f).copy(alpha = 0.96f * alpha),
                ),
                start = topLeft,
                end = topLeft + Offset(size.width, size.height),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f * alpha),
            topLeft = topLeft + Offset(minDimension * 0.03f, minDimension * 0.03f),
            size = Size(size.width - (minDimension * 0.06f), size.height - (minDimension * 0.06f)),
            cornerRadius = CornerRadius(
                (cornerRadius.x - minDimension * 0.03f).coerceAtLeast(0f),
                (cornerRadius.y - minDimension * 0.03f).coerceAtLeast(0f),
            ),
            style = Stroke(width = (minDimension * 0.018f).coerceAtLeast(0.8f)),
        )

        val mainTeeth = 20f

        data class GearSpec(
            val centerFraction: Offset,
            val radiusFactor: Float,
            val toothDepthFactor: Float,
            val teeth: Int,
            val direction: Float,
            val baseRotation: Float,
            val color: Color,
        )
        listOf(
            GearSpec(Offset(0.29f, 0.69f), 0.250f, 0.026f, 20, -1f, -18f, toneColor),
            GearSpec(Offset(0.74f, 0.6f), 0.164f, 0.024f, 17, 1f, 12f, tintColor(toneColor, 0.12f)),
            GearSpec(
                Offset(0.53f, 0.26f),
                0.208f,
                0.021f,
                15,
                1f,
                -8f,
                tintColor(toneColor, 0.05f)
            ),
        ).forEach { gear ->
            drawGear(
                center = Offset(
                    topLeft.x + size.width * gear.centerFraction.x,
                    topLeft.y + size.height * gear.centerFraction.y,
                ),
                radius = minDimension * gear.radiusFactor,
                toothDepth = minDimension * gear.toothDepthFactor,
                teeth = gear.teeth,
                rotationDegrees = gear.baseRotation + (gear.direction * spinDegrees * (mainTeeth / gear.teeth)),
                toneColor = gear.color,
                alpha = alpha,
            )
        }
    }
}


@Preview
@Composable
internal fun BlockCellPreviewPreview() {
    BlockCellPreview(
        baseColor = Color.Red,
        style = BlockVisualStyle.NeonGlow,
        size = 100.dp,
        modifier = Modifier,
        alpha = 1f,
        pulse = 0f,
    )
}

private fun DrawScope.drawGear(
    center: Offset,
    radius: Float,
    toothDepth: Float,
    teeth: Int,
    rotationDegrees: Float,
    toneColor: Color,
    alpha: Float,
) {
    val outerRadius = radius + toothDepth
    val rotationRadians = rotationDegrees * (PI.toFloat() / 180f)
    val toothStep = (PI.toFloat() * 2f) / teeth
    val rootRadius = radius * 0.84f
    val path = Path().apply {
        for (index in 0 until teeth) {
            val baseAngle = rotationRadians + (toothStep * index)
            val points = listOf(
                Offset(
                    x = center.x + (cos(baseAngle - toothStep * 0.34f) * rootRadius),
                    y = center.y + (sin(baseAngle - toothStep * 0.34f) * rootRadius),
                ),
                Offset(
                    x = center.x + (cos(baseAngle - toothStep * 0.12f) * outerRadius),
                    y = center.y + (sin(baseAngle - toothStep * 0.12f) * outerRadius),
                ),
                Offset(
                    x = center.x + (cos(baseAngle) * outerRadius),
                    y = center.y + (sin(baseAngle) * outerRadius),
                ),
                Offset(
                    x = center.x + (cos(baseAngle + toothStep * 0.12f) * outerRadius),
                    y = center.y + (sin(baseAngle + toothStep * 0.12f) * outerRadius),
                ),
                Offset(
                    x = center.x + (cos(baseAngle + toothStep * 0.34f) * rootRadius),
                    y = center.y + (sin(baseAngle + toothStep * 0.34f) * rootRadius),
                ),
            )
            points.forEachIndexed { pointIndex, point ->
                if (index == 0 && pointIndex == 0) moveTo(point.x, point.y) else lineTo(
                    point.x,
                    point.y
                )
            }
        }
        close()
    }
    val ringRadius = radius * 0.66f
    val innerRadius = radius * 0.26f

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                tintColor(toneColor, 0.24f).copy(alpha = 0.98f * alpha),
                toneColor.copy(alpha = 0.96f * alpha),
                shadeColor(toneColor, 0.32f).copy(alpha = 0.98f * alpha),
            ),
            center = center,
            radius = outerRadius,
        ),
    )
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.18f * alpha),
        style = Stroke(width = (radius * 0.075f).coerceAtLeast(0.9f)),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tintColor(toneColor, 0.18f).copy(alpha = 0.98f * alpha),
                shadeColor(toneColor, 0.16f).copy(alpha = 0.94f * alpha),
            ),
            center = center + Offset(-radius * 0.04f, -radius * 0.08f),
            radius = radius,
        ),
        center = center,
        radius = radius,
    )
    drawCircle(
        color = shadeColor(toneColor, 0.42f).copy(alpha = 0.92f * alpha),
        center = center + Offset(radius * 0.02f, radius * 0.04f),
        radius = ringRadius,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tintColor(toneColor, 0.14f).copy(alpha = 0.94f * alpha),
                shadeColor(toneColor, 0.28f).copy(alpha = 0.96f * alpha),
            ),
            center = center,
            radius = ringRadius,
        ),
        center = center,
        radius = ringRadius * 0.94f,
    )
    repeat(3) { spokeIndex ->
        val spokeAngle = rotationRadians + (spokeIndex * ((PI.toFloat() * 2f) / 3f))
        drawLine(
            color = shadeColor(toneColor, 0.34f).copy(alpha = 0.82f * alpha),
            start = Offset(
                center.x + (cos(spokeAngle) * innerRadius * 0.72f),
                center.y + (sin(spokeAngle) * innerRadius * 0.72f),
            ),
            end = Offset(
                center.x + (cos(spokeAngle) * ringRadius * 0.84f),
                center.y + (sin(spokeAngle) * ringRadius * 0.84f),
            ),
            strokeWidth = (radius * 0.16f).coerceAtLeast(1.3f),
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = 0.08f * alpha),
        center = center + Offset(-radius * 0.08f, -radius * 0.12f),
        radius = ringRadius * 0.58f,
    )
    drawCircle(
        color = shadeColor(toneColor, 0.52f).copy(alpha = 0.98f * alpha),
        center = center,
        radius = innerRadius,
    )
}

private fun DrawScope.drawPixelPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val clipShape = roundedClipPath(topLeft, size, cornerRadius)
    val pixelCount = 7
    val cellWidth = size.width / pixelCount
    val cellHeight = size.height / pixelCount
    val covered = Array(pixelCount) { BooleanArray(pixelCount) }

    clipPath(clipShape) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    shadeColor(toneColor, 0.12f).copy(alpha = 0.86f * alpha),
                    toneColor.copy(alpha = 0.92f * alpha),
                    tintColor(toneColor, 0.08f).copy(alpha = 0.88f * alpha),
                ),
                start = topLeft,
                end = topLeft + Offset(size.width, size.height),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )

        for (row in 0 until pixelCount) {
            for (col in 0 until pixelCount) {
                if (covered[row][col]) continue

                val noiseA = row * 73 + col * 29
                val noiseB = row * 19 + col * 61 + 7
                val variant = (noiseA + noiseB) % 4
                val clusterWidth = when {
                    col == pixelCount - 1 -> 1
                    variant >= 2 && noiseA % 4 == 0 -> 2
                    else -> 1
                }
                val clusterHeight = when {
                    row == pixelCount - 1 -> 1
                    variant == 3 && noiseB % 3 == 0 -> 2
                    variant == 1 && noiseA % 5 == 0 -> 2
                    else -> 1
                }
                val actualWidth = clusterWidth.coerceAtMost(pixelCount - col)
                val actualHeight = clusterHeight.coerceAtMost(pixelCount - row)

                for (coverRow in row until row + actualHeight) {
                    for (coverCol in col until col + actualWidth) {
                        covered[coverRow][coverCol] = true
                    }
                }

                val primaryLayer = (row + col + variant) % 2 == 0
                val pixelColor = if (primaryLayer) {
                    tintColor(toneColor, if (variant == 3) 0.16f else 0.08f)
                } else {
                    shadeColor(toneColor, if (variant == 1) 0.14f else 0.06f)
                }
                val pixelAlpha = if (primaryLayer) 0.54f * alpha else 0.96f * alpha
                val pixelTopLeft = Offset(
                    x = topLeft.x + (col * cellWidth),
                    y = topLeft.y + (row * cellHeight),
                )
                val pixelSize = Size(cellWidth * actualWidth, cellHeight * actualHeight)

                drawRect(
                    color = pixelColor.copy(alpha = pixelAlpha),
                    topLeft = pixelTopLeft,
                    size = pixelSize,
                )
                if (actualWidth > 1 || actualHeight > 1) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.08f * alpha),
                        topLeft = pixelTopLeft,
                        size = Size(pixelSize.width, (cellHeight * 0.22f).coerceAtLeast(1f)),
                    )
                }
            }
        }
    }
}

private fun tintColor(color: Color, amount: Float): Color =
    lerp(color, Color.White, amount.coerceIn(0f, 1f))

private fun shadeColor(color: Color, amount: Float): Color =
    lerp(color, Color.Black, amount.coerceIn(0f, 1f))

private fun DrawScope.drawMarbleVeins(
    topLeft: Offset,
    size: Size,
    primary: Color,
    secondary: Color,
    strokeWidth: Float,
) {
    val paths = listOf(
        marbleVeinPath(
            topLeft = topLeft,
            size = size,
            points = listOf(
                Offset(0.10f, 0.08f),
                Offset(0.34f, 0.22f),
                Offset(0.22f, 0.48f),
                Offset(0.42f, 0.72f),
                Offset(0.22f, 0.96f),
            ),
        ),
        marbleVeinPath(
            topLeft = topLeft,
            size = size,
            points = listOf(
                Offset(0.62f, 0.02f),
                Offset(0.84f, 0.18f),
                Offset(0.58f, 0.40f),
                Offset(0.84f, 0.66f),
                Offset(0.66f, 0.96f),
            ),
        ),
        marbleVeinPath(
            topLeft = topLeft,
            size = size,
            points = listOf(
                Offset(0.00f, 0.28f),
                Offset(0.22f, 0.38f),
                Offset(0.08f, 0.62f),
                Offset(0.28f, 0.82f),
                Offset(0.12f, 1.00f),
            ),
        ),
    )

    paths.forEachIndexed { index, path ->
        drawPath(
            path = path,
            color = if (index == 1) secondary else primary,
            style = Stroke(width = strokeWidth * if (index == 1) 0.72f else 1f),
        )
    }
}

private fun DrawScope.drawBurstRays(
    center: Offset,
    radius: Float,
    rayCount: Int,
    color: Color,
    alpha: Float,
    strokeWidth: Float,
) {
    repeat(rayCount) { index ->
        val angle = ((2.0 * PI * index) / rayCount).toFloat()
        val rayRadius = if (index % 2 == 0) radius else radius * 0.72f
        val end = center + Offset(cos(angle) * rayRadius, sin(angle) * rayRadius)
        drawLine(
            color = color.copy(alpha = alpha * if (index % 2 == 0) 1f else 0.62f),
            start = center,
            end = end,
            strokeWidth = strokeWidth,
        )
    }
}


private fun DrawScope.drawHoneycombPattern(
    topLeft: Offset,
    size: Size,
    alpha: Float,
    toneColor: Color,
) {
    val minSize = minOf(size.width, size.height)
    val radius = minSize * 0.115f
    val horizontalStep = radius * 1.74f
    val verticalStep = radius * 1.52f
    val strokeColor = shadeColor(toneColor, 0.28f).copy(alpha = 0.36f * alpha)
    val highlightColor = tintColor(toneColor, 0.30f).copy(alpha = 0.18f * alpha)
    val inset = radius * 0.36f
    clipRect(
        left = topLeft.x + inset,
        top = topLeft.y + inset,
        right = topLeft.x + size.width - inset,
        bottom = topLeft.y + size.height - inset,
    ) {
        var rowIndex = 0
        var y = topLeft.y + radius
        while (y <= topLeft.y + size.height) {
            val offset = if (rowIndex % 2 == 0) 0f else horizontalStep / 2f
            var x = topLeft.x + radius + offset
            while (x <= topLeft.x + size.width) {
                val center = Offset(x, y)
                val hexagon = hexagonPath(center = center, radius = radius)
                drawPath(
                    path = hexagon,
                    color = strokeColor,
                    style = Stroke(width = (minSize * 0.023f).coerceAtLeast(1.1f)),
                )
                drawPath(
                    path = hexagon,
                    color = highlightColor,
                    style = Stroke(width = (minSize * 0.010f).coerceAtLeast(0.6f)),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f * alpha),
                    center = center,
                    radius = radius * 0.16f,
                )
                x += horizontalStep
            }
            y += verticalStep
            rowIndex += 1
        }
    }
}

private fun DrawScope.drawSpiderWebPattern(
    topLeft: Offset,
    size: Size,
    alpha: Float,
    lineColor: Color,
) {
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)
    val anchors = listOf(
        Offset(0.00f, 0.00f),
        Offset(0.22f, 0.04f),
        Offset(0.50f, 0.00f),
        Offset(0.78f, 0.04f),
        Offset(1.00f, 0.00f),
        Offset(0.96f, 0.22f),
        Offset(1.00f, 0.50f),
        Offset(0.96f, 0.78f),
        Offset(1.00f, 1.00f),
        Offset(0.78f, 0.96f),
        Offset(0.50f, 1.00f),
        Offset(0.22f, 0.96f),
        Offset(0.00f, 1.00f),
        Offset(0.04f, 0.78f),
        Offset(0.00f, 0.50f),
        Offset(0.04f, 0.22f),
    )

    anchors.forEach { anchorFraction ->
        val anchor = topLeft + Offset(size.width * anchorFraction.x, size.height * anchorFraction.y)
        drawLine(
            color = lineColor,
            start = center,
            end = anchor,
            strokeWidth = (minOf(size.width, size.height) * 0.016f).coerceAtLeast(0.7f),
        )
    }

    listOf(0.14f, 0.28f, 0.44f, 0.62f, 0.82f).forEachIndexed { index, factor ->
        val ringPath = Path().apply {
            anchors.forEachIndexed { anchorIndex, anchorFraction ->
                val point = center + Offset(
                    x = (topLeft.x + (size.width * anchorFraction.x) - center.x) * factor,
                    y = (topLeft.y + (size.height * anchorFraction.y) - center.y) * factor,
                )
                if (anchorIndex == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(
            path = ringPath,
            color = lineColor.copy(alpha = (0.60f - (index * 0.07f)).coerceAtLeast(0.24f) * alpha),
            style = Stroke(width = (minOf(size.width, size.height) * 0.014f).coerceAtLeast(0.7f)),
        )
    }

    anchors.windowed(size = 2, step = 2, partialWindows = false)
        .forEach { (startFraction, endFraction) ->
            val start =
                topLeft + Offset(size.width * startFraction.x, size.height * startFraction.y)
            val end = topLeft + Offset(size.width * endFraction.x, size.height * endFraction.y)
            drawLine(
                color = lineColor.copy(alpha = 0.18f * alpha),
                start = start,
                end = end,
                strokeWidth = (minOf(size.width, size.height) * 0.010f).coerceAtLeast(0.5f),
            )
        }

    drawCircle(
        color = Color.White.copy(alpha = 0.22f * alpha),
        center = center,
        radius = minOf(size.width, size.height) * 0.05f,
    )
}

private fun DrawScope.drawCosmicField(
    topLeft: Offset,
    size: Size,
    baseGlow: Color,
    alpha: Float,
    pulse: Float,
) {
    val minDimension = minOf(size.width, size.height)
    val phase = pulse.coerceIn(0f, 1f)
    listOf(
        Triple(Offset(0.26f, 0.32f), 0.34f, baseGlow.copy(alpha = 0.34f * alpha)),
        Triple(Offset(0.64f, 0.44f), 0.28f, Color.White.copy(alpha = 0.18f * alpha)),
        Triple(Offset(0.76f, 0.24f), 0.20f, tintColor(baseGlow, 0.32f).copy(alpha = 0.22f * alpha)),
        Triple(Offset(0.44f, 0.74f), 0.24f, Color(0xFF6FC3FF).copy(alpha = 0.16f * alpha)),
    ).forEachIndexed { index, (fraction, radiusFactor, color) ->
        val orbit = phase * PI.toFloat() * 2f + index
        val center = topLeft + Offset(
            size.width * fraction.x + cos(orbit) * size.width * 0.010f,
            size.height * fraction.y + sin(orbit * 0.82f) * size.height * 0.010f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = minDimension * radiusFactor,
            ),
            center = center,
            radius = minDimension * radiusFactor,
        )
    }

    drawArc(
        color = Color.White.copy(alpha = 0.10f * alpha),
        startAngle = -24f,
        sweepAngle = 208f,
        useCenter = false,
        topLeft = topLeft + Offset(size.width * 0.14f, size.height * 0.22f),
        size = Size(size.width * 0.68f, size.height * 0.42f),
        style = Stroke(width = (minDimension * 0.022f).coerceAtLeast(0.9f)),
    )
    drawArc(
        color = tintColor(baseGlow, 0.42f).copy(alpha = 0.12f * alpha),
        startAngle = 116f,
        sweepAngle = 164f,
        useCenter = false,
        topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.10f),
        size = Size(size.width * 0.84f, size.height * 0.70f),
        style = Stroke(width = (minDimension * 0.016f).coerceAtLeast(0.7f)),
    )

    stableStarField(
        seed = cosmicSeed(
            topLeft = topLeft,
            size = size,
            colorSeed = baseGlow.hashCode()
        )
    ).forEachIndexed { index, star ->
        val fraction = Offset(star.x, star.y)
        val twinkleWave = (sin((phase + (index * 0.13f)) * PI.toFloat() * 2f) * 0.5f) + 0.5f
        val driftAmount = if (star.twinkle) 0.006f else 0.0025f
        val center = topLeft + Offset(
            size.width * fraction.x + cos((phase + index) * PI.toFloat() * 2f) * size.width * driftAmount,
            size.height * fraction.y + sin((phase * 0.8f + index) * PI.toFloat() * 2f) * size.height * driftAmount,
        )
        val radius = minDimension * star.radiusFactor
        val starColor = when (index % 4) {
            0 -> Color.White
            1 -> Color(0xFFBFE7FF)
            2 -> Color(0xFFFFE5A8)
            else -> tintColor(baseGlow, 0.50f)
        }
        drawCircle(
            color = starColor.copy(alpha = star.alpha * (0.72f + (twinkleWave * 0.28f)) * alpha),
            center = center,
            radius = radius
        )
        if (star.twinkle) {
            drawBurstRays(
                center = center,
                radius = radius * (2.4f + twinkleWave),
                rayCount = 4,
                color = starColor,
                alpha = (0.10f + (twinkleWave * 0.18f)) * alpha,
                strokeWidth = radius * (0.28f + (twinkleWave * 0.18f)),
            )
        }
    }
}

private fun DrawScope.drawRubikCubeFace(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    palette: BlockColorPalette,
) {
    val minDimension = minOf(size.width, size.height)
    val shellCorner = CornerRadius(
        x = (cornerRadius.x * 0.34f).coerceAtLeast(minDimension * 0.06f),
        y = (cornerRadius.y * 0.34f).coerceAtLeast(minDimension * 0.06f),
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                tintColor(toneColor, 0.18f).copy(alpha = alpha),
                toneColor.copy(alpha = alpha),
                shadeColor(toneColor, 0.22f).copy(alpha = alpha),
            ),
            start = topLeft,
            end = topLeft + Offset(size.width, size.height),
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = shellCorner,
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.18f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = shellCorner,
        style = Stroke(width = (minDimension * 0.040f).coerceAtLeast(1.1f)),
    )

    val gridCount = 4
    val shellInset = (minDimension * 0.08f).coerceIn(1.2f, 3.6f)
    val stickerGap = (minDimension * 0.022f).coerceIn(0.6f, 1.4f)
    val faceSize = (minDimension - (shellInset * 2f)).coerceAtLeast(minDimension * 0.54f)
    val stickerSize = ((faceSize - (stickerGap * (gridCount - 1))) / gridCount).coerceAtLeast(0f)
    val faceTopLeft = topLeft + Offset((size.width - faceSize) / 2f, (size.height - faceSize) / 2f)
    val stickerCorner = CornerRadius(
        x = (stickerSize * 0.16f).coerceAtLeast(0.6f),
        y = (stickerSize * 0.16f).coerceAtLeast(0.6f),
    )
    val alphaBoost = if (palette == BlockColorPalette.Monochrome) 0.10f else 0f

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f * alpha),
        topLeft = faceTopLeft - Offset(stickerGap, stickerGap),
        size = Size(faceSize + (stickerGap * 2f), faceSize + (stickerGap * 2f)),
        cornerRadius = CornerRadius(shellCorner.x * 0.72f, shellCorner.y * 0.72f),
    )

    repeat(gridCount) { row ->
        repeat(gridCount) { column ->
            val stickerTopLeft = faceTopLeft + Offset(
                x = column * (stickerSize + stickerGap),
                y = row * (stickerSize + stickerGap),
            )
            val intensity = ((gridCount * 2) - row - column).toFloat() / (gridCount * 2)
            val stickerAlpha =
                (0.42f + (intensity * 0.34f) + alphaBoost).coerceIn(0.34f, 0.92f) * alpha
            val stickerColor = if ((row + column) % 2 == 0) {
                tintColor(toneColor, 0.10f + (intensity * 0.12f)).copy(alpha = stickerAlpha)
            } else {
                shadeColor(toneColor, 0.04f + ((1f - intensity) * 0.14f)).copy(alpha = stickerAlpha)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(
                            toneColor,
                            0.12f
                        ).copy(alpha = (stickerAlpha * 0.96f).coerceAtMost(alpha)),
                        stickerColor,
                        shadeColor(
                            toneColor,
                            0.18f
                        ).copy(alpha = (stickerAlpha * 0.94f).coerceAtMost(alpha)),
                    ),
                    startY = stickerTopLeft.y,
                    endY = stickerTopLeft.y + stickerSize,
                ),
                topLeft = stickerTopLeft,
                size = Size(stickerSize, stickerSize),
                cornerRadius = stickerCorner,
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.16f * alpha),
                topLeft = stickerTopLeft,
                size = Size(stickerSize, stickerSize),
                cornerRadius = stickerCorner,
                style = Stroke(width = (stickerSize * 0.08f).coerceAtLeast(0.8f)),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f * alpha),
                topLeft = stickerTopLeft + Offset(stickerSize * 0.10f, stickerSize * 0.08f),
                size = Size(stickerSize * 0.62f, stickerSize * 0.20f),
                cornerRadius = CornerRadius(stickerCorner.x * 0.7f, stickerCorner.y * 0.7f),
            )
        }
    }
}

private fun DrawScope.drawTornadoPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    pulse: Float,
) {
    val minDimension = minOf(size.width, size.height)
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)
    val rotationOffset = pulse * 2f * PI.toFloat()

    // Background less faded as requested
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                toneColor.copy(alpha = 0.68f * alpha),
                Color.Transparent,
            ),
            center = center,
            radius = minDimension * 0.54f,
        ),
        center = center,
        radius = minDimension * 0.54f,
    )

    // Swirling tornado effect
    val tornadoPath = Path()
    val segments = 40
    val rotations = 3f
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val radius = (0.05f + t * 0.35f) * minDimension
        val angle = t * rotations * 2f * PI.toFloat() + rotationOffset
        val x = center.x + cos(angle) * radius
        val y = (center.y + (minDimension * 0.35f)) - (t * minDimension * 0.7f)
        if (i == 0) tornadoPath.moveTo(x, y) else tornadoPath.lineTo(x, y)
    }

    drawPath(
        path = tornadoPath,
        color = toneColor.copy(alpha = alpha),
        style = Stroke(
            width = (minDimension * 0.06f).coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
        ),
    )

    repeat(3) { index ->
        val arcRotation = (rotationOffset * (1.2f + index * 0.4f)) % (2f * PI.toFloat())
        drawArc(
            color = toneColor.copy(alpha = (0.25f - index * 0.05f).coerceAtLeast(0.05f) * alpha),
            startAngle = arcRotation * (180f / PI.toFloat()),
            sweepAngle = 120f,
            useCenter = false,
            topLeft = topLeft + Offset(
                size.width * (0.1f + index * 0.08f),
                size.height * (0.1f + index * 0.08f)
            ),
            size = Size(size.width * (0.8f - index * 0.16f), size.height * (0.8f - index * 0.16f)),
            style = Stroke(width = (minDimension * 0.02f).coerceAtLeast(1f), cap = StrokeCap.Round),
        )
    }

    // Border in main color as requested
    drawRoundRect(
        color = toneColor.copy(alpha = 0.8f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(minDimension * 0.16f, minDimension * 0.16f),
        style = Stroke(width = (minDimension * 0.05f).coerceAtLeast(1.5f))
    )
}

private fun angularPanelPath(
    topLeft: Offset,
    size: Size,
    insetFraction: Float,
    notchFraction: Float,
): Path {
    val insetX = size.width * insetFraction
    val insetY = size.height * insetFraction
    val notchX = size.width * notchFraction
    val notchY = size.height * notchFraction
    return Path().apply {
        moveTo(topLeft.x + insetX + notchX, topLeft.y + insetY)
        lineTo(topLeft.x + size.width - insetX, topLeft.y + insetY)
        lineTo(topLeft.x + size.width - insetX, topLeft.y + size.height - insetY - notchY)
        lineTo(topLeft.x + size.width - insetX - notchX, topLeft.y + size.height - insetY)
        lineTo(topLeft.x + insetX, topLeft.y + size.height - insetY)
        lineTo(topLeft.x + insetX, topLeft.y + insetY + notchY)
        close()
    }
}

private fun DrawScope.drawBrickPattern(
    topLeft: Offset,
    size: Size,
    baseTone: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
) {
    val mortarColor = shadeColor(baseTone, 0.56f).copy(alpha = 0.38f * alpha)
    val brickStroke = Color.White.copy(alpha = 0.12f * alpha)
    val mortarWidth = (minOf(size.width, size.height) * 0.050f).coerceIn(1.2f, 2.8f)
    val rowBands = listOf(
        0.00f to 0.36f,
        0.36f to 0.68f,
        0.68f to 1.00f,
    )
    val rowJointFractions = listOf(
        listOf(0.50f),
        listOf(0.26f, 0.74f),
        listOf(0.50f),
    )

    listOf(0.36f, 0.68f).forEach { rowFraction ->
        val y = topLeft.y + size.height * rowFraction
        drawLine(
            color = mortarColor,
            start = Offset(topLeft.x, y),
            end = Offset(topLeft.x + size.width, y),
            strokeWidth = mortarWidth,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f * alpha),
            start = Offset(topLeft.x, y - mortarWidth * 0.34f),
            end = Offset(topLeft.x + size.width, y - mortarWidth * 0.34f),
            strokeWidth = mortarWidth * 0.26f,
        )
    }

    rowBands.forEachIndexed { index, (rowStartFraction, rowEndFraction) ->
        val rowStart = topLeft.y + size.height * rowStartFraction
        val rowEnd = topLeft.y + size.height * rowEndFraction
        rowJointFractions[index].forEach { jointFraction ->
            val x = topLeft.x + size.width * jointFraction
            drawLine(
                color = mortarColor,
                start = Offset(x, rowStart),
                end = Offset(x, rowEnd),
                strokeWidth = mortarWidth,
            )
        }
    }

    listOf(
        Pair(Offset(0.10f, 0.14f), Size(0.28f, 0.10f)),
        Pair(Offset(0.60f, 0.14f), Size(0.22f, 0.10f)),
        Pair(Offset(0.08f, 0.48f), Size(0.18f, 0.08f)),
        Pair(Offset(0.40f, 0.46f), Size(0.18f, 0.08f)),
        Pair(Offset(0.70f, 0.80f), Size(0.18f, 0.07f)),
    ).forEach { (offsetFraction, sizeFraction) ->
        val accentTopLeft =
            topLeft + Offset(size.width * offsetFraction.x, size.height * offsetFraction.y)
        drawRoundRect(
            color = brickStroke,
            topLeft = accentTopLeft,
            size = Size(size.width * sizeFraction.width, size.height * sizeFraction.height),
            cornerRadius = CornerRadius(cornerRadius.x * 0.20f, cornerRadius.y * 0.20f),
        )
    }

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius.x * 0.52f, cornerRadius.y * 0.52f),
        style = Stroke(width = (minOf(size.width, size.height) * 0.028f).coerceAtLeast(1f)),
    )
}

private fun DrawScope.drawSoundWavePattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    pulse: Float,
) {
    val minDim = minOf(size.width, size.height)
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)
    val strokeWidth = (minDim * 0.072f).coerceIn(2.4f, 6.0f)

    // Base container: faded version of the tone color
    drawRoundRect(
        color = toneColor.copy(alpha = 0.25f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(minDim * 0.22f, minDim * 0.22f),
    )

    // Horizontal baseline (also faded tone color)
    drawLine(
        color = toneColor.copy(alpha = 0.40f * alpha),
        start = Offset(topLeft.x + size.width * 0.12f, center.y),
        end = Offset(topLeft.x + size.width * 0.88f, center.y),
        strokeWidth = strokeWidth * 0.30f,
    )

    // spikes
    val spikeCount = 9
    val spikeGap = (size.width * 0.72f) / (spikeCount - 1)
    val startX = topLeft.x + size.width * 0.14f

    val seed = (topLeft.x.toInt() * 37 + topLeft.y.toInt())

    for (i in 0 until spikeCount) {
        val x = startX + i * spikeGap
        val distFromCenter = abs(i - (spikeCount - 1) / 2f) / ((spikeCount - 1) / 2f)
        val centerWeight = 1.0f - (distFromCenter * 0.6f)

        val spikeSeed = seed + i * 997
        val uniqueFactor = 0.4f + (spikeSeed % 60) / 100f

        val baseHeight = minDim * 0.75f * centerWeight * uniqueFactor
        val pulseEffect = 0.12f * minDim * sin(pulse * 6.8f + i * 0.8f + seed * 0.15f)
        val finalHeight = (baseHeight + pulseEffect).coerceIn(minDim * 0.10f, minDim * 0.90f)

        // Main spike (Solid tone color)
        drawLine(
            color = toneColor.copy(alpha = alpha),
            start = Offset(x, center.y - finalHeight / 2f),
            end = Offset(x, center.y + finalHeight / 2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawPrismRefraction(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    // Base background layer
    val prismBg = lerp(Color.White, toneColor, 0.20f).copy(alpha = alpha)
    drawRoundRect(
        color = prismBg,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
    )

    // CORE PEAK POINT ANIMATED
    val peakMotionRadius = size.width * 0.12f // Slightly smaller radius
    val peakMotionAngle = pulse * PI.toFloat() // Half speed rotation (0 to 180 deg and back)
    val peakOffsetX = cos(peakMotionAngle) * peakMotionRadius
    val peakOffsetY = sin(peakMotionAngle) * peakMotionRadius

    val peak = topLeft + Offset(
        size.width * 0.5f + peakOffsetX,
        size.height * 0.5f + peakOffsetY
    )

    // Facet drawing with tiling logic to cover whole area
    fun drawSolidFacet(points: List<Offset>, facetAlpha: Float, isHighlight: Boolean) {
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
                close()
            }
        }

        val displayColor = if (isHighlight) {
            tintColor(toneColor, facetAlpha * 0.7f)
        } else {
            shadeColor(toneColor, facetAlpha * 0.7f)
        }.copy(alpha = alpha * 0.95f)

        drawPath(path = path, color = displayColor)

        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.22f * alpha),
            style = Stroke(width = 0.9f)
        )
    }

    val corners = listOf(
        topLeft,
        topLeft + Offset(size.width, 0f),
        topLeft + Offset(size.width, size.height),
        topLeft + Offset(0f, size.height)
    )

    // Midpoints on each edge to create 8 non-overlapping segments
    val tMid = topLeft + Offset(size.width * 0.5f, 0f)
    val rMid = topLeft + Offset(size.width, size.height * 0.5f)
    val bMid = topLeft + Offset(size.width * 0.5f, size.height)
    val lMid = topLeft + Offset(0f, size.height * 0.5f)

    // Draw 8 non-overlapping facets (triangles) that meet at the peak
    drawSolidFacet(listOf(corners[0], tMid, peak), 0.08f, true)
    drawSolidFacet(listOf(tMid, corners[1], peak), 0.25f, false)
    drawSolidFacet(listOf(corners[1], rMid, peak), 0.05f, true)
    drawSolidFacet(listOf(rMid, corners[2], peak), 0.42f, false)
    drawSolidFacet(listOf(corners[2], bMid, peak), 0.12f, true)
    drawSolidFacet(listOf(bMid, corners[3], peak), 0.38f, false)
    drawSolidFacet(listOf(corners[3], lMid, peak), 0.15f, true)
    drawSolidFacet(listOf(lMid, corners[0], peak), 0.52f, false)

    // Sharp light streaks
    drawLine(
        color = Color.White.copy(alpha = 0.35f * alpha),
        start = corners[0],
        end = peak,
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = 0.25f * alpha),
        start = corners[2],
        end = peak,
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )

    // Shimmer at center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f * alpha), Color.Transparent),
            center = peak,
            radius = size.width * 0.20f
        ),
        center = peak,
        radius = size.width * 0.20f
    )

    // Outer frame
    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.8f)
    )
}

private fun DrawScope.drawGridSplitPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
) {
    val minDim = minOf(size.width, size.height)
    val gridCount = 3
    val gapPx = (minDim * 0.05f).coerceIn(1.5f, 4f)
    val cellSize = (minDim - (gapPx * (gridCount - 1))) / gridCount

    val innerCornerRadius = CornerRadius(cornerRadius.x * 0.4f, cornerRadius.y * 0.4f)

    for (row in 0 until gridCount) {
        for (col in 0 until gridCount) {
            val cellTopLeft = topLeft + Offset(
                col * (cellSize + gapPx),
                row * (cellSize + gapPx)
            )

            // Base cell body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(toneColor, 0.10f).copy(alpha = alpha),
                        shadeColor(toneColor, 0.15f).copy(alpha = alpha),
                    ),
                    startY = cellTopLeft.y,
                    endY = cellTopLeft.y + cellSize
                ),
                topLeft = cellTopLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = innerCornerRadius,
            )

            // Cell highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f * alpha),
                topLeft = cellTopLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = innerCornerRadius,
                style = Stroke(width = 1.0f)
            )
        }
    }
}

private data class StableStar(
    val x: Float,
    val y: Float,
    val radiusFactor: Float,
    val alpha: Float,
    val twinkle: Boolean,
)

private fun cosmicSeed(topLeft: Offset, size: Size, colorSeed: Int): Int {
    return CosmicFieldSeed
}

private fun stableStarField(seed: Int): List<StableStar> {
    var state = seed.toLong() and 0xFFFF_FFFFL

    fun nextFloat(): Float {
        state = ((state * 1_664_525L) + 1_013_904_223L) and 0xFFFF_FFFFL
        return (((state ushr 8) and 0x00FF_FFFFL).toFloat() / 0x00FF_FFFF.toFloat()).coerceIn(
            0f,
            1f
        )
    }

    return List(CosmicStarCount) {
        StableStar(
            x = 0.08f + (nextFloat() * 0.84f),
            y = 0.08f + (nextFloat() * 0.84f),
            radiusFactor = 0.010f + (nextFloat() * 0.026f),
            alpha = 0.52f + (nextFloat() * 0.44f),
            twinkle = nextFloat() > 0.56f,
        )
    }
}

private fun marbleVeinPath(
    topLeft: Offset,
    size: Size,
    points: List<Offset>,
): Path = Path().apply {
    val resolved = points.map { fraction ->
        Offset(topLeft.x + size.width * fraction.x, topLeft.y + size.height * fraction.y)
    }
    if (resolved.isEmpty()) return@apply
    moveTo(resolved.first().x, resolved.first().y)
    resolved.windowed(size = 2).forEach { (start, end) ->
        val controlX = (start.x + end.x) / 2f
        cubicTo(
            controlX,
            start.y,
            controlX,
            end.y,
            end.x,
            end.y,
        )
    }
}

private fun hexagonPath(center: Offset, radius: Float): Path = Path().apply {
    repeat(6) { index ->
        val angle = ((PI / 180.0) * ((60 * index) - 30)).toFloat()
        val point = center + Offset(cos(angle) * radius, sin(angle) * radius)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}


@Immutable
private data class BoardCellVisual(
    val fillInsetPx: Float,
    val previewInsetPx: Float,
    val specialInsetPx: Float,
    val cornerRadius: CornerRadius,
    val innerCornerRadiusPx: Float,
)

private fun boardCellVisual(
    cellWidth: Float,
    cellHeight: Float,
    style: BlockVisualStyle,
): BoardCellVisual {
    val minSize = minOf(cellWidth, cellHeight)
    val fillInset = (minSize * 0.085f).coerceIn(1.5f, 3.2f)
    val previewInset = (minSize * 0.06f).coerceIn(1.4f, 2.4f)
    val specialInset = (minSize * 0.14f).coerceIn(4f, 6f)
    val outerRadius = minSize * 0.16f * style.cornerScale()
    return BoardCellVisual(
        fillInsetPx = fillInset,
        previewInsetPx = previewInset,
        specialInsetPx = specialInset,
        cornerRadius = CornerRadius(outerRadius, outerRadius),
        innerCornerRadiusPx = outerRadius * when (style) {
            BlockVisualStyle.Flat -> 0.78f
            BlockVisualStyle.Bubble -> 0.92f
            BlockVisualStyle.Outline -> 0.70f
            BlockVisualStyle.Sharp3D -> 0.22f
            BlockVisualStyle.Wood -> 0.66f
            BlockVisualStyle.GridSplit -> 0.28f
            BlockVisualStyle.Crystal -> 0f
            BlockVisualStyle.DynamicLiquid -> 0.70f
            BlockVisualStyle.Tornado -> 0.88f
            BlockVisualStyle.HoneycombTexture -> 0.72f
            BlockVisualStyle.SpiderWeb -> 0.44f
            BlockVisualStyle.Cosmic -> 0.56f
            BlockVisualStyle.Brick -> 0.46f
            BlockVisualStyle.SoundWave -> 0.82f
            BlockVisualStyle.Prism -> 0.68f
            BlockVisualStyle.Flame -> 0.88f
            BlockVisualStyle.Gears -> 0.18f
            BlockVisualStyle.Pixel -> 0.08f
            BlockVisualStyle.Cyberpunk -> 0.68f
            BlockVisualStyle.NeonGlow -> 0.82f
            BlockVisualStyle.LiquidMarble -> 0.90f
            BlockVisualStyle.Holographic -> 0.44f
            BlockVisualStyle.GlitchTech -> 0.18f
            BlockVisualStyle.AuraEnergy -> 0.96f
            BlockVisualStyle.CircuitBoard -> 0.54f
            BlockVisualStyle.LightBurst -> 0.76f
        },
    )
}

private fun DrawScope.drawNeonGlowPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val glowPulse = (0.7f + 0.3f * sin(pulse * PI.toFloat() * 2f)).coerceIn(0f, 1f)
    val minDim = minOf(size.width, size.height)

    // Outer glow layers
    for (i in 1..3) {
        val glowSize = Size(
            size.width + (i * minDim * 0.08f),
            size.height + (i * minDim * 0.08f)
        )
        val glowTopLeft = Offset(
            topLeft.x - (glowSize.width - size.width) / 2f,
            topLeft.y - (glowSize.height - size.height) / 2f
        )
        drawRoundRect(
            color = toneColor.copy(alpha = 0.08f * (4 - i) * glowPulse * alpha),
            topLeft = glowTopLeft,
            size = glowSize,
            cornerRadius = CornerRadius(cornerRadius.x * 1.4f, cornerRadius.y * 1.4f),
        )
    }

    // Inner Wiring
    clipPath(roundedClipPath(topLeft, size, cornerRadius)) {
        // Multiple Neon Tubes
        neonGlowTubeSpecs().forEach { spec ->
            val insetFactor = spec.insetFactor
            val tubeSize = Size(
                (size.width * (1f - insetFactor * 2)).coerceAtLeast(0f),
                (size.height * (1f - insetFactor * 2)).coerceAtLeast(0f)
            )
            val tubeTopLeft = topLeft + Offset(size.width * insetFactor, size.height * insetFactor)
            val tubeCorner = CornerRadius(
                (cornerRadius.x * spec.cornerScale).coerceAtLeast(0f),
                (cornerRadius.y * spec.cornerScale).coerceAtLeast(0f)
            )

            drawRoundRect(
                color = toneColor.copy(alpha = spec.tubeAlpha * alpha),
                topLeft = tubeTopLeft,
                size = tubeSize,
                cornerRadius = tubeCorner,
                style = Stroke(width = spec.strokeWidthDp.dp.toPx())
            )

            drawRoundRect(
                color = Color.White.copy(alpha = spec.coreAlpha * alpha),
                topLeft = tubeTopLeft,
                size = tubeSize,
                cornerRadius = tubeCorner,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

internal data class NeonGlowTubeSpec(
    val insetFactor: Float,
    val strokeWidthDp: Float,
    val tubeAlpha: Float,
    val coreAlpha: Float,
    val cornerScale: Float,
)

internal fun neonGlowTubeSpecs(): List<NeonGlowTubeSpec> = listOf(
    NeonGlowTubeSpec(
        insetFactor = 0.09f,
        strokeWidthDp = 4.0f,
        tubeAlpha = 0.95f,
        coreAlpha = 0.90f,
        cornerScale = 0.85f,
    ),
    NeonGlowTubeSpec(
        insetFactor = 0.18f,
        strokeWidthDp = 4.0f,
        tubeAlpha = 0.70f,
        coreAlpha = 0.65f,
        cornerScale = 0.70f,
    ),
    NeonGlowTubeSpec(
        insetFactor = 0.27f,
        strokeWidthDp = 4.0f,
        tubeAlpha = 0.45f,
        coreAlpha = 0.40f,
        cornerScale = 0.55f,
    ),
)

private fun DrawScope.drawLiquidMarblePattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val phase = pulse * PI.toFloat() * 2f
    val minDim = minOf(size.width, size.height)

    clipPath(roundedClipPath(topLeft, size, cornerRadius)) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    shadeColor(toneColor, 0.45f).copy(alpha = alpha),
                    toneColor.copy(alpha = alpha),
                    tintColor(toneColor, 0.25f).copy(alpha = alpha),
                ),
                start = topLeft + Offset(size.width * 0.5f * sin(phase * 0.4f), 0f),
                end = topLeft + Offset(size.width * (1f - 0.5f * sin(phase * 0.4f)), size.height)
            ),
            topLeft = topLeft,
            size = size
        )

        repeat(5) { i ->
            val veinPhase = phase * (1.1f + i * 0.15f) + (i * PI.toFloat() * 0.4f)
            val veinPath = Path().apply {
                moveTo(topLeft.x + size.width * (i * 0.2f), topLeft.y)
                cubicTo(
                    topLeft.x + size.width * (0.5f + 0.5f * cos(veinPhase)),
                    topLeft.y + size.height * (0.2f + 0.3f * sin(veinPhase * 0.7f)),
                    topLeft.x + size.width * (0.5f + 0.5f * sin(veinPhase * 1.3f)),
                    topLeft.y + size.height * (0.8f + 0.2f * cos(veinPhase)),
                    topLeft.x + size.width * (1f - i * 0.2f),
                    topLeft.y + size.height,
                )
            }
            drawPath(
                path = veinPath,
                color = Color.White.copy(alpha = (0.22f + 0.12f * sin(phase + i)) * alpha),
                style = Stroke(width = minDim * (0.04f + 0.06f * i / 5f), cap = StrokeCap.Round)
            )
            drawPath(
                path = veinPath,
                color = tintColor(toneColor, 0.2f).copy(alpha = 0.12f * alpha),
                style = Stroke(width = minDim * 0.14f, cap = StrokeCap.Round)
            )
        }

        val sheenPos = Offset(
            size.width * (0.35f + 0.15f * cos(phase * 0.6f)),
            size.height * (0.35f + 0.15f * sin(phase * 0.6f))
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f * alpha), Color.Transparent),
                center = topLeft + sheenPos,
                radius = minDim * 0.75f
            ),
            topLeft = topLeft + sheenPos - Offset(minDim * 0.375f, minDim * 0.375f),
            size = Size(minDim * 0.75f, minDim * 0.75f)
        )
    }
}

private fun DrawScope.drawHolographicPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val phase = pulse * PI.toFloat() * 2f

    clipPath(roundedClipPath(topLeft, size, cornerRadius)) {
        val colors = listOf(
            tintColor(toneColor, 0.7f),
            tintColor(toneColor, 0.35f),
            toneColor,
            shadeColor(toneColor, 0.35f),
            shadeColor(toneColor, 0.7f),
            shadeColor(toneColor, 0.35f),
            toneColor,
            tintColor(toneColor, 0.35f)
        )

        val shiftedColors = colors.indices.map { i ->
            val offsetIndex = (i + (pulse * colors.size).toInt()) % colors.size
            colors[offsetIndex].copy(alpha = 0.75f * alpha)
        }

        drawRect(
            brush = Brush.linearGradient(
                colors = shiftedColors,
                start = topLeft + Offset(size.width * cos(phase), size.height * sin(phase)),
                end = topLeft + Offset(size.width * cos(phase + PI.toFloat()), size.height * sin(phase + PI.toFloat()))
            ),
            topLeft = topLeft,
            size = size
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f * alpha),
                    Color.White.copy(alpha = 0.15f * alpha),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.15f * alpha)
                ),
                startY = topLeft.y,
                endY = topLeft.y + size.height
            ),
            topLeft = topLeft,
            size = size
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.65f * alpha),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawGlitchTechPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val rand = kotlin.random.Random((pulse * 20).toInt())
    val isGlitching = rand.nextFloat() > 0.85f
    val glitchOffset = if (isGlitching) Offset(rand.nextFloat() * 4f - 2f, rand.nextFloat() * 4f - 2f) else Offset.Zero

    clipPath(roundedClipPath(topLeft, size, cornerRadius)) {
        drawRect(color = Color(0xFF1A1A1A).copy(alpha = alpha), topLeft = topLeft, size = size)

        repeat(5) { i ->
            val rectTop = topLeft.y + (i * size.height / 5f)
            val rectHeight = size.height / 5f
            val rectWidth = size.width * (0.4f + 0.5f * rand.nextFloat())
            drawRect(
                color = toneColor.copy(alpha = 0.15f * alpha),
                topLeft = Offset(topLeft.x, rectTop),
                size = Size(rectWidth, rectHeight)
            )
        }

        if (isGlitching) {
            drawRect(
                color = Color.Cyan.copy(alpha = 0.4f * alpha),
                topLeft = topLeft + glitchOffset,
                size = size
            )
            drawRect(
                color = Color.Magenta.copy(alpha = 0.4f * alpha),
                topLeft = topLeft - glitchOffset,
                size = size
            )
        }

        drawRect(
            color = toneColor.copy(alpha = 0.6f * alpha),
            topLeft = topLeft,
            size = size
        )

        repeat(10) { i ->
            drawLine(
                color = Color.Black.copy(alpha = 0.2f * alpha),
                start = Offset(topLeft.x, topLeft.y + (i * size.height / 10f)),
                end = Offset(topLeft.x + size.width, topLeft.y + (i * size.height / 10f)),
                strokeWidth = 1f
            )
        }
    }
}

private fun DrawScope.drawAuraEnergyPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val phase = pulse * PI.toFloat() * 2f
    val minDim = minOf(size.width, size.height)
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)

    // Animated Aura particles/rays
    repeat(16) { i ->
        val angle = (i * (360f / 16f)) * (PI / 180f).toFloat() + (phase * 0.5f)
        val rayLength = minDim * (0.7f + 0.35f * sin(phase * 2.5f + i))
        val end = center + Offset(cos(angle) * rayLength, sin(angle) * rayLength)
        
        drawLine(
            brush = Brush.radialGradient(
                colors = listOf(toneColor.copy(alpha = 0.85f * alpha), Color.Transparent),
                center = center,
                radius = rayLength
            ),
            start = center,
            end = end,
            strokeWidth = minDim * 0.18f,
            cap = StrokeCap.Round
        )

        // Add prominent energy sparks at ray ends
        if (i % 2 == 0) {
            val sparkPos = center + Offset(cos(angle) * rayLength * 0.85f, sin(angle) * rayLength * 0.85f)
            drawCircle(
                color = Color.White.copy(alpha = (0.5f + 0.5f * sin(phase + i)) * alpha),
                center = sparkPos,
                radius = minDim * 0.04f
            )
        }
    }

    // Core block
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                tintColor(toneColor, 0.45f).copy(alpha = alpha),
                toneColor.copy(alpha = alpha),
                shadeColor(toneColor, 0.45f).copy(alpha = alpha)
            ),
            center = center,
            radius = minDim * 0.52f
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius
    )
    
    // Bright center pulse
    drawCircle(
        color = Color.White.copy(alpha = (0.35f + 0.35f * sin(phase)) * alpha),
        center = center,
        radius = minDim * 0.38f
    )
}

private fun DrawScope.drawCircuitBoardPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val techColor = shadeColor(toneColor, 0.4f)
    
    clipPath(roundedClipPath(topLeft, size, cornerRadius)) {
        drawRect(color = techColor.copy(alpha = alpha), topLeft = topLeft, size = size)
        
        val pathColor = toneColor.copy(alpha = 0.6f * alpha)
        val dataColor = Color.White.copy(alpha = 0.8f * alpha)
        
        // Static circuit paths
        val paths = listOf(
            listOf(Offset(0.1f, 0.1f), Offset(0.4f, 0.1f), Offset(0.5f, 0.2f), Offset(0.5f, 0.5f)),
            listOf(Offset(0.9f, 0.9f), Offset(0.6f, 0.9f), Offset(0.5f, 0.8f), Offset(0.5f, 0.5f)),
            listOf(Offset(0.1f, 0.9f), Offset(0.1f, 0.6f), Offset(0.2f, 0.5f), Offset(0.5f, 0.5f)),
            listOf(Offset(0.9f, 0.1f), Offset(0.9f, 0.4f), Offset(0.8f, 0.5f), Offset(0.5f, 0.5f))
        )
        
        paths.forEach { points ->
            val p = Path().apply {
                moveTo(topLeft.x + size.width * points[0].x, topLeft.y + size.height * points[0].y)
                for (i in 1 until points.size) {
                    lineTo(topLeft.x + size.width * points[i].x, topLeft.y + size.height * points[i].y)
                }
            }
            drawPath(path = p, color = pathColor, style = Stroke(width = 1.5.dp.toPx()))
            
            // Nodes
            drawCircle(color = pathColor, center = topLeft + Offset(size.width * points[0].x, size.height * points[0].y), radius = 2.dp.toPx())
        }
        
        // Animated "data" pulses
        val dataProgress = (pulse * 2f) % 1f
        paths.forEach { points ->
            val pointIndex = (dataProgress * (points.size - 1)).toInt()
            val segmentProgress = (dataProgress * (points.size - 1)) % 1f
            val start = points[pointIndex]
            val end = points[pointIndex + 1]
            val currentPos = Offset(
                lerp(start.x, end.x, segmentProgress),
                lerp(start.y, end.y, segmentProgress)
            )
            drawCircle(color = dataColor, center = topLeft + Offset(size.width * currentPos.x, size.height * currentPos.y), radius = 2.5.dp.toPx())
        }
        
        // Center Chip
        drawRoundRect(
            color = toneColor.copy(alpha = alpha),
            topLeft = topLeft + Offset(size.width * 0.35f, size.height * 0.35f),
            size = Size(size.width * 0.3f, size.height * 0.3f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

private fun DrawScope.drawElectricPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val clipShape = roundedClipPath(topLeft, size, cornerRadius)
    val minDim = minOf(size.width, size.height)
    val phase = pulse.coerceIn(0f, 1f)
    val driftA = sin(phase * PI.toFloat() * 2f)
    val driftB = cos(phase * PI.toFloat() * 2f)
    val auroraA = Color(0xFF79F2FF)
    val auroraB = Color(0xFF91FFB8)
    val auroraC = Color(0xFFD6A8FF)
    val ribbonWidth = minDim * 0.22f

    clipPath(clipShape) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    shadeColor(toneColor, 0.34f).copy(alpha = alpha),
                    toneColor.copy(alpha = alpha),
                    tintColor(toneColor, 0.06f).copy(alpha = alpha),
                ),
                startY = topLeft.y,
                endY = topLeft.y + size.height,
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )
        val ribbonA = Path().apply {
            moveTo(topLeft.x - ribbonWidth * 0.2f, topLeft.y + size.height * 0.18f)
            cubicTo(
                topLeft.x + size.width * 0.18f,
                topLeft.y + size.height * (0.02f + driftA * 0.05f),
                topLeft.x + size.width * 0.48f,
                topLeft.y + size.height * (0.38f - driftB * 0.06f),
                topLeft.x + size.width * 0.78f,
                topLeft.y + size.height * (0.12f + driftA * 0.04f),
            )
            lineTo(topLeft.x + size.width * 0.78f, topLeft.y + size.height * 0.12f + ribbonWidth)
            cubicTo(
                topLeft.x + size.width * 0.50f,
                topLeft.y + size.height * (0.46f + driftB * 0.03f),
                topLeft.x + size.width * 0.22f,
                topLeft.y + size.height * (0.18f + driftA * 0.05f),
                topLeft.x - ribbonWidth * 0.2f,
                topLeft.y + size.height * 0.18f + ribbonWidth,
            )
            close()
        }
        val ribbonB = Path().apply {
            moveTo(topLeft.x + size.width * 0.16f, topLeft.y + size.height * 0.72f)
            cubicTo(
                topLeft.x + size.width * 0.30f,
                topLeft.y + size.height * (0.52f - driftB * 0.05f),
                topLeft.x + size.width * 0.62f,
                topLeft.y + size.height * (0.96f + driftA * 0.03f),
                topLeft.x + size.width * 0.90f,
                topLeft.y + size.height * 0.64f,
            )
            lineTo(topLeft.x + size.width * 0.90f, topLeft.y + size.height * 0.64f + ribbonWidth * 0.8f)
            cubicTo(
                topLeft.x + size.width * 0.60f,
                topLeft.y + size.height * (1.02f - driftA * 0.03f),
                topLeft.x + size.width * 0.30f,
                topLeft.y + size.height * (0.62f + driftB * 0.04f),
                topLeft.x + size.width * 0.16f,
                topLeft.y + size.height * 0.72f + ribbonWidth * 0.8f,
            )
            close()
        }
        drawPath(
            path = ribbonA,
            brush = Brush.linearGradient(
                colors = listOf(
                    auroraA.copy(alpha = 0.10f * alpha),
                    auroraC.copy(alpha = 0.32f * alpha),
                    auroraA.copy(alpha = 0.08f * alpha),
                ),
                start = topLeft,
                end = topLeft + Offset(size.width, size.height),
            ),
        )
        drawPath(
            path = ribbonB,
            brush = Brush.linearGradient(
                colors = listOf(
                    auroraB.copy(alpha = 0.08f * alpha),
                    auroraA.copy(alpha = 0.26f * alpha),
                    auroraB.copy(alpha = 0.10f * alpha),
                ),
                start = topLeft + Offset(0f, size.height),
                end = topLeft + Offset(size.width, 0f),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.26f * alpha),
                    Color.Transparent,
                ),
                center = topLeft + Offset(size.width * 0.52f, size.height * (0.46f + driftA * 0.03f)),
                radius = minDim * 0.38f,
            ),
            center = topLeft + Offset(size.width * 0.52f, size.height * (0.46f + driftA * 0.03f)),
            radius = minDim * 0.38f,
        )
    }

    drawRoundRect(
        color = auroraA.copy(alpha = 0.34f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = (minDim * 0.022f).coerceAtLeast(1.0f)),
    )
}

private fun DrawScope.drawLavaPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val clipShape = roundedClipPath(topLeft, size, cornerRadius)
    val minDim = minOf(size.width, size.height)
    val glassTop = tintColor(toneColor, 0.22f)
    val glassMid = toneColor
    val glassEdge = shadeColor(toneColor, 0.34f)
    val facetLight = Color.White.copy(alpha = 0.32f * alpha)

    clipPath(clipShape) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    glassTop.copy(alpha = alpha),
                    glassMid.copy(alpha = alpha),
                    glassEdge.copy(alpha = alpha),
                ),
                startY = topLeft.y,
                endY = topLeft.y + size.height,
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )

        val facets = listOf(
            listOf(Offset(0.10f, 0.18f), Offset(0.42f, 0.08f), Offset(0.54f, 0.34f), Offset(0.18f, 0.44f)),
            listOf(Offset(0.46f, 0.08f), Offset(0.88f, 0.18f), Offset(0.76f, 0.46f), Offset(0.54f, 0.34f)),
            listOf(Offset(0.18f, 0.48f), Offset(0.54f, 0.38f), Offset(0.42f, 0.88f), Offset(0.08f, 0.76f)),
            listOf(Offset(0.58f, 0.38f), Offset(0.78f, 0.48f), Offset(0.92f, 0.80f), Offset(0.46f, 0.90f)),
        )
        facets.forEachIndexed { index, points ->
            val path = Path().apply {
                moveTo(topLeft.x + size.width * points.first().x, topLeft.y + size.height * points.first().y)
                points.drop(1).forEach { point ->
                    lineTo(topLeft.x + size.width * point.x, topLeft.y + size.height * point.y)
                }
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (index % 2 == 0) facetLight else Color.White.copy(alpha = 0.14f * alpha),
                        Color.Transparent,
                    ),
                    start = topLeft,
                    end = topLeft + Offset(size.width, size.height),
                ),
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.18f * alpha),
                style = Stroke(width = (minDim * 0.014f).coerceAtLeast(0.9f)),
            )
        }

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.20f * alpha),
                    Color.Transparent,
                ),
                start = topLeft + Offset(size.width * 0.14f, size.height * 0.10f),
                end = topLeft + Offset(size.width * 0.58f, size.height * 0.58f),
            ),
            topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
            size = Size(size.width * 0.42f, size.height * 0.16f),
            cornerRadius = CornerRadius(cornerRadius.x * 0.44f, cornerRadius.y * 0.44f),
        )
    }
}

private fun DrawScope.drawLightBurstPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    val clipShape = roundedClipPath(topLeft, size, cornerRadius)
    val minDim = minOf(size.width, size.height)
    val center = topLeft + Offset(size.width * 0.5f, size.height * 0.5f)
    val phase = pulse.coerceIn(0f, 1f)
    val burst = 0.28f + (0.22f * sin(phase * PI.toFloat()).coerceIn(0f, 1f))
    val lightTone = tintColor(toneColor, 0.30f)
    val sweepAngle = phase * PI.toFloat() * 2f
    val orbitRadius = minDim * 0.30f

    clipPath(clipShape) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.46f + burst) * alpha),
                    lightTone.copy(alpha = 0.94f * alpha),
                    shadeColor(toneColor, 0.12f).copy(alpha = alpha),
                ),
                center = center,
                radius = minDim * 0.78f,
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )

        drawRoundRect(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.16f * alpha),
                    Color.Transparent,
                    lightTone.copy(alpha = 0.10f * alpha),
                    Color.Transparent,
                ),
                center = center,
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )

        repeat(8) { index ->
            val angle = ((PI.toFloat() * 2f) / 8f) * index + sweepAngle
            val inner = minDim * 0.12f
            val outer = minDim * 0.54f
            val start = center + Offset(cos(angle) * inner, sin(angle) * inner)
            val end = center + Offset(cos(angle) * outer, sin(angle) * outer)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f * alpha),
                        lightTone.copy(alpha = 0.18f * alpha),
                        Color.Transparent,
                    ),
                    start = start,
                    end = end,
                ),
                start = start,
                end = end,
                strokeWidth = (minDim * 0.030f).coerceAtLeast(1.2f),
                cap = StrokeCap.Round,
            )
        }

        repeat(3) { index ->
            val sparkAngle = sweepAngle + (index * (PI.toFloat() * 2f / 3f))
            val sparkCenter = center + Offset(cos(sparkAngle) * orbitRadius, sin(sparkAngle) * orbitRadius)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.84f * alpha),
                        Color.Transparent,
                    ),
                    center = sparkCenter,
                    radius = minDim * 0.08f,
                ),
                center = sparkCenter,
                radius = minDim * 0.08f,
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.86f * alpha),
                    Color.White.copy(alpha = 0.24f * alpha),
                    Color.Transparent,
                ),
                center = center,
                radius = minDim * 0.26f,
            ),
            center = center,
            radius = minDim * 0.26f,
        )
    }

    drawRoundRect(
        color = Color.White.copy(alpha = 0.28f * alpha),
        topLeft = topLeft + Offset(size.width * 0.04f, size.height * 0.04f),
        size = Size(size.width * 0.92f, size.height * 0.92f),
        cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.86f),
        style = Stroke(width = (minDim * 0.018f).coerceAtLeast(0.9f)),
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

@Composable
internal fun boardCellInsetDp(cellSize: Dp): Dp {
    val density = LocalDensity.current
    return with(density) {
        (cellSize.toPx() * 0.085f).coerceIn(1.5f, 3.2f).toDp()
    }
}

@Composable
internal fun boardCellCornerRadiusDp(
    cellSize: Dp,
    style: BlockVisualStyle,
): Dp {
    val density = LocalDensity.current
    return with(density) {
        (cellSize.toPx() * 0.16f * style.cornerScale()).toDp()
    }
}

internal fun boardCellCornerRadiusPx(
    cellSizePx: Float,
    style: BlockVisualStyle,
): Float = (cellSizePx * 0.16f * style.cornerScale()).coerceAtLeast(0f)

internal fun boardFrameCornerRadiusDp(style: BlockVisualStyle): Dp = style.frameCornerRadius()
