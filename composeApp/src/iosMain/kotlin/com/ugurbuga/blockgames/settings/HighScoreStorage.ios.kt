package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import platform.Foundation.NSUserDefaults

actual object HighScoreStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(mode: GameMode): Int {
        val key = keyFor(mode)
        if (defaults.objectForKey(key) != null) {
            return defaults.integerForKey(key).toIntOrDefault(DefaultHighScore)
        }
        val legacyKey = legacyKeyFor(mode) ?: return DefaultHighScore
        return defaults.integerForKey(legacyKey).toIntOrDefault(DefaultHighScore)
    }

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
            GameplayStyle.DigitShift -> "DigitShift"
            GameplayStyle.SumShift -> "SumShift"
        }
        return when (mode) {
            GameMode.Classic -> "highScoreClassic$suffix"
            GameMode.TimeAttack -> "highScoreTimeAttack$suffix"
        }
    }

    private fun legacyKeyFor(mode: GameMode): String? = when (GlobalPlatformConfig.gameplayStyle) {
        GameplayStyle.DigitShift -> when (mode) {
            GameMode.Classic -> "highScoreClassicWordShift"
            GameMode.TimeAttack -> "highScoreTimeAttackWordShift"
        }
        else -> null
    }
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
