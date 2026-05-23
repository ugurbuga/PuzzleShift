package com.ugurbuga.blockgames.game.logic

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
import com.ugurbuga.blockgames.game.model.LaunchBarState
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.settings.GameSessionCodec
import kotlin.random.Random

internal class BlockWiseGameLogic(
    private val random: Random = Random.Default,
    private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val TRAY_SIZE = 3
        private const val REWARDED_REVIVE_ROW_COUNT = 4
        private const val PLAYABLE_PIECE_GENERATION_ATTEMPTS = 24
    }

    override fun restoreGame(state: GameState): GameState {
        return state.copy(nextPieceId = maxOf(state.nextPieceId, GameSessionCodec.maxPieceId(state) + 1L))
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
        var currentNextId = 1L
        val tray = List(TRAY_SIZE) {
            val (piece, nextId) = createPiece(level = 1, nextPieceId = currentNextId)
            currentNextId = nextId
            piece
        }
        val state = GameState(
            config = config,
            gameMode = mode,
            gameplayStyle = GameplayStyle.BlockWise,
            board = board,
            activePiece = tray.firstOrNull(),
            nextQueue = tray.drop(1),
            holdPiece = null,
            canHold = false,
            lastPlacementColumn = null,
            score = 0,
            lastMoveScore = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            combo = ComboState(),
            perfectDropStreak = 0,
            launchBar = LaunchBarState(),
            columnPressure = emptyList(),
            softLock = null,
            status = GameStatus.Running,
            recentlyClearedRows = emptySet(),
            recentlyClearedColumns = emptySet(),
            lastResolvedLines = 0,
            lastChainDepth = 0,
            specialChainCount = 0,
            clearAnimationToken = 0L,
            screenShakeToken = 0L,
            impactFlashToken = 0L,
            comboPopupToken = 0L,
            floatingFeedback = null,
            feedbackToken = 0L,
            rewardedReviveUsed = false,
            nextPieceId = currentNextId,
            remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
            message = gameText(GameTextKey.GameMessageSelectColumn),
            activeChallenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) }),
        )
        return if (hasAnyValidPlacement(state.board, state.trayPieces, state.config)) {
            state
        } else {
            state.copy(
                status = GameStatus.GameOver,
                message = gameText(GameTextKey.GameMessageNoOpening),
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackOverflow),
                    emphasis = FeedbackEmphasis.Danger,
                    token = 1L,
                ),
                feedbackToken = 1L,
            )
        }
    }

    override fun previewPlacement(
        state: GameState,
        pieceId: Long,
        origin: GridPoint,
    ): PlacementPreview? {
        val piece = state.trayPieces.firstOrNull { it.id == pieceId } ?: return null
        if (state.status != GameStatus.Running) return null
        if (!isValidPlacement(state.board, piece, origin)) return null

        val occupiedCells = piece.cellsAt(origin)
        val placedBoard = state.board.fill(points = occupiedCells, tone = piece.tone)
        val clearedRows = placedBoard.fullRows().toSet()
        val clearedColumns = placedBoard.fullColumns().toSet()
        return PlacementPreview(
            selectedColumn = origin.column,
            entryAnchor = origin,
            landingAnchor = origin,
            occupiedCells = occupiedCells,
            coveredColumns = origin.column..<(origin.column + piece.width),
            isPerfectDrop = false,
            clearedRows = clearedRows,
            clearedColumns = clearedColumns,
        )
    }

    override fun previewPlacement(
        state: GameState,
        column: Int,
    ): PlacementPreview? {
        val piece = state.activePiece ?: return null
        val origin = findFirstValidOrigin(
            board = state.board,
            piece = piece,
            config = state.config,
            preferredColumn = column,
        ) ?: return null
        return previewPlacement(state = state, pieceId = piece.id, origin = origin)
    }

    override fun previewImpactPoints(
        state: GameState,
        preview: PlacementPreview?,
    ): Set<GridPoint> {
        val resolvedPreview = preview ?: return emptySet()
        if (state.status != GameStatus.Running) return emptySet()
        return buildSet {
            addAll(state.board.occupiedPointsInRows(resolvedPreview.clearedRows))
            addAll(state.board.occupiedPointsInColumns(resolvedPreview.clearedColumns))
        }
    }

    override fun placePiece(
        state: GameState,
        pieceId: Long,
        origin: GridPoint,
    ): GameMoveResult {
        val piece = state.trayPieces.firstOrNull { it.id == pieceId } ?: return invalidMove(state)
        if (state.status != GameStatus.Running) return invalidMove(state)

        val preview = previewPlacement(state = state, pieceId = pieceId, origin = origin) ?: return invalidMove(state)
        val placedBoard = state.board.fill(points = preview.occupiedCells, tone = piece.tone)
        val clearedRows = placedBoard.fullRows().toSet()
        val clearedColumns = placedBoard.fullColumns().toSet()
        val clearedPoints = buildSet {
            addAll(placedBoard.occupiedPointsInRows(clearedRows))
            addAll(placedBoard.occupiedPointsInColumns(clearedColumns))
        }
        val resolvedBoard = placedBoard.clearLines(rowsToClear = clearedRows, columnsToClear = clearedColumns)
        val totalLinesCleared = clearedRows.size + clearedColumns.size
        val nextComboChain = if (totalLinesCleared == 0) 0 else state.combo.chain + 1
        val comboState = ComboState(
            chain = nextComboChain,
            best = maxOf(state.combo.best, nextComboChain),
        )
        val totalClearedBlocks = clearedPoints.size
        val boardSize = state.config.columns * state.config.rows
        val nextScore = state.score + scoreCalculator.calculateScore(
            ScoreCalculator.ScoreParams(
                tilesPlaced = piece.cells.size,
                linesCleared = totalLinesCleared,
                currentStreak = nextComboChain,
                specialBlocksTriggered = emptyList(),
                areaTilesCleared = 0,
                isBoardCleared = resolvedBoard.isEmpty(),
                isPerfectPlacement = false,
                isHardPlacement = false,
                moveDurationMillis = null,
                boardFillRatio = if (boardSize == 0) 0f else placedBoard.occupiedCount.toFloat() / boardSize.toFloat(),
                chainReactionCount = if (clearedRows.isNotEmpty() && clearedColumns.isNotEmpty()) 1 else 0,
            ),
        )
        val lastMoveScore = nextScore - state.score
        val awardedTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
            val blocksBonus = totalClearedBlocks * GameLogic.TIME_ATTACK_BONUS_PER_CLEARED_BLOCK_MILLIS
            val scoreBonus = (nextScore / GameLogic.TIME_ATTACK_SCORE_BONUS_THRESHOLD - state.score / GameLogic.TIME_ATTACK_SCORE_BONUS_THRESHOLD) * GameLogic.TIME_ATTACK_SCORE_BONUS_MILLIS
            blocksBonus + scoreBonus
        } else {
            0L
        }
        val nextRemainingTimeMillis = state.remainingTimeMillis?.plus(awardedTimeMillis)
        val updatedChallenge = updateChallengeProgress(
            challenge = state.activeChallenge,
            rowsCleared = clearedRows.size,
            columnsCleared = clearedColumns.size,
            scoreGain = lastMoveScore,
        )
        val (trayAfterPlacement, nextPieceId) = replenishTray(
            remainingPieces = state.trayPieces.filterNot { it.id == pieceId },
            level = computeLevel(state.difficultyStage, state.linesCleared + totalLinesCleared, state.config),
            nextPieceId = state.nextPieceId,
        )
        val nextActivePiece = trayAfterPlacement.firstOrNull()
        val nextQueue = trayAfterPlacement.drop(1)
        val hasAnyMove = hasAnyValidPlacement(resolvedBoard, trayAfterPlacement, state.config)
        val challengeCompleted = updatedChallenge?.isCompleted == true
        val nextStatus = when {
            challengeCompleted -> GameStatus.GameOver
            hasAnyMove -> GameStatus.Running
            else -> GameStatus.GameOver
        }
        val events = linkedSetOf(GameEvent.PlacementAccepted)
        if (totalLinesCleared > 0) events += GameEvent.LineClear
        if (nextComboChain > 1) events += GameEvent.Combo
        if (clearedRows.isNotEmpty() && clearedColumns.isNotEmpty()) events += GameEvent.ChainReaction
        if (nextStatus == GameStatus.GameOver) events += GameEvent.GameOver
        if (challengeCompleted) events += GameEvent.ChallengeCompleted

        val nextToken = state.feedbackToken + 1L
        val floatingFeedback = when {
            totalLinesCleared > 0 -> FloatingFeedback(
                text = gameText(GameTextKey.FeedbackClear, lastMoveScore),
                emphasis = FeedbackEmphasis.Bonus,
                token = nextToken,
            )
            lastMoveScore > 0 -> FloatingFeedback(
                text = gameText(GameTextKey.FeedbackScoreOnly, lastMoveScore),
                emphasis = FeedbackEmphasis.Info,
                token = nextToken,
            )
            else -> null
        }

        return GameMoveResult(
            state = state.copy(
                board = resolvedBoard,
                activePiece = nextActivePiece,
                nextQueue = nextQueue,
                holdPiece = null,
                canHold = false,
                lastPlacementColumn = origin.column,
                score = nextScore,
                lastMoveScore = lastMoveScore,
                linesCleared = state.linesCleared + totalLinesCleared,
                level = computeLevel(state.difficultyStage, state.linesCleared + totalLinesCleared, state.config),
                difficultyStage = state.difficultyStage,
                secondsUntilDifficultyIncrease = state.secondsUntilDifficultyIncrease,
                combo = comboState,
                perfectDropStreak = 0,
                launchBar = LaunchBarState(),
                columnPressure = emptyList(),
                softLock = null,
                status = nextStatus,
                recentlyClearedRows = clearedRows,
                recentlyClearedColumns = clearedColumns,
                lastResolvedLines = totalLinesCleared,
                lastChainDepth = when {
                    clearedRows.isNotEmpty() && clearedColumns.isNotEmpty() -> 2
                    totalLinesCleared > 0 -> 1
                    else -> 0
                },
                specialChainCount = 0,
                clearAnimationToken = if (totalLinesCleared > 0) state.clearAnimationToken + 1L else state.clearAnimationToken,
                screenShakeToken = if (totalLinesCleared > 0 || nextStatus == GameStatus.GameOver) state.screenShakeToken + 1L else state.screenShakeToken,
                impactFlashToken = if (totalLinesCleared > 0) state.impactFlashToken + 1L else state.impactFlashToken,
                comboPopupToken = if (nextComboChain > 1 || totalLinesCleared > 0) state.comboPopupToken + 1L else state.comboPopupToken,
                floatingFeedback = floatingFeedback,
                feedbackToken = nextToken,
                rewardedReviveUsed = state.rewardedReviveUsed,
                nextPieceId = nextPieceId,
                remainingTimeMillis = nextRemainingTimeMillis,
                message = when {
                    nextStatus == GameStatus.GameOver -> gameText(GameTextKey.GameMessageNoOpening)
                    totalLinesCleared > 0 -> gameText(GameTextKey.GameMessageLinesCleared, totalLinesCleared)
                    else -> gameText(GameTextKey.GameMessageGoodShot)
                },
                activeChallenge = updatedChallenge,
            ),
            preview = preview,
            events = events,
        )
    }

    override fun placePiece(
        state: GameState,
        column: Int,
    ): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val preview = previewPlacement(state = state, column = column) ?: return invalidMove(state)
        return placePiece(
            state = state,
            pieceId = piece.id,
            origin = preview.landingAnchor,
        )
    }

    override fun holdPiece(state: GameState): GameMoveResult = invalidMove(state)

    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)

        val currentNextId = state.nextPieceId
        val (newTray, nextId) = replenishTray(emptyList(), state.level, currentNextId)

        val nextToken = state.feedbackToken + 1L
        val nextState = state.copy(
            activePiece = newTray.firstOrNull(),
            nextQueue = newTray.drop(1),
            nextPieceId = nextId,
            message = gameText(GameTextKey.GameMessageAdRewardBlockWise),
            floatingFeedback = FloatingFeedback(
                text = gameText(GameTextKey.FeedbackAdRewardBlockWise),
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

        val rowsToClear = selectRewardRows(state.board, REWARDED_REVIVE_ROW_COUNT)
        val revivedBoard = state.board.clearLines(rowsToClear = rowsToClear, columnsToClear = emptySet())
        val (revivedTray, nextPieceId) = ensurePlayableTrayAfterRevive(
            board = revivedBoard,
            trayPieces = state.trayPieces.ifEmpty { listOfNotNull(state.activePiece) },
            level = state.level,
            config = state.config,
            nextPieceId = state.nextPieceId,
        )
        val nextToken = state.feedbackToken + 1L
        return GameMoveResult(
            state = state.copy(
                board = revivedBoard,
                activePiece = revivedTray.firstOrNull(),
                nextQueue = revivedTray.drop(1),
                lastMoveScore = 0,
                combo = ComboState(),
                launchBar = LaunchBarState(),
                columnPressure = emptyList(),
                softLock = null,
                status = GameStatus.Running,
                recentlyClearedRows = rowsToClear,
                recentlyClearedColumns = emptySet(),
                lastResolvedLines = rowsToClear.size,
                lastChainDepth = if (rowsToClear.isEmpty()) 0 else 1,
                specialChainCount = 0,
                clearAnimationToken = state.clearAnimationToken + 1L,
                screenShakeToken = state.screenShakeToken + 1L,
                impactFlashToken = state.impactFlashToken + 1L,
                comboPopupToken = state.comboPopupToken + 1L,
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackExtraLife, rowsToClear.size),
                    emphasis = FeedbackEmphasis.Bonus,
                    token = nextToken,
                ),
                feedbackToken = nextToken,
                rewardedReviveUsed = true,
                nextPieceId = nextPieceId,
                remainingTimeMillis = state.remainingTimeMillis?.plus(GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS),
                message = gameText(GameTextKey.GameMessageExtraLifeUsed, rowsToClear.size),
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

        val nextSeconds = state.secondsUntilDifficultyIncrease - 1
        if (nextSeconds > 0) {
            return state.copy(
                secondsUntilDifficultyIncrease = nextSeconds,
                remainingTimeMillis = nextRemainingTimeMillis,
            )
        }

        val nextDifficultyStage = state.difficultyStage + 1
        val nextLevel = computeLevel(
            difficultyStage = nextDifficultyStage,
            clearedLines = state.linesCleared,
            config = state.config,
        )

        return state.copy(
            difficultyStage = nextDifficultyStage,
            level = nextLevel,
            secondsUntilDifficultyIncrease = state.config.difficultyIntervalSeconds,
            remainingTimeMillis = nextRemainingTimeMillis,
            message = gameText(GameTextKey.GameMessageTempoUp),
        )
    }

    fun hasAnyValidPlacement(
        board: BoardMatrix,
        piece: Piece,
        config: GameConfig,
    ): Boolean {
        if (piece.width > config.columns || piece.height > config.rows) return false
        for (row in 0..(config.rows - piece.height)) {
            for (column in 0..(config.columns - piece.width)) {
                if (isValidPlacement(board, piece, GridPoint(column = column, row = row))) {
                    return true
                }
            }
        }
        return false
    }

    fun hasAnyValidPlacement(
        board: BoardMatrix,
        pieces: List<Piece>,
        config: GameConfig,
    ): Boolean = pieces.any { piece -> hasAnyValidPlacement(board, piece, config) }

    private fun updateChallengeProgress(
        challenge: DailyChallenge?,
        rowsCleared: Int,
        columnsCleared: Int,
        scoreGain: Int,
    ): DailyChallenge? {
        if (challenge == null) return null
        val updatedTasks = challenge.tasks.map { task ->
            val progressGain = when (task.type) {
                ChallengeTaskType.ClearBlocks -> 0
                ChallengeTaskType.ClearRows -> rowsCleared
                ChallengeTaskType.ReachScore -> scoreGain
                ChallengeTaskType.TriggerSpecial -> 0
                ChallengeTaskType.PerfectPlacement -> 0
                ChallengeTaskType.ChainReaction -> 0
                ChallengeTaskType.ClearColumns -> columnsCleared
                ChallengeTaskType.PlacePieces -> 1
                ChallengeTaskType.ClearRounds -> 0
                ChallengeTaskType.ClearBothDirections -> if (rowsCleared > 0 && columnsCleared > 0) 1 else 0
                ChallengeTaskType.SolveWords -> 0
            }
            task.copy(current = task.current + progressGain)
        }
        return challenge.copy(tasks = updatedTasks)
    }

    private fun computeLevel(
        difficultyStage: Int,
        clearedLines: Int,
        config: GameConfig,
    ): Int = 1 + difficultyStage + (clearedLines / config.linesPerLevel.coerceAtLeast(1))

    private fun replenishTray(
        remainingPieces: List<Piece>,
        level: Int,
        nextPieceId: Long,
    ): Pair<List<Piece>, Long> {
        val tray = remainingPieces.toMutableList()
        var currentNextId = nextPieceId
        while (tray.size < TRAY_SIZE) {
            val (piece, nextId) = createPiece(level = level, nextPieceId = currentNextId)
            tray += piece
            currentNextId = nextId
        }
        return tray.toList() to currentNextId
    }

    private fun ensurePlayableTrayAfterRevive(
        board: BoardMatrix,
        trayPieces: List<Piece>,
        level: Int,
        config: GameConfig,
        nextPieceId: Long,
    ): Pair<List<Piece>, Long> {
        val playableExisting = trayPieces.filter { hasAnyValidPlacement(board, it, config) }
        if (playableExisting.isNotEmpty()) {
            return replenishTray(playableExisting.take(TRAY_SIZE), level, nextPieceId)
        }
        repeat(PLAYABLE_PIECE_GENERATION_ATTEMPTS) {
            val (candidate, nextId) = createPiece(level = level, nextPieceId = nextPieceId)
            if (hasAnyValidPlacement(board, candidate, config)) {
                return replenishTray(listOf(candidate), level, nextId)
            }
        }
        val (fallback, nextId) = createFallbackPlayablePiece(board, config, nextPieceId)
        return replenishTray(listOf(fallback), level, nextId)
    }

    private fun selectRewardRows(
        board: BoardMatrix,
        maxRowsToClear: Int,
    ): Set<Int> {
        if (maxRowsToClear <= 0) return emptySet()
        return (0 until board.rows)
            .filter { row ->
                (0 until board.columns).any { column -> board.isOccupied(column, row) }
            }
            .shuffled(random)
            .take(maxRowsToClear)
            .toSet()
    }

    private fun createFallbackPlayablePiece(
        board: BoardMatrix,
        config: GameConfig,
        nextPieceId: Long,
    ): Pair<Piece, Long> {
        val candidateKinds = listOf(PieceKind.Domino, PieceKind.TriL, PieceKind.Square, PieceKind.T, PieceKind.I)
        candidateKinds.forEach { kind ->
            repeat(4) { rotation ->
                val rotatedCells = rotateAndNormalize(kind.template, rotation)
                val candidate = Piece(
                    id = nextPieceId,
                    kind = kind,
                    tone = CellTone.entries.random(random),
                    cells = rotatedCells,
                    width = rotatedCells.maxOf { it.column } + 1,
                    height = rotatedCells.maxOf { it.row } + 1,
                    special = SpecialBlockType.None,
                )
                if (hasAnyValidPlacement(board = board, piece = candidate, config = config)) {
                    return candidate to (nextPieceId + 1L)
                }
            }
        }

        return Piece(
            id = nextPieceId,
            kind = PieceKind.Domino,
            tone = CellTone.entries.random(random),
            cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
            width = 2,
            height = 1,
            special = SpecialBlockType.None,
        ) to (nextPieceId + 1L)
    }

    private fun createPiece(level: Int, nextPieceId: Long): Pair<Piece, Long> {
        val availableKinds = PieceKind.entries.filter { it.unlockLevel <= level }
        val kind = availableKinds.random(random)
        val rotationCount = if (kind == PieceKind.Square || kind == PieceKind.Plus || kind == PieceKind.Square3x3) 0 else random.nextInt(4)
        val rotatedCells = rotateAndNormalize(kind.template, rotationCount)
        return Piece(
            id = nextPieceId,
            kind = kind,
            tone = CellTone.entries.random(random),
            cells = rotatedCells,
            width = rotatedCells.maxOf { it.column } + 1,
            height = rotatedCells.maxOf { it.row } + 1,
            special = SpecialBlockType.None,
        ) to (nextPieceId + 1L)
    }

    private fun rotateAndNormalize(
        cells: List<GridPoint>,
        rotationCount: Int,
    ): List<GridPoint> {
        var rotated = cells
        repeat(rotationCount % 4) {
            rotated = rotated.map { point ->
                GridPoint(
                    column = -point.row,
                    row = point.column,
                )
            }
        }

        val minColumn = rotated.minOf { it.column }
        val minRow = rotated.minOf { it.row }
        return rotated
            .map { point ->
                GridPoint(
                    column = point.column - minColumn,
                    row = point.row - minRow,
                )
            }
            .sortedWith(compareBy(GridPoint::row, GridPoint::column))
    }

    private fun isValidPlacement(
        board: BoardMatrix,
        piece: Piece,
        origin: GridPoint,
    ): Boolean {
        for (point in piece.cellsAt(origin)) {
            if (point.column !in 0 until board.columns || point.row !in 0 until board.rows) {
                return false
            }
            if (board.isOccupied(point.column, point.row)) {
                return false
            }
        }
        return true
    }

    private fun findFirstValidOrigin(
        board: BoardMatrix,
        piece: Piece,
        config: GameConfig,
        preferredColumn: Int? = null,
    ): GridPoint? {
        if (piece.width > config.columns || piece.height > config.rows) return null
        val maxColumn = config.columns - piece.width
        val maxRow = config.rows - piece.height
        val columns = buildList {
            preferredColumn?.coerceIn(0, maxColumn)?.let(::add)
            addAll((0..maxColumn).filterNot { it == preferredColumn })
        }
        columns.forEach { column ->
            for (row in maxRow downTo 0) {
                val origin = GridPoint(column = column, row = row)
                if (isValidPlacement(board, piece, origin)) return origin
            }
        }
        return null
    }

    private fun invalidMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state,
        events = setOf(GameEvent.InvalidDrop),
    )
}