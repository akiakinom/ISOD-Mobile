package dev.akinom.isod.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarResponseDto(
    @SerialName("semesters") val semesters: List<SemesterDto>,
    @SerialName("substitutions") val substitutions: List<SubstitutionDto>,
    @SerialName("breaks") val breaks: List<BreakDto>,
    @SerialName("exams") val exams: List<ExamDto>,
    @SerialName("deans") val deans: List<DeanDto>
)

@Serializable
data class SemesterDto(
    @SerialName("id") val id: Int,
    @SerialName("semester_id") val semesterId: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("is_published") val isPublished: Int,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: String
)

@Serializable
data class SubstitutionDto(
    @SerialName("id") val id: Int,
    @SerialName("original_date") val originalDate: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("is_published") val isPublished: Int,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: String
)

@Serializable
data class BreakDto(
    @SerialName("id") val id: Int,
    @SerialName("type") val type: String,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    @SerialName("is_published") val isPublished: Int,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: String
)

@Serializable
data class ExamDto(
    @SerialName("id") val id: Int,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    @SerialName("is_published") val isPublished: Int,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: String
)

@Serializable
data class DeanDto(
    @SerialName("id") val id: Int,
    @SerialName("date") val date: String,
    @SerialName("time_from") val timeFrom: String,
    @SerialName("time_to") val timeTo: String,
    @SerialName("is_published") val isPublished: Int,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: String
)

@Serializable
data class EventDto(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("location") val location: String?,
    @SerialName("event_date") val eventDate: String,
    @SerialName("description") val description: String?,
    @SerialName("image") val image: String?,
    @SerialName("is_published") val isPublished: Int,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("author") val author: String
) {
    fun getImageUrl(): String? = image?.let { "http://57.128.253.159:6767/uploads/$it" }
}
