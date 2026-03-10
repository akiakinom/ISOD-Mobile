package dev.akinom.isod.data.repository

import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.isExcluded
import dev.akinom.isod.domain.toTimetableEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TimetableRepository(
    private val planRepo: PlanRepository,
    private val usosRepo: UsosRepository,
) {
    /**
     * Returns a merged, sorted list of timetable entries for the given week.
     *
     * - ISOD: filters out WF and DSJO prefixes, uses dayOfWeek for ordering
     * - USOS: filters to only activities whose date falls within [weekStart, weekStart+6]
     * - Combined: sorted by dayOfWeek then startTime
     *
     * @param semester  ISOD semester code e.g. "2026L"
     * @param weekStart Monday of the desired week, "yyyy-mm-dd"
     */
    fun getTimetable(semester: String, weekStart: String): Flow<List<TimetableEntry>> =
        combine(
            planRepo.getPlan(semester),
            usosRepo.getTimetable(weekStart),
        ) { planItems, usosActivities ->
            val isodEntries = planItems
                .filter { !it.isExcluded() }
                .map { it.toTimetableEntry() }

            val weekDates = weekDates(weekStart)
            val usosEntries = usosActivities
                .filter { it.startTime.take(10) in weekDates }
                .map { it.toTimetableEntry() }

            (isodEntries + usosEntries)
                .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        }
}

private fun weekDates(weekStart: String): Set<String> {
    val parts = weekStart.split("-")
    if (parts.size != 3) return emptySet()
    val y = parts[0].toIntOrNull() ?: return emptySet()
    val m = parts[1].toIntOrNull() ?: return emptySet()
    val d = parts[2].toIntOrNull() ?: return emptySet()
    return (0..6).map { offset ->
        val totalDays = d + offset
        normalizeDate(y, m, totalDays)
    }.toSet()
}

private fun normalizeDate(year: Int, month: Int, day: Int): String {
    val daysInMonth = daysInMonth(year, month)
    return when {
        day <= daysInMonth -> "%04d-%02d-%02d".format(year, month, day)
        month == 12        -> "%04d-%02d-%02d".format(year + 1, 1, day - daysInMonth)
        else               -> "%04d-%02d-%02d".format(year, month + 1, day - daysInMonth)
    }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11            -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else                   -> 30
}