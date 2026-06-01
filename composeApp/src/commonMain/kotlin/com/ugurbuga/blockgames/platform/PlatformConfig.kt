package com.ugurbuga.blockgames.platform

import com.ugurbuga.blockgames.game.model.GameplayStyle
import kotlin.concurrent.Volatile

object GlobalPlatformConfig {
    @Volatile var isDebug: Boolean = false
    @Volatile var gameplayStyle: GameplayStyle = GameplayStyle.StackShift
}
