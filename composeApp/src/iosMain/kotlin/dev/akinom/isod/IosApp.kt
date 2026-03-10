package dev.akinom.isod

import dev.akinom.isod.di.initKoin

fun initApp() {
    initKoin()

    IosNotificationScheduler.register()

    dev.akinom.isod.notifications.NotificationService().requestPermission()
}

fun scheduleBackgroundTasks() {
    IosNotificationScheduler.scheduleNext()
}
