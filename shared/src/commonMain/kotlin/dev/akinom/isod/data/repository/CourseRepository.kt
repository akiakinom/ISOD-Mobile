package dev.akinom.isod.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.akinom.isod.ClassDetailEntity
import dev.akinom.isod.CourseEntity
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.data.cache.CacheConfig
import dev.akinom.isod.data.cache.currentTimeMillis
import dev.akinom.isod.data.cache.isStale
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.IsodResult
import dev.akinom.isod.domain.ClassAnnouncement
import dev.akinom.isod.domain.ClassColumn
import dev.akinom.isod.domain.ClassDetail
import dev.akinom.isod.domain.ClassHeader
import dev.akinom.isod.domain.Course
import dev.akinom.isod.domain.CourseClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class CourseRepository(
    private val db: IsodDatabase,
    private val api: IsodApiClient,
    private val scope: CoroutineScope,
) {
    private val courseQueries      = db.courseQueries
    private val classDetailQueries = db.courseQueries

    // ── Courses ───────────────────────────────────────────────────────────────

    fun getCourses(semester: String): Flow<List<Course>> =
        courseQueries.selectAllCourses(semester)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }
            .onStart { refreshCoursesIfStale(semester) }

    suspend fun refreshCourses(semester: String) {
        when (val result = api.getCourses(semester)) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                db.transaction {
                    courseQueries.deleteAllCourses(semester)
                    result.data.forEach { course ->
                        courseQueries.upsertCourse(course.toEntity(semester, now))
                    }
                }
            }
            is IsodResult.Error -> println("⚠️ CourseRepository refresh failed: ${result.message}")
        }
    }

    // ── Class Detail ──────────────────────────────────────────────────────────

    fun getClassDetail(classId: String): Flow<ClassDetail?> =
        classDetailQueries.selectClassDetail(classId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
            .onStart { refreshClassDetailIfStale(classId) }

    suspend fun refreshClassDetail(classId: String) {
        when (val result = api.getClassDetail(classId)) {
            is IsodResult.Success -> {
                val now = currentTimeMillis()
                classDetailQueries.upsertClassDetail(result.data.toEntity(now))
            }
            is IsodResult.Error -> println("⚠️ CourseRepository class detail refresh failed: ${result.message}")
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun refreshCoursesIfStale(semester: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = courseQueries.coursesLastUpdated(semester).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.COURSES_TTL_MS)) {
                refreshCourses(semester)
            }
        }
    }

    private fun refreshClassDetailIfStale(classId: String) {
        scope.launch(Dispatchers.IO) {
            val lastUpdated = classDetailQueries.classDetailLastUpdated(classId).executeAsOneOrNull()
            if (lastUpdated == null || isStale(lastUpdated, CacheConfig.CLASS_TTL_MS)) {
                refreshClassDetail(classId)
            }
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun CourseEntity.toDomain() = Course(
    id                = id,
    courseNumber      = courseNumber,
    courseName        = courseName,
    courseVersion     = courseVersion,
    passType          = passType,
    courseManager     = courseManager,
    hours             = hours,
    ects              = ects.toInt(),
    finalGradeNumeric = finalGradeNumeric,
    finalGradeComment = finalGradeComment,
    classes           = json.decodeFromString<List<CourseClass>>(classes),
)

private fun Course.toEntity(semester: String, now: Long) = CourseEntity(
    id                = id,
    courseNumber      = courseNumber,
    courseName        = courseName,
    courseVersion     = courseVersion,
    passType          = passType,
    courseManager     = courseManager,
    hours             = hours,
    ects              = ects.toLong(),
    finalGradeNumeric = finalGradeNumeric,
    finalGradeComment = finalGradeComment,
    classes           = json.encodeToString(classes),
    semester          = semester,
    lastUpdated       = now,
)

private fun ClassDetailEntity.toDomain() = ClassDetail(
    id               = id,
    header           = json.decodeFromString<ClassHeader>(headerJson),
    announcements    = json.decodeFromString<List<ClassAnnouncement>>(announcementsJson),
    columns          = json.decodeFromString<List<ClassColumn>>(columnsJson),
    summary          = summary,
    summaryNotes     = summaryNotes,
    credit           = credit,
    creditModifiedBy = creditModifiedBy,
    semester         = semester,
    studentNo        = studentNo,
    usosId           = usosId,
    username         = username,
    firstname        = firstname,
    lastname         = lastname,
    summaryModifiedBy = summaryModifiedBy,
)

private fun ClassDetail.toEntity(now: Long) = ClassDetailEntity(
    id                = id,
    headerJson        = json.encodeToString(header),
    announcementsJson = json.encodeToString(announcements),
    columnsJson       = json.encodeToString(columns),
    summary           = summary,
    summaryNotes      = summaryNotes,
    credit            = credit,
    creditModifiedBy  = creditModifiedBy,
    semester          = semester,
    studentNo         = studentNo,
    usosId            = usosId,
    username          = username,
    firstname         = firstname,
    lastname          = lastname,
    summaryModifiedBy = summaryModifiedBy,
    lastUpdated       = now,
)
