package com.ugurbuga.blockgames.ui.game.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_chain_body
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_chain_title
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_intro_body
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_intro_title
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_match_body
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_match_title
import blockgames.composeapp.generated.resources.interactive_onboarding_chainshift_tap_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_waiting
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingScene
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStage
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import org.jetbrains.compose.resources.stringResource

@Immutable
data class ChainShiftInteractiveGameOnboardingUi(
    val scene: ChainShiftOnboardingScene,
    val currentStep: Int,
    val totalSteps: Int,
    val isAwaitingPlacementCommit: Boolean,
)

@Composable
fun ChainShiftInteractiveGameOnboardingOverlay(
    ui: ChainShiftInteractiveGameOnboardingUi,
    boardRect: Rect,
    cellSizePx: Float,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val targetRect = remember(ui.scene, boardRect, cellSizePx) {
        if (boardRect == Rect.Zero || cellSizePx <= 0f) {
            null
        } else {
            val point = ui.scene.guidePoint
            Rect(
                left = boardRect.left + (point.column * cellSizePx),
                top = boardRect.top + (point.row * cellSizePx),
                right = boardRect.left + ((point.column + 1) * cellSizePx),
                bottom = boardRect.top + ((point.row + 1) * cellSizePx),
            )
        }
    }
    val visualState = rememberChainShiftInteractiveOnboardingVisualState(ui)

    Box(modifier = modifier.fillMaxSize()) {
        InteractiveOnboardingTargetHighlight(
            targetRect = targetRect,
            guideColor = visualState.guideColor,
            isSuccessState = false,
            isAwaitingPlacementCommit = ui.isAwaitingPlacementCommit,
        )
        InteractiveOnboardingInfoCard(
            currentStep = ui.currentStep,
            totalSteps = ui.totalSteps,
            visualState = visualState,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun rememberChainShiftInteractiveOnboardingVisualState(
    ui: ChainShiftInteractiveGameOnboardingUi,
): InteractiveOnboardingVisualState {
    val introTitle = stringResource(Res.string.interactive_onboarding_chainshift_intro_title)
    val matchTitle = stringResource(Res.string.interactive_onboarding_chainshift_match_title)
    val chainTitle = stringResource(Res.string.interactive_onboarding_chainshift_chain_title)
    val introBody = stringResource(Res.string.interactive_onboarding_chainshift_intro_body)
    val matchBody = stringResource(Res.string.interactive_onboarding_chainshift_match_body)
    val chainBody = stringResource(Res.string.interactive_onboarding_chainshift_chain_body)
    val tapHint = stringResource(Res.string.interactive_onboarding_chainshift_tap_hint)
    val waitingHint = stringResource(Res.string.interactive_onboarding_waiting)
    val uiColors = BlockGamesThemeTokens.uiColors

    return remember(
        ui,
        introTitle,
        matchTitle,
        chainTitle,
        introBody,
        matchBody,
        chainBody,
        tapHint,
        waitingHint,
        uiColors.guideAccent,
        uiColors.warning,
    ) {
        val title = when (ui.scene.stage) {
            ChainShiftOnboardingStage.FirstInsert -> introTitle
            ChainShiftOnboardingStage.CreateMatch -> matchTitle
            ChainShiftOnboardingStage.TriggerChain -> chainTitle
        }
        val body = when (ui.scene.stage) {
            ChainShiftOnboardingStage.FirstInsert -> introBody
            ChainShiftOnboardingStage.CreateMatch -> matchBody
            ChainShiftOnboardingStage.TriggerChain -> chainBody
        }
        val guideColor = if (ui.isAwaitingPlacementCommit) uiColors.warning else uiColors.guideAccent
        InteractiveOnboardingVisualState(
            title = title,
            body = body,
            hint = if (ui.isAwaitingPlacementCommit) waitingHint else tapHint,
            guideColor = guideColor,
            guideBadgeTextColor = if (guideColor.luminance() > 0.58f) Color(0xFF101114) else Color.White,
            hintContainerAlpha = 0.20f,
            hintBorderAlpha = 0.58f,
            isSuccessState = false,
        )
    }
}

