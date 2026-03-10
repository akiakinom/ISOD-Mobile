package dev.akinom.isod.domain

import kotlinx.serialization.Serializable

enum class TimetableSource { ISOD, USOS }

@Serializable
data class TimetableEntry(
    val id: String,
    val source: TimetableSource,
    val courseName: String,
    val courseNameShort: String,
    val courseType: String,
    val startTime: String,         // "hh:mm" normalized
    val endTime: String,           // "hh:mm" normalized
    val dayOfWeek: Int,            // 1=Mon … 7=Sun
    val date: String?,             // "yyyy-mm-dd" — USOS only
    val building: String,
    val buildingShort: String,
    val room: String,
    val lecturerIds: List<Long>,   // raw IDs — resolved to names later
    val lecturerNames: List<String>,
    val groups: List<String>,
    val frequency: String?,
) {
    val dedupeKey: String get() = "${dayOfWeek}_${startTime}_${courseName.normalizedForDedup()}"

    fun formatDisplay(): String {
        val lecturer = lecturerNames.joinToString(", ")
        val building = buildingShort.ifBlank { building.abbreviate() }
        val location = listOfNotNull(
            building.ifBlank { null },
            room.ifBlank { null },
        ).joinToString(" ")
        val lecturerPart = if (lecturer.isNotBlank()) " - $lecturer" else ""
        return "$courseName ($courseNameShort) [$courseType] $startTime-$endTime $location$lecturerPart"
    }
}

private fun String.normalizedForDedup(): String =
    lowercase().replace(Regex("[^a-z0-9ąćęłńóśźż]"), "").take(20)

private fun String.abbreviate(): String =
    split(" ")
        .filter { it.length > 1 }
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(4)

private val ISOD_EXCLUDED_PREFIXES = listOf("WF", "DSJO")

fun PlanItem.isExcluded(): Boolean =
    ISOD_EXCLUDED_PREFIXES.any { courseNameShort.startsWith(it) }

fun PlanItem.toTimetableEntry() = TimetableEntry(
    id              = "isod_$id",
    source          = TimetableSource.ISOD,
    courseName      = courseName,
    courseNameShort = courseNameShort,
    courseType = typeOfClasses.toPolishClassType(),
    startTime       = startTime.parseTime(),
    endTime         = endTime.parseTime(),
    dayOfWeek       = dayOfWeek,
    date            = null,
    building        = building,
    buildingShort   = buildingShort,
    room            = room,
    lecturerIds     = emptyList(),
    lecturerNames   = teachers,
    groups          = groups,
    frequency       = cycle,
)

fun UsosActivity.toTimetableEntry(): TimetableEntry {
    val date      = startTime.take(10)
    val dayOfWeek = date.toDayOfWeek()
    val name      = courseName?.get() ?: this.name.get()
    return TimetableEntry(
        id              = "usos_${courseId ?: type}_$startTime",
        source          = TimetableSource.USOS,
        courseName      = name,
        courseNameShort = generateShortName(name),
        courseType = classtypeName?.get("pl")?.toPolishClassType()
            ?: classtypeName?.get("en")?.toPolishClassType()
            ?: type.toPolishClassType(),
        startTime       = startTime.drop(11).take(5),   // "yyyy-mm-dd hh:mm:ss" → "hh:mm"
        endTime         = endTime.drop(11).take(5),
        dayOfWeek       = dayOfWeek,
        date            = date,
        building        = buildingName?.get() ?: "",
        buildingShort   = buildingName?.get()?.abbreviate() ?: "",
        room            = roomNumber ?: "",
        lecturerIds     = lecturerIds,
        lecturerNames   = emptyList(),                  // filled in by repository after name lookup
        groups          = groupNumber?.let { listOf("Gr $it") } ?: emptyList(),
        frequency       = frequency,
    )
}

private fun String.parseTime(): String {
    return try {
        val parts  = trim().split(":")
        var hour   = parts[0].trim().toInt()
        val minute = parts[1].trim()
        val isPm   = contains("PM", ignoreCase = true)
        val isAm   = contains("AM", ignoreCase = true)
        if (isPm && hour != 12) hour += 12
        if (isAm && hour == 12) hour = 0
        
        val h = hour.toString().padStart(2, '0')
        val m = minute.filter { it.isDigit() }.take(2).padStart(2, '0')
        "$h:$m"
    } catch (e: Exception) { this }
}

fun String.toDayOfWeek(): Int {
    val parts = split("-")
    if (parts.size != 3) return 0
    var y = parts[0].toIntOrNull() ?: return 0
    val m = parts[1].toIntOrNull() ?: return 0
    val d = parts[2].toIntOrNull() ?: return 0
    val month = if (m < 3) { y--; m + 12 } else m
    val k = y % 100
    val j = y / 100
    val h = (d + (13 * (month + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
    return ((h + 5) % 7) + 1
}

fun String.toPolishClassType(): String = when (uppercase()) {
    "W"   -> "wykład"
    "C"   -> "ćwiczenia"
    "L"   -> "laboratorium"
    "S"   -> "seminarium"
    "P"   -> "projekt"
    "LECTURE", "LEC"        -> "wykład"
    "TUTORIAL", "TUT"       -> "ćwiczenia"
    "LABORATORY", "LAB"     -> "laboratorium"
    "SEMINAR"               -> "seminarium"
    "PROJECT", "PRO"        -> "projekt"
    else -> this.lowercase()
}