package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.digitshift_delete
import blockgames.composeapp.generated.resources.digitshift_enter
import blockgames.composeapp.generated.resources.digitshift_guess_label
import blockgames.composeapp.generated.resources.game_message_digitshift_enter_word
import blockgames.composeapp.generated.resources.interactive_onboarding_digitshift_solve_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_digitshift_submit_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_digitshift_type_hint
import blockgames.composeapp.generated.resources.restart_cancel
import blockgames.composeapp.generated.resources.restart_confirm
import blockgames.composeapp.generated.resources.restart_confirm_body
import blockgames.composeapp.generated.resources.restart_confirm_title
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.logic.DigitShiftLexicon
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.DigitShiftGuess
import com.ugurbuga.blockgames.game.model.DigitShiftLetterState
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.DigitShiftOnboardingScene
import com.ugurbuga.blockgames.settings.DigitShiftOnboardingStage
import com.ugurbuga.blockgames.settings.DigitShiftOnboardingStateFactory
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.RestartConfirmDialog
import com.ugurbuga.blockgames.ui.game.blockForegroundTint
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.resolveGameText
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val DigitShiftRevealStepDelayMillis = 210L
private const val DigitShiftSolvedPauseMillis = 420L
private const val DigitShiftFlipAnimationMillis = 380
private const val DigitShiftKeyboardKeyHeightDp = 48
private const val DigitShiftKeyboardGapDp = 4
private const val DigitShiftKeyboardControlKeyWeight = 1f
private const val DigitShiftKeyboardDeleteKeyWeight = 1f

private data class DigitShiftFeedbackPalette(
    val container: Color,
    val border: Color,
    val content: Color,
)

@Composable
internal fun DigitShiftGameScreen(
    modifier: Modifier = Modifier,
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    adController: GameAdController = NoOpGameAdController,
    interactiveOnboardingScene: DigitShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onAppendToken: (String) -> Unit,
    onDeleteToken: () -> Unit,
    onSubmitGuess: () -> Unit,
    onAdvanceRound: () -> Unit,
    onRestart: () -> Unit,
    onRewardedRevive: () -> Unit,
    onBack: () -> Unit,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val stylePulse = LocalBlockStylePulse.current
    val pack = remember(gameState.digitShiftLocaleTag) { DigitShiftLexicon.packFor(gameState.digitShiftLocaleTag) }
    val latestGuess = gameState.digitShiftGuesses.lastOrNull()
    val latestGuessFingerprint = remember(latestGuess) { latestGuess?.fingerprint() }
    val solutionFingerprint = remember(gameState.digitShiftSolution) { DigitShiftLexicon.keyOf(gameState.digitShiftSolution) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var revealAnimationInitialized by remember { mutableStateOf(false) }
    var lastSolutionFingerprint by remember { mutableStateOf(solutionFingerprint) }
    var lastAnimatedGuessFingerprint by remember { mutableStateOf<String?>(latestGuessFingerprint) }
    var animatingGuessIndex by remember { mutableStateOf<Int?>(null) }
    var revealedCellCount by remember { mutableStateOf(Int.MAX_VALUE) }
    var displayedKeyboardHints by remember { mutableStateOf(gameState.digitShiftKeyboardHints) }

    LaunchedEffect(solutionFingerprint, latestGuessFingerprint, gameState.digitShiftGuesses.size, gameState.digitShiftKeyboardHints, gameState.digitShiftAwaitingNextRound) {
        if (!revealAnimationInitialized) {
            revealAnimationInitialized = true
            lastSolutionFingerprint = solutionFingerprint
            lastAnimatedGuessFingerprint = latestGuessFingerprint
            displayedKeyboardHints = gameState.digitShiftKeyboardHints
            animatingGuessIndex = null
            revealedCellCount = Int.MAX_VALUE
            return@LaunchedEffect
        }

        if (solutionFingerprint != lastSolutionFingerprint || gameState.digitShiftGuesses.isEmpty()) {
            lastSolutionFingerprint = solutionFingerprint
            lastAnimatedGuessFingerprint = latestGuessFingerprint
            displayedKeyboardHints = gameState.digitShiftKeyboardHints
            animatingGuessIndex = null
            revealedCellCount = Int.MAX_VALUE
            return@LaunchedEffect
        }

        if (latestGuess == null || latestGuessFingerprint == null || latestGuessFingerprint == lastAnimatedGuessFingerprint) {
            displayedKeyboardHints = gameState.digitShiftKeyboardHints
            animatingGuessIndex = null
            revealedCellCount = Int.MAX_VALUE
            return@LaunchedEffect
        }

        lastAnimatedGuessFingerprint = latestGuessFingerprint
        animatingGuessIndex = gameState.digitShiftGuesses.lastIndex
        revealedCellCount = 0
        displayedKeyboardHints = buildKeyboardHints(gameState.digitShiftGuesses.dropLast(1))

        latestGuess.tokens.zip(latestGuess.states).forEachIndexed { index, (token, state) ->
            delay(DigitShiftRevealStepDelayMillis)
            revealedCellCount = index + 1
            displayedKeyboardHints = mergeKeyboardHint(displayedKeyboardHints, token, state)
        }

        animatingGuessIndex = null
        revealedCellCount = Int.MAX_VALUE
        displayedKeyboardHints = gameState.digitShiftKeyboardHints

        if (gameState.digitShiftAwaitingNextRound && latestGuess.states.all { it == DigitShiftLetterState.Correct }) {
            delay(DigitShiftSolvedPauseMillis)
            onAdvanceRound()
        }
    }

    val inputEnabled = gameState.status == GameStatus.Running && !gameState.digitShiftAwaitingNextRound && animatingGuessIndex == null

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
            showExtraLifeButton = adController !== NoOpGameAdController && gameState.activeChallenge?.isCompleted != true,
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
            .background(uiColors.panel.copy(alpha = 0.18f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MinimalTopBar(
            gameState = gameState,
            scoreHighlightStrengthProvider = { 0f },
            scoreHighlightScaleProvider = { 1f },
            remainingTimeLabel = stringResource(Res.string.time_remaining),
            onBack = onBack,
            onRestart = { showRestartDialog = true },
            stylePulse = 0f,
        )

        interactiveOnboardingScene?.let { scene ->
            DigitShiftOnboardingHintCard(
                scene = scene,
                currentStep = interactiveOnboardingCurrentStep,
                totalSteps = interactiveOnboardingTotalSteps,
            )
        }

        if (gameState.message.key != GameTextKey.GameMessageDigitShiftSolved) {
            DigitShiftMessageCard(
                message = resolveGameText(gameState.message),
            )
        }

        DigitShiftBoard(
            gameState = gameState,
            animatedGuessIndex = animatingGuessIndex,
            revealedCellCount = revealedCellCount,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            stylePulse = stylePulse,
        )

        DigitShiftKeyboard(
            keyboardRows = pack.keyboardRows(gameState.config.columns),
            keyboardHints = displayedKeyboardHints,
            enabled = inputEnabled,
            onAppendToken = onAppendToken,
            onDeleteToken = onDeleteToken,
            onSubmitGuess = onSubmitGuess,
        )
    }
}

@Composable
internal fun DigitShiftBoard(
    gameState: GameState,
    animatedGuessIndex: Int? = null,
    revealedCellCount: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    stylePulse: Float = 0f,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = BlockGamesThemeTokens.uiColors.gameSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, BlockGamesThemeTokens.uiColors.panelStroke.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            val spacing = if (maxWidth < 360.dp || maxHeight < 420.dp) 6.dp else 8.dp
            val widthBasedCellSize =
                (maxWidth - (spacing * (gameState.config.columns - 1))) / gameState.config.columns
            val heightBasedCellSize =
                (maxHeight - (spacing * (gameState.config.rows - 1))) / gameState.config.rows
            val cellSize = if (widthBasedCellSize < heightBasedCellSize) widthBasedCellSize else heightBasedCellSize
            val density = LocalDensity.current
            val boardWidth = with(density) {
                ((cellSize.toPx() * gameState.config.columns) + (spacing.toPx() * (gameState.config.columns - 1))).toDp()
            }
            val boardHeight = with(density) {
                ((cellSize.toPx() * gameState.config.rows) + (spacing.toPx() * (gameState.config.rows - 1))).toDp()
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredWidth(boardWidth)
                    .requiredHeight(boardHeight),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(gameState.config.rows) { rowIndex ->
                    val rowGuess = gameState.digitShiftGuesses.getOrNull(rowIndex)
                    val currentRow = if (rowIndex == gameState.digitShiftGuesses.size && gameState.status == GameStatus.Running) {
                        gameState.digitShiftCurrentGuess
                    } else {
                        emptyList()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        repeat(gameState.config.columns) { columnIndex ->
                            val state = rowGuess?.states?.getOrNull(columnIndex)?.takeIf {
                                rowIndex != animatedGuessIndex || columnIndex < revealedCellCount
                            }
                            DigitShiftCell(
                                token = rowGuess?.tokens?.getOrNull(columnIndex) ?: currentRow.getOrNull(columnIndex).orEmpty(),
                                state = state,
                                size = cellSize,
                                modifier = Modifier.size(cellSize),
                                pulse = stylePulse,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitShiftCell(
    token: String,
    state: DigitShiftLetterState?,
    size: Dp,
    modifier: Modifier = Modifier,
    pulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val palette = digitShiftPaletteFor(state)
    val animatedBackground by animateColorAsState(
        targetValue = palette.container,
        animationSpec = tween(durationMillis = DigitShiftFlipAnimationMillis),
        label = "digitShiftCellBackground",
    )
    val animatedBorder by animateColorAsState(
        targetValue = palette.border,
        animationSpec = tween(durationMillis = DigitShiftFlipAnimationMillis),
        label = "digitShiftCellBorder",
    )
    var flipped by remember(token, state) { mutableStateOf(state == null || token.isBlank()) }
    LaunchedEffect(token, state) {
        if (token.isBlank() || state == null) {
            flipped = true
            return@LaunchedEffect
        }
        flipped = false
        delay(18L)
        flipped = true
    }
    val flipProgress by animateFloatAsState(
        targetValue = if (flipped) 1f else 0f,
        animationSpec = tween(durationMillis = DigitShiftFlipAnimationMillis),
        label = "digitShiftCellFlip",
    )
    val density = LocalDensity.current
    val isDark = isBlockGamesDarkTheme(settings)
    val tintColor = blockForegroundTint(
        style = settings.blockVisualStyle,
        isDarkTheme = isDark,
        palette = settings.blockColorPalette,
    ).copy(alpha = if (state == null || state == DigitShiftLetterState.Unknown) 0.88f else 1f)

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationX = -180f * (1f - flipProgress)
                scaleX = 0.88f + (flipProgress * 0.12f)
                scaleY = 0.72f + (flipProgress * 0.28f)
                alpha = 0.76f + (flipProgress * 0.24f)
                transformOrigin = TransformOrigin(0.5f, 1f)
                cameraDistance = with(density) { 12.dp.toPx() }
            },
        shape = RoundedCornerShape(boardCellCornerRadiusDp(size, settings.blockVisualStyle)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, animatedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BlockCellPreview(
                baseColor = animatedBackground,
                style = settings.blockVisualStyle,
                size = size,
                modifier = Modifier.matchParentSize(),
                alpha = 1f,
                pulse = pulse,
            )
            val fontSize = when {
                size <= 34.dp -> 12.sp
                size <= 44.dp -> 16.sp
                token.length > 1 -> 14.sp
                else -> 24.sp
            }
            Text(
                text = token,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSize),
                fontWeight = FontWeight.Black,
                color = tintColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}


@Composable
internal fun DigitShiftKeyboard(
    keyboardRows: List<List<String>>,
    keyboardHints: Map<String, DigitShiftLetterState>,
    enabled: Boolean,
    onAppendToken: (String) -> Unit,
    onDeleteToken: () -> Unit,
    onSubmitGuess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedRows = remember(keyboardRows) {
        when {
            keyboardRows.size >= 2 -> keyboardRows.take(2)
            keyboardRows.isEmpty() -> listOf(emptyList(), emptyList())
            else -> keyboardRows + List(2 - keyboardRows.size) { emptyList() }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DigitShiftKeyboardGapDp.dp),
    ) {
        DigitShiftKeyboardLetterRow(
            tokens = resolvedRows[0],
            keyboardHints = keyboardHints,
            enabled = enabled,
            onAppendToken = onAppendToken,
            trailingControl = {
                DigitShiftKeyboardButton(
                    label = stringResource(Res.string.digitshift_delete),
                    state = DigitShiftLetterState.Absent,
                    enabled = enabled,
                    onClick = onDeleteToken,
                    modifier = Modifier.weight(DigitShiftKeyboardDeleteKeyWeight),
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    showLabel = false,
                )
            },
        )
        DigitShiftKeyboardLetterRow(
            tokens = resolvedRows.getOrElse(1) { emptyList() },
            keyboardHints = keyboardHints,
            enabled = enabled,
            onAppendToken = onAppendToken,
            trailingControl = {
                DigitShiftKeyboardButton(
                    label = stringResource(Res.string.digitshift_enter),
                    state = DigitShiftLetterState.Correct,
                    enabled = enabled,
                    onClick = onSubmitGuess,
                    modifier = Modifier.weight(DigitShiftKeyboardControlKeyWeight),
                    icon = Icons.Filled.Check,
                    showLabel = false,
                )
            },
        )
    }
}

@Composable
private fun DigitShiftKeyboardLetterRow(
    tokens: List<String>,
    keyboardHints: Map<String, DigitShiftLetterState>,
    enabled: Boolean,
    onAppendToken: (String) -> Unit,
    trailingControl: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DigitShiftKeyboardGapDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tokens.forEach { token ->
            DigitShiftKeyboardButton(
                label = token,
                state = keyboardHints[token] ?: DigitShiftLetterState.Unknown,
                enabled = enabled,
                onClick = { onAppendToken(token) },
                modifier = Modifier.weight(digitShiftKeyWeight(token)),
            )
        }
        trailingControl?.invoke(this)
    }
}

@Composable
private fun RowScope.DigitShiftKeyboardButton(
    label: String,
    state: DigitShiftLetterState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showLabel: Boolean = true,
) {
    val settings = LocalAppSettings.current
    val palette = digitShiftPaletteFor(
        state = state,
        enabled = enabled,
        isKeyboardKey = true,
    )
    val animatedBackground by animateColorAsState(
        palette.container,
        tween(DigitShiftFlipAnimationMillis),
        label = "digitShiftKeyBackground",
    )
    val animatedBorder by animateColorAsState(
        palette.border,
        tween(DigitShiftFlipAnimationMillis),
        label = "digitShiftKeyBorder",
    )
    val shape = RoundedCornerShape(boardCellCornerRadiusDp(DigitShiftKeyboardKeyHeightDp.dp, settings.blockVisualStyle))
    val isDark = isBlockGamesDarkTheme(settings)
    val tintColor = blockForegroundTint(
        style = settings.blockVisualStyle,
        isDarkTheme = isDark,
        palette = settings.blockColorPalette,
    ).copy(alpha = if (state == DigitShiftLetterState.Unknown) 0.92f else 1f)

    Card(
        modifier = modifier
            .height(DigitShiftKeyboardKeyHeightDp.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.72f },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, animatedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BlockCellPreview(
                baseColor = animatedBackground,
                style = settings.blockVisualStyle,
                size = DigitShiftKeyboardKeyHeightDp.dp,
                modifier = Modifier.matchParentSize(),
                alpha = 1f,
            )
            val compactTextStyle = when {
                maxWidth < 18.dp -> MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                maxWidth < 22.dp -> MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                maxWidth < 28.dp || label.length >= 4 -> MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                maxWidth < 34.dp || label.length >= 2 -> MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                else -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = if (showLabel) 2.dp else 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null && !showLabel) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = label,
                        color = tintColor,
                        style = compactTextStyle,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

private fun DigitShiftGuess.fingerprint(): String = buildString {
    append(DigitShiftLexicon.keyOf(tokens))
    append('#')
    append(states.joinToString(separator = ",") { it.ordinal.toString() })
}

private fun buildKeyboardHints(guesses: List<DigitShiftGuess>): Map<String, DigitShiftLetterState> =
    guesses.fold(emptyMap(), ::mergeKeyboardHints)

private fun mergeKeyboardHints(
    existing: Map<String, DigitShiftLetterState>,
    guess: DigitShiftGuess,
): Map<String, DigitShiftLetterState> {
    var updated = existing
    guess.tokens.zip(guess.states).forEach { (token, state) ->
        updated = mergeKeyboardHint(updated, token, state)
    }
    return updated
}

private fun mergeKeyboardHint(
    existing: Map<String, DigitShiftLetterState>,
    token: String,
    state: DigitShiftLetterState,
): Map<String, DigitShiftLetterState> {
    val previous = existing[token] ?: DigitShiftLetterState.Unknown
    if (state.ordinal < previous.ordinal) return existing
    return existing + (token to state)
}

@Composable
private fun digitShiftPaletteFor(
    state: DigitShiftLetterState?,
    enabled: Boolean = true,
    isKeyboardKey: Boolean = false,
): DigitShiftFeedbackPalette {
    val uiColors = BlockGamesThemeTokens.uiColors
    val colorScheme = MaterialTheme.colorScheme
    val base = when (state) {
        DigitShiftLetterState.Correct -> DigitShiftFeedbackPalette(
            container = lerp(uiColors.success, uiColors.actionSuccess, 0.28f).copy(alpha = if (isKeyboardKey) 0.98f else 0.94f),
            border = lerp(uiColors.success, Color.White, 0.18f),
            content = Color.White,
        )

        DigitShiftLetterState.Present -> DigitShiftFeedbackPalette(
            container = lerp(uiColors.warning, uiColors.actionWarning, 0.18f).copy(alpha = if (isKeyboardKey) 0.98f else 0.94f),
            border = lerp(uiColors.warning, Color.White, 0.14f),
            content = Color.White,
        )

        DigitShiftLetterState.Absent -> DigitShiftFeedbackPalette(
            container = Color(0xFF24262B).copy(alpha = if (isKeyboardKey) 0.99f else 0.95f),
            border = Color(0xFF0F1115),
            content = Color(0xFFF2F4F7),
        )

        DigitShiftLetterState.Unknown,
        null,
        -> DigitShiftFeedbackPalette(
            container = if (isKeyboardKey) {
                lerp(uiColors.metricCard, uiColors.panelHighlight, 0.10f).copy(alpha = 0.96f)
            } else {
                uiColors.metricCard.copy(alpha = 0.90f)
            },
            border = uiColors.panelStroke.copy(alpha = if (isKeyboardKey) 0.86f else 0.62f),
            content = if (isKeyboardKey) colorScheme.onSurface else uiColors.subtitle,
        )
    }
    return if (enabled) base else base.copy(
        container = lerp(base.container, uiColors.panelMuted, 0.42f),
        border = lerp(base.border, uiColors.panelStroke, 0.28f),
        content = base.content.copy(alpha = 0.70f),
    )
}

private fun digitShiftKeyWeight(token: String): Float = when {
    token.length >= 3 -> 1.35f
    token.length == 2 -> 1.15f
    else -> 1f
}


@Composable
private fun DigitShiftMessageCard(
    message: String,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.62f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = message.ifBlank { stringResource(Res.string.game_message_digitshift_enter_word) },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DigitShiftOnboardingHintCard(
    scene: DigitShiftOnboardingScene,
    currentStep: Int,
    totalSteps: Int,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelHighlight.copy(alpha = 0.24f)),
        border = BorderStroke(1.dp, uiColors.guideAccent.copy(alpha = 0.44f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = uiColors.guideAccent,
                )
                Text(
                    text = if (totalSteps > 0) stringResource(Res.string.digitshift_guess_label, currentStep.coerceAtLeast(1)) else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = uiColors.guideAccent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (totalSteps > 0) {
                    Text(
                        text = "$currentStep/$totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = uiColors.subtitle,
                    )
                }
            }
            Text(
                text = when (scene.stage) {
                    DigitShiftOnboardingStage.FirstGuess -> stringResource(Res.string.interactive_onboarding_digitshift_type_hint)
                    DigitShiftOnboardingStage.ReadHints -> stringResource(Res.string.interactive_onboarding_digitshift_submit_hint)
                    DigitShiftOnboardingStage.SolveWord -> stringResource(Res.string.interactive_onboarding_digitshift_solve_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = scene.suggestedGuess.joinToString(separator = ""),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = uiColors.guideAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun previewDigitShiftState(): GameState {
    val config = GameConfig(columns = 5, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999)
    return GameState(
        config = config,
        gameplayStyle = GameplayStyle.DigitShift,
        board = BoardMatrix.empty(config.columns, config.rows),
        activePiece = null,
        nextQueue = emptyList(),
        score = 860,
        linesCleared = 0,
        level = 1,
        difficultyStage = 0,
        secondsUntilDifficultyIncrease = 9_999,
        status = GameStatus.Running,
        digitShiftLocaleTag = "en",
        digitShiftSolution = listOf("1", "2", "3", "4", "5"),
        digitShiftGuesses = listOf(
            DigitShiftGuess(
                tokens = listOf("1", "0", "9", "8", "7"),
                states = listOf(
                    DigitShiftLetterState.Correct,
                    DigitShiftLetterState.Absent,
                    DigitShiftLetterState.Absent,
                    DigitShiftLetterState.Absent,
                    DigitShiftLetterState.Absent
                )
            ),
            DigitShiftGuess(
                tokens = listOf("1", "3", "2", "5", "4"),
                states = listOf(
                    DigitShiftLetterState.Correct,
                    DigitShiftLetterState.Present,
                    DigitShiftLetterState.Present,
                    DigitShiftLetterState.Present,
                    DigitShiftLetterState.Present
                )
            )
        ),
        digitShiftCurrentGuess = listOf("1", "2", "3"),
        digitShiftKeyboardHints = mapOf(
            "1" to DigitShiftLetterState.Correct,
            "2" to DigitShiftLetterState.Present,
            "3" to DigitShiftLetterState.Present,
            "4" to DigitShiftLetterState.Unknown,
            "5" to DigitShiftLetterState.Present,
            "0" to DigitShiftLetterState.Absent,
            "9" to DigitShiftLetterState.Absent,
            "8" to DigitShiftLetterState.Absent,
            "7" to DigitShiftLetterState.Absent,
        )
    )
}

@Preview(name = "DigitShift Game", widthDp = 412, heightDp = 915)
@Composable
private fun DigitShiftGameScreenPreview() {
    BlockGamesTheme(settings = AppSettings(themeMode = AppThemeMode.Light)) {
        DigitShiftGameScreen(
            gameState = previewDigitShiftState(),
            highestScore = 1240,
            showNewHighScoreMessage = false,
            onAppendToken = {},
            onDeleteToken = {},
            onSubmitGuess = {},
            onAdvanceRound = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
        )
    }
}

@Preview(name = "DigitShift Game Dark", widthDp = 412, heightDp = 915)
@Composable
private fun DigitShiftGameScreenDarkPreview() {
    BlockGamesTheme(settings = AppSettings(themeMode = AppThemeMode.Dark)) {
        DigitShiftGameScreen(
            gameState = previewDigitShiftState(),
            highestScore = 1240,
            showNewHighScoreMessage = false,
            onAppendToken = {},
            onDeleteToken = {},
            onSubmitGuess = {},
            onAdvanceRound = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
        )
    }
}

@Preview(name = "DigitShift Onboarding", widthDp = 412, heightDp = 915)
@Composable
private fun DigitShiftGameScreenOnboardingPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        DigitShiftGameScreen(
            gameState = previewDigitShiftState(),
            highestScore = 1240,
            showNewHighScoreMessage = false,
            interactiveOnboardingScene = DigitShiftOnboardingStateFactory.scene(DigitShiftOnboardingStage.ReadHints),
            interactiveOnboardingCurrentStep = 2,
            interactiveOnboardingTotalSteps = 3,
            onAppendToken = {},
            onDeleteToken = {},
            onSubmitGuess = {},
            onAdvanceRound = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
        )
    }
}

@Preview(name = "DigitShift GameOver", widthDp = 412, heightDp = 915)
@Composable
private fun DigitShiftGameOverPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        DigitShiftGameScreen(
            gameState = previewDigitShiftState().copy(status = GameStatus.GameOver),
            highestScore = 2000,
            showNewHighScoreMessage = true,
            onAppendToken = {},
            onDeleteToken = {},
            onSubmitGuess = {},
            onAdvanceRound = {},
            onRestart = {},
            onRewardedRevive = {},
            onBack = {},
        )
    }
}

