package com.ugurbuga.blockgames.ui.game.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.interactive_onboarding_aim_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_column_clearer_body
import blockgames.composeapp.generated.resources.interactive_onboarding_column_clearer_title
import blockgames.composeapp.generated.resources.interactive_onboarding_drag_body
import blockgames.composeapp.generated.resources.interactive_onboarding_drag_ready
import blockgames.composeapp.generated.resources.interactive_onboarding_drag_title
import blockgames.composeapp.generated.resources.interactive_onboarding_drag_to_board
import blockgames.composeapp.generated.resources.interactive_onboarding_heavy_body
import blockgames.composeapp.generated.resources.interactive_onboarding_heavy_title
import blockgames.composeapp.generated.resources.interactive_onboarding_line_clear_body
import blockgames.composeapp.generated.resources.interactive_onboarding_line_clear_title
import blockgames.composeapp.generated.resources.interactive_onboarding_target_locked
import blockgames.composeapp.generated.resources.interactive_onboarding_waiting
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingScene
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStage
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import com.ugurbuga.blockgames.settings.StackShiftOnboardingTarget
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import org.jetbrains.compose.resources.stringResource

@Immutable
data class BlockWiseInteractiveGameOnboardingUi(
    val scene: BlockWiseOnboardingScene,
    val currentStep: Int,
    val totalSteps: Int,
    val hasDraggedAwayFromSpawn: Boolean,
    val isTargetAligned: Boolean,
    val isAwaitingPlacementCommit: Boolean,
)

private val onboardingLogic = GameLogic.create()


@Composable
internal fun BlockWiseOnboardingTargetOverlay(
    ui: BlockWiseInteractiveGameOnboardingUi,
    boardRect: Rect,
    trayRect: Rect,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val suppressTargetRect = (ui.scene.target == StackShiftOnboardingTarget.Tray) && ui.hasDraggedAwayFromSpawn
    val targetRect = remember(ui.scene, boardRect, trayRect, cellSizePx, suppressTargetRect) {
        if (suppressTargetRect) {
            null
        } else {
            blockWiseOnboardingTargetRect(
                scene = ui.scene,
                boardRect = boardRect,
                trayRect = trayRect,
                cellSizePx = cellSizePx,
            )
        }
    }
    val visualState = rememberBlockWiseInteractiveOnboardingVisualState(ui = ui)
    Box(modifier = modifier.fillMaxSize()) {
        InteractiveOnboardingTargetHighlight(
            targetRect = targetRect,
            guideColor = visualState.guideColor,
            isSuccessState = visualState.isSuccessState,
            isAwaitingPlacementCommit = ui.isAwaitingPlacementCommit,
        )
    }
}

@Composable
internal fun rememberBlockWiseInteractiveOnboardingVisualState(
    ui: BlockWiseInteractiveGameOnboardingUi,
): InteractiveOnboardingVisualState {
    val dragTitle = stringResource(Res.string.interactive_onboarding_drag_title)
    val lineClearTitle = stringResource(Res.string.interactive_onboarding_line_clear_title)
    val columnClearTitle = stringResource(Res.string.interactive_onboarding_column_clearer_title)
    val crossClearTitle = stringResource(Res.string.interactive_onboarding_heavy_title)
    val dragBody = stringResource(Res.string.interactive_onboarding_drag_body)
    val lineClearBody = stringResource(Res.string.interactive_onboarding_line_clear_body)
    val columnClearBody = stringResource(Res.string.interactive_onboarding_column_clearer_body)
    val crossClearBody = stringResource(Res.string.interactive_onboarding_heavy_body)
    val waitingHint = stringResource(Res.string.interactive_onboarding_waiting)
    val targetLockedHint = stringResource(Res.string.interactive_onboarding_target_locked)
    val dragReadyHint = stringResource(Res.string.interactive_onboarding_drag_ready)
    val dragToBoardHint = stringResource(Res.string.interactive_onboarding_drag_to_board)
    val aimHint = stringResource(Res.string.interactive_onboarding_aim_hint)
    val uiColors = BlockGamesThemeTokens.uiColors
    return remember(
        ui,
        uiColors.warning,
        uiColors.success,
        uiColors.guideAccent,
        dragTitle,
        lineClearTitle,
        columnClearTitle,
        crossClearTitle,
        dragBody,
        lineClearBody,
        columnClearBody,
        crossClearBody,
        waitingHint,
        targetLockedHint,
        dragReadyHint,
        dragToBoardHint,
        aimHint,
    ) {
        val isSuccessState = ui.isTargetAligned
        val title = when (ui.scene.stage) {
            BlockWiseOnboardingStage.DragToBoard -> dragTitle
            BlockWiseOnboardingStage.LineClear -> lineClearTitle
            BlockWiseOnboardingStage.ColumnClear -> columnClearTitle
            BlockWiseOnboardingStage.CrossClear -> crossClearTitle
        }
        val body = when (ui.scene.stage) {
            BlockWiseOnboardingStage.DragToBoard -> dragBody
            BlockWiseOnboardingStage.LineClear -> lineClearBody
            BlockWiseOnboardingStage.ColumnClear -> columnClearBody
            BlockWiseOnboardingStage.CrossClear -> crossClearBody
        }
        val hint = when {
            ui.isAwaitingPlacementCommit -> waitingHint
            isSuccessState -> {
                if (ui.scene.stage == BlockWiseOnboardingStage.DragToBoard) dragReadyHint else targetLockedHint
            }
            ui.scene.stage == BlockWiseOnboardingStage.DragToBoard -> dragToBoardHint
            else -> aimHint
        }
        val guideColor = when {
            ui.isAwaitingPlacementCommit -> uiColors.warning
            isSuccessState -> uiColors.success
            else -> uiColors.guideAccent
        }
        InteractiveOnboardingVisualState(
            title = title,
            body = body,
            hint = hint,
            guideColor = guideColor,
            guideBadgeTextColor = if (guideColor.luminance() > 0.58f) Color(0xFF101114) else Color.White,
            hintContainerAlpha = 0.20f,
            hintBorderAlpha = 0.58f,
            isSuccessState = isSuccessState,
        )
    }
}

@Composable
internal fun BlockWiseInteractiveOnboardingInfoCard(
    ui: BlockWiseInteractiveGameOnboardingUi,
    visualState: InteractiveOnboardingVisualState = rememberBlockWiseInteractiveOnboardingVisualState(ui),
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



private fun blockWiseOnboardingTargetRect(
    scene: BlockWiseOnboardingScene,
    boardRect: Rect,
    trayRect: Rect,
    cellSizePx: Float,
): Rect? = when (scene.target) {
    StackShiftOnboardingTarget.Tray -> {
        if (trayRect != Rect.Zero) {
            trayRect.expand(horizontalPadding = -14f, verticalPadding = -10f)
        } else null
    }
    StackShiftOnboardingTarget.Board -> {
        if ((boardRect == Rect.Zero) || (cellSizePx <= 0f)) return null
        blockWiseOnboardingTargetPreview(scene)?.toBoardRect(boardRect, cellSizePx)?.expand(padding = 8f)
            ?: boardRect.expand(padding = 6f)
    }
}

private fun blockWiseOnboardingTargetPreview(scene: BlockWiseOnboardingScene): PlacementPreview? {
    val point = scene.guidePoint ?: return null
    val pieceId = scene.gameState.activePiece?.id ?: return null
    return onboardingLogic.previewPlacement(scene.gameState, pieceId, point)
}

private fun PlacementPreview.toBoardRect(boardRect: Rect, cellSizePx: Float): Rect? {
    if (occupiedCells.isEmpty() || (boardRect == Rect.Zero) || (cellSizePx <= 0f)) return null
    val minColumn = occupiedCells.minOf { it.column }
    val maxColumn = occupiedCells.maxOf { it.column }
    val minRow = occupiedCells.minOf { it.row }
    val maxRow = occupiedCells.maxOf { it.row }
    return Rect(
        left = boardRect.left + (minColumn * cellSizePx),
        top = boardRect.top + (minRow * cellSizePx),
        right = boardRect.left + ((maxColumn + 1) * cellSizePx),
        bottom = boardRect.top + ((maxRow + 1) * cellSizePx),
    )
}

private fun Rect.expand(padding: Float): Rect = Rect(
    left = left - padding,
    top = top - padding,
    right = right + padding,
    bottom = bottom + padding,
)

private fun Rect.expand(horizontalPadding: Float, verticalPadding: Float): Rect = Rect(
    left = left - horizontalPadding,
    top = top - verticalPadding,
    right = right + horizontalPadding,
    bottom = bottom + verticalPadding,
)


@Preview(name = "Onboarding Card - Default")
@Composable
private fun BlockWiseInteractiveOnboardingInfoCardPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            BlockWiseInteractiveOnboardingInfoCard(
                ui = BlockWiseInteractiveGameOnboardingUi(
                    scene = BlockWiseOnboardingStateFactory.scene(BlockWiseOnboardingStage.DragToBoard),
                    currentStep = 1,
                    totalSteps = 4,
                    hasDraggedAwayFromSpawn = false,
                    isTargetAligned = false,
                    isAwaitingPlacementCommit = false,
                ),
            )
        }
    }
}

@Preview(name = "Onboarding Card - Success")
@Composable
private fun BlockWiseInteractiveOnboardingInfoCardSuccessPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            BlockWiseInteractiveOnboardingInfoCard(
                ui = BlockWiseInteractiveGameOnboardingUi(
                    scene = BlockWiseOnboardingStateFactory.scene(BlockWiseOnboardingStage.DragToBoard),
                    currentStep = 1,
                    totalSteps = 4,
                    hasDraggedAwayFromSpawn = true,
                    isTargetAligned = true,
                    isAwaitingPlacementCommit = false,
                ),
            )
        }
    }
}
