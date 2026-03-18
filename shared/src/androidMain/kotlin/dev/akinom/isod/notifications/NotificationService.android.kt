package dev.akinom.isod.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

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

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("newsHash", payload.newsHash)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = createNotificationBitmap(payload)

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(getIconResId(payload.type))
            .setLargeIcon(largeIcon)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(payload.id.hashCode(), notification)
    }

    private fun createNotificationBitmap(payload: NotificationPayload): Bitmap? {
        val subjectCode = payload.subjectCode ?: return null

        val isCourseCode = subjectCode.length <= 6 &&
                subjectCode.all { it.isUpperCase() || it.isDigit() } &&
                subjectCode.uppercase() != "WRS"

        if (!isCourseCode) return null

        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.DKGRAY
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        paint.textSize = when {
            subjectCode.length <= 3 -> size * 0.4f
            subjectCode.length <= 5 -> size * 0.3f
            else -> size * 0.25f
        }

        val bounds = Rect()
        paint.getTextBounds(subjectCode, 0, subjectCode.length, bounds)
        canvas.drawText(subjectCode, size / 2f, (size / 2f) + (bounds.height() / 2f), paint)

        val iconRes = getIconResId(payload.type)
        val drawable = ContextCompat.getDrawable(context, iconRes)
        if (drawable != null) {
            val iconSize = (size * 0.3f).toInt()
            drawable.setTint(Color.GRAY)
            drawable.setBounds(size - iconSize, size - iconSize, size, size)
            drawable.draw(canvas)
        }

        return bitmap
    }

    private fun getIconResId(type: NotificationType): Int {
        return when (type) {
            NotificationType.IMPORTANT -> android.R.drawable.ic_dialog_alert
            NotificationType.GRADE -> android.R.drawable.ic_menu_edit
            NotificationType.CLASS_MESSAGE -> android.R.drawable.ic_menu_send
            NotificationType.DZIEKANAT -> android.R.drawable.ic_menu_myplaces
            NotificationType.WRS -> android.R.drawable.ic_menu_share
            NotificationType.CLASS_SIGN_UP_UPDATE -> android.R.drawable.ic_menu_agenda
            NotificationType.OTHER -> android.R.drawable.ic_dialog_info
        }
    }

    actual fun requestPermission() {
        // Permission must be requested from an Activity — this is a no-op here.
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
