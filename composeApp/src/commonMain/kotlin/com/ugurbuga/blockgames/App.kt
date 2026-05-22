package com.ugurbuga.blockgames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.cancel
import blockgames.composeapp.generated.resources.game_message_ad_reward_blocksort
import blockgames.composeapp.generated.resources.game_message_ad_reward_blockwise
import blockgames.composeapp.generated.resources.game_message_ad_reward_boomblocks
import blockgames.composeapp.generated.resources.game_message_ad_reward_chainshift
import blockgames.composeapp.generated.resources.game_message_ad_reward_mergeshift
import blockgames.composeapp.generated.resources.leave_session_confirm
import blockgames.composeapp.generated.resources.leave_session_confirm_body
import blockgames.composeapp.generated.resources.leave_session_confirm_title
import blockgames.composeapp.generated.resources.reward_earned_message
import blockgames.composeapp.generated.resources.reward_piece_column_message
import blockgames.composeapp.generated.resources.reward_piece_row_message
import com.ugurbuga.blockgames.ads.AppFooterAdSlot
import com.ugurbuga.blockgames.ads.rememberPlatformGameAdController
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.localization.AppEnvironment
import com.ugurbuga.blockgames.localization.appStringResource
import com.ugurbuga.blockgames.localization.currentDeviceLocaleTag
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.platform.getCurrentDate
import com.ugurbuga.blockgames.platform.rememberNotificationManager
import com.ugurbuga.blockgames.presentation.game.GameViewModel
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.AppSettingsStorage
import com.ugurbuga.blockgames.settings.BlockSortOnboardingStateFactory
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import com.ugurbuga.blockgames.settings.BoomBlocksOnboardingStateFactory
import com.ugurbuga.blockgames.settings.ChainShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.GameSessionSlot
import com.ugurbuga.blockgames.settings.GameSessionStorage
import com.ugurbuga.blockgames.settings.HighScoreStorage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.RewardedTokenAdReward
import com.ugurbuga.blockgames.settings.StackShiftGameOnboardingStateFactory
import com.ugurbuga.blockgames.settings.awardBonusTokens
import com.ugurbuga.blockgames.settings.awardCompletedChallenge
import com.ugurbuga.blockgames.settings.awardScoreTokens
import com.ugurbuga.blockgames.settings.hasSeenTutorialFor
import com.ugurbuga.blockgames.settings.hasShownInteractiveOnboardingFor
import com.ugurbuga.blockgames.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.blockgames.settings.logLanguageBootstrapDecision
import com.ugurbuga.blockgames.settings.markInteractiveOnboardingShown
import com.ugurbuga.blockgames.settings.markTutorialSeen
import com.ugurbuga.blockgames.settings.recordAppOpened
import com.ugurbuga.blockgames.settings.sanitized
import com.ugurbuga.blockgames.settings.sessionSlot
import com.ugurbuga.blockgames.settings.sessionSlotFor
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryActionNames
import com.ugurbuga.blockgames.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.blockgames.telemetry.rememberAppTelemetry
import com.ugurbuga.blockgames.ui.game.RewardFeedbackCard
import com.ugurbuga.blockgames.ui.game.ThemedConfirmDialog
import com.ugurbuga.blockgames.ui.game.dailychallenge.DailyChallengeScreen
import com.ugurbuga.blockgames.ui.game.game.BlockGamesGameApp
import com.ugurbuga.blockgames.ui.game.gametutorial.GameTutorialScreen
import com.ugurbuga.blockgames.ui.game.home.HomeScreen
import com.ugurbuga.blockgames.ui.game.selection.AppSelectionScreen
import com.ugurbuga.blockgames.ui.game.settings.AppSettingsScreen
import com.ugurbuga.blockgames.ui.theme.LocalBlockGamesUiColors
import com.ugurbuga.blockgames.ui.theme.blockGamesThemeSpec
import com.ugurbuga.blockgames.ui.theme.blockGamesTypography
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration.Companion.milliseconds

enum class AppRoute {
    Home,
    Game,
    InteractiveOnboarding,
    Settings,
    Tutorial,
    DailyChallenges,
    Selection,
}

internal fun isUsableSavedSession(
    state: GameState,
    slot: GameSessionSlot,
): Boolean {
    if (state.gameplayStyle == GameplayStyle.BlockSort) {
        if (state.config.columns <= 0 || state.config.rows <= 0) return false
    } else {
        val defaultConfig = GameConfig.default(state.gameplayStyle)
        if (state.config.columns != defaultConfig.columns || state.config.rows != defaultConfig.rows) return false
    }

    if (slot is GameSessionSlot.DailyChallenge) {
        val activeChallenge = state.activeChallenge ?: return false
        val normalizedSlotDateId =
            "${activeChallenge.year}-${activeChallenge.month.toString().padStart(2, '0')}-${activeChallenge.day.toString().padStart(2, '0')}"
        if (slot.dateId != normalizedSlotDateId) return false
    }
    if (state.status != GameStatus.Running) return true
    return when (state.gameplayStyle) {
        GameplayStyle.BlockWise -> state.trayPieces.isNotEmpty()
        GameplayStyle.StackShift -> state.activePiece != null
        GameplayStyle.ChainShift -> state.activePiece != null
        GameplayStyle.MergeShift -> state.activePiece != null
        GameplayStyle.BoomBlocks -> true
        GameplayStyle.BlockSort -> state.board.occupiedCount > 0
    }
}

internal fun resolveStartupRoute(
    loadedSettings: AppSettings,
    startupGameplayStyleOverride: GameplayStyle? = null,
): AppRoute = when {
    startupGameplayStyleOverride != null -> AppRoute.Home
    loadedSettings.selectedGameplayStyle == null -> AppRoute.Selection
    else -> AppRoute.Home
}

internal fun resolveStartupGameplayStyle(
    loadedSettings: AppSettings,
    startupGameplayStyleOverride: GameplayStyle? = null,
): GameplayStyle =
    startupGameplayStyleOverride ?: loadedSettings.selectedGameplayStyle ?: GameplayStyle.StackShift

internal fun resolvePersistedStartupGameplayStyle(
    loadedSettings: AppSettings,
    startupGameplayStyleOverride: GameplayStyle? = null,
): GameplayStyle? =
    startupGameplayStyleOverride?.takeIf { it != loadedSettings.selectedGameplayStyle }

@Composable
fun BlockGamesTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = isBlockGamesDarkTheme(settings)
    val themeSpec = blockGamesThemeSpec(
        settings = settings,
        darkTheme = darkTheme,
    )
    PlatformSystemBarsEffect(darkTheme = darkTheme)
    CompositionLocalProvider(LocalBlockGamesUiColors provides themeSpec.uiColors) {
        val typography = blockGamesTypography()
        MaterialTheme(
            colorScheme = themeSpec.colorScheme,
            typography = typography,
            content = content,
        )
    }
}

data class RewardFeedbackState(
    val messageRes: StringResource? = null,
    val messageArgs: List<Any> = emptyList(),
    val icon: ImageVector = Icons.Filled.Stars,
    val visible: Boolean = false
)

internal data class RewardFeedbackSpec(
    val messageRes: StringResource,
    val icon: ImageVector,
)

internal fun rewardedDockFeedbackSpec(
    gameplayStyle: GameplayStyle,
    specialType: SpecialBlockType,
): RewardFeedbackSpec {
    return when (gameplayStyle) {
        GameplayStyle.StackShift -> {
            if (specialType == SpecialBlockType.RowClearer) {
                RewardFeedbackSpec(
                    messageRes = Res.string.reward_piece_row_message,
                    icon = Icons.Filled.SwapHoriz,
                )
            } else {
                RewardFeedbackSpec(
                    messageRes = Res.string.reward_piece_column_message,
                    icon = Icons.Filled.SwapVert,
                )
            }
        }

        GameplayStyle.BlockWise -> RewardFeedbackSpec(
            messageRes = Res.string.game_message_ad_reward_blockwise,
            icon = Icons.Filled.Refresh,
        )

        GameplayStyle.ChainShift -> RewardFeedbackSpec(
            messageRes = Res.string.game_message_ad_reward_chainshift,
            icon = Icons.Filled.Refresh,
        )

        GameplayStyle.MergeShift -> RewardFeedbackSpec(
            messageRes = Res.string.game_message_ad_reward_mergeshift,
            icon = Icons.Filled.Refresh,
        )

        GameplayStyle.BoomBlocks -> RewardFeedbackSpec(
            messageRes = Res.string.game_message_ad_reward_boomblocks,
            icon = Icons.Filled.AutoAwesome,
        )

        GameplayStyle.BlockSort -> RewardFeedbackSpec(
            messageRes = Res.string.game_message_ad_reward_blocksort,
            icon = Icons.Filled.Refresh,
        )
    }
}

@Composable
fun BlockGamesRoot(
    settings: AppSettings,
    telemetry: AppTelemetry,
    gameViewModel: GameViewModel,
    classicHighScore: Int,
    timeAttackHighScore: Int,
    rewardFeedback: RewardFeedbackState,
    onRewardFeedbackDismiss: () -> Unit,
    onPlayRequested: () -> Unit,
    onTimeAttackRequested: () -> Unit,
    onPlayChallengeRequested: (DailyChallenge) -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateBack: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onRewardedTokensEarned: () -> Unit,
    onReplaceActivePieceRewarded: (SpecialBlockType) -> Unit,
    onTutorialFinishRequested: () -> Unit,
    onInteractiveOnboardingFinished: (GameState) -> Unit,
    onInteractiveOnboardingReturnHome: (GameState) -> Unit,
    onGameplayStyleSelected: (GameplayStyle) -> Unit,
    onNavigateToSelection: () -> Unit,
    settingsTabIndex: Int,
    onSettingsTabIndexChange: (Int) -> Unit,
    currentRoute: AppRoute = AppRoute.Home,
    showLeaveSessionDialog: Boolean = false,
    onDismissLeaveSessionDialog: () -> Unit = {},
    onConfirmLeaveSessionDialog: () -> Unit = {},
) {
    val adController = rememberPlatformGameAdController()
    val notificationManager = rememberNotificationManager()

    AppEnvironment(settings = settings) {
        LaunchedEffect(Unit) {
            onSettingsChange(settings.recordAppOpened(currentEpochMillis()))
        }
        LaunchedEffect(settings.challengeProgress, settings.lastAppOpenedAtEpochMillis) {
            notificationManager.cancelDailyChallengeNotification()
            notificationManager.scheduleDailyChallengeNotification()
            notificationManager.cancelMissYouNotification()
            notificationManager.scheduleMissYouNotification()
        }
        notificationManager.RequestPermission()
        BlockGamesTheme(settings = settings) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        when (currentRoute) {
                            AppRoute.Home -> {
                                HomeScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    settings = settings,
                                    classicHighScore = classicHighScore,
                                    timeAttackHighScore = timeAttackHighScore,
                                    telemetry = telemetry,
                                    onPlay = onPlayRequested,
                                    onPlayTimeAttack = onTimeAttackRequested,
                                    onOpenTutorial = {
                                        telemetry.logUserAction(TelemetryActionNames.OpenTutorial)
                                        onNavigateToTutorial()
                                    },
                                    onOpenTheme = {
                                        telemetry.logUserAction(TelemetryActionNames.OpenTheme)
                                        onNavigateToTheme()
                                    },
                                    onOpenLanguage = {
                                        telemetry.logUserAction(TelemetryActionNames.OpenLanguage)
                                        onNavigateToLanguage()
                                    },
                                    onOpenChallenges = {
                                        onNavigateToChallenges()
                                    },
                                    onSwitchGame = onNavigateToSelection,
                                    notificationManager = notificationManager,
                                )
                            }

                            AppRoute.DailyChallenges -> {
                                val date = remember { getCurrentDate() }
                                DailyChallengeScreen(
                                    currentYear = date.year,
                                    currentMonth = date.month,
                                    currentDay = date.day,
                                    progress = settings.challengeProgress,
                                    onBack = onNavigateBack,
                                    onPlayChallenge = onPlayChallengeRequested,
                                )
                            }

                            AppRoute.Settings -> {
                                AppSettingsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    settings = settings,
                                    onSettingsChange = onSettingsChange,
                                    onRewardedTokensRequested = onRewardedTokensEarned,
                                    onBack = onNavigateBack,
                                    onOpenSelection = onNavigateToSelection,
                                    adController = adController,
                                    selectedTabIndex = settingsTabIndex,
                                    onSelectedTabIndexChange = onSettingsTabIndexChange,
                                )
                            }

                            AppRoute.Tutorial -> {
                                GameTutorialScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    onBack = onNavigateBack,
                                    onFinish = onTutorialFinishRequested,
                                    adController = adController,
                                )
                            }

                            AppRoute.Game,
                            AppRoute.InteractiveOnboarding,
                                -> {
                                BlockGamesGameApp(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    viewModel = gameViewModel,
                                    interactiveOnboardingEnabled = currentRoute == AppRoute.InteractiveOnboarding,
                                    onInteractiveOnboardingFinished = { finalState ->
                                        onInteractiveOnboardingFinished(finalState)
                                    },
                                    onInteractiveOnboardingReturnHome = onInteractiveOnboardingReturnHome,
                                    onReplaceActivePieceRewarded = onReplaceActivePieceRewarded,
                                    onBack = onNavigateBack,
                                    adController = adController,
                                )
                            }

                            AppRoute.Selection -> {
                                AppSelectionScreen(
                                    currentStyle = settings.selectedGameplayStyle,
                                    onGameplayStyleSelected = onGameplayStyleSelected,
                                    telemetry = telemetry,
                                    onBack = onNavigateBack,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    if (currentRoute != AppRoute.Settings) {
                        AppFooterAdSlot(
                            adController = adController,
                            onOpenSelection = onNavigateToSelection,
                        )
                    }
                }

                if (showLeaveSessionDialog) {
                    LeaveSessionConfirmDialog(
                        onDismissRequest = onDismissLeaveSessionDialog,
                        onConfirm = onConfirmLeaveSessionDialog,
                    )
                }

                RewardFeedbackCard(
                    message = if (rewardFeedback.messageRes != null) {
                        appStringResource(
                            rewardFeedback.messageRes,
                            *rewardFeedback.messageArgs.toTypedArray()
                        )
                    } else "",
                    icon = rewardFeedback.icon,
                    visible = rewardFeedback.visible,
                    onDismiss = onRewardFeedbackDismiss
                )
            }
        }
    }
}

@Composable
@Preview
fun App() {
    BlockGamesAppHost(bootstrapLogSource = "app_common")
}

@Composable
fun BlockGamesAppHost(
    bootstrapLogSource: String,
    startupGameplayStyleOverride: GameplayStyle? = null,
    beforeRoot: @Composable (settings: AppSettings, canNavigateBack: Boolean, onRequestBack: () -> Unit) -> Unit = { _, _, _ -> },
) {
    val telemetry = rememberAppTelemetry()
    val initialBootstrapResult = remember(startupGameplayStyleOverride) {
        val loadedSettings = AppSettingsStorage.load()
        val startupGameplayStyle = resolveStartupGameplayStyle(
            loadedSettings = loadedSettings,
            startupGameplayStyleOverride = startupGameplayStyleOverride,
        )
        val persistedStartupGameplayStyle = resolvePersistedStartupGameplayStyle(
            loadedSettings = loadedSettings,
            startupGameplayStyleOverride = startupGameplayStyleOverride,
        )
        val settingsToBootstrap = if (persistedStartupGameplayStyle != null) {
            loadedSettings.copy(selectedGameplayStyle = persistedStartupGameplayStyle)
        } else {
            loadedSettings
        }
        val bootstrapResult = initializeAppSettingsForFirstLaunch(
            settings = settingsToBootstrap,
            deviceLocaleTag = currentDeviceLocaleTag(),
        )
        GlobalPlatformConfig.gameplayStyle = bootstrapResult.settings.selectedGameplayStyle ?: startupGameplayStyle
        bootstrapResult.copy(
            shouldPersist = bootstrapResult.shouldPersist || persistedStartupGameplayStyle != null,
        )
    }
    var settings by remember(initialBootstrapResult) { mutableStateOf(initialBootstrapResult.settings.sanitized()) }
    var settingsTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(settings.selectedGameplayStyle) {
        settings.selectedGameplayStyle?.let {
            GlobalPlatformConfig.gameplayStyle = it
        }
    }

    var rewardFeedback by remember { mutableStateOf(RewardFeedbackState()) }

    var routeStack by remember(initialBootstrapResult) {
        mutableStateOf(
            listOf(
                resolveStartupRoute(
                    loadedSettings = initialBootstrapResult.settings,
                    startupGameplayStyleOverride = startupGameplayStyleOverride,
                )
            )
        )
    }
    var showLeaveSessionDialog by remember { mutableStateOf(false) }
    val persistActiveSession = remember { mutableStateOf(true) }
    var pendingSessionState by remember { mutableStateOf<GameState?>(null) }
    var pendingRequestedGameMode by remember { mutableStateOf<GameMode?>(null) }
    fun persistSettings(updated: AppSettings) {
        val sanitizedSettings = updated.sanitized()
        settings = sanitizedSettings
        AppSettingsStorage.save(sanitizedSettings)
    }

    fun createGameViewModel(initialState: GameState?) = GameViewModel(
        initialState = initialState,
        onStateChanged = { state ->
            if (persistActiveSession.value) {
                pendingSessionState = state
            }
        },
        onChallengeCompleted = { completedChallenge ->
            persistSettings(settings.awardCompletedChallenge(completedChallenge))
            GameSessionStorage.clear(sessionSlotFor(GameMode.Classic, completedChallenge))
        },
        onGameOver = { finalState ->
            persistSettings(settings.awardScoreTokens(finalState.score))
        },
    )

    fun loadSavedSession(
        slot: GameSessionSlot,
        expectedGameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
    ): GameState? {
        val slotStyle = slot.style
        if (slotStyle != null && slotStyle != expectedGameplayStyle) return null
        val savedState = GameSessionStorage.load(slot) ?: return null
        if (savedState.gameplayStyle != expectedGameplayStyle || !isUsableSavedSession(savedState, slot)) {
            GameSessionStorage.clear(slot)
            return null
        }
        return savedState
    }

    val gameViewModelState = remember(settings.selectedGameplayStyle) {
        val lastSlot = settings.lastActiveSlot
        val initialStyle = settings.selectedGameplayStyle ?: GlobalPlatformConfig.gameplayStyle
        val initialSession = lastSlot?.let { loadSavedSession(it, initialStyle) }
        mutableStateOf(createGameViewModel(initialState = initialSession))
    }

    var gameViewModel by gameViewModelState

    fun restoreOrRestartSession(
        slot: GameSessionSlot,
        gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
        fallback: () -> Unit,
    ) {
        persistActiveSession.value = true
        GlobalPlatformConfig.gameplayStyle = gameplayStyle
        persistSettings(
            settings.copy(
                lastActiveSlot = slot,
                selectedGameplayStyle = gameplayStyle,
            )
        )
        val savedState = loadSavedSession(slot, gameplayStyle)
        if (savedState != null) {
            gameViewModel.replaceState(savedState)
        } else {
            fallback()
        }
    }

    val currentRoute = routeStack.lastOrNull() ?: AppRoute.Home
    val canNavigateBack = routeStack.size > 1
    fun navigateTo(route: AppRoute) {
        if (routeStack.lastOrNull() == route) return
        routeStack = routeStack + route
    }

    fun replaceTop(route: AppRoute) {
        routeStack = if (routeStack.isEmpty()) {
            listOf(route)
        } else {
            routeStack.dropLast(1) + route
        }
    }

    fun navigateBack() {
        if (routeStack.size <= 1) return
        showLeaveSessionDialog = false
        val leavingRoute = routeStack.lastOrNull()
        routeStack = routeStack.dropLast(1)

        // Save immediately when leaving a game
        if (leavingRoute == AppRoute.Game || leavingRoute == AppRoute.InteractiveOnboarding) {
            pendingSessionState?.let { state ->
                GameSessionStorage.save(state.sessionSlot(), state)
            }
        }

        if (leavingRoute == AppRoute.Tutorial || leavingRoute == AppRoute.InteractiveOnboarding) {
            pendingRequestedGameMode = null
        }

        if (leavingRoute == AppRoute.InteractiveOnboarding) {
            persistActiveSession.value = true
            pendingSessionState = null
            gameViewModel =
                createGameViewModel(loadSavedSession(sessionSlotFor(GameMode.Classic)))
        }
    }

    fun navigateHome() {
        showLeaveSessionDialog = false
        pendingRequestedGameMode = null
        routeStack = listOf(AppRoute.Home)
    }

    fun requestBackNavigation() {
        val isGameOver = gameViewModel.snapshotState().status == GameStatus.GameOver
        if ((currentRoute == AppRoute.Game || currentRoute == AppRoute.InteractiveOnboarding) && !isGameOver) {
            showLeaveSessionDialog = true
        } else {
            navigateBack()
        }
    }

    fun prepareInteractiveOnboarding() {
        persistActiveSession.value = false
        pendingSessionState = null
        val initialState = when (GlobalPlatformConfig.gameplayStyle) {
            GameplayStyle.BlockWise -> BlockWiseOnboardingStateFactory.initialState()
            GameplayStyle.ChainShift -> ChainShiftOnboardingStateFactory.initialState()
            GameplayStyle.MergeShift -> MergeShiftOnboardingStateFactory.initialState()
            GameplayStyle.BoomBlocks -> BoomBlocksOnboardingStateFactory.initialState()
            GameplayStyle.BlockSort -> BlockSortOnboardingStateFactory.initialState()
            else -> StackShiftGameOnboardingStateFactory.initialState()
        }
        gameViewModel = createGameViewModel(initialState)
    }

    fun startPlayFlow(mode: GameMode = GameMode.Classic) {
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        val sessionSlot = sessionSlotFor(mode = mode, gameplayStyle = gameplayStyle)
        if (!settings.hasSeenTutorialFor(gameplayStyle)) {
            pendingRequestedGameMode = mode
            navigateTo(AppRoute.Tutorial)
        } else if (!settings.hasShownInteractiveOnboardingFor(gameplayStyle)) {
            pendingRequestedGameMode = mode
            prepareInteractiveOnboarding()
            navigateTo(AppRoute.InteractiveOnboarding)
        } else {
            pendingRequestedGameMode = null
            restoreOrRestartSession(slot = sessionSlot, gameplayStyle = gameplayStyle) {
                gameViewModel.restart(
                    config = GameConfig.default(gameplayStyle),
                    mode = mode,
                )
            }
            navigateTo(AppRoute.Game)
        }
    }


    fun completeInteractiveOnboarding(finalState: GameState, returnHome: Boolean) {
        persistActiveSession.value = true
        val gameplayStyle = GlobalPlatformConfig.gameplayStyle
        if (!settings.hasShownInteractiveOnboardingFor(gameplayStyle)) {
            persistSettings(settings.markInteractiveOnboardingShown(gameplayStyle))
        }
        if (returnHome) {
            navigateHome()
        } else {
            val requestedMode = pendingRequestedGameMode ?: GameMode.Classic
            pendingRequestedGameMode = null
            val gameplayStyle = GlobalPlatformConfig.gameplayStyle
            telemetry.logUserAction(
                if (requestedMode == GameMode.TimeAttack) {
                    TelemetryActionNames.StartTimeAttackFromHome
                } else {
                    TelemetryActionNames.StartGameFromHome
                }
            )
            restoreOrRestartSession(
                slot = sessionSlotFor(requestedMode, gameplayStyle = gameplayStyle),
                gameplayStyle = gameplayStyle,
            ) {
                gameViewModel.restart(
                    config = GameConfig.default(gameplayStyle),
                    mode = requestedMode,
                )
            }
            replaceTop(AppRoute.Game)
        }
    }

    LaunchedEffect(initialBootstrapResult, telemetry) {
        logLanguageBootstrapDecision(
            source = bootstrapLogSource,
            result = initialBootstrapResult,
            telemetry = telemetry,
        )
        if (initialBootstrapResult.shouldPersist) {
            AppSettingsStorage.save(settings)
        }
    }
    LaunchedEffect(persistActiveSession.value, pendingSessionState) {
        val state = pendingSessionState ?: return@LaunchedEffect
        if (!persistActiveSession.value) return@LaunchedEffect
        delay(200.milliseconds)
        if (persistActiveSession.value && (pendingSessionState == state)) {
            GameSessionStorage.save(state.sessionSlot(), state)
        }
    }
    DisposableEffect(gameViewModel) {
        onDispose(gameViewModel::dispose)
    }

    beforeRoot(settings, canNavigateBack, ::requestBackNavigation)

    BlockGamesRoot(
        settings = settings,
        telemetry = telemetry,
        gameViewModel = gameViewModel,
        classicHighScore = HighScoreStorage.load(GameMode.Classic),
        timeAttackHighScore = HighScoreStorage.load(GameMode.TimeAttack),
        rewardFeedback = rewardFeedback,
        onRewardFeedbackDismiss = { rewardFeedback = rewardFeedback.copy(visible = false) },
        onPlayRequested = {
            telemetry.logUserAction(TelemetryActionNames.StartGameFromHome)
            startPlayFlow(GameMode.Classic)
        },
        onTimeAttackRequested = {
            telemetry.logUserAction(TelemetryActionNames.StartTimeAttackFromHome)
            startPlayFlow(GameMode.TimeAttack)
        },
        onNavigateToTheme = {
            settingsTabIndex = 0
            navigateTo(AppRoute.Settings)
        },
        onPlayChallengeRequested = { challenge ->
            restoreOrRestartSession(
                slot = sessionSlotFor(GameMode.Classic, challenge),
                gameplayStyle = challenge.style,
            ) {
                gameViewModel.restart(
                    config = GameConfig.default(challenge.style),
                    challenge = challenge,
                    mode = GameMode.Classic,
                )
            }
            val restoredChallenge = gameViewModel.snapshotState().activeChallenge
            val isMatchingChallenge = restoredChallenge?.let {
                it.year == challenge.year && it.month == challenge.month && it.day == challenge.day
            } == true
            if (!isMatchingChallenge) {
                gameViewModel.restart(
                    config = GameConfig.default(challenge.style),
                    challenge = challenge,
                    mode = GameMode.Classic,
                )
            }
            navigateTo(AppRoute.Game)
        },
        onNavigateToLanguage = {
            settingsTabIndex = 2
            navigateTo(AppRoute.Settings)
        },
        onNavigateToTutorial = {
            pendingRequestedGameMode = null
            navigateTo(AppRoute.Tutorial)
        },
        onNavigateToChallenges = { navigateTo(AppRoute.DailyChallenges) },
        onNavigateBack = ::requestBackNavigation,
        onSettingsChange = { updated ->
            val shouldLogSettingsChange = updated.copy(
                hasSeenTutorial = settings.hasSeenTutorial,
                hasShownInteractiveOnboarding = settings.hasShownInteractiveOnboarding,
                seenTutorialStyles = settings.seenTutorialStyles,
                shownInteractiveOnboardingStyles = settings.shownInteractiveOnboardingStyles,
                hasInitializedLanguage = settings.hasInitializedLanguage,
            ) != settings
            persistSettings(updated)
            if (shouldLogSettingsChange) {
                telemetry.logUserAction(TelemetryActionNames.SettingsChanged)
            }
        },
        onRewardedTokensEarned = {
            persistSettings(settings.awardBonusTokens(RewardedTokenAdReward))
            rewardFeedback = RewardFeedbackState(
                messageRes = Res.string.reward_earned_message,
                messageArgs = listOf(RewardedTokenAdReward),
                icon = Icons.Filled.Stars,
                visible = true
            )
        },
        onReplaceActivePieceRewarded = { specialType ->
            telemetry.logUserAction("replace_active_piece_${specialType.name.lowercase()}")
            val gameplayStyle = gameViewModel.snapshotState().gameplayStyle
            val rewardFeedbackSpec = rewardedDockFeedbackSpec(
                gameplayStyle = gameplayStyle,
                specialType = specialType,
            )
            gameViewModel.replaceActivePiece(specialType)

            rewardFeedback = RewardFeedbackState(
                messageRes = rewardFeedbackSpec.messageRes,
                icon = rewardFeedbackSpec.icon,
                visible = true
            )
        },
        onTutorialFinishRequested = {
            val gameplayStyle = GlobalPlatformConfig.gameplayStyle
            if (!settings.hasSeenTutorialFor(gameplayStyle)) {
                persistSettings(settings.markTutorialSeen(gameplayStyle))
            }
            telemetry.logUserAction(TelemetryActionNames.OpenInteractiveOnboarding)
            prepareInteractiveOnboarding()
            replaceTop(AppRoute.InteractiveOnboarding)
        },
        onInteractiveOnboardingFinished = { finalState ->
            completeInteractiveOnboarding(finalState = finalState, returnHome = false)
        },
        onInteractiveOnboardingReturnHome = { finalState ->
            completeInteractiveOnboarding(finalState = finalState, returnHome = true)
        },
        onGameplayStyleSelected = { style ->
            pendingSessionState?.let { state ->
                GameSessionStorage.save(state.sessionSlot(), state)
            }
            GlobalPlatformConfig.gameplayStyle = style
            persistSettings(settings.copy(selectedGameplayStyle = style))
            replaceTop(AppRoute.Home)
        },
        onNavigateToSelection = {
            navigateTo(AppRoute.Selection)
        },
        settingsTabIndex = settingsTabIndex,
        onSettingsTabIndexChange = { settingsTabIndex = it },
        currentRoute = currentRoute,
        showLeaveSessionDialog = showLeaveSessionDialog,
        onDismissLeaveSessionDialog = { showLeaveSessionDialog = false },
        onConfirmLeaveSessionDialog = ::navigateBack,
    )

    LaunchedEffect(settings) {
        telemetry.logUserProperty(TelemetryUserPropertyNames.Language, settings.language.localeTag)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemeMode, settings.themeMode.name)
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.ThemePalette,
            settings.themeColorPalette.name
        )
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.BlockPalette,
            settings.blockColorPalette.name
        )
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.BlockStyle,
            settings.blockVisualStyle.name
        )
    }
}

@Composable
fun LeaveSessionConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    ThemedConfirmDialog(
        onDismissRequest = onDismissRequest,
        title = appStringResource(Res.string.leave_session_confirm_title),
        message = appStringResource(Res.string.leave_session_confirm_body),
        confirmLabel = appStringResource(Res.string.leave_session_confirm),
        dismissLabel = appStringResource(Res.string.cancel),
        onConfirm = onConfirm,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        dismissButtonIcon = Icons.Filled.Close,
    )
}
