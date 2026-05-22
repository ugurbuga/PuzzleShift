package com.ugurbuga.blockgames.game.model

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig

@Immutable
enum class ChallengeTaskType(
    val stableId: String,
    val supportedStyles: Set<GameplayStyle>,
) {
    ClearBlocks(
        stableId = "clear_blocks",
        supportedStyles = setOf(GameplayStyle.StackShift, GameplayStyle.ChainShift),
    ),
    ReachScore(
        stableId = "reach_score",
        supportedStyles = GameplayStyle.entries.toSet(),
    ),
    TriggerSpecial(
        stableId = "trigger_special",
        supportedStyles = setOf(GameplayStyle.StackShift, GameplayStyle.MergeShift),
    ),
    PerfectPlacement(
        stableId = "perfect_placement",
        supportedStyles = setOf(GameplayStyle.StackShift),
    ),
    ChainReaction(
        stableId = "chain_reaction",
        supportedStyles = setOf(GameplayStyle.StackShift, GameplayStyle.ChainShift),
    ),
    ClearRows(
        stableId = "clear_rows",
        supportedStyles = setOf(GameplayStyle.BlockWise),
    ),
    ClearColumns(
        stableId = "clear_columns",
        supportedStyles = setOf(GameplayStyle.BlockWise, GameplayStyle.BlockSort),
    ),
    PlacePieces(
        stableId = "place_pieces",
        supportedStyles = setOf(GameplayStyle.BlockWise, GameplayStyle.BoomBlocks, GameplayStyle.ChainShift),
    ),
    ClearRounds(
        stableId = "clear_rounds",
        supportedStyles = setOf(GameplayStyle.BlockSort),
    ),
    ClearBothDirections(
        stableId = "clear_both_directions",
        supportedStyles = setOf(GameplayStyle.BlockWise),
    );

    companion object {
        fun forStyle(gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle): List<ChallengeTaskType> =
            entries.filter { gameplayStyle in it.supportedStyles }

        fun fromStableId(stableId: String): ChallengeTaskType? = entries.firstOrNull { it.stableId == stableId }

        fun fromLegacyOrdinal(
            gameplayStyle: GameplayStyle,
            ordinal: Int,
        ): ChallengeTaskType? = when (gameplayStyle) {
            GameplayStyle.StackShift -> listOf(
                ClearBlocks,
                ReachScore,
                TriggerSpecial,
                PerfectPlacement,
                ChainReaction,
            ).getOrNull(ordinal)

            GameplayStyle.BlockWise -> listOf(
                ClearRows,
                ReachScore,
                ClearColumns,
                PlacePieces,
                ClearBothDirections,
            ).getOrNull(ordinal)

            GameplayStyle.ChainShift -> listOf(
                ClearBlocks,
                ReachScore,
                PlacePieces,
                ChainReaction,
            ).getOrNull(ordinal)

            GameplayStyle.MergeShift -> listOf(
                ReachScore,
                TriggerSpecial,
            ).getOrNull(ordinal)

            GameplayStyle.BoomBlocks -> listOf(
                ReachScore,
                PlacePieces,
            ).getOrNull(ordinal)

            GameplayStyle.BlockSort -> listOf(
                ReachScore,
                PlacePieces,
                ClearColumns,
                ClearRounds,
            ).getOrNull(ordinal)
        }
    }
}

@Immutable
data class ChallengeTask(
    val type: ChallengeTaskType,
    val target: Int,
    val current: Int = 0,
) {
    val isCompleted: Boolean get() = current >= target
}

@Immutable
data class DailyChallenge(
    val year: Int,
    val month: Int,
    val day: Int,
    val style: GameplayStyle,
    val tasks: List<ChallengeTask>,
) {
    val isCompleted: Boolean get() = tasks.all { it.isCompleted }
}

@Immutable
data class ChallengeProgress(
    val completedDays: Map<String, Set<Int>> = emptyMap() // "YYYY-MM" -> Set of days
)
