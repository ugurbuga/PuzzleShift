package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.AppLanguage

internal const val WordShiftMinWordLength = 4
internal const val WordShiftDefaultWordLength = 5
internal const val WordShiftMaxWordLength = 7
internal const val WordShiftWordsPerLanguage = 1_000
internal const val WordShiftMaxAttempts = 6

internal data class WordShiftLanguagePack(
    val language: AppLanguage,
    val wordsByLength: Map<Int, List<List<String>>>,
    private val keyboardRows: List<List<String>>,
) {
    private val allowedWordsByLength: Map<Int, Set<String>> = wordsByLength.mapValues { (_, words) ->
        words.mapTo(linkedSetOf(), WordShiftLexicon::keyOf)
    }

    val supportedLengths: List<Int> = wordsByLength.keys.sorted()
    val totalWordCount: Int = wordsByLength.values.sumOf { it.size }

    fun words(length: Int): List<List<String>> = wordsByLength[length] ?: emptyList()

    fun allowedWords(length: Int): Set<String> = allowedWordsByLength[length] ?: emptySet()

    fun keyboardRows(@Suppress("UNUSED_PARAMETER") length: Int): List<List<String>> = keyboardRows
}

internal object WordShiftLexicon {
    private val packs: Map<AppLanguage, WordShiftLanguagePack> = mapOf(
        AppLanguage.English to pack(
            language = AppLanguage.English,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P",
                "A S D F G H J K L",
                "Z X C V B N M",
            ),
        ),
        AppLanguage.Turkish to pack(
            language = AppLanguage.Turkish,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P Ğ Ü",
                "A S D F G H J K L Ş İ",
                "Z X C V B N M Ö Ç",
            ),
        ),
        AppLanguage.Spanish to pack(
            language = AppLanguage.Spanish,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P",
                "A S D F G H J K L Ñ",
                "Z X C V B N M",
            ),
        ),
        AppLanguage.French to pack(
            language = AppLanguage.French,
            keyboardRows = keyboardRows(
                "A Z E R T Y U I O P",
                "Q S D F G H J K L M",
                "W X C V B N Ç À Â É È Ê Ë Î Ï Ô Ù Ü",
            ),
        ),
        AppLanguage.German to pack(
            language = AppLanguage.German,
            keyboardRows = keyboardRows(
                "Q W E R T Z U I O P Ü",
                "A S D F G H J K L Ö Ä",
                "Y X C V B N M ß",
            ),
        ),
        AppLanguage.Russian to pack(
            language = AppLanguage.Russian,
            keyboardRows = keyboardRows(
                "Ё Й Ц У К Е Н Г Ш Щ З Х Ъ",
                "Ф Ы В А П Р О Л Д Ж Э",
                "Я Ч С М И Т Ь Б Ю",
            ),
        ),
        AppLanguage.ChineseSimplified to pack(
            language = AppLanguage.ChineseSimplified,
            keyboardRows = keyboardRows(
                "春 风 细 雨 声 晨 光 照 书 页 月",
                "远 山 白 云 间 星 河 入 梦 来 花",
                "海 吹 树 叶 彩 灯 映 长 街 夜",
            ),
        ),
        AppLanguage.Hindi to pack(
            language = AppLanguage.Hindi,
            keyboardRows = keyboardRows(
                "आ का श गं गा न दी कि ना रा सू",
                "र ज मु खी मे घ ध नु ष चां",
                "द नी त गु ल मो ह प व सा",
            ),
        ),
        AppLanguage.Arabic to pack(
            language = AppLanguage.Arabic,
            keyboardRows = keyboardRows(
                "ض ص ث ق ف غ ع ه خ ح ج د",
                "ش س ي ب ل ا ت ن م ك ط",
                "ئ ء ؤ ر ى ة و ز ظ ذ",
            ),
        ),
        AppLanguage.Portuguese to pack(
            language = AppLanguage.Portuguese,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P",
                "A S D F G H J K L Ç",
                "Z X C V B N M Á Â Ã É Ê Í Ó Ô Õ Ú",
            ),
        ),
        AppLanguage.Indonesian to pack(
            language = AppLanguage.Indonesian,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P",
                "A S D F G H J K L",
                "Z X C V B N M",
            ),
        ),
        AppLanguage.Japanese to pack(
            language = AppLanguage.Japanese,
            keyboardRows = keyboardRows(
                "あ い う え お か き く け こ が ぐ",
                "さ し す せ そ た ち つ て と な の",
                "は ま も や ゆ ら り ろ わ を ん ぶ ご",
            ),
        ),
        AppLanguage.Korean to pack(
            language = AppLanguage.Korean,
            keyboardRows = keyboardRows(
                "무 지 개 다 리 초 록 나 잎",
                "별 빛 하 늘 길 아 침 이 슬",
                "바 람 결 소 달",
            ),
        ),
        AppLanguage.Italian to pack(
            language = AppLanguage.Italian,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P",
                "A S D F G H J K L",
                "Z X C V B N M À È É Ì Ò Ù",
            ),
        ),
        AppLanguage.Dutch to pack(
            language = AppLanguage.Dutch,
            keyboardRows = keyboardRows(
                "Q W E R T Y U I O P",
                "A S D F G H J K L",
                "Z X C V B N M É Ë Ï Ĳ",
            ),
        ),
    )

    fun packFor(localeTag: String): WordShiftLanguagePack {
        val language = AppLanguage.fromDeviceLocaleTag(localeTag) ?: AppLanguage.English
        return packs[language] ?: packs.getValue(AppLanguage.English)
    }

    fun isSupportedWordLength(length: Int): Boolean = length in WordShiftMinWordLength..WordShiftMaxWordLength

    fun keyOf(tokens: List<String>): String = tokens.joinToString(separator = "|")

    private fun pack(
        language: AppLanguage,
        keyboardRows: List<List<String>> = emptyList(),
    ): WordShiftLanguagePack {
        val wordsByLength = parseWordsByLength(
            wordListText = GeneratedWordShiftWordLists.csvFor(language),
            keyboardRows = keyboardRows,
        )
        val resolvedKeyboardRows = keyboardRows.takeIf { it.isNotEmpty() } ?: deriveKeyboardRows(wordsByLength)
        require(resolvedKeyboardRows.size == 3) {
            "WordShift keyboard must contain exactly 3 rows for ${language.localeTag}"
        }
        validateKeyboardCoverage(language, wordsByLength, resolvedKeyboardRows)
        return WordShiftLanguagePack(
            language = language,
            wordsByLength = wordsByLength,
            keyboardRows = resolvedKeyboardRows,
        )
    }

    private fun parseWordsByLength(
        wordListText: String,
        keyboardRows: List<List<String>>,
    ): Map<Int, List<List<String>>> {
        val uniqueWords = linkedMapOf<String, List<String>>()
        wordListText.split(Regex("[,\\r\\n]+"))
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { parseWordEntry(it, keyboardRows.flatten()) }
            .forEach { tokens ->
                val key = keyOf(tokens)
                if (key !in uniqueWords) {
                    uniqueWords[key] = tokens
                }
            }

        return (WordShiftMinWordLength..WordShiftMaxWordLength).associateWith { length ->
            uniqueWords.values.filter { it.size == length }
        }.also { grouped ->
            require(uniqueWords.size == WordShiftWordsPerLanguage) {
                "WordShift txt word list must provide exactly $WordShiftWordsPerLanguage entries separated by new lines or commas, found=${uniqueWords.size}"
            }
            grouped.forEach { (length, words) ->
                require(words.isNotEmpty()) {
                    "WordShift txt word list must include at least one word for length=$length"
                }
            }
        }
    }

    private fun parseWordEntry(
        entry: String,
        keyboardTokens: List<String>,
    ): List<String> {
        val spacedTokens = entry.split(Regex("\\s+")).filter(String::isNotBlank)
        return if (spacedTokens.size > 1) {
            spacedTokens
        } else {
            tokenizeCompactedEntry(entry, keyboardTokens)
        }
    }

    private fun tokenizeCompactedEntry(
        entry: String,
        keyboardTokens: List<String>,
    ): List<String> {
        val normalized = entry.replace(Regex("\\s+"), "")
        if (normalized.isBlank()) return emptyList()
        if (keyboardTokens.isEmpty()) return normalized.map(Char::toString)

        val candidates = keyboardTokens
            .asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .sortedWith(compareByDescending<String> { it.length }.thenBy { it })
            .toList()

        val tokens = mutableListOf<String>()
        var index = 0
        while (index < normalized.length) {
            val match = candidates.firstOrNull { candidate ->
                normalized.startsWith(candidate, startIndex = index)
            } ?: return normalized.map(Char::toString)
            tokens += match
            index += match.length
        }
        return tokens
    }

    private fun validateKeyboardCoverage(
        language: AppLanguage,
        wordsByLength: Map<Int, List<List<String>>>,
        keyboardRows: List<List<String>>,
    ) {
        val keyboardTokens = keyboardRows.flatten().toSet()
        val missingTokens = wordsByLength.values
            .flatten()
            .flatten()
            .filter { it !in keyboardTokens }
            .distinct()
        require(missingTokens.isEmpty()) {
            "Keyboard layout for ${language.localeTag} is missing tokens: $missingTokens"
        }
    }

    private fun deriveKeyboardRows(wordsByLength: Map<Int, List<List<String>>>): List<List<String>> {
        val uniqueTokens = linkedSetOf<String>()
        wordsByLength.values
            .flatten()
            .flatten()
            .forEach(uniqueTokens::add)
        val rowCount = when {
            uniqueTokens.size <= 10 -> 1
            uniqueTokens.size <= 20 -> 2
            uniqueTokens.size <= 30 -> 3
            else -> 4
        }
        val chunkSize = ((uniqueTokens.size + rowCount - 1) / rowCount).coerceAtLeast(1)
        return uniqueTokens.toList().chunked(chunkSize)
    }

    private fun keyboardRows(vararg rows: String): List<List<String>> = rows.map { row ->
        row.split(Regex("\\s+")).filter(String::isNotBlank)
    }
}

