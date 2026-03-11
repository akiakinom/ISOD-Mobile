package dev.akinom.isod

import dev.akinom.isod.di.initKoin
import dev.akinom.isod.di.notificationModule

fun initApp() {
    initKoin(additionalModules = listOf(notificationModule))

    IosNotificationScheduler.register()

    dev.akinom.isod.notifications.NotificationService().requestPermission()
}

fun scheduleBackgroundTasks() {
    IosNotificationScheduler.scheduleNext()
}
