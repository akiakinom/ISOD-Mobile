package dev.akinom.isod.domain

import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.di.initKoin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierISO8601
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate

class TimetableWidgetProvider : KoinComponent {
    
    init {
        initKoin()
    }

    private val repository: TimetableRepository by inject()
    private val scope = MainScope()

    fun getTodayDayOfWeek(): Int {
        val cal = NSCalendar(NSCalendarIdentifierISO8601)
        val weekday = cal.component(NSCalendarUnitWeekday, fromDate = NSDate()).toInt()
        return when (weekday) {
            2 -> 1 // Mon
            3 -> 2 // Tue
            4 -> 3 // Wed
            5 -> 4 // Thu
            6 -> 5 // Fri
            7 -> 6 // Sat
            1 -> 7 // Sun
            else -> 1
        }
    }

    fun getCurrentTime(): String {
        val cal = NSCalendar(NSCalendarIdentifierISO8601)
        val components = cal.components(NSCalendarUnitHour or NSCalendarUnitMinute, fromDate = NSDate())
        val h = components.hour.toString().padStart(2, '0')
        val m = components.minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    fun getDashboardSchedule(completion: (Boolean, List<TimetableEntry>) -> Unit) {
        scope.launch {
            try {
                val monday = currentWeekMonday()
                val semester = currentSemester()
                val timetable = repository.getTimetable(semester, monday).first()
                val today = getTodayDayOfWeek()
                val now = getCurrentTime()
                
                val currentWeek = AcademicCalendar.getCurrentWeek(semester)
                
                val result = TimetableWidgetLogic.getDashboardSchedule(
                    entries = timetable,
                    todayDayOfWeek = today,
                    currentTime = now,
                    currentWeek = currentWeek,
                    todayDate = AcademicCalendar.getToday()
                )
                completion(result.first, result.second)
            } catch (e: Exception) {
                completion(false, emptyList())
            }
        }
    }

    // Keep old one for compatibility if needed, but redirected
    fun getTodaySchedule(completion: (List<TimetableEntry>) -> Unit) {
        getDashboardSchedule { _, items -> completion(items) }
    }

    fun getNextClasses(completion: (List<TimetableEntry>) -> Unit) {
        scope.launch {
            try {
                val monday = currentWeekMonday()
                val semester = currentSemester()
                val timetable = repository.getTimetable(semester, monday).first()
                val today = getTodayDayOfWeek()
                val now = getCurrentTime()
                val currentWeek = AcademicCalendar.getCurrentWeek(semester)
                
                completion(TimetableWidgetLogic.getNextClasses(
                    entries = timetable,
                    todayDayOfWeek = today,
                    currentTime = now,
                    currentWeek = currentWeek,
                    todayDate = AcademicCalendar.getToday()
                ))
            } catch (e: Exception) {
                completion(emptyList())
            }
        }
    }
}
