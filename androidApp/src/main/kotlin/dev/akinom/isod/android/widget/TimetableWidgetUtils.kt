package dev.akinom.isod.android.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
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

    fun widgetTypeToColor(type: String): ColorProvider {
        val t = type.uppercase()
        val projectYellow = Color(0xFFFFE082) // Consistent Muted Yellow
        return when {
            t.contains("WYK") || t.contains("W") -> ColorProvider(Color(0xFF2196F3))
            t.contains("LAB") || t.contains("L") -> ColorProvider(Color(0xFF4CAF50))
            t.contains("ĆWI") || t.contains("C") -> ColorProvider(Color(0xFF009688))
            t.contains("PRO") || t.contains("P") -> ColorProvider(projectYellow)
            t.contains("SEM") || t.contains("S") -> ColorProvider(Color(0xFF9C27B0))
            else -> ColorProvider(Color(0xFF757575))
        }
    }
}
