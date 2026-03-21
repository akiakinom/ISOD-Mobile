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
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.NewsItem
import org.jetbrains.compose.resources.stringResource

@Composable
fun typeToColor(type: String): Color {
    val typeShort = type.take(3).uppercase()
    
    val blue = Color(0xFF2196F3)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF9800)
    val yellow = Color(0xFFFFEB3B)
    val red = Color(0xFFF44336)
    val gray = Color(0xFF9E9E9E)

    return when (typeShort) {
        "WYK" -> blue
        "LAB" -> green
        "ĆWI" -> orange
        "PRO" -> yellow
        "WF" -> red
        "SEM" -> gray
        else -> {
            when(type.take(1).uppercase()) {
                "W" -> blue
                "L" -> green
                "C" -> orange
                "P" -> yellow
                "S" -> gray
                else -> MaterialTheme.colorScheme.outline
            }
        }
    }
}

fun typeToIcon(type: String): ImageVector {
    val typeShort = type.take(3).uppercase()
    return when (typeShort) {
        "WYK" -> Icons.Default.School
        "LAB" -> Icons.Default.Terminal
        "ĆWI" -> Icons.Default.Functions
        "PRO" -> Icons.AutoMirrored.Filled.Assignment
        "WF" -> Icons.AutoMirrored.Filled.DirectionsRun
        "SEM" -> Icons.Default.CoPresent
        else -> {
            when (type.take(1).uppercase()) {
                "W" -> Icons.Default.School
                "L" -> Icons.Default.Terminal
                "C" -> Icons.Default.Functions
                "P" -> Icons.AutoMirrored.Filled.Assignment
                "S" -> Icons.Default.CoPresent
                else -> Icons.AutoMirrored.Filled.Assignment
            }
        }
    }
}

@Composable
fun getTagColors(tag: String): Pair<Color, Color> {
    val projectYellow = Color(0xFFF9A825)
    if (tag == "WRS") {
        return projectYellow.copy(alpha = 0.2f) to projectYellow
    }
    
    // Consistent "random" colors based on tag hash
    val palette = listOf(
        Color(0xFFE3F2FD) to Color(0xFF1565C0), // Blue
        Color(0xFFF3E5F5) to Color(0xFF7B1FA2), // Purple
        Color(0xFFE8F5E9) to Color(0xFF2E7D32), // Green
        Color(0xFFFFF3E0) to Color(0xFFEF6C00), // Orange
        Color(0xFFFCE4EC) to Color(0xFFC2185B), // Pink
        Color(0xFFE0F2F1) to Color(0xFF00695C), // Teal
        Color(0xFFE8EAF6) to Color(0xFF283593), // Indigo
        Color(0xFFFFFDE7) to Color(0xFFFBC02D), // Yellow
    )
    
    val index = (tag.hashCode().let { if (it < 0) -it else it }) % palette.size
    return palette[index]
}

fun NewsType.toIcon(): ImageVector {
    return when (this) {
        NewsType.DEANS_OFFICE -> Icons.Default.Campaign
        NewsType.FACULTY_STUDENT_COUNCIL -> Icons.Default.Bolt
        NewsType.IMPORTANT -> Icons.Default.Report
        NewsType.GRADE -> Icons.Default.Star
        NewsType.CLASS -> Icons.AutoMirrored.Filled.Comment
        NewsType.TIMETABLE_UPDATE -> Icons.Default.Schedule
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
        NewsType.OTHER -> ""
    }
}

fun NewsHeader.parseDateToSortable(): String {
    return date?.toString() ?: ""
}
