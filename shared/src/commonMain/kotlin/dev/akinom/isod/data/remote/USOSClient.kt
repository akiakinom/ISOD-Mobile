package dev.akinom.isod.data.remote

import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.OAuth1Signer
import dev.akinom.isod.data.remote.dto.UsosCourseEditionGradesDto
import dev.akinom.isod.data.remote.dto.UsosActivityDto
import dev.akinom.isod.data.remote.dto.UsosGradeDto
import dev.akinom.isod.data.remote.dto.UsosUserInfoDto
import dev.akinom.isod.domain.UsosActivity
import dev.akinom.isod.domain.UsosUserInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val BASE_URL        = "https://apps.usos.pw.edu.pl"
private const val TT_USER_URL     = "$BASE_URL/services/tt/user"
private const val USER_INFO_URL   = "$BASE_URL/services/users/user"
private const val USERS_URL       = "$BASE_URL/services/users/users"
private const val GRADES_TERMS_URL = "$BASE_URL/services/grades/terms2"

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
    suspend fun getTimetable(
        start: String,
        days: Int = 7,
    ): UsosResult<List<UsosActivity>> = fetch(
        url = TT_USER_URL,
        params = mapOf(
            "start" to start,
            "days" to days.toString(),
            "fields" to listOf(
                "type", "start_time", "end_time", "name", "url",
                "course_id", "course_name", "classtype_name",
                "lecturer_ids", "group_number",
                "building_name", "building_id",
                "room_number", "room_id",
                "unit_id", "classtype_id", "frequency",
            ).joinToString("|"),
        ),
    ) { body ->
        json.decodeFromString<List<UsosActivityDto>>(body).map { it.toDomain() }
    }

    suspend fun getGrades(termId: String): UsosResult<Map<String, UsosCourseEditionGradesDto>> =
        fetch(
            url = GRADES_TERMS_URL,
            params = mapOf(
                "term_ids" to termId,
                "fields" to "value_symbol|passes|value_description|counts_into_average|comment",
            ),
        ) { body ->
            val root = json.parseToJsonElement(body).jsonObject
            val termObj = root[termId]?.jsonObject ?: return@fetch emptyMap()
            termObj.entries.mapNotNull { (courseId, courseValue) ->
                runCatching {
                    val grades = json.decodeFromString<List<UsosGradeDto>>(courseValue.toString())
                    courseId to UsosCourseEditionGradesDto(courseGrades = grades)
                }.getOrNull()
            }.toMap()
        }


    suspend fun getLecturerNames(ids: List<Long>): UsosResult<Map<Long, String>> {
        if (ids.isEmpty()) return UsosResult.Success(emptyMap())
        return fetch(
            url    = USERS_URL,
            params = mapOf(
                "user_ids" to ids.joinToString("|"),
                "fields"   to "id|first_name|last_name",
            ),
        ) { body ->
            val obj = json.parseToJsonElement(body).jsonObject
            obj.entries.mapNotNull { (key, value) ->
                val userId    = key.toLongOrNull() ?: return@mapNotNull null
                val person    = value.jsonObject
                val firstName = person["first_name"]?.jsonPrimitive?.content ?: ""
                val lastName  = person["last_name"]?.jsonPrimitive?.content ?: ""
                userId to "$firstName $lastName".trim()
            }.toMap()
        }
    }

    suspend fun getUserInfo(): UsosResult<UsosUserInfo> = fetch(
        url    = USER_INFO_URL,
        params = mapOf("fields" to "id|first_name|last_name|student_number|photo_urls"),
    ) { body ->
        json.decodeFromString<UsosUserInfoDto>(body).toDomain()
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