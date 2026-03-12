package dev.akinom.isod

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class MainTab(val titleRes: StringResource, val icon: ImageVector) {
    Dashboard(Res.string.tab_home, Icons.Default.Dashboard),
    Schedule(Res.string.tab_schedule, Icons.Default.EventNote),
    Grades(Res.string.tab_grades, Icons.Default.School),
    News(Res.string.tab_news, Icons.Default.Newspaper)
}

data class MainScreen(val initialTab: MainTab? = null) : Screen {
    @Composable
    override fun Content() {
        var selectedTab by remember { mutableStateOf(initialTab ?: MainTab.Dashboard) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        TabItem(
                            tab = tab,
                            isSelected = selectedTab == tab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = paddingValues.calculateBottomPadding())) {
                Crossfade(targetState = selectedTab, modifier = Modifier.fillMaxSize()) { tab ->
                    when (tab) {
                        MainTab.Dashboard -> HomeScreen(onMoveToTab = { selectedTab = it }).Content()
                        MainTab.Schedule -> ScheduleScreen().Content()
                        MainTab.Grades -> GradesScreen().Content()
                        MainTab.News -> NewsScreen().Content()
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
        icon = { Icon(tab.icon, contentDescription = title) },
        label = { Text(title) }
    )
}
