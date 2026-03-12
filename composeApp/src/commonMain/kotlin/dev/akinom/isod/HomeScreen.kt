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
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableWidgetLogic
import dev.akinom.isod.news.NewsDetailScreen
import dev.akinom.isod.news.capitalize
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
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeScreenModel(val semester: String) : ScreenModel, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()
    private val newsRepo: NewsRepository           by inject()

    val weekMonday = currentWeekMonday()

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
    val onMoveToTab: (MainTab) -> Unit = {}
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
        
        val todaysClasses = remember(timetable, today) {
            timetable.filter { it.dayOfWeek == today }.sortedBy { it.startTime }
        }

        val nextClasses = remember(timetable, today, now) {
            TimetableWidgetLogic.getNextClasses(timetable, today, now)
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
                    NextClassCard(nextClass, today) {
                        onMoveToTab(MainTab.Schedule)
                    }
                }

                // Today's Timeline Summary
                DashboardSection(
                    title = stringResource(Res.string.todays_timeline),
                    icon = Icons.Default.CalendarToday,
                    onSeeAll = { onMoveToTab(MainTab.Schedule) }
                ) {
                    if (todaysClasses.isEmpty()) {
                        EmptyDashboardState(stringResource(Res.string.no_classes_today))
                    } else {
                        todaysClasses.take(3).forEach { entry ->
                            CompactTimetableItem(entry) {
                                onMoveToTab(MainTab.Schedule)
                            }
                        }
                    }
                }

                // Recent Announcements
                DashboardSection(
                    title = stringResource(Res.string.announcements),
                    icon = Icons.Default.Campaign,
                    onSeeAll = { onMoveToTab(MainTab.News) }
                ) {
                    if (news.isEmpty()) {
                        EmptyDashboardState(stringResource(Res.string.all_caught_up))
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
private fun NextClassCard(entry: TimetableEntry, today: Int, onClick: () -> Unit) {
    val accentColor = typeToColor(entry.courseType)
    
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
        Box(modifier = Modifier.background(
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
                    Text(
                        stringResource(Res.string.upcoming),
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
                
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = timeLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
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
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onSeeAll,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
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
            Text(entry.startTime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
            Text(entry.endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Box(modifier = Modifier.height(36.dp).width(3.dp).background(accentColor, CircleShape))
        
        Spacer(Modifier.width(12.dp))

        Column {
            Text(entry.courseNameShort, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = typeColor.copy(alpha = 0.1f),
                contentColor = typeColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (typeRes != null) stringResource(typeRes) else item.type.name.replace("_", " ").lowercase().capitalize(),
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
private fun EmptyDashboardState(message: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
