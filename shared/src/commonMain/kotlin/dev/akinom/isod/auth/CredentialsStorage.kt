package dev.akinom.isod.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

private const val KEY_ISOD_USERNAME     = "isod_username"
private const val KEY_ISOD_API_KEY      = "isod_api_key"
private const val KEY_USOS_TOKEN        = "usos_token"
private const val KEY_USOS_TOKEN_SECRET = "usos_token_secret"

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

    fun isFullyLinked(): Boolean = hasIsodCredentials() && hasUsosTokens()

    fun clearAll() {
        clearIsodCredentials()
        clearUsosTokens()
    }
}