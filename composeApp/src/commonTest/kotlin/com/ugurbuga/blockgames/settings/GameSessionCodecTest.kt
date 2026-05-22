package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.logic.ScoreCalculator
import com.ugurbuga.blockgames.game.logic.WordShiftGameLogic
import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.WordShiftGuess
import com.ugurbuga.blockgames.game.model.WordShiftLetterState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameSessionCodecTest {
    private val logic = GameLogic.create(random = Random(21))

    @Test
    fun encodeDecode_preservesTimeAttackSessionDetails() {
        val challenge = DailyChallenge(
            year = 2026,
            month = 4,
            day = 28,
            style = GameplayStyle.BlockWise,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.ClearRows, target = 4, current = 2),
                ChallengeTask(type = ChallengeTaskType.ReachScore, target = 1200, current = 450),
            ),
        )
        val state = logic.newGame(
            config = GameConfig(columns = 6, rows = 8),
            challenge = challenge,
            mode = GameMode.TimeAttack,
        ).copy(
            remainingTimeMillis = 42_000L,
            recentlyClearedColumns = setOf(1, 4),
            rewardedReviveUsed = true,
            activeChallenge = challenge,
        )

        val decoded = GameSessionCodec.decode(GameSessionCodec.encode(state))

        assertNotNull(decoded)
        assertEquals(GameMode.TimeAttack, decoded.gameMode)
        assertEquals(42_000L, decoded.remainingTimeMillis)
        assertEquals(setOf(1, 4), decoded.recentlyClearedColumns)
        assertTrue(decoded.rewardedReviveUsed)
        assertNotNull(decoded.activeChallenge)
        assertEquals(challenge.year, decoded.activeChallenge.year)
        assertEquals(challenge.month, decoded.activeChallenge.month)
        assertEquals(challenge.day, decoded.activeChallenge.day)
        assertEquals(challenge.tasks, decoded.activeChallenge.tasks)
    }

    @Test
    fun sessionSlotFor_routesClassicTimeAttackAndChallengeSeparately() {
        val classicState = logic.newGame()
        val timeAttackState = logic.newGame(mode = GameMode.TimeAttack)
        val challenge = DailyChallenge(
            year = 2026,
            month = 4,
            day = 28,
            style = GameplayStyle.BlockWise,
            tasks = listOf(ChallengeTask(type = ChallengeTaskType.PlacePieces, target = 6)),
        )
        val challengeState = logic.newGame(challenge = challenge)

        assertEquals(GameSessionSlot.Classic(classicState.gameplayStyle), classicState.sessionSlot())
        assertEquals(GameSessionSlot.TimeAttack(timeAttackState.gameplayStyle), timeAttackState.sessionSlot())
        assertEquals(
            GameSessionSlot.DailyChallenge("2026-04-28", challenge.style),
            challengeState.sessionSlot(),
        )
    }

    @Test
    fun encodeDecode_preservesBlockSortScoreSuppressionState() {
        val state = GameLogic.create(random = Random(8)).newGame(
            config = GameConfig.default(GameplayStyle.BlockSort),
            mode = GameMode.Classic,
        ).copy(
            gameplayStyle = GameplayStyle.BlockSort,
            blockSortBonusEmptyColumnUsed = true,
            blockSortScoredMoveSignatures = setOf("c0x1x101x102", "c2x3x205"),
        )

        val decoded = GameSessionCodec.decode(GameSessionCodec.encode(state))

        assertNotNull(decoded)
        assertEquals(GameplayStyle.BlockSort, decoded.gameplayStyle)
        assertTrue(decoded.blockSortBonusEmptyColumnUsed)
        assertEquals(state.blockSortScoredMoveSignatures, decoded.blockSortScoredMoveSignatures)
    }

    @Test
    fun encodeDecode_preservesWordShiftSessionDetails() {
        val challenge = DailyChallenge(
            year = 2026,
            month = 5,
            day = 22,
            style = GameplayStyle.WordShift,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.SolveWords, target = 3, current = 1),
                ChallengeTask(type = ChallengeTaskType.ReachScore, target = 2_000, current = 1_350),
            ),
        )
        val state = WordShiftGameLogic(random = Random(13), scoreCalculator = ScoreCalculator()).newGame(
            config = GameConfig.default(GameplayStyle.WordShift),
            challenge = challenge,
            mode = GameMode.TimeAttack,
        ).copy(
            config = GameConfig(columns = 7, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            score = 1_350,
            linesCleared = 1,
            remainingTimeMillis = 33_000L,
            activeChallenge = challenge,
            wordShiftLocaleTag = "en",
            wordShiftSolution = listOf("S", "T", "A", "R", "L", "I", "T"),
            wordShiftGuesses = listOf(
                WordShiftGuess(
                    tokens = listOf("A", "P", "P", "L", "E", "S", "E"),
                    states = listOf(
                        WordShiftLetterState.Absent,
                        WordShiftLetterState.Present,
                        WordShiftLetterState.Absent,
                        WordShiftLetterState.Absent,
                        WordShiftLetterState.Correct,
                        WordShiftLetterState.Absent,
                        WordShiftLetterState.Absent,
                    ),
                ),
            ),
            wordShiftCurrentGuess = listOf("C", "L", "O"),
            wordShiftKeyboardHints = mapOf(
                "A" to WordShiftLetterState.Absent,
                "P" to WordShiftLetterState.Present,
                "E" to WordShiftLetterState.Correct,
            ),
            wordShiftAwaitingNextRound = true,
        )

        val decoded = GameSessionCodec.decode(GameSessionCodec.encode(state))

        assertNotNull(decoded)
        assertEquals(GameplayStyle.WordShift, decoded.gameplayStyle)
        assertEquals(7, decoded.config.columns)
        assertEquals(33_000L, decoded.remainingTimeMillis)
        assertEquals(state.wordShiftLocaleTag, decoded.wordShiftLocaleTag)
        assertEquals(state.wordShiftSolution, decoded.wordShiftSolution)
        assertEquals(state.wordShiftGuesses, decoded.wordShiftGuesses)
        assertEquals(state.wordShiftCurrentGuess, decoded.wordShiftCurrentGuess)
        assertEquals(state.wordShiftKeyboardHints, decoded.wordShiftKeyboardHints)
        assertTrue(decoded.wordShiftAwaitingNextRound)
        assertEquals(challenge.tasks, decoded.activeChallenge?.tasks)
    }
}

