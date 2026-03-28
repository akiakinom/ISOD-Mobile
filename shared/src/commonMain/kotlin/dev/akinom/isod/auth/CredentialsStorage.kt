package dev.akinom.isod.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.akinom.isod.domain.NewsType

private const val KEY_ISOD_USERNAME     = "isod_username"
private const val KEY_ISOD_API_KEY      = "isod_api_key"
private const val KEY_USOS_TOKEN        = "usos_token"
private const val KEY_USOS_TOKEN_SECRET = "usos_token_secret"
private const val KEY_THEME             = "app_theme"
private const val KEY_WIDGET_SHOW_ALL_DAY = "widget_show_all_day"
private const val KEY_NOTIF_PREFIX      = "notif_enabled_"

enum class AppThemeSetting {
    SYSTEM, LIGHT, DARK
}

class CredentialsStorage(
    private val settings: Settings,
) {
    fun saveIsodCredentials(username: String, apiKey: String) {
        settings[KEY_ISOD_USERNAME] = username
        settings[KEY_ISOD_API_KEY]  = apiKey
    }

    fun getIsodUsername(): String? = settings.getStringOrNull(KEY_ISOD_USERNAME)
    fun getIsodApiKey(): String?   = settings.getStringOrNull(KEY_ISOD_API_KEY)

    fun hasIsodCredentials(): Boolean =
        getIsodUsername() != null && getIsodApiKey() != null

    fun clearIsodCredentials() {
        settings.remove(KEY_ISOD_USERNAME)
        settings.remove(KEY_ISOD_API_KEY)
    }

    fun saveUsosTokens(token: String, tokenSecret: String) {
        settings[KEY_USOS_TOKEN]        = token
        settings[KEY_USOS_TOKEN_SECRET] = tokenSecret
    }

    fun getUsosToken(): String?       = settings.getStringOrNull(KEY_USOS_TOKEN)
    fun getUsosTokenSecret(): String? = settings.getStringOrNull(KEY_USOS_TOKEN_SECRET)

    fun hasUsosTokens(): Boolean =
        getUsosToken() != null && getUsosTokenSecret() != null

    fun clearUsosTokens() {
        settings.remove(KEY_USOS_TOKEN)
        settings.remove(KEY_USOS_TOKEN_SECRET)
    }

    fun getTheme(): AppThemeSetting {
        val themeName = settings.getString(KEY_THEME, AppThemeSetting.SYSTEM.name)
        return try {
            AppThemeSetting.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppThemeSetting.SYSTEM
        }
    }

    fun setTheme(theme: AppThemeSetting) {
        settings[KEY_THEME] = theme.name
    }

    fun shouldShowAllDayInWidget(): Boolean = settings.getBoolean(KEY_WIDGET_SHOW_ALL_DAY, false)

    fun setShowAllDayInWidget(showAllDay: Boolean) {
        settings[KEY_WIDGET_SHOW_ALL_DAY] = showAllDay
    }

    fun isNotificationEnabled(type: NewsType): Boolean {
        return settings.getBoolean(KEY_NOTIF_PREFIX + type.name, true)
    }

    fun setNotificationEnabled(type: NewsType, enabled: Boolean) {
        settings[KEY_NOTIF_PREFIX + type.name] = enabled
    }

    fun isFullyLinked(): Boolean = hasIsodCredentials() && hasUsosTokens()

    fun clearAll() {
        clearIsodCredentials()
        clearUsosTokens()
    }
}
