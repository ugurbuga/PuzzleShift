package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DigitShiftGameLogicTest {

    private val logic = DigitShiftGameLogic(random = Random(0), scoreCalculator = ScoreCalculator())

    @Test
    fun newGame_usesDigitShiftDefaults() {
        val state = logic.newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = null,
            mode = GameMode.Classic,
        )

        assertEquals(GameplayStyle.DigitShift, state.gameplayStyle)
        assertEquals(5, state.config.columns)
        assertEquals(6, state.config.rows)
        assertNull(state.activePiece)
        assertTrue(state.nextQueue.isEmpty())
        assertEquals(GameTextKey.GameMessageDigitShiftEnterWord, state.message.key)
        assertEquals(state.config.columns, state.digitShiftSolution.size)
        assertTrue(state.digitShiftCurrentGuess.isEmpty())
    }

    @Test
    fun submitWordGuess_solvingWord_advancesRoundAndCompletesChallenge() {
        val solution = listOf("1", "2", "3", "4", "5")
        val challenge = DailyChallenge(
            year = 2026,
            month = 5,
            day = 22,
            style = GameplayStyle.DigitShift,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.SolveWords, target = 1),
                ChallengeTask(type = ChallengeTaskType.ReachScore, target = 600),
            ),
        )
        val initialState = logic.newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = challenge,
            mode = GameMode.Classic,
        ).copy(
            digitShiftLocaleTag = "en",
            digitShiftSolution = solution,
        )
        val readyState = solution.fold(initialState) { state, token ->
            logic.appendWordToken(state, token).state
        }

        val result = logic.submitWordGuess(readyState)
        val updatedChallenge = assertNotNull(result.state.activeChallenge)

        assertTrue(GameEvent.PlacementAccepted in result.events)
        assertTrue(GameEvent.LineClear in result.events)
        assertTrue(GameEvent.ChallengeCompleted in result.events)
        assertEquals(1, result.state.linesCleared)
        assertTrue(result.state.score >= 600)
        assertEquals(GameTextKey.GameMessageDigitShiftSolved, result.state.message.key)
        assertTrue(result.state.digitShiftCurrentGuess.isEmpty())
        assertEquals(solution, result.state.digitShiftSolution)
        assertTrue(result.state.digitShiftAwaitingNextRound)
        assertTrue(updatedChallenge.isCompleted)
        assertEquals(
            1,
            updatedChallenge.tasks.first { it.type == ChallengeTaskType.SolveWords }.current,
        )
        assertEquals(
            result.state.score,
            updatedChallenge.tasks.first { it.type == ChallengeTaskType.ReachScore }.current,
        )

        val advancedState = logic.advanceWordRound(result.state).state

        assertFalse(advancedState.digitShiftAwaitingNextRound)
        assertEquals(GameTextKey.GameMessageDigitShiftEnterWord, advancedState.message.key)
        assertEquals(6, advancedState.digitShiftSolution.size)
        assertEquals(advancedState.config.columns, advancedState.digitShiftSolution.size)
        assertEquals(8, advancedState.config.rows)
    }

    @Test
    fun appendWordToken_rejectsNonDigitTokens() {
        val solution = listOf("1", "2", "3", "4", "5")
        val initialState = logic.newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            digitShiftLocaleTag = "en",
            digitShiftSolution = solution,
        )
        val result = logic.appendWordToken(initialState, "Q")

        assertTrue(GameEvent.InvalidDrop in result.events)
        assertTrue(result.state.digitShiftGuesses.isEmpty())
        assertTrue(result.state.digitShiftCurrentGuess.isEmpty())
    }

    @Test
    fun restoreGame_normalizesLegacyShorterSessionsToFiveDigits() {
        val legacyState = logic.newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            config = GameConfig(columns = 4, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            digitShiftLocaleTag = "en",
            digitShiftSolution = listOf("7", "8", "9", "0"),
            digitShiftCurrentGuess = listOf("7", "8", "9", "0"),
        )

        val restoredState = logic.restoreGame(legacyState)

        assertEquals(5, restoredState.config.columns)
        assertEquals(5, restoredState.digitShiftSolution.size)
        assertEquals(4, restoredState.digitShiftCurrentGuess.size)
        assertTrue(restoredState.digitShiftCurrentGuess.all { it in DigitShiftDigitTokens })
    }

    @Test
    fun restoreGame_keepsSixDigitRoundsAtEightAttempts() {
        val sixDigitState = logic.newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            config = GameConfig(columns = 6, rows = 8, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            digitShiftLocaleTag = "en",
            digitShiftSolution = listOf("1", "2", "3", "4", "5", "6"),
            digitShiftCurrentGuess = listOf("1", "2", "3"),
        )

        val restoredState = logic.restoreGame(sixDigitState)

        assertEquals(6, restoredState.config.columns)
        assertEquals(8, restoredState.config.rows)
        assertEquals(6, restoredState.digitShiftSolution.size)
        assertEquals(3, restoredState.digitShiftCurrentGuess.size)
    }

    @Test
    fun submitWordGuess_allowsEightAttemptsForSixDigitRounds() {
        val wrongGuess = listOf("0", "0", "0", "0", "0", "0")
        var state = logic.newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            config = GameConfig(columns = 6, rows = 8, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            digitShiftLocaleTag = "en",
            digitShiftSolution = listOf("1", "2", "3", "4", "5", "6"),
        )

        repeat(7) {
            val readyState = wrongGuess.fold(state) { currentState, token ->
                logic.appendWordToken(currentState, token).state
            }
            val result = logic.submitWordGuess(readyState)
            assertEquals(8 - (it + 1), result.state.digitShiftAttemptsRemaining)
            assertEquals(GameTextKey.GameMessageDigitShiftKeepTrying, result.state.message.key)
            assertFalse(GameEvent.GameOver in result.events)
            state = result.state
        }

        val finalReadyState = wrongGuess.fold(state) { currentState, token ->
            logic.appendWordToken(currentState, token).state
        }
        val finalResult = logic.submitWordGuess(finalReadyState)

        assertEquals(0, finalResult.state.digitShiftAttemptsRemaining)
        assertEquals(GameTextKey.GameMessageDigitShiftFailed, finalResult.state.message.key)
        assertTrue(GameEvent.GameOver in finalResult.events)
    }
}


