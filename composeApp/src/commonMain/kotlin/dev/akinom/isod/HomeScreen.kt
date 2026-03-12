package dev.akinom.isod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.auth.currentDayOfWeek
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.auth.currentTimeHHmm
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.NewsRepository
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableWidgetLogic
import dev.akinom.isod.news.NewsDetailScreen
import dev.akinom.isod.news.getDisplaySubject
import dev.akinom.isod.news.getTagColors
import dev.akinom.isod.news.parseDateToSortable
import dev.akinom.isod.news.parseSubject
import dev.akinom.isod.news.toColor
import dev.akinom.isod.news.toStringRes
import dev.akinom.isod.news.typeToColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeScreenModel(val semester: String) : ScreenModel, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()
    private val newsRepo: NewsRepository           by inject()

    val weekMonday = currentWeekMonday()
    val currentWeek = AcademicCalendar.getCurrentWeek(semester)

    val timetable: StateFlow<List<TimetableEntry>> =
        timetableRepo.getTimetable(semester, weekMonday)
            .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val news: StateFlow<List<NewsHeader>> =
        newsRepo.getNewsHeaders(semester)
            .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun refresh() {
        val newsJob = screenModelScope.launch { newsRepo.refreshHeaders(semester) }
        newsJob.join()
        delay(300)
    }
}

class HomeScreen(
    val semester: String = currentSemester(),
    val onMoveToTab: (MainTab, Int?) -> Unit = { _, _ -> }
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel(semester) }
        val timetable by screenModel.timetable.collectAsState()
        val news by screenModel.news.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val today = currentDayOfWeek()
        val now = currentTimeHHmm()
        
        val (isAfterLessons, dashboardClasses) = remember(timetable, today, now, screenModel.currentWeek) {
            TimetableWidgetLogic.getDashboardSchedule(
                entries = timetable,
                todayDayOfWeek = today,
                currentTime = now,
                currentWeek = screenModel.currentWeek,
                todayDate = AcademicCalendar.getToday()
            )
        }

        val nextClasses = remember(timetable, today, now, screenModel.currentWeek) {
            TimetableWidgetLogic.getNextClasses(
                entries = timetable,
                todayDayOfWeek = today,
                currentTime = now,
                currentWeek = screenModel.currentWeek,
                todayDate = AcademicCalendar.getToday()
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    screenModel.refresh()
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        @Suppress("DEPRECATION")
                        Text(
                            stringResource(Res.string.hello),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { navigator.push(SettingsScreen()) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        @Suppress("DEPRECATION")
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Next Class Card
                val nextClass = nextClasses.firstOrNull()
                if (nextClass != null) {
                    NextClassCard(nextClass, today, now) {
                        onMoveToTab(MainTab.Schedule, nextClass.dayOfWeek)
                    }
                }

                // Today's/Tomorrow's Timeline Summary
                DashboardSection(
                    title = stringResource(if (isAfterLessons) Res.string.tomorrows_timeline else Res.string.todays_timeline),
                    icon = Icons.Default.CalendarToday,
                    onSeeAll = { 
                        val targetDay = if (isAfterLessons) (today % 7) + 1 else today
                        onMoveToTab(MainTab.Schedule, targetDay)
                    }
                ) {
                    if (dashboardClasses.isEmpty()) {
                        EmptyDashboardState(
                            message = stringResource(if (isAfterLessons) Res.string.no_classes_tomorrow else Res.string.no_classes_today),
                            icon = Icons.Default.NightsStay
                        )
                    } else {
                        dashboardClasses.take(3).forEach { entry ->
                            CompactTimetableItem(entry) {
                                onMoveToTab(MainTab.Schedule, entry.dayOfWeek)
                            }
                        }
                    }
                }

                // Latest Announcements
                DashboardSection(
                    title = stringResource(Res.string.announcements),
                    icon = Icons.Default.Notifications,
                    onSeeAll = { onMoveToTab(MainTab.News, null) }
                ) {
                    if (news.isEmpty()) {
                        EmptyDashboardState(
                            message = stringResource(Res.string.no_news_yet),
                            icon = Icons.Default.Inbox
                        )
                    } else {
                        val sortedNews = remember(news) {
                            news.sortedByDescending { it.parseDateToSortable() }
                        }
                        sortedNews.take(3).forEach { item ->
                            CompactNewsItem(item) {
                                navigator.push(NewsDetailScreen(item.hash))
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NextClassCard(entry: TimetableEntry, today: Int, currentTime: String, onClick: () -> Unit) {
    val accentColor = typeToColor(entry.courseType)
    val isNow = entry.dayOfWeek == today && currentTime >= entry.startTime && currentTime < entry.endTime
    
    val timeLabel = when {
        entry.dayOfWeek == today -> entry.startTime
        entry.dayOfWeek == (today % 7) + 1 -> stringResource(Res.string.tomorrow_at, entry.startTime)
        else -> {
            val days = listOf(
                stringResource(Res.string.full_day_mon),
                stringResource(Res.string.full_day_tue),
                stringResource(Res.string.full_day_wed),
                stringResource(Res.string.full_day_thu),
                stringResource(Res.string.full_day_fri),
                stringResource(Res.string.full_day_sat),
                stringResource(Res.string.full_day_sun)
            )
            stringResource(Res.string.on_day_at, days[entry.dayOfWeek - 1], entry.startTime)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(
            Brush.linearGradient(
                listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.05f))
            )
        )) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        stringResource(if (isNow) Res.string.now else Res.string.upcoming),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = entry.courseName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp), tint = accentColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${entry.buildingShort} ${entry.room}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                
                if (isNow) {
                    val progress = remember(entry, currentTime) {
                        try {
                            val start = entry.startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                            val end = entry.endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                            val current = currentTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                            ((current - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
                        } catch (e: Exception) {
                            0f
                        }
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.1f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.startTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = entry.endTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Surface(
                        color = accentColor,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = timeLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    icon: ImageVector,
    onSeeAll: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextButton(
                onClick = onSeeAll,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                @Suppress("DEPRECATION")
                Text(stringResource(Res.string.see_all), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CompactTimetableItem(entry: TimetableEntry, onClick: () -> Unit) {
    val accentColor = typeToColor(entry.courseType)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(55.dp)) {
            @Suppress("DEPRECATION")
            Text(entry.startTime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
            Text(entry.endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Box(modifier = Modifier.height(36.dp).width(3.dp).background(accentColor, CircleShape))
        
        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = entry.courseNameShort,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.shortType,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    " • ${entry.buildingShort} ${entry.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactNewsItem(item: NewsHeader, onClick: () -> Unit) {
    val typeColor = item.type.toColor()
    val typeRes = item.type.toStringRes()
    val parsed = remember(item.subject) { parseSubject(item.subject) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = typeColor.copy(alpha = 0.1f),
                contentColor = typeColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (typeRes != null) stringResource(typeRes) else item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (parsed.tag != null) {
                val (tagContainer, tagContent) = getTagColors(parsed.tag)
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = tagContainer,
                    contentColor = tagContent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = parsed.tag,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(Modifier.height(6.6.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (parsed.isGradeUpdate) {
                Icon(Icons.Default.Star, null, Modifier.size(16.6.dp).padding(end = 4.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = parsed.getDisplaySubject(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (parsed.isGradeUpdate) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (parsed.isGradeUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyDashboardState(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
