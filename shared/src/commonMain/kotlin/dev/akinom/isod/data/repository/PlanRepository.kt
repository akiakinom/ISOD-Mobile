package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.ISODMobileDatabase
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.decodeStringList
import dev.akinom.isod.data.cache.encodeToString
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.data.remote.dto.toClassType
import dev.akinom.isod.domain.PlanItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class PlanRepository(
    private val db: ISODMobileDatabase,
    private val api: IsodApiClient,
    private val scope: CoroutineScope,
) {
    private val queries = db.planItemQueries

    fun getPlan(semester: String): Flow<List<PlanItem>> =
        queries.selectAll(semester)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshIfStale(semester) }

    suspend fun refresh(semester: String) {
        when (val result = api.getPlan(semester)) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                db.transaction {
                    queries.deleteAll(semester)
                    result.data.forEach { item ->
                        queries.upsert(item.toEntity(semester, now))
                    }
                }
            }
            is IsodResult.Error -> println("⚠️ PlanRepository refresh failed: ${result.message}")
        }
    }

    private fun refreshIfStale(semester: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.lastUpdated(semester).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.PLAN_TTL_MS)) {
                refresh(semester)
            }
        }
    }
}

private fun dev.akinom.isod.PlanItemEntity.toDomain() = PlanItem(
    id              = id,
    courseName      = courseName,
    courseNameShort = courseNameShort,
    courseNumber    = courseNumber,
    courseVersion   = courseVersion,
    teachers        = teachers.decodeStringList(),
    startTime       = startTime,
    endTime         = endTime,
    dayOfWeek       = dayOfWeek.toInt(),
    cycle           = cycle,
    cycleShort      = cycleShort,
    groups          = groups.decodeStringList(),
    building        = building,
    buildingShort   = buildingShort,
    room            = room,
    typeOfClasses   = typeOfClasses.toClassType(),
)

private fun PlanItem.toEntity(semester: String, now: Long) = dev.akinom.isod.PlanItemEntity(
    id              = id,
    courseName      = courseName,
    courseNameShort = courseNameShort,
    courseNumber    = courseNumber,
    courseVersion   = courseVersion,
    teachers        = teachers.encodeToString(),
    startTime       = startTime,
    endTime         = endTime,
    dayOfWeek       = dayOfWeek.toLong(),
    cycle           = cycle,
    cycleShort      = cycleShort,
    groups          = groups.encodeToString(),
    building        = building,
    buildingShort   = buildingShort,
    room            = room,
    typeOfClasses   = typeOfClasses.name,
    semester        = semester,
    lastUpdated     = now,
)