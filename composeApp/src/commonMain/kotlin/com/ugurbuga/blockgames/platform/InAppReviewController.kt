package com.ugurbuga.blockgames.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface InAppReviewController {
    val isSupported: Boolean

    fun launchReviewRequest(onComplete: (Boolean) -> Unit = {})
}

object NoOpInAppReviewController : InAppReviewController {
    override val isSupported: Boolean = false

    override fun launchReviewRequest(onComplete: (Boolean) -> Unit) {
        onComplete(false)
    }
}

@Composable
expect fun rememberInAppReviewController(): InAppReviewController

