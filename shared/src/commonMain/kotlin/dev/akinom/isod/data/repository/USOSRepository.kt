package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.UsosActivityEntity
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.data.remote.dto.toClassType
import dev.akinom.isod.domain.UsosActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

private const val TIMETABLE_TTL_MS = 30 * 60 * 1000L  // 30 minutes

class UsosRepository(
    private val db: IsodDatabase,
    private val api: UsosApiClient,
    private val scope: CoroutineScope,
) {
    private val queries = db.usosActivityQueries

    fun getTimetable(weekStart: String): Flow<List<UsosActivity>> =
        queries.selectByWeek(weekStart)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshIfStale(weekStart) }

    suspend fun refresh(weekStart: String) {
        when (val result = api.getTimetable(start = weekStart, days = 7)) {
            is UsosResult.Success -> {
                val activities = result.data
                val lecturerIds = activities.flatMap { it.lecturerIds }.distinct()
                
                val lecturerNames = if (lecturerIds.isNotEmpty()) {
                    when (val namesResult = api.getLecturerNames(lecturerIds)) {
                        is UsosResult.Success -> namesResult.data
                        else -> emptyMap()
                    }
                } else {
                    emptyMap()
                }

                val activitiesWithLecturers = activities.map { activity ->
                    activity.copy(
                        lecturers = activity.lecturerIds.mapNotNull { lecturerNames[it] }
                    )
                }

                val now = currentTimeMillis()
                db.transaction {
                    queries.deleteByWeek(weekStart)
                    activitiesWithLecturers.forEach { activity ->
                        queries.upsert(activity.toEntity(now))
                    }
                }
            }
            is UsosResult.NotLinked -> println("ℹ️ USOS not linked, skipping timetable fetch")
            is UsosResult.Error     -> println("⚠️ UsosRepository refresh failed: ${result.message}")
        }
    }

    private fun refreshIfStale(weekStart: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.lastUpdated(dayOfWeek = weekStart).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, TIMETABLE_TTL_MS)) {
                refresh(weekStart)
            }
        }
    }
}

private fun UsosActivityEntity.toDomain(): UsosActivity {

    return UsosActivity(
        id            = id,
        type          = type.toClassType(),
        startTime     = startTime,
        endTime       = endTime,
        dayOfWeek     = dayOfWeek,
        name          = name,
        lecturers     = lecturers.let { json.decodeFromString<List<String>>(it.toString()) },
        building      = building,
        roomNumber    = roomNumber,
    )
}

private fun UsosActivity.toEntity(now: Long): UsosActivityEntity {

    return UsosActivityEntity(
        id = id,
        type = type.toString(),
        startTime = startTime,
        endTime = endTime,
        dayOfWeek = dayOfWeek,
        name = name,
        lecturers = lecturers.let { json.decodeFromString<List<String>>(it.toString()) },
        building = building,
        roomNumber = roomNumber,
        lastUpdated = now,
    )
}
