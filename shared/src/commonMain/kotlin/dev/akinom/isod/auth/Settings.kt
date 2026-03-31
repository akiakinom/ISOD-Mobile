package dev.akinom.isod.auth

import com.russhwolf.settings.Settings

expect fun createSettings(): Settings

interface PlatformWidgetUpdater {
    fun updateAllWidgets()
}

object TimetableWidgetUpdater {
    var provider: PlatformWidgetUpdater? = null
    fun update() = provider?.updateAllWidgets()
}

interface PlatformLessonService {
    fun startService()
    fun stopService()
}

object LessonServiceControl {
    var provider: PlatformLessonService? = null
    fun start() = provider?.startService()
    fun stop() = provider?.stopService()
}
