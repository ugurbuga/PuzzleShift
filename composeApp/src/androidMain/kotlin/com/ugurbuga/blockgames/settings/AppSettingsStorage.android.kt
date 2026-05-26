package com.ugurbuga.blockgames.settings

import android.content.Context
import android.content.SharedPreferences
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.gameplayStyleFromPersistedValue
import com.ugurbuga.blockgames.game.model.persistedKeys
import com.ugurbuga.blockgames.game.model.resolveUnifiedThemePalette

actual object AppSettingsStorage {
    private const val Namespace = "com.ugurbuga.blockgames.settings"
    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"

    private val prefs: SharedPreferences by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(): AppSettings {
        val hasStoredLanguage = prefs.contains(KeyLanguage)
        val defaultSettings = AppSettings()
        val legacyThemePalette = prefs.takeIf { it.contains(KeyThemeColorPalette) }?.let {
            AppColorPalette.entries.getOrElse(
                prefs.getInt(KeyThemeColorPalette, defaultSettings.themeColorPalette.ordinal)
            ) { defaultSettings.themeColorPalette }
        }
        val legacyBlockPalette = prefs.takeIf { it.contains(KeyBlockColorPalette) }?.let {
            BlockColorPalette.entries.getOrElse(
                prefs.getInt(KeyBlockColorPalette, defaultSettings.blockColorPalette.ordinal)
            ) { defaultSettings.blockColorPalette }
        }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(
                prefs.getInt(
                    KeyLanguage,
                    defaultSettings.language.ordinal
                )
            ) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(
                prefs.getInt(
                    KeyThemeMode,
                    defaultSettings.themeMode.ordinal
                )
            ) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(
                themePalette = legacyThemePalette,
                blockPalette = legacyBlockPalette,
            ),
            blockVisualStyle = BlockVisualStyle.entries.getOrElse(
                prefs.getInt(
                    KeyBlockVisualStyle,
                    defaultSettings.blockVisualStyle.ordinal
                )
            ) { defaultSettings.blockVisualStyle },
            hasSeenTutorial = prefs.getBoolean(KeyHasSeenTutorial, defaultSettings.hasSeenTutorial),
            hasShownInteractiveOnboarding = prefs.getBoolean(
                KeyHasShownInteractiveOnboarding,
                defaultSettings.hasShownInteractiveOnboarding
            ),
            seenTutorialStyles = decodeEnumSet(
                prefs.getSafeString(KeySeenTutorialStyles, null),
                GameplayStyle.entries,
            ),
            shownInteractiveOnboardingStyles = decodeEnumSet(
                prefs.getSafeString(KeyShownInteractiveOnboardingStyles, null),
                GameplayStyle.entries,
            ),
            hasInitializedLanguage = prefs.getBoolean(
                KeyHasInitializedLanguage,
                defaultSettings.hasInitializedLanguage
            ) || hasStoredLanguage,
            tokenBalance = prefs.getInt(KeyTokenBalance, defaultSettings.tokenBalance),
            unlockedThemeModes = decodeEnumSet(
                prefs.getSafeString(KeyUnlockedThemeModes, null),
                AppThemeMode.entries,
            ),
            unlockedThemePalettes = decodeEnumSet(
                prefs.getSafeString(KeyUnlockedThemePalettes, null),
                AppColorPalette.entries,
            ),
            unlockedBlockStyles = decodeEnumSet(
                prefs.getSafeString(KeyUnlockedBlockStyles, null),
                BlockVisualStyle.entries,
            ),
            styleChallengeProgress = GameplayStyle.entries.associateWith { style ->
                decodeChallengeProgress(
                    style.persistedKeys()
                        .asSequence()
                        .mapNotNull { key -> prefs.getSafeString(KeyChallengeProgressPrefix + key, null) }
                        .firstOrNull()
                )
            }.filterValues { it.completedDays.isNotEmpty() },
            lastAppOpenedAtEpochMillis = prefs.getLong(
                KeyLastAppOpenedAtEpochMillis,
                defaultSettings.lastAppOpenedAtEpochMillis,
            ),
            totalGameplayDurationMillis = prefs.getLong(
                KeyTotalGameplayDurationMillis,
                defaultSettings.totalGameplayDurationMillis,
            ),
            hasRequestedInAppReview = prefs.getBoolean(
                KeyHasRequestedInAppReview,
                defaultSettings.hasRequestedInAppReview,
            ),
            lastActiveSlot = prefs.getString(KeyLastActiveSlot, null)?.let {
                GameSessionSlot.fromKey(it)
            },
            selectedGameplayStyle = prefs.getString(KeySelectedGameplayStyle, null)?.let {
                gameplayStyleFromPersistedValue(it)
            },
        ).sanitized()
    }

    private fun SharedPreferences.getSafeString(key: String, defaultValue: String?): String? {
        return try {
            getString(key, defaultValue)
        } catch (_: ClassCastException) {
            getStringSet(key, null)?.joinToString(";") ?: defaultValue
        }
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        prefs.edit()
            .putInt(KeyLanguage, sanitized.language.ordinal)
            .putInt(KeyThemeMode, sanitized.themeMode.ordinal)
            .putInt(KeyThemeColorPalette, sanitized.themeColorPalette.ordinal)
            .putInt(KeyBlockColorPalette, sanitized.blockColorPalette.ordinal)
            .putInt(KeyBlockVisualStyle, sanitized.blockVisualStyle.ordinal)
            .putBoolean(KeyHasSeenTutorial, sanitized.hasSeenTutorial)
            .putBoolean(KeyHasShownInteractiveOnboarding, sanitized.hasShownInteractiveOnboarding)
            .putString(KeySeenTutorialStyles, encodeEnumSet(sanitized.seenTutorialStyles))
            .putString(
                KeyShownInteractiveOnboardingStyles,
                encodeEnumSet(sanitized.shownInteractiveOnboardingStyles)
            )
            .putBoolean(KeyHasInitializedLanguage, sanitized.hasInitializedLanguage)
            .putInt(KeyTokenBalance, sanitized.tokenBalance)
            .putString(KeyUnlockedThemeModes, encodeEnumSet(sanitized.unlockedThemeModes))
            .putString(KeyUnlockedThemePalettes, encodeEnumSet(sanitized.unlockedThemePalettes))
            .putString(KeyUnlockedBlockStyles, encodeEnumSet(sanitized.unlockedBlockStyles))
            .apply {
                sanitized.styleChallengeProgress.forEach { (style, progress) ->
                    putString(KeyChallengeProgressPrefix + style.name.lowercase(), encodeChallengeProgress(progress))
                }
            }
            .putLong(KeyLastAppOpenedAtEpochMillis, sanitized.lastAppOpenedAtEpochMillis)
            .putLong(KeyTotalGameplayDurationMillis, sanitized.totalGameplayDurationMillis)
            .putBoolean(KeyHasRequestedInAppReview, sanitized.hasRequestedInAppReview)
            .apply {
                if (sanitized.lastActiveSlot != null) {
                    putString(KeyLastActiveSlot, sanitized.lastActiveSlot.key)
                } else {
                    remove(KeyLastActiveSlot)
                }
                if (sanitized.selectedGameplayStyle != null) {
                    putString(KeySelectedGameplayStyle, sanitized.selectedGameplayStyle.name)
                } else {
                    remove(KeySelectedGameplayStyle)
                }
            }
            .apply()
    }

    private const val KeyLastActiveSlot = "lastActiveSlot"
    private const val KeySelectedGameplayStyle = "selectedGameplayStyle"
    private const val KeyLastAppOpenedAtEpochMillis = "lastAppOpenedAtEpochMillis"
    private const val KeyTotalGameplayDurationMillis = "totalGameplayDurationMillis"
    private const val KeyHasRequestedInAppReview = "hasRequestedInAppReview"
    private const val KeyChallengeProgressPrefix = "challengeProgress_"
    private const val KeyTokenBalance = "tokenBalance"
    private const val KeyUnlockedThemeModes = "unlockedThemeModes"
    private const val KeyUnlockedThemePalettes = "unlockedThemePalettes"
    private const val KeyUnlockedBlockStyles = "unlockedBlockStyles"

    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeySeenTutorialStyles = "seenTutorialStyles"
    private const val KeyShownInteractiveOnboardingStyles = "shownInteractiveOnboardingStyles"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"
}
