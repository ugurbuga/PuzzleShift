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
        val suffix = when (GlobalPlatformConfig.gameplayStyle) {
            GameplayStyle.StackShift -> ""
            GameplayStyle.BlockWise -> "BlockWise"
            GameplayStyle.ChainShift -> "ChainShift"
            GameplayStyle.MergeShift -> "MergeShift"
            GameplayStyle.BoomBlocks -> "BoomBlocks"
            GameplayStyle.BlockSort -> "BlockSort"
            GameplayStyle.WordShift -> "WordShift"
        }
        return when (mode) {
            GameMode.Classic -> "highScoreClassic$suffix"
            GameMode.TimeAttack -> "highScoreTimeAttack$suffix"
        }
    }
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
