package com.ugurbuga.blockgames.ui.game.gametutorial

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.block_properties_column_clearer_desc
import blockgames.composeapp.generated.resources.block_properties_column_clearer_title
import blockgames.composeapp.generated.resources.block_properties_ghost_desc
import blockgames.composeapp.generated.resources.block_properties_ghost_title
import blockgames.composeapp.generated.resources.block_properties_heavy_desc
import blockgames.composeapp.generated.resources.block_properties_heavy_title
import blockgames.composeapp.generated.resources.block_properties_row_clearer_desc
import blockgames.composeapp.generated.resources.block_properties_row_clearer_title
import blockgames.composeapp.generated.resources.blocksort_goal_hint
import blockgames.composeapp.generated.resources.blocksort_round_complete_hint
import blockgames.composeapp.generated.resources.game_message_select_column
import blockgames.composeapp.generated.resources.game_message_special_chain_board
import blockgames.composeapp.generated.resources.game_message_special_lines
import blockgames.composeapp.generated.resources.game_message_tempo_up
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_finish_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_match_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_blocksort_pick_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_tap_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_tap_hint
import blockgames.composeapp.generated.resources.launch_drag_hint
import blockgames.composeapp.generated.resources.launch_drag_hint_blockwise
import blockgames.composeapp.generated.resources.launch_soft_lock_message
import blockgames.composeapp.generated.resources.piece_properties_active
import blockgames.composeapp.generated.resources.queue_next_short
import blockgames.composeapp.generated.resources.restart
import blockgames.composeapp.generated.resources.selection_switch_game
import blockgames.composeapp.generated.resources.settings_challenges
import blockgames.composeapp.generated.resources.settings_theme
import blockgames.composeapp.generated.resources.settings_tutorial
import blockgames.composeapp.generated.resources.tutorial_back
import blockgames.composeapp.generated.resources.tutorial_blocksort_finish_body
import blockgames.composeapp.generated.resources.tutorial_blocksort_finish_title
import blockgames.composeapp.generated.resources.tutorial_blocksort_intro_body
import blockgames.composeapp.generated.resources.tutorial_blocksort_intro_title
import blockgames.composeapp.generated.resources.tutorial_blocksort_ready_body
import blockgames.composeapp.generated.resources.tutorial_blocksort_ready_title
import blockgames.composeapp.generated.resources.tutorial_blocksort_rules_body
import blockgames.composeapp.generated.resources.tutorial_blocksort_rules_title
import blockgames.composeapp.generated.resources.tutorial_boomblocks_gravity_body
import blockgames.composeapp.generated.resources.tutorial_boomblocks_gravity_title
import blockgames.composeapp.generated.resources.tutorial_boomblocks_intro_body
import blockgames.composeapp.generated.resources.tutorial_boomblocks_intro_title
import blockgames.composeapp.generated.resources.tutorial_boomblocks_ready_body
import blockgames.composeapp.generated.resources.tutorial_boomblocks_ready_title
import blockgames.composeapp.generated.resources.tutorial_chainshift_combo_body
import blockgames.composeapp.generated.resources.tutorial_chainshift_combo_title
import blockgames.composeapp.generated.resources.tutorial_chainshift_intro_body
import blockgames.composeapp.generated.resources.tutorial_chainshift_intro_title
import blockgames.composeapp.generated.resources.tutorial_chainshift_path_body
import blockgames.composeapp.generated.resources.tutorial_chainshift_path_title
import blockgames.composeapp.generated.resources.tutorial_chainshift_ready_body
import blockgames.composeapp.generated.resources.tutorial_chainshift_ready_title
import blockgames.composeapp.generated.resources.tutorial_finish
import blockgames.composeapp.generated.resources.tutorial_intro_body
import blockgames.composeapp.generated.resources.tutorial_intro_title
import blockgames.composeapp.generated.resources.tutorial_mergeshift_intro_body
import blockgames.composeapp.generated.resources.tutorial_mergeshift_intro_title
import blockgames.composeapp.generated.resources.tutorial_mergeshift_ready_body
import blockgames.composeapp.generated.resources.tutorial_mergeshift_ready_title
import blockgames.composeapp.generated.resources.tutorial_mergeshift_specials_body
import blockgames.composeapp.generated.resources.tutorial_mergeshift_specials_title
import blockgames.composeapp.generated.resources.tutorial_mergeshift_systems_body
import blockgames.composeapp.generated.resources.tutorial_mergeshift_systems_title
import blockgames.composeapp.generated.resources.tutorial_next
import blockgames.composeapp.generated.resources.tutorial_ready_body
import blockgames.composeapp.generated.resources.tutorial_ready_settings_hint
import blockgames.composeapp.generated.resources.tutorial_ready_title
import blockgames.composeapp.generated.resources.tutorial_ready_tutorial_hint
import blockgames.composeapp.generated.resources.tutorial_specials_body
import blockgames.composeapp.generated.resources.tutorial_specials_title
import blockgames.composeapp.generated.resources.tutorial_stackshift_intro_body
import blockgames.composeapp.generated.resources.tutorial_stackshift_intro_title
import blockgames.composeapp.generated.resources.tutorial_stackshift_ready_body
import blockgames.composeapp.generated.resources.tutorial_stackshift_ready_settings_hint
import blockgames.composeapp.generated.resources.tutorial_stackshift_ready_title
import blockgames.composeapp.generated.resources.tutorial_stackshift_ready_tutorial_hint
import blockgames.composeapp.generated.resources.tutorial_stackshift_specials_body
import blockgames.composeapp.generated.resources.tutorial_stackshift_specials_title
import blockgames.composeapp.generated.resources.tutorial_stackshift_systems_body
import blockgames.composeapp.generated.resources.tutorial_stackshift_systems_title
import blockgames.composeapp.generated.resources.tutorial_step_counter
import blockgames.composeapp.generated.resources.tutorial_systems_body
import blockgames.composeapp.generated.resources.tutorial_systems_title
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.logic.BlockSortGameLogic
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingScene
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStage
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.BlockSortOnboardingScene
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStage
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStateFactory
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.BlockStyleActionButton
import com.ugurbuga.blockgames.ui.game.BoardGrid
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.PieceBlocks
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.blockStyleIconTint
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.boardFrameCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.game.BlockSortBoard
import com.ugurbuga.blockgames.ui.game.game.GameGrid
import com.ugurbuga.blockgames.ui.game.game.LaunchGuideLineOverlay
import com.ugurbuga.blockgames.ui.game.game.columnToLeft
import com.ugurbuga.blockgames.ui.game.game.pieceSpawnTopLeft
import com.ugurbuga.blockgames.ui.game.game.resolveSelectedColumn
import com.ugurbuga.blockgames.ui.game.rememberBlockStylePulse
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private val TutorialMiniDockHeight = 88.dp
private const val TutorialIntroColumns = 10
private const val TutorialIntroRows = 10
private const val TutorialCompactColumns = 5
private const val TutorialCompactRows = 5
private const val TutorialDemoAutoplayStartDelayMillis = 720L
private const val TutorialDemoAutoplayStepDelayMillis = 1650L
private const val TutorialDemoTravelDurationMillis = 1120
private const val TutorialDemoSoftLockDelayMillis = 220L
private const val TutorialDemoResolutionHoldMillis = 980L
private const val TutorialDemoResetDelayMillis = 480L
private const val TutorialDemoClearAnimationDurationMillis = 620
private const val TutorialDemoBoardShiftDurationMillis = 360
private const val TutorialDemoTrayPulseDurationMillis = 900
private const val TutorialDemoTrayPulseScaleBoost = 0.05f
private val TutorialSamplePiece = Piece(
    id = -1,
    kind = PieceKind.T,
    tone = CellTone.Cyan,
    cells = listOf(
        GridPoint(column = 1, row = 0),
        GridPoint(column = 0, row = 1),
        GridPoint(column = 1, row = 1),
        GridPoint(column = 2, row = 1),
    ),
    width = 3,
    height = 2,
)
private val TutorialNextPiece = Piece(
    id = -2,
    kind = PieceKind.L,
    tone = CellTone.Gold,
    cells = listOf(
        GridPoint(column = 0, row = 0),
        GridPoint(column = 0, row = 1),
        GridPoint(column = 0, row = 2),
        GridPoint(column = 1, row = 2),
    ),
    width = 2,
    height = 3,
    special = SpecialBlockType.Ghost,
)
private val TutorialBlockWisePrimaryPiece = Piece(
    id = -301,
    kind = PieceKind.TriL,
    tone = CellTone.Cyan,
    cells = listOf(
        GridPoint(column = 0, row = 0),
        GridPoint(column = 1, row = 0),
        GridPoint(column = 0, row = 1),
    ),
    width = 2,
    height = 2,
)
private val TutorialBlockWiseSecondaryPiece = Piece(
    id = -302,
    kind = PieceKind.Domino,
    tone = CellTone.Gold,
    cells = listOf(
        GridPoint(column = 0, row = 0),
        GridPoint(column = 1, row = 0),
    ),
    width = 2,
    height = 1,
)
private val TutorialBlockWiseTertiaryPiece = Piece(
    id = -303,
    kind = PieceKind.Square,
    tone = CellTone.Violet,
    cells = listOf(
        GridPoint(column = 0, row = 0),
        GridPoint(column = 1, row = 0),
        GridPoint(column = 0, row = 1),
        GridPoint(column = 1, row = 1),
    ),
    width = 2,
    height = 2,
)
private val TutorialBlockWiseTargetOrigin = GridPoint(column = 2, row = 1)

private enum class TutorialPage {
    StackShiftIntro,
    StackShiftSystems,
    StackShiftSpecials,
    StackShiftReady,
    BlockWiseIntro,
    BlockWiseSystems,
    BlockWiseSpecials,
    BlockWiseReady,
    ChainShiftIntro,
    ChainShiftPath,
    ChainShiftCombo,
    ChainShiftReady,
    MergeShiftIntro,
    MergeShiftSystems,
    MergeShiftSpecials,
    MergeShiftReady,
    BoomBlocksIntro,
    BoomBlocksGravity,
    BoomBlocksReady,
    BlockSortIntro,
    BlockSortRules,
    BlockSortFinish,
    BlockSortReady,
}

internal fun tutorialBoomBlocksIntroScene(): TutorialDemoScene {
    val target = setOf(GridPoint(2, 2), GridPoint(1, 2), GridPoint(3, 2))
    val board = BoardMatrix.empty(columns = 5, rows = 5)
        .fillPattern(
            exclude = target,
            disallowedTones = setOf(CellTone.Cyan),
        )
        .fill(GridPoint(2, 2), CellTone.Cyan)
        .fill(GridPoint(1, 2), CellTone.Cyan)
        .fill(GridPoint(3, 2), CellTone.Cyan)
    return TutorialDemoScene(
        gameState = tutorialPreviewGameState(
            board = board,
            activePiece = Piece(
                id = -401,
                kind = PieceKind.Single,
                tone = CellTone.Cyan,
                cells = listOf(GridPoint(0, 0)),
                width = 1,
                height = 1
            ),
            spawnColumn = 2,
            gameplayStyle = GameplayStyle.BoomBlocks,
        ),
        spawnColumn = 2,
        autoColumns = listOf(2),
    )
}

internal fun tutorialBoomBlocksGravityScene(): TutorialDemoScene {
    val target = setOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(0, 1))
    val board = BoardMatrix.empty(columns = 5, rows = 5)
        .fillPattern(
            exclude = target,
            disallowedTones = setOf(CellTone.Coral),
        )
        .fill(GridPoint(0, 0), CellTone.Coral)
        .fill(GridPoint(1, 0), CellTone.Coral)
        .fill(GridPoint(0, 1), CellTone.Coral)
    return TutorialDemoScene(
        gameState = tutorialPreviewGameState(
            board = board,
            activePiece = Piece(
                id = -402,
                kind = PieceKind.Single,
                tone = CellTone.Coral,
                cells = listOf(GridPoint(0, 0)),
                width = 1,
                height = 1
            ),
            spawnColumn = 0,
            gameplayStyle = GameplayStyle.BoomBlocks,
        ),
        spawnColumn = 0,
        autoColumns = listOf(0),
    )
}

internal fun BoardMatrix.fillPattern(
    exclude: Set<GridPoint> = emptySet(),
    disallowedTones: Set<CellTone> = emptySet(),
): BoardMatrix {
    var result = this
    var id = 1000
    val tones = listOf(
        CellTone.Cyan,
        CellTone.Gold,
        CellTone.Violet,
        CellTone.Emerald,
        CellTone.Coral,
    ).filterNot(disallowedTones::contains)
    for (c in 0 until columns) {
        for (r in 0 until rows) {
            val point = GridPoint(c, r)
            if (point in exclude) continue
            val tone = tones[(c + (r * 2)) % tones.size]
            result = result.fill(listOf(point), tone, value = id++)
        }
    }
    return result
}

internal fun BoardMatrix.fill(point: GridPoint, tone: CellTone): BoardMatrix {
    return this.fill(listOf(point), tone, value = (point.column * 100 + point.row + 5000))
}

internal data class TutorialDemoScene(
    val gameState: GameState,
    val spawnColumn: Int,
    val autoColumns: List<Int>,
)

internal fun tutorialPreviewGameState(
    board: BoardMatrix,
    activePiece: Piece,
    nextQueue: List<Piece> = listOf(TutorialNextPiece),
    spawnColumn: Int,
    gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
): GameState = GameState(
    config = GameConfig(columns = board.columns, rows = board.rows),
    gameplayStyle = gameplayStyle,
    board = board,
    activePiece = activePiece,
    nextQueue = nextQueue,
    holdPiece = null,
    canHold = true,
    lastPlacementColumn = spawnColumn,
    score = 0,
    linesCleared = 0,
    level = 1,
    difficultyStage = 0,
    secondsUntilDifficultyIncrease = 18,
    status = GameStatus.Running,
    message = gameText(GameTextKey.GameMessageSelectColumn),
)

private fun tutorialIntroScene(): TutorialDemoScene {
    val board = BoardMatrix.empty(columns = TutorialIntroColumns, rows = TutorialIntroRows)
        .fill(
            points = listOf(
                GridPoint(1, 5),
                GridPoint(2, 5),
                GridPoint(3, 5),
                GridPoint(6, 4),
                GridPoint(7, 4),
                GridPoint(8, 4),
                GridPoint(4, 3),
                GridPoint(5, 3),
            ),
            tone = CellTone.Blue,
        )
        .fill(
            points = listOf(
                GridPoint(2, 4),
                GridPoint(7, 3),
            ),
            tone = CellTone.Gold,
        )
    return TutorialDemoScene(
        gameState = tutorialPreviewGameState(
            board = board,
            activePiece = TutorialSamplePiece.copy(id = -101, tone = CellTone.Cyan),
            spawnColumn = 0,
        ),
        spawnColumn = 0,
        autoColumns = listOf(0, 2, 4, 6, 7, 5, 3, 1),
    )
}

private fun tutorialSpecialScene(
    tone: CellTone,
    special: SpecialBlockType,
): TutorialDemoScene {
    val board = when (special) {
        SpecialBlockType.ColumnClearer -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(1, 3),
                    GridPoint(1, 2),
                    GridPoint(2, 2),
                    GridPoint(3, 2),
                    GridPoint(4, 2),
                ),
                tone = CellTone.Blue,
            )
            .fill(
                points = listOf(
                    GridPoint(4, 1),
                ),
                tone = CellTone.Gold,
            )

        SpecialBlockType.RowClearer -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 2),
                    GridPoint(1, 2),
                    GridPoint(3, 2),
                    GridPoint(4, 2),
                    GridPoint(1, 1),
                    GridPoint(2, 1),
                    GridPoint(4, 1),
                ),
                tone = CellTone.Emerald,
            )

        SpecialBlockType.Ghost -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(1, 2),
                    GridPoint(2, 2),
                    GridPoint(4, 2),
                    GridPoint(0, 1),
                    GridPoint(3, 1),
                ),
                tone = CellTone.Violet,
            )
            .fill(
                points = listOf(GridPoint(2, 1)),
                tone = CellTone.Gold,
            )

        SpecialBlockType.Heavy -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(1, 3),
                    GridPoint(3, 2),
                    GridPoint(4, 2),
                    GridPoint(0, 1),
                    GridPoint(2, 1),
                    GridPoint(3, 1),
                ),
                tone = CellTone.Coral,
            )

        SpecialBlockType.None -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
    }
    val activePiece = when (special) {
        SpecialBlockType.Ghost -> TutorialNextPiece.copy(id = -202L, tone = tone, special = special)
        else -> TutorialSamplePiece.copy(
            id = -201L - special.ordinal,
            tone = tone,
            special = special
        )
    }
    val spawnColumn = if (special == SpecialBlockType.Ghost) 1 else 1
    val autoColumns = when (special) {
        SpecialBlockType.Ghost -> listOf(0, 1, 2, 3, 2, 1)
        else -> listOf(0, 1, 2, 1)
    }
    return TutorialDemoScene(
        gameState = tutorialPreviewGameState(
            board = board,
            activePiece = activePiece,
            spawnColumn = spawnColumn,
        ),
        spawnColumn = spawnColumn,
        autoColumns = autoColumns,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameTutorialScreen(
    modifier: Modifier = Modifier,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    initialPage: Int = 0,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    LogScreen(telemetry, TelemetryScreenNames.Tutorial)
    val tutorialPages = remember(gameplayStyle) {
        when (gameplayStyle) {
            GameplayStyle.StackShift -> listOf(
                TutorialPage.StackShiftIntro,
                TutorialPage.StackShiftSystems,
                TutorialPage.StackShiftSpecials,
                TutorialPage.StackShiftReady,
            )

            GameplayStyle.BlockWise -> listOf(
                TutorialPage.BlockWiseIntro,
                TutorialPage.StackShiftSystems,
                TutorialPage.BlockWiseSystems,
                TutorialPage.BlockWiseReady,
            )

            GameplayStyle.ChainShift -> listOf(
                TutorialPage.ChainShiftIntro,
                TutorialPage.ChainShiftPath,
                TutorialPage.ChainShiftCombo,
                TutorialPage.ChainShiftReady,
            )

            GameplayStyle.MergeShift -> listOf(
                TutorialPage.MergeShiftIntro,
                TutorialPage.MergeShiftSystems,
                TutorialPage.MergeShiftSpecials,
                TutorialPage.MergeShiftReady,
            )

            GameplayStyle.BoomBlocks -> listOf(
                TutorialPage.BoomBlocksIntro,
                TutorialPage.BoomBlocksGravity,
                TutorialPage.StackShiftSystems,
                TutorialPage.BoomBlocksReady,
            )

            GameplayStyle.BlockSort -> listOf(
                TutorialPage.BlockSortIntro,
                TutorialPage.BlockSortRules,
                TutorialPage.BlockSortFinish,
                TutorialPage.BlockSortReady,
            )
        }
    }
    val totalSteps = tutorialPages.size
    val uiColors = BlockGamesThemeTokens.uiColors
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (totalSteps - 1).coerceAtLeast(0))) { totalSteps }
    val coroutineScope = rememberCoroutineScope()
    val currentStep = pagerState.currentPage
    val isLastStep = (currentStep == (totalSteps - 1))

    var showBlockWiseFinishDialog by remember { mutableStateOf(false) }

    if (showBlockWiseFinishDialog) {
        InteractiveOnboardingCompletionDialog(
            onStartGame = onFinish,
            onReturnHome = onBack,
        )
    }

    val transition = rememberInfiniteTransition(label = "tutorialStylePulse")
    val stylePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.tutorial_back),
                        onClick = onBack,
                        size = 44.dp,
                        pulse = stylePulse,
                    )

                    Text(
                        text = stringResource(Res.string.settings_tutorial),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )

                    TutorialStepChip(
                        currentStep = currentStep + 1,
                        totalSteps = totalSteps,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    uiColors.panelHighlight.copy(alpha = 0.14f),
                                    uiColors.launchGlow.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        userScrollEnabled = true,
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        TutorialScrollablePage {
                            when (tutorialPages[page]) {
                                TutorialPage.StackShiftIntro -> TutorialIntroStep()
                                TutorialPage.StackShiftSystems -> TutorialSystemsStep()
                                TutorialPage.StackShiftSpecials -> TutorialSpecialsStep()
                                TutorialPage.StackShiftReady -> TutorialReadyStep()
                                TutorialPage.BlockWiseIntro -> TutorialBlockWiseIntroStep()
                                TutorialPage.BlockWiseSystems -> TutorialBlockWiseSystemsStep()
                                TutorialPage.BlockWiseSpecials -> TutorialBlockWiseSpecialsStep()
                                TutorialPage.BlockWiseReady -> TutorialBlockWiseReadyStep()
                                TutorialPage.ChainShiftIntro -> TutorialChainShiftIntroStep()
                                TutorialPage.ChainShiftPath -> TutorialChainShiftPathStep()
                                TutorialPage.ChainShiftCombo -> TutorialChainShiftComboStep()
                                TutorialPage.ChainShiftReady -> TutorialChainShiftReadyStep()
                                TutorialPage.MergeShiftIntro -> TutorialMergeShiftIntroStep()
                                TutorialPage.MergeShiftSystems -> TutorialMergeShiftSystemsStep()
                                TutorialPage.MergeShiftSpecials -> TutorialMergeShiftSpecialsStep()
                                TutorialPage.MergeShiftReady -> TutorialMergeShiftReadyStep()
                                TutorialPage.BoomBlocksIntro -> TutorialBoomBlocksIntroStep()
                                TutorialPage.BoomBlocksGravity -> TutorialBoomBlocksGravityStep()
                                TutorialPage.BoomBlocksReady -> TutorialBoomBlocksReadyStep()
                                TutorialPage.BlockSortIntro -> TutorialBlockSortIntroStep()
                                TutorialPage.BlockSortRules -> TutorialBlockSortRulesStep()
                                TutorialPage.BlockSortFinish -> TutorialBlockSortFinishStep()
                                TutorialPage.BlockSortReady -> TutorialBlockSortReadyStep()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStep > 0) {
                        TopBarActionBlockButton(
                            tone = CellTone.Cyan,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tutorial_back),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentStep - 1)
                                }
                            },
                            size = 40.dp,
                            pulse = stylePulse,
                        )
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    BlockStyleActionButton(
                        text = if (isLastStep) stringResource(Res.string.tutorial_finish) else stringResource(
                            Res.string.tutorial_next
                        ),
                        icon = if (isLastStep) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            if (isLastStep) {
                                onFinish()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentStep + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .widthIn(max = 180.dp)
                            .height(48.dp),
                        emphasized = true,
                        tone = if (isLastStep) CellTone.Emerald else CellTone.Cyan,
                        iconOnRight = true,
                        pulse = stylePulse,
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialStepChip(currentStep: Int, totalSteps: Int) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = Modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.68f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = stringResource(Res.string.tutorial_step_counter, currentStep, totalSteps),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TutorialScrollablePage(
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )

        if (scrollState.canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.panel.copy(alpha = 0.82f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        if (scrollState.canScrollForward) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                uiColors.panel.copy(alpha = 0.88f),
                            ),
                        ),
                    )
                    .padding(top = 18.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "⌄",
                    style = MaterialTheme.typography.titleMedium,
                    color = uiColors.subtitle,
                )
                Text(
                    text = "⌄",
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun TutorialMiniBoardShell(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val boardStyle = settings.blockVisualStyle
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(
                boardFrameCornerRadiusDp(
                    boardStyle
                )
            ),
            elevation = 10.dp,
        ),
        shape = RoundedCornerShape(
            boardFrameCornerRadiusDp(
                boardStyle
            )
        ),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.boardOutline.copy(alpha = 0.84f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.boardGradientTop.copy(alpha = 0.24f),
                            uiColors.boardGradientBottom.copy(alpha = 0.28f),
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}

@Composable
private fun TutorialIntroStep(launchPreviewColumn: Int? = null) {
    TutorialSection(
        title = stringResource(Res.string.tutorial_stackshift_intro_title),
        body = stringResource(Res.string.tutorial_stackshift_intro_body),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TutorialPieceCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.piece_properties_active),
                piece = TutorialSamplePiece,
                alpha = 1f,
            )
            TutorialPieceCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.queue_next_short),
                piece = TutorialNextPiece,
                alpha = 0.58f,
            )
        }
        TutorialLaunchBoardDemo(lockedColumn = launchPreviewColumn)
        TutorialHintCard(text = stringResource(Res.string.launch_drag_hint))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorialSystemsStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_stackshift_systems_title),
        body = stringResource(Res.string.tutorial_stackshift_systems_body),
    ) {
        TutorialSystemTiles()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorialSystemTiles() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TutorialActionTile(
            tone = CellTone.Emerald,
            icon = Icons.Filled.EmojiEvents,
            title = stringResource(Res.string.settings_challenges),
        )
        TutorialActionTile(
            tone = CellTone.Gold,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(Res.string.settings_tutorial),
        )
        TutorialActionTile(
            tone = CellTone.Violet,
            icon = Icons.Filled.Palette,
            title = stringResource(Res.string.settings_theme),
        )
        TutorialActionTile(
            tone = CellTone.Coral,
            icon = Icons.Default.SwapHoriz,
            title = stringResource(Res.string.selection_switch_game),
        )
        TutorialActionTile(
            tone = CellTone.Cyan,
            icon = Icons.Filled.Refresh,
            title = stringResource(Res.string.restart),
        )
    }
}

@Composable
private fun TutorialSpecialsStep(previewColumns: Map<SpecialBlockType, Int> = emptyMap()) {
    TutorialSection(
        title = stringResource(Res.string.tutorial_stackshift_specials_title),
        body = stringResource(Res.string.tutorial_stackshift_specials_body),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TutorialAnimatedSpecialsShowcase(previewColumns = previewColumns)
            TutorialSpecialCard(
                tone = CellTone.Emerald,
                special = SpecialBlockType.ColumnClearer,
                title = stringResource(Res.string.block_properties_column_clearer_title),
                body = stringResource(Res.string.block_properties_column_clearer_desc),
            )
            TutorialSpecialCard(
                tone = CellTone.Amber,
                special = SpecialBlockType.RowClearer,
                title = stringResource(Res.string.block_properties_row_clearer_title),
                body = stringResource(Res.string.block_properties_row_clearer_desc),
            )
            TutorialSpecialCard(
                tone = CellTone.Violet,
                special = SpecialBlockType.Ghost,
                title = stringResource(Res.string.block_properties_ghost_title),
                body = stringResource(Res.string.block_properties_ghost_desc),
            )
            TutorialSpecialCard(
                tone = CellTone.Coral,
                special = SpecialBlockType.Heavy,
                title = stringResource(Res.string.block_properties_heavy_title),
                body = stringResource(Res.string.block_properties_heavy_desc),
            )
        }
    }
}

@Composable
private fun TutorialReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_stackshift_ready_title),
        body = stringResource(Res.string.tutorial_stackshift_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.tutorial_stackshift_ready_tutorial_hint))
        TutorialHintCard(text = stringResource(Res.string.tutorial_stackshift_ready_settings_hint))
    }
}

@Composable
private fun TutorialBlockWiseIntroStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_intro_title),
        body = stringResource(Res.string.tutorial_intro_body),
    ) {
        TutorialBlockWisePlacementDemo()
        TutorialHintCard(text = stringResource(Res.string.launch_drag_hint_blockwise))
    }
}

@Composable
private fun TutorialBlockWiseSystemsStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_systems_title),
        body = stringResource(Res.string.tutorial_systems_body),
    ) {
        TutorialMiniBoardShell {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TutorialTrayPieceChip(
                        piece = TutorialBlockWisePrimaryPiece,
                        hidden = false,
                        modifier = Modifier.weight(1f)
                    )
                    TutorialTrayPieceChip(
                        piece = TutorialBlockWiseSecondaryPiece,
                        hidden = false,
                        modifier = Modifier.weight(1f)
                    )
                    TutorialTrayPieceChip(
                        piece = TutorialBlockWiseTertiaryPiece,
                        hidden = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        TutorialHintCard(text = stringResource(Res.string.game_message_tempo_up))
    }
}

@Composable
private fun TutorialBlockWiseSpecialsStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_specials_title),
        body = stringResource(Res.string.tutorial_specials_body),
    ) {
        TutorialBlockWisePlacementDemo()
        TutorialHintCard(text = stringResource(Res.string.game_message_special_chain_board, "1"))
    }
}

@Composable
private fun TutorialBlockWiseReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_ready_title),
        body = stringResource(Res.string.tutorial_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_tutorial_hint))
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_settings_hint))
    }
}

@Composable
private fun TutorialChainShiftIntroStep() {
    val scene = remember { ChainShiftOnboardingStateFactory.scene(ChainShiftOnboardingStage.FirstInsert) }
    TutorialSection(
        title = stringResource(Res.string.tutorial_chainshift_intro_title),
        body = stringResource(Res.string.tutorial_chainshift_intro_body),
    ) {
        TutorialChainShiftBoardDemo(scene = scene)
    }
}

@Composable
private fun TutorialChainShiftPathStep() {
    val scene = remember { ChainShiftOnboardingStateFactory.scene(ChainShiftOnboardingStage.FirstInsert) }
    TutorialSection(
        title = stringResource(Res.string.tutorial_chainshift_path_title),
        body = stringResource(Res.string.tutorial_chainshift_path_body),
    ) {
        TutorialChainShiftBoardDemo(scene = scene)
        TutorialHintCard(text = stringResource(Res.string.interactive_onboarding_chainshift_tap_hint))
    }
}

@Composable
private fun TutorialChainShiftComboStep() {
    val scene = remember { ChainShiftOnboardingStateFactory.scene(ChainShiftOnboardingStage.TriggerChain) }
    TutorialSection(
        title = stringResource(Res.string.tutorial_chainshift_combo_title),
        body = stringResource(Res.string.tutorial_chainshift_combo_body),
    ) {
        TutorialChainShiftBoardDemo(scene = scene)
        TutorialHintCard(text = stringResource(Res.string.interactive_onboarding_chainshift_tap_hint))
    }
}

@Composable
private fun TutorialChainShiftReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_chainshift_ready_title),
        body = stringResource(Res.string.tutorial_chainshift_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_settings_hint))
    }
}

@Composable
private fun TutorialChainShiftBoardDemo(
    scene: ChainShiftOnboardingScene,
    modifier: Modifier = Modifier,
) {
    TutorialMiniBoardShell(modifier = modifier) {
        BoardGrid(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(scene.gameState.config.columns.toFloat() / scene.gameState.config.rows.toFloat()),
            gameState = scene.gameState,
            preview = null,
            impactedPreviewCells = setOf(scene.guidePoint),
            activeColumn = null,
            activePiece = null,
            isDragging = false,
            stylePulse = 0f,
        )
    }
}

@Composable
private fun TutorialMergeShiftIntroStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_mergeshift_intro_title),
        body = stringResource(Res.string.tutorial_mergeshift_intro_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.game_message_select_column))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorialMergeShiftSystemsStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_mergeshift_systems_title),
        body = stringResource(Res.string.tutorial_mergeshift_systems_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.launch_soft_lock_message))
        TutorialSystemTiles()
    }
}

@Composable
private fun TutorialMergeShiftSpecialsStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_mergeshift_specials_title),
        body = stringResource(Res.string.tutorial_mergeshift_specials_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.game_message_special_lines, "2"))
    }
}

@Composable
private fun TutorialMergeShiftReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_mergeshift_ready_title),
        body = stringResource(Res.string.tutorial_mergeshift_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_settings_hint))
    }
}

@Composable
private fun TutorialBoomBlocksIntroStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_boomblocks_intro_title),
        body = stringResource(Res.string.tutorial_boomblocks_intro_body),
    ) {
        TutorialBoomBlocksDemo(scene = remember { tutorialBoomBlocksIntroScene() })
        TutorialHintCard(text = stringResource(Res.string.interactive_onboarding_boomblocks_tap_hint))
    }
}

@Composable
private fun TutorialBoomBlocksGravityStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_boomblocks_gravity_title),
        body = stringResource(Res.string.tutorial_boomblocks_gravity_body),
    ) {
        TutorialBoomBlocksDemo(scene = remember { tutorialBoomBlocksGravityScene() })
        TutorialHintCard(text = stringResource(Res.string.launch_soft_lock_message))
    }
}

@Composable
private fun TutorialBoomBlocksReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_boomblocks_ready_title),
        body = stringResource(Res.string.tutorial_boomblocks_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_settings_hint))
    }
}

@Composable
private fun TutorialBlockSortIntroStep() {
    val scene = remember { BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.PickSource) }
    TutorialSection(
        title = stringResource(Res.string.tutorial_blocksort_intro_title),
        body = stringResource(Res.string.tutorial_blocksort_intro_body),
    ) {
        TutorialBlockSortBoardDemo(scene = scene)
        TutorialHintCard(text = stringResource(Res.string.interactive_onboarding_blocksort_pick_hint))
    }
}

@Composable
private fun TutorialBlockSortRulesStep() {
    val scene = remember { BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.MatchColor) }
    TutorialSection(
        title = stringResource(Res.string.tutorial_blocksort_rules_title),
        body = stringResource(Res.string.tutorial_blocksort_rules_body),
    ) {
        TutorialBlockSortBoardDemo(scene = scene)
        TutorialHintCard(text = stringResource(Res.string.interactive_onboarding_blocksort_match_hint))
    }
}

@Composable
private fun TutorialBlockSortFinishStep() {
    val scene = remember { BlockSortOnboardingStateFactory.scene(BlockSortOnboardingStage.FinishColumn) }
    TutorialSection(
        title = stringResource(Res.string.tutorial_blocksort_finish_title),
        body = stringResource(Res.string.tutorial_blocksort_finish_body),
    ) {
        TutorialBlockSortBoardDemo(scene = scene)
        TutorialHintCard(text = stringResource(Res.string.interactive_onboarding_blocksort_finish_hint))
    }
}

@Composable
private fun TutorialBlockSortReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_blocksort_ready_title),
        body = stringResource(Res.string.tutorial_blocksort_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.blocksort_goal_hint))
        TutorialHintCard(text = stringResource(Res.string.blocksort_round_complete_hint, 1200))
    }
}

@Composable
private fun TutorialBlockSortBoardDemo(
    scene: BlockSortOnboardingScene,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val boardStyle = settings.blockVisualStyle
    val logic = remember { BlockSortGameLogic() }

    TutorialMiniBoardShell(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        ) {
            BlockSortBoard(
                gameState = scene.gameState,
                selectedSourceColumn = scene.guideSourceColumn,
                interactiveOnboardingScene = scene,
                onRequestPreview = { source, target ->
                    if (source != scene.guideSourceColumn) {
                        null
                    } else {
                        logic.previewPlacement(scene.gameState, source.toLong(), GridPoint(target, 0))
                    }
                },
                onColumnTapped = {},
                palette = settings.blockColorPalette,
                boardStylePulse = 0f,
                boardStyle = boardStyle,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TutorialBoomBlocksDemo(
    scene: TutorialDemoScene,
    modifier: Modifier = Modifier,
) {
    val gameLogic = remember { GameLogic.create() }
    var gameState by remember(scene) { mutableStateOf(scene.gameState) }

    val infiniteTransition = rememberInfiniteTransition(label = "boomBlocksTutorial")
    val hintPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hintPhase",
    )
    val stylePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stylePulse",
    )

    val targetPoint = remember(scene) {
        var found: GridPoint? = null
        for (c in 0 until scene.gameState.board.columns) {
            for (r in 0 until scene.gameState.board.rows) {
                if (scene.gameState.board.toneAt(c, r) != CellTone.Blue) {
                    found = GridPoint(c, r)
                    break
                }
            }
            if (found != null) break
        }
        found ?: GridPoint(2, 2)
    }

    LaunchedEffect(scene) {
        while (isActive) {
            gameState = scene.gameState
            delay(1500)
            val result = gameLogic.placePiece(gameState, 0L, targetPoint)
            gameState = result.state
            delay(2500)
        }
    }

    TutorialMiniBoardShell(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows.toFloat())
        ) {
            GameGrid(
                gameState = gameState,
                modifier = Modifier.fillMaxSize(),
                hintEnabled = true,
                hintPhase = hintPhase,
                stylePulse = stylePulse,
            )
        }
    }
}

@Composable
private fun TutorialPracticeTrayPieceChip(
    piece: Piece,
    hidden: Boolean,
    onRectChanged: (Rect) -> Unit,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { onRectChanged(it.boundsInRoot()) }
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .pointerInput(piece.id) {
                detectDragGestures(
                    onDragStart = onStartDrag,
                    onDragEnd = onEndDrag,
                    onDragCancel = onCancelDrag,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                )
            },
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.7f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PieceBlocks(
                piece = piece,
                cellSize = 16.dp
            )
        }
    }
}

@Composable
private fun TutorialBlockWisePlacementDemo() {
    val settings = LocalAppSettings.current
    val density = LocalDensity.current
    val boardStyle = settings.blockVisualStyle
    val gameLogic = remember { GameLogic.create() }
    val initialState = remember {
        GameState(
            config = GameConfig(columns = TutorialCompactColumns, rows = TutorialCompactRows),
            gameplayStyle = GameplayStyle.BlockWise,
            board = BoardMatrix.empty(columns = TutorialCompactColumns, rows = TutorialCompactRows)
                .fill(
                    points = listOf(
                        GridPoint(0, 2),
                        GridPoint(1, 2),
                        GridPoint(4, 1),
                        GridPoint(3, 3),
                        GridPoint(4, 3),
                        GridPoint(1, 4),
                    ),
                    tone = CellTone.Blue,
                )
                .fill(
                    points = listOf(
                        GridPoint(0, 4),
                        GridPoint(2, 4),
                    ),
                    tone = CellTone.Gold,
                ),
            activePiece = TutorialBlockWisePrimaryPiece,
            nextQueue = listOf(TutorialBlockWiseSecondaryPiece, TutorialBlockWiseTertiaryPiece),
            canHold = false,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 0,
            status = GameStatus.Running,
            message = gameText(GameTextKey.LaunchDragHintBlockWise),
        )
    }
    val primaryPiece = initialState.activePiece ?: return
    val placementPreview = remember(initialState) {
        gameLogic.previewPlacement(initialState, primaryPiece.id, TutorialBlockWiseTargetOrigin)
    }
    val previewImpactPoints = remember(initialState, placementPreview) {
        gameLogic.previewImpactPoints(initialState, placementPreview)
    }
    val placedState = remember(initialState) {
        gameLogic.placePiece(initialState, primaryPiece.id, TutorialBlockWiseTargetOrigin).state
    }
    var hostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var primaryTrayPieceRectInRoot by remember { mutableStateOf(Rect.Zero) }
    val animationTransition = rememberInfiniteTransition(label = "tutorialBlockWisePlacement")
    val animationPhase by animationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tutorialBlockWisePlacementPhase",
    )
    val boardRect = boardRectInRoot.toLocalRect(hostRectInRoot)
    val trayPieceRect = primaryTrayPieceRectInRoot.toLocalRect(hostRectInRoot)
    val cellSizePx = if (boardRect != Rect.Zero) boardRect.width / initialState.config.columns else 0f
    val pieceCellDp = with(density) { cellSizePx.coerceAtLeast(1f).toDp() }
    val launchCellCornerRadius =
        boardCellCornerRadiusDp(
            cellSize = pieceCellDp,
            style = boardStyle,
        )
    val moveProgress = tutorialProgress(animationPhase, start = 0.18f, end = 0.58f)
    val startTopLeft = remember(trayPieceRect, boardRect, cellSizePx) {
        if (trayPieceRect == Rect.Zero || boardRect == Rect.Zero || cellSizePx <= 0f) {
            null
        } else {
            Offset(
                x = trayPieceRect.center.x - ((primaryPiece.width * cellSizePx) / 2f),
                y = trayPieceRect.center.y - ((primaryPiece.height * cellSizePx) / 2f),
            )
        }
    }
    val targetTopLeft = remember(boardRect, cellSizePx) {
        if (boardRect == Rect.Zero || cellSizePx <= 0f) {
            null
        } else {
            TutorialBlockWiseTargetOrigin.toLocalTopLeft(boardRect, cellSizePx)
        }
    }
    val overlayTopLeft = when {
        startTopLeft == null || targetTopLeft == null -> null
        animationPhase < 0.18f -> startTopLeft
        animationPhase < 0.58f -> lerpOffset(startTopLeft, targetTopLeft, moveProgress)
        animationPhase < 0.78f -> targetTopLeft
        else -> null
    }
    val showPlacedState = animationPhase in 0.58f..0.78f
    val boardState = if (showPlacedState) placedState else initialState
    val boardPreview = if (showPlacedState) null else placementPreview
    val selectedTrayPieceHidden = animationPhase < 0.78f
    val handAlpha = if (animationPhase < 0.58f && overlayTopLeft != null) 1f else 0f

    TutorialMiniBoardShell {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    hostRectInRoot = coordinates.boundsInRoot()
                },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BoardGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(boardState.config.columns.toFloat() / boardState.config.rows.toFloat())
                        .onGloballyPositioned { coordinates ->
                            boardRectInRoot = coordinates.boundsInRoot()
                        },
                    gameState = boardState,
                    preview = boardPreview,
                    impactedPreviewCells = if (showPlacedState) emptySet() else previewImpactPoints,
                    activeColumn = boardPreview?.selectedColumn,
                    activePiece = if (showPlacedState) null else primaryPiece,
                    isDragging = overlayTopLeft != null,
                )

                TutorialMiniBottomDock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TutorialMiniDockHeight),
                    onTrayPositioned = {},
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TutorialTrayPieceChip(
                            piece = TutorialBlockWisePrimaryPiece,
                            hidden = selectedTrayPieceHidden,
                            modifier = Modifier.weight(1f),
                            onPositioned = { primaryTrayPieceRectInRoot = it },
                        )
                        TutorialTrayPieceChip(
                            piece = TutorialBlockWiseSecondaryPiece,
                            hidden = false,
                            modifier = Modifier.weight(1f),
                        )
                        TutorialTrayPieceChip(
                            piece = TutorialBlockWiseTertiaryPiece,
                            hidden = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (overlayTopLeft != null && cellSizePx > 0f) {
                PieceBlocks(
                    piece = primaryPiece,
                    cellSize = pieceCellDp,
                    cellCornerRadius = launchCellCornerRadius,
                    modifier = Modifier.graphicsLayer(
                        translationX = overlayTopLeft.x,
                        translationY = overlayTopLeft.y,
                        transformOrigin = TransformOrigin(0f, 0f),
                    ),
                )

                TutorialDemoHand(
                    x = with(density) { overlayTopLeft.x.toDp() },
                    y = with(density) { overlayTopLeft.y.toDp() },
                    pieceWidth = primaryPiece.width,
                    pieceHeight = primaryPiece.height,
                    cellSize = pieceCellDp,
                    alpha = handAlpha,
                )
            }
        }
    }
}

@Composable
private fun TutorialLaunchBoardDemo(
    lockedColumn: Int? = null,
) {
    TutorialMiniGameDemo(
        scene = remember { tutorialIntroScene() },
        compact = false,
        lockedColumn = lockedColumn,
    )
}

@Composable
private fun TutorialAnimatedSpecialsShowcase(
    previewColumns: Map<SpecialBlockType, Int> = emptyMap(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Emerald,
                special = SpecialBlockType.ColumnClearer,
                lockedColumn = previewColumns[SpecialBlockType.ColumnClearer],
            )
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Amber,
                special = SpecialBlockType.RowClearer,
                lockedColumn = previewColumns[SpecialBlockType.RowClearer],
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Violet,
                special = SpecialBlockType.Ghost,
                lockedColumn = previewColumns[SpecialBlockType.Ghost],
            )
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Coral,
                special = SpecialBlockType.Heavy,
                lockedColumn = previewColumns[SpecialBlockType.Heavy],
            )
        }
    }
}

@Composable
private fun TutorialSpecialBoardDemo(
    tone: CellTone,
    special: SpecialBlockType,
    modifier: Modifier = Modifier,
    lockedColumn: Int? = null,
) {
    TutorialMiniGameDemo(
        scene = remember(tone, special) { tutorialSpecialScene(tone = tone, special = special) },
        modifier = modifier,
        compact = true,
        lockedColumn = lockedColumn,
        badgeTone = tone,
        badgeSpecial = special,
    )
}

@Composable
private fun TutorialMiniGameDemo(
    scene: TutorialDemoScene,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    lockedColumn: Int? = null,
    badgeTone: CellTone? = null,
    badgeSpecial: SpecialBlockType = SpecialBlockType.None,
) {
    val settings = LocalAppSettings.current
    val density = LocalDensity.current
    val boardStyle = settings.blockVisualStyle
    val gameLogic = remember { GameLogic.create() }
    var gameState by remember(scene, lockedColumn) { mutableStateOf(scene.gameState) }
    val activePiece = gameState.activePiece
    var hostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var overlayTopLeft by remember(
        scene.gameState.activePiece?.id,
        lockedColumn
    ) { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(value = false) }

    val boardRect = boardRectInRoot.toLocalRect(hostRectInRoot)
    val trayRect = trayRectInRoot.toLocalRect(hostRectInRoot)
    val cellSizePx = if (boardRect != Rect.Zero) {
        boardRect.width / gameState.config.columns
    } else {
        0f
    }
    val maxColumn = activePiece?.let { (gameState.config.columns - it.width).coerceAtLeast(0) } ?: 0
    val resolvedLockedColumn = lockedColumn?.coerceIn(0, maxColumn)
    val spawnColumn = scene.spawnColumn.coerceIn(0, maxColumn)
    val spawnTopLeft = pieceSpawnTopLeft(
        piece = activePiece,
        trayRect = trayRect,
        boardRect = boardRect,
        cellSizePx = cellSizePx,
        column = spawnColumn,
    )
    val autoColumns = remember(scene.autoColumns, maxColumn, spawnColumn) {
        scene.autoColumns
            .ifEmpty { listOf(spawnColumn) }
            .map { it.coerceIn(0, maxColumn) }
            .ifEmpty { listOf(spawnColumn) }
    }
    val pieceCellDp = with(density) { cellSizePx.coerceAtLeast(1f).toDp() }
    val launchCellCornerRadius =
        boardCellCornerRadiusDp(
            cellSize = pieceCellDp,
            style = boardStyle,
        )
    val trayPulseTransition = rememberInfiniteTransition(label = "tutorialTrayPulse")
    val trayPulsePhase by trayPulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TutorialDemoTrayPulseDurationMillis),
        ),
        label = "tutorialTrayPulsePhase",
    )
    val animatedOverlayX by animateFloatAsState(
        targetValue = overlayTopLeft?.x ?: 0f,
        animationSpec = if (isDragging) {
            snap()
        } else {
            tween(durationMillis = TutorialDemoTravelDurationMillis)
        },
        label = "tutorialOverlayX",
    )
    val animatedOverlayY by animateFloatAsState(
        targetValue = overlayTopLeft?.y ?: 0f,
        animationSpec = if (isDragging) {
            snap()
        } else {
            tween(durationMillis = TutorialDemoTravelDurationMillis)
        },
        label = "tutorialOverlayY",
    )
    val overlayPreviewTopLeft = overlayTopLeft?.let {
        Offset(x = animatedOverlayX, y = animatedOverlayY)
    }
    val selectedColumn = resolvedLockedColumn ?: resolveSelectedColumn(
        piece = activePiece,
        overlayTopLeft = overlayPreviewTopLeft,
        boardRect = boardRect,
        cellSizePx = cellSizePx,
        boardColumns = gameState.config.columns,
    )
    val placementPreview = remember(gameState, selectedColumn) {
        selectedColumn?.let { gameLogic.previewPlacement(gameState, it) }
    }
    val previewImpactPoints = remember(gameState, placementPreview) {
        gameLogic.previewImpactPoints(gameState, placementPreview)
    }
    val trayPieceScale = when {
        (activePiece == null) || (overlayTopLeft == null) || (resolvedLockedColumn != null) -> 1f
        isDragging -> 1f
        else -> 1f + (TutorialDemoTrayPulseScaleBoost * trayPulsePhase)
    }

    LaunchedEffect(
        activePiece?.id,
        boardRect,
        trayRect,
        resolvedLockedColumn,
        spawnColumn,
        cellSizePx
    ) {
        if (boardRect == Rect.Zero || trayRect == Rect.Zero || cellSizePx <= 0f) return@LaunchedEffect
        if (resolvedLockedColumn != null && gameState.clearAnimationToken != scene.gameState.clearAnimationToken) {
            return@LaunchedEffect
        }
        overlayTopLeft = pieceSpawnTopLeft(
            piece = activePiece,
            trayRect = trayRect,
            boardRect = boardRect,
            cellSizePx = cellSizePx,
            column = spawnColumn,
        )
    }

    LaunchedEffect(
        activePiece?.id,
        boardRect,
        trayRect,
        resolvedLockedColumn,
        isDragging,
        autoColumns
    ) {
        if (boardRect == Rect.Zero || trayRect == Rect.Zero || cellSizePx <= 0f) return@LaunchedEffect
        if (resolvedLockedColumn != null || isDragging || activePiece == null) return@LaunchedEffect
        delay(TutorialDemoAutoplayStartDelayMillis.milliseconds)
        var autoIndex = 0
        while (isActive && !isDragging) {
            val column = autoColumns[autoIndex % autoColumns.size]
            overlayTopLeft =
                pieceSpawnTopLeft(
                    piece = activePiece,
                    trayRect = trayRect,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                    column = column,
                )
            autoIndex += 1
            delay(TutorialDemoAutoplayStepDelayMillis.milliseconds)
        }
    }

    var isHandVisible by remember { mutableStateOf(true) }

    LaunchedEffect(scene, resolvedLockedColumn, boardRect, trayRect, cellSizePx, isDragging) {
        if (resolvedLockedColumn == null) return@LaunchedEffect
        if (boardRect == Rect.Zero || trayRect == Rect.Zero || cellSizePx <= 0f || isDragging) return@LaunchedEffect

        while (isActive && !isDragging) {
            gameState = scene.gameState
            isHandVisible = true
            overlayTopLeft =
                pieceSpawnTopLeft(
                    piece = scene.gameState.activePiece,
                    trayRect = trayRect,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                    column = spawnColumn,
                )
            delay(TutorialDemoAutoplayStartDelayMillis.milliseconds)

            overlayTopLeft =
                pieceSpawnTopLeft(
                    piece = scene.gameState.activePiece,
                    trayRect = trayRect,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                    column = resolvedLockedColumn,
                )
            delay(TutorialDemoTravelDurationMillis.milliseconds)

            val placed = gameLogic.placePiece(scene.gameState, resolvedLockedColumn)
            val lockedPreview = placed.state.softLock?.preview
            if (lockedPreview == null) {
                delay(TutorialDemoResetDelayMillis.milliseconds)
                continue
            }

            isHandVisible = false
            overlayTopLeft = lockedPreview.entryAnchor.toLocalTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
            delay((TutorialDemoTravelDurationMillis / 2).milliseconds)
            overlayTopLeft = lockedPreview.landingAnchor.toLocalTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
            delay((TutorialDemoTravelDurationMillis / 3).milliseconds)

            gameState = placed.state
            delay(TutorialDemoSoftLockDelayMillis.milliseconds)

            val committed = gameLogic.commitSoftLock(placed.state)
            gameState = committed.state
            overlayTopLeft = null
            delay(TutorialDemoResolutionHoldMillis.milliseconds)
            delay(TutorialDemoResetDelayMillis.milliseconds)
        }
    }

    TutorialMiniBoardShell(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    hostRectInRoot = coordinates.boundsInRoot()
                },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BoardGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows.toFloat())
                            .onGloballyPositioned { coordinates ->
                                boardRectInRoot = coordinates.boundsInRoot()
                            },
                        gameState = gameState,
                        preview = placementPreview,
                        impactedPreviewCells = previewImpactPoints,
                        guidedColumns = resolvedLockedColumn?.let(::setOf).orEmpty(),
                        activeColumn = selectedColumn,
                        activePiece = activePiece,
                        isDragging = isDragging || (resolvedLockedColumn == null && autoColumns.size > 1),
                        clearFlashDurationMillis = if (resolvedLockedColumn != null) {
                            TutorialDemoClearAnimationDurationMillis
                        } else {
                            420
                        },
                        boardShiftDurationMillis = if (resolvedLockedColumn != null) {
                            TutorialDemoBoardShiftDurationMillis
                        } else {
                            220
                        },
                    )

                    LaunchGuideLineOverlay(
                        preview = placementPreview,
                        activePiece = activePiece,
                        pieceTopLeft = overlayPreviewTopLeft,
                        boardRect = boardRect,
                        cellSizePx = cellSizePx,
                    )

                    if (badgeTone != null && badgeSpecial != SpecialBlockType.None) {
                        BlockCellPreview(
                            tone = badgeTone,
                            palette = settings.blockColorPalette,
                            style = boardStyle,
                            size = 18.dp,
                            special = badgeSpecial,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        )
                    }
                }

                TutorialMiniBottomDock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TutorialMiniDockHeight),
                    onTrayPositioned = { trayRectInRoot = it },
                )
            }

            if (activePiece != null && overlayTopLeft != null && cellSizePx > 0f) {
                PieceBlocks(
                    piece = activePiece,
                    cellSize = pieceCellDp,
                    cellCornerRadius = launchCellCornerRadius,
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = animatedOverlayX,
                            translationY = animatedOverlayY,
                            scaleX = trayPieceScale,
                            scaleY = trayPieceScale,
                            transformOrigin = TransformOrigin(0f, 0f),
                        )
                        .then(
                            if (resolvedLockedColumn == null) {
                                Modifier.pointerInput(
                                    activePiece.id,
                                    boardRect,
                                    cellSizePx,
                                ) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            overlayTopLeft = if (selectedColumn != null) {
                                                overlayTopLeft?.let { current ->
                                                    Offset(
                                                        columnToLeft(
                                                            selectedColumn,
                                                            boardRect,
                                                            cellSizePx
                                                        ), current.y
                                                    )
                                                }
                                            } else {
                                                spawnTopLeft
                                            }
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            overlayTopLeft = spawnTopLeft
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val current = overlayTopLeft ?: spawnTopLeft
                                            ?: return@detectDragGestures
                                            val minLeft = boardRect.left
                                            val maxLeft =
                                                (boardRect.right - (activePiece.width * cellSizePx)).coerceAtLeast(
                                                    minLeft
                                                )
                                            overlayTopLeft = Offset(
                                                (current.x + dragAmount.x).coerceIn(
                                                    minLeft,
                                                    maxLeft
                                                ),
                                                current.y,
                                            )
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                )

                val handX = with(density) { animatedOverlayX.toDp() }
                val handY = with(density) { animatedOverlayY.toDp() }

                TutorialDemoHand(
                    x = handX,
                    y = handY,
                    pieceWidth = activePiece.width,
                    pieceHeight = activePiece.height,
                    cellSize = pieceCellDp,
                    alpha = if ((isDragging || resolvedLockedColumn != null) && isHandVisible) 1f else 0.75f,
                )
            }
        }
    }
}

@Composable
private fun TutorialDemoHand(
    x: Dp,
    y: Dp,
    pieceWidth: Int,
    pieceHeight: Int,
    cellSize: Dp,
    alpha: Float,
) {
    val isDark = isBlockGamesDarkTheme(LocalAppSettings.current)
    val handColor = if (isDark) Color.White else Color(0xFF101114)
    val handSize = cellSize * 1.5f

    Box(
        modifier = Modifier
            .offset(
                x = x + (cellSize * pieceWidth / 2f) - (handSize * 0.38f),
                y = y + (cellSize * pieceHeight) - (cellSize * 0.42f),
            )
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = -12f
            },
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = Color.Black.copy(alpha = if (isDark) 0.4f else 0.2f),
            modifier = Modifier
                .offset(1.dp, 1.dp)
                .size(handSize),
        )
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = handColor,
            modifier = Modifier.size(handSize),
        )
    }
}

@Composable
private fun TutorialMiniBottomDock(
    onTrayPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    trayContent: @Composable BoxScope.() -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
            elevation = 10.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.68f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.launchGlow.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
                    .background(uiColors.panelMuted.copy(alpha = 0.48f))
                    .border(
                        width = 1.dp,
                        color = uiColors.boardEmptyCellBorder.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                    )
                    .onGloballyPositioned { coordinates ->
                        onTrayPositioned(coordinates.boundsInRoot())
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(GameUiShapeTokens.chipCorner))
                        .background(uiColors.launchGlow.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = uiColors.boardEmptyCellBorder.copy(alpha = 0.46f),
                            shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
                        ),
                )

                trayContent()
            }
        }
    }
}

@Composable
private fun TutorialTrayPieceChip(
    piece: Piece,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    onPositioned: ((Rect) -> Unit)? = null,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .then(
                if (onPositioned != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        onPositioned(coordinates.boundsInRoot())
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.7f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PieceBlocks(
                piece = piece,
                cellSize = 16.dp
            )
        }
    }
}

@Composable
private fun TutorialSection(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = Modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = uiColors.subtitle,
            )
            content()
        }
    }
}

@Composable
private fun TutorialPieceCard(
    title: String,
    piece: Piece,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted.copy(alpha = 0.76f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.68f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = uiColors.subtitle.copy(alpha = alpha.coerceIn(0f, 1f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            PieceBlocks(
                piece = piece,
                cellSize = 22.dp,
                alpha = alpha,
            )
        }
    }
}

@Composable
private fun TutorialActionTile(
    tone: CellTone,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val blockStyle = settings.blockVisualStyle
    val stylePulse = rememberBlockStylePulse(style = blockStyle)
    val iconTint = blockStyleIconTint(style = blockStyle)

    Column(
        modifier = modifier.widthIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BlockCellPreview(
                tone = tone,
                palette = settings.blockColorPalette,
                style = blockStyle,
                size = 40.dp,
                pulse = stylePulse,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TutorialSpecialCard(
    tone: CellTone,
    special: SpecialBlockType,
    title: String,
    body: String,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = Modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.70f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BlockCellPreview(
                tone = tone,
                palette = LocalAppSettings.current.blockColorPalette,
                style = LocalAppSettings.current.blockVisualStyle,
                size = 34.dp,
                special = special,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                )
            }
        }
    }
}

@Composable
private fun TutorialHintCard(text: String) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = Modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.hintCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.hintCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.62f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left - hostRect.left,
        top - hostRect.top,
        right - hostRect.left,
        bottom - hostRect.top,
    )
}

private fun GridPoint.toLocalTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

private fun tutorialProgress(
    phase: Float,
    start: Float,
    end: Float,
): Float = ((phase - start) / (end - start)).coerceIn(0f, 1f)

private fun lerpOffset(
    start: Offset,
    end: Offset,
    progress: Float,
): Offset = Offset(
    x = lerp(start.x, end.x, progress.coerceIn(0f, 1f)),
    y = lerp(start.y, end.y, progress.coerceIn(0f, 1f)),
)

@Preview(name = "Tutorial - Intro", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialIntroStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 0,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Systems", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialSystemsStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 1,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Specials", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialSpecialsStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 2,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Ready", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialReadyStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 3,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BlockWise - Intro", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBlockWiseIntroStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 0,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BlockWise - App Features", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBlockWiseAppFeaturesStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 1,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BlockWise - Systems", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBlockWiseSystemsStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 2,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BlockWise - Ready", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBlockWiseReadyStepPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 3,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Full Screen", widthDp = 412, heightDp = 915)
@Composable
private fun GameTutorialScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BoomBlocks - Intro", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBoomBlocksIntroStepPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.BoomBlocks
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 0,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BoomBlocks - Gravity", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBoomBlocksGravityStepPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.BoomBlocks
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 1,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - BoomBlocks - Ready", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialBoomBlocksReadyStepPreview() {
    GlobalPlatformConfig.gameplayStyle = GameplayStyle.BoomBlocks
    BlockGamesTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 3,
            onBack = {},
            onFinish = {},
        )
    }
}


