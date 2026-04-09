package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.akinom.isod.CourseGradeEntity
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.data.remote.dto.removeTitles
import dev.akinom.isod.data.remote.dto.toClassType
import dev.akinom.isod.domain.ClassGrade
import dev.akinom.isod.domain.CourseGrade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private const val GRADES_TTL_MS = 15 * 60 * 1000L  // 15 minutes

class GradesRepository(
    private val db: IsodDatabase,
    private val isodApi: IsodApiClient,
    private val usosApi: UsosApiClient,
    private val scope: CoroutineScope,
) {
    private val queries get() = db.courseGradeQueries

    fun getGrades(semester: String, usosTermId: String): Flow<List<CourseGrade>> =
        queries.selectBySemester(semester)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshGradesIfStale(semester, usosTermId) }

    private fun refreshGradesIfStale(semester: String, usosTermId: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = queries.lastUpdated(semester).executeAsOneOrNull() ?: 0L
            if (isStale(lastUpdated, GRADES_TTL_MS)) {
                refreshGrades(semester, usosTermId)
            }
        }
    }

    suspend fun refreshGrades(semester: String, usosTermId: String, forceDetails: Boolean = false) {
        val fresh = fetchFresh(semester, usosTermId)
        if (fresh.isNotEmpty()) {
            persist(semester, fresh, forceDetails)
        }
    }

    suspend fun refreshCourseDetails(course: CourseGrade, semester: String): CourseGrade {
        val updatedClasses = course.classGrades.map { cls ->
            val detail = when (val r = isodApi.getClassDetail(cls.classId)) {
                is IsodResult.Success -> r.data
                else -> null
            }
            if (detail != null) {
                cls.copy(
                    credit = detail.credit ?: cls.credit,
                    columns = detail.columns,
                    summary = detail.summary ?: cls.summary,
                    summaryNotes = detail.summaryNotes ?: cls.summaryNotes,
                    summaryModifiedBy = detail.summaryModifiedBy ?: cls.summaryModifiedBy,
                    announcements = detail.announcements,
                    teachers = detail.header.teachers.removeTitles(),
                    place = detail.header.place,
                    day = detail.header.day,
                    time = "${detail.header.timeFrom} - ${detail.header.timeTo}"
                )
            } else {
                cls
            }
        }
        val updatedCourse = course.copy(classGrades = updatedClasses)
        
        queries.updateClassGrades(
            classGradesJson = json.encodeToString(updatedClasses),
            courseId = course.courseId,
            semester = semester
        )
        
        return updatedCourse
    }

    private fun queryCached(semester: String): List<CourseGrade> =
        queries.selectBySemester(semester).executeAsList().map { it.toDomain() }

    private suspend fun fetchFresh(semester: String, usosTermId: String): List<CourseGrade> {
        val courses = when (val r = isodApi.getCourses(semester)) {
            is IsodResult.Success -> r.data
            else -> return emptyList()
        }

        return courses.map { course ->
            val classGrades = course.classes.map { cls ->
                ClassGrade(
                    classId   = cls.id,
                    classType = cls.type,
                    credit    = null,
                    columns   = emptyList(),
                    summary   = null,
                    summaryNotes = null,
                    summaryModifiedBy = null,
                    announcements = emptyList()
                )
            }

            CourseGrade(
                courseId          = course.id,
                courseNumber      = course.courseNumber,
                courseName        = course.courseName,
                ects              = course.ects,
                passType          = course.passType,
                finalGrade        = course.finalGradeNumeric?.toString(),
                finalGradeComment = course.finalGradeComment,
                classGrades       = classGrades,
            )
        }
    }

    private fun persist(semester: String, grades: List<CourseGrade>, forceDetails: Boolean = false) {
        val now = currentTimeMillis()
        db.transaction {
            val existing = if (forceDetails) emptyMap() else queryCached(semester).associateBy { it.courseId }
            
            queries.deleteBySemester(semester)
            grades.forEach { g ->
                val classGradesToSave = if (g.classGrades.all { it.columns.isEmpty() }) {
                    existing[g.courseId]?.classGrades ?: g.classGrades
                } else {
                    g.classGrades
                }

                val gradeToPersist = g.copy(classGrades = classGradesToSave)

                queries.upsert(
                    CourseGradeEntity(
                        courseId          = gradeToPersist.courseId,
                        semester          = semester,
                        courseNumber      = gradeToPersist.courseNumber,
                        courseName        = gradeToPersist.courseName,
                        ects              = gradeToPersist.ects.toLong(),
                        passType          = gradeToPersist.passType,
                        finalGrade        = gradeToPersist.finalGrade,
                        finalGradeComment = gradeToPersist.finalGradeComment,
                        classGradesJson   = json.encodeToString(gradeToPersist.classGrades),
                        lastUpdated       = now,
                    )
                )
            }
        }
    }
}

private fun CourseGradeEntity.toDomain() = CourseGrade(
    courseId          = courseId,
    courseNumber      = courseNumber,
    courseName        = courseName,
    ects              = ects.toInt(),
    passType          = passType,
    finalGrade        = finalGrade,
    finalGradeComment = finalGradeComment,
    classGrades       = runCatching {
        json.decodeFromString<List<ClassGrade>>(classGradesJson)
    }.getOrDefault(emptyList()),
)
