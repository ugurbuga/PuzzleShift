package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.AppLanguage
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigitShiftLexiconTest {

    @Test
    fun packsProvideFiveAndSixDigitKeypads() {
        AppLanguage.entries.forEach { language ->
            val pack = DigitShiftLexicon.packFor(language.localeTag)
            val keyboardRows = pack.keyboardRows(DigitShiftDefaultWordLength)
            val keyboardTokens = keyboardRows.flatten().toSet()

            assertEquals(listOf(5, 6), pack.supportedLengths, "Unexpected supported lengths for ${language.localeTag}")
            assertEquals(listOf(listOf("1", "2", "3", "4", "5"), listOf("6", "7", "8", "9", "0")), keyboardRows)
            assertEquals(DigitShiftDigitTokens.toSet(), keyboardTokens)
            assertEquals(6, digitShiftAttemptsForLength(5))
            assertEquals(8, digitShiftAttemptsForLength(6))
            pack.supportedLengths.forEach { length ->
                val solution = pack.randomSolution(length = length, random = Random(42 + length))
                assertEquals(length, solution.size, "Unexpected solution length for ${language.localeTag} length=$length")
                assertTrue(solution.all { it in keyboardTokens }, "Unexpected digit for ${language.localeTag} length=$length")
            }
        }
    }
}

