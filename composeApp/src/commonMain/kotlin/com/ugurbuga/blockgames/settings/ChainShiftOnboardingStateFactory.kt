package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.ChainShiftPath
import com.ugurbuga.blockgames.game.logic.ChainShiftGameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText

@Immutable
data class ChainShiftOnboardingScene(
    val stage: ChainShiftOnboardingStage,
    val gameState: GameState,
    val guidePoint: GridPoint,
)

@Immutable
enum class ChainShiftOnboardingStage : OnboardingStage {
    FirstInsert,
    CreateMatch,
    TriggerChain,
}

object ChainShiftOnboardingStateFactory {
    private val logic = ChainShiftGameLogic()
    private val config = GameConfig(
        columns = 9,
        rows = 15,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<ChainShiftOnboardingStage> = listOf(
        ChainShiftOnboardingStage.FirstInsert,
        ChainShiftOnboardingStage.CreateMatch,
        ChainShiftOnboardingStage.TriggerChain,
    )

    private val sceneCache = stages.associateWith(::buildScene)

    fun initialState(): GameState = scene(stages.first()).gameState

    fun cleanGameState(): GameState = logic.newGame(config = config)

    fun scene(stage: ChainShiftOnboardingStage): ChainShiftOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: ChainShiftOnboardingStage): ChainShiftOnboardingScene {
        val path = ChainShiftPath.spiral(config)
        return when (stage) {
            ChainShiftOnboardingStage.FirstInsert -> {
                val guidePoint = path[2]
                ChainShiftOnboardingScene(
                    stage = stage,
                    guidePoint = guidePoint,
                    gameState = scriptedState(
                        chainTones = listOf(
                            CellTone.Cyan,
                            CellTone.Gold,
                            CellTone.Violet,
                            CellTone.Emerald,
                            CellTone.Gold,
                            CellTone.Coral,
                        ),
                        activeTone = CellTone.Emerald,
                        guidePoint = guidePoint,
                    ),
                )
            }

            ChainShiftOnboardingStage.CreateMatch -> {
                val guidePoint = path[0]
                ChainShiftOnboardingScene(
                    stage = stage,
                    guidePoint = guidePoint,
                    gameState = scriptedState(
                        chainTones = listOf(
                            CellTone.Gold,
                            CellTone.Cyan,
                            CellTone.Cyan,
                            CellTone.Violet,
                            CellTone.Emerald,
                            CellTone.Coral,
                        ),
                        activeTone = CellTone.Cyan,
                        guidePoint = guidePoint,
                    ),
                )
            }

            ChainShiftOnboardingStage.TriggerChain -> {
                val guidePoint = path[1]
                ChainShiftOnboardingScene(
                    stage = stage,
                    guidePoint = guidePoint,
                    gameState = scriptedState(
                        chainTones = listOf(
                            CellTone.Gold,
                            CellTone.Gold,
                            CellTone.Cyan,
                            CellTone.Cyan,
                            CellTone.Gold,
                            CellTone.Gold,
                            CellTone.Violet,
                        ),
                        activeTone = CellTone.Cyan,
                        guidePoint = guidePoint,
                    ),
                )
            }
        }
    }

    private fun scriptedState(
        chainTones: List<CellTone>,
        activeTone: CellTone,
        guidePoint: GridPoint,
    ): GameState {
        val path = ChainShiftPath.spiral(config)
        var board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
        chainTones.forEachIndexed { index, tone ->
            board = board.fill(
                points = listOf(path[index]),
                tone = tone,
                special = SpecialBlockType.None,
                value = index + 1,
            )
        }
        return GameState(
            config = config,
            gameplayStyle = GameplayStyle.ChainShift,
            board = board,
            activePiece = singlePiece(id = 1000L, tone = activeTone),
            nextQueue = listOf(
                singlePiece(id = 1001L, tone = CellTone.Emerald),
                singlePiece(id = 1002L, tone = CellTone.Violet),
            ),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            status = GameStatus.Running,
            onboardingGuidePoint = guidePoint,
            message = gameText(GameTextKey.GameMessageSelectColumn),
            nextPieceId = 1003L,
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
}

