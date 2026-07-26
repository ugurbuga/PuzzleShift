package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Videocam
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.ad_reward_boomblocks_clear
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingScene
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStage
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStateFactory
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.GameOverDialogRevealDurationMillis
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.dailychallenge.ChallengeTasksDock
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.game.onboarding.BoomBlocksInteractiveGameOnboardingOverlay
import com.ugurbuga.blockgames.ui.game.onboarding.BoomBlocksInteractiveGameOnboardingUi
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BOOM_BLOCKS_HINT_CYCLE_MILLIS = 1_100
private const val BOOM_BLOCKS_HINT_IDLE_DELAY_MILLIS = 3_000L
private const val BOOM_BLOCKS_HINT_BLINK_DURATION_MILLIS = 900L
private const val BOOM_BLOCKS_HINT_REPEAT_INTERVAL_MILLIS = 3_000L
private const val BOOM_BLOCKS_EXPLOSION_DURATION_MILLIS = 350
private const val BOOM_BLOCKS_FALL_DELAY_MILLIS = 220L
private const val BOOM_BLOCKS_FALL_DURATION_MILLIS = 450
private const val BOOM_BLOCKS_GAME_OVER_ROW_CLEAR_DURATION_MILLIS = 92

@Composable
fun BoomBlocksGameScreen(
    gameState: GameState,
    onTapCell: (GridPoint) -> Unit,
    onReplaceActivePiece: (SpecialBlockType) -> Unit = {},
    onRestart: () -> Unit,
    onRewardedRevive: () -> Unit = {},
    onBack: () -> Unit,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    adController: GameAdController = NoOpGameAdController,
    interactiveOnboardingScene: BoomBlocksOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    var cellSize by remember { mutableStateOf(0f) }
    var boardRectInRoot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var hostRectInRoot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var rewardedReviveLoading by remember { mutableStateOf(false) }

    var currentTime by remember { mutableStateOf(currentEpochMillis()) }
    LaunchedEffect(gameState.status) {
        while (gameState.status == GameStatus.Running) {
            kotlinx.coroutines.delay(150)
            currentTime = currentEpochMillis()
        }
    }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            gameOverBoardClearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (BOOM_BLOCKS_GAME_OVER_ROW_CLEAR_DURATION_MILLIS * gameState.config.rows).coerceAtLeast(1200),
                    easing = FastOutSlowInEasing,
                ),
            )
            showGameOverDialog = true
            gameOverDialogRevealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = GameOverDialogRevealDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            gameOverBoardClearProgress.snapTo(0f)
            gameOverDialogRevealProgress.snapTo(0f)
            showGameOverDialog = false
        }
    }

    val idleDurationMillis = (currentTime - gameState.lastActionTime).coerceAtLeast(0L)
    val hintBlinkWindowMillis = (idleDurationMillis - BOOM_BLOCKS_HINT_IDLE_DELAY_MILLIS)
        .takeIf { it >= 0L }
        ?.rem(BOOM_BLOCKS_HINT_REPEAT_INTERVAL_MILLIS)
    val showIdleHint = gameState.status == GameStatus.Running &&
        interactiveOnboardingScene == null &&
        hintBlinkWindowMillis != null &&
        hintBlinkWindowMillis < BOOM_BLOCKS_HINT_BLINK_DURATION_MILLIS

    val infiniteTransition = rememberInfiniteTransition(label = "boomBlocks")
    val hintPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BOOM_BLOCKS_HINT_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hintPhase",
    )

    val stylePulse = LocalBlockStylePulse.current
    val boardTopSpacing = if (interactiveOnboardingScene != null) 176.dp else 24.dp

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .onGloballyPositioned { coordinates ->
                    hostRectInRoot = coordinates.boundsInRoot()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MinimalTopBar(
                    gameState = gameState,
                    scoreHighlightStrengthProvider = { 0f },
                    scoreHighlightScaleProvider = { 1f },
                    remainingTimeLabel = stringResource(Res.string.time_remaining),
                    onBack = onBack,
                    onRestart = onRestart,
                    controlsEnabled = interactiveOnboardingScene == null,
                )

                Spacer(modifier = Modifier.height(boardTopSpacing))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows.toFloat())
                        .clip(RoundedCornerShape(GameUiShapeTokens.panelCorner))
                        .background(uiColors.gameSurface.copy(alpha = 0.8f))
                        .onGloballyPositioned { coordinates ->
                            cellSize = coordinates.size.width.toFloat() / gameState.config.columns
                            boardRectInRoot = coordinates.boundsInRoot()
                        }
                        .pointerInput(gameState.status, interactiveOnboardingScene) {
                            if (gameState.status == GameStatus.Running) {
                                detectTapGestures { offset ->
                                    val col = (offset.x / cellSize).toInt()
                                    val row = (offset.y / cellSize).toInt()
                                    if (col in 0 until gameState.config.columns && row in 0 until gameState.config.rows) {
                                        val tappedPoint = GridPoint(col, row)
                                        if (interactiveOnboardingScene != null) {
                                            val guidePoint = interactiveOnboardingScene.guidePoint
                                            if (guidePoint != null) {
                                                val group = findConnectedGroup(gameState.board, guidePoint)
                                                if (tappedPoint in group) {
                                                    onTapCell(tappedPoint)
                                                }
                                            }
                                        } else {
                                            onTapCell(tappedPoint)
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    GameGrid(
                        gameState = gameState,
                        modifier = Modifier.fillMaxSize(),
                        hintEnabled = showIdleHint,
                        hintPhase = hintPhase,
                        stylePulse = stylePulse,
                    )

                    if (gameState.status == GameStatus.GameOver || gameOverBoardClearProgress.value > 0f) {
                        GameOverBoardClearOverlay(
                            revealProgress = gameOverBoardClearProgress.value,
                            rowCount = gameState.config.rows,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(
                    modifier = if (interactiveOnboardingScene != null) {
                        Modifier.height(16.dp)
                    } else {
                        Modifier.weight(1f)
                    }
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameState.activeChallenge != null) {
                        ChallengeTasksDock(
                            challenge = gameState.activeChallenge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (interactiveOnboardingScene == null) {
                        SpecialActionAdButton(
                            tone = CellTone.Violet,
                            icon = Icons.Filled.AutoAwesome,
                            adController = adController,
                            onActivated = { onReplaceActivePiece(SpecialBlockType.None) },
                            stylePulse = stylePulse,
                        )
                    }
                }
            }

            if (interactiveOnboardingScene != null) {
                val boardRect = remember(boardRectInRoot, hostRectInRoot) {
                    if (boardRectInRoot == androidx.compose.ui.geometry.Rect.Zero || hostRectInRoot == androidx.compose.ui.geometry.Rect.Zero) {
                        androidx.compose.ui.geometry.Rect.Zero
                    } else {
                        androidx.compose.ui.geometry.Rect(
                            left = boardRectInRoot.left - hostRectInRoot.left,
                            top = boardRectInRoot.top - hostRectInRoot.top,
                            right = boardRectInRoot.right - hostRectInRoot.left,
                            bottom = boardRectInRoot.bottom - hostRectInRoot.top,
                        )
                    }
                }
                BoomBlocksInteractiveGameOnboardingOverlay(
                    ui = BoomBlocksInteractiveGameOnboardingUi(
                        scene = interactiveOnboardingScene,
                        currentStep = interactiveOnboardingCurrentStep,
                        totalSteps = interactiveOnboardingTotalSteps,
                        isAwaitingPlacementCommit = interactiveOnboardingAwaitingCommit,
                    ),
                    boardRect = boardRect,
                    cellSizePx = cellSize,
                    onBack = onBack,
                )
            }
        }

        if (interactiveOnboardingCompletionDialogVisible) {
            InteractiveOnboardingCompletionDialog(
                onStartGame = onInteractiveOnboardingStartGame,
                onReturnHome = onInteractiveOnboardingReturnHome,
            )
        }

        if (showGameOverDialog) {
            GameOverDialog(
                gameState = gameState,
                highestScore = highestScore,
                showNewHighScoreMessage = showNewHighScoreMessage,
                revealProgressProvider = { gameOverDialogRevealProgress.value },
                canUseExtraLife = !gameState.rewardedReviveUsed,
                isExtraLifeLoading = rewardedReviveLoading,
                showExtraLifeButton = adController !== NoOpGameAdController,
                onPlayAgain = {
                    if (gameState.activeChallenge?.isCompleted == true) {
                        onBack()
                    } else {
                        adController.showRestartInterstitial {
                            onRestart()
                        }
                    }
                },
                onUseExtraLife = onUseExtraLife@{
                    if (rewardedReviveLoading || gameState.rewardedReviveUsed) return@onUseExtraLife
                    rewardedReviveLoading = true
                    adController.showRewardedRevive { rewarded ->
                        rewardedReviveLoading = false
                        if (rewarded) {
                            onRewardedRevive()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SpecialActionAdButton(
    tone: CellTone,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    adController: GameAdController,
    onActivated: () -> Unit,
    stylePulse: Float = 0f,
) {
    var loading by remember { mutableStateOf(false) }

    TopBarActionBlockButton(
        tone = tone,
        icon = icon,
        contentDescription = stringResource(Res.string.ad_reward_boomblocks_clear),
        onClick = onClick@{
            if (loading) return@onClick
            loading = true
            adController.showRewardedAd { success ->
                loading = false
                if (success) {
                    onActivated()
                }
            }
        },
        enabled = !loading,
        pulse = stylePulse,
        size = 40.dp,
        showAdIcon = true,
        adIcon = Icons.Outlined.Videocam,
        extraAlpha = 0.8f,
    )
}

private enum class GravityDir { Up, Down, Left, Right }

private data class VisualBlock(
    val id: Int,
    val tone: CellTone,
    val sourceRow: Float,
    val targetRow: Int,
    val sourceCol: Float,
    val targetCol: Int
)

@Composable
internal fun GameGrid(
    gameState: GameState,
    modifier: Modifier = Modifier,
    hintEnabled: Boolean = false,
    hintPhase: Float = 0f,
    stylePulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val isDarkTheme = isBlockGamesDarkTheme(settings)
    val resolvedStyle = settings.blockVisualStyle
    val board = gameState.board

    var previousBoard by remember { mutableStateOf<BoardMatrix?>(null) }
    var visualBlocks by remember { mutableStateOf<List<VisualBlock>>(emptyList()) }
    val animationProgress = remember { Animatable(0f) }

    val explosionProgress = remember { Animatable(0f) }
    var explodedPoints by remember { mutableStateOf(emptySet<GridPoint>()) }
    var explosionTones by remember { mutableStateOf(mapOf<GridPoint, CellTone>()) }

    LaunchedEffect(board, gameState.clearAnimationToken) {
        val prev = previousBoard

        // Reset previous board on restart or significant change to prevent sliding from old game sessions
        val isRestart = gameState.status == GameStatus.Running && gameState.score == 0
        if (isRestart || prev == null || prev.columns != board.columns || prev.rows != board.rows) {
            val initialBlocks = mutableListOf<VisualBlock>()
            for (c in 0 until board.columns) {
                for (r in 0 until board.rows) {
                    val cell = board.cellAt(c, r) ?: continue
                    initialBlocks.add(VisualBlock(
                        id = cell.value,
                        tone = cell.tone,
                        sourceRow = r.toFloat(),
                        targetRow = r,
                        sourceCol = c.toFloat(),
                        targetCol = c,
                    ))
                }
            }
            visualBlocks = initialBlocks
            previousBoard = board
            animationProgress.snapTo(1f)
            return@LaunchedEffect
        }

        // Determine gravity direction based on explosion center
        val group = gameState.recentlyExplodedPoints
        val gravityDirection = if (group.isNotEmpty()) {
            val avgRow = group.map { it.row }.average().toFloat()
            val avgCol = group.map { it.column }.average().toFloat()
            val rows = board.rows.toFloat()
            val cols = board.columns.toFloat()

            val relDistToTop = avgRow / rows
            val relDistToBottom = (rows - 1 - avgRow) / rows
            val relDistToLeft = avgCol / cols
            val relDistToRight = (cols - 1 - avgCol) / cols

            val minDist = minOf(relDistToTop, relDistToBottom, relDistToLeft, relDistToRight)
            when (minDist) {
                relDistToTop -> GravityDir.Up
                relDistToBottom -> GravityDir.Down
                relDistToLeft -> GravityDir.Left
                else -> GravityDir.Right
            }
        } else GravityDir.Down

        // Map blocks to their previous positions
        val prevBlockPositions = mutableMapOf<Int, GridPoint>()
        for (c in 0 until prev.columns) {
            for (r in 0 until prev.rows) {
                prev.cellAt(c, r)?.let { prevBlockPositions[it.value] = GridPoint(c, r) }
            }
        }

        val newBlocks = mutableListOf<VisualBlock>()
        val columnRefillOffsets = IntArray(board.columns)
        val rowRefillOffsets = IntArray(board.rows)

        for (c in 0 until board.columns) {
            for (r in 0 until board.rows) {
                val cell = board.cellAt(c, r) ?: continue
                val prevPos = prevBlockPositions[cell.value]

                if (prevPos != null) {
                    newBlocks.add(VisualBlock(
                        id = cell.value,
                        tone = cell.tone,
                        sourceRow = prevPos.row.toFloat(),
                        targetRow = r,
                        sourceCol = prevPos.column.toFloat(),
                        targetCol = c,
                    ))
                } else {
                    // New block logic
                    val (sourceCol, sourceRow) = when (gravityDirection) {
                        GravityDir.Up -> {
                            val offset = board.rows + columnRefillOffsets[c]
                            columnRefillOffsets[c]++
                            c.toFloat() to offset.toFloat()
                        }
                        GravityDir.Down -> {
                            val offset = -1 - columnRefillOffsets[c]
                            columnRefillOffsets[c]++
                            c.toFloat() to offset.toFloat()
                        }
                        GravityDir.Left -> {
                            val offset = board.columns + rowRefillOffsets[r]
                            rowRefillOffsets[r]++
                            offset.toFloat() to r.toFloat()
                        }
                        GravityDir.Right -> {
                            val offset = -1 - rowRefillOffsets[r]
                            rowRefillOffsets[r]++
                            offset.toFloat() to r.toFloat()
                        }
                    }
                    newBlocks.add(VisualBlock(
                        id = cell.value,
                        tone = cell.tone,
                        sourceRow = sourceRow,
                        targetRow = r,
                        sourceCol = sourceCol,
                        targetCol = c,
                    ))
                }
            }
        }

        visualBlocks = newBlocks
        previousBoard = board
        animationProgress.stop()
        animationProgress.snapTo(0f)

        if (gameState.recentlyExplodedPoints.isNotEmpty()) {
            kotlinx.coroutines.delay(BOOM_BLOCKS_FALL_DELAY_MILLIS)
        }

        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(BOOM_BLOCKS_FALL_DURATION_MILLIS, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(gameState.clearAnimationToken) {
        if (gameState.recentlyExplodedPoints.isNotEmpty()) {
            explosionTones = gameState.recentlyExplodedTones
            explodedPoints = gameState.recentlyExplodedPoints
            explosionProgress.stop()
            explosionProgress.snapTo(1f)
            explosionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(BOOM_BLOCKS_EXPLOSION_DURATION_MILLIS, easing = LinearEasing)
            )
            explodedPoints = emptySet()
            explosionTones = emptyMap()
        }
    }

    val explodableGroups = remember(board) {
        val visited = mutableSetOf<GridPoint>()
        val result = mutableListOf<Set<GridPoint>>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.columns) {
                val point = GridPoint(col, row)
                if (point !in visited) {
                    val group = findConnectedGroup(board, point)
                    if (group.size >= 3) {
                        result.add(group)
                    }
                    visited.addAll(group)
                }
            }
        }
        result
    }

    val explodableCells = remember(explodableGroups) {
        explodableGroups.flatten().toSet()
    }

    val guidedGroup = remember(board, gameState.onboardingGuidePoint) {
        gameState.onboardingGuidePoint?.let { findConnectedGroup(board, it) } ?: emptySet()
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cellWidth = size.width / board.columns
        val cellHeight = size.height / board.rows

        clipRect {
            visualBlocks.forEach { block ->
                val progress = animationProgress.value
                val currentRow = block.sourceRow + (block.targetRow - block.sourceRow) * progress
                val currentCol = block.sourceCol + (block.targetCol - block.sourceCol) * progress

                val isExplodable = GridPoint(block.targetCol, block.targetRow) in explodableCells
                val isGuided = GridPoint(block.targetCol, block.targetRow) in guidedGroup
                val hintOffset = ((block.targetCol * 0.13f) + (block.targetRow * 0.07f)) % 1f
                val cellHintPhase = (hintPhase + hintOffset) % 1f
                val shimmer = ((sin(cellHintPhase * 2.0 * PI).toFloat()) + 1f) / 2f
                val emphasis = when {
                    isGuided -> 1f
                    !isExplodable -> 0f
                    hintEnabled -> 1f
                    else -> 0f
                }
                val scale = if (isExplodable && emphasis > 0f) {
                    1.01f + ((0.018f + (0.032f * emphasis)) * shimmer)
                } else {
                    1f
                }
                val drawSize = cellWidth * 0.92f * scale
                val offsetX = (cellWidth - drawSize) / 2
                val offsetY = (cellHeight - drawSize) / 2
                val blockTopLeft = Offset(
                    x = currentCol * cellWidth + offsetX,
                    y = currentRow * cellHeight + offsetY,
                )

                if (isExplodable && emphasis > 0f) {
                    val toneColor = block.tone.paletteColor(settings.blockColorPalette, isDarkTheme)
                    val glowSize = drawSize * (1.10f + (0.06f * emphasis))
                    val glowTopLeft = Offset(
                        x = blockTopLeft.x - (glowSize - drawSize) / 2f,
                        y = blockTopLeft.y - (glowSize - drawSize) / 2f,
                    )
                    val cornerRadiusPx = boardCellCornerRadiusPx(glowSize, resolvedStyle)
                    drawRoundRect(
                        color = toneColor.copy(alpha = 0.10f + (0.14f * emphasis) + (0.10f * shimmer)),
                        topLeft = glowTopLeft,
                        size = Size(glowSize, glowSize),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    )
                    drawRoundRect(
                        color = toneColor.copy(alpha = 0.10f + (0.16f * emphasis) + (0.12f * shimmer)),
                        topLeft = blockTopLeft,
                        size = Size(drawSize, drawSize),
                        cornerRadius = CornerRadius(boardCellCornerRadiusPx(drawSize, resolvedStyle)),
                        style = Stroke(width = cellWidth * (0.035f + (0.02f * emphasis))),
                    )
                }

                val cornerRadiusPx = boardCellCornerRadiusPx(drawSize, resolvedStyle)
                drawCellBody(
                    tone = block.tone,
                    palette = settings.blockColorPalette,
                    style = resolvedStyle,
                    isDark = isDarkTheme,
                    topLeft = blockTopLeft,
                    size = Size(drawSize, drawSize),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    pulse = stylePulse,
                )
            }

            if (explosionProgress.value > 0) {
                val alpha = explosionProgress.value

                explodedPoints.forEach { point ->
                    val tone = explosionTones[point] ?: CellTone.Cyan
                    val toneColor = tone.paletteColor(settings.blockColorPalette, isDarkTheme)
                    val centerX = point.column * cellWidth + cellWidth / 2
                    val centerY = point.row * cellHeight + cellHeight / 2

                    val radius = cellWidth * (1f - alpha) * 2.5f
                    drawCircle(
                        color = toneColor.copy(alpha = alpha * 0.4f),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )

                    repeat(12) { i ->
                        val angle = (i * 30f) * (PI / 180f).toFloat()
                        val dist = cellWidth * (1f - alpha) * 3f
                        val sparkleX = centerX + cos(angle.toDouble()).toFloat() * dist
                        val sparkleY = centerY + sin(angle.toDouble()).toFloat() * dist
                        drawCircle(
                            color = toneColor.copy(alpha = alpha),
                            radius = 5.dp.toPx() * alpha,
                            center = Offset(sparkleX, sparkleY)
                        )
                    }
                }
            }
        }
    }
}

internal fun findConnectedGroup(
    board: BoardMatrix,
    start: GridPoint
): Set<GridPoint> {
    val tone = board.toneAt(start.column, start.row) ?: return emptySet()
    val group = mutableSetOf<GridPoint>()
    val queue = mutableListOf(start)

    while (queue.isNotEmpty()) {
        val point = queue.removeAt(0)
        if (point in group) continue
        if (board.toneAt(point.column, point.row) == tone) {
            group.add(point)
            val neighbors = listOf(
                GridPoint(point.column + 1, point.row),
                GridPoint(point.column - 1, point.row),
                GridPoint(point.column, point.row + 1),
                GridPoint(point.column, point.row - 1)
            )
            for (neighbor in neighbors) {
                if (neighbor.column in 0 until board.columns &&
                    neighbor.row in 0 until board.rows &&
                    neighbor !in group
                ) {
                    queue.add(neighbor)
                }
            }
        }
    }
    return group
}

@Preview
@Composable
private fun BoomBlocksOnboardingBasicPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        val stage = BoomBlocksOnboardingStage.BasicExplosion
        val scene = BoomBlocksOnboardingStateFactory.scene(stage)
        BoomBlocksGameScreen(
            gameState = scene.gameState,
            onTapCell = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 1,
            interactiveOnboardingTotalSteps = 3,
        )
    }
}

@Preview
@Composable
private fun BoomBlocksOnboardingLargePreview() {
    BlockGamesTheme(settings = AppSettings()) {
        val stage = BoomBlocksOnboardingStage.LargeExplosion
        val scene = BoomBlocksOnboardingStateFactory.scene(stage)
        BoomBlocksGameScreen(
            gameState = scene.gameState,
            onTapCell = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 2,
            interactiveOnboardingTotalSteps = 3,
        )
    }
}

@Preview
@Composable
private fun BoomBlocksOnboardingGravityPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        val stage = BoomBlocksOnboardingStage.GravityShift
        val scene = BoomBlocksOnboardingStateFactory.scene(stage)
        BoomBlocksGameScreen(
            gameState = scene.gameState,
            onTapCell = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 3,
            interactiveOnboardingTotalSteps = 4,
        )
    }
}

@Preview
@Composable
private fun BoomBlocksOnboardingStrategicPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        val stage = BoomBlocksOnboardingStage.StrategicClears
        val scene = BoomBlocksOnboardingStateFactory.scene(stage)
        BoomBlocksGameScreen(
            gameState = scene.gameState,
            onTapCell = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 4,
            interactiveOnboardingTotalSteps = 4,
        )
    }
}

@Preview(name = "BoomBlocks Game", widthDp = 412, heightDp = 915)
@Composable
private fun BoomBlocksGameScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        BoomBlocksGameScreen(
            gameState = previewBoomBlocksState(),
            onTapCell = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
            highestScore = 2450,
        )
    }
}

@Preview(name = "BoomBlocks Game Dark", widthDp = 412, heightDp = 915)
@Composable
private fun BoomBlocksGameScreenDarkPreview() {
    BlockGamesTheme(settings = AppSettings(themeMode = com.ugurbuga.blockgames.game.model.AppThemeMode.Dark)) {
        BoomBlocksGameScreen(
            gameState = previewBoomBlocksState(),
            onTapCell = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
            highestScore = 2450,
        )
    }
}

private fun previewBoomBlocksState(): GameState {
    val config = GameConfig(columns = 8, rows = 12, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999)
    var board = BoardMatrix.empty(config.columns, config.rows)
    val tones = CellTone.entries.take(5)
    for (row in 6 until config.rows) {
        for (col in 0 until config.columns) {
            board = board.fill(
                points = listOf(GridPoint(col, row)),
                tone = tones[(col + row * 3) % tones.size],
                value = 0
            )
        }
    }
    return GameState(
        config = config,
        gameplayStyle = GameplayStyle.BoomBlocks,
        board = board,
        activePiece = null,
        nextQueue = emptyList(),
        score = 1240,
        linesCleared = 12,
        level = 3,
        difficultyStage = 1,
        secondsUntilDifficultyIncrease = 9_999,
        status = GameStatus.Running,
    )
}
