package dev.akinom.isod.android.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.TimetableEntry
import dev.akinom.isod.domain.TimetableWidgetLogic
import java.util.Calendar
import kotlinx.datetime.LocalDate

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

    fun getDashboardSchedule(entries: List<TimetableEntry>, currentWeek: Int?): Pair<Boolean, List<TimetableEntry>> {
        return TimetableWidgetLogic.getDashboardSchedule(
            entries = entries,
            todayDayOfWeek = getTodayDayOfWeek(),
            currentTime = getCurrentTime(),
            currentWeek = currentWeek,
            todayDate = AcademicCalendar.getToday()
        )
    }

    fun getNextClasses(entries: List<TimetableEntry>, currentWeek: Int?): List<TimetableEntry> {
        return TimetableWidgetLogic.getNextClasses(
            entries = entries,
            todayDayOfWeek = getTodayDayOfWeek(),
            currentTime = getCurrentTime(),
            currentWeek = currentWeek,
            todayDate = AcademicCalendar.getToday()
        )
    }

    fun widgetTypeToColor(type: String): ColorProvider {
        val t = type.uppercase()
        val labGreen = Color(0xFF4CAF50)
        val wykBlue = Color(0xFF2196F3)
        val cwiOrange = Color(0xFFFF9800)
        val proYellow = Color(0xFFFFEB3B)
        val wfRed = Color(0xFFF44336)
        val semGray = Color(0xFF9E9E9E)

        return when {
            t.startsWith("WF") -> ColorProvider(wfRed)
            t.startsWith("W") -> ColorProvider(wykBlue)
            t.startsWith("L") -> ColorProvider(labGreen)
            t.startsWith("C") || t.startsWith("Ć") -> ColorProvider(cwiOrange)
            t.startsWith("P") -> ColorProvider(proYellow)
            t.startsWith("S") -> ColorProvider(semGray)
            else -> ColorProvider(Color(0xFF757575))
        }
    }
}
