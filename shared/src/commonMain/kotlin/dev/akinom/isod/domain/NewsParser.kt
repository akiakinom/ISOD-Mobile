package dev.akinom.isod.domain

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

fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
