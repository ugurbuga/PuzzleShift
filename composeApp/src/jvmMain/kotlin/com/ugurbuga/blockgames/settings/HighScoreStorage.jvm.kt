package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import java.util.prefs.Preferences

actual object HighScoreStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(mode: GameMode): Int {
        val key = keyFor(mode)
        if (prefs.get(key, null) != null) return prefs.getInt(key, DefaultHighScore)
        return legacyKeyFor(mode)?.let { legacyKey ->
            prefs.getInt(legacyKey, DefaultHighScore)
        } ?: DefaultHighScore
    }

    actual fun save(highScore: Int, mode: GameMode) {
        prefs.putInt(keyFor(mode), highScore.coerceAtLeast(DefaultHighScore))
    }

    private const val Namespace = "com.ugurbuga.blockgames.high_score"
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
