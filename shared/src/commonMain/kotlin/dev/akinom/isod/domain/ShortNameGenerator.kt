package dev.akinom.isod.domain

/**
 * Generates a 3-5 char short name from a full course name.
 * Strategy:
 * 1. Take first letter of each significant word (skip short words like "i", "z", "w")
 * 2. Append trailing digits if present (e.g. "2" in "JIMP2")
 * 3. Trim or pad to 3-5 chars, always uppercase
 *
 * Examples:
 *   "Języki i metody programowania 2" → "JIMP2"
 *   "Bazy danych"                     → "BAZ"
 *   "Podstawy inżynierii oprogramowania" → "PINOP" (too long → "PINO")
 *   "Wychowanie fizyczne"              → "WF"  (but this is filtered out anyway)
 */
fun generateShortName(fullName: String): String {
    val skipWords = setOf("i", "z", "w", "do", "na", "po", "o", "a", "the", "of", "and", "in")

    // Extract trailing number if present (e.g. "... 2" or "...2")
    val trailingNumber = Regex("""(\d+)$""").find(fullName.trim())?.value ?: ""
    val nameWithoutNumber = fullName.trim().removeSuffix(trailingNumber).trim()

    val words = nameWithoutNumber
        .split(" ", "-", "/")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val initials = words
        .filter { it.lowercase() !in skipWords }
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val raw = initials + trailingNumber

    return when {
        raw.length < 3 -> {
            // Too short — take more chars from first significant word
            val firstWord = words.firstOrNull { it.lowercase() !in skipWords } ?: return raw.padEnd(3, 'X')
            (firstWord.take(3 - raw.length).uppercase() + raw.drop(firstWord.take(3 - raw.length).length))
                .take(5)
        }
        raw.length > 5 -> raw.take(4) + trailingNumber.takeLast(1)   // keep trailing digit
        else           -> raw
    }.uppercase()
}