package dev.akinom.isod.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

actual class NotificationService(private val context: Context) {

    init {
        createChannel()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    actual fun notify(payload: NotificationPayload) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(payload.id.hashCode(), notification)
    }

    actual fun requestPermission() {
        // Permission must be requested from an Activity — this is a no-op here.
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Using getIdentifier to avoid compile-time R class resolution issues in the shared module's androidMain.
            // This is safer when resources are defined in the shared module but accessed in a way that might
            // not have the R class generated yet during Koin initialization.
            val name = try {
                val id = context.resources.getIdentifier("notif_channel_news_name", "string", context.packageName)
                if (id != 0) context.getString(id) else "ISOD News"
            } catch (e: Exception) {
                "ISOD News"
            }

            val desc = try {
                val id = context.resources.getIdentifier("notif_channel_news_desc", "string", context.packageName)
                if (id != 0) context.getString(id) else "Notifications for new ISOD announcements and news"
            } catch (e: Exception) {
                "Notifications for new ISOD announcements and news"
            }

            val channel = NotificationChannel(
                "isod_news",
                name,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = desc
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
