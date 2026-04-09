package dev.akinom.isod.domain

import kotlinx.serialization.Serializable

@Serializable
data class CourseGrade(
    val courseId: String,          // ISOD course id
    val courseNumber: String,
    val courseName: String,
    val ects: Int,
    val passType: String,          // "Egzamin", "Zaliczenie", etc.

    // Final grade — from ISOD finalGradeNumeric or USOS value_symbol
    val finalGrade: String?,
    val finalGradeComment: String?,

    // ISOD partial grades per class session
    val classGrades: List<ClassGrade>,
) {
    val displayFinalGrade: String? get() = if (finalGrade == "0.0") "NZAL" else finalGrade
}

@Serializable
data class ClassGrade(
    val classId: String,
    val classType: ClassType,
    val credit: String?,           // final credit for this class session
    val columns: List<ClassColumn>,
    val summary: String?,
    val summaryNotes: String?,
    val summaryModifiedBy: String?,
    val announcements: List<ClassAnnouncement> = emptyList(),
    val teachers: String? = null,
    val place: String? = null,
    val day: String? = null,
    val time: String? = null,
) {
    val displayCredit: String? get() = if (credit == "0.0") "NZAL" else credit
}
