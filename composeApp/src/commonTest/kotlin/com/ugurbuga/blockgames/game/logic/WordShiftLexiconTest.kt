package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordShiftLexiconTest {

    @Test
    fun packsProvideOneThousandWordsPerLanguageAcrossSupportedLengths() {
        AppLanguage.entries.forEach { language ->
            val pack = WordShiftLexicon.packFor(language.localeTag)
            val keyboardTokens = pack.keyboardRows(WordShiftDefaultWordLength).flatten().toSet()

            assertEquals(listOf(4, 5, 6, 7), pack.supportedLengths, "Unexpected supported lengths for ${language.localeTag}")
            assertEquals(1_000, pack.totalWordCount, "Unexpected total word count for ${language.localeTag}")
            pack.supportedLengths.forEach { length ->
                assertTrue(pack.words(length).isNotEmpty(), "Missing words for ${language.localeTag} length=$length")
                assertEquals(pack.words(length).size, pack.allowedWords(length).size, "Unexpected dictionary size for ${language.localeTag} length=$length")
                assertTrue(pack.words(length).all { it.size == length }, "Invalid token length for ${language.localeTag} length=$length")
                assertTrue(
                    pack.words(length).flatten().all { it in keyboardTokens },
                    "Keyboard is missing tokens for ${language.localeTag} length=$length",
                )
            }
        }
    }
}

