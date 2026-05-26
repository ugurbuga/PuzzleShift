package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.restart_cancel
import blockgames.composeapp.generated.resources.restart_confirm
import blockgames.composeapp.generated.resources.restart_confirm_body
import blockgames.composeapp.generated.resources.restart_confirm_title
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
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.SumShiftOnboardingScene
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStage
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStateFactory
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.RestartConfirmDialog
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.rememberBlockStylePulse
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import org.jetbrains.compose.resources.stringResource

private enum class SumShiftCellInteractionMode {
    Select,
    Disable,
}

@Composable
internal fun SumShiftGameScreen(
    gameState: GameState,
    onTapCell: (GridPoint) -> Unit,
    onRestart: () -> Unit,
    onManualDisabledCellsChange: (Set<GridPoint>) -> Unit = {},
    onWrongTap: () -> Unit = {},
    onRewardedRevive: () -> Unit = {},
    onRewardedHint: () -> Unit = {},
    onBack: () -> Unit,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    adController: GameAdController = NoOpGameAdController,
    interactiveOnboardingScene: SumShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    var showRestartDialog by remember { mutableStateOf(false) }
    var interactionMode by remember { mutableStateOf(SumShiftCellInteractionMode.Select) }
    var displayedBoardState by remember { mutableStateOf(gameState) }
    var outgoingBoardState by remember { mutableStateOf<GameState?>(null) }
    var lastAnimatedClearToken by remember { mutableLongStateOf(gameState.clearAnimationToken) }
    var wrongTapPoint by remember { mutableStateOf<GridPoint?>(null) }
    val outgoingClearProgress = remember { Animatable(1f) }
    val incomingRevealProgress = remember { Animatable(1f) }
    val blockStylePulse = rememberBlockStylePulse(style = LocalAppSettings.current.blockVisualStyle)

    LaunchedEffect(gameState) {
        if (gameState.sumShiftPreparingBoard && displayedBoardState.board == gameState.board) {
            outgoingBoardState = null
            outgoingClearProgress.snapTo(1f)
            incomingRevealProgress.snapTo(1f)
            return@LaunchedEffect
        }

        val shouldAnimateBoardTransition =
            gameState.status == GameStatus.Running &&
                gameState.clearAnimationToken != lastAnimatedClearToken &&
                displayedBoardState.board != gameState.board

        if (!shouldAnimateBoardTransition) {
            outgoingBoardState = null
            displayedBoardState = gameState
            outgoingClearProgress.snapTo(1f)
            incomingRevealProgress.snapTo(1f)
            lastAnimatedClearToken = gameState.clearAnimationToken
            return@LaunchedEffect
        }

        outgoingBoardState = displayedBoardState
        displayedBoardState = gameState
        outgoingClearProgress.snapTo(0f)
        incomingRevealProgress.snapTo(0f)
        outgoingClearProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 820),
        )
        outgoingBoardState = null
        incomingRevealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 920),
        )
        lastAnimatedClearToken = gameState.clearAnimationToken
    }

    LaunchedEffect(wrongTapPoint) {
        wrongTapPoint ?: return@LaunchedEffect
        kotlinx.coroutines.delay(280L)
        wrongTapPoint = null
    }

    val systemDisabledCells = remember(gameState) { gameState.systemDisabledSumShiftCells() }
    val disabledCells = remember(gameState.sumShiftManualDisabledCells, systemDisabledCells) {
        gameState.sumShiftManualDisabledCells + systemDisabledCells
    }
    val guidedCells = remember(interactiveOnboardingScene, gameState.sumShiftSelectedCells) {
        interactiveOnboardingScene?.remainingRequiredSelection(gameState.sumShiftSelectedCells).orEmpty()
    }
    val effectiveInteractionMode = if (interactiveOnboardingScene != null) {
        SumShiftCellInteractionMode.Select
    } else {
        interactionMode
    }
    val hasRewardedHint = remember(gameState) { gameState.findSumShiftHintPoint() != null }
    val isPreparingBoard = gameState.sumShiftPreparingBoard
    val isSolvedBoard = gameState.isSumShiftSolvedBoard()
    val shouldShowFullPreparationCard =
        isPreparingBoard &&
            gameState.score == 0 &&
            gameState.linesCleared == 0 &&
            gameState.clearAnimationToken == 0L
    val boardInteractionEnabled =
        gameState.status == GameStatus.Running && !isPreparingBoard && !isSolvedBoard && outgoingBoardState == null && incomingRevealProgress.value >= 0.999f


    if (showRestartDialog) {
        RestartConfirmDialog(
            onDismissRequest = { showRestartDialog = false },
            title = stringResource(Res.string.restart_confirm_title),
            message = stringResource(Res.string.restart_confirm_body),
            confirmLabel = stringResource(Res.string.restart_confirm),
            dismissLabel = stringResource(Res.string.restart_cancel),
            onConfirm = {
                showRestartDialog = false
                if (adController === NoOpGameAdController) {
                    onRestart()
                } else {
                    adController.showRestartInterstitial {
                        onRestart()
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

    if (gameState.status == GameStatus.GameOver) {
        GameOverDialog(
            gameState = gameState,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            revealProgressProvider = { 1f },
            canUseExtraLife = !gameState.rewardedReviveUsed,
            isExtraLifeLoading = false,
            showExtraLifeButton = adController !== NoOpGameAdController,
            onPlayAgain = {
                if (adController === NoOpGameAdController) {
                    onRestart()
                } else {
                    adController.showRestartInterstitial {
                        onRestart()
                    }
                }
            },
            onUseExtraLife = onRewardedRevive,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundBrush(uiColors))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MinimalTopBar(
            gameState = gameState,
            scoreHighlightStrengthProvider = { 0f },
            scoreHighlightScaleProvider = { 1f },
            remainingTimeLabel = stringResource(Res.string.time_remaining),
            onBack = onBack,
            onRestart = { showRestartDialog = true },
        )

        if (interactiveOnboardingScene != null) {
            SumShiftSummaryCard(
                gameState = gameState,
                highestScore = highestScore,
            )
        } else {
            SumShiftMistakeStatusCard(
                mistakesUsed = gameState.sumShiftMistakesUsed,
            )
        }

        interactiveOnboardingScene?.let { scene ->
            SumShiftOnboardingHintCard(
                scene = scene,
                currentStep = interactiveOnboardingCurrentStep,
                totalSteps = interactiveOnboardingTotalSteps,
            )
        }

        when {
            shouldShowFullPreparationCard -> {
                SumShiftPreparingBoardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    title = "Oyun hazırlanıyor",
                    body = "Rastgele mod seçildi, yeni düzen oluşturuluyor.",
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    SumShiftBoardCard(
                        gameState = displayedBoardState,
                        modifier = Modifier.fillMaxSize(),
                        outgoingGameState = outgoingBoardState,
                        outgoingClearProgress = outgoingClearProgress.value,
                        incomingRevealProgress = incomingRevealProgress.value,
                        disabledCells = disabledCells,
                        manualDisabledCells = gameState.sumShiftManualDisabledCells,
                        guidedCells = guidedCells,
                        stylePulse = blockStylePulse,
                        wrongTapPoint = wrongTapPoint,
                        onTapCell = cellTap@ { point ->
                            when (effectiveInteractionMode) {
                                SumShiftCellInteractionMode.Select -> {
                                    if (interactiveOnboardingScene != null && !interactiveOnboardingScene.allowsGuidedTap(point, gameState.sumShiftSelectedCells)) {
                                        wrongTapPoint = point
                                        onWrongTap()
                                        return@cellTap
                                    }
                                    if (point !in disabledCells) {
                                        val nextSelected = gameState.sumShiftSelectedCells.toggle(point)
                                        if (gameState.isSumShiftPlayableWith(nextSelected, gameState.sumShiftManualDisabledCells)) {
                                            onTapCell(point)
                                        } else {
                                            wrongTapPoint = point
                                            onWrongTap()
                                        }
                                    }
                                }

                                SumShiftCellInteractionMode.Disable -> {
                                    if (point in systemDisabledCells && point !in gameState.sumShiftManualDisabledCells) {
                                        return@cellTap
                                    }

                                    val nextManualDisabledCells = gameState.sumShiftManualDisabledCells.toggle(point)
                                    val shouldDisable = point !in gameState.sumShiftManualDisabledCells
                                    val nextSelected = if (shouldDisable && point in gameState.sumShiftSelectedCells) {
                                        gameState.sumShiftSelectedCells - point
                                    } else {
                                        gameState.sumShiftSelectedCells
                                    }
                                    if (gameState.isSumShiftPlayableWith(nextSelected, nextManualDisabledCells)) {
                                        if (shouldDisable && point in gameState.sumShiftSelectedCells) {
                                            onTapCell(point)
                                        }
                                        onManualDisabledCellsChange(nextManualDisabledCells)
                                    } else {
                                        wrongTapPoint = point
                                        onWrongTap()
                                    }
                                }
                            }
                        },
                        controlsEnabled = boardInteractionEnabled,
                    )

                    if (isPreparingBoard) {
                        SumShiftPreparingOverlay()
                    }
                }
            }
        }

        if (interactiveOnboardingScene == null) {
            SumShiftControlsRow(
                interactionMode = interactionMode,
                onInteractionModeChange = { interactionMode = it },
                onClearMarks = {
                    onManualDisabledCellsChange(emptySet())
                    gameState.sumShiftSelectedCells
                        .sortedWith(compareBy(GridPoint::row, GridPoint::column))
                        .forEach(onTapCell)
                },
                onRewardedHint = onRewardedHint,
                controlsEnabled = boardInteractionEnabled,
                adController = adController,
                stylePulse = blockStylePulse,
                hintEnabled = hasRewardedHint,
            )
        }
    }
}

@Composable
private fun SumShiftSummaryCard(
    gameState: GameState,
    highestScore: Int,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(shape = shape, elevation = 10.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SumShiftInfoChip(
                label = "Rows",
                value = "${gameState.completedSumShiftRows()}/${gameState.sumShiftRowTargets.size}",
                modifier = Modifier.weight(1f),
            )
            SumShiftInfoChip(
                label = "Cols",
                value = "${gameState.completedSumShiftColumns()}/${gameState.sumShiftColumnTargets.size}",
                modifier = Modifier.weight(1f),
            )
            SumShiftInfoChip(
                label = "Hata",
                value = "${(1 - gameState.sumShiftMistakesUsed).coerceAtLeast(0)}",
                modifier = Modifier.weight(1f),
            )
            SumShiftInfoChip(
                label = "Best",
                value = highestScore.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SumShiftMistakeStatusCard(
    mistakesUsed: Int,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val remaining = (1 - mistakesUsed).coerceAtLeast(0)
    val shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = uiColors.metricCard.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.70f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hata hakkı",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = uiColors.subtitle,
            )
            Text(
                text = "$remaining / 1",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = if (remaining > 0) uiColors.success else uiColors.danger,
            )
        }
    }
}

@Composable
private fun SumShiftInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
    Surface(
        modifier = modifier,
        shape = shape,
        color = uiColors.metricCard.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.68f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = uiColors.subtitle,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SumShiftPreparingBoardCard(
    modifier: Modifier = Modifier,
    title: String = "Oyun hazırlanıyor",
    body: String = "Yeni düzen oluşturuluyor, lütfen bekle.",
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .blockGamesSurfaceShadow(shape = shape, elevation = 16.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.80f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SumShiftPreparingOverlay() {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.panelCorner)

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .widthIn(max = 260.dp),
        shape = shape,
        color = uiColors.panel.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.74f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Yeni oyun hazırlanıyor",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Eski tahta kısa süre daha ekranda kalıyor.",
                style = MaterialTheme.typography.bodySmall,
                color = uiColors.subtitle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SumShiftControlsRow(
    interactionMode: SumShiftCellInteractionMode,
    onInteractionModeChange: (SumShiftCellInteractionMode) -> Unit,
    onClearMarks: () -> Unit,
    onRewardedHint: () -> Unit,
    controlsEnabled: Boolean,
    adController: GameAdController,
    stylePulse: Float,
    hintEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SumShiftToolButton(
            icon = Icons.Filled.Refresh,
            onClick = onClearMarks,
            modifier = Modifier.weight(1f),
            tone = CellTone.Gold,
            enabled = controlsEnabled,
        )
        SumShiftInteractionModeSwitch(
            selectedMode = interactionMode,
            onModeChange = onInteractionModeChange,
            modifier = Modifier.weight(1.2f),
            enabled = controlsEnabled,
        )
        SumShiftToolButton(
            icon = Icons.Filled.Lightbulb,
            onClick = onRewardedHint,
            modifier = Modifier.weight(1f),
            tone = CellTone.Cyan,
            enabled = controlsEnabled && hintEnabled && adController !== NoOpGameAdController,
            adController = adController,
            stylePulse = stylePulse,
            isRewarded = true,
        )
    }
}

@Composable
private fun SumShiftToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: CellTone,
    enabled: Boolean = true,
    adController: GameAdController = NoOpGameAdController,
    stylePulse: Float = 0f,
    isRewarded: Boolean = false,
) {
    if (isRewarded) {
        var loading by remember { mutableStateOf(false) }
        Box(
            modifier = modifier.height(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            TopBarActionBlockButton(
                tone = tone,
                icon = icon,
                contentDescription = "Hint",
                onClick = onClick@{
                    if (loading) return@onClick
                    loading = true
                    adController.showRewardedAd { success ->
                        loading = false
                        if (success) {
                            onClick()
                        }
                    }
                },
                enabled = enabled && !loading,
                pulse = stylePulse,
                size = 52.dp,
                showAdIcon = true,
                extraAlpha = 0.94f,
            )
        }
        return
    }

    val uiColors = BlockGamesThemeTokens.uiColors
    val accent = when (tone) {
        CellTone.Gold, CellTone.Amber -> uiColors.actionWarning
        CellTone.Emerald, CellTone.Lime -> uiColors.actionSuccess
        CellTone.Coral, CellTone.Rose -> uiColors.actionDanger
        else -> uiColors.actionPrimary
    }
    val shape = RoundedCornerShape(GameUiShapeTokens.buttonCorner)

    Surface(
        modifier = modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.72f)
            .blockGamesSurfaceShadow(shape = shape, elevation = 8.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = if (enabled) accent.copy(alpha = 0.94f) else uiColors.panelMuted.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accent.copy(alpha = 0.76f) else uiColors.panelStroke.copy(alpha = 0.74f),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) uiColors.actionIcon else uiColors.actionIconDisabled,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SumShiftInteractionModeSwitch(
    selectedMode: SumShiftCellInteractionMode,
    onModeChange: (SumShiftCellInteractionMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.buttonCorner)

    Surface(
        modifier = modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.72f)
            .blockGamesSurfaceShadow(shape = shape, elevation = 6.dp),
        shape = shape,
        color = uiColors.metricCard.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.74f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SumShiftInteractionModeOption(
                icon = Icons.Filled.TouchApp,
                selected = selectedMode == SumShiftCellInteractionMode.Select,
                enabled = enabled,
                tone = CellTone.Cyan,
                onClick = { onModeChange(SumShiftCellInteractionMode.Select) },
                modifier = Modifier.weight(1f),
            )
            SumShiftInteractionModeOption(
                icon = Icons.Filled.Close,
                selected = selectedMode == SumShiftCellInteractionMode.Disable,
                enabled = enabled,
                tone = CellTone.Gold,
                onClick = { onModeChange(SumShiftCellInteractionMode.Disable) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SumShiftInteractionModeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    tone: CellTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val highlight = when (tone) {
        CellTone.Gold, CellTone.Amber -> uiColors.actionWarning
        CellTone.Emerald, CellTone.Lime -> uiColors.actionSuccess
        CellTone.Coral, CellTone.Rose -> uiColors.actionDanger
        else -> uiColors.actionPrimary
    }
    val shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                color = if (selected) highlight.copy(alpha = 0.92f) else Color.Transparent,
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                selected -> uiColors.actionIcon
                enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> uiColors.actionIconDisabled
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun SumShiftBoardCard(
    gameState: GameState,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = false,
    outgoingGameState: GameState? = null,
    outgoingClearProgress: Float = 1f,
    incomingRevealProgress: Float = 1f,
    disabledCells: Set<GridPoint> = emptySet(),
    manualDisabledCells: Set<GridPoint> = emptySet(),
    guidedCells: Set<GridPoint> = emptySet(),
    stylePulse: Float = 0f,
    wrongTapPoint: GridPoint? = null,
    onTapCell: (GridPoint) -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val boardPanelShape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .blockGamesSurfaceShadow(shape = boardPanelShape, elevation = 16.dp),
        shape = boardPanelShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.80f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            val columns = gameState.config.columns.coerceAtLeast(1)
            val rows = gameState.config.rows.coerceAtLeast(1)
            val laneGap = if (rows >= 7) 6.dp else 8.dp
            val gridGap = if (rows >= 7) 5.dp else 7.dp
            val widthBasedCell = (maxWidth - laneGap - (gridGap * (columns - 1))) / (columns + 1)
            val heightBasedCell = (maxHeight - laneGap - (gridGap * (rows - 1))) / (rows + 1)
            val cellSize = if (widthBasedCell < heightBasedCell) widthBasedCell else heightBasedCell
            val targetSize = cellSize
            val boardWidth = (cellSize * columns) + (gridGap * (columns - 1))
            val boardHeight = (cellSize * rows) + (gridGap * (rows - 1))

            Box(
                modifier = Modifier
                    .width(targetSize + laneGap + boardWidth)
                    .height(targetSize + laneGap + boardHeight),
                contentAlignment = Alignment.Center,
            ) {
                SumShiftPuzzleBoard(
                    gameState = gameState,
                    cellSize = cellSize,
                    targetSize = targetSize,
                    gridGap = gridGap,
                    laneGap = laneGap,
                    controlsEnabled = controlsEnabled,
                    disabledCells = disabledCells,
                    manualDisabledCells = manualDisabledCells,
                    guidedCells = guidedCells,
                    stylePulse = stylePulse,
                    wrongTapPoint = wrongTapPoint,
                    topTargetAlpha = sumShiftRevealAlphaFromBottom(0, rows + 1, incomingRevealProgress),
                    rowAlphaProvider = { rowIndex ->
                        sumShiftRevealAlphaFromBottom(rowIndex + 1, rows + 1, incomingRevealProgress)
                    },
                    onTapCell = onTapCell,
                )

                outgoingGameState?.let { previousGameState ->
                    SumShiftPuzzleBoard(
                        gameState = previousGameState,
                        cellSize = cellSize,
                        targetSize = targetSize,
                        gridGap = gridGap,
                        laneGap = laneGap,
                        controlsEnabled = false,
                        disabledCells = emptySet(),
                        manualDisabledCells = emptySet(),
                        guidedCells = emptySet(),
                        stylePulse = 0f,
                        wrongTapPoint = null,
                        topTargetAlpha = sumShiftClearAlpha(0, rows + 1, outgoingClearProgress),
                        rowAlphaProvider = { rowIndex ->
                            sumShiftClearAlpha(rowIndex + 1, rows + 1, outgoingClearProgress)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SumShiftPuzzleBoard(
    gameState: GameState,
    cellSize: Dp,
    targetSize: Dp,
    gridGap: Dp,
    laneGap: Dp,
    controlsEnabled: Boolean,
    disabledCells: Set<GridPoint>,
    manualDisabledCells: Set<GridPoint>,
    guidedCells: Set<GridPoint>,
    stylePulse: Float,
    wrongTapPoint: GridPoint?,
    topTargetAlpha: Float = 1f,
    rowAlphaProvider: (Int) -> Float = { 1f },
    modifier: Modifier = Modifier,
    onTapCell: (GridPoint) -> Unit = {},
) {
    val columns = gameState.config.columns.coerceAtLeast(1)
    val rows = gameState.config.rows.coerceAtLeast(1)
    val boardWidth = (cellSize * columns) + (gridGap * (columns - 1))

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(laneGap),
    ) {
        Row(
            modifier = Modifier
                .width(boardWidth)
                .alpha(topTargetAlpha),
            horizontalArrangement = Arrangement.spacedBy(gridGap),
        ) {
            repeat(columns) { columnIndex ->
                SumShiftTargetCell(
                    value = gameState.sumShiftColumnTargets.getOrElse(columnIndex) { 0 },
                    currentValue = gameState.selectableSumShiftColumnSum(
                        columnIndex = columnIndex,
                        disabledCells = disabledCells,
                    ),
                    completed = gameState.isSumShiftColumnCompleted(columnIndex),
                    size = targetSize,
                    stylePulse = stylePulse,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(laneGap)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(gridGap),
            ) {
                repeat(rows) { rowIndex ->
                    val rowAlpha = rowAlphaProvider(rowIndex)
                    SumShiftTargetCell(
                        value = gameState.sumShiftRowTargets.getOrElse(rowIndex) { 0 },
                        currentValue = gameState.selectableSumShiftRowSum(
                            rowIndex = rowIndex,
                            disabledCells = disabledCells,
                        ),
                        completed = gameState.isSumShiftRowCompleted(rowIndex),
                        size = targetSize,
                        stylePulse = stylePulse,
                        modifier = Modifier.alpha(rowAlpha),
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(gridGap),
            ) {
                repeat(rows) { rowIndex ->
                    val rowAlpha = rowAlphaProvider(rowIndex)
                    Row(
                        modifier = Modifier.alpha(rowAlpha),
                        horizontalArrangement = Arrangement.spacedBy(gridGap),
                    ) {
                        repeat(columns) { columnIndex ->
                            val point = GridPoint(column = columnIndex, row = rowIndex)
                            val boardCell = gameState.board.cellAt(columnIndex, rowIndex)
                            SumShiftNumberCell(
                                value = boardCell?.value ?: 0,
                                selected = point in gameState.sumShiftSelectedCells,
                                disabled = point in disabledCells,
                                systemDisabled = point in disabledCells && point !in manualDisabledCells,
                                manualDisabled = point in manualDisabledCells,
                                guided = point in guidedCells,
                                completed = gameState.isSumShiftRowCompleted(rowIndex) || gameState.isSumShiftColumnCompleted(columnIndex),
                                wrongTapped = point == wrongTapPoint,
                                size = cellSize,
                                stylePulse = stylePulse,
                                enabled = sumShiftCellIsEnabled(
                                    controlsEnabled = controlsEnabled,
                                    disabled = point in disabledCells,
                                    manualDisabled = point in manualDisabledCells,
                                ),
                                onClick = { onTapCell(point) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SumShiftOnboardingHintCard(
    scene: SumShiftOnboardingScene,
    currentStep: Int,
    totalSteps: Int,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
        color = uiColors.metricCard.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.70f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Adım $currentStep / $totalSteps",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(scene.hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = uiColors.subtitle,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun SumShiftTargetCell(
    value: Int,
    currentValue: Int,
    completed: Boolean,
    size: Dp,
    stylePulse: Float,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val uiColors = BlockGamesThemeTokens.uiColors
    val accent = if (completed) uiColors.success else MaterialTheme.colorScheme.primary
    val blockStyle = settings.blockVisualStyle
    val shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
    Surface(
        modifier = modifier
            .size(size)
            .blockGamesSurfaceShadow(shape = shape, elevation = if (completed) 6.dp else 3.dp),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = if (completed) 1.6.dp else 1.dp,
            color = if (completed) accent.copy(alpha = 0.80f) else uiColors.panelStroke.copy(alpha = 0.74f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BlockCellPreview(
                baseColor = if (completed) {
                    accent.copy(alpha = 0.20f)
                } else {
                    uiColors.metricCard.copy(alpha = 0.96f)
                },
                style = blockStyle,
                size = size,
                alpha = 0.98f,
                pulse = stylePulse,
                modifier = Modifier.matchParentSize(),
            )
            Text(
                text = value.toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = when {
                        size <= 26.dp -> 11.sp
                        size <= 30.dp -> 13.sp
                        size <= 38.dp -> 16.sp
                        else -> 18.sp
                    },
                ),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = when {
                    size <= 26.dp -> 11.sp
                    size <= 30.dp -> 13.sp
                    size <= 38.dp -> 16.sp
                    else -> 18.sp
                },
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                text = currentValue.toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        size <= 26.dp -> 6.sp
                        size <= 30.dp -> 7.sp
                        size <= 38.dp -> 8.sp
                        else -> 9.sp
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                lineHeight = when {
                    size <= 26.dp -> 6.sp
                    size <= 30.dp -> 7.sp
                    size <= 38.dp -> 8.sp
                    else -> 9.sp
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (size <= 30.dp) 1.dp else 2.dp),
            )
        }
    }
}

@Composable
private fun SumShiftNumberCell(
    value: Int,
    selected: Boolean,
    disabled: Boolean,
    systemDisabled: Boolean,
    manualDisabled: Boolean,
    guided: Boolean,
    completed: Boolean,
    wrongTapped: Boolean,
    size: Dp,
    stylePulse: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val settings = LocalAppSettings.current
    val uiColors = BlockGamesThemeTokens.uiColors
    val colorScheme = MaterialTheme.colorScheme
    val blockStyle = settings.blockVisualStyle
    val shape = RoundedCornerShape(boardCellCornerRadiusDp(size, blockStyle))
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseBlockColor = when {
        wrongTapped -> uiColors.danger.copy(alpha = 0.92f)
        disabled -> lerp(uiColors.panelMuted, Color.Black, 0.40f).copy(alpha = 0.98f)
        guided -> colorScheme.primary.copy(alpha = 0.14f)
        selected && completed -> colorScheme.primary.copy(alpha = 0.22f)
        selected -> colorScheme.primaryContainer
        else -> uiColors.boardEmptyCell
    }
    val overlayColor = if (pressed && !wrongTapped) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Transparent
    }
    val borderColor = when {
        wrongTapped -> uiColors.danger.copy(alpha = 0.96f)
        disabled -> lerp(uiColors.panelStroke, Color.Black, 0.26f).copy(alpha = 0.92f)
        guided -> colorScheme.primary.copy(alpha = 0.88f)
        selected && completed -> colorScheme.primary
        selected -> colorScheme.primary.copy(alpha = 0.86f)
        else -> uiColors.boardEmptyCellBorder
    }
    val textColor = when {
        wrongTapped -> Color.White
        disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        selected -> colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val emphasized = selected || guided || wrongTapped
    val visuallyActive = emphasized || pressed
    val blockAlpha = when {
        wrongTapped -> 1f
        else -> 0.98f
    }
    val blockPulse = when {
        wrongTapped -> stylePulse
        selected || guided || completed -> (stylePulse * 0.95f).coerceAtLeast(0.12f)
        else -> stylePulse * 0.55f
    }
    val animatedContainerColor = animateColorAsState(
        targetValue = overlayColor,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.92f),
        label = "sumShiftCellContainerColor",
    )
    val animatedBorderColor = animateColorAsState(
        targetValue = borderColor,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.92f),
        label = "sumShiftCellBorderColor",
    )
    val animatedTextColor = animateColorAsState(
        targetValue = textColor,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.92f),
        label = "sumShiftCellTextColor",
    )
    val animatedScale = animateFloatAsState(
        targetValue = when {
            wrongTapped -> 0.96f
            pressed -> 0.985f
            emphasized -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            stiffness = if (pressed || wrongTapped) 1_400f else 1_000f,
            dampingRatio = if (wrongTapped) 0.72f else 0.82f,
        ),
        label = "sumShiftCellScale",
    )
    val guidedBadgeAlpha = animateFloatAsState(
        targetValue = if (guided && !selected) 1f else 0f,
        animationSpec = spring(stiffness = 1_100f, dampingRatio = 0.88f),
        label = "sumShiftGuidedBadgeAlpha",
    )
    val guidedBadgeScale = animateFloatAsState(
        targetValue = if (guided && !selected) 1f else 0.82f,
        animationSpec = spring(stiffness = 1_100f, dampingRatio = 0.88f),
        label = "sumShiftGuidedBadgeScale",
    )
    val borderWidth = if (visuallyActive) 1.6.dp else 1.dp
    val shadowElevation = if (visuallyActive) 5.dp else 0.dp

    Surface(
        modifier = Modifier
            .size(size)
            .blockGamesSurfaceShadow(shape = shape, elevation = shadowElevation)
            .scale(animatedScale.value)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(borderWidth, animatedBorderColor.value),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BlockCellPreview(
                baseColor = baseBlockColor,
                style = blockStyle,
                size = size,
                alpha = blockAlpha,
                pulse = blockPulse,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(animatedContainerColor.value),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .alpha(guidedBadgeAlpha.value)
                    .scale(guidedBadgeScale.value),
                shape = RoundedCornerShape(999.dp),
                color = colorScheme.primary.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(3.dp)
                        .size(if (size <= 38.dp) 12.dp else 14.dp),
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (size <= 38.dp) 18.sp else 26.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = if (disabled) 0.42f else 0.72f),
                        offset = Offset(1.5f, 2f),
                        blurRadius = 4f,
                    ),
                ),
                color = animatedTextColor.value,
                textDecoration = if (manualDisabled) TextDecoration.LineThrough else null,
            )
        }
    }
}


private fun sumShiftClearAlpha(rowIndex: Int, rowCount: Int, progress: Float): Float {
    if (progress <= 0f) return 1f
    if (progress >= 1f) return 0f
    val revealIndex = progress * rowCount
    return (1f - (revealIndex - rowIndex).coerceIn(0f, 1f)).coerceIn(0f, 1f)
}

private fun sumShiftRevealAlpha(rowIndex: Int, rowCount: Int, progress: Float): Float {
    if (progress <= 0f) return 0f
    if (progress >= 1f) return 1f
    val revealIndex = progress * rowCount
    return (revealIndex - rowIndex).coerceIn(0f, 1f)
}

private fun sumShiftRevealAlphaFromBottom(rowIndex: Int, rowCount: Int, progress: Float): Float =
    sumShiftRevealAlpha(
        rowIndex = (rowCount - 1 - rowIndex).coerceAtLeast(0),
        rowCount = rowCount,
        progress = progress,
    )

internal fun GameState.systemDisabledSumShiftCells(
    selectedCells: Set<GridPoint> = sumShiftSelectedCells,
): Set<GridPoint> = buildSet {
    sumShiftRowTargets.indices.forEach { rowIndex ->
        if (sumShiftRowTargets.getOrNull(rowIndex) != selectedSumShiftRowSum(rowIndex, selectedCells)) return@forEach
        repeat(config.columns) { columnIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point !in selectedCells) {
                add(point)
            }
        }
    }
    sumShiftColumnTargets.indices.forEach { columnIndex ->
        if (sumShiftColumnTargets.getOrNull(columnIndex) != selectedSumShiftColumnSum(columnIndex, selectedCells)) return@forEach
        repeat(config.rows) { rowIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point !in selectedCells) {
                add(point)
            }
        }
    }
}

internal fun sumShiftCellIsEnabled(
    controlsEnabled: Boolean,
    disabled: Boolean,
    manualDisabled: Boolean,
): Boolean = controlsEnabled && (!disabled || manualDisabled)

internal fun SumShiftOnboardingScene.allowsGuidedTap(
    point: GridPoint,
    currentSelection: Set<GridPoint>,
): Boolean {
    val remainingRequired = remainingRequiredSelection(currentSelection)
    return remainingRequired.isEmpty() || point in remainingRequired
}

internal fun GameState.isSumShiftPlayableWith(
    selectedCells: Set<GridPoint>,
    manualDisabledCells: Set<GridPoint>,
): Boolean {
    val rowPlayable = sumShiftRowTargets.indices.all { rowIndex ->
        val selectedSum = (0 until config.columns).sumOf { columnIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point in selectedCells) board.cellAt(columnIndex, rowIndex)?.value ?: 0 else 0
        }
        val enabledSum = (0 until config.columns).sumOf { columnIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point !in manualDisabledCells) board.cellAt(columnIndex, rowIndex)?.value ?: 0 else 0
        }
        selectedSum <= sumShiftRowTargets[rowIndex] && enabledSum >= sumShiftRowTargets[rowIndex]
    }
    val columnPlayable = sumShiftColumnTargets.indices.all { columnIndex ->
        val selectedSum = (0 until config.rows).sumOf { rowIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point in selectedCells) board.cellAt(columnIndex, rowIndex)?.value ?: 0 else 0
        }
        val enabledSum = (0 until config.rows).sumOf { rowIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point !in manualDisabledCells) board.cellAt(columnIndex, rowIndex)?.value ?: 0 else 0
        }
        selectedSum <= sumShiftColumnTargets[columnIndex] && enabledSum >= sumShiftColumnTargets[columnIndex]
    }
    return rowPlayable && columnPlayable
}

internal fun GameState.autoSelectableSumShiftCells(): Set<GridPoint> = buildSet {
    sumShiftRowTargets.indices.forEach { rowIndex ->
        val enabledPoints = (0 until config.columns).map { columnIndex -> GridPoint(columnIndex, rowIndex) }
            .filter { it !in sumShiftManualDisabledCells }
        val enabledSum = enabledPoints.sumOf { point -> board.cellAt(point.column, point.row)?.value ?: 0 }
        if (enabledSum == sumShiftRowTargets[rowIndex]) {
            addAll(enabledPoints)
        }
    }
    sumShiftColumnTargets.indices.forEach { columnIndex ->
        val enabledPoints = (0 until config.rows).map { rowIndex -> GridPoint(columnIndex, rowIndex) }
            .filter { it !in sumShiftManualDisabledCells }
        val enabledSum = enabledPoints.sumOf { point -> board.cellAt(point.column, point.row)?.value ?: 0 }
        if (enabledSum == sumShiftColumnTargets[columnIndex]) {
            addAll(enabledPoints)
        }
    }
}

private fun Set<GridPoint>.toggle(point: GridPoint): Set<GridPoint> =
    if (point in this) this - point else this + point

internal fun GameState.selectedSumShiftRowSum(rowIndex: Int): Int =
    selectedSumShiftRowSum(rowIndex = rowIndex, selectedCells = sumShiftSelectedCells)

internal fun GameState.selectedSumShiftRowSum(
    rowIndex: Int,
    selectedCells: Set<GridPoint>,
): Int =
    (0 until config.columns).sumOf { column ->
        val point = GridPoint(column, rowIndex)
        if (point in selectedCells) board.cellAt(column, rowIndex)?.value ?: 0 else 0
    }

internal fun GameState.selectedSumShiftColumnSum(columnIndex: Int): Int =
    selectedSumShiftColumnSum(columnIndex = columnIndex, selectedCells = sumShiftSelectedCells)

internal fun GameState.selectedSumShiftColumnSum(
    columnIndex: Int,
    selectedCells: Set<GridPoint>,
): Int =
    (0 until config.rows).sumOf { row ->
        val point = GridPoint(columnIndex, row)
        if (point in selectedCells) board.cellAt(columnIndex, row)?.value ?: 0 else 0
    }

internal fun GameState.selectableSumShiftRowSum(
    rowIndex: Int,
    disabledCells: Set<GridPoint>,
): Int =
    (0 until config.columns).sumOf { column ->
        val point = GridPoint(column, rowIndex)
        if (point !in disabledCells) board.cellAt(column, rowIndex)?.value ?: 0 else 0
    }

internal fun GameState.selectableSumShiftColumnSum(
    columnIndex: Int,
    disabledCells: Set<GridPoint>,
): Int =
    (0 until config.rows).sumOf { row ->
        val point = GridPoint(columnIndex, row)
        if (point !in disabledCells) board.cellAt(columnIndex, row)?.value ?: 0 else 0
    }

internal fun GameState.isSumShiftRowCompleted(rowIndex: Int): Boolean =
    sumShiftRowTargets.getOrNull(rowIndex) != null && selectedSumShiftRowSum(rowIndex) == sumShiftRowTargets[rowIndex]

internal fun GameState.isSumShiftColumnCompleted(columnIndex: Int): Boolean =
    sumShiftColumnTargets.getOrNull(columnIndex) != null && selectedSumShiftColumnSum(columnIndex) == sumShiftColumnTargets[columnIndex]

internal fun GameState.completedSumShiftRows(): Int =
    sumShiftRowTargets.indices.count(::isSumShiftRowCompleted)

internal fun GameState.completedSumShiftColumns(): Int =
    sumShiftColumnTargets.indices.count(::isSumShiftColumnCompleted)

internal fun GameState.isSumShiftSolvedBoard(): Boolean =
    sumShiftRowTargets.isNotEmpty() &&
        sumShiftColumnTargets.isNotEmpty() &&
        completedSumShiftRows() == sumShiftRowTargets.size &&
        completedSumShiftColumns() == sumShiftColumnTargets.size

internal fun GameState.findSumShiftHintPoint(): GridPoint? {
    if (gameplayStyle != GameplayStyle.SumShift || status != GameStatus.Running || isSumShiftSolvedBoard()) {
        return null
    }

    val currentDisabledCells = sumShiftManualDisabledCells + systemDisabledSumShiftCells()

    return solveSumShiftHintPoint(
        lockedSelectedCells = sumShiftSelectedCells,
        manualDisabledCells = sumShiftManualDisabledCells,
    ) ?: solveSumShiftHintPoint(
        lockedSelectedCells = emptySet(),
        manualDisabledCells = sumShiftManualDisabledCells,
    )?.takeIf { it !in currentDisabledCells && it !in sumShiftSelectedCells }
}

private fun GameState.solveSumShiftHintPoint(
    lockedSelectedCells: Set<GridPoint>,
    manualDisabledCells: Set<GridPoint>,
): GridPoint? {
    val disabledCells = manualDisabledCells + systemDisabledSumShiftCells(selectedCells = lockedSelectedCells)
    val fixedSelectedCells = lockedSelectedCells.filterTo(linkedSetOf()) { it !in manualDisabledCells }
    val rowSelected = IntArray(config.rows) { rowIndex -> selectedSumShiftRowSum(rowIndex, fixedSelectedCells) }
    val columnSelected = IntArray(config.columns) { columnIndex -> selectedSumShiftColumnSum(columnIndex, fixedSelectedCells) }
    val rowRemaining = IntArray(config.rows)
    val columnRemaining = IntArray(config.columns)
    val candidates = buildList {
        repeat(config.rows) { rowIndex ->
            repeat(config.columns) { columnIndex ->
                val point = GridPoint(columnIndex, rowIndex)
                if (point in fixedSelectedCells || point in disabledCells) return@repeat
                val value = board.cellAt(columnIndex, rowIndex)?.value ?: 0
                rowRemaining[rowIndex] += value
                columnRemaining[columnIndex] += value
                add(point)
            }
        }
    }.sortedBy { point ->
        val rowSlack = (sumShiftRowTargets[point.row] - rowSelected[point.row]).coerceAtLeast(0)
        val columnSlack = (sumShiftColumnTargets[point.column] - columnSelected[point.column]).coerceAtLeast(0)
        minOf(rowSlack, columnSlack)
    }

    if (!sumShiftConstraintsRemainFeasible(rowSelected, rowRemaining, sumShiftRowTargets) ||
        !sumShiftConstraintsRemainFeasible(columnSelected, columnRemaining, sumShiftColumnTargets)
    ) {
        return null
    }

    val solution = fixedSelectedCells.toMutableSet()

    fun search(index: Int): Boolean {
        if (index >= candidates.size) {
            return rowSelected.indices.all { rowSelected[it] == sumShiftRowTargets[it] } &&
                columnSelected.indices.all { columnSelected[it] == sumShiftColumnTargets[it] }
        }

        val point = candidates[index]
        val value = board.cellAt(point.column, point.row)?.value ?: 0
        rowRemaining[point.row] -= value
        columnRemaining[point.column] -= value

        val canInclude =
            rowSelected[point.row] + value <= sumShiftRowTargets[point.row] &&
                columnSelected[point.column] + value <= sumShiftColumnTargets[point.column]

        if (canInclude) {
            rowSelected[point.row] += value
            columnSelected[point.column] += value
            if (sumShiftConstraintIsFeasible(point.row, rowSelected, rowRemaining, sumShiftRowTargets) &&
                sumShiftConstraintIsFeasible(point.column, columnSelected, columnRemaining, sumShiftColumnTargets)
            ) {
                solution += point
                if (search(index + 1)) {
                    return true
                }
                solution -= point
            }
            rowSelected[point.row] -= value
            columnSelected[point.column] -= value
        }

        if (sumShiftConstraintIsFeasible(point.row, rowSelected, rowRemaining, sumShiftRowTargets) &&
            sumShiftConstraintIsFeasible(point.column, columnSelected, columnRemaining, sumShiftColumnTargets) &&
            search(index + 1)
        ) {
            return true
        }

        rowRemaining[point.row] += value
        columnRemaining[point.column] += value
        return false
    }

    if (!search(0)) return null
    return solution.firstOrNull { it !in sumShiftSelectedCells && it !in disabledCells }
}

private fun sumShiftConstraintsRemainFeasible(
    selected: IntArray,
    remaining: IntArray,
    targets: List<Int>,
): Boolean = selected.indices.all { index ->
    sumShiftConstraintIsFeasible(index, selected, remaining, targets)
}

private fun sumShiftConstraintIsFeasible(
    index: Int,
    selected: IntArray,
    remaining: IntArray,
    targets: List<Int>,
): Boolean {
    val target = targets.getOrElse(index) { 0 }
    return selected[index] <= target && selected[index] + remaining[index] >= target
}

private fun previewSumShiftState(rows: Int = 6): GameState {
    val values = listOf(
        listOf(2, 4, 1, 5, 3),
        listOf(6, 1, 3, 2, 4),
        listOf(1, 7, 2, 4, 5),
        listOf(3, 2, 6, 1, 2),
        listOf(5, 4, 1, 3, 6),
        listOf(4, 3, 5, 2, 1),
        listOf(7, 2, 4, 1, 3),
        listOf(3, 6, 2, 5, 4),
    ).take(rows)
    var board = BoardMatrix.empty(columns = 5, rows = rows)
    values.forEachIndexed { row, rowValues ->
        rowValues.forEachIndexed { column, value ->
            board = board.fill(
                points = listOf(GridPoint(column, row)),
                tone = listOf(CellTone.Cyan, CellTone.Violet, CellTone.Emerald, CellTone.Gold, CellTone.Blue)[(column + row) % 5],
                value = value,
            )
        }
    }
    return GameState(
        config = GameConfig(columns = 5, rows = rows, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
        gameplayStyle = GameplayStyle.SumShift,
        board = board,
        activePiece = null,
        nextQueue = emptyList(),
        holdPiece = null,
        canHold = false,
        score = 860,
        linesCleared = 2,
        level = 1,
        difficultyStage = 0,
        secondsUntilDifficultyIncrease = 9_999,
        status = GameStatus.Running,
        sumShiftRowTargets = listOf(6, 6, 9, 8, 8, 7, 10, 9).take(rows),
        sumShiftColumnTargets = listOf(7, 11, 8, 5, 6),
        sumShiftSelectedCells = setOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(3, 1),
            GridPoint(4, 1),
            GridPoint(1, 2),
            GridPoint(2, 2),
            GridPoint(2, 3),
            GridPoint(0, 4),
            GridPoint(3, 4),
            GridPoint(1, 5),
            GridPoint(4, 6),
        ).filter { it.row < rows }.toSet(),
    )
}

@Preview(name = "SumShift Game", widthDp = 412, heightDp = 915)
@Composable
private fun SumShiftGameScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        SumShiftGameScreen(
            gameState = previewSumShiftState(rows = 6),
            onTapCell = {},
            onRestart = {},
            onBack = {},
            highestScore = 1240,
        )
    }
}

@Preview(name = "SumShift Game 5x8", widthDp = 412, heightDp = 915)
@Composable
private fun SumShiftGameScreenTallPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        SumShiftGameScreen(
            gameState = previewSumShiftState(rows = 8),
            onTapCell = {},
            onRestart = {},
            onBack = {},
            highestScore = 1240,
            interactiveOnboardingScene = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStage.MatchRow),
            interactiveOnboardingCurrentStep = 1,
            interactiveOnboardingTotalSteps = 3,
        )
    }
}

