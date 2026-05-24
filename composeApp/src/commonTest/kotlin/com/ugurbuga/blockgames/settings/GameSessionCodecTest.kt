package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.logic.ScoreCalculator
import com.ugurbuga.blockgames.game.logic.DigitShiftGameLogic
import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.DigitShiftGuess
import com.ugurbuga.blockgames.game.model.DigitShiftLetterState
import com.ugurbuga.blockgames.game.model.GridPoint
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
    fun encodeDecode_preservesDigitShiftSessionDetails() {
        val challenge = DailyChallenge(
            year = 2026,
            month = 5,
            day = 22,
            style = GameplayStyle.DigitShift,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.SolveWords, target = 3, current = 1),
                ChallengeTask(type = ChallengeTaskType.ReachScore, target = 2_000, current = 1_350),
            ),
        )
        val state = DigitShiftGameLogic(random = Random(13), scoreCalculator = ScoreCalculator()).newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
            challenge = challenge,
            mode = GameMode.TimeAttack,
        ).copy(
            config = GameConfig(columns = 5, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            score = 1_350,
            linesCleared = 1,
            remainingTimeMillis = 33_000L,
            activeChallenge = challenge,
            digitShiftLocaleTag = "en",
            digitShiftSolution = listOf("1", "2", "3", "4", "5"),
            digitShiftGuesses = listOf(
                DigitShiftGuess(
                    tokens = listOf("1", "7", "0", "4", "8"),
                    states = listOf(
                        DigitShiftLetterState.Correct,
                        DigitShiftLetterState.Absent,
                        DigitShiftLetterState.Absent,
                        DigitShiftLetterState.Correct,
                        DigitShiftLetterState.Absent,
                    ),
                ),
            ),
            digitShiftCurrentGuess = listOf("8", "8", "8"),
            digitShiftKeyboardHints = mapOf(
                "1" to DigitShiftLetterState.Correct,
                "2" to DigitShiftLetterState.Present,
                "8" to DigitShiftLetterState.Absent,
            ),
            digitShiftAwaitingNextRound = true,
        )

        val decoded = GameSessionCodec.decode(GameSessionCodec.encode(state))

        assertNotNull(decoded)
        assertEquals(GameplayStyle.DigitShift, decoded.gameplayStyle)
        assertEquals(5, decoded.config.columns)
        assertEquals(33_000L, decoded.remainingTimeMillis)
        assertEquals(state.digitShiftLocaleTag, decoded.digitShiftLocaleTag)
        assertEquals(state.digitShiftSolution, decoded.digitShiftSolution)
        assertEquals(state.digitShiftGuesses, decoded.digitShiftGuesses)
        assertEquals(state.digitShiftCurrentGuess, decoded.digitShiftCurrentGuess)
        assertEquals(state.digitShiftKeyboardHints, decoded.digitShiftKeyboardHints)
        assertTrue(decoded.digitShiftAwaitingNextRound)
        assertEquals(challenge.tasks, decoded.activeChallenge?.tasks)
    }

    @Test
    fun encodeDecode_preservesSumShiftSessionDetails() {
        val state = com.ugurbuga.blockgames.game.logic.SumShiftGameLogic(random = Random(5), scoreCalculator = ScoreCalculator()).newGame(
            config = GameConfig(columns = 5, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            challenge = null,
            mode = GameMode.TimeAttack,
        ).copy(
            score = 1_420,
            remainingTimeMillis = 27_000L,
            sumShiftRowTargets = listOf(6, 6, 9, 8, 8, 7),
            sumShiftColumnTargets = listOf(7, 11, 8, 5, 6),
            sumShiftSelectedCells = setOf(
                GridPoint(0, 0),
                GridPoint(1, 0),
                GridPoint(3, 1),
                GridPoint(4, 1),
                GridPoint(1, 2),
                GridPoint(4, 5),
            ),
            sumShiftManualDisabledCells = setOf(
                GridPoint(3, 4),
                GridPoint(0, 5),
                GridPoint(4, 4),
            ),
        )

        val decoded = GameSessionCodec.decode(GameSessionCodec.encode(state))

        assertNotNull(decoded)
        assertEquals(GameplayStyle.SumShift, decoded.gameplayStyle)
        assertEquals(5, decoded.config.columns)
        assertEquals(6, decoded.config.rows)
        assertEquals(27_000L, decoded.remainingTimeMillis)
        assertEquals(state.sumShiftRowTargets, decoded.sumShiftRowTargets)
        assertEquals(state.sumShiftColumnTargets, decoded.sumShiftColumnTargets)
        assertEquals(state.sumShiftSelectedCells, decoded.sumShiftSelectedCells)
        assertEquals(state.sumShiftManualDisabledCells, decoded.sumShiftManualDisabledCells)
    }
}

