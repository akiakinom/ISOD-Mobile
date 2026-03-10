package dev.akinom.isod.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.NewsRepository
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableSource
import dev.akinom.isod.grades.GradesScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val SEMESTER = "2026L"

class HomeScreenModel : ScreenModel, KoinComponent {
    private val timetableRepo: TimetableRepository by inject()
    private val newsRepo: NewsRepository           by inject()

    val weekMonday = currentWeekMonday()

    val timetable: StateFlow<List<TimetableEntry>> =
        timetableRepo.getTimetable(SEMESTER, weekMonday)
            .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val news: StateFlow<List<NewsHeader>> =
        newsRepo.getNewsHeaders(SEMESTER)
            .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val timetable   by screenModel.timetable.collectAsState()
        val news        by screenModel.news.collectAsState()
        val navigator   = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "🛠 Debug — $SEMESTER",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            val isodCount = timetable.count { it.source == TimetableSource.ISOD }
            val usosCount = timetable.count { it.source == TimetableSource.USOS }
            DebugSection(
                title  = "📅 Timetable — week of ${screenModel.weekMonday} (${timetable.size})",
                status = if (timetable.isEmpty()) "⏳" else "✅ ISOD:$isodCount USOS:$usosCount",
            ) {
                Button(
                    onClick = { navigator.push(GradesScreen()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("View Grades 🎓", fontSize = 12.sp)
                }

                Spacer(Modifier.height(4.dp))

                timetable.forEach { entry ->
                    val source = if (entry.source == TimetableSource.USOS) "🔵" else "⚪"
                    DebugRow("$source  Day${entry.dayOfWeek}  ${entry.formatDisplay()}")
                }
                if (timetable.isEmpty()) DebugRow("No entries")
            }

            DebugSection(
                title  = "📰 News (${news.size})",
                status = if (news.isEmpty()) "⏳" else "✅",
            ) {
                news.take(5).forEach { item ->
                    DebugRow("[${item.type.name}] ${item.subject.take(60)}")
                }
                if (news.size > 5) DebugRow("… and ${news.size - 5} more")
            }
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    status: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Text(text = status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        content()
    }
}

@Composable
private fun DebugRow(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
