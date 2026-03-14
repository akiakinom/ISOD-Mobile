package dev.akinom.isod.notifications

import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.NewsHeaderEntity
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.domain.NewsType

class NewsNotificationChecker(
    private val db: IsodDatabase,
    private val isodApi: IsodApiClient,
    private val notificationService: NotificationService,
    private val semester: String,
) {
    private val queries get() = db.newsQueries

    suspend fun check() {
        val freshHeaders = when (val result = isodApi.getNewsHeaders(semester)) {
            is IsodResult.Success -> result.data
            else -> {
                println("⚠️ NewsNotificationChecker: failed to fetch news")
                return
            }
        }

        val now = currentTimeMillis()

        val existing = queries.selectAllHeaders(semester).executeAsList()
        val isFirstSync = existing.isEmpty()
        val existingHashes = existing.map { it.hash }.toSet()

        db.transaction {
            freshHeaders
                .filter { it.hash !in existingHashes }
                .forEach { header ->
                    queries.upsertHeader(
                        NewsHeaderEntity(
                            hash                = header.hash,
                            semester            = semester,
                            subject             = header.subject,
                            type                = header.type.code,
                            modifiedDate        = header.modifiedDate,
                            modifiedBy          = header.modifiedBy,
                            noAttachments       = header.noAttachments.toLong(),
                            hasSentNotification = if (isFirstSync) 1L else 0L,
                            lastUpdated         = now,
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
                    id       = entity.hash,
                    title    = NewsType.fromCode(entity.type).toNotificationTitle(),
                    body     = entity.subject,
                    newsHash = entity.hash,
                )
            )
            queries.markNotificationSent(entity.hash)
        }
    }

    fun suppressHistorical() {
        queries.markAllNotificationsSent()
    }
}

private fun NewsType.toNotificationTitle(): String = when (this) {
    NewsType.QUIZ         -> "📝 Quiz"
    NewsType.ANNOUNCEMENT -> "📢 Announcement"
    NewsType.IMPORTANT    -> "⚠️ Important"
    NewsType.PROJECT_STATUS -> "🎓 Grade"
    NewsType.PROJECT_GROUP_CHANGE -> "📎 Material"
    NewsType.CLASS_ENROLLMENT -> "✅ Enrollment"
    else                  -> "📬 ISOD"
}
