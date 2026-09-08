package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.FluffyBlockCollector
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import kotlin.random.Random

internal class FluffyBlockGameLogic(
    private val random: Random = Random.Default,
    @Suppress("UNUSED_PARAMETER") private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val MIN_COLLECTORS = 8
        private const val MAX_COLLECTORS = 12
        private const val MIN_CAPACITY = 20
        private const val MAX_CAPACITY = 40
    }

    override fun restoreGame(state: GameState): GameState = state

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode
    ): GameState {
        val startConfig = config.copy(columns = 10, rows = 16)
        var board = BoardMatrix.empty(columns = startConfig.columns, rows = startConfig.rows)
        
        val availableKinds = listOf(PieceKind.Square, PieceKind.L, PieceKind.J, PieceKind.TriL, PieceKind.Plus)
        val tonePool = CellTone.entries.shuffled(random).take(4) 

        val collectors = mutableListOf<FluffyBlockCollector>()
        val occupied = mutableSetOf<GridPoint>()

        repeat(random.nextInt(MIN_COLLECTORS, MAX_COLLECTORS + 1)) { i ->
            val tone = tonePool[i % tonePool.size]
            val kind = availableKinds.random(random)
            val anchor = findSafeAnchor(startConfig, kind, occupied)
            val collector = FluffyBlockCollector(id = (i + 1).toLong(), tone = tone, kind = kind, anchor = anchor, remainingCapacity = random.nextInt(MIN_CAPACITY, MAX_CAPACITY))
            collectors.add(collector)
            occupied.addAll(collector.cells)
        }

        val allPoints = (0 until startConfig.columns).flatMap { c -> (0 until startConfig.rows).map { r -> GridPoint(c, r) } }.filter { it !in occupied }.toMutableList()
        collectors.forEach { collector ->
            val neighbors = collector.cells.flatMap { pt -> listOf(GridPoint(pt.column+1, pt.row), GridPoint(pt.column-1, pt.row), GridPoint(pt.column, pt.row+1), GridPoint(pt.column, pt.row-1)) }.filter { it.column in 0 until startConfig.columns && it.row in 0 until startConfig.rows && it in allPoints }
            neighbors.take(3).forEach { pt ->
                board = board.fill(listOf(pt), collector.tone)
                allPoints.remove(pt)
            }
        }
        allPoints.forEach { pt -> board = board.fill(listOf(pt), tonePool.random(random)) }

        return GameState(
            config = startConfig, gameMode = mode, gameplayStyle = GameplayStyle.FluffyBlock, board = board,
            activePiece = null, nextQueue = emptyList(), score = 0, linesCleared = 0, level = 1, status = GameStatus.Running,
            difficultyStage = 0, secondsUntilDifficultyIncrease = startConfig.difficultyIntervalSeconds,
            nextPieceId = (collectors.size + 1).toLong(), message = gameText(GameTextKey.GameMessageSelectColumn), fluffyBlockCollectors = collectors
        )
    }

    private fun findSafeAnchor(config: GameConfig, kind: PieceKind, occupied: Set<GridPoint>): GridPoint {
        var attempts = 0
        while (attempts < 100) {
            val anchor = GridPoint(random.nextInt(config.columns - 2), random.nextInt(config.rows - 2))
            val cells = kind.template.map { it + anchor }
            if (cells.all { it.column in 0 until config.columns && it.row in 0 until config.rows && it !in occupied }) return anchor
            attempts++
        }
        return GridPoint(0, 0)
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null
    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? = null
    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> = emptySet()
    override fun placePiece(state: GameState, column: Int): GameMoveResult = invalidMove(state)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        val collectorIndex = state.fluffyBlockCollectors.indexOfFirst { it.id == pieceId }
        if (collectorIndex == -1) return invalidMove(state)
        val collector = state.fluffyBlockCollectors[collectorIndex]
        if (collector.anchor == origin) return invalidMove(state)

        val dx = (origin.column - collector.anchor.column).coerceIn(-1, 1)
        val dy = (origin.row - collector.anchor.row).coerceIn(-1, 1)
        if (dx != 0 && dy != 0) return invalidMove(state)

        var currentAnchor = collector.anchor
        var finalBoard = state.board
        var totalEaten = 0
        var scoreGain = 0
        val otherCollectors = state.fluffyBlockCollectors.filter { it.id != pieceId }

        while (totalEaten < collector.remainingCapacity) {
            val nextAnchor = GridPoint(currentAnchor.column + dx, currentAnchor.row + dy)
            val nextCells = collector.kind.template.map { it + nextAnchor }
            val canMoveToNext = nextCells.all { pt ->
                val inBounds = pt.column in 0 until state.config.columns && pt.row in 0 until state.config.rows
                if (!inBounds) return@all false
                val cell = finalBoard.cellAt(pt.column, pt.row)
                if (cell != null && cell.tone != collector.tone) return@all false
                if (otherCollectors.any { other -> pt in other.cells }) return@all false
                true
            }
            if (!canMoveToNext) break
            currentAnchor = nextAnchor
            val eatenInStep = nextCells.filter { pt -> finalBoard.cellAt(pt.column, pt.row)?.tone == collector.tone }.toSet()
            if (eatenInStep.isNotEmpty()) {
                finalBoard = finalBoard.clearPoints(eatenInStep)
                totalEaten += eatenInStep.size
                scoreGain += eatenInStep.size * 10
                finalBoard = replenishFromOpposite(finalBoard, state.config, otherCollectors + collector.copy(anchor = currentAnchor))
            }
        }
        if (currentAnchor == collector.anchor) return invalidMove(state)

        val updatedCollectors = state.fluffyBlockCollectors.toMutableList()
        val newCapacity = collector.remainingCapacity - totalEaten
        if (newCapacity <= 0) { updatedCollectors.removeAt(collectorIndex) } 
        else { updatedCollectors[collectorIndex] = collector.copy(anchor = currentAnchor, remainingCapacity = newCapacity, collectedCount = collector.collectedCount + totalEaten) }

        while (updatedCollectors.size < MIN_COLLECTORS) {
            val newC = spawnNewCollector(updatedCollectors, state.config)
            if (newC != null) updatedCollectors.add(newC) else break
        }

        val nextState = state.copy(
            board = finalBoard, score = state.score + scoreGain, lastMoveScore = scoreGain,
            clearAnimationToken = state.clearAnimationToken + 1, fluffyBlockCollectors = updatedCollectors,
            status = if (updatedCollectors.isEmpty()) GameStatus.GameOver else state.status
        )
        return GameMoveResult(nextState, events = if (totalEaten > 0) mutableSetOf(GameEvent.PlacementAccepted, GameEvent.LineClear) else mutableSetOf(GameEvent.PlacementAccepted))
    }

    private fun replenishFromOpposite(board: BoardMatrix, config: GameConfig, collectors: List<FluffyBlockCollector>): BoardMatrix {
        var nextBoard = board
        val collCells = collectors.flatMap { it.cells }.toSet()
        val tones = CellTone.entries.shuffled(random).take(4)
        for (r in 0 until config.rows) {
            for (c in 0 until config.columns) {
                val pt = GridPoint(c, r)
                if (nextBoard.cellAt(c, r) == null && pt !in collCells) {
                    nextBoard = nextBoard.fill(listOf(pt), tones.random(random))
                }
            }
        }
        return nextBoard
    }

    private fun spawnNewCollector(collectors: List<FluffyBlockCollector>, config: GameConfig): FluffyBlockCollector? {
        val occ = collectors.flatMap { it.cells }.toSet()
        val tone = CellTone.entries.random(random)
        val availableKinds = listOf(PieceKind.Square, PieceKind.L, PieceKind.J, PieceKind.TriL, PieceKind.Plus)
        val edgeAnchors = mutableListOf<GridPoint>()
        for (c in 0 until config.columns - 2) { edgeAnchors.add(GridPoint(c, 0)); edgeAnchors.add(GridPoint(c, config.rows - 2)) }
        for (r in 0 until config.rows - 2) { edgeAnchors.add(GridPoint(0, r)); edgeAnchors.add(GridPoint(config.columns - 2, r)) }
        edgeAnchors.shuffled(random).forEach { anchor ->
            val kind = availableKinds.random(random)
            val cells = kind.template.map { it + anchor }
            if (cells.all { it.column in 0 until config.columns && it.row in 0 until config.rows && it !in occ }) {
                return FluffyBlockCollector(id = random.nextLong(), tone = tone, kind = kind, anchor = anchor, remainingCapacity = random.nextInt(MIN_CAPACITY, MAX_CAPACITY))
            }
        }
        return null
    }

    override fun holdPiece(state: GameState): GameMoveResult = GameMoveResult(state)
    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult = GameMoveResult(state)
    override fun commitSoftLock(state: GameState): GameMoveResult = GameMoveResult(state)
    override fun reviveFromReward(state: GameState): GameMoveResult = GameMoveResult(state)
    override fun tick(state: GameState): GameState = state
    private fun invalidMove(state: GameState) = GameMoveResult(state)
}
