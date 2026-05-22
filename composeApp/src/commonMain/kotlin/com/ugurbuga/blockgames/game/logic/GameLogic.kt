package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import kotlin.random.Random

interface GameLogic {
    fun restoreGame(state: GameState): GameState
    fun newGame(
        config: GameConfig = GameConfig.default(),
        challenge: DailyChallenge? = null,
        mode: GameMode = GameMode.Classic,
    ): GameState

    fun previewPlacement(state: GameState, column: Int): PlacementPreview?
    fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview?
    fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint>
    fun placePiece(state: GameState, column: Int): GameMoveResult
    fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult
    fun holdPiece(state: GameState): GameMoveResult
    fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult
    fun commitSoftLock(state: GameState): GameMoveResult
    fun reviveFromReward(state: GameState): GameMoveResult
    fun tick(state: GameState): GameState

    companion object {
        const val DEFAULT_TIME_ATTACK_DURATION_MILLIS = 90_000L
        const val TIME_ATTACK_BONUS_PER_CLEARED_BLOCK_MILLIS = 300L
        const val TIME_ATTACK_SCORE_BONUS_THRESHOLD = 1_000
        const val TIME_ATTACK_SCORE_BONUS_MILLIS = 2_000L
        const val TIME_ATTACK_REVIVE_BONUS_MILLIS = 15_000L

        fun create(
            random: Random = Random.Default,
            scoreCalculator: ScoreCalculator = ScoreCalculator(),
        ): GameLogic = AdaptiveGameLogic(random, scoreCalculator)
    }
}

private class AdaptiveGameLogic(
    private val random: Random,
    private val scoreCalculator: ScoreCalculator,
) : GameLogic {

    private fun gameLogic(gameplayStyle: GameplayStyle): GameLogic = when (gameplayStyle) {
        GameplayStyle.BlockWise -> BlockWiseGameLogic(random, scoreCalculator)
        GameplayStyle.StackShift -> StackShiftGameLogic(random, scoreCalculator)
        GameplayStyle.ChainShift -> ChainShiftGameLogic(random, scoreCalculator)
        GameplayStyle.MergeShift -> MergeShiftGameLogic(random, scoreCalculator)
        GameplayStyle.BoomBlocks -> BoomBlocksGameLogic(random, scoreCalculator)
        GameplayStyle.BlockSort -> BlockSortGameLogic(random, scoreCalculator)
    }

    override fun restoreGame(state: GameState) = gameLogic(state.gameplayStyle).restoreGame(state)

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode
    ): GameState {
        val gameplayStyle = challenge?.style ?: GlobalPlatformConfig.gameplayStyle
        return gameLogic(gameplayStyle).newGame(config, challenge, mode)
    }

    override fun previewPlacement(state: GameState, column: Int) =
        gameLogic(state.gameplayStyle).previewPlacement(state, column)

    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint) =
        gameLogic(state.gameplayStyle).previewPlacement(state, pieceId, origin)

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?) =
        gameLogic(state.gameplayStyle).previewImpactPoints(state, preview)

    override fun placePiece(state: GameState, column: Int) =
        gameLogic(state.gameplayStyle).placePiece(state, column)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint) =
        gameLogic(state.gameplayStyle).placePiece(state, pieceId, origin)

    override fun holdPiece(state: GameState) = gameLogic(state.gameplayStyle).holdPiece(state)

    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType) =
        gameLogic(state.gameplayStyle).replaceActivePiece(state, specialType)

    override fun commitSoftLock(state: GameState) = gameLogic(state.gameplayStyle).commitSoftLock(state)

    override fun reviveFromReward(state: GameState) = gameLogic(state.gameplayStyle).reviveFromReward(state)

    override fun tick(state: GameState) = gameLogic(state.gameplayStyle).tick(state)
}

enum class GameEvent {
    PlacementAccepted,
    InvalidDrop,
    LineClear,
    ChainReaction,
    Combo,
    PerfectDrop,
    HoldUsed,
    SoftLockStarted,
    SoftLockAdjusted,
    SpecialTriggered,
    LaunchBoostCharged,
    PressureCritical,
    GameOver,
    ChallengeCompleted,
    Revived,
    Restarted,
    Paused,
    Resumed,
}

data class GameMoveResult(
    val state: GameState,
    val preview: PlacementPreview? = null,
    val events: Set<GameEvent> = emptySet(),
)
