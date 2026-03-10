package dev.akinom.isod.data.repository

import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.domain.ClassGrade
import dev.akinom.isod.domain.CourseGrade

class GradesRepository(
    private val isodApi: IsodApiClient,
    private val usosApi: UsosApiClient,
) {
    suspend fun getGrades(semester: String, usosTermId: String): List<CourseGrade> {
        // 1. Fetch ISOD course list
        val courses = when (val r = isodApi.getCourses(semester)) {
            is IsodResult.Success -> r.data
            else -> emptyList()
        }

        // 2. Fetch USOS grades for the term (best-effort — empty if not linked)
        val usosGrades = when (val r = usosApi.getGrades(usosTermId)) {
            is UsosResult.Success -> r.data
            else -> emptyMap()
        }

        // 3. For each course fetch ISOD class detail (columns + credit)
        return courses.map { course ->
            // Build per-class partial grades
            val classGrades = course.classes.map { cls ->
                val detail = when (val r = isodApi.getClassDetail(cls.id)) {
                    is IsodResult.Success -> r.data
                    else -> null
                }
                ClassGrade(
                    classId   = cls.id,
                    classType = cls.type,
                    credit    = detail?.credit,
                    columns   = detail?.columns ?: emptyList(),
                    summary   = detail?.summary,
                )
            }

            // Match USOS grade by courseNumber == courseId
            val usosEntry = usosGrades[course.courseNumber]
            val usosFinalGrade = usosEntry?.courseGrades
                ?.maxByOrNull { it.examSessionNumber ?: 0 }

            CourseGrade(
                courseId          = course.id,
                courseNumber      = course.courseNumber,
                courseName        = course.courseName,
                ects              = course.ects,
                passType          = course.passType,
                finalGrade        = course.finalGradeNumeric?.toString()
                    ?: usosFinalGrade?.valueSymbol,
                finalGradeComment = course.finalGradeComment
                    ?: usosFinalGrade?.valueDescription?.get(),
                passes            = usosFinalGrade?.passes,
                countsIntoAverage = usosFinalGrade?.countsIntoAverage,
                classGrades       = classGrades,
                hasIsod           = true,
                hasUsos           = usosEntry != null,
            )
        }
    }
}
