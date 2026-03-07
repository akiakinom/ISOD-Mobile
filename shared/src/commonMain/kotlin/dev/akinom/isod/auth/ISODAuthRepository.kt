package dev.akinom.isod.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class IsodUserInfo(
    val username: String,
    val semester: String,
    val firstname: String,
    val lastname: String,
    val studentNo: String,
)

sealed class IsodAuthResult {
    data class Success(val user: IsodUserInfo) : IsodAuthResult()
    data class Error(val message: String)      : IsodAuthResult()
}

class IsodAuthRepository {

    private val client = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun validateCredentials(
        username: String,
        apiKey: String,
    ): IsodAuthResult {
        return try {
            val response: HttpResponse = client.get(
                "https://isod.ee.pw.edu.pl/isod-portal/wapi"
            ) {
                parameter("q", "myplan")
                parameter("username", username.trim())
                parameter("apikey", apiKey.trim())
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyAsText()
                    val user = json.decodeFromString<IsodUserInfo>(body)
                    IsodAuthResult.Success(user)
                }
                HttpStatusCode.BadRequest -> {
                    IsodAuthResult.Error("Invalid username or API key.")
                }
                else -> {
                    IsodAuthResult.Error("Unexpected error: ${response.status.value}")
                }
            }
        } catch (e: Exception) {
            println("❌ ISOD error: ${e::class.simpleName}: ${e.message}")
            IsodAuthResult.Error(
                if (e.message?.contains("Unable to resolve") == true ||
                    e.message?.contains("UnknownHostException") == true)
                    "No internet connection."
                else
                    "Could not reach ISOD. Try again later."
            )
        }
    }
}