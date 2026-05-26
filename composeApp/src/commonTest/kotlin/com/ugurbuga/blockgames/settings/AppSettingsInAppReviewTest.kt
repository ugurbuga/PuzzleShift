package com.ugurbuga.blockgames.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsInAppReviewTest {
    @Test
    fun recordGameplayDurationAccumulatesPositiveDuration() {
        val updated = AppSettings(totalGameplayDurationMillis = 2_000L)
            .recordGameplayDuration(3_500L)

        assertEquals(5_500L, updated.totalGameplayDurationMillis)
    }

    @Test
    fun sanitizedClampsNegativeGameplayDuration() {
        val sanitized = AppSettings(totalGameplayDurationMillis = -25L).sanitized()

        assertEquals(0L, sanitized.totalGameplayDurationMillis)
    }

    @Test
    fun inAppReviewEligibilityRequiresThresholdAndSingleRequest() {
        val eligible = AppSettings(
            totalGameplayDurationMillis = InAppReviewPromptThresholdMillis,
        )

        assertTrue(eligible.isEligibleForInAppReview())
        assertFalse(eligible.markInAppReviewRequested().isEligibleForInAppReview())
    }
}

