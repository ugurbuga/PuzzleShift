package com.ugurbuga.blockgames.settings

import android.content.Context
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig

actual object GameSessionStorage {
    private const val Namespace = "com.ugurbuga.blockgames.game_session"

    private val prefs by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(slot: GameSessionSlot): GameState? {
        val serialized = prefs.getString(keyFor(slot), null) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear(slot) }
    }

    actual fun save(slot: GameSessionSlot, state: GameState) {
        prefs.edit()
            .putString(keyFor(slot), GameSessionCodec.encode(state))
            .apply()
    }

    actual fun clear(slot: GameSessionSlot) {
        prefs.edit()
            .remove(keyFor(slot))
            .apply()
    }

    actual fun clear() {
        val keysToRemove = prefs.all.keys.filter {
            it == "gameState_classic" ||
                it.startsWith("gameState_classic_") ||
                it == "gameState_time_attack" ||
                it.startsWith("gameState_time_attack_") ||
                it.startsWith("gameState_daily_challenge_")
        }

        if (keysToRemove.isNotEmpty()) {
            val editor = prefs.edit()
            keysToRemove.forEach(editor::remove)
            editor.apply()
        }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val allowedKeys = allowedDateIds.map { "gameState_daily_challenge_${style}_$it" }.toSet()
        val prefix = "gameState_daily_challenge_${style}_"
        val toRemove = prefs.all.keys
            .filter { it.startsWith(prefix) && it !in allowedKeys }

        if (toRemove.isNotEmpty()) {
            val editor = prefs.edit()
            toRemove.forEach { editor.remove(it) }
            editor.apply()
        }
    }

    actual fun getSavedDailyChallengeDays(yearMonth: String): Set<Int> {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val prefix = "gameState_daily_challenge_${style}_$yearMonth-"
        return prefs.all.keys
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
            .toSet()
    }

    private fun keyFor(slot: GameSessionSlot): String = "gameState_${slot.key}"
}
