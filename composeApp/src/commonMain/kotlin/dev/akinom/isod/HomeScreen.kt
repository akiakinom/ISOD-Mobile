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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.currentDayOfWeek
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.auth.currentTimeHHmm
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.NewsRepository
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.NewsType
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableWidgetLogic
import dev.akinom.isod.news.NewsDetailScreen
import dev.akinom.isod.news.getTagColors
import dev.akinom.isod.news.parseDateToSortable
import dev.akinom.isod.news.toColor
import dev.akinom.isod.news.toIcon
import dev.akinom.isod.news.toLabel
import dev.akinom.isod.news.typeToColor
import dev.akinom.isod.news.typeToIcon
import dev.akinom.isod.notifications.NewsNotificationChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val SHOW_DEBUG_ACTIONS = false // Flag to hide/show debug button

class HomeScreenModel(val semester: String) : ScreenModel, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()
    private val newsRepo: NewsRepository by inject()
    private val storage: CredentialsStorage by inject()
    private val checker: NewsNotificationChecker by inject()
    private val db: ISODMobileDatabase by inject()

    val isBuchmanp = storage.getIsodUsername() == "buchmanp"

    val weekMonday = currentWeekMonday()
    val currentWeek = AcademicCalendar.getCurrentWeek(semester)

    val timetable: StateFlow<List<TimetableEntry>> =
        timetableRepo.getTimetable(semester)
            .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val news: StateFlow<List<NewsHeader>> =
        newsRepo.getNewsHeaders(semester)
            .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun refresh() {
        val newsJob = screenModelScope.launch { newsRepo.refreshHeaders(semester) }
        newsJob.join()
        delay(300)
    }

    fun debugForceCheckWorker() {
        screenModelScope.launch {
            try {
                checker.check()
            } catch (e: Exception) {
                println("❌ Debug force check failed: ${e.message}")
            }
        }
    }

    fun debugMarkAllUnsent() {
        screenModelScope.launch {
            db.newsQueries.markAllNotificationsUnsent()
            println("✅ Debug: All notifications marked as unsent")
        }
    }
    
    fun markNewsAsRead(id: String) {
        screenModelScope.launch {
            newsRepo.markAsRead(id)
        }
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
        
        val remainingClasses = remember(timetable, today, now, screenModel.currentWeek) {
            TimetableWidgetLogic.getRemainingClasses(
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            if (screenModel.isBuchmanp) "Szalom!" else stringResource(Res.string.hello),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.W700,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (SHOW_DEBUG_ACTIONS) {
                            IconButton(
                                onClick = { screenModel.debugMarkAllUnsent() },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Reset Notifs",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            IconButton(
                                onClick = { screenModel.debugForceCheckWorker() },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.BugReport,
                                    contentDescription = "Debug Check",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
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
                }

                // Next Class Card
                val nextClass = remainingClasses.firstOrNull()
                if (nextClass != null) {
                    NextClassCard(nextClass, today, now) {
                        onMoveToTab(MainTab.Schedule, nextClass.dayOfWeek)
                    }
                }

                // Remaining classes of the day
                remainingClasses.drop(1).forEach { entry ->
                    CompactTimetableItem(entry) {
                        onMoveToTab(MainTab.Schedule, entry.dayOfWeek)
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
                                screenModel.markNewsAsRead(item.id)
                                navigator.push(NewsDetailScreen(item.id))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun NextClassCard(entry: TimetableEntry, today: Int, currentTime: String, onClick: () -> Unit) {
    val accentColor = typeToColor(entry.courseType)
    val typeIcon = typeToIcon(entry.courseType)
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
                    Icon(typeIcon, null, modifier = Modifier.size(16.dp), tint = accentColor)
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
                    @Suppress("DEPRECATION")
                    Text(
                        text = entry.displayLocation,
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
                            @Suppress("DEPRECATION")
                            Text(
                                text = entry.startTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            @Suppress("DEPRECATION")
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
    val typeIcon = typeToIcon(entry.courseType)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            @Suppress("DEPRECATION")
            Text(entry.startTime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accentColor)
            @Suppress("DEPRECATION")
            Text(entry.endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Box(modifier = Modifier.height(36.dp).width(3.dp).background(accentColor, CircleShape))
        
        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = entry.courseName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(typeIcon, null, modifier = Modifier.size(12.dp), tint = accentColor)
                Spacer(Modifier.width(4.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = entry.shortType,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                @Suppress("DEPRECATION")
                Text(
                    " • ${entry.displayLocation}",
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
    val icon = item.type.toIcon()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.type != NewsType.OTHER) {
                Surface(
                    color = typeColor.copy(alpha = 0.1f),
                    contentColor = typeColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(icon, null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = item.type.toLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (item.label.isNotEmpty()) {
                val (tagContainer, tagContent) = getTagColors(item.label)
                if (item.type != NewsType.OTHER) Spacer(Modifier.width(8.dp))
                Surface(
                    color = tagContainer,
                    contentColor = tagContent,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (item.isNew) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }

        if (item.type != NewsType.OTHER || item.label.isNotEmpty())
            Spacer(Modifier.height(6.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            @Suppress("DEPRECATION")
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isNew) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
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
        @Suppress("DEPRECATION")
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
