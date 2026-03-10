package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.NewsHeaderEntity
import dev.akinom.isod.NewsItemEntity
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.decodeStringList
import dev.akinom.isod.data.cache.encodeToString
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.domain.NewsAttachment
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class NewsRepository(
    private val db: IsodDatabase,
    private val api: IsodApiClient,
    private val scope: CoroutineScope,
) {
    private val headerQueries = db.newsQueries
    private val itemQueries   = db.newsQueries

    fun getNewsHeaders(semester: String): Flow<List<NewsHeader>> =
        headerQueries.selectAllHeaders(semester)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshHeadersIfStale(semester) }

    suspend fun refreshHeaders(semester: String) {
        when (val result = api.getNewsHeaders(semester)) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                val existing = headerQueries.selectAllHeaders(semester)
                    .executeAsList()
                    .associate { it.hash to it.hasSentNotification }
                db.transaction {
                    headerQueries.deleteAllHeaders(semester)
                    result.data.forEach { header ->
                        headerQueries.upsertHeader(
                            header.toEntity(
                                semester = semester,
                                now = now,
                                existingNotificationState = existing[header.hash] ?: 0L,
                            )
                        )
                    }
                }
            }
            is IsodResult.Error -> println("⚠️ NewsRepository headers refresh failed: ${result.message}")
        }
    }

    fun getNewsItem(hash: String): Flow<NewsItem?> =
        itemQueries.selectItem(hash)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
            .onStart { fetchItemIfMissing(hash) }

    suspend fun fetchItem(hash: String) {
        when (val result = api.getNewsItem(hash)) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                itemQueries.upsertItem(result.data.toEntity(now))
            }
            is IsodResult.Error -> println("⚠️ NewsRepository item fetch failed: ${result.message}")
        }
    }

    private fun refreshHeadersIfStale(semester: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = headerQueries.headersLastUpdated(semester).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.NEWS_TTL_MS)) {
                refreshHeaders(semester)
            }
        }
    }

    private fun fetchItemIfMissing(hash: String) {
        scope.launch(Dispatchers.IO) {
            val cached = itemQueries.selectItem(hash).executeAsOneOrNull()
            if (cached == null) fetchItem(hash)
        }
    }
}

private fun NewsHeaderEntity.toDomain() = NewsHeader(
    hash          = hash,
    subject       = subject,
    modifiedDate  = modifiedDate,
    modifiedBy    = modifiedBy,
    type          = NewsType.fromCode(type),
    noAttachments = noAttachments.toInt(),
)

private fun NewsHeader.toEntity(
    semester: String,
    now: Long,
    existingNotificationState: Long = 0L,
) = NewsHeaderEntity(
    hash                = hash,
    subject             = subject,
    modifiedDate        = modifiedDate,
    modifiedBy          = modifiedBy,
    type                = type.code,
    noAttachments       = noAttachments.toLong(),
    semester            = semester,
    hasSentNotification = existingNotificationState,
    lastUpdated         = now,
)

private fun NewsItemEntity.toDomain() = NewsItem(
    hash         = hash,
    subject      = subject,
    content      = content,
    modifiedDate = modifiedDate,
    modifiedBy   = modifiedBy,
    type         = NewsType.fromCode(type),
    attachments  = attachments.decodeStringList()
        .map { json.decodeFromString<NewsAttachment>(it) },
)

private fun NewsItem.toEntity(now: Long) = NewsItemEntity(
    hash         = hash,
    subject      = subject,
    content      = content,
    modifiedDate = modifiedDate,
    modifiedBy   = modifiedBy,
    type         = type.code,
    attachments  = json.encodeToString(attachments),
    semester     = "",
    lastUpdated  = now,
)