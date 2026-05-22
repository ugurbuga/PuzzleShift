package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig

actual object HighScoreStorage {
    actual fun load(mode: GameMode): Int = BrowserStorage.get(keyFor(mode))?.toIntOrNull() ?: 0

    actual fun save(highScore: Int, mode: GameMode) {
        BrowserStorage.set(keyFor(mode), highScore.toString())
    }

    private fun keyFor(mode: GameMode): String {
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        return when (gameplayStyle) {
        GameplayStyle.StackShift -> when (mode) {
            GameMode.Classic -> "stackshift.highscore.classic"
            GameMode.TimeAttack -> "stackshift.highscore.time_attack"
        }
        GameplayStyle.BlockWise -> when (mode) {
            GameMode.Classic -> "blockwise.highscore.classic"
            GameMode.TimeAttack -> "blockwise.highscore.time_attack"
        }
        GameplayStyle.ChainShift -> when (mode) {
            GameMode.Classic -> "chainshift.highscore.classic"
            GameMode.TimeAttack -> "chainshift.highscore.time_attack"
        }
        GameplayStyle.MergeShift -> when (mode) {
            GameMode.Classic -> "mergeshift.highscore.classic"
            GameMode.TimeAttack -> "mergeshift.highscore.time_attack"
        }
        GameplayStyle.BoomBlocks -> when (mode) {
            GameMode.Classic -> "boomblocks.highscore.classic"
            GameMode.TimeAttack -> "boomblocks.highscore.time_attack"
        }
        GameplayStyle.BlockSort -> when (mode) {
            GameMode.Classic -> "blocksort.highscore.classic"
            GameMode.TimeAttack -> "blocksort.highscore.time_attack"
        }
        }
    }
}

