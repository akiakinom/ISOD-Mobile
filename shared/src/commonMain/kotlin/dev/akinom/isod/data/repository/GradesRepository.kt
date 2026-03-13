package dev.akinom.isod.data.repository

import dev.akinom.isod.CourseGradeEntity
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.domain.ClassGrade
import dev.akinom.isod.domain.CourseGrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private const val GRADES_TTL_MS = 15 * 60 * 1000L  // 15 minutes

class GradesRepository(
    private val db: IsodDatabase,
    private val isodApi: IsodApiClient,
    private val usosApi: UsosApiClient,
) {
    private val queries get() = db.courseGradeQueries

    fun getGrades(semester: String, usosTermId: String): Flow<List<CourseGrade>> = flow {
        val cached = queryCached(semester)
        if (cached.isNotEmpty()) {
            emit(cached)
            val lastUpdated = queries.lastUpdated(semester).executeAsOneOrNull() ?: 0L
            if (!isStale(lastUpdated, GRADES_TTL_MS)) return@flow
        }

        val fresh = fetchFresh(semester, usosTermId)
        if (fresh.isNotEmpty()) {
            val persisted = persist(semester, fresh)
            emit(persisted)
        } else if (cached.isEmpty()) {
            emit(emptyList())
        }
    }

    suspend fun refreshCourseDetails(course: CourseGrade, semester: String): CourseGrade {
        val updatedClasses = course.classGrades.map { cls ->
            val detail = when (val r = isodApi.getClassDetail(cls.classId)) {
                is IsodResult.Success -> r.data
                else -> null
            }
            cls.copy(
                credit = detail?.credit ?: cls.credit,
                columns = detail?.columns ?: cls.columns,
                summary = detail?.summary ?: cls.summary
            )
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

        val usosGrades = when (val r = usosApi.getGrades(usosTermId)) {
            is UsosResult.Success -> r.data
            else -> emptyMap()
        }

        return courses.map { course ->
            val classGrades = course.classes.map { cls ->
                ClassGrade(
                    classId   = cls.id,
                    classType = cls.type,
                    credit    = null,
                    columns   = emptyList(),
                    summary   = null,
                )
            }

            val usosGradeList = usosGrades[course.courseNumber] ?: emptyList()
            val usosFinal = usosGradeList.maxByOrNull { it.examSessionNumber ?: 0 }

            CourseGrade(
                courseId          = course.id,
                courseNumber      = course.courseNumber,
                courseName        = course.courseName,
                ects              = course.ects,
                passType          = course.passType,
                finalGrade        = course.finalGradeNumeric?.toString() ?: usosFinal?.valueSymbol,
                finalGradeComment = course.finalGradeComment ?: usosFinal?.valueDescription?.get(),
                passes            = usosFinal?.passes,
                countsIntoAverage = usosFinal?.countsIntoAverage,
                classGrades       = classGrades,
                hasIsod           = true,
                hasUsos           = usosGradeList.isNotEmpty(),
            )
        }
    }

    private fun persist(semester: String, grades: List<CourseGrade>): List<CourseGrade> {
        val now = currentTimeMillis()
        val persisted = mutableListOf<CourseGrade>()
        db.transaction {
            // We want to preserve existing class details if we have them
            val existing = queryCached(semester).associateBy { it.courseId }
            
            queries.deleteBySemester(semester)
            grades.forEach { g ->
                val classGradesToSave = if (g.classGrades.all { it.columns.isEmpty() }) {
                    existing[g.courseId]?.classGrades ?: g.classGrades
                } else {
                    g.classGrades
                }

                val gradeToPersist = g.copy(classGrades = classGradesToSave)
                persisted.add(gradeToPersist)

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
                        passes            = gradeToPersist.passes?.let { if (it) 1L else 0L },
                        countsIntoAverage = gradeToPersist.countsIntoAverage?.let { if (it) 1L else 0L },
                        classGradesJson   = json.encodeToString(gradeToPersist.classGrades),
                        hasIsod           = if (gradeToPersist.hasIsod) 1L else 0L,
                        hasUsos           = if (gradeToPersist.hasUsos) 1L else 0L,
                        lastUpdated       = now,
                    )
                )
            }
        }
        return persisted
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
    passes            = passes?.let { it == 1L },
    countsIntoAverage = countsIntoAverage?.let { it == 1L },
    classGrades       = runCatching {
        json.decodeFromString<List<ClassGrade>>(classGradesJson)
    }.getOrDefault(emptyList()),
    hasIsod           = hasIsod == 1L,
    hasUsos           = hasUsos == 1L,
)
