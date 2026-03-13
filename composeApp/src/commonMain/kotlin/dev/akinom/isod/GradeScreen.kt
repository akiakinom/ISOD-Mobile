package dev.akinom.isod

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.data.repository.GradesRepository
import dev.akinom.isod.domain.ClassGrade
import dev.akinom.isod.domain.CourseGrade
import dev.akinom.isod.news.typeToColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class GradesState {
    data object Loading : GradesState()
    data class Loaded(val grades: List<CourseGrade>) : GradesState()
    data class Error(val message: String) : GradesState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GradesScreenModel : ScreenModel, KoinComponent {
    private val repo: GradesRepository by inject()

    private val _semester = MutableStateFlow(currentSemester())
    val semester: StateFlow<String> = _semester

    private val _state = MutableStateFlow<GradesState>(GradesState.Loading)
    val state: StateFlow<GradesState> = _state

    private val _loadingCourseDetails = MutableStateFlow<Set<String>>(emptySet())
    val loadingCourseDetails: StateFlow<Set<String>> = _loadingCourseDetails

    init {
        observeGrades()
    }

    private fun observeGrades() {
        screenModelScope.launch {
            _semester.flatMapLatest { sem ->
                _state.value = GradesState.Loading
                repo.getGrades(sem, sem)
            }.catch { 
                _state.value = GradesState.Error(it.message ?: "Unknown error") 
            }.collect { 
                _state.value = GradesState.Loaded(it) 
            }
        }
    }

    fun changeSemester(newSemester: String) {
        _semester.value = newSemester
    }

    fun loadCourseDetails(course: CourseGrade) {
        if (course.classGrades.any { it.columns.isNotEmpty() }) return
        
        screenModelScope.launch {
            _loadingCourseDetails.update { it + course.courseId }
            try {
                repo.refreshCourseDetails(course, _semester.value)
            } finally {
                _loadingCourseDetails.update { it - course.courseId }
            }
        }
    }

    val availableSemesters: List<String> by lazy {
        val current = currentSemester()
        val year = current.substring(0, 4).toInt()
        val type = current.substring(4)
        
        val list = mutableListOf<String>()
        var currYear = year
        var currType = type
        
        repeat(6) {
            list.add("$currYear$currType")
            if (currType == "L") {
                currType = "Z"
                currYear -= 1
            } else {
                currType = "L"
            }
        }
        list
    }
}

class GradesScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { GradesScreenModel() }
        val state by screenModel.state.collectAsState()
        val selectedSemester by screenModel.semester.collectAsState()
        val loadingDetails by screenModel.loadingCourseDetails.collectAsState()

        var showSemesterPicker by remember { mutableStateOf(false) }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Box {
                            Column(
                                modifier = Modifier.clickable { showSemesterPicker = true },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(Res.string.grades_title), fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        selectedSemester,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (showSemesterPicker) {
                                SemesterDropdown(
                                    semesters = screenModel.availableSemesters,
                                    onSelected = {
                                        screenModel.changeSemester(it)
                                        showSemesterPicker = false
                                    },
                                    onDismiss = { showSemesterPicker = false }
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when (val s = state) {
                    is GradesState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        }
                    }
                    is GradesState.Error -> ErrorMessage(s.message)
                    is GradesState.Loaded -> {
                        if (s.grades.isEmpty()) {
                            EmptyState(selectedSemester)
                        } else {
                            GradesList(s.grades, loadingDetails) { course ->
                                screenModel.loadCourseDetails(course)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SemesterDropdown(
    semesters: List<String>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        semesters.forEach { sem ->
            DropdownMenuItem(
                text = { Text(sem) },
                onClick = { onSelected(sem) }
            )
        }
    }
}

@Composable
private fun GradesList(
    grades: List<CourseGrade>,
    loadingDetails: Set<String>,
    onCourseClick: (CourseGrade) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(grades, key = { it.courseId }) { course ->
            CourseGradeCard(
                course = course,
                isLoadingDetails = loadingDetails.contains(course.courseId),
                onClick = { onCourseClick(course) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseGradeCard(
    course: CourseGrade,
    isLoadingDetails: Boolean,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = {
            expanded = !expanded
            if (expanded) onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp, 
            if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    @Suppress("DEPRECATION")
                    Text(
                        text = "${course.courseNumber} • ${course.ects} ECTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                GradeSummary(course)

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (isLoadingDetails) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        if (course.classGrades.isEmpty()) {
                            Text(
                                stringResource(Res.string.no_detailed_grades),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            course.classGrades.forEach { cls ->
                                ClassDetailSection(cls)
                                if (cls != course.classGrades.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }

                        course.finalGradeComment?.let { comment ->
                            if (comment.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = comment,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeSummary(course: CourseGrade) {
    val grade = course.displayFinalGrade
    if (grade != null) {
        val color = when (course.passes) {
            false -> MaterialTheme.colorScheme.error
            true  -> MaterialTheme.colorScheme.primary
            else  -> MaterialTheme.colorScheme.outline
        }

        Surface(
            color = color.copy(alpha = 0.1f),
            contentColor = color,
            shape = CircleShape,
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Text(
                text = grade,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassDetailSection(cls: ClassGrade) {
    val accentColor = typeToColor(cls.classType)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val label = when(cls.classType.take(1).uppercase()) {
                        "W" -> "W"
                        "L" -> "L"
                        "C" -> "Ć"
                        "P" -> "P"
                        "S" -> "S"
                        else -> cls.classType.take(1).uppercase()
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when(cls.classType.uppercase()) {
                        "W" -> stringResource(Res.string.class_lecture)
                        "L" -> stringResource(Res.string.class_laboratory)
                        "C" -> stringResource(Res.string.class_exercises)
                        "P" -> stringResource(Res.string.class_project)
                        "S" -> stringResource(Res.string.class_seminar)
                        else -> cls.classType
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            cls.displayCredit?.let {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (cls.columns.isEmpty()) {
            Text(
                stringResource(Res.string.no_partial_grades),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 32.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cls.columns.forEach { col ->
                    val value = col.value?.ifBlank { null }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = col.name ?: stringResource(Res.string.grade_value_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value ?: "∅",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (value == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(semester: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.no_grades_for_semester, semester), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text("❌ $message", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
    }
}
