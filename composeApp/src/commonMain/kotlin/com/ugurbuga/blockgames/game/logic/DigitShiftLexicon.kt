package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.AppLanguage
import kotlin.random.Random

internal const val DigitShiftDefaultWordLength = 5
internal const val DigitShiftDefaultMaxAttempts = 6
private val DigitShiftSupportedLengths: List<Int> = listOf(5, 6)

internal fun digitShiftAttemptsForLength(length: Int): Int = when (length) {
    6 -> 8
    else -> DigitShiftDefaultMaxAttempts
}

internal val DigitShiftDigitTokens: List<String> = ('0'..'9').map(Char::toString)
private val DigitShiftKeyboardRows: List<List<String>> = listOf(
    listOf("1", "2", "3", "4", "5"),
    listOf("6", "7", "8", "9", "0"),
)

internal data class DigitShiftLanguagePack(
    val language: AppLanguage,
    private val keyboardRows: List<List<String>>,
    val supportedLengths: List<Int> = DigitShiftSupportedLengths,
) {
    val digitTokens: List<String> = DigitShiftDigitTokens

    fun keyboardRows(@Suppress("UNUSED_PARAMETER") length: Int): List<List<String>> = keyboardRows

    fun isSupportedToken(token: String): Boolean = token in digitTokens

    fun randomSolution(
        length: Int,
        random: Random,
    ): List<String> = List(length) { digitTokens[random.nextInt(digitTokens.size)] }
}

internal object DigitShiftLexicon {
    private val supportedLengths: List<Int> = DigitShiftSupportedLengths

    fun packFor(localeTag: String): DigitShiftLanguagePack {
        val language = AppLanguage.fromDeviceLocaleTag(localeTag) ?: AppLanguage.English
        return DigitShiftLanguagePack(
            language = language,
            keyboardRows = DigitShiftKeyboardRows,
            supportedLengths = supportedLengths,
        )
    }

    fun isSupportedWordLength(length: Int): Boolean = length in supportedLengths

    fun keyOf(tokens: List<String>): String = tokens.joinToString(separator = "|")
}

