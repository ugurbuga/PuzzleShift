package com.ugurbuga.blockgames.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val BannerRefreshIntervalMillis = 60_000L
private val AndroidAdMobAdUnitIdRegex = Regex("^ca-app-pub-\\d+/\\d+$")

@Composable
actual fun rememberPlatformGameAdController(): GameAdController {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val inspectionMode = LocalInspectionMode.current
    val controller = remember(applicationContext) {
        AndroidGameAdController(
            context = applicationContext,
            activityProvider = { context.findActivity() },
        )
    }

    DisposableEffect(controller, inspectionMode) {
        if (!inspectionMode) {
            controller.preloadAds()
        }
        onDispose { }
    }

    return controller
}

private class AndroidGameAdController(
    private val context: Context,
    private val activityProvider: () -> Activity?,
) : GameAdController {
    override val bannerPresentationMode: BannerPresentationMode = BannerPresentationMode.Inline

    private var mobileAdsInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedReviveAd: RewardedAd? = null
    private var rewardedSpecialAd: RewardedInterstitialAd? = null

    fun preloadAds() {
        try {
            initializeMobileAdsIfNeeded()
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to initialize MobileAds", e)
            return
        }
        try {
            if (interstitialAd == null) {
                loadInterstitial()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to load interstitial ad", e)
        }
        try {
            if (rewardedReviveAd == null) {
                loadRewardedRevive()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to load rewarded revive ad", e)
        }
        try {
            if (rewardedSpecialAd == null) {
                loadRewardedSpecial()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Failed to load rewarded special ad", e)
        }
    }

    override fun showRestartInterstitial(onFinished: () -> Unit) {
        try {
            preloadAds()
            val activity = activityProvider()
            val ad = interstitialAd
            if (activity == null || ad == null) {
                onFinished()
                loadInterstitial()
                return
            }
            interstitialAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onFinished()
                    loadInterstitial()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onFinished()
                    loadInterstitial()
                }
            }
            try {
                ad.show(activity)
            } catch (e: Exception) {
                Log.e("GameAds", "Failed to show interstitial ad", e)
                onFinished()
                loadInterstitial()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in showRestartInterstitial", e)
            onFinished()
        }
    }

    override fun showRewardedRevive(onResult: (Boolean) -> Unit) {
        try {
            preloadAds()
            val activity = activityProvider()
            val ad = rewardedReviveAd
            if (activity == null || ad == null) {
                onResult(false)
                loadRewardedRevive()
                return
            }
            rewardedReviveAd = null
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onResult(rewardEarned)
                    loadRewardedRevive()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onResult(false)
                    loadRewardedRevive()
                }
            }
            try {
                ad.show(activity) {
                    rewardEarned = true
                }
            } catch (e: Exception) {
                Log.e("GameAds", "Failed to show rewarded revive ad", e)
                onResult(false)
                loadRewardedRevive()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in showRewardedRevive", e)
            onResult(false)
        }
    }

    override fun showRewardedAd(onResult: (Boolean) -> Unit) {
        try {
            preloadAds()
            val activity = activityProvider()
            val ad = rewardedSpecialAd
            if (activity == null || ad == null) {
                onResult(false)
                loadRewardedSpecial()
                return
            }
            rewardedSpecialAd = null
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onResult(rewardEarned)
                    loadRewardedSpecial()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onResult(false)
                    loadRewardedSpecial()
                }
            }
            try {
                ad.show(activity) {
                    rewardEarned = true
                }
            } catch (e: Exception) {
                Log.e("GameAds", "Failed to show rewarded special ad", e)
                onResult(false)
                loadRewardedSpecial()
            }
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in showRewardedAd", e)
            onResult(false)
        }
    }

    @Composable
    override fun Banner(modifier: Modifier, onLoadStateChanged: (Boolean) -> Unit) {
        val inspectionMode = LocalInspectionMode.current
        val currentOnLoadStateChanged by rememberUpdatedState(onLoadStateChanged)
        LaunchedEffect(inspectionMode) {
            currentOnLoadStateChanged(false)
        }
        if (inspectionMode) return
        val bannerAdUnitId = validatedAdUnitId(
            propertyName = "AndroidAdMobIds.BannerAdUnitId",
            rawValue = AndroidAdMobIds.BannerAdUnitId,
        ) ?: return

        var bannerAdView by remember { mutableStateOf<AdView?>(null) }

        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                AdView(viewContext).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = bannerAdUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            currentOnLoadStateChanged(true)
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            currentOnLoadStateChanged(false)
                            Log.e("GameAds", "Banner ad failed to load: $loadAdError")
                        }
                    }
                }
            },
            update = { view ->
                view.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        currentOnLoadStateChanged(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        currentOnLoadStateChanged(false)
                        Log.e("GameAds", "Banner ad failed to load: $loadAdError")
                    }
                }
                if (bannerAdView !== view) {
                    bannerAdView = view
                }
            },
        )

        val currentBannerAdView = bannerAdView ?: return

        LaunchedEffect(currentBannerAdView) {
            refreshBannerAd(currentBannerAdView)
            while (true) {
                delay(BannerRefreshIntervalMillis.milliseconds)
                refreshBannerAd(currentBannerAdView)
            }
        }

        DisposableEffect(currentBannerAdView) {
            onDispose {
                currentOnLoadStateChanged(false)
                currentBannerAdView.destroy()
                bannerAdView = null
            }
        }
    }

    private fun initializeMobileAdsIfNeeded() {
        if (mobileAdsInitialized) return
        try {
            MobileAds.initialize(context)
            mobileAdsInitialized = true
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in MobileAds.initialize", e)
        }
    }

    private fun loadInterstitial() {
        val adUnitId = validatedAdUnitId(
            propertyName = "AndroidAdMobIds.InterstitialAdUnitId",
            rawValue = AndroidAdMobIds.InterstitialAdUnitId,
        ) ?: run {
            interstitialAd = null
            return
        }
        try {
            InterstitialAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        Log.e("GameAds", "Interstitial ad failed to load: $loadAdError")
                    }
                },
            )
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in loadInterstitial", e)
        }
    }

    private fun loadRewardedRevive() {
        val adUnitId = validatedAdUnitId(
            propertyName = "AndroidAdMobIds.RewardedReviveAdUnitId",
            rawValue = AndroidAdMobIds.RewardedReviveAdUnitId,
        ) ?: run {
            rewardedReviveAd = null
            return
        }
        try {
            RewardedAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedReviveAd = ad
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedReviveAd = null
                        Log.e("GameAds", "Rewarded revive ad failed to load: $loadAdError")
                    }
                },
            )
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in loadRewardedRevive", e)
        }
    }

    private fun loadRewardedSpecial() {
        val adUnitId = validatedAdUnitId(
            propertyName = "AndroidAdMobIds.RewardedSpecialAdUnitId",
            rawValue = AndroidAdMobIds.RewardedSpecialAdUnitId,
        ) ?: run {
            rewardedSpecialAd = null
            return
        }
        try {
            RewardedInterstitialAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        rewardedSpecialAd = ad
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedSpecialAd = null
                        Log.e("GameAds", "Rewarded special ad failed to load: $loadAdError")
                    }
                },
            )
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in loadRewardedSpecial", e)
        }
    }

    private fun refreshBannerAd(adView: AdView) {
        try {
            adView.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Log.e("GameAds", "Exception in refreshBannerAd", e)
        }
    }

    private fun validatedAdUnitId(propertyName: String, rawValue: String): String? {
        val normalized = rawValue.normalizeAdMobIdentifier()
        if (normalized.isBlank()) {
            Log.w("GameAds", "$propertyName is blank; skipping ad load.")
            return null
        }
        if (!normalized.matches(AndroidAdMobAdUnitIdRegex)) {
            Log.e(
                "GameAds",
                "$propertyName is not a valid Android AdMob ad unit ID. " +
                    "Expected format ca-app-pub-XXXXXXXXXXXXXXXX/NNNNNNNNNN but was ${normalized.redactedAdMobIdentifier()}. " +
                    "Check ads.properties and remove surrounding quotes or app-id (~) values.",
            )
            return null
        }
        return normalized
    }
}

object AndroidAdMobIds {
    var BannerAdUnitId: String = ""
    var InterstitialAdUnitId: String = ""
    var RewardedReviveAdUnitId: String = ""
    var RewardedSpecialAdUnitId: String = ""
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun String.normalizeAdMobIdentifier(): String =
    trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")

private fun String.redactedAdMobIdentifier(): String {
    if (isBlank()) return "<blank>"
    if (length <= 12) return "<redacted>"
    return take(12) + "..." + takeLast(4)
}

