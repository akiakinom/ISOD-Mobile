package dev.akinom.isod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import dev.akinom.isod.auth.currentDayOfWeek
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.news.typeToColor
import dev.akinom.isod.news.typeToIcon
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScheduleScreen(
    val semester: String = currentSemester(),
    val initialDayOfWeek: Int? = null
) : Screen, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel(semester) }
        val timetable by screenModel.timetable.collectAsState()
        val currentWeek = screenModel.currentWeek
        
        val days = listOf(
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
        val pagerState = rememberPagerState(initialPage = initialDay, pageCount = { days.size })
        val scope = rememberCoroutineScope()

        // Sync pager state when initialDayOfWeek changes (e.g. navigation from Dashboard)
        LaunchedEffect(initialDayOfWeek) {
            initialDayOfWeek?.let { day ->
                val targetPage = if (day > 5) 0 else day - 1
                if (pagerState.currentPage != targetPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }

        var selectedEntryForOverride by remember { mutableStateOf<TimetableEntry?>(null) }

        val weekMonday = remember(screenModel.weekMonday) {
            val parts = screenModel.weekMonday.split("-")
            LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(Res.string.weekly_schedule), fontWeight = FontWeight.Bold)
                            if (currentWeek != null) {
                                Text(
                                    stringResource(Res.string.week_number, currentWeek),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
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

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(Res.string.day_substitution_label, substitutedDayName),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Day Selector
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    divider = {},
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    days.forEachIndexed { index, day ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { 
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    day,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val dayOfWeek = page + 1
                    
                    val currentDayDate = remember(page, weekMonday) {
                        LocalDate.fromEpochDays(weekMonday.toEpochDays() + page)
                    }
                    val effectiveDayOfWeek = remember(currentDayDate) {
                        AcademicCalendar.getEffectiveDayOfWeek(currentDayDate)
                    }
                    
                    val filteredEntries = timetable.filter { it.dayOfWeek == effectiveDayOfWeek }

                    if (filteredEntries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.NightsStay,
                                    null,
                                    modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Text(stringResource(Res.string.no_classes_scheduled), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(Res.string.take_rest), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val groupedEntries = groupOverlapping(filteredEntries)
                            items(groupedEntries) { group ->
                                if (group.size == 1) {
                                    ScheduleItem(
                                        entry = group[0],
                                        isSplit = false,
                                        currentWeek = currentWeek,
                                        onLongClick = { selectedEntryForOverride = it }
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        group.forEach { entry ->
                                            ScheduleItem(
                                                entry = entry,
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                isSplit = true,
                                                currentWeek = currentWeek,
                                                onLongClick = { selectedEntryForOverride = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedEntryForOverride != null) {
                val entry = selectedEntryForOverride!!
                AlertDialog(
                    onDismissRequest = { selectedEntryForOverride = null },
                    title = { Text(stringResource(Res.string.override_cycle_title)) },
                    text = {
                        Column {
                            Text(
                                text = entry.courseName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            val options = listOf(
                                CycleOption("SEM", Res.string.cycle_sem, Icons.Default.DateRange),
                                CycleOption("1PS", Res.string.cycle_1ps, Icons.Default.ArrowUpward),
                                CycleOption("2PS", Res.string.cycle_2ps, Icons.Default.ArrowDownward),
                                CycleOption("PA", Res.string.cycle_pa, Icons.Default.Repeat),
                                CycleOption("NP", Res.string.cycle_np, Icons.Default.RepeatOne),
                                CycleOption("NONE", Res.string.cycle_none, Icons.Default.VisibilityOff)
                            )

                            options.forEach { option ->
                                val isSelected = (entry.userCycleOverride ?: entry.cycle) == option.code
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            timetableRepo.setCycleOverride(entry.id, option.code)
                                            selectedEntryForOverride = null
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(option.label),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                }
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            TextButton(
                                onClick = {
                                    timetableRepo.setCycleOverride(entry.id, null)
                                    selectedEntryForOverride = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.reset_to_default))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedEntryForOverride = null }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

private data class CycleOption(val code: String, val label: StringResource, val icon: ImageVector)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleItem(
    entry: TimetableEntry,
    modifier: Modifier = Modifier,
    isSplit: Boolean = false,
    currentWeek: Int? = null,
    onLongClick: (TimetableEntry) -> Unit = {}
) {
    val isActive = entry.isActive(currentWeek)
    val accentColor = typeToColor(entry.courseType)
    val typeIcon = typeToIcon(entry.courseType)
    val typeDisplay = entry.shortType

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else 0.4f)
            .saturation(if (isActive) 1f else 0f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongClick(entry) }
                )
        ) {
            // Time Sidebar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "${entry.startTime} — ${entry.endTime}",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.userCycleOverride != null) {
                            Icon(
                                Icons.Default.Edit,
                                null,
                                modifier = Modifier.size(12.dp).padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = accentColor.copy(alpha = 0.1f),
                            contentColor = accentColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Icon(typeIcon, null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = typeDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = entry.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(4.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        text = entry.displayLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isSplit && entry.lecturerNames.isNotEmpty()) {
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(4.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = entry.lecturerNames.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                
                if (isSplit && entry.lecturerNames.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(4.dp))
                        @Suppress("DEPRECATION")
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
