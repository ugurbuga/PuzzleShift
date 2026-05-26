package com.ugurbuga.blockgames.ui.game.selection

import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppSelectionScreenCommonTest {

    @Test
    fun initialExpandedSelectionStyle_defaultsToStackShiftWhenNothingIsSelected() {
        assertEquals(GameplayStyle.StackShift, initialExpandedSelectionStyle(currentStyle = null))
    }

    @Test
    fun initialExpandedSelectionStyle_collapsesAllCardsWhenAnAppIsAlreadySelected() {
        assertEquals(null, initialExpandedSelectionStyle(currentStyle = GameplayStyle.MergeShift))
    }

    @Test
    fun selectionTone_mapsEachGameplayStyleToItsAccentTone() {
        assertEquals(CellTone.Cyan, GameplayStyle.StackShift.selectionTone())
        assertEquals(CellTone.Amber, GameplayStyle.BlockWise.selectionTone())
        assertEquals(CellTone.Violet, GameplayStyle.MergeShift.selectionTone())
        assertEquals(CellTone.Coral, GameplayStyle.BoomBlocks.selectionTone())
    }

    @Test
    fun mergeShiftPreferredColumnsForStep_cyclesEverySixSteps() {
        assertContentEquals(listOf(0, 2, 1), mergeShiftPreferredColumnsForStep(0))
        assertContentEquals(listOf(1, 2, 0), mergeShiftPreferredColumnsForStep(5))
        assertContentEquals(mergeShiftPreferredColumnsForStep(0), mergeShiftPreferredColumnsForStep(6))
    }

    @Test
    fun findPreferredColumnPlacement_triesPreferredColumnsBeforeFallbackColumns() {
        val logic = GameLogic.create(random = Random(42))
        val piece = dominoPiece()
        val state = logic.newGame(GameConfig(columns = 4, rows = 4)).copy(
            gameplayStyle = GameplayStyle.MergeShift,
            activePiece = piece,
            nextQueue = emptyList(),
        )

        val placement = findPreferredColumnPlacement(
            state = state,
            preferredColumns = listOf(2),
            previewProvider = { _, column ->
                when (column) {
                    0 -> previewAt(column)
                    2 -> null
                    else -> null
                }
            },
        )

        assertNotNull(placement)
        assertEquals(0, placement.first)
        assertEquals(GridPoint(0, 0), placement.second.landingAnchor)
    }

    @Test
    fun demoTrayFractions_accountForPieceSizeAndSlotCenter() {
        val piece = Piece(
            id = 1L,
            kind = PieceKind.Square,
            tone = CellTone.Gold,
            cells = listOf(
                GridPoint(0, 0),
                GridPoint(1, 0),
                GridPoint(0, 1),
                GridPoint(1, 1),
            ),
            width = 2,
            height = 2,
            special = SpecialBlockType.None,
        )

        assertEquals(0.08f, demoTrayLeftFraction(piece = piece, slotIndex = 0, columns = 10), absoluteTolerance = 0.0001f)
        assertEquals(1.01f, demoTrayTopFraction(piece = piece, rows = 10), absoluteTolerance = 0.0001f)
    }

    private fun dominoPiece(): Piece = Piece(
        id = 7L,
        kind = PieceKind.Domino,
        tone = CellTone.Cyan,
        cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
        width = 2,
        height = 1,
        special = SpecialBlockType.None,
    )

    private fun previewAt(column: Int): PlacementPreview = PlacementPreview(
        selectedColumn = column,
        entryAnchor = GridPoint(column, 0),
        landingAnchor = GridPoint(column, 0),
        occupiedCells = listOf(GridPoint(column, 0), GridPoint(column + 1, 0)),
        coveredColumns = column..(column + 1),
    )
}

