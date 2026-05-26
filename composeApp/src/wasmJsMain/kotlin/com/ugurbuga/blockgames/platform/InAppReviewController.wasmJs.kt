package com.ugurbuga.blockgames.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberInAppReviewController(): InAppReviewController = NoOpInAppReviewController

