package com.ugurbuga.blockgames.presentation.game

import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameStore(
    private val gameLogic: GameLogic,
    private val scope: CoroutineScope,
    initialState: GameState? = null,
    private val onStateChanged: (GameState) -> Unit = {},
    private val onEvents: (Set<GameEvent>) -> Unit = {},
) {
    private val reducer = GameReducer(gameLogic)
    private val _uiState = MutableStateFlow(
        GameUiState(
            gameState = restoreState(
                initialState ?: gameLogic.newGame()
            ),
        ),
    )
    private val effectHandler = GameEffectHandler(
        scope = scope,
        stateProvider = { _uiState.value.gameState },
        dispatchIntent = ::dispatch,
    )

    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        onStateChanged(_uiState.value.gameState)
    }

    fun previewPlacement(column: Int): PlacementPreview? {
        val state = _uiState.value.gameState
        return gameLogic.previewPlacement(state = state, column = column)
    }

    fun previewPlacement(
        pieceId: Long,
        origin: GridPoint,
    ): PlacementPreview? {
        val state = _uiState.value.gameState
        return gameLogic.previewPlacement(state = state, pieceId = pieceId, origin = origin)
    }

    fun previewImpactPoints(preview: PlacementPreview?): Set<GridPoint> {
        val state = _uiState.value.gameState
        return gameLogic.previewImpactPoints(state = state, preview = preview)
    }

    fun dispatch(intent: GameIntent): Set<GameEvent> {
        val result = reducer.reduce(
            state = _uiState.value.gameState,
            action = intent.toAction(),
        )
        updateState(result.state)
        onEvents(result.events)
        result.effects.forEach(effectHandler::handle)
        return result.events
    }

    fun tick() {
        val result = reducer.reduce(
            state = _uiState.value.gameState,
            action = GameAction.Tick,
        )
        updateState(result.state)
        onEvents(result.events)
        result.effects.forEach(effectHandler::handle)
    }

    fun replaceState(state: GameState) {
        effectHandler.handle(GameEffect.CancelSoftLockTimer)
        updateState(restoreState(state))
    }

    fun replaceStateDirect(state: GameState) {
        effectHandler.handle(GameEffect.CancelSoftLockTimer)
        updateState(state)
    }

    fun dispose() {
        effectHandler.dispose()
    }

    private fun updateState(state: GameState) {
        _uiState.update { current -> current.copy(gameState = state) }
        onStateChanged(state)
    }

    private fun restoreState(state: GameState): GameState = gameLogic.restoreGame(state)
}
