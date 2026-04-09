package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.ISODMobileDatabase
import dev.akinom.isod.UsosClassEntity
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.data.remote.dto.toClassType
import dev.akinom.isod.domain.UsosClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

private const val TIMETABLE_TTL_MS = 30 * 60 * 1000L  // 30 minutes

class UsosRepository(
    private val db: ISODMobileDatabase,
    private val api: UsosApiClient,
    private val scope: CoroutineScope,
) {
    private val queries = db.usosClassQueries

    fun getTimetable(): Flow<List<UsosClass>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshIfStale() }

    suspend fun refresh() {
        when (val result = api.getTimetable()) {
            is UsosResult.Success -> {
                val activities = result.data
                val lecturerIds = activities.flatMap { it.lecturersId }.distinct()
                
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
                        lecturers = activity.lecturersId.mapNotNull { lecturerNames[it] }
                    )
                }

                val now = currentTimeMillis()
                db.transaction {
                    queries.deleteAll()
                    activitiesWithLecturers.forEach { activity ->
                        queries.upsert(activity.toEntity(now))
                    }
                }
            }
            is UsosResult.NotLinked -> println("ℹ️ USOS not linked, skipping timetable fetch")
            is UsosResult.Error     -> println("⚠️ UsosRepository refresh failed: ${result.message}")
        }
    }

    private fun refreshIfStale() {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.lastUpdated().executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, TIMETABLE_TTL_MS)) {
                refresh()
            }
        }
    }
}

private fun UsosClassEntity.toDomain(): UsosClass {
    return UsosClass(
        id            = id,
        type          = type.toClassType(),
        startTime     = startTime,
        endTime       = endTime,
        dayOfWeek     = dayOfWeek.toInt(),
        name          = name,
        lecturers     = lecturers?.split(", ") ?: emptyList(),
        building      = building,
        roomNumber    = roomNumber,
    )
}

private fun UsosClass.toEntity(now: Long): UsosClassEntity {
    return UsosClassEntity(
        id = id,
        type = type.toString(),
        startTime = startTime,
        endTime = endTime,
        dayOfWeek = dayOfWeek.toLong(),
        name = name,
        lecturers = lecturers.joinToString { ", " },
        building = building,
        roomNumber = roomNumber,
        lastUpdated = now,
    )
}
