package com.ugurbuga.blockgames.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory

private class AndroidInAppReviewController(
    private val activityProvider: () -> Activity?,
) : InAppReviewController {
    override val isSupported: Boolean
        get() = activityProvider() != null

    override fun launchReviewRequest(onComplete: (Boolean) -> Unit) {
        val activity = activityProvider() ?: run {
            onComplete(false)
            return
        }
        val reviewManager = ReviewManagerFactory.create(activity)
        reviewManager.requestReviewFlow()
            .addOnCompleteListener { requestTask ->
                if (!requestTask.isSuccessful) {
                    onComplete(false)
                    return@addOnCompleteListener
                }
                reviewManager.launchReviewFlow(activity, requestTask.result)
                    .addOnCompleteListener {
                        onComplete(true)
                    }
            }
    }
}

@Composable
actual fun rememberInAppReviewController(): InAppReviewController {
    val context = LocalContext.current
    return remember(context) {
        AndroidInAppReviewController(
            activityProvider = { context.findActivity() },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

