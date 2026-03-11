package dev.akinom.isod.domain

object TimetableWidgetLogic {
    fun filterToday(entries: List<TimetableEntry>, todayDayOfWeek: Int): List<TimetableEntry> {
        return entries.filter { it.dayOfWeek == todayDayOfWeek }
            .sortedBy { it.startTime }
    }

    fun getNextClasses(
        entries: List<TimetableEntry>,
        todayDayOfWeek: Int,
        currentTime: String // "HH:mm"
    ): List<TimetableEntry> {
        val sortedEntries = entries.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        
        val current = sortedEntries.find { 
            it.dayOfWeek == todayDayOfWeek && it.startTime <= currentTime && it.endTime > currentTime 
        }
        
        val future = sortedEntries.filter { 
            (it.dayOfWeek == todayDayOfWeek && it.startTime > currentTime) || (it.dayOfWeek > todayDayOfWeek)
        }

        return if (current != null) {
            listOfNotNull(current, future.firstOrNull())
        } else {
            future.take(2)
        }
    }
}
