package com.ugurbuga.blockgames.presentation.game

import androidx.compose.runtime.Stable
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.feedback.GameHaptic
import com.ugurbuga.blockgames.platform.feedback.GameSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

@Stable
class GameViewModel(
    private val gameLogic: GameLogic = GameLogic.create(),
    initialState: GameState? = null,
    private val onStateChanged: (GameState) -> Unit = {},
    private val onChallengeCompleted: (DailyChallenge) -> Unit = {},
    private val onGameOver: (GameState) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val store = GameStore(
        gameLogic = gameLogic,
        scope = scope,
        initialState = initialState,
        onStateChanged = onStateChanged,
        onEvents = { events ->
            if (GameEvent.GameOver in events) {
                onGameOver(uiState.value.gameState)
            }
            if (GameEvent.ChallengeCompleted in events) {
                uiState.value.gameState.activeChallenge?.let(onChallengeCompleted)
            }
        }
    )
    private val feedbackMapper = GameFeedbackMapper()

    val uiState: StateFlow<GameUiState> = store.uiState

    fun tick() {
        store.tick()
    }

    fun previewPlacement(column: Int): PlacementPreview? {
        return store.previewPlacement(column)
    }

    fun previewPlacement(
        pieceId: Long,
        origin: GridPoint,
    ): PlacementPreview? {
        return store.previewPlacement(pieceId, origin)
    }

    fun previewImpactPoints(preview: PlacementPreview?): Set<GridPoint> {
        return store.previewImpactPoints(preview)
    }

    fun placePiece(column: Int): InteractionFeedback = dispatch(GameIntent.PlacePiece(
        pieceId = uiState.value.gameState.activePiece?.id ?: -1L,
        origin = store.previewPlacement(column)?.landingAnchor ?: GridPoint(0, 0),
    ))

    fun placePiece(pieceId: Long, origin: GridPoint): InteractionFeedback = dispatch(GameIntent.PlacePiece(pieceId, origin))

    fun placePieceResult(column: Int): GameDispatchResult {
        val pieceId = uiState.value.gameState.activePiece?.id ?: return GameDispatchResult()
        val origin = store.previewPlacement(column)?.landingAnchor ?: return GameDispatchResult()
        return dispatchResult(GameIntent.PlacePiece(pieceId, origin))
    }

    fun placePieceResult(pieceId: Long, origin: GridPoint): GameDispatchResult = dispatchResult(GameIntent.PlacePiece(pieceId, origin))

    fun holdPiece(): InteractionFeedback = dispatch(GameIntent.HoldPiece)

    fun reviveFromReward(): InteractionFeedback = dispatch(GameIntent.ReviveFromReward)

    fun appendWordToken(token: String): InteractionFeedback = dispatch(GameIntent.AppendWordToken(token))

    fun deleteWordToken(): InteractionFeedback = dispatch(GameIntent.DeleteWordToken)

    fun submitWordGuess(): InteractionFeedback = dispatch(GameIntent.SubmitWordGuess)

    fun advanceWordRound(): InteractionFeedback = dispatch(GameIntent.AdvanceWordRound)

    fun replaceActivePiece(specialType: SpecialBlockType): InteractionFeedback =
        dispatch(GameIntent.ReplaceActivePiece(specialType))

    fun restart(
        config: GameConfig = uiState.value.gameState.config,
        challenge: DailyChallenge? = uiState.value.gameState.activeChallenge,
        mode: GameMode = uiState.value.gameState.gameMode,
    ): InteractionFeedback {
        return dispatch(GameIntent.Restart(config, challenge, mode))
    }

    fun replaceState(state: GameState) {
        store.replaceState(state)
    }

    fun snapshotState(): GameState = uiState.value.gameState

    fun dispose() {
        store.dispose()
        scope.cancel()
    }

    fun dispatch(intent: GameIntent): InteractionFeedback {
        return dispatchResult(intent).feedback
    }

    fun dispatchResult(intent: GameIntent): GameDispatchResult {
        val events = store.dispatch(intent)
        return GameDispatchResult(
            events = events,
            feedback = feedbackMapper.map(events),
        )
    }
}

data class GameUiState(
    val gameState: GameState,
)


data class InteractionFeedback(
    val sounds: Set<GameSound> = emptySet(),
    val haptics: Set<GameHaptic> = emptySet(),
) {
    companion object {
        val None = InteractionFeedback()
    }
}

data class GameDispatchResult(
    val events: Set<GameEvent> = emptySet(),
    val feedback: InteractionFeedback = InteractionFeedback.None,
)
