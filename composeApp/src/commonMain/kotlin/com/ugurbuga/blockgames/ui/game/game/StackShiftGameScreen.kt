package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.launch_boost_active
import blockgames.composeapp.generated.resources.launch_drag_hint
import blockgames.composeapp.generated.resources.launch_drag_hint_blockwise
import blockgames.composeapp.generated.resources.launch_special_chance
import blockgames.composeapp.generated.resources.restart_cancel
import blockgames.composeapp.generated.resources.restart_confirm
import blockgames.composeapp.generated.resources.restart_confirm_body
import blockgames.composeapp.generated.resources.restart_confirm_title
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.FeedbackEmphasis
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.game.model.toTopLeft
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.localization.appNameStringResource
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.feedback.GameHaptic
import com.ugurbuga.blockgames.platform.feedback.GameHaptics
import com.ugurbuga.blockgames.platform.feedback.GameSound
import com.ugurbuga.blockgames.platform.feedback.NoOpGameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.blockgames.platform.feedback.SoundEffectPlayer
import com.ugurbuga.blockgames.presentation.game.GameDispatchResult
import com.ugurbuga.blockgames.presentation.game.InteractionFeedback
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.StackShiftGameOnboardingStateFactory
import com.ugurbuga.blockgames.settings.StackShiftOnboardingScene
import com.ugurbuga.blockgames.settings.StackShiftOnboardingStage
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryActionNames
import com.ugurbuga.blockgames.ui.game.BoardGrid
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.GameOverDialogRevealDurationMillis
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.PieceBlocks
import com.ugurbuga.blockgames.ui.game.RestartConfirmDialog
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.boardFrameCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.dailychallenge.ChallengeTasksDock
import com.ugurbuga.blockgames.ui.game.onboarding.GameInteractiveOnboardingUi
import com.ugurbuga.blockgames.ui.game.onboarding.InteractiveOnboardingInfoCard
import com.ugurbuga.blockgames.ui.game.onboarding.InteractiveOnboardingTargetOverlay
import com.ugurbuga.blockgames.ui.game.onboarding.rememberInteractiveOnboardingVisualState
import com.ugurbuga.blockgames.ui.game.resolveGameText
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.BlockGamesUiColors
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val LaunchAnimationMillis = 140L
private const val EntryAnimationMillis = 70L
private const val LaunchOverlayDisplayDelayMillis = 250L
private const val NextPieceScale = 0.45f
private const val QueuePreviewAlpha = 0.58f

private const val MetricHighlightPulseScale = 1.08f
private const val MetricHighlightPulseUpDurationMillis = 180
private const val MetricHighlightPulseDownDurationMillis = 520
private const val TrayPulseScaleBoost = 0.055f
private const val TrayPulseUpDurationMillis = 760
private const val TrayPulseDownDurationMillis = 920
private const val DockPanelAlpha = 0.90f
private const val DockPanelStrokeAlpha = 0.74f
private const val DockPanelGlowAlpha = 0.12f
private const val GameOverBoardRowCoverAlpha = 0.92f
private const val GameOverBoardRowClearDurationMillis = 92
private const val ScreenShakeStepDurationMillis = 42
private const val ScreenShakeFinalStepDurationMillis = 48

private data class GameLayoutSpec(
    val cellSize: Dp,
    val boardWidth: Dp,
    val boardHeight: Dp,
    val dockHeight: Dp,
)

private fun rememberGameLayoutSpec(
    maxWidth: Dp,
    maxHeight: Dp,
    columns: Int,
    rows: Int,
): GameLayoutSpec {
    val dockChromeExtra = 4.dp
    val contentSpacing = 8.dp
    val widthLimitedCell = maxWidth / columns.coerceAtLeast(1)
    val heightLimitedCell =
        ((maxHeight - dockChromeExtra - contentSpacing).coerceAtLeast(0.dp)) / (rows + 4).coerceAtLeast(
            1
        )
    val cellSize = minOf(widthLimitedCell, heightLimitedCell).coerceAtLeast(8.dp)
    return GameLayoutSpec(
        cellSize = cellSize,
        boardWidth = cellSize * columns,
        boardHeight = cellSize * rows,
        dockHeight = (cellSize * 4) + dockChromeExtra,
    )
}

@Composable
fun StackShiftGameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> GameDispatchResult,
    onHoldPiece: () -> InteractionFeedback,
    onReplaceActivePiece: (SpecialBlockType) -> InteractionFeedback,
    onRestart: () -> InteractionFeedback,
    onRewardedRevive: () -> InteractionFeedback,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    showLaunchOverlayInitially: Boolean = false,
    onLaunchOverlayFinished: () -> Unit = {},
    showNewHighScoreMessage: Boolean = false,
    interactiveOnboardingScene: StackShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var overlayVisible by rememberSaveable { mutableStateOf(showLaunchOverlayInitially) }
    val uiColors = BlockGamesThemeTokens.uiColors

    Box(modifier = modifier.fillMaxSize()) {
        GameScreen(
            gameState = gameState,
            onRequestPreview = onRequestPreview,
            onResolvePreviewImpact = onResolvePreviewImpact,
            onPlacePiece = onPlacePiece,
            onHoldPiece = onHoldPiece,
            onReplaceActivePiece = onReplaceActivePiece,
            onRestart = onRestart,
            onRewardedRevive = onRewardedRevive,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onOpenTutorial = onOpenTutorial,
            telemetry = telemetry,
            adController = adController,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            interactiveOnboardingScene = interactiveOnboardingScene,
            interactiveOnboardingCurrentStep = interactiveOnboardingCurrentStep,
            interactiveOnboardingTotalSteps = interactiveOnboardingTotalSteps,
            interactiveOnboardingAwaitingCommit = interactiveOnboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = interactiveOnboardingCompletionDialogVisible,
            onInteractiveOnboardingStartGame = onInteractiveOnboardingStartGame,
            onInteractiveOnboardingReturnHome = onInteractiveOnboardingReturnHome,
        )

        if (overlayVisible) {
            GameLaunchOverlay(
                uiColors = uiColors,
                onFinished = {
                    overlayVisible = false
                    onLaunchOverlayFinished()
                },
            )
        }
    }
}

@Composable
fun GameLaunchOverlay(
    uiColors: BlockGamesUiColors,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    LaunchedEffect(Unit) {
        delay(LaunchOverlayDisplayDelayMillis.milliseconds)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(uiColors.overlay),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = appNameStringResource(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    if (gameplayStyle == GameplayStyle.BlockWise) {
                        Res.string.launch_drag_hint_blockwise
                    } else {
                        Res.string.launch_drag_hint
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> GameDispatchResult,
    onHoldPiece: () -> InteractionFeedback,
    onReplaceActivePiece: (SpecialBlockType) -> InteractionFeedback,
    onRestart: () -> InteractionFeedback,
    onRewardedRevive: () -> InteractionFeedback,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    interactiveOnboardingScene: StackShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val uiColors = BlockGamesThemeTokens.uiColors
    val colorScheme = MaterialTheme.colorScheme
    val settings = LocalAppSettings.current
    val resolvedPreviewStyle = settings.blockVisualStyle
    val updatedPreviewProvider by rememberUpdatedState(onRequestPreview)
    val updatedPreviewImpactProvider by rememberUpdatedState(onResolvePreviewImpact)
    val updatedPlacePiece by rememberUpdatedState(onPlacePiece)
    val updatedHoldPiece by rememberUpdatedState(onHoldPiece)
    val updatedReplaceActivePiece by rememberUpdatedState(onReplaceActivePiece)
    val updatedRestart by rememberUpdatedState(onRestart)
    val updatedRewardedRevive by rememberUpdatedState(onRewardedRevive)
    var showRestartDialog by remember { mutableStateOf(value = false) }
    val screenShakeX = remember { Animatable(0f) }
    val screenShakeY = remember { Animatable(0f) }
    val impactFlashAlpha = remember { Animatable(0f) }
    val comboDriftY = remember { Animatable(18f) }
    val comboAlpha = remember { Animatable(0f) }
    val metricPulsePhase = remember { Animatable(0f) }
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var highScoreHighlightActive by remember { mutableStateOf(value = false) }
    var celebratedHighScore by remember { mutableIntStateOf(highestScore) }
    var showGameOverDialog by remember { mutableStateOf(value = false) }
    var rewardedReviveLoading by remember { mutableStateOf(value = false) }

    val stylePulse = LocalBlockStylePulse.current

    val interactiveOnboardingEnabled = interactiveOnboardingScene != null
    val interactiveOnboardingAcceptedColumns = interactiveOnboardingScene?.acceptedColumns.orEmpty()
    val topBarControlsEnabled = !interactiveOnboardingEnabled

    val activePiece = gameState.activePiece
    var overlayHostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    val overlayTopLeftState = remember(activePiece?.id) {
        mutableStateOf<Offset?>(null)
    }
    var isDragging by remember { mutableStateOf(value = false) }
    var isLaunching by remember { mutableStateOf(value = false) }
    val boardRect = boardRectInRoot.toStackShiftLocalRect(overlayHostRectInRoot)
    val trayRect = trayRectInRoot.toStackShiftLocalRect(overlayHostRectInRoot)
    val cellSizePx = if (boardRect.width == 0f) 0f else boardRect.width / gameState.config.columns.toFloat()
    val spawnColumn by remember(
        activePiece?.id,
        gameState.lastPlacementColumn,
        gameState.config.columns
    ) {
        derivedStateOf {
            resolveSpawnColumn(
                piece = activePiece,
                boardColumns = gameState.config.columns,
                lastPlacementColumn = gameState.lastPlacementColumn,
            )
        }
    }
    val spawnTopLeft by remember(activePiece?.id, trayRect, boardRect, cellSizePx, spawnColumn) {
        derivedStateOf {
            gameState.softLock?.preview?.landingAnchor?.toTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
                ?: pieceSpawnTopLeft(
                    piece = activePiece,
                    trayRect = trayRect,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                    column = spawnColumn,
                )
        }
    }
    val spawnRect by remember(spawnTopLeft, activePiece?.id, cellSizePx) {
        derivedStateOf {
            val topLeft = spawnTopLeft ?: return@derivedStateOf Rect.Zero
            val piece = activePiece ?: return@derivedStateOf Rect.Zero
            if (cellSizePx <= 0f) return@derivedStateOf Rect.Zero
            Rect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + (piece.width * cellSizePx),
                bottom = topLeft.y + (piece.height * cellSizePx),
            )
        }
    }

    LaunchedEffect(activePiece?.id, spawnTopLeft) {
        if (spawnTopLeft != null) {
            overlayTopLeftState.value = spawnTopLeft
            isDragging = false
            isLaunching = false
        }
    }

    val selectedColumn by remember(activePiece?.id, boardRect, cellSizePx) {
        derivedStateOf {
            resolveSelectedColumn(
                piece = activePiece,
                overlayTopLeft = overlayTopLeftState.value,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                boardColumns = gameState.config.columns,
            )
        }
    }

    val hasDraggedAwayFromSpawn by remember(spawnTopLeft, cellSizePx) {
        derivedStateOf {
            val spawn = spawnTopLeft ?: return@derivedStateOf false
            val overlay = overlayTopLeftState.value ?: return@derivedStateOf false
            abs(overlay.x - spawn.x) >= (cellSizePx * 0.45f)
        }
    }

    val placementPreview by remember(
        selectedColumn,
        activePiece?.id,
        gameState.status,
        gameState.board,
        interactiveOnboardingAcceptedColumns,
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running) return@derivedStateOf null
            if (interactiveOnboardingAcceptedColumns.isNotEmpty() && selectedColumn !in interactiveOnboardingAcceptedColumns) {
                return@derivedStateOf null
            }
            if (gameState.softLock != null && !isDragging) {
                gameState.softLock.preview
            } else {
                selectedColumn?.let(updatedPreviewProvider)
            }
        }
    }
    val interactiveOnboardingTargetAligned by remember(
        interactiveOnboardingScene,
        interactiveOnboardingAcceptedColumns,
        selectedColumn,
        placementPreview,
        isDragging,
    ) {
        derivedStateOf {
            interactiveOnboardingScene != null &&
                    interactiveOnboardingScene.stage != StackShiftOnboardingStage.DragAndLaunch &&
                    isDragging &&
                    placementPreview != null &&
                    selectedColumn != null &&
                    (interactiveOnboardingAcceptedColumns.isEmpty() || selectedColumn in interactiveOnboardingAcceptedColumns)
        }
    }
    val resolvedInteractiveOnboardingUi = interactiveOnboardingScene?.let { scene ->
        GameInteractiveOnboardingUi(
            scene = scene,
            currentStep = interactiveOnboardingCurrentStep,
            totalSteps = interactiveOnboardingTotalSteps,
            hasDraggedAwayFromSpawn = hasDraggedAwayFromSpawn,
            isTargetAligned = interactiveOnboardingTargetAligned,
            isAwaitingPlacementCommit = interactiveOnboardingAwaitingCommit,
        )
    }
    val resolvedInteractiveOnboardingVisualState = resolvedInteractiveOnboardingUi?.let {
        rememberInteractiveOnboardingVisualState(it)
    }

    val scoreHighlightActive = highScoreHighlightActive
    val anyMetricHighlightActive = highScoreHighlightActive && !interactiveOnboardingEnabled

    LaunchedEffect(anyMetricHighlightActive, gameState.status) {
        if (!anyMetricHighlightActive || gameState.status == GameStatus.GameOver) {
            metricPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        metricPulsePhase.snapTo(0f)
        while (isActive && anyMetricHighlightActive && gameState.status != GameStatus.GameOver) {
            metricPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            metricPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val previewImpactPoints by remember(
        placementPreview,
        activePiece?.id,
        gameState.board,
        gameState.status,
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running || placementPreview == null) {
                emptySet()
            } else {
                updatedPreviewImpactProvider(placementPreview)
            }
        }
    }

    val displayOverlayTopLeft by remember(
        activePiece?.id,
        selectedColumn,
        boardRect,
        cellSizePx,
        isLaunching,
        isDragging,
    ) {
        derivedStateOf {
            val current = overlayTopLeftState.value ?: return@derivedStateOf null
            if (isDragging) return@derivedStateOf current

            val snappedColumn = selectedColumn
            if (snappedColumn == null || boardRect == Rect.Zero || cellSizePx <= 0f || isLaunching) {
                return@derivedStateOf current
            }
            current.copy(x = columnToLeft(snappedColumn, boardRect, cellSizePx))
        }
    }

    val isPieceInTrayArea by remember(displayOverlayTopLeft, trayRect) {
        derivedStateOf {
            val overlay = displayOverlayTopLeft ?: return@derivedStateOf false
            trayRect != Rect.Zero && overlay.y >= trayRect.top - (cellSizePx * 0.5f)
        }
    }

    val showOnboardingIndicators = interactiveOnboardingEnabled &&
            !isDragging &&
            !isLaunching &&
            isPieceInTrayArea

    val shouldPulseTrayPiece =
        activePiece != null &&
                spawnTopLeft != null &&
                gameState.status == GameStatus.Running &&
                gameState.softLock == null &&
                !interactiveOnboardingEnabled &&
                !isDragging &&
                !isLaunching

    LaunchedEffect(gameState.screenShakeToken) {
        if (gameState.screenShakeToken == 0L) return@LaunchedEffect
        if (interactiveOnboardingEnabled) {
            screenShakeX.snapTo(0f)
            screenShakeY.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(3) {
            screenShakeX.animateTo(
                targetValue = ((-8..8).random()).toFloat(),
                animationSpec = tween(durationMillis = ScreenShakeStepDurationMillis),
            )
            screenShakeY.animateTo(
                targetValue = ((-8..8).random()).toFloat(),
                animationSpec = tween(durationMillis = ScreenShakeStepDurationMillis),
            )
        }
        screenShakeX.animateTo(
            0f,
            animationSpec = tween(durationMillis = ScreenShakeFinalStepDurationMillis)
        )
        screenShakeY.animateTo(
            0f,
            animationSpec = tween(durationMillis = ScreenShakeFinalStepDurationMillis)
        )
    }

    LaunchedEffect(gameState.impactFlashToken) {
        if (gameState.impactFlashToken == 0L) return@LaunchedEffect
        impactFlashAlpha.snapTo(0.42f)
        impactFlashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(gameState.comboPopupToken) {
        if (gameState.comboPopupToken == 0L) return@LaunchedEffect
        comboAlpha.snapTo(0f)
        comboDriftY.snapTo(18f)
        coroutineScope.launch {
            comboAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
            delay(800.milliseconds)
            comboAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        comboDriftY.animateTo(
            targetValue = -32f,
            animationSpec = tween(durationMillis = 1460, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            gameOverBoardClearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (GameOverBoardRowClearDurationMillis * gameState.config.rows).coerceAtLeast(
                        1200
                    ),
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

    LaunchedEffect(highestScore, gameState.score, gameState.status, gameState.linesCleared) {
        when {
            gameState.status == GameStatus.GameOver ||
                    (gameState.status == GameStatus.Running && gameState.score == 0 && gameState.linesCleared == 0) -> {
                highScoreHighlightActive = false
                celebratedHighScore = highestScore
                metricPulsePhase.animateTo(0f, animationSpec = snap())
            }

            highestScore > celebratedHighScore -> {
                celebratedHighScore = highestScore
                highScoreHighlightActive = true
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = screenShakeX.value
                        translationY = screenShakeY.value
                    }
                    .onGloballyPositioned { coordinates ->
                        val newBounds = coordinates.boundsInRoot()
                        if (overlayHostRectInRoot != newBounds) {
                            overlayHostRectInRoot = newBounds
                        }
                    },
            ) {
                val scoreHighlightStrengthProvider = { if (scoreHighlightActive) 1f else 0f }
                val scorePulseScaleProvider = {
                    if (scoreHighlightActive) {
                        1f + (MetricHighlightPulseScale - 1f) * metricPulsePhase.value
                    } else {
                        1f
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!interactiveOnboardingEnabled) {
                        MinimalTopBar(
                            gameState = gameState,
                            scoreHighlightStrengthProvider = scoreHighlightStrengthProvider,
                            scoreHighlightScaleProvider = scorePulseScaleProvider,
                            remainingTimeLabel = stringResource(Res.string.time_remaining),
                            onBack = onBack,
                            onRestart = {
                                if (!topBarControlsEnabled) return@MinimalTopBar
                                showRestartDialog = true
                            },
                            controlsEnabled = topBarControlsEnabled,
                            stylePulse = stylePulse,
                        )
                    }

                    resolvedInteractiveOnboardingUi?.let { onboardingUi ->
                        InteractiveOnboardingInfoCard(
                            currentStep = onboardingUi.currentStep,
                            totalSteps = onboardingUi.totalSteps,
                            visualState = resolvedInteractiveOnboardingVisualState
                                ?: rememberInteractiveOnboardingVisualState(onboardingUi),
                            onBack = onBack,
                            stylePulse = stylePulse,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        val layoutSpec = rememberGameLayoutSpec(
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            columns = gameState.config.columns,
                            rows = gameState.config.rows,
                        )
                        val resolvedCellSizePx =
                            with(LocalDensity.current) { layoutSpec.cellSize.toPx() }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val boardShape =
                                RoundedCornerShape(
                                    boardFrameCornerRadiusDp(
                                        resolvedPreviewStyle
                                    )
                                )
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = layoutSpec.boardWidth,
                                        height = layoutSpec.boardHeight
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                BoardGrid(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .onGloballyPositioned { coordinates ->
                                            val newBounds = coordinates.boundsInRoot()
                                            if (boardRectInRoot != newBounds) {
                                                boardRectInRoot = newBounds
                                            }
                                        },
                                    gameState = gameState,
                                    preview = placementPreview,
                                    impactedPreviewCells = previewImpactPoints,
                                    guidedColumns = emptySet(),
                                    activeColumn = selectedColumn,
                                    activePiece = activePiece,
                                    isDragging = isDragging,
                                    gameOverClearProgressProvider = { gameOverBoardClearProgress.value },
                                    showClearFlash = true,
                                    stylePulse = stylePulse,
                                )

                                if (gameState.status == GameStatus.GameOver || gameOverBoardClearProgress.value > 0f) {
                                    GameOverBoardClearOverlay(
                                        revealProgress = gameOverBoardClearProgress.value,
                                        rowCount = gameState.config.rows,
                                        modifier = Modifier.matchParentSize()
                                            .clip(boardShape),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            MinimalBottomDock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(layoutSpec.dockHeight)
                                    .onGloballyPositioned { coordinates ->
                                        val newBounds = coordinates.boundsInRoot()
                                        if (trayRectInRoot != newBounds) {
                                            trayRectInRoot = newBounds
                                        }
                                    },
                                gameState = gameState,
                                cellSizePx = resolvedCellSizePx,
                                showNextPiece = !interactiveOnboardingEnabled,
                                highlightColor = resolvedInteractiveOnboardingVisualState?.guideColor,
                                highlightDock = resolvedInteractiveOnboardingUi?.let {
                                    it.scene.stage == StackShiftOnboardingStage.DragAndLaunch && it.hasDraggedAwayFromSpawn
                                } == true,
                                adController = adController,
                                onReplaceActivePiece = { type ->
                                    dispatchFeedback(
                                        updatedReplaceActivePiece(type),
                                        soundPlayer,
                                        haptics
                                    )
                                },
                                stylePulse = stylePulse,
                            )
                        }
                    }
                }

                resolvedInteractiveOnboardingUi?.let { onboardingUi ->

                    InteractiveOnboardingTargetOverlay(
                        ui = onboardingUi,
                        boardRect = boardRect,
                        trayRect = trayRect,
                        spawnRect = spawnRect,
                        cellSizePx = cellSizePx,
                    )
                }

                LaunchGuideLineOverlay(
                    preview = placementPreview,
                    activePiece = activePiece,
                    pieceTopLeft = displayOverlayTopLeft,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                )

                gameState.floatingFeedback?.let { floatingFeedback ->
                    FloatingFeedbackOverlay(
                        text = resolveGameText(
                            floatingFeedback.text
                        ),
                        isBonus = floatingFeedback.emphasis != FeedbackEmphasis.Info,
                        alpha = comboAlpha.value,
                        driftY = comboDriftY.value,
                    )
                }

                ImpactFlashOverlay(alphaProvider = { impactFlashAlpha.value })

                if (activePiece != null && overlayTopLeftState.value != null && cellSizePx > 0f) {
                    ActivePieceOverlay(
                        piece = activePiece,
                        cellSizePx = cellSizePx,
                        displayOverlayTopLeft = displayOverlayTopLeft,
                        isDragging = isDragging,
                        isLaunching = isLaunching,
                        isSoftLockActive = gameState.isSoftLockActive,
                        isPaused = false,
                        shouldPulseTrayPiece = shouldPulseTrayPiece,
                        pointerModifier = Modifier.pointerInput(
                            activePiece.id,
                            gameState.status,
                            isLaunching,
                            boardRect,
                            cellSizePx,
                        ) {
                            detectDragGestures(
                                onDragStart = {
                                    if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                    isDragging = true
                                    dispatchFeedback(
                                        InteractionFeedback(
                                            sounds = setOf(GameSound.Grab),
                                            haptics = setOf(GameHaptic.Light),
                                        ),
                                        soundPlayer = soundPlayer,
                                        haptics = haptics,
                                    )
                                },
                                onDrag = { change, dragAmount ->
                                    if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                    change.consume()
                                    val current =
                                        overlayTopLeftState.value ?: return@detectDragGestures
                                    overlayTopLeftState.value =
                                        current.copy(x = current.x + dragAmount.x)
                                },
                                onDragEnd = {
                                    if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                    isDragging = false

                                    val preview = placementPreview
                                    val column = selectedColumn
                                    val currentSpawn = spawnTopLeft
                                    if (preview == null || column == null || currentSpawn == null) {
                                        overlayTopLeftState.value = currentSpawn
                                        dispatchFeedback(
                                            InteractionFeedback(
                                                sounds = setOf(GameSound.DropInvalid),
                                                haptics = setOf(GameHaptic.Warning),
                                            ),
                                            soundPlayer = soundPlayer,
                                            haptics = haptics,
                                        )
                                        return@detectDragGestures
                                    }

                                    if (gameState.softLock != null) {
                                        overlayTopLeftState.value = preview.landingAnchor.toTopLeft(
                                            boardRect = boardRect,
                                            cellSizePx = cellSizePx
                                        )
                                        val result = updatedPlacePiece(column)
                                        dispatchFeedback(result.feedback, soundPlayer, haptics)
                                    } else {
                                        isLaunching = true
                                        overlayTopLeftState.value = preview.entryAnchor.toTopLeft(
                                            boardRect = boardRect,
                                            cellSizePx = cellSizePx
                                        )
                                        coroutineScope.launch {
                                            delay(EntryAnimationMillis.milliseconds)
                                            overlayTopLeftState.value =
                                                preview.landingAnchor.toTopLeft(
                                                    boardRect = boardRect,
                                                    cellSizePx = cellSizePx
                                                )
                                            delay(LaunchAnimationMillis.milliseconds)
                                            val result = updatedPlacePiece(column)
                                            dispatchFeedback(result.feedback, soundPlayer, haptics)
                                            isLaunching = false
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    spawnTopLeft?.let { overlayTopLeftState.value = it }
                                },
                            )
                        },
                        stylePulse = stylePulse,
                        showOnboardingIndicators = showOnboardingIndicators,
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
                                telemetry.logUserAction(TelemetryActionNames.PlayAgain)
                                adController.showRestartInterstitial {
                                    dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                                }
                            }
                        },
                        onUseExtraLife = {
                            if (rewardedReviveLoading || gameState.rewardedReviveUsed) return@GameOverDialog
                            rewardedReviveLoading = true
                            adController.showRewardedRevive { rewarded ->
                                rewardedReviveLoading = false
                                if (rewarded) {
                                    dispatchFeedback(updatedRewardedRevive(), soundPlayer, haptics)
                                }
                            }
                        },
                    )
                }

                if (interactiveOnboardingCompletionDialogVisible) {
                    InteractiveOnboardingCompletionDialog(
                        onStartGame = onInteractiveOnboardingStartGame,
                        onReturnHome = onInteractiveOnboardingReturnHome,
                    )
                }

                if (showRestartDialog) {
                    RestartConfirmDialog(
                        onDismissRequest = { showRestartDialog = false },
                        title = stringResource(Res.string.restart_confirm_title),
                        message = stringResource(Res.string.restart_confirm_body),
                        confirmLabel = stringResource(Res.string.restart_confirm),
                        dismissLabel = stringResource(Res.string.restart_cancel),
                        onConfirm = {
                            showRestartDialog = false
                            adController.showRestartInterstitial {
                                dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingFeedbackOverlay(
    text: String,
    isBonus: Boolean,
    alpha: Float,
    driftY: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.001f) return
    FloatingFeedbackBubble(
        text = text,
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = 96.dp),
        isBonus = isBonus,
        alpha = alpha,
        driftY = driftY,
    )
}

@Composable
private fun ImpactFlashOverlay(
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = alphaProvider().coerceIn(0f, 1f)
            }
            .background(Color.White),
    )
}

@Composable
internal fun BoxScope.LaunchGuideLineOverlay(
    preview: PlacementPreview?,
    activePiece: Piece?,
    pieceTopLeft: Offset?,
    boardRect: Rect,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    if (preview == null || activePiece == null || pieceTopLeft == null || boardRect == Rect.Zero || cellSizePx <= 0f) return

    val settings = LocalAppSettings.current
    val isDarkTheme = isBlockGamesDarkTheme(settings)
    val resolvedPreviewStyle = settings.blockVisualStyle
    val baseColor = remember(activePiece.id, activePiece.tone, settings.blockColorPalette, isDarkTheme) {
        activePiece.tone.paletteColor(settings.blockColorPalette, isDarkTheme)
    }
    val guideSegments = remember(preview, activePiece.id, pieceTopLeft, boardRect, cellSizePx) {
        launchGuideSegments(
            preview = preview,
            activePiece = activePiece,
            pieceTopLeft = pieceTopLeft,
            boardRect = boardRect,
            cellSizePx = cellSizePx,
        )
    }
    if (guideSegments.isEmpty()) return

    val guideAlpha = if (isDarkTheme) 0.22f else 0.16f

    Canvas(modifier = modifier.matchParentSize()) {
        val blockCornerRadius =
            boardCellCornerRadiusPx(
                cellSizePx = cellSizePx,
                style = resolvedPreviewStyle,
            )
        guideSegments.forEach { guideRect ->
            val clampedCornerRadius =
                minOf(blockCornerRadius, guideRect.width / 2f, guideRect.height / 2f)
            val cornerRadius = CornerRadius(clampedCornerRadius, clampedCornerRadius)
            drawRoundRect(
                color = baseColor.copy(alpha = guideAlpha),
                topLeft = guideRect.topLeft,
                size = guideRect.size,
                cornerRadius = cornerRadius,
            )
        }
    }
}

internal fun launchGuideSegments(
    preview: PlacementPreview,
    activePiece: Piece,
    pieceTopLeft: Offset,
    boardRect: Rect,
    cellSizePx: Float,
): List<Rect> {
    if (preview.occupiedCells.isEmpty() || activePiece.cells.isEmpty() || boardRect == Rect.Zero || cellSizePx <= 0f) {
        return emptyList()
    }

    val bottommostPieceRowsByColumn = activePiece.cells
        .groupBy(GridPoint::column)
        .mapValues { (_, points) -> points.maxOf(GridPoint::row) }
    val topmostPreviewRowsByColumn = preview.occupiedCells
        .groupBy(GridPoint::column)
        .mapValues { (_, points) -> points.minOf(GridPoint::row) }

    return bottommostPieceRowsByColumn.entries.sortedBy { it.key }
        .mapNotNull { (localColumn, bottommostPieceRow) ->
            val boardColumn = preview.selectedColumn + localColumn
            val topmostPreviewRow =
                topmostPreviewRowsByColumn[boardColumn] ?: return@mapNotNull null
            val guideTop = boardRect.top + (topmostPreviewRow * cellSizePx)
            val guideBottom = pieceTopLeft.y + ((bottommostPieceRow + 1) * cellSizePx)
            val guideHeight = guideBottom - guideTop
            if (guideHeight <= 0f) return@mapNotNull null

            Rect(
                left = pieceTopLeft.x + (localColumn * cellSizePx),
                top = guideTop,
                right = pieceTopLeft.x + ((localColumn + 1) * cellSizePx),
                bottom = guideBottom,
            )
        }
}

@Composable
private fun ActivePieceOverlay(
    piece: Piece,
    cellSizePx: Float,
    displayOverlayTopLeft: Offset?,
    isDragging: Boolean,
    isLaunching: Boolean,
    isSoftLockActive: Boolean,
    isPaused: Boolean = false,
    shouldPulseTrayPiece: Boolean,
    pointerModifier: Modifier,
    stylePulse: Float = 0f,
    showOnboardingIndicators: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (displayOverlayTopLeft == null || cellSizePx <= 0f) return
    val density = LocalDensity.current
    val settings = LocalAppSettings.current
    val overlayPulsePhase = remember { Animatable(0f) }
    val overlayX by animateFloatAsState(
        targetValue = displayOverlayTopLeft.x,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing,
        ),
        label = "overlayX",
    )
    val overlayY by animateFloatAsState(
        targetValue = displayOverlayTopLeft.y,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing,
        ),
        label = "overlayY",
    )
    val overlayScale by animateFloatAsState(
        targetValue = when {
            isLaunching -> 1.06f
            isSoftLockActive -> 0.98f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "overlayScale",
    )
    LaunchedEffect(shouldPulseTrayPiece, piece.id) {
        if (!shouldPulseTrayPiece) {
            overlayPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        overlayPulsePhase.snapTo(0f)
        while (isActive && shouldPulseTrayPiece) {
            overlayPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = TrayPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            overlayPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = TrayPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }
    val trayPulseScaleProvider = {
        if (shouldPulseTrayPiece) {
            1f + (TrayPulseScaleBoost * overlayPulsePhase.value)
        } else {
            1f
        }
    }
    val pieceCellDp = with(density) { cellSizePx.toDp() }
    val resolvedPreviewStyle = settings.blockVisualStyle
    val launchCellCornerRadius =
        boardCellCornerRadiusDp(
            cellSize = pieceCellDp,
            style = resolvedPreviewStyle,
        )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = overlayX
                translationY = overlayY
                val trayScale = trayPulseScaleProvider()
                scaleX = overlayScale * trayScale
                scaleY = overlayScale * trayScale
                transformOrigin = TransformOrigin(0f, 0f)
            },
        contentAlignment = Alignment.TopStart,
    ) {
        PieceBlocks(
            piece = piece,
            cellSize = pieceCellDp,
            cellCornerRadius = launchCellCornerRadius,
            alpha = 1f,
            pulse = stylePulse,
            modifier = pointerModifier,
        )

        if (showOnboardingIndicators) {
            val indicatorTransition = rememberInfiniteTransition(label = "indicatorTransition")
            val indicatorAlpha by indicatorTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "indicatorAlpha",
            )
            val indicatorNudge by indicatorTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "indicatorNudge",
            )
            val isDark = isBlockGamesDarkTheme(settings)
            val indicatorColor = if (isDark) Color.White else Color(0xFF101114)
            val handSize = pieceCellDp * 1.3f
            val arrowSize = pieceCellDp * 0.72f
            val pieceWidthDp = pieceCellDp * piece.width.toFloat()
            val pieceHeightDp = pieceCellDp * piece.height.toFloat()

            // Center Hand
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = indicatorColor.copy(alpha = indicatorAlpha),
                modifier = Modifier
                    .size(handSize)
                    .offset(
                        x = (pieceWidthDp * 0.5f) - (handSize * 0.5f) + 2.dp,
                        y = pieceHeightDp - (pieceCellDp * 0.35f) + indicatorNudge.dp
                    )
                    .graphicsLayer { rotationZ = -15f }
            )

            // Left Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = indicatorColor.copy(alpha = indicatorAlpha * 0.7f),
                modifier = Modifier
                    .size(arrowSize)
                    .offset(
                        x = (pieceWidthDp * 0.5f) - (handSize * 0.75f) - (indicatorNudge * 0.3f).dp,
                        y = pieceHeightDp + (pieceCellDp * 0.12f)
                    )
            )

            // Right Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = indicatorColor.copy(alpha = indicatorAlpha * 0.7f),
                modifier = Modifier
                    .size(arrowSize)
                    .offset(
                        x = (pieceWidthDp * 0.5f) + (handSize * 0.35f) + (indicatorNudge * 0.3f).dp,
                        y = pieceHeightDp + (pieceCellDp * 0.12f)
                    )
            )
        }
    }
}

@Composable
private fun MinimalBottomDock(
    gameState: GameState,
    cellSizePx: Float,
    showNextPiece: Boolean,
    adController: GameAdController,
    onReplaceActivePiece: (SpecialBlockType) -> Unit,
    highlightColor: Color? = null,
    highlightDock: Boolean = false,
    stylePulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val resolvedHighlightColor = highlightColor ?: uiColors.actionButton
    val dockPulseAlpha = if (highlightDock) 0.10f + (0.08f * stylePulse) else 0f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = DockPanelAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (highlightDock) 2.dp else 1.dp,
            color = if (highlightDock) {
                resolvedHighlightColor.copy(alpha = 0.92f)
            } else {
                uiColors.panelStroke.copy(alpha = DockPanelStrokeAlpha)
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (highlightDock) {
                                resolvedHighlightColor.copy(alpha = 0.22f + dockPulseAlpha)
                            } else {
                                uiColors.panelHighlight.copy(alpha = DockPanelGlowAlpha)
                            },
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (gameState.activeChallenge != null) {
                    ChallengeTasksDock(
                        challenge = gameState.activeChallenge,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LaunchBarView(
                        gameState = gameState,
                        stylePulse = stylePulse,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (showNextPiece) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HoldAndQueueStrip(
                            queue = gameState.nextQueue,
                            cellSize = (cellSizePx * NextPieceScale),
                            stylePulse = stylePulse,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpecialActionAdButton(
                                specialType = SpecialBlockType.RowClearer,
                                tone = CellTone.Cyan,
                                icon = Icons.Filled.SwapHoriz,
                                adController = adController,
                                onActivated = { onReplaceActivePiece(SpecialBlockType.RowClearer) },
                                stylePulse = stylePulse,
                            )
                            SpecialActionAdButton(
                                specialType = SpecialBlockType.ColumnClearer,
                                tone = CellTone.Emerald,
                                icon = Icons.Filled.SwapVert,
                                adController = adController,
                                onActivated = { onReplaceActivePiece(SpecialBlockType.ColumnClearer) },
                                stylePulse = stylePulse,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialActionAdButton(
    specialType: SpecialBlockType,
    tone: CellTone,
    icon: ImageVector,
    adController: GameAdController,
    onActivated: () -> Unit,
    stylePulse: Float = 0f,
) {
    var loading by remember { mutableStateOf(false) }

    TopBarActionBlockButton(
        tone = tone,
        icon = icon,
        contentDescription = specialType.name,
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
        extraAlpha = 0.75f,
    )
}

@Composable
private fun LaunchBarView(
    gameState: GameState,
    stylePulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val progressPercent = (gameState.launchBar.progress * 100).toInt()
    val isBoostActive = gameState.launchBar.isBoostActive
    val animatedProgress by animateFloatAsState(
        targetValue = gameState.launchBar.progress,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "launchBarProgress",
    )
    val boostGlow by rememberInfiniteTransition(label = "launchBarBoostGlow").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "launchBarGlow",
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(GameUiShapeTokens.chipCorner))
                .background(uiColors.launchTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceAtLeast(if (animatedProgress > 0f) 0.08f else 0f))
                    .clip(RoundedCornerShape(GameUiShapeTokens.chipCorner))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                uiColors.launchGlow.copy(alpha = if (isBoostActive) boostGlow else 0.8f + (0.2f * stylePulse)),
                                uiColors.success.copy(alpha = 0.92f),
                                uiColors.launchAccent,
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isBoostActive) resolveGameText(
                        gameText(GameTextKey.Boost)
                    ) else resolveGameText(
                        gameText(GameTextKey.Launch)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBoostActive) "x${gameState.launchBar.boostTurnsRemaining}" else "%$progressPercent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = if (isBoostActive) stringResource(
                Res.string.launch_boost_active,
                gameState.launchBar.boostTurnsRemaining
            ) else stringResource(
                Res.string.launch_special_chance,
                (gameState.launchBar.specialPieceChance * 100).toInt()
            ),
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HoldAndQueueStrip(
    queue: List<Piece>,
    cellSize: Float,
    stylePulse: Float = 0f,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        queue.take(1).forEachIndexed { index, piece ->
            QueueSlot(
                label = if (index == 0) resolveGameText(
                    gameText(GameTextKey.QueueNextShort)
                ) else "",
                piece = piece,
                cellSize = cellSize,
                alpha = 1f,
                stylePulse = stylePulse,
            )
        }
    }
}

@Composable
private fun QueueSlot(
    label: String,
    piece: Piece?,
    cellSize: Float,
    alpha: Float = 1f,
    stylePulse: Float = 0f,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val density = LocalDensity.current
    val cellSizeDp = with(density) { cellSize.toDp() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = uiColors.subtitle.copy(alpha = alpha * 0.82f),
            )
        }
        Box(
            modifier = Modifier
                .size(width = cellSizeDp * 2.8f, height = cellSizeDp * 2.8f)
                .clip(RoundedCornerShape(GameUiShapeTokens.previewSlotCorner))
                .background(uiColors.panelMuted.copy(alpha = alpha * 0.42f))
                .border(
                    width = 1.dp,
                    color = uiColors.boardEmptyCellBorder.copy(alpha = alpha * 0.52f),
                    shape = RoundedCornerShape(GameUiShapeTokens.previewSlotCorner),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (piece != null) {
                PieceBlocks(
                    piece = piece,
                    cellSize = cellSizeDp * 0.72f,
                    alpha = alpha * QueuePreviewAlpha,
                    pulse = stylePulse,
                )
            } else {
                Text(
                    text = resolveGameText(
                        gameText(GameTextKey.QueueEmpty)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = uiColors.subtitle.copy(alpha = 0.32f),
                )
            }
        }
    }
}

@Composable
fun FloatingFeedbackBubble(
    text: String,
    modifier: Modifier = Modifier,
    isBonus: Boolean = false,
    alpha: Float = 1f,
    driftY: Float = 0f,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = driftY
            }
            .clip(RoundedCornerShape(GameUiShapeTokens.badgeCorner))
            .background(
                if (isBonus) uiColors.success.copy(alpha = 0.94f) else uiColors.metricCard.copy(
                    alpha = 0.94f
                )
            )
            .border(
                width = 1.dp,
                color = if (isBonus) Color.White.copy(alpha = 0.42f) else uiColors.panelStroke.copy(
                    alpha = 0.52f
                ),
                shape = RoundedCornerShape(GameUiShapeTokens.badgeCorner),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isBonus) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun rememberGameHaptics(): GameHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        object : GameHaptics {
            override fun perform(effect: GameHaptic) {
                val type = when (effect) {
                    GameHaptic.Light -> HapticFeedbackType.TextHandleMove
                    GameHaptic.Medium -> HapticFeedbackType.LongPress
                    GameHaptic.Heavy -> HapticFeedbackType.LongPress
                    GameHaptic.Warning -> HapticFeedbackType.LongPress
                    GameHaptic.Success -> HapticFeedbackType.LongPress
                }
                hapticFeedback.performHapticFeedback(type)
            }
        }
    }
}

fun dispatchFeedback(
    feedback: InteractionFeedback,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
) {
    feedback.sounds.forEach { soundPlayer.play(it) }
    feedback.haptics.forEach { haptics.perform(it) }
}

fun resolveSelectedColumn(
    piece: Piece?,
    overlayTopLeft: Offset?,
    boardRect: Rect,
    cellSizePx: Float,
    boardColumns: Int,
): Int? {
    if (piece == null || overlayTopLeft == null || boardRect == Rect.Zero || cellSizePx <= 0f) return null
    val maxColumn = boardColumns - piece.width
    if (maxColumn < 0) return null
    val approximateColumn = ((overlayTopLeft.x - boardRect.left) / cellSizePx).roundToInt()
    return approximateColumn.coerceIn(0, maxColumn)
}

fun pieceSpawnTopLeft(
    piece: Piece?,
    trayRect: Rect,
    boardRect: Rect,
    cellSizePx: Float,
    column: Int?,
): Offset? {
    if (piece == null || trayRect == Rect.Zero || boardRect == Rect.Zero || cellSizePx <= 0f || column == null) return null
    return Offset(
        x = columnToLeft(column, boardRect, cellSizePx),
        y = trayRect.center.y - (piece.height * cellSizePx) / 2f,
    )
}

fun resolveSpawnColumn(piece: Piece?, boardColumns: Int, lastPlacementColumn: Int?): Int? {
    if (piece == null) return null
    val last = lastPlacementColumn ?: (boardColumns / 2)
    return last.coerceIn(0, (boardColumns - piece.width).coerceAtLeast(0))
}

fun columnToLeft(column: Int, boardRect: Rect, cellSizePx: Float): Float =
    boardRect.left + (column * cellSizePx)

internal fun Rect.toStackShiftLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        this.left - hostRect.left,
        this.top - hostRect.top,
        this.right - hostRect.left,
        this.bottom - hostRect.top,
    )
}

@Preview
@Composable
private fun GameScreenLightPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Light,
        blockVisualStyle = BlockVisualStyle.Prism,
        themeColorPalette = AppColorPalette.Classic
    )
    BlockGamesTheme(settings = settings) {
        GameScreen(
            gameState = previewGameState(),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { previewImpactPointsPreview() },
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onReplaceActivePiece = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
        )
    }
}

@Preview
@Composable
private fun GameScreenDarkPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.DynamicLiquid,
        themeColorPalette = AppColorPalette.ModernNeon
    )
    BlockGamesTheme(settings = settings) {
        GameScreen(
            gameState = previewGameState(),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { previewImpactPointsPreview() },
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onReplaceActivePiece = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
        )
    }
}


@Preview(name = "GameScreen - Onboarding - Launch", widthDp = 412, heightDp = 915)
@Composable
private fun GameScreenOnboardingLaunchPreview() {
    val settings = AppSettings(themeMode = AppThemeMode.Dark)
    val scene = StackShiftGameOnboardingStateFactory.scene(
        StackShiftOnboardingStage.DragAndLaunch
    )
    BlockGamesTheme(settings = settings) {
        GameScreen(
            gameState = scene.gameState,
            onRequestPreview = { null },
            onResolvePreviewImpact = { emptySet() },
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onReplaceActivePiece = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 1,
            interactiveOnboardingTotalSteps = 6,
        )
    }
}

@Preview(name = "GameScreen - Onboarding - Special", widthDp = 412, heightDp = 915)
@Composable
private fun GameScreenOnboardingSpecialPreview() {
    val settings = AppSettings(themeMode = AppThemeMode.Dark)
    val scene = StackShiftGameOnboardingStateFactory.scene(
        StackShiftOnboardingStage.ColumnClearer
    )
    BlockGamesTheme(settings = settings) {
        GameScreen(
            gameState = scene.gameState,
            onRequestPreview = { null },
            onResolvePreviewImpact = { emptySet() },
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onReplaceActivePiece = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 0,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 3,
            interactiveOnboardingTotalSteps = 6,
        )
    }
}


private fun previewGameState(
    status: GameStatus = GameStatus.Running,
    boardPoints: List<Pair<GridPoint, CellTone>>? = null,
): GameState {
    val config = GameConfig(columns = 10, rows = 16)
    var board = BoardMatrix.empty(columns = 10, rows = 16)

    if (boardPoints != null) {
        boardPoints.forEach { (point, tone) ->
            board = board.fill(listOf(point), tone)
        }
    } else {
        board = board
            .fill(
                points = listOf(
                    GridPoint(0, 0),
                    GridPoint(0, 1),
                    GridPoint(1, 0),
                    GridPoint(1, 1),
                    GridPoint(2, 0),
                ),
                tone = CellTone.Blue,
            ).fill(
                points = listOf(
                    GridPoint(2, 3),
                    GridPoint(2, 4),
                    GridPoint(3, 3),
                    GridPoint(4, 3),
                    GridPoint(5, 4),
                    GridPoint(6, 5),
                ),
                tone = CellTone.Emerald,
            )
            .fill(
                points = listOf(
                    GridPoint(2, 6),
                    GridPoint(6, 6),
                    GridPoint(7, 7),
                ),
                tone = CellTone.Gold,
            )
            .fill(
                points = listOf(
                    GridPoint(8, 1),
                    GridPoint(8, 2),
                    GridPoint(9, 1),
                ),
                tone = CellTone.Coral,
            )
    }
    return GameState(
        config = config,
        board = board,
        score = 14200,
        level = 3,
        linesCleared = 18,
        difficultyStage = 2,
        secondsUntilDifficultyIncrease = 12,
        activePiece = Piece(
            id = 1,
            kind = PieceKind.T,
            tone = CellTone.Gold,
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
        nextQueue = listOf(
            Piece(
                id = 2,
                kind = PieceKind.Domino,
                tone = CellTone.Cyan,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
                width = 2,
                height = 1,
                special = SpecialBlockType.Ghost,
            ),
            Piece(
                id = 3,
                kind = PieceKind.Square,
                tone = CellTone.Violet,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(0, 1), GridPoint(1, 1)),
                width = 2,
                height = 2,
                special = SpecialBlockType.None,
            ),
        ),
        status = status,
    )
}

private fun previewPlacementPreview(): PlacementPreview = PlacementPreview(
    selectedColumn = 3,
    entryAnchor = GridPoint(3, 0),
    landingAnchor = GridPoint(3, 8),
    occupiedCells = listOf(GridPoint(3, 8), GridPoint(4, 8), GridPoint(5, 8), GridPoint(4, 9)),
    coveredColumns = 3..5,
)

private fun previewImpactPointsPreview(): Set<GridPoint> = setOf(
    GridPoint(3, 8),
    GridPoint(4, 8),
    GridPoint(5, 8),
    GridPoint(4, 9)
)

@Composable
fun GameOverBoardClearOverlay(
    revealProgress: Float,
    rowCount: Int,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val fullyClearedRows = (revealProgress * rowCount).toInt()
    val partialClearAlpha = 1f - ((revealProgress * rowCount) - fullyClearedRows)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellHeight = size.height / rowCount
        for (i in 0 until fullyClearedRows) {
            drawRect(
                color = uiColors.panel.copy(alpha = GameOverBoardRowCoverAlpha),
                topLeft = Offset(0f, size.height - (i + 1) * cellHeight),
                size = Size(size.width, cellHeight)
            )
        }
        if (fullyClearedRows < rowCount) {
            drawRect(
                color = uiColors.panel.copy(alpha = GameOverBoardRowCoverAlpha * (1f - partialClearAlpha)),
                topLeft = Offset(0f, size.height - (fullyClearedRows + 1) * cellHeight),
                size = Size(size.width, cellHeight),
            )
        }
    }
}




