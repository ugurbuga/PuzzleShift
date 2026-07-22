package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.blocksort_no_moves_hint
import blockgames.composeapp.generated.resources.blocksort_round_label
import blockgames.composeapp.generated.resources.blocksort_select_source_hint
import blockgames.composeapp.generated.resources.blocksort_selected_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_finish_body
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_finish_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_finish_title
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_match_body
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_match_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_match_title
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_pick_body
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_pick_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_pick_title
import blockgames.composeapp.generated.resources.interactive_onboarding_step_counter
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.BoardCell
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.BlockSortOnboardingScene
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStage
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.GameOverDialogRevealDurationMillis
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.RestartConfirmDialog
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.dailychallenge.ChallengeTasksDock
import com.ugurbuga.blockgames.ui.game.resolveGameText
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import org.jetbrains.compose.resources.stringResource

private const val BlockSortGameOverRowClearDurationMillis = 92
internal const val BlockSortMoveAnimationDurationMillis = 2720
internal const val BlockSortFinalMoveAnimationDurationMillis = 720
internal const val BlockSortLevelAnimationDurationMillis = 2200
internal const val BlockSortTotalTransitionPauseDurationMillis =
    BlockSortFinalMoveAnimationDurationMillis + BlockSortLevelAnimationDurationMillis
private const val BlockSortLevelTransitionHoldEnd = 0f
private const val BlockSortLevelTransitionClearEnd = 0.62f
private const val BlockSortLevelTransitionColumnExitEnd = 0.72f
private const val BlockSortLevelTransitionColumnEntryEnd = 0.84f
private val BlockSortColumnGap = 10.dp
private val BlockSortCellGap = 6.dp
private val BlockSortBoardRowGap = 14.dp
private val BlockSortColumnHorizontalPadding = 6.dp
private val BlockSortColumnVerticalPadding = 8.dp
private val BlockSortMoveAnimationEasing = CubicBezierEasing(0.16f, 0.84f, 0.24f, 1f)
private val BlockSortLevelAnimationEasing = CubicBezierEasing(0.16f, 1f, 0.30f, 1f)

internal data class BlockSortLevelTransitionPhases(
    val oldBoardClearProgress: Float,
    val oldBoardColumnExitProgress: Float,
    val newBoardColumnEntryProgress: Float,
    val newBoardRevealProgress: Float,
    val currentBoardAlpha: Float,
    val previousBoardAlpha: Float,
    val nextRoundBadgeAlpha: Float,
)

@Composable
fun BlockSortGameScreen(
    gameState: GameState,
    onRequestPreview: (Int, Int) -> PlacementPreview?,
    onMove: (Int, Int) -> Boolean,
    onRestart: () -> Unit,
    onRewardedRevive: () -> Unit = {},
    onRewardedAddEmptyColumn: () -> Unit = {},
    onBack: () -> Unit,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    adController: GameAdController = NoOpGameAdController,
    interactiveOnboardingScene: BlockSortOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val boardStyle = settings.blockVisualStyle
    val palette = settings.blockColorPalette

    var selectedSourceColumn by remember(gameState.board, interactiveOnboardingScene?.stage) {
        mutableStateOf<Int?>(null)
    }
    var previousDisplayedRoundLevel by remember { mutableIntStateOf(gameState.level) }
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var rewardedReviveLoading by remember(gameState.status) { mutableStateOf(false) }
    var rewardedAddColumnLoading by remember(gameState.level, gameState.config.columns) { mutableStateOf(false) }
    val roundDisplayTransitionProgress = remember { Animatable(1f) }
    val interactionPulse = rememberBlockSortInteractionPulse(
        enabled = interactiveOnboardingScene != null || selectedSourceColumn != null,
    )
    val isPendingRoundDisplayTransition = gameState.level > previousDisplayedRoundLevel
    val displayedRoundLevel = if (
        isPendingRoundDisplayTransition &&
        shouldShowPreviousBlockSortRound(roundDisplayTransitionProgress.value)
    ) {
        previousDisplayedRoundLevel
    } else {
        gameState.level
    }

    LaunchedEffect(gameState.level) {
        if (gameState.level > previousDisplayedRoundLevel) {
            roundDisplayTransitionProgress.snapTo(0f)
            try {
                roundDisplayTransitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = BlockSortTotalTransitionPauseDurationMillis,
                        easing = BlockSortLevelAnimationEasing,
                    ),
                )
            } finally {
                previousDisplayedRoundLevel = gameState.level
            }
        } else {
            previousDisplayedRoundLevel = gameState.level
        }
    }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            gameOverBoardClearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (BlockSortGameOverRowClearDurationMillis * gameState.config.rows).coerceAtLeast(1200),
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
            showRestartDialog = false
        } else {
            gameOverBoardClearProgress.snapTo(0f)
            gameOverDialogRevealProgress.snapTo(0f)
            showGameOverDialog = false
            rewardedReviveLoading = false
            rewardedAddColumnLoading = false
        }
    }

    LaunchedEffect(gameState.board, gameState.status) {
        val currentSelection = selectedSourceColumn
        if (gameState.status != GameStatus.Running || currentSelection == null || gameState.board.topOccupiedRow(currentSelection) == null) {
            selectedSourceColumn = null
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
            showExtraLifeButton = adController !== NoOpGameAdController && gameState.activeChallenge?.isCompleted != true,
            onPlayAgain = {
                if (gameState.activeChallenge?.isCompleted == true) {
                    onBack()
                } else {
                    adController.showRestartInterstitial {
                        onRestart()
                    }
                }
            },
            onUseExtraLife = {
                if (rewardedReviveLoading || gameState.rewardedReviveUsed) return@GameOverDialog
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

    if (showRestartDialog) {
        RestartConfirmDialog(
            onDismissRequest = { showRestartDialog = false },
            title = resolveGameText(gameText(GameTextKey.RestartConfirmTitle)),
            message = resolveGameText(gameText(GameTextKey.RestartConfirmBody)),
            confirmLabel = resolveGameText(gameText(GameTextKey.RestartConfirm)),
            dismissLabel = resolveGameText(gameText(GameTextKey.RestartCancel)),
            onConfirm = {
                showRestartDialog = false
                adController.showRestartInterstitial {
                    onRestart()
                }
            },
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .safeDrawingPadding(),
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
                    onRestart = { showRestartDialog = true },
                    controlsEnabled = interactiveOnboardingScene == null,
                    stylePulse = interactionPulse,
                )

                AnimatedVisibility(
                    visible = interactiveOnboardingScene != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    interactiveOnboardingScene?.let { scene ->
                        BlockSortOnboardingCard(
                            scene = scene,
                            currentStep = interactiveOnboardingCurrentStep,
                            totalSteps = interactiveOnboardingTotalSteps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                BlockSortBoard(
                    gameState = gameState,
                    selectedSourceColumn = selectedSourceColumn,
                    interactiveOnboardingScene = interactiveOnboardingScene,
                    onRequestPreview = onRequestPreview,
                    onColumnTapped = { column ->
                        if (gameState.status != GameStatus.Running) return@BlockSortBoard
                        val occupied = gameState.board.topOccupiedRow(column) != null
                        val onboardingScene = interactiveOnboardingScene
                        val selected = selectedSourceColumn
                        if (selected == null) {
                            if (!occupied) return@BlockSortBoard
                            if (onboardingScene != null && column != onboardingScene.guideSourceColumn) return@BlockSortBoard
                            selectedSourceColumn = column
                        } else {
                            if (column == selected) {
                                selectedSourceColumn = null
                                return@BlockSortBoard
                            }
                            if (onboardingScene != null && column !in onboardingScene.acceptedTargetColumns) {
                                selectedSourceColumn = null
                                return@BlockSortBoard
                            }
                            if (onMove(selected, column)) {
                                selectedSourceColumn = null
                            } else {
                                selectedSourceColumn = null
                            }
                        }
                    },
                    palette = palette,
                    boardStylePulse = interactionPulse,
                    boardStyle = boardStyle,
                    gameOverClearProgress = gameOverBoardClearProgress.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                BlockSortInfoPanel(
                    gameState = gameState,
                    displayedRoundLevel = displayedRoundLevel,
                    selectedSourceColumn = selectedSourceColumn,
                    interactiveOnboardingScene = interactiveOnboardingScene,
                    adController = adController,
                    rewardedAddColumnLoading = rewardedAddColumnLoading,
                    onRewardedAddEmptyColumn = {
                        if (rewardedAddColumnLoading || gameState.blockSortBonusEmptyColumnUsed) return@BlockSortInfoPanel
                        rewardedAddColumnLoading = true
                        adController.showRewardedAd { success ->
                            rewardedAddColumnLoading = false
                            if (success) {
                                onRewardedAddEmptyColumn()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun BlockSortBoard(
    gameState: GameState,
    selectedSourceColumn: Int?,
    interactiveOnboardingScene: BlockSortOnboardingScene?,
    onRequestPreview: (Int, Int) -> PlacementPreview?,
    onColumnTapped: (Int) -> Unit,
    palette: com.ugurbuga.blockgames.game.model.BlockColorPalette,
    boardStylePulse: Float,
    boardStyle: com.ugurbuga.blockgames.game.model.BlockVisualStyle,
    gameOverClearProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val density = LocalDensity.current
    var previousBoard by remember { mutableStateOf(gameState.board) }
    var previousLevel by remember { mutableIntStateOf(gameState.level) }
    var levelTransitionPreviousBoard by remember { mutableStateOf<com.ugurbuga.blockgames.game.model.BoardMatrix?>(null) }
    var levelTransitionSourceBoard by remember { mutableStateOf<com.ugurbuga.blockgames.game.model.BoardMatrix?>(null) }
    var levelTransitionAnimatedCells by remember { mutableStateOf<List<BlockSortAnimatedCell>>(emptyList()) }
    var levelTransitionHiddenTargets by remember { mutableStateOf<Set<GridPoint>>(emptySet()) }
    var animatedCells by remember { mutableStateOf<List<BlockSortAnimatedCell>>(emptyList()) }
    var hiddenAnimatedTargets by remember { mutableStateOf<Set<GridPoint>>(emptySet()) }
    val moveAnimationProgress = remember { Animatable(1f) }
    val levelMoveAnimationProgress = remember { Animatable(1f) }
    val levelTransitionProgress = remember { Animatable(1f) }
    val isPendingLevelTransition = isPendingBlockSortLevelTransition(
        previousLevel = previousLevel,
        currentLevel = gameState.level,
        hasActiveTransitionBoard = levelTransitionPreviousBoard != null,
    )
    val pendingCompletedBoard = gameState.blockSortCompletedRoundBoard.takeIf { isPendingLevelTransition }
    val transitionPreviousBoard = levelTransitionPreviousBoard ?: pendingCompletedBoard
    val transitionSourceBoard = levelTransitionSourceBoard ?: previousBoard.takeIf { pendingCompletedBoard != null }
    val pendingLevelAnimatedCells = remember(
        transitionSourceBoard,
        transitionPreviousBoard,
        gameState.blockSortLastMovedCellValues,
    ) {
        if (transitionSourceBoard == null || transitionPreviousBoard == null) {
            emptyList()
        } else {
            resolveAnimatedMoveCells(
                previousBoard = transitionSourceBoard,
                currentBoard = transitionPreviousBoard,
                allowedCellValues = gameState.blockSortLastMovedCellValues,
                maxMovedCells = transitionPreviousBoard.rows,
            )
        }
    }
    val activeLevelAnimatedCells = levelTransitionAnimatedCells.ifEmpty { pendingLevelAnimatedCells }
    val activeLevelHiddenTargets = levelTransitionHiddenTargets.ifEmpty {
        activeLevelAnimatedCells.mapTo(mutableSetOf(), BlockSortAnimatedCell::to)
    }
    val levelMoveProgress = if (isPendingLevelTransition && activeLevelAnimatedCells.isNotEmpty()) 0f else levelMoveAnimationProgress.value
    val isLevelMoveAnimating =
        transitionPreviousBoard != null && activeLevelAnimatedCells.isNotEmpty() && levelMoveProgress < 1f
    val isLevelTransitioning = transitionPreviousBoard != null

    val previewMap = remember(gameState.board, selectedSourceColumn, interactiveOnboardingScene?.stage, isLevelTransitioning) {
        if (isLevelTransitioning) {
            emptyMap()
        } else {
            buildMap {
                val source = selectedSourceColumn ?: return@buildMap
                for (target in 0 until gameState.config.columns) {
                    put(target, onRequestPreview(source, target))
                }
            }
        }
    }
    val validTargetColumns = remember(previewMap, selectedSourceColumn) {
        previewMap.filterValues { it != null }.keys - setOfNotNull(selectedSourceColumn)
    }
    val pulsingCells = remember(gameState.board, selectedSourceColumn, isLevelTransitioning) {
        if (isLevelTransitioning) emptySet() else resolveMovableStackCells(gameState.board, selectedSourceColumn)
    }

    LaunchedEffect(gameState.board, gameState.level) {
        val oldBoard = previousBoard
        val oldLevel = previousLevel
        if (oldBoard != gameState.board) {
            hiddenAnimatedTargets = emptySet()
            animatedCells = emptyList()
            if (gameState.level == oldLevel) {
                val moveCells = resolveAnimatedMoveCells(
                    previousBoard = oldBoard,
                    currentBoard = gameState.board,
                    allowedCellValues = gameState.blockSortLastMovedCellValues,
                    maxMovedCells = gameState.config.rows,
                )
                previousBoard = gameState.board
                previousLevel = gameState.level
                if (moveCells.isNotEmpty()) {
                    animatedCells = moveCells
                    hiddenAnimatedTargets = moveCells.mapTo(mutableSetOf(), BlockSortAnimatedCell::to)
                    moveAnimationProgress.snapTo(0f)
                    try {
                        moveAnimationProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = BlockSortMoveAnimationDurationMillis, easing = BlockSortMoveAnimationEasing),
                        )
                    } finally {
                        hiddenAnimatedTargets = emptySet()
                        animatedCells = emptyList()
                    }
                }
            } else if (gameState.level > oldLevel) {
                val completedBoard = gameState.blockSortCompletedRoundBoard ?: oldBoard
                val levelMoveCells = resolveAnimatedMoveCells(
                    previousBoard = oldBoard,
                    currentBoard = completedBoard,
                    allowedCellValues = gameState.blockSortLastMovedCellValues,
                    maxMovedCells = completedBoard.rows,
                )
                levelTransitionSourceBoard = oldBoard
                levelTransitionPreviousBoard = completedBoard
                levelTransitionAnimatedCells = levelMoveCells
                levelTransitionHiddenTargets = levelMoveCells.mapTo(mutableSetOf(), BlockSortAnimatedCell::to)
                levelMoveAnimationProgress.snapTo(0f)
                levelTransitionProgress.snapTo(0f)
                try {
                    if (levelMoveCells.isNotEmpty()) {
                        levelMoveAnimationProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = BlockSortFinalMoveAnimationDurationMillis, easing = BlockSortMoveAnimationEasing),
                        )
                    }
                    levelTransitionHiddenTargets = emptySet()
                    levelTransitionAnimatedCells = emptyList()
                    levelTransitionSourceBoard = null
                    levelTransitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = BlockSortLevelAnimationDurationMillis, easing = BlockSortLevelAnimationEasing),
                    )
                } finally {
                    levelTransitionPreviousBoard = null
                    levelTransitionSourceBoard = null
                    levelTransitionAnimatedCells = emptyList()
                    levelTransitionHiddenTargets = emptySet()
                    previousBoard = gameState.board
                    previousLevel = gameState.level
                }
            } else {
                previousBoard = gameState.board
                previousLevel = gameState.level
            }
        } else {
            previousBoard = gameState.board
            previousLevel = gameState.level
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            val currentBoardLayout = remember(maxWidth, maxHeight, gameState.config.columns, gameState.config.rows) {
                calculateBlockSortBoardLayout(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    columnCount = gameState.config.columns,
                    rowCount = gameState.config.rows,
                )
            }
            val previousRoundBoard = transitionPreviousBoard
            val previousBoardLayout = remember(
                maxWidth,
                maxHeight,
                previousRoundBoard?.columns,
                previousRoundBoard?.rows,
            ) {
                previousRoundBoard?.let {
                    calculateBlockSortBoardLayout(
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        columnCount = it.columns,
                        rowCount = it.rows,
                    )
                }
            }
            val transitionPhases = if (isLevelTransitioning) {
                resolveBlockSortLevelTransitionPhases(
                    if (isPendingLevelTransition) 0f else levelTransitionProgress.value,
                )
            } else {
                BlockSortLevelTransitionPhases(
                    oldBoardClearProgress = 1f,
                    oldBoardColumnExitProgress = 0f,
                    newBoardColumnEntryProgress = 1f,
                    newBoardRevealProgress = 1f,
                    currentBoardAlpha = 1f,
                    previousBoardAlpha = 0f,
                    nextRoundBadgeAlpha = 0f,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        width = maxOf(currentBoardLayout.boardWidth, previousBoardLayout?.boardWidth ?: currentBoardLayout.boardWidth),
                        height = maxOf(currentBoardLayout.boardHeight, previousBoardLayout?.boardHeight ?: currentBoardLayout.boardHeight),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BlockSortBoardColumnsLayer(
                    board = gameState.board,
                    rowCount = gameState.config.rows,
                    layout = currentBoardLayout,
                    palette = palette,
                    boardStyle = boardStyle,
                    boardStylePulse = boardStylePulse,
                    selectedSourceColumn = selectedSourceColumn,
                    guideSourceColumn = interactiveOnboardingScene?.guideSourceColumn,
                    guideTargetColumns = interactiveOnboardingScene?.acceptedTargetColumns.orEmpty(),
                    previewMap = previewMap,
                    pulsingCells = pulsingCells,
                    hiddenCells = hiddenAnimatedTargets,
                    validTargetColumns = validTargetColumns,
                    interactionsEnabled = !isLevelTransitioning,
                    rowClearProgress = if (gameState.status == GameStatus.GameOver) gameOverClearProgress else 0f,
                    clearFromBottom = gameState.status == GameStatus.GameOver,
                    rowRevealProgress = transitionPhases.newBoardRevealProgress,
                    revealFromBottom = isLevelTransitioning,
                    columnFrameProgress = transitionPhases.newBoardColumnEntryProgress,
                    layerAlpha = transitionPhases.currentBoardAlpha,
                    layerScale = 1f,
                    layerTranslationY = 0.dp,
                    onColumnTapped = onColumnTapped,
                )

                previousRoundBoard?.let {
                    BlockSortBoardColumnsLayer(
                        board = it,
                        rowCount = it.rows,
                        layout = previousBoardLayout ?: currentBoardLayout,
                        palette = palette,
                        boardStyle = boardStyle,
                        boardStylePulse = boardStylePulse,
                        selectedSourceColumn = null,
                        guideSourceColumn = null,
                        guideTargetColumns = emptySet(),
                        previewMap = emptyMap(),
                        pulsingCells = emptySet(),
                        hiddenCells = if (isLevelMoveAnimating) activeLevelHiddenTargets else emptySet(),
                        validTargetColumns = emptySet(),
                        interactionsEnabled = false,
                        rowClearProgress = transitionPhases.oldBoardClearProgress,
                        clearFromBottom = false,
                        rowRevealProgress = 1f,
                        revealFromBottom = false,
                        columnFrameProgress = 1f - transitionPhases.oldBoardColumnExitProgress,
                        layerAlpha = transitionPhases.previousBoardAlpha,
                        layerScale = 1f,
                        layerTranslationY = 0.dp,
                        onColumnTapped = {},
                    )
                }

                if (isLevelMoveAnimating) {
                    val levelMoveLayout = previousBoardLayout ?: currentBoardLayout
                    val easedProgress = BlockSortMoveAnimationEasing.transform(levelMoveProgress)
                    activeLevelAnimatedCells.forEach { animatedCell ->
                        val from = levelMoveLayout.cellTopLeft(animatedCell.from)
                        val to = levelMoveLayout.cellTopLeft(animatedCell.to)
                        val fromXPx = density.run { from.x.toPx() }
                        val fromYPx = density.run { from.y.toPx() }
                        val toXPx = density.run { to.x.toPx() }
                        val toYPx = density.run { to.y.toPx() }
                        BlockCellPreview(
                            tone = animatedCell.tone,
                            palette = palette,
                            style = boardStyle,
                            size = levelMoveLayout.cellSize,
                            special = animatedCell.special,
                            pulse = 0.10f + (boardStylePulse * 0.30f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .graphicsLayer {
                                    translationX = lerpFloat(fromXPx, toXPx, easedProgress)
                                    translationY = lerpFloat(fromYPx, toYPx, easedProgress)
                                },
                        )
                    }
                }

                if (animatedCells.isNotEmpty() && !isLevelTransitioning) {
                    val easedProgress = BlockSortMoveAnimationEasing.transform(moveAnimationProgress.value)
                    animatedCells.forEach { animatedCell ->
                        val from = currentBoardLayout.cellTopLeft(animatedCell.from)
                        val to = currentBoardLayout.cellTopLeft(animatedCell.to)
                        val fromXPx = density.run { from.x.toPx() }
                        val fromYPx = density.run { from.y.toPx() }
                        val toXPx = density.run { to.x.toPx() }
                        val toYPx = density.run { to.y.toPx() }
                        BlockCellPreview(
                            tone = animatedCell.tone,
                            palette = palette,
                            style = boardStyle,
                            size = currentBoardLayout.cellSize,
                            special = animatedCell.special,
                            pulse = 0.10f + (boardStylePulse * 0.30f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .graphicsLayer {
                                    translationX = lerpFloat(fromXPx, toXPx, easedProgress)
                                    translationY = lerpFloat(fromYPx, toYPx, easedProgress)
                                },
                        )
                    }
                }

                if (transitionPhases.nextRoundBadgeAlpha > 0.01f) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .graphicsLayer {
                                alpha = transitionPhases.nextRoundBadgeAlpha
                                scaleX = 0.96f + (0.04f * transitionPhases.nextRoundBadgeAlpha)
                                scaleY = 0.96f + (0.04f * transitionPhases.nextRoundBadgeAlpha)
                            },
                        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
                        border = BorderStroke(1.dp, uiColors.success.copy(alpha = 0.42f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            text = "${stringResource(Res.string.blocksort_round_label)} ${gameState.level}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun BlockSortBoardColumnsLayer(
    board: com.ugurbuga.blockgames.game.model.BoardMatrix,
    rowCount: Int,
    layout: BlockSortBoardLayout,
    palette: com.ugurbuga.blockgames.game.model.BlockColorPalette,
    boardStyle: com.ugurbuga.blockgames.game.model.BlockVisualStyle,
    boardStylePulse: Float,
    selectedSourceColumn: Int?,
    guideSourceColumn: Int?,
    guideTargetColumns: Set<Int>,
    previewMap: Map<Int, PlacementPreview?>,
    pulsingCells: Set<GridPoint>,
    hiddenCells: Set<GridPoint>,
    validTargetColumns: Set<Int>,
    interactionsEnabled: Boolean,
    rowClearProgress: Float,
    clearFromBottom: Boolean,
    rowRevealProgress: Float,
    revealFromBottom: Boolean,
    columnFrameProgress: Float,
    layerAlpha: Float,
    layerScale: Float,
    layerTranslationY: androidx.compose.ui.unit.Dp,
    onColumnTapped: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .graphicsLayer {
                alpha = layerAlpha
                scaleX = layerScale
                scaleY = layerScale
                translationY = layerTranslationY.value
            },
        verticalArrangement = Arrangement.spacedBy(layout.boardRowGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        layout.columnRows.forEach { rowColumns ->
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(layout.columnGap),
            ) {
                rowColumns.forEach { column ->
                    BlockSortColumn(
                        board = board,
                        rowCount = rowCount,
                        column = column,
                        cellSize = layout.cellSize,
                        cellGap = layout.cellGap,
                        palette = palette,
                        boardStyle = boardStyle,
                        stylePulse = boardStylePulse,
                        isSelected = selectedSourceColumn == column,
                        isCompleted = isCompletedColumn(board, rowCount, column),
                        isGuideSource = guideSourceColumn == column,
                        isGuideTarget = column in guideTargetColumns,
                        isValidTarget = column in validTargetColumns,
                        previewCells = previewMap[column]?.occupiedCells.orEmpty().toSet(),
                        pulsingCells = pulsingCells,
                        hiddenCells = hiddenCells,
                        interactionsEnabled = interactionsEnabled,
                        rowClearProgress = rowClearProgress,
                        clearFromBottom = clearFromBottom,
                        rowRevealProgress = rowRevealProgress,
                        revealFromBottom = revealFromBottom,
                        columnFrameProgress = columnFrameProgress,
                        onTap = { onColumnTapped(column) },
                        modifier = Modifier.size(layout.columnWidth, layout.columnHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockSortColumn(
    board: com.ugurbuga.blockgames.game.model.BoardMatrix,
    rowCount: Int,
    column: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    cellGap: androidx.compose.ui.unit.Dp,
    palette: com.ugurbuga.blockgames.game.model.BlockColorPalette,
    boardStyle: com.ugurbuga.blockgames.game.model.BlockVisualStyle,
    stylePulse: Float,
    isSelected: Boolean,
    isCompleted: Boolean,
    isGuideSource: Boolean,
    isGuideTarget: Boolean,
    isValidTarget: Boolean,
    previewCells: Set<GridPoint>,
    pulsingCells: Set<GridPoint>,
    hiddenCells: Set<GridPoint>,
    interactionsEnabled: Boolean,
    rowClearProgress: Float,
    clearFromBottom: Boolean,
    rowRevealProgress: Float,
    revealFromBottom: Boolean,
    columnFrameProgress: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val borderColor = when {
        isSelected -> uiColors.launchGlow.copy(alpha = 0.86f)
        isValidTarget -> lerp(uiColors.panelStroke, uiColors.guideAccent, 0.36f + (stylePulse * 0.40f))
        isGuideSource || isGuideTarget -> uiColors.guideAccent.copy(alpha = 0.78f)
        isCompleted -> uiColors.success.copy(alpha = 0.76f)
        else -> uiColors.panelStroke.copy(alpha = 0.52f)
    }
    val containerColor = when {
        isSelected -> lerp(uiColors.panel, uiColors.launchGlow.copy(alpha = 0.22f), 0.28f)
        isValidTarget -> lerp(uiColors.panel, uiColors.guideAccent.copy(alpha = 0.16f + (stylePulse * 0.10f)), 0.22f)
        isGuideSource || isGuideTarget -> lerp(uiColors.panel, uiColors.guideAccent.copy(alpha = 0.18f), 0.22f)
        isCompleted -> lerp(uiColors.panel, uiColors.success.copy(alpha = 0.16f), 0.20f)
        else -> uiColors.panel.copy(alpha = 0.78f)
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
            .graphicsLayer {
                alpha = columnFrameProgress.coerceIn(0f, 1f)
                val scale = 0.92f + (0.08f * columnFrameProgress.coerceIn(0f, 1f))
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = interactionsEnabled, onClick = onTap),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.copy(alpha = 1f - (0.10f * rowClearProgress)),
        ),
        border = BorderStroke(
            width = if (isSelected || isGuideSource || isGuideTarget || isValidTarget) 1.5.dp else 1.dp,
            color = borderColor,
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
            for (row in 0 until rowCount) {
                val point = GridPoint(column, row)
                val cell = board.cellAt(column, row)
                val cellAlpha = if (clearFromBottom) {
                    rowClearAlphaFromBottom(row = row, rowCount = rowCount, clearProgress = rowClearProgress)
                } else {
                    rowClearAlpha(row = row, rowCount = rowCount, clearProgress = rowClearProgress)
                }
                val revealAlpha = if (revealFromBottom) {
                    rowRevealAlphaFromBottom(row = row, rowCount = rowCount, revealProgress = rowRevealProgress)
                } else {
                    rowRevealAlpha(row = row, rowCount = rowCount, revealProgress = rowRevealProgress)
                }
                val visibleAlpha = cellAlpha * revealAlpha
                val slotAlpha = cellAlpha
                val showOccupiedCell = cell != null && point !in hiddenCells && revealAlpha > 0.01f
                Box(
                    modifier = Modifier.size(cellSize),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!showOccupiedCell) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(uiColors.boardEmptyCell)
                                .graphicsLayer {
                                    alpha = slotAlpha * if (point in previewCells) (0.72f + (stylePulse * 0.28f)) else 1f
                                    scaleX = if (point in previewCells) 1f + (stylePulse * 0.09f) else 1f
                                    scaleY = if (point in previewCells) 1f + (stylePulse * 0.09f) else 1f
                                },
                        )
                    } else {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                alpha = visibleAlpha
                                val scaleBoost = when {
                                    point in pulsingCells -> 0.045f * stylePulse
                                    point in previewCells -> 0.080f * stylePulse
                                    else -> 0f
                                }
                                scaleX = 1f + scaleBoost
                                scaleY = 1f + scaleBoost
                            },
                        ) {
                            BlockCellPreview(
                                tone = cell.tone,
                                palette = palette,
                                style = boardStyle,
                                size = cellSize,
                                alpha = visibleAlpha,
                                pulse = when {
                                    point in pulsingCells -> 0.34f + (stylePulse * 0.78f)
                                    isGuideSource || isGuideTarget || isCompleted -> stylePulse * 0.55f
                                    else -> 0f
                                },
                            )
                        }
                    }

                    if (point in previewCells) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.5.dp,
                                    color = uiColors.guideAccent.copy(alpha = 0.56f + (0.22f * stylePulse)),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .background(uiColors.guideAccent.copy(alpha = 0.12f + (0.12f * stylePulse)))
                                .graphicsLayer {
                                    alpha = maxOf(slotAlpha, visibleAlpha) * (0.72f + (0.20f * stylePulse))
                                    scaleX = 0.92f + (0.16f * stylePulse)
                                    scaleY = 0.92f + (0.16f * stylePulse)
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockSortStatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.46f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = uiColors.guideAccent,
                modifier = Modifier.size(16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = uiColors.subtitle)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun BlockSortInfoPanel(
    gameState: GameState,
    displayedRoundLevel: Int,
    selectedSourceColumn: Int?,
    interactiveOnboardingScene: BlockSortOnboardingScene?,
    adController: GameAdController,
    rewardedAddColumnLoading: Boolean,
    onRewardedAddEmptyColumn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val helperText = when {
        interactiveOnboardingScene != null -> interactiveOnboardingScene.hintText()
        gameState.status == GameStatus.GameOver -> stringResource(Res.string.blocksort_no_moves_hint)
        selectedSourceColumn != null -> stringResource(Res.string.blocksort_selected_hint)
        else -> stringResource(Res.string.blocksort_select_source_hint)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BlockSortStatPill(
                    label = stringResource(Res.string.blocksort_round_label),
                    value = displayedRoundLevel.toString(),
                    icon = Icons.Filled.Flag,
                )
                if (adController !== NoOpGameAdController) {
                    BlockSortExtraColumnAdButton(
                        enabled = !rewardedAddColumnLoading && !gameState.blockSortBonusEmptyColumnUsed && gameState.status == GameStatus.Running,
                        onActivated = onRewardedAddEmptyColumn,
                    )
                }
                Text(
                    text = helperText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = uiColors.subtitle,
                    maxLines = 2,
                )
            }

            gameState.activeChallenge?.let { challenge ->
                Card(
                    shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                    colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.54f)),
                    border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.42f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    ChallengeTasksDock(
                        challenge = challenge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockSortExtraColumnAdButton(
    enabled: Boolean,
    onActivated: () -> Unit,
) {
    TopBarActionBlockButton(
        tone = CellTone.Gold,
        icon = Icons.Filled.Add,
        contentDescription = "Add empty column",
        onClick = onActivated,
        enabled = enabled,
        size = 40.dp,
        showAdIcon = true,
        extraAlpha = 0.84f,
    )
}

private fun isCompletedColumn(board: com.ugurbuga.blockgames.game.model.BoardMatrix, rowCount: Int, column: Int): Boolean {
    if (board.filledCellCount(column) != rowCount) return false
    val tone = board.topOccupiedRow(column)?.let { board.toneAt(column, it) } ?: return false
    for (row in 0 until rowCount) {
        if (board.toneAt(column, row) != tone) return false
    }
    return true
}

private data class BlockSortAnimatedCell(
    val from: GridPoint,
    val to: GridPoint,
    val tone: CellTone,
    val special: com.ugurbuga.blockgames.game.model.SpecialBlockType,
)

private data class BlockSortBoardLayout(
    val columnRows: List<List<Int>>,
    val cellSize: androidx.compose.ui.unit.Dp,
    val cellGap: androidx.compose.ui.unit.Dp,
    val columnGap: androidx.compose.ui.unit.Dp,
    val boardRowGap: androidx.compose.ui.unit.Dp,
    val columnWidth: androidx.compose.ui.unit.Dp,
    val columnHeight: androidx.compose.ui.unit.Dp,
    val boardWidth: androidx.compose.ui.unit.Dp,
    val boardHeight: androidx.compose.ui.unit.Dp,
) {
    fun cellTopLeft(point: GridPoint): androidx.compose.ui.unit.DpOffset {
        val columnOrigin = columnTopLeft(point.column)
        return androidx.compose.ui.unit.DpOffset(
            x = columnOrigin.x + BlockSortColumnHorizontalPadding,
            y = columnOrigin.y + BlockSortColumnVerticalPadding + ((cellSize + cellGap) * point.row),
        )
    }

    private fun columnTopLeft(column: Int): androidx.compose.ui.unit.DpOffset {
        columnRows.forEachIndexed { rowIndex, rowColumns ->
            val columnIndex = rowColumns.indexOf(column)
            if (columnIndex >= 0) {
                val rowWidth = (columnWidth * rowColumns.size) + (columnGap * (rowColumns.size - 1).coerceAtLeast(0))
                val startX = (boardWidth - rowWidth) / 2
                return androidx.compose.ui.unit.DpOffset(
                    x = startX + ((columnWidth + columnGap) * columnIndex),
                    y = (columnHeight + boardRowGap) * rowIndex,
                )
            }
        }
        return androidx.compose.ui.unit.DpOffset.Zero
    }
}

internal fun blockSortColumnGroups(columnCount: Int): List<List<Int>> {
    if (columnCount <= 4) return listOf((0 until columnCount).toList())
    val firstRowCount = (columnCount + 1) / 2
    return listOf(
        (0 until firstRowCount).toList(),
        (firstRowCount until columnCount).toList(),
    )
}

private fun calculateBlockSortBoardLayout(
    maxWidth: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp,
    columnCount: Int,
    rowCount: Int,
): BlockSortBoardLayout {
    val columnRows = blockSortColumnGroups(columnCount)
    val boardRowCount = columnRows.size
    val densestRow = columnRows.maxOf { it.size }
    val widthBasedCell = (
        maxWidth - (BlockSortColumnGap * (densestRow - 1).coerceAtLeast(0)) -
            (BlockSortColumnHorizontalPadding * 2 * densestRow)
        ) / densestRow
    val heightPerBoardRow = (
        maxHeight - (BlockSortBoardRowGap * (boardRowCount - 1).coerceAtLeast(0))
        ) / boardRowCount
    val heightBasedCell = (
        heightPerBoardRow - (BlockSortColumnVerticalPadding * 2) -
            (BlockSortCellGap * (rowCount - 1).coerceAtLeast(0))
        ) / rowCount
    val cellSize = minOf(widthBasedCell, heightBasedCell).coerceAtLeast(12.dp)
    val columnWidth = cellSize + (BlockSortColumnHorizontalPadding * 2)
    val columnHeight = (cellSize * rowCount) + (BlockSortCellGap * (rowCount - 1).coerceAtLeast(0)) + (BlockSortColumnVerticalPadding * 2)
    val boardWidth = columnRows.maxOf { rowColumns ->
        (columnWidth * rowColumns.size) + (BlockSortColumnGap * (rowColumns.size - 1).coerceAtLeast(0))
    }
    val boardHeight = (columnHeight * boardRowCount) + (BlockSortBoardRowGap * (boardRowCount - 1).coerceAtLeast(0))
    return BlockSortBoardLayout(
        columnRows = columnRows,
        cellSize = cellSize,
        cellGap = BlockSortCellGap,
        columnGap = BlockSortColumnGap,
        boardRowGap = BlockSortBoardRowGap,
        columnWidth = columnWidth,
        columnHeight = columnHeight,
        boardWidth = boardWidth,
        boardHeight = boardHeight,
    )
}

private fun resolveAnimatedMoveCells(
    previousBoard: com.ugurbuga.blockgames.game.model.BoardMatrix,
    currentBoard: com.ugurbuga.blockgames.game.model.BoardMatrix,
    allowedCellValues: Set<Int>,
    maxMovedCells: Int,
): List<BlockSortAnimatedCell> {
    if (previousBoard.columns != currentBoard.columns || previousBoard.rows != currentBoard.rows) return emptyList()
    if (previousBoard.occupiedCount != currentBoard.occupiedCount) return emptyList()
    if (allowedCellValues.isEmpty()) return emptyList()

    val beforeEntries = boardCellEntries(previousBoard, allowedCellValues)
    val currentEntries = boardCellEntries(currentBoard, allowedCellValues)
    if (beforeEntries.size != currentEntries.size) return emptyList()

    val moved = beforeEntries.mapNotNull { (key, beforeEntry) ->
        val currentEntry = currentEntries[key] ?: return emptyList()
        if (beforeEntry.first == currentEntry.first) {
            null
        } else {
            BlockSortAnimatedCell(
                from = beforeEntry.first,
                to = currentEntry.first,
                tone = currentEntry.second.tone,
                special = currentEntry.second.special,
            )
        }
    }
    if (moved.isEmpty() || moved.size > maxMovedCells) return emptyList()
    if (moved.map { it.tone }.toSet().size > 1) return emptyList()
    return moved.sortedBy { it.from.row }
}

private fun boardCellEntries(
    board: com.ugurbuga.blockgames.game.model.BoardMatrix,
    allowedCellValues: Set<Int>,
): Map<Int, Pair<GridPoint, BoardCell>> = buildMap {
    for (column in 0 until board.columns) {
        for (row in 0 until board.rows) {
            val cell = board.cellAt(column, row) ?: continue
            val animationKey = boardCellAnimationKey(cell, column, row)
            if (cell.value !in allowedCellValues) continue
            put(animationKey, GridPoint(column, row) to cell)
        }
    }
}

private fun boardCellAnimationKey(cell: BoardCell, column: Int, row: Int): Int {
    return cell.value.takeIf { it != 0 }
        ?: (((cell.tone.ordinal + 1) * 10_000) + ((column + 1) * 100) + row)
}


private fun lerpFloat(start: Float, stop: Float, progress: Float): Float = start + ((stop - start) * progress)

private fun transitionSegmentProgress(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

internal fun isPendingBlockSortLevelTransition(
    previousLevel: Int,
    currentLevel: Int,
    hasActiveTransitionBoard: Boolean,
): Boolean {
    return !hasActiveTransitionBoard && currentLevel > previousLevel
}

internal fun shouldShowPreviousBlockSortRound(progress: Float): Boolean {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val previousRoundVisibleUntilMillis =
        BlockSortFinalMoveAnimationDurationMillis + (BlockSortLevelAnimationDurationMillis * BlockSortLevelTransitionColumnExitEnd)
    val visibilityThreshold = previousRoundVisibleUntilMillis / BlockSortTotalTransitionPauseDurationMillis.toFloat()
    return clampedProgress < visibilityThreshold
}

internal fun resolveBlockSortLevelTransitionPhases(progress: Float): BlockSortLevelTransitionPhases {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val oldBoardClearProgress = transitionSegmentProgress(
        value = clampedProgress,
        start = BlockSortLevelTransitionHoldEnd,
        end = BlockSortLevelTransitionClearEnd,
    )
    val oldBoardColumnExitProgress = transitionSegmentProgress(
        value = clampedProgress,
        start = BlockSortLevelTransitionClearEnd,
        end = BlockSortLevelTransitionColumnExitEnd,
    )
    val newBoardColumnEntryProgress = transitionSegmentProgress(
        value = clampedProgress,
        start = BlockSortLevelTransitionColumnExitEnd,
        end = BlockSortLevelTransitionColumnEntryEnd,
    )
    val newBoardRevealProgress = transitionSegmentProgress(
        value = clampedProgress,
        start = BlockSortLevelTransitionColumnEntryEnd,
        end = 1f,
    )
    return BlockSortLevelTransitionPhases(
        oldBoardClearProgress = oldBoardClearProgress,
        oldBoardColumnExitProgress = oldBoardColumnExitProgress,
        newBoardColumnEntryProgress = newBoardColumnEntryProgress,
        newBoardRevealProgress = newBoardRevealProgress,
        currentBoardAlpha = if (clampedProgress < BlockSortLevelTransitionColumnExitEnd) 0f else 1f,
        previousBoardAlpha = if (clampedProgress >= BlockSortLevelTransitionColumnEntryEnd) 0f else 1f,
        nextRoundBadgeAlpha = transitionSegmentProgress(
            value = clampedProgress,
            start = BlockSortLevelTransitionColumnEntryEnd,
            end = 1f,
        ),
    )
}

internal fun rowClearAlpha(row: Int, rowCount: Int, clearProgress: Float): Float {
    if (clearProgress <= 0f) return 1f
    if (clearProgress >= 1f) return 0f
    val revealIndex = clearProgress * rowCount
    return (1f - (revealIndex - row).coerceIn(0f, 1f)).coerceIn(0f, 1f)
}

internal fun rowClearAlphaFromBottom(row: Int, rowCount: Int, clearProgress: Float): Float {
    return rowClearAlpha(
        row = (rowCount - 1 - row).coerceAtLeast(0),
        rowCount = rowCount,
        clearProgress = clearProgress,
    )
}

internal fun rowRevealAlpha(row: Int, rowCount: Int, revealProgress: Float): Float {
    if (revealProgress <= 0f) return 0f
    if (revealProgress >= 1f) return 1f
    val revealIndex = revealProgress * rowCount
    return (revealIndex - row).coerceIn(0f, 1f)
}

internal fun rowRevealAlphaFromBottom(row: Int, rowCount: Int, revealProgress: Float): Float {
    return rowRevealAlpha(
        row = (rowCount - 1 - row).coerceAtLeast(0),
        rowCount = rowCount,
        revealProgress = revealProgress,
    )
}

internal fun resolveMovableStackCells(
    board: com.ugurbuga.blockgames.game.model.BoardMatrix,
    selectedSourceColumn: Int?,
): Set<GridPoint> {
    val sourceColumn = selectedSourceColumn ?: return emptySet()
    val sourceTopRow = board.topOccupiedRow(sourceColumn) ?: return emptySet()
    val sourceTone = board.toneAt(sourceColumn, sourceTopRow) ?: return emptySet()
    return buildSet {
        for (row in sourceTopRow until board.rows) {
            if (board.toneAt(sourceColumn, row) != sourceTone) break
            add(GridPoint(sourceColumn, row))
        }
    }
}

@Composable
private fun rememberBlockSortInteractionPulse(enabled: Boolean): Float {
    if (!enabled) return 0f
    return LocalBlockStylePulse.current
}

@Composable
private fun BlockSortOnboardingCard(
    scene: BlockSortOnboardingScene,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, uiColors.guideAccent.copy(alpha = 0.42f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.guideAccent.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.interactive_onboarding_step_counter, currentStep, totalSteps),
                style = MaterialTheme.typography.labelMedium,
                color = uiColors.guideAccent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = scene.titleText(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = scene.bodyText(),
                style = MaterialTheme.typography.bodyMedium,
                color = uiColors.subtitle,
            )
            Text(
                text = scene.hintText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BlockSortOnboardingScene.titleText() = when (stage) {
    BlockSortOnboardingStage.PickSource -> stringResource(Res.string.interactive_onboarding_blocksort_pick_title)
    BlockSortOnboardingStage.MatchColor -> stringResource(Res.string.interactive_onboarding_blocksort_match_title)
    BlockSortOnboardingStage.FinishColumn -> stringResource(Res.string.interactive_onboarding_blocksort_finish_title)
}

@Composable
private fun BlockSortOnboardingScene.bodyText() = when (stage) {
    BlockSortOnboardingStage.PickSource -> stringResource(Res.string.interactive_onboarding_blocksort_pick_body)
    BlockSortOnboardingStage.MatchColor -> stringResource(Res.string.interactive_onboarding_blocksort_match_body)
    BlockSortOnboardingStage.FinishColumn -> stringResource(Res.string.interactive_onboarding_blocksort_finish_body)
}

@Composable
private fun BlockSortOnboardingScene.hintText() = when (stage) {
    BlockSortOnboardingStage.PickSource -> stringResource(Res.string.interactive_onboarding_blocksort_pick_hint)
    BlockSortOnboardingStage.MatchColor -> stringResource(Res.string.interactive_onboarding_blocksort_match_hint)
    BlockSortOnboardingStage.FinishColumn -> stringResource(Res.string.interactive_onboarding_blocksort_finish_hint)
}

@Preview(name = "BlockSort Game", widthDp = 412, heightDp = 915)
@Composable
private fun BlockSortGameScreenPreview() {
    BlockGamesTheme(
        settings = AppSettings(),
    ) {
        BlockSortGameScreen(
            gameState = GameState(
                config = GameConfig.default(GameplayStyle.BlockSort),
                gameplayStyle = GameplayStyle.BlockSort,
                board = previewBoard(),
                activePiece = null,
                nextQueue = emptyList(),
                score = 1200,
                linesCleared = 5,
                level = 2,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
            onRequestPreview = { _, _ -> null },
            onMove = { _, _ -> false },
            onRestart = {},
            onBack = {},
            highestScore = 4200,
        )
    }
}

@Preview(name = "BlockSort Onboarding", widthDp = 412, heightDp = 915)
@Composable
private fun BlockSortGameScreenOnboardingPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        val scene = com.ugurbuga.blockgames.settings.BlockSortOnboardingStateFactory.scene(
            com.ugurbuga.blockgames.settings.BlockSortOnboardingStage.MatchColor
        )
        BlockSortGameScreen(
            gameState = scene.gameState,
            onRequestPreview = { _, _ -> null },
            onMove = { _, _ -> false },
            onRestart = {},
            onBack = {},
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 2,
            interactiveOnboardingTotalSteps = 3,
        )
    }
}

private fun previewBoard() = listOf(
    listOf(CellTone.Gold, CellTone.Cyan),
    listOf(CellTone.Gold, CellTone.Gold),
    listOf(CellTone.Violet, CellTone.Cyan),
    emptyList(),
    listOf(CellTone.Violet, CellTone.Violet),
    emptyList(),
).let { columns ->
    var board = com.ugurbuga.blockgames.game.model.BoardMatrix.empty(columns = columns.size, rows = 4)
    columns.forEachIndexed { column, stack ->
        stack.forEachIndexed { index, tone ->
            board = board.fill(listOf(GridPoint(column, 3 - index)), tone = tone, value = (column * 10) + index)
        }
    }
    board
}

