package com.ugurbuga.blockgames.ui.game.settings

import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsScreenCommonTest {

    @Test
    fun unlockRequest_reportsAffordabilityFromTokenBalance() {
        val affordable = unlockRequest(
            label = "Crystal",
            priceTokens = 120,
            currentBalance = 120,
            onUnlock = { it },
        )
        val expensive = unlockRequest(
            label = "Pixel",
            priceTokens = 121,
            currentBalance = 120,
            onUnlock = { AppSettings() },
        )

        assertTrue(affordable.canAfford)
        assertFalse(expensive.canAfford)
    }

    @Test
    fun visibleBlockStyles_matchesCuratedOrderAndOmitsHiddenStyles() {
        val styles = visibleBlockStyles()

        assertEquals(BlockVisualStyle.Flat, styles.first())
        assertEquals(BlockVisualStyle.LightBurst, styles.last())
        assertEquals(26, styles.size)
        assertTrue(BlockVisualStyle.DynamicLiquid in styles)
        assertTrue(BlockVisualStyle.Prism in styles)
        assertFalse(styles.any { it.name == "Electric" })
        assertFalse(styles.any { it.name == "Lava" })
    }
}

