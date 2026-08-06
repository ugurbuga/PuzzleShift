package com.ugurbuga.blockgames.ui.game.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_theme_dark
import blockgames.composeapp.generated.resources.app_theme_light
import blockgames.composeapp.generated.resources.app_theme_system
import blockgames.composeapp.generated.resources.block_style_aura_energy
import blockgames.composeapp.generated.resources.block_style_brick
import blockgames.composeapp.generated.resources.block_style_bubble
import blockgames.composeapp.generated.resources.block_style_circuit_board
import blockgames.composeapp.generated.resources.block_style_cosmic
import blockgames.composeapp.generated.resources.block_style_crystal
import blockgames.composeapp.generated.resources.block_style_cyberpunk
import blockgames.composeapp.generated.resources.block_style_dynamic_liquid
import blockgames.composeapp.generated.resources.block_style_flame
import blockgames.composeapp.generated.resources.block_style_flat
import blockgames.composeapp.generated.resources.block_style_gears
import blockgames.composeapp.generated.resources.block_style_glitch_tech
import blockgames.composeapp.generated.resources.block_style_grid_split
import blockgames.composeapp.generated.resources.block_style_holographic
import blockgames.composeapp.generated.resources.block_style_honeycomb_texture
import blockgames.composeapp.generated.resources.block_style_light_burst
import blockgames.composeapp.generated.resources.block_style_liquid_marble
import blockgames.composeapp.generated.resources.block_style_neon_glow
import blockgames.composeapp.generated.resources.block_style_outline
import blockgames.composeapp.generated.resources.block_style_pixel
import blockgames.composeapp.generated.resources.block_style_prism
import blockgames.composeapp.generated.resources.block_style_sharp_3d
import blockgames.composeapp.generated.resources.block_style_sound_wave
import blockgames.composeapp.generated.resources.block_style_spider_web
import blockgames.composeapp.generated.resources.block_style_tornado
import blockgames.composeapp.generated.resources.block_style_wood
import blockgames.composeapp.generated.resources.cancel
import blockgames.composeapp.generated.resources.rewarded_tokens_button
import blockgames.composeapp.generated.resources.rewarded_tokens_button_bullet
import blockgames.composeapp.generated.resources.settings_block_style
import blockgames.composeapp.generated.resources.settings_color_palette
import blockgames.composeapp.generated.resources.settings_language
import blockgames.composeapp.generated.resources.settings_theme
import blockgames.composeapp.generated.resources.settings_tokens_balance
import blockgames.composeapp.generated.resources.settings_tokens_earn_challenge
import blockgames.composeapp.generated.resources.settings_tokens_earn_score
import blockgames.composeapp.generated.resources.theme_palette_aurora
import blockgames.composeapp.generated.resources.theme_palette_classic
import blockgames.composeapp.generated.resources.theme_palette_minimal_monochrome
import blockgames.composeapp.generated.resources.theme_palette_modern_neon
import blockgames.composeapp.generated.resources.theme_palette_soft_pastel
import blockgames.composeapp.generated.resources.theme_palette_sunset
import blockgames.composeapp.generated.resources.unlock_dialog_confirm
import blockgames.composeapp.generated.resources.unlock_dialog_insufficient_title
import blockgames.composeapp.generated.resources.unlock_dialog_message
import blockgames.composeapp.generated.resources.unlock_dialog_not_enough
import blockgames.composeapp.generated.resources.unlock_dialog_title
import blockgames.composeapp.generated.resources.unlock_dialog_watch_ad
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.ads.AppFooterAdSlot
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.normalizeBlockVisualStyle
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalBlockStylePulse
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.DailyChallengeTokenReward
import com.ugurbuga.blockgames.settings.RewardedTokenAdReward
import com.ugurbuga.blockgames.settings.ScorePointsPerToken
import com.ugurbuga.blockgames.settings.isBlockStyleUnlocked
import com.ugurbuga.blockgames.settings.isThemePaletteUnlocked
import com.ugurbuga.blockgames.settings.selectBlockStyle
import com.ugurbuga.blockgames.settings.selectThemeMode
import com.ugurbuga.blockgames.settings.selectThemePalette
import com.ugurbuga.blockgames.settings.tokenCost
import com.ugurbuga.blockgames.settings.unlockBlockStyle
import com.ugurbuga.blockgames.settings.unlockThemePalette
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.ThemedConfirmDialog
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesThemeSpec
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val ScreenContentMaxWidth = 920.dp

@Composable
fun AppSettingsScreen(
    telemetry: AppTelemetry = NoOpAppTelemetry,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onRewardedTokensRequested: () -> Unit,
    onBack: () -> Unit,
    onOpenSelection: () -> Unit,
    adController: GameAdController? = null,
    selectedTabIndex: Int = 0,
    onSelectedTabIndexChange: (Int) -> Unit = {},
    initialTabIndex: Int = 0,
    modifier: Modifier = Modifier,
) {
    LogScreen(telemetry, TelemetryScreenNames.Theme)
    val scope = rememberCoroutineScope()
    val isSystemDark = isSystemInDarkTheme()

    // Animation States
    var revealOrigin by remember { mutableStateOf<Offset?>(null) }
    val revealProgress = remember { Animatable(0f) }
    var pendingSettings by remember { mutableStateOf<AppSettings?>(null) }
    var isAnimating by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = if (selectedTabIndex != 0) selectedTabIndex else initialTabIndex
    ) { 3 }

    // Hoist UI states to share between animation layers
    val scrollStates = List(3) { rememberScrollState() }
    val tokenCardExpanded = remember { mutableStateOf(false) }

    fun performThemeChange(nextSettings: AppSettings, offset: Offset) {
        if (isAnimating || nextSettings == settings) return

        // Check if theme or palette actually changed
        val currentIsDark = settings.themeMode.isDark ?: isSystemDark
        val nextIsDark = nextSettings.themeMode.isDark ?: isSystemDark
        val themeChanged =
            currentIsDark != nextIsDark || nextSettings.themeColorPalette != settings.themeColorPalette

        if (!themeChanged) {
            onSettingsChange(nextSettings)
            return
        }

        isAnimating = true
        revealOrigin = offset
        pendingSettings = nextSettings

        scope.launch {
            try {
                revealProgress.snapTo(0f)
                delay(32) // Wait for Layer 2 to be ready
                revealProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
            } finally {
                onSettingsChange(nextSettings)
                pendingSettings = null
                revealOrigin = null
                revealProgress.snapTo(0f)
                isAnimating = false
            }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onSelectedTabIndexChange(page)
        }
    }

    val transition = rememberInfiniteTransition(label = "settingsStylePulse")
    val sharedPulse = LocalBlockStylePulse.current
    val previewToneStep = transition.animateFloat(
        initialValue = 0f,
        targetValue = CellTone.entries.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CellTone.entries.size * 3500,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "previewToneStep",
    )

    var pendingUnlockRequest by remember { mutableStateOf<UnlockRequest?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Base (Current)
            BlockGamesTheme(settings = settings) {
                SettingsMainContent(
                    settings = settings,
                    pagerState = pagerState,
                    scrollStates = scrollStates,
                    tokenCardExpanded = tokenCardExpanded.value,
                    onTokenCardExpandedChange = { tokenCardExpanded.value = it },
                    stylePulseProvider = { sharedPulse },
                    previewToneStepProvider = { previewToneStep.value },
                    onBack = onBack,
                    onRewardedTokensRequested = onRewardedTokensRequested,
                    onSettingsChange = onSettingsChange,
                    onThemeChangeRequest = { next, offset -> performThemeChange(next, offset) },
                    onUnlockRequest = { pendingUnlockRequest = it },
                    onOpenSelection = onOpenSelection,
                    adController = adController,
                    isStatic = pendingSettings != null,
                )
            }

            // Layer 2: Reveal (New)
            if (pendingSettings != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .circularReveal(revealProgress.value, revealOrigin)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                ) {
                    BlockGamesTheme(settings = pendingSettings!!) {
                        SettingsMainContent(
                            settings = pendingSettings!!,
                            pagerState = pagerState,
                            scrollStates = scrollStates,
                            tokenCardExpanded = tokenCardExpanded.value,
                            onTokenCardExpandedChange = { tokenCardExpanded.value = it },
                            stylePulseProvider = { sharedPulse },
                            previewToneStepProvider = { previewToneStep.value },
                            onBack = onBack,
                            onRewardedTokensRequested = onRewardedTokensRequested,
                            onSettingsChange = onSettingsChange,
                            onThemeChangeRequest = { next, offset ->
                                performThemeChange(
                                    next,
                                    offset
                                )
                            },
                            onUnlockRequest = { pendingUnlockRequest = it },
                            onOpenSelection = onOpenSelection,
                            adController = adController,
                            isStatic = true,
                        )
                    }
                }
            }

            // Global interaction blocker during animation
            if (isAnimating) {
                Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {})
            }
        }

        pendingUnlockRequest?.let { request ->
            UnlockOptionDialog(
                request = request,
                onDismissRequest = { pendingUnlockRequest = null },
                onConfirm = {
                    request.onUnlock(settings)?.let(onSettingsChange)
                    pendingUnlockRequest = null
                },
                onWatchAd = onRewardedTokensRequested,
                adController = adController,
            )
        }
    }
}

@Composable
private fun SettingsMainContent(
    settings: AppSettings,
    pagerState: androidx.compose.foundation.pager.PagerState,
    scrollStates: List<androidx.compose.foundation.ScrollState>,
    tokenCardExpanded: Boolean,
    onTokenCardExpandedChange: (Boolean) -> Unit,
    stylePulseProvider: () -> Float,
    previewToneStepProvider: () -> Float,
    onBack: () -> Unit,
    onRewardedTokensRequested: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onThemeChangeRequest: (AppSettings, Offset) -> Unit,
    onUnlockRequest: (UnlockRequest) -> Unit,
    onOpenSelection: () -> Unit,
    adController: GameAdController?,
    modifier: Modifier = Modifier,
    isStatic: Boolean = false,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val darkTheme = isBlockGamesDarkTheme(settings)
    val scope = rememberCoroutineScope()

    val isBlockStyleTabActive = pagerState.currentPage == 1

    val pulseProvider: () -> Float = if (isBlockStyleTabActive) {
        stylePulseProvider
    } else {
        { 0f }
    }
    val previewColorProvider: () -> Color = {
        interpolatedPreviewColor(
            palette = settings.blockColorPalette,
            progress = previewToneStepProvider(),
            isDark = darkTheme,
        )
    }

    val themeOptions = themeModeOptions(settings.themeMode)
    val paletteOptions = themePaletteOptions(settings, darkTheme)
    val styleOptions = blockStyleOptions(
        settings = settings,
        pulseProvider = pulseProvider,
        previewColorProvider = previewColorProvider,
    )
    val langOptions = languageOptions(settings.language)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(appBackgroundBrush(uiColors))
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarActionBlockButton(
                    tone = CellTone.Cyan,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.settings_theme),
                    onClick = onBack,
                    size = 44.dp,
                    pulse = stylePulseProvider(),
                )
                Text(
                    text = stringResource(Res.string.settings_theme),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(44.dp))
            }

            TokenBalanceCard(
                settings = settings,
                onRewardedTokensRequested = onRewardedTokensRequested,
                adController = adController,
                expanded = tokenCardExpanded,
                onExpandedChange = onTokenCardExpandedChange,
                stylePulseProvider = stylePulseProvider,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )

            SettingsTabSwitcher(
                pagerState = pagerState,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )

            if (isStatic) {
                val pageIndex = pagerState.currentPage.coerceIn(0, 2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollStates[pageIndex])
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SettingsPageContent(
                        pageIndex = pageIndex,
                        settings = settings,
                        themeOptions = themeOptions,
                        paletteOptions = paletteOptions,
                        styleOptions = styleOptions,
                        langOptions = langOptions,
                        onSettingsChange = onSettingsChange,
                        onThemeChangeRequest = onThemeChangeRequest,
                        onUnlockRequest = onUnlockRequest
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    pageSpacing = 16.dp,
                    verticalAlignment = Alignment.Top,
                ) { pageIndex ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollStates[pageIndex])
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SettingsPageContent(
                            pageIndex = pageIndex,
                            settings = settings,
                            themeOptions = themeOptions,
                            paletteOptions = paletteOptions,
                            styleOptions = styleOptions,
                            langOptions = langOptions,
                            onSettingsChange = onSettingsChange,
                            onThemeChangeRequest = onThemeChangeRequest,
                            onUnlockRequest = onUnlockRequest
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
            if (adController != null) {
                AppFooterAdSlot(
                    adController = adController,
                    onOpenSelection = onOpenSelection,
                )
            }
        }
    }
}

@Composable
private fun SettingsPageContent(
    pageIndex: Int,
    settings: AppSettings,
    themeOptions: List<SettingsOption<AppThemeMode>>,
    paletteOptions: List<SettingsOption<AppColorPalette>>,
    styleOptions: List<SettingsOption<BlockVisualStyle>>,
    langOptions: List<SettingsOption<AppLanguage>>,
    onSettingsChange: (AppSettings) -> Unit,
    onThemeChangeRequest: (AppSettings, Offset) -> Unit,
    onUnlockRequest: (UnlockRequest) -> Unit,
) {
    when (pageIndex) {
        0 -> {
            SettingsSectionCard(title = "") {
                SectionHeader(stringResource(Res.string.settings_theme))
                SelectionGrid(
                    columns = 3,
                    selectedValue = settings.themeMode,
                    options = themeOptions,
                    onSelected = {},
                    onOptionPositioned = { _, _ -> },
                    onOptionClickWithOffset = { mode, offset ->
                        onThemeChangeRequest(settings.selectThemeMode(mode), offset)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(stringResource(Res.string.settings_color_palette))
                SelectionGrid(
                    columns = 3,
                    selectedValue = settings.themeColorPalette,
                    options = paletteOptions,
                    onSelected = {},
                    onOptionClickWithOffset = { palette, offset ->
                        onThemeChangeRequest(settings.selectThemePalette(palette), offset)
                    },
                    onLockedSelected = { option ->
                        onUnlockRequest(
                            unlockRequest(
                                label = option.label,
                                priceTokens = option.priceTokens,
                                currentBalance = settings.tokenBalance,
                            ) { it.unlockThemePalette(option.value) }
                        )
                    }
                )
            }
        }

        1 -> {
            SettingsSectionCard(title = "") {
                SectionHeader(stringResource(Res.string.settings_block_style))
                SelectionGrid(
                    columns = 3,
                    selectedValue = settings.blockVisualStyle,
                    options = styleOptions,
                    onSelected = {
                        onSettingsChange(
                            settings.selectBlockStyle(
                                it
                            )
                        )
                    },
                    onLockedSelected = { option ->
                        onUnlockRequest(
                            unlockRequest(
                                label = option.label,
                                priceTokens = option.priceTokens,
                                currentBalance = settings.tokenBalance,
                            ) { it.unlockBlockStyle(option.value) }
                        )
                    }
                )
            }
        }

        else -> {
            SettingsSectionCard(title = "") {
                SectionHeader(stringResource(Res.string.settings_language))
                SelectionGrid(
                    columns = 3,
                    selectedValue = settings.language,
                    options = langOptions,
                    onSelected = { onSettingsChange(settings.copy(language = it)) }
                )
            }
        }
    }
}

fun Modifier.circularReveal(
    progress: Float,
    origin: Offset?
) = graphicsLayer {
    if (progress <= 0f || origin == null) {
        clip = true
        shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline = Outline.Generic(Path())
        }
    } else {
        clip = true
        shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val maxRadius = size.width.coerceAtLeast(size.height) * 1.5f
                val radius = maxRadius * progress
                val path = Path().apply {
                    addOval(Rect(center = origin, radius = radius))
                }
                return Outline.Generic(path)
            }
        }
    }
}

@Composable
private fun SettingsTabSwitcher(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (Int) -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.buttonCorner)
    val tabs = listOf(
        Triple(Res.string.settings_theme, Icons.Filled.Palette, 0),
        Triple(Res.string.settings_block_style, Icons.Filled.Layers, 1),
        Triple(Res.string.settings_language, Icons.Filled.Translate, 2),
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .widthIn(max = ScreenContentMaxWidth)
            .height(72.dp)
            .background(uiColors.panel.copy(alpha = 0.4f), shape)
            .padding(4.dp)
    ) {
        // Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth(1f / 3f)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX =
                        (pagerState.currentPage + pagerState.currentPageOffsetFraction) * size.width
                }
                .background(color = uiColors.actionPrimary, shape = shape)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { (titleRes, icon, index) ->
                val isSelected = pagerState.currentPage == index
                val contentColor =
                    if (isSelected) uiColors.actionIcon else uiColors.subtitle.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = uiColors.subtitle.copy(alpha = 0.7f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    trailingContent: (@Composable (() -> Unit))? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.panelCorner)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = ScreenContentMaxWidth)
            .background(uiColors.panel.copy(alpha = 0.95f), shape)
            .border(1.dp, uiColors.panelStroke.copy(alpha = 0.3f), shape)
            .padding(20.dp)
    ) {
        if (title.isNotEmpty() || trailingContent != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = uiColors.actionPrimary,
                        letterSpacing = 1.sp
                    )
                }
                trailingContent?.invoke()
            }
        }
        content()
    }
}

@Composable
private fun <T> SelectionGrid(
    columns: Int,
    selectedValue: T,
    options: List<SettingsOption<T>>,
    onSelected: (T) -> Unit,
    onLockedSelected: (SettingsOption<T>) -> Unit = {},
    onOptionPositioned: (T, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    onOptionClickWithOffset: (T, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val chunks = options.chunked(columns)
        chunks.forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowOptions.forEach { option ->
                    val isSelected = option.value == selectedValue
                    var itemCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { coords ->
                                val position = coords.positionInWindow()
                                itemCenter = androidx.compose.ui.geometry.Offset(
                                    x = position.x + coords.size.width / 2f,
                                    y = position.y + coords.size.height / 2f
                                )
                                onOptionPositioned(option.value, itemCenter)
                            }
                    ) {
                        GridSelectionItem(
                            selected = isSelected,
                            locked = option.locked,
                            label = option.label,
                            preview = option.preview,
                            onClick = {
                                onOptionClickWithOffset(option.value, itemCenter)
                                if (option.locked) onLockedSelected(option) else onSelected(option.value)
                            }
                        )
                    }
                }
                // Fill empty slots in the last row if necessary
                if (rowOptions.size < columns) {
                    repeat(columns - rowOptions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridSelectionItem(
    selected: Boolean,
    locked: Boolean,
    label: String,
    preview: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (selected) uiColors.actionPrimary.copy(alpha = 0.1f) else uiColors.panelMuted.copy(
                        alpha = 0.3f
                    ),
                    shape = shape
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) uiColors.actionPrimary else uiColors.panelStroke.copy(
                        alpha = 0.15f
                    ),
                    shape = shape
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                preview?.invoke()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else uiColors.subtitle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (locked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            Column(
                modifier = Modifier.matchParentSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Transparent,
                    maxLines = 2,
                    minLines = 2
                )
            }
        }
    }
}

@Composable
private fun TokenBalanceCard(
    settings: AppSettings,
    onRewardedTokensRequested: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    stylePulseProvider: () -> Float,
    adController: GameAdController? = null,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val surfaceShape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    var adLoading by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = ScreenContentMaxWidth),
        shape = surfaceShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        Res.string.settings_tokens_balance,
                        settings.tokenBalance
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                if (adController != null) {
                    TopBarActionBlockButton(
                        tone = CellTone.Gold,
                        icon = Icons.Filled.Stars,
                        contentDescription = stringResource(
                            Res.string.rewarded_tokens_button,
                            RewardedTokenAdReward
                        ),
                        onClick = {
                            if (adLoading) return@TopBarActionBlockButton
                            adLoading = true
                            adController.showRewardedAd { success ->
                                adLoading = false
                                if (success) {
                                    onRewardedTokensRequested()
                                }
                            }
                        },
                        enabled = !adLoading,
                        size = 44.dp,
                        showAdIcon = true,
                        pulse = stylePulseProvider(),
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = uiColors.subtitle.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = stringResource(
                            Res.string.settings_tokens_earn_challenge,
                            DailyChallengeTokenReward
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = uiColors.subtitle,
                    )
                    Text(
                        text = stringResource(
                            Res.string.settings_tokens_earn_score,
                            ScorePointsPerToken
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = uiColors.subtitle,
                    )
                    Text(
                        text = stringResource(
                            Res.string.rewarded_tokens_button_bullet,
                            RewardedTokenAdReward
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = uiColors.subtitle,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockOptionDialog(
    request: UnlockRequest,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onWatchAd: () -> Unit,
    adController: GameAdController? = null,
) {
    if (request.canAfford) {
        ThemedConfirmDialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(Res.string.unlock_dialog_title, request.label),
            message = stringResource(
                Res.string.unlock_dialog_message,
                request.label,
                request.priceTokens,
                request.currentBalance,
            ),
            confirmLabel = stringResource(Res.string.unlock_dialog_confirm),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = onConfirm,
            icon = Icons.Filled.Lock,
        )
    } else {
        var adLoading by remember { mutableStateOf(false) }

        ThemedConfirmDialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(Res.string.unlock_dialog_insufficient_title, request.label),
            message = stringResource(
                Res.string.unlock_dialog_not_enough,
                request.priceTokens,
                request.currentBalance,
            ),
            confirmLabel = stringResource(Res.string.unlock_dialog_watch_ad),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = {
                if (adLoading) return@ThemedConfirmDialog
                if (adController != null) {
                    adLoading = true
                    adController.showRewardedAd { success ->
                        adLoading = false
                        if (success) {
                            onWatchAd()
                            onDismissRequest()
                        }
                    }
                } else {
                    onWatchAd()
                    onDismissRequest()
                }
            },
            icon = Icons.Outlined.Videocam,
        )
    }
}

@Composable
private fun languageOptions(selected: AppLanguage): List<SettingsOption<AppLanguage>> =
    AppLanguage.entries.map { language ->
        SettingsOption(
            value = language,
            label = stringResource(language.labelRes),
            preview = { LanguagePreview(language = language) },
        )
    }

@Composable
private fun themeModeOptions(selected: AppThemeMode): List<SettingsOption<AppThemeMode>> =
    AppThemeMode.entries.map { mode ->
        SettingsOption(
            value = mode,
            label = when (mode) {
                AppThemeMode.System -> stringResource(Res.string.app_theme_system)
                AppThemeMode.Light -> stringResource(Res.string.app_theme_light)
                AppThemeMode.Dark -> stringResource(Res.string.app_theme_dark)
            },
            preview = { ThemeModePreview(mode) },
        )
    }

@Composable
private fun themePaletteOptions(
    settings: AppSettings,
    darkTheme: Boolean,
): List<SettingsOption<AppColorPalette>> =
    AppColorPalette.entries.map { palette ->
        SettingsOption(
            value = palette,
            label = when (palette) {
                AppColorPalette.Classic -> stringResource(Res.string.theme_palette_classic)
                AppColorPalette.Aurora -> stringResource(Res.string.theme_palette_aurora)
                AppColorPalette.Sunset -> stringResource(Res.string.theme_palette_sunset)
                AppColorPalette.ModernNeon -> stringResource(Res.string.theme_palette_modern_neon)
                AppColorPalette.SoftPastel -> stringResource(Res.string.theme_palette_soft_pastel)
                AppColorPalette.MinimalMonochrome -> stringResource(Res.string.theme_palette_minimal_monochrome)
            },
            preview = { ThemePalettePreview(palette = palette, darkTheme = darkTheme) },
            locked = !settings.isThemePaletteUnlocked(palette),
            priceTokens = palette.tokenCost(),
        )
    }

@Composable
private fun blockStyleOptions(
    settings: AppSettings,
    pulseProvider: () -> Float,
    previewColorProvider: () -> Color,
): List<SettingsOption<BlockVisualStyle>> {
    return visibleBlockStyles().map { visibleStyle ->
        val style = normalizeBlockVisualStyle(visibleStyle)
        SettingsOption(
            value = style,
            label = when (style) {
                BlockVisualStyle.Flat -> stringResource(Res.string.block_style_flat)
                BlockVisualStyle.Bubble -> stringResource(Res.string.block_style_bubble)
                BlockVisualStyle.Outline -> stringResource(Res.string.block_style_outline)
                BlockVisualStyle.Sharp3D -> stringResource(Res.string.block_style_sharp_3d)
                BlockVisualStyle.Wood -> stringResource(Res.string.block_style_wood)
                BlockVisualStyle.GridSplit -> stringResource(Res.string.block_style_grid_split)
                BlockVisualStyle.Crystal -> stringResource(Res.string.block_style_crystal)
                BlockVisualStyle.DynamicLiquid -> stringResource(Res.string.block_style_dynamic_liquid)
                BlockVisualStyle.Tornado -> stringResource(Res.string.block_style_tornado)
                BlockVisualStyle.HoneycombTexture -> stringResource(Res.string.block_style_honeycomb_texture)
                BlockVisualStyle.SpiderWeb -> stringResource(Res.string.block_style_spider_web)
                BlockVisualStyle.Cosmic -> stringResource(Res.string.block_style_cosmic)
                BlockVisualStyle.Brick -> stringResource(Res.string.block_style_brick)
                BlockVisualStyle.SoundWave -> stringResource(Res.string.block_style_sound_wave)
                BlockVisualStyle.Prism -> stringResource(Res.string.block_style_prism)
                BlockVisualStyle.Flame -> stringResource(Res.string.block_style_flame)
                BlockVisualStyle.Gears -> stringResource(Res.string.block_style_gears)
                BlockVisualStyle.Pixel -> stringResource(Res.string.block_style_pixel)
                BlockVisualStyle.Cyberpunk -> stringResource(Res.string.block_style_cyberpunk)
                BlockVisualStyle.NeonGlow -> stringResource(Res.string.block_style_neon_glow)
                BlockVisualStyle.LiquidMarble -> stringResource(Res.string.block_style_liquid_marble)
                BlockVisualStyle.Holographic -> stringResource(Res.string.block_style_holographic)
                BlockVisualStyle.GlitchTech -> stringResource(Res.string.block_style_glitch_tech)
                BlockVisualStyle.AuraEnergy -> stringResource(Res.string.block_style_aura_energy)
                BlockVisualStyle.CircuitBoard -> stringResource(Res.string.block_style_circuit_board)
                BlockVisualStyle.LightBurst -> stringResource(Res.string.block_style_light_burst)
            },
            preview = {
                BlockCellPreview(
                    baseColor = previewColorProvider(),
                    style = style,
                    size = 40.dp,
                    pulse = if (style == BlockVisualStyle.Pixel) 0f else pulseProvider(),
                )
            },
            locked = !settings.isBlockStyleUnlocked(style),
            priceTokens = style.tokenCost(),
        )
    }
}

internal fun visibleBlockStyles(): List<BlockVisualStyle> = listOf(
    BlockVisualStyle.Flat,
    BlockVisualStyle.Bubble,
    BlockVisualStyle.Outline,
    BlockVisualStyle.Sharp3D,
    BlockVisualStyle.Wood,
    BlockVisualStyle.GridSplit,
    BlockVisualStyle.Crystal,
    BlockVisualStyle.DynamicLiquid,
    BlockVisualStyle.Tornado,
    BlockVisualStyle.HoneycombTexture,
    BlockVisualStyle.SpiderWeb,
    BlockVisualStyle.Cosmic,
    BlockVisualStyle.Brick,
    BlockVisualStyle.SoundWave,
    BlockVisualStyle.Prism,
    BlockVisualStyle.Flame,
    BlockVisualStyle.Gears,
    BlockVisualStyle.Pixel,
    BlockVisualStyle.Cyberpunk,
    BlockVisualStyle.NeonGlow,
    BlockVisualStyle.LiquidMarble,
    BlockVisualStyle.Holographic,
    BlockVisualStyle.GlitchTech,
    BlockVisualStyle.AuraEnergy,
    BlockVisualStyle.CircuitBoard,
    BlockVisualStyle.LightBurst,
)

private fun interpolatedPreviewColor(
    palette: BlockColorPalette,
    progress: Float,
    isDark: Boolean,
): Color {
    val tones = CellTone.entries
    val normalized = ((progress % tones.size) + tones.size) % tones.size
    val startIndex = normalized.toInt().coerceIn(0, tones.lastIndex)
    val endIndex = (startIndex + 1) % tones.size
    val blend = normalized - startIndex
    return lerp(
        tones[startIndex].paletteColor(palette, isDark),
        tones[endIndex].paletteColor(palette, isDark),
        blend,
    )
}

@Composable
private fun LanguagePreview(language: AppLanguage) {
    Text(text = language.flag, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ThemeModePreview(mode: AppThemeMode) {
    val icon = when (mode) {
        AppThemeMode.System -> Icons.Default.BrightnessAuto
        AppThemeMode.Light -> Icons.Default.LightMode
        AppThemeMode.Dark -> Icons.Default.DarkMode
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = BlockGamesThemeTokens.uiColors.actionPrimary
    )
}

@Composable
private fun ThemePalettePreview(palette: AppColorPalette, darkTheme: Boolean) {
    val scheme = blockGamesThemeSpec(palette = palette, darkTheme = darkTheme).colorScheme
    BoxPreview(
        colors = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.outlineVariant),
        size = 18.dp
    )
}

@Composable
private fun BoxPreview(colors: List<Color>, size: androidx.compose.ui.unit.Dp = 12.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val rows = colors.chunked(2)
        rows.forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(color, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }
        }
    }
}

private data class SettingsOption<T>(
    val value: T,
    val label: String,
    val preview: (@Composable () -> Unit)? = null,
    val locked: Boolean = false,
    val priceTokens: Int = 0,
)

internal data class UnlockRequest(
    val label: String,
    val priceTokens: Int,
    val currentBalance: Int,
    val onUnlock: (AppSettings) -> AppSettings?,
) {
    val canAfford: Boolean get() = currentBalance >= priceTokens
}

internal fun unlockRequest(
    label: String,
    priceTokens: Int,
    currentBalance: Int,
    onUnlock: (AppSettings) -> AppSettings?,
): UnlockRequest = UnlockRequest(label, priceTokens, currentBalance, onUnlock)

@Preview
@Composable
private fun PreviewThemeTab() {
    val settings =
        AppSettings(themeMode = AppThemeMode.Dark, themeColorPalette = AppColorPalette.ModernNeon)
    BlockGamesTheme(settings = settings) {
        AppSettingsScreen(
            settings = settings,
            adController = NoOpGameAdController,
            onSettingsChange = {},
            onRewardedTokensRequested = {},
            onBack = {},
            onOpenSelection = {},
            initialTabIndex = 0
        )
    }
}

@Preview
@Composable
private fun PreviewBlockStyleTab() {
    val settings = AppSettings(blockVisualStyle = BlockVisualStyle.Cyberpunk, tokenBalance = 500)
    BlockGamesTheme(settings = settings) {
        AppSettingsScreen(
            settings = settings,
            onSettingsChange = {},
            onRewardedTokensRequested = {},
            onBack = {},
            onOpenSelection = {},
            initialTabIndex = 1
        )
    }
}

@Preview
@Composable
private fun PreviewLanguageTab() {
    val settings =
        AppSettings(language = AppLanguage.French, blockVisualStyle = BlockVisualStyle.Crystal)
    AppSettingsScreen(
        settings = settings,
        onSettingsChange = {},
        onRewardedTokensRequested = {},
        onBack = {},
        onOpenSelection = {},
        initialTabIndex = 2
    )
}
@Preview(name = "Settings Screen", widthDp = 412, heightDp = 915)
@Composable
private fun SettingsScreenPreview() {
    val settings = AppSettings(
        tokenBalance = 1250,
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.CircuitBoard,
        themeColorPalette = AppColorPalette.Sunset
    )
    BlockGamesTheme(settings = settings) {
        AppSettingsScreen(
            settings = settings,
            onSettingsChange = {},
            onRewardedTokensRequested = {},
            onBack = {},
            onOpenSelection = {},
            adController = NoOpGameAdController,
            initialTabIndex = 0
        )
    }
}

@Preview(name = "Settings Screen Light", widthDp = 412, heightDp = 915)
@Composable
private fun SettingsScreenLightPreview() {
    val settings = AppSettings(
        tokenBalance = 500,
        themeMode = AppThemeMode.Light,
        blockVisualStyle = BlockVisualStyle.Bubble,
        themeColorPalette = AppColorPalette.SoftPastel
    )
    BlockGamesTheme(settings = settings) {
        AppSettingsScreen(
            settings = settings,
            onSettingsChange = {},
            onRewardedTokensRequested = {},
            onBack = {},
            onOpenSelection = {},
            adController = NoOpGameAdController,
            initialTabIndex = 0
        )
    }
}

@Preview(name = "Settings Block Style Tab", widthDp = 412, heightDp = 915)
@Composable
private fun SettingsBlockStyleTabPreview() {
    val settings = AppSettings(
        tokenBalance = 2000,
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Cyberpunk,
        themeColorPalette = AppColorPalette.ModernNeon
    )
    BlockGamesTheme(settings = settings) {
        AppSettingsScreen(
            settings = settings,
            onSettingsChange = {},
            onRewardedTokensRequested = {},
            onBack = {},
            onOpenSelection = {},
            adController = NoOpGameAdController,
            initialTabIndex = 1
        )
    }
}

@Preview(name = "Settings Language Tab", widthDp = 412, heightDp = 915)
@Composable
private fun SettingsLanguageTabPreview() {
    val settings = AppSettings(
        language = AppLanguage.Turkish,
        themeMode = AppThemeMode.System
    )
    BlockGamesTheme(settings = settings) {
        AppSettingsScreen(
            settings = settings,
            onSettingsChange = {},
            onRewardedTokensRequested = {},
            onBack = {},
            onOpenSelection = {},
            adController = NoOpGameAdController,
            initialTabIndex = 2
        )
    }
}
