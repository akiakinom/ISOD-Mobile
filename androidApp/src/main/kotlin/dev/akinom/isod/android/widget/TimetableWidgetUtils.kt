package dev.akinom.isod.android.widget

import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableWidgetLogic
import java.util.Calendar

object TimetableWidgetUtils {
    fun getTodayDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
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
        return TimetableWidgetLogic.filterToday(entries, getTodayDayOfWeek())
    }

    fun getNextClasses(entries: List<TimetableEntry>): List<TimetableEntry> {
        return TimetableWidgetLogic.getNextClasses(entries, getTodayDayOfWeek(), getCurrentTime())
    }
}
