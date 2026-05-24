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
import kotlin.random.Random

internal class SumShiftGameLogic(
    private val random: Random = Random.Default,
    scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {
    @Suppress("unused")
    private val ignoredScoreCalculator = scoreCalculator

    override fun restoreGame(state: GameState): GameState {
        val config = normalizeConfig(state.config)
        if (
            state.gameplayStyle != GameplayStyle.SumShift ||
            state.board.columns != config.columns ||
            state.board.rows != config.rows ||
            state.sumShiftRowTargets.size != config.rows ||
            state.sumShiftColumnTargets.size != config.columns
        ) {
            return newGame(config = config, challenge = state.activeChallenge, mode = state.gameMode)
        }
        val sanitizedSelection = state.sumShiftSelectedCells.filterTo(linkedSetOf()) { point ->
            point.column in 0 until config.columns && point.row in 0 until config.rows
        }
        val sanitizedManualDisabled = state.sumShiftManualDisabledCells.filterTo(linkedSetOf()) { point ->
            point.column in 0 until config.columns && point.row in 0 until config.rows
        }
        val shouldAdvanceToNextBoard = state.sumShiftPreparingBoard
        val restoredState = state.copy(
            config = config,
            sumShiftSelectedCells = sanitizedSelection,
            sumShiftManualDisabledCells = sanitizedManualDisabled,
            sumShiftPreparingBoard = false,
        )
        return if (shouldAdvanceToNextBoard || isSolved(restoredState)) {
            generateNextSumShiftBoard(restoredState, random)
        } else {
            restoredState
        }
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        return createSumShiftNewGame(config, challenge, mode, random)
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null

    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? =
        if (origin.isInside(state.config)) {
            PlacementPreview(
                selectedColumn = origin.column,
                entryAnchor = origin,
                landingAnchor = origin,
                occupiedCells = listOf(origin),
                coveredColumns = origin.column..origin.column,
            )
        } else {
            null
        }

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> =
        preview?.occupiedCells?.toSet().orEmpty()

    override fun placePiece(state: GameState, column: Int): GameMoveResult = invalidMove(state)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        if (state.status != GameStatus.Running || !origin.isInside(state.config)) return invalidMove(state)

        val nextSelection = state.sumShiftSelectedCells.toMutableSet().apply {
            if (!add(origin)) remove(origin)
        }
        val updatedState = state.copy(
            sumShiftSelectedCells = resolveSumShiftAutoSelectedCells(
                state = state,
                selectedCells = nextSelection,
                manualDisabledCells = state.sumShiftManualDisabledCells,
            ),
            lastActionTime = currentEpochMillis(),
        )

        val allSolved = isSolved(updatedState)
        if (!allSolved) {
            return GameMoveResult(
                state = updatedState,
                events = setOf(GameEvent.PlacementAccepted),
            )
        }

        val scoreGain = 180 + (updatedState.sumShiftSelectedCells.size * 24) + (updatedState.config.rows * 18)
        val nextScore = updatedState.score + scoreGain
        val nextSolvedBoards = updatedState.linesCleared + 1
        val nextLevel = 1 + (nextSolvedBoards / 3)
        val updatedChallenge = updateChallenge(
            challenge = updatedState.activeChallenge,
            score = nextScore,
            solvedBoards = nextSolvedBoards,
        )
        val challengeCompleted = updatedState.activeChallenge?.isCompleted != true && updatedChallenge?.isCompleted == true
        val awardedTimeMillis = if (updatedState.gameMode == GameMode.TimeAttack) {
            2_500L + (updatedState.sumShiftSelectedCells.size * 120L)
        } else {
            0L
        }

        return GameMoveResult(
            state = updatedState.copy(
                score = nextScore,
                lastMoveScore = scoreGain,
                linesCleared = nextSolvedBoards,
                level = nextLevel,
                difficultyStage = nextLevel - 1,
                sumShiftManualDisabledCells = emptySet(),
                sumShiftPreparingBoard = false,
                feedbackToken = updatedState.feedbackToken + 1,
                activeChallenge = updatedChallenge,
                remainingTimeMillis = updatedState.remainingTimeMillis?.plus(awardedTimeMillis),
            ),
            events = buildSet {
                add(GameEvent.PlacementAccepted)
                add(GameEvent.LineClear)
                if (challengeCompleted) add(GameEvent.ChallengeCompleted)
            },
        )
    }

    override fun holdPiece(state: GameState): GameMoveResult = GameMoveResult(state)

    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult = GameMoveResult(state)

    override fun commitSoftLock(state: GameState): GameMoveResult = GameMoveResult(state)

    override fun reviveFromReward(state: GameState): GameMoveResult {
        if (state.status != GameStatus.GameOver || state.rewardedReviveUsed) return GameMoveResult(state)
        return GameMoveResult(
            state = state.copy(
                status = GameStatus.Running,
                rewardedReviveUsed = true,
                remainingTimeMillis = state.remainingTimeMillis?.plus(GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS),
            ),
            events = setOf(GameEvent.Revived),
        )
    }

    override fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running) return state
        val remainingTimeMillis = state.remainingTimeMillis ?: return state
        val nextRemainingTimeMillis = (remainingTimeMillis - 1_000L).coerceAtLeast(0L)
        return if (nextRemainingTimeMillis <= 0L) {
            state.copy(
                status = GameStatus.GameOver,
                remainingTimeMillis = 0L,
            )
        } else {
            state.copy(remainingTimeMillis = nextRemainingTimeMillis)
        }
    }

    private fun normalizeConfig(config: GameConfig): GameConfig = normalizeSumShiftConfig(config)

    private fun isSolved(state: GameState): Boolean {
        val rowsSolved = state.sumShiftRowTargets.indices.all { index ->
            rowSum(state, index) == state.sumShiftRowTargets[index]
        }
        val columnsSolved = state.sumShiftColumnTargets.indices.all { index ->
            columnSum(state, index) == state.sumShiftColumnTargets[index]
        }
        return rowsSolved && columnsSolved
    }

    private fun rowSum(state: GameState, rowIndex: Int): Int =
        (0 until state.config.columns).sumOf { column ->
            val point = GridPoint(column, rowIndex)
            if (point in state.sumShiftSelectedCells) state.board.cellAt(column, rowIndex)?.value ?: 0 else 0
        }

    private fun columnSum(state: GameState, columnIndex: Int): Int =
        (0 until state.config.rows).sumOf { row ->
            val point = GridPoint(columnIndex, row)
            if (point in state.sumShiftSelectedCells) state.board.cellAt(columnIndex, row)?.value ?: 0 else 0
        }

    private fun updateChallenge(
        challenge: DailyChallenge?,
        score: Int,
        solvedBoards: Int,
    ): DailyChallenge? {
        challenge ?: return null
        return challenge.copy(
            tasks = challenge.tasks.map { task ->
                when (task.type) {
                    ChallengeTaskType.ReachScore -> task.copy(current = score)
                    else -> task
                }
            },
        )
    }

    private fun invalidMove(state: GameState): GameMoveResult =
        GameMoveResult(state = state, events = setOf(GameEvent.InvalidDrop))
}

private data class SumShiftPuzzle(
    val board: BoardMatrix,
    val rowTargets: List<Int>,
    val columnTargets: List<Int>,
)

private enum class SumShiftPatternStyle {
    RandomWalk,
    Bands,
    Clusters,
    Waves,
    Scatter,
}

internal fun createSumShiftNewGame(
    config: GameConfig,
    challenge: DailyChallenge?,
    mode: GameMode,
    random: Random = Random.Default,
): GameState {
    val resolvedConfig = normalizeSumShiftConfig(config)
    val puzzle = generateSumShiftPuzzle(resolvedConfig, random)
    return GameState(
        config = resolvedConfig,
        gameMode = mode,
        gameplayStyle = GameplayStyle.SumShift,
        board = puzzle.board,
        activePiece = null,
        nextQueue = emptyList(),
        holdPiece = null,
        canHold = false,
        score = 0,
        linesCleared = 0,
        level = 1,
        difficultyStage = 0,
        secondsUntilDifficultyIncrease = resolvedConfig.difficultyIntervalSeconds,
        status = GameStatus.Running,
        lastActionTime = currentEpochMillis(),
        remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
        activeChallenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) }),
        sumShiftRowTargets = puzzle.rowTargets,
        sumShiftColumnTargets = puzzle.columnTargets,
        sumShiftSelectedCells = emptySet(),
        sumShiftManualDisabledCells = emptySet(),
        sumShiftMistakesUsed = 0,
        sumShiftPreparingBoard = false,
    )
}

internal fun generateNextSumShiftBoard(
    state: GameState,
    random: Random = Random.Default,
): GameState {
    val resolvedConfig = normalizeSumShiftConfig(state.config)
    val puzzle = generateSumShiftPuzzle(resolvedConfig, random)
    return state.copy(
        config = resolvedConfig,
        board = puzzle.board,
        status = GameStatus.Running,
        sumShiftRowTargets = puzzle.rowTargets,
        sumShiftColumnTargets = puzzle.columnTargets,
        sumShiftSelectedCells = emptySet(),
        sumShiftManualDisabledCells = emptySet(),
        sumShiftMistakesUsed = 0,
        sumShiftPreparingBoard = false,
        clearAnimationToken = state.clearAnimationToken + 1,
        lastActionTime = currentEpochMillis(),
    )
}

private fun generateSumShiftPuzzle(
    config: GameConfig,
    random: Random = Random.Default,
): SumShiftPuzzle {
    val columns = config.columns
    val rows = config.rows
    val area = rows * columns
    val targetDensity = random.nextDouble(from = 0.30, until = 0.54)
    val candidateCount = when {
        area >= 48 -> 80
        area >= 36 -> 64
        else -> 48
    }

    var bestPuzzle: SumShiftPuzzle? = null
    var bestScore = Int.MIN_VALUE

    repeat(candidateCount) {
        val values = generateSumShiftValues(rows = rows, columns = columns, random = random)
        val mask = generateMaskCandidate(
            rows = rows,
            columns = columns,
            targetDensity = targetDensity,
            style = SumShiftPatternStyle.entries.random(random),
            random = random,
        )
        if (!isUsefulSumShiftMask(mask, rows, columns)) return@repeat

        val puzzle = buildSumShiftPuzzle(values = values, solution = mask)
        val score = scoreSumShiftPuzzle(mask = mask, puzzle = puzzle)
        if (score > bestScore) {
            bestScore = score
            bestPuzzle = puzzle
        }
    }

    return bestPuzzle ?: fallbackSumShiftPuzzle(rows = rows, columns = columns, random = random)
}

internal fun isValidSumShiftConfig(config: GameConfig): Boolean {
    if (config.columns !in 5..6) return false
    val minimumRows = config.columns.coerceAtLeast(5)
    return config.rows in minimumRows..9
}

internal fun normalizeSumShiftConfig(config: GameConfig): GameConfig {
    val columns = config.columns.coerceIn(5, 6)
    val minimumRows = columns.coerceAtLeast(5)
    return GameConfig(
        columns = columns,
        rows = config.rows.coerceIn(minimumRows, 9),
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )
}

internal fun randomSumShiftConfig(random: Random = Random.Default): GameConfig {
    val options = buildList {
        for (rows in 5..9) add(GameConfig(columns = 5, rows = rows, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999))
        for (rows in 6..9) add(GameConfig(columns = 6, rows = rows, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999))
    }
    return normalizeSumShiftConfig(options.random(random))
}

internal fun resolveSumShiftAutoSelectedCells(
    state: GameState,
    selectedCells: Set<GridPoint> = state.sumShiftSelectedCells,
    manualDisabledCells: Set<GridPoint> = state.sumShiftManualDisabledCells,
): Set<GridPoint> {
    val resolved = selectedCells.toMutableSet()
    while (true) {
        val disabledCells = manualDisabledCells + systemDisabledSumShiftCells(state, resolved)
        val nextAutoSelected = buildSet {
            state.sumShiftRowTargets.indices.forEach { rowIndex ->
                val enabledPoints = (0 until state.config.columns)
                    .map { columnIndex -> GridPoint(columnIndex, rowIndex) }
                    .filter { it !in disabledCells }
                val enabledSum = enabledPoints.sumOf { point ->
                    state.board.cellAt(point.column, point.row)?.value ?: 0
                }
                if (enabledSum == state.sumShiftRowTargets[rowIndex]) {
                    addAll(enabledPoints)
                }
            }
            state.sumShiftColumnTargets.indices.forEach { columnIndex ->
                val enabledPoints = (0 until state.config.rows)
                    .map { rowIndex -> GridPoint(columnIndex, rowIndex) }
                    .filter { it !in disabledCells }
                val enabledSum = enabledPoints.sumOf { point ->
                    state.board.cellAt(point.column, point.row)?.value ?: 0
                }
                if (enabledSum == state.sumShiftColumnTargets[columnIndex]) {
                    addAll(enabledPoints)
                }
            }
        }
        val changed = resolved.addAll(nextAutoSelected)
        if (!changed) return resolved
    }
}

private fun systemDisabledSumShiftCells(
    state: GameState,
    selectedCells: Set<GridPoint>,
): Set<GridPoint> = buildSet {
    state.sumShiftRowTargets.indices.forEach { rowIndex ->
        if (selectedSumShiftRow(state, selectedCells, rowIndex) != state.sumShiftRowTargets[rowIndex]) return@forEach
        repeat(state.config.columns) { columnIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point !in selectedCells) {
                add(point)
            }
        }
    }
    state.sumShiftColumnTargets.indices.forEach { columnIndex ->
        if (selectedSumShiftColumn(state, selectedCells, columnIndex) != state.sumShiftColumnTargets[columnIndex]) return@forEach
        repeat(state.config.rows) { rowIndex ->
            val point = GridPoint(columnIndex, rowIndex)
            if (point !in selectedCells) {
                add(point)
            }
        }
    }
}

private fun selectedSumShiftRow(
    state: GameState,
    selectedCells: Set<GridPoint>,
    rowIndex: Int,
): Int =
    (0 until state.config.columns).sumOf { columnIndex ->
        val point = GridPoint(columnIndex, rowIndex)
        if (point in selectedCells) state.board.cellAt(columnIndex, rowIndex)?.value ?: 0 else 0
    }

private fun selectedSumShiftColumn(
    state: GameState,
    selectedCells: Set<GridPoint>,
    columnIndex: Int,
): Int =
    (0 until state.config.rows).sumOf { rowIndex ->
        val point = GridPoint(columnIndex, rowIndex)
        if (point in selectedCells) state.board.cellAt(columnIndex, rowIndex)?.value ?: 0 else 0
    }

private fun GridPoint.isInside(config: GameConfig): Boolean =
    column in 0 until config.columns && row in 0 until config.rows

private fun generateSumShiftValues(
    rows: Int,
    columns: Int,
    random: Random,
): List<List<Int>> {
    val rowBiases = List(rows) { random.nextInt(0, 3) }
    val columnBiases = List(columns) { random.nextInt(0, 3) }
    return List(rows) { row ->
        List(columns) { column ->
            val raw = random.nextInt(1, 10) + rowBiases[row] + columnBiases[column]
            ((raw - 1) % 9) + 1
        }
    }
}

private fun generateMaskCandidate(
    rows: Int,
    columns: Int,
    targetDensity: Double,
    style: SumShiftPatternStyle,
    random: Random,
): List<List<Boolean>> {
    val targetSelectedCount = ((rows * columns) * targetDensity).toInt().coerceIn(rows + 1, (rows * columns) - columns)
    val mask = MutableList(rows) { MutableList(columns) { false } }

    when (style) {
        SumShiftPatternStyle.RandomWalk -> fillRandomWalkMask(mask, targetSelectedCount, random)
        SumShiftPatternStyle.Bands -> fillBandMask(mask, targetSelectedCount, random)
        SumShiftPatternStyle.Clusters -> fillClusterMask(mask, targetSelectedCount, random)
        SumShiftPatternStyle.Waves -> fillWaveMask(mask, targetSelectedCount, random)
        SumShiftPatternStyle.Scatter -> fillScatterMask(mask, targetSelectedCount, random)
    }

    repairMaskCoverage(mask, random)
    repairMaskHoles(mask, random)
    trimMaskToTarget(mask, targetSelectedCount, random)
    repairMaskCoverage(mask, random)
    repairMaskHoles(mask, random)
    return mask.map { it.toList() }
}

private fun fillRandomWalkMask(
    mask: MutableList<MutableList<Boolean>>,
    targetSelectedCount: Int,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    var row = random.nextInt(rows)
    var column = random.nextInt(columns)
    repeat(targetSelectedCount * 5) {
        mask[row][column] = true
        if (selectedCount(mask) >= targetSelectedCount) return
        val neighbors = buildList {
            if (row > 0) add(row - 1 to column)
            if (row < rows - 1) add(row + 1 to column)
            if (column > 0) add(row to column - 1)
            if (column < columns - 1) add(row to column + 1)
        }
        val (nextRow, nextColumn) = if (random.nextFloat() < 0.18f) {
            random.nextInt(rows) to random.nextInt(columns)
        } else {
            neighbors.random(random)
        }
        row = nextRow
        column = nextColumn
    }
}

private fun fillBandMask(
    mask: MutableList<MutableList<Boolean>>,
    targetSelectedCount: Int,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    repeat(rows) { row ->
        val segmentCount = if (random.nextFloat() < 0.34f) 2 else 1
        repeat(segmentCount) {
            val start = random.nextInt(columns)
            val length = random.nextInt(1, (columns / 2).coerceAtLeast(2) + 1)
            for (column in start until (start + length).coerceAtMost(columns)) {
                mask[row][column] = true
            }
        }
    }
    if (selectedCount(mask) < targetSelectedCount) {
        repeat(columns) { column ->
            if (random.nextBoolean()) {
                val start = random.nextInt(rows)
                val length = random.nextInt(1, (rows / 3).coerceAtLeast(2) + 1)
                for (row in start until (start + length).coerceAtMost(rows)) {
                    mask[row][column] = true
                }
            }
        }
    }
}

private fun fillClusterMask(
    mask: MutableList<MutableList<Boolean>>,
    targetSelectedCount: Int,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    val centers = List(random.nextInt(2, 5)) {
        GridPoint(column = random.nextInt(columns), row = random.nextInt(rows))
    }
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val minDistance = centers.minOf { center ->
                kotlin.math.abs(center.column - column) + kotlin.math.abs(center.row - row)
            }
            val chance = when (minDistance) {
                0 -> 0.92f
                1 -> 0.68f
                2 -> 0.38f
                else -> 0.16f
            }
            if (random.nextFloat() < chance) {
                mask[row][column] = true
            }
        }
    }
    if (selectedCount(mask) < targetSelectedCount / 2) {
        fillScatterMask(mask, targetSelectedCount, random)
    }
}

private fun fillWaveMask(
    mask: MutableList<MutableList<Boolean>>,
    targetSelectedCount: Int,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    val period = random.nextInt(2, 5)
    val rowWeight = random.nextInt(1, 4)
    val columnWeight = random.nextInt(1, 4)
    val offset = random.nextInt(period)
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val isWaveCell = ((row * rowWeight) + (column * columnWeight) + offset) % period == 0
            if (isWaveCell || random.nextFloat() < 0.12f) {
                mask[row][column] = true
            }
        }
    }
    if (selectedCount(mask) < targetSelectedCount / 2) {
        fillRandomWalkMask(mask, targetSelectedCount, random)
    }
}

private fun fillScatterMask(
    mask: MutableList<MutableList<Boolean>>,
    targetSelectedCount: Int,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    val density = targetSelectedCount.toFloat() / (rows * columns).toFloat()
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val neighborBonus = countSelectedNeighbors(mask, row, column) * 0.08f
            if (random.nextFloat() < density + neighborBonus) {
                mask[row][column] = true
            }
        }
    }
}

private fun repairMaskCoverage(
    mask: MutableList<MutableList<Boolean>>,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    repeat(rows) { row ->
        if (mask[row].none { it }) {
            mask[row][random.nextInt(columns)] = true
        }
    }
    repeat(columns) { column ->
        if ((0 until rows).none { row -> mask[row][column] }) {
            mask[random.nextInt(rows)][column] = true
        }
    }
}

private fun repairMaskHoles(
    mask: MutableList<MutableList<Boolean>>,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    repeat(rows) { row ->
        if (mask[row].all { it }) {
            val removableColumns = (0 until columns).filter { column ->
                (0 until rows).count { currentRow -> mask[currentRow][column] } > 1
            }
            val columnToClear = removableColumns.randomOrNull(random) ?: random.nextInt(columns)
            mask[row][columnToClear] = false
        }
    }
    repeat(columns) { column ->
        if ((0 until rows).all { row -> mask[row][column] }) {
            val removableRows = (0 until rows).filter { row -> mask[row].count { it } > 1 }
            val rowToClear = removableRows.randomOrNull(random) ?: random.nextInt(rows)
            mask[rowToClear][column] = false
        }
    }
}

private fun trimMaskToTarget(
    mask: MutableList<MutableList<Boolean>>,
    targetSelectedCount: Int,
    random: Random,
) {
    val rows = mask.size
    val columns = mask.firstOrNull()?.size ?: return
    while (selectedCount(mask) > targetSelectedCount) {
        val removablePoints = buildList {
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    if (!mask[row][column]) continue
                    if (mask[row].count { it } <= 1) continue
                    if ((0 until rows).count { currentRow -> mask[currentRow][column] } <= 1) continue
                    add(GridPoint(column = column, row = row))
                }
            }
        }
        val point = removablePoints.randomOrNull(random) ?: break
        mask[point.row][point.column] = false
    }
}

private fun isUsefulSumShiftMask(
    mask: List<List<Boolean>>,
    rows: Int,
    columns: Int,
): Boolean {
    if (mask.any { row -> row.none { it } || row.all { it } }) return false
    if ((0 until columns).any { column ->
            (0 until rows).none { row -> mask[row][column] } || (0 until rows).all { row -> mask[row][column] }
        }) {
        return false
    }
    return true
}

private fun buildSumShiftPuzzle(
    values: List<List<Int>>,
    solution: List<List<Boolean>>,
): SumShiftPuzzle {
    val rows = values.size
    val columns = values.firstOrNull()?.size ?: 0
    val rowTargets = List(rows) { row ->
        values[row].mapIndexed { column, value -> if (solution[row][column]) value else 0 }.sum()
    }
    val columnTargets = List(columns) { column ->
        values.indices.sumOf { row -> if (solution[row][column]) values[row][column] else 0 }
    }
    var board = BoardMatrix.empty(columns = columns, rows = rows)
    repeat(rows) { row ->
        repeat(columns) { column ->
            board = board.fill(
                points = listOf(GridPoint(column, row)),
                tone = sumShiftToneAt(column = column, row = row),
                value = values[row][column],
            )
        }
    }
    return SumShiftPuzzle(board = board, rowTargets = rowTargets, columnTargets = columnTargets)
}

private fun scoreSumShiftPuzzle(
    mask: List<List<Boolean>>,
    puzzle: SumShiftPuzzle,
): Int {
    val rowCounts = mask.map { row -> row.count { it } }
    val columnCounts = puzzle.columnTargets.indices.map { column -> mask.indices.count { row -> mask[row][column] } }
    val rowSpread = (puzzle.rowTargets.maxOrNull() ?: 0) - (puzzle.rowTargets.minOrNull() ?: 0)
    val columnSpread = (puzzle.columnTargets.maxOrNull() ?: 0) - (puzzle.columnTargets.minOrNull() ?: 0)
    return (puzzle.rowTargets.distinct().size * 14) +
        (puzzle.columnTargets.distinct().size * 16) +
        (rowCounts.distinct().size * 9) +
        (columnCounts.distinct().size * 11) +
        rowSpread +
        columnSpread +
        (selectedCount(mask) * 2)
}

private fun fallbackSumShiftPuzzle(
    rows: Int,
    columns: Int,
    random: Random,
): SumShiftPuzzle {
    val values = List(rows) { List(columns) { random.nextInt(1, 10) } }
    val solution = List(rows) { row ->
        List(columns) { column ->
            column == (row + random.nextInt(columns)) % columns || random.nextFloat() < 0.22f
        }
    }
    return buildSumShiftPuzzle(values = values, solution = solution)
}

private fun selectedCount(mask: List<List<Boolean>>): Int = mask.sumOf { row -> row.count { it } }

private fun countSelectedNeighbors(
    mask: List<List<Boolean>>,
    row: Int,
    column: Int,
): Int {
    var count = 0
    for (rowOffset in -1..1) {
        for (columnOffset in -1..1) {
            if (rowOffset == 0 && columnOffset == 0) continue
            val nextRow = row + rowOffset
            val nextColumn = column + columnOffset
            if (nextRow !in mask.indices || nextColumn !in mask.first().indices) continue
            if (mask[nextRow][nextColumn]) count += 1
        }
    }
    return count
}

private fun sumShiftToneAt(column: Int, row: Int): CellTone = listOf(
    CellTone.Cyan,
    CellTone.Violet,
    CellTone.Emerald,
    CellTone.Gold,
    CellTone.Blue,
).let { tones ->
    tones[(column + (row * 2)) % tones.size]
}

