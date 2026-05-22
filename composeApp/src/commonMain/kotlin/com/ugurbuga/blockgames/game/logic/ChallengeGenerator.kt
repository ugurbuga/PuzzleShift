package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import kotlin.random.Random

object ChallengeGenerator {
    fun generate(
        year: Int,
        month: Int,
        day: Int,
    ): DailyChallenge {
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        val seed = (year * 10000 + month * 100 + day).toLong()
        val random = Random(seed)

        val types = ChallengeTaskType.forStyle(gameplayStyle).toMutableList()
        if (types.isEmpty()) {
            return DailyChallenge(year, month, day, gameplayStyle, emptyList())
        }
        val taskCount = random.nextInt(2, 4).coerceAtMost(types.size)
        val tasks = mutableListOf<ChallengeTask>()

        repeat(taskCount) {
            val type = types.removeAt(random.nextInt(types.size))
            val target = when (type) {
                ChallengeTaskType.ClearBlocks -> when (gameplayStyle) {
                    GameplayStyle.BoomBlocks -> (random.nextInt(30, 80) * 10)
                    GameplayStyle.ChainShift -> (random.nextInt(8, 18) * 10)
                    else -> (random.nextInt(10, 25) * 10)
                }
                ChallengeTaskType.ReachScore -> when (gameplayStyle) {
                    GameplayStyle.StackShift -> (random.nextInt(5, 20) * 1000)
                    GameplayStyle.BlockWise -> random.nextInt(2, 8) * 1000
                    GameplayStyle.ChainShift -> random.nextInt(4, 12) * 1000
                    GameplayStyle.MergeShift -> (random.nextInt(5, 15) * 1000)
                    GameplayStyle.BoomBlocks -> random.nextInt(15, 40) * 1000
                    GameplayStyle.BlockSort -> random.nextInt(3, 10) * 1000
                }
                ChallengeTaskType.TriggerSpecial -> random.nextInt(2, 6)
                ChallengeTaskType.PerfectPlacement -> random.nextInt(10, 20)
                ChallengeTaskType.ChainReaction -> random.nextInt(1, 3)
                ChallengeTaskType.ClearRows -> random.nextInt(3, 9)
                ChallengeTaskType.ClearColumns -> when (gameplayStyle) {
                    GameplayStyle.BlockSort -> random.nextInt(4, 10)
                    else -> random.nextInt(3, 9)
                }
                ChallengeTaskType.PlacePieces -> when (gameplayStyle) {
                    GameplayStyle.BoomBlocks -> random.nextInt(40, 100)
                    GameplayStyle.ChainShift -> random.nextInt(10, 24)
                    GameplayStyle.BlockSort -> random.nextInt(10, 24)
                    else -> random.nextInt(12, 28)
                }
                ChallengeTaskType.ClearRounds -> random.nextInt(1, 4)
                ChallengeTaskType.ClearBothDirections -> random.nextInt(1, 4)
            }
            tasks.add(ChallengeTask(type, target))
        }

        return DailyChallenge(year, month, day, gameplayStyle, tasks)
    }
}
