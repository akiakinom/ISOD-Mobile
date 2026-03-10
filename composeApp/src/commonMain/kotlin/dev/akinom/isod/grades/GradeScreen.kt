package dev.akinom.isod.grades

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
import dev.akinom.isod.data.repository.GradesRepository
import dev.akinom.isod.domain.CourseGrade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val SEMESTER     = "2026L"
private const val USOS_TERM_ID = "2026L"

sealed class GradesState {
    object Loading : GradesState()
    data class Loaded(val grades: List<CourseGrade>) : GradesState()
    data class Error(val message: String) : GradesState()
}

class GradesScreenModel : ScreenModel, KoinComponent {
    private val repo: GradesRepository by inject()

    private val _state = MutableStateFlow<GradesState>(GradesState.Loading)
    val state: StateFlow<GradesState> = _state

    init {
        screenModelScope.launch {
            _state.value = GradesState.Loading
            repo.getGrades(SEMESTER, USOS_TERM_ID)
                .catch { _state.value = GradesState.Error(it.message ?: "Unknown error") }
                .collect { _state.value = GradesState.Loaded(it) }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

class GradesScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { GradesScreenModel() }
        val state by screenModel.state.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "🎓 Grades — $SEMESTER",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            when (val s = state) {
                is GradesState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GradesState.Error -> {
                    DebugRow("❌ ${s.message}")
                }
                is GradesState.Loaded -> {
                    if (s.grades.isEmpty()) {
                        DebugRow("No courses found")
                    } else {
                        s.grades.forEach { course ->
                            CourseCard(course)
                        }
                    }
                }
            }
        }
    }
}

// ── Course card ───────────────────────────────────────────────────────────────

@Composable
private fun CourseCard(course: CourseGrade) {
    val sources = buildString {
        if (course.hasIsod) append("ISOD ")
        if (course.hasUsos) append("USOS")
    }.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${course.courseName} (${course.courseNumber})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${course.ects} ECTS · ${course.passType} · $sources",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Final grade badge
            val grade = course.finalGrade
            if (grade != null) {
                val passColor = when (course.passes) {
                    false -> MaterialTheme.colorScheme.error
                    true  -> MaterialTheme.colorScheme.primary
                    else  -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = grade,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = passColor,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else {
                Text(
                    text = "—",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // Final grade comment
        if (!course.finalGradeComment.isNullOrBlank()) {
            DebugRow("💬 ${course.finalGradeComment}")
        }

        // counts into average
        if (course.countsIntoAverage != null) {
            DebugRow(if (course.countsIntoAverage == true) "📊 Counts into average" else "📊 Does not count into average")
        }

        // Class partial grades
        if (course.classGrades.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            course.classGrades.forEach { cls ->
                ClassGradeRow(cls)
            }
        }
    }
}

// ── Class grade row ───────────────────────────────────────────────────────────

@Composable
private fun ClassGradeRow(cls: dev.akinom.isod.domain.ClassGrade) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "[${cls.classType}]",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!cls.credit.isNullOrBlank()) {
                Text(
                    text = "✅ ${cls.credit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        cls.columns.forEach { col ->
            val value = col.value?.ifBlank { null }
            if (value != null) {
                val weightText = if (col.weight != 1.0 && col.weight != 0.0) " (waga: ${col.weight})" else ""
                DebugRow("  · ${col.name}: $value$weightText")
            }
        }

        if (!cls.summary.isNullOrBlank()) {
            DebugRow("  Σ ${cls.summary}")
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun DebugRow(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 1.dp),
    )
}
