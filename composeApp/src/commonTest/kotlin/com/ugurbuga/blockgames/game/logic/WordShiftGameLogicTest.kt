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

class WordShiftGameLogicTest {

    private val logic = WordShiftGameLogic(random = Random(0), scoreCalculator = ScoreCalculator())

    @Test
    fun newGame_usesWordShiftDefaults() {
        val state = logic.newGame(
            config = GameConfig.default(GameplayStyle.WordShift),
            challenge = null,
            mode = GameMode.Classic,
        )

        assertEquals(GameplayStyle.WordShift, state.gameplayStyle)
        assertTrue(state.config.columns in 4..7)
        assertEquals(6, state.config.rows)
        assertNull(state.activePiece)
        assertTrue(state.nextQueue.isEmpty())
        assertEquals(GameTextKey.GameMessageWordShiftEnterWord, state.message.key)
        assertEquals(state.config.columns, state.wordShiftSolution.size)
        assertTrue(state.wordShiftCurrentGuess.isEmpty())
    }

    @Test
    fun submitWordGuess_solvingWord_advancesRoundAndCompletesChallenge() {
        val solution = WordShiftLexicon.packFor("en").words(5).first()
        val challenge = DailyChallenge(
            year = 2026,
            month = 5,
            day = 22,
            style = GameplayStyle.WordShift,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.SolveWords, target = 1),
                ChallengeTask(type = ChallengeTaskType.ReachScore, target = 600),
            ),
        )
        val initialState = logic.newGame(
            config = GameConfig.default(GameplayStyle.WordShift),
            challenge = challenge,
            mode = GameMode.Classic,
        ).copy(
            wordShiftLocaleTag = "en",
            wordShiftSolution = solution,
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
        assertEquals(GameTextKey.GameMessageWordShiftSolved, result.state.message.key)
        assertTrue(result.state.wordShiftCurrentGuess.isEmpty())
        assertEquals(solution, result.state.wordShiftSolution)
        assertTrue(result.state.wordShiftAwaitingNextRound)
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

        assertFalse(advancedState.wordShiftAwaitingNextRound)
        assertEquals(GameTextKey.GameMessageWordShiftEnterWord, advancedState.message.key)
        assertTrue(advancedState.wordShiftSolution.size in 4..7)
        assertEquals(advancedState.config.columns, advancedState.wordShiftSolution.size)
    }

    @Test
    fun submitWordGuess_rejectsGuessOutsideDictionary() {
        val solution = WordShiftLexicon.packFor("en").words(5).first()
        val initialState = logic.newGame(
            config = GameConfig.default(GameplayStyle.WordShift),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            wordShiftLocaleTag = "en",
            wordShiftSolution = solution,
        )
        val invalidTokens = listOf("Q", "Q", "Q", "Q", "Q")
        val readyState = invalidTokens.fold(initialState) { state, token ->
            logic.appendWordToken(state, token).state
        }

        val result = logic.submitWordGuess(readyState)

        assertTrue(GameEvent.InvalidDrop in result.events)
        assertEquals(GameTextKey.GameMessageWordShiftNotInDictionary, result.state.message.key)
        assertTrue(result.state.wordShiftGuesses.isEmpty())
        assertEquals(invalidTokens, result.state.wordShiftCurrentGuess)
    }

    @Test
    fun submitWordGuess_supportsFourTokenRounds() {
        val solution = WordShiftLexicon.packFor("en").words(4).first()
        val initialState = logic.newGame(
            config = GameConfig.default(GameplayStyle.WordShift),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            config = GameConfig(columns = 4, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            wordShiftLocaleTag = "en",
            wordShiftSolution = solution,
        )
        val readyState = solution.fold(initialState) { state, token ->
            logic.appendWordToken(state, token).state
        }

        val result = logic.submitWordGuess(readyState)

        assertTrue(GameEvent.PlacementAccepted in result.events)
        assertEquals(1, result.state.linesCleared)
        assertEquals(4, initialState.config.columns)
        assertEquals(GameTextKey.GameMessageWordShiftSolved, result.state.message.key)
        assertTrue(result.state.wordShiftAwaitingNextRound)

        val advancedState = logic.advanceWordRound(result.state).state

        assertEquals(GameTextKey.GameMessageWordShiftEnterWord, advancedState.message.key)
        assertFalse(advancedState.wordShiftAwaitingNextRound)
    }
}


