package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.blockVisualStyleFromPersistedOrdinal
import com.ugurbuga.blockgames.game.model.blockVisualStyleFromPersistedValue
import com.ugurbuga.blockgames.game.model.gameplayStyleFromPersistedValue
import com.ugurbuga.blockgames.game.model.normalizeBlockVisualStyle
import com.ugurbuga.blockgames.game.model.persistedKeys
import com.ugurbuga.blockgames.game.model.resolveUnifiedThemePalette
import java.util.prefs.Preferences

actual object AppSettingsStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(): AppSettings {
        val hasStoredLanguage = prefs.get(KeyLanguage, null) != null
        val defaultSettings = AppSettings()
        val legacyThemePalette = prefs.get(KeyThemeColorPalette, null)?.let {
            AppColorPalette.entries.getOrElse(prefs.getInt(KeyThemeColorPalette, defaultSettings.themeColorPalette.ordinal)) { defaultSettings.themeColorPalette }
        }
        val legacyBlockPalette = prefs.get(KeyBlockColorPalette, null)?.let {
            BlockColorPalette.entries.getOrElse(prefs.getInt(KeyBlockColorPalette, defaultSettings.blockColorPalette.ordinal)) { defaultSettings.blockColorPalette }
        }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(prefs.getInt(KeyLanguage, defaultSettings.language.ordinal)) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(prefs.getInt(KeyThemeMode, defaultSettings.themeMode.ordinal)) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(themePalette = legacyThemePalette, blockPalette = legacyBlockPalette),
            blockVisualStyle = blockVisualStyleFromPersistedValue(
                prefs.get(KeyBlockVisualStyle, null),
            ) ?: blockVisualStyleFromPersistedOrdinal(
                prefs.getInt(KeyBlockVisualStyle, defaultSettings.blockVisualStyle.ordinal),
                defaultSettings.blockVisualStyle,
            ),
            hasSeenTutorial = prefs.getBoolean(KeyHasSeenTutorial, defaultSettings.hasSeenTutorial),
            hasShownInteractiveOnboarding = prefs.getBoolean(KeyHasShownInteractiveOnboarding, defaultSettings.hasShownInteractiveOnboarding),
            seenTutorialStyles = decodeEnumSet(prefs.getSafeString(KeySeenTutorialStyles, null), GameplayStyle.entries),
            shownInteractiveOnboardingStyles = decodeEnumSet(
                prefs.getSafeString(KeyShownInteractiveOnboardingStyles, null),
                GameplayStyle.entries,
            ),
            hasInitializedLanguage = prefs.getBoolean(KeyHasInitializedLanguage, defaultSettings.hasInitializedLanguage) || hasStoredLanguage,
            tokenBalance = prefs.getInt(KeyTokenBalance, defaultSettings.tokenBalance),
            unlockedThemeModes = decodeEnumSet(prefs.getSafeString(KeyUnlockedThemeModes, null), AppThemeMode.entries),
            unlockedThemePalettes = decodeEnumSet(prefs.getSafeString(KeyUnlockedThemePalettes, null), AppColorPalette.entries),
            unlockedBlockStyles = decodeEnumSet(prefs.getSafeString(KeyUnlockedBlockStyles, null), BlockVisualStyle.entries),
            styleChallengeProgress = GameplayStyle.entries.associateWith { style ->
                decodeChallengeProgress(
                    style.persistedKeys()
                        .asSequence()
                        .mapNotNull { key -> prefs.getSafeString(KeyChallengeProgressPrefix + key, null) }
                        .firstOrNull()
                )
            }.filterValues { it.completedDays.isNotEmpty() },
            lastAppOpenedAtEpochMillis = prefs.getLong(KeyLastAppOpenedAtEpochMillis, defaultSettings.lastAppOpenedAtEpochMillis),
            totalGameplayDurationMillis = prefs.getLong(KeyTotalGameplayDurationMillis, defaultSettings.totalGameplayDurationMillis),
            hasRequestedInAppReview = prefs.getBoolean(KeyHasRequestedInAppReview, defaultSettings.hasRequestedInAppReview),
            lastActiveSlot = prefs.get(KeyLastActiveSlot, null)?.let {
                GameSessionSlot.fromKey(it)
            },
            selectedGameplayStyle = prefs.get(KeySelectedGameplayStyle, null)?.let {
                gameplayStyleFromPersistedValue(it)
            },
        ).sanitized()
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        prefs.putInt(KeyLanguage, sanitized.language.ordinal)
        prefs.putInt(KeyThemeMode, sanitized.themeMode.ordinal)
        prefs.putInt(KeyThemeColorPalette, sanitized.themeColorPalette.ordinal)
        prefs.putInt(KeyBlockColorPalette, sanitized.blockColorPalette.ordinal)
        prefs.put(KeyBlockVisualStyle, normalizeBlockVisualStyle(sanitized.blockVisualStyle).name)
        prefs.putBoolean(KeyHasSeenTutorial, sanitized.hasSeenTutorial)
        prefs.putBoolean(KeyHasShownInteractiveOnboarding, sanitized.hasShownInteractiveOnboarding)
        prefs.put(KeySeenTutorialStyles, encodeEnumSet(sanitized.seenTutorialStyles))
        prefs.put(
            KeyShownInteractiveOnboardingStyles,
            encodeEnumSet(sanitized.shownInteractiveOnboardingStyles)
        )
        prefs.putBoolean(KeyHasInitializedLanguage, sanitized.hasInitializedLanguage)
        prefs.putInt(KeyTokenBalance, sanitized.tokenBalance)
        prefs.put(KeyUnlockedThemeModes, encodeEnumSet(sanitized.unlockedThemeModes))
        prefs.put(KeyUnlockedThemePalettes, encodeEnumSet(sanitized.unlockedThemePalettes))
        prefs.put(KeyUnlockedBlockStyles, encodeEnumSet(sanitized.unlockedBlockStyles))
        sanitized.styleChallengeProgress.forEach { (style, progress) ->
            prefs.put(KeyChallengeProgressPrefix + style.name.lowercase(), encodeChallengeProgress(progress))
        }
        prefs.putLong(KeyLastAppOpenedAtEpochMillis, sanitized.lastAppOpenedAtEpochMillis)
        prefs.putLong(KeyTotalGameplayDurationMillis, sanitized.totalGameplayDurationMillis)
        prefs.putBoolean(KeyHasRequestedInAppReview, sanitized.hasRequestedInAppReview)
        if (sanitized.lastActiveSlot != null) {
            prefs.put(KeyLastActiveSlot, sanitized.lastActiveSlot.key)
        } else {
            prefs.remove(KeyLastActiveSlot)
        }
        if (sanitized.selectedGameplayStyle != null) {
            prefs.put(KeySelectedGameplayStyle, sanitized.selectedGameplayStyle.name)
        } else {
            prefs.remove(KeySelectedGameplayStyle)
        }
    }

    private const val KeySelectedGameplayStyle = "selectedGameplayStyle"
    private const val KeyLastActiveSlot = "lastActiveSlot"

    private const val Namespace = "com.ugurbuga.blockgames.settings"
    private const val KeyLastAppOpenedAtEpochMillis = "lastAppOpenedAtEpochMillis"
    private const val KeyTotalGameplayDurationMillis = "totalGameplayDurationMillis"
    private const val KeyHasRequestedInAppReview = "hasRequestedInAppReview"
    private const val KeyChallengeProgressPrefix = "challengeProgress_"
    private const val KeyTokenBalance = "tokenBalance"
    private const val KeyUnlockedThemeModes = "unlockedThemeModes"
    private const val KeyUnlockedThemePalettes = "unlockedThemePalettes"
    private const val KeyUnlockedBlockStyles = "unlockedBlockStyles"
    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"
    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeySeenTutorialStyles = "seenTutorialStyles"
    private const val KeyShownInteractiveOnboardingStyles = "shownInteractiveOnboardingStyles"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"

    private fun Preferences.getSafeString(key: String, defaultValue: String?): String? =
        runCatching { get(key, defaultValue) }.getOrDefault(defaultValue)
}
