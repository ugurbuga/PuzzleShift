package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.WordShiftLanguagePack
import com.ugurbuga.blockgames.game.logic.WordShiftDefaultWordLength
import com.ugurbuga.blockgames.game.logic.WordShiftLexicon
import com.ugurbuga.blockgames.game.logic.WordShiftMaxAttempts
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameText
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.WordShiftGuess
import com.ugurbuga.blockgames.game.model.WordShiftLetterState
import com.ugurbuga.blockgames.game.model.gameText

@Immutable
enum class WordShiftOnboardingStage : OnboardingStage {
    FirstGuess,
    ReadHints,
    SolveWord,
}

@Immutable
data class WordShiftOnboardingScene(
    val stage: WordShiftOnboardingStage,
    val gameState: GameState,
    val suggestedGuess: List<String>,
)

object WordShiftOnboardingStateFactory {
    val stages: List<WordShiftOnboardingStage> = listOf(
        WordShiftOnboardingStage.FirstGuess,
        WordShiftOnboardingStage.ReadHints,
        WordShiftOnboardingStage.SolveWord,
    )

    fun initialState(): GameState = scene(stages.first()).gameState

    fun cleanGameState(): GameState =
        com.ugurbuga.blockgames.game.logic.WordShiftGameLogic().newGame(
            config = GameConfig.default(GameplayStyle.WordShift),
        )

    fun scene(stage: WordShiftOnboardingStage): WordShiftOnboardingScene {
        val pack = WordShiftLexicon.packFor(AppSettingsStorage.load().language.localeTag)
        return buildScene(pack, stage)
    }

    private fun buildScene(
        pack: WordShiftLanguagePack,
        stage: WordShiftOnboardingStage,
    ): WordShiftOnboardingScene {
        val tutorialWords = pack.words(WordShiftDefaultWordLength)
        val guessA = tutorialWords[0]
        val solution = tutorialWords[1]
        val guessB = tutorialWords[2]
        return when (stage) {
            WordShiftOnboardingStage.FirstGuess -> WordShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    localeTag = pack.language.localeTag,
                    solution = solution,
                    guesses = emptyList(),
                    message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
                ),
                suggestedGuess = guessA,
            )

            WordShiftOnboardingStage.ReadHints -> WordShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    localeTag = pack.language.localeTag,
                    solution = solution,
                    guesses = listOf(guessA),
                    message = gameText(GameTextKey.GameMessageWordShiftKeepTrying),
                ),
                suggestedGuess = guessB,
            )

            WordShiftOnboardingStage.SolveWord -> WordShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    localeTag = pack.language.localeTag,
                    solution = solution,
                    guesses = listOf(guessA, guessB),
                    message = gameText(GameTextKey.GameMessageWordShiftKeepTrying),
                ),
                suggestedGuess = solution,
            )
        }
    }

    private fun scriptedState(
        localeTag: String,
        solution: List<String>,
        guesses: List<List<String>>,
        message: GameText,
    ): GameState {
        val config = GameConfig(
            columns = solution.size,
            rows = WordShiftMaxAttempts,
            difficultyIntervalSeconds = 9_999,
            linesPerLevel = 9_999,
        )
        val evaluatedGuesses = guesses.map { guess ->
            WordShiftGuess(tokens = guess, states = evaluate(solution, guess))
        }
        val keyboardHints = buildMap<String, WordShiftLetterState> {
            evaluatedGuesses.forEach { guess ->
                guess.tokens.zip(guess.states).forEach { (token, state) ->
                    val previous = get(token) ?: WordShiftLetterState.Unknown
                    if (state.priority >= previous.priority) {
                        put(token, state)
                    }
                }
            }
        }
        return GameState(
            config = config,
            gameplayStyle = GameplayStyle.WordShift,
            board = BoardMatrix.empty(columns = config.columns, rows = config.rows),
            activePiece = null,
            nextQueue = emptyList(),
            holdPiece = null,
            canHold = false,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = 9_999,
            status = GameStatus.Running,
            message = message,
            wordShiftLocaleTag = localeTag,
            wordShiftSolution = solution,
            wordShiftGuesses = evaluatedGuesses,
            wordShiftCurrentGuess = emptyList(),
            wordShiftKeyboardHints = keyboardHints,
        )
    }

    private fun evaluate(
        solution: List<String>,
        guess: List<String>,
    ): List<WordShiftLetterState> {
        val result = MutableList(solution.size) { WordShiftLetterState.Absent }
        val remaining = solution.groupingBy { it }.eachCount().toMutableMap()
        guess.indices.forEach { index ->
            if (guess[index] == solution[index]) {
                result[index] = WordShiftLetterState.Correct
                remaining[guess[index]] = (remaining[guess[index]] ?: 0) - 1
            }
        }
        guess.indices.forEach { index ->
            if (result[index] == WordShiftLetterState.Correct) return@forEach
            val token = guess[index]
            if ((remaining[token] ?: 0) > 0) {
                result[index] = WordShiftLetterState.Present
                remaining[token] = (remaining[token] ?: 0) - 1
            }
        }
        return result
    }
}

private val WordShiftLetterState.priority: Int
    get() = when (this) {
        WordShiftLetterState.Unknown -> 0
        WordShiftLetterState.Absent -> 1
        WordShiftLetterState.Present -> 2
        WordShiftLetterState.Correct -> 3
    }

