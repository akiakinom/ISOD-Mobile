package dev.akinom.isod.domain

import kotlinx.serialization.Serializable

@Serializable
data class LangDict(
    val pl: String = "",
    val en: String = "",
) {
    fun get(lang: String = "pl"): String = when (lang) {
        "en" -> en.ifBlank { pl }
        else -> pl.ifBlank { en }
    }
}

@Serializable
data class UsosActivity(
    val type: String,                  // "classgroup", "classgroup2", "meeting", "exam"
    val startTime: String,             // "yyyy-mm-dd hh:mm:ss"
    val endTime: String,
    val name: LangDict,
    val url: String? = null,

    // classgroup / classgroup2 fields
    val courseId: String? = null,
    val courseName: LangDict? = null,
    val classtypeName: LangDict? = null,
    val lecturerIds: List<Long> = emptyList(),
    val lecturers: List<String> = emptyList(),
    val groupNumber: Int? = null,
    val buildingName: LangDict? = null,
    val buildingId: String? = null,
    val roomNumber: String? = null,
    val roomId: Int? = null,
    val unitId: Long? = null,
    val classtypeId: String? = null,
    val frequency: String? = null,
)

@Serializable
data class UsosUserInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val studentNumber: String? = null,
    val photoUrl: String? = null,
)
