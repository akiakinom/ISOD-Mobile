package dev.akinom.isod.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
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
        LocalDate(2026, 3, 13) to 4 // 30.03.2025 uses Friday's schedule
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
        
        val startMonday = config.start.toEpochDays() - (config.start.dayOfWeek.isoDayNumber - 1)
        val todayMonday = today.toEpochDays() - (today.dayOfWeek.isoDayNumber - 1)
        
        val week = ((todayMonday - startMonday) / 7) + 1
        
        if (today < config.start || week > 15) return null
        
        return week.toInt()
    }
}
