package dev.akinom.isod.domain

import kotlinx.datetime.LocalDateTime

data class Event(
    val id: Int,
    val title: String,
    val location: String?,
    val date: LocalDateTime,
    val description: String?,
    val imageUrl: String?
)
