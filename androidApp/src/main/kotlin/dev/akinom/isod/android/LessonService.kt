package dev.akinom.isod.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.currentDayOfWeek
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.auth.currentTimeHHmm
import dev.akinom.isod.auth.currentWeekMonday
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.domain.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LessonService : Service() {

    private val repository: TimetableRepository by inject()
    private val storage: CredentialsStorage by inject()
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    companion object {
        const val CHANNEL_ID = "ongoing_lesson"
        const val NOTIFICATION_ID = 1001
        
        fun start(context: Context) {
            val intent = Intent(context, LessonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, LessonService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startUpdating()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    private fun startUpdating() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateNotification()
                handler.postDelayed(this, 60000) // Update every minute
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun updateNotification() {
        serviceScope.launch {
            val semester = currentSemester()
            val weekStart = currentWeekMonday()
            val entries = repository.getTimetable(semester, weekStart).firstOrNull() ?: emptyList()
            
            val now = currentTimeHHmm()
            val dow = currentDayOfWeek()
            
            val currentClass = entries.find { it.dayOfWeek == dow && it.startTime <= now && it.endTime > now }
            val nextClass = if (currentClass == null) {
                entries.filter { it.dayOfWeek == dow && it.startTime > now }
                    .sortedBy { it.startTime }
                    .firstOrNull()
            } else null

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (currentClass != null) {
                val progress = calculateProgress(currentClass.startTime, currentClass.endTime, now)
                val notification = createNotification(currentClass, progress, 100, false)
                startForeground(NOTIFICATION_ID, notification)
            } else if (nextClass != null) {
                val diff = timeToMinutes(nextClass.startTime) - timeToMinutes(now)
                if (diff in 1..15) {
                    val notification = createNotification(nextClass, diff, 15, true)
                    startForeground(NOTIFICATION_ID, notification)
                } else {
                    stopForeground(true)
                }
            } else {
                stopForeground(true)
            }
        }
    }

    private fun calculateProgress(start: String, end: String, now: String): Int {
        val startMin = timeToMinutes(start)
        val endMin = timeToMinutes(end)
        val nowMin = timeToMinutes(now)
        
        if (endMin <= startMin) return 0
        val total = endMin - startMin
        val elapsed = nowMin - startMin
        return ((elapsed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun createNotification(entry: TimetableEntry, value: Int, max: Int, isUpcoming: Boolean): android.app.Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon later
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)

        if (isUpcoming) {
            builder.setContentTitle(entry.courseName)
            builder.setContentText(getString(R.string.notif_starts_in, value))
            // No progress bar for upcoming, or maybe countdown? 
            // User requested "lesson x in 15 minutes", no progress bar mentioned for upcoming.
        } else {
            builder.setContentTitle(entry.courseName)
            builder.setContentText("${entry.startTime} - ${entry.endTime} (${entry.displayLocation})")
            builder.setProgress(max, value, false)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notif_ongoing_lesson_channel_name)
            val descriptionText = getString(R.string.notif_ongoing_lesson_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
