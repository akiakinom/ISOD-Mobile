package dev.akinom.isod.domain

import kotlinx.datetime.LocalDate
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
    val typeOfClasses: ClassType,
)

data class NewsHeader(
    val id: String,
    val title: String,
    val date: LocalDate?,
    val author: String,
    val type: NewsType,
    val label: String,
)

data class NewsItem(
    val id: String,
    val title: String,
    val content: String,
    val date: LocalDate?,
    val author: String,
    val type: NewsType,
    val label: String,
)

@Serializable
data class NewsAttachment(
    val filename: String,
    val size: Long,
)

@Serializable
enum class NewsType {
    IMPORTANT,
    GRADE,
    CLASS,
    DEANS_OFFICE,
    FACULTY_STUDENT_COUNCIL,
    TIMETABLE_UPDATE,
    OTHER
}

@Serializable
enum class ClassType {
    LECTURE,
    LABORATORY,
    EXERCISES,
    PROJECT,
    SEMINAR,
    PHYSICAL_EDUCATION,
    OTHER
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
    val type: ClassType,
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
    val studentNo: String?,
    val usosId: String?,
    val username: String?,
    val firstname: String?,
    val lastname: String?,
    val summaryModifiedBy: String?,
)

@Serializable
data class ClassHeader(
    val id: String,
    val courseNumber: String,
    val courseName: String,
    val type: ClassType,
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
    val valueNote: String?,
    val weight: Double,
    val accounted: Boolean,
    val date: String?,
    val dateModified: String?,
    val personModifying: String?,
    val personModifyingTitle: String?,
    val indexOrder: Int,
)
