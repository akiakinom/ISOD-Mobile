package dev.akinom.isod.notifications

data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val channelId: String = "isod_news",
    val newsHash: String? = null,
)

expect class NotificationService {
    fun notify(payload: NotificationPayload)
    fun requestPermission()
}
