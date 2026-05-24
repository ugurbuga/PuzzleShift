package com.ugurbuga.blockgames.presentation.game

import androidx.compose.runtime.Stable
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.logic.createSumShiftNewGame
import com.ugurbuga.blockgames.game.logic.generateNextSumShiftBoard
import com.ugurbuga.blockgames.game.logic.normalizeSumShiftConfig
import com.ugurbuga.blockgames.game.logic.randomSumShiftConfig
import com.ugurbuga.blockgames.game.logic.resolveSumShiftAutoSelectedCells
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.platform.feedback.GameHaptic
import com.ugurbuga.blockgames.platform.feedback.GameSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class GameViewModel(
    private val gameLogic: GameLogic = GameLogic.create(),
    initialState: GameState? = null,
    private val onStateChanged: (GameState) -> Unit = {},
    private val onChallengeCompleted: (DailyChallenge) -> Unit = {},
    private val onGameOver: (GameState) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private var sumShiftPreparationRequestId: Long = 0L
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

    fun placeSumShiftCellResult(
        origin: GridPoint,
        scheduleNextBoard: Boolean = true,
    ): GameDispatchResult {
        val result = dispatchResult(GameIntent.PlacePiece(pieceId = 0L, origin = origin))
        if (scheduleNextBoard && GameEvent.LineClear in result.events) {
            scheduleNextSumShiftBoardAfterCompletion()
        }
        return result
    }

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

    fun restartSumShift(
        config: GameConfig = uiState.value.gameState.config,
        challenge: DailyChallenge? = uiState.value.gameState.activeChallenge,
        mode: GameMode = uiState.value.gameState.gameMode,
    ): InteractionFeedback {
        val preparingConfig = normalizeSumShiftConfig(config)
        val currentState = uiState.value.gameState
        val preparingState = currentState.copy(
            config = preparingConfig,
            gameMode = mode,
            gameplayStyle = GameplayStyle.SumShift,
            activeChallenge = challenge,
            status = com.ugurbuga.blockgames.game.model.GameStatus.Running,
            sumShiftSelectedCells = emptySet(),
            sumShiftPreparingBoard = true,
        )
        store.replaceStateDirect(preparingState)
        launchSumShiftPreparation(minimumVisibleMillis = 900L) {
            val effectiveConfig = if (challenge == null) randomSumShiftConfig() else preparingConfig
            createSumShiftNewGame(
                config = effectiveConfig,
                challenge = challenge,
                mode = mode,
            )
        }
        return feedbackMapper.map(setOf(GameEvent.Restarted))
    }

    fun replaceState(state: GameState) {
        store.replaceState(state)
    }

    fun replaceStateDirect(state: GameState) {
        store.replaceStateDirect(state)
    }

    fun updateSumShiftManualDisabledCells(points: Set<GridPoint>) {
        val state = uiState.value.gameState
        if (state.gameplayStyle != GameplayStyle.SumShift) return
        val nextSelectedCells = resolveSumShiftAutoSelectedCells(
            state = state,
            selectedCells = state.sumShiftSelectedCells - points,
            manualDisabledCells = points,
        )
        store.replaceStateDirect(
            state.copy(
                sumShiftManualDisabledCells = points,
                sumShiftSelectedCells = nextSelectedCells,
                lastActionTime = currentEpochMillis(),
            )
        )
    }

    fun recordSumShiftMistake(): InteractionFeedback {
        val state = uiState.value.gameState
        if (state.gameplayStyle != GameplayStyle.SumShift || state.status != GameStatus.Running) {
            return InteractionFeedback.None
        }
        val nextMistakes = state.sumShiftMistakesUsed + 1
        val nextState = state.copy(
            sumShiftMistakesUsed = nextMistakes,
            status = if (nextMistakes >= 2) GameStatus.GameOver else state.status,
            lastActionTime = currentEpochMillis(),
        )
        store.replaceStateDirect(nextState)
        return feedbackMapper.map(
            buildSet {
                add(GameEvent.InvalidDrop)
                if (nextState.status == GameStatus.GameOver) {
                    add(GameEvent.GameOver)
                }
            }
        )
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

    private fun scheduleNextSumShiftBoardAfterCompletion() {
        val completedState = uiState.value.gameState
        if (completedState.gameplayStyle != GameplayStyle.SumShift) return

        val requestId = ++sumShiftPreparationRequestId
        scope.launch {
            delay(700L)
            if (requestId != sumShiftPreparationRequestId) return@launch

            val preparingState = uiState.value.gameState.copy(
                sumShiftPreparingBoard = true,
                lastActionTime = currentEpochMillis(),
            )
            store.replaceStateDirect(preparingState)

            val nextConfig = if (preparingState.activeChallenge == null) randomSumShiftConfig() else preparingState.config
            val startedAt = currentEpochMillis()
            val generatedState = withContext(Dispatchers.Default) {
                generateNextSumShiftBoard(preparingState.copy(config = nextConfig))
            }
            val elapsed = currentEpochMillis() - startedAt
            val minimumVisibleMillis = 1_350L
            if (elapsed < minimumVisibleMillis) {
                delay(minimumVisibleMillis - elapsed)
            }
            if (requestId == sumShiftPreparationRequestId) {
                store.replaceStateDirect(generatedState)
            }
        }
    }

    private fun launchSumShiftPreparation(
        minimumVisibleMillis: Long,
        producer: () -> GameState,
    ) {
        val requestId = ++sumShiftPreparationRequestId
        scope.launch {
            val startedAt = currentEpochMillis()
            val generatedState = withContext(Dispatchers.Default) {
                producer()
            }
            val elapsed = currentEpochMillis() - startedAt
            if (elapsed < minimumVisibleMillis) {
                delay(minimumVisibleMillis - elapsed)
            }
            if (requestId == sumShiftPreparationRequestId) {
                store.replaceStateDirect(generatedState)
            }
        }
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
