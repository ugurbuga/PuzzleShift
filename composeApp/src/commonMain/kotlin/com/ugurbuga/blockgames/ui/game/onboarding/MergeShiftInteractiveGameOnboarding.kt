package com.ugurbuga.blockgames.ui.game.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.interactive_onboarding_aim_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_launch_body
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_launch_title
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_merge_horizontal_body
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_merge_horizontal_title
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_merge_multi_body
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_merge_multi_title
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_merge_vertical_body
import blockgames.composeapp.generated.resources.interactive_onboarding_mergeshift_merge_vertical_title
import blockgames.composeapp.generated.resources.interactive_onboarding_target_locked
import blockgames.composeapp.generated.resources.interactive_onboarding_waiting
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingScene
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStateFactory
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import org.jetbrains.compose.resources.stringResource

@Immutable
data class MergeShiftInteractiveGameOnboardingUi(
    val scene: MergeShiftOnboardingScene,
    val currentStep: Int,
    val totalSteps: Int,
    val isTargetAligned: Boolean,
    val isAwaitingPlacementCommit: Boolean,
)

@Composable
internal fun rememberMergeShiftInteractiveOnboardingVisualState(
    ui: MergeShiftInteractiveGameOnboardingUi,
): InteractiveOnboardingVisualState {
    val launchTitle = stringResource(Res.string.interactive_onboarding_mergeshift_launch_title)
    val verticalMergeTitle = stringResource(Res.string.interactive_onboarding_mergeshift_merge_vertical_title)
    val horizontalMergeTitle = stringResource(Res.string.interactive_onboarding_mergeshift_merge_horizontal_title)
    val multiMergeTitle = stringResource(Res.string.interactive_onboarding_mergeshift_merge_multi_title)

    val launchBody = stringResource(Res.string.interactive_onboarding_mergeshift_launch_body)
    val verticalMergeBody = stringResource(Res.string.interactive_onboarding_mergeshift_merge_vertical_body)
    val horizontalMergeBody = stringResource(Res.string.interactive_onboarding_mergeshift_merge_horizontal_body)
    val multiMergeBody = stringResource(Res.string.interactive_onboarding_mergeshift_merge_multi_body)

    val waitingHint = stringResource(Res.string.interactive_onboarding_waiting)
    val targetLockedHint = stringResource(Res.string.interactive_onboarding_target_locked)
    val aimHint = stringResource(Res.string.interactive_onboarding_aim_hint)
    
    val uiColors = BlockGamesThemeTokens.uiColors
    
    return remember(
        ui,
        uiColors.warning,
        uiColors.success,
        uiColors.guideAccent,
        launchTitle,
        verticalMergeTitle,
        horizontalMergeTitle,
        multiMergeTitle,
        launchBody,
        verticalMergeBody,
        horizontalMergeBody,
        multiMergeBody,
        waitingHint,
        targetLockedHint,
        aimHint,
    ) {
        val isSuccessState = ui.isTargetAligned
        val title = when (ui.scene.stage) {
            MergeShiftOnboardingStage.Launch -> launchTitle
            MergeShiftOnboardingStage.VerticalMerge -> verticalMergeTitle
            MergeShiftOnboardingStage.HorizontalMerge -> horizontalMergeTitle
            MergeShiftOnboardingStage.MultiMerge -> multiMergeTitle
        }
        val body = when (ui.scene.stage) {
            MergeShiftOnboardingStage.Launch -> launchBody
            MergeShiftOnboardingStage.VerticalMerge -> verticalMergeBody
            MergeShiftOnboardingStage.HorizontalMerge -> horizontalMergeBody
            MergeShiftOnboardingStage.MultiMerge -> multiMergeBody
        }
        val hint = when {
            ui.isAwaitingPlacementCommit -> waitingHint
            isSuccessState -> targetLockedHint
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
internal fun MergeShiftOnboardingTargetOverlay(
    ui: MergeShiftInteractiveGameOnboardingUi,
    boardRect: Rect,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val guideColumn = ui.scene.guideColumn ?: return
    val targetColor = if (ui.isTargetAligned) {
        BlockGamesThemeTokens.uiColors.success
    } else {
        BlockGamesThemeTokens.uiColors.guideAccent
    }

    // In MergeShift, we highlight the cell where it will land.
    var landingRow: Int? = null
    for (r in 0 until ui.scene.gameState.config.rows) {
        if (ui.scene.gameState.board.cellAt(guideColumn, r) == null) {
            landingRow = r
            break
        }
    }
    if (landingRow == null) return

    val targetRect = Rect(
        left = boardRect.left + guideColumn * cellSizePx,
        top = boardRect.top + landingRow * cellSizePx,
        right = boardRect.left + (guideColumn + 1) * cellSizePx,
        bottom = boardRect.top + (landingRow + 1) * cellSizePx
    )

    InteractiveOnboardingTargetHighlight(
        targetRect = targetRect,
        guideColor = targetColor,
        isSuccessState = ui.isTargetAligned,
        isAwaitingPlacementCommit = ui.isAwaitingPlacementCommit,
        modifier = modifier,
    )
}

@Preview(name = "MergeShift Onboarding Info Card")
@Composable
private fun MergeShiftOnboardingInfoCardPreview() {
    val scene = MergeShiftOnboardingStateFactory.scene(MergeShiftOnboardingStage.Launch)
    val ui = MergeShiftInteractiveGameOnboardingUi(
        scene = scene,
        currentStep = 1,
        totalSteps = 4,
        isTargetAligned = false,
        isAwaitingPlacementCommit = false,
    )
    val visualState = rememberMergeShiftInteractiveOnboardingVisualState(ui = ui)
    BlockGamesTheme(settings = AppSettings()) {
        InteractiveOnboardingInfoCard(
            currentStep = ui.currentStep,
            totalSteps = ui.totalSteps,
            visualState = visualState,
        )
    }
}


