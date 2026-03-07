package dev.akinom.isod.auth

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private var appContext: Context? = null

fun initSettingsContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createSettings(): Settings {
    val context = appContext ?: throw IllegalStateException("Settings context not initialized. Call initSettingsContext(context) first.")
    return SharedPreferencesSettings(
        context.getSharedPreferences("isod_settings", Context.MODE_PRIVATE)
    )
}
