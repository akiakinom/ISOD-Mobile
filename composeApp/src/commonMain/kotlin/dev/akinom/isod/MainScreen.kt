package dev.akinom.isod

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import dev.akinom.isod.news.NewsScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class MainTab(val titleRes: StringResource, val icon: ImageVector) {
    Dashboard(Res.string.tab_home, Icons.Default.Dashboard),
    Schedule(Res.string.tab_schedule, Icons.AutoMirrored.Filled.EventNote),
    Grades(Res.string.tab_grades, Icons.Default.School),
    News(Res.string.tab_news, Icons.Default.Newspaper)
}

data class MainScreen(
    val initialTab: MainTab? = null,
    val initialDayOfWeek: Int? = null
) : Screen {
    @Composable
    override fun Content() {
        val pagerState = rememberPagerState(
            initialPage = MainTab.entries.indexOf(initialTab ?: MainTab.Dashboard),
            pageCount = { MainTab.entries.size }
        )
        val scope = rememberCoroutineScope()
        var scheduleDayOverride by remember { mutableStateOf(initialDayOfWeek) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEachIndexed { index, tab ->
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
                when (MainTab.entries[page]) {
                    MainTab.Dashboard -> HomeScreen(onMoveToTab = { targetTab, day ->
                        scheduleDayOverride = day
                        scope.launch {
                            pagerState.animateScrollToPage(MainTab.entries.indexOf(targetTab))
                        }
                    }).Content()
                    MainTab.Schedule -> ScheduleScreen(initialDayOfWeek = scheduleDayOverride).Content()
                    MainTab.Grades -> GradesScreen().Content()
                    MainTab.News -> NewsScreen().Content()
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
        icon = { Icon(tab.icon, contentDescription = title) },
        label = { Text(title) }
    )
}
