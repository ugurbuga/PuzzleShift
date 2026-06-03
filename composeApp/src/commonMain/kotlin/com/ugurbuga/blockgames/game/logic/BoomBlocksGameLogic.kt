package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
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
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.settings.GameSessionCodec
import kotlin.random.Random

internal class BoomBlocksGameLogic(
    private val random: Random = Random.Default,
    private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val MIN_EXPLODE_SIZE = 3
        private const val REWARDED_REVIVE_GROUP_MIN_SIZE = 3
        private const val STARTING_BOARD_GENERATION_ATTEMPTS = 24
        private val BOOM_BLOCKS_TONES = listOf(
            CellTone.Cyan,
            CellTone.Gold,
            CellTone.Violet,
            CellTone.Emerald,
            CellTone.Coral,
        )
    }

    override fun restoreGame(state: GameState): GameState {
        return state.copy(nextPieceId = maxOf(state.nextPieceId, GameSessionCodec.maxPieceId(state) + 1L))
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val (board, nextId) = generatePlayableStartingBoard(config)

        return GameState(
            config = config,
            gameMode = mode,
            gameplayStyle = GameplayStyle.BoomBlocks,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            status = GameStatus.Running,
            lastActionTime = currentEpochMillis(),
            remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
            nextPieceId = nextId,
            activeChallenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) }),
        )
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null
    
    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? {
        if (state.status != GameStatus.Running) return null

        if (state.onboardingGuidePoint != null) {
            val targetGroup = findConnectedGroup(state.board, state.onboardingGuidePoint)
            if (origin !in targetGroup) return null
        }

        val group = findConnectedGroup(state.board, origin)
        if (group.size < MIN_EXPLODE_SIZE) return null
        
        return PlacementPreview(
            selectedColumn = origin.column,
            entryAnchor = origin,
            landingAnchor = origin,
            occupiedCells = group.toList(),
            coveredColumns = group.minOf { it.column }..group.maxOf { it.column }
        )
    }

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> {
        return preview?.occupiedCells?.toSet() ?: emptySet()
    }

    override fun placePiece(state: GameState, column: Int): GameMoveResult = invalidMove(state)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)
        
        if (state.onboardingGuidePoint != null) {
            val targetGroup = findConnectedGroup(state.board, state.onboardingGuidePoint)
            if (origin !in targetGroup) return invalidMove(state)
        }

        val group = findConnectedGroup(state.board, origin)
        if (group.size < MIN_EXPLODE_SIZE) return invalidMove(state)
        
        // 1. Clear the exploded group
        val boardAfterClear = state.board.clearPoints(group)
        
        // 2. Determine gravity direction based on explosion center
        // We want new blocks to come from the opposite side of the explosion
        val avgRow = group.map { it.row }.average().toFloat()
        val avgCol = group.map { it.column }.average().toFloat()
        
        val rows = state.board.rows.toFloat()
        val cols = state.board.columns.toFloat()
        
        // Normalize distances based on board dimensions to ensure fairness
        val relDistToTop = avgRow / rows
        val relDistToBottom = (rows - 1 - avgRow) / rows
        val relDistToLeft = avgCol / cols
        val relDistToRight = (cols - 1 - avgCol) / cols
        
        val minDist = minOf(relDistToTop, relDistToBottom, relDistToLeft, relDistToRight)
        
        val boardAfterGravity = when (minDist) {
            relDistToTop -> boardAfterClear.applyGravityUp() // Closest to top -> Pushed UP (comes from bottom)
            relDistToBottom -> boardAfterClear.applyGravityDown() // Closest to bottom -> Pushed DOWN (comes from top)
            relDistToLeft -> boardAfterClear.applyGravityLeft() // Closest to left -> Pushed LEFT (comes from right)
            else -> boardAfterClear.applyGravityRight() // Closest to right -> Pushed RIGHT (comes from left)
        }
        
        // 3. Refill the empty spaces
        var nextId = state.nextPieceId
        if (nextId < 1) nextId = 1 // Ensure positive IDs

        val (refilledBoard, refilledNextId) = refillBoard(boardAfterGravity, BOOM_BLOCKS_TONES, nextId)
        val isOnboarding = state.secondsUntilDifficultyIncrease >= 9999
        
        val (finalBoard, finalNextId) = if (isOnboarding) {
            ensureExplodableBoard(refilledBoard, BOOM_BLOCKS_TONES, refilledNextId)
        } else {
            refilledBoard to refilledNextId
        }
        nextId = finalNextId

        val scoreGain = scoreCalculator.calculateScore(
            ScoreCalculator.ScoreParams(
                tilesPlaced = 0,
                linesCleared = 0,
                currentStreak = 0,
                areaTilesCleared = group.size,
                isBoardCleared = finalBoard.isEmpty(),
                boardFillRatio = 1f,
            )
        )
        
        val nextScore = state.score + scoreGain
        val awardedTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
            val blocksBonus = group.size * GameLogic.TIME_ATTACK_BONUS_PER_CLEARED_BLOCK_MILLIS
            val scoreBonus = (nextScore / GameLogic.TIME_ATTACK_SCORE_BONUS_THRESHOLD - state.score / GameLogic.TIME_ATTACK_SCORE_BONUS_THRESHOLD) * GameLogic.TIME_ATTACK_SCORE_BONUS_MILLIS
            blocksBonus + scoreBonus
        } else 0L

        val hasMoves = hasAnyExplodableGroup(finalBoard)
        val nextStatus = if (hasMoves || isOnboarding) GameStatus.Running else GameStatus.GameOver
        
        val explodedTones = group.associateWith { point ->
            state.board.toneAt(point.column, point.row) ?: CellTone.Cyan
        }

        val updatedChallenge = updateChallengeProgress(
            challenge = state.activeChallenge,
            scoreGain = scoreGain,
            blocksCleared = group.size
        )
        
        val events = mutableSetOf(GameEvent.PlacementAccepted)
        if (updatedChallenge != null && updatedChallenge.isCompleted && state.activeChallenge?.isCompleted == false) {
            events.add(GameEvent.ChallengeCompleted)
        }
        
        return GameMoveResult(
            state = state.copy(
                board = finalBoard,
                score = nextScore,
                lastMoveScore = scoreGain,
                status = nextStatus,
                lastActionTime = currentEpochMillis(),
                nextPieceId = nextId,
                recentlyExplodedPoints = group,
                recentlyExplodedTones = explodedTones,
                clearAnimationToken = state.clearAnimationToken + 1,
                impactFlashToken = state.impactFlashToken + 1,
                feedbackToken = state.feedbackToken + 1,
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackClear, scoreGain),
                    emphasis = FeedbackEmphasis.Bonus,
                    token = state.feedbackToken + 1
                ),
                activeChallenge = updatedChallenge,
                remainingTimeMillis = state.remainingTimeMillis?.plus(awardedTimeMillis),
            ),
            events = events
        )
    }

    private fun updateChallengeProgress(
        challenge: DailyChallenge?,
        scoreGain: Int,
        blocksCleared: Int
    ): DailyChallenge? {
        if (challenge == null) return null

        val updatedTasks = challenge.tasks.map { task ->
            val progressGain = when (task.type) {
                ChallengeTaskType.ClearBlocks -> blocksCleared
                ChallengeTaskType.ReachScore -> scoreGain
                ChallengeTaskType.PlacePieces -> 1
                else -> 0
            }
            task.copy(current = task.current + progressGain)
        }

        return challenge.copy(tasks = updatedTasks)
    }


    private fun findConnectedGroup(board: BoardMatrix, start: GridPoint): Set<GridPoint> {
        val tone = board.toneAt(start.column, start.row) ?: return emptySet()
        val group = mutableSetOf<GridPoint>()
        val queue = mutableListOf(start)
        
        while (queue.isNotEmpty()) {
            val point = queue.removeAt(0)
            if (point in group) continue
            if (board.toneAt(point.column, point.row) == tone) {
                group.add(point)
                val neighbors = listOf(
                    GridPoint(point.column + 1, point.row),
                    GridPoint(point.column - 1, point.row),
                    GridPoint(point.column, point.row + 1),
                    GridPoint(point.column, point.row - 1)
                )
                for (neighbor in neighbors) {
                    if (neighbor.column in 0 until board.columns && 
                        neighbor.row in 0 until board.rows &&
                        neighbor !in group) {
                        queue.add(neighbor)
                    }
                }
            }
        }
        return group
    }

    private fun hasAnyExplodableGroup(board: BoardMatrix): Boolean {
        val visited = mutableSetOf<GridPoint>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.columns) {
                val point = GridPoint(col, row)
                if (point !in visited) {
                    val group = findConnectedGroup(board, point)
                    if (group.size >= MIN_EXPLODE_SIZE) return true
                    visited.addAll(group)
                }
            }
        }
        return false
    }

    private fun presentTones(board: BoardMatrix): List<CellTone> = buildSet {
        for (row in 0 until board.rows) {
            for (col in 0 until board.columns) {
                board.toneAt(col, row)?.let(::add)
            }
        }
    }.toList()

    private fun generatePlayableStartingBoard(config: GameConfig): Pair<BoardMatrix, Long> {
        repeat(STARTING_BOARD_GENERATION_ATTEMPTS) {
            val candidate = generateRandomBoard(config)
            if (hasAnyExplodableGroup(candidate.first)) {
                return candidate
            }
        }

        val (fallbackBoard, nextId) = generateRandomBoard(config)
        return ensureExplodableBoard(
            board = fallbackBoard,
            refillTones = BOOM_BLOCKS_TONES,
            nextPieceId = nextId,
        )
    }

    private fun generateRandomBoard(config: GameConfig): Pair<BoardMatrix, Long> {
        var board = BoardMatrix.empty(config.columns, config.rows)
        var nextId = 1L

        for (row in 0 until config.rows) {
            for (col in 0 until config.columns) {
                board = board.fill(
                    points = listOf(GridPoint(col, row)),
                    tone = BOOM_BLOCKS_TONES.random(random),
                    value = nextId.toInt(),
                )
                nextId++
            }
        }

        return board to nextId
    }

    private fun pointsForTone(board: BoardMatrix, tone: CellTone): Set<GridPoint> = buildSet {
        for (row in 0 until board.rows) {
            for (col in 0 until board.columns) {
                if (board.toneAt(col, row) == tone) {
                    add(GridPoint(col, row))
                }
            }
        }
    }

    private fun refillBoard(
        board: BoardMatrix,
        refillTones: List<CellTone>,
        nextPieceId: Long,
    ): Pair<BoardMatrix, Long> {
        var currentBoard = board
        var currentNextId = nextPieceId
        for (col in 0 until currentBoard.columns) {
            for (row in 0 until currentBoard.rows) {
                if (!currentBoard.isOccupied(col, row)) {
                    currentBoard = currentBoard.fill(
                        points = listOf(GridPoint(col, row)),
                        tone = refillTones.random(random),
                        value = currentNextId.toInt(),
                    )
                    currentNextId++
                }
            }
        }
        return currentBoard to currentNextId
    }

    private fun ensureExplodableBoard(
        board: BoardMatrix,
        refillTones: List<CellTone>,
        nextPieceId: Long,
    ): Pair<BoardMatrix, Long> {
        if (hasAnyExplodableGroup(board)) return board to nextPieceId
        val guaranteedTone = refillTones.firstOrNull() ?: return board to nextPieceId
        val anchorPoints = listOf(
            GridPoint(0, 0),
            GridPoint(1.coerceAtMost(board.columns - 1), 0),
            GridPoint(0, 1.coerceAtMost(board.rows - 1)),
        ).distinct()
        if (anchorPoints.size < REWARDED_REVIVE_GROUP_MIN_SIZE) return board to nextPieceId

        var currentBoard = board
        var currentNextId = nextPieceId
        anchorPoints.forEach { point ->
            currentBoard = currentBoard.fill(
                points = listOf(point),
                tone = guaranteedTone,
                value = currentNextId.toInt(),
            )
            currentNextId++
        }
        return currentBoard to currentNextId
    }

    override fun holdPiece(state: GameState): GameMoveResult = invalidMove(state)
    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)

        val availableTones = presentTones(state.board)
        if (availableTones.isEmpty()) return invalidMove(state)
        val removedTone = availableTones.random(random)
        val removedPoints = pointsForTone(state.board, removedTone)

        val boardAfterClear = state.board.clearPoints(removedPoints).applyGravityDown()
        val (refilledBoard, refilledNextId) = refillBoard(
            boardAfterClear,
            BOOM_BLOCKS_TONES,
            state.nextPieceId
        )

        val isOnboarding = state.secondsUntilDifficultyIncrease >= 9999
        val (finalBoard, finalNextId) = if (isOnboarding) {
            ensureExplodableBoard(refilledBoard, BOOM_BLOCKS_TONES, refilledNextId)
        } else {
            refilledBoard to refilledNextId
        }

        val nextToken = state.feedbackToken + 1L
        val nextState = state.copy(
            board = finalBoard,
            nextPieceId = finalNextId,
            recentlyExplodedPoints = removedPoints,
            recentlyExplodedTones = removedPoints.associateWith { removedTone },
            clearAnimationToken = state.clearAnimationToken + 1,
            impactFlashToken = state.impactFlashToken + 1,
            message = gameText(GameTextKey.GameMessageAdRewardBoomBlocks),
            floatingFeedback = FloatingFeedback(
                text = gameText(GameTextKey.FeedbackAdRewardBoomBlocks, removedTone.name),
                emphasis = FeedbackEmphasis.Bonus,
                token = nextToken,
            ),
            feedbackToken = nextToken,
        )
        return GameMoveResult(state = nextState, events = setOf(GameEvent.SpecialTriggered))
    }
    override fun commitSoftLock(state: GameState): GameMoveResult = invalidMove(state)
    override fun reviveFromReward(state: GameState): GameMoveResult {
        if (state.status != GameStatus.GameOver || state.rewardedReviveUsed) {
            return invalidMove(state)
        }

        val availableTones = presentTones(state.board)
        if (availableTones.isEmpty()) return invalidMove(state)
        val removedTone = availableTones.random(random)
        val removedPoints = pointsForTone(state.board, removedTone)
        if (removedPoints.isEmpty()) return invalidMove(state)

        val refillTones = BOOM_BLOCKS_TONES.filterNot { it == removedTone }
        var nextPieceId = state.nextPieceId
        var revivedBoard = state.board.clearPoints(removedPoints).applyGravityDown()
        refillBoard(
            board = revivedBoard,
            refillTones = refillTones,
            nextPieceId = nextPieceId,
        ).also { (filledBoard, nextId) ->
            revivedBoard = filledBoard
            nextPieceId = nextId
        }
        ensureExplodableBoard(
            board = revivedBoard,
            refillTones = refillTones,
            nextPieceId = nextPieceId,
        ).also { (playableBoard, nextId) ->
            revivedBoard = playableBoard
            nextPieceId = nextId
        }

        val nextToken = state.feedbackToken + 1L
        return GameMoveResult(
            state = state.copy(
                board = revivedBoard,
                status = GameStatus.Running,
                nextPieceId = nextPieceId,
                recentlyExplodedPoints = removedPoints,
                recentlyExplodedTones = removedPoints.associateWith { removedTone },
                clearAnimationToken = state.clearAnimationToken + 1L,
                impactFlashToken = state.impactFlashToken + 1L,
                screenShakeToken = state.screenShakeToken + 1L,
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackExtraLife, removedPoints.size),
                    emphasis = FeedbackEmphasis.Bonus,
                    token = nextToken,
                ),
                feedbackToken = nextToken,
                rewardedReviveUsed = true,
                lastActionTime = currentEpochMillis(),
                remainingTimeMillis = state.remainingTimeMillis?.plus(GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS),
                message = gameText(GameTextKey.GameMessageExtraLifeUsed, removedPoints.size),
            ),
            events = setOf(GameEvent.Revived),
        )
    }

    override fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running) return state

        val nextRemainingTimeMillis = state.remainingTimeMillis?.minus(1_000L)
        if (nextRemainingTimeMillis != null && nextRemainingTimeMillis <= 0L) {
            return state.copy(
                remainingTimeMillis = 0L,
                status = GameStatus.GameOver,
                message = gameText(GameTextKey.GameOverTitle),
            )
        }

        return state.copy(
            remainingTimeMillis = nextRemainingTimeMillis,
        )
    }

    private fun invalidMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state,
        events = setOf(GameEvent.InvalidDrop),
    )
}
