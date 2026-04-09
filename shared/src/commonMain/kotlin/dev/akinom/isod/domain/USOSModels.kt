package dev.akinom.isod.domain

import kotlinx.serialization.Serializable

@Serializable
data class UsosActivity(
    val id: String,
    val type: ClassType,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val name: String,
    val lecturers: List<String> = emptyList(),
    val lecturersId: List<Int> = emptyList(),
    val building: String?,
    val roomNumber: String?
)

@Serializable
data class UsosUserInfo(
    val id: String,
    val name: String,
)
