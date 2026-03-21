package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.NewsHeaderEntity
import dev.akinom.isod.NewsItemEntity
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.domain.NewsHeader
import dev.akinom.isod.domain.NewsItem
import dev.akinom.isod.domain.NewsType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class NewsRepository(
    private val db: IsodDatabase,
    private val api: IsodApiClient,
    private val scope: CoroutineScope,
) {
    private val newsQueries = db.newsQueries

    fun getNewsHeaders(semester: String): Flow<List<NewsHeader>> =
        newsQueries.selectAllHeaders(semester)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshHeadersIfStale(semester) }

    suspend fun refreshHeaders(semester: String) {
        when (val result = api.getNewsHeaders()) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                val existingRows = newsQueries.selectAllHeaders(semester).executeAsList()
                val isFirstSync = existingRows.isEmpty()
                val existing = existingRows.associate { it.id to it.hasSentNotification }
                
                db.transaction {
                    newsQueries.deleteAllHeaders(semester)
                    result.data.forEach { header ->
                        val notificationState = existing[header.id] ?: if (isFirstSync) 1L else 0L
                        newsQueries.upsertHeader(
                            header.toEntity(
                                semester = semester,
                                now = now,
                                existingNotificationState = notificationState,
                            )
                        )
                    }
                }
            }
            is IsodResult.Error -> println("⚠️ NewsRepository headers refresh failed: ${result.message}")
        }
    }

    fun getNewsItem(id: String): Flow<NewsItem?> =
        newsQueries.selectItem(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
            .onStart { fetchItemIfMissing(id) }

    suspend fun fetchItem(id: String) {
        when (val result = api.getNewsItem(id)) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                newsQueries.upsertItem(result.data.toEntity(now))
            }
            is IsodResult.Error -> println("⚠️ NewsRepository item fetch failed: ${result.message}")
        }
    }

    private fun refreshHeadersIfStale(semester: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = newsQueries.headersLastUpdated(semester).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.NEWS_TTL_MS)) {
                refreshHeaders(semester)
            }
        }
    }

    private fun fetchItemIfMissing(id: String) {
        scope.launch(Dispatchers.IO) {
            val cached = newsQueries.selectItem(id).executeAsOneOrNull()
            if (cached == null) fetchItem(id)
        }
    }
}

private fun NewsHeaderEntity.toDomain() = NewsHeader(
    id = id,
    title = title,
    date = date?.let { LocalDate.parse(it) },
    author = author,
    type = try { NewsType.valueOf(type) } catch (e: Exception) { NewsType.OTHER },
    label = label,
)

private fun NewsHeader.toEntity(
    semester: String,
    now: Long,
    existingNotificationState: Long = 0L,
) = NewsHeaderEntity(
    id = id,
    semester = semester,
    title = title,
    date = date?.toString(),
    author = author,
    type = type.name,
    label = label,
    hasSentNotification = existingNotificationState,
    lastUpdated = now,
)

private fun NewsItemEntity.toDomain() = NewsItem(
    id = id,
    title = title,
    content = content,
    date = date?.let { LocalDate.parse(it) },
    author = author,
    type = try { NewsType.valueOf(type) } catch (e: Exception) { NewsType.OTHER },
    label = label,
)

private fun NewsItem.toEntity(now: Long) = NewsItemEntity(
    id = id,
    semester = "",
    title = title,
    content = content,
    date = date?.toString(),
    author = author,
    type = type.name,
    label = label,
    lastUpdated = now,
)
