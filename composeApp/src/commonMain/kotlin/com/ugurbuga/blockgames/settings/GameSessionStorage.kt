package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.ColumnPressure
import com.ugurbuga.blockgames.game.model.ComboState
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameText
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.LaunchBarState
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.PressureLevel
import com.ugurbuga.blockgames.game.model.SoftLockState
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.WordShiftGuess
import com.ugurbuga.blockgames.game.model.WordShiftLetterState
import com.ugurbuga.blockgames.game.model.gameplayStyleFromPersistedValue
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig

sealed class GameSessionSlot {
    abstract val key: String
    abstract val style: GameplayStyle?

    data class Classic(override val style: GameplayStyle) : GameSessionSlot() {
        override val key: String = "classic_${style.name.lowercase()}"
    }

    data class TimeAttack(override val style: GameplayStyle) : GameSessionSlot() {
        override val key: String = "time_attack_${style.name.lowercase()}"
    }

    data class DailyChallenge(val dateId: String, override val style: GameplayStyle) : GameSessionSlot() {
        override val key: String = "daily_challenge_${style.name.lowercase()}_$dateId"
    }

    companion object {
        fun fromKey(key: String): GameSessionSlot? = when {
            key == "classic" -> Classic(GlobalPlatformConfig.gameplayStyle)
            key.startsWith("classic_") -> {
                val styleName = key.removePrefix("classic_")
                val style = gameplayStyleFromPersistedValue(styleName)
                style?.let(::Classic)
            }

            key == "time_attack" -> TimeAttack(GlobalPlatformConfig.gameplayStyle)
            key.startsWith("time_attack_") -> {
                val styleName = key.removePrefix("time_attack_")
                val style = gameplayStyleFromPersistedValue(styleName)
                style?.let(::TimeAttack)
            }

            key.startsWith("daily_challenge_") -> {
                val parts = key.removePrefix("daily_challenge_").split("_", limit = 2)
                if (parts.size == 2) {
                    val style = gameplayStyleFromPersistedValue(parts[0])
                    if (style != null) DailyChallenge(parts[1], style) else null
                } else null
            }

            else -> null
        }
    }
}

fun sessionSlotFor(
    mode: GameMode,
    challenge: DailyChallenge? = null,
    gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
): GameSessionSlot = when {
    challenge != null -> GameSessionSlot.DailyChallenge(
        dateId = "${challenge.year}-${challenge.month.toString().padStart(2, '0')}-${
            challenge.day.toString().padStart(2, '0')
        }",
        style = challenge.style,
    )

    mode == GameMode.TimeAttack -> GameSessionSlot.TimeAttack(gameplayStyle)
    else -> GameSessionSlot.Classic(gameplayStyle)
}

fun GameState.sessionSlot(): GameSessionSlot = sessionSlotFor(
    mode = gameMode,
    challenge = activeChallenge,
    gameplayStyle = gameplayStyle,
)

expect object GameSessionStorage {
    fun load(slot: GameSessionSlot): GameState?
    fun save(slot: GameSessionSlot, state: GameState)
    fun clear(slot: GameSessionSlot)
    fun clear()
    fun cleanup(allowedDateIds: List<String>)
    fun getSavedDailyChallengeDays(yearMonth: String): Set<Int>
}

internal object GameSessionCodec {
    private const val Version = 9
    private const val SectionSeparator = '|'
    private const val FieldSeparator = ','
    private const val ListSeparator = ';'
    private const val PieceListSeparator = '~'
    private const val PointSeparator = ':'
    private const val CellSeparator = '.'
    private const val EmptyToken = "-"
    private const val WordShiftTokenSeparator = '^'
    private const val WordShiftGuessSeparator = '!'
    private const val WordShiftGuessPartSeparator = '*'
    private const val WordShiftHintSeparator = '&'
    private const val WordShiftHintPartSeparator = '%'

    fun encode(state: GameState): String = listOf(
        Version.toString(),
        encodeConfig(state.config),
        encodeBoard(state.board),
        encodePiece(state.activePiece),
        encodePieceList(state.nextQueue),
        encodePiece(state.holdPiece),
        encodeHoldState(state.canHold, state.lastPlacementColumn),
        encodeProgressState(state),
        encodeComboState(state.combo),
        state.perfectDropStreak.toString(),
        encodeLaunchBarState(state.launchBar),
        encodeColumnPressure(state.columnPressure),
        encodeSoftLock(state.softLock),
        state.status.ordinal.toString(),
        encodeIntSet(state.recentlyClearedRows),
        encodeSummaryState(state),
        encodeVisualState(state),
        encodeGameText(state.message),
        encodeActivityState(state),
        encodeChallenge(state.activeChallenge),
        encodeWordShiftState(state),
    ).joinToString(separator = SectionSeparator.toString())

    fun decode(value: String): GameState? {
        val parts = value.split(SectionSeparator, limit = 21)
        val version = parts.firstOrNull()?.toIntOrNull() ?: return null
        if (version !in 2..Version) return null
        val expectedPartCount = when {
            version >= 8 -> 21
            version >= 3 -> 20
            else -> 18
        }
        if (parts.size != expectedPartCount) return null

        val config = decodeConfig(parts[1]) ?: return null
        val board = decodeBoard(parts[2]) ?: return null
        val activePiece = decodePiece(parts[3])
        val nextQueue = decodePieceList(parts[4]) ?: return null
        val holdPiece = decodePieceOrNull(parts[5])
        val holdState = decodeHoldState(parts[6]) ?: return null
        val progress = decodeProgressState(parts[7]) ?: return null
        val combo = decodeComboState(parts[8]) ?: return null
        val perfectDropStreak = parts[9].toIntOrNull() ?: return null
        val launchBar = decodeLaunchBarState(parts[10]) ?: return null
        val columnPressure = decodeColumnPressure(parts[11]) ?: return null
        val softLock = decodeSoftLock(parts[12])
        val status = GameStatus.entries.getOrNull(parts[13].toIntOrNull() ?: return null) ?: return null
        val recentlyClearedRows = decodeIntSet(parts[14]) ?: return null
        val summary = decodeSummaryState(parts[15]) ?: return null
        val visual = decodeVisualState(parts[16]) ?: return null
        val message = decodeGameText(parts[17]) ?: GameText(GameTextKey.GameMessageSelectColumn)
        val inferredGameplayStyle = inferLegacyGameplayStyle(nextQueue)
        val activity = if (version >= 7) {
            decodeActivityState(parts[18]) ?: return null
        } else if (version >= 4) {
            decodeActivityStateV6(parts[18]) ?: return null
        } else if (version >= 3) {
            decodeLegacyActivityState(parts[18], inferredGameplayStyle) ?: return null
        } else {
            ActivityState(gameplayStyle = inferredGameplayStyle)
        }
        val activeChallenge = if (version >= 3) decodeChallenge(parts[19]) else null
        val wordShiftState = if (version >= 8) decodeWordShiftState(parts[20]) ?: return null else WordShiftState()

        return GameState(
            config = config,
            gameMode = progress.gameMode,
            gameplayStyle = activity.gameplayStyle,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            holdPiece = holdPiece,
            canHold = holdState.canHold,
            lastPlacementColumn = holdState.lastPlacementColumn,
            score = progress.score,
            lastMoveScore = progress.lastMoveScore,
            linesCleared = progress.linesCleared,
            level = progress.level,
            difficultyStage = progress.difficultyStage,
            secondsUntilDifficultyIncrease = progress.secondsUntilDifficultyIncrease,
            combo = combo,
            perfectDropStreak = perfectDropStreak,
            launchBar = launchBar,
            columnPressure = columnPressure,
            softLock = softLock,
            status = status,
            recentlyClearedRows = recentlyClearedRows,
            recentlyClearedColumns = activity.recentlyClearedColumns,
            lastResolvedLines = summary.lastResolvedLines,
            lastChainDepth = summary.lastChainDepth,
            specialChainCount = summary.specialChainCount,
            clearAnimationToken = visual.clearAnimationToken,
            screenShakeToken = visual.screenShakeToken,
            impactFlashToken = visual.impactFlashToken,
            comboPopupToken = visual.comboPopupToken,
            floatingFeedback = null,
            feedbackToken = visual.feedbackToken,
            rewardedReviveUsed = activity.rewardedReviveUsed,
            remainingTimeMillis = progress.remainingTimeMillis,
            nextPieceId = activity.nextPieceId,
            message = message,
            activeChallenge = activeChallenge,
            blockSortBonusEmptyColumnUsed = activity.blockSortBonusEmptyColumnUsed,
            blockSortScoredMoveSignatures = activity.blockSortScoredMoveSignatures,
            wordShiftLocaleTag = wordShiftState.localeTag,
            wordShiftSolution = wordShiftState.solution,
            wordShiftGuesses = wordShiftState.guesses,
            wordShiftCurrentGuess = wordShiftState.currentGuess,
            wordShiftKeyboardHints = wordShiftState.keyboardHints,
            wordShiftAwaitingNextRound = wordShiftState.awaitingNextRound,
        )
    }

    fun maxPieceId(state: GameState): Long = buildList<Long> {
        state.activePiece?.let { add(it.id) }
        state.holdPiece?.let { add(it.id) }
        addAll(state.nextQueue.map(Piece::id))
        state.softLock?.let { add(it.pieceId) }
        
        // Include board cell values for modes like BoomBlocks/MergeShift
        for (row in 0 until state.board.rows) {
            for (col in 0 until state.board.columns) {
                state.board.cellAt(col, row)?.let { add(it.value.toLong()) }
            }
        }
    }.maxOrNull() ?: 0L

    private fun encodeConfig(config: GameConfig): String = listOf(
        config.columns,
        config.rows,
        config.difficultyIntervalSeconds,
        config.linesPerLevel,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeConfig(value: String): GameConfig? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 4) return null
        return GameConfig(
            columns = parts[0].toIntOrNull() ?: return null,
            rows = parts[1].toIntOrNull() ?: return null,
            difficultyIntervalSeconds = parts[2].toIntOrNull() ?: return null,
            linesPerLevel = parts[3].toIntOrNull() ?: return null,
        )
    }

    private fun encodeBoard(board: BoardMatrix): String = buildList {
        add(board.columns.toString())
        add(board.rows.toString())
        for (row in 0 until board.rows) {
            for (column in 0 until board.columns) {
                val cell = board.cellAt(column, row)
                add(
                    cell?.let {
                        "${it.tone.ordinal}$CellSeparator${it.special.ordinal}$CellSeparator${it.value}"
                    } ?: EmptyToken,
                )
            }
        }
    }.joinToString(separator = FieldSeparator.toString())

    private fun decodeBoard(value: String): BoardMatrix? {
        val parts = value.split(FieldSeparator)
        if (parts.size < 2) return null
        val columns = parts[0].toIntOrNull() ?: return null
        val rows = parts[1].toIntOrNull() ?: return null
        val expectedCells = columns * rows
        if (parts.size != expectedCells + 2) return null

        var board = BoardMatrix.empty(columns = columns, rows = rows)
        var index = 2
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val token = parts[index++]
                if (token == EmptyToken) continue
                val cellParts = token.split(CellSeparator)
                if (cellParts.size < 2) return null
                val tone = CellTone.entries.getOrNull(cellParts[0].toIntOrNull() ?: return null) ?: return null
                val special = SpecialBlockType.entries.getOrNull(cellParts[1].toIntOrNull() ?: return null) ?: return null
                val cellValue = cellParts.getOrNull(2)?.toIntOrNull() ?: 0
                board = board.fill(listOf(GridPoint(column = column, row = row)), tone = tone, special = special, value = cellValue)
            }
        }
        return board
    }

    private fun encodePiece(piece: Piece?): String {
        if (piece == null) return EmptyToken
        val cells = piece.cells.joinToString(separator = ListSeparator.toString()) { point ->
            "${point.column}$PointSeparator${point.row}"
        }
        return listOf(
            piece.id.toString(),
            piece.kind.ordinal.toString(),
            piece.tone.ordinal.toString(),
            piece.special.ordinal.toString(),
            cells,
            piece.value.toString(),
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun encodePieceList(pieces: List<Piece>): String = pieces.joinToString(separator = PieceListSeparator.toString()) { piece ->
        encodePiece(piece)
    }

    private fun decodePieceOrNull(value: String): Piece? = if (value == EmptyToken || value.isBlank()) null else decodePiece(value)

    private fun decodePieceList(value: String): List<Piece>? {
        if (value.isBlank()) return emptyList()
        return value.split(PieceListSeparator).mapNotNull { token ->
            decodePieceOrNull(token)
        }
    }

    private fun decodePiece(value: String): Piece? {
        val parts = value.split(FieldSeparator)
        if (parts.size < 5) return null
        val id = parts[0].toLongOrNull() ?: return null
        val kind = PieceKind.entries.getOrNull(parts[1].toIntOrNull() ?: return null) ?: return null
        val tone = CellTone.entries.getOrNull(parts[2].toIntOrNull() ?: return null) ?: return null
        val special = SpecialBlockType.entries.getOrNull(parts[3].toIntOrNull() ?: return null) ?: return null
        val cells = decodePoints(parts[4]) ?: return null
        val pieceValue = parts.getOrNull(5)?.toIntOrNull() ?: 0
        val width = cells.maxOfOrNull(GridPoint::column)?.plus(1) ?: 0
        val height = cells.maxOfOrNull(GridPoint::row)?.plus(1) ?: 0
        return Piece(
            id = id,
            kind = kind,
            tone = tone,
            cells = cells,
            width = width,
            height = height,
            special = special,
            value = pieceValue,
        )
    }

    private fun encodeHoldState(canHold: Boolean, lastPlacementColumn: Int?): String = listOf(
        canHold.toString(),
        lastPlacementColumn?.toString() ?: EmptyToken,
    ).joinToString(separator = FieldSeparator.toString())

    private data class HoldState(
        val canHold: Boolean,
        val lastPlacementColumn: Int?,
    )

    private fun decodeHoldState(value: String): HoldState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 2) return null
        return HoldState(
            canHold = parts[0].toBooleanStrictOrNull() ?: return null,
            lastPlacementColumn = parts[1].takeIf { it != EmptyToken }?.toIntOrNull(),
        )
    }

    private data class ProgressState(
        val score: Int,
        val lastMoveScore: Int,
        val linesCleared: Int,
        val level: Int,
        val difficultyStage: Int,
        val secondsUntilDifficultyIncrease: Int,
        val gameMode: GameMode,
        val remainingTimeMillis: Long?,
    )

    private fun encodeProgressState(state: GameState): String = listOf(
        state.score,
        state.lastMoveScore,
        state.linesCleared,
        state.level,
        state.difficultyStage,
        state.secondsUntilDifficultyIncrease,
        state.gameMode.ordinal,
        state.remainingTimeMillis?.toString() ?: EmptyToken,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeProgressState(value: String): ProgressState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 6 && parts.size != 8) return null
        return ProgressState(
            score = parts[0].toIntOrNull() ?: return null,
            lastMoveScore = parts[1].toIntOrNull() ?: return null,
            linesCleared = parts[2].toIntOrNull() ?: return null,
            level = parts[3].toIntOrNull() ?: return null,
            difficultyStage = parts[4].toIntOrNull() ?: return null,
            secondsUntilDifficultyIncrease = parts[5].toIntOrNull() ?: return null,
            gameMode = if (parts.size >= 7) {
                GameMode.entries.getOrNull(parts[6].toIntOrNull() ?: return null) ?: return null
            } else {
                GameMode.Classic
            },
            remainingTimeMillis = if (parts.size >= 8) {
                parts[7].takeIf { it != EmptyToken }?.toLongOrNull()
            } else {
                null
            },
        )
    }

    private fun encodeComboState(combo: ComboState): String = listOf(combo.chain, combo.best).joinToString(separator = FieldSeparator.toString())

    private fun decodeComboState(value: String): ComboState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 2) return null
        return ComboState(
            chain = parts[0].toIntOrNull() ?: return null,
            best = parts[1].toIntOrNull() ?: return null,
        )
    }

    private fun encodeLaunchBarState(state: LaunchBarState): String = listOf(
        state.progress,
        state.boostTurnsRemaining,
        state.lastGain,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeLaunchBarState(value: String): LaunchBarState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 3) return null
        return LaunchBarState(
            progress = parts[0].toFloatOrNull() ?: return null,
            boostTurnsRemaining = parts[1].toIntOrNull() ?: return null,
            lastGain = parts[2].toFloatOrNull() ?: return null,
        )
    }

    private fun encodeColumnPressure(pressures: List<ColumnPressure>): String = pressures.joinToString(separator = ListSeparator.toString()) { pressure ->
        listOf(
            pressure.column,
            pressure.filledCells,
            pressure.fillRatio,
            pressure.level.ordinal,
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodeColumnPressure(value: String): List<ColumnPressure>? {
        if (value.isBlank()) return emptyList()
        return value.split(ListSeparator).mapNotNull { token ->
            val parts = token.split(FieldSeparator)
            if (parts.size != 4) return null
            val level = PressureLevel.entries.getOrNull(parts[3].toIntOrNull() ?: return null) ?: return null
            ColumnPressure(
                column = parts[0].toIntOrNull() ?: return null,
                filledCells = parts[1].toIntOrNull() ?: return null,
                fillRatio = parts[2].toFloatOrNull() ?: return null,
                level = level,
            )
        }
    }

    private fun encodeSoftLock(softLock: SoftLockState?): String {
        if (softLock == null) return EmptyToken
        return listOf(
            softLock.pieceId,
            softLock.remainingMillis,
            softLock.revision,
            encodePreview(softLock.preview),
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodeSoftLock(value: String): SoftLockState? {
        if (value == EmptyToken || value.isBlank()) return null
        val parts = value.split(FieldSeparator, limit = 4)
        if (parts.size != 4) return null
        return SoftLockState(
            pieceId = parts[0].toLongOrNull() ?: return null,
            remainingMillis = parts[1].toLongOrNull() ?: return null,
            revision = parts[2].toLongOrNull() ?: return null,
            preview = decodePreview(parts[3]) ?: return null,
        )
    }

    private fun encodePreview(preview: PlacementPreview): String {
        val occupiedCells = preview.occupiedCells.joinToString(separator = ListSeparator.toString()) { point ->
            "${point.column}$PointSeparator${point.row}"
        }
        return listOf(
            preview.selectedColumn,
            preview.entryAnchor.column,
            preview.entryAnchor.row,
            preview.landingAnchor.column,
            preview.landingAnchor.row,
            preview.coveredColumns.first,
            preview.coveredColumns.last,
            preview.isPerfectDrop,
            occupiedCells,
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodePreview(value: String): PlacementPreview? {
        val parts = value.split(FieldSeparator, limit = 9)
        if (parts.size != 9) return null
        val occupiedCells = decodePoints(parts[8]) ?: return null
        return PlacementPreview(
            selectedColumn = parts[0].toIntOrNull() ?: return null,
            entryAnchor = GridPoint(
                column = parts[1].toIntOrNull() ?: return null,
                row = parts[2].toIntOrNull() ?: return null,
            ),
            landingAnchor = GridPoint(
                column = parts[3].toIntOrNull() ?: return null,
                row = parts[4].toIntOrNull() ?: return null,
            ),
            occupiedCells = occupiedCells,
            coveredColumns = (parts[5].toIntOrNull() ?: return null)..(parts[6].toIntOrNull() ?: return null),
            isPerfectDrop = parts[7].toBooleanStrictOrNull() ?: return null,
        )
    }

    private data class SummaryState(
        val lastResolvedLines: Int,
        val lastChainDepth: Int,
        val specialChainCount: Int,
    )

    private fun encodeSummaryState(state: GameState): String = listOf(
        state.lastResolvedLines,
        state.lastChainDepth,
        state.specialChainCount,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeSummaryState(value: String): SummaryState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 3) return null
        return SummaryState(
            lastResolvedLines = parts[0].toIntOrNull() ?: return null,
            lastChainDepth = parts[1].toIntOrNull() ?: return null,
            specialChainCount = parts[2].toIntOrNull() ?: return null,
        )
    }

    private data class VisualState(
        val clearAnimationToken: Long,
        val screenShakeToken: Long,
        val impactFlashToken: Long,
        val comboPopupToken: Long,
        val feedbackToken: Long,
    )

    private data class ActivityState(
        val recentlyClearedColumns: Set<Int> = emptySet(),
        val rewardedReviveUsed: Boolean = false,
        val gameplayStyle: GameplayStyle = GameplayStyle.StackShift,
        val nextPieceId: Long = 1L,
        val blockSortBonusEmptyColumnUsed: Boolean = false,
        val blockSortScoredMoveSignatures: Set<String> = emptySet(),
    )

    private fun encodeVisualState(state: GameState): String = listOf(
        state.clearAnimationToken,
        state.screenShakeToken,
        state.impactFlashToken,
        state.comboPopupToken,
        state.feedbackToken,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeVisualState(value: String): VisualState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 5) return null
        return VisualState(
            clearAnimationToken = parts[0].toLongOrNull() ?: return null,
            screenShakeToken = parts[1].toLongOrNull() ?: return null,
            impactFlashToken = parts[2].toLongOrNull() ?: return null,
            comboPopupToken = parts[3].toLongOrNull() ?: return null,
            feedbackToken = parts[4].toLongOrNull() ?: return null,
        )
    }

    private fun encodeActivityState(state: GameState): String = listOf(
        encodeIntSet(state.recentlyClearedColumns),
        state.rewardedReviveUsed,
        state.gameplayStyle.ordinal,
        state.nextPieceId.toString(),
        state.blockSortBonusEmptyColumnUsed,
        encodeStringSet(state.blockSortScoredMoveSignatures),
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeActivityState(value: String): ActivityState? {
        val parts = value.split(FieldSeparator)
        if (parts.size !in 3..6) return null
        return ActivityState(
            recentlyClearedColumns = decodeIntSet(parts[0]) ?: return null,
            rewardedReviveUsed = parts[1].toBooleanStrictOrNull() ?: return null,
            gameplayStyle = GameplayStyle.entries.getOrNull(parts[2].toIntOrNull() ?: return null) ?: return null,
            nextPieceId = if (parts.size >= 4) parts[3].toLongOrNull() ?: 1L else 1L,
            blockSortBonusEmptyColumnUsed = if (parts.size >= 5) parts[4].toBooleanStrictOrNull() ?: return null else false,
            blockSortScoredMoveSignatures = if (parts.size >= 6) decodeStringSet(parts[5]) ?: return null else emptySet(),
        )
    }

    private fun decodeActivityStateV6(value: String): ActivityState? {
        val parts = value.split(FieldSeparator)
        if (parts.size !in 3..5) return null
        return ActivityState(
            recentlyClearedColumns = decodeIntSet(parts[0]) ?: return null,
            rewardedReviveUsed = parts[1].toBooleanStrictOrNull() ?: return null,
            gameplayStyle = GameplayStyle.entries.getOrNull(parts[2].toIntOrNull() ?: return null) ?: return null,
            nextPieceId = if (parts.size >= 4) parts[3].toLongOrNull() ?: 1L else 1L,
            blockSortBonusEmptyColumnUsed = false,
            blockSortScoredMoveSignatures = if (parts.size >= 5) decodeStringSet(parts[4]) ?: return null else emptySet(),
        )
    }

    private fun decodeLegacyActivityState(
        value: String,
        gameplayStyle: GameplayStyle,
    ): ActivityState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 2) return null
        return ActivityState(
            recentlyClearedColumns = decodeIntSet(parts[0]) ?: return null,
            rewardedReviveUsed = parts[1].toBooleanStrictOrNull() ?: return null,
            gameplayStyle = gameplayStyle,
        )
    }

    private fun inferLegacyGameplayStyle(nextQueue: List<Piece>): GameplayStyle =
        if (nextQueue.size <= 2) GameplayStyle.BlockWise else GameplayStyle.StackShift

    private fun encodeChallenge(challenge: DailyChallenge?): String {
        if (challenge == null) return EmptyToken
        val tasks = challenge.tasks.joinToString(separator = ListSeparator.toString()) { task ->
            listOf(task.type.stableId, task.target, task.current).joinToString(separator = PointSeparator.toString())
        }
        return listOf(
            challenge.year,
            challenge.month,
            challenge.day,
            tasks,
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodeChallenge(
        value: String,
    ): DailyChallenge? {
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        if (value == EmptyToken || value.isBlank()) return null
        val parts = value.split(FieldSeparator, limit = 4)
        if (parts.size != 4) return null
        val tasks = if (parts[3].isBlank()) {
            emptyList()
        } else {
            parts[3].split(ListSeparator).map { token ->
                val taskParts = token.split(PointSeparator)
                if (taskParts.size != 3) return null
                ChallengeTask(
                    type = decodeChallengeTaskType(taskParts[0]) ?: return null,
                    target = taskParts[1].toIntOrNull() ?: return null,
                    current = taskParts[2].toIntOrNull() ?: return null,
                )
            }
        }
        return DailyChallenge(
            year = parts[0].toIntOrNull() ?: return null,
            month = parts[1].toIntOrNull() ?: return null,
            day = parts[2].toIntOrNull() ?: return null,
            style = gameplayStyle,
            tasks = tasks,
        )
    }

    private fun decodeChallengeTaskType(
        token: String,
    ): ChallengeTaskType? {
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        return ChallengeTaskType.fromStableId(token)
            ?: token.toIntOrNull()?.let { ChallengeTaskType.fromLegacyOrdinal(gameplayStyle, it) }
    }

    private fun encodeGameText(text: GameText): String = buildList {
        add(text.key.ordinal.toString())
        addAll(text.args)
    }.joinToString(separator = FieldSeparator.toString())

    private fun decodeGameText(value: String): GameText? {
        if (value.isBlank()) return null
        val parts = value.split(FieldSeparator)
        val key = GameTextKey.entries.getOrNull(parts.firstOrNull()?.toIntOrNull() ?: return null) ?: return null
        return GameText(key = key, args = parts.drop(1))
    }

    private data class WordShiftState(
        val localeTag: String = "",
        val solution: List<String> = emptyList(),
        val currentGuess: List<String> = emptyList(),
        val guesses: List<WordShiftGuess> = emptyList(),
        val keyboardHints: Map<String, WordShiftLetterState> = emptyMap(),
        val awaitingNextRound: Boolean = false,
    )

    private fun encodeWordShiftState(state: GameState): String = listOf(
        state.wordShiftLocaleTag.ifBlank { EmptyToken },
        encodeWordShiftTokens(state.wordShiftSolution),
        encodeWordShiftTokens(state.wordShiftCurrentGuess),
        encodeWordShiftGuesses(state.wordShiftGuesses),
        encodeWordShiftHints(state.wordShiftKeyboardHints),
        if (state.wordShiftAwaitingNextRound) "1" else "0",
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeWordShiftState(value: String): WordShiftState? {
        val parts = value.split(FieldSeparator, limit = 6)
        if (parts.size !in 5..6) return null
        return WordShiftState(
            localeTag = parts[0].takeUnless { it == EmptyToken }.orEmpty(),
            solution = decodeWordShiftTokens(parts[1]) ?: return null,
            currentGuess = decodeWordShiftTokens(parts[2]) ?: return null,
            guesses = decodeWordShiftGuesses(parts[3]) ?: return null,
            keyboardHints = decodeWordShiftHints(parts[4]) ?: return null,
            awaitingNextRound = parts.getOrNull(5) == "1",
        )
    }

    private fun encodeWordShiftTokens(tokens: List<String>): String =
        if (tokens.isEmpty()) EmptyToken else tokens.joinToString(separator = WordShiftTokenSeparator.toString())

    private fun decodeWordShiftTokens(value: String): List<String>? = when {
        value == EmptyToken || value.isBlank() -> emptyList()
        else -> value.split(WordShiftTokenSeparator).filter(String::isNotBlank)
    }

    private fun encodeWordShiftGuesses(guesses: List<WordShiftGuess>): String =
        if (guesses.isEmpty()) {
            EmptyToken
        } else {
            guesses.joinToString(separator = WordShiftGuessSeparator.toString()) { guess ->
                listOf(
                    encodeWordShiftTokens(guess.tokens),
                    guess.states.joinToString(separator = WordShiftTokenSeparator.toString()) { it.ordinal.toString() },
                ).joinToString(separator = WordShiftGuessPartSeparator.toString())
            }
        }

    private fun decodeWordShiftGuesses(value: String): List<WordShiftGuess>? {
        if (value == EmptyToken || value.isBlank()) return emptyList()
        return value.split(WordShiftGuessSeparator).map { token ->
            val parts = token.split(WordShiftGuessPartSeparator, limit = 2)
            if (parts.size != 2) return null
            WordShiftGuess(
                tokens = decodeWordShiftTokens(parts[0]) ?: return null,
                states = if (parts[1].isBlank()) {
                    emptyList()
                } else {
                    parts[1].split(WordShiftTokenSeparator).map { stateToken ->
                        WordShiftLetterState.entries.getOrNull(stateToken.toIntOrNull() ?: return null) ?: return null
                    }
                },
            )
        }
    }

    private fun encodeWordShiftHints(hints: Map<String, WordShiftLetterState>): String =
        if (hints.isEmpty()) {
            EmptyToken
        } else {
            hints.entries.sortedBy { it.key }.joinToString(separator = WordShiftHintSeparator.toString()) { entry ->
                "${entry.key}$WordShiftHintPartSeparator${entry.value.ordinal}"
            }
        }

    private fun decodeWordShiftHints(value: String): Map<String, WordShiftLetterState>? {
        if (value == EmptyToken || value.isBlank()) return emptyMap()
        return buildMap {
            value.split(WordShiftHintSeparator).forEach { token ->
                val parts = token.split(WordShiftHintPartSeparator, limit = 2)
                if (parts.size != 2) return null
                val state = WordShiftLetterState.entries.getOrNull(parts[1].toIntOrNull() ?: return null) ?: return null
                put(parts[0], state)
            }
        }
    }

    private fun encodeIntSet(values: Set<Int>): String = values.sorted().joinToString(separator = ListSeparator.toString())

    private fun encodeStringSet(values: Set<String>): String = values.sorted().joinToString(separator = ListSeparator.toString())

    private fun decodeIntSet(value: String): Set<Int>? {
        if (value.isBlank()) return emptySet()
        return value.split(ListSeparator).mapNotNull { token -> token.toIntOrNull() }.toSet()
    }

    private fun decodeStringSet(value: String): Set<String>? {
        if (value.isBlank()) return emptySet()
        return value.split(ListSeparator).filter(String::isNotBlank).toSet()
    }

    private fun decodePoints(value: String): List<GridPoint>? {
        if (value.isBlank()) return emptyList()
        return value.split(ListSeparator).mapNotNull { token ->
            val parts = token.split(PointSeparator)
            if (parts.size != 2) return null
            GridPoint(
                column = parts[0].toIntOrNull() ?: return null,
                row = parts[1].toIntOrNull() ?: return null,
            )
        }
    }
}
