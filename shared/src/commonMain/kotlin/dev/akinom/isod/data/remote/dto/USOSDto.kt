package dev.akinom.isod.data.remote.dto

import dev.akinom.isod.domain.UsosActivity
import dev.akinom.isod.domain.UsosUserInfo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

fun String.toDayOfWeek(): Int {
    val isoString = this.replace(" ", "T")
    val dateTime = LocalDateTime.parse(isoString)
    return dateTime.dayOfWeek.isoDayNumber
}

@Serializable
data class UsosActivityDto(
    @SerialName("start_time")             val startTime: String = "",
    @SerialName("end_time")               val endTime: String = "",
    @SerialName("course_id")              val courseId: String = "",
    @SerialName("course_name")            val courseName: JsonObject = JsonObject(emptyMap()),
    @SerialName("lecturer_ids")           val lecturerIds: List<Long> = emptyList(),
    @SerialName("building_id")            val buildingId: String? = null,
    @SerialName("room_number")            val roomNumber: String? = null,
) {
    fun toDomain() = UsosActivity(
        type          = courseName["pl"].toString().split(" - ").last().toClassType(),
        startTime     = startTime.drop(11), // Drop "yyyy-MM-dd "
        endTime       = endTime,
        name          = courseName["pl"].toString(),
        lecturers     = emptyList(),
        building      = buildingId?.drop(5),
        roomNumber    = roomNumber,
        id            = courseId,
        dayOfWeek     = startTime.toDayOfWeek()
    )
}

@Serializable
data class UsosUserInfoDto(
    val id: String = "",
    @SerialName("first_name")     val firstName: String = "",
    @SerialName("last_name")      val lastName: String = "",
) {
    fun toDomain() = UsosUserInfo(
        id            = id,
        name          = "$firstName $lastName",
    )
}