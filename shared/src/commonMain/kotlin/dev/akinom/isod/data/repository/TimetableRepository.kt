package dev.akinom.isod.data.repository

import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableSource
import dev.akinom.isod.domain.isExcluded
import dev.akinom.isod.domain.toTimetableEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TimetableRepository(
    private val planRepo: PlanRepository,
    private val usosRepo: UsosRepository,
    private val usosApi: UsosApiClient,
) {
    fun getTimetable(semester: String, weekStart: String): Flow<List<TimetableEntry>> =
        combine(
            planRepo.getPlan(semester),
            usosRepo.getTimetable(weekStart),
        ) { planItems, usosActivities ->

            val weekDates = weekDates(weekStart)

            val isodEntries = planItems
                .filter { !it.isExcluded() }
                .map { it.toTimetableEntry() }

            val usosEntries = usosActivities
                .filter { it.startTime.take(10) in weekDates }
                .map { it.toTimetableEntry() }

            val isodShortNames: Map<String, String> = isodEntries
                .associate { it.dedupeKey to it.courseNameShort }

            val merged = LinkedHashMap<String, TimetableEntry>()
            usosEntries.forEach { entry ->
                val isodShortName = isodShortNames[entry.dedupeKey]
                merged[entry.dedupeKey] = if (isodShortName != null) {
                    entry.copy(courseNameShort = isodShortName)
                } else {
                    entry
                }
            }
            isodEntries.forEach { merged[it.dedupeKey] = it }

            val entries = merged.values
                .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))

            val allLecturerIds = entries
                .filter { it.source == TimetableSource.USOS }
                .flatMap { it.lecturerIds }
                .distinct()

            val lecturerNames = when (val result = usosApi.getLecturerNames(allLecturerIds)) {
                is UsosResult.Success -> result.data
                else                  -> emptyMap()
            }

            entries.map { entry ->
                if (entry.source == TimetableSource.USOS && entry.lecturerIds.isNotEmpty()) {
                    entry.copy(lecturerNames = entry.lecturerIds.mapNotNull { lecturerNames[it] })
                } else {
                    entry
                }
            }
        }
}

private fun weekDates(weekStart: String): Set<String> {
    val parts = weekStart.split("-")
    if (parts.size != 3) return emptySet()
    val y = parts[0].toIntOrNull() ?: return emptySet()
    val m = parts[1].toIntOrNull() ?: return emptySet()
    val d = parts[2].toIntOrNull() ?: return emptySet()
    return (0..6).map { offset -> normalizeDate(y, m, d + offset) }.toSet()
}

private fun normalizeDate(year: Int, month: Int, day: Int): String {
    val dim = daysInMonth(year, month)
    return when {
        day <= dim  -> formatDate(year, month, day)
        month == 12 -> formatDate(year + 1, 1, day - dim)
        else        -> formatDate(year, month + 1, day - dim)
    }
}

private fun formatDate(year: Int, month: Int, day: Int): String {
    val y = year.toString().padStart(4, '0')
    val m = month.toString().padStart(2, '0')
    val d = day.toString().padStart(2, '0')
    return "$y-$m-$d"
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11            -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else                   -> 30
}
