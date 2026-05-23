package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.storageKey
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig

actual object HighScoreStorage {
    actual fun load(mode: GameMode): Int {
        val key = keyFor(mode)
        return BrowserStorage.get(key)?.toIntOrNull()
            ?: legacyKeyFor(mode)?.let(BrowserStorage::get)?.toIntOrNull()
            ?: 0
    }

    actual fun save(highScore: Int, mode: GameMode) {
        BrowserStorage.set(keyFor(mode), highScore.toString())
    }

    private fun keyFor(mode: GameMode): String =
        "${GlobalPlatformConfig.gameplayStyle.storageKey()}.highscore.${mode.name.lowercase()}"

    private fun legacyKeyFor(mode: GameMode): String? =
        if (GlobalPlatformConfig.gameplayStyle.name == "DigitShift") {
            "wordshift.highscore.${mode.name.lowercase()}"
        } else {
            null
        }
}

