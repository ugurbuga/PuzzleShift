package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardCell
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.ComboState
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.FeedbackEmphasis
import com.ugurbuga.blockgames.game.model.FloatingFeedback
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.settings.GameSessionCodec
import kotlin.random.Random

internal class ChainShiftGameLogic(
    private val random: Random = Random.Default,
    private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val MinimumMatchSize = 3
        private const val StartingChainLength = 6
        private const val ReviveTrimCount = 7
        private val ChainShiftTones = listOf(
            CellTone.Cyan,
            CellTone.Gold,
            CellTone.Violet,
            CellTone.Emerald,
            CellTone.Coral,
        )
    }

    override fun restoreGame(state: GameState): GameState {
        val maxBoardCellId = ChainShiftPath.spiral(state.config)
            .mapNotNull { point -> state.board.cellAt(point.column, point.row)?.value }
            .maxOrNull()
            ?.toLong()
            ?: 0L
        return state.copy(
            nextPieceId = maxOf(
                state.nextPieceId,
                GameSessionCodec.maxPieceId(state) + 1L,
                maxBoardCellId + 1L,
            ),
        )
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val path = ChainShiftPath.spiral(config)
        var nextPieceId = 1L
        val startingCells = buildInitialChain(
            maxLength = path.size.coerceAtLeast(StartingChainLength),
            desiredLength = StartingChainLength,
            nextStableId = { stableCellValue(nextPieceId++) },
        )
        val activePiece = singlePiece(nextPieceId++, tone = randomTone())
        val nextQueue = listOf(
            singlePiece(nextPieceId++, tone = randomTone()),
            singlePiece(nextPieceId++, tone = randomTone()),
        )
        return GameState(
            config = config,
            gameMode = mode,
            gameplayStyle = GameplayStyle.ChainShift,
            board = boardFromOccupied(
                config = config,
                path = path,
                occupied = startingCells.withIndex().associate { it.index to it.value },
            ),
            activePiece = activePiece,
            nextQueue = nextQueue,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            status = GameStatus.Running,
            lastActionTime = currentEpochMillis(),
            remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
            nextPieceId = nextPieceId,
            activeChallenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) }),
            message = gameText(GameTextKey.GameMessageSelectColumn),
        )
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null

    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? {
        if (state.status != GameStatus.Running) return null
        if (state.activePiece?.id != pieceId) return null
        if (state.onboardingGuidePoint != null && origin != state.onboardingGuidePoint) return null

        val path = ChainShiftPath.spiral(state.config)
        val lookup = ChainShiftPath.indexLookup(state.config)
        val targetIndex = lookup[origin] ?: return null

        // Only allow targeting occupied cells
        if (state.board.cellAt(origin.column, origin.row) == null) return null

        val landingIndex = targetIndex
        return PlacementPreview(
            selectedColumn = origin.column,
            entryAnchor = origin,
            landingAnchor = path[landingIndex],
            occupiedCells = listOf(path[landingIndex]),
            coveredColumns = path[landingIndex].column..path[landingIndex].column,
        )
    }

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> =
        preview?.occupiedCells?.toSet() ?: emptySet()

    override fun placePiece(state: GameState, column: Int): GameMoveResult = invalidMove(state)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)
        val activePiece = state.activePiece ?: return invalidMove(state)
        if (activePiece.id != pieceId) return invalidMove(state)
        val preview = previewPlacement(state, pieceId, origin) ?: return invalidMove(state)

        val path = ChainShiftPath.spiral(state.config)
        val occupied = extractOccupied(state.board, path).toMutableMap()
        val insertionIndex = path.indexOf(preview.landingAnchor)
        if (insertionIndex !in path.indices) return invalidMove(state)
        val shifted = if (occupied.containsKey(insertionIndex)) {
            shiftOccupiedTowardCenter(occupied, fromIndex = insertionIndex, maxIndex = path.lastIndex) ?: return gameOverMove(state)
        } else {
            occupied
        }

        shifted[insertionIndex] = BoardCell(
            tone = activePiece.tone,
            special = SpecialBlockType.None,
            value = stableCellValue(activePiece.id),
        )
        val resolution = resolveMatches(shifted, path)
        val nextQueueState = advanceQueue(state.nextQueue, state.nextPieceId)
        val board = boardFromOccupied(state.config, path, resolution.occupied)
        val scoreGain = scoreForMove(
            state = state,
            chainSize = resolution.occupied.size,
            tilesPlaced = 1,
            clearedBlocks = resolution.totalCleared,
            groupCount = resolution.groupCount,
            chainDepth = resolution.chainDepth,
        )
        val nextScore = state.score + scoreGain
        val awardedTimeMillis = awardedTimeMillis(
            state = state,
            clearedBlocks = resolution.totalCleared,
            previousScore = state.score,
            nextScore = nextScore,
        )
        val nextCombo = if (resolution.totalCleared > 0) {
            ComboState(
                chain = state.combo.chain + 1,
                best = maxOf(state.combo.best, state.combo.chain + 1),
            )
        } else {
            state.combo.copy(chain = 0)
        }
        val updatedChallenge = updateChallengeProgress(
            challenge = state.activeChallenge,
            scoreGain = scoreGain,
            clearedBlocks = resolution.totalCleared,
            chainDepth = resolution.chainDepth,
            placedPieces = 1,
        )
        val actionTime = currentEpochMillis()
        val nextState = state.copy(
            board = board,
            activePiece = nextQueueState.activePiece,
            nextQueue = nextQueueState.nextQueue,
            nextPieceId = nextQueueState.nextPieceId,
            score = nextScore,
            lastMoveScore = scoreGain,
            linesCleared = state.linesCleared + resolution.totalCleared,
            combo = nextCombo,
            lastResolvedLines = resolution.totalCleared,
            lastChainDepth = resolution.chainDepth,
            recentlyExplodedPoints = resolution.clearedPoints,
            recentlyExplodedTones = resolution.clearedTones,
            clearAnimationToken = if (resolution.totalCleared > 0) state.clearAnimationToken + 1 else state.clearAnimationToken,
            impactFlashToken = state.impactFlashToken + 1,
            feedbackToken = state.feedbackToken + 1,
            floatingFeedback = if (scoreGain > 0) {
                FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackClear, scoreGain),
                    emphasis = if (resolution.chainDepth > 1) FeedbackEmphasis.Bonus else FeedbackEmphasis.Info,
                    token = state.feedbackToken + 1,
                )
            } else null,
            lastActionTime = actionTime,
            activeChallenge = updatedChallenge,
            remainingTimeMillis = state.remainingTimeMillis?.plus(awardedTimeMillis),
            status = if (path.lastIndex in resolution.occupied.keys) GameStatus.GameOver else GameStatus.Running,
        )
        val events = buildSet {
            add(GameEvent.PlacementAccepted)
            if (resolution.totalCleared > 0) add(GameEvent.LineClear)
            if (resolution.chainDepth > 1) add(GameEvent.ChainReaction)
            if (updatedChallenge != null && updatedChallenge.isCompleted && state.activeChallenge?.isCompleted == false) {
                add(GameEvent.ChallengeCompleted)
            }
            if (nextState.status == GameStatus.GameOver) add(GameEvent.GameOver)
        }
        return GameMoveResult(
            state = nextState,
            events = events,
        )
    }

    override fun holdPiece(state: GameState): GameMoveResult = GameMoveResult(state = state)

    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult {
        if (state.status != GameStatus.Running) return GameMoveResult(state = state)
        val replacement = singlePiece(id = state.nextPieceId, tone = randomTone())
        return GameMoveResult(
            state = state.copy(
                activePiece = replacement,
                nextPieceId = state.nextPieceId + 1,
                feedbackToken = state.feedbackToken + 1,
                lastActionTime = currentEpochMillis(),
            ),
            events = setOf(GameEvent.SpecialTriggered),
        )
    }

    override fun commitSoftLock(state: GameState): GameMoveResult = GameMoveResult(state = state)

    override fun reviveFromReward(state: GameState): GameMoveResult {
        if (state.rewardedReviveUsed) return GameMoveResult(state = state)
        val path = ChainShiftPath.spiral(state.config)
        val occupied = extractOccupied(state.board, path)
        if (occupied.isEmpty()) return GameMoveResult(state = state)
        val removeCount = minOf(ReviveTrimCount, occupied.size.coerceAtLeast(1) / 2 + 1)
        val trimmed = occupied.toMutableMap().apply {
            keys.sortedDescending().take(removeCount).forEach(::remove)
        }
        return GameMoveResult(
            state = state.copy(
                board = boardFromOccupied(state.config, path, trimmed),
                status = GameStatus.Running,
                rewardedReviveUsed = true,
                remainingTimeMillis = state.remainingTimeMillis?.plus(GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS),
                feedbackToken = state.feedbackToken + 1,
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackExtraLife),
                    emphasis = FeedbackEmphasis.Bonus,
                    token = state.feedbackToken + 1,
                ),
            ),
            events = setOf(GameEvent.Revived),
        )
    }

    override fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running) return state
        val nextRemainingTimeMillis = state.remainingTimeMillis?.minus(1_000L)?.coerceAtLeast(0L)
        if (state.remainingTimeMillis != null && nextRemainingTimeMillis == 0L) {
            return state.copy(status = GameStatus.GameOver, remainingTimeMillis = 0L)
        }

        val path = ChainShiftPath.spiral(state.config)
        val currentOccupied = extractOccupied(state.board, path)
        if (path.lastIndex in currentOccupied.keys) {
            return state.copy(status = GameStatus.GameOver, remainingTimeMillis = nextRemainingTimeMillis)
        }

        val advancedOccupied = shiftOccupiedTowardCenter(currentOccupied, fromIndex = 0, maxIndex = path.lastIndex)
            ?: return state.copy(status = GameStatus.GameOver, remainingTimeMillis = nextRemainingTimeMillis)
        advancedOccupied[0] = randomBoardCell(stableCellValue(state.nextPieceId))
        val resolution = resolveMatches(advancedOccupied, path)
        val board = boardFromOccupied(state.config, path, resolution.occupied)
        val levelUp = state.secondsUntilDifficultyIncrease <= 1
        val nextStatus = if (path.lastIndex in resolution.occupied.keys) GameStatus.GameOver else GameStatus.Running
        val nextState = state.copy(
            board = board,
            status = nextStatus,
            linesCleared = state.linesCleared + resolution.totalCleared,
            lastResolvedLines = resolution.totalCleared,
            lastChainDepth = resolution.chainDepth,
            recentlyExplodedPoints = resolution.clearedPoints,
            recentlyExplodedTones = resolution.clearedTones,
            clearAnimationToken = if (resolution.totalCleared > 0) state.clearAnimationToken + 1 else state.clearAnimationToken,
            lastActionTime = currentEpochMillis(),
            remainingTimeMillis = nextRemainingTimeMillis,
            nextPieceId = state.nextPieceId + 1,
            level = if (levelUp) state.level + 1 else state.level,
            difficultyStage = if (levelUp) state.difficultyStage + 1 else state.difficultyStage,
            secondsUntilDifficultyIncrease = if (levelUp) state.config.difficultyIntervalSeconds else state.secondsUntilDifficultyIncrease - 1,
        )
        return nextState
    }

    private fun awardedTimeMillis(
        state: GameState,
        clearedBlocks: Int,
        previousScore: Int,
        nextScore: Int,
    ): Long {
        if (state.gameMode != GameMode.TimeAttack || clearedBlocks <= 0) return 0L
        val blocksBonus = clearedBlocks * GameLogic.TIME_ATTACK_BONUS_PER_CLEARED_BLOCK_MILLIS
        val scoreBonus = (
            nextScore / GameLogic.TIME_ATTACK_SCORE_BONUS_THRESHOLD -
                previousScore / GameLogic.TIME_ATTACK_SCORE_BONUS_THRESHOLD
            ) * GameLogic.TIME_ATTACK_SCORE_BONUS_MILLIS
        return blocksBonus + scoreBonus
    }

    private fun scoreForMove(
        state: GameState,
        chainSize: Int,
        tilesPlaced: Int,
        clearedBlocks: Int,
        groupCount: Int,
        chainDepth: Int,
    ): Int {
        if (clearedBlocks <= 0 && tilesPlaced <= 0) return 0
        return scoreCalculator.calculateScore(
            ScoreCalculator.ScoreParams(
                tilesPlaced = tilesPlaced,
                linesCleared = groupCount,
                currentStreak = state.combo.chain,
                areaTilesCleared = clearedBlocks,
                boardFillRatio = chainSize.toFloat() / ChainShiftPath.spiral(state.config).size.toFloat(),
                chainReactionCount = (chainDepth - 1).coerceAtLeast(0),
            ),
        )
    }

    private fun updateChallengeProgress(
        challenge: DailyChallenge?,
        scoreGain: Int,
        clearedBlocks: Int,
        chainDepth: Int,
        placedPieces: Int,
    ): DailyChallenge? {
        if (challenge == null) return null
        return challenge.copy(
            tasks = challenge.tasks.map { task ->
                val gain = when (task.type) {
                    ChallengeTaskType.ClearBlocks -> clearedBlocks
                    ChallengeTaskType.ReachScore -> scoreGain
                    ChallengeTaskType.PlacePieces -> placedPieces
                    ChallengeTaskType.ChainReaction -> if (chainDepth > 1) 1 else 0
                    else -> 0
                }
                task.copy(current = task.current + gain)
            },
        )
    }

    private fun extractOccupied(board: BoardMatrix, path: List<GridPoint>): Map<Int, BoardCell> = buildMap {
        path.forEachIndexed { index, point ->
            board.cellAt(point.column, point.row)?.let { cell -> put(index, cell) }
        }
    }

    private fun boardFromOccupied(
        config: GameConfig,
        path: List<GridPoint>,
        occupied: Map<Int, BoardCell>,
    ): BoardMatrix {
        var board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
        occupied.toSortedMap().forEach { (index, cell) ->
            val point = path.getOrNull(index) ?: return@forEach
            board = board.fill(
                points = listOf(point),
                tone = cell.tone,
                special = cell.special,
                value = cell.value,
            )
        }
        return board
    }

    private fun shiftOccupiedTowardCenter(
        occupied: Map<Int, BoardCell>,
        fromIndex: Int,
        maxIndex: Int,
    ): MutableMap<Int, BoardCell>? {
        if (occupied.keys.any { it >= fromIndex && it >= maxIndex }) return null
        val shifted = occupied.toMutableMap()
        occupied.keys.sortedDescending().forEach { index ->
            if (index < fromIndex) return@forEach
            val cell = shifted.remove(index) ?: return@forEach
            val nextIndex = index + 1
            if (nextIndex > maxIndex) return null
            shifted[nextIndex] = cell
        }
        return shifted
    }

    private fun buildInitialChain(
        maxLength: Int,
        desiredLength: Int,
        nextStableId: () -> Int,
    ): List<BoardCell> {
        val targetLength = desiredLength.coerceAtMost(maxLength)
        val tones = mutableListOf<CellTone>()
        repeat(targetLength) {
            tones += nextTone(tones)
        }
        return tones.map { tone -> boardCell(tone = tone, value = nextStableId()) }
    }

    private fun resolveMatches(
        input: Map<Int, BoardCell>,
        path: List<GridPoint>,
    ): ChainResolution {
        val working = input.toMutableMap()
        val clearedPoints = linkedSetOf<GridPoint>()
        val clearedTones = linkedMapOf<GridPoint, CellTone>()
        var totalCleared = 0
        var chainDepth = 0
        var groupCount = 0

        while (true) {
            val sortedIndices = working.keys.sorted()
            val removalIndices = mutableSetOf<Int>()
            var cursor = 0
            while (cursor < sortedIndices.size) {
                val startIndex = sortedIndices[cursor]
                val tone = working.getValue(startIndex).tone
                val run = mutableListOf(startIndex)
                var nextCursor = cursor + 1
                var previousIndex = startIndex
                while (nextCursor < sortedIndices.size) {
                    val candidateIndex = sortedIndices[nextCursor]
                    val candidateCell = working.getValue(candidateIndex)
                    if (candidateIndex != previousIndex + 1 || candidateCell.tone != tone) break
                    run += candidateIndex
                    previousIndex = candidateIndex
                    nextCursor += 1
                }
                if (run.size >= MinimumMatchSize) {
                    removalIndices += run
                    groupCount += 1
                }
                cursor = nextCursor
            }
            if (removalIndices.isEmpty()) break

            chainDepth += 1
            removalIndices.sortedDescending().forEach { removeIndex ->
                val point = path[removeIndex]
                val removed = working.remove(removeIndex) ?: return@forEach
                clearedPoints += point
                clearedTones[point] = removed.tone
                totalCleared += 1
            }

            val remainingCells: List<BoardCell> = working.toSortedMap().values.toList()
            working.clear()
            remainingCells.forEachIndexed { index: Int, boardCell: BoardCell ->
                working[index] = boardCell
            }
        }

        return ChainResolution(
            occupied = working.toSortedMap(),
            totalCleared = totalCleared,
            chainDepth = chainDepth,
            groupCount = groupCount,
            clearedPoints = clearedPoints,
            clearedTones = clearedTones,
        )
    }

    private fun advanceQueue(nextQueue: List<Piece>, nextPieceId: Long): QueueAdvance {
        var nextId = nextPieceId
        val activePiece = nextQueue.firstOrNull() ?: singlePiece(nextId++, randomTone())
        val remaining = nextQueue.drop(1).toMutableList()
        while (remaining.size < 2) {
            remaining += singlePiece(nextId++, randomTone())
        }
        return QueueAdvance(
            activePiece = activePiece,
            nextQueue = remaining.take(2),
            nextPieceId = nextId,
        )
    }

    private fun singlePiece(id: Long, tone: CellTone): Piece = Piece(
        id = id,
        kind = PieceKind.Single,
        tone = tone,
        cells = listOf(GridPoint(0, 0)),
        width = 1,
        height = 1,
    )

    private fun boardCell(
        tone: CellTone,
        value: Int,
    ): BoardCell = BoardCell(
        tone = tone,
        special = SpecialBlockType.None,
        value = value,
    )

    private fun randomBoardCell(value: Int): BoardCell = boardCell(randomTone(), value = value)

    private fun stableCellValue(sourceId: Long): Int = sourceId.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()

    private fun randomTone(): CellTone = ChainShiftTones[random.nextInt(ChainShiftTones.size)]

    private fun nextTone(previous: List<CellTone>): CellTone {
        val lastTwo = previous.takeLast(2)
        val allowed = if (lastTwo.size == 2 && lastTwo[0] == lastTwo[1]) {
            ChainShiftTones.filterNot { it == lastTwo[0] }
        } else {
            ChainShiftTones
        }
        return allowed[random.nextInt(allowed.size)]
    }

    private fun invalidMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state,
        events = setOf(GameEvent.InvalidDrop),
    )

    private fun gameOverMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state.copy(status = GameStatus.GameOver),
        events = setOf(GameEvent.GameOver),
    )

    private data class QueueAdvance(
        val activePiece: Piece,
        val nextQueue: List<Piece>,
        val nextPieceId: Long,
    )

    private data class ChainResolution(
        val occupied: Map<Int, BoardCell>,
        val totalCleared: Int,
        val chainDepth: Int,
        val groupCount: Int,
        val clearedPoints: Set<GridPoint>,
        val clearedTones: Map<GridPoint, CellTone>,
    )
}

