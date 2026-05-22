package com.ugurbuga.blockgames.presentation.game

import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.SpecialBlockType

internal class GameReducer(
    private val gameLogic: GameLogic,
) {
    fun reduce(
        state: GameState,
        action: GameAction,
    ): ReduceResult = when (action) {
        is GameAction.PlacePiece -> {
            val result = gameLogic.placePiece(
                state = state,
                pieceId = action.pieceId,
                origin = action.origin,
            )
            val effects = when (state.gameplayStyle) {
                GameplayStyle.BlockWise,
                GameplayStyle.ChainShift,
                GameplayStyle.BoomBlocks,
                GameplayStyle.BlockSort -> emptyList()
                GameplayStyle.StackShift,
                GameplayStyle.MergeShift -> result.state.softLock?.let { softLock ->
                    listOf(
                        GameEffect.CancelSoftLockTimer,
                        GameEffect.StartSoftLockTimer(
                            revision = softLock.revision,
                            delayMillis = softLock.remainingMillis,
                        ),
                    )
                } ?: emptyList()
            }
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = effects,
            )
        }

        GameAction.CommitSoftLock -> {
            val result = gameLogic.commitSoftLock(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        GameAction.HoldPiece -> {
            val result = gameLogic.holdPiece(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        GameAction.ReviveFromReward -> {
            val result = gameLogic.reviveFromReward(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        is GameAction.ReplaceActivePiece -> {
            val result = gameLogic.replaceActivePiece(state, action.specialType)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        is GameAction.Restart -> ReduceResult(
            state = gameLogic.newGame(
                config = action.config,
                challenge = action.challenge,
                mode = action.mode,
            ),
            events = setOf(GameEvent.Restarted),
            effects = listOf(GameEffect.CancelSoftLockTimer),
        )

        GameAction.TogglePause -> {
            val nextStatus = if (state.status == GameStatus.Paused) GameStatus.Running else GameStatus.Paused
            ReduceResult(
                state = state.copy(status = nextStatus),
                events = setOf(if (nextStatus == GameStatus.Paused) GameEvent.Paused else GameEvent.Resumed),
                effects = emptyList(),
            )
        }

        GameAction.Tick -> {
            val nextState = gameLogic.tick(state)
            ReduceResult(
                state = nextState,
                events = buildSet {
                    if (state.status != GameStatus.GameOver && nextState.status == GameStatus.GameOver) {
                        add(GameEvent.GameOver)
                    }
                },
                effects = emptyList(),
            )
        }
    }
}

sealed interface GameIntent {
    data class PlacePiece(val pieceId: Long, val origin: GridPoint) : GameIntent
    data object CommitSoftLock : GameIntent
    data object HoldPiece : GameIntent
    data object ReviveFromReward : GameIntent
    data class Restart(
        val config: GameConfig,
        val challenge: DailyChallenge? = null,
        val mode: GameMode = GameMode.Classic,
    ) : GameIntent
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameIntent
    data object TogglePause : GameIntent
}

internal sealed interface GameAction {
    data class PlacePiece(val pieceId: Long, val origin: GridPoint) : GameAction
    data object CommitSoftLock : GameAction
    data object HoldPiece : GameAction
    data object ReviveFromReward : GameAction
    data class Restart(
        val config: GameConfig,
        val challenge: DailyChallenge? = null,
        val mode: GameMode = GameMode.Classic,
    ) : GameAction
    data object Tick : GameAction
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameAction
    data object TogglePause : GameAction
}

internal sealed interface GameEffect {
    data class StartSoftLockTimer(
        val revision: Long,
        val delayMillis: Long,
    ) : GameEffect

    data object CancelSoftLockTimer : GameEffect
}

internal data class ReduceResult(
    val state: GameState,
    val events: Set<GameEvent>,
    val effects: List<GameEffect>,
)

internal fun GameIntent.toAction(): GameAction = when (this) {
    is GameIntent.PlacePiece -> GameAction.PlacePiece(pieceId, origin)
    GameIntent.CommitSoftLock -> GameAction.CommitSoftLock
    GameIntent.HoldPiece -> GameAction.HoldPiece
    GameIntent.ReviveFromReward -> GameAction.ReviveFromReward
    is GameIntent.Restart -> GameAction.Restart(config, challenge, mode)
    is GameIntent.ReplaceActivePiece -> GameAction.ReplaceActivePiece(specialType)
    GameIntent.TogglePause -> GameAction.TogglePause
}
