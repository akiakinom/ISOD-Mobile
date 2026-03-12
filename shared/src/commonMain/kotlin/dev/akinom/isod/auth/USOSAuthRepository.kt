package dev.akinom.isod.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

private const val BASE_URL      = "https://apps.usos.pw.edu.pl"
private const val REQUEST_TOKEN = "$BASE_URL/services/oauth/request_token"
private const val AUTHORIZE_URL = "$BASE_URL/services/oauth/authorize"
private const val ACCESS_TOKEN  = "$BASE_URL/services/oauth/access_token"
const val USOS_CALLBACK_URL     = "isodmobile://oauth/callback"

data class UsosRequestToken(
    val token: String,
    val tokenSecret: String,
    val authorizeUrl: String,
)

data class UsosAccessToken(
    val token: String,
    val tokenSecret: String,
)

sealed class UsosAuthResult {
    data class RequestTokenSuccess(val requestToken: UsosRequestToken) : UsosAuthResult()
    data class AccessTokenSuccess(val accessToken: UsosAccessToken)    : UsosAuthResult()
    data class Error(val message: String, val isNetworkError: Boolean = false) : UsosAuthResult()
}

class UsosAuthRepository(
    private val consumerKey: String,
    private val consumerSecret: String,
) {
    private val client = HttpClient()

    suspend fun getRequestToken(): UsosAuthResult {
        return try {
            val authHeader = OAuth1Signer.buildHeader(
                method         = "POST",
                url            = REQUEST_TOKEN,
                consumerKey    = consumerKey,
                consumerSecret = consumerSecret,
                extraParams    = mapOf("oauth_callback" to USOS_CALLBACK_URL),
            )

            val response = client.post(REQUEST_TOKEN) {
                header(HttpHeaders.Authorization, authHeader)
                parameter("oauth_callback", USOS_CALLBACK_URL)
            }

            val body = response.bodyAsText()
            println("📡 USOS request token response fetched.")

            val params = parseOAuthResponse(body)
            val token       = params["oauth_token"]        ?: return UsosAuthResult.Error("Missing oauth_token")
            val tokenSecret = params["oauth_token_secret"] ?: return UsosAuthResult.Error("Missing oauth_token_secret")

            UsosAuthResult.RequestTokenSuccess(
                UsosRequestToken(
                    token        = token,
                    tokenSecret  = tokenSecret,
                    authorizeUrl = "$AUTHORIZE_URL?oauth_token=$token",
                )
            )
        } catch (e: Exception) {
            UsosAuthResult.Error("Network error", isNetworkError = true)
        }
    }

    suspend fun getAccessToken(
        requestToken: String,
        requestTokenSecret: String,
        verifier: String,
    ): UsosAuthResult {
        return try {
            val authHeader = OAuth1Signer.buildHeader(
                method         = "POST",
                url            = ACCESS_TOKEN,
                consumerKey    = consumerKey,
                consumerSecret = consumerSecret,
                token          = requestToken,
                tokenSecret    = requestTokenSecret,
                extraParams    = mapOf("oauth_verifier" to verifier),
            )

            val response = client.post(ACCESS_TOKEN) {
                header(HttpHeaders.Authorization, authHeader)
                parameter("oauth_verifier", verifier)
            }

            val body = response.bodyAsText()
            println("📡 USOS access token response fetched.")

            val params = parseOAuthResponse(body)
            val token       = params["oauth_token"]        ?: return UsosAuthResult.Error("Missing oauth_token")
            val tokenSecret = params["oauth_token_secret"] ?: return UsosAuthResult.Error("Missing oauth_token_secret")

            UsosAuthResult.AccessTokenSuccess(UsosAccessToken(token, tokenSecret))
        } catch (e: Exception) {
            UsosAuthResult.Error("Network error", isNetworkError = true)
        }
    }

    private fun parseOAuthResponse(body: String): Map<String, String> =
        body.trim().split("&")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
}
