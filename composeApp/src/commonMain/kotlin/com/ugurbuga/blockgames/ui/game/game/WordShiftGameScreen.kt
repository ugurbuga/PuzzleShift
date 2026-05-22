package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.game_message_wordshift_enter_word
import blockgames.composeapp.generated.resources.interactive_onboarding_wordshift_solve_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_wordshift_submit_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_wordshift_type_hint
import blockgames.composeapp.generated.resources.restart_cancel
import blockgames.composeapp.generated.resources.restart_confirm
import blockgames.composeapp.generated.resources.restart_confirm_body
import blockgames.composeapp.generated.resources.restart_confirm_title
import blockgames.composeapp.generated.resources.time_remaining
import blockgames.composeapp.generated.resources.wordshift_delete
import blockgames.composeapp.generated.resources.wordshift_enter
import blockgames.composeapp.generated.resources.wordshift_guess_label
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.logic.WordShiftLexicon
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.WordShiftGuess
import com.ugurbuga.blockgames.game.model.WordShiftLetterState
import com.ugurbuga.blockgames.settings.WordShiftOnboardingScene
import com.ugurbuga.blockgames.settings.WordShiftOnboardingStage
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.RestartConfirmDialog
import com.ugurbuga.blockgames.ui.game.resolveGameText
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay

private const val WordShiftRevealStepDelayMillis = 170L
private const val WordShiftSolvedPauseMillis = 420L
private const val WordShiftFlipAnimationMillis = 260
private const val WordShiftKeyboardKeyHeightDp = 44
private const val WordShiftKeyboardGapDp = 4
private const val WordShiftKeyboardSecondRowIndent = 0.55f
private const val WordShiftKeyboardControlKeyWeight = 1.15f
private const val WordShiftKeyboardDeleteKeyWeight = 1.15f

private data class WordShiftFeedbackPalette(
    val container: Color,
    val border: Color,
    val content: Color,
)

@Composable
internal fun WordShiftGameScreen(
    modifier: Modifier = Modifier,
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    adController: GameAdController = NoOpGameAdController,
    interactiveOnboardingScene: WordShiftOnboardingScene? = null,
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
    val pack = remember(gameState.wordShiftLocaleTag) { WordShiftLexicon.packFor(gameState.wordShiftLocaleTag) }
    val latestGuess = gameState.wordShiftGuesses.lastOrNull()
    val latestGuessFingerprint = remember(latestGuess) { latestGuess?.fingerprint() }
    val solutionFingerprint = remember(gameState.wordShiftSolution) { WordShiftLexicon.keyOf(gameState.wordShiftSolution) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var revealAnimationInitialized by remember { mutableStateOf(false) }
    var lastSolutionFingerprint by remember { mutableStateOf(solutionFingerprint) }
    var lastAnimatedGuessFingerprint by remember { mutableStateOf<String?>(latestGuessFingerprint) }
    var animatingGuessIndex by remember { mutableStateOf<Int?>(null) }
    var revealedCellCount by remember { mutableStateOf(Int.MAX_VALUE) }
    var displayedKeyboardHints by remember { mutableStateOf(gameState.wordShiftKeyboardHints) }

    LaunchedEffect(solutionFingerprint, latestGuessFingerprint, gameState.wordShiftGuesses.size, gameState.wordShiftKeyboardHints, gameState.wordShiftAwaitingNextRound) {
        if (!revealAnimationInitialized) {
            revealAnimationInitialized = true
            lastSolutionFingerprint = solutionFingerprint
            lastAnimatedGuessFingerprint = latestGuessFingerprint
            displayedKeyboardHints = gameState.wordShiftKeyboardHints
            animatingGuessIndex = null
            revealedCellCount = Int.MAX_VALUE
            return@LaunchedEffect
        }

        if (solutionFingerprint != lastSolutionFingerprint || gameState.wordShiftGuesses.isEmpty()) {
            lastSolutionFingerprint = solutionFingerprint
            lastAnimatedGuessFingerprint = latestGuessFingerprint
            displayedKeyboardHints = gameState.wordShiftKeyboardHints
            animatingGuessIndex = null
            revealedCellCount = Int.MAX_VALUE
            return@LaunchedEffect
        }

        if (latestGuess == null || latestGuessFingerprint == null || latestGuessFingerprint == lastAnimatedGuessFingerprint) {
            displayedKeyboardHints = gameState.wordShiftKeyboardHints
            animatingGuessIndex = null
            revealedCellCount = Int.MAX_VALUE
            return@LaunchedEffect
        }

        lastAnimatedGuessFingerprint = latestGuessFingerprint
        animatingGuessIndex = gameState.wordShiftGuesses.lastIndex
        revealedCellCount = 0
        displayedKeyboardHints = buildKeyboardHints(gameState.wordShiftGuesses.dropLast(1))

        latestGuess.tokens.zip(latestGuess.states).forEachIndexed { index, (token, state) ->
            delay(WordShiftRevealStepDelayMillis)
            revealedCellCount = index + 1
            displayedKeyboardHints = mergeKeyboardHint(displayedKeyboardHints, token, state)
        }

        animatingGuessIndex = null
        revealedCellCount = Int.MAX_VALUE
        displayedKeyboardHints = gameState.wordShiftKeyboardHints

        if (gameState.wordShiftAwaitingNextRound && latestGuess.states.all { it == WordShiftLetterState.Correct }) {
            delay(WordShiftSolvedPauseMillis)
            onAdvanceRound()
        }
    }

    val inputEnabled = gameState.status == GameStatus.Running && !gameState.wordShiftAwaitingNextRound && animatingGuessIndex == null

    if (showRestartDialog) {
        RestartConfirmDialog(
            onDismissRequest = { showRestartDialog = false },
            title = stringResource(Res.string.restart_confirm_title),
            message = stringResource(Res.string.restart_confirm_body),
            confirmLabel = stringResource(Res.string.restart_confirm),
            dismissLabel = stringResource(Res.string.restart_cancel),
            onConfirm = {
                showRestartDialog = false
                onRestart()
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
            onPlayAgain = onRestart,
            onUseExtraLife = onRewardedRevive,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(uiColors.panel.copy(alpha = 0.18f))
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            interactiveOnboardingScene?.let { scene ->
                WordShiftOnboardingHintCard(
                    scene = scene,
                    currentStep = interactiveOnboardingCurrentStep,
                    totalSteps = interactiveOnboardingTotalSteps,
                )
            }

            WordShiftBoard(
                gameState = gameState,
                animatedGuessIndex = animatingGuessIndex,
                revealedCellCount = revealedCellCount,
                modifier = Modifier.fillMaxWidth(),
            )

            if (gameState.message.key != GameTextKey.GameMessageWordShiftSolved) {
                WordShiftMessageCard(
                    message = resolveGameText(gameState.message),
                )
            }

            WordShiftKeyboard(
                keyboardRows = pack.keyboardRows(gameState.config.columns),
                keyboardHints = displayedKeyboardHints,
                enabled = inputEnabled,
                onAppendToken = onAppendToken,
                onDeleteToken = onDeleteToken,
                onSubmitGuess = onSubmitGuess,
            )
        }
    }
}

@Composable
internal fun WordShiftBoard(
    gameState: GameState,
    animatedGuessIndex: Int? = null,
    revealedCellCount: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
            elevation = 10.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = BlockGamesThemeTokens.uiColors.gameSurface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, BlockGamesThemeTokens.uiColors.panelStroke.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(gameState.config.rows) { rowIndex ->
                val rowGuess = gameState.wordShiftGuesses.getOrNull(rowIndex)
                val currentRow = if (rowIndex == gameState.wordShiftGuesses.size && gameState.status == GameStatus.Running) {
                    gameState.wordShiftCurrentGuess
                } else {
                    emptyList()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(gameState.config.columns) { columnIndex ->
                        val state = rowGuess?.states?.getOrNull(columnIndex)?.takeIf {
                            rowIndex != animatedGuessIndex || columnIndex < revealedCellCount
                        }
                        WordShiftCell(
                            token = rowGuess?.tokens?.getOrNull(columnIndex) ?: currentRow.getOrNull(columnIndex).orEmpty(),
                            state = state,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordShiftCell(
    token: String,
    state: WordShiftLetterState?,
    modifier: Modifier = Modifier,
) {
    val palette = wordShiftPaletteFor(state)
    val animatedBackground by animateColorAsState(
        targetValue = palette.container,
        animationSpec = tween(durationMillis = WordShiftFlipAnimationMillis),
        label = "wordShiftCellBackground",
    )
    val animatedBorder by animateColorAsState(
        targetValue = palette.border,
        animationSpec = tween(durationMillis = WordShiftFlipAnimationMillis),
        label = "wordShiftCellBorder",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = palette.content,
        animationSpec = tween(durationMillis = WordShiftFlipAnimationMillis),
        label = "wordShiftCellContent",
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
        animationSpec = tween(durationMillis = WordShiftFlipAnimationMillis),
        label = "wordShiftCellFlip",
    )
    val density = LocalDensity.current
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationX = (1f - flipProgress) * 88f
                scaleX = 0.96f + (flipProgress * 0.04f)
                scaleY = 0.90f + (flipProgress * 0.10f)
                cameraDistance = with(density) { 18.dp.toPx() }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBackground),
        border = BorderStroke(1.dp, animatedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = token,
                style = if (token.length > 1) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = animatedContentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WordShiftKeyboard(
    keyboardRows: List<List<String>>,
    keyboardHints: Map<String, WordShiftLetterState>,
    enabled: Boolean,
    onAppendToken: (String) -> Unit,
    onDeleteToken: () -> Unit,
    onSubmitGuess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedRows = remember(keyboardRows) {
        when {
            keyboardRows.size >= 3 -> keyboardRows.take(3)
            keyboardRows.isEmpty() -> listOf(emptyList(), emptyList(), emptyList())
            else -> keyboardRows + List(3 - keyboardRows.size) { emptyList() }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WordShiftKeyboardGapDp.dp),
    ) {
        WordShiftKeyboardLetterRow(
            tokens = resolvedRows[0],
            keyboardHints = keyboardHints,
            enabled = enabled,
            onAppendToken = onAppendToken,
        )
        WordShiftKeyboardLetterRow(
            tokens = resolvedRows[1],
            keyboardHints = keyboardHints,
            enabled = enabled,
            onAppendToken = onAppendToken,
            leadingSpacerWeight = WordShiftKeyboardSecondRowIndent,
            trailingSpacerWeight = WordShiftKeyboardSecondRowIndent,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WordShiftKeyboardGapDp.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WordShiftKeyboardButton(
                label = stringResource(Res.string.wordshift_enter),
                state = WordShiftLetterState.Correct,
                enabled = enabled,
                onClick = onSubmitGuess,
                modifier = Modifier.weight(WordShiftKeyboardControlKeyWeight),
                icon = Icons.Filled.Check,
                showLabel = false,
            )
            resolvedRows[2].forEach { token ->
                WordShiftKeyboardButton(
                    label = token,
                    state = keyboardHints[token] ?: WordShiftLetterState.Unknown,
                    enabled = enabled,
                    onClick = { onAppendToken(token) },
                    modifier = Modifier.weight(wordShiftKeyWeight(token)),
                )
            }
            WordShiftKeyboardButton(
                label = stringResource(Res.string.wordshift_delete),
                state = WordShiftLetterState.Absent,
                enabled = enabled,
                onClick = onDeleteToken,
                modifier = Modifier.weight(WordShiftKeyboardDeleteKeyWeight),
                icon = Icons.AutoMirrored.Filled.Backspace,
                showLabel = false,
            )
        }
    }
}

@Composable
private fun WordShiftKeyboardLetterRow(
    tokens: List<String>,
    keyboardHints: Map<String, WordShiftLetterState>,
    enabled: Boolean,
    onAppendToken: (String) -> Unit,
    leadingSpacerWeight: Float = 0f,
    trailingSpacerWeight: Float = 0f,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WordShiftKeyboardGapDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingSpacerWeight > 0f) {
            Spacer(modifier = Modifier.weight(leadingSpacerWeight))
        }
        tokens.forEach { token ->
            WordShiftKeyboardButton(
                label = token,
                state = keyboardHints[token] ?: WordShiftLetterState.Unknown,
                enabled = enabled,
                onClick = { onAppendToken(token) },
                modifier = Modifier.weight(wordShiftKeyWeight(token)),
            )
        }
        if (trailingSpacerWeight > 0f) {
            Spacer(modifier = Modifier.weight(trailingSpacerWeight))
        }
    }
}

@Composable
private fun RowScope.WordShiftKeyboardButton(
    label: String,
    state: WordShiftLetterState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showLabel: Boolean = true,
) {
    val palette = wordShiftPaletteFor(
        state = state,
        enabled = enabled,
        isKeyboardKey = true,
    )
    val animatedBackground by animateColorAsState(palette.container, tween(180), label = "wordShiftKeyBackground")
    val animatedBorder by animateColorAsState(palette.border, tween(180), label = "wordShiftKeyBorder")
    val animatedContent by animateColorAsState(palette.content, tween(180), label = "wordShiftKeyContent")
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .height(WordShiftKeyboardKeyHeightDp.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.72f },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = animatedBackground),
        border = BorderStroke(1.dp, animatedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (showLabel) 2.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            val compactTextStyle = when {
                maxWidth < 18.dp -> MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                maxWidth < 22.dp -> MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                maxWidth < 28.dp || label.length >= 4 -> MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                maxWidth < 34.dp || label.length >= 2 -> MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                else -> MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null && !showLabel) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = animatedContent,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = label,
                        color = animatedContent,
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

private fun WordShiftGuess.fingerprint(): String = buildString {
    append(WordShiftLexicon.keyOf(tokens))
    append('#')
    append(states.joinToString(separator = ",") { it.ordinal.toString() })
}

private fun buildKeyboardHints(guesses: List<WordShiftGuess>): Map<String, WordShiftLetterState> =
    guesses.fold(emptyMap(), ::mergeKeyboardHints)

private fun mergeKeyboardHints(
    existing: Map<String, WordShiftLetterState>,
    guess: WordShiftGuess,
): Map<String, WordShiftLetterState> {
    var updated = existing
    guess.tokens.zip(guess.states).forEach { (token, state) ->
        updated = mergeKeyboardHint(updated, token, state)
    }
    return updated
}

private fun mergeKeyboardHint(
    existing: Map<String, WordShiftLetterState>,
    token: String,
    state: WordShiftLetterState,
): Map<String, WordShiftLetterState> {
    val previous = existing[token] ?: WordShiftLetterState.Unknown
    if (state.ordinal < previous.ordinal) return existing
    return existing + (token to state)
}

@Composable
private fun wordShiftPaletteFor(
    state: WordShiftLetterState?,
    enabled: Boolean = true,
    isKeyboardKey: Boolean = false,
): WordShiftFeedbackPalette {
    val uiColors = BlockGamesThemeTokens.uiColors
    val colorScheme = MaterialTheme.colorScheme
    val base = when (state) {
        WordShiftLetterState.Correct -> WordShiftFeedbackPalette(
            container = lerp(uiColors.success, uiColors.actionSuccess, 0.28f).copy(alpha = if (isKeyboardKey) 0.98f else 0.94f),
            border = lerp(uiColors.success, Color.White, 0.18f),
            content = Color.White,
        )

        WordShiftLetterState.Present -> WordShiftFeedbackPalette(
            container = lerp(uiColors.warning, uiColors.actionWarning, 0.18f).copy(alpha = if (isKeyboardKey) 0.98f else 0.94f),
            border = lerp(uiColors.warning, Color.White, 0.14f),
            content = Color.White,
        )

        WordShiftLetterState.Absent -> WordShiftFeedbackPalette(
            container = lerp(uiColors.panelMuted, colorScheme.surfaceVariant, 0.38f).copy(alpha = if (isKeyboardKey) 0.98f else 0.92f),
            border = lerp(uiColors.panelStroke, colorScheme.onSurfaceVariant, 0.22f),
            content = lerp(colorScheme.onSurface, Color.White, 0.10f),
        )

        WordShiftLetterState.Unknown,
        null,
        -> WordShiftFeedbackPalette(
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

private fun wordShiftKeyWeight(token: String): Float = when {
    token.length >= 3 -> 1.35f
    token.length == 2 -> 1.15f
    else -> 1f
}


@Composable
private fun WordShiftMessageCard(
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
            text = message.ifBlank { stringResource(Res.string.game_message_wordshift_enter_word) },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WordShiftOnboardingHintCard(
    scene: WordShiftOnboardingScene,
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
                    text = if (totalSteps > 0) stringResource(Res.string.wordshift_guess_label, currentStep.coerceAtLeast(1)) else "",
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
                    WordShiftOnboardingStage.FirstGuess -> stringResource(Res.string.interactive_onboarding_wordshift_type_hint)
                    WordShiftOnboardingStage.ReadHints -> stringResource(Res.string.interactive_onboarding_wordshift_submit_hint)
                    WordShiftOnboardingStage.SolveWord -> stringResource(Res.string.interactive_onboarding_wordshift_solve_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = scene.suggestedGuess.joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = uiColors.guideAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

