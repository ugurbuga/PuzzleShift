package com.ugurbuga.blockgames.ui.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardGridCommonTest {

    @Test
    fun neonGlowTubeSpecs_returnsThreeConsistentTubes() {
        val specs = neonGlowTubeSpecs()

        assertEquals(3, specs.size)
        assertEquals(0.09f, specs.first().insetFactor)
        assertEquals(4.0f, specs.first().strokeWidthDp)
        assertEquals(0.27f, specs.last().insetFactor)
        assertEquals(4.0f, specs.last().strokeWidthDp)

        assertTrue(specs.zipWithNext().all { (outer, inner) -> outer.insetFactor < inner.insetFactor })
        assertTrue(specs.all { it.strokeWidthDp == 4.0f })
    }
}
