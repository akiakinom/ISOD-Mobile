package dev.akinom.isod.domain

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
        currentTime: String // "HH:mm"
    ): List<TimetableEntry> {
        val sortedEntries = entries.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        val currentMinutes = timeToMinutes(currentTime)

        val current = sortedEntries.find {
            it.dayOfWeek == todayDayOfWeek && it.startTime <= currentTime && it.endTime > currentTime
        }

        val isEndingSoon = current?.let {
            val endMinutes = timeToMinutes(it.endTime)
            endMinutes - currentMinutes <= 45
        } ?: false

        val future = sortedEntries.filter {
            (it.dayOfWeek == todayDayOfWeek && it.startTime > currentTime) || (it.dayOfWeek > todayDayOfWeek)
        }

        return if (current != null && !isEndingSoon) {
            listOfNotNull(current, future.firstOrNull())
        } else {
            future.take(2)
        }
    }

    fun getDashboardSchedule(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String,
        currentWeek: Int?
    ): Pair<Boolean, List<TimetableEntry>> {
        val todayClasses = entries.filter { it.dayOfWeek == todayDayOfWeek && it.isActive(currentWeek) }
            .sortedBy { it.startTime }

        val isAfterLessons = if (todayClasses.isEmpty()) {
            currentTime > "18:00"
        } else {
            currentTime > todayClasses.last().endTime
        }

        return if (isAfterLessons) {
            val tomorrow = (todayDayOfWeek % 7) + 1
            val tomorrowWeek = if (todayDayOfWeek == 7) currentWeek?.plus(1) else currentWeek
            true to entries.filter { it.dayOfWeek == tomorrow && it.isActive(tomorrowWeek) }.sortedBy { it.startTime }
        } else {
            false to todayClasses
        }
    }
}
