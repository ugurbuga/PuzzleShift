package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
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
import kotlin.math.min
import kotlin.random.Random

internal class BlockSortGameLogic(
    private val random: Random = Random.Default,
    @Suppress("unused") private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val MoveScorePerBlock = 50
        private const val CompletedColumnBonus = 120
        private const val RoundClearBonusBase = 600
        private const val RoundClearBonusPerLevel = 150
        private const val TimeAttackRoundBonusMillis = 5_000L
    }

    override fun restoreGame(state: GameState): GameState {
        val normalizedColumns = state.board.columns.takeIf { it > 0 } ?: roundColumns(level = state.level)
        val normalizedRows = state.board.rows.takeIf { it > 0 } ?: roundCapacity(level = state.level)
        val normalizedConfig = state.config.copy(
            columns = normalizedColumns,
            rows = normalizedRows,
            difficultyIntervalSeconds = 9_999,
            linesPerLevel = 9_999,
        )
        val hasMoves = hasAnyValidMove(state.copy(config = normalizedConfig))
        val status = when {
            state.status != GameStatus.Running -> state.status
            hasMoves -> GameStatus.Running
            else -> GameStatus.GameOver
        }
        return state.copy(
            config = normalizedConfig,
            status = status,
            difficultyStage = blockSortRoundDifficulty(state.level).ordinal,
            blockSortCompletedRoundBoard = null,
            blockSortLastMovedCellValues = emptySet(),
        )
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        return createRoundState(
            level = 1,
            totalScore = 0,
            totalSortedColumns = 0,
            challenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) }),
            mode = mode,
            remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
        )
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null

    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? {
        if (state.status != GameStatus.Running) return null
        val sourceColumn = pieceId.toInt()
        val targetColumn = origin.column
        val move = resolveMove(state, sourceColumn, targetColumn) ?: return null
        return PlacementPreview(
            selectedColumn = targetColumn,
            entryAnchor = GridPoint(sourceColumn, move.sourceTopRow),
            landingAnchor = GridPoint(targetColumn, move.targetRows.first()),
            occupiedCells = move.targetRows.map { row -> GridPoint(targetColumn, row) },
            coveredColumns = targetColumn..targetColumn,
            isPerfectDrop = state.board.isColumnEmpty(targetColumn),
        )
    }

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> {
        return preview?.occupiedCells?.toSet().orEmpty()
    }

    override fun placePiece(state: GameState, column: Int): GameMoveResult = invalidMove(state)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)
        val sourceColumn = pieceId.toInt()
        val targetColumn = origin.column
        val move = resolveMove(state, sourceColumn, targetColumn) ?: return invalidMove(state)
        val preview = previewPlacement(state, pieceId, origin)
            ?: return invalidMove(state)

        val movedCells = move.movedRows.mapNotNull { row -> state.board.cellAt(sourceColumn, row) }
        val movedCellValues = movedCells.mapNotNull { cell -> cell.value.takeIf { it != 0 } }.toSet()
        val moveScoreSignature = blockSortMoveScoreSignature(
            sourceColumn = sourceColumn,
            targetColumn = targetColumn,
            movedCellValues = movedCellValues,
        )
        val sourceWasCompleted = isCompletedColumn(state.board, sourceColumn)
        val moveIsScoreEligible = !sourceWasCompleted && moveScoreSignature !in state.blockSortScoredMoveSignatures
        var board = state.board.clearPoints(move.movedRows.mapTo(mutableSetOf()) { row -> GridPoint(sourceColumn, row) })
        move.targetRows.forEachIndexed { index, row ->
            val movedCell = movedCells.getOrNull(index) ?: return@forEachIndexed
            board = board.fill(
                points = listOf(GridPoint(targetColumn, row)),
                tone = movedCell.tone,
                special = movedCell.special,
                value = movedCell.value,
            )
        }

        val newlyCompletedColumns = setOf(sourceColumn, targetColumn).count { column ->
            isCompletedColumn(board, column) && !isCompletedColumn(state.board, column)
        }
        val moveScore = if (moveIsScoreEligible) {
            (move.moveCount * MoveScorePerBlock) + (newlyCompletedColumns * CompletedColumnBonus)
        } else {
            0
        }
        val nextScore = state.score + moveScore
        val nextSortedColumns = state.linesCleared + newlyCompletedColumns

        var updatedChallenge = updateChallengeProgress(
            challenge = state.activeChallenge,
            placedPieces = 1,
            score = nextScore,
            completedColumns = newlyCompletedColumns,
            clearedRounds = 0,
        )
        val events = mutableSetOf(GameEvent.PlacementAccepted)
        if (newlyCompletedColumns > 0) {
            events += GameEvent.LineClear
        }
        if (updatedChallenge?.isCompleted == true && state.activeChallenge?.isCompleted == false) {
            events += GameEvent.ChallengeCompleted
        }

        val baseState = state.copy(
            board = board,
            score = nextScore,
            lastMoveScore = moveScore,
            linesCleared = nextSortedColumns,
            recentlyClearedColumns = setOf(targetColumn).filterTo(mutableSetOf()) { isCompletedColumn(board, it) },
            status = GameStatus.Running,
            lastActionTime = currentEpochMillis(),
            activeChallenge = updatedChallenge,
            blockSortCompletedRoundBoard = null,
            blockSortLastMovedCellValues = movedCellValues,
            blockSortScoredMoveSignatures = state.blockSortScoredMoveSignatures + moveScoreSignature,
        )

        return if (isRoundSolved(board)) {
            val roundBonus = RoundClearBonusBase + (state.level * RoundClearBonusPerLevel)
            updatedChallenge = updateChallengeProgress(
                challenge = updatedChallenge,
                placedPieces = 0,
                score = nextScore + roundBonus,
                completedColumns = 0,
                clearedRounds = 1,
            )
            if (updatedChallenge?.isCompleted == true && state.activeChallenge?.isCompleted == false) {
                events += GameEvent.ChallengeCompleted
            }
            val nextLevel = state.level + 1
            val nextRoundState = createRoundState(
                level = nextLevel,
                totalScore = nextScore + roundBonus,
                totalSortedColumns = nextSortedColumns,
                challenge = updatedChallenge,
                mode = state.gameMode,
                remainingTimeMillis = state.remainingTimeMillis?.plus(TimeAttackRoundBonusMillis),
            )
            GameMoveResult(
                state = nextRoundState.copy(
                    lastMoveScore = moveScore + roundBonus,
                    blockSortCompletedRoundBoard = board,
                    blockSortLastMovedCellValues = movedCellValues,
                    blockSortBonusEmptyColumnUsed = false,
                    blockSortScoredMoveSignatures = emptySet(),
                ),
                preview = preview,
                events = events,
            )
        } else {
            val hasMoves = hasAnyValidMove(baseState)
            val finalState = if (hasMoves) {
                baseState
            } else {
                baseState.copy(status = GameStatus.GameOver)
            }
            if (!hasMoves) {
                events += GameEvent.GameOver
            }
            GameMoveResult(
                state = finalState,
                preview = preview,
                events = events,
            )
        }
    }

    override fun holdPiece(state: GameState): GameMoveResult = invalidMove(state)

    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)
        if (state.blockSortBonusEmptyColumnUsed) return invalidMove(state)
        val expandedBoard = addEmptyColumns(state.board, extraCount = 1)
        return GameMoveResult(
            state = state.copy(
                config = state.config.copy(columns = expandedBoard.columns),
                board = expandedBoard,
                lastActionTime = currentEpochMillis(),
                recentlyClearedColumns = emptySet(),
                blockSortCompletedRoundBoard = null,
                blockSortLastMovedCellValues = emptySet(),
                blockSortBonusEmptyColumnUsed = true,
            ),
            events = setOf(GameEvent.SpecialTriggered),
        )
    }

    override fun commitSoftLock(state: GameState): GameMoveResult = invalidMove(state)

    override fun reviveFromReward(state: GameState): GameMoveResult {
        if (state.rewardedReviveUsed) return invalidMove(state)
        val reshuffledBoard = reshuffleBoard(state.board, state.level)
            ?: generateRoundBoard(level = state.level)
        return GameMoveResult(
            state = state.copy(
                board = reshuffledBoard,
                status = GameStatus.Running,
                rewardedReviveUsed = true,
                recentlyClearedColumns = emptySet(),
                lastActionTime = currentEpochMillis(),
                blockSortCompletedRoundBoard = null,
                blockSortLastMovedCellValues = emptySet(),
                blockSortScoredMoveSignatures = emptySet(),
            ),
            events = setOf(GameEvent.Revived),
        )
    }

    override fun tick(state: GameState): GameState {
        val remainingTimeMillis = state.remainingTimeMillis ?: return state
        if (state.status != GameStatus.Running) return state
        val nextRemainingTime = (remainingTimeMillis - 1_000L).coerceAtLeast(0L)
        return if (nextRemainingTime == 0L) {
            state.copy(
                remainingTimeMillis = 0L,
                status = GameStatus.GameOver,
            )
        } else {
            state.copy(remainingTimeMillis = nextRemainingTime)
        }
    }

    private fun createRoundState(
        level: Int,
        totalScore: Int,
        totalSortedColumns: Int,
        challenge: DailyChallenge?,
        mode: GameMode,
        remainingTimeMillis: Long?,
    ): GameState {
        val columns = roundColumns(level)
        val rows = roundCapacity(level)
        val config = GameConfig(
            columns = columns,
            rows = rows,
            difficultyIntervalSeconds = 9_999,
            linesPerLevel = 9_999,
        )
        return GameState(
            config = config,
            gameMode = mode,
            gameplayStyle = GameplayStyle.BlockSort,
            board = generateRoundBoard(level),
            activePiece = null,
            nextQueue = emptyList(),
            score = totalScore,
            lastMoveScore = 0,
            linesCleared = totalSortedColumns,
            level = level,
            difficultyStage = blockSortRoundDifficulty(level).ordinal,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            status = GameStatus.Running,
            remainingTimeMillis = remainingTimeMillis,
            lastActionTime = currentEpochMillis(),
            activeChallenge = challenge,
            blockSortCompletedRoundBoard = null,
            blockSortLastMovedCellValues = emptySet(),
            blockSortBonusEmptyColumnUsed = false,
            blockSortScoredMoveSignatures = emptySet(),
        )
    }

    private fun roundColumns(level: Int): Int {
        return blockSortRoundColumns(level)
    }

    private fun roundCapacity(level: Int): Int = blockSortRoundCapacity(level)

    private fun roundColorCount(level: Int): Int {
        return (roundColumns(level) - blockSortRoundEmptyColumns()).coerceAtLeast(3)
    }

    private fun generateRoundBoard(level: Int): BoardMatrix {
        val columns = roundColumns(level)
        val rows = roundCapacity(level)
        val difficulty = blockSortRoundDifficulty(level)
        val colors = availableTones(level).take(roundColorCount(level))
        var bestBoard: BoardMatrix? = null
        var bestScore = Int.MIN_VALUE

        repeat(48) { attempt ->
            val containers = MutableList(columns) { mutableListOf<CellTone>() }
            colors.forEachIndexed { index, tone ->
                repeat(rows) { containers[index].add(tone) }
            }
            containers.shuffle(random)

            val reverseMultiplier = when (difficulty) {
                BlockSortRoundDifficulty.Easy -> 1.65f
                BlockSortRoundDifficulty.Medium -> 2.05f
                BlockSortRoundDifficulty.Hard -> 2.45f
            }
            val reverseSteps = ((columns * rows * reverseMultiplier) + (level * 4) + (attempt * 2)).toInt()
            repeat(reverseSteps) {
                val targetIndices = containers.indices.filter { containers[it].isNotEmpty() }
                if (targetIndices.isEmpty()) return@repeat
                val target = targetIndices.random(random)
                val targetTopTone = containers[target].last()
                val targetRun = containers[target].asReversed().takeWhile { it == targetTopTone }.size
                val targetSize = containers[target].size
                val maxTransferOut = when {
                    targetRun == targetSize -> targetRun
                    targetRun > 1 -> targetRun - 1
                    else -> 0
                }
                if (maxTransferOut <= 0) return@repeat

                val sourceIndices = containers.indices.filter { source ->
                    source != target && containers[source].size < rows && (
                        containers[source].isEmpty() || containers[source].last() != targetTopTone
                    )
                }
                if (sourceIndices.isEmpty()) return@repeat

                val source = sourceIndices.random(random)
                val free = rows - containers[source].size
                val maxTransfer = min(maxTransferOut, free)
                if (maxTransfer <= 0) return@repeat

                val transfer = 1 + random.nextInt(maxTransfer)
                repeat(transfer) {
                    containers[source].add(containers[target].removeAt(containers[target].lastIndex))
                }
            }

            val board = containers.toBoardMatrix(columns, rows)
            if (isInterestingStartingBoard(board)) {
                return board
            }

            val score = startingBoardScore(board)
            if (!isRoundSolved(board) && hasAnyValidMove(board) && score > bestScore) {
                bestBoard = board
                bestScore = score
            }
        }

        return bestBoard ?: colors
            .map { tone -> MutableList(rows) { tone } }
            .plus(List(blockSortRoundEmptyColumns()) { mutableListOf() })
            .toBoardMatrix(columns, rows)
    }

    private fun availableTones(level: Int): List<CellTone> {
        val tones = CellTone.entries.toMutableList()
        tones.shuffle(Random(level * 91 + 17))
        return tones
    }

    private fun reshuffleBoard(board: BoardMatrix, level: Int): BoardMatrix? {
        val columns = MutableList(board.columns) { mutableListOf<CellTone>() }
        for (column in 0 until board.columns) {
            for (row in (board.rows - 1) downTo 0) {
                val tone = board.toneAt(column, row) ?: continue
                columns[column].add(tone)
            }
        }
        val flattened = columns.flatten()
        if (flattened.isEmpty()) return null

        var bestBoard: BoardMatrix? = null
        var bestScore = Int.MIN_VALUE

        repeat(24) { attempt ->
            val nextColumns = MutableList(board.columns) { mutableListOf<CellTone>() }
            val shuffled = flattened.shuffled(Random(level * 101 + attempt + 31))
            var cursor = 0
            for (column in 0 until board.columns) {
                val originalSize = columns[column].size
                repeat(originalSize) {
                    nextColumns[column].add(shuffled[cursor++])
                }
            }
            val reshuffled = nextColumns.toBoardMatrix(board.columns)
            if (!isRoundSolved(reshuffled) && hasAnyValidMove(reshuffled) && occupiedColumnsAreMixed(reshuffled)) {
                return reshuffled
            }
            val score = startingBoardScore(reshuffled)
            if (!isRoundSolved(reshuffled) && hasAnyValidMove(reshuffled) && score > bestScore) {
                bestBoard = reshuffled
                bestScore = score
            }
        }
        return bestBoard
    }

    private fun resolveMove(state: GameState, sourceColumn: Int, targetColumn: Int): BlockSortMove? {
        if (sourceColumn !in 0 until state.config.columns || targetColumn !in 0 until state.config.columns) return null
        if (sourceColumn == targetColumn) return null
        val sourceTopRow = state.board.topOccupiedRow(sourceColumn) ?: return null
        val sourceTone = state.board.toneAt(sourceColumn, sourceTopRow) ?: return null
        val targetFilled = state.board.filledCellCount(targetColumn)
        val freeSlots = state.config.rows - targetFilled
        if (freeSlots <= 0) return null
        val targetTopRow = state.board.topOccupiedRow(targetColumn)
        val targetTone = targetTopRow?.let { state.board.toneAt(targetColumn, it) }
        if (targetTone != null && targetTone != sourceTone) return null

        var runLength = 0
        for (row in sourceTopRow until state.config.rows) {
            if (state.board.toneAt(sourceColumn, row) == sourceTone) {
                runLength += 1
            } else {
                break
            }
        }
        if (runLength <= 0) return null

        val moveCount = min(runLength, freeSlots)
        if (moveCount <= 0) return null
        val movedRows = (sourceTopRow until (sourceTopRow + moveCount)).toList()
        val firstTargetRow = if (targetTopRow != null) targetTopRow - moveCount else state.config.rows - moveCount
        if (firstTargetRow < 0) return null
        val targetRows = (firstTargetRow until (firstTargetRow + moveCount)).toList()
        return BlockSortMove(
            sourceTopRow = sourceTopRow,
            moveCount = moveCount,
            movedRows = movedRows,
            targetRows = targetRows,
        )
    }

    private fun hasAnyValidMove(state: GameState): Boolean = hasAnyValidMove(state.board)

    private fun hasAnyValidMove(board: BoardMatrix): Boolean {
        for (source in 0 until board.columns) {
            val sourceTopRow = board.topOccupiedRow(source) ?: continue
            val sourceTone = board.toneAt(source, sourceTopRow) ?: continue
            for (target in 0 until board.columns) {
                if (source == target) continue
                val targetHeight = board.filledCellCount(target)
                if (targetHeight >= board.rows) continue
                val targetTopRow = board.topOccupiedRow(target)
                val targetTone = targetTopRow?.let { board.toneAt(target, it) }
                if (targetTone == null || targetTone == sourceTone) {
                    return true
                }
            }
        }
        return false
    }

    private fun isRoundSolved(board: BoardMatrix): Boolean {
        if (board.occupiedCount == 0) return false
        for (column in 0 until board.columns) {
            val filled = board.filledCellCount(column)
            if (filled == 0) continue
            if (!isCompletedColumn(board, column)) return false
        }
        return true
    }

    private fun isCompletedColumn(board: BoardMatrix, column: Int): Boolean {
        if (board.filledCellCount(column) != board.rows) return false
        val topTone = board.topOccupiedRow(column)?.let { board.toneAt(column, it) } ?: return false
        for (row in 0 until board.rows) {
            if (board.toneAt(column, row) != topTone) return false
        }
        return true
    }

    private fun isInterestingStartingBoard(board: BoardMatrix): Boolean {
        if (board.occupiedCount == 0) return false
        if (isRoundSolved(board) || !hasAnyValidMove(board)) return false
        return occupiedColumnsAreMixed(board)
    }

    private fun occupiedColumnsAreMixed(board: BoardMatrix): Boolean {
        val occupiedColumns = (0 until board.columns).filter { board.filledCellCount(it) > 0 }
        if (occupiedColumns.isEmpty()) return false
        return occupiedColumns.all { column ->
            val filled = board.filledCellCount(column)
            filled > 1 && distinctToneCount(board, column) > 1
        }
    }

    private fun distinctToneCount(board: BoardMatrix, column: Int): Int = buildSet {
        for (row in 0 until board.rows) {
            board.toneAt(column, row)?.let(::add)
        }
    }.size

    private fun startingBoardScore(board: BoardMatrix): Int {
        var score = 0
        for (column in 0 until board.columns) {
            val filled = board.filledCellCount(column)
            if (filled == 0) continue
            val distinctTones = distinctToneCount(board, column)
            score += filled * 4
            score += distinctTones * 14
            if (filled <= 1) score -= 18
            if (distinctTones <= 1) score -= 24
        }
        return score
    }

    private fun updateChallengeProgress(
        challenge: DailyChallenge?,
        placedPieces: Int,
        score: Int,
        completedColumns: Int,
        clearedRounds: Int,
    ): DailyChallenge? {
        challenge ?: return null
        return challenge.copy(
            tasks = challenge.tasks.map { task ->
                when (task.type) {
                    ChallengeTaskType.ReachScore -> task.copy(current = score.coerceAtLeast(task.current))
                    ChallengeTaskType.PlacePieces -> task.copy(current = task.current + placedPieces)
                    ChallengeTaskType.ClearColumns -> task.copy(current = task.current + completedColumns)
                    ChallengeTaskType.ClearRounds -> task.copy(current = task.current + clearedRounds)
                    else -> task
                }
            },
        )
    }

    private fun invalidMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state,
        events = setOf(GameEvent.InvalidDrop),
    )

    private data class BlockSortMove(
        val sourceTopRow: Int,
        val moveCount: Int,
        val movedRows: List<Int>,
        val targetRows: List<Int>,
    )
}

internal fun blockSortRoundCapacity(level: Int): Int {
    return when (blockSortRoundDifficulty(level)) {
        BlockSortRoundDifficulty.Easy -> 4
        BlockSortRoundDifficulty.Medium -> 5
        BlockSortRoundDifficulty.Hard -> 6
    }
}

internal fun blockSortRoundColumns(level: Int): Int {
    val cycleIndex = (level - 1).coerceAtLeast(0) / 6
    val baseColumns = (6 + cycleIndex).coerceAtMost(8)
    val difficultyOffset = when (blockSortRoundDifficulty(level)) {
        BlockSortRoundDifficulty.Easy -> 0
        BlockSortRoundDifficulty.Medium -> 1
        BlockSortRoundDifficulty.Hard -> 2
    }
    return (baseColumns + difficultyOffset).coerceAtMost(9)
}

internal fun blockSortRoundEmptyColumns(): Int {
    return 1
}

internal enum class BlockSortRoundDifficulty {
    Easy,
    Medium,
    Hard,
}

internal fun blockSortRoundDifficulty(level: Int): BlockSortRoundDifficulty {
    return when ((level - 1).mod(6)) {
        0, 1, 2 -> BlockSortRoundDifficulty.Easy
        3, 4 -> BlockSortRoundDifficulty.Medium
        else -> BlockSortRoundDifficulty.Hard
    }
}

private fun addEmptyColumns(
    board: BoardMatrix,
    extraCount: Int,
): BoardMatrix {
    if (extraCount <= 0) return board
    var expanded = BoardMatrix.empty(columns = board.columns + extraCount, rows = board.rows)
    for (column in 0 until board.columns) {
        for (row in 0 until board.rows) {
            val cell = board.cellAt(column, row) ?: continue
            expanded = expanded.fill(
                points = listOf(GridPoint(column, row)),
                tone = cell.tone,
                special = cell.special,
                value = cell.value,
            )
        }
    }
    return expanded
}

private fun List<MutableList<CellTone>>.toBoardMatrix(columnCount: Int, rows: Int = 4): BoardMatrix {
    var board = BoardMatrix.empty(columns = columnCount, rows = rows)
    forEachIndexed { column, stack ->
        stack.forEachIndexed { index, tone ->
            val row = rows - 1 - index
            board = board.fill(
                points = listOf(GridPoint(column, row)),
                tone = tone,
                value = (column * 1000) + index + 1,
            )
        }
    }
    return board
}

private fun blockSortMoveScoreSignature(
    sourceColumn: Int,
    targetColumn: Int,
    movedCellValues: Set<Int>,
): String {
    val minColumn = minOf(sourceColumn, targetColumn)
    val maxColumn = maxOf(sourceColumn, targetColumn)
    val cellPart = movedCellValues.sorted().joinToString(separator = "x")
    return "c${minColumn}x${maxColumn}x$cellPart"
}


