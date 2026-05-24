package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.interactive_onboarding_sumshift_column_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_sumshift_finish_hint
import blockgames.composeapp.generated.resources.interactive_onboarding_sumshift_row_hint
import com.ugurbuga.blockgames.game.logic.SumShiftGameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import org.jetbrains.compose.resources.StringResource

@Immutable
enum class SumShiftOnboardingStage : OnboardingStage {
    MatchRow,
    MatchColumn,
    FinishPuzzle,
}

@Immutable
data class SumShiftOnboardingScene(
    val stage: SumShiftOnboardingStage,
    val gameState: GameState,
    val hintRes: StringResource,
    val requiredSelection: Set<GridPoint>,
    val advanceWhenSolved: Boolean = false,
) {
    fun remainingRequiredSelection(currentSelection: Set<GridPoint>): Set<GridPoint> =
        requiredSelection.filterTo(linkedSetOf()) { it !in currentSelection }
}

object SumShiftOnboardingStateFactory {
    private val onboardingConfig = GameConfig(
        columns = 4,
        rows = 4,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<SumShiftOnboardingStage> = listOf(
        SumShiftOnboardingStage.MatchRow,
        SumShiftOnboardingStage.MatchColumn,
        SumShiftOnboardingStage.FinishPuzzle,
    )

    private val sceneCache: Map<SumShiftOnboardingStage, SumShiftOnboardingScene> =
        stages.associateWith(::buildScene)

    fun initialState(): GameState = scene(stages.first()).gameState

    fun cleanGameState(): GameState = SumShiftGameLogic().newGame(
        config = GameConfig.default(GameplayStyle.SumShift),
        challenge = null,
        mode = com.ugurbuga.blockgames.game.model.GameMode.Classic,
    )

    fun scene(stage: SumShiftOnboardingStage): SumShiftOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: SumShiftOnboardingStage): SumShiftOnboardingScene = when (stage) {
        SumShiftOnboardingStage.MatchRow -> {
            val values = listOf(
                listOf(2, 3, 8, 9),
                listOf(6, 5, 7, 4),
                listOf(7, 8, 6, 5),
                listOf(4, 9, 5, 7),
            )
            val baseSelection = setOf(GridPoint(0, 0))
            val requiredSelection = setOf(GridPoint(1, 0))
            SumShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    values = values,
                    solution = setOf(
                        GridPoint(0, 0),
                        GridPoint(1, 0),
                        GridPoint(3, 1),
                    ),
                    initiallySelected = baseSelection,
                ),
                hintRes = Res.string.interactive_onboarding_sumshift_row_hint,
                requiredSelection = requiredSelection,
            )
        }

        SumShiftOnboardingStage.MatchColumn -> {
            val values = listOf(
                listOf(8, 6, 3, 7),
                listOf(7, 5, 2, 8),
                listOf(9, 4, 6, 5),
                listOf(4, 7, 8, 1),
            )
            val baseSelection = setOf(GridPoint(2, 0))
            val requiredSelection = setOf(GridPoint(2, 1))
            SumShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    values = values,
                    solution = setOf(
                        GridPoint(2, 0),
                        GridPoint(2, 1),
                        GridPoint(0, 3),
                    ),
                    initiallySelected = baseSelection,
                ),
                hintRes = Res.string.interactive_onboarding_sumshift_column_hint,
                requiredSelection = requiredSelection,
            )
        }

        SumShiftOnboardingStage.FinishPuzzle -> {
            val values = listOf(
                listOf(2, 8, 7, 4),
                listOf(9, 6, 3, 8),
                listOf(7, 5, 4, 9),
                listOf(8, 7, 6, 5),
            )
            val solution = setOf(
                GridPoint(0, 0),
                GridPoint(2, 2),
                GridPoint(2, 1),
            )
            val baseSelection = setOf(
                GridPoint(0, 0),
                GridPoint(2, 1),
            )
            val requiredSelection = setOf(GridPoint(2, 2))
            SumShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    values = values,
                    solution = solution,
                    initiallySelected = baseSelection,
                ),
                hintRes = Res.string.interactive_onboarding_sumshift_finish_hint,
                requiredSelection = requiredSelection,
            )
        }
    }

    private fun scriptedState(
        values: List<List<Int>>,
        solution: Set<GridPoint>,
        initiallySelected: Set<GridPoint> = emptySet(),
    ): GameState {
        val rows = values.size
        val columns = values.firstOrNull()?.size ?: 5
        var board = BoardMatrix.empty(columns = columns, rows = rows)
        values.forEachIndexed { rowIndex, rowValues ->
            rowValues.forEachIndexed { columnIndex, value ->
                board = board.fill(
                    points = listOf(GridPoint(columnIndex, rowIndex)),
                    tone = onboardingToneAt(columnIndex, rowIndex),
                    value = value,
                )
            }
        }
        val rowTargets = List(rows) { rowIndex ->
            values[rowIndex].mapIndexed { columnIndex, value ->
                if (GridPoint(columnIndex, rowIndex) in solution) value else 0
            }.sum()
        }
        val columnTargets = List(columns) { columnIndex ->
            values.indices.sumOf { rowIndex ->
                if (GridPoint(columnIndex, rowIndex) in solution) values[rowIndex][columnIndex] else 0
            }
        }
        return GameState(
            config = onboardingConfig.copy(columns = columns, rows = rows),
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
            status = GameStatus.Running,
            sumShiftRowTargets = rowTargets,
            sumShiftColumnTargets = columnTargets,
            sumShiftSelectedCells = initiallySelected,
            sumShiftManualDisabledCells = emptySet(),
            sumShiftMistakesUsed = 0,
        )
    }
}

private fun onboardingToneAt(column: Int, row: Int): CellTone = listOf(
    CellTone.Cyan,
    CellTone.Violet,
    CellTone.Emerald,
    CellTone.Gold,
    CellTone.Blue,
).let { tones ->
    tones[(column + row) % tones.size]
}

