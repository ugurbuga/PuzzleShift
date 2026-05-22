package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.blockgames.platform.feedback.SoundEffectPlayer
import com.ugurbuga.blockgames.presentation.game.GameDispatchResult
import com.ugurbuga.blockgames.presentation.game.GameIntent
import com.ugurbuga.blockgames.presentation.game.GameViewModel
import com.ugurbuga.blockgames.presentation.game.InteractionFeedback
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStage
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStage
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStateFactory
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStage
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.HighScoreStorage
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStage
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStateFactory
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.OnboardingStage
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.StackShiftGameOnboardingStateFactory
import com.ugurbuga.blockgames.settings.StackShiftOnboardingStage
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryActionNames
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


private const val InteractiveOnboardingStageAdvanceDelayMillis = 720L
private const val RewardedReviveTelemetryAction = "rewarded_revive"
private const val MoveColumnBlockSortTelemetryAction = "move_column_blocksort"

@NonSkippableComposable
@Composable
fun BlockGamesGameApp(
    modifier: Modifier = Modifier,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    viewModel: GameViewModel = remember { GameViewModel() },
    interactiveOnboardingEnabled: Boolean = false,
    onInteractiveOnboardingFinished: (GameState) -> Unit = {},
    onInteractiveOnboardingReturnHome: (GameState) -> Unit = {},
    onReplaceActivePieceRewarded: (SpecialBlockType) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
) {
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    val haptics = rememberGameHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val onboardingStages: List<OnboardingStage> = remember(gameplayStyle) {
        when (gameplayStyle) {
            GameplayStyle.BlockWise -> BlockWiseOnboardingStateFactory.stages
            GameplayStyle.ChainShift -> ChainShiftOnboardingStateFactory.stages
            GameplayStyle.MergeShift -> MergeShiftOnboardingStateFactory.stages
            GameplayStyle.BoomBlocks -> BoomBlocksOnboardingStateFactory.stages
            GameplayStyle.BlockSort -> BlockSortOnboardingStateFactory.stages
            else -> StackShiftGameOnboardingStateFactory.stages
        }
    }
    var onboardingStage by remember(interactiveOnboardingEnabled, gameplayStyle) {
        mutableStateOf<OnboardingStage?>(
            if (interactiveOnboardingEnabled) onboardingStages.firstOrNull() else null,
        )
    }
    var onboardingAwaitingCommit by remember(interactiveOnboardingEnabled, onboardingStage) {
        mutableStateOf(value = false)
    }
    var onboardingAdvanceRequest by remember(interactiveOnboardingEnabled) {
        mutableStateOf<InteractiveOnboardingAdvanceRequest?>(null)
    }
    var showOnboardingCompletionDialog by remember(interactiveOnboardingEnabled) {
        mutableStateOf(value = false)
    }
    var pendingOnboardingCompletionState by remember(interactiveOnboardingEnabled) {
        mutableStateOf<GameState?>(null)
    }
    val shouldShowLaunchOverlay = rememberSaveable { mutableStateOf(value = true) }
    var blockSortTimePauseUntilEpochMillis by remember(uiState.gameState.gameplayStyle, uiState.gameState.gameMode) {
        mutableStateOf(0L)
    }
    var previousBlockSortLevel by remember(uiState.gameState.gameplayStyle, uiState.gameState.gameMode) {
        mutableIntStateOf(uiState.gameState.level)
    }

    LaunchedEffect(
        uiState.gameState.gameplayStyle,
        uiState.gameState.gameMode,
        uiState.gameState.level,
        uiState.gameState.status,
    ) {
        if (
            uiState.gameState.gameplayStyle == GameplayStyle.BlockSort &&
            uiState.gameState.gameMode == com.ugurbuga.blockgames.game.model.GameMode.TimeAttack &&
            uiState.gameState.status == GameStatus.Running &&
            uiState.gameState.level > previousBlockSortLevel
        ) {
            blockSortTimePauseUntilEpochMillis =
                currentEpochMillis() + BlockSortTotalTransitionPauseDurationMillis.toLong()
        }
        previousBlockSortLevel = uiState.gameState.level
    }

    LaunchedEffect(
        uiState.gameState.status,
        uiState.gameState.gameplayStyle,
        uiState.gameState.gameMode,
        uiState.gameState.level,
        blockSortTimePauseUntilEpochMillis,
    ) {
        while (uiState.gameState.status == GameStatus.Running) {
            val pauseRemainingMillis = if (
                uiState.gameState.gameplayStyle == GameplayStyle.BlockSort &&
                uiState.gameState.gameMode == com.ugurbuga.blockgames.game.model.GameMode.TimeAttack
            ) {
                (blockSortTimePauseUntilEpochMillis - currentEpochMillis()).coerceAtLeast(0L)
            } else {
                0L
            }
            if (pauseRemainingMillis > 0L) {
                delay(minOf(pauseRemainingMillis, 250L).milliseconds)
                continue
            }
            delay(1000.milliseconds)
            viewModel.tick()
        }
    }

    val stackShiftOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is StackShiftOnboardingStage) {
            StackShiftGameOnboardingStateFactory.scene(onboardingStage as StackShiftOnboardingStage)
        } else null
    }

    val blockWiseOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is BlockWiseOnboardingStage) {
            BlockWiseOnboardingStateFactory.scene(onboardingStage as BlockWiseOnboardingStage)
        } else null
    }

    val mergeShiftOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is MergeShiftOnboardingStage) {
            MergeShiftOnboardingStateFactory.scene(onboardingStage as MergeShiftOnboardingStage)
        } else null
    }

    val chainShiftOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is ChainShiftOnboardingStage) {
            ChainShiftOnboardingStateFactory.scene(onboardingStage as ChainShiftOnboardingStage)
        } else null
    }

    val boomBlocksOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is BoomBlocksOnboardingStage) {
            BoomBlocksOnboardingStateFactory.scene(onboardingStage as BoomBlocksOnboardingStage)
        } else null
    }

    val blockSortOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is BlockSortOnboardingStage) {
            BlockSortOnboardingStateFactory.scene(onboardingStage as BlockSortOnboardingStage)
        } else null
    }

    val onboardingSceneGameState = stackShiftOnboardingScene?.gameState
        ?: blockWiseOnboardingScene?.gameState
        ?: chainShiftOnboardingScene?.gameState
        ?: mergeShiftOnboardingScene?.gameState
        ?: boomBlocksOnboardingScene?.gameState
        ?: blockSortOnboardingScene?.gameState

    val displayGameState by remember(
        uiState.gameState,
        onboardingAdvanceRequest,
        showOnboardingCompletionDialog,
    ) {
        derivedStateOf {
            if (onboardingAdvanceRequest != null || showOnboardingCompletionDialog) {
                uiState.gameState.hiddenOnboardingPreviewState()
            } else {
                uiState.gameState
            }
        }
    }

    LaunchedEffect(interactiveOnboardingEnabled, onboardingSceneGameState) {
        if (!interactiveOnboardingEnabled || onboardingSceneGameState == null) return@LaunchedEffect
        onboardingAwaitingCommit = false
        onboardingAdvanceRequest = null
        showOnboardingCompletionDialog = false
        pendingOnboardingCompletionState = null
        viewModel.replaceState(onboardingSceneGameState)

        val stageName = onboardingStage?.name ?: "unknown"

        telemetry.logUserAction(
            action = "interactive_onboarding_stage_shown",
            parameters = mapOf("stage" to stageName),
        )
    }

    LaunchedEffect(interactiveOnboardingEnabled, onboardingAdvanceRequest) {
        val request = onboardingAdvanceRequest ?: return@LaunchedEffect
        if (!interactiveOnboardingEnabled) {
            onboardingAdvanceRequest = null
            onboardingAwaitingCommit = false
            return@LaunchedEffect
        }

        delay(InteractiveOnboardingStageAdvanceDelayMillis.milliseconds)

        if (onboardingAdvanceRequest != request || onboardingStage != request.completedStage) return@LaunchedEffect

        val stageName = request.completedStage.name

        telemetry.logUserAction(
            action = "interactive_onboarding_stage_completed",
            parameters = mapOf("stage" to stageName),
        )

        if (request.nextStage != null) {
            onboardingStage = request.nextStage
        } else {
            pendingOnboardingCompletionState = when (gameplayStyle) {
                GameplayStyle.BlockWise -> BlockWiseOnboardingStateFactory.cleanGameState()
                GameplayStyle.ChainShift -> ChainShiftOnboardingStateFactory.cleanGameState()
                GameplayStyle.MergeShift -> MergeShiftOnboardingStateFactory.cleanGameState()
                GameplayStyle.BoomBlocks -> BoomBlocksOnboardingStateFactory.cleanGameState()
                GameplayStyle.BlockSort -> BlockSortOnboardingStateFactory.cleanGameState()
                else -> StackShiftGameOnboardingStateFactory.cleanGameState()
            }
            showOnboardingCompletionDialog = true
            onboardingStage = null
        }

        onboardingAwaitingCommit = false
        onboardingAdvanceRequest = null
    }

    LaunchedEffect(
        interactiveOnboardingEnabled,
        onboardingStage,
        onboardingAwaitingCommit,
        onboardingAdvanceRequest,
        uiState.gameState.softLock,
        uiState.gameState.board,
        uiState.gameState.activePiece?.id,
        uiState.gameState.score,
        uiState.gameState.linesCleared,
    ) {
        if (!interactiveOnboardingEnabled || !onboardingAwaitingCommit || onboardingAdvanceRequest != null) {
            return@LaunchedEffect
        }
        val sceneGameState = onboardingSceneGameState ?: return@LaunchedEffect

        val placementCommitted = uiState.gameState.softLock == null && (
                uiState.gameState.board != sceneGameState.board ||
                        uiState.gameState.activePiece?.id != sceneGameState.activePiece?.id ||
                        uiState.gameState.score != sceneGameState.score ||
                        uiState.gameState.linesCleared != sceneGameState.linesCleared
                )
        if (!placementCommitted) return@LaunchedEffect

        val currentStage = onboardingStage ?: return@LaunchedEffect
        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
            completedStage = currentStage,
            nextStage = nextInteractiveOnboardingStage(currentStage, onboardingStages),
        )
    }

    var highestScore by remember(uiState.gameState.gameMode, uiState.gameState.gameplayStyle) {
        mutableIntStateOf(
            HighScoreStorage.load(
                uiState.gameState.gameMode
            )
        )
    }
    var newHighScoreReached by remember(
        uiState.gameState.gameMode,
        uiState.gameState.gameplayStyle
    ) { mutableStateOf(value = false) }
    LaunchedEffect(
        uiState.gameState.score,
        uiState.gameState.gameMode,
        uiState.gameState.gameplayStyle
    ) {
        if (uiState.gameState.score > highestScore) {
            telemetry.logHighScoreReached(
                newScore = uiState.gameState.score,
                previousHighScore = highestScore,
            )
            highestScore = uiState.gameState.score
            HighScoreStorage.save(
                highestScore,
                uiState.gameState.gameMode
            )
            newHighScoreReached = true
        }
    }
    LaunchedEffect(
        uiState.gameState.status,
        uiState.gameState.score,
        uiState.gameState.linesCleared,
    ) {
        if (uiState.gameState.status == GameStatus.Running && uiState.gameState.score == 0 && uiState.gameState.linesCleared == 0) {
            newHighScoreReached = false
        }
    }

    LogScreen(telemetry, TelemetryScreenNames.Game)
    when (gameplayStyle) {
        GameplayStyle.BoomBlocks -> BoomBlocksGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onTapCell = { origin ->
                telemetry.logUserAction("tap_cell_boomblocks")
                // We use placePieceResult with a dummy pieceId for BoomBlocks
                val result = viewModel.placePieceResult(0L, origin)
                dispatchFeedback(result.feedback, soundPlayer, haptics)

                if (interactiveOnboardingEnabled && boomBlocksOnboardingScene != null) {
                    if (GameEvent.PlacementAccepted in result.events) {
                        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                            completedStage = boomBlocksOnboardingScene.stage,
                            nextStage = nextInteractiveOnboardingStage(
                                boomBlocksOnboardingScene.stage,
                                onboardingStages
                            ),
                        )
                    }
                }
            },
            onReplaceActivePiece = onReplaceActivePieceRewarded,
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                viewModel.restart(
                    config = restartConfigForStyle(uiState.gameState, GameplayStyle.BoomBlocks),
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                dispatchFeedback(viewModel.reviveFromReward(), soundPlayer, haptics)
            },
            onBack = onBack,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
            adController = adController,
            interactiveOnboardingScene = boomBlocksOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.BlockSort -> BlockSortGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = { sourceColumn, targetColumn ->
                viewModel.previewPlacement(sourceColumn.toLong(), GridPoint(targetColumn, 0))
            },
            onMove = { sourceColumn, targetColumn ->
                telemetry.logUserAction(MoveColumnBlockSortTelemetryAction)
                val result = viewModel.placePieceResult(sourceColumn.toLong(), GridPoint(targetColumn, 0))
                dispatchFeedback(result.feedback, soundPlayer, haptics)

                if (interactiveOnboardingEnabled && blockSortOnboardingScene != null) {
                    if (GameEvent.PlacementAccepted in result.events) {
                        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                            completedStage = blockSortOnboardingScene.stage,
                            nextStage = nextInteractiveOnboardingStage(
                                blockSortOnboardingScene.stage,
                                onboardingStages,
                            ),
                        )
                    }
                }

                GameEvent.PlacementAccepted in result.events
            },
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                dispatchFeedback(
                    viewModel.restart(
                        config = restartConfigForStyle(uiState.gameState, GameplayStyle.BlockSort),
                    ),
                    soundPlayer,
                    haptics,
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction(RewardedReviveTelemetryAction)
                dispatchFeedback(viewModel.reviveFromReward(), soundPlayer, haptics)
            },
            onRewardedAddEmptyColumn = {
                onReplaceActivePieceRewarded(SpecialBlockType.None)
            },
            onBack = onBack,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
            adController = adController,
            interactiveOnboardingScene = blockSortOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.BlockWise -> BlockWiseGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = { pieceId, origin -> viewModel.previewPlacement(pieceId, origin) },
            onResolvePreviewImpact = viewModel::previewImpactPoints,
            onPlacePiece = { pieceId, origin ->
                telemetry.logUserAction("place_piece_free")
                val result = viewModel.placePieceResult(pieceId, origin)
                dispatchFeedback(result.feedback, soundPlayer, haptics)

                if (interactiveOnboardingEnabled && blockWiseOnboardingScene != null) {
                    if (GameEvent.PlacementAccepted in result.events) {
                        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                            completedStage = blockWiseOnboardingScene.stage,
                            nextStage = nextInteractiveOnboardingStage(
                                blockWiseOnboardingScene.stage,
                                onboardingStages
                            ),
                        )
                    }
                }
            },
            onReplaceActivePiece = onReplaceActivePieceRewarded,
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                dispatchFeedback(
                    viewModel.restart(
                        config = restartConfigForStyle(uiState.gameState, GameplayStyle.BlockWise),
                    ),
                    soundPlayer,
                    haptics,
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                dispatchFeedback(viewModel.reviveFromReward(), soundPlayer, haptics)
            },
            onBack = onBack,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
            adController = adController,
            telemetry = telemetry,
            interactiveOnboardingScene = blockWiseOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.ChainShift -> ChainShiftGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = { pieceId, origin -> viewModel.previewPlacement(pieceId, origin) },
            onShootTarget = { origin ->
                telemetry.logUserAction("place_piece_chainshift")
                val pieceId = uiState.gameState.activePiece?.id
                if (pieceId != null) {
                    val result = viewModel.placePieceResult(pieceId, origin)
                    if (interactiveOnboardingEnabled && chainShiftOnboardingScene != null) {
                        if (GameEvent.PlacementAccepted in result.events) {
                            onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                                completedStage = chainShiftOnboardingScene.stage,
                                nextStage = nextInteractiveOnboardingStage(
                                    chainShiftOnboardingScene.stage,
                                    onboardingStages,
                                ),
                            )
                        }
                    }

                    result
                } else {
                    GameDispatchResult()
                }
            },
            soundPlayer = soundPlayer,
            haptics = haptics,
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                dispatchFeedback(
                    viewModel.restart(
                        config = restartConfigForStyle(uiState.gameState, GameplayStyle.ChainShift),
                    ),
                    soundPlayer,
                    haptics,
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction(RewardedReviveTelemetryAction)
                dispatchFeedback(viewModel.reviveFromReward(), soundPlayer, haptics)
            },
            onBack = onBack,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
            adController = adController,
            interactiveOnboardingScene = chainShiftOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.StackShift -> StackShiftGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = viewModel::previewPlacement,
            onResolvePreviewImpact = viewModel::previewImpactPoints,
            onPlacePiece = { column ->
                telemetry.logUserAction("place_piece_stackshift")
                val initialResult = viewModel.placePieceResult(column)
                val result = if (
                    interactiveOnboardingEnabled &&
                    stackShiftOnboardingScene != null &&
                    (GameEvent.SoftLockStarted in initialResult.events || GameEvent.SoftLockAdjusted in initialResult.events)
                ) {
                    val commitResult = viewModel.dispatchResult(GameIntent.CommitSoftLock)
                    GameDispatchResult(
                        events = initialResult.events + commitResult.events,
                        feedback = commitResult.feedback,
                    )
                } else {
                    initialResult
                }

                val scene = stackShiftOnboardingScene
                if (!interactiveOnboardingEnabled || scene == null) {
                    result
                } else {
                    when {
                        GameEvent.PlacementAccepted in result.events -> {
                            onboardingAwaitingCommit = false
                            onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                                completedStage = scene.stage,
                                nextStage = nextInteractiveOnboardingStage(
                                    scene.stage,
                                    onboardingStages
                                ),
                            )
                        }

                        GameEvent.SoftLockStarted in result.events || GameEvent.SoftLockAdjusted in result.events -> {
                            onboardingAwaitingCommit = true
                            onboardingAdvanceRequest = null
                        }
                    }
                    result
                }
            },
            onHoldPiece = viewModel::holdPiece,
            onReplaceActivePiece = { specialType ->
                onReplaceActivePieceRewarded(specialType)
                InteractionFeedback.None
            },
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                viewModel.restart(
                    config = restartConfigForStyle(uiState.gameState, GameplayStyle.StackShift),
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                viewModel.reviveFromReward()
            },
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onOpenTutorial = onOpenTutorial,
            telemetry = telemetry,
            adController = adController,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            showLaunchOverlayInitially = shouldShowLaunchOverlay.value,
            onLaunchOverlayFinished = { shouldShowLaunchOverlay.value = false },
            showNewHighScoreMessage = newHighScoreReached,
            interactiveOnboardingScene = stackShiftOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.MergeShift -> MergeShiftGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = viewModel::previewPlacement,
            onRequestImpactPoints = viewModel::previewImpactPoints,
            onPlacePiece = { column ->
                telemetry.logUserAction("place_piece_mergeshift")
                viewModel.placePieceResult(column).also { result ->
                    val scene = mergeShiftOnboardingScene
                    if (!interactiveOnboardingEnabled || scene == null) return@also

                    if (GameEvent.PlacementAccepted in result.events) {
                        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                            completedStage = scene.stage,
                            nextStage = nextInteractiveOnboardingStage(
                                scene.stage,
                                onboardingStages
                            ),
                        )
                    }
                }
            },
            onReplaceActivePiece = onReplaceActivePieceRewarded,
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                viewModel.restart(
                    config = restartConfigForStyle(uiState.gameState, GameplayStyle.MergeShift),
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                viewModel.reviveFromReward()
            },
            onTick = viewModel::tick,
            onBack = onBack,
            showNewHighScoreMessage = newHighScoreReached,
            adController = adController,
            telemetry = telemetry,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            interactiveOnboardingScene = mergeShiftOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )
    }
}

internal fun GameState.hiddenOnboardingPreviewState(): GameState = copy(
    activePiece = null,
    nextQueue = emptyList(),
    holdPiece = null,
    canHold = false,
)

internal fun restartConfigForStyle(
    state: GameState,
    expectedStyle: GameplayStyle,
): GameConfig {
    val defaultConfig = GameConfig.default(expectedStyle)
    return if (
        state.gameplayStyle == expectedStyle &&
        state.config.columns == defaultConfig.columns &&
        state.config.rows == defaultConfig.rows
    ) {
        state.config
    } else {
        defaultConfig
    }
}

private data class InteractiveOnboardingAdvanceRequest(
    val completedStage: OnboardingStage,
    val nextStage: OnboardingStage?,
)

internal fun nextInteractiveOnboardingStage(
    currentStage: OnboardingStage,
    stages: List<OnboardingStage>,
): OnboardingStage? {
    val currentIndex = stages.indexOf(currentStage)
    if (currentIndex < 0) return null
    return stages.getOrNull(currentIndex + 1)
}

@Preview(name = "Game App", widthDp = 412, heightDp = 915)
@Composable
private fun BlockGamesGameAppPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.StackShift
    BlockGamesTheme(settings = AppSettings()) {
        BlockGamesGameApp(
            telemetry = NoOpAppTelemetry,
        )
    }
}

