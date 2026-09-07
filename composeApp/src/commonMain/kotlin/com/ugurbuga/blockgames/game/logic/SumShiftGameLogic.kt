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
import kotlin.math.abs
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
            (state.gameplayStyle != GameplayStyle.SumShift) ||
            (state.board.columns != config.columns) ||
            (state.board.rows != config.rows) ||
            (state.sumShiftRowTargets.size != config.rows) ||
            (state.sumShiftColumnTargets.size != config.columns)
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

        if (!isSolved(updatedState)) {
            return GameMoveResult(
                state = updatedState,
                events = setOf(GameEvent.PlacementAccepted),
            )
        }

        return finalizeSolvedSumShiftState(updatedState)
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

    private fun isSolved(state: GameState): Boolean = isSolvedSumShiftState(state)

    private fun invalidMove(state: GameState): GameMoveResult =
        GameMoveResult(state = state, events = setOf(GameEvent.InvalidDrop))
}

internal fun isSolvedSumShiftState(state: GameState): Boolean {
    val rowsSolved = state.sumShiftRowTargets.indices.all { index ->
        selectedSumShiftRow(state, state.sumShiftSelectedCells, index) == state.sumShiftRowTargets[index]
    }
    val columnsSolved = state.sumShiftColumnTargets.indices.all { index ->
        selectedSumShiftColumn(state, state.sumShiftSelectedCells, index) == state.sumShiftColumnTargets[index]
    }
    return rowsSolved && columnsSolved
}

internal fun finalizeSolvedSumShiftState(state: GameState): GameMoveResult {
    val scoreGain = 180 + (state.sumShiftSelectedCells.size * 24) + (state.config.rows * 18)
    val nextScore = state.score + scoreGain
    val nextSolvedBoards = state.linesCleared + 1
    val nextLevel = 1 + (nextSolvedBoards / 3)
    val updatedChallenge = updateSumShiftChallenge(
        challenge = state.activeChallenge,
        score = nextScore,
    )
    val challengeCompleted = state.activeChallenge?.isCompleted != true && updatedChallenge?.isCompleted == true
    val awardedTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
        2_500L + (state.sumShiftSelectedCells.size * 120L)
    } else {
        0L
    }

    return GameMoveResult(
        state = state.copy(
            score = nextScore,
            lastMoveScore = scoreGain,
            linesCleared = nextSolvedBoards,
            level = nextLevel,
            difficultyStage = nextLevel - 1,
            sumShiftManualDisabledCells = emptySet(),
            sumShiftPreparingBoard = false,
            feedbackToken = state.feedbackToken + 1,
            activeChallenge = updatedChallenge,
            remainingTimeMillis = state.remainingTimeMillis?.plus(awardedTimeMillis),
        ),
        events = buildSet {
            add(GameEvent.PlacementAccepted)
            add(GameEvent.LineClear)
            if (challengeCompleted) add(GameEvent.ChallengeCompleted)
        },
    )
}

private fun updateSumShiftChallenge(
    challenge: DailyChallenge?,
    score: Int,
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

private val SumShiftSupportedConfigs = listOf(
    GameConfig(columns = 5, rows = 6, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
    GameConfig(columns = 5, rows = 7, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
    GameConfig(columns = 5, rows = 8, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
    GameConfig(columns = 6, rows = 7, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
    GameConfig(columns = 6, rows = 8, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
    GameConfig(columns = 7, rows = 9, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
)

internal fun sumShiftSupportedConfigs(): List<GameConfig> = SumShiftSupportedConfigs

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
        area >= 96 -> 28
        area >= 84 -> 36
        area >= 72 -> 44
        area >= 56 -> 52
        area >= 42 -> 60
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
        val score = scoreSumShiftPuzzle(mask = mask, rows = rows, columns = columns, puzzle = puzzle)
        if (score > bestScore) {
            bestScore = score
            bestPuzzle = puzzle
        }
    }

    return bestPuzzle ?: fallbackSumShiftPuzzle(rows = rows, columns = columns, random = random)
}

internal fun isValidSumShiftConfig(config: GameConfig): Boolean {
    return SumShiftSupportedConfigs.any { supported ->
        supported.columns == config.columns && supported.rows == config.rows
    }
}

internal fun normalizeSumShiftConfig(config: GameConfig): GameConfig {
    val nearest = SumShiftSupportedConfigs.minWithOrNull(
        compareBy(
            { abs(it.columns - config.columns) },
            { abs(it.rows - config.rows) },
            { abs((it.columns * it.rows) - (config.columns * config.rows)) },
        )
    ) ?: SumShiftSupportedConfigs.first()
    return nearest.copy(
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )
}

internal fun randomSumShiftConfig(random: Random = Random.Default): GameConfig {
    return SumShiftSupportedConfigs.random(random)
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
                    .asSequence()
                    .map { columnIndex -> GridPoint(columnIndex, rowIndex) }
                    .filter { it !in disabledCells }
                    .toList()
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
): Array<IntArray> {
    val rowBiases = IntArray(rows) { random.nextInt(0, 3) }
    val columnBiases = IntArray(columns) { random.nextInt(0, 3) }
    return Array(rows) { row ->
        IntArray(columns) { column ->
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
): BooleanArray {
    val targetSelectedCount = ((rows * columns) * targetDensity).toInt().coerceIn(rows + 1, (rows * columns) - columns)
    val mask = BooleanArray(rows * columns)

    when (style) {
        SumShiftPatternStyle.RandomWalk -> fillRandomWalkMask(mask, rows, columns, targetSelectedCount, random)
        SumShiftPatternStyle.Bands -> fillBandMask(mask, rows, columns, targetSelectedCount, random)
        SumShiftPatternStyle.Clusters -> fillClusterMask(mask, rows, columns, random)
        SumShiftPatternStyle.Waves -> fillWaveMask(mask, rows, columns, random)
        SumShiftPatternStyle.Scatter -> fillScatterMask(mask, rows, columns, targetSelectedCount, random)
    }

    repairMaskCoverage(mask, rows, columns, random)
    repairMaskHoles(mask, rows, columns, random)
    trimMaskToTarget(mask, rows, columns, targetSelectedCount, random)
    repairMaskCoverage(mask, rows, columns, random)
    repairMaskHoles(mask, rows, columns, random)
    return mask
}

private fun fillRandomWalkMask(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    targetSelectedCount: Int,
    random: Random,
) {
    var row = random.nextInt(rows)
    var column = random.nextInt(columns)
    var currentCount = 0
    repeat(targetSelectedCount * 5) {
        val idx = row * columns + column
        if (!mask[idx]) {
            mask[idx] = true
            currentCount++
        }
        if (currentCount >= targetSelectedCount) return
        
        when (random.nextInt(4)) {
            0 -> if (row > 0) row--
            1 -> if (row < rows - 1) row++
            2 -> if (column > 0) column--
            3 -> if (column < columns - 1) column++
        }
        if (random.nextFloat() < 0.12f) {
            row = random.nextInt(rows)
            column = random.nextInt(columns)
        }
    }
}

private fun fillBandMask(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    targetSelectedCount: Int,
    random: Random,
) {
    repeat(rows) { row ->
        val segmentCount = if (random.nextFloat() < 0.34f) 2 else 1
        repeat(segmentCount) {
            val start = random.nextInt(columns)
            val length = random.nextInt(1, (columns / 2).coerceAtLeast(2) + 1)
            for (column in start until (start + length).coerceAtMost(columns)) {
                mask[row * columns + column] = true
            }
        }
    }
    if (mask.count { it } < targetSelectedCount) {
        repeat(columns) { column ->
            if (random.nextBoolean()) {
                val start = random.nextInt(rows)
                val length = random.nextInt(1, (rows / 3).coerceAtLeast(2) + 1)
                for (row in start until (start + length).coerceAtMost(rows)) {
                    mask[row * columns + column] = true
                }
            }
        }
    }
}

private fun fillClusterMask(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    random: Random,
) {
    val centersCount = random.nextInt(2, 5)
    val centersR = IntArray(centersCount) { random.nextInt(rows) }
    val centersC = IntArray(centersCount) { random.nextInt(columns) }

    for (row in 0 until rows) {
        for (column in 0 until columns) {
            var minDistance = Int.MAX_VALUE
            for (i in 0 until centersCount) {
                val d = abs(centersR[i] - row) + abs(centersC[i] - column)
                if (d < minDistance) minDistance = d
            }
            val chance = when (minDistance) {
                0 -> 0.92f
                1 -> 0.68f
                2 -> 0.38f
                else -> 0.16f
            }
            if (random.nextFloat() < chance) {
                mask[row * columns + column] = true
            }
        }
    }
}

private fun fillWaveMask(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    random: Random,
) {
    val period = random.nextInt(2, 5)
    val rowWeight = random.nextInt(1, 4)
    val columnWeight = random.nextInt(1, 4)
    val offset = random.nextInt(period)
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val isWaveCell = ((row * rowWeight) + (column * columnWeight) + offset) % period == 0
            if (isWaveCell || random.nextFloat() < 0.12f) {
                mask[row * columns + column] = true
            }
        }
    }
}

private fun fillScatterMask(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    targetSelectedCount: Int,
    random: Random,
) {
    val density = targetSelectedCount.toFloat() / (rows * columns).toFloat()
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val neighbors = countSelectedNeighbors(mask, rows, columns, row, column)
            val neighborBonus = neighbors * 0.08f
            if (random.nextFloat() < density + neighborBonus) {
                mask[row * columns + column] = true
            }
        }
    }
}

private fun repairMaskCoverage(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    random: Random,
) {
    repeat(rows) { row ->
        var hasAny = false
        for (col in 0 until columns) if (mask[row * columns + col]) { hasAny = true; break }
        if (!hasAny) mask[row * columns + random.nextInt(columns)] = true
    }
    repeat(columns) { column ->
        var hasAny = false
        for (row in 0 until rows) if (mask[row * columns + column]) { hasAny = true; break }
        if (!hasAny) mask[random.nextInt(rows) * columns + column] = true
    }
}

private fun repairMaskHoles(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    random: Random,
) {
    repeat(rows) { row ->
        var allSelected = true
        for (col in 0 until columns) if (!mask[row * columns + col]) { allSelected = false; break }
        if (allSelected) {
            val candidates = mutableListOf<Int>()
            for (col in 0 until columns) {
                var colCount = 0
                for (r in 0 until rows) if (mask[r * columns + col]) colCount++
                if (colCount > 1) candidates.add(col)
            }
            val colToClear = if (candidates.isNotEmpty()) candidates.random(random) else random.nextInt(columns)
            mask[row * columns + colToClear] = false
        }
    }
    repeat(columns) { column ->
        var allSelected = true
        for (row in 0 until rows) if (!mask[row * columns + column]) { allSelected = false; break }
        if (allSelected) {
            val candidates = mutableListOf<Int>()
            for (row in 0 until rows) {
                var rowCount = 0
                for (c in 0 until columns) if (mask[row * columns + c]) rowCount++
                if (rowCount > 1) candidates.add(row)
            }
            val rowToClear = if (candidates.isNotEmpty()) candidates.random(random) else random.nextInt(rows)
            mask[rowToClear * columns + column] = false
        }
    }
}

private fun trimMaskToTarget(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    targetSelectedCount: Int,
    random: Random,
) {
    var currentCount = mask.count { it }
    val points = mutableListOf<Int>()
    for (i in mask.indices) if (mask[i]) points.add(i)
    points.shuffle(random)

    for (idx in points) {
        if (currentCount <= targetSelectedCount) break
        val r = idx / columns
        val c = idx % columns
        
        var rowCount = 0
        for (col in 0 until columns) if (mask[r * columns + col]) rowCount++
        if (rowCount <= 1) continue
        
        var colCount = 0
        for (row in 0 until rows) if (mask[row * columns + c]) colCount++
        if (colCount <= 1) continue
        
        mask[idx] = false
        currentCount--
    }
}

private fun isUsefulSumShiftMask(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
): Boolean {
    repeat(rows) { r ->
        var hasAny = false
        var all = true
        for (c in 0 until columns) {
            if (mask[r * columns + c]) hasAny = true else all = false
        }
        if (!hasAny || all) return false
    }
    repeat(columns) { c ->
        var hasAny = false
        var all = true
        for (r in 0 until rows) {
            if (mask[r * columns + c]) hasAny = true else all = false
        }
        if (!hasAny || all) return false
    }
    return true
}

private fun buildSumShiftPuzzle(
    values: Array<IntArray>,
    solution: BooleanArray,
): SumShiftPuzzle {
    val rows = values.size
    val columns = values[0].size
    val rowTargets = IntArray(rows)
    val columnTargets = IntArray(columns)
    
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            if (solution[r * columns + c]) {
                val v = values[r][c]
                rowTargets[r] += v
                columnTargets[c] += v
            }
        }
    }
    
    var board = BoardMatrix.empty(columns = columns, rows = rows)
    repeat(rows) { r ->
        repeat(columns) { c ->
            board = board.fill(
                points = listOf(GridPoint(c, r)),
                tone = sumShiftToneAt(column = c, row = r),
                value = values[r][c],
            )
        }
    }
    return SumShiftPuzzle(board = board, rowTargets = rowTargets.toList(), columnTargets = columnTargets.toList())
}

private fun scoreSumShiftPuzzle(
    mask: BooleanArray,
    rows: Int,
    columns: Int,
    puzzle: SumShiftPuzzle,
): Int {
    val rowCounts = IntArray(rows)
    val colCounts = IntArray(columns)
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            if (mask[r * columns + c]) {
                rowCounts[r]++
                colCounts[c]++
            }
        }
    }
    
    val rowSpread = (puzzle.rowTargets.maxOrNull() ?: 0) - (puzzle.rowTargets.minOrNull() ?: 0)
    val columnSpread = (puzzle.columnTargets.maxOrNull() ?: 0) - (puzzle.columnTargets.minOrNull() ?: 0)
    
    return (puzzle.rowTargets.distinct().size * 14) +
        (puzzle.columnTargets.distinct().size * 16) +
        (rowCounts.distinct().size * 9) +
        (colCounts.distinct().size * 11) +
        rowSpread +
        columnSpread +
        (mask.count { it } * 2)
}

private fun countSelectedNeighbors(mask: BooleanArray, rows: Int, columns: Int, row: Int, col: Int): Int {
    var count = 0
    if (row > 0 && mask[(row - 1) * columns + col]) count++
    if (row < rows - 1 && mask[(row + 1) * columns + col]) count++
    if (col > 0 && mask[row * columns + (col - 1)]) count++
    if (col < columns - 1 && mask[row * columns + (col + 1)]) count++
    return count
}

private fun fallbackSumShiftPuzzle(
    rows: Int,
    columns: Int,
    random: Random,
): SumShiftPuzzle {
    val values = Array(rows) { IntArray(columns) { random.nextInt(1, 10) } }
    val solution = BooleanArray(rows * columns)
    repeat(rows) { r ->
        solution[r * columns + random.nextInt(columns)] = true
    }
    return buildSumShiftPuzzle(values, solution)
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

