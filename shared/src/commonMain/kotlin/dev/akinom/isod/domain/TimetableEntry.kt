package dev.akinom.isod.domain

import dev.akinom.isod.shared.Res
import dev.akinom.isod.shared.*
import org.jetbrains.compose.resources.getString

data class TimetableEntry(
    val id: String,
    val courseName: String,
    val courseNameShort: String,
    val courseType: String, // "W", "L", "C", "P", "S"
    val dayOfWeek: Int, // 1-7 (Mon-Sun)
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val building: String,
    val buildingShort: String,
    val room: String,
    val lecturerNames: List<String>,
    val source: TimetableSource = TimetableSource.ISOD,
    val lecturerIds: List<Long> = emptyList(),
) {
    val shortType: String get() = when(courseType.uppercase()) {
        "W" -> "WYK"
        "L" -> "LAB"
        "C", "Ć" -> "ĆWI"
        "P" -> "PRO"
        "S" -> "SEM"
        else -> courseType.take(3).uppercase()
    }

    val dedupeKey: String get() = "${courseName}_${dayOfWeek}_${startTime}"
}

enum class TimetableSource {
    ISOD, USOS
}

fun PlanItem.toTimetableEntry() = TimetableEntry(
    id = "${courseName}_${dayOfWeek}_${startTime}",
    courseName = courseName,
    courseNameShort = courseNameShort,
    courseType = when {
        typeOfClasses.contains("W", ignoreCase = true) -> "W"
        typeOfClasses.contains("L", ignoreCase = true) -> "L"
        typeOfClasses.contains("C", ignoreCase = true) -> "C"
        typeOfClasses.contains("P", ignoreCase = true) -> "P"
        typeOfClasses.contains("S", ignoreCase = true) -> "S"
        else -> typeOfClasses.take(1).uppercase()
    },
    dayOfWeek = dayOfWeek,
    startTime = startTime.to24h(),
    endTime = endTime.to24h(),
    building = building,
    buildingShort = buildingShort,
    room = room,
    lecturerNames = teachers,
    source = TimetableSource.ISOD
)

fun UsosActivity.toTimetableEntry(): TimetableEntry {
    val name = courseName?.get("pl") ?: name.get("pl")
    val dow = getDayOfWeekFromDate(startTime)
    return TimetableEntry(
        id = "${name}_${dow}_${startTime}",
        courseName = name,
        courseNameShort = generateShortName(name),
        courseType = when(type.lowercase()) {
            "classgroup", "classgroup2" -> {
                val pl = classtypeName?.get("pl")?.lowercase() ?: ""
                when {
                    pl.contains("wykład") -> "W"
                    pl.contains("laboratorium") -> "L"
                    pl.contains("ćwiczenia") -> "C"
                    pl.contains("projekt") -> "P"
                    pl.contains("seminarium") -> "S"
                    else -> "W"
                }
            }
            "meeting" -> "S"
            "exam" -> "E"
            else -> "W"
        },
        dayOfWeek = dow,
        startTime = startTime.substring(11, 16),
        endTime = endTime.substring(11, 16),
        building = buildingName?.get("pl") ?: "",
        buildingShort = buildingId ?: "",
        room = roomNumber ?: "",
        lecturerNames = lecturers,
        source = TimetableSource.USOS,
        lecturerIds = lecturerIds
    )
}

private fun getDayOfWeekFromDate(dateStr: String): Int {
    val parts = dateStr.take(10).split("-")
    if (parts.size != 3) return 1
    var y = parts[0].toIntOrNull() ?: return 1
    val m = parts[1].toIntOrNull() ?: return 1
    val d = parts[2].toIntOrNull() ?: return 1
    val month = if (m < 3) { y--; m + 12 } else m
    val k = y % 100
    val j = y / 100
    val h = (d + (13 * (month + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
    return ((h + 5) % 7) + 1
}

fun PlanItem.isExcluded(): Boolean {
    val shortName = courseNameShort.uppercase()
    return shortName.startsWith("DSJO") || shortName.startsWith("WF")
}

private fun String.to24h(): String {
    if (!contains("AM", true) && !contains("PM", true)) return this.take(5)
    val isPm   = contains("PM", ignoreCase = true)
    val isAm   = contains("AM", ignoreCase = true)
    val clean  = replace("AM", "", true).replace("PM", "", true).trim()
    val parts  = clean.split(":")
    if (parts.size < 2) return clean
    
    var hour = parts[0].toInt()
    val min  = parts[1]
    
    if (isPm && hour < 12) hour += 12
    if (isAm && hour == 12) hour = 0
    
    return "${hour.toString().padStart(2, '0')}:$min"
}

suspend fun TimetableEntry.getFullTypeName(): String {
    return when(courseType.uppercase()) {
        "W"   -> getString(Res.string.class_lecture)
        "L"   -> getString(Res.string.class_laboratory)
        "C", "Ć" -> getString(Res.string.class_exercises)
        "P"   -> getString(Res.string.class_project)
        "S"   -> getString(Res.string.class_seminar)
        else -> courseType
    }
}
