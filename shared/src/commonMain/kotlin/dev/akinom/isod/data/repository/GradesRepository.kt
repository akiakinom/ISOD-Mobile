package dev.akinom.isod.data.repository

import dev.akinom.isod.CourseGradeEntity
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.remote.UsosResult
import dev.akinom.isod.data.remote.dto.UsosGradeDto
import dev.akinom.isod.domain.ClassGrade
import dev.akinom.isod.domain.CourseGrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private const val GRADES_TTL_MS = 5 * 60 * 1000L  // 5 minutes

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
            persist(semester, fresh)
            emit(fresh)
        } else if (cached.isEmpty()) {
            emit(emptyList())
        }
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

    private fun persist(semester: String, grades: List<CourseGrade>) {
        val now = currentTimeMillis()
        db.transaction {
            queries.deleteBySemester(semester)
            grades.forEach { g ->
                queries.upsert(
                    CourseGradeEntity(
                        courseId          = g.courseId,
                        semester          = semester,
                        courseNumber      = g.courseNumber,
                        courseName        = g.courseName,
                        ects              = g.ects.toLong(),
                        passType          = g.passType,
                        finalGrade        = g.finalGrade,
                        finalGradeComment = g.finalGradeComment,
                        passes            = g.passes?.let { if (it) 1L else 0L },
                        countsIntoAverage = g.countsIntoAverage?.let { if (it) 1L else 0L },
                        classGradesJson   = json.encodeToString(g.classGrades),
                        hasIsod           = if (g.hasIsod) 1L else 0L,
                        hasUsos           = if (g.hasUsos) 1L else 0L,
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
    passes            = passes?.let { it == 1L },
    countsIntoAverage = countsIntoAverage?.let { it == 1L },
    classGrades       = runCatching {
        json.decodeFromString<List<ClassGrade>>(classGradesJson)
    }.getOrDefault(emptyList()),
    hasIsod           = hasIsod == 1L,
    hasUsos           = hasUsos == 1L,
)