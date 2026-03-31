package dev.akinom.isod.android

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.notifications.NewsNotificationChecker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "isod_news_notification"

class NewsNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val storage: CredentialsStorage by inject()
    private val checker: NewsNotificationChecker by inject()

    override suspend fun doWork(): Result {
        if (!storage.hasIsodCredentials()) {
            // If user is not logged in, we don't need to check for news.
            // We return success so WorkManager doesn't retry, but we stop here.
            return Result.success()
        }

        return try {
            checker.check()
            Result.success()
        } catch (e: Exception) {
            println("❌ NewsNotificationWorker failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NewsNotificationWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
