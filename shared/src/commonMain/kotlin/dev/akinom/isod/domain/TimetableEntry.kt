package dev.akinom.isod.domain

import kotlinx.serialization.Serializable

enum class TimetableSource { ISOD, USOS }

@Serializable
data class TimetableEntry(
    val id: String,
    val source: TimetableSource,
    val courseName: String,
    val courseNameShort: String,
    val courseType: String,        // "W", "L", "C", "WF", etc.
    val startTime: String,         // "yyyy-mm-dd hh:mm:ss" (USOS) or "hh:mm AM/PM" (ISOD)
    val endTime: String,
    val dayOfWeek: Int,            // 1=Mon … 7=Sun
    val date: String?,             // "yyyy-mm-dd" — only available from USOS
    val building: String,
    val room: String,
    val teachers: List<String>,
    val groups: List<String>,
    val frequency: String?,
)

private val ISOD_EXCLUDED_PREFIXES = listOf("WF", "DSJO")

fun PlanItem.isExcluded(): Boolean =
    ISOD_EXCLUDED_PREFIXES.any { courseNameShort.startsWith(it) }

fun PlanItem.toTimetableEntry() = TimetableEntry(
    id             = "isod_$id",
    source         = TimetableSource.ISOD,
    courseName     = courseName,
    courseNameShort = courseNameShort,
    courseType     = typeOfClasses,
    startTime      = startTime,
    endTime        = endTime,
    dayOfWeek      = dayOfWeek,
    date           = null,
    building       = building,
    room           = room,
    teachers       = teachers,
    groups         = groups,
    frequency      = cycle,
)

fun UsosActivity.toTimetableEntry(): TimetableEntry {
    val date      = startTime.take(10)
    val dayOfWeek = date.toDayOfWeek()
    return TimetableEntry(
        id              = "usos_${courseId ?: type}_$startTime",
        source          = TimetableSource.USOS,
        courseName      = courseName?.get() ?: name.get(),
        courseNameShort = courseName?.get() ?: name.get(),
        courseType      = classtypeName?.get("en") ?: type,
        startTime       = startTime,
        endTime         = endTime,
        dayOfWeek       = dayOfWeek,
        date            = date,
        building        = buildingName?.get() ?: "",
        room            = roomNumber ?: "",
        teachers        = emptyList(),
        groups          = groupNumber?.let { listOf("Gr $it") } ?: emptyList(),
        frequency       = frequency,
    )
}

private fun String.toDayOfWeek(): Int {
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