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
