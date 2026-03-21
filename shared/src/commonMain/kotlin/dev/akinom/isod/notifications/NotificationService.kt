package dev.akinom.isod.notifications

import dev.akinom.isod.domain.NewsType


data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val subjectCode: String? = null,
    val channelId: String = "isod_news",
    val newsHash: String? = null,
)

expect class NotificationService {
    fun notify(payload: NotificationPayload)
    fun requestPermission()
}
