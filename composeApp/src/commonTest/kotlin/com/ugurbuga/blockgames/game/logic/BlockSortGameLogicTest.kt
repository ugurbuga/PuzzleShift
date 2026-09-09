package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockSortGameLogicTest {

    @Test
    fun roundCapacity_cyclesAcrossRounds() {
        assertEquals(listOf(4, 4, 4, 5, 5, 6), (1..6).map(::blockSortRoundCapacity))
    }

    @Test
    fun roundColumns_increaseAcrossProgression() {
        assertEquals(listOf(6, 6, 6, 7, 7, 8, 7, 7, 7, 8, 8, 9), (1..12).map(::blockSortRoundColumns))
    }

    @Test
    fun roundDifficulty_repeatsThreeEasyTwoMediumOneHard() {
        assertEquals(
            listOf(
                BlockSortRoundDifficulty.Easy,
                BlockSortRoundDifficulty.Easy,
                BlockSortRoundDifficulty.Easy,
                BlockSortRoundDifficulty.Medium,
                BlockSortRoundDifficulty.Medium,
                BlockSortRoundDifficulty.Hard,
                BlockSortRoundDifficulty.Easy,
                BlockSortRoundDifficulty.Easy,
                BlockSortRoundDifficulty.Easy,
                BlockSortRoundDifficulty.Medium,
                BlockSortRoundDifficulty.Medium,
                BlockSortRoundDifficulty.Hard,
            ),
            (1..12).map(::blockSortRoundDifficulty),
        )
    }

    @Test
    fun roundEmptyColumns_alwaysOne() {
        assertEquals(List(8) { 1 }, (1..8).map { blockSortRoundEmptyColumns() })
    }

    @Test
    fun replaceActivePiece_addsExactlyOneEmptyColumn_forCurrentRound() {
        var board = BoardMatrix.empty(columns = 3, rows = 4)
        board = board.fill(points = listOf(GridPoint(0, 3)), tone = CellTone.Gold, value = 101)

        val logic = BlockSortGameLogic(random = Random(0))
        val initialState = GameState(
            config = GameConfig(columns = 3, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            gameplayStyle = GameplayStyle.BlockSort,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 9_999,
            status = GameStatus.Running,
        )

        val result = logic.replaceActivePiece(initialState, SpecialBlockType.None)

        assertEquals(4, result.state.config.columns)
        assertEquals(4, result.state.board.columns)
        assertTrue(result.state.board.isColumnEmpty(3))
        assertTrue(result.state.blockSortBonusEmptyColumnUsed)
        assertEquals(initialState.board.cellAt(0, 3), result.state.board.cellAt(0, 3))
    }

    @Test
    fun placePiece_updatesBlockSortSpecificChallengeTasks() {
        var board = BoardMatrix.empty(columns = 3, rows = 2)
        board = board.fill(points = listOf(GridPoint(0, 1)), tone = CellTone.Gold, value = 101)
        board = board.fill(points = listOf(GridPoint(1, 1)), tone = CellTone.Gold, value = 201)
        board = board.fill(points = listOf(GridPoint(2, 0), GridPoint(2, 1)), tone = CellTone.Cyan, value = 301)

        val challenge = DailyChallenge(
            year = 2026,
            month = 5,
            day = 16,
            style = GameplayStyle.BlockSort,
            tasks = listOf(
                ChallengeTask(type = ChallengeTaskType.ClearColumns, target = 2),
                ChallengeTask(type = ChallengeTaskType.ClearRounds, target = 1),
            ),
        )

        val result = BlockSortGameLogic(random = Random(0)).placePiece(
            state = GameState(
                config = GameConfig(columns = 3, rows = 2, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
                activeChallenge = challenge,
            ),
            pieceId = 0L,
            origin = GridPoint(1, 0),
        )

        val updatedChallenge = result.state.activeChallenge
        assertEquals(1, updatedChallenge?.tasks?.first { it.type == ChallengeTaskType.ClearColumns }?.current)
        assertEquals(1, updatedChallenge?.tasks?.first { it.type == ChallengeTaskType.ClearRounds }?.current)
    }

    @Test
    fun placePiece_recordsOnlyLatestMovedCellValuesForAnimation() {
        var board = BoardMatrix.empty(columns = 4, rows = 4)
        board = board.fill(points = listOf(GridPoint(0, 2)), tone = CellTone.Gold, value = 101)
        board = board.fill(points = listOf(GridPoint(0, 3)), tone = CellTone.Gold, value = 102)
        board = board.fill(points = listOf(GridPoint(2, 3)), tone = CellTone.Cyan, value = 201)

        val result = BlockSortGameLogic(random = Random(0)).placePiece(
            state = GameState(
                config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
            pieceId = 0L,
            origin = GridPoint(1, 0),
        )

        assertEquals(setOf(101, 102), result.state.blockSortLastMovedCellValues)
    }

    @Test
    fun placePiece_preservesSolvedRoundBoardWhenAdvancingLevel() {
        var board = BoardMatrix.empty(columns = 3, rows = 2)
        board = board.fill(points = listOf(GridPoint(0, 1)), tone = CellTone.Gold, value = 101)
        board = board.fill(points = listOf(GridPoint(1, 1)), tone = CellTone.Gold, value = 102)
        board = board.fill(points = listOf(GridPoint(2, 0), GridPoint(2, 1)), tone = CellTone.Cyan, value = 201)

        val result = BlockSortGameLogic(random = Random(0)).placePiece(
            state = GameState(
                config = GameConfig(columns = 3, rows = 2, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
            pieceId = 1L,
            origin = GridPoint(0, 0),
        )

        val completedBoard = result.state.blockSortCompletedRoundBoard
        assertEquals(2, result.state.level)
        assertEquals(setOf(102), result.state.blockSortLastMovedCellValues)
        assertEquals(CellTone.Gold, completedBoard?.toneAt(0, 0))
        assertEquals(CellTone.Gold, completedBoard?.toneAt(0, 1))
        assertEquals(0, completedBoard?.filledCellCount(1))
    }

    @Test
    fun placePiece_doesNotAwardScore_whenMovingOutOfCompletedColumn() {
        var board = BoardMatrix.empty(columns = 4, rows = 4)
        repeat(4) { index ->
            board = board.fill(points = listOf(GridPoint(0, index)), tone = CellTone.Gold, value = 100 + index)
        }
        board = board.fill(points = listOf(GridPoint(2, 3)), tone = CellTone.Cyan, value = 250)

        val result = BlockSortGameLogic(random = Random(0)).placePiece(
            state = GameState(
                config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 250,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
            pieceId = 0L,
            origin = GridPoint(1, 0),
        )

        assertEquals(250, result.state.score)
        assertEquals(0, result.state.lastMoveScore)
    }

    @Test
    fun placePiece_doesNotAwardScore_forRepeatedSameStackBetweenSameColumns() {
        var board = BoardMatrix.empty(columns = 3, rows = 4)
        board = board.fill(points = listOf(GridPoint(0, 2)), tone = CellTone.Gold, value = 101)
        board = board.fill(points = listOf(GridPoint(0, 3)), tone = CellTone.Gold, value = 102)

        val logic = BlockSortGameLogic(random = Random(0))
        val initialState = GameState(
            config = GameConfig(columns = 3, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            gameplayStyle = GameplayStyle.BlockSort,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 9_999,
            status = GameStatus.Running,
        )

        val firstMove = logic.placePiece(initialState, pieceId = 0L, origin = GridPoint(1, 0))
        val returnMove = logic.placePiece(firstMove.state, pieceId = 1L, origin = GridPoint(0, 0))

        assertTrue(firstMove.state.score > 0)
        assertEquals(firstMove.state.score, returnMove.state.score)
        assertEquals(0, returnMove.state.lastMoveScore)
    }

    @Test
    fun newGame_startsWithMixedOccupiedColumns() {
        val state = BlockSortGameLogic(random = Random(7)).newGame(
            config = GameConfig.default(GameplayStyle.BlockSort),
            mode = GameMode.Classic,
        )

        val occupiedColumns = (0 until state.config.columns).filter { column ->
            state.board.filledCellCount(column) > 0
        }
        assertTrue(occupiedColumns.isNotEmpty())

        occupiedColumns.forEach { column ->
            val tones = buildSet {
                for (row in 0 until state.config.rows) {
                    state.board.toneAt(column, row)?.let(::add)
                }
            }
            assertTrue(state.board.filledCellCount(column) > 1, "column $column should contain multiple pieces")
            assertTrue(tones.size > 1, "column $column should start mixed")
        }
    }

    @Test
    fun restoreGame_marksBoardWithoutMovesAsGameOver() {
        var board = BoardMatrix.empty(columns = 4, rows = 4)
        listOf(CellTone.Cyan, CellTone.Gold, CellTone.Violet, CellTone.Emerald).forEachIndexed { column, tone ->
            repeat(4) { index ->
                board = board.fill(
                    points = listOf(GridPoint(column, 3 - index)),
                    tone = tone,
                    value = (column * 10) + index,
                )
            }
        }

        val restored = BlockSortGameLogic(random = Random(0)).restoreGame(
            GameState(
                config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
        )

        assertEquals(GameStatus.GameOver, restored.status)
    }
}

