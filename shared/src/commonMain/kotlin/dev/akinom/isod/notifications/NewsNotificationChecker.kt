package dev.akinom.isod.notifications

import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.NewsHeaderEntity
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult

class NewsNotificationChecker(
    private val db: IsodDatabase,
    private val isodApi: IsodApiClient,
    private val notificationService: NotificationService,
    private val semester: String,
) {
    private val queries get() = db.newsQueries

    suspend fun check() {
        val freshHeaders = when (val result = isodApi.getNewsHeaders()) {
            is IsodResult.Success -> result.data
            else -> {
                println("⚠️ NewsNotificationChecker: failed to fetch news")
                return
            }
        }

        val now = currentTimeMillis()

        val existing = queries.selectAllHeaders(semester).executeAsList()
        val isFirstSync = existing.isEmpty()
        val existingHashes = existing.map { it.id }.toSet()

        db.transaction {
            freshHeaders
                .filter { it.id !in existingHashes }
                .forEach { header ->
                    queries.upsertHeader(
                        NewsHeaderEntity(
                            id = header.id,
                            semester = semester,
                            title = header.title,
                            type = header.type.toString(),
                            label = header.label,
                            date = header.date.toString(),
                            author = header.author,
                            hasSentNotification = if (isFirstSync) 1L else 0L,
                            lastUpdated = now,
                        )
                    )
                }
        }

        val unsent = queries.selectUnsentNotifications().executeAsList()
        if (unsent.isEmpty()) return

        println("🔔 NewsNotificationChecker: ${unsent.size} unsent notifications")

        unsent.forEach { entity ->
            notificationService.notify(
                NotificationPayload(
                    id          = entity.id,
                    title       = entity.title,
                    body        = entity.label,
                    type        = entity.type,
                    newsHash    = entity.id,
                )
            )
            queries.markNotificationSent(entity.id)
        }
    }

    fun suppressHistorical() {
        queries.markAllNotificationsSent()
    }
}