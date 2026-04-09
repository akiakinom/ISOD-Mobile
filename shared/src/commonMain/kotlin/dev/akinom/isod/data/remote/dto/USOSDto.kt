package dev.akinom.isod.data.remote.dto

import dev.akinom.isod.domain.UsosClass
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
data class UsosClassDto(
    @SerialName("start_time")             val startTime: String = "",
    @SerialName("end_time")               val endTime: String = "",
    @SerialName("course_id")              val courseId: String = "",
    @SerialName("course_name")            val courseName: JsonObject = JsonObject(emptyMap()),
    @SerialName("lecturer_ids")           val lecturerIds: List<Int> = emptyList(),
    @SerialName("building_id")            val buildingId: String? = null,
    @SerialName("room_number")            val roomNumber: String? = null,
    @SerialName("classtype_name")         val classtypeName: JsonObject = JsonObject(emptyMap())
) {
    fun toDomain() = UsosClass(
        type          = classtypeName["pl"].toString().toClassType(),
        startTime     = startTime.drop(11).dropLast(3), // Drop "yyyy-MM-dd "
        endTime       = endTime.drop(11).dropLast(3),
        name          = courseName["pl"].toString().trim('"'),
        lecturers     = emptyList(),
        lecturersId   = lecturerIds,
        building      = buildingId?.drop(5),
        roomNumber    = roomNumber,
        id            = courseId,
        dayOfWeek     = startTime.toDayOfWeek()
    )
}