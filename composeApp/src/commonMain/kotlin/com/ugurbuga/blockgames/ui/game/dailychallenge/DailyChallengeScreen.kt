package com.ugurbuga.blockgames.ui.game.dailychallenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.calendar_month_year_format
import blockgames.composeapp.generated.resources.challenge_info_chain_reaction
import blockgames.composeapp.generated.resources.challenge_info_chain_reaction_title
import blockgames.composeapp.generated.resources.challenge_info_clear_blocks
import blockgames.composeapp.generated.resources.challenge_info_clear_blocks_title
import blockgames.composeapp.generated.resources.challenge_info_clear_both_directions
import blockgames.composeapp.generated.resources.challenge_info_clear_both_directions_title
import blockgames.composeapp.generated.resources.challenge_info_clear_columns
import blockgames.composeapp.generated.resources.challenge_info_clear_columns_title
import blockgames.composeapp.generated.resources.challenge_info_clear_rounds
import blockgames.composeapp.generated.resources.challenge_info_clear_rounds_title
import blockgames.composeapp.generated.resources.challenge_info_clear_rows
import blockgames.composeapp.generated.resources.challenge_info_clear_rows_title
import blockgames.composeapp.generated.resources.challenge_info_perfect_placement
import blockgames.composeapp.generated.resources.challenge_info_perfect_placement_title
import blockgames.composeapp.generated.resources.challenge_info_place_pieces
import blockgames.composeapp.generated.resources.challenge_info_place_pieces_title
import blockgames.composeapp.generated.resources.challenge_info_reach_score
import blockgames.composeapp.generated.resources.challenge_info_reach_score_title
import blockgames.composeapp.generated.resources.challenge_info_solve_words
import blockgames.composeapp.generated.resources.challenge_info_solve_words_title
import blockgames.composeapp.generated.resources.challenge_info_title
import blockgames.composeapp.generated.resources.challenge_info_trigger_special
import blockgames.composeapp.generated.resources.challenge_info_trigger_special_title
import blockgames.composeapp.generated.resources.challenge_progress_value
import blockgames.composeapp.generated.resources.challenge_task_chain_reaction
import blockgames.composeapp.generated.resources.challenge_task_clear_blocks
import blockgames.composeapp.generated.resources.challenge_task_clear_both_directions
import blockgames.composeapp.generated.resources.challenge_task_clear_columns
import blockgames.composeapp.generated.resources.challenge_task_clear_rounds
import blockgames.composeapp.generated.resources.challenge_task_clear_rows
import blockgames.composeapp.generated.resources.challenge_task_complete_columns
import blockgames.composeapp.generated.resources.challenge_task_explode_groups
import blockgames.composeapp.generated.resources.challenge_task_perfect_placement
import blockgames.composeapp.generated.resources.challenge_task_place_pieces
import blockgames.composeapp.generated.resources.challenge_task_reach_score
import blockgames.composeapp.generated.resources.challenge_task_solve_words
import blockgames.composeapp.generated.resources.challenge_task_trigger_special
import blockgames.composeapp.generated.resources.challenge_tasks_title
import blockgames.composeapp.generated.resources.continue_label
import blockgames.composeapp.generated.resources.day_friday_short
import blockgames.composeapp.generated.resources.day_monday_short
import blockgames.composeapp.generated.resources.day_saturday_short
import blockgames.composeapp.generated.resources.day_sunday_short
import blockgames.composeapp.generated.resources.day_thursday_short
import blockgames.composeapp.generated.resources.day_tuesday_short
import blockgames.composeapp.generated.resources.day_wednesday_short
import blockgames.composeapp.generated.resources.home_play_cta
import blockgames.composeapp.generated.resources.month_april
import blockgames.composeapp.generated.resources.month_august
import blockgames.composeapp.generated.resources.month_december
import blockgames.composeapp.generated.resources.month_february
import blockgames.composeapp.generated.resources.month_january
import blockgames.composeapp.generated.resources.month_july
import blockgames.composeapp.generated.resources.month_june
import blockgames.composeapp.generated.resources.month_march
import blockgames.composeapp.generated.resources.month_may
import blockgames.composeapp.generated.resources.month_november
import blockgames.composeapp.generated.resources.month_october
import blockgames.composeapp.generated.resources.month_september
import blockgames.composeapp.generated.resources.settings_challenges
import blockgames.composeapp.generated.resources.tutorial_back
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.logic.ChallengeGenerator
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ChallengeProgress
import com.ugurbuga.blockgames.game.model.ChallengeTask
import com.ugurbuga.blockgames.game.model.ChallengeTaskType
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.color
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.GameSessionSlot
import com.ugurbuga.blockgames.settings.GameSessionStorage
import com.ugurbuga.blockgames.ui.game.BlockStyleActionButton
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.game.rememberBlockStylePulse
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DailyChallengeScreen(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    progress: ChallengeProgress,
    onBack: () -> Unit,
    onPlayChallenge: (DailyChallenge) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val months = remember(currentYear, currentMonth) {
        getPreviousMonths(currentYear, currentMonth, 6)
    }
    val pagerState = rememberPagerState(initialPage = months.size - 1) { months.size }
    val coroutineScope = rememberCoroutineScope()

    var selectedDay by remember(currentDay) { mutableStateOf(currentDay) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val currentMonthYear = months[pagerState.currentPage]

    val selectedChallenge = remember(currentMonthYear, selectedDay, progress) {
        val key = "${currentMonthYear.year}-${currentMonthYear.month.toString().padStart(2, '0')}"
        val dateId = "$key-${selectedDay.toString().padStart(2, '0')}"
        val isCompleted = progress.completedDays[key]?.contains(selectedDay) == true

        if (isCompleted) {
            val base = ChallengeGenerator.generate(
                year = currentMonthYear.year,
                month = currentMonthYear.month,
                day = selectedDay,
            )
            base.copy(tasks = base.tasks.map { it.copy(current = it.target) })
        } else {
            val savedState = GameSessionStorage.load(
                GameSessionSlot.DailyChallenge(
                    dateId = dateId,
                    style = com.ugurbuga.blockgames.platform.GlobalPlatformConfig.gameplayStyle
                )
            )
            savedState?.activeChallenge ?: ChallengeGenerator.generate(
                year = currentMonthYear.year,
                month = currentMonthYear.month,
                day = selectedDay,
            )
        }
    }

    val monthProgress = remember(currentMonthYear, progress) {
        val key = "${currentMonthYear.year}-${currentMonthYear.month.toString().padStart(2, '0')}"
        progress.completedDays[key]?.size?.toFloat()
            ?.div(getDaysInMonth(currentMonthYear.year, currentMonthYear.month).toFloat()) ?: 0f
    }

    val settings = LocalAppSettings.current
    val blockStyle = settings.blockVisualStyle
    val stylePulse = rememberBlockStylePulse(style = blockStyle)

    LaunchedEffect(months) {
        val allowedDateIds = months.flatMap { ym ->
            (1..getDaysInMonth(ym.year, ym.month)).map { day ->
                "${ym.year}-${ym.month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            }
        }
        GameSessionStorage.cleanup(allowedDateIds)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.tutorial_back),
                        onClick = onBack,
                        size = 44.dp,
                        pulse = stylePulse,
                    )
                }

                Text(
                    text = stringResource(Res.string.settings_challenges),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                TrophyIcon(
                    fillProgress = monthProgress,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                )
            }

            // Month Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }

                Text(
                    text = stringResource(
                        Res.string.calendar_month_year_format,
                        monthName(currentMonthYear.month),
                        currentMonthYear.year,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        if (pagerState.currentPage < months.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < months.size - 1
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // Calendar Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val monthYear = months[page]
                val inProgressDays = remember(monthYear) {
                    GameSessionStorage.getSavedDailyChallengeDays(
                        "${monthYear.year}-${monthYear.month.toString().padStart(2, '0')}"
                    )
                }
                CalendarGrid(
                    year = monthYear.year,
                    month = monthYear.month,
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    currentDay = currentDay,
                    completedDays = progress.completedDays["${monthYear.year}-${
                        monthYear.month.toString().padStart(2, '0')
                    }"] ?: emptySet(),
                    inProgressDays = inProgressDays,
                    selectedDay = if (page == pagerState.currentPage) selectedDay else null,
                    onDayClick = { selectedDay = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Selected Day Tasks
            ChallengeTasksCard(
                challenge = selectedChallenge,
                onPlay = { onPlayChallenge(selectedChallenge) },
                onShowInfo = { showInfoDialog = true },
                stylePulse = stylePulse,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        if (showInfoDialog) {
            ChallengeInfoDialog(
                stylePulse = stylePulse,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

@Composable
fun TrophyIcon(
    fillProgress: Float,
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val palette = settings.blockColorPalette
    val isDark = isBlockGamesDarkTheme(settings)

    val baseColor = CellTone.Gold.paletteColor(palette, isDark)
    val emptyColor =
        if (isDark) Color.DarkGray.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val trophyPath = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            lineTo(w * 0.7f, h * 0.2f)
            lineTo(w * 0.65f, h * 0.6f)
            lineTo(w * 0.55f, h * 0.6f)
            lineTo(w * 0.6f, h * 0.8f)
            lineTo(w * 0.4f, h * 0.8f)
            lineTo(w * 0.45f, h * 0.6f)
            lineTo(w * 0.35f, h * 0.6f)
            close()

            // Handles
            moveTo(w * 0.3f, h * 0.25f)
            cubicTo(w * 0.15f, h * 0.25f, w * 0.15f, h * 0.5f, w * 0.33f, h * 0.5f)
            moveTo(w * 0.7f, h * 0.25f)
            cubicTo(w * 0.85f, h * 0.25f, w * 0.85f, h * 0.5f, w * 0.67f, h * 0.5f)
        }

        // Draw empty trophy
        drawPath(
            path = trophyPath,
            color = emptyColor,
            style = Fill
        )

        // Draw filled trophy
        clipRect(
            top = h * (1f - fillProgress),
            bottom = h,
            left = 0f,
            right = w
        ) {
            drawPath(
                path = trophyPath,
                color = baseColor,
                style = Fill
            )

            // Highlight shine
            clipPath(trophyPath) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                        start = Offset(w * 0.4f, 0f),
                        end = Offset(w * 0.6f, h)
                    )
                )
            }
        }

        // Outline
        drawPath(
            path = trophyPath,
            color = baseColor.copy(alpha = 0.8f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun CalendarGrid(
    year: Int,
    month: Int,
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    completedDays: Set<Int>,
    inProgressDays: Set<Int>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit
) {
    val daysInMonth = getDaysInMonth(year, month)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Week headers
        val daysOfWeek = listOf(
            Res.string.day_monday_short,
            Res.string.day_tuesday_short,
            Res.string.day_wednesday_short,
            Res.string.day_thursday_short,
            Res.string.day_friday_short,
            Res.string.day_saturday_short,
            Res.string.day_sunday_short
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            daysOfWeek.forEach { dayRes ->
                Text(
                    text = stringResource(dayRes),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Days
        val totalCells = daysInMonth
        val rows = (totalCells + 6) / 7

        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colIndex in 0 until 7) {
                    val dayIndex = rowIndex * 7 + colIndex
                    if (dayIndex < daysInMonth) {
                        val day = dayIndex + 1
                        val isCompleted = day in completedDays
                        val isSelected = day == selectedDay

                        val isFuture = when {
                            year > currentYear -> true
                            year == currentYear && month > currentMonth -> true
                            year == currentYear && month == currentMonth && day > currentDay -> true
                            else -> false
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            DayCell(
                                day = day,
                                isCompleted = isCompleted,
                                isInProgress = day in inProgressDays,
                                isSelected = isSelected,
                                isEnabled = !isFuture,
                                onClick = { onDayClick(day) }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isCompleted: Boolean,
    isInProgress: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val isDark = isBlockGamesDarkTheme(settings)
    val palette = settings.blockColorPalette

    val baseColor = when {
        isCompleted -> CellTone.Emerald.paletteColor(palette, isDark)
        isSelected -> MaterialTheme.colorScheme.primary
        !isEnabled -> uiColors.panelMuted.copy(alpha = 0.2f)
        else -> uiColors.panelMuted.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isSelected -> Color.White
        isInProgress && !isCompleted -> CellTone.Amber.paletteColor(palette, isDark)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(baseColor)
            .clickable(enabled = isEnabled, onClick = onClick)
            .border(
                width = if (isSelected || (isInProgress && !isCompleted)) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected || isCompleted -> Color.White
                    !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                }
            )
        }
    }
}

@Composable
fun ChallengeTasksCard(
    challenge: DailyChallenge,
    onPlay: () -> Unit,
    onShowInfo: () -> Unit,
    stylePulse: Float,
    modifier: Modifier = Modifier
) {
    val uiColors = BlockGamesThemeTokens.uiColors

    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
            elevation = 8.dp
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(Res.string.challenge_tasks_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onShowInfo,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            challenge.tasks.forEach { task ->
                ChallengeTaskItem(task = task)
            }

            Spacer(modifier = Modifier.height(4.dp))

            BlockStyleActionButton(
                text = stringResource(Res.string.home_play_cta),
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                tone = CellTone.Cyan,
                pulse = stylePulse,
            )
        }
    }
}

@Composable
fun ChallengeTasksDock(
    challenge: DailyChallenge,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        challenge.tasks.forEach { task ->
            ChallengeTaskItem(task = task, compact = true)
        }
    }
}

@Composable
fun ChallengeTaskItem(
    task: ChallengeTask,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val isCompleted = task.isCompleted

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 16.dp else 24.dp)
                .clip(CircleShape)
                .background(if (isCompleted) CellTone.Emerald.color() else uiColors.panelMuted)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 10.dp else 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))

        Text(
            text = taskDescription(task),
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            color = if (isCompleted) CellTone.Emerald.color() else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = stringResource(Res.string.challenge_progress_value, task.current, task.target),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (compact) 0.4f else 0.5f),
            fontSize = if (compact) 9.sp else 11.sp
        )
    }
}

@Composable
fun taskDescription(task: ChallengeTask): String {
    val gameplayStyle = LocalAppSettings.current.selectedGameplayStyle
        ?: com.ugurbuga.blockgames.platform.GlobalPlatformConfig.gameplayStyle

    val res = when (task.type) {
        ChallengeTaskType.ClearBlocks -> Res.string.challenge_task_clear_blocks
        ChallengeTaskType.TriggerSpecial -> Res.string.challenge_task_trigger_special
        ChallengeTaskType.PerfectPlacement -> Res.string.challenge_task_perfect_placement
        ChallengeTaskType.ChainReaction -> Res.string.challenge_task_chain_reaction
        ChallengeTaskType.ClearRows -> Res.string.challenge_task_clear_rows
        ChallengeTaskType.ReachScore -> Res.string.challenge_task_reach_score
        ChallengeTaskType.ClearColumns -> {
            if (gameplayStyle == GameplayStyle.BlockSort) {
                Res.string.challenge_task_complete_columns
            } else {
                Res.string.challenge_task_clear_columns
            }
        }
        ChallengeTaskType.PlacePieces -> {
            if (gameplayStyle == GameplayStyle.BoomBlocks) {
                Res.string.challenge_task_explode_groups
            } else {
                Res.string.challenge_task_place_pieces
            }
        }
        ChallengeTaskType.ClearRounds -> Res.string.challenge_task_clear_rounds
        ChallengeTaskType.ClearBothDirections -> Res.string.challenge_task_clear_both_directions
        ChallengeTaskType.SolveWords -> Res.string.challenge_task_solve_words
    }
    return stringResource(res, task.target)
}

@Composable
fun ChallengeInfoDialog(
    stylePulse: Float,
    onDismiss: () -> Unit
) {
    val uiColors = BlockGamesThemeTokens.uiColors

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface),
            border = BorderStroke(1.dp, uiColors.panelStroke),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.challenge_info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChallengeTaskType.forStyle().forEach { taskType ->
                        ChallengeInfoItem(
                            title = challengeInfoTitle(taskType),
                            description = challengeInfoDescription(taskType),
                        )
                    }
                }

                BlockStyleActionButton(
                    text = stringResource(Res.string.continue_label),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    tone = CellTone.Cyan,
                    pulse = stylePulse,
                )
            }
        }
    }
}

internal fun challengeInfoTitleRes(taskType: ChallengeTaskType): StringResource = when (taskType) {
        ChallengeTaskType.ClearBlocks -> Res.string.challenge_info_clear_blocks_title
        ChallengeTaskType.ReachScore -> Res.string.challenge_info_reach_score_title
        ChallengeTaskType.TriggerSpecial -> Res.string.challenge_info_trigger_special_title
        ChallengeTaskType.PerfectPlacement -> Res.string.challenge_info_perfect_placement_title
        ChallengeTaskType.ChainReaction -> Res.string.challenge_info_chain_reaction_title
        ChallengeTaskType.ClearRows -> Res.string.challenge_info_clear_rows_title
        ChallengeTaskType.ClearColumns -> Res.string.challenge_info_clear_columns_title
        ChallengeTaskType.PlacePieces -> Res.string.challenge_info_place_pieces_title
        ChallengeTaskType.ClearRounds -> Res.string.challenge_info_clear_rounds_title
        ChallengeTaskType.ClearBothDirections -> Res.string.challenge_info_clear_both_directions_title
        ChallengeTaskType.SolveWords -> Res.string.challenge_info_solve_words_title
    }

@Composable
private fun challengeInfoTitle(taskType: ChallengeTaskType): String = stringResource(challengeInfoTitleRes(taskType))

internal fun challengeInfoDescriptionRes(taskType: ChallengeTaskType): StringResource = when (taskType) {
        ChallengeTaskType.ClearBlocks -> Res.string.challenge_info_clear_blocks
        ChallengeTaskType.ReachScore -> Res.string.challenge_info_reach_score
        ChallengeTaskType.TriggerSpecial -> Res.string.challenge_info_trigger_special
        ChallengeTaskType.PerfectPlacement -> Res.string.challenge_info_perfect_placement
        ChallengeTaskType.ChainReaction -> Res.string.challenge_info_chain_reaction
        ChallengeTaskType.ClearRows -> Res.string.challenge_info_clear_rows
        ChallengeTaskType.ClearColumns -> Res.string.challenge_info_clear_columns
        ChallengeTaskType.PlacePieces -> Res.string.challenge_info_place_pieces
        ChallengeTaskType.ClearRounds -> Res.string.challenge_info_clear_rounds
        ChallengeTaskType.ClearBothDirections -> Res.string.challenge_info_clear_both_directions
        ChallengeTaskType.SolveWords -> Res.string.challenge_info_solve_words
    }

@Composable
private fun challengeInfoDescription(taskType: ChallengeTaskType): String = stringResource(challengeInfoDescriptionRes(taskType))

@Composable
private fun ChallengeInfoItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title.trim(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

internal fun monthNameRes(month: Int): StringResource? = when (month) {
    1 -> Res.string.month_january
    2 -> Res.string.month_february
    3 -> Res.string.month_march
    4 -> Res.string.month_april
    5 -> Res.string.month_may
    6 -> Res.string.month_june
    7 -> Res.string.month_july
    8 -> Res.string.month_august
    9 -> Res.string.month_september
    10 -> Res.string.month_october
    11 -> Res.string.month_november
    12 -> Res.string.month_december
    else -> null
}

@Composable
fun monthName(month: Int): String {
    return monthNameRes(month)?.let { stringResource(it) } ?: ""
}

data class YearMonth(val year: Int, val month: Int)

fun getPreviousMonths(year: Int, month: Int, count: Int): List<YearMonth> {
    val months = mutableListOf<YearMonth>()
    var y = year
    var m = month
    repeat(count) {
        months.add(0, YearMonth(y, m))
        m--
        if (m == 0) {
            m = 12
            y--
        }
    }
    return months
}

fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        else -> 30
    }
}

@Preview
@Composable
fun DailyChallengeScreenPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Flat,
        themeColorPalette = AppColorPalette.Classic
    )
    BlockGamesTheme(settings = settings) {
        DailyChallengeScreen(
            currentYear = 2025,
            currentMonth = 12,
            currentDay = 8,
            progress = ChallengeProgress(),
            onBack = {},
            onPlayChallenge = {}
        )
    }
}

@Preview
@Composable
fun DailyChallengeScreenCompletedMonthPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Crystal,
        themeColorPalette = AppColorPalette.Classic
    )
    val year = 2025
    val month = 12
    val daysInMonth = getDaysInMonth(year, month)
    BlockGamesTheme(settings = settings) {
        DailyChallengeScreen(
            currentYear = year,
            currentMonth = month,
            currentDay = 31,
            progress = ChallengeProgress(
                completedDays = mapOf(
                    "$year-${month.toString().padStart(2, '0')}" to (1..daysInMonth).toSet()
                )
            ),
            onBack = {},
            onPlayChallenge = {}
        )
    }
}

@Preview
@Composable
fun DailyChallengeScreenHalfCompletedMonthPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Crystal,
        themeColorPalette = AppColorPalette.Classic
    )
    val year = 2025
    val month = 12
    val daysInMonth = getDaysInMonth(year, month)
    BlockGamesTheme(settings = settings) {
        DailyChallengeScreen(
            currentYear = year,
            currentMonth = month,
            currentDay = 15,
            progress = ChallengeProgress(
                completedDays = mapOf(
                    "$year-${month.toString().padStart(2, '0')}" to (1..(daysInMonth / 2)).toSet()
                )
            ),
            onBack = {},
            onPlayChallenge = {}
        )
    }
}

@Preview
@Composable
fun DailyChallengeScreenLightPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Light,
        blockVisualStyle = BlockVisualStyle.Bubble,
        themeColorPalette = AppColorPalette.SoftPastel
    )
    BlockGamesTheme(settings = settings) {
        DailyChallengeScreen(
            currentYear = 2025,
            currentMonth = 12,
            currentDay = 8,
            progress = ChallengeProgress(),
            onBack = {},
            onPlayChallenge = {}
        )
    }
}
