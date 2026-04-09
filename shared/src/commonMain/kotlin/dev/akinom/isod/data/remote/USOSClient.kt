package dev.akinom.isod.data.remote

import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.OAuth1Signer
import dev.akinom.isod.data.remote.dto.UsosClassDto
import dev.akinom.isod.domain.UsosClass
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val BASE_URL         = "https://apps.usos.pw.edu.pl"
private const val TT_USER_URL      = "$BASE_URL/services/tt/user"
private const val USERS_URL        = "$BASE_URL/services/users/users"

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

sealed class UsosResult<out T> {
    data class Success<T>(val data: T) : UsosResult<T>()
    data class Error(val message: String) : UsosResult<Nothing>()
    object NotLinked : UsosResult<Nothing>()
}

class UsosApiClient(
    private val httpClient: HttpClient,
    private val storage: CredentialsStorage,
    private val consumerKey: String,
    private val consumerSecret: String,
) {

    suspend fun getTimetable(): UsosResult<List<UsosClass>> = fetch(
        url    = TT_USER_URL,
        params = mapOf(
            "fields" to listOf(
                "start_time", "end_time",
                "course_id", "course_name",
                "lecturer_ids", "classtype_name",
                "building_id", "room_number"
            ).joinToString("|"),
        ),
    ) { body ->
        json.decodeFromString<List<UsosClassDto>>(body).map { it.toDomain() }
    }

    suspend fun getLecturerNames(ids: List<Int>): UsosResult<Map<Int, String>> {
        if (ids.isEmpty()) return UsosResult.Success(emptyMap())
        return fetch(
            url    = USERS_URL,
            params = mapOf(
                "user_ids" to ids.joinToString("|"),
                "fields"   to "id|first_name|last_name",
            ),
        ) { body ->
            val obj = json.parseToJsonElement(body).jsonObject
            obj.entries.associate { (key, value) ->
                val userId = key.toInt()
                val person = value.jsonObject
                val firstName = person["first_name"]?.jsonPrimitive?.content ?: ""
                val lastName = person["last_name"]?.jsonPrimitive?.content ?: ""
                userId to "$firstName $lastName".trim()
            }
        }
    }

    private suspend fun <T> fetch(
        url: String,
        params: Map<String, String> = emptyMap(),
        parse: (String) -> T,
    ): UsosResult<T> {
        val token       = storage.getUsosToken()       ?: return UsosResult.NotLinked
        val tokenSecret = storage.getUsosTokenSecret() ?: return UsosResult.NotLinked

        return try {
            val authHeader = OAuth1Signer.buildHeader(
                method         = "GET",
                url            = url,
                consumerKey    = consumerKey,
                consumerSecret = consumerSecret,
                token          = token,
                tokenSecret    = tokenSecret,
                extraParams    = params,
            )

            val response = httpClient.get(url) {
                header(HttpHeaders.Authorization, authHeader)
                params.forEach { (k, v) -> parameter(k, v) }
            }

            val body = response.bodyAsText()
            println("📡 USOS [${url.substringAfterLast('/')}]: ${body.take(200)}")
            UsosResult.Success(parse(body))
        } catch (e: Exception) {
            println("❌ USOS [${url.substringAfterLast('/')}] ${e::class.simpleName}: ${e.message}")
            UsosResult.Error(e.message ?: "Unknown error")
        }
    }
}
