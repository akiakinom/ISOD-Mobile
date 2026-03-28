package dev.akinom.isod

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import dev.akinom.isod.auth.currentDayOfWeek
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.news.toLabel
import dev.akinom.isod.news.toShortLabel
import dev.akinom.isod.news.typeToColor
import dev.akinom.isod.news.typeToIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleScreenModel(val semester: String) : ScreenModel, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()

    private val _selectedWeek = MutableStateFlow(AcademicCalendar.getCurrentWeek(semester) ?: 1)
    val selectedWeek: StateFlow<Int> = _selectedWeek

    val actualCurrentWeek = AcademicCalendar.getCurrentWeek(semester)

    val weekMonday: StateFlow<LocalDate> = _selectedWeek
        .map { week ->
            AcademicCalendar.getMondayOfWeek(semester, week) ?: AcademicCalendar.getToday()
        }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), AcademicCalendar.getToday())

    val timetable: StateFlow<List<TimetableEntry>> =
        weekMonday.flatMapLatest { monday ->
            timetableRepo.getTimetable(semester, monday.toString())
        }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectWeek(week: Int) {
        _selectedWeek.value = week.coerceIn(1, 15)
    }

    val availableWeeks = (1..15).toList()
}

class ScheduleScreen(
    val semester: String = currentSemester(),
    val initialDayOfWeek: Int? = null
) : Screen, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { ScheduleScreenModel(semester) }
        val timetable by screenModel.timetable.collectAsState()
        val selectedWeek by screenModel.selectedWeek.collectAsState()
        val weekMonday by screenModel.weekMonday.collectAsState()

        val dayNames = listOf(
            stringResource(Res.string.day_mon),
            stringResource(Res.string.day_tue),
            stringResource(Res.string.day_wed),
            stringResource(Res.string.day_thu),
            stringResource(Res.string.day_fri)
        )

        val initialDay = remember {
            val day = initialDayOfWeek ?: currentDayOfWeek()
            if (day > 5) 0 else day - 1
        }
        val pagerState = rememberPagerState(initialPage = initialDay, pageCount = { dayNames.size })
        val scope = rememberCoroutineScope()

        val currentTime by produceState<LocalTime>(
            initialValue = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        ) {
            while (true) {
                delay(30_000)
                value = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
            }
        }

        val todayDate = remember { AcademicCalendar.getToday() }
        val isViewingCurrentWeek = remember(selectedWeek, screenModel.actualCurrentWeek) {
            selectedWeek == screenModel.actualCurrentWeek
        }

        // Sync pager state when initialDayOfWeek changes
        LaunchedEffect(initialDayOfWeek) {
            initialDayOfWeek?.let { day ->
                val targetPage = if (day > 5) 0 else day - 1
                if (pagerState.currentPage != targetPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }

        var selectedEntryForOverride by remember { mutableStateOf<TimetableEntry?>(null) }
        var showWeekPicker by remember { mutableStateOf(false) }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { screenModel.selectWeek(selectedWeek - 1) },
                                enabled = selectedWeek > 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                            }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showWeekPicker = true }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(Res.string.weekly_schedule), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(Res.string.week_number, selectedWeek),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { screenModel.selectWeek(selectedWeek + 1) },
                                enabled = selectedWeek < 15
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                val showJumpToToday = !isViewingCurrentWeek
                AnimatedVisibility(
                    visible = showJumpToToday,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            scope.launch {
                                screenModel.actualCurrentWeek?.let { screenModel.selectWeek(it) }
                            }
                        },
                        icon = { Icon(Icons.Default.Today, null) },
                        text = { Text(stringResource(Res.string.current_week)) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            bottomBar = {
                val currentDayDate = remember(pagerState.currentPage, weekMonday) {
                    LocalDate.fromEpochDays(weekMonday.toEpochDays() + pagerState.currentPage)
                }
                val substitution = remember(currentDayDate) {
                    AcademicCalendar.getDaySubstitution(currentDayDate)
                }

                if (substitution != null) {
                    val substitutedDayName = when (substitution) {
                        1 -> stringResource(Res.string.full_day_mon)
                        2 -> stringResource(Res.string.full_day_tue)
                        3 -> stringResource(Res.string.full_day_wed)
                        4 -> stringResource(Res.string.full_day_thu)
                        5 -> stringResource(Res.string.full_day_fri)
                        6 -> stringResource(Res.string.full_day_sat)
                        7 -> stringResource(Res.string.full_day_sun)
                        else -> ""
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.day_substitution_label, substitutedDayName),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Modern Date Ribbon
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {},
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(pagerState.currentPage, true),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                ) {
                    dayNames.forEachIndexed { index, name ->
                        val date = remember(weekMonday, index) {
                            LocalDate.fromEpochDays(weekMonday.toEpochDays() + index)
                        }
                        val isToday = date == todayDate

                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = date.day.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val currentDayDate = remember(page, weekMonday) {
                        LocalDate.fromEpochDays(weekMonday.toEpochDays() + page)
                    }
                    val effectiveDayOfWeek = remember(currentDayDate) {
                        AcademicCalendar.getEffectiveDayOfWeek(currentDayDate)
                    }

                    val filteredEntries = timetable.filter { it.dayOfWeek == effectiveDayOfWeek }

                    if (filteredEntries.isEmpty()) {
                        EmptyDayView()
                    } else {
                        val groupedEntries = groupOverlapping(filteredEntries)
                        val sortedGroups = groupedEntries.sortedBy { it.minOf { e -> e.startTime.toMinutes() } }
                        val isToday = currentDayDate == todayDate

                        ScheduleTimeline(
                            groups = sortedGroups,
                            isToday = isToday,
                            currentTime = currentTime,
                            selectedWeek = selectedWeek,
                            onLongClick = { selectedEntryForOverride = it }
                        )
                    }
                }
            }

            // Week Picker Bottom Sheet
            if (showWeekPicker) {
                WeekBottomSheet(
                    weeks = screenModel.availableWeeks,
                    currentWeek = selectedWeek,
                    actualCurrentWeek = screenModel.actualCurrentWeek,
                    semesterId = semester,
                    onSelected = {
                        screenModel.selectWeek(it)
                        showWeekPicker = false
                    },
                    onDismiss = { showWeekPicker = false }
                )
            }

            // Override Dialog
            if (selectedEntryForOverride != null) {
                OverrideDialog(
                    entry = selectedEntryForOverride!!,
                    onDismiss = { selectedEntryForOverride = null },
                    onOverride = { entryId, code ->
                        timetableRepo.setCycleOverride(entryId, code)
                        selectedEntryForOverride = null
                    },
                    onReset = { entryId ->
                        timetableRepo.setCycleOverride(entryId, null)
                        selectedEntryForOverride = null
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyDayView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    Icons.Default.NightsStay,
                    null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(Res.string.no_classes_scheduled), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(Res.string.take_rest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScheduleTimeline(
    groups: List<List<TimetableEntry>>,
    isToday: Boolean,
    currentTime: LocalTime,
    selectedWeek: Int?,
    onLongClick: (TimetableEntry) -> Unit
) {
    val listState = rememberLazyListState()
    val currentMinutes = currentTime.hour * 60 + currentTime.minute

    // Auto-scroll to current class on first load if it's today
    LaunchedEffect(isToday) {
        if (isToday) {
            val currentGroupIdx = groups.indexOfFirst { group ->
                val start = group.minOf { it.startTime.toMinutes() }
                val end = group.maxOf { it.endTime.toMinutes() }
                currentMinutes in start..end
            }
            if (currentGroupIdx != -1) {
                // Adjust scroll index slightly to account for potential separators
                // Since separators are inserted between items, we just scroll to the group index * 2 roughly
                // But LazyColumn index is literal.
                // A better way is to find the index in the LazyColumn, but for now we'll just scroll.
                listState.animateScrollToItem(currentGroupIdx.coerceAtLeast(0))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vertical Timeline Line - Centered behind the times column
        Box(
            modifier = Modifier
                .padding(start = 40.dp) // Adjusted to align with centered times (8dp padding + 64dp/2 center = 40dp)
                .fillMaxHeight()
                .width(1.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 24.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            var indicatorShown = false

            // Initial NOW indicator if before first class
            val firstStart = groups.firstOrNull()?.minOf { it.startTime.toMinutes() } ?: 0
            if (isToday && currentMinutes < firstStart) {
                item { TimelineNowIndicator() }
                indicatorShown = true
            }

            groups.forEachIndexed { index, group ->
                val groupStart = group.minOf { it.startTime.toMinutes() }
                val groupEnd = group.maxOf { it.endTime.toMinutes() }

                // Gap or NOW separator between classes
                if (index > 0) {
                    val prevEnd = groups[index - 1].maxOf { it.endTime.toMinutes() }
                    val gap = groupStart - prevEnd
                    val isNowInGap = isToday && currentMinutes in prevEnd until groupStart

                    if (gap > 45) {
                        item {
                            TimelineGapSeparator(
                                gapMinutes = gap,
                                isNow = isNowInGap
                            )
                        }
                        if (isNowInGap) indicatorShown = true
                    } else if (isNowInGap) {
                        item { TimelineNowIndicator() }
                        indicatorShown = true
                    }
                }

                item {
                    TimelineGroup(
                        group = group,
                        isToday = isToday,
                        currentMinutes = currentMinutes,
                        selectedWeek = selectedWeek,
                        onLongClick = onLongClick
                    )
                }

                if (isToday && currentMinutes in groupStart..groupEnd) {
                    indicatorShown = true
                }
            }

            // Indicator AFTER all
            val lastEnd = groups.lastOrNull()?.maxOf { it.endTime.toMinutes() } ?: 0
            if (isToday && !indicatorShown && groups.isNotEmpty() && currentMinutes > lastEnd) {
                item { TimelineNowIndicator() }
                indicatorShown = true
            }
        }
    }
}

@Composable
private fun TimelineGroup(
    group: List<TimetableEntry>,
    isToday: Boolean,
    currentMinutes: Int,
    selectedWeek: Int?,
    onLongClick: (TimetableEntry) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Column - Centered horizontally
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val startTime = group.first().startTime
            val endTime = group.first().endTime

            Text(
                text = startTime,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // The Cards
        Box(modifier = Modifier.weight(1f)) {
            if (group.size == 1) {
                ScheduleItemV2(
                    entry = group[0],
                    isToday = isToday,
                    currentMinutes = currentMinutes,
                    selectedWeek = selectedWeek,
                    onLongClick = onLongClick
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.sortedBy { it.courseName }.forEach { entry ->
                        ScheduleItemV2(
                            entry = entry,
                            modifier = Modifier.weight(1f),
                            isToday = isToday,
                            currentMinutes = currentMinutes,
                            selectedWeek = selectedWeek,
                            isSplit = true,
                            onLongClick = onLongClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleItemV2(
    entry: TimetableEntry,
    modifier: Modifier = Modifier,
    isToday: Boolean,
    currentMinutes: Int,
    selectedWeek: Int?,
    isSplit: Boolean = false,
    onLongClick: (TimetableEntry) -> Unit
) {
    val startMin = entry.startTime.toMinutes()
    val endMin = entry.endTime.toMinutes()
    val isHappeningNow = isToday && currentMinutes in startMin..endMin
    val isActive = entry.isActive(selectedWeek)
    val accentColor = typeToColor(entry.courseType)

    val progress = if (isHappeningNow && endMin > startMin) {
        (currentMinutes - startMin).toFloat() / (endMin - startMin).toFloat()
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else 0.4f)
            .saturation(if (isActive) 1f else 0f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHappeningNow) 8.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHappeningNow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = if (isHappeningNow) 2.dp else 1.dp,
            color = if (isHappeningNow) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    if (isHappeningNow && progress > 0f) {
                        drawRect(
                            color = accentColor.copy(alpha = 0.1f),
                            size = size.copy(height = size.height * progress)
                        )
                    }
                }
                .combinedClickable(
                    onClick = { /* TODO: Navigate to SubjectScreen once implemented */ },
                    onLongClick = { onLongClick(entry) }
                )
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = entry.shortType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (isHappeningNow) {
                        val remaining = endMin - currentMinutes
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${remaining}m left",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = entry.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = entry.displayLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isSplit && entry.lecturerNames.isNotEmpty()) {
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = entry.lecturerNames.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNowIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 36.dp) // Aligned with the 40dp axis (40 - 8/2 = 36)
                .size(8.dp)
                .drawBehind {
                    drawCircle(
                        color = Color.Red,
                        radius = size.minDimension / 2,
                        alpha = glowAlpha
                    )
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.2f),
                        radius = (size.minDimension / 2) * 2.5f * glowAlpha
                    )
                }
        )

        Spacer(Modifier.width(28.dp))

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color.Red.copy(alpha = 0.4f)
        )

        Text(
            text = stringResource(Res.string.now),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Red,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun TimelineGapSeparator(
    gapMinutes: Int,
    isNow: Boolean,
) {
    val color = if (isNow) Color.Red else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by if (isNow) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        if (isNow) {
            Box(
                modifier = Modifier
                    .padding(start = 36.dp)
                    .size(8.dp)
                    .drawBehind {
                        drawCircle(color = Color.Red, radius = size.minDimension / 2, alpha = glowAlpha)
                        drawCircle(color = Color.Red.copy(alpha = 0.2f), radius = (size.minDimension / 2) * 2.5f * glowAlpha)
                    }
            )
            Spacer(Modifier.width(28.dp))
        } else {
            Spacer(Modifier.width(72.dp))
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            WavyLine(
                modifier = Modifier.fillMaxWidth(),
                color = color,
                thickness = if (isNow) 1.5.dp else 1.dp
            )

            val gapText = if (gapMinutes >= 60) {
                stringResource(Res.string.gap_label_hours_mins, gapMinutes / 60, gapMinutes % 60)
            } else {
                stringResource(Res.string.gap_label_mins, gapMinutes)
            }

            val displayText = if (isNow) "${stringResource(Res.string.now)} • $gapText" else gapText

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = if (isNow) FontWeight.Black else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WavyLine(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    waveLength: Float = 32f,
    waveHeight: Float = 8f,
    thickness: Dp = 1.dp
) {
    Canvas(modifier = modifier.height(waveHeight.dp * 2)) {
        val path = Path()
        val width = size.width
        val height = size.height

        path.moveTo(0f, height / 2)
        var x = 0f
        while (x < width) {
            path.relativeQuadraticTo(waveLength / 4, -waveHeight, waveLength / 2, 0f)
            path.relativeQuadraticTo(waveLength / 4, waveHeight, waveLength / 2, 0f)
            x += waveLength
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekBottomSheet(
    weeks: List<Int>,
    currentWeek: Int,
    actualCurrentWeek: Int?,
    semesterId: String,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = stringResource(Res.string.weekly_schedule),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 3x5 Grid
            weeks.chunked(3).forEach { rowWeeks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowWeeks.forEach { week ->
                        val monday = AcademicCalendar.getMondayOfWeek(semesterId, week)
                        val range = monday?.let { AcademicCalendar.getWeekRangeString(it) } ?: ""
                        val isActual = week == actualCurrentWeek
                        val isSelected = week == currentWeek

                        WeekGridItem(
                            week = week,
                            isSelected = isSelected,
                            isActual = isActual,
                            dateRange = range,
                            onClick = { onSelected(week) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty spaces if last row is not full
                    if (rowWeeks.size < 3) {
                        repeat(3 - rowWeeks.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WeekGridItem(
    week: Int,
    isSelected: Boolean,
    isActual: Boolean,
    dateRange: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = if (isActual) 2.dp else 1.dp,
            color = if (isActual) MaterialTheme.colorScheme.primary
            else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.height(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isActual) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = week.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isSelected || isActual) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (dateRange.isNotEmpty()) {
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OverrideDialog(
    entry: TimetableEntry,
    onDismiss: () -> Unit,
    onOverride: (String, String) -> Unit,
    onReset: (String) -> Unit
) {
    var tempOverrideCode by remember { mutableStateOf(entry.userCycleOverride ?: entry.cycle) }

    // helper to get active weeks for a standard code
    fun getWeeksForCode(code: String): Set<Int> {
        return (1..15).filter { week ->
            when (code.uppercase()) {
                "SEM" -> true
                "1PS" -> week <= 8
                "2PS" -> week >= 8
                "PA" -> week % 2 == 0
                "NP" -> week % 2 != 0
                else -> false
            }
        }.toSet()
    }

    var selectedWeeks by remember {
        val isCustom = tempOverrideCode.startsWith("W:")
        mutableStateOf(
            if (isCustom) {
                tempOverrideCode.removePrefix("W:").split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            } else {
                getWeeksForCode(tempOverrideCode)
            }
        )
    }

    val options = listOf(
        CycleOption("SEM", Res.string.cycle_sem, Icons.Default.DateRange),
        CycleOption("NONE", Res.string.cycle_none, Icons.Default.VisibilityOff),
        CycleOption("1PS", Res.string.cycle_1ps, Icons.Default.LooksOne),
        CycleOption("2PS", Res.string.cycle_2ps, Icons.Default.LooksTwo),
        CycleOption("PA", Res.string.cycle_pa, Icons.Default.Repeat),
        CycleOption("NP", Res.string.cycle_np, Icons.Default.RepeatOne)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.override_cycle_title)) },
        text = {
            Column {
                Text(
                    text = "${entry.courseName} [${entry.courseType.toLabel()}]",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Standard options in a grid
                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = !tempOverrideCode.startsWith("W:") && tempOverrideCode == option.code
                            Surface(
                                onClick = {
                                    tempOverrideCode = option.code
                                    selectedWeeks = getWeeksForCode(option.code)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(option.label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = stringResource(Res.string.cycle_custom),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Custom weeks grid (5 items per row for alignment)
                (1..15).chunked(5).forEach { rowWeeks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowWeeks.forEach { week ->
                            val isSelected = selectedWeeks.contains(week)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newWeeks = if (isSelected) {
                                        selectedWeeks - week
                                    } else {
                                        selectedWeeks + week
                                    }
                                    selectedWeeks = newWeeks
                                    tempOverrideCode = "W:" + newWeeks.sorted().joinToString(",")
                                },
                                label = {
                                    Text(
                                        week.toString(),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier.weight(1f).height(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        tempOverrideCode = entry.cycle
                        selectedWeeks = getWeeksForCode(entry.cycle)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.reset_to_default))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tempOverrideCode == entry.cycle && entry.userCycleOverride != null) {
                        onReset(entry.id)
                    } else {
                        onOverride(entry.id, tempOverrideCode)
                    }
                },
                enabled = !tempOverrideCode.startsWith("W:") || selectedWeeks.isNotEmpty()
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

private data class CycleOption(val code: String, val label: StringResource, val icon: ImageVector)

fun Modifier.saturation(value: Float): Modifier = if (value != 1f) {
    this.drawWithCache {
        val matrix = ColorMatrix().apply { setToSaturation(value) }
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                drawContent()
                canvas.restore()
            }
        }
    }
} else this

private fun String.toMinutes(): Int {
    val parts = split(":")
    if (parts.size < 2) return 0
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}

private fun TimetableEntry.overlapsWith(other: TimetableEntry): Boolean {
    val start1 = this.startTime.toMinutes()
    val end1 = this.endTime.toMinutes()
    val start2 = other.startTime.toMinutes()
    val end2 = other.endTime.toMinutes()
    return maxOf(start1, start2) < minOf(end1, end2)
}

private fun groupOverlapping(entries: List<TimetableEntry>): List<List<TimetableEntry>> {
    val sorted = entries.sortedBy { it.startTime }
    val result = mutableListOf<MutableList<TimetableEntry>>()

    for (entry in sorted) {
        var added = false
        for (group in result) {
            if (group.any { it.overlapsWith(entry) }) {
                group.add(entry)
                added = true
                break
            }
        }
        if (!added) {
            result.add(mutableListOf(entry))
        }
    }
    return result
}
