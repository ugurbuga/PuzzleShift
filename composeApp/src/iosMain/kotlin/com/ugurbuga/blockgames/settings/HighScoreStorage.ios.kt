package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import platform.Foundation.NSUserDefaults

actual object HighScoreStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(mode: GameMode): Int =
        defaults.integerForKey(keyFor(mode)).toIntOrDefault(DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode) {
        defaults.setInteger(
            highScore.coerceAtLeast(DefaultHighScore).toLong(),
            forKey = keyFor(mode)
        )
    }

    private const val DefaultHighScore = 0

    private fun keyFor(mode: GameMode): String {
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        return when (gameplayStyle) {
            GameplayStyle.StackShift -> when (mode) {
                GameMode.Classic -> "highScoreClassic"
                GameMode.TimeAttack -> "highScoreTimeAttack"
            }

            GameplayStyle.BlockWise -> when (mode) {
                GameMode.Classic -> "highScoreClassicBlockWise"
                GameMode.TimeAttack -> "highScoreTimeAttackBlockWise"
            }

            GameplayStyle.ChainShift -> when (mode) {
                GameMode.Classic -> "highScoreClassicChainShift"
                GameMode.TimeAttack -> "highScoreTimeAttackChainShift"
            }

            GameplayStyle.MergeShift -> when (mode) {
                GameMode.Classic -> "highScoreClassicMergeShift"
                GameMode.TimeAttack -> "highScoreTimeAttackMergeShift"
            }

            GameplayStyle.BoomBlocks -> when (mode) {
                GameMode.Classic -> "highScoreClassicBoomBlocks"
                GameMode.TimeAttack -> "highScoreTimeAttackBoomBlocks"
            }

            GameplayStyle.BlockSort -> when (mode) {
                GameMode.Classic -> "highScoreClassicBlockSort"
                GameMode.TimeAttack -> "highScoreTimeAttackBlockSort"
            }
        }
    }
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
