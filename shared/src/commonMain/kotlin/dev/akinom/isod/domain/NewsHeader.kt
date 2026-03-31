package dev.akinom.isod.domain

import kotlinx.datetime.LocalDateTime

data class NewsHeader(
    val id: String,
    val title: String,
    val date: LocalDateTime?,
    val author: String,
    val type: NewsType,
    val label: String,
    val isNew: Boolean = false,
)
