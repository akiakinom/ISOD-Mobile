package dev.akinom.isod.news

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.akinom.isod.domain.NewsType
import dev.akinom.isod.Res
import dev.akinom.isod.*
import dev.akinom.isod.domain.ClassType
import dev.akinom.isod.domain.NewsHeader
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.time.Clock

@Composable
fun typeToColor(type: ClassType): Color {
    val blue = Color(0xFF2196F3)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF9800)
    val yellow = Color(0xFFFF0051)
    val red = Color(0xFFF44336)
    val gray = Color(0xFF9E9E9E)

    return when (type) {
        ClassType.LECTURE -> blue
        ClassType.LABORATORY -> green
        ClassType.EXERCISES -> orange
        ClassType.PROJECT -> yellow
        ClassType.PHYSICAL_EDUCATION -> red
        ClassType.SEMINAR -> gray
        ClassType.OTHER -> MaterialTheme.colorScheme.outline
    }
}

fun typeToIcon(type: ClassType): ImageVector {
    return when (type) {
        ClassType.LECTURE -> Icons.Default.School
        ClassType.LABORATORY -> Icons.Default.Terminal
        ClassType.EXERCISES -> Icons.Default.Functions
        ClassType.PROJECT -> Icons.AutoMirrored.Filled.Assignment
        ClassType.PHYSICAL_EDUCATION -> Icons.AutoMirrored.Filled.DirectionsRun
        ClassType.SEMINAR -> Icons.Default.CoPresent
        ClassType.OTHER -> Icons.AutoMirrored.Filled.Assignment
    }
}

@Composable
fun ClassType.toLabel(): String {
    return when (this) {
        ClassType.LECTURE -> stringResource(Res.string.class_lecture)
        ClassType.LABORATORY -> stringResource(Res.string.class_laboratory)
        ClassType.EXERCISES -> stringResource(Res.string.class_exercises)
        ClassType.PROJECT -> stringResource(Res.string.class_project)
        ClassType.SEMINAR -> stringResource(Res.string.class_seminar)
        ClassType.PHYSICAL_EDUCATION -> "WF"
        ClassType.OTHER -> "Inne"
    }
}

fun ClassType.toShortLabel(): String {
    return when (this) {
        ClassType.LECTURE -> "W"
        ClassType.LABORATORY -> "L"
        ClassType.EXERCISES -> "Ć"
        ClassType.PROJECT -> "P"
        ClassType.SEMINAR -> "S"
        ClassType.PHYSICAL_EDUCATION -> "WF"
        ClassType.OTHER -> "?"
    }
}

@Composable
fun getTagColors(tag: String): Pair<Color, Color> {
    if (tag == "WRS") {
        return MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) to MaterialTheme.colorScheme.tertiary
    }

    val palettes = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant,
    )
    
    val index = (tag.hashCode().let { if (it < 0) -it else it }) % palettes.size
    return palettes[index]
}

@Composable
fun NewsType.toIcon(): ImageVector {
    return when (this) {
        NewsType.DEANS_OFFICE -> Icons.Default.Campaign
        NewsType.FACULTY_STUDENT_COUNCIL -> vectorResource(Res.drawable.wrs_logo)
        NewsType.IMPORTANT -> Icons.Default.Report
        NewsType.GRADE -> Icons.Default.Star
        NewsType.CLASS -> Icons.AutoMirrored.Filled.Comment
        NewsType.TIMETABLE_UPDATE -> Icons.Default.Schedule
        NewsType.EXAM -> Icons.Default.HistoryEdu
        NewsType.DEADLINE -> Icons.Default.Timer
        NewsType.STUDENT_EVENT -> Icons.Default.Event
        else -> Icons.Default.Notifications
    }
}

fun NewsType.toColor(): Color {
    return when (this) {
        NewsType.IMPORTANT -> Color(0xFFF44336)
        NewsType.GRADE -> Color(0xFF4CAF50)
        NewsType.CLASS -> Color(0xFF2196F3)
        NewsType.DEANS_OFFICE -> Color(0xFFFF9800)
        NewsType.FACULTY_STUDENT_COUNCIL -> Color(0xFFFFC107)
        NewsType.TIMETABLE_UPDATE -> Color(0xFF9C27B0)
        NewsType.EXAM -> Color(0xFFE91E63)
        NewsType.DEADLINE -> Color(0xFF795548)
        NewsType.STUDENT_EVENT -> Color(0xFF00BCD4)
        NewsType.OTHER -> Color(0xFF9E9E9E)
    }
}

@Composable
fun NewsType.toLabel(): String {
    return when (this) {
        NewsType.IMPORTANT -> stringResource(Res.string.news_type_important)
        NewsType.GRADE -> stringResource(Res.string.news_type_grade)
        NewsType.CLASS -> stringResource(Res.string.news_type_class)
        NewsType.DEANS_OFFICE -> stringResource(Res.string.news_type_deans_office)
        NewsType.FACULTY_STUDENT_COUNCIL -> stringResource(Res.string.news_type_wrs)
        NewsType.TIMETABLE_UPDATE -> stringResource(Res.string.news_type_timetable)
        NewsType.EXAM -> stringResource(Res.string.news_type_exam)
        NewsType.DEADLINE -> stringResource(Res.string.news_type_deadline)
        NewsType.STUDENT_EVENT -> stringResource(Res.string.news_type_student_event)
        NewsType.OTHER -> ""
    }
}

fun NewsHeader.parseDateToSortable(): String {
    return date?.toString() ?: ""
}

@Composable
fun LocalDateTime.formatFriendly(): String {
    val datePart = "${day.toString().padStart(2, '0')}.${month.number.toString().padStart(2, '0')}.${year}"
    val timePart = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    
    return stringResource(Res.string.date_at_format, datePart, timePart)
}

@Composable
fun LocalDate.formatHeader(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (this) {
        now -> stringResource(Res.string.today)
        now.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.yesterday)
        LocalDate(1970, 1, 1) -> stringResource(Res.string.other_date)
        else -> {
            val datePart = "${day.toString().padStart(2, '0')}.${month.number.toString().padStart(2, '0')}.${year}"
            datePart
        }
    }
}

fun LocalDateTime.formatTimeOnly(): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
