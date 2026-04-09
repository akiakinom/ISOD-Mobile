package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.BreakEntity
import dev.akinom.isod.DeanEntity
import dev.akinom.isod.ExamEntity
import dev.akinom.isod.ISODMobileDatabase
import dev.akinom.isod.SemesterEntity
import dev.akinom.isod.SubstitutionEntity
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.AkinomApiClient
import dev.akinom.isod.domain.AcademicBreak
import dev.akinom.isod.domain.AcademicCalendar
import dev.akinom.isod.domain.AcademicDean
import dev.akinom.isod.domain.AcademicExam
import dev.akinom.isod.domain.SemesterConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class AcademicCalendarRepository(
    private val db: ISODMobileDatabase,
    private val api: AkinomApiClient,
    private val scope: CoroutineScope,
) {
    private val queries = db.academicCalendarQueries

    init {
        observeAndSync()
        refreshIfStale()
    }

    fun getSemestersFlow(): Flow<List<SemesterEntity>> = 
        queries.selectAllSemesters().asFlow().mapToList(Dispatchers.IO)

    private fun observeAndSync() {
        val semestersFlow = getSemestersFlow()
            .onEach { rows: List<SemesterEntity> ->
                val configs = rows.map { SemesterConfig(it.id, LocalDate.parse(it.startDate)) }
                AcademicCalendar.updateSemesters(configs)
            }

        val substitutionsFlow = queries.selectAllSubstitutions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .onEach { rows: List<SubstitutionEntity> ->
                val subs = rows.associate { LocalDate.parse(it.originalDate) to it.dayOfWeek.toInt() }
                AcademicCalendar.updateSubstitutions(subs)
            }

        val breaksFlow = queries.selectAllBreaks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .onEach { rows: List<BreakEntity> ->
                val items = rows.map { AcademicBreak(it.id.toInt(), it.type, LocalDate.parse(it.dateFrom), LocalDate.parse(it.dateTo)) }
                AcademicCalendar.updateBreaks(items)
            }

        val examsFlow = queries.selectAllExams()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .onEach { rows: List<ExamEntity> ->
                val items = rows.map { AcademicExam(it.id.toInt(), LocalDate.parse(it.dateFrom), LocalDate.parse(it.dateTo)) }
                AcademicCalendar.updateExams(items)
            }

        val deansFlow = queries.selectAllDeans()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .onEach { rows: List<DeanEntity> ->
                val items = rows.map { AcademicDean(it.id.toInt(), LocalDate.parse(it.date), it.timeFrom, it.timeTo) }
                AcademicCalendar.updateDeans(items)
            }

        combine(semestersFlow, substitutionsFlow, breaksFlow, examsFlow, deansFlow) { _, _, _, _, _ -> }.launchIn(scope)
    }

    private fun refreshIfStale() {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.getMetadata().executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.ACADEMIC_CALENDAR_TTL_MS)) {
                refresh()
            }
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            try {
                val response = api.getCalendar()
                db.transaction {
                    queries.deleteAllSemesters()
                    queries.deleteAllSubstitutions()
                    queries.deleteAllBreaks()
                    queries.deleteAllExams()
                    queries.deleteAllDeans()
                    
                    response.semesters.forEach { 
                        queries.upsertSemester(SemesterEntity(it.semesterId, it.startDate))
                    }
                    response.substitutions.forEach {
                        queries.upsertSubstitution(SubstitutionEntity(it.originalDate, it.dayOfWeek.toLong()))
                    }
                    response.breaks.forEach {
                        queries.upsertBreak(BreakEntity(it.id.toLong(), it.type, it.dateFrom, it.dateTo))
                    }
                    response.exams.forEach {
                        queries.upsertExam(ExamEntity(it.id.toLong(), it.dateFrom, it.dateTo))
                    }
                    response.deans.forEach {
                        queries.upsertDean(DeanEntity(it.id.toLong(), it.date, it.timeFrom, it.timeTo))
                    }
                    queries.insertMetadata(currentTimeMillis())
                }
            } catch (e: Exception) {
                println("⚠️ AcademicCalendarRepository refresh failed: ${e.message}")
                
                // Fallback: If the database is empty, insert a default semester so the app remains functional
                db.transaction {
                    if (queries.selectAllSemesters().executeAsList().isEmpty()) {
                        queries.upsertSemester(SemesterEntity("2026L", "2026-02-23"))
                    }
                }
            }
        }
    }
}
