package dev.akinom.isod.domain

import kotlinx.serialization.Serializable

data class PlanItem(
    val id: Long,
    val courseName: String,
    val courseNameShort: String,
    val courseNumber: String,
    val courseVersion: String,
    val teachers: List<String>,
    val startTime: String,         // "04:15:00 PM"
    val endTime: String,
    val dayOfWeek: Int,            // 1=Mon … 7=Sun (ISO)
    val cycle: String,
    val cycleShort: String,
    val groups: List<String>,
    val building: String,
    val buildingShort: String,
    val room: String,
    val typeOfClasses: String,     // "W", "L", "C", "P"
)

data class NewsHeader(
    val hash: String,
    val subject: String,
    val modifiedDate: String,
    val modifiedBy: String,
    val type: NewsType,
    val noAttachments: Int,
)

data class NewsItem(
    val hash: String,
    val subject: String,
    val content: String,
    val modifiedDate: String,
    val modifiedBy: String,
    val type: NewsType,
    val attachments: List<NewsAttachment>,
)

@Serializable
data class NewsAttachment(
    val filename: String,
    val size: Long,
)

enum class NewsType(val code: String) {
    ANNOUNCEMENT("1000"),
    QUIZ("1001"),
    IMPORTANT("1002"),
    PROJECT_STATUS("1003"),
    PROJECT_GROUP_CHANGE("1004"),
    CLASS_ENROLLMENT("1005"),
    UNKNOWN("-1");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

data class Course(
    val courseNumber: String,
    val courseName: String,
    val courseVersion: String,
    val passType: String,
    val courseManager: String,
    val hours: String,
    val ects: Int,
    val finalGradeNumeric: Double?,
    val finalGradeComment: String?,
    val id: String,
    val classes: List<CourseClass>,
)

@Serializable
data class CourseClass(
    val id: String,
    val courseNumber: String,
    val courseName: String,
    val type: String,              // "W", "L", "C", etc.
    val hours: Int,
    val day: String,
    val timeFrom: String,
    val timeTo: String,
    val cycle: String,
    val groups: String,
    val place: String,
    val teachers: String,
    val academicSemester: String,
    val enrollmentStatus: String,
)

data class ClassDetail(
    val id: String,
    val header: ClassHeader,
    val announcements: List<ClassAnnouncement>,
    val columns: List<ClassColumn>,
    val summary: String?,
    val summaryNotes: String?,
    val credit: String?,
    val creditModifiedBy: String?,
    val semester: String,
)

@Serializable
data class ClassHeader(
    val courseNumber: String,
    val courseName: String,
    val type: String,
    val hours: Int,
    val day: String,
    val timeFrom: String,
    val timeTo: String,
    val cycle: String,
    val groups: String,
    val place: String,
    val teachers: String,
    val academicSemester: String,
)

@Serializable
data class ClassAnnouncement(
    val title: String,
    val content: String,
    val author: String,
    val dateModified: String,
    val dateExpired: String?,
)

@Serializable
data class ClassColumn(
    val name: String?,
    val type: String,
    val value: String?,
    val weight: Double,
    val accounted: Boolean,
    val date: String?,
    val personModifying: String?,
)