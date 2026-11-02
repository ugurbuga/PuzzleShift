package com.ugurbuga.blockgames.ui.game.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ugurbuga.blockgames.game.model.BoardMatrix
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_basic_body
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_basic_title
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_gravity_body
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_gravity_title
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_large_body
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_large_title
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_strategic_body
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_strategic_title
import blockgames.composeapp.generated.resources.interactive_onboarding_boomblocks_tap_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_waiting
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingScene
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStage
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStateFactory
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import org.jetbrains.compose.resources.stringResource

@Immutable
data class BoomBlocksInteractiveGameOnboardingUi(
    val scene: BoomBlocksOnboardingScene,
    val currentStep: Int,
    val totalSteps: Int,
    val isAwaitingPlacementCommit: Boolean,
)

@Composable
fun BoomBlocksInteractiveGameOnboardingOverlay(
    ui: BoomBlocksInteractiveGameOnboardingUi,
    boardRect: Rect,
    cellSizePx: Float, onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val targetRect = remember(ui.scene, boardRect, cellSizePx) {
        boomBlocksOnboardingTargetRect(
            scene = ui.scene,
            boardRect = boardRect,
            cellSizePx = cellSizePx,
        )
    }
    val visualState = rememberBoomBlocksInteractiveOnboardingVisualState(ui = ui)

    Box(modifier = modifier.fillMaxSize()) {
        InteractiveOnboardingTargetHighlight(
            targetRect = targetRect,
            guideColor = visualState.guideColor,
            isSuccessState = false,
            isAwaitingPlacementCommit = ui.isAwaitingPlacementCommit,
        )

        BoomBlocksInteractiveOnboardingInfoCard(
            ui = ui,
            visualState = visualState,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun rememberBoomBlocksInteractiveOnboardingVisualState(
    ui: BoomBlocksInteractiveGameOnboardingUi,
): InteractiveOnboardingVisualState {
    val basicTitle = stringResource(Res.string.interactive_onboarding_boomblocks_basic_title)
    val largeTitle = stringResource(Res.string.interactive_onboarding_boomblocks_large_title)
    val gravityTitle = stringResource(Res.string.interactive_onboarding_boomblocks_gravity_title)
    val strategicTitle = stringResource(Res.string.interactive_onboarding_boomblocks_strategic_title)
    
    val basicBody = stringResource(Res.string.interactive_onboarding_boomblocks_basic_body)
    val largeBody = stringResource(Res.string.interactive_onboarding_boomblocks_large_body)
    val gravityBody = stringResource(Res.string.interactive_onboarding_boomblocks_gravity_body)
    val strategicBody = stringResource(Res.string.interactive_onboarding_boomblocks_strategic_body)
    
    val tapHint = stringResource(Res.string.interactive_onboarding_boomblocks_tap_hint)
    val waitingHint = stringResource(Res.string.interactive_onboarding_waiting)
    
    val uiColors = BlockGamesThemeTokens.uiColors
    
    return remember(
        ui,
        uiColors.warning,
        uiColors.guideAccent,
        basicTitle,
        largeTitle,
        gravityTitle,
        basicBody,
        largeBody,
        gravityBody,
        tapHint,
        waitingHint,
    ) {
        val title = when (ui.scene.stage) {
            BoomBlocksOnboardingStage.BasicExplosion -> basicTitle
            BoomBlocksOnboardingStage.LargeExplosion -> largeTitle
            BoomBlocksOnboardingStage.GravityShift -> gravityTitle
            BoomBlocksOnboardingStage.StrategicClears -> strategicTitle
        }
        val body = when (ui.scene.stage) {
            BoomBlocksOnboardingStage.BasicExplosion -> basicBody
            BoomBlocksOnboardingStage.LargeExplosion -> largeBody
            BoomBlocksOnboardingStage.GravityShift -> gravityBody
            BoomBlocksOnboardingStage.StrategicClears -> strategicBody
        }
        val hint = if (ui.isAwaitingPlacementCommit) waitingHint else tapHint
        val guideColor = if (ui.isAwaitingPlacementCommit) uiColors.warning else uiColors.guideAccent
        
        InteractiveOnboardingVisualState(
            title = title,
            body = body,
            hint = hint,
            guideColor = guideColor,
            guideBadgeTextColor = if (guideColor.luminance() > 0.58f) Color(0xFF101114) else Color.White,
            hintContainerAlpha = 0.20f,
            hintBorderAlpha = 0.58f,
            isSuccessState = false,
        )
    }
}

@Composable
internal fun BoomBlocksInteractiveOnboardingInfoCard(
    ui: BoomBlocksInteractiveGameOnboardingUi,
    visualState: InteractiveOnboardingVisualState = rememberBoomBlocksInteractiveOnboardingVisualState(ui),
    onBack: (() -> Unit)? = null,
    stylePulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    InteractiveOnboardingInfoCard(
        currentStep = ui.currentStep,
        totalSteps = ui.totalSteps,
        visualState = visualState,
        onBack = onBack,
        stylePulse = stylePulse,
        modifier = modifier,
    )
}

private fun boomBlocksOnboardingTargetRect(
    scene: BoomBlocksOnboardingScene,
    boardRect: Rect,
    cellSizePx: Float,
): Rect? {
    if ((boardRect == Rect.Zero) || (cellSizePx <= 0f)) return null
    val guidePoint = scene.guidePoint ?: return null
    
    // For BoomBlocks, we want to highlight the connected group at guidePoint
    val group = findConnectedGroup(scene.gameState.board, guidePoint)
    if (group.isEmpty()) return null
    
    val minColumn = group.minOf { it.column }
    val maxColumn = group.maxOf { it.column }
    val minRow = group.minOf { it.row }
    val maxRow = group.maxOf { it.row }
    
    return Rect(
        left = boardRect.left + (minColumn * cellSizePx),
        top = boardRect.top + (minRow * cellSizePx),
        right = boardRect.left + ((maxColumn + 1) * cellSizePx),
        bottom = boardRect.top + ((maxRow + 1) * cellSizePx),
    ).expand(padding = 4f)
}

private fun findConnectedGroup(
    board: BoardMatrix,
    start: GridPoint
): Set<GridPoint> {
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

private fun Rect.expand(padding: Float): Rect = Rect(
    left = left - padding,
    top = top - padding,
    right = right + padding,
    bottom = bottom + padding,
)

@Preview(name = "BoomBlocks Onboarding Card")
@Composable
private fun BoomBlocksInteractiveOnboardingInfoCardPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            BoomBlocksInteractiveOnboardingInfoCard(
                ui = BoomBlocksInteractiveGameOnboardingUi(
                    scene = BoomBlocksOnboardingStateFactory.scene(BoomBlocksOnboardingStage.BasicExplosion),
                    currentStep = 1,
                    totalSteps = 3,
                    isAwaitingPlacementCommit = false,
                ),
            )
        }
    }
}
