package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.gameplayStyleFromPersistedValue
import com.ugurbuga.blockgames.game.model.persistedKeys
import com.ugurbuga.blockgames.game.model.resolveUnifiedThemePalette
import platform.Foundation.NSUserDefaults

actual object AppSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppSettings {
        val hasStoredLanguage = defaults.objectForKey(KeyLanguage) != null
        val defaultSettings = AppSettings()
        val legacyThemePalette = defaults.objectForKey(KeyThemeColorPalette)?.let {
            AppColorPalette.entries.getOrElse(defaults.integerForKey(KeyThemeColorPalette).toInt()) { defaultSettings.themeColorPalette }
        }
        val legacyBlockPalette = defaults.objectForKey(KeyBlockColorPalette)?.let {
            BlockColorPalette.entries.getOrElse(defaults.integerForKey(KeyBlockColorPalette).toInt()) { defaultSettings.blockColorPalette }
        }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(defaults.integerForKey(KeyLanguage).toInt()) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(defaults.integerForKey(KeyThemeMode).toInt()) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(themePalette = legacyThemePalette, blockPalette = legacyBlockPalette),
            blockVisualStyle = BlockVisualStyle.entries.getOrElse(defaults.integerForKey(KeyBlockVisualStyle).toInt()) { defaultSettings.blockVisualStyle },
            hasSeenTutorial = defaults.boolForKey(KeyHasSeenTutorial),
            hasShownInteractiveOnboarding = defaults.boolForKey(KeyHasShownInteractiveOnboarding),
            seenTutorialStyles = decodeEnumSet(defaults.getSafeString(KeySeenTutorialStyles), GameplayStyle.entries),
            shownInteractiveOnboardingStyles = decodeEnumSet(
                defaults.getSafeString(KeyShownInteractiveOnboardingStyles),
                GameplayStyle.entries,
            ),
            hasInitializedLanguage = defaults.boolForKey(KeyHasInitializedLanguage) || hasStoredLanguage,
            tokenBalance = defaults.integerForKey(KeyTokenBalance).toInt(),
            unlockedThemeModes = decodeEnumSet(defaults.getSafeString(KeyUnlockedThemeModes), AppThemeMode.entries),
            unlockedThemePalettes = decodeEnumSet(defaults.getSafeString(KeyUnlockedThemePalettes), AppColorPalette.entries),
            unlockedBlockStyles = decodeEnumSet(defaults.getSafeString(KeyUnlockedBlockStyles), com.ugurbuga.blockgames.game.model.BlockVisualStyle.entries),
            styleChallengeProgress = com.ugurbuga.blockgames.game.model.GameplayStyle.entries.associateWith { style ->
                decodeChallengeProgress(
                    style.persistedKeys()
                        .asSequence()
                        .mapNotNull { key -> defaults.getSafeString(KeyChallengeProgressPrefix + key) }
                        .firstOrNull()
                )
            }.filterValues { it.completedDays.isNotEmpty() },
            lastAppOpenedAtEpochMillis = defaults.objectForKey(KeyLastAppOpenedAtEpochMillis)?.let {
                defaults.stringForKey(KeyLastAppOpenedAtEpochMillis)?.toLongOrNull()
                    ?: defaults.integerForKey(KeyLastAppOpenedAtEpochMillis).toLong()
            } ?: defaultSettings.lastAppOpenedAtEpochMillis,
            totalGameplayDurationMillis = defaults.objectForKey(KeyTotalGameplayDurationMillis)?.let {
                defaults.stringForKey(KeyTotalGameplayDurationMillis)?.toLongOrNull()
                    ?: defaults.integerForKey(KeyTotalGameplayDurationMillis).toLong()
            } ?: defaultSettings.totalGameplayDurationMillis,
            hasRequestedInAppReview = defaults.boolForKey(KeyHasRequestedInAppReview),
            lastActiveSlot = defaults.stringForKey(KeyLastActiveSlot)?.let {
                GameSessionSlot.fromKey(it)
            },
            selectedGameplayStyle = defaults.stringForKey(KeySelectedGameplayStyle)?.let {
                gameplayStyleFromPersistedValue(it)
            },
        ).sanitized()
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        defaults.setInteger(sanitized.language.ordinal.toLong(), forKey = KeyLanguage)
        defaults.setInteger(sanitized.themeMode.ordinal.toLong(), forKey = KeyThemeMode)
        defaults.setInteger(sanitized.themeColorPalette.ordinal.toLong(), forKey = KeyThemeColorPalette)
        defaults.setInteger(sanitized.blockColorPalette.ordinal.toLong(), forKey = KeyBlockColorPalette)
        defaults.setInteger(sanitized.blockVisualStyle.ordinal.toLong(), forKey = KeyBlockVisualStyle)
        defaults.setBool(sanitized.hasSeenTutorial, forKey = KeyHasSeenTutorial)
        defaults.setBool(sanitized.hasShownInteractiveOnboarding, forKey = KeyHasShownInteractiveOnboarding)
        defaults.setObject(encodeEnumSet(sanitized.seenTutorialStyles), forKey = KeySeenTutorialStyles)
        defaults.setObject(
            encodeEnumSet(sanitized.shownInteractiveOnboardingStyles),
            forKey = KeyShownInteractiveOnboardingStyles,
        )
        defaults.setBool(sanitized.hasInitializedLanguage, forKey = KeyHasInitializedLanguage)
        defaults.setInteger(sanitized.tokenBalance.toLong(), forKey = KeyTokenBalance)
        defaults.setObject(encodeEnumSet(sanitized.unlockedThemeModes), forKey = KeyUnlockedThemeModes)
        defaults.setObject(encodeEnumSet(sanitized.unlockedThemePalettes), forKey = KeyUnlockedThemePalettes)
        defaults.setObject(encodeEnumSet(sanitized.unlockedBlockStyles), forKey = KeyUnlockedBlockStyles)
        sanitized.styleChallengeProgress.forEach { (style, progress) ->
            defaults.setObject(encodeChallengeProgress(progress), forKey = KeyChallengeProgressPrefix + style.name.lowercase())
        }
        defaults.setObject(sanitized.lastAppOpenedAtEpochMillis.toString(), forKey = KeyLastAppOpenedAtEpochMillis)
        defaults.setObject(sanitized.totalGameplayDurationMillis.toString(), forKey = KeyTotalGameplayDurationMillis)
        defaults.setBool(sanitized.hasRequestedInAppReview, forKey = KeyHasRequestedInAppReview)
        if (sanitized.lastActiveSlot != null) {
            defaults.setObject(sanitized.lastActiveSlot.key, forKey = KeyLastActiveSlot)
        } else {
            defaults.removeObjectForKey(KeyLastActiveSlot)
        }
        if (sanitized.selectedGameplayStyle != null) {
            defaults.setObject(sanitized.selectedGameplayStyle.name, forKey = KeySelectedGameplayStyle)
        } else {
            defaults.removeObjectForKey(KeySelectedGameplayStyle)
        }
    }

    private const val KeySelectedGameplayStyle = "selectedGameplayStyle"
    private const val KeyLastActiveSlot = "lastActiveSlot"
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

    private fun NSUserDefaults.getSafeString(key: String): String? {
        stringForKey(key)?.takeIf(String::isNotBlank)?.let { return it }
        return stringArrayForKey(key)
            ?.filterIsInstance<String>()
            ?.joinToString(separator = ";")
            ?.takeIf(String::isNotBlank)
    }
}

