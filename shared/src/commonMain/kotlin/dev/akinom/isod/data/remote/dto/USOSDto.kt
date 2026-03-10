package dev.akinom.isod.data.remote.dto

import dev.akinom.isod.domain.LangDict
import dev.akinom.isod.domain.UsosActivity
import dev.akinom.isod.domain.UsosUserInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

typealias TimetableResponseDto = List<UsosActivityDto>

@Serializable
data class UsosActivityDto(
    val type: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time")   val endTime: String = "",
    val name: LangDict = LangDict(),
    val url: String? = null,

    @SerialName("course_id")              val courseId: String? = null,
    @SerialName("course_name")            val courseName: LangDict? = null,
    @SerialName("classtype_name")         val classtypeName: LangDict? = null,
    @SerialName("lecturer_ids")           val lecturerIds: List<Long> = emptyList(),
    @SerialName("group_number")           val groupNumber: Int? = null,
    @SerialName("building_name")          val buildingName: LangDict? = null,
    @SerialName("building_id")            val buildingId: String? = null,
    @SerialName("room_number")            val roomNumber: String? = null,
    @SerialName("room_id")                val roomId: Int? = null,
    @SerialName("unit_id")                val unitId: Long? = null,
    @SerialName("classtype_id")           val classtypeId: String? = null,
    val frequency: String? = null,
) {
    fun toDomain() = UsosActivity(
        type          = type,
        startTime     = startTime,
        endTime       = endTime,
        name          = name,
        url           = url,
        courseId      = courseId,
        courseName    = courseName,
        classtypeName = classtypeName,
        lecturerIds   = lecturerIds,
        lecturers     = emptyList(),
        groupNumber   = groupNumber,
        buildingName  = buildingName,
        buildingId    = buildingId,
        roomNumber    = roomNumber,
        roomId        = roomId,
        unitId        = unitId,
        classtypeId   = classtypeId,
        frequency     = frequency,
    )
}

@Serializable
data class UsosUserInfoDto(
    val id: String = "",
    @SerialName("first_name")     val firstName: String = "",
    @SerialName("last_name")      val lastName: String = "",
    @SerialName("student_number") val studentNumber: String? = null,
    @SerialName("photo_urls")     val photoUrls: Map<String, String>? = null,
) {
    fun toDomain() = UsosUserInfo(
        id            = id,
        firstName     = firstName,
        lastName      = lastName,
        studentNumber = studentNumber,
        photoUrl      = photoUrls?.get("50x50"),
    )
}
@Serializable
data class UsosGradeDto(
    @SerialName("value_symbol")        val valueSymbol: String? = null,
    @SerialName("passes")              val passes: Boolean? = null,
    @SerialName("value_description")   val valueDescription: LangDict? = null,
    @SerialName("counts_into_average") val countsIntoAverage: Boolean? = null,
    @SerialName("comment")             val comment: String? = null,
    @SerialName("exam_id")             val examId: String? = null,
    @SerialName("exam_session_number") val examSessionNumber: Int? = null,
)

@Serializable
data class UsosCourseEditionGradesDto(
    @SerialName("course_grades")       val courseGrades: List<UsosGradeDto> = emptyList(),
    @SerialName("course_units_grades") val courseUnitsGrades: JsonObject? = null,
)