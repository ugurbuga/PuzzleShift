package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GridPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FluffyBlockGameLogicTest {

    private val logic = FluffyBlockGameLogic()

    @Test
    fun testNewGame() {
        val state = logic.newGame()
        assertEquals(10, state.config.columns)
        assertEquals(16, state.config.rows)
        assertTrue(state.board.occupiedCount > 0)
        assertTrue(state.fluffyBlockCollectors.isNotEmpty())
    }

    @Test
    fun testEatingMatchingColor() {
        val state = logic.newGame()
        val collector = state.fluffyBlockCollectors.first()
        val targetTone = collector.tone
        val targetPoint = collector.cells.first() // Use a point that the collector actually covers
        
        // Setup board: clear board and fill a covered point with matching color
        var board = BoardMatrix.empty(state.config.columns, state.config.rows)
        board = board.fill(listOf(targetPoint), targetTone)
        
        // Use a state with ONLY this collector to avoid overlap issues in test
        val currentState = state.copy(board = board, fluffyBlockCollectors = listOf(collector))
        
        // "Move" collector to its current position to trigger eating
        val result = logic.placePiece(currentState, collector.id, collector.anchor)
        
        assertTrue(result.state.board.cellAt(targetPoint.column, targetPoint.row) == null, "Block at targetPoint should be eaten")
        assertTrue(result.state.score > currentState.score, "Score should increase after eating")
        assertEquals(collector.collectedCount + 1, result.state.fluffyBlockCollectors.first().collectedCount)
    }

    @Test
    fun testMovementRestrictions() {
        val state = logic.newGame()
        val collector = state.fluffyBlockCollectors.first()
        val otherTone = CellTone.entries.first { it != collector.tone }
        
        // Setup board with a DIFFERENT color block at a position that will be covered by the move
        val neighbor = GridPoint(collector.anchor.column + 1, collector.anchor.row)
        val blockedPoint = collector.kind.template.first() + neighbor
        
        var board = BoardMatrix.empty(state.config.columns, state.config.rows)
        board = board.fill(listOf(blockedPoint), otherTone)
        
        // Use a state with ONLY this collector
        val currentState = state.copy(board = board, fluffyBlockCollectors = listOf(collector))
        
        // Try to move collector onto the wrong colored block
        val result = logic.placePiece(currentState, collector.id, neighbor)
        
        // Should be rejected (returns old state)
        assertEquals(currentState.fluffyBlockCollectors.first().anchor, result.state.fluffyBlockCollectors.first().anchor)
    }
}
