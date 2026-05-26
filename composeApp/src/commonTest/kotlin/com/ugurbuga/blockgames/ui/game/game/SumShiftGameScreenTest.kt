package com.ugurbuga.blockgames.ui.game.game

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.settings.SumShiftOnboardingStateFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SumShiftGameScreenTest {
    @Test
    fun systemDisabledSumShiftCells_includesCompletedRowsAndColumns() {
        var board = BoardMatrix.empty(columns = 3, rows = 3)
        val values = listOf(
            listOf(2, 3, 1),
            listOf(5, 4, 2),
            listOf(6, 1, 7),
        )
        values.forEachIndexed { row, rowValues ->
            rowValues.forEachIndexed { column, value ->
                board = board.fill(
                    points = listOf(GridPoint(column, row)),
                    tone = CellTone.Cyan,
                    value = value,
                )
            }
        }

        val state = GameState(
            config = GameConfig(columns = 3, rows = 3, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            gameplayStyle = GameplayStyle.SumShift,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            holdPiece = null,
            canHold = false,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 9_999,
            sumShiftRowTargets = listOf(2, 99, 99),
            sumShiftColumnTargets = listOf(99, 4, 99),
            sumShiftSelectedCells = setOf(
                GridPoint(0, 0),
                GridPoint(1, 1),
            ),
        )

        assertEquals(
            setOf(
                GridPoint(1, 0),
                GridPoint(2, 0),
                GridPoint(1, 2),
            ),
            state.systemDisabledSumShiftCells(),
        )
    }

    @Test
    fun selectableSumShiftSums_excludeCurrentlyDisabledCells() {
        var board = BoardMatrix.empty(columns = 3, rows = 3)
        val values = listOf(
            listOf(2, 3, 1),
            listOf(5, 4, 2),
            listOf(6, 1, 7),
        )
        values.forEachIndexed { row, rowValues ->
            rowValues.forEachIndexed { column, value ->
                board = board.fill(
                    points = listOf(GridPoint(column, row)),
                    tone = CellTone.Cyan,
                    value = value,
                )
            }
        }

        val state = GameState(
            config = GameConfig(columns = 3, rows = 3, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
            gameplayStyle = GameplayStyle.SumShift,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            holdPiece = null,
            canHold = false,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 9_999,
            sumShiftRowTargets = listOf(2, 99, 99),
            sumShiftColumnTargets = listOf(99, 4, 99),
            sumShiftSelectedCells = setOf(
                GridPoint(0, 0),
                GridPoint(1, 1),
            ),
            sumShiftManualDisabledCells = setOf(GridPoint(2, 2)),
        )

        val disabledCells = state.systemDisabledSumShiftCells() + state.sumShiftManualDisabledCells

        assertEquals(2, state.selectableSumShiftRowSum(rowIndex = 0, disabledCells = disabledCells))
        assertEquals(13, state.selectableSumShiftColumnSum(columnIndex = 0, disabledCells = disabledCells))
        assertEquals(2, state.selectableSumShiftColumnSum(columnIndex = 2, disabledCells = disabledCells))
    }

    @Test
    fun sumShiftCellIsEnabled_keepsManualDisabledCellsClickableInDisableMode() {
        assertTrue(
            sumShiftCellIsEnabled(
                controlsEnabled = true,
                disabled = true,
                manualDisabled = true,
            )
        )
        assertFalse(
            sumShiftCellIsEnabled(
                controlsEnabled = true,
                disabled = true,
                manualDisabled = false,
            )
        )
        assertFalse(
            sumShiftCellIsEnabled(
                controlsEnabled = false,
                disabled = false,
                manualDisabled = false,
            )
        )
    }

    @Test
    fun onboardingScenes_exposeSingleGuidedMove() {
        SumShiftOnboardingStateFactory.stages.forEach { stage ->
            val scene = SumShiftOnboardingStateFactory.scene(stage)
            val guidedCells = scene.remainingRequiredSelection(scene.gameState.sumShiftSelectedCells)

            assertEquals(4, scene.gameState.config.columns)
            assertEquals(4, scene.gameState.config.rows)
            assertEquals(1, scene.requiredSelection.size)
            assertEquals(scene.requiredSelection, guidedCells)
        }
    }

    @Test
    fun allowsGuidedTap_blocksNonRequiredCellsDuringOnboarding() {
        val scene = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStateFactory.stages.first())
        val requiredPoint = scene.requiredSelection.single()
        val blockedPoint = GridPoint(3, 3)

        assertTrue(scene.allowsGuidedTap(requiredPoint, scene.gameState.sumShiftSelectedCells))
        assertNotEquals(requiredPoint, blockedPoint)
        assertFalse(scene.allowsGuidedTap(blockedPoint, scene.gameState.sumShiftSelectedCells))
    }

    @Test
    fun findSumShiftHintPoint_returnsRemainingCorrectMoveForOnboardingScene() {
        val scene = SumShiftOnboardingStateFactory.scene(SumShiftOnboardingStateFactory.stages.first())

        assertEquals(scene.requiredSelection.single(), scene.gameState.findSumShiftHintPoint())
    }

    @Test
    fun shouldShowSumShiftCompletionToast_showsForSolvedBoardAndSolvedPreparationState() {
        assertTrue(
            shouldShowSumShiftCompletionToast(
                isSolvedBoard = true,
                isPreparingBoard = false,
                shouldShowFullPreparationCard = false,
            )
        )
        assertTrue(
            shouldShowSumShiftCompletionToast(
                isSolvedBoard = false,
                isPreparingBoard = true,
                shouldShowFullPreparationCard = false,
            )
        )
    }

    @Test
    fun shouldShowSumShiftCompletionToast_hidesForInitialFullPreparation() {
        assertFalse(
            shouldShowSumShiftCompletionToast(
                isSolvedBoard = false,
                isPreparingBoard = true,
                shouldShowFullPreparationCard = true,
            )
        )
    }
}

