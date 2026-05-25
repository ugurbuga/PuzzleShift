package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStage
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStateFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SumShiftGameLogicTest {
    private val logic = SumShiftGameLogic(random = Random(9), scoreCalculator = ScoreCalculator())

    @Test
    fun newGame_normalizesToSupportedBoardSizes() {
        val state = logic.newGame(
            config = GameConfig(columns = 5, rows = 12, difficultyIntervalSeconds = 1, linesPerLevel = 1),
            challenge = null,
            mode = GameMode.Classic,
        )

        val hardState = logic.newGame(
            config = GameConfig(columns = 9, rows = 20, difficultyIntervalSeconds = 1, linesPerLevel = 1),
            challenge = null,
            mode = GameMode.Classic,
        )

        assertEquals(GameplayStyle.SumShift, state.gameplayStyle)
        assertEquals(5, state.config.columns)
        assertEquals(8, state.config.rows)
        assertEquals(8, state.sumShiftRowTargets.size)
        assertEquals(5, state.sumShiftColumnTargets.size)

        assertEquals(7, hardState.config.columns)
        assertEquals(9, hardState.config.rows)
        assertEquals(9, hardState.sumShiftRowTargets.size)
        assertEquals(7, hardState.sumShiftColumnTargets.size)
    }

    @Test
    fun randomConfig_usesOnlySupportedSmallerBoardTiers() {
        val supported = sumShiftSupportedConfigs().map { it.columns to it.rows }.toSet()

        repeat(40) {
            val config = randomSumShiftConfig(Random(it + 1))
            assertTrue((config.columns to config.rows) in supported)
        }
    }

    @Test
    fun placePiece_completingRequiredSelectionAdvancesToNextPuzzle() {
        val scene = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStage.FinishPuzzle)
        var state = scene.gameState
        var lastResult: GameMoveResult? = null

        scene.requiredSelection.forEach { point ->
            lastResult = logic.placePiece(state, pieceId = 0L, origin = point)
            state = requireNotNull(lastResult).state
        }

        val result = lastResult ?: error("Expected final result")
        assertTrue(GameEvent.LineClear in result.events)
        assertFalse(result.state.sumShiftPreparingBoard)
        assertEquals(1, result.state.linesCleared)
        val nextState = generateNextSumShiftBoard(result.state, Random(99))
        assertFalse(nextState.sumShiftPreparingBoard)
        assertTrue(nextState.sumShiftSelectedCells.isEmpty())
        assertEquals(nextState.config.rows, nextState.sumShiftRowTargets.size)
        assertEquals(nextState.config.columns, nextState.sumShiftColumnTargets.size)
    }

    @Test
    fun tick_timeAttackExpiresIntoGameOver() {
        val initial = logic.newGame(
            config = GameConfig.default(GameplayStyle.SumShift),
            challenge = null,
            mode = GameMode.TimeAttack,
        ).copy(remainingTimeMillis = 500L)

        val next = logic.tick(initial)

        assertEquals(GameStatus.GameOver, next.status)
        assertEquals(0L, next.remainingTimeMillis)
    }

    @Test
    fun placePiece_tappingSelectedCellTogglesItOff() {
        val scene = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStage.MatchRow)
        val state = scene.gameState.copy(
            sumShiftSelectedCells = emptySet(),
            sumShiftRowTargets = List(scene.gameState.config.rows) { 99 },
            sumShiftColumnTargets = List(scene.gameState.config.columns) { 99 },
        )
        val point = GridPoint(1, 0)

        val selected = logic.placePiece(state, 0L, point).state
        val deselected = logic.placePiece(selected, 0L, point).state

        assertTrue(point in selected.sumShiftSelectedCells)
        assertTrue(point !in deselected.sumShiftSelectedCells)
    }

    @Test
    fun newGame_generatesDifferentBoardsAcrossRuns() {
        val first = logic.newGame(
            config = GameConfig.default(GameplayStyle.SumShift),
            challenge = null,
            mode = GameMode.Classic,
        )
        val second = logic.newGame(
            config = GameConfig.default(GameplayStyle.SumShift),
            challenge = null,
            mode = GameMode.Classic,
        )

        assertNotEquals(first.board, second.board)
        assertNotEquals(first.sumShiftRowTargets, second.sumShiftRowTargets)
    }

    @Test
    fun placePiece_autoCompletesChainedRowsAndColumnsUntilStable() {
        var board = BoardMatrix.empty(columns = 2, rows = 2)
        board = board.fill(points = listOf(GridPoint(0, 0)), tone = CellTone.Cyan, value = 1)
        board = board.fill(points = listOf(GridPoint(1, 0)), tone = CellTone.Gold, value = 2)
        board = board.fill(points = listOf(GridPoint(0, 1)), tone = CellTone.Violet, value = 3)
        board = board.fill(points = listOf(GridPoint(1, 1)), tone = CellTone.Emerald, value = 4)

        val initial = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStage.MatchRow).gameState.copy(
            config = GameConfig(columns = 2, rows = 2, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            board = board,
            sumShiftRowTargets = listOf(3, 7),
            sumShiftColumnTargets = listOf(4, 6),
            sumShiftSelectedCells = setOf(GridPoint(0, 0)),
            sumShiftManualDisabledCells = emptySet(),
            sumShiftPreparingBoard = false,
        )

        val next = logic.placePiece(initial, 0L, GridPoint(1, 0)).state

        assertTrue(GridPoint(0, 1) in next.sumShiftSelectedCells)
        assertTrue(GridPoint(1, 1) in next.sumShiftSelectedCells)
    }

    @Test
    fun placePiece_rechecksRowsAndColumnsAfterSystemDisabledCellsChange() {
        var board = BoardMatrix.empty(columns = 2, rows = 2)
        board = board.fill(points = listOf(GridPoint(0, 0)), tone = CellTone.Cyan, value = 1)
        board = board.fill(points = listOf(GridPoint(1, 0)), tone = CellTone.Gold, value = 2)
        board = board.fill(points = listOf(GridPoint(0, 1)), tone = CellTone.Violet, value = 3)
        board = board.fill(points = listOf(GridPoint(1, 1)), tone = CellTone.Emerald, value = 4)

        val initial = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStage.MatchRow).gameState.copy(
            config = GameConfig(columns = 2, rows = 2, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            board = board,
            sumShiftRowTargets = listOf(1, 7),
            sumShiftColumnTargets = listOf(4, 4),
            sumShiftSelectedCells = emptySet(),
            sumShiftManualDisabledCells = emptySet(),
            sumShiftPreparingBoard = false,
        )

        val next = logic.placePiece(initial, 0L, GridPoint(0, 0)).state

        assertTrue(GridPoint(0, 0) in next.sumShiftSelectedCells)
        assertTrue(GridPoint(0, 1) in next.sumShiftSelectedCells)
        assertTrue(GridPoint(1, 1) in next.sumShiftSelectedCells)
        assertTrue(GridPoint(1, 0) !in next.sumShiftSelectedCells)
        assertEquals(1, next.linesCleared)
    }
}


