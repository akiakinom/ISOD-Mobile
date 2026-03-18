package dev.akinom.isod.notifications

import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.NewsHeaderEntity
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.domain.NewsType
import dev.akinom.isod.domain.parseSubject

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
            val parsed = parseSubject(entity.subject)
            val type = determineNotificationType(NewsType.fromCode(entity.type), parsed.tag)
            
            notificationService.notify(
                NotificationPayload(
                    id          = entity.hash,
                    title       = type.toTitle(parsed.tag),
                    body        = parsed.displaySubject,
                    type        = type,
                    subjectCode = parsed.tag,
                    newsHash    = entity.hash,
                )
            )
            queries.markNotificationSent(entity.hash)
        }
    }

    private fun determineNotificationType(newsType: NewsType, tag: String?): NotificationType {
        if (tag?.uppercase() == "DZIEKANAT") return NotificationType.DZIEKANAT
        if (tag?.uppercase() == "WRS") return NotificationType.WRS
        
        return when (newsType) {
            NewsType.IMPORTANT -> NotificationType.IMPORTANT
            NewsType.QUIZ, NewsType.PROJECT_STATUS -> NotificationType.GRADE
            NewsType.ANNOUNCEMENT, NewsType.PROJECT_GROUP_CHANGE -> NotificationType.CLASS_MESSAGE
            NewsType.CLASS_ENROLLMENT -> NotificationType.CLASS_SIGN_UP_UPDATE
            else -> NotificationType.OTHER
        }
    }

    fun suppressHistorical() {
        queries.markAllNotificationsSent()
    }
}

private fun NotificationType.toTitle(tag: String?): String = when (this) {
    NotificationType.IMPORTANT -> "Important"
    NotificationType.GRADE -> "Grade"
    NotificationType.CLASS_MESSAGE -> tag ?: "Class Message"
    NotificationType.DZIEKANAT -> "Dziekanat"
    NotificationType.WRS -> "WRS"
    NotificationType.CLASS_SIGN_UP_UPDATE -> "Enrollment"
    NotificationType.OTHER -> "ISOD"
}
