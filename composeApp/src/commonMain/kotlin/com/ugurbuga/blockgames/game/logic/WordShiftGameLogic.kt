package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.WordShiftGuess
import com.ugurbuga.blockgames.game.model.WordShiftLetterState
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.settings.AppSettingsStorage
import kotlin.random.Random

class WordShiftGameLogic(
    private val random: Random = Random.Default,
    scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {
    @Suppress("unused")
    private val ignoredScoreCalculator = scoreCalculator

    override fun restoreGame(state: GameState): GameState {
        val pack = languagePackFor(state.wordShiftLocaleTag)
        val expectedLength = resolveWordLength(
            solution = state.wordShiftSolution,
            configColumns = state.config.columns,
        )
        val solution = state.wordShiftSolution.takeIf { it.size == expectedLength }
            ?: solutionForRound(
                pack = pack,
                roundIndex = state.linesCleared,
                challenge = state.activeChallenge,
                mode = state.gameMode,
                preferredLength = expectedLength,
            )
        val guesses = state.wordShiftGuesses.filter { guess ->
            guess.tokens.size == solution.size && guess.states.size == solution.size
        }.take(WordShiftMaxAttempts)
        val keyboardHints = if (state.wordShiftKeyboardHints.isNotEmpty()) {
            state.wordShiftKeyboardHints
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
        val config = wordConfig(solution.size)
        val restoredState = state.copy(
            config = config,
            gameplayStyle = GameplayStyle.WordShift,
            board = com.ugurbuga.blockgames.game.model.BoardMatrix.empty(columns = config.columns, rows = config.rows),
            activePiece = null,
            nextQueue = emptyList(),
            holdPiece = null,
            canHold = false,
            lastPlacementColumn = null,
            wordShiftLocaleTag = pack.language.localeTag,
            wordShiftSolution = solution,
            wordShiftGuesses = guesses,
            wordShiftCurrentGuess = state.wordShiftCurrentGuess.take(solution.size),
            wordShiftKeyboardHints = keyboardHints,
            wordShiftAwaitingNextRound = state.wordShiftAwaitingNextRound && restoredStatus == GameStatus.Running,
            remainingTimeMillis = remainingTimeMillis,
            status = restoredStatus,
            message = when {
                restoredStatus == GameStatus.GameOver && guesses.size >= WordShiftMaxAttempts ->
                    gameText(GameTextKey.GameMessageWordShiftFailed, solution.joinToString(""))
                state.message.key == GameTextKey.GameMessageSelectColumn -> gameText(GameTextKey.GameMessageWordShiftEnterWord)
                else -> state.message
            },
        )
        return if (restoredState.wordShiftAwaitingNextRound) {
            startRound(
                state = restoredState,
                roundIndex = restoredState.linesCleared,
                message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
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
                config = GameConfig.default(GameplayStyle.WordShift),
                gameMode = mode,
                gameplayStyle = GameplayStyle.WordShift,
                board = com.ugurbuga.blockgames.game.model.BoardMatrix.empty(columns = WordShiftDefaultWordLength, rows = WordShiftMaxAttempts),
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
                message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
                activeChallenge = initialChallenge,
                wordShiftLocaleTag = pack.language.localeTag,
                nextPieceId = random.nextLong().coerceAtLeast(1L),
            ),
            roundIndex = 0,
            message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
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
            wordShiftGuesses = emptyList(),
            wordShiftCurrentGuess = emptyList(),
            remainingTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
                (state.remainingTimeMillis ?: 0L) + GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS
            } else {
                null
            },
            wordShiftAwaitingNextRound = false,
            message = gameText(GameTextKey.GameMessageWordShiftRevived),
        )
        return GameMoveResult(
            state = revivedState,
            events = setOf(GameEvent.Revived),
        )
    }

    override fun appendWordToken(state: GameState, token: String): GameMoveResult {
        if (state.status != GameStatus.Running || state.wordShiftAwaitingNextRound) return GameMoveResult(state)
        if (state.wordShiftCurrentGuess.size >= resolveWordLength(state.wordShiftSolution, state.config.columns)) return GameMoveResult(state)
        return GameMoveResult(
            state = state.copy(
                wordShiftCurrentGuess = state.wordShiftCurrentGuess + token,
                message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
            )
        )
    }

    override fun deleteWordToken(state: GameState): GameMoveResult {
        if (state.status != GameStatus.Running || state.wordShiftAwaitingNextRound || state.wordShiftCurrentGuess.isEmpty()) return GameMoveResult(state)
        return GameMoveResult(
            state = state.copy(
                wordShiftCurrentGuess = state.wordShiftCurrentGuess.dropLast(1),
                message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
            )
        )
    }

    override fun submitWordGuess(state: GameState): GameMoveResult {
        if (state.status != GameStatus.Running || state.wordShiftAwaitingNextRound) return GameMoveResult(state)
        val guessTokens = state.wordShiftCurrentGuess
        val wordLength = resolveWordLength(state.wordShiftSolution, state.config.columns)
        if (guessTokens.size != wordLength) {
            return GameMoveResult(
                state = state.copy(message = gameText(GameTextKey.GameMessageWordShiftNotEnoughLetters)),
                events = setOf(GameEvent.InvalidDrop),
            )
        }
        val pack = languagePackFor(state.wordShiftLocaleTag)
        if (WordShiftLexicon.keyOf(guessTokens) !in pack.allowedWords(wordLength)) {
            return GameMoveResult(
                state = state.copy(message = gameText(GameTextKey.GameMessageWordShiftNotInDictionary)),
                events = setOf(GameEvent.InvalidDrop),
            )
        }

        val evaluation = evaluate(solution = state.wordShiftSolution, guess = guessTokens)
        val guess = WordShiftGuess(tokens = guessTokens, states = evaluation)
        val guesses = state.wordShiftGuesses + guess
        val keyboardHints = mergeKeyboardHints(state.wordShiftKeyboardHints, guess)

        if (guessTokens == state.wordShiftSolution) {
            val guessesUsed = guesses.size
            val addedScore = 600 + ((WordShiftMaxAttempts - guessesUsed).coerceAtLeast(0) * 150)
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
                wordShiftGuesses = guesses,
                wordShiftKeyboardHints = keyboardHints,
                wordShiftCurrentGuess = emptyList(),
                wordShiftAwaitingNextRound = true,
                remainingTimeMillis = if (state.gameMode == GameMode.TimeAttack) {
                    (state.remainingTimeMillis ?: 0L) + 8_000L
                } else {
                    null
                },
                message = gameText(GameTextKey.GameMessageWordShiftSolved),
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

        val failedRound = guesses.size >= WordShiftMaxAttempts
        val score = state.score
        val updatedChallenge = updateChallenge(state.activeChallenge, score = score, solvedWords = state.linesCleared)
        val completedChallenge = state.activeChallenge?.isCompleted != true && updatedChallenge?.isCompleted == true
        val nextState = state.copy(
            wordShiftGuesses = guesses,
            wordShiftCurrentGuess = emptyList(),
            wordShiftKeyboardHints = keyboardHints,
            wordShiftAwaitingNextRound = false,
            activeChallenge = updatedChallenge,
            status = if (failedRound) GameStatus.GameOver else GameStatus.Running,
            message = if (failedRound) {
                gameText(GameTextKey.GameMessageWordShiftFailed, state.wordShiftSolution.joinToString(""))
            } else {
                gameText(GameTextKey.GameMessageWordShiftKeepTrying)
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
        if (!state.wordShiftAwaitingNextRound || state.status != GameStatus.Running) return GameMoveResult(state)
        return GameMoveResult(
            state = startRound(
                state = state,
                roundIndex = state.linesCleared,
                message = gameText(GameTextKey.GameMessageWordShiftEnterWord),
            ),
        )
    }

    override fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running || state.gameMode != GameMode.TimeAttack || state.wordShiftAwaitingNextRound) return state
        val remainingTimeMillis = (state.remainingTimeMillis ?: GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS) - 1_000L
        if (remainingTimeMillis > 0L) {
            return state.copy(remainingTimeMillis = remainingTimeMillis)
        }
        return state.copy(
            remainingTimeMillis = 0L,
            status = GameStatus.GameOver,
            message = gameText(GameTextKey.GameMessageWordShiftFailed, state.wordShiftSolution.joinToString("")),
        )
    }

    private fun startRound(
        state: GameState,
        roundIndex: Int,
        message: com.ugurbuga.blockgames.game.model.GameText,
    ): GameState {
        val pack = languagePackFor(state.wordShiftLocaleTag)
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
            wordShiftLocaleTag = pack.language.localeTag,
            wordShiftSolution = solution,
            wordShiftGuesses = emptyList(),
            wordShiftCurrentGuess = emptyList(),
            wordShiftKeyboardHints = emptyMap(),
            wordShiftAwaitingNextRound = false,
            status = GameStatus.Running,
            message = message,
        )
    }

    private fun evaluate(
        solution: List<String>,
        guess: List<String>,
    ): List<WordShiftLetterState> {
        val result = MutableList(guess.size) { WordShiftLetterState.Absent }
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

    private fun buildKeyboardHints(guesses: List<WordShiftGuess>): Map<String, WordShiftLetterState> = guesses
        .fold(emptyMap(), ::mergeKeyboardHints)

    private fun mergeKeyboardHints(
        existing: Map<String, WordShiftLetterState>,
        guess: WordShiftGuess,
    ): Map<String, WordShiftLetterState> {
        val updated = existing.toMutableMap()
        guess.tokens.zip(guess.states).forEach { (token, state) ->
            val previous = (updated[token] ?: WordShiftLetterState.Unknown)
            if (state.priority >= previous.priority) {
                updated[token] = state
            }
        }
        return updated
    }

    private fun solutionForRound(
        pack: WordShiftLanguagePack,
        roundIndex: Int,
        challenge: DailyChallenge?,
        mode: GameMode,
        preferredLength: Int? = null,
    ): List<String> {
        val length = preferredLength?.takeIf { it in pack.supportedLengths }
            ?: roundLengthForRound(pack, roundIndex)
        val words = pack.words(length)
        val seedBase = challenge?.let { (it.year * 10_000) + (it.month * 100) + it.day }
            ?: (((pack.language.ordinal + 1) * 31) + ((mode.ordinal + 1) * 17))
        val index = ((seedBase + (roundIndex * 7)) % words.size).let { if (it < 0) it + words.size else it }
        return words[index]
    }

    private fun languagePackFor(localeTag: String): WordShiftLanguagePack {
        val effectiveLocaleTag = localeTag.ifBlank { AppSettingsStorage.load().language.localeTag }
        return WordShiftLexicon.packFor(effectiveLocaleTag)
    }

    private fun roundLengthForRound(
        pack: WordShiftLanguagePack,
        roundIndex: Int,
    ): Int {
        val preferredOrder = listOf(WordShiftDefaultWordLength, 4, 6, 7)
            .filter { it in pack.supportedLengths }
        val lengths = if (preferredOrder.isNotEmpty()) preferredOrder else pack.supportedLengths
        return lengths[roundIndex.mod(lengths.size)]
    }

    private fun resolveWordLength(
        solution: List<String>,
        configColumns: Int,
    ): Int = when {
        WordShiftLexicon.isSupportedWordLength(solution.size) -> solution.size
        WordShiftLexicon.isSupportedWordLength(configColumns) -> configColumns
        else -> WordShiftDefaultWordLength
    }

    private fun wordConfig(wordLength: Int): GameConfig = GameConfig(
        columns = wordLength,
        rows = WordShiftMaxAttempts,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

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

private val WordShiftLetterState.priority: Int
    get() = when (this) {
        WordShiftLetterState.Unknown -> 0
        WordShiftLetterState.Absent -> 1
        WordShiftLetterState.Present -> 2
        WordShiftLetterState.Correct -> 3
    }

