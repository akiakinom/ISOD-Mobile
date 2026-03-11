package dev.akinom.isod.data.remote

import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.data.remote.dto.*
import dev.akinom.isod.domain.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://isod.ee.pw.edu.pl/isod-portal/wapi"

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

sealed class IsodResult<out T> {
    data class Success<T>(val data: T) : IsodResult<T>()
    data class Error(val message: String) : IsodResult<Nothing>()
}

class IsodApiClient(
    private val httpClient: HttpClient,
    private val storage: CredentialsStorage,
) {

    suspend fun getPlan(semester: String? = null): IsodResult<List<PlanItem>> =
        fetch("myplan", buildMap {
            if (semester != null) put("semester", semester)
        }) { body ->
            json.decodeFromString<PlanResponseDto>(body).planItems.map { it.toDomain() }
        }

    suspend fun getNewsHeaders(
        semester: String? = null,
        from: Int? = null,
        to: Int? = null,
    ): IsodResult<List<NewsHeader>> =
        fetch("mynewsheaders", buildMap {
            if (semester != null) put("semester", semester)
            if (from != null)    put("from", from.toString())
            if (to != null)      put("to", to.toString())
        }) { body ->
            json.decodeFromString<NewsHeadersResponseDto>(body).items.map { it.toDomain() }
        }

    suspend fun getNewsFull(
        semester: String? = null,
        from: Int? = null,
        to: Int? = null,
    ): IsodResult<List<NewsItem>> =
        fetch("mynewsfull", buildMap {
            if (semester != null) put("semester", semester)
            if (from != null)    put("from", from.toString())
            if (to != null)      put("to", to.toString())
        }) { body ->
            json.decodeFromString<NewsFullResponseDto>(body).items.map { it.toDomain() }
        }

    suspend fun getNewsItem(hash: String): IsodResult<NewsItem> =
        fetch("mynewsfull", mapOf("hash" to hash)) { body ->
            json.decodeFromString<NewsFullResponseDto>(body).items
                .firstOrNull()?.toDomain()
                ?: throw IllegalStateException("No item for hash $hash")
        }

    suspend fun getCourses(semester: String): IsodResult<List<Course>> =
        fetch("mycourses", mapOf("semester" to semester)) { body ->
            json.decodeFromString<CoursesResponseDto>(body).items.map { it.toDomain() }
        }

    suspend fun getClassDetail(classId: String): IsodResult<ClassDetail> =
        fetch("myclass", mapOf("id" to classId)) { body ->
            json.decodeFromString<ClassDetailDto>(body).toDomain()
        }

    private suspend fun <T> fetch(
        query: String,
        extraParams: Map<String, String> = emptyMap(),
        parse: (String) -> T,
    ): IsodResult<T> = try {
        val username = storage.getIsodUsername() ?: ""
        val apiKey = storage.getIsodApiKey() ?: ""
        
        if (username.isBlank() || apiKey.isBlank()) {
            return IsodResult.Error("Missing ISOD credentials")
        }

        val response = httpClient.get(BASE_URL) {
            parameter("q", query)
            parameter("username", username)
            parameter("apikey", apiKey)
            extraParams.forEach { (k, v) -> parameter(k, v) }
        }
        val body = response.bodyAsText()
        IsodResult.Success(parse(body))
    } catch (e: Exception) {
        println("❌ ISOD [$query] ${e::class.simpleName}: ${e.message}")
        IsodResult.Error(e.message ?: "Unknown error")
    }
}
