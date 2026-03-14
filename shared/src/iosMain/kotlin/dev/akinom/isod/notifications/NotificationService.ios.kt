package dev.akinom.isod.notifications

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

actual class NotificationService {

    actual fun notify(payload: NotificationPayload) {
        val content = UNMutableNotificationContent().apply {
            setTitle(payload.title)
            setBody(payload.body)
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound())
            setUserInfo(payload.newsHash?.let { mapOf("newsHash" to it) } ?: emptyMap<Any?, Any?>())
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = payload.id,
            content    = content,
            trigger    = null,  // deliver immediately
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { error ->
                if (error != null) println("❌ iOS notification error: ${error.localizedDescription}")
            }
    }

    actual fun requestPermission() {
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or
                        UNAuthorizationOptionSound or
                        UNAuthorizationOptionBadge
            ) { granted, error ->
                println(if (granted) "✅ Notification permission granted" else "❌ Notification permission denied: ${error?.localizedDescription}")
            }
    }
}
