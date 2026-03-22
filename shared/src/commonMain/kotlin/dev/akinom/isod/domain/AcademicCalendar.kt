package dev.akinom.isod.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

data class SemesterConfig(
    val id: String, // e.g., "2023Z", "2024L"
    val start: LocalDate
)

object AcademicCalendar {
    private val configurations = listOf(
        SemesterConfig("2026L", LocalDate(2026, 2, 23))
    )

    private val daySubstitutions = listOf(
        LocalDate(2026, 4, 27) to 5,
        LocalDate(2026, 5, 12) to 5,
        LocalDate(2026, 6, 3) to 5
    )

    fun getConfiguration(semesterId: String): SemesterConfig? {
        return configurations.find { it.id == semesterId }
    }

    fun getDaySubstitution(date: LocalDate): Int? {
        return daySubstitutions.find { it.first == date }?.second
    }

    fun getEffectiveDayOfWeek(date: LocalDate): Int {
        return getDaySubstitution(date) ?: date.dayOfWeek.isoDayNumber
    }

    fun getToday(): LocalDate = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun getTomorrow(): LocalDate {
        val today = getToday()
        return LocalDate.fromEpochDays(today.toEpochDays() + 1)
    }

    fun getCurrentWeek(semesterId: String, today: LocalDate = getToday()): Int? {
        val config = getConfiguration(semesterId) ?: return null
        
        val isoDay = today.dayOfWeek.isoDayNumber
        val adjustedToday = if (isoDay > 5) {
            LocalDate.fromEpochDays(today.toEpochDays() + (8 - isoDay))
        } else {
            today
        }

        val startMonday = config.start.toEpochDays() - (config.start.dayOfWeek.isoDayNumber - 1)
        val todayMonday = adjustedToday.toEpochDays() - (adjustedToday.dayOfWeek.isoDayNumber - 1)
        
        val week = ((todayMonday - startMonday) / 7) + 1
        
        if (adjustedToday < config.start || week > 15) return null
        
        return week.toInt()
    }

    fun getMondayOfWeek(semesterId: String, weekNumber: Int): LocalDate? {
        val config = getConfiguration(semesterId) ?: return null
        val startMondayEpoch = config.start.toEpochDays() - (config.start.dayOfWeek.isoDayNumber - 1)
        return LocalDate.fromEpochDays(startMondayEpoch + (weekNumber - 1) * 7)
    }

    fun getWeekRangeString(monday: LocalDate): String {
        val sunday = LocalDate.fromEpochDays(monday.toEpochDays() + 4)
        fun format(d: LocalDate) = "${d.day.toString().padStart(2, '0')}.${d.month.number.toString().padStart(2, '0')}"
        return "${format(monday)} - ${format(sunday)}"
    }
}
