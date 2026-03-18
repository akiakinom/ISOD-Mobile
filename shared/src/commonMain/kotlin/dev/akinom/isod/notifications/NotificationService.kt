package dev.akinom.isod.notifications

enum class NotificationType {
    IMPORTANT,
    GRADE,
    CLASS_MESSAGE,
    DZIEKANAT,
    WRS,
    CLASS_SIGN_UP_UPDATE,
    OTHER
}

data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType = NotificationType.OTHER,
    val subjectCode: String? = null,
    val channelId: String = "isod_news",
    val newsHash: String? = null,
)

expect class NotificationService {
    fun notify(payload: NotificationPayload)
    fun requestPermission()
}
