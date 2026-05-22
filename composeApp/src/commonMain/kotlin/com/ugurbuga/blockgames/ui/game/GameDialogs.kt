package com.ugurbuga.blockgames.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.boost
import blockgames.composeapp.generated.resources.challenge_completed
import blockgames.composeapp.generated.resources.color_amber
import blockgames.composeapp.generated.resources.color_blue
import blockgames.composeapp.generated.resources.color_coral
import blockgames.composeapp.generated.resources.color_cyan
import blockgames.composeapp.generated.resources.color_emerald
import blockgames.composeapp.generated.resources.color_gold
import blockgames.composeapp.generated.resources.color_lime
import blockgames.composeapp.generated.resources.color_rose
import blockgames.composeapp.generated.resources.color_violet
import blockgames.composeapp.generated.resources.continue_label
import blockgames.composeapp.generated.resources.danger
import blockgames.composeapp.generated.resources.danger_none
import blockgames.composeapp.generated.resources.feedback_ad_reward_blockwise
import blockgames.composeapp.generated.resources.feedback_ad_reward_boomblocks
import blockgames.composeapp.generated.resources.feedback_ad_reward_mergeshift
import blockgames.composeapp.generated.resources.feedback_chain
import blockgames.composeapp.generated.resources.feedback_clear
import blockgames.composeapp.generated.resources.feedback_extra_life
import blockgames.composeapp.generated.resources.feedback_hold_armed
import blockgames.composeapp.generated.resources.feedback_micro_adjust
import blockgames.composeapp.generated.resources.feedback_overflow
import blockgames.composeapp.generated.resources.feedback_perfect
import blockgames.composeapp.generated.resources.feedback_perfect_lane
import blockgames.composeapp.generated.resources.feedback_score_only
import blockgames.composeapp.generated.resources.feedback_soft_lock
import blockgames.composeapp.generated.resources.feedback_special
import blockgames.composeapp.generated.resources.feedback_special_chain
import blockgames.composeapp.generated.resources.feedback_swap
import blockgames.composeapp.generated.resources.game_message_ad_reward_blockwise
import blockgames.composeapp.generated.resources.game_message_ad_reward_boomblocks
import blockgames.composeapp.generated.resources.game_message_ad_reward_mergeshift
import blockgames.composeapp.generated.resources.game_message_ad_reward_stackshift
import blockgames.composeapp.generated.resources.game_message_chain_lines
import blockgames.composeapp.generated.resources.game_message_extra_life_used
import blockgames.composeapp.generated.resources.game_message_good_shot
import blockgames.composeapp.generated.resources.game_message_hold_updated
import blockgames.composeapp.generated.resources.game_message_lines_cleared
import blockgames.composeapp.generated.resources.game_message_no_opening
import blockgames.composeapp.generated.resources.game_message_overflow
import blockgames.composeapp.generated.resources.game_message_perfect_drop
import blockgames.composeapp.generated.resources.game_message_pressure_game_over
import blockgames.composeapp.generated.resources.game_message_select_column
import blockgames.composeapp.generated.resources.game_message_soft_lock
import blockgames.composeapp.generated.resources.game_message_special_chain_board
import blockgames.composeapp.generated.resources.game_message_special_lines
import blockgames.composeapp.generated.resources.game_message_special_triggered
import blockgames.composeapp.generated.resources.game_message_tempo_critical
import blockgames.composeapp.generated.resources.game_message_tempo_up
import blockgames.composeapp.generated.resources.game_message_wordshift_enter_word
import blockgames.composeapp.generated.resources.game_message_wordshift_failed
import blockgames.composeapp.generated.resources.game_message_wordshift_keep_trying
import blockgames.composeapp.generated.resources.game_message_wordshift_not_enough_letters
import blockgames.composeapp.generated.resources.game_message_wordshift_not_in_dictionary
import blockgames.composeapp.generated.resources.game_message_wordshift_revived
import blockgames.composeapp.generated.resources.game_message_wordshift_solved
import blockgames.composeapp.generated.resources.game_over_extra_life
import blockgames.composeapp.generated.resources.game_over_extra_life_loading
import blockgames.composeapp.generated.resources.game_over_new_high_score
import blockgames.composeapp.generated.resources.game_over_title
import blockgames.composeapp.generated.resources.high_score
import blockgames.composeapp.generated.resources.high_score_new_record
import blockgames.composeapp.generated.resources.hold
import blockgames.composeapp.generated.resources.launch_bar
import blockgames.composeapp.generated.resources.launch_boost_active
import blockgames.composeapp.generated.resources.launch_chain_message
import blockgames.composeapp.generated.resources.launch_drag_hint
import blockgames.composeapp.generated.resources.launch_drag_hint_blockwise
import blockgames.composeapp.generated.resources.launch_game_over
import blockgames.composeapp.generated.resources.launch_label
import blockgames.composeapp.generated.resources.launch_soft_lock_message
import blockgames.composeapp.generated.resources.launch_special_chance
import blockgames.composeapp.generated.resources.lines
import blockgames.composeapp.generated.resources.piece_properties_none
import blockgames.composeapp.generated.resources.play_again
import blockgames.composeapp.generated.resources.queue_empty
import blockgames.composeapp.generated.resources.queue_next_short
import blockgames.composeapp.generated.resources.restart
import blockgames.composeapp.generated.resources.restart_cancel
import blockgames.composeapp.generated.resources.restart_confirm
import blockgames.composeapp.generated.resources.restart_confirm_body
import blockgames.composeapp.generated.resources.restart_confirm_title
import blockgames.composeapp.generated.resources.return_home
import blockgames.composeapp.generated.resources.score
import blockgames.composeapp.generated.resources.settings_challenges
import blockgames.composeapp.generated.resources.special_column_clearer
import blockgames.composeapp.generated.resources.special_ghost
import blockgames.composeapp.generated.resources.special_heavy
import blockgames.composeapp.generated.resources.special_row_clearer
import blockgames.composeapp.generated.resources.time_minutes_seconds_format
import blockgames.composeapp.generated.resources.time_remaining
import blockgames.composeapp.generated.resources.tutorial_back
import blockgames.composeapp.generated.resources.tutorial_finish
import blockgames.composeapp.generated.resources.tutorial_ready_body
import blockgames.composeapp.generated.resources.tutorial_ready_title
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameText
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.appNameResourceId
import com.ugurbuga.blockgames.localization.formatAppString
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.BlockGamesUiColors
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val GameOverDialogWidth = 420.dp
private val GameOverDialogRevealOffsetDp = 12.dp
private val GameOverDialogCardPadding = 24.dp
private val GameOverDialogIconSize = 40.dp
internal const val GameOverDialogRevealDurationMillis = 260
private const val MetricHighlightPulseScale = 1.08f
private const val MetricHighlightPulseUpDurationMillis = 180
private const val MetricHighlightPulseDownDurationMillis = 520
internal val TopBarActionIconSize = 20.dp
internal val TopBarActionBlockSize = 28.dp

internal val TopBarVerticalPadding = 6.dp
internal val TopBarRowSpacing = 4.dp
internal val TopBarIconSpacing = 4.dp
internal val TopBarMetricLaunchSpacing = 8.dp
internal val TopBarActionRailSize = 44.dp
internal val TopBarMetricHeight = 48.dp
internal val MetricChipHorizontalPadding = 16.dp
internal val MetricChipVerticalPadding = 7.dp
internal val RecordIndicatorIconSize = 12.dp
internal val RecordIndicatorSideGap = 6.dp
internal const val MetricHighlightThreshold = 0.08f

internal const val TopBarPanelAlpha = 0.88f
internal const val TopBarPanelStrokeAlpha = 0.72f
internal const val TopBarPanelGlowAlpha = 0.14f

internal val TopBarActionBlockTones = listOf(
    CellTone.Cyan,
    CellTone.Gold,
    CellTone.Violet,
    CellTone.Lime,
    CellTone.Amber,
)

@Composable
internal fun GameOverDialog(
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    revealProgressProvider: () -> Float,
    canUseExtraLife: Boolean,
    isExtraLifeLoading: Boolean,
    showExtraLifeButton: Boolean,
    onPlayAgain: () -> Unit,
    onUseExtraLife: () -> Unit,
) {
    val density = LocalDensity.current
    val revealOffsetPx = with(density) { GameOverDialogRevealOffsetDp.toPx() }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val revealProgress = revealProgressProvider()
        GameOverDialogContent(
            gameState = gameState,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            canUseExtraLife = canUseExtraLife,
            isExtraLifeLoading = isExtraLifeLoading,
            showExtraLifeButton = showExtraLifeButton,
            onPlayAgain = onPlayAgain,
            onUseExtraLife = onUseExtraLife,
            modifier = Modifier
                .widthIn(max = GameOverDialogWidth)
                .graphicsLayer {
                    alpha = revealProgress
                    scaleX = 0.90f + (0.10f * revealProgress)
                    scaleY = 0.90f + (0.10f * revealProgress)
                    translationY = (1f - revealProgress) * revealOffsetPx
                },
        )
    }
}

@Composable
internal fun GameOverDialogContent(
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    canUseExtraLife: Boolean,
    isExtraLifeLoading: Boolean,
    showExtraLifeButton: Boolean,
    onPlayAgain: () -> Unit,
    onUseExtraLife: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val recordPulsePhase = remember { Animatable(0f) }
    val dialogContainerColor = dialogContainerColor(uiColors)

    LaunchedEffect(showNewHighScoreMessage) {
        if (!showNewHighScoreMessage) {
            recordPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        recordPulsePhase.snapTo(0f)
        while (isActive && showNewHighScoreMessage) {
            recordPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            recordPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
            elevation = 12.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = dialogContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(GameOverDialogCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogHeroIcon(
                icon = Icons.Filled.EmojiEvents,
                iconColors = listOf(
                    if (showNewHighScoreMessage) uiColors.success.copy(alpha = 0.94f) else uiColors.warning.copy(
                        alpha = 0.94f
                    ),
                    if (showNewHighScoreMessage) uiColors.success.copy(alpha = 0.70f) else uiColors.danger.copy(
                        alpha = 0.72f
                    ),
                    uiColors.panel.copy(alpha = 0.12f),
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX =
                        if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.28f * recordPulsePhase.value) else 1f
                    scaleY =
                        if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.28f * recordPulsePhase.value) else 1f
                },
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val isChallengeWin = gameState.activeChallenge?.isCompleted == true
                Text(
                    text = when {
                        isChallengeWin -> stringResource(Res.string.challenge_completed)
                        showNewHighScoreMessage -> stringResource(Res.string.high_score_new_record)
                        else -> resolveGameText(gameText(GameTextKey.GameOverTitle))
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (showNewHighScoreMessage || isChallengeWin) uiColors.success else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (showNewHighScoreMessage) resolveGameText(gameText(GameTextKey.GameOverNewHighScore)) else resolveGameText(
                        gameState.message
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GameOverStatChip(
                    title = if (showNewHighScoreMessage) stringResource(Res.string.high_score_new_record) else resolveGameText(
                        gameText(GameTextKey.HighScore)
                    ),
                    value = highestScore.toString(),
                    accentColor = if (showNewHighScoreMessage) uiColors.success else null,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX =
                                if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.10f * recordPulsePhase.value) else 1f
                            scaleY =
                                if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.10f * recordPulsePhase.value) else 1f
                        }
                )
                GameOverStatChip(
                    title = resolveGameText(gameText(GameTextKey.Score)),
                    value = gameState.score.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showExtraLifeButton) {
                    DialogActionButton(
                        text = if (isExtraLifeLoading) {
                            stringResource(Res.string.game_over_extra_life_loading)
                        } else {
                            stringResource(Res.string.game_over_extra_life)
                        },
                        onClick = onUseExtraLife,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canUseExtraLife && !isExtraLifeLoading,
                        emphasized = true,
                        icon = Icons.Outlined.Videocam,
                    )
                }

                val isChallengeWin = gameState.activeChallenge?.isCompleted == true
                DialogActionButton(
                    text = if (isChallengeWin) stringResource(Res.string.settings_challenges) else stringResource(
                        Res.string.play_again
                    ),
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = !showExtraLifeButton || isChallengeWin,
                    icon = if (isChallengeWin) Icons.Default.EmojiEvents else Icons.Filled.Refresh,
                )
            }
        }
    }
}

@Composable
internal fun GameOverStatChip(
    title: String,
    value: String,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            accentColor?.copy(alpha = 0.52f) ?: uiColors.panelStroke.copy(alpha = 0.52f)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor ?: uiColors.subtitle,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun RestartConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
) {
    ThemedConfirmDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        dismissLabel = dismissLabel,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun ThemedConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    icon: ImageVector = Icons.Filled.Refresh,
    iconColors: List<Color>? = null,
    dismissButtonIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Dialog(onDismissRequest = onDismissRequest) {
        GameEventDialogCard(
            modifier = Modifier.widthIn(max = GameOverDialogWidth),
            title = title,
            message = message,
            buttonLabel = confirmLabel,
            onAction = onConfirm,
            secondaryButtonLabel = dismissLabel,
            secondaryButtonIcon = dismissButtonIcon,
            onSecondaryAction = onDismissRequest,
            icon = icon,
            iconColors = iconColors ?: listOf(
                uiColors.danger.copy(alpha = 0.94f),
                uiColors.warning.copy(alpha = 0.78f),
                uiColors.panelHighlight.copy(alpha = 0.22f),
            ),
        )
    }
}

@Composable
internal fun GameEventDialogCard(
    title: String,
    message: String,
    buttonLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.EmojiEvents,
    iconColors: List<Color> = listOf(Color.Yellow, Color.Red),
    secondaryButtonLabel: String? = null,
    primaryButtonIcon: ImageVector? = icon,
    secondaryButtonIcon: ImageVector? = if (secondaryButtonLabel != null) Icons.AutoMirrored.Filled.ArrowBack else null,
    secondaryButtonContentColor: Color? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val dialogContainerColor = dialogContainerColor(uiColors)
    val dialogShape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = dialogShape,
            elevation = 12.dp,
        ),
        shape = dialogShape,
        colors = CardDefaults.cardColors(containerColor = dialogContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(GameOverDialogCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogHeroIcon(
                icon = icon,
                iconColors = iconColors,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = dialogMessageColor(uiColors),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogActionButton(
                    text = buttonLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = true,
                    icon = primaryButtonIcon,
                )

                if (secondaryButtonLabel != null && onSecondaryAction != null) {
                    DialogActionButton(
                        text = secondaryButtonLabel,
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth(),
                        emphasized = false,
                        icon = secondaryButtonIcon,
                        contentColor = secondaryButtonContentColor,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DialogHeroIcon(
    icon: ImageVector,
    iconColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val resolvedStyle = settings.blockVisualStyle
    val effectivePulse = rememberBlockStylePulse(style = resolvedStyle)
    val iconTint = blockStyleIconTint(style = resolvedStyle)
    val tileSize = 78.dp
    val tileShape = RoundedCornerShape(boardCellCornerRadiusDp(tileSize, resolvedStyle))
    val heroTone = when (iconColors.size % 4) {
        1 -> CellTone.Gold
        2 -> CellTone.Violet
        3 -> CellTone.Emerald
        else -> CellTone.Cyan
    }
    val isDarkTheme = isBlockGamesDarkTheme(settings)
    val heroColor = remember(settings.blockColorPalette, heroTone, isDarkTheme) {
        heroTone.paletteColor(settings.blockColorPalette, isDarkTheme)
    }

    Box(
        modifier = modifier
            .size(tileSize)
            .blockGamesSurfaceShadow(
                shape = tileShape,
                elevation = 10.dp,
            )
            .clip(tileShape)
            .background(heroColor.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        BlockCellPreview(
            tone = heroTone,
            palette = settings.blockColorPalette,
            style = resolvedStyle,
            size = 60.dp,
            pulse = effectivePulse,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(GameOverDialogIconSize),
        )
    }
}


@Composable
internal fun DialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color? = null,
) {
    BlockStyleActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        emphasized = emphasized,
        icon = icon,
        contentColor = contentColor,
    )
}

@Composable
internal fun BlockStyleActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color? = null,
    tone: CellTone = if (emphasized) CellTone.Cyan else CellTone.Violet,
    height: Dp = 52.dp,
    pulse: Float = 0f,
    iconOnRight: Boolean = false,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val resolvedStyle = settings.blockVisualStyle
    val effectivePulse = rememberBlockStylePulse(
        style = resolvedStyle,
        pulse = pulse,
    )
    val resolvedTone = if (enabled) tone else CellTone.Gold
    val buttonColors = rememberActionButtonColors(
        tone = resolvedTone,
        enabled = enabled,
        emphasized = emphasized,
    )
    val resolvedContentColor = resolveActionButtonContentColor(
        contentColor = contentColor,
        fallbackColor = blockStyleIconTint(
            style = resolvedStyle,
            enabled = enabled,
        ),
    )
    val buttonShape = RoundedCornerShape(boardCellCornerRadiusDp(height, resolvedStyle))

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.72f }
            .blockGamesSurfaceShadow(
                shape = buttonShape,
                elevation = if (emphasized) 10.dp else 5.dp,
            )
            .clip(buttonShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadiusPx = boardCellCornerRadiusPx(size.minDimension, resolvedStyle)
            drawCellBody(
                baseColor = buttonColors.container,
                palette = settings.blockColorPalette,
                style = resolvedStyle,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                pulse = effectivePulse,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null && !iconOnRight) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedContentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                color = resolvedContentColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (icon != null && iconOnRight) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedContentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

internal fun resolveActionButtonContentColor(
    contentColor: Color?,
    fallbackColor: Color,
): Color = contentColor ?: fallbackColor

@Composable
internal fun MinimalTopBar(
    gameState: GameState,
    scoreHighlightStrengthProvider: () -> Float,
    scoreHighlightScaleProvider: () -> Float,
    remainingTimeLabel: String,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    controlsEnabled: Boolean = true,
    stylePulse: Float = 0f,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val restartLabel = resolveGameText(gameText(GameTextKey.Restart))
    val backLabel = stringResource(Res.string.tutorial_back)
    val panelStrokeColor = uiColors.panelStroke.copy(alpha = TopBarPanelStrokeAlpha)
    val panelGlow = Brush.verticalGradient(
        colors = listOf(
            uiColors.panelHighlight.copy(alpha = TopBarPanelGlowAlpha),
            uiColors.launchGlow.copy(alpha = 0.10f),
            Color.Transparent,
        ),
    )
    Card(
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = TopBarPanelAlpha)),
        border = BorderStroke(1.dp, panelStrokeColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
            elevation = 10.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(panelGlow)
                .padding(horizontal = 10.dp, vertical = TopBarVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(TopBarRowSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopBarActionBlockButton(
                    tone = TopBarActionBlockTones[0],
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backLabel,
                    onClick = onBack,
                    enabled = true,
                    pulse = stylePulse,
                    size = TopBarActionRailSize,
                )
                CompactMetricChip(
                    title = resolveGameText(gameText(GameTextKey.Score)),
                    value = gameState.score.toString(),
                    highlightStrengthProvider = scoreHighlightStrengthProvider,
                    scaleProvider = scoreHighlightScaleProvider,
                    modifier = Modifier.weight(1f).height(TopBarMetricHeight),
                )
                gameState.remainingTimeMillis?.let { remainingTimeMillis ->
                    CompactMetricChip(
                        title = remainingTimeLabel,
                        value = formatRemainingTime(remainingTimeMillis),
                        modifier = Modifier.weight(1f).height(TopBarMetricHeight),
                    )
                }
                TopBarActionBlockButton(
                    tone = TopBarActionBlockTones[2],
                    icon = Icons.Filled.Refresh,
                    contentDescription = restartLabel,
                    onClick = onRestart,
                    enabled = controlsEnabled,
                    pulse = stylePulse,
                    size = TopBarActionRailSize,
                )
            }
        }
    }
}

@Composable
internal fun formatRemainingTime(remainingTimeMillis: Long): String {
    val totalSeconds = (remainingTimeMillis.coerceAtLeast(0L) + 999L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return stringResource(
        Res.string.time_minutes_seconds_format,
        minutes,
        seconds.toString().padStart(2, '0'),
    )
}

@Composable
internal fun CompactMetricChip(
    title: String,
    value: String,
    highlightStrengthProvider: () -> Float = { 0f },
    scaleProvider: () -> Float = { 1f },
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val highlightStrength = highlightStrengthProvider()
    val isHighlighted = highlightStrength > MetricHighlightThreshold

    Card(
        modifier = modifier
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
                elevation = 5.dp,
            )
            .graphicsLayer {
                val scale = scaleProvider()
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (isHighlighted) 1.6.dp else 1.dp,
            color = if (isHighlighted) {
                uiColors.success.copy(alpha = (0.50f + highlightStrength * 0.50f).coerceIn(0f, 1f))
            } else {
                uiColors.panelStroke.copy(alpha = 0.72f)
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 1.dp)
                .graphicsLayer {
                    alpha = (0.92f + highlightStrength * 0.08f).coerceIn(0f, 1f)
                }
                .padding(
                    horizontal = MetricChipHorizontalPadding,
                    vertical = MetricChipVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isHighlighted) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = uiColors.success,
                    modifier = Modifier
                        .size(RecordIndicatorIconSize)
                        .graphicsLayer { alpha = highlightStrengthProvider().coerceIn(0f, 1f) },
                )
                Spacer(modifier = Modifier.width(RecordIndicatorSideGap))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = value,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            if (isHighlighted) {
                Spacer(modifier = Modifier.width(RecordIndicatorSideGap))
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = uiColors.success,
                    modifier = Modifier
                        .size(RecordIndicatorIconSize)
                        .graphicsLayer { alpha = highlightStrength.coerceIn(0f, 1f) },
                )
            }
        }
    }
}

@Composable
internal fun InteractiveOnboardingCompletionDialog(
    onStartGame: () -> Unit,
    onReturnHome: () -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Dialog(onDismissRequest = {}) {
        GameEventDialogCard(
            title = stringResource(Res.string.tutorial_ready_title),
            message = stringResource(Res.string.tutorial_ready_body),
            buttonLabel = stringResource(Res.string.tutorial_finish),
            onAction = onStartGame,
            modifier = Modifier.widthIn(max = GameOverDialogWidth),
            icon = Icons.Filled.EmojiEvents,
            iconColors = listOf(
                uiColors.success.copy(alpha = 0.94f),
                uiColors.launchGlow.copy(alpha = 0.84f),
                uiColors.panel.copy(alpha = 0.12f),
            ),
            secondaryButtonLabel = stringResource(Res.string.return_home),
            primaryButtonIcon = Icons.Filled.PlayArrow,
            secondaryButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onSecondaryAction = onReturnHome,
        )
    }
}

@Composable
internal fun resolveGameText(text: GameText): String {
    val resourceId = text.key.stringResourceId()
    val resolvedText = if (text.args.isEmpty()) {
        stringResource(resourceId)
    } else {
        val finalArgs = text.args.map { arg ->
            val tone = CellTone.entries.find { it.name == arg }
            if (tone != null) stringResource(tone.stringResourceId()) else arg
        }
        stringResource(resourceId, *finalArgs.toTypedArray())
    }
    return formatAppString(resolvedText)
}

internal fun SpecialBlockType.shortLabel(): GameText {
    return when (this) {
        SpecialBlockType.ColumnClearer -> gameText(GameTextKey.SpecialColumnClearer)
        SpecialBlockType.RowClearer -> gameText(GameTextKey.SpecialRowClearer)
        SpecialBlockType.Ghost -> gameText(GameTextKey.SpecialGhost)
        SpecialBlockType.Heavy -> gameText(GameTextKey.SpecialHeavy)
        SpecialBlockType.None -> gameText(GameTextKey.PiecePropertiesNone)
    }
}

internal fun GameTextKey.stringResourceId(): StringResource {
    return when (this) {
        GameTextKey.AppTitle -> appNameResourceId()
        GameTextKey.Hold -> Res.string.hold
        GameTextKey.Restart -> Res.string.restart
        GameTextKey.RestartConfirmTitle -> Res.string.restart_confirm_title
        GameTextKey.RestartConfirmBody -> Res.string.restart_confirm_body
        GameTextKey.RestartConfirm -> Res.string.restart_confirm
        GameTextKey.RestartCancel -> Res.string.restart_cancel
        GameTextKey.Score -> Res.string.score
        GameTextKey.HighScore -> Res.string.high_score
        GameTextKey.Lines -> Res.string.lines
        GameTextKey.Boost -> Res.string.boost
        GameTextKey.Danger -> Res.string.danger
        GameTextKey.DangerNone -> Res.string.danger_none
        GameTextKey.Launch -> Res.string.launch_label
        GameTextKey.LaunchBar -> Res.string.launch_bar
        GameTextKey.LaunchBoostActive -> Res.string.launch_boost_active
        GameTextKey.LaunchSpecialChance -> Res.string.launch_special_chance
        GameTextKey.LaunchSoftLockMessage -> Res.string.launch_soft_lock_message
        GameTextKey.LaunchChainMessage -> Res.string.launch_chain_message
        GameTextKey.LaunchGameOver -> Res.string.launch_game_over
        GameTextKey.LaunchDragHint -> Res.string.launch_drag_hint
        GameTextKey.LaunchDragHintBlockWise -> Res.string.launch_drag_hint_blockwise
        GameTextKey.QueueHold -> Res.string.hold
        GameTextKey.QueueNextShort -> Res.string.queue_next_short
        GameTextKey.QueueEmpty -> Res.string.queue_empty
        GameTextKey.GameOverTitle -> Res.string.game_over_title
        GameTextKey.Continue -> Res.string.continue_label
        GameTextKey.GameOverExtraLife -> Res.string.game_over_extra_life
        GameTextKey.GameOverExtraLifeLoading -> Res.string.game_over_extra_life_loading
        GameTextKey.PlayAgain -> Res.string.play_again
        GameTextKey.GameOverNewHighScore -> Res.string.game_over_new_high_score
        GameTextKey.GameMessageSelectColumn -> Res.string.game_message_select_column
        GameTextKey.GameMessageNoOpening -> Res.string.game_message_no_opening
        GameTextKey.GameMessageSoftLock -> Res.string.game_message_soft_lock
        GameTextKey.GameMessageOverflow -> Res.string.game_message_overflow
        GameTextKey.GameMessageSpecialChainBoard -> Res.string.game_message_special_chain_board
        GameTextKey.GameMessagePressureGameOver -> Res.string.game_message_pressure_game_over
        GameTextKey.GameMessageSpecialLines -> Res.string.game_message_special_lines
        GameTextKey.GameMessageSpecialTriggered -> Res.string.game_message_special_triggered
        GameTextKey.GameMessagePerfectDrop -> Res.string.game_message_perfect_drop
        GameTextKey.GameMessageAdRewardBlockWise -> Res.string.game_message_ad_reward_blockwise
        GameTextKey.GameMessageAdRewardMergeShift -> Res.string.game_message_ad_reward_mergeshift
        GameTextKey.GameMessageAdRewardBoomBlocks -> Res.string.game_message_ad_reward_boomblocks
        GameTextKey.GameMessageAdRewardStackShift -> Res.string.game_message_ad_reward_stackshift
        GameTextKey.GameMessageChainLines -> Res.string.game_message_chain_lines
        GameTextKey.GameMessageLinesCleared -> Res.string.game_message_lines_cleared
        GameTextKey.GameMessageGoodShot -> Res.string.game_message_good_shot
        GameTextKey.GameMessageHoldUpdated -> Res.string.game_message_hold_updated
        GameTextKey.GameMessageTempoCritical -> Res.string.game_message_tempo_critical
        GameTextKey.GameMessageTempoUp -> Res.string.game_message_tempo_up
        GameTextKey.GameMessageExtraLifeUsed -> Res.string.game_message_extra_life_used
        GameTextKey.FeedbackOverflow -> Res.string.feedback_overflow
        GameTextKey.FeedbackPerfectLane -> Res.string.feedback_perfect_lane
        GameTextKey.FeedbackMicroAdjust -> Res.string.feedback_micro_adjust
        GameTextKey.FeedbackSoftLock -> Res.string.feedback_soft_lock
        GameTextKey.FeedbackHoldArmed -> Res.string.feedback_hold_armed
        GameTextKey.FeedbackSwap -> Res.string.feedback_swap
        GameTextKey.FeedbackSpecialChain -> Res.string.feedback_special_chain
        GameTextKey.FeedbackSpecial -> Res.string.feedback_special
        GameTextKey.FeedbackPerfect -> Res.string.feedback_perfect
        GameTextKey.FeedbackChain -> Res.string.feedback_chain
        GameTextKey.FeedbackClear -> Res.string.feedback_clear
        GameTextKey.FeedbackScoreOnly -> Res.string.feedback_score_only
        GameTextKey.FeedbackExtraLife -> Res.string.feedback_extra_life
        GameTextKey.SpecialColumnClearer -> Res.string.special_column_clearer
        GameTextKey.SpecialRowClearer -> Res.string.special_row_clearer
        GameTextKey.SpecialGhost -> Res.string.special_ghost
        GameTextKey.SpecialHeavy -> Res.string.special_heavy
        GameTextKey.PiecePropertiesNone -> Res.string.piece_properties_none
        GameTextKey.FeedbackAdRewardBlockWise -> Res.string.feedback_ad_reward_blockwise
        GameTextKey.FeedbackAdRewardMergeShift -> Res.string.feedback_ad_reward_mergeshift
        GameTextKey.FeedbackAdRewardBoomBlocks -> Res.string.feedback_ad_reward_boomblocks
        GameTextKey.GameMessageWordShiftEnterWord -> Res.string.game_message_wordshift_enter_word
        GameTextKey.GameMessageWordShiftNotEnoughLetters -> Res.string.game_message_wordshift_not_enough_letters
        GameTextKey.GameMessageWordShiftNotInDictionary -> Res.string.game_message_wordshift_not_in_dictionary
        GameTextKey.GameMessageWordShiftKeepTrying -> Res.string.game_message_wordshift_keep_trying
        GameTextKey.GameMessageWordShiftSolved -> Res.string.game_message_wordshift_solved
        GameTextKey.GameMessageWordShiftFailed -> Res.string.game_message_wordshift_failed
        GameTextKey.GameMessageWordShiftRevived -> Res.string.game_message_wordshift_revived
    }
}

internal fun CellTone.stringResourceId(): StringResource {
    return when (this) {
        CellTone.Cyan -> Res.string.color_cyan
        CellTone.Gold -> Res.string.color_gold
        CellTone.Violet -> Res.string.color_violet
        CellTone.Emerald -> Res.string.color_emerald
        CellTone.Coral -> Res.string.color_coral
        CellTone.Blue -> Res.string.color_blue
        CellTone.Rose -> Res.string.color_rose
        CellTone.Lime -> Res.string.color_lime
        CellTone.Amber -> Res.string.color_amber
    }
}

@Composable
internal fun dialogContainerColor(
    uiColors: BlockGamesUiColors,
): Color {
    val settings = LocalAppSettings.current
    return if (isBlockGamesDarkTheme(settings)) {
        uiColors.panel.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.995f)
    }
}

@Composable
internal fun dialogMessageColor(
    uiColors: BlockGamesUiColors,
): Color {
    val settings = LocalAppSettings.current
    return if (isBlockGamesDarkTheme(settings)) {
        uiColors.subtitle
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
    }
}

@Composable
internal fun rememberBlockStylePulse(
    style: BlockVisualStyle,
    pulse: Float = 0f,
): Float {
    val needsPulse = style == BlockVisualStyle.DynamicLiquid ||
            style == BlockVisualStyle.Tornado ||
            style == BlockVisualStyle.Prism ||
            style == BlockVisualStyle.SoundWave ||
            style == BlockVisualStyle.Flame

    if (pulse != 0f || !needsPulse) {
        return pulse
    }

    val transition = rememberInfiniteTransition(label = "blockStyleButtonPulse")
    val animatedPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blockStyleButtonPulseValue",
    )
    return animatedPulse
}

internal data class ActionButtonColors(
    val container: Color,
    val content: Color,
)

@Composable
internal fun rememberActionButtonColors(
    tone: CellTone,
    enabled: Boolean,
    emphasized: Boolean,
): ActionButtonColors {
    val uiColors = BlockGamesThemeTokens.uiColors
    if (!enabled) {
        return ActionButtonColors(
            container = lerp(uiColors.actionButtonDisabled, uiColors.panelMuted, 0.16f),
            content = uiColors.actionIconDisabled,
        )
    }

    val (base, accent) = when (tone) {
        CellTone.Cyan, CellTone.Blue ->
            uiColors.actionPrimary to uiColors.selectionStackShift

        CellTone.Violet ->
            uiColors.actionSecondary to uiColors.selectionMergeShift

        CellTone.Emerald, CellTone.Lime ->
            uiColors.actionSuccess to uiColors.selectionBlockWise

        CellTone.Gold, CellTone.Amber ->
            uiColors.actionWarning to lerp(uiColors.selectionBlockWise, uiColors.actionWarning, 0.45f)

        CellTone.Coral, CellTone.Rose ->
            uiColors.actionDanger to uiColors.selectionBoomBlocks
    }
    val container = lerp(
        lerp(base, accent, if (emphasized) 0.16f else 0.08f),
        Color.White,
        if (emphasized) 0.04f else 0.015f,
    )
    return ActionButtonColors(
        container = container,
        content = actionButtonContentColorFor(container),
    )
}

private fun actionButtonContentColorFor(background: Color): Color =
    if (background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f > 0.62f) {
        Color(0xFF08111F)
    } else {
        Color.White
    }

@Composable
internal fun TopBarActionBlockButton(
    tone: CellTone,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    pulse: Float = 0f,
    size: Dp = TopBarActionBlockSize,
    showAdIcon: Boolean = false,
    adIcon: ImageVector = Icons.Outlined.Videocam,
    extraAlpha: Float = 1f,
) {
    val settings = LocalAppSettings.current
    val resolvedBlockStyle = settings.blockVisualStyle
    val launchCellCornerRadius = boardCellCornerRadiusDp(
        cellSize = size,
        style = resolvedBlockStyle,
    )
    val effectivePulse = rememberBlockStylePulse(
        style = resolvedBlockStyle,
        pulse = pulse,
    )
    val buttonColors = rememberActionButtonColors(
        tone = tone,
        enabled = enabled,
        emphasized = true,
    )
    val contentTint = blockStyleIconTint(
        style = resolvedBlockStyle,
        enabled = enabled,
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f * extraAlpha else 0.72f * extraAlpha }
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(launchCellCornerRadius),
                elevation = 5.dp,
            )
            .clip(RoundedCornerShape(launchCellCornerRadius))
            .clickable(enabled = enabled) { onClick.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasSize = this.size
            val cornerRadiusPx = launchCellCornerRadius.toPx()
            drawCellBody(
                baseColor = buttonColors.container,
                palette = settings.blockColorPalette,
                style = resolvedBlockStyle,
                topLeft = Offset.Zero,
                size = canvasSize,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                pulse = effectivePulse,
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentTint,
            modifier = Modifier.size(TopBarActionIconSize),
        )

        if (showAdIcon) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Icon(
                    imageVector = adIcon,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewDialogContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview
@Composable
private fun GameOverDialogPreview() {
    val settings = AppSettings()
    val logic = GameLogic.create()
    val gameState = logic.newGame(config = GameConfig.default(GameplayStyle.StackShift))
        .copy(score = 1250, status = GameStatus.GameOver)

    BlockGamesTheme(settings = settings) {
        PreviewDialogContainer {
            GameOverDialogContent(
                gameState = gameState,
                highestScore = 2500,
                showNewHighScoreMessage = false,
                canUseExtraLife = true,
                isExtraLifeLoading = false,
                showExtraLifeButton = true,
                onPlayAgain = {},
                onUseExtraLife = {},
            )
        }
    }
}

@Preview
@Composable
private fun NewHighScoreDialogPreview() {
    val settings = AppSettings()
    val logic = GameLogic.create()
    val gameState = logic.newGame(config = GameConfig.default(GameplayStyle.StackShift))
        .copy(score = 3200, status = GameStatus.GameOver)

    BlockGamesTheme(settings = settings) {
        PreviewDialogContainer {
            GameOverDialogContent(
                gameState = gameState,
                highestScore = 3200,
                showNewHighScoreMessage = true,
                canUseExtraLife = true,
                isExtraLifeLoading = false,
                showExtraLifeButton = true,
                onPlayAgain = {},
                onUseExtraLife = {},
            )
        }
    }
}

@Preview
@Composable
private fun RestartConfirmDialogPreview() {
    val settings = AppSettings()
    BlockGamesTheme(settings = settings) {
        PreviewDialogContainer {
            RestartConfirmDialog(
                onDismissRequest = {},
                title = stringResource(Res.string.restart_confirm_title),
                message = stringResource(Res.string.restart_confirm_body),
                confirmLabel = stringResource(Res.string.restart_confirm),
                dismissLabel = stringResource(Res.string.restart_cancel),
                onConfirm = {},
            )
        }
    }
}

@Preview
@Composable
private fun InteractiveOnboardingCompletionDialogPreview() {
    val settings = AppSettings()
    BlockGamesTheme(settings = settings) {
        PreviewDialogContainer {
            InteractiveOnboardingCompletionDialog(
                onStartGame = {},
                onReturnHome = {},
            )
        }
    }
}


@Preview
@Composable
private fun MinimalTopBarPreview() {
    val settings = AppSettings()
    val logic = GameLogic.create()
    val gameState = logic.newGame(config = GameConfig.default(GameplayStyle.StackShift))
        .copy(score = 450)

    BlockGamesTheme(settings = settings) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            MinimalTopBar(
                gameState = gameState,
                scoreHighlightStrengthProvider = { 0f },
                scoreHighlightScaleProvider = { 1f },
                remainingTimeLabel = stringResource(Res.string.time_remaining),
                onBack = {},
                onRestart = {},
            )
        }
    }
}
