package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.FluffyBlockCollector
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.GridPoint

object FluffyBlockOnboardingStateFactory {
    val stages: List<OnboardingStage> = emptyList()

    fun initialState(): GameState {
        val style = GameplayStyle.FluffyBlock
        val board = BoardMatrix.empty(columns = 10, rows = 16)
            .fill(listOf(GridPoint(0, 0), GridPoint(1, 0)), CellTone.Cyan)
            .fill(listOf(GridPoint(4, 2), GridPoint(5, 2), GridPoint(4, 3)), CellTone.Gold)
            .fill(listOf(GridPoint(2, 5)), CellTone.Violet)

        val collectors = listOf(
            FluffyBlockCollector(1, CellTone.Cyan, PieceKind.Square, GridPoint(0, 0), remainingCapacity = 10),
            FluffyBlockCollector(2, CellTone.Gold, PieceKind.L, GridPoint(4, 2), remainingCapacity = 10),
            FluffyBlockCollector(3, CellTone.Violet, PieceKind.TriL, GridPoint(2, 5), remainingCapacity = 10)
        )
        
        return GameState(
            config = GameConfig(columns = 10, rows = 16),
            gameplayStyle = style,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 18,
            nextPieceId = 4,
            fluffyBlockCollectors = collectors
        )
    }

    fun cleanGameState(): GameState = initialState()
}
