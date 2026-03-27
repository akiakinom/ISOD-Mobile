package dev.akinom.isod.domain

import kotlinx.datetime.LocalDate

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

    fun getNextClasses(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String, // "HH:mm"
        currentWeek: Int? = null,
        todayDate: LocalDate? = null
    ): List<TimetableEntry> {
        val effectiveDayOfWeek = todayDate?.let { AcademicCalendar.getEffectiveDayOfWeek(it) } ?: todayDayOfWeek
        
        val sortedEntries = entries.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        val currentMinutes = timeToMinutes(currentTime)

        val current = sortedEntries.find {
            it.dayOfWeek == effectiveDayOfWeek && it.startTime <= currentTime && it.endTime > currentTime && it.isActive(currentWeek)
        }

        val futureThisWeek = sortedEntries.filter {
            ((it.dayOfWeek == effectiveDayOfWeek && it.startTime > currentTime) || (it.dayOfWeek > effectiveDayOfWeek)) && it.isActive(currentWeek)
        }
        
        val nextWeek = currentWeek?.plus(1)
        val futureNextWeek = sortedEntries.filter { it.isActive(nextWeek) }
        
        val allFuture = futureThisWeek + futureNextWeek

        val isEndingSoon = current?.let {
            val endMinutes = timeToMinutes(it.endTime)
            endMinutes - currentMinutes <= 20 && allFuture.isNotEmpty()
        } ?: false

        return if (current != null && !isEndingSoon) {
            listOfNotNull(current) + allFuture.take(1)
        } else {
            allFuture.take(2)
        }
    }

    fun getRemainingClasses(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String,
        currentWeek: Int? = null,
        todayDate: LocalDate? = null
    ): List<TimetableEntry> {
        val nextClasses = getNextClasses(entries, todayDayOfWeek, currentTime, currentWeek, todayDate)
        if (nextClasses.isEmpty()) return emptyList()

        val firstNext = nextClasses.first()
        val effectiveDayOfWeek = todayDate?.let { AcademicCalendar.getEffectiveDayOfWeek(it) } ?: todayDayOfWeek

        val isFirstNextToday = firstNext.dayOfWeek == effectiveDayOfWeek
        val targetWeek = if (isFirstNextToday) {
            currentWeek
        } else {
            if (firstNext.dayOfWeek <= effectiveDayOfWeek) (currentWeek?.plus(1) ?: 1) else currentWeek
        }

        return entries.filter { it.dayOfWeek == firstNext.dayOfWeek && it.isActive(targetWeek) }
            .sortedBy { it.startTime }
            .filter { it.startTime >= firstNext.startTime }
    }

    fun getDashboardSchedule(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String,
        currentWeek: Int? = null,
        todayDate: LocalDate? = null
    ): Pair<Boolean, List<TimetableEntry>> {
        val nextClasses = getNextClasses(entries, todayDayOfWeek, currentTime, currentWeek, todayDate)
        val firstNext = nextClasses.firstOrNull()
        
        val effectiveTodayDayOfWeek = todayDate?.let { AcademicCalendar.getEffectiveDayOfWeek(it) } ?: todayDayOfWeek
        
        val todayClasses = entries.filter { it.dayOfWeek == effectiveTodayDayOfWeek && it.isActive(currentWeek) }
            .sortedBy { it.startTime }

        val isAfterLessons = if (todayClasses.isEmpty()) {
            currentTime > "18:00"
        } else {
            currentTime > todayClasses.last().endTime
        }

        val baseList = if (isAfterLessons) {
            val tomorrowDate = todayDate?.let { LocalDate.fromEpochDays(it.toEpochDays() + 1) }
            val effectiveTomorrowDayOfWeek = tomorrowDate?.let { AcademicCalendar.getEffectiveDayOfWeek(it) } ?: ((todayDayOfWeek % 7) + 1)
            val tomorrowWeek = if (todayDayOfWeek == 7) (currentWeek?.plus(1) ?: 1) else currentWeek
            entries.filter { it.dayOfWeek == effectiveTomorrowDayOfWeek && it.isActive(tomorrowWeek) }.sortedBy { it.startTime }
        } else {
            // Include current class if it exists
            val currentClass = todayClasses.find { it.startTime <= currentTime && it.endTime > currentTime }
            if (currentClass != null) {
                todayClasses.filter { it.startTime >= currentClass.startTime }
            } else {
                todayClasses.filter { it.startTime >= (firstNext?.startTime ?: "00:00") }
            }
        }

        return isAfterLessons to baseList
    }
}
