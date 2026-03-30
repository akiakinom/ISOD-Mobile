package dev.akinom.isod.data.remote

import dev.akinom.isod.data.remote.dto.CalendarResponseDto
import dev.akinom.isod.data.remote.dto.EventDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class AkinomApiClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "http://57.128.253.159:6767/api"

    suspend fun getCalendar(): CalendarResponseDto {
        return httpClient.get("$baseUrl/calendar").body()
    }

    suspend fun getEvents(): List<EventDto> {
        return httpClient.get("$baseUrl/events").body()
    }
}
