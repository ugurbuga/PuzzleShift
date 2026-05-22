package com.ugurbuga.blockgames.game.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_language_arabic
import blockgames.composeapp.generated.resources.app_language_chinese_simplified
import blockgames.composeapp.generated.resources.app_language_dutch
import blockgames.composeapp.generated.resources.app_language_english
import blockgames.composeapp.generated.resources.app_language_french
import blockgames.composeapp.generated.resources.app_language_german
import blockgames.composeapp.generated.resources.app_language_hindi
import blockgames.composeapp.generated.resources.app_language_indonesian
import blockgames.composeapp.generated.resources.app_language_italian
import blockgames.composeapp.generated.resources.app_language_japanese
import blockgames.composeapp.generated.resources.app_language_korean
import blockgames.composeapp.generated.resources.app_language_portuguese
import blockgames.composeapp.generated.resources.app_language_russian
import blockgames.composeapp.generated.resources.app_language_spanish
import blockgames.composeapp.generated.resources.app_language_turkish
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import org.jetbrains.compose.resources.StringResource
import kotlin.math.max

enum class AppLanguage(
    val localeTag: String,
    val labelRes: StringResource,
    val flag: String,
) {
    English(localeTag = "en", labelRes = Res.string.app_language_english, flag = "🇺🇸"),
    Turkish(localeTag = "tr", labelRes = Res.string.app_language_turkish, flag = "🇹🇷"),
    Spanish(localeTag = "es", labelRes = Res.string.app_language_spanish, flag = "🇪🇸"),
    French(localeTag = "fr", labelRes = Res.string.app_language_french, flag = "🇫🇷"),
    German(localeTag = "de", labelRes = Res.string.app_language_german, flag = "🇩🇪"),
    Russian(localeTag = "ru", labelRes = Res.string.app_language_russian, flag = "🇷🇺"),
    ChineseSimplified(localeTag = "zh-Hans", labelRes = Res.string.app_language_chinese_simplified, flag = "🇨🇳"),
    Hindi(localeTag = "hi", labelRes = Res.string.app_language_hindi, flag = "🇮🇳"),
    Arabic(localeTag = "ar", labelRes = Res.string.app_language_arabic, flag = "🇸🇦"),
    Portuguese(localeTag = "pt", labelRes = Res.string.app_language_portuguese, flag = "🇵🇹"),
    Indonesian(localeTag = "id", labelRes = Res.string.app_language_indonesian, flag = "🇮🇩"),
    Japanese(localeTag = "ja", labelRes = Res.string.app_language_japanese, flag = "🇯🇵"),
    Korean(localeTag = "ko", labelRes = Res.string.app_language_korean, flag = "🇰🇷"),
    Italian(localeTag = "it", labelRes = Res.string.app_language_italian, flag = "🇮🇹"),
    Dutch(localeTag = "nl", labelRes = Res.string.app_language_dutch, flag = "🇳🇱"),

    ;

    companion object {
        fun fromDeviceLocaleTag(localeTag: String?): AppLanguage? {
            val normalized = localeTag
                ?.trim()
                ?.replace('_', '-')
                ?.takeIf(String::isNotBlank)
                ?: return null

            entries.firstOrNull { it.localeTag.equals(normalized, ignoreCase = true) }?.let { return it }

            val lowerTag = normalized.lowercase()
            when {
                lowerTag == "zh" ||
                        lowerTag.startsWith("zh-cn") ||
                        lowerTag.startsWith("zh-sg") ||
                        lowerTag.startsWith("zh-hans") -> return ChineseSimplified

                lowerTag.startsWith("zh-tw") ||
                        lowerTag.startsWith("zh-hk") ||
                        lowerTag.startsWith("zh-mo") ||
                        lowerTag.startsWith("zh-hant") -> return null
            }

            val languageCode = lowerTag.substringBefore('-')
            return entries.firstOrNull { candidate ->
                candidate.localeTag.lowercase().substringBefore('-') == languageCode
            }
        }
    }
}

enum class AppThemeMode(
    val isDark: Boolean?,
) {
    System(isDark = null),
    Light(isDark = false),
    Dark(isDark = true),
}

enum class AppColorPalette {
    Classic,
    Aurora,
    Sunset,
    ModernNeon,
    SoftPastel,
    MinimalMonochrome,
}

enum class BlockColorPalette {
    Classic,
    Candy,
    Neon,
    Earth,
    Monochrome,
    Aurora,
    Sunset,
    SoftPastel,
}

fun AppColorPalette.toBlockColorPalette(): BlockColorPalette = when (this) {
    AppColorPalette.Classic -> BlockColorPalette.Classic
    AppColorPalette.Aurora -> BlockColorPalette.Aurora
    AppColorPalette.Sunset -> BlockColorPalette.Sunset
    AppColorPalette.ModernNeon -> BlockColorPalette.Neon
    AppColorPalette.SoftPastel -> BlockColorPalette.SoftPastel
    AppColorPalette.MinimalMonochrome -> BlockColorPalette.Monochrome
}

fun BlockColorPalette.toThemeColorPalette(): AppColorPalette = when (this) {
    BlockColorPalette.Classic -> AppColorPalette.Classic
    BlockColorPalette.Candy -> AppColorPalette.SoftPastel
    BlockColorPalette.Neon -> AppColorPalette.ModernNeon
    BlockColorPalette.Earth -> AppColorPalette.Aurora
    BlockColorPalette.Monochrome -> AppColorPalette.MinimalMonochrome
    BlockColorPalette.Aurora -> AppColorPalette.Aurora
    BlockColorPalette.Sunset -> AppColorPalette.Sunset
    BlockColorPalette.SoftPastel -> AppColorPalette.SoftPastel
}

fun resolveUnifiedThemePalette(
    themePalette: AppColorPalette?,
    blockPalette: BlockColorPalette?,
): AppColorPalette {
    return when {
        themePalette != null && themePalette != AppColorPalette.Classic -> themePalette
        blockPalette != null && blockPalette != BlockColorPalette.Classic -> blockPalette.toThemeColorPalette()
        themePalette != null -> themePalette
        blockPalette != null -> blockPalette.toThemeColorPalette()
        else -> AppColorPalette.Classic
    }
}

enum class BlockVisualStyle {
    Flat,
    Bubble,
    Outline,
    Sharp3D,
    Wood,
    GridSplit,
    Crystal,
    DynamicLiquid,
    Tornado,
    HoneycombTexture,
    SpiderWeb,
    Cosmic,
    Brick,
    SoundWave,
    Prism,
    Flame,
    Gears,
    Pixel,
    Cyberpunk,
    NeonGlow,
    LiquidMarble,
    Holographic,
    GlitchTech,
    AuraEnergy,
    CircuitBoard,
    ;

    fun cornerScale(): Float = when (this) {
        Flat -> 1.0f
        Bubble -> 1.20f
        Outline -> 0.82f
        Sharp3D -> 0.30f
        Wood -> 0.76f
        GridSplit -> 0.54f
        Crystal -> 0f
        DynamicLiquid -> 0.85f
        Tornado -> 1.00f
        HoneycombTexture -> 0.78f
        SpiderWeb -> 0.34f
        Cosmic -> 0.54f
        Brick -> 0.46f
        SoundWave -> 0.88f
        Prism -> 0f
        Flame -> 1.15f
        Gears -> 0.12f
        Pixel -> 0.10f
        Cyberpunk -> 0.44f
        NeonGlow -> 0.90f
        LiquidMarble -> 1.10f
        Holographic -> 0.20f
        GlitchTech -> 0.15f
        AuraEnergy -> 1.30f
        CircuitBoard -> 0.45f
    }

    fun frameCornerRadius(): Dp = when (this) {
        Flat -> 18.dp
        Bubble -> 22.dp
        Outline -> 14.dp
        Sharp3D -> 6.dp
        Wood -> 12.dp
        GridSplit -> 10.dp
        Crystal -> 0.dp
        DynamicLiquid -> 18.dp
        Tornado -> 18.dp
        HoneycombTexture -> 12.dp
        SpiderWeb -> 6.dp
        Cosmic -> 10.dp
        Brick -> 8.dp
        SoundWave -> 16.dp
        Prism -> 0.dp
        Flame -> 20.dp
        Gears -> 4.dp
        Pixel -> 4.dp
        Cyberpunk -> 8.dp
        NeonGlow -> 16.dp
        LiquidMarble -> 20.dp
        Holographic -> 4.dp
        GlitchTech -> 2.dp
        AuraEnergy -> 24.dp
        CircuitBoard -> 8.dp
    }
}

@Immutable
data class GridPoint(
    val column: Int,
    val row: Int,
) {
    operator fun plus(other: GridPoint): GridPoint =
        GridPoint(column = column + other.column, row = row + other.row)
}

@Immutable
data class GameConfig(
    val columns: Int = 10,
    val rows: Int = 12,
    val difficultyIntervalSeconds: Int = 18,
    val linesPerLevel: Int = 6,
) {
    companion object {
        fun default(gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle): GameConfig = when (gameplayStyle) {
            GameplayStyle.BlockWise -> GameConfig(columns = 8, rows = 10)
            GameplayStyle.StackShift -> GameConfig(columns = 10, rows = 12)
            GameplayStyle.ChainShift -> GameConfig(columns = 9, rows = 15, difficultyIntervalSeconds = 16, linesPerLevel = 9)
            GameplayStyle.MergeShift -> GameConfig(columns = 3, rows = 5)
            GameplayStyle.BoomBlocks -> GameConfig(columns = 6, rows = 8)
            GameplayStyle.BlockSort -> GameConfig(columns = 6, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999)
        }
    }
}

enum class GameMode {
    Classic,
    TimeAttack,
}

enum class GameplayStyle {
    StackShift,
    BlockWise,
    ChainShift,
    MergeShift,
    BoomBlocks,
    BlockSort,
}

fun GameplayStyle.storageKey(): String = when (this) {
    GameplayStyle.BlockSort -> "blocksort"
    else -> name.lowercase()
}

fun GameplayStyle.persistedKeys(): List<String> = listOf(storageKey())

fun gameplayStyleFromPersistedValue(raw: String?): GameplayStyle? {
    val normalized = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
    return GameplayStyle.entries.firstOrNull { style ->
        normalized.equals(style.name, ignoreCase = true) ||
            normalized.equals(style.storageKey(), ignoreCase = true)
    }
}

enum class GameStatus {
    Running,
    Paused,
    GameOver,
}

enum class SpecialBlockType {
    None,
    ColumnClearer,
    RowClearer,
    Ghost,
    Heavy,
}

enum class PressureLevel {
    Calm,
    Warning,
    Critical,
    Overflow,
}

enum class FeedbackEmphasis {
    Info,
    Bonus,
    Danger,
}

enum class GameTextKey {
    AppTitle,
    Hold,
    Restart,
    RestartConfirmTitle,
    RestartConfirmBody,
    RestartConfirm,
    RestartCancel,
    Score,
    HighScore,
    Lines,
    Boost,
    Danger,
    DangerNone,
    Launch,
    LaunchBar,
    LaunchBoostActive,
    LaunchSpecialChance,
    LaunchSoftLockMessage,
    LaunchChainMessage,
    LaunchGameOver,
    LaunchDragHint,
    LaunchDragHintBlockWise,
    QueueHold,
    QueueNextShort,
    QueueEmpty,
    GameOverTitle,
    Continue,
    GameOverExtraLife,
    GameOverExtraLifeLoading,
    PlayAgain,
    GameOverNewHighScore,
    GameMessageSelectColumn,
    GameMessageNoOpening,
    GameMessageSoftLock,
    GameMessageOverflow,
    GameMessageSpecialChainBoard,
    GameMessagePressureGameOver,
    GameMessageSpecialLines,
    GameMessageSpecialTriggered,
    GameMessagePerfectDrop,
    GameMessageChainLines,
    GameMessageLinesCleared,
    GameMessageGoodShot,
    GameMessageHoldUpdated,
    GameMessageTempoCritical,
    GameMessageTempoUp,
    GameMessageExtraLifeUsed,
    FeedbackOverflow,
    FeedbackPerfectLane,
    FeedbackMicroAdjust,
    FeedbackSoftLock,
    FeedbackHoldArmed,
    FeedbackSwap,
    FeedbackSpecialChain,
    FeedbackSpecial,
    FeedbackPerfect,
    FeedbackChain,
    FeedbackClear,
    FeedbackScoreOnly,
    FeedbackExtraLife,
    SpecialColumnClearer,
    SpecialRowClearer,
    SpecialGhost,
    SpecialHeavy,
    PiecePropertiesNone,
    GameMessageAdRewardBlockWise,
    GameMessageAdRewardMergeShift,
    GameMessageAdRewardBoomBlocks,
    GameMessageAdRewardStackShift,
    FeedbackAdRewardBlockWise,
    FeedbackAdRewardMergeShift,
    FeedbackAdRewardBoomBlocks,
}

@Immutable
data class GameText(
    val key: GameTextKey,
    val args: List<String> = emptyList(),
)

enum class CellTone {
    Cyan,
    Gold,
    Violet,
    Emerald,
    Coral,
    Blue,
    Rose,
    Lime,
    Amber,
}

enum class PieceKind(
    val unlockLevel: Int,
    val template: List<GridPoint>,
) {
    Domino(
        unlockLevel = 1,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
        ),
    ),
    Single(
        unlockLevel = 1,
        template = listOf(
            GridPoint(0, 0),
        ),
    ),
    TriL(
        unlockLevel = 1,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
    ),
    Square(
        unlockLevel = 1,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
    ),
    T(
        unlockLevel = 2,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(1, 1),
        ),
    ),
    L(
        unlockLevel = 2,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(0, 1),
            GridPoint(0, 2),
            GridPoint(1, 2),
        ),
    ),
    J(
        unlockLevel = 2,
        template = listOf(
            GridPoint(1, 0),
            GridPoint(1, 1),
            GridPoint(1, 2),
            GridPoint(0, 2),
        ),
    ),
    S(
        unlockLevel = 3,
        template = listOf(
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
    ),
    Z(
        unlockLevel = 3,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(1, 1),
            GridPoint(2, 1),
        ),
    ),
    I(
        unlockLevel = 4,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(3, 0),
        ),
    ),
    Plus(
        unlockLevel = 5,
        template = listOf(
            GridPoint(1, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
            GridPoint(2, 1),
            GridPoint(1, 2),
        ),
    ),
    Square3x3(
        unlockLevel = 5,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
            GridPoint(2, 1),
            GridPoint(0, 2),
            GridPoint(1, 2),
            GridPoint(2, 2),
        ),
    ),
}

@Immutable
data class Piece(
    val id: Long,
    val kind: PieceKind,
    val tone: CellTone,
    val cells: List<GridPoint>,
    val width: Int,
    val height: Int,
    val special: SpecialBlockType = SpecialBlockType.None,
    val value: Int = 0,
) {
    fun cellsAt(anchor: GridPoint): List<GridPoint> = cells.map(anchor::plus)
}

@Immutable
data class ComboState(
    val chain: Int = 0,
    val best: Int = 0,
) {
    val multiplier: Int
        get() = max(1, chain)
}

@Immutable
data class ColumnPressure(
    val column: Int,
    val filledCells: Int,
    val fillRatio: Float,
    val level: PressureLevel,
)

@Immutable
data class LaunchBarState(
    val progress: Float = 0f,
    val boostTurnsRemaining: Int = 0,
    val lastGain: Float = 0f,
) {
    val isBoostActive: Boolean get() = boostTurnsRemaining > 0
    val specialPieceChance: Float get() = (0.06f + (progress * 0.18f) + if (isBoostActive) 0.18f else 0f).coerceAtMost(0.55f)
}

@Immutable
data class SoftLockState(
    val pieceId: Long,
    val preview: PlacementPreview,
    val remainingMillis: Long,
    val revision: Long,
)

@Immutable
data class FloatingFeedback(
    val text: GameText,
    val emphasis: FeedbackEmphasis,
    val token: Long,
)

@Immutable
data class BoardCell(
    val tone: CellTone,
    val special: SpecialBlockType,
    val value: Int = 0,
)

@Immutable
class BoardMatrix private constructor(
    val columns: Int,
    val rows: Int,
    private val cells: IntArray,
) {
    fun cellAt(column: Int, row: Int): BoardCell? {
        if (column !in 0 until columns || row !in 0 until rows) return null
        val encoded = cells[indexOf(column, row)]
        return encoded.takeIf { it != EMPTY_CELL }?.let(::decodeCell)
    }

    fun toneAt(column: Int, row: Int): CellTone? {
        return cellAt(column, row)?.tone
    }

    fun specialAt(column: Int, row: Int): SpecialBlockType? = cellAt(column, row)?.special

    fun isOccupied(column: Int, row: Int): Boolean = toneAt(column, row) != null

    fun occupiedPointsInRows(rowsToRead: Set<Int>): List<GridPoint> = buildList {
        rowsToRead.forEach { row ->
            if (row !in 0 until rows) return@forEach
            for (column in 0 until columns) {
                if (cells[indexOf(column, row)] != EMPTY_CELL) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    fun occupiedPointsInColumns(columnsToRead: Set<Int>): List<GridPoint> = buildList {
        columnsToRead.forEach { column ->
            if (column !in 0 until columns) return@forEach
            for (row in 0 until rows) {
                if (cells[indexOf(column, row)] != EMPTY_CELL) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    fun occupiedNeighborPoints(center: GridPoint, radius: Int = 1): List<GridPoint> = buildList {
        for (row in (center.row - radius)..(center.row + radius)) {
            if (row !in 0 until rows) continue
            for (column in (center.column - radius)..(center.column + radius)) {
                if (column !in 0 until columns) continue
                if (column == center.column && row == center.row) continue
                if (cells[indexOf(column, row)] != EMPTY_CELL) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    fun topOccupiedRow(column: Int): Int? {
        if (column !in 0 until columns) return null
        for (row in 0 until rows) {
            if (cells[indexOf(column, row)] != EMPTY_CELL) return row
        }
        return null
    }

    fun bottomOccupiedRow(column: Int): Int? {
        if (column !in 0 until columns) return null
        for (row in (rows - 1) downTo 0) {
            if (cells[indexOf(column, row)] != EMPTY_CELL) return row
        }
        return null
    }

    fun filledCellCount(column: Int): Int {
        if (column !in 0 until columns) return 0
        var count = 0
        for (row in 0 until rows) {
            if (cells[indexOf(column, row)] != EMPTY_CELL) count += 1
        }
        return count
    }

    fun columnHeight(column: Int): Int = filledCellCount(column)

    fun isColumnEmpty(column: Int): Boolean = topOccupiedRow(column) == null

    fun resize(newColumns: Int, newRows: Int): BoardMatrix {
        if (newColumns == columns && newRows == rows) return this
        val next = IntArray(newColumns * newRows) { EMPTY_CELL }
        for (r in 0 until minOf(rows, newRows)) {
            for (c in 0 until minOf(columns, newColumns)) {
                val oldIndex = r * columns + c
                val newIndex = r * newColumns + c
                next[newIndex] = cells[oldIndex]
            }
        }
        return BoardMatrix(newColumns, newRows, next)
    }

    val occupiedCount: Int by lazy {
        var count = 0
        for (i in cells.indices) {
            if (cells[i] != EMPTY_CELL) count++
        }
        count
    }

    fun isEmpty(): Boolean = occupiedCount == 0

    fun clearColumns(columnsToClear: Set<Int>): BoardMatrix {
        if (columnsToClear.isEmpty()) return this
        val next = cells.copyOf()
        columnsToClear.forEach { column ->
            if (column !in 0 until columns) return@forEach
            for (row in 0 until rows) {
                next[indexOf(column, row)] = EMPTY_CELL
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun clearPoints(points: Set<GridPoint>): BoardMatrix {
        if (points.isEmpty()) return this
        val next = cells.copyOf()
        points.forEach { point ->
            if (point.column !in 0 until columns || point.row !in 0 until rows) return@forEach
            next[indexOf(point.column, point.row)] = EMPTY_CELL
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun pushColumnsDown(columnsToPush: Set<Int>, protectedPoints: Set<GridPoint> = emptySet()): BoardMatrix {
        if (columnsToPush.isEmpty()) return this
        val next = cells.copyOf()
        val protectedLookup = protectedPoints.groupBy(GridPoint::column).mapValues { entry -> entry.value.map(GridPoint::row).toSet() }
        columnsToPush.forEach { column ->
            if (column !in 0 until columns) return@forEach
            val protectedRows = protectedLookup[column].orEmpty()
            for (row in rows - 2 downTo 0) {
                val currentIndex = indexOf(column, row)
                val belowIndex = indexOf(column, row + 1)
                if (row in protectedRows || (row + 1) in protectedRows) continue
                if (next[currentIndex] == EMPTY_CELL || next[belowIndex] != EMPTY_CELL) continue
                next[belowIndex] = next[currentIndex]
                next[currentIndex] = EMPTY_CELL
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun fill(
        points: List<GridPoint>,
        tone: CellTone,
        special: SpecialBlockType = SpecialBlockType.None,
        value: Int = 0,
    ): BoardMatrix {
        val next = cells.copyOf()
        val encoded = encodeCell(tone = tone, special = special, value = value)
        points.forEach { point ->
            next[indexOf(point.column, point.row)] = encoded
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun fullRows(): List<Int> = buildList {
        for (row in 0 until rows) {
            var isFull = true
            for (column in 0 until columns) {
                if (cells[indexOf(column, row)] == EMPTY_CELL) {
                    isFull = false
                    break
                }
            }
            if (isFull) add(row)
        }
    }

    fun fullColumns(): List<Int> = buildList {
        for (column in 0 until columns) {
            var isFull = true
            for (row in 0 until rows) {
                if (cells[indexOf(column, row)] == EMPTY_CELL) {
                    isFull = false
                    break
                }
            }
            if (isFull) add(column)
        }
    }

    fun clearLines(
        rowsToClear: Set<Int>,
        columnsToClear: Set<Int>,
    ): BoardMatrix {
        if (rowsToClear.isEmpty() && columnsToClear.isEmpty()) return this
        val next = cells.copyOf()
        rowsToClear.forEach { row ->
            if (row !in 0 until rows) return@forEach
            for (column in 0 until columns) {
                next[indexOf(column, row)] = EMPTY_CELL
            }
        }
        columnsToClear.forEach { column ->
            if (column !in 0 until columns) return@forEach
            for (row in 0 until rows) {
                next[indexOf(column, row)] = EMPTY_CELL
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun clearRows(rowsToClear: Set<Int>): BoardMatrix {
        if (rowsToClear.isEmpty()) return this

        val next = IntArray(cells.size) { EMPTY_CELL }
        for (column in 0 until columns) {
            var clearedRowsAbove = 0
            for (sourceRow in 0 until rows) {
                if (sourceRow in rowsToClear) {
                    clearedRowsAbove += 1
                    continue
                }

                val cellValue = cells[indexOf(column, sourceRow)]
                if (cellValue == EMPTY_CELL) continue

                val targetRow = sourceRow - clearedRowsAbove
                next[indexOf(column, targetRow)] = cellValue
            }
        }

        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun applyGravityUp(): BoardMatrix {
        val next = IntArray(cells.size) { EMPTY_CELL }
        for (column in 0 until columns) {
            var targetRow = 0
            for (sourceRow in 0 until rows) {
                val cellValue = cells[indexOf(column, sourceRow)]
                if (cellValue != EMPTY_CELL) {
                    next[indexOf(column, targetRow)] = cellValue
                    targetRow++
                }
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun applyGravityDown(): BoardMatrix {
        val next = IntArray(cells.size) { EMPTY_CELL }
        for (column in 0 until columns) {
            var targetRow = rows - 1
            for (sourceRow in rows - 1 downTo 0) {
                val cellValue = cells[indexOf(column, sourceRow)]
                if (cellValue != EMPTY_CELL) {
                    next[indexOf(column, targetRow)] = cellValue
                    targetRow--
                }
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun applyGravityLeft(): BoardMatrix {
        val next = IntArray(cells.size) { EMPTY_CELL }
        for (row in 0 until rows) {
            var targetCol = 0
            for (sourceCol in 0 until columns) {
                val cellValue = cells[indexOf(sourceCol, row)]
                if (cellValue != EMPTY_CELL) {
                    next[indexOf(targetCol, row)] = cellValue
                    targetCol++
                }
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun applyGravityRight(): BoardMatrix {
        val next = IntArray(cells.size) { EMPTY_CELL }
        for (row in 0 until rows) {
            var targetCol = columns - 1
            for (sourceCol in columns - 1 downTo 0) {
                val cellValue = cells[indexOf(sourceCol, row)]
                if (cellValue != EMPTY_CELL) {
                    next[indexOf(targetCol, row)] = cellValue
                    targetCol--
                }
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    @Deprecated("Use applyGravityUp()", ReplaceWith("applyGravityUp()"))
    fun applyGravity(): BoardMatrix = applyGravityUp()

    private fun indexOf(column: Int, row: Int): Int = row * columns + column

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardMatrix) return false
        return columns == other.columns && rows == other.rows && cells.contentEquals(other.cells)
    }

    override fun hashCode(): Int {
        var result = columns
        result = 31 * result + rows
        result = 31 * result + cells.contentHashCode()
        return result
    }

    companion object {
        private const val EMPTY_CELL = -1
        private const val TONE_BITS = 8
        private const val SPECIAL_BITS = 8
        private const val SPECIAL_SHIFT = TONE_BITS
        private const val VALUE_SHIFT = TONE_BITS + SPECIAL_BITS

        fun empty(columns: Int, rows: Int): BoardMatrix =
            BoardMatrix(
                columns = columns,
                rows = rows,
                cells = IntArray(columns * rows) { EMPTY_CELL },
            )

        private fun encodeCell(
            tone: CellTone,
            special: SpecialBlockType,
            value: Int,
        ): Int = tone.ordinal or (special.ordinal shl SPECIAL_SHIFT) or (value shl VALUE_SHIFT)

        private fun decodeCell(encoded: Int): BoardCell = BoardCell(
            tone = CellTone.entries[encoded and 0xFF],
            special = SpecialBlockType.entries[(encoded shr SPECIAL_SHIFT) and 0xFF],
            value = (encoded shr VALUE_SHIFT),
        )
    }
}

@Immutable
data class PlacementPreview(
    val selectedColumn: Int,
    val entryAnchor: GridPoint,
    val landingAnchor: GridPoint,
    val occupiedCells: List<GridPoint>,
    val coveredColumns: IntRange,
    val isPerfectDrop: Boolean = false,
    val clearedRows: Set<Int> = emptySet(),
    val clearedColumns: Set<Int> = emptySet(),
)

@Immutable
data class GameState(
    val config: GameConfig,
    val gameMode: GameMode = GameMode.Classic,
    val gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
    val board: BoardMatrix,
    val activePiece: Piece?,
    val nextQueue: List<Piece>,
    val holdPiece: Piece? = null,
    val canHold: Boolean = true,
    val lastPlacementColumn: Int? = null,
    val score: Int,
    val lastMoveScore: Int = 0,
    val linesCleared: Int,
    val level: Int,
    val difficultyStage: Int,
    val secondsUntilDifficultyIncrease: Int,
    val combo: ComboState = ComboState(),
    val perfectDropStreak: Int = 0,
    val launchBar: LaunchBarState = LaunchBarState(),
    val columnPressure: List<ColumnPressure> = emptyList(),
    val softLock: SoftLockState? = null,
    val status: GameStatus = GameStatus.Running,
    val recentlyClearedRows: Set<Int> = emptySet(),
    val recentlyClearedColumns: Set<Int> = emptySet(),
    val recentlyMergedPoints: Set<GridPoint> = emptySet(),
    val recentlyExplodedPoints: Set<GridPoint> = emptySet(),
    val recentlyExplodedTones: Map<GridPoint, CellTone> = emptyMap(),
    val lastResolvedLines: Int = 0,
    val lastChainDepth: Int = 0,
    val specialChainCount: Int = 0,
    val clearAnimationToken: Long = 0L,
    val screenShakeToken: Long = 0L,
    val impactFlashToken: Long = 0L,
    val comboPopupToken: Long = 0L,
    val floatingFeedback: FloatingFeedback? = null,
    val feedbackToken: Long = 0L,
    val rewardedReviveUsed: Boolean = false,
    val nextPieceId: Long = 1L,
    val onboardingGuidePoint: GridPoint? = null,
    val lastActionTime: Long = 0L,
    val remainingTimeMillis: Long? = null,
    val message: GameText = GameText(GameTextKey.GameMessageSelectColumn),
    val activeChallenge: DailyChallenge? = null,
    val blockSortCompletedRoundBoard: BoardMatrix? = null,
    val blockSortLastMovedCellValues: Set<Int> = emptySet(),
    val blockSortBonusEmptyColumnUsed: Boolean = false,
    val blockSortScoredMoveSignatures: Set<String> = emptySet(),
) {
    val nextPiece: Piece?
        get() = nextQueue.firstOrNull()

    val trayPieces: List<Piece>
        get() = buildList {
            activePiece?.let(::add)
            addAll(nextQueue.take(2))
        }

    val isSoftLockActive: Boolean
        get() = softLock != null

    val criticalColumns: Set<Int>
        get() = columnPressure.filter { it.level == PressureLevel.Critical || it.level == PressureLevel.Overflow }.map(ColumnPressure::column).toSet()

    val isTimeAttack: Boolean
        get() = gameMode == GameMode.TimeAttack
}

fun gameText(
    key: GameTextKey,
    vararg args: Any,
): GameText = GameText(
    key = key,
    args = args.map(Any::toString),
)

fun GridPoint.toTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

fun formatMergeValue(value: Int): String {
    return when {
        value >= 1024 * 1024 -> "${value / (1024 * 1024)}M"
        value >= 1024 -> "${value / 1024}K"
        else -> value.toString()
    }
}

fun CellTone.color(): Color = paletteColor(BlockColorPalette.Classic, isDark = true)

fun CellTone.paletteColor(palette: BlockColorPalette, isDark: Boolean = true): Color = when (palette) {
    BlockColorPalette.Classic -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFF22D3EE)
            CellTone.Gold -> Color(0xFFFACC15)
            CellTone.Violet -> Color(0xFF8B5CF6)
            CellTone.Emerald -> Color(0xFF22C55E)
            CellTone.Coral -> Color(0xFFFB7185)
            CellTone.Blue -> Color(0xFF2563EB)
            CellTone.Rose -> Color(0xFFF472B6)
            CellTone.Lime -> Color(0xFF84CC16)
            CellTone.Amber -> Color(0xFFF59E0B)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF0891B2)
            CellTone.Gold -> Color(0xFFCA8A04)
            CellTone.Violet -> Color(0xFF7C3AED)
            CellTone.Emerald -> Color(0xFF059669)
            CellTone.Coral -> Color(0xFFE11D48)
            CellTone.Blue -> Color(0xFF1D4ED8)
            CellTone.Rose -> Color(0xFFDB2777)
            CellTone.Lime -> Color(0xFF4D7C0F)
            CellTone.Amber -> Color(0xFFB45309)
        }
    }

    BlockColorPalette.Candy -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFF6EE7F9)
            CellTone.Gold -> Color(0xFFFDE047)
            CellTone.Violet -> Color(0xFFC084FC)
            CellTone.Emerald -> Color(0xFF86EFAC)
            CellTone.Coral -> Color(0xFFFF8FAB)
            CellTone.Blue -> Color(0xFF60A5FA)
            CellTone.Rose -> Color(0xFFF9A8D4)
            CellTone.Lime -> Color(0xFFBEF264)
            CellTone.Amber -> Color(0xFFF9BA8F)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF06B6D4)
            CellTone.Gold -> Color(0xFFEAB308)
            CellTone.Violet -> Color(0xFF9333EA)
            CellTone.Emerald -> Color(0xFF10B981)
            CellTone.Coral -> Color(0xFFF43F5E)
            CellTone.Blue -> Color(0xFF3B82F6)
            CellTone.Rose -> Color(0xFFEC4899)
            CellTone.Lime -> Color(0xFF84CC16)
            CellTone.Amber -> Color(0xFFF59E0B)
        }
    }

    BlockColorPalette.Neon -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFF00F5FF)
            CellTone.Gold -> Color(0xFFFFE600)
            CellTone.Violet -> Color(0xFFB517FF)
            CellTone.Emerald -> Color(0xFF00FF85)
            CellTone.Coral -> Color(0xFFFF4D8D)
            CellTone.Blue -> Color(0xFF2962FF)
            CellTone.Rose -> Color(0xFFFF2FB3)
            CellTone.Lime -> Color(0xFFA3FF12)
            CellTone.Amber -> Color(0xFFFF9F1C)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF00CED1)
            CellTone.Gold -> Color(0xFFD4AF37)
            CellTone.Violet -> Color(0xFF9400D3)
            CellTone.Emerald -> Color(0xFF2E8B57)
            CellTone.Coral -> Color(0xFFCD5C5C)
            CellTone.Blue -> Color(0xFF0000FF)
            CellTone.Rose -> Color(0xFFC71585)
            CellTone.Lime -> Color(0xFF32CD32)
            CellTone.Amber -> Color(0xFFFF8C00)
        }
    }

    BlockColorPalette.Earth -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFF3C8DAD)
            CellTone.Gold -> Color(0xFFD4A373)
            CellTone.Violet -> Color(0xFF7B6D9C)
            CellTone.Emerald -> Color(0xFF5B8C5A)
            CellTone.Coral -> Color(0xFFC06C52)
            CellTone.Blue -> Color(0xFF4666C8)
            CellTone.Rose -> Color(0xFFA55C78)
            CellTone.Lime -> Color(0xFF92B84B)
            CellTone.Amber -> Color(0xFFB97A3D)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF2E6B82)
            CellTone.Gold -> Color(0xFFA67C52)
            CellTone.Violet -> Color(0xFF5D5276)
            CellTone.Emerald -> Color(0xFF446944)
            CellTone.Coral -> Color(0xFF90513E)
            CellTone.Blue -> Color(0xFF354D96)
            CellTone.Rose -> Color(0xFF7C455A)
            CellTone.Lime -> Color(0xFF6E8A38)
            CellTone.Amber -> Color(0xFF8B5C2E)
        }
    }

    BlockColorPalette.Monochrome -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFFF8FAFC)
            CellTone.Gold -> Color(0xFFE2E8F0)
            CellTone.Violet -> Color(0xFFCBD5E1)
            CellTone.Emerald -> Color(0xFF94A3B8)
            CellTone.Coral -> Color(0xFF64748B)
            CellTone.Blue -> Color(0xFF334155)
            CellTone.Rose -> Color(0xFF475569)
            CellTone.Lime -> Color(0xFF1E293B)
            CellTone.Amber -> Color(0xFF0F172A)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF0F172A)
            CellTone.Gold -> Color(0xFF1E293B)
            CellTone.Violet -> Color(0xFF334155)
            CellTone.Emerald -> Color(0xFF475569)
            CellTone.Coral -> Color(0xFF64748B)
            CellTone.Blue -> Color(0xFF94A3B8)
            CellTone.Rose -> Color(0xFFCBD5E1)
            CellTone.Lime -> Color(0xFFE2E8F0)
            CellTone.Amber -> Color(0xFFF8FAFC)
        }
    }

    BlockColorPalette.Aurora -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFF7DD3FC)
            CellTone.Gold -> Color(0xFFFDE68A)
            CellTone.Violet -> Color(0xFFA78BFA)
            CellTone.Emerald -> Color(0xFF5EEAD4)
            CellTone.Coral -> Color(0xFFF9A8D4)
            CellTone.Blue -> Color(0xFF3B82F6)
            CellTone.Rose -> Color(0xFFE9A8FF)
            CellTone.Lime -> Color(0xFFA7F3D0)
            CellTone.Amber -> Color(0xFFFBBF24)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF0284C7)
            CellTone.Gold -> Color(0xFFD97706)
            CellTone.Violet -> Color(0xFF7C3AED)
            CellTone.Emerald -> Color(0xFF0D9488)
            CellTone.Coral -> Color(0xFFDB2777)
            CellTone.Blue -> Color(0xFF2563EB)
            CellTone.Rose -> Color(0xFF9333EA)
            CellTone.Lime -> Color(0xFF059669)
            CellTone.Amber -> Color(0xFFB45309)
        }
    }

    BlockColorPalette.Sunset -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFFFF9F68)
            CellTone.Gold -> Color(0xFFFCD34D)
            CellTone.Violet -> Color(0xFFC084FC)
            CellTone.Emerald -> Color(0xFFFB923C)
            CellTone.Coral -> Color(0xFFFF5D73)
            CellTone.Blue -> Color(0xFF6366F1)
            CellTone.Rose -> Color(0xFFFF8FB1)
            CellTone.Lime -> Color(0xFFFBBF24)
            CellTone.Amber -> Color(0xFFEA580C)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFFD97706)
            CellTone.Gold -> Color(0xFFB45309)
            CellTone.Violet -> Color(0xFF9333EA)
            CellTone.Emerald -> Color(0xFFEA580C)
            CellTone.Coral -> Color(0xFFBE123C)
            CellTone.Blue -> Color(0xFF4338CA)
            CellTone.Rose -> Color(0xFFDB2777)
            CellTone.Lime -> Color(0xFFD97706)
            CellTone.Amber -> Color(0xFF9A3412)
        }
    }

    BlockColorPalette.SoftPastel -> if (isDark) {
        when (this) {
            CellTone.Cyan -> Color(0xFFB7E9F7)
            CellTone.Gold -> Color(0xFFF7E3A1)
            CellTone.Violet -> Color(0xFFD5C2FF)
            CellTone.Emerald -> Color(0xFFB7F0D1)
            CellTone.Coral -> Color(0xFFF8B4C7)
            CellTone.Blue -> Color(0xFFA9C7FF)
            CellTone.Rose -> Color(0xFFFBCFE8)
            CellTone.Lime -> Color(0xFFD9F99D)
            CellTone.Amber -> Color(0xFFF5C38B)
        }
    } else {
        when (this) {
            CellTone.Cyan -> Color(0xFF5AB9D1)
            CellTone.Gold -> Color(0xFFD1B55A)
            CellTone.Violet -> Color(0xFF8B5AD1)
            CellTone.Emerald -> Color(0xFF5AD18B)
            CellTone.Coral -> Color(0xFFD15A7C)
            CellTone.Blue -> Color(0xFF5A8BD1)
            CellTone.Rose -> Color(0xFFD15AB9)
            CellTone.Lime -> Color(0xFFB9D15A)
            CellTone.Amber -> Color(0xFFD18B5A)
        }
    }
}

fun normalizeBlockVisualStyle(style: BlockVisualStyle): BlockVisualStyle = style

fun boardSpecialIcon(type: SpecialBlockType): ImageVector = when (type) {
    SpecialBlockType.ColumnClearer -> Icons.Filled.SwapVert
    SpecialBlockType.RowClearer -> Icons.Filled.SwapHoriz
    SpecialBlockType.Ghost -> Icons.Filled.Layers
    SpecialBlockType.Heavy -> Icons.Filled.Hub
    SpecialBlockType.None -> Icons.AutoMirrored.Filled.HelpOutline
}
