package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.logic.ChainShiftPath
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.BoardCell
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.platform.feedback.GameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpGameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.blockgames.platform.feedback.SoundEffectPlayer
import com.ugurbuga.blockgames.presentation.game.GameDispatchResult
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingScene
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.dailychallenge.ChallengeTasksDock
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.game.onboarding.ChainShiftInteractiveGameOnboardingOverlay
import com.ugurbuga.blockgames.ui.game.onboarding.ChainShiftInteractiveGameOnboardingUi
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

private const val ChainShiftMovementWindowMillis = 1000L
private const val ChainShiftRetreatDurationMillis = 260
private const val ChainShiftExplosionDurationMillis = 340
private const val ChainShiftMovementProgressCap = 1.0f
private const val ChainShiftLeadInStartCells = 1.65f
private const val ChainShiftLeadInNearCells = 0.72f
private const val ChainShiftAimDeadZoneCells = 0.42f
private const val ChainShiftLaunchDurationMillis = 320
private const val ChainShiftSettleDurationMillis = 180
private const val ChainShiftAimAngleToleranceRadians = 0.15f
private const val ChainShiftMissLaunchOvershootCells = 2.0f

@Composable
fun ChainShiftGameScreen(
    gameState: GameState,
    onRequestPreview: (Long, GridPoint) -> PlacementPreview?,
    onShootTarget: (GridPoint) -> GameDispatchResult,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    haptics: GameHaptics = NoOpGameHaptics,
    onRestart: () -> Unit,
    onRewardedRevive: () -> Unit = {},
    onBack: () -> Unit,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    adController: GameAdController = NoOpGameAdController,
    interactiveOnboardingScene: ChainShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val boardStyle = settings.blockVisualStyle
    val palette = settings.blockColorPalette
    val darkTheme = isBlockGamesDarkTheme(settings)
    val showExtraLifeButton = adController !== NoOpGameAdController
    val path = remember(gameState.config) { ChainShiftPath.spiral(gameState.config) }
    val coroutineScope = rememberCoroutineScope()
    val updatedShootTarget by rememberUpdatedState(onShootTarget)

    var cellSize by remember { mutableStateOf(0f) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var hostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var currentTime by remember { mutableLongStateOf(currentEpochMillis()) }

    var aimTrajectory by remember { mutableStateOf<ChainShiftAimTrajectory?>(null) }
    var activeLaunch by remember { mutableStateOf<ChainShiftLaunchAnimation?>(null) }
    var activeSettle by remember { mutableStateOf<ChainShiftSettleAnimation?>(null) }
    var isLaunching by remember { mutableStateOf(false) }

    var previousChainCells by remember { mutableStateOf<List<ChainShiftRenderedCell>?>(null) }
    var transitionCells by remember { mutableStateOf<List<ChainShiftTransitionCell>>(emptyList()) }
    var explodingCells by remember { mutableStateOf<List<ChainShiftExplodingCell>>(emptyList()) }
    val chainTransitionProgress = remember { Animatable(1f) }
    val explosionProgress = remember { Animatable(1f) }
    val launchProgress = remember { Animatable(0f) }
    val settleProgress = remember { Animatable(1f) }

    val boardCenter by remember(gameState.config, cellSize) {
        derivedStateOf {
            Offset(
                x = (gameState.config.columns * cellSize) / 2f,
                y = (gameState.config.rows * cellSize) / 2f,
            )
        }
    }

    val pathLookup by remember(path) {
        derivedStateOf { path.withIndex().associate { it.value to it.index } }
    }
    val currentChainCells by remember(gameState.board, path) {
        derivedStateOf { extractChainCells(gameState, path) }
    }

    LaunchedEffect(gameState.status, interactiveOnboardingScene, gameState.lastActionTime) {
        while (gameState.status == GameStatus.Running) {
            currentTime = currentEpochMillis()
            delay(16)
        }
    }

    val movementProgress = remember(currentTime, gameState.lastActionTime, interactiveOnboardingScene, gameState.status) {
        if (interactiveOnboardingScene != null || gameState.status != GameStatus.Running) {
            1f
        } else {
            ((currentTime - gameState.lastActionTime).coerceAtLeast(0L).toFloat() / ChainShiftMovementWindowMillis.toFloat())
                .coerceIn(0f, ChainShiftMovementProgressCap)
        }
    }
    val movementOffset = 1f - movementProgress.coerceIn(0f, 1f)

    val aimCandidates by remember(gameState.activePiece?.id, gameState.board, path, cellSize, boardCenter, movementOffset) {
        derivedStateOf {
            val pieceId = gameState.activePiece?.id ?: return@derivedStateOf emptyList()
            if (cellSize <= 0f) return@derivedStateOf emptyList()
            buildChainShiftAimCandidates(
                path = path,
                gameState = gameState,
                movementOffset = movementOffset,
                pieceId = pieceId,
                cellSizePx = cellSize,
                previewProvider = onRequestPreview,
                center = boardCenter,
            )
        }
    }

    LaunchedEffect(gameState.board, gameState.clearAnimationToken, gameState.status, gameState.feedbackToken) {
        val previous = previousChainCells
        val isFreshGame =
            gameState.status == GameStatus.Running &&
                gameState.score == 0 &&
                gameState.linesCleared == 0 &&
                gameState.clearAnimationToken == 0L &&
                gameState.feedbackToken == 0L
        if (previous == null || isFreshGame) {
            transitionCells = currentChainCells.map { cell ->
                ChainShiftTransitionCell(
                    id = cell.cell.value,
                    tone = cell.cell.tone,
                    sourceIndex = cell.index.toFloat(),
                    targetIndex = cell.index.toFloat(),
                )
            }
            previousChainCells = currentChainCells
            chainTransitionProgress.snapTo(1f)
            return@LaunchedEffect
        }

        val isAmbientForwardAdvance = gameState.recentlyExplodedPoints.isEmpty()
        if (isAmbientForwardAdvance) {
            transitionCells = currentChainCells.map { cell ->
                ChainShiftTransitionCell(
                    id = cell.cell.value,
                    tone = cell.cell.tone,
                    sourceIndex = cell.index.toFloat(),
                    targetIndex = cell.index.toFloat(),
                )
            }
            previousChainCells = currentChainCells
            chainTransitionProgress.snapTo(1f)
            return@LaunchedEffect
        }

        val previousById = previous.associateBy { it.cell.value }
        val nextTransitionCells = currentChainCells.map { cell ->
            val targetIndex = cell.index.toFloat()
            val sourceIndex = previousById[cell.cell.value]?.index?.toFloat() ?: targetIndex
            ChainShiftTransitionCell(
                id = cell.cell.value,
                tone = cell.cell.tone,
                sourceIndex = sourceIndex,
                targetIndex = targetIndex,
            )
        }
        previousChainCells = currentChainCells
        val hasVisibleShift = nextTransitionCells.any { cell ->
            (cell.targetIndex - cell.sourceIndex).absoluteValue >= 0.02f
        }
        if (!hasVisibleShift) {
            transitionCells = nextTransitionCells
            chainTransitionProgress.snapTo(1f)
            return@LaunchedEffect
        }

        transitionCells = nextTransitionCells.map { it.copy(targetIndex = it.sourceIndex) }
        chainTransitionProgress.snapTo(0f)
        transitionCells = nextTransitionCells
        chainTransitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (gameState.recentlyExplodedPoints.isNotEmpty()) {
                    ChainShiftRetreatDurationMillis + 70
                } else {
                    ChainShiftRetreatDurationMillis
                },
                easing = LinearEasing,
            ),
        )
    }

    LaunchedEffect(gameState.clearAnimationToken) {
        if (gameState.recentlyExplodedPoints.isEmpty()) {
            explodingCells = emptyList()
            explosionProgress.snapTo(1f)
            return@LaunchedEffect
        }
        explodingCells = gameState.recentlyExplodedPoints.mapNotNull { point ->
            val tone = gameState.recentlyExplodedTones[point] ?: return@mapNotNull null
            val index = pathLookup[point] ?: return@mapNotNull null
            ChainShiftExplodingCell(
                tone = tone,
                index = index,
                movementOffset = movementOffset,
            )
        }
        explosionProgress.snapTo(0f)
        explosionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ChainShiftExplosionDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
        explodingCells = emptyList()
    }

    suspend fun launchProjectile(trajectory: ChainShiftAimTrajectory) {
        if (isLaunching || cellSize <= 0f) return
        val activePiece = gameState.activePiece ?: return
        val pieceIdInt = activePiece.id.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        
        val direction = trajectory.direction
        val boardSize = Size(
            width = gameState.config.columns * cellSize,
            height = gameState.config.rows * cellSize,
        )
        val exitPoint = rayToBoardExitPoint(boardCenter, direction, boardSize, cellSize * ChainShiftMissLaunchOvershootCells)
        
        val launch = ChainShiftLaunchAnimation(
            pieceId = pieceIdInt,
            tone = activePiece.tone,
            start = boardCenter,
            targetIndex = 0f,
            hitsChain = true,
            missTarget = exitPoint
        )
        isLaunching = true
        activeSettle = null
        activeLaunch = launch
        
        var hitCell: ChainShiftRenderedCell? = null
        launchProgress.snapTo(0f)
        
        val startTime = currentEpochMillis()
        while (currentEpochMillis() - startTime < ChainShiftLaunchDurationMillis && hitCell == null) {
            val progress = ((currentEpochMillis() - startTime).toFloat() / ChainShiftLaunchDurationMillis).coerceIn(0f, 1f)
            launchProgress.snapTo(progress)
            
            val projectilePos = boardCenter + (exitPoint - boardCenter) * progress
            val currentCells = extractChainCells(gameState, path)
            hitCell = currentCells.find { cell ->
                val cellPos = sampleChainShiftPathCenter(path, cellSize, cell.index.toFloat() - movementOffset)
                val dx = projectilePos.x - cellPos.x
                val dy = projectilePos.y - cellPos.y
                sqrt((dx * dx) + (dy * dy)) < cellSize * 0.48f
            }
            if (hitCell != null) break
            delay(16)
        }

        if (hitCell != null) {
            val result = updatedShootTarget(hitCell!!.point)
            result.feedback.sounds.forEach { soundPlayer.play(it) }
            result.feedback.haptics.forEach { haptics.perform(it) }
            
            activeLaunch = null
            activeSettle = ChainShiftSettleAnimation(
                pieceId = launch.pieceId,
                tone = launch.tone,
                targetIndex = hitCell!!.index.toFloat(),
            )
            settleProgress.snapTo(0f)
            settleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ChainShiftSettleDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            activeSettle = null
        } else {
            // Missed the chain entirely
            activeLaunch = null
        }
        isLaunching = false
    }



    fun updateAim(pointer: Offset) {
        aimTrajectory = resolveAimTrajectory(
            pointer = pointer,
            boardCenter = boardCenter,
            candidates = aimCandidates,
            cellSizePx = cellSize,
            boardSize = Size(
                width = gameState.config.columns * cellSize,
                height = gameState.config.rows * cellSize,
            ),
            overshootPx = cellSize * ChainShiftMissLaunchOvershootCells,
        )
    }

    val transition = rememberInfiniteTransition(label = "chainShiftPulse")
    val stylePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chainShiftStylePulse",
    )
    val boardTopSpacing = if (interactiveOnboardingScene != null) 176.dp else 24.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(uiColors.screenGradientTop, uiColors.screenGradientBottom),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .onGloballyPositioned { hostRectInRoot = it.boundsInRoot() },
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
                    stylePulse = stylePulse,
                )

                Spacer(modifier = Modifier.height(boardTopSpacing))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows.toFloat())
                        .blockGamesSurfaceShadow(
                            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                            elevation = 8.dp,
                        )
                        .clip(RoundedCornerShape(GameUiShapeTokens.panelCorner))
                        .background(uiColors.gameSurface.copy(alpha = 0.84f))
                        .onGloballyPositioned {
                            cellSize = it.size.width.toFloat() / gameState.config.columns
                            boardRectInRoot = it.boundsInRoot()
                        }
                        .pointerInput(
                            gameState.status,
                            gameState.activePiece?.id,
                            cellSize,
                        ) {
                            if (gameState.status != GameStatus.Running || cellSize <= 0f) return@pointerInput
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (isLaunching) continue

                                    updateAim(down.position)

                                    var released = false
                                    while (!released) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                            ?: break

                                        if (change.pressed) {
                                            updateAim(change.position)
                                        } else {
                                            val trajectory = aimTrajectory
                                            aimTrajectory = null
                                            if (trajectory != null && !isLaunching) {
                                                coroutineScope.launch {
                                                    launchProjectile(trajectory)
                                                }
                                            }
                                            released = true
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawChainShiftBoardBackground(
                            columns = gameState.config.columns,
                            rows = gameState.config.rows,
                            cellSizePx = cellSize,
                            boardStrokeColor = uiColors.boardOutline.copy(alpha = 0.28f),
                        )

                        val lineStroke = (size.minDimension / (gameState.config.columns * 10f)).coerceAtLeast(2f)

                        val isAmbientTick = gameState.recentlyExplodedPoints.isEmpty()
                        val renderedCells = if (transitionCells.isNotEmpty() && !isAmbientTick) {
                            transitionCells
                        } else {
                            currentChainCells.map { cell ->
                                ChainShiftTransitionCell(
                                    id = cell.cell.value,
                                    tone = cell.cell.tone,
                                    sourceIndex = cell.index.toFloat(),
                                    targetIndex = cell.index.toFloat(),
                                )
                            }
                        }
                        renderedCells.forEach { renderedCell ->
                            if (renderedCell.id == activeLaunch?.pieceId || renderedCell.id == activeSettle?.pieceId) {
                                return@forEach
                            }
                            val center = sampleChainShiftPathCenter(
                                path = path,
                                cellSizePx = cellSize,
                                fractionalIndex = lerpFloat(
                                    start = renderedCell.sourceIndex,
                                    end = renderedCell.targetIndex,
                                    progress = chainTransitionProgress.value,
                                ) - movementOffset,
                            )
                            val blockSize = cellSize * 0.92f
                            val topLeft = Offset(center.x - (blockSize / 2f), center.y - (blockSize / 2f))
                            drawCellBody(
                                tone = renderedCell.tone,
                                palette = palette,
                                style = boardStyle,
                                isDark = darkTheme,
                                topLeft = topLeft,
                                size = Size(blockSize, blockSize),
                                cornerRadius = CornerRadius(
                                    x = boardCellCornerRadiusPx(blockSize, boardStyle),
                                    y = boardCellCornerRadiusPx(blockSize, boardStyle),
                                ),
                                pulse = stylePulse,
                            )
                        }

                        explodingCells.forEach { explodingCell ->
                            val center = sampleChainShiftPathCenter(
                                path = path,
                                cellSizePx = cellSize,
                                fractionalIndex = explodingCell.index.toFloat() - explodingCell.movementOffset,
                            )
                            val reveal = explosionProgress.value
                            val explosionAlpha = 1f - reveal
                            val blockSize = cellSize * (0.92f + (0.26f * reveal))
                            drawCircle(
                                color = uiColors.guideAccent.copy(alpha = 0.18f * explosionAlpha),
                                radius = cellSize * (0.34f + (0.80f * reveal)),
                                center = center,
                            )
                            drawCircle(
                                color = uiColors.guideAccent.copy(alpha = 0.34f * explosionAlpha),
                                radius = cellSize * (0.16f + (0.42f * reveal)),
                                center = center,
                                style = Stroke(width = lineStroke * (1.1f + reveal)),
                            )
                            drawCellBody(
                                tone = explodingCell.tone,
                                palette = palette,
                                style = boardStyle,
                                isDark = darkTheme,
                                topLeft = Offset(center.x - (blockSize / 2f), center.y - (blockSize / 2f)),
                                size = Size(blockSize, blockSize),
                                cornerRadius = CornerRadius(
                                    x = boardCellCornerRadiusPx(blockSize, boardStyle),
                                    y = boardCellCornerRadiusPx(blockSize, boardStyle),
                                ),
                                pulse = stylePulse,
                                alpha = explosionAlpha,
                            )
                        }

                        // Aiming Guideline
                        aimTrajectory?.let { trajectory ->
                            val activeTone = gameState.activePiece?.tone
                            val guideColor = activeTone?.paletteColor(palette, darkTheme) ?: uiColors.guideAccent
                            
                            // Thick guide line always visible
                            drawLine(
                                color = guideColor.copy(alpha = 0.35f),
                                start = boardCenter,
                                end = trajectory.lineEnd,
                                strokeWidth = cellSize * 0.86f,
                                cap = StrokeCap.Round,
                            )

                            // If pointing at a target, draw the preview box
                            if (trajectory.selection != null) {
                                val targetCenter = trajectory.selection.preview.landingAnchor.center(cellSize)
                                val previewCorner = boardCellCornerRadiusPx(cellSize * 0.86f, boardStyle)
                                drawRoundRect(
                                    color = guideColor.copy(alpha = 0.4f),
                                    topLeft = Offset(
                                        x = targetCenter.x - (cellSize * 0.43f),
                                        y = targetCenter.y - (cellSize * 0.43f),
                                    ),
                                    size = Size(cellSize * 0.86f, cellSize * 0.86f),
                                    cornerRadius = CornerRadius(previewCorner, previewCorner),
                                )
                            }
                        }

                        // Projectile Animation
                        activeLaunch?.let { launch ->
                            val currentTarget = launch.missTarget ?: Offset.Zero
                            val projectileCenter = launch.start + ((currentTarget - launch.start) * launchProgress.value)
                            val launchColor = launch.tone.paletteColor(palette, darkTheme)
                            
                            // PreviewLine stays visible during flight
                            drawLine(
                                color = launchColor.copy(alpha = 0.35f),
                                start = launch.start,
                                end = currentTarget,
                                strokeWidth = cellSize * 0.86f,
                                cap = StrokeCap.Round,
                            )

                            drawCellBody(
                                tone = launch.tone,
                                palette = palette,
                                style = boardStyle,
                                isDark = darkTheme,
                                topLeft = Offset(
                                    x = projectileCenter.x - (cellSize * 0.42f),
                                    y = projectileCenter.y - (cellSize * 0.42f),
                                ),
                                size = Size(cellSize * 0.84f, cellSize * 0.84f),
                                cornerRadius = CornerRadius(
                                    x = boardCellCornerRadiusPx(cellSize * 0.84f, boardStyle),
                                    y = boardCellCornerRadiusPx(cellSize * 0.84f, boardStyle),
                                ),
                                pulse = stylePulse,
                            )
                        }

                        // Settle Animation
                        activeSettle?.let { settle ->
                            val currentTarget = sampleChainShiftPathCenter(
                                path = path,
                                cellSizePx = cellSize,
                                fractionalIndex = settle.targetIndex - movementOffset,
                            )
                            val settleScale = 1.14f - (0.14f * settleProgress.value)
                            val settleAlpha = 1f - settleProgress.value
                            val settleSize = cellSize * 0.86f * settleScale
                            drawCellBody(
                                tone = settle.tone,
                                palette = palette,
                                style = boardStyle,
                                isDark = darkTheme,
                                topLeft = Offset(
                                    x = currentTarget.x - (settleSize / 2f),
                                    y = currentTarget.y - (settleSize / 2f),
                                ),
                                size = Size(settleSize, settleSize),
                                cornerRadius = CornerRadius(
                                    x = boardCellCornerRadiusPx(settleSize, boardStyle),
                                    y = boardCellCornerRadiusPx(settleSize, boardStyle),
                                ),
                                pulse = stylePulse,
                                alpha = settleAlpha,
                            )
                        }

                        // Central active piece (only shown when not launching)
                        if (activeLaunch == null && activeSettle == null && !isLaunching) gameState.activePiece?.let { activePiece ->
                            val blockSize = cellSize * 0.86f
                            drawCellBody(
                                tone = activePiece.tone,
                                palette = palette,
                                style = boardStyle,
                                isDark = darkTheme,
                                topLeft = Offset(boardCenter.x - (blockSize / 2f), boardCenter.y - (blockSize / 2f)),
                                size = Size(blockSize, blockSize),
                                cornerRadius = CornerRadius(
                                    x = boardCellCornerRadiusPx(blockSize, boardStyle),
                                    y = boardCellCornerRadiusPx(blockSize, boardStyle),
                                ),
                                pulse = stylePulse,
                            )
                        }
                    }
                }

                Spacer(
                    modifier = if (interactiveOnboardingScene != null) {
                        Modifier.height(16.dp)
                    } else {
                        Modifier.weight(1f)
                    },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (gameState.activeChallenge != null) {
                        ChallengeTasksDock(
                            challenge = gameState.activeChallenge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (interactiveOnboardingScene != null) {
                val boardRect = remember(boardRectInRoot, hostRectInRoot) {
                    if (boardRectInRoot == Rect.Zero || hostRectInRoot == Rect.Zero) {
                        Rect.Zero
                    } else {
                        Rect(
                            left = boardRectInRoot.left - hostRectInRoot.left,
                            top = boardRectInRoot.top - hostRectInRoot.top,
                            right = boardRectInRoot.right - hostRectInRoot.left,
                            bottom = boardRectInRoot.bottom - hostRectInRoot.top,
                        )
                    }
                }
                ChainShiftInteractiveGameOnboardingOverlay(
                    ui = ChainShiftInteractiveGameOnboardingUi(
                        scene = interactiveOnboardingScene,
                        currentStep = interactiveOnboardingCurrentStep,
                        totalSteps = interactiveOnboardingTotalSteps,
                        isAwaitingPlacementCommit = interactiveOnboardingAwaitingCommit,
                    ),
                    boardRect = boardRect,
                    cellSizePx = cellSize,
                    modifier = Modifier.fillMaxSize(),
                    onBack = onInteractiveOnboardingReturnHome,
                )
            }
        }

        if (gameState.status == GameStatus.GameOver) {
            GameOverDialog(
                gameState = gameState,
                highestScore = highestScore,
                showNewHighScoreMessage = showNewHighScoreMessage,
                revealProgressProvider = { 1f },
                canUseExtraLife = !gameState.rewardedReviveUsed,
                isExtraLifeLoading = false,
                showExtraLifeButton = showExtraLifeButton,
                onPlayAgain = onRestart,
                onUseExtraLife = onRewardedRevive,
            )
        }

        if (interactiveOnboardingCompletionDialogVisible) {
            InteractiveOnboardingCompletionDialog(
                onStartGame = onInteractiveOnboardingStartGame,
                onReturnHome = onInteractiveOnboardingReturnHome,
            )
        }
    }
}

private data class ChainShiftAimSelection(
    val origin: GridPoint,
    val preview: PlacementPreview,
)

private data class ChainShiftAimCandidate(
    val selection: ChainShiftAimSelection,
    val targetCenter: Offset,
    val angleRadians: Float,
    val radiusPx: Float,
)

private data class ChainShiftAimTrajectory(
    val selection: ChainShiftAimSelection?,
    val direction: Offset,
    val lineEnd: Offset,
)

private data class ChainShiftLaunchAnimation(
    val pieceId: Int,
    val tone: CellTone,
    val start: Offset,
    val targetIndex: Float,
    val targetCellId: Int? = null,
    val hitsChain: Boolean,
    val missTarget: Offset? = null,
)

private data class ChainShiftSettleAnimation(
    val pieceId: Int,
    val tone: CellTone,
    val targetIndex: Float,
)

private data class ChainShiftRenderedCell(
    val index: Int,
    val point: GridPoint,
    val cell: BoardCell,
)

private data class ChainShiftTransitionCell(
    val id: Int,
    val tone: CellTone,
    val sourceIndex: Float,
    val targetIndex: Float,
)

private data class ChainShiftExplodingCell(
    val tone: CellTone,
    val index: Int,
    val movementOffset: Float,
)

private fun extractChainCells(
    gameState: GameState,
    path: List<GridPoint>,
): List<ChainShiftRenderedCell> = buildList {
    path.forEachIndexed { index, point ->
        val cell = gameState.board.cellAt(point.column, point.row) ?: return@forEachIndexed
        add(ChainShiftRenderedCell(index = index, point = point, cell = cell))
    }
}

private fun lerpFloat(
    start: Float,
    end: Float,
    progress: Float,
): Float = start + ((end - start) * progress.coerceIn(0f, 1f))

private fun sampleChainShiftPathCenter(
    path: List<GridPoint>,
    cellSizePx: Float,
    fractionalIndex: Float,
): Offset {
    if (path.isEmpty()) return Offset.Zero
    if (fractionalIndex < 0f) {
        val first = path.first().center(cellSizePx)
        val nearEntry = Offset(
            x = first.x - (cellSizePx * ChainShiftLeadInNearCells),
            y = first.y - (cellSizePx * ChainShiftLeadInNearCells),
        )
        val farEntry = Offset(
            x = first.x - (cellSizePx * ChainShiftLeadInStartCells),
            y = first.y - (cellSizePx * ChainShiftLeadInStartCells),
        )
        val leadProgress = (fractionalIndex + 1f).coerceIn(0f, 1f)
        val start = if (fractionalIndex < -0.5f) farEntry else nearEntry
        val end = if (fractionalIndex < -0.5f) nearEntry else first
        val segmentProgress = if (fractionalIndex < -0.5f) {
            (leadProgress / 0.5f).coerceIn(0f, 1f)
        } else {
            ((leadProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
        }
        return Offset(
            x = lerpFloat(start.x, end.x, segmentProgress),
            y = lerpFloat(start.y, end.y, segmentProgress),
        )
    }
    val clampedIndex = fractionalIndex.coerceIn(0f, path.lastIndex.toFloat())
    val startIndex = floor(clampedIndex).toInt().coerceIn(0, path.lastIndex)
    val endIndex = (startIndex + 1).coerceAtMost(path.lastIndex)
    val progress = clampedIndex - startIndex.toFloat()
    val start = path[startIndex].center(cellSizePx)
    val end = path[endIndex].center(cellSizePx)
    return Offset(
        x = lerpFloat(start.x, end.x, progress),
        y = lerpFloat(start.y, end.y, progress),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChainShiftBoardBackground(
    columns: Int,
    rows: Int,
    cellSizePx: Float,
    boardStrokeColor: Color,
) {
    if (cellSizePx <= 0f) return
    val corner = CornerRadius(cellSizePx * 0.16f, cellSizePx * 0.16f)
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            drawRoundRect(
                color = boardStrokeColor,
                topLeft = Offset(column * cellSizePx, row * cellSizePx),
                size = Size(cellSizePx, cellSizePx),
                cornerRadius = corner,
                style = Stroke(width = 1.1f),
            )
        }
    }
}

private fun buildChainShiftAimCandidates(
    path: List<GridPoint>,
    gameState: GameState,
    movementOffset: Float,
    pieceId: Long,
    cellSizePx: Float,
    previewProvider: (Long, GridPoint) -> PlacementPreview?,
    center: Offset,
): List<ChainShiftAimCandidate> {
    val chainCells = extractChainCells(gameState, path)
    return chainCells.mapNotNull { rendered ->
        previewProvider(pieceId, rendered.point)?.let { preview ->
            val targetCenter = sampleChainShiftPathCenter(
                path = path,
                cellSizePx = cellSizePx,
                fractionalIndex = rendered.index.toFloat() - movementOffset,
            )
            val delta = targetCenter - center
            ChainShiftAimCandidate(
                selection = ChainShiftAimSelection(origin = rendered.point, preview = preview),
                targetCenter = targetCenter,
                angleRadians = atan2(delta.y, delta.x),
                radiusPx = sqrt((delta.x * delta.x) + (delta.y * delta.y)),
            )
        }
    }
}

private fun resolveAimTrajectory(
    pointer: Offset,
    boardCenter: Offset,
    candidates: List<ChainShiftAimCandidate>,
    cellSizePx: Float,
    boardSize: Size,
    overshootPx: Float,
): ChainShiftAimTrajectory? {
    val dx = pointer.x - boardCenter.x
    val dy = pointer.y - boardCenter.y
    val distanceSquared = (dx * dx) + (dy * dy)
    
    // Very small deadzone to allow 360 rotation even near the center
    if (distanceSquared < 100f) {
        return null
    }
    
    val distance = sqrt(distanceSquared)
    val direction = Offset(dx / distance, dy / distance)
    val pointerAngle = atan2(dy, dx)
    
    // Always calculate the line end to be the board exit point
    val lineEnd = rayToBoardExitPoint(
        start = boardCenter,
        direction = direction,
        boardSize = boardSize,
        overshootPx = overshootPx,
    )
    
    val hitCandidates = candidates.filter { candidate ->
        val angleDistance = circularAngleDistance(pointerAngle, candidate.angleRadians)
        val rayDistance = distanceFromRay(
            rayStart = boardCenter,
            rayDirection = direction,
            point = candidate.targetCenter,
        )
        // High precision check: ray must pass through the block's hit area
        angleDistance <= ChainShiftAimAngleToleranceRadians || rayDistance <= (cellSizePx * 0.42f)
    }

    // Among those on the ray, we hit the innermost one first
    val selectedCandidate = hitCandidates.minByOrNull { it.radiusPx }

    return ChainShiftAimTrajectory(
        selection = selectedCandidate?.selection,
        direction = direction,
        lineEnd = lineEnd,
    )
}

private fun circularAngleDistance(first: Float, second: Float): Float {
    val raw = (first - second).absoluteValue
    return minOf(raw, (kotlin.math.PI.toFloat() * 2f) - raw)
}

private fun distanceFromRay(
    rayStart: Offset,
    rayDirection: Offset,
    point: Offset,
): Float {
    val delta = point - rayStart
    val projection = (delta.x * rayDirection.x) + (delta.y * rayDirection.y)
    if (projection <= 0f) return sqrt((delta.x * delta.x) + (delta.y * delta.y))
    val closest = rayStart + (rayDirection * projection)
    val distance = point - closest
    return sqrt((distance.x * distance.x) + (distance.y * distance.y))
}

private fun rayToBoardExitPoint(
    start: Offset,
    direction: Offset,
    boardSize: Size,
    overshootPx: Float,
): Offset {
    val intersections = buildList {
        if (direction.x > 0f) add((boardSize.width - start.x) / direction.x)
        if (direction.x < 0f) add((0f - start.x) / direction.x)
        if (direction.y > 0f) add((boardSize.height - start.y) / direction.y)
        if (direction.y < 0f) add((0f - start.y) / direction.y)
    }.filter { it > 0f }
    val distance = intersections.minOrNull() ?: 0f
    return start + (direction * (distance + overshootPx))
}

private fun GridPoint.center(cellSize: Float): Offset = Offset(
    x = ((column + 0.5f) * cellSize),
    y = ((row + 0.5f) * cellSize),
)


@Preview
@Composable
private fun ChainShiftGameScreenPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        themeColorPalette = AppColorPalette.ModernNeon,
        blockVisualStyle = BlockVisualStyle.Prism
    )
    BlockGamesTheme(settings = settings) {
        ChainShiftGameScreen(
            gameState = previewChainShiftGameState(),
            onRequestPreview = { _, _ -> null },
            onShootTarget = { GameDispatchResult() },
            onRestart = {},
            onBack = {},
            highestScore = 50000,
        )
    }
}

private fun previewChainShiftGameState(): GameState {
    val config = GameConfig(columns = 9, rows = 9)
    val path = ChainShiftPath.spiral(config)
    var board = BoardMatrix.empty(config.columns, config.rows)
    val tones = listOf(CellTone.Cyan, CellTone.Gold, CellTone.Violet, CellTone.Emerald, CellTone.Coral)

    // Populate a part of the spiral
    path.take(15).forEachIndexed { index, point ->
        board = board.fill(
            points = listOf(point),
            tone = tones[index % tones.size],
            value = index + 1
        )
    }

    return GameState(
        config = config,
        gameplayStyle = GameplayStyle.ChainShift,
        board = board,
        score = 1250,
        linesCleared = 0,
        level = 1,
        difficultyStage = 0,
        secondsUntilDifficultyIncrease = 15,
        status = GameStatus.Running,
        activePiece = Piece(
            id = 100,
            kind = PieceKind.Single,
            tone = CellTone.Cyan,
            cells = listOf(GridPoint(0, 0)),
            width = 1,
            height = 1
        ),
        nextQueue = listOf(
            Piece(id = 101, kind = PieceKind.Single, tone = CellTone.Gold, cells = listOf(GridPoint(0, 0)), width = 1, height = 1)
        )
    )
}
