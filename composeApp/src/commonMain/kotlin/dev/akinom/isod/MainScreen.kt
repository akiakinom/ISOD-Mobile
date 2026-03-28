package dev.akinom.isod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.akinom.isod.news.NewsScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class MainTab(
    val titleRes: StringResource,
    val icon: ImageVector? = null,
    val drawable: DrawableResource? = null
) {
    Dashboard(Res.string.tab_home, icon = Icons.Default.Dashboard),
    Schedule(Res.string.tab_schedule, icon = Icons.AutoMirrored.Filled.EventNote),
    Grades(Res.string.tab_grades, icon = Icons.Default.School),
    News(Res.string.tab_news, icon = Icons.Default.Newspaper),
    Skibidi(Res.string.tab_skibidi_toilet, drawable = Res.drawable.meow)
}

data class MainScreen(
    val initialTab: MainTab? = null,
    val initialDayOfWeek: Int? = null
) : Screen {
    @Composable
    override fun Content() {
        val tabs = remember {
            val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val isAprilFools = (now.month.number == 4 && now.day == 1)
            MainTab.entries.filter { it != MainTab.Skibidi || isAprilFools }
        }

        val pagerState = rememberPagerState(
            initialPage = tabs.indexOf(initialTab ?: MainTab.Dashboard).let { if (it == -1) 0 else it },
            pageCount = { tabs.size }
        )
        val scope = rememberCoroutineScope()
        var scheduleDayOverride by remember { mutableStateOf(initialDayOfWeek) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        TabItem(
                            tab = tab,
                            isSelected = pagerState.currentPage == index,
                            onClick = { 
                                if (tab == MainTab.Schedule) {
                                    scheduleDayOverride = null // Reset override when manually clicking tab
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                beyondViewportPageCount = 1
            ) { page ->
                when (val tab = tabs[page]) {
                    MainTab.Dashboard -> HomeScreen(onMoveToTab = { targetTab, day ->
                        scheduleDayOverride = day
                        scope.launch {
                            val targetIndex = tabs.indexOf(targetTab)
                            if (targetIndex != -1) {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                    }).Content()
                    MainTab.Schedule -> ScheduleScreen(initialDayOfWeek = scheduleDayOverride).Content()
                    MainTab.Grades -> GradesScreen().Content()
                    MainTab.Exams -> ExamsScreen().Content()
                    MainTab.News -> NewsScreen().Content()
                    MainTab.Skibidi -> {
                        val isVisible = pagerState.currentPage == page
                        SkibidiToilet(isVisible = isVisible).Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val title = stringResource(tab.titleRes)
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            if (tab.icon != null) {
                Icon(tab.icon, contentDescription = title)
            } else if (tab.drawable != null) {
                Icon(
                    painter = painterResource(tab.drawable),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        label = { Text(title) }
    )
}
