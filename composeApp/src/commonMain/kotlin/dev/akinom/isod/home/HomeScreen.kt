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
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.NewsRepository
import dev.akinom.isod.data.repository.PlanRepository
import dev.akinom.isod.data.repository.UsosRepository
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.PlanItem
import dev.akinom.isod.domain.UsosActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val SEMESTER = "2026L"

class HomeScreenModel : ScreenModel, KoinComponent {
    private val planRepo: PlanRepository by inject()
    private val newsRepo: NewsRepository by inject()
    private val usosRepo: UsosRepository by inject()

    val plan: StateFlow<List<PlanItem>> = planRepo.getPlan(SEMESTER)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usos: StateFlow<List<UsosActivity>> = usosRepo.getTimetable(currentWeekMonday())
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val news: StateFlow<List<NewsHeader>> = newsRepo.getNewsHeaders(SEMESTER)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}


class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val plan        by screenModel.plan.collectAsState()
        val news        by screenModel.news.collectAsState()
        val usos        by screenModel.usos.collectAsState()

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

            DebugSection(
                title  = "📅 Plan (${plan.size} items)",
                status = if (plan.isEmpty()) "⏳ loading..." else "✅ loaded",
            ) {
                plan.take(5).forEach { item ->
                    DebugRow("${item.dayOfWeek} ${item.startTime}–${item.endTime}  ${item.courseNameShort}  ${item.room}")
                }
                if (plan.size > 5) DebugRow("… and ${plan.size - 5} more")
            }

            DebugSection(
                title  = "📅 USOS Plan (${plan.size} items)",
                status = if (usos.isEmpty()) "⏳ loading..." else "✅ loaded",
            ) {
                usos.take(20).forEach { item ->
                    DebugRow("${item.startTime}–${item.endTime}  ${item.courseName}  ${item.roomNumber}")
                }
                if (plan.size > 20) DebugRow("… and ${plan.size - 5} more")
            }

            DebugSection(
                title  = "📰 News (${news.size} items)",
                status = if (news.isEmpty()) "⏳ loading..." else "✅ loaded",
            ) {
                news.take(5).forEach { item ->
                    DebugRow("[${item.type.name}] ${item.subject.take(50)}")
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
            Text(text = title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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