package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.FluffyBlockCollector
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.GameOverDialogRevealDurationMillis
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

@Composable
fun FluffyBlockGameScreen(
    gameState: GameState,
    @Suppress("UNUSED_PARAMETER") onRequestPreview: (Long, GridPoint) -> PlacementPreview?,
    onMoveCollector: (Long, GridPoint) -> Unit,
    onRestart: () -> Unit,
    onRewardedRevive: () -> Unit = {},
    onBack: () -> Unit,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    adController: GameAdController = NoOpGameAdController,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val appSettings = LocalAppSettings.current
    var cellSize by remember { mutableStateOf(0f) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    val isDark = isBlockGamesDarkTheme(appSettings)

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            gameOverBoardClearProgress.animateTo(1f, tween(1200))
            showGameOverDialog = true
            gameOverDialogRevealProgress.animateTo(1f, tween(GameOverDialogRevealDurationMillis))
        } else {
            gameOverBoardClearProgress.snapTo(0f)
            gameOverDialogRevealProgress.snapTo(0f)
            showGameOverDialog = false
        }
    }

    var draggingCollectorId by remember { mutableStateOf<Long?>(null) }
    var dragInitialTouchOffset by remember { mutableStateOf(Offset.Zero) }
    var dragInitialAnchor by remember { mutableStateOf(GridPoint(0, 0)) }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        uiColors.screenGradientTop,
                        uiColors.screenGradientBottom,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            MinimalTopBar(
                gameState = gameState,
                scoreHighlightStrengthProvider = { 0f },
                scoreHighlightScaleProvider = { 1f },
                remainingTimeLabel = "",
                onBack = onBack,
                onRestart = onRestart
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(GameUiShapeTokens.panelCorner))
                        .background(uiColors.gameSurface)
                        .onGloballyPositioned { coordinates ->
                            boardRectInRoot = coordinates.boundsInRoot()
                            cellSize = boardRectInRoot.width / gameState.config.columns
                        }
                        .pointerInput(gameState.status, gameState.fluffyBlockCollectors) {
                            if (gameState.status != GameStatus.Running) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val col = (offset.x / cellSize).toInt().coerceIn(0, gameState.config.columns - 1)
                                    val row = (offset.y / cellSize).toInt().coerceIn(0, gameState.config.rows - 1)
                                    val touchPoint = GridPoint(col, row)
                                    
                                    val collector = gameState.fluffyBlockCollectors.find { it.cells.contains(touchPoint) }
                                    if (collector != null) {
                                        draggingCollectorId = collector.id
                                        dragInitialAnchor = collector.anchor
                                        dragInitialTouchOffset = offset
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (draggingCollectorId != null) {
                                        val currentOffset = change.position
                                        val deltaX = currentOffset.x - dragInitialTouchOffset.x
                                        val deltaY = currentOffset.y - dragInitialTouchOffset.y
                                        
                                        // Use a threshold to trigger a slide
                                        val threshold = cellSize * 0.4f
                                        if (abs(deltaX) > threshold || abs(deltaY) > threshold) {
                                            val dx = if (abs(deltaX) > abs(deltaY)) deltaX.sign.toInt() else 0
                                            val dy = if (abs(deltaY) >= abs(deltaX)) deltaY.sign.toInt() else 0
                                            
                                            val currentCollector = gameState.fluffyBlockCollectors.find { it.id == draggingCollectorId }
                                            if (currentCollector != null) {
                                                val targetStep = GridPoint(
                                                    currentCollector.anchor.column + dx,
                                                    currentCollector.anchor.row + dy
                                                )
                                                
                                                // Only trigger if it's a valid step (within board)
                                                if (targetStep.column in 0 until gameState.config.columns && 
                                                    targetStep.row in 0 until gameState.config.rows) {
                                                    onMoveCollector(draggingCollectorId!!, targetStep)
                                                    
                                                    // Reset drag reference to allow subsequent slides in the same drag session
                                                    dragInitialTouchOffset = currentOffset
                                                    dragInitialAnchor = currentCollector.anchor
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { draggingCollectorId = null },
                                onDragCancel = { draggingCollectorId = null }
                            )
                        }
                ) {
                    val stylePulse = LocalBlockStylePulse.current
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val boardClearY = size.height * (1f - gameOverBoardClearProgress.value)
                        val cornerRadius = boardCellCornerRadiusPx(cellSize, appSettings.blockVisualStyle)
                        val cr = CornerRadius(cornerRadius, cornerRadius)
                        
                        clipRect(bottom = boardClearY) {
                            // Draw Board Cells (Fluffies)
                            for (r in 0 until gameState.config.rows) {
                                for (c in 0 until gameState.config.columns) {
                                    val cell = gameState.board.cellAt(c, r) ?: continue
                                    
                                    drawCellBody(
                                        baseColor = cell.tone.paletteColor(appSettings.blockColorPalette, isDark),
                                        palette = appSettings.blockColorPalette,
                                        style = appSettings.blockVisualStyle,
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize, cellSize),
                                        cornerRadius = cr,
                                        alpha = 1f,
                                        pulse = stylePulse
                                    )
                                }
                            }
                            
                            // Draw Collectors
                            gameState.fluffyBlockCollectors.forEach { collector ->
                                val color = collector.tone.paletteColor(appSettings.blockColorPalette, isDark)
                                
                                collector.kind.template.forEach { cellOffset ->
                                    val pt = cellOffset + collector.anchor
                                    // Outer border
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(pt.column * cellSize + 2f, pt.row * cellSize + 2f),
                                        size = Size(cellSize - 4f, cellSize - 4f),
                                        cornerRadius = cr,
                                        style = Stroke(width = 4f)
                                    )
                                    // Inner fill
                                    drawRoundRect(
                                        color = color.copy(alpha = 0.15f),
                                        topLeft = Offset(pt.column * cellSize + 4f, pt.row * cellSize + 4f),
                                        size = Size(cellSize - 8f, cellSize - 8f),
                                        cornerRadius = cr
                                    )
                                }
                                
                                // Collected count badge (now remaining capacity)
                                val text = collector.remainingCapacity.toString()
                                val textResult = textMeasurer.measure(
                                    text = text,
                                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                )
                                val textOffset = Offset(
                                    collector.anchor.column * cellSize + (cellSize - textResult.size.width) / 2f,
                                    collector.anchor.row * cellSize + (cellSize - textResult.size.height) / 2f
                                )
                                
                                // Draw text background circle inside anchor cell
                                drawCircle(
                                    color = color,
                                    radius = cellSize * 0.4f,
                                    center = Offset(collector.anchor.column * cellSize + cellSize / 2f, collector.anchor.row * cellSize + cellSize / 2f)
                                )
                                
                                drawText(
                                    textLayoutResult = textResult,
                                    topLeft = textOffset
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom Tray
            gameState.activePiece?.let { piece ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .height(64.dp)
                                .aspectRatio(1f)
                                .background(uiColors.gameSurface, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                           Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
                               val cornerRadius = boardCellCornerRadiusPx(size.width, appSettings.blockVisualStyle)
                               drawCellBody(
                                   baseColor = piece.tone.paletteColor(appSettings.blockColorPalette, isDark),
                                   palette = appSettings.blockColorPalette,
                                   style = appSettings.blockVisualStyle,
                                   topLeft = Offset.Zero,
                                   size = size,
                                   cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                   alpha = 1f,
                                   pulse = 0f
                               )
                           }
                        }
                    }
                }
            }
        }

        if (showGameOverDialog) {
            GameOverDialog(
                gameState = gameState,
                highestScore = highestScore,
                showNewHighScoreMessage = showNewHighScoreMessage,
                revealProgressProvider = { gameOverDialogRevealProgress.value },
                canUseExtraLife = false,
                isExtraLifeLoading = false,
                showExtraLifeButton = false,
                onPlayAgain = onRestart,
                onUseExtraLife = onRewardedRevive
            )
        }
    }
}

@Preview(name = "Fluffy Block - Dense Board")
@Composable
private fun FluffyBlockGameScreenDensePreview() {
    BlockGamesTheme(settings = AppSettings()) {
        val columns = 10
        val rows = 14
        var board = BoardMatrix.empty(columns, rows)
        val random = Random(123)
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                if (random.nextFloat() < 0.7f) {
                    board = board.fill(listOf(GridPoint(c, r)), CellTone.entries.random(random))
                }
            }
        }
        
        FluffyBlockGameScreen(
            gameState = GameState(
                config = GameConfig(columns = columns, rows = rows),
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 18,
                gameplayStyle = GameplayStyle.FluffyBlock,
                fluffyBlockCollectors = listOf(
                    FluffyBlockCollector(1, CellTone.Coral, PieceKind.Square, GridPoint(1, 1), 14),
                    FluffyBlockCollector(2, CellTone.Gold, PieceKind.L, GridPoint(4, 3), 11),
                    FluffyBlockCollector(3, CellTone.Emerald, PieceKind.Plus, GridPoint(7, 5), 18),
                    FluffyBlockCollector(4, CellTone.Blue, PieceKind.Square, GridPoint(2, 8), 12),
                    FluffyBlockCollector(5, CellTone.Violet, PieceKind.TriL, GridPoint(5, 10), 12)
                )
            ),
            onRequestPreview = { _, _ -> null },
            onMoveCollector = { _, _ -> },
            onRestart = {},
            onBack = {},
            highestScore = 5000
        )
    }
}

@Preview(name = "Fluffy Block - Dark Mode")
@Composable
private fun FluffyBlockGameScreenDarkPreview() {
    BlockGamesTheme(settings = AppSettings(themeMode = AppThemeMode.Dark)) {
        FluffyBlockGameScreen(
            gameState = GameState(
                config = GameConfig(columns = 8, rows = 12),
                board = BoardMatrix.empty(8, 12)
                    .fill(listOf(GridPoint(1,1), GridPoint(2,2)), CellTone.Emerald)
                    .fill(listOf(GridPoint(6,6), GridPoint(7,7)), CellTone.Blue),
                activePiece = null,
                nextQueue = emptyList(),
                score = 3420,
                linesCleared = 42,
                level = 3,
                difficultyStage = 1,
                secondsUntilDifficultyIncrease = 12,
                gameplayStyle = GameplayStyle.FluffyBlock,
                fluffyBlockCollectors = listOf(
                    FluffyBlockCollector(3, CellTone.Emerald, PieceKind.J, GridPoint(1, 1), 18),
                    FluffyBlockCollector(4, CellTone.Blue, PieceKind.Square, GridPoint(5, 5), 12)
                )
            ),
            onRequestPreview = { _, _ -> null },
            onMoveCollector = { _, _ -> },
            onRestart = {},
            onBack = {},
            highestScore = 10000
        )
    }
}
