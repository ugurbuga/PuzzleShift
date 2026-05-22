package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStage
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStateFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChainShiftGameLogicTest {

    private val logic = ChainShiftGameLogic(random = Random(0), scoreCalculator = ScoreCalculator())

    @Test
    fun newGame_usesChainShiftDefaults() {
        val state = logic.newGame(config = GameConfig.default(GameplayStyle.ChainShift), challenge = null, mode = com.ugurbuga.blockgames.game.model.GameMode.Classic)

        assertEquals(GameplayStyle.ChainShift, state.gameplayStyle)
        assertEquals(9, state.config.columns)
        assertEquals(15, state.config.rows)
        assertNotNull(state.activePiece)
        assertTrue(chainLength(state) >= 6)
    }

    @Test
    fun placement_canCreateMatchAndIncreaseScore() {
        val scene = ChainShiftOnboardingStateFactory.scene(ChainShiftOnboardingStage.CreateMatch)
        val activePiece = scene.gameState.activePiece ?: error("expected active piece")

        val preview = logic.previewPlacement(scene.gameState, activePiece.id, scene.guidePoint)
        assertNotNull(preview)

        val result = logic.placePiece(scene.gameState, activePiece.id, scene.guidePoint)

        assertTrue(result.state.score > scene.gameState.score)
        assertTrue(result.state.recentlyExplodedPoints.isNotEmpty())
    }

    @Test
    fun tick_advancesChainTowardCenter() {
        val state = logic.newGame(config = GameConfig.default(GameplayStyle.ChainShift), challenge = null, mode = com.ugurbuga.blockgames.game.model.GameMode.Classic)

        val nextState = logic.tick(state)

        assertTrue(chainLength(nextState) >= chainLength(state))
    }

    @Test
    fun placePiece_canTargetEmptyFutureTrackCell() {
        val state = logic.newGame(config = GameConfig.default(GameplayStyle.ChainShift), challenge = null, mode = com.ugurbuga.blockgames.game.model.GameMode.Classic)
        val activePiece = state.activePiece ?: error("expected active piece")
        val path = ChainShiftPath.spiral(state.config)
        val target = path[12]

        assertNull(state.board.cellAt(target.column, target.row))

        val preview = logic.previewPlacement(state, activePiece.id, target)
        assertNotNull(preview)
        assertEquals(target, preview.landingAnchor)

        val result = logic.placePiece(state, activePiece.id, target)

        assertEquals(activePiece.tone, result.state.board.cellAt(target.column, target.row)?.tone)
    }

    @Test
    fun placePiece_resetsLastActionTimeAfterSuccessfulShot() {
        val initialState = logic.newGame(
            config = GameConfig.default(GameplayStyle.ChainShift),
            challenge = null,
            mode = com.ugurbuga.blockgames.game.model.GameMode.Classic,
        )
        val state = initialState.copy(lastActionTime = 0L)
        val activePiece = state.activePiece ?: error("expected active piece")
        val origin = ChainShiftPath.spiral(state.config).first()

        val result = logic.placePiece(state, activePiece.id, origin)

        assertTrue(result.state.lastActionTime > 0L)
    }

    private fun chainLength(state: com.ugurbuga.blockgames.game.model.GameState): Int {
        val path = ChainShiftPath.spiral(state.config)
        return path.takeWhile { point -> state.board.cellAt(point.column, point.row) != null }.size
    }
}

