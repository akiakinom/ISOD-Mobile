package dev.akinom.isod.domain

import dev.akinom.isod.shared.Res
import dev.akinom.isod.shared.*
import org.jetbrains.compose.resources.getString

data class TimetableEntry(
    val id: String,
    val courseName: String,
    val courseNameShort: String,
    val courseType: ClassType,
    val dayOfWeek: Int, // 1-7 (Mon-Sun)
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val building: String,
    val buildingShort: String,
    val room: String,
    val lecturerNames: List<String>,
    val source: TimetableSource = TimetableSource.ISOD,
    val lecturerIds: List<Long> = emptyList(),
    val cycle: String = "SEM",
    val userCycleOverride: String? = null
) {
    val shortType: String get() = when(courseType) {
        ClassType.LECTURE -> "WYK"
        ClassType.LABORATORY -> "LAB"
        ClassType.EXERCISES -> "ĆWI"
        ClassType.PROJECT -> "PRO"
        ClassType.SEMINAR -> "SEM"
        ClassType.PHYSICAL_EDUCATION -> "WF"
        ClassType.OTHER -> "???"
    }

    val displayLocation: String get() {
        val b = buildingShort.trim()
        val r = room.trim()
        return when {
            b.isEmpty() -> r
            r.isEmpty() -> b
            b.uppercase() == r.uppercase() -> b
            else -> "$b $r"
        }
    }

    val dedupeKey: String get() = "${courseName}_${dayOfWeek}_${startTime}"

    fun isActive(currentWeek: Int?): Boolean {
        val activeCycle = userCycleOverride ?: cycle
        if (activeCycle.uppercase() == "NONE") return false
        if (currentWeek == null) return true

        val upperCycle = activeCycle.uppercase()
        
        // Handle custom ranges/exceptions
        if (upperCycle.contains("!") || upperCycle.startsWith("W:")) {
            val (base, exception) = if (upperCycle.startsWith("W:")) {
                "NONE" to upperCycle.substring(2) // NONE base because we'll treat W: as inclusion
            } else {
                val parts = upperCycle.split("!")
                parts[0].ifEmpty { "SEM" } to parts.getOrElse(1) { "" }
            }

            val isInBase = if (upperCycle.startsWith("W:")) {
                false // We use the range as inclusion
            } else {
                when (base) {
                    "SEM" -> true
                    "1PS" -> currentWeek <= 8
                    "2PS" -> currentWeek >= 8
                    "PA" -> currentWeek % 2 == 0
                    "NP" -> currentWeek % 2 != 0
                    else -> true
                }
            }

            val inRanges = exception.split(",").any { range ->
                val r = range.trim()
                if (r.isEmpty()) return@any false
                if (r.contains("-")) {
                    val rangeParts = r.split("-")
                    val start = rangeParts[0].toIntOrNull() ?: 0
                    val end = rangeParts.getOrNull(1)?.toIntOrNull() ?: 99
                    currentWeek in start..end
                } else {
                    r.toIntOrNull() == currentWeek
                }
            }

            return if (upperCycle.startsWith("W:")) inRanges else (isInBase && !inRanges)
        }

        return when (upperCycle) {
            "SEM" -> true
            "1PS" -> currentWeek <= 8
            "2PS" -> currentWeek >= 8
            "PA" -> currentWeek % 2 == 0
            "NP" -> currentWeek % 2 != 0
            else -> true
        }
    }
}

enum class TimetableSource {
    ISOD, USOS
}

fun PlanItem.toTimetableEntry() = TimetableEntry(
    id = "${courseName}_${dayOfWeek}_${startTime}",
    courseName = courseName,
    courseNameShort = courseNameShort,
    courseType = typeOfClasses,
    dayOfWeek = dayOfWeek,
    startTime = startTime.to24h(),
    endTime = endTime.to24h(),
    building = building,
    buildingShort = buildingShort,
    room = room,
    lecturerNames = teachers,
    source = TimetableSource.ISOD,
    cycle = cycleShort.ifBlank { "SEM" }
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
                    pl.contains("wykład") -> ClassType.LECTURE
                    pl.contains("laboratorium") -> ClassType.LABORATORY
                    pl.contains("ćwiczenia") -> ClassType.EXERCISES
                    pl.contains("projekt") -> ClassType.PROJECT
                    pl.contains("seminarium") -> ClassType.SEMINAR
                    pl.contains("wychowanie fizyczne") || pl.contains("wf") -> ClassType.PHYSICAL_EDUCATION
                    else -> ClassType.LECTURE
                }
            }
            "meeting" -> ClassType.SEMINAR
            "exam" -> ClassType.OTHER
            else -> ClassType.LECTURE
        },
        dayOfWeek = dow,
        startTime = startTime.substring(11, 16),
        endTime = endTime.substring(11, 16),
        building = buildingName?.get("pl") ?: "",
        buildingShort = buildingId ?: "",
        room = roomNumber ?: "",
        lecturerNames = lecturers,
        source = TimetableSource.USOS,
        lecturerIds = lecturerIds,
        cycle = frequency ?: "SEM"
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
    val loc = "$building $buildingShort $room".uppercase()
    return shortName.startsWith("DSJO") || loc.contains("RIV")
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
    return when(courseType) {
        ClassType.LECTURE -> getString(Res.string.class_lecture)
        ClassType.LABORATORY -> getString(Res.string.class_laboratory)
        ClassType.EXERCISES -> getString(Res.string.class_exercises)
        ClassType.PROJECT -> getString(Res.string.class_project)
        ClassType.SEMINAR -> getString(Res.string.class_seminar)
        ClassType.PHYSICAL_EDUCATION -> "WF"
        ClassType.OTHER -> "Inne"
    }
}
