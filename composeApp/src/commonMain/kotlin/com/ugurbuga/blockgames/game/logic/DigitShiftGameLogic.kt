package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.DigitShiftGuess
import com.ugurbuga.blockgames.game.model.DigitShiftLetterState
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.settings.AppSettingsStorage
import kotlin.random.Random

class DigitShiftGameLogic(
    private val random: Random = Random.Default,
    scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {
    @Suppress("unused")
    private val ignoredScoreCalculator = scoreCalculator

    override fun restoreGame(state: GameState): GameState {
        val pack = languagePackFor(state.digitShiftLocaleTag)
        val expectedLength = resolveWordLength(
            solution = state.digitShiftSolution,
            configColumns = state.config.columns,
        )
        val solution = state.digitShiftSolution.takeIf { tokens ->
            tokens.size == expectedLength && tokens.all(pack::isSupportedToken)
        }
            ?: solutionForRound(
                pack = pack,
                roundIndex = state.linesCleared,
                challenge = state.activeChallenge,
                mode = state.gameMode,
            )
        val guesses = state.digitShiftGuesses.filter { guess ->
            guess.tokens.size == solution.size &&
                guess.states.size == solution.size &&
                guess.tokens.all(pack::isSupportedToken)
        }.take(digitShiftAttemptsForLength(solution.size))
        val keyboardHints = if (state.digitShiftKeyboardHints.isNotEmpty()) {
            state.digitShiftKeyboardHints.filterKeys(pack::isSupportedToken)
        } else {
            buildKeyboardHints(guesses)
        }
        val remainingTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
            (state.remainingTimeMillis ?: GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS).coerceAtLeast(0L)
        } else {
            null
        }
        val restoredStatus = when {
            state.status == GameStatus.GameOver -> GameStatus.GameOver
            state.gameMode == GameMode.TimeAttack && remainingTimeMillis == 0L -> GameStatus.GameOver
            else -> GameStatus.Running
        }
        val config = wordConfig(expectedLength)
        val restoredState = state.copy(
            config = config,
            gameplayStyle = GameplayStyle.DigitShift,
            board = com.ugurbuga.blockgames.game.model.BoardMatrix.empty(columns = config.columns, rows = config.rows),
            activePiece = null,
            nextQueue = emptyList(),
            holdPiece = null,
            canHold = false,
            lastPlacementColumn = null,
            digitShiftLocaleTag = pack.language.localeTag,
            digitShiftSolution = solution,
            digitShiftGuesses = guesses,
            digitShiftCurrentGuess = state.digitShiftCurrentGuess.filter(pack::isSupportedToken).take(expectedLength),
            digitShiftKeyboardHints = keyboardHints,
            digitShiftAwaitingNextRound = state.digitShiftAwaitingNextRound && restoredStatus == GameStatus.Running,
            remainingTimeMillis = remainingTimeMillis,
            status = restoredStatus,
            message = when {
                restoredStatus == GameStatus.GameOver && guesses.size >= digitShiftAttemptsForLength(solution.size) ->
                    gameText(GameTextKey.GameMessageDigitShiftFailed, solution.joinToString(""))
                state.message.key == GameTextKey.GameMessageSelectColumn -> gameText(GameTextKey.GameMessageDigitShiftEnterWord)
                else -> state.message
            },
        )
        return if (restoredState.digitShiftAwaitingNextRound) {
            startRound(
                state = restoredState,
                roundIndex = restoredState.linesCleared,
                message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
            )
        } else {
            restoredState
        }
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val pack = languagePackFor(AppSettingsStorage.load().language.localeTag)
        val initialChallenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) })
        return startRound(
            state = GameState(
                config = GameConfig.default(GameplayStyle.DigitShift),
                gameMode = mode,
                gameplayStyle = GameplayStyle.DigitShift,
                board = com.ugurbuga.blockgames.game.model.BoardMatrix.empty(
                    columns = DigitShiftDefaultWordLength,
                    rows = DigitShiftDefaultMaxAttempts,
                ),
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
                remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
                message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
                activeChallenge = initialChallenge,
                digitShiftLocaleTag = pack.language.localeTag,
                nextPieceId = random.nextLong().coerceAtLeast(1L),
            ),
            roundIndex = 0,
            message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
        )
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null

    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? = null

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> = emptySet()

    override fun placePiece(state: GameState, column: Int): GameMoveResult =
        GameMoveResult(state = state, events = setOf(GameEvent.InvalidDrop))

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult =
        GameMoveResult(state = state, events = setOf(GameEvent.InvalidDrop))

    override fun holdPiece(state: GameState): GameMoveResult = GameMoveResult(state)

    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult = GameMoveResult(state)

    override fun commitSoftLock(state: GameState): GameMoveResult = GameMoveResult(state)

    override fun reviveFromReward(state: GameState): GameMoveResult {
        if (state.status != GameStatus.GameOver || state.rewardedReviveUsed) return GameMoveResult(state)
        val revivedState = state.copy(
            status = GameStatus.Running,
            rewardedReviveUsed = true,
            digitShiftGuesses = emptyList(),
            digitShiftCurrentGuess = emptyList(),
            remainingTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
                (state.remainingTimeMillis ?: 0L) + GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS
            } else {
                null
            },
            digitShiftAwaitingNextRound = false,
            message = gameText(GameTextKey.GameMessageDigitShiftRevived),
        )
        return GameMoveResult(
            state = revivedState,
            events = setOf(GameEvent.Revived),
        )
    }

    override fun appendWordToken(state: GameState, token: String): GameMoveResult {
        if (state.status != GameStatus.Running || state.digitShiftAwaitingNextRound) return GameMoveResult(state)
        val pack = languagePackFor(state.digitShiftLocaleTag)
        if (!pack.isSupportedToken(token)) {
            return GameMoveResult(state = state, events = setOf(GameEvent.InvalidDrop))
        }
        if (state.digitShiftCurrentGuess.size >= resolveWordLength(state.digitShiftSolution, state.config.columns)) return GameMoveResult(state)
        return GameMoveResult(
            state = state.copy(
                digitShiftCurrentGuess = state.digitShiftCurrentGuess + token,
                message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
            )
        )
    }

    override fun deleteWordToken(state: GameState): GameMoveResult {
        if (state.status != GameStatus.Running || state.digitShiftAwaitingNextRound || state.digitShiftCurrentGuess.isEmpty()) return GameMoveResult(state)
        return GameMoveResult(
            state = state.copy(
                digitShiftCurrentGuess = state.digitShiftCurrentGuess.dropLast(1),
                message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
            )
        )
    }

    override fun submitWordGuess(state: GameState): GameMoveResult {
        if (state.status != GameStatus.Running || state.digitShiftAwaitingNextRound) return GameMoveResult(state)
        val guessTokens = state.digitShiftCurrentGuess
        val wordLength = resolveWordLength(state.digitShiftSolution, state.config.columns)
        if (guessTokens.size != wordLength) {
            return GameMoveResult(
                state = state.copy(message = gameText(GameTextKey.GameMessageDigitShiftNotEnoughLetters)),
                events = setOf(GameEvent.InvalidDrop),
            )
        }

        val evaluation = evaluate(solution = state.digitShiftSolution, guess = guessTokens)
        val guess = DigitShiftGuess(tokens = guessTokens, states = evaluation)
        val guesses = state.digitShiftGuesses + guess
        val keyboardHints = mergeKeyboardHints(state.digitShiftKeyboardHints, guess)

        if (guessTokens == state.digitShiftSolution) {
            val guessesUsed = guesses.size
            val maxAttempts = digitShiftAttemptsForLength(wordLength)
            val addedScore = 600 + ((maxAttempts - guessesUsed).coerceAtLeast(0) * 150)
            val score = state.score + addedScore
            val solvedWords = state.linesCleared + 1
            val updatedChallenge = updateChallenge(state.activeChallenge, score = score, solvedWords = solvedWords)
            val completedChallenge = state.activeChallenge?.isCompleted != true && updatedChallenge?.isCompleted == true
            val nextState = state.copy(
                score = score,
                linesCleared = solvedWords,
                level = 1 + (solvedWords / 3),
                difficultyStage = solvedWords / 3,
                activeChallenge = updatedChallenge,
                digitShiftGuesses = guesses,
                digitShiftKeyboardHints = keyboardHints,
                digitShiftCurrentGuess = emptyList(),
                digitShiftAwaitingNextRound = true,
                remainingTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
                    (state.remainingTimeMillis ?: 0L) + 8_000L
                } else {
                    null
                },
                message = gameText(GameTextKey.GameMessageDigitShiftSolved),
            )
            return GameMoveResult(
                state = nextState,
                events = buildSet {
                    add(GameEvent.PlacementAccepted)
                    add(GameEvent.LineClear)
                    if (completedChallenge) add(GameEvent.ChallengeCompleted)
                },
            )
        }

        val failedRound = guesses.size >= digitShiftAttemptsForLength(wordLength)
        val score = state.score
        val updatedChallenge = updateChallenge(state.activeChallenge, score = score, solvedWords = state.linesCleared)
        val completedChallenge = state.activeChallenge?.isCompleted != true && updatedChallenge?.isCompleted == true
        val nextState = state.copy(
            digitShiftGuesses = guesses,
            digitShiftCurrentGuess = emptyList(),
            digitShiftKeyboardHints = keyboardHints,
            digitShiftAwaitingNextRound = false,
            activeChallenge = updatedChallenge,
            status = if (failedRound) GameStatus.GameOver else GameStatus.Running,
            message = if (failedRound) {
                gameText(GameTextKey.GameMessageDigitShiftFailed, state.digitShiftSolution.joinToString(""))
            } else {
                gameText(GameTextKey.GameMessageDigitShiftKeepTrying)
            },
        )
        return GameMoveResult(
            state = nextState,
            events = buildSet {
                add(GameEvent.PlacementAccepted)
                if (failedRound) add(GameEvent.GameOver)
                if (completedChallenge) add(GameEvent.ChallengeCompleted)
            },
        )
    }

    override fun advanceWordRound(state: GameState): GameMoveResult {
        if (!state.digitShiftAwaitingNextRound || state.status != GameStatus.Running) return GameMoveResult(state)
        return GameMoveResult(
            state = startRound(
                state = state,
                roundIndex = state.linesCleared,
                message = gameText(GameTextKey.GameMessageDigitShiftEnterWord),
            ),
        )
    }

    override fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running || state.gameMode != GameMode.TimeAttack || state.digitShiftAwaitingNextRound) return state
        val remainingTimeMillis = (state.remainingTimeMillis ?: GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS) - 1_000L
        if (remainingTimeMillis > 0L) {
            return state.copy(remainingTimeMillis = remainingTimeMillis)
        }
        return state.copy(
            remainingTimeMillis = 0L,
            status = GameStatus.GameOver,
            message = gameText(GameTextKey.GameMessageDigitShiftFailed, state.digitShiftSolution.joinToString("")),
        )
    }

    private fun startRound(
        state: GameState,
        roundIndex: Int,
        message: com.ugurbuga.blockgames.game.model.GameText,
    ): GameState {
        val pack = languagePackFor(state.digitShiftLocaleTag)
        val solution = solutionForRound(
            pack = pack,
            roundIndex = roundIndex,
            challenge = state.activeChallenge,
            mode = state.gameMode,
        )
        val config = wordConfig(solution.size)
        return state.copy(
            config = config,
            board = com.ugurbuga.blockgames.game.model.BoardMatrix.empty(columns = config.columns, rows = config.rows),
            digitShiftLocaleTag = pack.language.localeTag,
            digitShiftSolution = solution,
            digitShiftGuesses = emptyList(),
            digitShiftCurrentGuess = emptyList(),
            digitShiftKeyboardHints = emptyMap(),
            digitShiftAwaitingNextRound = false,
            status = GameStatus.Running,
            message = message,
        )
    }

    private fun evaluate(
        solution: List<String>,
        guess: List<String>,
    ): List<DigitShiftLetterState> {
        val result = MutableList(guess.size) { DigitShiftLetterState.Absent }
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

    private fun buildKeyboardHints(guesses: List<DigitShiftGuess>): Map<String, DigitShiftLetterState> = guesses
        .fold(emptyMap(), ::mergeKeyboardHints)

    private fun mergeKeyboardHints(
        existing: Map<String, DigitShiftLetterState>,
        guess: DigitShiftGuess,
    ): Map<String, DigitShiftLetterState> {
        val updated = existing.toMutableMap()
        guess.tokens.zip(guess.states).forEach { (token, state) ->
            val previous = (updated[token] ?: DigitShiftLetterState.Unknown)
            if (state.priority >= previous.priority) {
                updated[token] = state
            }
        }
        return updated
    }

    private fun solutionForRound(
        pack: DigitShiftLanguagePack,
        roundIndex: Int,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): List<String> {
        val seededRandom = challenge?.let {
            Random(
                ((it.year * 10_000) + (it.month * 100) + it.day + (roundIndex * 31) + ((mode.ordinal + 1) * 17)),
            )
        } ?: random
        return pack.randomSolution(length = roundLengthForRound(roundIndex), random = seededRandom)
    }

    private fun languagePackFor(localeTag: String): DigitShiftLanguagePack {
        val effectiveLocaleTag = localeTag.ifBlank { AppSettingsStorage.load().language.localeTag }
        return DigitShiftLexicon.packFor(effectiveLocaleTag)
    }

    private fun resolveWordLength(
        solution: List<String>,
        configColumns: Int,
    ): Int = when {
        DigitShiftLexicon.isSupportedWordLength(solution.size) -> solution.size
        DigitShiftLexicon.isSupportedWordLength(configColumns) -> configColumns
        else -> DigitShiftDefaultWordLength
    }

    private fun wordConfig(length: Int): GameConfig = GameConfig(
        columns = length,
        rows = digitShiftAttemptsForLength(length),
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    private fun roundLengthForRound(roundIndex: Int): Int = when {
        roundIndex % 2 == 0 -> DigitShiftDefaultWordLength
        else -> 6
    }

    private fun updateChallenge(
        challenge: DailyChallenge?,
        score: Int,
        solvedWords: Int,
    ): DailyChallenge? {
        challenge ?: return null
        return challenge.copy(
            tasks = challenge.tasks.map { task ->
                when (task.type) {
                    ChallengeTaskType.ReachScore -> task.copy(current = score)
                    else -> if (task.type.stableId == "solve_words") task.copy(current = solvedWords) else task
                }
            },
        )
    }
}

private val DigitShiftLetterState.priority: Int
    get() = when (this) {
        DigitShiftLetterState.Unknown -> 0
        DigitShiftLetterState.Absent -> 1
        DigitShiftLetterState.Present -> 2
        DigitShiftLetterState.Correct -> 3
    }

