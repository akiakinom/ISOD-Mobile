package dev.akinom.isod.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

object TimetableWidgetLogic {
    fun filterToday(entries: List<TimetableEntry>, todayDayOfWeek: Int): List<TimetableEntry> {
        return entries.filter { it.dayOfWeek == todayDayOfWeek }
            .sortedBy { it.startTime }
    }

    private fun timeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Finds the next 1 or 2 classes starting from the current time.
     * Correctly handles weekend transitions and week cycles.
     */
    fun getNextClasses(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String, // "HH:mm"
        currentWeek: Int? = null,
        todayDate: LocalDate? = null
    ): List<TimetableEntry> {
        val today = todayDate ?: AcademicCalendar.getToday()
        val physicalDayOfWeek = today.dayOfWeek.isoDayNumber
        
        val baseWeek = currentWeek ?: 1
        val baseDay = AcademicCalendar.getEffectiveDayOfWeek(today)
        val baseTime = currentTime

        // If it's the weekend, AcademicCalendar.getCurrentWeek already adjusted to the next week.
        // We start searching from Monday of that adjusted week.
        val (startWeek, startDay, startTime) = if (physicalDayOfWeek > 5) {
            Triple(baseWeek, 1, "00:00")
        } else {
            Triple(baseWeek, baseDay, baseTime)
        }

        // Search through current (possibly adjusted) week and the next one
        for (wOffset in 0..1) {
            val w = startWeek + wOffset
            for (d in 1..7) {
                // Skip days that are already in the past for the first week we check
                if (wOffset == 0 && d < startDay) continue
                
                val t = if (wOffset == 0 && d == startDay) startTime else "00:00"
                
                val dayEntries = entries.filter { it.dayOfWeek == d && it.isActive(w) }
                    .sortedBy { it.startTime }
                
                val currentClass = if (wOffset == 0 && d == startDay) {
                    dayEntries.find { it.startTime <= t && it.endTime > t }
                } else null
                
                val futureClasses = dayEntries.filter { it.startTime > t }
                
                if (currentClass != null || futureClasses.isNotEmpty()) {
                    val isEndingSoon = currentClass?.let {
                        timeToMinutes(it.endTime) - timeToMinutes(t) <= 20 && futureClasses.isNotEmpty()
                    } ?: false

                    return if (currentClass != null && !isEndingSoon) {
                        (listOf(currentClass) + futureClasses.take(1)).take(2)
                    } else {
                        futureClasses.take(2)
                    }
                }
            }
        }

        return emptyList()
    }

    /**
     * Gets all remaining classes for the day of the next scheduled class.
     */
    fun getRemainingClasses(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String,
        currentWeek: Int? = null,
        todayDate: LocalDate? = null
    ): List<TimetableEntry> {
        val nextOnes = getNextClasses(entries, todayDayOfWeek, currentTime, currentWeek, todayDate)
        if (nextOnes.isEmpty()) return emptyList()
        
        val firstNext = nextOnes.first()
        val today = todayDate ?: AcademicCalendar.getToday()
        val physicalDayOfWeek = today.dayOfWeek.isoDayNumber
        val effectiveTodayDay = AcademicCalendar.getEffectiveDayOfWeek(today)
        
        var targetWeek = currentWeek ?: 1
        
        // If it's weekday and we wrapped around to a lower day number, it must be the next week.
        if (physicalDayOfWeek <= 5 && firstNext.dayOfWeek < effectiveTodayDay) {
            targetWeek += 1
        }
        
        return entries.filter { it.dayOfWeek == firstNext.dayOfWeek && it.isActive(targetWeek) }
            .sortedBy { it.startTime }
            .filter { it.startTime >= firstNext.startTime }
    }

    /**
     * Gets the schedule for the dashboard (Today or Tomorrow).
     */
    fun getDashboardSchedule(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String,
        currentWeek: Int? = null,
        todayDate: LocalDate? = null
    ): Pair<Boolean, List<TimetableEntry>> {
        val today = todayDate ?: AcademicCalendar.getToday()
        val physicalDayOfWeek = today.dayOfWeek.isoDayNumber
        val effectiveTodayDay = AcademicCalendar.getEffectiveDayOfWeek(today)
        val week = currentWeek ?: 1
        
        val todayClasses = entries.filter { it.dayOfWeek == effectiveTodayDay && it.isActive(week) }
            .sortedBy { it.startTime }

        val isAfterLessons = if (todayClasses.isEmpty()) {
            currentTime > "18:00" || physicalDayOfWeek > 5
        } else {
            currentTime > todayClasses.last().endTime
        }

        return if (isAfterLessons) {
            val tomorrowDate = LocalDate.fromEpochDays(today.toEpochDays() + 1)
            val tomorrowDay = AcademicCalendar.getEffectiveDayOfWeek(tomorrowDate)
            // Due to AcademicCalendar.getCurrentWeek behavior, 'week' is already correct for tomorrow
            // when it's Saturday, Sunday, or a normal weekday transition.
            val list = entries.filter { it.dayOfWeek == tomorrowDay && it.isActive(week) }.sortedBy { it.startTime }
            true to list
        } else {
            val currentClass = todayClasses.find { it.startTime <= currentTime && it.endTime > currentTime }
            val baseTime = currentClass?.startTime ?: currentTime
            val list = todayClasses.filter { it.startTime >= baseTime }
            false to list
        }
    }
}
