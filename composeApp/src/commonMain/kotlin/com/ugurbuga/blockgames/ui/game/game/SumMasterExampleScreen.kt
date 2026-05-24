package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStage
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStateFactory

@Composable
internal fun SumShiftExampleScreen() {
    SumShiftGameScreen(
        gameState = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStage.FinishPuzzle).gameState,
        onTapCell = {},
        onRestart = {},
        onBack = {},
        highestScore = 1240,
    )
}

@Preview(name = "SumShift Example", widthDp = 412, heightDp = 915)
@Composable
private fun SumShiftExampleScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        SumShiftExampleScreen()
    }
}
