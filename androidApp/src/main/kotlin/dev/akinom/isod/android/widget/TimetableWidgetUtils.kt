package dev.akinom.isod.android.widget

import dev.akinom.isod.domain.TimetableEntry
import java.util.Calendar

object TimetableWidgetUtils {
    fun getTodayDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.MONDAY = 2, ..., Calendar.SATURDAY = 7, Calendar.SUNDAY = 1
        // TimetableEntry: 1=Mon ... 7=Sun
        return when (dow) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun getCurrentTime(): String {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$h:$m"
    }

    fun filterToday(entries: List<TimetableEntry>): List<TimetableEntry> {
        val today = getTodayDayOfWeek()
        return entries.filter { it.dayOfWeek == today }
            .sortedBy { it.startTime }
    }

    fun getNextClasses(entries: List<TimetableEntry>): List<TimetableEntry> {
        val now = getCurrentTime()
        val today = getTodayDayOfWeek()
        
        // Filter entries for today and later in the week
        val sortedEntries = entries.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        
        // Current class: startTime <= now < endTime
        val current = sortedEntries.find { it.dayOfWeek == today && it.startTime <= now && it.endTime > now }
        
        // Future classes starting from now
        val future = sortedEntries.filter { 
            (it.dayOfWeek == today && it.startTime > now) || (it.dayOfWeek > today)
        }

        return if (current != null) {
            listOfNotNull(current, future.firstOrNull())
        } else {
            future.take(2)
        }
    }
}
