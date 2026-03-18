package dev.akinom.isod.news

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.akinom.isod.domain.NewsType
import dev.akinom.isod.Res
import dev.akinom.isod.*
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.ParsedSubject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NewsType.toColor(): Color {
    return when (this) {
        NewsType.ANNOUNCEMENT -> MaterialTheme.colorScheme.primary
        NewsType.QUIZ -> Color(0xFFE91E63)
        NewsType.IMPORTANT -> MaterialTheme.colorScheme.error
        NewsType.PROJECT_STATUS -> Color(0xFF9C27B0)
        NewsType.PROJECT_GROUP_CHANGE -> Color(0xFF673AB7)
        NewsType.CLASS_ENROLLMENT -> Color(0xFF009688)
        else -> MaterialTheme.colorScheme.outline
    }
}

fun NewsType.toStringRes(): StringResource? {
    return when (this) {
        NewsType.ANNOUNCEMENT -> Res.string.news_type_announcement
        NewsType.QUIZ -> Res.string.news_type_quiz
        NewsType.IMPORTANT -> Res.string.news_type_important
        NewsType.PROJECT_STATUS -> Res.string.news_type_project_status
        NewsType.PROJECT_GROUP_CHANGE -> Res.string.news_type_project_group_change
        NewsType.CLASS_ENROLLMENT -> Res.string.news_type_class_enrollment
        else -> null
    }
}

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
        "WF" -> Icons.Default.DirectionsRun
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

@Composable
fun ParsedSubject.getDisplaySubject(): String {
    val grade = this.gradeValue
    val isUpdate = this.isGradeUpdate
    val subj = this.displaySubject
    return if (isUpdate && grade != null) {
        stringResource(Res.string.new_grade, grade)
    } else {
        subj
    }
}

fun NewsHeader.toIcon(tag: String? = null): ImageVector {
    if (tag?.uppercase() == "DZIEKANAT") return Icons.Default.Campaign
    if (tag?.uppercase() == "WRS") return Icons.Default.Bolt
    
    return when (this.type) {
        NewsType.IMPORTANT -> Icons.Default.Report
        NewsType.QUIZ, NewsType.PROJECT_STATUS -> Icons.Default.Quiz
        NewsType.ANNOUNCEMENT, NewsType.PROJECT_GROUP_CHANGE -> Icons.AutoMirrored.Filled.Comment
        NewsType.CLASS_ENROLLMENT -> Icons.AutoMirrored.Filled.Assignment
        else -> Icons.Default.Notifications
    }
}

/**
 * Parses "DD.MM.YYYY HH:MM:SS" into "YYYYMMDDHHMMSS" for sorting.
 */
fun NewsHeader.parseDateToSortable(): String {
    return try {
        val parts = modifiedDate.split(" ")
        val dateParts = parts[0].split(".")
        val timePart = if (parts.size > 1) parts[1] else "00:00:00"
        
        val day = dateParts[0].padStart(2, '0')
        val month = dateParts[1].padStart(2, '0')
        val year = dateParts[2]
        
        "$year$month$day$timePart"
    } catch (e: Exception) {
        modifiedDate
    }
}
