package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.EventEntity
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.AkinomApiClient
import dev.akinom.isod.domain.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class EventRepository(
    private val db: IsodDatabase,
    private val api: AkinomApiClient,
    private val scope: CoroutineScope,
) {
    private val queries = db.eventQueries

    fun getEvents(): Flow<List<Event>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows: List<EventEntity> -> rows.map { it.toDomain() } }
            .onStart { refreshIfStale() }

    fun refreshIfStale() {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.lastUpdated().executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.NEWS_TTL_MS)) {
                refresh()
            }
        }
    }

    suspend fun refresh() {
        try {
            val response = api.getEvents()
            val now = currentTimeMillis()
            db.transaction {
                queries.deleteAll()
                response.forEach { dto ->
                    queries.upsert(
                        EventEntity(
                            id = dto.id.toLong(),
                            title = dto.title,
                            location = dto.location,
                            eventDate = dto.eventDate,
                            description = dto.description,
                            imageUrl = dto.getImageUrl(),
                            lastUpdated = now
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("⚠️ EventRepository refresh failed: ${e.message}")
        }
    }
}

private fun EventEntity.toDomain() = Event(
    id = id.toInt(),
    title = title,
    location = location,
    date = LocalDateTime.parse(eventDate),
    description = description,
    imageUrl = imageUrl
)
