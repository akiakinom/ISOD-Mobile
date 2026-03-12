package dev.akinom.isod.domain

import kotlinx.datetime.Clock
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

    fun getConfiguration(semesterId: String): SemesterConfig? {
        return configurations.find { it.id == semesterId }
    }

    fun getCurrentWeek(semesterId: String, today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date): Int? {
        val config = getConfiguration(semesterId) ?: return null
        
        val startMonday = config.start.toEpochDays() - (config.start.dayOfWeek.isoDayNumber - 1)
        val todayMonday = today.toEpochDays() - (today.dayOfWeek.isoDayNumber - 1)
        
        val week = ((todayMonday - startMonday) / 7) + 1
        
        if (today < config.start || week > 15) return null
        
        return week
    }
}
