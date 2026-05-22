package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import java.util.prefs.Preferences

actual object HighScoreStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(mode: GameMode): Int = prefs.getInt(keyFor(mode), DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode) {
        prefs.putInt(keyFor(mode), highScore.coerceAtLeast(DefaultHighScore))
    }

    private const val Namespace = "com.ugurbuga.blockgames.high_score"
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
