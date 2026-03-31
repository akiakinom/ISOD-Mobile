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
import dev.akinom.isod.domain.NewsType
import androidx.core.graphics.createBitmap

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

        val largeIcon = createLargeIcon(payload)
        val smallIconRes = getResId("ic_isod_notif", "drawable")
        val accentColor = getNewsColor(payload.type)

        val builder = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(if (smallIconRes != 0) smallIconRes else android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(accentColor)
            .setSilent(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        NotificationManagerCompat.from(context).notify(payload.id.hashCode(), builder.build())
    }

    private fun createLargeIcon(payload: NotificationPayload): Bitmap? {
        val subjectCode = payload.subjectCode
        if (subjectCode != null && isSubjectIcon(subjectCode)) {
            return createSubjectBitmap(subjectCode, payload.type)
        }

        val newsType = try { NewsType.valueOf(payload.type) } catch (e: Exception) { NewsType.OTHER }
        val iconName = if (newsType == NewsType.FACULTY_STUDENT_COUNCIL) "wrs_logo" else "isod_logo"
        val iconRes = getResId(iconName, "drawable")
        
        if (iconRes == 0) return null

        return ContextCompat.getDrawable(context, iconRes)?.let { drawable ->
            val size = 192
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun isSubjectIcon(subjectCode: String): Boolean {
        return subjectCode.length <= 6 &&
                subjectCode.all { it.isUpperCase() || it.isDigit() } &&
                subjectCode.uppercase() != "WRS"
    }

    private fun createSubjectBitmap(text: String, typeString: String): Bitmap {
        val size = 192
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = getNewsColor(typeString)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = when {
            text.length <= 3 -> size * 0.4f
            text.length <= 5 -> size * 0.3f
            else -> size * 0.25f
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, size / 2f, (size / 2f) + (bounds.height() / 2f), paint)
        
        return bitmap
    }

    private fun getNewsColor(typeString: String): Int {
        val type = try { NewsType.valueOf(typeString) } catch (e: Exception) { NewsType.OTHER }
        return when (type) {
            NewsType.IMPORTANT -> 0xFFF44336.toInt()
            NewsType.GRADE -> 0xFF4CAF50.toInt()
            NewsType.CLASS -> 0xFF2196F3.toInt()
            NewsType.DEANS_OFFICE -> 0xFFFF9800.toInt()
            NewsType.FACULTY_STUDENT_COUNCIL -> 0xFFFFC107.toInt()
            NewsType.TIMETABLE_UPDATE -> 0xFF9C27B0.toInt()
            else -> 0xFF9E9E9E.toInt()
        }
    }

    private fun getResId(name: String, type: String): Int {
        return context.resources.getIdentifier(name, type, context.packageName)
    }

    actual fun requestPermission() { }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "isod_news", 
                "ISOD News", 
                NotificationManager.IMPORTANCE_LOW // Silent
            )
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setSound(null, null)
            manager.createNotificationChannel(channel)
        }
    }
}
