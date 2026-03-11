package dev.akinom.isod

import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.notifications.NewsNotificationChecker
import dev.akinom.isod.notifications.NotificationService
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

private const val BG_TASK_ID = "dev.akinom.isod.news_check"

object IosNotificationScheduler : KoinComponent {

    private val db: IsodDatabase       by inject()
    private val isodApi: IsodApiClient by inject()

    fun register() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = BG_TASK_ID,
            usingQueue = null,
        ) { task ->
            handleTask(task!!)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun scheduleNext() {
        val request = BGAppRefreshTaskRequest(identifier = BG_TASK_ID).apply {
            earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(15 * 60.0)
        }
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        } catch (e: Exception) {
            println("❌ BGTask schedule failed: ${e.message}")
        }
    }

    private fun handleTask(task: BGTask) {
        scheduleNext()

        val job = CoroutineScope(Dispatchers.Default).launch {
            try {
                val checker = NewsNotificationChecker(
                    db                  = db,
                    isodApi             = isodApi,
                    notificationService = NotificationService(),
                    semester            = currentSemester(),
                )
                checker.check()
                task.setTaskCompletedWithSuccess(true)
            } catch (e: Exception) {
                println("❌ BGTask work failed: ${e.message}")
                task.setTaskCompletedWithSuccess(false)
            }
        }

        task.expirationHandler = { job.cancel() }
    }
}