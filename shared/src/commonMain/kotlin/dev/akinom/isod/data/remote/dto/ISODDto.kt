package dev.akinom.isod.data.remote.dto

import dev.akinom.isod.domain.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun String.removeTitles(): String {
    val titles = listOf(
        "prof. dr hab. inż.", "prof. dr hab.", "dr hab. inż.", "dr hab.", "dr inż.", 
        "mgr inż.", "dr.", "prof.", "mgr.", "inz.", "inż.", "mgr", "hab.", "dr"
    )
    var result = this
    titles.forEach { title ->
        val pattern = "(?i)^$title\\s+".toRegex()
        result = result.replace(pattern, "").trim()
    }
    return result
}

@Serializable
data class PlanResponseDto(
    val username: String,
    val semester: String,
    val firstname: String,
    val lastname: String,
    val planItems: List<PlanItemDto> = emptyList(),
)


@Serializable
data class PlanItemDto(
    val id: Long,
    val courseName: String,
    val courseNameShort: String = "",
    val courseNumber: String,
    val courseVersion: String = "",
    val teachers: List<String> = emptyList(),
    val startTime: String,
    val endTime: String,
    val dayOfWeek: String,
    val cycle: String = "",
    val cycleShort: String = "",
    val groups: List<String> = emptyList(),
    val building: String = "",
    val buildingShort: String = "",
    val room: String = "",
    val typeOfClasses: String = "",
) {
    fun toDomain() = PlanItem(
        id            = id,
        courseName    = courseName,
        courseNameShort = courseNameShort,
        courseNumber  = courseNumber,
        courseVersion = courseVersion,
        teachers      = teachers.map { it.removeTitles() },
        startTime     = startTime,
        endTime       = endTime,
        dayOfWeek     = dayOfWeek.toIntOrNull() ?: 0,
        cycle         = cycle,
        cycleShort    = cycleShort,
        groups        = groups,
        building      = building,
        buildingShort = buildingShort,
        room          = room,
        typeOfClasses = typeOfClasses.toClassType(),
    )
}

@Serializable
data class NewsHeadersResponseDto(
    val items: List<NewsHeaderDto> = emptyList(),
    val username: String = "",
    val semester: String = "",
)

fun String.toNewsType(type: Int = -1): NewsType {
    return when {
        startsWith("Zajęcia -") -> NewsType.GRADE
        startsWith("Ogłoszenie -") -> NewsType.CLASS
        startsWith("[DZIEKANAT]") -> NewsType.DEANS_OFFICE
        startsWith("Informacja o zapisach") -> NewsType.TIMETABLE_UPDATE
        startsWith("[WRS]") -> NewsType.FACULTY_STUDENT_COUNCIL
        else -> NewsType.OTHER
    }
}

fun String.toClassType(): ClassType {
    val upper = this.uppercase()
    return when {
        upper.contains("WYCHOWANIE FIZYCZNE") || upper == "WF" -> ClassType.PHYSICAL_EDUCATION
        upper.contains("WYKŁAD") || upper.contains("LECTURE") || upper == "W" -> ClassType.LECTURE
        upper.contains("LABORATORIUM") || upper.contains("LABORATORY") || upper == "L" -> ClassType.LABORATORY
        upper.contains("ĆWICZENIA") || upper.contains("EXERCISE") || upper == "C" || upper == "Ć" -> ClassType.EXERCISES
        upper.contains("PROJEKT") || upper.contains("PROJECT") || upper == "P" -> ClassType.PROJECT
        upper.contains("SEMINARIUM") || upper.contains("SEMINAR") || upper == "S" -> ClassType.SEMINAR
        else -> ClassType.OTHER
    }
}

fun String.toNewsLabel(): String {
    return when {
        startsWith("Zajęcia -") || startsWith("Ogłoszenie -") -> {
            substringAfter("- ").substringBefore(":").trim()
        }
        startsWith("[WRS]") || startsWith("[DZIEKANAT]") -> ""
        Regex("^\\[([A-Z0-9+]+)]").containsMatchIn(this) -> {
            Regex("^\\[([A-Z0-9+]+)]").find(this)!!.groupValues[1].lowercase()
                .replaceFirstChar { it.uppercase() }
        }
        else -> ""
    }
}

fun String.toNewsTitle(): String {
    return when {
        startsWith("Zajęcia -") -> substringAfter("Zajęcia -").substringAfter(":").trim()
        startsWith("Ogłoszenie -") -> substringAfter("Ogłoszenie -").substringAfter(":").trim()
        startsWith("[DZIEKANAT]") -> removePrefix("[DZIEKANAT]").trim()
        startsWith("[WRS]") -> removePrefix("[WRS]").trim()
        startsWith("Informacja o zapisach") -> this
        else -> replace(Regex("^\\[([A-Z0-9+]+)]\\s*"), "")
    }.replaceFirstChar { it.uppercase() }
}

fun String.toLocalDateTime(): LocalDateTime? = runCatching {
    val (datePart, timePart) = split(" ")
    val (day, month, year) = datePart.split(".").map { it.toInt() }
    val (hour, minute) = timePart.split(":").map { it.toInt() }
    LocalDateTime(year, month, day, hour, minute)
}.getOrNull()

@Serializable
data class NewsHeaderDto(
    val hash: String,
    val subject: String,
    val modifiedDate: String = "",
    val modifiedBy: String = "",
    val type: Int = -1,
    val noAttachments: Int = 0,
) {

    fun toDomain() = NewsHeader(
        id = hash,
        title = subject.toNewsTitle(),
        date = modifiedDate.toLocalDateTime(),
        author = modifiedBy.removeTitles(),
        type = subject.toNewsType(type),
        label = subject.toNewsLabel()
    )
}

@Serializable
data class NewsFullResponseDto(
    val items: List<NewsItemDto> = emptyList(),
    val username: String = "",
    val semester: String = "",
)

@Serializable
data class NewsItemDto(
    val hash: String,
    val subject: String,
    val content: String = "",
    val modifiedDate: String = "",
    val modifiedBy: String = "",
    val type: Int = -1,
    val attachments: List<NewsAttachmentDto> = emptyList(),
    val noAttachments: Int = 0,
) {
    fun toDomain() = NewsItem(
        id = hash,
        title = subject.toNewsTitle(),
        content = content,
        date = modifiedDate.toLocalDateTime(),
        author = modifiedBy.removeTitles(),
        type = subject.toNewsType(type),
        label = subject.toNewsLabel()
    )
}

@Serializable
data class NewsAttachmentDto(
    val filename: String,
    val size: Long = 0,
) {
    fun toDomain() = NewsAttachment(filename = filename, size = size)
}

@Serializable
data class CoursesResponseDto(
    val items: List<CourseDto> = emptyList(),
    val reserveListItems: List<CourseDto> = emptyList(),
    val username: String = "",
    val semester: String = "",
)

@Serializable
data class CourseDto(
    val courseNumber: String,
    val courseName: String,
    val courseVersion: String = "",
    val passType: String = "",
    val courseManager: String = "",
    val hours: String = "",
    @SerialName("ECTS") val ects: Int = 0,
    val finalGradeNumeric: Double? = null,
    val finalGradeComment: String? = null,
    val id: String,
    val classes: List<CourseClassDto> = emptyList(),
) {
    fun toDomain() = Course(
        courseNumber      = courseNumber,
        courseName        = courseName,
        courseVersion     = courseVersion,
        passType          = passType,
        courseManager     = courseManager,
        hours             = hours,
        ects              = ects,
        finalGradeNumeric = finalGradeNumeric,
        finalGradeComment = finalGradeComment,
        id                = id,
        classes           = classes.map { it.toDomain() },
    )
}

@Serializable
data class CourseClassDto(
    val id: String,
    val courseNumber: String,
    val courseName: String,
    val type: String = "",
    val hours: Int = 0,
    val day: String = "",
    val timeFrom: String = "",
    val timeTo: String = "",
    val cycle: String = "",
    val groups: String = "",
    val place: String = "",
    val teachers: String = "",
    val academicSemester: String = "",
    val enrollmentStatus: String = "",
) {
    fun toDomain() = CourseClass(
        id               = id,
        courseNumber     = courseNumber,
        courseName       = courseName,
        type             = type.toClassType(),
        hours            = hours,
        day              = day,
        timeFrom         = timeFrom,
        timeTo           = timeTo,
        cycle            = cycle,
        groups           = groups,
        place            = place,
        teachers         = teachers,
        academicSemester = academicSemester,
        enrollmentStatus = enrollmentStatus,
    )
}

@Serializable
data class ClassDetailDto(
    val id: String,
    val header: ClassHeaderDto,
    val announcements: List<ClassAnnouncementDto> = emptyList(),
    val columns: List<ClassColumnDto> = emptyList(),
    val summary: String? = null,
    val summaryNotes: String? = null,
    val credit: String? = null,
    val creditModifiedBy: String? = null,
    val semester: String = "",
    val studentNo: String? = null,
    val usosId: String? = null,
    val username: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val summaryModifiedBy: String? = null,
) {
    fun toDomain() = ClassDetail(
        id              = id,
        header          = header.toDomain(),
        announcements   = announcements.map { it.toDomain() },
        columns         = columns.map { it.toDomain() },
        summary         = summary,
        summaryNotes    = summaryNotes,
        credit          = credit,
        creditModifiedBy = creditModifiedBy,
        semester        = semester,
        studentNo       = studentNo,
        usosId          = usosId,
        username        = username,
        firstname       = firstname,
        lastname        = lastname,
        summaryModifiedBy = summaryModifiedBy,
    )
}

@Serializable
data class ClassHeaderDto(
    val id: String = "",
    val courseNumber: String,
    val courseName: String,
    val type: String = "",
    val hours: Int = 0,
    val day: String = "",
    val timeFrom: String = "",
    val timeTo: String = "",
    val cycle: String = "",
    val groups: String = "",
    val place: String = "",
    val teachers: String = "",
    val academicSemester: String = "",
) {
    fun toDomain() = ClassHeader(
        id               = id,
        courseNumber     = courseNumber,
        courseName       = courseName,
        type             = type.toClassType(),
        hours            = hours,
        day              = day,
        timeFrom         = timeFrom,
        timeTo           = timeTo,
        cycle            = cycle,
        groups           = groups,
        place            = place,
        teachers         = teachers,
        academicSemester = academicSemester,
    )
}

@Serializable
data class ClassAnnouncementDto(
    val title: String,
    val content: String = "",
    val author: String = "",
    val dateModified: String = "",
    val dateExpired: String? = null,
) {
    fun toDomain() = ClassAnnouncement(
        title        = title,
        content      = content,
        author       = author,
        dateModified = dateModified,
        dateExpired  = dateExpired,
    )
}

@Serializable
data class ClassColumnDto(
    val name: String? = null,
    val type: String = "",
    val value: String? = null,
    val valueNote: String? = null,
    val weight: Double = 1.0,
    val accounted: Boolean = false,
    val date: String? = null,
    val dateModified: String? = null,
    val personModifying: String? = null,
    val personModifyingTitle: String? = null,
    val indexOrder: Int = 0,
) {
    fun toDomain() = ClassColumn(
        name            = name,
        type            = type,
        value           = value,
        valueNote       = valueNote,
        weight          = weight,
        accounted       = accounted,
        date            = date,
        dateModified    = dateModified,
        personModifying = personModifying,
        personModifyingTitle = personModifyingTitle,
        indexOrder      = indexOrder,
    )
}
