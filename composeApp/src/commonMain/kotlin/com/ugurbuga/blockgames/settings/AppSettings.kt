package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.ChallengeProgress
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.toBlockColorPalette

@Immutable
data class AppSettings(
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val themeColorPalette: AppColorPalette = AppColorPalette.Classic,
    val blockVisualStyle: BlockVisualStyle = BlockVisualStyle.Flat,
    val tokenBalance: Int = 0,
    val unlockedThemeModes: Set<AppThemeMode> = AppThemeMode.entries.toSet(),
    val unlockedThemePalettes: Set<AppColorPalette> = setOf(AppColorPalette.Classic),
    val unlockedBlockStyles: Set<BlockVisualStyle> = setOf(BlockVisualStyle.Flat),
    val styleChallengeProgress: Map<GameplayStyle, ChallengeProgress> = emptyMap(),
    val lastAppOpenedAtEpochMillis: Long = 0L,
    val totalGameplayDurationMillis: Long = 0L,
    val hasSeenTutorial: Boolean = false,
    val hasShownInteractiveOnboarding: Boolean = false,
    val hasRequestedInAppReview: Boolean = false,
    val seenTutorialStyles: Set<GameplayStyle> = emptySet(),
    val shownInteractiveOnboardingStyles: Set<GameplayStyle> = emptySet(),
    val hasInitializedLanguage: Boolean = false,
    val lastActiveSlot: GameSessionSlot? = null,
    val selectedGameplayStyle: GameplayStyle? = null,
) {
    val challengeProgress: ChallengeProgress
        get() = styleChallengeProgress[selectedGameplayStyle ?: GameplayStyle.StackShift] ?: ChallengeProgress()

    val blockColorPalette: BlockColorPalette
        get() = themeColorPalette.toBlockColorPalette()
}
