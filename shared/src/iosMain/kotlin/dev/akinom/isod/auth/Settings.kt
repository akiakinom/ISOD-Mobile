package dev.akinom.isod.auth

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(): Settings {
    val defaults = NSUserDefaults(suiteName = "group.dev.akinom.isod")
    return NSUserDefaultsSettings(defaults)
}
