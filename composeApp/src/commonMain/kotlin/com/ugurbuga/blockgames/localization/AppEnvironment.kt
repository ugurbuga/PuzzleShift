package com.ugurbuga.blockgames.localization

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.settings.AppSettings

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/**
 * Shared, app-wide animated pulse for animated block visual styles.
 * All consumers read from the same value so animations stay in sync.
 */
val LocalBlockStylePulse = compositionLocalOf { 0f }

/** Duration used for the shared block-style pulse animation. */
const val BlockStylePulseDurationMillis = 3200

/** Returns `true` for [BlockVisualStyle] entries that need an animated pulse. */
fun isAnimatedBlockStyle(style: BlockVisualStyle): Boolean = when (style) {
    BlockVisualStyle.DynamicLiquid,
    BlockVisualStyle.Tornado,
    BlockVisualStyle.Prism,
    BlockVisualStyle.SoundWave,
    BlockVisualStyle.Flame,
    BlockVisualStyle.Cosmic,
    BlockVisualStyle.Gears,
    BlockVisualStyle.Cyberpunk,
    BlockVisualStyle.NeonGlow,
    BlockVisualStyle.LiquidMarble,
    BlockVisualStyle.Holographic,
    BlockVisualStyle.GlitchTech,
    BlockVisualStyle.AuraEnergy,
    BlockVisualStyle.CircuitBoard -> true
    else -> false
}

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): Array<ProvidedValue<*>>
}

expect fun currentDeviceLocaleTag(): String

@Composable
fun AppEnvironment(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "globalBlockStylePulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BlockStylePulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "globalBlockStylePulseValue",
    )
    CompositionLocalProvider(
        LocalAppSettings provides settings,
        LocalBlockStylePulse provides pulse,
        *(LocalAppLocale provides settings.language.localeTag),
    ) {
        key(settings.language.localeTag) {
            content()
        }
    }
}
