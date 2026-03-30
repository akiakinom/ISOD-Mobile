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

data class AcademicBreak(
    val id: Int,
    val type: String,
    val dateFrom: LocalDate,
    val dateTo: LocalDate
)

data class AcademicExam(
    val id: Int,
    val dateFrom: LocalDate,
    val dateTo: LocalDate
)

data class AcademicDean(
    val id: Int,
    val date: LocalDate,
    val timeFrom: String,
    val timeTo: String
)

object AcademicCalendar {
    private var configurations = listOf<SemesterConfig>()
    private var daySubstitutions = mapOf<LocalDate, Int>()
    private var breaks = listOf<AcademicBreak>()
    private var exams = listOf<AcademicExam>()
    private var deans = listOf<AcademicDean>()

    fun updateSemesters(newConfigs: List<SemesterConfig>) {
        configurations = newConfigs
    }

    fun updateSubstitutions(newSubstitutions: Map<LocalDate, Int>) {
        daySubstitutions = newSubstitutions
    }

    fun updateBreaks(newBreaks: List<AcademicBreak>) {
        breaks = newBreaks
    }

    fun updateExams(newExams: List<AcademicExam>) {
        exams = newExams
    }

    fun updateDeans(newDeans: List<AcademicDean>) {
        deans = newDeans
    }

    fun getConfiguration(semesterId: String): SemesterConfig? {
        return configurations.find { it.id == semesterId }
    }

    fun getDaySubstitution(date: LocalDate): Int? {
        return daySubstitutions[date]
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
        
        // We allow weeks slightly outside 1-15 range if they are close, 
        // but typically 15 is the limit for a semester.
        if (adjustedToday < config.start || week > 16) return null
        
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

    fun isBreak(date: LocalDate): Boolean {
        return breaks.any { date >= it.dateFrom && date <= it.dateTo }
    }

    fun isExam(date: LocalDate): Boolean {
        return exams.any { date >= it.dateFrom && date <= it.dateTo }
    }

    fun getDeanHours(date: LocalDate): AcademicDean? {
        return deans.find { it.date == date }
    }

    fun getBreaks(): List<AcademicBreak> = breaks
    fun getExams(): List<AcademicExam> = exams
    fun getDeans(): List<AcademicDean> = deans
}
