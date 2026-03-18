package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.UsosActivityEntity
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.domain.LangDict
import dev.akinom.isod.domain.UsosActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
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
                        queries.upsert(activity.toEntity(weekStart, now))
                    }
                }
            }
            is UsosResult.NotLinked -> println("ℹ️ USOS not linked, skipping timetable fetch")
            is UsosResult.Error     -> println("⚠️ UsosRepository refresh failed: ${result.message}")
        }
    }

    private fun refreshIfStale(weekStart: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.lastUpdated(weekStart).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, TIMETABLE_TTL_MS)) {
                refresh(weekStart)
            }
        }
    }
}

private fun UsosActivityEntity.toDomain(): UsosActivity {
    val bNameDict = buildingNameJson?.let { json.decodeFromString<LangDict>(it) }
    val bId = buildingNameJson?.let {
        runCatching { json.parseToJsonElement(it).jsonObject["building_id"]?.jsonPrimitive?.content }.getOrNull()
    }

    return UsosActivity(
        type          = type,
        startTime     = startTime,
        endTime       = endTime,
        name          = json.decodeFromString<LangDict>(nameJson),
        courseId      = courseId,
        courseName    = courseNameJson?.let { json.decodeFromString<LangDict>(it) },
        classtypeName = classtypeNameJson?.let { json.decodeFromString<LangDict>(it) },
        lecturers     = lecturersJson?.let { json.decodeFromString<List<String>>(it) } ?: emptyList(),
        buildingName  = bNameDict,
        buildingId    = bId,
        groupNumber   = groupNumber?.toInt(),
        roomNumber    = roomNumber,
        frequency     = frequency,
    )
}

private fun UsosActivity.toEntity(weekStart: String, now: Long): UsosActivityEntity {
    // Composite ID so the same activity doesn't get duplicated across refreshes
    val id = "${courseId ?: type}_${startTime}"

    val bNameJson = if (buildingName != null || buildingId != null) {
        buildJsonObject {
            buildingName?.let {
                put("pl", it.pl)
                put("en", it.en)
            }
            buildingId?.let { put("building_id", it) }
        }.toString()
    } else null

    return UsosActivityEntity(
        id                = id,
        type              = type,
        startTime         = startTime,
        endTime           = endTime,
        nameJson          = json.encodeToString(name),
        courseId          = courseId,
        courseNameJson    = courseName?.let { json.encodeToString(it) },
        classtypeNameJson = classtypeName?.let { json.encodeToString(it) },
        groupNumber       = groupNumber?.toLong(),
        lecturersJson     = json.encodeToString(lecturers),
        buildingNameJson  = bNameJson,
        roomNumber        = roomNumber,
        frequency         = frequency,
        weekStart         = weekStart,
        lastUpdated       = now,
    )
}
