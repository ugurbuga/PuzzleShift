package com.ugurbuga.blockgames.ui.game.selection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_chainshift
import blockgames.composeapp.generated.resources.app_title_blocksort
import blockgames.composeapp.generated.resources.app_title_blockwise
import blockgames.composeapp.generated.resources.app_title_boomblocks
import blockgames.composeapp.generated.resources.app_title_mergeshift
import blockgames.composeapp.generated.resources.app_title_stackshift
import blockgames.composeapp.generated.resources.home_play_cta
import blockgames.composeapp.generated.resources.selection_active_game_badge
import blockgames.composeapp.generated.resources.selection_chainshift_desc
import blockgames.composeapp.generated.resources.selection_blocksort_desc
import blockgames.composeapp.generated.resources.selection_blockwise_desc
import blockgames.composeapp.generated.resources.selection_boomblocks_desc
import blockgames.composeapp.generated.resources.selection_mergeshift_desc
import blockgames.composeapp.generated.resources.selection_stackshift_desc
import blockgames.composeapp.generated.resources.selection_title
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.AppFooterBannerHeight
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStage
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStage
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStateFactory
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStage
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStage
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStateFactory
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.StackShiftGameOnboardingStateFactory
import com.ugurbuga.blockgames.settings.StackShiftOnboardingStage
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.BlockStyleActionButton
import com.ugurbuga.blockgames.ui.game.BoardGrid
import com.ugurbuga.blockgames.ui.game.PieceBlocks
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.game.BlockSortBoard
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import com.ugurbuga.blockgames.ui.game.game.GameGrid as BoomBlocksGameGrid

private val SelectionContentMaxWidth = 600.dp
private const val SelectionDemoBoardHeightFractionWithTray = 0.78f
private const val SelectionDemoTrayYFraction = 1.11f
private const val SelectionDemoTrayPieceScale = 0.78f
private const val SelectionDemoActionCount = 6
private val SelectionDemoTraySlotCenters = listOf(0.18f, 0.50f, 0.82f)

private data class BoomBlocksDemoStep(
    val tapPoint: GridPoint,
    val nextState: GameState,
)

private data class BlockWiseDemoStep(
    val beforeState: GameState,
    val pieceId: Long,
    val target: GridPoint,
    val preview: PlacementPreview,
    val nextState: GameState,
)

private data class BlockWiseDemoScenario(
    val steps: List<BlockWiseDemoStep>,
)

private data class StackShiftDemoStep(
    val beforeState: GameState,
    val targetColumn: Int,
    val preview: PlacementPreview,
    val softLockedState: GameState,
    val committedState: GameState,
)

private data class StackShiftDemoScenario(
    val seed: Int,
    val steps: List<StackShiftDemoStep>,
)

private data class BoomBlocksDemoScenario(
    val initialState: GameState,
    val steps: List<BoomBlocksDemoStep>,
)

private data class BlockSortDemoStep(
    val beforeState: GameState,
    val sourceColumn: Int,
    val targetColumn: Int,
    val preview: PlacementPreview,
    val nextState: GameState,
)

private data class SelectionGameSpec(
    val style: GameplayStyle,
    val titleRes: StringResource,
    val descriptionRes: StringResource,
)

@Composable
fun AppSelectionScreen(
    currentStyle: GameplayStyle?,
    onGameplayStyleSelected: (GameplayStyle) -> Unit,
    telemetry: AppTelemetry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    LogScreen(telemetry, TelemetryScreenNames.Selection)
    val items = remember {
        listOf(
            SelectionGameSpec(GameplayStyle.StackShift, Res.string.app_title_stackshift, Res.string.selection_stackshift_desc),
            SelectionGameSpec(GameplayStyle.BlockWise, Res.string.app_title_blockwise, Res.string.selection_blockwise_desc),
            SelectionGameSpec(GameplayStyle.ChainShift, Res.string.app_title_chainshift, Res.string.selection_chainshift_desc),
            SelectionGameSpec(GameplayStyle.BlockSort, Res.string.app_title_blocksort, Res.string.selection_blocksort_desc),
            SelectionGameSpec(GameplayStyle.MergeShift, Res.string.app_title_mergeshift, Res.string.selection_mergeshift_desc),
            SelectionGameSpec(GameplayStyle.BoomBlocks, Res.string.app_title_boomblocks, Res.string.selection_boomblocks_desc),
        )
    }

    val listState = rememberLazyListState()
    val expandedStyleState = remember(currentStyle) { mutableStateOf(initialExpandedSelectionStyle(currentStyle)) }
    val density = LocalDensity.current
    val footerClearance = AppFooterBannerHeight + with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    } + 20.dp

    val transition = rememberInfiniteTransition(label = "selectionStylePulse")
    val stylePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stylePulse",
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .statusBarsPadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = footerClearance + 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopBarActionBlockButton(
                            tone = CellTone.Cyan,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.selection_title),
                            onClick = onBack,
                            size = 44.dp,
                            pulse = stylePulse,
                        )
                        Text(
                            text = stringResource(Res.string.selection_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }

                itemsIndexed(items) { _, item ->
                    val style = item.style
                    val isExpanded = expandedStyleState.value == style
                    SelectionItem(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .widthIn(max = SelectionContentMaxWidth),
                        title = stringResource(item.titleRes),
                        description = stringResource(item.descriptionRes),
                        tone = style.selectionTone(),
                        style = style,
                        isExpanded = isExpanded,
                        isActive = style == currentStyle,
                        footerClearance = footerClearance,
                        onExpandToggle = {
                            expandedStyleState.value = if (isExpanded) null else style
                        },
                        onPlayClick = { onGameplayStyleSelected(style) },
                        stylePulse = stylePulse,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionItem(
    title: String,
    description: String,
    tone: CellTone,
    style: GameplayStyle,
    isExpanded: Boolean,
    isActive: Boolean,
    footerClearance: Dp,
    onExpandToggle: () -> Unit,
    onPlayClick: () -> Unit,
    stylePulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val accentColor = remember(style, uiColors) { uiColors.selectionAccentFor(style) }
    val cardInteractionSource = remember { MutableInteractionSource() }
    val itemBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isExpanded, footerClearance) {
        if (isExpanded) {
            delay(250)
            itemBringIntoViewRequester.bringIntoView()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(itemBringIntoViewRequester)
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = if (isExpanded) 12.dp else 8.dp,
            )
            .clip(RoundedCornerShape(GameUiShapeTokens.panelCorner))
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onExpandToggle,
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                lerp(uiColors.panel, accentColor.copy(alpha = 0.18f), 0.14f)
            } else {
                lerp(uiColors.gameSurface.copy(alpha = 0.90f), accentColor.copy(alpha = 0.10f), 0.10f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isExpanded) {
            BorderStroke(1.dp, accentColor.copy(alpha = 0.52f))
        } else if (isActive) {
            BorderStroke(1.dp, accentColor.copy(alpha = 0.34f))
        } else null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                contentColor = accentColor,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
                            ) {
                                Text(
                                    text = stringResource(Res.string.selection_active_game_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = uiColors.subtitle,
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
                            .background(lerp(uiColors.gameSurface.copy(alpha = 0.56f), accentColor.copy(alpha = 0.10f), 0.14f))
                            .border(1.dp, accentColor.copy(alpha = 0.26f), RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
                            .padding(8.dp)
                    ) {
                        GameDemoView(style = style, stylePulse = stylePulse)
                    }

                    BlockStyleActionButton(
                        text = stringResource(Res.string.home_play_cta),
                        onClick = onPlayClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        tone = tone,
                        pulse = stylePulse,
                        icon = Icons.Default.PlayArrow,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameDemoView(style: GameplayStyle, stylePulse: Float) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val boardStyle = remember(settings.blockVisualStyle) {
        settings.blockVisualStyle
    }
    val stackShiftScenario = remember(style) {
        if (style == GameplayStyle.StackShift) buildStackShiftDemoScenario() else null
    }
    val blockWiseScenario = remember(style) {
        if (style == GameplayStyle.BlockWise) buildBlockWiseDemoScenario() else null
    }
    val blockSortScenario = remember(style) {
        if (style == GameplayStyle.BlockSort) buildBlockSortDemoScenario() else emptyList()
    }
    val chainShiftScene = remember(style) {
        if (style == GameplayStyle.ChainShift) {
            ChainShiftOnboardingStateFactory.scene(ChainShiftOnboardingStage.CreateMatch)
        } else null
    }
    val logic = remember(style, stackShiftScenario?.seed) {
        com.ugurbuga.blockgames.game.logic.GameLogic.create(
            random = Random(stackShiftScenario?.seed ?: (style.ordinal + 17)),
        )
    }
    val initialSceneState = remember(style) {
        when (style) {
            GameplayStyle.StackShift -> stackShiftScenario?.steps?.firstOrNull()?.beforeState
                ?: StackShiftGameOnboardingStateFactory.scene(StackShiftOnboardingStage.LineClear).gameState
            GameplayStyle.BlockWise -> blockWiseScenario?.steps?.firstOrNull()?.beforeState
                ?: BlockWiseOnboardingStateFactory.scene(BlockWiseOnboardingStage.CrossClear).gameState
            GameplayStyle.ChainShift -> chainShiftScene?.gameState
                ?: ChainShiftOnboardingStateFactory.scene(ChainShiftOnboardingStage.CreateMatch).gameState
            GameplayStyle.BlockSort -> blockSortScenario.firstOrNull()?.beforeState
                ?: BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.PickSource).gameState
            GameplayStyle.MergeShift -> MergeShiftOnboardingStateFactory.scene(MergeShiftOnboardingStage.VerticalMerge).gameState
            GameplayStyle.BoomBlocks -> BoomBlocksOnboardingStateFactory.scene(BoomBlocksOnboardingStage.BasicExplosion).gameState
        }
    }
    val boomBlocksScenario = remember(style) {
        if (style == GameplayStyle.BoomBlocks) buildBoomBlocksDemoScenario() else null
    }

    var gameState by remember(style) { mutableStateOf(initialSceneState) }
    val animatedX = remember(style) { Animatable(0.5f) }
    val animatedY = remember(style) { Animatable(1.1f) }
    var activeDemoPiece by remember(style) { mutableStateOf<Piece?>(null) }
    var activePreview by remember(style) { mutableStateOf<PlacementPreview?>(null) }
    var impactedPreviewCells by remember(style) { mutableStateOf(emptySet<GridPoint>()) }
    var settledDemoPiece by remember(style) { mutableStateOf<Piece?>(null) }
    var settledPreview by remember(style) { mutableStateOf<PlacementPreview?>(null) }
    var blockSortSelectedSource by remember(style) { mutableStateOf<Int?>(null) }
    var isBoomTapVisible by remember(style) { mutableStateOf(false) }
    val boomTapScale = remember(style) { Animatable(0f) }

    LaunchedEffect(style) {
        while (true) {
            gameState = boomBlocksScenario?.initialState ?: initialSceneState
            activeDemoPiece = null
            activePreview = null
            impactedPreviewCells = emptySet()
            settledDemoPiece = null
            settledPreview = null
            blockSortSelectedSource = null
            isBoomTapVisible = false
            delay(1000)

            when (style) {
                GameplayStyle.StackShift -> {
                    stackShiftScenario?.steps
                        ?.take(SelectionDemoActionCount)
                        ?.forEachIndexed { stepIndex, step ->
                        val currentState = step.beforeState
                        val currentConfig = currentState.config
                        val piece = currentState.activePiece ?: return@forEachIndexed
                        if (stepIndex == 0 || gameState != currentState) {
                            gameState = currentState
                            settledDemoPiece = null
                            settledPreview = null
                        }
                        activeDemoPiece = piece
                        activePreview = step.preview
                        impactedPreviewCells = logic.previewImpactPoints(currentState, step.preview)

                        animatedX.snapTo(demoTrayLeftFraction(piece, slotIndex = 0, columns = currentConfig.columns))
                        animatedY.snapTo(demoTrayTopFraction(piece, rows = currentConfig.rows))
                        delay(400)

                        animatedX.animateTo(
                            step.preview.landingAnchor.column.toFloat() / currentConfig.columns.toFloat(),
                            tween(600, easing = FastOutSlowInEasing),
                        )
                        delay(150)
                        animatedY.animateTo(
                            step.preview.landingAnchor.row.toFloat() / currentConfig.rows.toFloat(),
                            tween(320, easing = FastOutSlowInEasing),
                        )

                        activeDemoPiece = null
                        activePreview = null
                        impactedPreviewCells = emptySet()
                        settledDemoPiece = piece
                        settledPreview = step.preview
                        gameState = step.softLockedState
                        delay(320)

                        gameState = step.committedState
                        settledDemoPiece = null
                        settledPreview = null
                        delay(1200)
                    }
                }

                GameplayStyle.BlockWise -> {
                    val scenarioSteps = blockWiseScenario?.steps.orEmpty()
                    if (scenarioSteps.isNotEmpty()) {
                        scenarioSteps.take(SelectionDemoActionCount).forEachIndexed { stepIndex, step ->
                            val currentState = step.beforeState
                            val currentConfig = currentState.config
                            val piece = currentState.trayPieces.firstOrNull { it.id == step.pieceId } ?: return@forEachIndexed
                            if (stepIndex == 0 || gameState != currentState) {
                                gameState = currentState
                            }
                            activeDemoPiece = null
                            activePreview = null
                            impactedPreviewCells = emptySet()
                            delay(if (stepIndex == 0) 300 else 500)

                            activeDemoPiece = piece
                            activePreview = step.preview
                            impactedPreviewCells = logic.previewImpactPoints(currentState, step.preview)

                            val traySlotIndex = currentState.trayPieces.indexOfFirst { it.id == piece.id }.coerceAtLeast(0)
                            animatedX.snapTo(demoTrayLeftFraction(piece, slotIndex = traySlotIndex, columns = currentConfig.columns))
                            animatedY.snapTo(demoTrayTopFraction(piece, rows = currentConfig.rows))
                            delay(400)

                            launch { animatedX.animateTo(step.target.column.toFloat() / currentConfig.columns.toFloat(), tween(800)) }
                            launch { animatedY.animateTo(step.target.row.toFloat() / currentConfig.rows.toFloat(), tween(800)) }
                            delay(900)

                            gameState = step.nextState
                            activeDemoPiece = null
                            activePreview = null
                            impactedPreviewCells = emptySet()
                            delay(1200)
                        }
                    } else {
                        repeat(SelectionDemoActionCount) {
                            val currentConfig = gameState.config
                            val piece = gameState.trayPieces.firstOrNull() ?: return@repeat
                            val (target, preview) = findPreferredGridPlacement(
                                state = gameState,
                                pieceId = piece.id,
                                previewProvider = logic::previewPlacement,
                            ) ?: return@repeat
                            activeDemoPiece = piece
                            activePreview = preview
                            impactedPreviewCells = logic.previewImpactPoints(gameState, preview)

                            animatedX.snapTo(demoTrayLeftFraction(piece, slotIndex = 0, columns = currentConfig.columns))
                            animatedY.snapTo(demoTrayTopFraction(piece, rows = currentConfig.rows))
                            delay(400)

                            launch { animatedX.animateTo(target.column.toFloat() / currentConfig.columns.toFloat(), tween(800)) }
                            launch { animatedY.animateTo(target.row.toFloat() / currentConfig.rows.toFloat(), tween(800)) }
                            delay(900)

                            val result = logic.placePiece(gameState, piece.id, target)
                            gameState = result.state
                            activeDemoPiece = null
                            activePreview = null
                            impactedPreviewCells = emptySet()
                            delay(1200)
                        }
                    }
                }

                GameplayStyle.BlockSort -> {
                    val scenarioSteps = blockSortScenario
                    if (scenarioSteps.isNotEmpty()) {
                        scenarioSteps.take(SelectionDemoActionCount).forEachIndexed { stepIndex, step ->
                            if (stepIndex == 0 || gameState != step.beforeState) {
                                gameState = step.beforeState
                            }
                            blockSortSelectedSource = step.sourceColumn
                            delay(if (stepIndex == 0) 500 else 700)
                            gameState = step.nextState
                            blockSortSelectedSource = null
                            delay(1200)
                        }
                    }
                }

                GameplayStyle.ChainShift -> {
                    repeat(SelectionDemoActionCount) {
                        val currentConfig = gameState.config
                        val piece = gameState.activePiece ?: return@repeat
                        val (target, preview) = findPreferredGridPlacement(
                            state = gameState,
                            pieceId = piece.id,
                            previewProvider = logic::previewPlacement,
                        ) ?: return@repeat
                        activeDemoPiece = piece
                        activePreview = preview
                        impactedPreviewCells = logic.previewImpactPoints(gameState, preview)

                        animatedX.snapTo(demoTrayLeftFraction(piece = piece, slotIndex = 0, columns = currentConfig.columns))
                        animatedY.snapTo(demoTrayTopFraction(piece = piece, rows = currentConfig.rows))
                        delay(400)

                        launch { animatedX.animateTo(target.column.toFloat() / currentConfig.columns.toFloat(), tween(700)) }
                        launch { animatedY.animateTo(target.row.toFloat() / currentConfig.rows.toFloat(), tween(700)) }
                        delay(850)

                        val result = logic.placePiece(gameState, piece.id, target)
                        gameState = result.state
                        activeDemoPiece = null
                        activePreview = null
                        impactedPreviewCells = emptySet()
                        delay(1200)
                    }
                }

                GameplayStyle.MergeShift -> {
                    repeat(SelectionDemoActionCount) { stepIndex ->
                        val currentConfig = gameState.config
                        val piece = gameState.activePiece ?: return@repeat
                        val (targetCol, preview) = findPreferredColumnPlacement(
                            state = gameState,
                            preferredColumns = mergeShiftPreferredColumnsForStep(stepIndex),
                            previewProvider = logic::previewPlacement,
                        ) ?: return@repeat
                        activeDemoPiece = piece
                        activePreview = preview
                        impactedPreviewCells = logic.previewImpactPoints(gameState, preview)

                        animatedX.snapTo(demoTrayLeftFraction(piece, slotIndex = 0, columns = currentConfig.columns))
                        animatedY.snapTo(demoTrayTopFraction(piece, rows = currentConfig.rows))
                        delay(400)

                        animatedX.animateTo(
                            preview.landingAnchor.column.toFloat() / currentConfig.columns.toFloat(),
                            tween(500),
                        )
                        delay(150)
                        animatedY.animateTo(
                            preview.landingAnchor.row.toFloat() / currentConfig.rows.toFloat(),
                            tween(280),
                        )

                        val result = logic.placePiece(gameState, targetCol)
                        gameState = result.state
                        activeDemoPiece = null
                        activePreview = null
                        impactedPreviewCells = emptySet()

                        // Resolve merges
                        repeat(2) {
                            delay(400)
                            gameState = logic.tick(gameState)
                        }
                        delay(1200)
                    }
                }

                GameplayStyle.BoomBlocks -> {
                    boomBlocksScenario?.steps?.take(SelectionDemoActionCount)?.forEach { step ->
                        val currentConfig = gameState.config
                        val pt = step.tapPoint
                        delay(800)
                        animatedX.snapTo((pt.column).toFloat() / currentConfig.columns.toFloat())
                        animatedY.snapTo((pt.row).toFloat() / currentConfig.rows.toFloat())

                        isBoomTapVisible = true
                        boomTapScale.snapTo(0f)
                        boomTapScale.animateTo(1f, tween(300))
                        delay(100)

                        gameState = step.nextState

                        delay(200)
                        isBoomTapVisible = false
                        delay(1400)
                    }
                }
            }
            delay(2000)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val columns = gameState.config.columns
        val rows = gameState.config.rows
        val availableBoardHeight = if (style == GameplayStyle.BoomBlocks || style == GameplayStyle.BlockSort) {
            maxHeight
        } else {
            maxHeight * SelectionDemoBoardHeightFractionWithTray
        }
        val boardWidth = minOf(maxWidth, availableBoardHeight * (columns.toFloat() / rows.toFloat()))
        val boardHeight = boardWidth * (rows.toFloat() / columns.toFloat())
        val boardOffsetX = (maxWidth - boardWidth) / 2

        val cellWidth = boardWidth / columns
        val cellHeight = boardHeight / rows
        val cellSize = minOf(cellWidth, cellHeight)

        val actualBoardWidth = cellSize * columns
        val actualBoardHeight = cellSize * rows

        Box(modifier = Modifier.fillMaxSize()) {
            if (style == GameplayStyle.BoomBlocks) {
                BoomBlocksGameGrid(
                    gameState = gameState,
                    modifier = Modifier
                        .offset(x = boardOffsetX)
                        .size(actualBoardWidth, actualBoardHeight),
                    hintEnabled = false,
                    hintPhase = 0f,
                    stylePulse = stylePulse,
                )
            } else if (style == GameplayStyle.BlockSort) {
                BlockSortBoard(
                    gameState = gameState,
                    selectedSourceColumn = blockSortSelectedSource,
                    interactiveOnboardingScene = null,
                    onRequestPreview = { source, target ->
                        if (blockSortSelectedSource != source) {
                            null
                        } else {
                            logic.previewPlacement(gameState, source.toLong(), GridPoint(target, 0))
                        }
                    },
                    onColumnTapped = {},
                    palette = settings.blockColorPalette,
                    boardStylePulse = stylePulse,
                    boardStyle = boardStyle,
                    modifier = Modifier
                        .offset(x = boardOffsetX)
                        .size(actualBoardWidth, actualBoardHeight),
                )
            } else {
                BoardGrid(
                    modifier = Modifier
                        .offset(x = boardOffsetX)
                        .size(actualBoardWidth, actualBoardHeight),
                    gameState = gameState,
                    preview = activePreview,
                    impactedPreviewCells = impactedPreviewCells,
                    activeColumn = activePreview?.selectedColumn,
                    activePiece = activeDemoPiece,
                    isDragging = activeDemoPiece != null,
                    stylePulse = stylePulse,
                )
                DemoTrayPieces(
                    pieces = gameState.trayPieces,
                    maxVisiblePieces = if (style == GameplayStyle.MergeShift) 1 else SelectionDemoTraySlotCenters.size,
                    activePieceId = activeDemoPiece?.id,
                    boardOffsetX = boardOffsetX,
                    boardWidth = actualBoardWidth,
                    boardHeight = actualBoardHeight,
                    cellSize = cellSize * SelectionDemoTrayPieceScale,
                )
            }

            // Animated piece overlay
            activeDemoPiece?.takeUnless { style == GameplayStyle.StackShift && gameState.softLock != null }?.let { piece ->
                val offsetX = boardOffsetX + (actualBoardWidth * animatedX.value)
                val offsetY = actualBoardHeight * animatedY.value

                PieceBlocks(
                    piece = piece,
                    cellSize = cellSize,
                    modifier = Modifier.offset(offsetX, offsetY)
                )
            }

            settledDemoPiece?.let { piece ->
                val preview = settledPreview ?: return@let
                val offsetX = boardOffsetX + (cellSize * preview.landingAnchor.column)
                val offsetY = cellSize * preview.landingAnchor.row

                PieceBlocks(
                    piece = piece,
                    cellSize = cellSize,
                    modifier = Modifier.offset(offsetX, offsetY),
                )
            }

            // Tap indicator for BoomBlocks
            if (isBoomTapVisible) {
                val tapAccent = uiColors.selectionAccentFor(GameplayStyle.BoomBlocks)
                val tapX = boardOffsetX + (actualBoardWidth * (animatedX.value + 0.5f / columns))
                val tapY = actualBoardHeight * (animatedY.value + 0.5f / rows)
                Box(
                    modifier = Modifier
                        .offset(tapX, tapY)
                        .size(cellSize * 1.2f)
                        .offset(-(cellSize * 0.6f), -(cellSize * 0.6f))
                        .graphicsLayer {
                            scaleX = boomTapScale.value
                            scaleY = boomTapScale.value
                            alpha = 1f - (boomTapScale.value * 0.5f)
                        }
                        .background(tapAccent.copy(alpha = 0.34f), CircleShape)
                        .border(2.dp, tapAccent.copy(alpha = 0.84f), CircleShape)
                )
            }
        }
    }
}

internal fun GameplayStyle.selectionTone(): CellTone = when (this) {
    GameplayStyle.StackShift -> CellTone.Cyan
    GameplayStyle.BlockWise -> CellTone.Amber
    GameplayStyle.ChainShift -> CellTone.Blue
    GameplayStyle.BlockSort -> CellTone.Emerald
    GameplayStyle.MergeShift -> CellTone.Violet
    GameplayStyle.BoomBlocks -> CellTone.Coral
}

private fun com.ugurbuga.blockgames.ui.theme.BlockGamesUiColors.selectionAccentFor(style: GameplayStyle): Color = when (style) {
    GameplayStyle.StackShift -> selectionStackShift
    GameplayStyle.BlockWise -> warning
    GameplayStyle.ChainShift -> selectionMergeShift
    GameplayStyle.BlockSort -> success
    GameplayStyle.MergeShift -> selectionMergeShift
    GameplayStyle.BoomBlocks -> danger
}

internal fun initialExpandedSelectionStyle(currentStyle: GameplayStyle?): GameplayStyle? =
    if (currentStyle == null) GameplayStyle.StackShift else null

private fun buildBlockSortDemoScenario(): List<BlockSortDemoStep> {
    val logic = com.ugurbuga.blockgames.game.logic.GameLogic.create(random = Random(0))
    return listOf(
        BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.PickSource),
        BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.MatchColor),
        BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.FinishColumn),
    ).mapNotNull { scene ->
        val targetColumn = scene.acceptedTargetColumns.firstOrNull() ?: return@mapNotNull null
        val preview = logic.previewPlacement(
            scene.gameState,
            scene.guideSourceColumn.toLong(),
            GridPoint(targetColumn, 0),
        ) ?: return@mapNotNull null
        val nextState = logic.placePiece(
            scene.gameState,
            scene.guideSourceColumn.toLong(),
            GridPoint(targetColumn, 0),
        ).state
        BlockSortDemoStep(
            beforeState = scene.gameState,
            sourceColumn = scene.guideSourceColumn,
            targetColumn = targetColumn,
            preview = preview,
            nextState = nextState,
        )
    }
}

@Composable
private fun DemoTrayPieces(
    pieces: List<Piece>,
    maxVisiblePieces: Int,
    activePieceId: Long?,
    boardOffsetX: androidx.compose.ui.unit.Dp,
    boardWidth: androidx.compose.ui.unit.Dp,
    boardHeight: androidx.compose.ui.unit.Dp,
    cellSize: androidx.compose.ui.unit.Dp,
) {
    pieces.take(maxVisiblePieces).forEachIndexed { index, piece ->
        val slotCenter = SelectionDemoTraySlotCenters[index]
        val pieceWidth = cellSize * piece.width
        val pieceHeight = cellSize * piece.height
        val left = boardOffsetX + (boardWidth * slotCenter) - (pieceWidth / 2)
        val top = (boardHeight * SelectionDemoTrayYFraction) - (pieceHeight / 2)

        Box(
            modifier = Modifier
                .offset(x = left, y = top)
                .size(
                    width = pieceWidth + 10.dp,
                    height = pieceHeight + 10.dp,
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PieceBlocks(
                piece = piece,
                cellSize = cellSize,
                alpha = if (piece.id == activePieceId) 0.30f else 0.92f,
            )
        }
    }
}

internal fun findPreferredColumnPlacement(
    state: GameState,
    preferredColumns: List<Int>,
    previewProvider: (GameState, Int) -> PlacementPreview?,
): Pair<Int, PlacementPreview>? {
    val piece = state.activePiece ?: return null
    val maxColumn = state.config.columns - piece.width
    if (maxColumn < 0) return null

    val candidateColumns = buildList {
        addAll(preferredColumns)
        addAll(0..maxColumn)
    }.distinct()

    return candidateColumns
        .filter { it in 0..maxColumn }
        .firstNotNullOfOrNull { column ->
            previewProvider(state, column)?.let { column to it }
        }
}

internal fun mergeShiftPreferredColumnsForStep(stepIndex: Int): List<Int> = when (stepIndex % 6) {
    0 -> listOf(0, 2, 1)
    1 -> listOf(2, 0, 1)
    2 -> listOf(1, 0, 2)
    3 -> listOf(0, 1, 2)
    4 -> listOf(2, 1, 0)
    else -> listOf(1, 2, 0)
}

private fun findPreferredGridPlacement(
    state: GameState,
    pieceId: Long,
    previewProvider: (GameState, Long, GridPoint) -> PlacementPreview?,
): Pair<GridPoint, PlacementPreview>? {
    val piece = state.trayPieces.firstOrNull { it.id == pieceId } ?: return null
    val maxColumn = state.config.columns - piece.width
    val maxRow = state.config.rows - piece.height
    if (maxColumn < 0 || maxRow < 0) return null

    val centerColumn = state.config.columns / 2f
    val centerRow = state.config.rows / 2f
    val candidatePoints = buildList {
        for (row in 0..maxRow) {
            for (column in 0..maxColumn) {
                add(GridPoint(column, row))
            }
        }
    }.sortedBy { point ->
        val dx = kotlin.math.abs(point.column - centerColumn)
        val dy = kotlin.math.abs(point.row - centerRow)
        (dy * 10f) + dx
    }

    return candidatePoints.firstNotNullOfOrNull { point ->
        previewProvider(state, pieceId, point)?.let { point to it }
    }
}

private data class GridPlacementCandidate(
    val piece: Piece,
    val target: GridPoint,
    val preview: PlacementPreview,
)

private fun findAllGridPlacements(
    state: GameState,
    previewProvider: (GameState, Long, GridPoint) -> PlacementPreview?,
): List<GridPlacementCandidate> = buildList {
    state.trayPieces.forEach { piece ->
        val maxColumn = state.config.columns - piece.width
        val maxRow = state.config.rows - piece.height
        if (maxColumn < 0 || maxRow < 0) return@forEach
        for (row in 0..maxRow) {
            for (column in 0..maxColumn) {
                val target = GridPoint(column, row)
                val preview = previewProvider(state, piece.id, target) ?: continue
                add(
                    GridPlacementCandidate(
                        piece = piece,
                        target = target,
                        preview = preview,
                    ),
                )
            }
        }
    }
}

private fun findAllColumnPlacements(
    state: GameState,
    previewProvider: (GameState, Int) -> PlacementPreview?,
): List<Pair<Int, PlacementPreview>> {
    val piece = state.activePiece ?: return emptyList()
    val maxColumn = state.config.columns - piece.width
    if (maxColumn < 0) return emptyList()
    return (0..maxColumn).mapNotNull { column ->
        previewProvider(state, column)?.let { column to it }
    }
}

internal fun demoTrayLeftFraction(
    piece: Piece,
    slotIndex: Int,
    columns: Int,
): Float {
    val slotCenter = SelectionDemoTraySlotCenters.getOrElse(slotIndex) { SelectionDemoTraySlotCenters.last() }
    val halfPieceWidth = piece.width / (columns.toFloat() * 2f)
    return slotCenter - halfPieceWidth
}

internal fun demoTrayTopFraction(
    piece: Piece,
    rows: Int,
): Float {
    val halfPieceHeight = piece.height / (rows.toFloat() * 2f)
    return SelectionDemoTrayYFraction - halfPieceHeight
}

private fun buildBoomBlocksDemoScenario(): BoomBlocksDemoScenario {
    val config = GameConfig(
        columns = 6,
        rows = 8,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    for (seed in 0..256) {
        val logic = com.ugurbuga.blockgames.game.logic.BoomBlocksGameLogic(random = Random(seed))
        val initialState = logic.newGame(config = config)
        var currentState = initialState
        val steps = mutableListOf<BoomBlocksDemoStep>()

        while (steps.size < SelectionDemoActionCount) {
            val tapPoint = findAnyExplodablePoint(currentState.board) ?: break
            val nextState = logic.placePiece(currentState, 0L, tapPoint).state
            steps += BoomBlocksDemoStep(
                tapPoint = tapPoint,
                nextState = nextState,
            )
            currentState = nextState
        }

        if (steps.size >= SelectionDemoActionCount) {
            return BoomBlocksDemoScenario(
                initialState = initialState,
                steps = steps,
            )
        }
    }

    val fallbackState = BoomBlocksOnboardingStateFactory
        .scene(BoomBlocksOnboardingStage.StrategicClears)
        .gameState
    val fallbackLogic = com.ugurbuga.blockgames.game.logic.BoomBlocksGameLogic(random = Random(0))
    var currentState = fallbackState
    val fallbackSteps = mutableListOf<BoomBlocksDemoStep>()

    while (fallbackSteps.size < SelectionDemoActionCount) {
        val tapPoint = findAnyExplodablePoint(currentState.board) ?: break
        val nextState = fallbackLogic.placePiece(currentState, 0L, tapPoint).state
        fallbackSteps += BoomBlocksDemoStep(
            tapPoint = tapPoint,
            nextState = nextState,
        )
        currentState = nextState
    }

    return BoomBlocksDemoScenario(
        initialState = fallbackState,
        steps = fallbackSteps,
    )
}

private fun buildBlockWiseDemoScenario(): BlockWiseDemoScenario {
    val denseBaseState = BlockWiseOnboardingStateFactory.scene(BlockWiseOnboardingStage.CrossClear).gameState
        .copy(
            board = BlockWiseOnboardingStateFactory.scene(BlockWiseOnboardingStage.CrossClear).gameState.board
                .fill(
                    points = listOf(
                        GridPoint(0, 3),
                        GridPoint(1, 3),
                        GridPoint(6, 2),
                        GridPoint(7, 2),
                        GridPoint(1, 8),
                        GridPoint(6, 8),
                    ),
                    tone = CellTone.Coral,
                )
                .fill(
                    points = listOf(
                        GridPoint(5, 1),
                        GridPoint(5, 2),
                    ),
                    tone = CellTone.Gold,
                ),
        )

    for (seed in 0..768) {
        val logic = com.ugurbuga.blockgames.game.logic.GameLogic.create(random = Random(seed))
        var state = denseBaseState
        val steps = mutableListOf<BlockWiseDemoStep>()
        var sawRowClear = false
        var sawColumnClear = false

        repeat(SelectionDemoActionCount) { stepIndex ->
            val candidates = findAllGridPlacements(state, logic::previewPlacement)
            if (candidates.isEmpty()) return@repeat

            val selected = candidates.maxByOrNull { candidate ->
                scoreBlockWiseDemoCandidate(
                    candidate = candidate,
                    state = state,
                    stepIndex = stepIndex,
                    sawRowClear = sawRowClear,
                    sawColumnClear = sawColumnClear,
                )
            } ?: return@repeat

            val result = logic.placePiece(state, selected.piece.id, selected.target)
            steps += BlockWiseDemoStep(
                beforeState = state,
                pieceId = selected.piece.id,
                target = selected.target,
                preview = selected.preview,
                nextState = result.state,
            )
            sawRowClear = sawRowClear || selected.preview.clearedRows.isNotEmpty()
            sawColumnClear = sawColumnClear || selected.preview.clearedColumns.isNotEmpty()
            state = result.state
        }

        if (steps.size >= SelectionDemoActionCount && sawRowClear && sawColumnClear) {
            return BlockWiseDemoScenario(steps = steps)
        }
    }

    val fallbackStages = listOf(
        BlockWiseOnboardingStage.CrossClear,
        BlockWiseOnboardingStage.LineClear,
        BlockWiseOnboardingStage.ColumnClear,
    )
    val fallbackSteps = fallbackStages.mapNotNull { stage ->
        val scene = BlockWiseOnboardingStateFactory.scene(stage)
        val guidePoint = scene.guidePoint ?: return@mapNotNull null
        val piece = scene.gameState.trayPieces.firstOrNull() ?: return@mapNotNull null
        val preview = com.ugurbuga.blockgames.game.logic.GameLogic.create(random = Random(stage.ordinal + 1))
            .previewPlacement(scene.gameState, piece.id, guidePoint)
            ?: return@mapNotNull null
        val nextState = com.ugurbuga.blockgames.game.logic.GameLogic.create(random = Random(stage.ordinal + 1))
            .placePiece(scene.gameState, piece.id, guidePoint)
            .state
        BlockWiseDemoStep(
            beforeState = scene.gameState,
            pieceId = piece.id,
            target = guidePoint,
            preview = preview,
            nextState = nextState,
        )
    }
    return BlockWiseDemoScenario(steps = fallbackSteps)
}

private fun buildStackShiftDemoScenario(): StackShiftDemoScenario {
    val baseScene = StackShiftGameOnboardingStateFactory.scene(StackShiftOnboardingStage.LineClear)
    val initialState = baseScene.gameState.copy(
        board = baseScene.gameState.board
            .fill(
                points = listOf(
                    GridPoint(0, 1),
                    GridPoint(1, 1),
                    GridPoint(8, 1),
                    GridPoint(9, 1),
                    GridPoint(2, 2),
                    GridPoint(7, 2),
                ),
                tone = CellTone.Coral,
            )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(9, 3),
                ),
                tone = CellTone.Gold,
            ),
    )

    for (seed in 0..1024) {
        val logic = com.ugurbuga.blockgames.game.logic.GameLogic.create(random = Random(seed))
        var state = initialState
        val steps = mutableListOf<StackShiftDemoStep>()

        repeat(SelectionDemoActionCount) { stepIndex ->
            val placements = findAllColumnPlacements(state, logic::previewPlacement)
            if (placements.isEmpty()) return@repeat

            val selected = placements
                .mapNotNull { (targetColumn, preview) ->
                    val softLocked = logic.placePiece(state, targetColumn)
                    val committed = logic.commitSoftLock(softLocked.state)
                    if (committed.state.status != com.ugurbuga.blockgames.game.model.GameStatus.Running && stepIndex < SelectionDemoActionCount - 1) {
                        null
                    } else {
                        StackShiftScenarioCandidate(
                            targetColumn = targetColumn,
                            preview = preview,
                            softLockedState = softLocked.state,
                            committedState = committed.state,
                            commitEvents = committed.events,
                        )
                    }
                }
                .maxByOrNull { candidate ->
                    scoreStackShiftDemoCandidate(
                        candidate = candidate,
                        stepIndex = stepIndex,
                        previousStep = steps.lastOrNull(),
                    )
                }
                ?: return@repeat

            steps += StackShiftDemoStep(
                beforeState = state,
                targetColumn = selected.targetColumn,
                preview = selected.preview,
                softLockedState = selected.softLockedState,
                committedState = selected.committedState,
            )
            state = selected.committedState
        }

        val secondStep = steps.getOrNull(1)
        val secondStepIsElevatedRowClearer = secondStep != null &&
            secondStep.beforeState.activePiece?.special == com.ugurbuga.blockgames.game.model.SpecialBlockType.RowClearer &&
            secondStep.committedState.clearAnimationToken > secondStep.beforeState.clearAnimationToken &&
            secondStep.preview.landingAnchor.row < (steps.firstOrNull()?.preview?.landingAnchor?.row ?: Int.MAX_VALUE)

        if (steps.size >= SelectionDemoActionCount && secondStepIsElevatedRowClearer) {
            return StackShiftDemoScenario(
                seed = seed,
                steps = steps,
            )
        }
    }

    val fallbackLogic = com.ugurbuga.blockgames.game.logic.GameLogic.create(random = Random(0))
    var fallbackState = initialState
    val fallbackSteps = mutableListOf<StackShiftDemoStep>()
    val fallbackColumns = listOf(4, 5, 3, 6, 2, 7)
    fallbackColumns.forEach { targetColumn ->
        val preview = fallbackLogic.previewPlacement(fallbackState, targetColumn) ?: return@forEach
        val softLocked = fallbackLogic.placePiece(fallbackState, targetColumn)
        val committed = fallbackLogic.commitSoftLock(softLocked.state)
        fallbackSteps += StackShiftDemoStep(
            beforeState = fallbackState,
            targetColumn = targetColumn,
            preview = preview,
            softLockedState = softLocked.state,
            committedState = committed.state,
        )
        fallbackState = committed.state
    }

    return StackShiftDemoScenario(
        seed = 0,
        steps = fallbackSteps,
    )
}

private data class StackShiftScenarioCandidate(
    val targetColumn: Int,
    val preview: PlacementPreview,
    val softLockedState: GameState,
    val committedState: GameState,
    val commitEvents: Set<com.ugurbuga.blockgames.game.logic.GameEvent>,
)

private fun scoreBlockWiseDemoCandidate(
    candidate: GridPlacementCandidate,
    state: GameState,
    stepIndex: Int,
    sawRowClear: Boolean,
    sawColumnClear: Boolean,
): Int {
    val centerColumn = state.config.columns / 2f
    val centerRow = state.config.rows / 2f
    val dx = kotlin.math.abs(candidate.target.column - centerColumn).toInt()
    val dy = kotlin.math.abs(candidate.target.row - centerRow).toInt()
    val placementBias = 40 - (dx * 3) - (dy * 2)
    val rowScore = when {
        !sawRowClear && candidate.preview.clearedRows.isNotEmpty() -> 320
        candidate.preview.clearedRows.isNotEmpty() -> 140
        else -> 0
    }
    val columnScore = when {
        !sawColumnClear && candidate.preview.clearedColumns.isNotEmpty() -> 320
        candidate.preview.clearedColumns.isNotEmpty() -> 140
        else -> 0
    }
    val comboScore = if (candidate.preview.clearedRows.isNotEmpty() && candidate.preview.clearedColumns.isNotEmpty()) 160 else 0
    val trayBias = (state.trayPieces.size - state.trayPieces.indexOfFirst { it.id == candidate.piece.id }.coerceAtLeast(0)) * 6
    return placementBias + rowScore + columnScore + comboScore + trayBias + (SelectionDemoActionCount - stepIndex) * 4
}

private fun scoreStackShiftDemoCandidate(
    candidate: StackShiftScenarioCandidate,
    stepIndex: Int,
    previousStep: StackShiftDemoStep?,
): Int {
    val landing = candidate.preview.landingAnchor
    val centerBias = 48 - (kotlin.math.abs(candidate.targetColumn - 4.5f) * 10f).toInt()
    val clearScore = (candidate.committedState.recentlyClearedRows.size * 180) + (candidate.committedState.recentlyClearedColumns.size * 120)
    val specialScore = if (candidate.commitEvents.contains(com.ugurbuga.blockgames.game.logic.GameEvent.SpecialTriggered)) 120 else 0
    val animationScore = if (candidate.committedState.clearAnimationToken > candidate.beforeSoftLockToken()) 80 else 0

    if (stepIndex == 0) {
        return centerBias + clearScore + specialScore + if (candidate.targetColumn == 4) 220 else 0
    }

    if (stepIndex == 1) {
        val previousRow = previousStep?.preview?.landingAnchor?.row ?: Int.MAX_VALUE
        val elevatedScore = if (landing.row < previousRow) 360 else -240
        val rowClearerScore = if (candidate.softLockedState.activePiece?.special == com.ugurbuga.blockgames.game.model.SpecialBlockType.RowClearer) 240 else -300
        val visibleClearScore = if (candidate.committedState.clearAnimationToken > candidate.softLockedState.clearAnimationToken) 260 else -220
        return centerBias + clearScore + specialScore + elevatedScore + rowClearerScore + visibleClearScore
    }

    return centerBias + clearScore + specialScore + animationScore
}

private fun StackShiftScenarioCandidate.beforeSoftLockToken(): Long = softLockedState.clearAnimationToken

private fun findAnyExplodablePoint(board: BoardMatrix): GridPoint? {
    val visited = mutableSetOf<GridPoint>()
    for (row in 0 until board.rows) {
        for (col in 0 until board.columns) {
            val point = GridPoint(col, row)
            if (point !in visited) {
                val group = findConnectedGroup(board, point)
                if (group.size >= 3) return point
                visited.addAll(group)
            }
        }
    }
    return null
}

private fun findConnectedGroup(board: BoardMatrix, start: GridPoint): Set<GridPoint> {
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
fun AppSelectionScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        AppSelectionScreen(
            currentStyle = null,
            onGameplayStyleSelected = {},
            telemetry = NoOpAppTelemetry,
            onBack = {},
        )
    }
}
