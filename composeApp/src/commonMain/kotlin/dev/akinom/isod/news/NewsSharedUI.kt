package dev.akinom.isod.news

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.akinom.isod.domain.NewsType
import dev.akinom.isod.Res
import dev.akinom.isod.*
import dev.akinom.isod.domain.NewsHeader
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
    val projectYellow = Color(0xFFF9A825)
    val labGreen = Color(0xFF4CAF50)
    val wykLavender = Color(0xFF9575CD)
    val cwiBlue = Color(0xFF2196F3)

    return when (typeShort) {
        "WYK" -> wykLavender
        "LAB" -> labGreen
        "ĆWI" -> cwiBlue
        "PRO" -> projectYellow
        "SEM" -> Color(0xFF9C27B0)
        else -> {
            when(type.take(1).uppercase()) {
                "W" -> wykLavender
                "L" -> labGreen
                "C" -> cwiBlue
                "P" -> projectYellow
                "S" -> Color(0xFF9C27B0)
                else -> MaterialTheme.colorScheme.outline
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

data class ParsedSubject(
    val tag: String?,
    val displaySubject: String,
    val isGradeUpdate: Boolean = false,
    val gradeValue: String? = null
)

fun parseSubject(subject: String): ParsedSubject {
    val acronyms = setOf("WRS")
    
    fun formatTag(tag: String): String {
        val t = tag.trim()
        val upper = t.uppercase()
        if (upper in acronyms) return upper
        if (upper == "DZIEKANAT") return "Dziekanat"
        
        // Keep course short names uppercase
        if (t.length <= 6 && t.all { it.isLetterOrDigit() && (it.isDigit() || it.isUpperCase()) }) return t

        return t.lowercase().replaceFirstChar { it.uppercase() }
    }
    
    fun formatSubject(subj: String): String {
        return subj.trim().replaceFirstChar { it.uppercase() }
    }

    fun cleanGradeValue(value: String): String {
        val v = value.trim()
        // Handle: 'ob' w polu 'obecność' bez komentarza
        val regex = Regex("""'([^']+)' w polu '([^']+)'(.*)""")
        val match = regex.find(v)
        if (match != null) {
            val grade = match.groupValues[1]
            val field = match.groupValues[2]
            return "$grade ($field)"
        }
        return v.removeSurrounding("'")
    }

    // 1. Grade update check: "Zajęcia - JIMP2: Nowa wartość: 5.0"
    val gradeMatch = Regex("""^Zajęcia\s*-\s*([^:]+):\s*Nowa wartość:\s*(.*)$""").find(subject)
    if (gradeMatch != null) {
        val gradeRaw = gradeMatch.groupValues[2].trim()
        val cleanedGrade = cleanGradeValue(gradeRaw)
        return ParsedSubject(
            tag = formatTag(gradeMatch.groupValues[1]),
            displaySubject = "Grade: $cleanedGrade", // Fallback
            isGradeUpdate = true,
            gradeValue = cleanedGrade
        )
    }

    // 2. Class related check: "Zajęcia - JIMP2: Coś tam" or "Ogłoszenie - JIMP2"
    val classMatch = Regex("""^(Zajęcia|Ogłoszenie)\s*-\s*([^:]+)(.*)$""").find(subject)
    if (classMatch != null) {
        val tag = formatTag(classMatch.groupValues[2])
        val rest = classMatch.groupValues[3].trim().removePrefix(":").trim()
        return ParsedSubject(
            tag = tag,
            displaySubject = if (rest.isEmpty()) formatSubject(subject) else formatSubject(rest)
        )
    }

    // 3. Bracket category check: "[DZIEKANAT] Subject"
    val bracketMatch = Regex("""^\[([^\]]+)\]\s*(.*)$""").find(subject)
    if (bracketMatch != null) {
        return ParsedSubject(
            tag = formatTag(bracketMatch.groupValues[1]),
            displaySubject = formatSubject(bracketMatch.groupValues[2])
        )
    }

    return ParsedSubject(null, formatSubject(subject))
}

@Composable
fun ParsedSubject.getDisplaySubject(): String {
    return if (isGradeUpdate && gradeValue != null) {
        stringResource(Res.string.new_grade, gradeValue)
    } else {
        displaySubject
    }
}

fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

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
