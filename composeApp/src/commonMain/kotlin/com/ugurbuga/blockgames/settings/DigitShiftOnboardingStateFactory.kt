package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.DigitShiftLanguagePack
import com.ugurbuga.blockgames.game.logic.DigitShiftLexicon
import com.ugurbuga.blockgames.game.logic.digitShiftAttemptsForLength
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameText
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.DigitShiftGuess
import com.ugurbuga.blockgames.game.model.DigitShiftLetterState
import com.ugurbuga.blockgames.game.model.gameText

@Immutable
enum class DigitShiftOnboardingStage : OnboardingStage {
    FirstGuess,
    ReadHints,
    SolveWord,
}

@Immutable
data class DigitShiftOnboardingScene(
    val stage: DigitShiftOnboardingStage,
    val gameState: GameState,
    val suggestedGuess: List<String>,
)

object DigitShiftOnboardingStateFactory {
    val stages: List<DigitShiftOnboardingStage> = listOf(
        DigitShiftOnboardingStage.FirstGuess,
        DigitShiftOnboardingStage.ReadHints,
        DigitShiftOnboardingStage.SolveWord,
    )

    fun initialState(): GameState = scene(stages.first()).gameState

    fun cleanGameState(): GameState =
        com.ugurbuga.blockgames.game.logic.DigitShiftGameLogic().newGame(
            config = GameConfig.default(GameplayStyle.DigitShift),
        )

    fun scene(stage: DigitShiftOnboardingStage): DigitShiftOnboardingScene {
        val pack = DigitShiftLexicon.packFor(AppSettingsStorage.load().language.localeTag)
        return buildScene(pack, stage)
    }

    private fun buildScene(
        pack: DigitShiftLanguagePack,
        stage: DigitShiftOnboardingStage,
    ): DigitShiftOnboardingScene {
        val solution = listOf("1", "2", "3", "4", "5")
        val guessA = listOf("1", "5", "6", "7", "8")
        val guessB = listOf("1", "2", "8", "4", "0")
        return when (stage) {
            DigitShiftOnboardingStage.FirstGuess -> DigitShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    localeTag = pack.language.localeTag,
                    solution = solution,
                    guesses = emptyList(),
                    message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
                ),
                suggestedGuess = guessA,
            )

            DigitShiftOnboardingStage.ReadHints -> DigitShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    localeTag = pack.language.localeTag,
                    solution = solution,
                    guesses = listOf(guessA),
                    message = gameText(GameTextKey.GameMessageDigitShiftKeepTrying),
                ),
                suggestedGuess = guessB,
            )

            DigitShiftOnboardingStage.SolveWord -> DigitShiftOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    localeTag = pack.language.localeTag,
                    solution = solution,
                    guesses = listOf(guessA, guessB),
                    message = gameText(GameTextKey.GameMessageDigitShiftKeepTrying),
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
            rows = digitShiftAttemptsForLength(solution.size),
            difficultyIntervalSeconds = 9_999,
            linesPerLevel = 9_999,
        )
        val evaluatedGuesses = guesses.map { guess ->
            DigitShiftGuess(tokens = guess, states = evaluate(solution, guess))
        }
        val keyboardHints = buildMap<String, DigitShiftLetterState> {
            evaluatedGuesses.forEach { guess ->
                guess.tokens.zip(guess.states).forEach { (token, state) ->
                    val previous = get(token) ?: DigitShiftLetterState.Unknown
                    if (state.priority >= previous.priority) {
                        put(token, state)
                    }
                }
            }
        }
        return GameState(
            config = config,
            gameplayStyle = GameplayStyle.DigitShift,
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
            digitShiftLocaleTag = localeTag,
            digitShiftSolution = solution,
            digitShiftGuesses = evaluatedGuesses,
            digitShiftCurrentGuess = emptyList(),
            digitShiftKeyboardHints = keyboardHints,
        )
    }

    private fun evaluate(
        solution: List<String>,
        guess: List<String>,
    ): List<DigitShiftLetterState> {
        val result = MutableList(solution.size) { DigitShiftLetterState.Absent }
        val remaining = solution.groupingBy { it }.eachCount().toMutableMap()
        guess.indices.forEach { index ->
            if (guess[index] == solution[index]) {
                result[index] = DigitShiftLetterState.Correct
                remaining[guess[index]] = (remaining[guess[index]] ?: 0) - 1
            }
        }
        guess.indices.forEach { index ->
            if (result[index] == DigitShiftLetterState.Correct) return@forEach
            val token = guess[index]
            if ((remaining[token] ?: 0) > 0) {
                result[index] = DigitShiftLetterState.Present
                remaining[token] = (remaining[token] ?: 0) - 1
            }
        }
        return result
    }
}

private val DigitShiftLetterState.priority: Int
    get() = when (this) {
        DigitShiftLetterState.Unknown -> 0
        DigitShiftLetterState.Absent -> 1
        DigitShiftLetterState.Present -> 2
        DigitShiftLetterState.Correct -> 3
    }

